package app.myfaq.shared.platform

import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Platform-agnostic assertions every [SecureStore] implementation
 * must satisfy. Android and iOS test source sets each instantiate a
 * real platform store and drive it through this contract.
 */
object SecureStoreContract {

    fun assertRoundTrip(store: SecureStore) {
        val key = "contract.roundtrip"
        store.remove(key)
        assertNull(store.get(key))

        store.put(key, "hello-secret")
        assertEquals("hello-secret", store.get(key))

        store.put(key, "updated")
        assertEquals("updated", store.get(key))

        store.remove(key)
        assertNull(store.get(key))
    }

    fun assertClearWipesEverything(store: SecureStore) {
        store.put("contract.a", "1")
        store.put("contract.b", "2")
        store.clear()
        assertNull(store.get("contract.a"))
        assertNull(store.get("contract.b"))
    }
}
