package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Int,
    @SerialName("faq_id")
    val faqId: Int = 0,
    val author: String = "",
    val body: String = "",
    val created: String? = null,
)
