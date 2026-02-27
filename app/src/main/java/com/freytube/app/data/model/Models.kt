package com.freytube.app.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════
// Trending / Search Result Video Item
// ═══════════════════════════════════════════════════════════════

data class StreamItem(
    @SerializedName("url") val url: String = "",
    @SerializedName("type") val type: String = "stream",
    @SerializedName("title") val title: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("uploaderName") val uploaderName: String = "",
    @SerializedName("uploaderUrl") val uploaderUrl: String = "",
    @SerializedName("uploaderAvatar") val uploaderAvatar: String = "",
    @SerializedName("uploadedDate") val uploadedDate: String = "",
    @SerializedName("shortDescription") val shortDescription: String? = null,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("views") val views: Long = 0,
    @SerializedName("uploaded") val uploaded: Long = 0,
    @SerializedName("uploaderVerified") val uploaderVerified: Boolean = false,
    @SerializedName("isShort") val isShort: Boolean = false
) {
    val videoId: String
        get() = url.removePrefix("/watch?v=")

    val formattedDuration: String
        get() {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedViews: String
        get() = when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
}

// ═══════════════════════════════════════════════════════════════
// Video Stream Details (Full video info)
// ═══════════════════════════════════════════════════════════════

data class VideoStream(
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("uploadDate") val uploadDate: String = "",
    @SerializedName("uploader") val uploader: String = "",
    @SerializedName("uploaderUrl") val uploaderUrl: String = "",
    @SerializedName("uploaderAvatar") val uploaderAvatar: String = "",
    @SerializedName("thumbnailUrl") val thumbnailUrl: String = "",
    @SerializedName("hls") val hls: String? = null,
    @SerializedName("dash") val dash: String? = null,
    @SerializedName("category") val category: String = "",
    @SerializedName("uploaderVerified") val uploaderVerified: Boolean = false,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("views") val views: Long = 0,
    @SerializedName("likes") val likes: Long = 0,
    @SerializedName("dislikes") val dislikes: Long = -1,
    @SerializedName("audioStreams") val audioStreams: List<PipedStream> = emptyList(),
    @SerializedName("videoStreams") val videoStreams: List<PipedStream> = emptyList(),
    @SerializedName("relatedStreams") val relatedStreams: List<StreamItem> = emptyList(),
    @SerializedName("subtitles") val subtitles: List<Subtitle> = emptyList(),
    @SerializedName("livestream") val livestream: Boolean = false,
    @SerializedName("proxyUrl") val proxyUrl: String = "",
    @SerializedName("chapters") val chapters: List<Chapter> = emptyList(),
    @SerializedName("uploaderSubscriberCount") val uploaderSubscriberCount: Long = 0,
    @SerializedName("previewFrames") val previewFrames: List<PreviewFrame> = emptyList()
) {
    val formattedViews: String
        get() = when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }

    val formattedLikes: String
        get() = when {
            likes >= 1_000_000 -> String.format("%.1fM", likes / 1_000_000.0)
            likes >= 1_000 -> String.format("%.1fK", likes / 1_000.0)
            else -> "$likes"
        }

    val formattedSubscribers: String
        get() = when {
            uploaderSubscriberCount >= 1_000_000 -> String.format("%.1fM subscribers", uploaderSubscriberCount / 1_000_000.0)
            uploaderSubscriberCount >= 1_000 -> String.format("%.1fK subscribers", uploaderSubscriberCount / 1_000.0)
            else -> "$uploaderSubscriberCount subscribers"
        }

    // Get best quality video streams sorted by resolution
    val sortedVideoStreams: List<PipedStream>
        get() = videoStreams
            .filter { it.videoOnly == false }
            .sortedByDescending { extractResolution(it.quality) }

    // Get video-only streams (for DASH - combine with audio)
    val videoOnlyStreams: List<PipedStream>
        get() = videoStreams
            .filter { it.videoOnly == true }
            .sortedByDescending { extractResolution(it.quality) }

    // Get best audio stream
    val bestAudioStream: PipedStream?
        get() = audioStreams.maxByOrNull { it.bitrate ?: 0 }

    private fun extractResolution(quality: String?): Int {
        return quality?.replace("p", "")?.replace("p60", "")?.toIntOrNull() ?: 0
    }
}

// ═══════════════════════════════════════════════════════════════
// Stream Quality Info
// ═══════════════════════════════════════════════════════════════

data class PipedStream(
    @SerializedName("url") val url: String = "",
    @SerializedName("format") val format: String = "",
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("mimeType") val mimeType: String = "",
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("videoOnly") val videoOnly: Boolean? = false,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("initStart") val initStart: Int? = null,
    @SerializedName("initEnd") val initEnd: Int? = null,
    @SerializedName("indexStart") val indexStart: Int? = null,
    @SerializedName("indexEnd") val indexEnd: Int? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("fps") val fps: Int? = null,
    @SerializedName("contentLength") val contentLength: Long? = null
) {
    val qualityLabel: String
        get() = quality ?: "${height}p"

    val isHighRes: Boolean
        get() = (height ?: 0) >= 1080

    val is4K: Boolean
        get() = (height ?: 0) >= 2160

    val is8K: Boolean
        get() = (height ?: 0) >= 4320

    val formattedSize: String
        get() {
            val bytes = contentLength ?: return "Unknown"
            return when {
                bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
                bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
                else -> "$bytes B"
            }
        }
}

