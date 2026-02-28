package com.freytube.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages Piped & Invidious API instances with automatic failover.
 * Inspired by how NewPipe/LibreTube handle instance rotation.
 *
 * Strategy:
 *  1. Try current instance
 *  2. On 502/503/timeout → rotate to next healthy Piped instance
 *  3. If all Piped instances fail → fall back to Invidious instances
 *  4. Periodically refresh instance list from upstream
 */
object InstanceManager {

    private const val TAG = "InstanceManager"

    // ── Piped instance list endpoint ──
    private const val PIPED_INSTANCES_URL = "https://piped-instances.kavin.rocks/"

    // ── Invidious instance list endpoint ──
    private const val INVIDIOUS_INSTANCES_URL = "https://api.invidious.io/instances.json?sort_by=type,health"

    // ── Defaults (fallback if fetching instance list fails) ──
    private val DEFAULT_PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.r4fo.com",
        "https://api.piped.projectsegfau.lt",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.moomoo.me",
        "https://pipedapi.darkness.services",
        "https://pipedapi.drgns.space"
    )

    private val DEFAULT_INVIDIOUS_INSTANCES = listOf(
        "https://invidious.nerdvpn.de",
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.materialio.us",
        "https://invidious.privacyredirect.com",
        "https://invidious.protokolla.fi"
    )

    // ── State ──
    private var pipedInstances: List<String> = DEFAULT_PIPED_INSTANCES
    private var invidiousInstances: List<String> = DEFAULT_INVIDIOUS_INSTANCES
    private var currentPipedIndex = 0
    private var currentInvidiousIndex = 0

    // Track failures: instance URL → consecutive failure count
    private val failureCounts = ConcurrentHashMap<String, Int>()
    // Track cooldowns: instance URL → timestamp when it can be retried
    private val cooldowns = ConcurrentHashMap<String, Long>()

    private const val MAX_FAILURES_BEFORE_COOLDOWN = 3
    private const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L  // 5 minutes

    private val mutex = Mutex()
    private var instancesFetched = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // ═══════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════

    /**
     * Get the current best Piped API base URL.
     */
    fun getCurrentPipedInstance(): String {
        val now = System.currentTimeMillis()
        val available = pipedInstances.filter { url ->
            val cooldownEnd = cooldowns[url] ?: 0L
            cooldownEnd <= now
        }
        if (available.isEmpty()) {
            // All in cooldown → clear cooldowns and retry all
            cooldowns.clear()
            failureCounts.clear()
            return pipedInstances.firstOrNull() ?: DEFAULT_PIPED_INSTANCES.first()
        }
        return available.getOrElse(currentPipedIndex % available.size) { available.first() }
    }

    /**
     * Get the current best Invidious API base URL.
     */
    fun getCurrentInvidiousInstance(): String {
        val now = System.currentTimeMillis()
        val available = invidiousInstances.filter { url ->
            val cooldownEnd = cooldowns[url] ?: 0L
            cooldownEnd <= now
        }
        if (available.isEmpty()) {
            cooldowns.clear()
            failureCounts.clear()
            return invidiousInstances.firstOrNull() ?: DEFAULT_INVIDIOUS_INSTANCES.first()
        }
        return available.getOrElse(currentInvidiousIndex % available.size) { available.first() }
    }

    /**
     * Report that an instance returned an error (502, 503, timeout, etc.).
     * Increments failure count and puts instance in cooldown if needed.
     */
    fun reportFailure(instanceUrl: String) {
        val normalized = normalizeUrl(instanceUrl)
        val count = (failureCounts[normalized] ?: 0) + 1
        failureCounts[normalized] = count
        Log.w(TAG, "Instance failure #$count: $normalized")

        if (count >= MAX_FAILURES_BEFORE_COOLDOWN) {
            cooldowns[normalized] = System.currentTimeMillis() + COOLDOWN_DURATION_MS
            Log.w(TAG, "Instance in cooldown for 5 min: $normalized")
        }
    }

    /**
     * Report that an instance succeeded. Resets its failure count.
     */
    fun reportSuccess(instanceUrl: String) {
        val normalized = normalizeUrl(instanceUrl)
        failureCounts.remove(normalized)
        cooldowns.remove(normalized)
    }

    /**
     * Rotate to the next available Piped instance.
     * @return the new instance URL, or null if all exhausted
     */
    fun rotateToNextPipedInstance(): String? {
        val now = System.currentTimeMillis()
        val available = pipedInstances.filter { url ->
            val cooldownEnd = cooldowns[url] ?: 0L
            cooldownEnd <= now
        }
        if (available.size <= 1) return null  // No alternative available

        currentPipedIndex = (currentPipedIndex + 1) % available.size
        val next = available[currentPipedIndex]
        Log.i(TAG, "Rotated to Piped instance: $next")
        return next
    }

    /**
     * Rotate to the next available Invidious instance.
     * @return the new instance URL, or null if all exhausted
     */
    fun rotateToNextInvidiousInstance(): String? {
        val now = System.currentTimeMillis()
        val available = invidiousInstances.filter { url ->
            val cooldownEnd = cooldowns[url] ?: 0L
            cooldownEnd <= now
        }
        if (available.size <= 1) return null

        currentInvidiousIndex = (currentInvidiousIndex + 1) % available.size
        val next = available[currentInvidiousIndex]
        Log.i(TAG, "Rotated to Invidious instance: $next")
        return next
    }

    /**
     * Check whether all Piped instances are exhausted / in cooldown.
     */
    fun allPipedInstancesExhausted(): Boolean {
        val now = System.currentTimeMillis()
        return pipedInstances.all { url ->
            val cooldownEnd = cooldowns[url] ?: 0L
            cooldownEnd > now
        }
    }

    /**
     * Fetch live instance lists from upstream. Call once on app startup.
     */
    suspend fun refreshInstances() {
        if (instancesFetched) return
        mutex.withLock {
            if (instancesFetched) return
            withContext(Dispatchers.IO) {
                try {
                    fetchPipedInstances()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch Piped instances, using defaults", e)
                }
                try {
                    fetchInvidiousInstances()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch Invidious instances, using defaults", e)
                }
            }
            instancesFetched = true
        }
    }

    /**
     * Set a user-preferred instance (from settings). Moves it to front of list.
     */
    fun setPreferredInstance(instanceUrl: String) {
        val normalized = normalizeUrl(instanceUrl)
        val mutable = pipedInstances.toMutableList()
        mutable.remove(normalized)
        mutable.add(0, normalized)
        pipedInstances = mutable
        currentPipedIndex = 0
        Log.i(TAG, "User preferred instance set: $normalized")
    }

    /**
     * Get the total count of available Piped instances.
     */
    fun getPipedInstanceCount(): Int = pipedInstances.size

    /**
     * Get the total count of available Invidious instances.
     */
    fun getInvidiousInstanceCount(): Int = invidiousInstances.size

    // ═══════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════

    private fun fetchPipedInstances() {
        val request = Request.Builder().url(PIPED_INSTANCES_URL).build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return
            try {
                val instances = Gson().fromJson(body, Array<PipedInstanceInfo>::class.java)
                val urls = instances
                    .filter { it.apiUrl.isNotBlank() }
                    .sortedByDescending { it.upToDate }
                    .map { normalizeUrl(it.apiUrl) }
                    .distinct()

                if (urls.isNotEmpty()) {
                    pipedInstances = urls
                    Log.i(TAG, "Fetched ${urls.size} Piped instances")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Piped instances JSON", e)
            }
        }
        response.close()
    }

    private fun fetchInvidiousInstances() {
        val request = Request.Builder().url(INVIDIOUS_INSTANCES_URL).build()
        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return
            try {
                // Invidious returns: [ ["domain", { ... }], ... ]
                val rawArray = Gson().fromJson(body, Array<Array<Any>>::class.java)
                val urls = rawArray
                    .mapNotNull { entry ->
                        val domain = entry.getOrNull(0) as? String
                        if (domain != null) "https://$domain" else null
                    }
                    .take(15)  // limit to top 15

                if (urls.isNotEmpty()) {
                    invidiousInstances = urls
                    Log.i(TAG, "Fetched ${urls.size} Invidious instances")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Invidious instances JSON", e)
            }
        }
        response.close()
    }

    private fun normalizeUrl(url: String): String {
        return url.trimEnd('/')
    }

    // ── Data classes for parsing ──

    private data class PipedInstanceInfo(
        @SerializedName("name") val name: String = "",
        @SerializedName("api_url") val apiUrl: String = "",
        @SerializedName("locations") val locations: String = "",
        @SerializedName("up_to_date") val upToDate: Boolean = true,
        @SerializedName("cdn") val cdn: Boolean = false,
        @SerializedName("registered") val registered: Int = 0
    )
}
