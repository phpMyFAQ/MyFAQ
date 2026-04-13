package app.myfaq.shared.ui

import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.api.dto.FaqSummary
import app.myfaq.shared.data.ActiveInstanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val aim: ActiveInstanceManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<Category>>> = _categories.asStateFlow()

    private val _faqList = MutableStateFlow<UiState<List<FaqSummary>>>(UiState.Loading)
    val faqList: StateFlow<UiState<List<FaqSummary>>> = _faqList.asStateFlow()

    fun loadCategories() {
        _categories.value = UiState.Loading
        scope.launch {
            try {
                _categories.value = UiState.Success(aim.repository.categories())
            } catch (e: Exception) {
                _categories.value = UiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }

    fun loadFaqsForCategory(categoryId: Int) {
        _faqList.value = UiState.Loading
        scope.launch {
            try {
                _faqList.value = UiState.Success(aim.repository.faqsByCategory(categoryId))
            } catch (e: Exception) {
                _faqList.value = UiState.Error(e.message ?: "Failed to load FAQs")
            }
        }
    }
}
