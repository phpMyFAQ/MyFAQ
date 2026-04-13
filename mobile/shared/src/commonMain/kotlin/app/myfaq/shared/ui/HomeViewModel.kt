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

    private val _news = MutableStateFlow<UiState<List<NewsItem>>>(UiState.Loading)
    val news: StateFlow<UiState<List<NewsItem>>> = _news.asStateFlow()

    fun loadAll() {
        loadSticky()
        loadPopular()
        loadLatest()
        loadNews()
    }

    fun loadSticky() = loadInto(_sticky) { aim.repository.faqsSticky() }
    fun loadPopular() = loadInto(_popular) { aim.repository.faqsPopular() }
    fun loadLatest() = loadInto(_latest) { aim.repository.faqsLatest() }
    fun loadNews() = loadInto(_news) { aim.repository.news() }

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
