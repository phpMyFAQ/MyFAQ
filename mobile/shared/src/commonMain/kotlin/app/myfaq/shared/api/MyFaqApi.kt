package app.myfaq.shared.api

import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
import app.myfaq.shared.api.dto.FaqSummary
import app.myfaq.shared.api.dto.Meta
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.api.dto.OpenQuestion
import app.myfaq.shared.api.dto.PopularSearch
import app.myfaq.shared.api.dto.SearchResult
import app.myfaq.shared.api.dto.Tag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Hand-written wrapper around the phpMyFAQ v3.2 REST API.
 *
 * Phase 1 adds all read endpoints. Write endpoints (login, question,
 * register) land in Phase 3 behind the Pro entitlement gate.
 */
interface MyFaqApi {
    // Bootstrap
    suspend fun meta(): Meta

    // Categories
    suspend fun categories(): List<Category>

    // FAQs
    suspend fun faqsByCategory(categoryId: Int): List<FaqSummary>
    suspend fun faqDetail(categoryId: Int, faqId: Int): FaqDetail
    suspend fun faqsPopular(): List<FaqSummary>
    suspend fun faqsLatest(): List<FaqSummary>
    suspend fun faqsTrending(): List<FaqSummary>
    suspend fun faqsSticky(): List<FaqSummary>

    // Search
    suspend fun search(query: String): List<SearchResult>
    suspend fun popularSearches(): List<PopularSearch>

    // Tags
    suspend fun tags(): List<Tag>

    // News
    suspend fun news(): List<NewsItem>

    // Comments
    suspend fun comments(recordId: Int): List<Comment>

    // Open questions
    suspend fun openQuestions(): List<OpenQuestion>
}

class MyFaqApiImpl(
    private val http: HttpClient,
    private val baseUrl: String,
) : MyFaqApi {

    private val api get() = "$baseUrl/api/v3.2"

    override suspend fun meta(): Meta =
        http.get("$api/meta").body()

    override suspend fun categories(): List<Category> =
        http.get("$api/categories").body()

    override suspend fun faqsByCategory(categoryId: Int): List<FaqSummary> =
        http.get("$api/faqs/$categoryId").body()

    override suspend fun faqDetail(categoryId: Int, faqId: Int): FaqDetail =
        http.get("$api/faq/$categoryId/$faqId").body()

    override suspend fun faqsPopular(): List<FaqSummary> =
        http.get("$api/faqs/popular").body()

    override suspend fun faqsLatest(): List<FaqSummary> =
        http.get("$api/faqs/latest").body()

    override suspend fun faqsTrending(): List<FaqSummary> =
        http.get("$api/faqs/trending").body()

    override suspend fun faqsSticky(): List<FaqSummary> =
        http.get("$api/faqs/sticky").body()

    override suspend fun search(query: String): List<SearchResult> =
        http.get("$api/search") { parameter("q", query) }.body()

    override suspend fun popularSearches(): List<PopularSearch> =
        http.get("$api/searches/popular").body()

    override suspend fun tags(): List<Tag> =
        http.get("$api/tags").body()

    override suspend fun news(): List<NewsItem> =
        http.get("$api/news").body()

    override suspend fun comments(recordId: Int): List<Comment> =
        http.get("$api/comments/$recordId").body()

    override suspend fun openQuestions(): List<OpenQuestion> =
        http.get("$api/open-questions").body()
}
