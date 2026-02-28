package com.freytube.app.data.repository

import android.util.Log
import com.freytube.app.data.api.*
import com.freytube.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repository with automatic Piped instance rotation + Invidious fallback.
 *
 * Flow on every API call:
 *  1. Try current Piped instance
 *  2. On 502/503/timeout → rotate to next Piped instance (up to N tries)
 *  3. If all Piped instances fail → try Invidious (up to N tries)
 *  4. If everything fails → return the last error
 */
class PipedRepository {

    companion object {
        private const val TAG = "PipedRepository"
        private const val MAX_PIPED_RETRIES = 4
        private const val MAX_INVIDIOUS_RETRIES = 3
    }

    // ═══════════════════════════════════════════════════════════
    // Core retry-with-failover wrapper
    // ═══════════════════════════════════════════════════════════

    /**
     * Execute a Piped API call with automatic instance rotation on failure.
     * If all Piped instances fail, falls back to [invidiousFallback].
     */
    private suspend fun <T> withFailover(
        invidiousFallback: (suspend () -> T)? = null,
        pipedCall: suspend (PipedApi) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        // ── Phase 1: Try Piped instances ──
        for (attempt in 1..MAX_PIPED_RETRIES) {
            val instanceUrl = InstanceManager.getCurrentPipedInstance()
            try {
                val api = RetrofitClient.getApi(instanceUrl)
                val result = pipedCall(api)
                InstanceManager.reportSuccess(instanceUrl)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastException = e
                val shouldRotate = isRetryableError(e)
                Log.w(TAG, "Piped attempt $attempt failed ($instanceUrl): ${e.message}")

                if (shouldRotate) {
                    InstanceManager.reportFailure(instanceUrl)
                    val next = InstanceManager.rotateToNextPipedInstance()
                    if (next != null) {
                        RetrofitClient.rebuildPipedApi(next)
                        Log.i(TAG, "Rotating to Piped instance: $next")
                    } else {
                        Log.w(TAG, "No more Piped instances to try")
                        break
                    }
                } else {
                    // Non-retryable error (e.g. 404) — don't rotate
                    return@withContext Result.failure(e)
                }
            }
        }

        // ── Phase 2: Invidious fallback ──
        if (invidiousFallback != null) {
            for (attempt in 1..MAX_INVIDIOUS_RETRIES) {
                val instanceUrl = InstanceManager.getCurrentInvidiousInstance()
                try {
                    val result = invidiousFallback()
                    InstanceManager.reportSuccess(instanceUrl)
                    Log.i(TAG, "Invidious fallback succeeded ($instanceUrl)")
                    return@withContext Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Invidious attempt $attempt failed ($instanceUrl): ${e.message}")

                    if (isRetryableError(e)) {
                        InstanceManager.reportFailure(instanceUrl)
                        val next = InstanceManager.rotateToNextInvidiousInstance()
                        if (next != null) {
                            RetrofitClient.rebuildInvidiousApi(next)
                        } else {
                            break
                        }
                    } else {
                        return@withContext Result.failure(e)
                    }
                }
            }
        }

        Result.failure(lastException ?: IOException("All instances exhausted"))
    }

