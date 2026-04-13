package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.PopularSearch
import app.myfaq.shared.api.dto.SearchResult
import app.myfaq.shared.data.ActiveInstanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val aim: ActiveInstanceManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _results = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Success(emptyList()))
    val results: StateFlow<UiState<List<SearchResult>>> = _results.asStateFlow()

    private val _popularSearches = MutableStateFlow<UiState<List<PopularSearch>>>(UiState.Loading)
    val popularSearches: StateFlow<UiState<List<PopularSearch>>> = _popularSearches.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var searchJob: Job? = null

    fun loadPopularSearches() {
        scope.launch {
            try {
                _popularSearches.value = UiState.Success(aim.repository.popularSearches())
            } catch (e: Exception) {
                _popularSearches.value = UiState.Error(e.message ?: "Failed to load suggestions")
            }
        }
    }

    /**
     * Debounced search. Waits 300ms after the last keystroke.
     */
    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = UiState.Success(emptyList())
            return
        }
        searchJob =
            scope.launch {
                delay(300)
                _results.value = UiState.Loading
                try {
                    _results.value = UiState.Success(aim.repository.search(newQuery))
                } catch (e: Exception) {
                    _results.value = UiState.Error(e.message ?: "Search failed")
                }
            }
    }
}
