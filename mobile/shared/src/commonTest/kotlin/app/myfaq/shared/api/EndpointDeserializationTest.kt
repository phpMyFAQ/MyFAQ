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
 * Updated for v4.0: paginated wrappers and new field names.
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

    // ── Categories (paginated) ─────────────────────────────────────

    @Test
    fun `categories deserializes paginated response`() = runTest {
        val result = api(CATEGORIES_JSON).categories()
        assertEquals(2, result.size)
        assertEquals("Getting Started", result[0].name)
        assertEquals(1, result[0].id)
        assertEquals(0, result[0].parentId)
        assertEquals("Sub-Category", result[1].name)
        assertEquals(1, result[1].parentId)
        assertEquals(1, result[0].level)
    }

    // ── FAQs ────────────────────────────────────────────────────────

    @Test
    fun `faqs by category deserializes paginated response with record_ fields`() = runTest {
        val result = api(FAQ_LIST_JSON).faqsByCategory(1)
        assertEquals(2, result.size)
        assertEquals("How do I install?", result[0].question)
        assertEquals(1, result[0].categoryId)
        assertEquals("20240115120000", result[0].updated)
        assertEquals(10, result[0].id)
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
        assertEquals(1, result.sticky)
        assertTrue(result.isSticky)
    }

    @Test
    fun `popular faqs deserializes plain array`() = runTest {
        val result = api(POPULAR_FAQS_JSON).faqsPopular()
        assertEquals(1, result.size)
        assertEquals("How can I survive without phpMyFAQ?", result[0].question)
        assertEquals(10, result[0].visits)
        assertEquals("2019-07-13T11:28:00+0200", result[0].date)
    }

    @Test
    fun `sticky faqs deserializes plain array`() = runTest {
        val result = api(STICKY_FAQS_JSON).faqsSticky()
        assertEquals(2, result.size)
        assertEquals("How can I survive without phpMyFAQ?", result[0].question)
        assertEquals(8, result[0].id)
        assertEquals(1, result[0].order)
    }

    // ── Search (paginated) ─────────────────────────────────────────

    @Test
    fun `search results deserialize from paginated wrapper`() = runTest {
        val result = api(SEARCH_RESULTS_JSON).search("test")
        assertEquals(1, result.size)
        assertEquals("Why are you using phpMyFAQ?", result[0].question)
        assertEquals(15, result[0].categoryId)
        assertEquals("Because it is cool!", result[0].answer)
        assertEquals("en", result[0].lang)
    }

    @Test
    fun `popular searches deserialize plain array`() = runTest {
        val result = api(POPULAR_SEARCHES_JSON).popularSearches()
        assertEquals(2, result.size)
        assertEquals("mac", result[0].searchTerm)
        assertEquals(18, result[0].count)
        assertEquals("en", result[0].lang)
    }

    // ── News (paginated) ───────────────────────────────────────────

    @Test
    fun `news items deserialize from paginated wrapper`() = runTest {
        val result = api(NEWS_JSON).news()
        assertEquals(1, result.size)
        assertEquals("Hallo, World!", result[0].header)
        assertEquals("phpMyFAQ User", result[0].authorName)
        assertEquals(true, result[0].active)
        assertEquals(true, result[0].allowComments)
    }

    // ── Comments (paginated) ───────────────────────────────────────

    @Test
    fun `comments deserialize from paginated wrapper`() = runTest {
        val result = api(COMMENTS_JSON).comments(142)
        assertEquals(1, result.size)
        assertEquals("phpMyFAQ User", result[0].username)
        assertEquals("Foo! Bar?", result[0].comment)
        assertEquals(142, result[0].recordId)
        assertEquals("faq", result[0].type)
    }

    // ── Open questions (paginated) ─────────────────────────────────

    @Test
    fun `open questions deserialize from paginated wrapper`() = runTest {
        val result = api(OPEN_QUESTIONS_JSON).openQuestions()
        assertEquals(1, result.size)
        assertEquals("Foo? Bar? Baz?", result[0].question)
        assertEquals("phpMyFAQ User", result[0].username)
        assertEquals(3, result[0].categoryId)
        assertEquals("N", result[0].isVisible)
    }

    // ── Tags (paginated) ───────────────────────────────────────────

    @Test
    fun `tags deserialize from paginated wrapper`() = runTest {
        val result = api(TAGS_JSON).tags()
        assertEquals(2, result.size)
        assertEquals("phpMyFAQ", result[0].name)
        assertEquals(4, result[0].id)
        assertEquals(3, result[0].frequency)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `glossary items deserialize from paginated wrapper`() = runTest {
        val result = api(GLOSSARY_JSON).glossary()
        assertEquals(2, result.size)
        assertEquals("API", result[0].item)
        assertEquals("Application Programming Interface", result[0].definition)
        assertEquals(1, result[0].id)
        assertEquals("en", result[0].language)
        assertEquals("FAQ", result[1].item)
    }

    @Test
    fun `empty paginated response`() = runTest {
        val result = api(EMPTY_PAGINATED_JSON).categories()
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

    // ── Fixtures (v4.0 format) ─────────────────────────────────────

    private companion object {
        // Paginated: categories
        const val CATEGORIES_JSON = """
        {
          "success": true,
          "data": [
            {"id": 1, "lang": "en", "parent_id": 0, "name": "Getting Started", "description": "Basics", "user_id": 1, "group_id": 1, "active": 1, "show_home": 1, "level": 1},
            {"id": 2, "lang": "en", "parent_id": 1, "name": "Sub-Category", "description": "Details", "user_id": 1, "group_id": 1, "active": 1, "show_home": 0, "level": 2}
          ],
          "meta": {
            "pagination": {"total": 2, "count": 2, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Paginated: faqs by category (record_* field names)
        const val FAQ_LIST_JSON = """
        {
          "success": true,
          "data": [
            {"record_id": 10, "category_id": 1, "record_title": "How do I install?", "record_preview": "Steps to install", "record_updated": "20240115120000", "record_lang": "en", "visits": 5},
            {"record_id": 11, "category_id": 1, "record_title": "How do I configure?", "record_preview": "Configuration guide", "record_updated": "20240120120000", "record_lang": "en", "visits": 3}
          ],
          "meta": {
            "pagination": {"total": 2, "count": 2, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Non-paginated: faq detail
        const val FAQ_DETAIL_JSON = """
        {
          "id": 42,
          "category_id": 1,
          "question": "How do I reset?",
          "answer": "<p>Follow these steps...</p>",
          "author": "admin",
          "created": "2024-01-01",
          "updated": "2024-03-15",
          "sticky": 1,
          "active": "yes",
          "tags": ["reset", "password"],
          "attachments": [
            {"id": 1, "filename": "guide.pdf", "filesize": 12345, "mime_type": "application/pdf"}
          ]
        }
        """

        const val FAQ_DETAIL_MINIMAL_JSON = """
        {"id": 1, "question": "Q"}
        """

        // Plain array: popular/latest/trending FAQs
        const val POPULAR_FAQS_JSON = """
        [
          {"date": "2019-07-13T11:28:00+0200", "question": "How can I survive without phpMyFAQ?", "answer": "A good question!", "visits": 10, "url": "https://www.example.org/content/1/36/de/how-can-i-survive-without-phpmyfaq.html"}
        ]
        """

        // Plain array: sticky FAQs (different shape)
        const val STICKY_FAQS_JSON = """
        [
          {"question": "How can I survive without phpMyFAQ?", "url": "https://www.example.org/content/1/36/de/how-can-i-survive-without-phpmyfaq.html", "id": 8, "order": 1},
          {"question": "Is there life after death?", "url": "https://www.example.org/content/1/1/de/is-there-life-after-death.html", "id": 10, "order": 2}
        ]
        """

        // Paginated: search results
        const val SEARCH_RESULTS_JSON = """
        {
          "success": true,
          "data": [
            {"id": 1, "lang": "en", "category_id": 15, "question": "Why are you using phpMyFAQ?", "answer": "Because it is cool!", "link": "https://www.example.org/content/15/1/en/why-are-you-using-phpmyfaq.html"}
          ],
          "meta": {
            "pagination": {"total": 1, "count": 1, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Plain array: popular searches (NOT paginated)
        const val POPULAR_SEARCHES_JSON = """
        [
          {"id": 3, "searchterm": "mac", "number": "18", "lang": "en"},
          {"id": 7, "searchterm": "test", "number": "9", "lang": "en"}
        ]
        """

        // Paginated: news
        const val NEWS_JSON = """
        {
          "success": true,
          "data": [
            {"id": 1, "lang": "en", "date": "2019-08-23T20:43:00+0200", "header": "Hallo, World!", "content": "Hello, phpMyFAQ!", "authorName": "phpMyFAQ User", "authorEmail": "user@example.org", "active": true, "allowComments": true, "link": "", "linkTitle": "", "target": "", "url": "https://www.example.org/news/1/de/hallo-phpmyfaq.html"}
          ],
          "meta": {
            "pagination": {"total": 1, "count": 1, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Paginated: comments
        const val COMMENTS_JSON = """
        {
          "success": true,
          "data": [
            {"id": 2, "recordId": 142, "categoryId": null, "type": "faq", "username": "phpMyFAQ User", "comment": "Foo! Bar?", "date": "2019-12-24T12:24:57+0100", "helped": null}
          ],
          "meta": {
            "pagination": {"total": 1, "count": 1, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Paginated: open questions
        const val OPEN_QUESTIONS_JSON = """
        {
          "success": true,
          "data": [
            {"id": 1, "lang": "en", "username": "phpMyFAQ User", "email": "user@example.org", "categoryId": 3, "question": "Foo? Bar? Baz?", "created": "20190106180429", "answerId": 0, "isVisible": "N"}
          ],
          "meta": {
            "pagination": {"total": 1, "count": 1, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Paginated: tags
        const val TAGS_JSON = """
        {
          "success": true,
          "data": [
            {"tagId": 4, "tagName": "phpMyFAQ", "tagFrequency": 3},
            {"tagId": 1, "tagName": "PHP 8", "tagFrequency": 2}
          ],
          "meta": {
            "pagination": {"total": 2, "count": 2, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Paginated: glossary
        const val GLOSSARY_JSON = """
        {
          "success": true,
          "data": [
            {"id": 1, "language": "en", "item": "API", "definition": "Application Programming Interface"},
            {"id": 2, "language": "en", "item": "FAQ", "definition": "Frequently Asked Questions"}
          ],
          "meta": {
            "pagination": {"total": 2, "count": 2, "per_page": 25, "current_page": 1, "total_pages": 1}
          }
        }
        """

        // Empty paginated response
        const val EMPTY_PAGINATED_JSON = """
        {
          "success": true,
          "data": [],
          "meta": {
            "pagination": {"total": 0, "count": 0, "per_page": 25, "current_page": 1, "total_pages": 0}
          }
        }
        """
    }
}
