package app.myfaq.shared.data

import kotlinx.datetime.Clock

/**
 * TTL-based JSON cache backed by the `cache_entries` SQLDelight table.
 *
 * Phase 1 caches raw JSON strings keyed by (instanceId, cacheKey).
 * Phase 2 replaces this with typed content tables; this layer remains
 * for any endpoint that doesn't warrant a dedicated table.
 */
class CacheStore(
    private val db: MyFaqDatabase,
) {
    fun get(
        instanceId: String,
        key: String,
    ): String? {
        val entry =
            db.cacheEntriesQueries.selectByKey(instanceId, key).executeAsOneOrNull()
                ?: return null
        val now = Clock.System.now().epochSeconds
        if (entry.fetched_at + entry.ttl_seconds < now) {
            // Stale — don't delete yet; caller may want stale fallback
            return null
        }
        return entry.json_body
    }

    /**
     * Returns cached JSON even if expired (for stale-while-revalidate).
     */
    fun getStale(
        instanceId: String,
        key: String,
    ): String? =
        db.cacheEntriesQueries
            .selectByKey(instanceId, key)
            .executeAsOneOrNull()
            ?.json_body

    fun put(
        instanceId: String,
        key: String,
        json: String,
        ttlSeconds: Long,
    ) {
        val now = Clock.System.now().epochSeconds
        db.cacheEntriesQueries.upsert(instanceId, key, json, now, ttlSeconds)
    }

    fun clearInstance(instanceId: String) {
        db.cacheEntriesQueries.deleteByInstance(instanceId)
    }

    fun evictExpired() {
        val now = Clock.System.now().epochSeconds
        db.cacheEntriesQueries.deleteExpired(now)
    }
}

/**
 * TTL constants from mobile-app-plan.md.
 */
object CacheTtl {
    const val CATEGORIES: Long = 86_400 // 24h
    const val FAQS: Long = 21_600 // 6h
    const val NEWS: Long = 3_600 // 1h
    const val SEARCH: Long = 600 // 10min
    const val TAGS: Long = 86_400 // 24h
    const val COMMENTS: Long = 3_600 // 1h
}
