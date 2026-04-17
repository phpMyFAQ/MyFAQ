package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.data.ActiveInstanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsDetailViewModel(
    private val aim: ActiveInstanceManager,
    private val newsId: Int,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _news = MutableStateFlow<UiState<NewsItem>>(UiState.Loading)
    val news: StateFlow<UiState<NewsItem>> = _news.asStateFlow()

    fun load() {
        _news.value = UiState.Loading
        scope.launch {
            try {
                val item = aim.repository.news().find { it.id == newsId }
                _news.value = if (item != null) {
                    UiState.Success(item)
                } else {
                    UiState.Error("News item not found")
                }
            } catch (e: Exception) {
                _news.value = UiState.Error(e.message ?: "Failed to load news")
            }
        }
    }
}