// ═══════════════════════════════════════════════════════════════
// Subtitle
// ═══════════════════════════════════════════════════════════════

data class Subtitle(
    @SerializedName("url") val url: String = "",
    @SerializedName("mimeType") val mimeType: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("autoGenerated") val autoGenerated: Boolean = false
)

// ═══════════════════════════════════════════════════════════════
// Chapter
// ═══════════════════════════════════════════════════════════════

data class Chapter(
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("start") val start: Long = 0
)

// ═══════════════════════════════════════════════════════════════
// Preview Frame
// ═══════════════════════════════════════════════════════════════

data class PreviewFrame(
    @SerializedName("urls") val urls: List<String> = emptyList(),
    @SerializedName("frameWidth") val frameWidth: Int = 0,
    @SerializedName("frameHeight") val frameHeight: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("durationPerFrame") val durationPerFrame: Long = 0,
    @SerializedName("framesPerPageX") val framesPerPageX: Int = 0,
    @SerializedName("framesPerPageY") val framesPerPageY: Int = 0
)

// ═══════════════════════════════════════════════════════════════
// Search Response
// ═══════════════════════════════════════════════════════════════

data class SearchResponse(
    @SerializedName("items") val items: List<StreamItem> = emptyList(),
    @SerializedName("nextpage") val nextpage: String? = null,
    @SerializedName("suggestion") val suggestion: String? = null,
    @SerializedName("corrected") val corrected: Boolean = false
)

// ═══════════════════════════════════════════════════════════════
// Channel
// ═══════════════════════════════════════════════════════════════

data class Channel(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("avatarUrl") val avatarUrl: String = "",
    @SerializedName("bannerUrl") val bannerUrl: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("nextpage") val nextpage: String? = null,
    @SerializedName("subscriberCount") val subscriberCount: Long = 0,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("relatedStreams") val relatedStreams: List<StreamItem> = emptyList()
) {
    val formattedSubscribers: String
        get() = when {
            subscriberCount >= 1_000_000 -> String.format("%.1fM", subscriberCount / 1_000_000.0)
            subscriberCount >= 1_000 -> String.format("%.1fK", subscriberCount / 1_000.0)
            else -> "$subscriberCount"
        }
}

// ═══════════════════════════════════════════════════════════════
// Comments
// ═══════════════════════════════════════════════════════════════

data class CommentsResponse(
    @SerializedName("comments") val comments: List<Comment> = emptyList(),
    @SerializedName("nextpage") val nextpage: String? = null,
    @SerializedName("disabled") val disabled: Boolean = false
)

data class Comment(
    @SerializedName("author") val author: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("commentId") val commentId: String = "",
    @SerializedName("commentText") val commentText: String = "",
    @SerializedName("commentedTime") val commentedTime: String = "",
    @SerializedName("commentorUrl") val commentorUrl: String = "",
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("hearted") val hearted: Boolean = false,
    @SerializedName("pinned") val pinned: Boolean = false,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("replyCount") val replyCount: Int = 0,
    @SerializedName("repliesPage") val repliesPage: String? = null,
    @SerializedName("creatorReplied") val creatorReplied: Boolean = false
)

// ═══════════════════════════════════════════════════════════════
// SponsorBlock Segments
// ═══════════════════════════════════════════════════════════════

data class SponsorBlockSegment(
    @SerializedName("category") val category: String = "",
    @SerializedName("actionType") val actionType: String = "",
    @SerializedName("segment") val segment: List<Double> = emptyList()
) {
    val startTime: Double get() = segment.getOrElse(0) { 0.0 }
    val endTime: Double get() = segment.getOrElse(1) { 0.0 }
}

// ═══════════════════════════════════════════════════════════════
// Piped Instances
// ═══════════════════════════════════════════════════════════════

data class PipedInstance(
    @SerializedName("name") val name: String = "",
    @SerializedName("api_url") val apiUrl: String = "",
    @SerializedName("locations") val locations: String = "",
    @SerializedName("version") val version: String = "",
    @SerializedName("up_to_date") val upToDate: Boolean = true,
    @SerializedName("cdn") val cdn: Boolean = false,
    @SerializedName("registered") val registered: Int = 0,
    @SerializedName("last_checked") val lastChecked: Long = 0
)

// ═══════════════════════════════════════════════════════════════
// Suggestions Response
// ═══════════════════════════════════════════════════════════════

typealias SuggestionsResponse = List<String>
