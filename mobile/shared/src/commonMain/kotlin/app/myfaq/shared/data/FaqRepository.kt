package app.myfaq.shared.data

import app.myfaq.shared.api.HttpClientFactory
import app.myfaq.shared.api.MyFaqApi
import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.FaqSummary
import app.myfaq.shared.api.dto.GlossaryItem
import app.myfaq.shared.api.dto.Meta
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.api.dto.OpenQuestion
import app.myfaq.shared.api.dto.PopularSearch
import app.myfaq.shared.api.dto.SearchResult
import app.myfaq.shared.api.dto.Tag
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

/**
 * Repository layer between API and UI. Checks TTL cache first,
 * falls back to network, caches result. On network error returns
 * stale cache if available.
 *
 * Phase 1 caches serialized JSON blobs. Phase 2 replaces with
 * typed SQLite tables for offline + FTS.
 */
class FaqRepository(
    private val api: MyFaqApi,
    private val cache: CacheStore,
    private val instanceId: String,
) {
    private val json = HttpClientFactory.json

    // --- Bootstrap ---

    suspend fun meta(): Meta =
        cached("meta", CacheTtl.FAQS) {
            json.encodeToString(api.meta())
        }.let { json.decodeFromString(it) }

    // --- Categories ---

    suspend fun categories(): List<Category> =
        cachedList("categories", CacheTtl.CATEGORIES) {
            api.categories()
        }

    // --- FAQs ---

    suspend fun faqsByCategory(categoryId: Int): List<FaqSummary> =
        cachedList("faqs/cat/$categoryId", CacheTtl.FAQS) {
            api.faqsByCategory(categoryId)
        }

    suspend fun faqDetail(
        categoryId: Int,
        faqId: Int,
    ): FaqDetail =
        cached("faq/$categoryId/$faqId", CacheTtl.FAQS) {
            json.encodeToString(api.faqDetail(categoryId, faqId))
        }.let { json.decodeFromString(it) }

    suspend fun faqsPopular(): List<FaqPopularItem> = cachedList("faqs/popular", CacheTtl.FAQS) { api.faqsPopular() }

    suspend fun faqsLatest(): List<FaqPopularItem> = cachedList("faqs/latest", CacheTtl.FAQS) { api.faqsLatest() }

    suspend fun faqsTrending(): List<FaqPopularItem> = cachedList("faqs/trending", CacheTtl.FAQS) { api.faqsTrending() }

    suspend fun faqsSticky(): List<FaqPopularItem> = cachedList("faqs/sticky", CacheTtl.FAQS) { api.faqsSticky() }

    // --- Search ---

    suspend fun search(query: String): List<SearchResult> =
        cachedList("search/$query", CacheTtl.SEARCH) { api.search(query) }

    suspend fun popularSearches(): List<PopularSearch> =
        cachedList("searches/popular", CacheTtl.SEARCH) { api.popularSearches() }

    // --- Tags ---

    suspend fun tags(): List<Tag> = cachedList("tags", CacheTtl.TAGS) { api.tags() }

    // --- News ---

    suspend fun news(): List<NewsItem> = cachedList("news", CacheTtl.NEWS) { api.news() }

    // --- Comments ---

    suspend fun comments(recordId: Int): List<Comment> =
        cachedList("comments/$recordId", CacheTtl.COMMENTS) { api.comments(recordId) }

    // --- Glossary ---

    suspend fun glossary(): List<GlossaryItem> = cachedList("glossary", CacheTtl.FAQS) { api.glossary() }

    // --- Open questions ---

    suspend fun openQuestions(): List<OpenQuestion> =
        cachedList("open-questions", CacheTtl.FAQS) { api.openQuestions() }

    // --- Cache ---

    fun clearCache() = cache.clearInstance(instanceId)

    // --- Private helpers ---

    private suspend inline fun cached(
        key: String,
        ttl: Long,
        crossinline fetch: suspend () -> String,
    ): String {
        cache.get(instanceId, key)?.let { return it }
        return try {
            val result = fetch()
            cache.put(instanceId, key, result, ttl)
            result
        } catch (e: Exception) {
            cache.getStale(instanceId, key) ?: throw e
        }
    }

    private suspend inline fun <reified T> cachedList(
        key: String,
        ttl: Long,
        crossinline fetch: suspend () -> List<T>,
    ): List<T> {
        val raw =
            cached(key, ttl) {
                json.encodeToString(ListSerializer(kotlinx.serialization.serializer<T>()), fetch())
            }
        return json.decodeFromString(ListSerializer(kotlinx.serialization.serializer<T>()), raw)
    }
}
