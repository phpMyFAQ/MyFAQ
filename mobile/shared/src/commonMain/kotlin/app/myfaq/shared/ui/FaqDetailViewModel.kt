package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
import app.myfaq.shared.data.ActiveInstanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaqDetailViewModel(
    private val aim: ActiveInstanceManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _faq = MutableStateFlow<UiState<FaqDetail>>(UiState.Loading)
    val faq: StateFlow<UiState<FaqDetail>> = _faq.asStateFlow()

    private val _comments = MutableStateFlow<UiState<List<Comment>>>(UiState.Loading)
    val comments: StateFlow<UiState<List<Comment>>> = _comments.asStateFlow()

    fun load(
        categoryId: Int,
        faqId: Int,
    ) {
        _faq.value = UiState.Loading
        _comments.value = UiState.Loading
        scope.launch {
            try {
                _faq.value = UiState.Success(aim.repository.faqDetail(categoryId, faqId))
            } catch (e: Exception) {
                _faq.value = UiState.Error(e.message ?: "Failed to load FAQ")
            }
        }
        scope.launch {
            try {
                _comments.value = UiState.Success(aim.repository.comments(faqId))
            } catch (e: Exception) {
                _comments.value = UiState.Error(e.message ?: "Failed to load comments")
            }
        }
    }
}
