package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.data.ActiveInstanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(
    private val aim: ActiveInstanceManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _sticky = MutableStateFlow<UiState<List<FaqPopularItem>>>(UiState.Loading)
    val sticky: StateFlow<UiState<List<FaqPopularItem>>> = _sticky.asStateFlow()

    private val _popular = MutableStateFlow<UiState<List<FaqPopularItem>>>(UiState.Loading)
    val popular: StateFlow<UiState<List<FaqPopularItem>>> = _popular.asStateFlow()

    private val _latest = MutableStateFlow<UiState<List<FaqPopularItem>>>(UiState.Loading)
    val latest: StateFlow<UiState<List<FaqPopularItem>>> = _latest.asStateFlow()

    private val _trending = MutableStateFlow<UiState<List<FaqPopularItem>>>(UiState.Loading)
    val trending: StateFlow<UiState<List<FaqPopularItem>>> = _trending.asStateFlow()

    private val _news = MutableStateFlow<UiState<List<NewsItem>>>(UiState.Loading)
    val news: StateFlow<UiState<List<NewsItem>>> = _news.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    init {
        // Reload when the active instance's identity OR language changes.
        // drop(1) so we don't double-fire on first emission alongside loadAll().
        scope.launch {
            aim.activeInstance
                .map { it?.id to it?.language }
                .distinctUntilChanged()
                .drop(1)
                .collect { loadAll() }
        }
    }

    fun loadAll() {
        loadTitle()
        loadSticky()
        loadPopular()
        loadLatest()
        loadTrending()
        loadNews()
    }

    fun loadTitle() {
        scope.launch {
            try {
                _title.value = aim.repository.meta().title
            } catch (_: Exception) {
                // Keep previous/null title — header falls back to default.
            }
        }
    }

    fun loadSticky() = loadInto(_sticky) { aim.repository.faqsSticky() }

    fun loadPopular() = loadInto(_popular) { aim.repository.faqsPopular() }

    fun loadLatest() = loadInto(_latest) { aim.repository.faqsLatest() }

    fun loadTrending() = loadInto(_trending) { aim.repository.faqsTrending() }

    fun loadNews() = loadInto(_news) { aim.repository.news() }

    /**
     * Pull-to-refresh handlers: nuke the per-instance cache so the next fetch
     * goes to the network with the current Accept-Language header. Without
     * this, a stale cache entry for "faqs/sticky" (etc.) would be returned
     * even though the user's Accept-Language has changed.
     */
    fun refreshSticky() {
        aim.repository.clearCache()
        loadSticky()
    }

    fun refreshPopular() {
        aim.repository.clearCache()
        loadPopular()
    }

    fun refreshLatest() {
        aim.repository.clearCache()
        loadLatest()
    }

    fun refreshTrending() {
        aim.repository.clearCache()
        loadTrending()
    }

    fun refreshNews() {
        aim.repository.clearCache()
        loadNews()
    }

    private fun <T> loadInto(
        flow: MutableStateFlow<UiState<T>>,
        fetch: suspend () -> T,
    ) {
        flow.value = UiState.Loading
        scope.launch {
            try {
                flow.value = UiState.Success(fetch())
            } catch (e: Exception) {
                flow.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
