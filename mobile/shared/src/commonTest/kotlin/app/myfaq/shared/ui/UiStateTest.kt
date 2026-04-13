package app.myfaq.shared.ui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Basic UiState sealed interface tests.
 * ViewModel integration tests require a real DB and are covered in
 * androidTest / iOS XCTest where DI is available.
 */
class UiStateTest {
    @Test
    fun `loading is not success`() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state is UiState.Loading)
    }

    @Test
    fun `success holds data`() {
        val state: UiState<String> = UiState.Success("hello")
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data == "hello")
    }

    @Test
    fun `error holds message`() {
        val state: UiState<String> = UiState.Error("boom")
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message == "boom")
    }
}
