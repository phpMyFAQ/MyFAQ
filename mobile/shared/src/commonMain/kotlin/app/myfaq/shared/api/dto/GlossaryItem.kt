package app.myfaq.shared.api.dto

import kotlinx.serialization.Serializable

/**
 * Glossary item from `GET /api/v4.0/glossary` (paginated).
 */
@Serializable
data class GlossaryItem(
    val id: Int = 0,
    val language: String? = null,
    val item: String = "",
    val definition: String = "",
)
