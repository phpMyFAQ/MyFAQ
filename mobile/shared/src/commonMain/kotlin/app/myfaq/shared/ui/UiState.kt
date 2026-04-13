package app.myfaq.shared.ui

/**
 * Generic tri-state for screen data loading.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
    ) : UiState<T>

    data class Error(
        val message: String,
    ) : UiState<Nothing>
}
