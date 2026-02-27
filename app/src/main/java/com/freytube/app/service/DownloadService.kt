package com.freytube.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freytube.app.FreyTubeApp
import com.freytube.app.R
import com.freytube.app.data.local.AppDatabase
import com.freytube.app.data.local.DownloadEntity
import com.freytube.app.data.local.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    companion object {
        const val ACTION_DOWNLOAD = "com.freytube.DOWNLOAD"
        const val ACTION_CANCEL = "com.freytube.CANCEL_DOWNLOAD"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_THUMBNAIL = "thumbnail"
        const val EXTRA_UPLOADER = "uploader"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_IS_AUDIO = "is_audio"
        private const val NOTIFICATION_ID = 2001

        fun startDownload(
            context: Context,
            videoId: String,
            downloadUrl: String,
            title: String,
            thumbnail: String,
            uploader: String,
            duration: Long,
            quality: String,
            isAudio: Boolean = false
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_THUMBNAIL, thumbnail)
                putExtra(EXTRA_UPLOADER, uploader)
                putExtra(EXTRA_DURATION, duration)
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_IS_AUDIO, isAudio)
            }
            context.startForegroundService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadDao by lazy { AppDatabase.getInstance(this).downloadDao() }
    private val activeDownloads = mutableMapOf<String, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("FreyTube Downloads", "Ready", 0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unknown"
                val thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL) ?: ""
                val uploader = intent.getStringExtra(EXTRA_UPLOADER) ?: ""
                val duration = intent.getLongExtra(EXTRA_DURATION, 0)
                val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "720p"
                val isAudio = intent.getBooleanExtra(EXTRA_IS_AUDIO, false)

                startVideoDownload(videoId, downloadUrl, title, thumbnail, uploader, duration, quality, isAudio)
            }
            ACTION_CANCEL -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
                videoId?.let { cancelDownload(it) }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVideoDownload(
        videoId: String,
        downloadUrl: String,
        title: String,
        thumbnail: String,
        uploader: String,
        duration: Long,
        quality: String,
        isAudio: Boolean
    ) {
        val job = scope.launch {
            try {
                val extension = if (isAudio) "m4a" else "mp4"
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                val fileName = "${sanitizedTitle}_${quality}.${extension}"

                val downloadDir = File(
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "FreyTube"
                )
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val outputFile = File(downloadDir, fileName)

                val entity = DownloadEntity(
                    videoId = videoId,
                    title = title,
                    thumbnail = thumbnail,
                    uploader = uploader,
                    duration = duration,
                    filePath = outputFile.absolutePath,
                    quality = quality,
                    downloadProgress = 0,
                    status = DownloadStatus.DOWNLOADING
                )
                downloadDao.insertDownload(entity)

                updateNotification("Downloading: $title", "Starting...", 0)

                val request = Request.Builder().url(downloadUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    downloadDao.updateProgress(videoId, 0, DownloadStatus.FAILED)
                    return@launch
                }

                val body = response.body ?: run {
                    downloadDao.updateProgress(videoId, 0, DownloadStatus.FAILED)
                    return@launch
                }

                val contentLength = body.contentLength()
                var bytesDownloaded = 0L

                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var lastProgressUpdate = 0

                        while (input.read(buffer).also { read = it } != -1) {
                            if (!isActive) {
                                output.close()
                                outputFile.delete()
                                downloadDao.updateProgress(videoId, 0, DownloadStatus.FAILED)
                                return@launch
                            }

                            output.write(buffer, 0, read)
                            bytesDownloaded += read

                            if (contentLength > 0) {
                                val progress = ((bytesDownloaded * 100) / contentLength).toInt()
                                if (progress > lastProgressUpdate) {
                                    lastProgressUpdate = progress
                                    downloadDao.updateProgress(videoId, progress, DownloadStatus.DOWNLOADING)
                                    updateNotification("Downloading: $title", "$progress%", progress)
                                }
                            }
                        }
                    }
                }

                downloadDao.updateProgress(videoId, 100, DownloadStatus.COMPLETED)
                downloadDao.insertDownload(
                    entity.copy(
                        fileSize = bytesDownloaded,
                        downloadProgress = 100,
                        status = DownloadStatus.COMPLETED
                    )
                )
                updateNotification("Download complete", title, 100)

            } catch (e: Exception) {
                downloadDao.updateProgress(videoId, 0, DownloadStatus.FAILED)
                updateNotification("Download failed", title, 0)
            } finally {
                activeDownloads.remove(videoId)
                if (activeDownloads.isEmpty()) {
                    delay(3000)
                    if (activeDownloads.isEmpty()) {
                        stopSelf()
                    }
                }
            }
        }
        activeDownloads[videoId] = job
    }

    private fun cancelDownload(videoId: String) {
        activeDownloads[videoId]?.cancel()
        activeDownloads.remove(videoId)
        scope.launch {
            downloadDao.updateProgress(videoId, 0, DownloadStatus.FAILED)
        }
    }

    private fun createNotification(title: String, text: String, progress: Int) =
        NotificationCompat.Builder(this, FreyTubeApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setSilent(true)
            .build()

    private fun updateNotification(title: String, text: String, progress: Int) {
        val notification = createNotification(title, text, progress)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
