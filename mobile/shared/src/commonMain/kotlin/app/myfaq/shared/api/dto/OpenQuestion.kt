package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open question from `/open-questions`.
 * v4.0 returns paginated wrapper.
 */
@Serializable
data class OpenQuestion(
    val id: Int = 0,
    val lang: String? = null,
    val username: String? = null,
    val email: String? = null,
    @SerialName("categoryId")
    val categoryId: Int? = null,
    val question: String = "",
    val created: String? = null,
    @SerialName("answerId")
    val answerId: Int? = null,
    @SerialName("isVisible")
    val isVisible: String? = null,
)
