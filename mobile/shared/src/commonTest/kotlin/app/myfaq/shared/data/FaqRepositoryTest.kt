package app.myfaq.shared.data

import app.myfaq.shared.api.FakeMyFaqApi
import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.NewsItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Cache contract tests for [FaqRepository]. Uses [InMemoryCacheStore] +
 * [FakeMyFaqApi] so the suite runs in `commonTest` without a SQLDelight
 * driver. Time is mocked via a mutable [now] reference.
 */
class FaqRepositoryTest {
    private val instanceId = "instance-1"
    private var now: Long = 1_000L
    private val cache = InMemoryCacheStore(now = { now })

    private fun newApi(block: FakeMyFaqApi.() -> Unit = {}): FakeMyFaqApi = FakeMyFaqApi().apply(block)

    private fun newRepo(api: FakeMyFaqApi = FakeMyFaqApi()): FaqRepository = FaqRepository(api, cache, instanceId)

    @Test
    fun `first call hits the network and caches the result`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "Q1")) }
                }
            val repo = newRepo(api)

            val result = repo.faqsSticky()

            assertEquals(1, result.size)
            assertEquals("Q1", result.first().question)
            assertEquals(1, api.faqsStickyCalls)
            assertNotNull(cache.get(instanceId, "faqs/sticky"))
        }

    @Test
    fun `second call within TTL serves from cache without hitting the network`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "Q1")) }
                }
            val repo = newRepo(api)

            repo.faqsSticky()
            now += 60 // well within FAQS TTL (6h)
            val second = repo.faqsSticky()

            assertEquals(1, api.faqsStickyCalls)
            assertEquals("Q1", second.first().question)
        }

    @Test
    fun `expired cache triggers a refetch`() =
        runTest {
            var responseCounter = 0
            val api =
                newApi {
                    faqsStickyResponse = {
                        responseCounter += 1
                        listOf(FaqPopularItem(question = "Q$responseCounter"))
                    }
                }
            val repo = newRepo(api)

            repo.faqsSticky()
            // FAQS TTL is 6h; jump 7h forward so the entry is expired.
            now += CacheTtl.FAQS + 3_600
            val second = repo.faqsSticky()

            assertEquals(2, api.faqsStickyCalls)
            assertEquals("Q2", second.first().question)
        }

    @Test
    fun `network failure falls back to stale cache`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "cached")) }
                }
            val repo = newRepo(api)

            // Prime the cache, then expire it and switch to a failing fetch.
            repo.faqsSticky()
            now += CacheTtl.FAQS + 3_600
            api.faqsStickyResponse = { error("network down") }

            val result = repo.faqsSticky()

            assertEquals(1, result.size)
            assertEquals("cached", result.first().question)
            assertEquals(2, api.faqsStickyCalls) // attempted again, then fell back
        }

    @Test
    fun `network failure with no cache rethrows`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { error("boom") }
                }
            val repo = newRepo(api)

            val ex = assertFailsWith<IllegalStateException> { repo.faqsSticky() }
            assertEquals("boom", ex.message)
        }

    @Test
    fun `clearCache wipes only the active instance`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "Q1")) }
                }
            val repo = newRepo(api)
            repo.faqsSticky()

            // Seed a different instance's cache entry.
            cache.put("other-instance", "faqs/sticky", "[]", CacheTtl.FAQS)

            repo.clearCache()

            assertNull(cache.get(instanceId, "faqs/sticky"))
            assertNotNull(cache.get("other-instance", "faqs/sticky"))
        }

    @Test
    fun `different cache keys do not collide`() =
        runTest {
            val api =
                newApi {
                    faqsStickyResponse = { listOf(FaqPopularItem(question = "sticky")) }
                    faqsPopularResponse = { listOf(FaqPopularItem(question = "popular")) }
                }
            val repo = newRepo(api)

            val sticky = repo.faqsSticky()
            val popular = repo.faqsPopular()

            assertEquals("sticky", sticky.first().question)
            assertEquals("popular", popular.first().question)
            assertEquals(1, api.faqsStickyCalls)
            assertEquals(1, api.faqsPopularCalls)
        }

    @Test
    fun `comments are scoped per record id`() =
        runTest {
            val api =
                newApi {
                    commentsResponse = { recordId ->
                        listOf(
                            app.myfaq.shared.api.dto.Comment(
                                id = recordId,
                                username = "u$recordId",
                                comment = "c$recordId",
                            ),
                        )
                    }
                }
            val repo = newRepo(api)

            val a = repo.comments(1)
            val b = repo.comments(2)
            // Re-fetch the first one — should be cached, no extra API call.
            val aAgain = repo.comments(1)

            assertEquals(1, a.first().id)
            assertEquals(2, b.first().id)
            assertEquals(1, aAgain.first().id)
            assertEquals(2, api.commentsCalls)
        }

    @Test
    fun `news caches and survives until TTL`() =
        runTest {
            val api =
                newApi {
                    newsResponse = { listOf(NewsItem(id = 1, header = "Hello")) }
                }
            val repo = newRepo(api)

            repo.news()
            now += CacheTtl.NEWS - 1
            repo.news()

            assertEquals(1, api.newsCalls)
        }

    @Test
    fun `news refetches once TTL elapses`() =
        runTest {
            val api =
                newApi {
                    newsResponse = { listOf(NewsItem(id = 1, header = "Hello")) }
                }
            val repo = newRepo(api)

            repo.news()
            now += CacheTtl.NEWS + 1
            repo.news()

            assertEquals(2, api.newsCalls)
        }
}
