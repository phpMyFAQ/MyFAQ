package app.myfaq.shared.api

import app.myfaq.shared.api.dto.Attachment
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

/**
 * Test double for [MyFaqApi]. Each suspend method delegates to a
 * mutable lambda so individual tests can program the response (success
 * data, throw exception, count invocations).
 *
 * Defaults return empty lists / minimal objects so a test only needs
 * to override the methods it cares about.
 */
class FakeMyFaqApi : MyFaqApi {
    var metaResponse: () -> Meta = { Meta() }
    var categoriesResponse: () -> List<Category> = { emptyList() }
    var faqsByCategoryResponse: (Int) -> List<FaqSummary> = { _ -> emptyList() }
    var faqDetailResponse: (Int, Int) -> FaqDetail = { _, _ -> FaqDetail(id = 0) }
    var faqsPopularResponse: () -> List<FaqPopularItem> = { emptyList() }
    var faqsLatestResponse: () -> List<FaqPopularItem> = { emptyList() }
    var faqsTrendingResponse: () -> List<FaqPopularItem> = { emptyList() }
    var faqsStickyResponse: () -> List<FaqPopularItem> = { emptyList() }
    var searchResponse: (String) -> List<SearchResult> = { _ -> emptyList() }
    var popularSearchesResponse: () -> List<PopularSearch> = { emptyList() }
    var tagsResponse: () -> List<Tag> = { emptyList() }
    var newsResponse: () -> List<NewsItem> = { emptyList() }
    var attachmentsResponse: (Int) -> List<Attachment> = { _ -> emptyList() }
    var commentsResponse: (Int) -> List<Comment> = { _ -> emptyList() }
    var glossaryResponse: () -> List<GlossaryItem> = { emptyList() }
    var openQuestionsResponse: () -> List<OpenQuestion> = { emptyList() }

    var metaCalls: Int = 0
        private set
    var faqsStickyCalls: Int = 0
        private set
    var faqsPopularCalls: Int = 0
        private set
    var faqDetailCalls: Int = 0
        private set
    var newsCalls: Int = 0
        private set
    var commentsCalls: Int = 0
        private set
    var searchCalls: Int = 0
        private set

    override suspend fun meta(): Meta {
        metaCalls += 1
        return metaResponse()
    }

    override suspend fun categories(): List<Category> = categoriesResponse()

    override suspend fun faqsByCategory(categoryId: Int): List<FaqSummary> = faqsByCategoryResponse(categoryId)

    override suspend fun faqDetail(
        categoryId: Int,
        faqId: Int,
    ): FaqDetail {
        faqDetailCalls += 1
        return faqDetailResponse(categoryId, faqId)
    }

    override suspend fun faqsPopular(): List<FaqPopularItem> {
        faqsPopularCalls += 1
        return faqsPopularResponse()
    }

    override suspend fun faqsLatest(): List<FaqPopularItem> = faqsLatestResponse()

    override suspend fun faqsTrending(): List<FaqPopularItem> = faqsTrendingResponse()

    override suspend fun faqsSticky(): List<FaqPopularItem> {
        faqsStickyCalls += 1
        return faqsStickyResponse()
    }

    override suspend fun search(query: String): List<SearchResult> {
        searchCalls += 1
        return searchResponse(query)
    }

    override suspend fun popularSearches(): List<PopularSearch> = popularSearchesResponse()

    override suspend fun tags(): List<Tag> = tagsResponse()

    override suspend fun news(): List<NewsItem> {
        newsCalls += 1
        return newsResponse()
    }

    override suspend fun attachments(faqId: Int): List<Attachment> = attachmentsResponse(faqId)

    override suspend fun comments(recordId: Int): List<Comment> {
        commentsCalls += 1
        return commentsResponse(recordId)
    }

    override suspend fun glossary(): List<GlossaryItem> = glossaryResponse()

    override suspend fun openQuestions(): List<OpenQuestion> = openQuestionsResponse()
}
