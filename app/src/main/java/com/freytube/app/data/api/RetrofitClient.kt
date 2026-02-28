package com.freytube.app.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manages Retrofit clients for both Piped and Invidious APIs.
 * Uses [InstanceManager] for automatic instance rotation on failures.
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    // ── Piped API ──
    private var currentPipedBaseUrl: String = ""
    private var pipedRetrofit: Retrofit? = null
    private var pipedApi: PipedApi? = null

    // ── Invidious API ──
    private var currentInvidiousBaseUrl: String = ""
    private var invidiousRetrofit: Retrofit? = null
    private var invidiousApi: InvidiousApi? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "FreyTube/1.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Get a PipedApi client pointing at the given (or current best) instance.
     */
    fun getApi(baseUrl: String? = null): PipedApi {
        val url = ensureTrailingSlash(baseUrl ?: InstanceManager.getCurrentPipedInstance())
        if (pipedApi == null || currentPipedBaseUrl != url) {
            currentPipedBaseUrl = url
            pipedRetrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            pipedApi = pipedRetrofit!!.create(PipedApi::class.java)
            Log.i(TAG, "Piped API → $url")
        }
        return pipedApi!!
    }

    /**
     * Get an InvidiousApi client pointing at the given (or current best) instance.
     */
    fun getInvidiousApi(baseUrl: String? = null): InvidiousApi {
        val url = ensureTrailingSlash(baseUrl ?: InstanceManager.getCurrentInvidiousInstance())
        if (invidiousApi == null || currentInvidiousBaseUrl != url) {
            currentInvidiousBaseUrl = url
            invidiousRetrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            invidiousApi = invidiousRetrofit!!.create(InvidiousApi::class.java)
            Log.i(TAG, "Invidious API → $url")
        }
        return invidiousApi!!
    }

    /**
     * Force-rebuild the Piped API client with a new base URL.
     * Called when the instance manager rotates to a new instance.
     */
    fun rebuildPipedApi(baseUrl: String): PipedApi {
        val url = ensureTrailingSlash(baseUrl)
        currentPipedBaseUrl = url
        pipedRetrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        pipedApi = pipedRetrofit!!.create(PipedApi::class.java)
        Log.i(TAG, "Rebuilt Piped API → $url")
        return pipedApi!!
    }

    /**
     * Force-rebuild the Invidious API client with a new base URL.
     */
    fun rebuildInvidiousApi(baseUrl: String): InvidiousApi {
        val url = ensureTrailingSlash(baseUrl)
        currentInvidiousBaseUrl = url
        invidiousRetrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        invidiousApi = invidiousRetrofit!!.create(InvidiousApi::class.java)
        Log.i(TAG, "Rebuilt Invidious API → $url")
        return invidiousApi!!
    }

    fun getOkHttpClient(): OkHttpClient = okHttpClient

    fun getCurrentPipedBaseUrl(): String = currentPipedBaseUrl

    fun getCurrentInvidiousBaseUrl(): String = currentInvidiousBaseUrl

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
