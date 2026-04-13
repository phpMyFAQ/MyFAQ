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
)
