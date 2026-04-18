package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.Attachment
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

    private val _attachments = MutableStateFlow<UiState<List<Attachment>>>(UiState.Loading)
    val attachments: StateFlow<UiState<List<Attachment>>> = _attachments.asStateFlow()

    fun load(
        categoryId: Int,
        faqId: Int,
    ) {
        _faq.value = UiState.Loading
        _comments.value = UiState.Loading
        _attachments.value = UiState.Loading
        scope.launch {
            try {
                val detail = aim.repository.faqDetail(categoryId, faqId)
                _faq.value = UiState.Success(detail)
                // FAQ detail returns attachments inline; no separate request needed.
                _attachments.value = UiState.Success(detail.attachments)
            } catch (e: Exception) {
                _faq.value = UiState.Error(e.message ?: "Failed to load FAQ")
                _attachments.value = UiState.Success(emptyList())
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
