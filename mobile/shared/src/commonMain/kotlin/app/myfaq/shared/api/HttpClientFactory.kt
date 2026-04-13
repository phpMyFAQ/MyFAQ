package app.myfaq.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration used by all platform HTTP clients
 * and by tests. `ignoreUnknownKeys` + `explicitNulls = false`
 * ensure forward compatibility with future phpMyFAQ versions.
 */
object HttpClientFactory {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            isLenient = true
        }

    /**
     * Install shared plugins on a platform-provided [HttpClient].
     */
    fun configure(client: HttpClient): HttpClient =
        client.config {
            install(ContentNegotiation) { json(json) }
        }
}

/**
 * Platform-specific HTTP client. Android → OkHttp, iOS → Darwin.
 */
expect fun createPlatformHttpClient(): HttpClient
