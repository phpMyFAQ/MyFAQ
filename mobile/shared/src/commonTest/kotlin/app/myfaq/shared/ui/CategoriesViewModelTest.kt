package app.myfaq.shared.ui

import app.myfaq.shared.api.FakeMyFaqApi
import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.api.dto.FaqSummary
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
class CategoriesViewModelTest {
    private fun setup(
        configure: FakeMyFaqApi.() -> Unit = {},
    ): Triple<CategoriesViewModel, FakeMyFaqApi, ActiveInstanceManager> {
        val api = FakeMyFaqApi().apply(configure)
        val cache = InMemoryCacheStore()
        val aim = ActiveInstanceManager(cache, apiFactory = { api })
        aim.setActive(testInstance())
        val vm = CategoriesViewModel(aim, scope = CoroutineScope(UnconfinedTestDispatcher()))
        return Triple(vm, api, aim)
    }

    @Test
    fun `loadCategories success`() =
        runTest {
            val (vm, _, _) =
                setup {
                    categoriesResponse = { listOf(Category(id = 1, name = "General")) }
                }

            vm.loadCategories()

            val state = vm.categories.value
            assertTrue(state is UiState.Success)
            assertEquals("General", state.data.first().name)
        }

    @Test
    fun `loadCategories error`() =
        runTest {
            val (vm, _, _) =
                setup {
                    categoriesResponse = { error("nope") }
                }

            vm.loadCategories()

            val state = vm.categories.value
            assertTrue(state is UiState.Error)
            assertEquals("nope", state.message)
        }

    @Test
    fun `loadFaqsForCategory passes id through`() =
        runTest {
            val (vm, _, _) =
                setup {
                    faqsByCategoryResponse = { id ->
                        listOf(FaqSummary(id = id, question = "F$id"))
                    }
                }

            vm.loadFaqsForCategory(42)

            val state = vm.faqList.value
            assertTrue(state is UiState.Success)
            assertEquals(42, state.data.first().id)
        }

    @Test
    fun `language change auto-reloads categories`() =
        runTest {
            var calls = 0
            val (vm, _, aim) =
                setup {
                    categoriesResponse = {
                        calls += 1
                        listOf(Category(id = 1, name = "C$calls"))
                    }
                }

            vm.loadCategories()
            assertEquals(1, calls)

            aim.setLanguage("de")

            assertEquals(2, calls)
        }
}
