package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FaqSummary(
    val id: Int,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val question: String,
    val updated: String? = null,
    val author: String? = null,
    @SerialName("is_sticky")
    val isSticky: Boolean = false,
)

@Serializable
data class FaqDetail(
    val id: Int,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val question: String,
    val answer: String = "",
    val keywords: String? = null,
    val author: String? = null,
    val created: String? = null,
    val updated: String? = null,
    @SerialName("is_sticky")
    val isSticky: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true,
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)

@Serializable
data class Attachment(
    val id: Int,
    val filename: String,
    val size: Long? = null,
    val mime: String? = null,
)
