package app.myfaq.shared.domain

/**
 * Domain representation of a registered phpMyFAQ workspace.
 *
 * Secrets (`x-pmf-token`, OAuth client id/secret, session cookies)
 * never live on this object — they live in [app.myfaq.shared.platform.SecureStore]
 * and are looked up by key derived from [id].
 */
data class Instance(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiVersion: String,
    val language: String = "en",
    val authMode: AuthMode,
    val lastSuccessfulPing: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class AuthMode {
    NONE,
    TOKEN,
    USER_SESSION,
    OAUTH2,
}
