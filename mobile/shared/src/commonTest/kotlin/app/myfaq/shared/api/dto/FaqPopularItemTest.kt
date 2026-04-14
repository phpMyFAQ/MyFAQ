package app.myfaq.shared.api.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FaqPopularItemTest {
    @Test
    fun `parses categoryId and faqId from standard URL`() {
        val item =
            FaqPopularItem(
                question = "Test",
                url = "https://www.example.org/content/15/42/en/some-slug.html",
            )
        assertEquals(15, item.parsedCategoryId)
        assertEquals(42, item.parsedFaqId)
    }

    @Test
    fun `parses IDs from URL with path prefix`() {
        val item =
            FaqPopularItem(
                question = "Test",
                url = "https://www.example.org/phpmyfaq/content/3/7/de/meine-frage.html",
            )
        assertEquals(3, item.parsedCategoryId)
        assertEquals(7, item.parsedFaqId)
    }

    @Test
    fun `returns null for URL without content path`() {
        val item =
            FaqPopularItem(
                question = "Test",
                url = "https://www.example.org/faq/15/42",
            )
        assertNull(item.parsedCategoryId)
        assertNull(item.parsedFaqId)
    }

    @Test
    fun `returns null when url is null`() {
        val item = FaqPopularItem(question = "Test", url = null)
        assertNull(item.parsedCategoryId)
        assertNull(item.parsedFaqId)
    }

    @Test
    fun `returns null for malformed URL segments`() {
        val item =
            FaqPopularItem(
                question = "Test",
                url = "https://www.example.org/content/abc/def/en/slug.html",
            )
        assertNull(item.parsedCategoryId)
        assertNull(item.parsedFaqId)
    }

    @Test
    fun `handles URL with only categoryId segment`() {
        val item =
            FaqPopularItem(
                question = "Test",
                url = "https://www.example.org/content/5",
            )
        assertEquals(5, item.parsedCategoryId)
        assertNull(item.parsedFaqId)
    }
}
