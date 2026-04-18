package app.myfaq.shared.ui

import app.myfaq.shared.api.FakeMyFaqApi
import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.Meta
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.InMemoryCacheStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private fun setup(
        configure: FakeMyFaqApi.() -> Unit = {},
    ): Triple<HomeViewModel, FakeMyFaqApi, ActiveInstanceManager> {
        val api = FakeMyFaqApi().apply(configure)
        val cache = InMemoryCacheStore()
        val aim = ActiveInstanceManager(cache, apiFactory = { api })
        aim.setActive(testInstance())
        val vm = HomeViewModel(aim, scope = CoroutineScope(UnconfinedTestDispatcher()))
        return Triple(vm, api, aim)
    }

    @Test
    fun `loadSticky transitions Loading to Success`() =
        runTest {
            val (vm, _, _) =
                setup {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "Q1")) }
                }

            vm.loadSticky()

            val state = vm.sticky.value
            assertTrue(state is UiState.Success)
            assertEquals("Q1", state.data.first().question)
        }

    @Test
    fun `loadSticky transitions Loading to Error on failure`() =
        runTest {
            val (vm, _, _) =
                setup {
                    faqsStickyResponse = { error("boom") }
                }

            vm.loadSticky()

            val state = vm.sticky.value
            assertTrue(state is UiState.Error)
            assertEquals("boom", state.message)
        }

    @Test
    fun `loadTitle populates title from meta`() =
        runTest {
            val (vm, _, _) =
                setup {
                    metaResponse = { Meta(title = "My FAQs") }
                }

            vm.loadTitle()

            assertEquals("My FAQs", vm.title.value)
        }

    @Test
    fun `loadTitle silently swallows errors`() =
        runTest {
            val (vm, _, _) =
                setup {
                    metaResponse = { error("offline") }
                }

            vm.loadTitle()

            // No crash; title stays null.
            assertEquals(null, vm.title.value)
        }

    @Test
    fun `language change auto-reloads sticky`() =
        runTest {
            var counter = 0
            val (vm, api, aim) =
                setup {
                    faqsStickyResponse = {
                        counter += 1
                        listOf(FaqPopularItem(question = "Q$counter"))
                    }
                }

            vm.loadSticky()
            assertEquals(1, api.faqsStickyCalls)

            // Switch language — observer in init should re-trigger loadAll().
            aim.setLanguage("de")

            // loadAll fires sticky among others.
            assertTrue(api.faqsStickyCalls >= 2)
            val state = vm.sticky.value
            assertTrue(state is UiState.Success)
        }

    @Test
    fun `refreshSticky clears cache and refetches`() =
        runTest {
            var counter = 0
            val (vm, api, _) =
                setup {
                    faqsStickyResponse = {
                        counter += 1
                        listOf(FaqPopularItem(question = "Q$counter"))
                    }
                }

            vm.loadSticky()
            assertEquals(1, api.faqsStickyCalls)

            // Without refresh, second call would hit cache.
            vm.refreshSticky()

            assertEquals(2, api.faqsStickyCalls)
            val state = vm.sticky.value
            assertTrue(state is UiState.Success)
            assertEquals("Q2", state.data.first().question)
        }
}
