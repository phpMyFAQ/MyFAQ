package app.myfaq.shared.data

/**
 * Test-only in-memory implementation of [CacheStore]. Lets repository
 * tests run in `commonTest` without a SQLDelight driver.
 *
 * The clock is injectable so tests can advance time deterministically
 * to exercise TTL expiration without `Thread.sleep`.
 */
class InMemoryCacheStore(
    private val now: () -> Long = { 0L },
) : CacheStore {
    private data class Entry(
        val json: String,
        val fetchedAt: Long,
        val ttlSeconds: Long,
    )

    private val entries = mutableMapOf<Pair<String, String>, Entry>()

    /** Number of put() calls made — handy for assertions. */
    var putCount: Int = 0
        private set

    override fun get(
        instanceId: String,
        key: String,
    ): String? {
        val entry = entries[instanceId to key] ?: return null
        if (entry.fetchedAt + entry.ttlSeconds < now()) return null
        return entry.json
    }

    override fun getStale(
        instanceId: String,
        key: String,
    ): String? = entries[instanceId to key]?.json

    override fun put(
        instanceId: String,
        key: String,
        json: String,
        ttlSeconds: Long,
    ) {
        entries[instanceId to key] = Entry(json, now(), ttlSeconds)
        putCount += 1
    }

    override fun clearInstance(instanceId: String) {
        entries.keys.removeAll { it.first == instanceId }
    }

    override fun evictExpired() {
        val cutoff = now()
        entries.entries.removeAll { it.value.fetchedAt + it.value.ttlSeconds < cutoff }
    }
}
