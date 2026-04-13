package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val id: Int,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val question: String,
    val answer: String? = null,
)

@Serializable
data class PopularSearch(
    @SerialName("search_term")
    val searchTerm: String,
    val count: Int = 0,
)
