package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * v4.0 paginated response wrapper. Many endpoints now return:
 * ```json
 * { "success": true, "data": [...], "meta": { "pagination": {...} } }
 * ```
 * The `data` field type varies by endpoint.
 */
@Serializable
data class PaginatedResponse<T>(
    val success: Boolean = true,
    val data: T,
    val meta: ResponseMeta? = null,
)

@Serializable
data class ResponseMeta(
    val pagination: Pagination? = null,
    val sorting: Sorting? = null,
)

@Serializable
data class Pagination(
    val total: Int = 0,
    val count: Int = 0,
    @SerialName("per_page")
    val perPage: Int = 25,
    @SerialName("current_page")
    val currentPage: Int = 1,
    @SerialName("total_pages")
    val totalPages: Int = 1,
    val offset: Int? = null,
    @SerialName("has_more")
    val hasMore: Boolean? = null,
    @SerialName("has_previous")
    val hasPrevious: Boolean? = null,
    val links: PaginationLinks? = null,
)

@Serializable
data class PaginationLinks(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null,
)

@Serializable
data class Sorting(
    val field: String? = null,
    val order: String? = null,
)
