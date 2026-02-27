package com.freytube.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.freytube.app.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class BackgroundPlayService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    companion object {
        const val ACTION_PLAY_VIDEO = "com.freytube.PLAY_VIDEO"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_AUDIO_URL = "audio_url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ARTWORK_URI = "artwork_uri"
        const val EXTRA_VIDEO_ID = "video_id"

        private const val COMMAND_SEEK_FORWARD = "SEEK_FORWARD"
        private const val COMMAND_SEEK_BACKWARD = "SEEK_BACKWARD"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY_VIDEO -> {
                val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL)
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "FreyTube"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val artworkUri = intent.getStringExtra(EXTRA_ARTWORK_URI)

                val url = audioUrl ?: videoUrl ?: return START_NOT_STICKY

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setDisplayTitle(title)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                player?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            }
        }

        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(COMMAND_SEEK_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SEEK_BACKWARD, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_SEEK_FORWARD -> {
                    player?.seekForward()
                }
                COMMAND_SEEK_BACKWARD -> {
                    player?.seekBack()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
