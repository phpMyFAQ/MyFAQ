package app.myfaq.shared.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload from `GET /api/v4.0/meta`.
 *
 * Unknown or missing fields are tolerated — the shared `Json`
 * instance is configured with `ignoreUnknownKeys = true` so future
 * phpMyFAQ releases can add fields without breaking the client.
 *
 * v4.0 field names:
 *   availableLanguages  (object, e.g. {"de":"German","en":"English"})
 *   enabledFeatures     (object)
 *   publicLogoUrl       (string)
 *   oauthDiscovery      (object)
 */
@Serializable
data class Meta(
    val version: String,
    val title: String,
    val language: String,
    /** v4.0: object map e.g. {"de":"German","en":"English"} */
    val availableLanguages: Map<String, String> = emptyMap(),
    val enabledFeatures: Map<String, Boolean> = emptyMap(),
    val publicLogoUrl: String? = null,
    val oauthDiscovery: OAuthDiscovery? = null,
) {
    /** Convenience: list of language codes (keys). */
    val languageCodes: List<String> get() = availableLanguages.keys.toList()
}

@Serializable
data class OAuthDiscovery(
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
)
