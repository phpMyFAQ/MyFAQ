package app.myfaq.shared.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsItem(
    val id: Int,
    val title: String,
    val body: String = "",
    val author: String? = null,
    val created: String? = null,
)
