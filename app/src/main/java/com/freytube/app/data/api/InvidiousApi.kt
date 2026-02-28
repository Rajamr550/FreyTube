package com.freytube.app.data.api

import com.freytube.app.data.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Invidious REST API — used as a fallback when all Piped instances are down.
 * Endpoints mirror Piped but response shapes differ slightly; the repository
 * layer maps Invidious responses → existing Piped data models.
 *
 * Reference: https://docs.invidious.io/api/
 */
interface InvidiousApi {

    // ═══════════════════════════════════════════════════════════
    // Trending
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/trending")
    suspend fun getTrending(
        @Query("region") region: String = "US"
    ): List<InvidiousVideoItem>

    // ═══════════════════════════════════════════════════════════
    // Video Details
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/videos/{videoId}")
    suspend fun getVideo(
        @Path("videoId") videoId: String
    ): InvidiousVideoDetail

    // ═══════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("page") page: Int = 1
    ): List<InvidiousVideoItem>

    // ═══════════════════════════════════════════════════════════
    // Search Suggestions
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/search/suggestions")
    suspend fun getSuggestions(
        @Query("q") query: String
    ): InvidiousSuggestions

    // ═══════════════════════════════════════════════════════════
    // Comments
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/comments/{videoId}")
    suspend fun getComments(
        @Path("videoId") videoId: String,
        @Query("continuation") continuation: String? = null
    ): InvidiousCommentsResponse

    // ═══════════════════════════════════════════════════════════
    // Channel
    // ═══════════════════════════════════════════════════════════

    @GET("api/v1/channels/{channelId}")
    suspend fun getChannel(
        @Path("channelId") channelId: String
    ): InvidiousChannel
}

// ═══════════════════════════════════════════════════════════════
// Invidious response data classes (mapped → Piped models in repo)
// ═══════════════════════════════════════════════════════════════

data class InvidiousVideoItem(
    @SerializedName("type") val type: String = "video",
    @SerializedName("title") val title: String = "",
    @SerializedName("videoId") val videoId: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("authorId") val authorId: String = "",
    @SerializedName("authorUrl") val authorUrl: String = "",
    @SerializedName("videoThumbnails") val videoThumbnails: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("description") val description: String = "",
    @SerializedName("viewCount") val viewCount: Long = 0,
    @SerializedName("published") val published: Long = 0,
    @SerializedName("publishedText") val publishedText: String = "",
    @SerializedName("lengthSeconds") val lengthSeconds: Long = 0,
    @SerializedName("liveNow") val liveNow: Boolean = false,
    @SerializedName("isUpcoming") val isUpcoming: Boolean = false,
    @SerializedName("authorVerified") val authorVerified: Boolean = false
)

