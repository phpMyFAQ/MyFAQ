package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search result from `/search`.
 * v4.0 returns paginated wrapper. Note: `id` and `category_id`
 * may arrive as strings in the JSON — we keep Int with defaults.
 */
@Serializable
data class SearchResult(
    val id: Int = 0,
    val lang: String? = null,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val question: String = "",
    val answer: String? = null,
    val link: String? = null,
)

/**
 * Popular search term from `/searches/popular`.
 * v4.0 returns a plain array (NOT paginated).
 */
@Serializable
data class PopularSearch(
    val id: Int = 0,
    @SerialName("searchterm")
    val searchTerm: String = "",
    val number: String = "0",
    val lang: String? = null,
) {
    /** Convenience: `number` arrives as string in the JSON. */
    val count: Int get() = number.toIntOrNull() ?: 0
}
