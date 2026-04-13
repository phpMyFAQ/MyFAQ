package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("parent_id")
    val parentId: Int? = null,
    val lang: String? = null,
    val image: String? = null,
    @SerialName("user_id")
    val userId: Int? = null,
    @SerialName("group_id")
    val groupId: Int? = null,
    val active: Int? = null,
    @SerialName("show_home")
    val showHome: Int? = null,
    val level: Int? = null,
)