data class InvidiousThumbnail(
    @SerializedName("quality") val quality: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class InvidiousVideoDetail(
    @SerializedName("title") val title: String = "",
    @SerializedName("videoId") val videoId: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("descriptionHtml") val descriptionHtml: String = "",
    @SerializedName("published") val published: Long = 0,
    @SerializedName("publishedText") val publishedText: String = "",
    @SerializedName("viewCount") val viewCount: Long = 0,
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("dislikeCount") val dislikeCount: Long = 0,
    @SerializedName("author") val author: String = "",
    @SerializedName("authorId") val authorId: String = "",
    @SerializedName("authorUrl") val authorUrl: String = "",
    @SerializedName("authorThumbnails") val authorThumbnails: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("videoThumbnails") val videoThumbnails: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("subCountText") val subCountText: String = "",
    @SerializedName("lengthSeconds") val lengthSeconds: Long = 0,
    @SerializedName("hlsUrl") val hlsUrl: String? = null,
    @SerializedName("dashUrl") val dashUrl: String? = null,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<InvidiousAdaptiveFormat> = emptyList(),
    @SerializedName("formatStreams") val formatStreams: List<InvidiousFormatStream> = emptyList(),
    @SerializedName("recommendedVideos") val recommendedVideos: List<InvidiousVideoItem> = emptyList(),
    @SerializedName("liveNow") val liveNow: Boolean = false,
    @SerializedName("genre") val genre: String = "",
    @SerializedName("subCount") val subCount: Long = 0,
    @SerializedName("authorVerified") val authorVerified: Boolean = false
)

data class InvidiousAdaptiveFormat(
    @SerializedName("index") val index: String? = null,
    @SerializedName("bitrate") val bitrate: String? = null,
    @SerializedName("init") val init: String? = null,
    @SerializedName("url") val url: String = "",
    @SerializedName("itag") val itag: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("clen") val clen: String? = null,
    @SerializedName("encoding") val encoding: String? = null,
    @SerializedName("qualityLabel") val qualityLabel: String? = null,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("container") val container: String? = null,
    @SerializedName("fps") val fps: Int? = null,
    @SerializedName("audioQuality") val audioQuality: String? = null,
    @SerializedName("audioSampleRate") val audioSampleRate: Int? = null,
    @SerializedName("audioChannels") val audioChannels: Int? = null
) {
    val isAudio: Boolean
        get() = type.startsWith("audio/")

    val isVideo: Boolean
        get() = type.startsWith("video/")

    val heightFromResolution: Int?
        get() = resolution?.removeSuffix("p")?.toIntOrNull()
}

data class InvidiousFormatStream(
    @SerializedName("url") val url: String = "",
    @SerializedName("itag") val itag: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("quality") val quality: String = "",
    @SerializedName("qualityLabel") val qualityLabel: String? = null,
    @SerializedName("container") val container: String? = null,
    @SerializedName("encoding") val encoding: String? = null,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("fps") val fps: Int? = null
)

data class InvidiousSuggestions(
    @SerializedName("query") val query: String = "",
    @SerializedName("suggestions") val suggestions: List<String> = emptyList()
)

data class InvidiousCommentsResponse(
    @SerializedName("commentCount") val commentCount: Int? = null,
    @SerializedName("comments") val comments: List<InvidiousComment> = emptyList(),
    @SerializedName("continuation") val continuation: String? = null
)

data class InvidiousComment(
    @SerializedName("author") val author: String = "",
    @SerializedName("authorThumbnails") val authorThumbnails: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("authorId") val authorId: String = "",
    @SerializedName("authorUrl") val authorUrl: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("contentHtml") val contentHtml: String = "",
    @SerializedName("published") val published: Long = 0,
    @SerializedName("publishedText") val publishedText: String = "",
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("commentId") val commentId: String = "",
    @SerializedName("authorIsChannelOwner") val authorIsChannelOwner: Boolean = false,
    @SerializedName("creatorHeart") val creatorHeart: Any? = null,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("replies") val replies: InvidiousCommentReplies? = null
)

data class InvidiousCommentReplies(
    @SerializedName("replyCount") val replyCount: Int = 0,
    @SerializedName("continuation") val continuation: String? = null
)

data class InvidiousChannel(
    @SerializedName("author") val author: String = "",
    @SerializedName("authorId") val authorId: String = "",
    @SerializedName("authorUrl") val authorUrl: String = "",
    @SerializedName("authorBanners") val authorBanners: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("authorThumbnails") val authorThumbnails: List<InvidiousThumbnail> = emptyList(),
    @SerializedName("subCount") val subCount: Long = 0,
    @SerializedName("totalViews") val totalViews: Long = 0,
    @SerializedName("description") val description: String = "",
    @SerializedName("descriptionHtml") val descriptionHtml: String = "",
    @SerializedName("isFamilyFriendly") val isFamilyFriendly: Boolean = true,
    @SerializedName("latestVideos") val latestVideos: List<InvidiousVideoItem> = emptyList(),
    @SerializedName("autoGenerated") val autoGenerated: Boolean = false,
    @SerializedName("tabs") val tabs: List<String> = emptyList()
)
