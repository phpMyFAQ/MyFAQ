package app.myfaq.shared.ui

import app.myfaq.shared.api.FakeMyFaqApi
import app.myfaq.shared.api.dto.PopularSearch
import app.myfaq.shared.api.dto.SearchResult
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.InMemoryCacheStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun `empty query yields empty Success immediately`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api = FakeMyFaqApi()
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.onQueryChanged("")
            advanceUntilIdle()

            val state = vm.results.value
            assertTrue(state is UiState.Success)
            assertTrue(state.data.isEmpty())
            assertEquals(0, api.searchCalls)
        }

    @Test
    fun `debounced search fires after 300ms`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api =
                FakeMyFaqApi().apply {
                    searchResponse = { q -> listOf(SearchResult(id = 1, question = q)) }
                }
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.onQueryChanged("kot")
            advanceTimeBy(299)
            assertEquals(0, api.searchCalls)

            advanceTimeBy(2)
            advanceUntilIdle()

            assertEquals(1, api.searchCalls)
            val state = vm.results.value
            assertTrue(state is UiState.Success)
            assertEquals("kot", state.data.first().question)
        }

    @Test
    fun `rapid typing cancels pending search`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api =
                FakeMyFaqApi().apply {
                    searchResponse = { q -> listOf(SearchResult(id = 1, question = q)) }
                }
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.onQueryChanged("k")
            advanceTimeBy(100)
            vm.onQueryChanged("ko")
            advanceTimeBy(100)
            vm.onQueryChanged("kot")
            advanceUntilIdle()

            // Only the final query fires.
            assertEquals(1, api.searchCalls)
            val state = vm.results.value
            assertTrue(state is UiState.Success)
            assertEquals("kot", state.data.first().question)
        }

    @Test
    fun `loadPopularSearches success`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api =
                FakeMyFaqApi().apply {
                    popularSearchesResponse = { listOf(PopularSearch(id = 1, searchTerm = "kmp")) }
                }
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.loadPopularSearches()
            advanceUntilIdle()

            val state = vm.popularSearches.value
            assertTrue(state is UiState.Success)
            assertEquals("kmp", state.data.first().searchTerm)
        }

    @Test
    fun `search error state`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api =
                FakeMyFaqApi().apply {
                    searchResponse = { error("server 500") }
                }
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.onQueryChanged("broken")
            advanceUntilIdle()

            val state = vm.results.value
            assertTrue(state is UiState.Error)
            assertEquals("server 500", state.message)
        }

    @Test
    fun `language change resets query and results`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val api =
                FakeMyFaqApi().apply {
                    searchResponse = { q -> listOf(SearchResult(id = 1, question = q)) }
                }
            val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
            aim.setActive(testInstance())
            val vm = SearchViewModel(aim, scope = CoroutineScope(dispatcher))

            vm.onQueryChanged("hi")
            advanceUntilIdle()
            assertEquals("hi", vm.query.value)

            aim.setLanguage("de")
            advanceUntilIdle()

            assertEquals("", vm.query.value)
            val state = vm.results.value
            assertTrue(state is UiState.Success)
            assertTrue(state.data.isEmpty())
        }
}
