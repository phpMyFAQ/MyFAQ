package app.myfaq.shared.domain

/**
 * Lightweight, pure-Kotlin HTML utilities for KMP.
 *
 * Used to strip tags and decode entities when displaying FAQ content
 * as plain text (list rows, search results, previews).
 * Full HTML rendering is handled by platform WebViews in the detail screen.
 */
object HtmlUtils {
    // Common HTML character-entity references (named).
    private val namedEntities =
        mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'",
            "nbsp" to "\u00A0",
            "ndash" to "\u2013",
            "mdash" to "\u2014",
            "laquo" to "\u00AB",
            "raquo" to "\u00BB",
            "bull" to "\u2022",
            "hellip" to "\u2026",
            "copy" to "\u00A9",
            "reg" to "\u00AE",
            "trade" to "\u2122",
            "euro" to "\u20AC",
            "pound" to "\u00A3",
            "yen" to "\u00A5",
            "deg" to "\u00B0",
            "times" to "\u00D7",
            "divide" to "\u00F7",
            "micro" to "\u00B5",
            "para" to "\u00B6",
            "sect" to "\u00A7",
            "sup1" to "\u00B9",
            "sup2" to "\u00B2",
            "sup3" to "\u00B3",
            "frac14" to "\u00BC",
            "frac12" to "\u00BD",
            "frac34" to "\u00BE",
        )

    private val tagRegex = Regex("<[^>]*>")
    private val entityRegex = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")
    private val whitespaceRegex = Regex("\\s{2,}")

    /**
     * Strip all HTML tags and decode entities, returning clean plain text.
     * Block-level tags (`<p>`, `<br>`, `<div>`, `<li>`) are replaced with
     * a single newline so the output preserves basic paragraph structure.
     */
    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""

        var text = html

        // Replace block-level tags with newline before stripping
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("</?(p|div|li|tr|h[1-6])[^>]*>", RegexOption.IGNORE_CASE), "\n")

        // Strip remaining tags
        text = tagRegex.replace(text, "")

        // Decode entities
        text = decodeEntities(text)

        // Collapse whitespace within lines but keep newlines
        text =
            text
                .lines()
                .joinToString("\n") { line -> whitespaceRegex.replace(line.trim(), " ") }

        // Collapse multiple blank lines
        text = text.replace(Regex("\n{3,}"), "\n\n")

        return text.trim()
    }

    /**
     * Decode HTML character entities (named, decimal, hex) in the given string.
     * Useful for cleaning question titles that contain encoded characters.
     */
    fun decodeEntities(text: String): String =
        entityRegex.replace(text) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x", ignoreCase = true) -> {
                    val codePoint = entity.substring(2).toIntOrNull(16)
                    if (codePoint != null) codePoint.toChar().toString() else match.value
                }
                entity.startsWith("#") -> {
                    val codePoint = entity.substring(1).toIntOrNull()
                    if (codePoint != null) codePoint.toChar().toString() else match.value
                }
                else -> namedEntities[entity.lowercase()] ?: match.value
            }
        }

    /**
     * Return a short plain-text preview from HTML content,
     * truncated to [maxLength] characters with ellipsis.
     */
    fun preview(
        html: String,
        maxLength: Int = 150,
    ): String {
        val plain = stripHtml(html).replace('\n', ' ').trim()
        return if (plain.length <= maxLength) {
            plain
        } else {
            plain.take(maxLength).trimEnd() + "…"
        }
    }
}
