package app.myfaq.shared.api.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun `deserializes string IDs from v4 API`() {
        val input = """{"id": "42", "category_id": "15", "question": "Test?"}"""
        val result = json.decodeFromString<SearchResult>(input)
        assertEquals(42, result.id)
        assertEquals(15, result.categoryId)
        assertEquals("Test?", result.question)
    }

    @Test
    fun `deserializes integer IDs`() {
        val input = """{"id": 7, "category_id": 3, "question": "Q"}"""
        val result = json.decodeFromString<SearchResult>(input)
        assertEquals(7, result.id)
        assertEquals(3, result.categoryId)
    }

    @Test
    fun `defaults to zero for missing IDs`() {
        val input = """{"question": "No IDs"}"""
        val result = json.decodeFromString<SearchResult>(input)
        assertEquals(0, result.id)
        assertEquals(0, result.categoryId)
    }

    @Test
    fun `preserves HTML in answer field`() {
        val input = """
        {
          "id": "1",
          "category_id": "2",
          "question": "Q",
          "answer": "<p>Hello <strong>world</strong></p><ul><li>Item</li></ul>"
        }
        """
        val result = json.decodeFromString<SearchResult>(input)
        assertEquals("<p>Hello <strong>world</strong></p><ul><li>Item</li></ul>", result.answer)
    }

    @Test
    fun `deserializes link field`() {
        val input = """
        {"id": "1", "category_id": "2", "question": "Q", "link": "https://example.org/faq/1"}
        """
        val result = json.decodeFromString<SearchResult>(input)
        assertEquals("https://example.org/faq/1", result.link)
    }
}
