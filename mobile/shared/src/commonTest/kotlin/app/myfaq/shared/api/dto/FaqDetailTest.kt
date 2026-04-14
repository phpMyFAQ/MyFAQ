package app.myfaq.shared.api.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaqDetailTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun `deserializes FAQ detail with HTML answer`() {
        val input = """
        {
          "id": 42,
          "category_id": 15,
          "question": "How do I configure?",
          "answer": "<p>Follow these <strong>steps</strong>:</p><ul><li>Step 1</li><li>Step 2</li></ul>"
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        assertEquals(42, detail.id)
        assertEquals(15, detail.categoryId)
        assertEquals("How do I configure?", detail.question)
        assertTrue(detail.answer.contains("<p>"))
        assertTrue(detail.answer.contains("<strong>"))
        assertTrue(detail.answer.contains("<ul>"))
    }

    @Test
    fun `preserves HTML entities in question field`() {
        val input = """
        {
          "id": 1,
          "question": "What&apos;s the &quot;best&quot; way to configure &amp; deploy?",
          "answer": "<p>Answer</p>"
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        // Raw deserialization preserves entities
        assertEquals(
            "What&apos;s the &quot;best&quot; way to configure &amp; deploy?",
            detail.question,
        )
    }

    @Test
    fun `deserializes tags`() {
        val input = """
        {
          "id": 1,
          "question": "Q",
          "answer": "<p>A</p>",
          "tags": ["php", "mysql", "security"]
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        assertEquals(listOf("php", "mysql", "security"), detail.tags)
    }

    @Test
    fun `defaults to empty tags when missing`() {
        val input = """{"id": 1, "question": "Q", "answer": "A"}"""
        val detail = json.decodeFromString<FaqDetail>(input)
        assertTrue(detail.tags.isEmpty())
    }

    @Test
    fun `deserializes attachments`() {
        val input = """
        {
          "id": 1,
          "question": "Q",
          "answer": "A",
          "attachments": [
            {"id": 10, "filename": "doc.pdf", "mime_type": "application/pdf"}
          ]
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        assertEquals(1, detail.attachments.size)
        assertEquals("doc.pdf", detail.attachments[0].filename)
    }

    @Test
    fun `isSticky maps integer to boolean`() {
        val input1 = """{"id": 1, "question": "Q", "answer": "A", "sticky": 1}"""
        assertTrue(json.decodeFromString<FaqDetail>(input1).isSticky)

        val input0 = """{"id": 1, "question": "Q", "answer": "A", "sticky": 0}"""
        assertTrue(!json.decodeFromString<FaqDetail>(input0).isSticky)
    }

    @Test
    fun `preserves complex HTML with images and code blocks`() {
        val input = """
        {
          "id": 1,
          "question": "Q",
          "answer": "<p>Text</p><img src=\"https://example.com/img.png\" alt=\"screenshot\"/><pre><code>echo 'hello';</code></pre>"
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        assertTrue(detail.answer.contains("<img"))
        assertTrue(detail.answer.contains("<pre>"))
        assertTrue(detail.answer.contains("<code>"))
    }

    @Test
    fun `deserializes all metadata fields`() {
        val input = """
        {
          "id": 99,
          "category_id": 5,
          "question": "Q",
          "answer": "A",
          "author": "Admin",
          "created": "2024-01-15",
          "updated": "2024-06-20",
          "solution_id": 1001,
          "lang": "en",
          "link": "https://faq.example.com/content/5/99/en/q.html"
        }
        """
        val detail = json.decodeFromString<FaqDetail>(input)
        assertEquals("Admin", detail.author)
        assertEquals("2024-01-15", detail.created)
        assertEquals("2024-06-20", detail.updated)
        assertEquals(1001, detail.solutionId)
        assertEquals("en", detail.lang)
    }
}
