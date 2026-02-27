package com.freytube.app.data.api

import com.freytube.app.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PipedApi {

    // ═══════════════════════════════════════════════════════════════
    // Trending
    // ═══════════════════════════════════════════════════════════════

    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String = "US"
    ): List<StreamItem>

    // ═══════════════════════════════════════════════════════════════
    // Video Streams
    // ═══════════════════════════════════════════════════════════════

    @GET("streams/{videoId}")
    suspend fun getStreams(
        @Path("videoId") videoId: String
    ): VideoStream

    // ═══════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "all"
    ): SearchResponse

    @GET("nextpage/search")
    suspend fun searchNextPage(
        @Query("q") query: String,
        @Query("filter") filter: String = "all",
        @Query("nextpage") nextpage: String
    ): SearchResponse

    // ═══════════════════════════════════════════════════════════════
    // Search Suggestions
    // ═══════════════════════════════════════════════════════════════

    @GET("suggestions")
    suspend fun getSuggestions(
        @Query("query") query: String
    ): SuggestionsResponse

    // ═══════════════════════════════════════════════════════════════
    // Channel
    // ═══════════════════════════════════════════════════════════════

    @GET("channel/{channelId}")
    suspend fun getChannel(
        @Path("channelId") channelId: String
    ): Channel

    @GET("nextpage/channel/{channelId}")
    suspend fun getChannelNextPage(
        @Path("channelId") channelId: String,
        @Query("nextpage") nextpage: String
    ): Channel

    // ═══════════════════════════════════════════════════════════════
    // Comments
    // ═══════════════════════════════════════════════════════════════

    @GET("comments/{videoId}")
    suspend fun getComments(
        @Path("videoId") videoId: String
    ): CommentsResponse

    @GET("nextpage/comments/{videoId}")
    suspend fun getCommentsNextPage(
        @Path("videoId") videoId: String,
        @Query("nextpage") nextpage: String
    ): CommentsResponse
}
