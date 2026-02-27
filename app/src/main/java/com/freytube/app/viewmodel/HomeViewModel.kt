package com.freytube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freytube.app.data.local.AppDatabase
import com.freytube.app.data.local.WatchHistoryEntity
import com.freytube.app.data.model.StreamItem
import com.freytube.app.data.repository.PipedRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val trendingVideos: List<StreamItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedRegion: String = "US"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PipedRepository()
    private val watchHistoryDao = AppDatabase.getInstance(application).watchHistoryDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentHistory: Flow<List<WatchHistoryEntity>> = watchHistoryDao.getRecentHistory(10)

    init {
        loadTrending()
    }

    fun loadTrending(region: String = "US") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedRegion = region) }

            repository.getTrending(region).fold(
                onSuccess = { videos ->
                    _uiState.update {
                        it.copy(
                            trendingVideos = videos,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load trending videos"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        loadTrending(_uiState.value.selectedRegion)
    }

    fun addToHistory(video: StreamItem) {
        viewModelScope.launch {
            watchHistoryDao.insertHistory(
                WatchHistoryEntity(
                    videoId = video.videoId,
                    title = video.title,
                    thumbnail = video.thumbnail,
                    uploader = video.uploaderName,
                    uploaderUrl = video.uploaderUrl,
                    duration = video.duration
                )
            )
        }
    }
}
