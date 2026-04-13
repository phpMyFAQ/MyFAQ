package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * News item from `/news`.
 * v4.0 returns paginated wrapper with richer fields.
 */
@Serializable
data class NewsItem(
    val id: Int = 0,
    val lang: String? = null,
    val date: String? = null,
    val header: String = "",
    val content: String = "",
    @SerialName("authorName")
    val authorName: String? = null,
    @SerialName("authorEmail")
    val authorEmail: String? = null,
    val active: Boolean? = null,
    @SerialName("allowComments")
    val allowComments: Boolean? = null,
    val link: String? = null,
    @SerialName("linkTitle")
    val linkTitle: String? = null,
    val target: String? = null,
    val url: String? = null,
)
