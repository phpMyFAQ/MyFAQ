package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FAQ summary returned by `/faqs/{categoryId}`, `/faqs/popular`, etc.
 * v4.0 uses `record_*` field names for category-scoped lists.
 */
@Serializable
data class FaqSummary(
    // v4.0 field names from /faqs/{categoryId}
    @SerialName("record_id")
    val id: Int = 0,
    @SerialName("category_id")
    val categoryId: Int = 0,
    @SerialName("record_title")
    val question: String = "",
    @SerialName("record_preview")
    val preview: String? = null,
    @SerialName("record_updated")
    val updated: String? = null,
    @SerialName("record_link")
    val link: String? = null,
    @SerialName("record_lang")
    val lang: String? = null,
    @SerialName("record_created")
    val created: String? = null,
    val visits: Int? = null,
    val author: String? = null,
    @SerialName("is_sticky")
    val isSticky: Boolean = false,
)

/**
 * Popular/latest/trending FAQ item.
 * These endpoints return a different shape than category lists.
 */
@Serializable
data class FaqPopularItem(
    val date: String? = null,
    val question: String = "",
    val answer: String? = null,
    val visits: Int? = null,
    val url: String? = null,
    val id: Int? = null,
    val order: Int? = null,
) {
    /**
     * Parse categoryId and faqId from the phpMyFAQ URL.
     * URL format: `.../content/{categoryId}/{faqId}/{lang}/slug.html`
     */
    val parsedCategoryId: Int?
        get() = parseUrlSegment(0)

    val parsedFaqId: Int?
        get() = parseUrlSegment(1)

    private fun parseUrlSegment(index: Int): Int? {
        val u = url ?: return null
        val contentIdx = u.indexOf("/content/")
        if (contentIdx == -1) return null
        val segments = u.substring(contentIdx + "/content/".length).split("/")
        return segments.getOrNull(index)?.toIntOrNull()
    }
}

/**
 * FAQ detail from `/faq/{categoryId}/{faqId}`.
 * v4.0 returns flat object (not wrapped in pagination).
 */
@Serializable
data class FaqDetail(
    val id: Int,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val question: String = "",
    val answer: String = "",
    val keywords: String? = null,
    val author: String? = null,
    val email: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val sticky: Int = 0,
    val active: String? = null,
    @SerialName("solution_id")
    val solutionId: Int? = null,
    @SerialName("revision_id")
    val revisionId: Int? = null,
    val comment: String? = null,
    val lang: String? = null,
    val link: String? = null,
    @SerialName("dateStart")
    val dateStart: String? = null,
    @SerialName("dateEnd")
    val dateEnd: String? = null,
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
) {
    /** Convenience: v4.0 uses int 1/0 for sticky, map to boolean. */
    val isSticky: Boolean get() = sticky != 0
}

@Serializable
data class Attachment(
    val id: Int? = null,
    val filename: String = "",
    val url: String? = null,
    val size: Long? = null,
    @SerialName("filesize")
    val filesize: Long? = null,
    val mime: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
)
