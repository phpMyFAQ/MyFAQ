package app.myfaq.shared.ui

import app.myfaq.shared.api.FakeMyFaqApi
import app.myfaq.shared.api.dto.Attachment
import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
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
class FaqDetailViewModelTest {
    private fun setup(configure: FakeMyFaqApi.() -> Unit = {}): Pair<FaqDetailViewModel, FakeMyFaqApi> {
        val api = FakeMyFaqApi().apply(configure)
        val aim = ActiveInstanceManager(InMemoryCacheStore(), apiFactory = { api })
        aim.setActive(testInstance())
        val vm = FaqDetailViewModel(aim, scope = CoroutineScope(UnconfinedTestDispatcher()))
        return vm to api
    }

    @Test
    fun `load populates faq attachments and comments`() =
        runTest {
            val (vm, _) =
                setup {
                    faqDetailResponse = { catId, faqId ->
                        FaqDetail(
                            id = faqId,
                            categoryId = catId,
                            question = "Q",
                            answer = "A",
                            attachments = listOf(Attachment(id = 1, filename = "doc.pdf")),
                        )
                    }
                    commentsResponse = { recId ->
                        listOf(Comment(id = recId, username = "alice", comment = "great"))
                    }
                }

            vm.load(categoryId = 7, faqId = 42)

            val faq = vm.faq.value
            assertTrue(faq is UiState.Success)
            assertEquals(42, faq.data.id)
            assertEquals(7, faq.data.categoryId)

            val attachments = vm.attachments.value
            assertTrue(attachments is UiState.Success)
            assertEquals("doc.pdf", attachments.data.first().filename)

            val comments = vm.comments.value
            assertTrue(comments is UiState.Success)
            assertEquals("alice", comments.data.first().username)
        }

    @Test
    fun `faq detail error sets Error and empty attachments`() =
        runTest {
            val (vm, _) =
                setup {
                    faqDetailResponse = { _, _ -> error("404") }
                }

            vm.load(categoryId = 1, faqId = 2)

            val faq = vm.faq.value
            assertTrue(faq is UiState.Error)
            assertEquals("404", faq.message)

            val attachments = vm.attachments.value
            assertTrue(attachments is UiState.Success)
            assertTrue(attachments.data.isEmpty())
        }

    @Test
    fun `comments error is independent from faq success`() =
        runTest {
            val (vm, _) =
                setup {
                    faqDetailResponse = { _, faqId -> FaqDetail(id = faqId) }
                    commentsResponse = { error("comments down") }
                }

            vm.load(categoryId = 1, faqId = 2)

            assertTrue(vm.faq.value is UiState.Success)
            val comments = vm.comments.value
            assertTrue(comments is UiState.Success || comments is UiState.Error)
            // commentsResponse threw, expect Error.
            assertTrue(comments is UiState.Error)
            assertEquals("comments down", comments.message)
        }
}
