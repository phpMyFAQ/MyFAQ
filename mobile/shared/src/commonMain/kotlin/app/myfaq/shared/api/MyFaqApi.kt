package app.myfaq.shared.api

import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.FaqSummary
import app.myfaq.shared.api.dto.GlossaryItem
import app.myfaq.shared.api.dto.Meta
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.api.dto.OpenQuestion
import app.myfaq.shared.api.dto.PaginatedResponse
import app.myfaq.shared.api.dto.PopularSearch
import app.myfaq.shared.api.dto.SearchResult
import app.myfaq.shared.api.dto.Tag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

/**
 * Hand-written wrapper around the phpMyFAQ v4.0 REST API.
 *
 * Phase 1 adds all read endpoints. Write endpoints (login, question,
 * register) land in Phase 3 behind the Pro entitlement gate.
 *
 * v4.0 changes:
 * - Most list endpoints return a paginated wrapper: `{ success, data, meta }`.
 * - Popular/latest/trending/sticky FAQs and popular searches remain plain arrays.
 * - `/meta` now at v4.0 path with updated field names (availableLanguages, enabledFeatures, etc.).
 */
interface MyFaqApi {
    // Bootstrap
    suspend fun meta(): Meta

    // Categories (paginated)
    suspend fun categories(): List<Category>

    // FAQs
    suspend fun faqsByCategory(categoryId: Int): List<FaqSummary> // paginated

    suspend fun faqDetail(
        categoryId: Int,
        faqId: Int,
    ): FaqDetail

    suspend fun faqsPopular(): List<FaqPopularItem> // plain array

    suspend fun faqsLatest(): List<FaqPopularItem> // plain array

    suspend fun faqsTrending(): List<FaqPopularItem> // plain array

    suspend fun faqsSticky(): List<FaqPopularItem> // plain array

    // Search
    suspend fun search(query: String): List<SearchResult> // paginated

    suspend fun popularSearches(): List<PopularSearch> // plain array

    // Tags (paginated)
    suspend fun tags(): List<Tag>

    // News (paginated)
    suspend fun news(): List<NewsItem>

    // Comments (paginated)
    suspend fun comments(recordId: Int): List<Comment>

    // Glossary (paginated)
    suspend fun glossary(): List<GlossaryItem>

    // Open questions (paginated)
    suspend fun openQuestions(): List<OpenQuestion>
}

class MyFaqApiImpl(
    private val http: HttpClient,
    private val baseUrl: String,
    private val language: String = "en",
) : MyFaqApi {
    private val api get() = "$baseUrl/api/v4.0"

    override suspend fun meta(): Meta = http.get("$api/meta").body()

    // --- Paginated endpoints: unwrap { success, data, meta } ---

    override suspend fun categories(): List<Category> =
        http
            .get("$api/categories") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<Category>>>()
            .data

    override suspend fun faqsByCategory(categoryId: Int): List<FaqSummary> =
        http
            .get("$api/faqs/$categoryId") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<FaqSummary>>>()
            .data

    override suspend fun faqDetail(
        categoryId: Int,
        faqId: Int,
    ): FaqDetail = http.get("$api/faq/$categoryId/$faqId") { header("Accept-Language", language) }.body()

    // --- Plain array endpoints (no pagination wrapper) ---

    override suspend fun faqsPopular(): List<FaqPopularItem> =
        http.get("$api/faqs/popular") { header("Accept-Language", language) }.body()

    override suspend fun faqsLatest(): List<FaqPopularItem> =
        http.get("$api/faqs/latest") { header("Accept-Language", language) }.body()

    override suspend fun faqsTrending(): List<FaqPopularItem> =
        http.get("$api/faqs/trending") { header("Accept-Language", language) }.body()

    override suspend fun faqsSticky(): List<FaqPopularItem> =
        http.get("$api/faqs/sticky") { header("Accept-Language", language) }.body()

    override suspend fun popularSearches(): List<PopularSearch> =
        http.get("$api/searches/popular") { header("Accept-Language", language) }.body()

    // --- Paginated endpoints ---

    override suspend fun search(query: String): List<SearchResult> =
        http
            .get("$api/search") {
                header("Accept-Language", language)
                parameter("q", query)
            }.body<PaginatedResponse<List<SearchResult>>>()
            .data

    override suspend fun tags(): List<Tag> =
        http
            .get("$api/tags") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<Tag>>>()
            .data

    override suspend fun news(): List<NewsItem> =
        http
            .get("$api/news") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<NewsItem>>>()
            .data

    override suspend fun comments(recordId: Int): List<Comment> =
        http
            .get("$api/comments/$recordId") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<Comment>>>()
            .data

    override suspend fun glossary(): List<GlossaryItem> =
        http
            .get("$api/glossary") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<GlossaryItem>>>()
            .data

    override suspend fun openQuestions(): List<OpenQuestion> =
        http
            .get("$api/open-questions") { header("Accept-Language", language) }
            .body<PaginatedResponse<List<OpenQuestion>>>()
            .data
}
