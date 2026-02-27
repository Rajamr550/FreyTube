package com.freytube.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.freytube.app.data.local.AppDatabase
import com.freytube.app.data.local.WatchHistoryEntity
import com.freytube.app.data.model.Comment
import com.freytube.app.data.model.PipedStream
import com.freytube.app.data.model.StreamItem
import com.freytube.app.data.model.VideoStream
import com.freytube.app.data.repository.PipedRepository
import com.freytube.app.service.BackgroundPlayService
import com.freytube.app.service.DownloadService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val videoStream: VideoStream? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentVideoId: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val selectedQuality: String = "720p",
    val availableQualities: List<String> = emptyList(),
    val playbackSpeed: Float = 1.0f,
    val isFullscreen: Boolean = false,
    val showControls: Boolean = true,
    val isBackgroundPlaying: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentsLoading: Boolean = false,
    val relatedVideos: List<StreamItem> = emptyList(),
    val showQualityDialog: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val showDownloadDialog: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PipedRepository()
    private val watchHistoryDao = AppDatabase.getInstance(application).watchHistoryDao()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    fun initializePlayer() {
        if (player != null) return

        player = ExoPlayer.Builder(getApplication<Application>())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _uiState.update {
                            it.copy(
                                isPlaying = playWhenReady && state == Player.STATE_READY,
                                duration = duration.coerceAtLeast(0L)
                            )
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                    }
                })
            }

        startPositionUpdates()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                player?.let { p ->
                    _uiState.update {
                        it.copy(
                            currentPosition = p.currentPosition.coerceAtLeast(0L),
                            duration = p.duration.coerceAtLeast(0L),
                            bufferedPercentage = p.bufferedPercentage
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun loadVideo(videoId: String) {
        if (videoId == _uiState.value.currentVideoId && _uiState.value.videoStream != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentVideoId = videoId) }

            repository.getStreams(videoId).fold(
                onSuccess = { stream ->
                    val qualities = mutableListOf<String>()

                    // Add combined streams
                    stream.sortedVideoStreams.forEach { s ->
                        s.quality?.let { q -> if (q !in qualities) qualities.add(q) }
                    }

                    // Add video-only streams (higher res)
                    stream.videoOnlyStreams.forEach { s ->
                        s.quality?.let { q ->
                            val label = "$q (DASH)"
                            if (label !in qualities) qualities.add(label)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            videoStream = stream,
                            isLoading = false,
                            availableQualities = qualities,
                            relatedVideos = stream.relatedStreams,
                            error = null
                        )
                    }

                    playStream(stream)
                    saveToHistory(videoId, stream)
                    loadComments(videoId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load video"
                        )
                    }
                }
            )
        }
    }

    private fun playStream(stream: VideoStream) {
        val player = player ?: return

        // Try HLS first (adaptive), then DASH, then direct streams
        val mediaItem = when {
            !stream.hls.isNullOrEmpty() -> {
                MediaItem.Builder()
                    .setUri(stream.hls)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            }
            stream.sortedVideoStreams.isNotEmpty() -> {
                val selectedStream = findBestStream(stream)
                MediaItem.Builder()
                    .setUri(selectedStream.url)
                    .build()
            }
            else -> return
        }

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun findBestStream(stream: VideoStream): PipedStream {
        val targetQuality = _uiState.value.selectedQuality.replace("p", "").toIntOrNull() ?: 720

        // Try combined streams first
        val combined = stream.sortedVideoStreams
        val bestCombined = combined.firstOrNull {
            val h = it.height ?: it.quality?.replace("p", "")?.toIntOrNull() ?: 0
            h <= targetQuality
        } ?: combined.lastOrNull()

        return bestCombined ?: stream.sortedVideoStreams.firstOrNull() ?: stream.videoOnlyStreams.first()
    }

    fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality, showQualityDialog = false) }

        val stream = _uiState.value.videoStream ?: return
        val currentPos = player?.currentPosition ?: 0L

        val cleanQuality = quality.replace(" (DASH)", "")
        val isDash = quality.contains("DASH")

        val targetStream = if (isDash) {
            stream.videoOnlyStreams.firstOrNull { it.quality == cleanQuality }
        } else {
            stream.sortedVideoStreams.firstOrNull { it.quality == cleanQuality }
        }

        targetStream?.let {
            val mediaItem = MediaItem.Builder().setUri(it.url).build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.seekTo(currentPos)
            player?.play()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
        player?.setPlaybackSpeed(speed)
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekForward() {
        player?.seekForward()
    }

    fun seekBackward() {
        player?.seekBack()
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun showQualityDialog() {
        _uiState.update { it.copy(showQualityDialog = true) }
    }

    fun dismissQualityDialog() {
        _uiState.update { it.copy(showQualityDialog = false) }
    }

    fun showSpeedDialog() {
        _uiState.update { it.copy(showSpeedDialog = true) }
    }

    fun dismissSpeedDialog() {
        _uiState.update { it.copy(showSpeedDialog = false) }
    }

    fun showDownloadDialog() {
        _uiState.update { it.copy(showDownloadDialog = true) }
    }

    fun dismissDownloadDialog() {
        _uiState.update { it.copy(showDownloadDialog = false) }
    }

    fun startBackgroundPlay() {
        val state = _uiState.value
        val stream = state.videoStream ?: return
        val context = getApplication<Application>()

        val audioStream = stream.bestAudioStream
        val videoStream = stream.sortedVideoStreams.firstOrNull()

        val intent = Intent(context, BackgroundPlayService::class.java).apply {
            action = BackgroundPlayService.ACTION_PLAY_VIDEO
            putExtra(BackgroundPlayService.EXTRA_AUDIO_URL, audioStream?.url ?: videoStream?.url)
            putExtra(BackgroundPlayService.EXTRA_TITLE, stream.title)
            putExtra(BackgroundPlayService.EXTRA_ARTIST, stream.uploader)
            putExtra(BackgroundPlayService.EXTRA_ARTWORK_URI, stream.thumbnailUrl)
            putExtra(BackgroundPlayService.EXTRA_VIDEO_ID, state.currentVideoId)
        }

        context.startForegroundService(intent)
        player?.pause()
        _uiState.update { it.copy(isBackgroundPlaying = true) }
    }

    fun stopBackgroundPlay() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, BackgroundPlayService::class.java))
        _uiState.update { it.copy(isBackgroundPlaying = false) }
    }

    fun downloadVideo(stream: PipedStream, isAudio: Boolean = false) {
        val state = _uiState.value
        val videoStream = state.videoStream ?: return
        val context = getApplication<Application>()

        DownloadService.startDownload(
            context = context,
            videoId = state.currentVideoId,
            downloadUrl = stream.url,
            title = videoStream.title,
            thumbnail = videoStream.thumbnailUrl,
            uploader = videoStream.uploader,
            duration = videoStream.duration,
            quality = stream.qualityLabel,
            isAudio = isAudio
        )

        _uiState.update { it.copy(showDownloadDialog = false) }
    }

    private fun loadComments(videoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentsLoading = true) }
            repository.getComments(videoId).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(comments = response.comments, commentsLoading = false)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(commentsLoading = false) }
                }
            )
        }
    }

    private fun saveToHistory(videoId: String, stream: VideoStream) {
        viewModelScope.launch {
            watchHistoryDao.insertHistory(
                WatchHistoryEntity(
                    videoId = videoId,
                    title = stream.title,
                    thumbnail = stream.thumbnailUrl,
                    uploader = stream.uploader,
                    uploaderUrl = stream.uploaderUrl,
                    duration = stream.duration
                )
            )
        }
    }

    fun releasePlayer() {
        positionUpdateJob?.cancel()
        player?.release()
        player = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
