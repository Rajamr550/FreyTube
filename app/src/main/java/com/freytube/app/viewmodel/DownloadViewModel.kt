package com.freytube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freytube.app.data.local.AppDatabase
import com.freytube.app.data.local.DownloadEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadDao = AppDatabase.getInstance(application).downloadDao()

    val downloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            // Delete file
            try {
                File(download.filePath).delete()
            } catch (_: Exception) {}

            // Delete from database
            downloadDao.deleteDownload(download)
        }
    }

    fun retryDownload(download: DownloadEntity) {
        // Would re-trigger the download service
        // For now, just reset the status
        viewModelScope.launch {
            downloadDao.updateProgress(
                download.videoId,
                0,
                com.freytube.app.data.local.DownloadStatus.PENDING
            )
        }
    }
}
