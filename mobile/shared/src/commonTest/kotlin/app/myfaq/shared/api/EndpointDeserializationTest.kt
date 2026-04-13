package app.myfaq.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests deserialization of all Phase 1 API endpoints using MockEngine.
 */
class EndpointDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun client(body: String): HttpClient =
        HttpClient(
            MockEngine { _ ->
                respond(
                    content = ByteReadChannel(body),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(json) }
        }

    private fun api(body: String) = MyFaqApiImpl(client(body), "https://test.example")

    // ── Categories ──────────────────────────────────────────────────

    @Test
    fun `categories deserializes list`() = runTest {
        val result = api(CATEGORIES_JSON).categories()
        assertEquals(2, result.size)
        assertEquals("Getting Started", result[0].name)
        assertEquals(1, result[0].id)
        assertEquals(null, result[0].parentId)
        assertEquals("Sub-Category", result[1].name)
        assertEquals(1, result[1].parentId)
    }

    // ── FAQs ────────────────────────────────────────────────────────

    @Test
    fun `faqs by category deserializes list`() = runTest {
        val result = api(FAQ_LIST_JSON).faqsByCategory(1)
        assertEquals(2, result.size)
        assertEquals("How do I install?", result[0].question)
        assertEquals(1, result[0].categoryId)
        assertEquals("2024-01-15", result[0].updated)
    }

    @Test
    fun `faq detail deserializes full record`() = runTest {
        val result = api(FAQ_DETAIL_JSON).faqDetail(1, 42)
        assertEquals(42, result.id)
        assertEquals("How do I reset?", result.question)
        assertEquals("<p>Follow these steps...</p>", result.answer)
        assertEquals(listOf("reset", "password"), result.tags)
        assertEquals(1, result.attachments.size)
        assertEquals("guide.pdf", result.attachments[0].filename)
    }

    @Test
    fun `popular faqs deserializes`() = runTest {
        val result = api(FAQ_LIST_JSON).faqsPopular()
        assertEquals(2, result.size)
    }

    @Test
    fun `sticky faqs deserializes`() = runTest {
        val result = api(STICKY_JSON).faqsSticky()
        assertEquals(1, result.size)
        assertTrue(result[0].isSticky)
    }

    // ── Search ──────────────────────────────────────────────────────

    @Test
    fun `search results deserialize`() = runTest {
        val result = api(SEARCH_RESULTS_JSON).search("test")
        assertEquals(1, result.size)
        assertEquals("How do I test?", result[0].question)
        assertEquals(3, result[0].categoryId)
    }

    @Test
    fun `popular searches deserialize`() = runTest {
        val result = api(POPULAR_SEARCHES_JSON).popularSearches()
        assertEquals(2, result.size)
        assertEquals("install", result[0].searchTerm)
        assertEquals(42, result[0].count)
    }

    // ── News ────────────────────────────────────────────────────────

    @Test
    fun `news items deserialize`() = runTest {
        val result = api(NEWS_JSON).news()
        assertEquals(1, result.size)
        assertEquals("New version released", result[0].title)
        assertEquals("admin", result[0].author)
    }

    // ── Comments ────────────────────────────────────────────────────

    @Test
    fun `comments deserialize`() = runTest {
        val result = api(COMMENTS_JSON).comments(42)
        assertEquals(2, result.size)
        assertEquals("alice", result[0].author)
        assertEquals("Great answer!", result[0].body)
        assertEquals(42, result[0].faqId)
    }

    // ── Open questions ──────────────────────────────────────────────

    @Test
    fun `open questions deserialize`() = runTest {
        val result = api(OPEN_QUESTIONS_JSON).openQuestions()
        assertEquals(1, result.size)
        assertEquals("Why is the sky blue?", result[0].question)
    }

    // ── Tags ────────────────────────────────────────────────────────

    @Test
    fun `tags deserialize`() = runTest {
        val result = api(TAGS_JSON).tags()
        assertEquals(3, result.size)
        assertEquals("install", result[0].name)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `empty list response`() = runTest {
        val result = api("[]").categories()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `faq detail with missing optional fields`() = runTest {
        val result = api(FAQ_DETAIL_MINIMAL_JSON).faqDetail(1, 1)
        assertEquals(1, result.id)
        assertEquals("Q", result.question)
        assertEquals("", result.answer)
        assertEquals(null, result.author)
        assertTrue(result.tags.isEmpty())
        assertTrue(result.attachments.isEmpty())
    }

    // ── Fixtures ────────────────────────────────────────────────────

    private companion object {
        const val CATEGORIES_JSON = """
        [
          {"id": 1, "name": "Getting Started", "description": "Basics"},
          {"id": 2, "name": "Sub-Category", "description": "Details", "parent_id": 1, "lang": "en"}
        ]
        """

        const val FAQ_LIST_JSON = """
        [
          {"id": 10, "category_id": 1, "question": "How do I install?", "updated": "2024-01-15"},
          {"id": 11, "category_id": 1, "question": "How do I configure?", "updated": "2024-01-20"}
        ]
        """

        const val FAQ_DETAIL_JSON = """
        {
          "id": 42,
          "category_id": 1,
          "question": "How do I reset?",
          "answer": "<p>Follow these steps...</p>",
          "author": "admin",
          "created": "2024-01-01",
          "updated": "2024-03-15",
          "is_sticky": false,
          "is_active": true,
          "tags": ["reset", "password"],
          "attachments": [
            {"id": 1, "filename": "guide.pdf", "size": 12345, "mime": "application/pdf"}
          ]
        }
        """

        const val FAQ_DETAIL_MINIMAL_JSON = """
        {"id": 1, "question": "Q"}
        """

        const val STICKY_JSON = """
        [{"id": 5, "category_id": 1, "question": "Important notice", "is_sticky": true}]
        """

        const val SEARCH_RESULTS_JSON = """
        [{"id": 7, "category_id": 3, "question": "How do I test?", "answer": "Like this..."}]
        """

        const val POPULAR_SEARCHES_JSON = """
        [
          {"search_term": "install", "count": 42},
          {"search_term": "reset password", "count": 17}
        ]
        """

        const val NEWS_JSON = """
        [{"id": 1, "title": "New version released", "body": "<p>Details here</p>", "author": "admin", "created": "2024-06-01"}]
        """

        const val COMMENTS_JSON = """
        [
          {"id": 1, "faq_id": 42, "author": "alice", "body": "Great answer!", "created": "2024-02-01"},
          {"id": 2, "faq_id": 42, "author": "bob", "body": "Thanks!", "created": "2024-02-02"}
        ]
        """

        const val OPEN_QUESTIONS_JSON = """
        [{"id": 1, "question": "Why is the sky blue?", "category_id": 2}]
        """

        const val TAGS_JSON = """
        [
          {"id": 1, "name": "install"},
          {"id": 2, "name": "configuration"},
          {"id": 3, "name": "troubleshooting"}
        ]
        """
    }
}
