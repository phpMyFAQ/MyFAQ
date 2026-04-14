package app.myfaq.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUtilsTest {
    // ── stripHtml ──────────────────────────────────────────────

    @Test
    fun `strips simple HTML tags`() {
        assertEquals("Hello World", HtmlUtils.stripHtml("<p>Hello World</p>"))
    }

    @Test
    fun `strips nested tags`() {
        assertEquals(
            "Bold and italic",
            HtmlUtils.stripHtml("<p><strong>Bold</strong> and <em>italic</em></p>"),
        )
    }

    @Test
    fun `converts br tags to newlines`() {
        val html = "Line 1<br>Line 2<br/>Line 3<br />"
        val result = HtmlUtils.stripHtml(html)
        assertEquals("Line 1\nLine 2\nLine 3", result)
    }

    @Test
    fun `converts block tags to newlines`() {
        val html = "<div>Block 1</div><div>Block 2</div>"
        val result = HtmlUtils.stripHtml(html)
        assertEquals("Block 1\n\nBlock 2", result)
    }

    @Test
    fun `handles list items`() {
        val html = "<ul><li>Item 1</li><li>Item 2</li></ul>"
        val result = HtmlUtils.stripHtml(html)
        assert(result.contains("Item 1"))
        assert(result.contains("Item 2"))
    }

    @Test
    fun `strips heading tags`() {
        assertEquals("Title", HtmlUtils.stripHtml("<h1>Title</h1>").trim())
    }

    @Test
    fun `returns empty for blank input`() {
        assertEquals("", HtmlUtils.stripHtml(""))
        assertEquals("", HtmlUtils.stripHtml("   "))
    }

    @Test
    fun `preserves plain text without tags`() {
        assertEquals("Just plain text", HtmlUtils.stripHtml("Just plain text"))
    }

    @Test
    fun `strips attributes from tags`() {
        assertEquals(
            "Linked text",
            HtmlUtils.stripHtml("""<a href="https://example.com" class="link">Linked text</a>"""),
        )
    }

    @Test
    fun `handles complex real-world HTML`() {
        val html =
            """
            <p>This is a <strong>FAQ answer</strong> with:</p>
            <ul>
                <li>Bullet 1</li>
                <li>Bullet 2</li>
            </ul>
            <p>And a <a href="https://example.com">link</a>.</p>
            """.trimIndent()
        val result = HtmlUtils.stripHtml(html)
        assert(result.contains("FAQ answer"))
        assert(result.contains("Bullet 1"))
        assert(result.contains("Bullet 2"))
        assert(result.contains("link"))
        assert(!result.contains("<"))
    }

    // ── decodeEntities ─────────────────────────────────────────

    @Test
    fun `decodes common named entities`() {
        assertEquals("A & B", HtmlUtils.decodeEntities("A &amp; B"))
        assertEquals("1 < 2", HtmlUtils.decodeEntities("1 &lt; 2"))
        assertEquals("3 > 2", HtmlUtils.decodeEntities("3 &gt; 2"))
        assertEquals("He said \"hi\"", HtmlUtils.decodeEntities("He said &quot;hi&quot;"))
    }

    @Test
    fun `decodes apostrophe entity`() {
        assertEquals("it's", HtmlUtils.decodeEntities("it&apos;s"))
    }

    @Test
    fun `decodes nbsp entity`() {
        assertEquals("a\u00A0b", HtmlUtils.decodeEntities("a&nbsp;b"))
    }

    @Test
    fun `decodes decimal numeric entities`() {
        assertEquals("©", HtmlUtils.decodeEntities("&#169;"))
        assertEquals("A", HtmlUtils.decodeEntities("&#65;"))
    }

    @Test
    fun `decodes hex numeric entities`() {
        assertEquals("©", HtmlUtils.decodeEntities("&#xA9;"))
        assertEquals("€", HtmlUtils.decodeEntities("&#x20AC;"))
    }

    @Test
    fun `preserves unknown entities`() {
        assertEquals("&unknown;", HtmlUtils.decodeEntities("&unknown;"))
    }

    @Test
    fun `decodes multiple mixed entities`() {
        assertEquals(
            "A & B < C \"D\"",
            HtmlUtils.decodeEntities("A &amp; B &lt; C &quot;D&quot;"),
        )
    }

    @Test
    fun `handles text without entities`() {
        assertEquals("No entities here", HtmlUtils.decodeEntities("No entities here"))
    }

    @Test
    fun `decodes currency entities`() {
        assertEquals("€", HtmlUtils.decodeEntities("&euro;"))
        assertEquals("£", HtmlUtils.decodeEntities("&pound;"))
        assertEquals("¥", HtmlUtils.decodeEntities("&yen;"))
    }

    @Test
    fun `decodes typographic entities`() {
        assertEquals("\u2013", HtmlUtils.decodeEntities("&ndash;")) // en-dash
        assertEquals("\u2014", HtmlUtils.decodeEntities("&mdash;")) // em-dash
        assertEquals("\u2026", HtmlUtils.decodeEntities("&hellip;")) // ellipsis
        assertEquals("\u2022", HtmlUtils.decodeEntities("&bull;")) // bullet
    }

    // ── stripHtml + decodeEntities combined ────────────────────

    @Test
    fun `stripHtml also decodes entities`() {
        assertEquals(
            "Tom & Jerry",
            HtmlUtils.stripHtml("<p>Tom &amp; Jerry</p>"),
        )
    }

    @Test
    fun `stripHtml decodes entities in questions with HTML`() {
        assertEquals(
            "What's the \"best\" way?",
            HtmlUtils.stripHtml("What&apos;s the &quot;best&quot; way?"),
        )
    }

    // ── preview ────────────────────────────────────────────────

    @Test
    fun `preview strips HTML and truncates`() {
        val html = "<p>${"A".repeat(200)}</p>"
        val result = HtmlUtils.preview(html, maxLength = 50)
        assertEquals(51, result.length) // 50 chars + ellipsis
        assert(result.endsWith("…"))
    }

    @Test
    fun `preview returns full text if short enough`() {
        val result = HtmlUtils.preview("<b>Short</b>")
        assertEquals("Short", result)
    }

    @Test
    fun `preview replaces newlines with spaces`() {
        val result = HtmlUtils.preview("<p>Line 1</p><p>Line 2</p>")
        assert(!result.contains("\n"))
        assert(result.contains("Line 1"))
        assert(result.contains("Line 2"))
    }

    @Test
    fun `preview handles empty HTML`() {
        assertEquals("", HtmlUtils.preview(""))
    }
}
