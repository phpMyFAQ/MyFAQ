package app.myfaq.shared.api

import app.myfaq.shared.api.dto.Meta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Hand-written wrapper around the generated phpMyFAQ API client.
 *
 * Phase 0 exposes only [meta] because that is the single call the
 * placeholder screen exercises. Later phases add categories, FAQs,
 * search, etc., always behind this interface so generated DTOs never
 * leak into DAOs or UI code.
 */
interface MyFaqApi {
    suspend fun meta(): Meta
}

class MyFaqApiImpl(
    private val http: HttpClient,
    private val baseUrl: String,
) : MyFaqApi {
    override suspend fun meta(): Meta =
        http.get("$baseUrl/api/v3.2/meta").body()
}
