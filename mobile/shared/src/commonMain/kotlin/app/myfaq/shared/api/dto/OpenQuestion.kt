package app.myfaq.shared.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenQuestion(
    val id: Int,
    val question: String,
    val author: String? = null,
    val created: String? = null,
)
