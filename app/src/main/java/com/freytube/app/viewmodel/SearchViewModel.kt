package com.freytube.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freytube.app.data.model.StreamItem
import com.freytube.app.data.repository.PipedRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<StreamItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextPage: String? = null,
    val isLoadingMore: Boolean = false,
    val showSuggestions: Boolean = false,
    val searchHistory: List<String> = emptyList()
)

class SearchViewModel : ViewModel() {

    private val repository = PipedRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private val _searchHistory = mutableListOf<String>()

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, showSuggestions = query.isNotEmpty()) }
        loadSuggestions(query)
    }

    private fun loadSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(300) // Debounce
            repository.getSuggestions(query).fold(
                onSuccess = { suggestions ->
                    _uiState.update { it.copy(suggestions = suggestions) }
                },
                onFailure = { /* ignore suggestion errors */ }
            )
        }
    }

    fun search(query: String = _uiState.value.query) {
        if (query.isBlank()) return

        // Add to search history
        _searchHistory.remove(query)
        _searchHistory.add(0, query)
        if (_searchHistory.size > 20) _searchHistory.removeAt(_searchHistory.lastIndex)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    query = query,
                    isLoading = true,
                    error = null,
                    showSuggestions = false,
                    searchHistory = _searchHistory.toList()
                )
            }

            repository.search(query).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            results = response.items,
                            isLoading = false,
                            nextPage = response.nextpage
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Search failed"
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val nextPage = state.nextPage ?: return
        if (state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            repository.searchNextPage(state.query, "all", nextPage).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            results = it.results + response.items,
                            isLoadingMore = false,
                            nextPage = response.nextpage
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            SearchUiState(searchHistory = _searchHistory.toList())
        }
    }

    fun hideSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }
}
