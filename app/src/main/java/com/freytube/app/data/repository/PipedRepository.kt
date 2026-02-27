package com.freytube.app.data.repository

import com.freytube.app.data.api.PipedApi
import com.freytube.app.data.api.RetrofitClient
import com.freytube.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PipedRepository(
    private val api: PipedApi = RetrofitClient.getApi()
) {

    // ═══════════════════════════════════════════════════════════════
    // Trending
    // ═══════════════════════════════════════════════════════════════

    suspend fun getTrending(region: String = "US"): Result<List<StreamItem>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getTrending(region))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Video Streams
    // ═══════════════════════════════════════════════════════════════

    suspend fun getStreams(videoId: String): Result<VideoStream> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getStreams(videoId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════

    suspend fun search(query: String, filter: String = "all"): Result<SearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.search(query, filter))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchNextPage(query: String, filter: String, nextpage: String): Result<SearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.searchNextPage(query, filter, nextpage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Suggestions
    // ═══════════════════════════════════════════════════════════════

    suspend fun getSuggestions(query: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getSuggestions(query))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Channel
    // ═══════════════════════════════════════════════════════════════

    suspend fun getChannel(channelId: String): Result<Channel> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getChannel(channelId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getChannelNextPage(channelId: String, nextpage: String): Result<Channel> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getChannelNextPage(channelId, nextpage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Comments
    // ═══════════════════════════════════════════════════════════════

    suspend fun getComments(videoId: String): Result<CommentsResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getComments(videoId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCommentsNextPage(videoId: String, nextpage: String): Result<CommentsResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getCommentsNextPage(videoId, nextpage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
