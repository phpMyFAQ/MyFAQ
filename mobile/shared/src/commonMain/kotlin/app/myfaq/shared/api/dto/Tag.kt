package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tag from `/tags`.
 * v4.0 returns paginated wrapper; field names use camelCase.
 */
@Serializable
data class Tag(
    @SerialName("tagId")
    val id: Int = 0,
    @SerialName("tagName")
    val name: String = "",
    @SerialName("tagFrequency")
    val frequency: Int = 0,
)