    /**
     * Determines if an error should trigger instance rotation.
     */
    private fun isRetryableError(e: Exception): Boolean {
        return when {
            e is HttpException && e.code() in listOf(502, 503, 504, 520, 521, 522, 523, 524) -> true
            e is SocketTimeoutException -> true
            e is IOException && e.message?.contains("timeout", ignoreCase = true) == true -> true
            e is IOException && e.message?.contains("reset", ignoreCase = true) == true -> true
            e is IOException && e.message?.contains("refused", ignoreCase = true) == true -> true
            else -> false
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Invidious → Piped model mappers
    // ═══════════════════════════════════════════════════════════

    private fun InvidiousVideoItem.toStreamItem(): StreamItem {
        val thumbUrl = videoThumbnails
            .firstOrNull { it.quality == "medium" }?.url
            ?: videoThumbnails.firstOrNull()?.url ?: ""

        return StreamItem(
            url = "/watch?v=$videoId",
            type = if (type == "video") "stream" else type,
            title = title,
            thumbnail = thumbUrl,
            uploaderName = author,
            uploaderUrl = authorUrl,
            uploaderAvatar = "",
            uploadedDate = publishedText,
            shortDescription = description.take(200),
            duration = lengthSeconds,
            views = viewCount,
            uploaded = published * 1000,
            uploaderVerified = authorVerified,
            isShort = lengthSeconds in 1..60
        )
    }

    private fun InvidiousVideoDetail.toVideoStream(): VideoStream {
        val thumbUrl = videoThumbnails
            .firstOrNull { it.quality == "medium" }?.url
            ?: videoThumbnails.firstOrNull()?.url ?: ""

        val avatarUrl = authorThumbnails.firstOrNull()?.url ?: ""

        val audioStreams = adaptiveFormats
            .filter { it.isAudio }
            .map { fmt ->
                PipedStream(
                    url = fmt.url,
                    format = fmt.container ?: "webm",
                    quality = fmt.audioQuality,
                    mimeType = fmt.type,
                    codec = fmt.encoding,
                    videoOnly = false,
                    bitrate = fmt.bitrate?.toIntOrNull(),
                    contentLength = fmt.clen?.toLongOrNull()
                )
            }

        val videoOnlyStreams = adaptiveFormats
            .filter { it.isVideo }
            .map { fmt ->
                PipedStream(
                    url = fmt.url,
                    format = fmt.container ?: "webm",
                    quality = fmt.qualityLabel ?: fmt.resolution,
                    mimeType = fmt.type,
                    codec = fmt.encoding,
                    videoOnly = true,
                    bitrate = fmt.bitrate?.toIntOrNull(),
                    width = null,
                    height = fmt.heightFromResolution,
                    fps = fmt.fps,
                    contentLength = fmt.clen?.toLongOrNull()
                )
            }

        val combinedStreams = formatStreams.map { fmt ->
            PipedStream(
                url = fmt.url,
                format = fmt.container ?: "mp4",
                quality = fmt.qualityLabel ?: fmt.quality,
                mimeType = fmt.type,
                codec = fmt.encoding,
                videoOnly = false,
                width = null,
                height = fmt.resolution?.removeSuffix("p")?.toIntOrNull(),
                fps = fmt.fps
            )
        }

        return VideoStream(
            title = title,
            description = description,
            uploadDate = publishedText,
            uploader = author,
            uploaderUrl = authorUrl,
            uploaderAvatar = avatarUrl,
            thumbnailUrl = thumbUrl,
            hls = hlsUrl,
            dash = dashUrl,
            category = genre,
            uploaderVerified = authorVerified,
            duration = lengthSeconds,
            views = viewCount,
            likes = likeCount,
            dislikes = dislikeCount,
            audioStreams = audioStreams,
            videoStreams = combinedStreams + videoOnlyStreams,
            relatedStreams = recommendedVideos.map { it.toStreamItem() },
            subtitles = emptyList(),
            livestream = liveNow,
            proxyUrl = "",
            chapters = emptyList(),
            uploaderSubscriberCount = subCount,
            previewFrames = emptyList()
        )
    }

    private fun InvidiousCommentsResponse.toCommentsResponse(): CommentsResponse {
        return CommentsResponse(
            comments = comments.map { c ->
                Comment(
                    author = c.author,
                    thumbnail = c.authorThumbnails.firstOrNull()?.url ?: "",
                    commentId = c.commentId,
                    commentText = c.contentHtml.ifBlank { c.content },
                    commentedTime = c.publishedText,
                    commentorUrl = c.authorUrl,
                    likeCount = c.likeCount,
                    hearted = c.creatorHeart != null,
                    pinned = c.isPinned,
                    verified = false,
                    replyCount = c.replies?.replyCount ?: 0,
                    repliesPage = c.replies?.continuation,
                    creatorReplied = c.authorIsChannelOwner
                )
            },
            nextpage = continuation,
            disabled = false
        )
    }

    private fun InvidiousChannel.toChannel(): Channel {
        val avatarUrl = authorThumbnails
            .maxByOrNull { it.width }?.url ?: ""
        val bannerUrl = authorBanners
            .maxByOrNull { it.width }?.url ?: ""

        return Channel(
            id = authorId,
            name = author,
            avatarUrl = avatarUrl,
            bannerUrl = bannerUrl,
            description = description,
            nextpage = null,
            subscriberCount = subCount,
            verified = false,
            relatedStreams = latestVideos.map { it.toStreamItem() }
        )
    }

    // ═══════════════════════════════════════════════════════════
    // Trending
    // ═══════════════════════════════════════════════════════════

    suspend fun getTrending(region: String = "US"): Result<List<StreamItem>> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getTrending(region).map { it.toStreamItem() }
            }
        ) { api ->
            api.getTrending(region)
        }

    // ═══════════════════════════════════════════════════════════
    // Video Streams
    // ═══════════════════════════════════════════════════════════

    suspend fun getStreams(videoId: String): Result<VideoStream> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getVideo(videoId).toVideoStream()
            }
        ) { api ->
            api.getStreams(videoId)
        }

    // ═══════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════

    suspend fun search(query: String, filter: String = "all"): Result<SearchResponse> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                val items = api.search(query).map { it.toStreamItem() }
                SearchResponse(items = items, nextpage = null)
            }
        ) { api ->
            api.search(query, filter)
        }

    suspend fun searchNextPage(query: String, filter: String, nextpage: String): Result<SearchResponse> =
        withFailover { api ->
            api.searchNextPage(query, filter, nextpage)
        }

    // ═══════════════════════════════════════════════════════════
    // Suggestions
    // ═══════════════════════════════════════════════════════════

    suspend fun getSuggestions(query: String): Result<List<String>> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getSuggestions(query).suggestions
            }
        ) { api ->
            api.getSuggestions(query)
        }

    // ═══════════════════════════════════════════════════════════
    // Channel
    // ═══════════════════════════════════════════════════════════

    suspend fun getChannel(channelId: String): Result<Channel> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getChannel(channelId).toChannel()
            }
        ) { api ->
            api.getChannel(channelId)
        }

    suspend fun getChannelNextPage(channelId: String, nextpage: String): Result<Channel> =
        withFailover { api ->
            api.getChannelNextPage(channelId, nextpage)
        }

    // ═══════════════════════════════════════════════════════════
    // Comments
    // ═══════════════════════════════════════════════════════════

    suspend fun getComments(videoId: String): Result<CommentsResponse> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getComments(videoId).toCommentsResponse()
            }
        ) { api ->
            api.getComments(videoId)
        }

    suspend fun getCommentsNextPage(videoId: String, nextpage: String): Result<CommentsResponse> =
        withFailover(
            invidiousFallback = {
                val api = RetrofitClient.getInvidiousApi()
                api.getComments(videoId, nextpage).toCommentsResponse()
            }
        ) { api ->
            api.getCommentsNextPage(videoId, nextpage)
        }
}
