package app.myfaq.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json

/**
 * Phase 0 HTTP client factory. The default engine is a [MockEngine]
 * wired up with an inline `/meta` fixture so the app can be
 * exercised end-to-end with no network. Phase 1 introduces the real
 * per-platform engines (OkHttp / Darwin).
 */
object HttpClientFactory {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun phase0MockClient(): HttpClient =
        HttpClient(
            MockEngine { _ ->
                respond(
                    content = ByteReadChannel(PHASE0_META_FIXTURE),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(json) }
        }
}

internal const val PHASE0_META_FIXTURE: String = """
{
  "version": "4.2.0",
  "title": "MyFAQ.app demo instance",
  "language": "en",
  "available_languages": ["en", "de"],
  "features": { "search": true, "ratings": true },
  "logo_url": null,
  "oauth": null
}
"""
