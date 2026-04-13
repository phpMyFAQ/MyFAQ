package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Comment from `/comments/{faqId}`.
 * v4.0 returns paginated wrapper; fields differ from v3.2.
 */
@Serializable
data class Comment(
    val id: Int = 0,
    @SerialName("recordId")
    val recordId: Int = 0,
    @SerialName("categoryId")
    val categoryId: Int? = null,
    val type: String? = null,
    val username: String = "",
    val comment: String = "",
    val date: String? = null,
    val helped: String? = null,
)
