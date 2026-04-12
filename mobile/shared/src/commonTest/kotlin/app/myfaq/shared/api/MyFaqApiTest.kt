package app.myfaq.shared.api

import app.myfaq.shared.api.dto.Meta
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyFaqApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun client(body: String): HttpClient =
        HttpClient(
            MockEngine { _ ->
                respond(
                    content = ByteReadChannel(body),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(json) }
        }

    @Test
    fun `meta deserializes full fixture`() = runTest {
        val api = MyFaqApiImpl(client(FULL_FIXTURE), baseUrl = "https://example.test")
        val meta = api.meta()
        assertEquals("4.2.0", meta.version)
        assertEquals("MyFAQ.app test instance", meta.title)
        assertEquals(listOf("en", "de", "fr"), meta.availableLanguages)
        assertEquals(true, meta.features["search"])
        assertEquals("https://example.test/oauth/authorize", meta.oauth?.authorizationEndpoint)
    }

    @Test
    fun `meta tolerates unknown keys`() = runTest {
        val api = MyFaqApiImpl(client(FUTURE_FIXTURE), baseUrl = "https://example.test")
        val meta: Meta = api.meta()
        assertEquals("4.3.0", meta.version)
        assertTrue(meta.availableLanguages.isEmpty())
    }

    @Test
    fun `meta loader renders success string`() = runTest {
        val api = MyFaqApiImpl(client(FULL_FIXTURE), baseUrl = "https://example.test")
        val loader = MetaLoader(api, scope = this)
        var result: String? = null
        loader.load(onSuccess = { result = it }, onError = { result = "err: $it" })
        testScheduler.advanceUntilIdle()
        assertEquals("MyFAQ.app test instance — phpMyFAQ 4.2.0", result)
    }

    private companion object {
        const val FULL_FIXTURE = """
        {
          "version": "4.2.0",
          "title": "MyFAQ.app test instance",
          "language": "en",
          "available_languages": ["en", "de", "fr"],
          "features": { "search": true, "ratings": true, "comments": false },
          "logo_url": "https://example.test/logo.png",
          "oauth": {
            "authorization_endpoint": "https://example.test/oauth/authorize",
            "token_endpoint": "https://example.test/oauth/token"
          }
        }
        """

        const val FUTURE_FIXTURE = """
        {
          "version": "4.3.0",
          "title": "Future instance",
          "language": "en",
          "brand_new_field": "ignored",
          "another_unknown": { "nested": 1 }
        }
        """
    }
}
