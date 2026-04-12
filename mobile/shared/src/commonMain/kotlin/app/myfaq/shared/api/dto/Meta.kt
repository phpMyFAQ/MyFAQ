package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload from `GET /api/v3.2/meta`.
 *
 * Unknown or missing fields are tolerated — the shared `Json`
 * instance is configured with `ignoreUnknownKeys = true` so future
 * phpMyFAQ releases can add fields without breaking the client.
 */
@Serializable
data class Meta(
    val version: String,
    val title: String,
    val language: String,
    @SerialName("available_languages")
    val availableLanguages: List<String> = emptyList(),
    val features: Map<String, Boolean> = emptyMap(),
    @SerialName("logo_url")
    val logoUrl: String? = null,
    val oauth: OAuthDiscovery? = null,
)

@Serializable
data class OAuthDiscovery(
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
)
