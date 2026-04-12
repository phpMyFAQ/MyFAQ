package app.myfaq.shared.data

import app.myfaq.shared.platform.DatabaseDriverFactory
import app.myfaq.shared.platform.SecureStore

private const val PASSPHRASE_KEY = "db.passphrase"
private const val PASSPHRASE_LENGTH = 32

/**
 * Lazily creates the encrypted [MyFaqDatabase] for the current install.
 *
 * The passphrase is generated on first launch, stored in
 * [SecureStore], and reused forever after. Dropping the app or
 * clearing storage wipes both the DB file and the passphrase
 * entry — there is no recovery path.
 */
class DatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
    private val secureStore: SecureStore,
    private val random: () -> ByteArray = ::defaultRandomBytes,
) {
    fun create(): MyFaqDatabase {
        val passphrase = loadOrCreatePassphrase()
        val driver = driverFactory.create(passphrase)
        return MyFaqDatabase(driver)
    }

    private fun loadOrCreatePassphrase(): ByteArray {
        val existing = secureStore.get(PASSPHRASE_KEY)
        if (existing != null) return existing.hexToByteArray()
        val fresh = random()
        secureStore.put(PASSPHRASE_KEY, fresh.toHexString())
        return fresh
    }
}

private fun defaultRandomBytes(): ByteArray =
    ByteArray(PASSPHRASE_LENGTH).also { bytes ->
        // kotlin.random.Random is not cryptographically secure on every
        // platform; platform code should override this via the random
        // lambda when wiring production DI. Phase 0 uses it so the
        // scaffolding compiles without adding a secure-random dep here.
        kotlin.random.Random.nextBytes(bytes)
    }

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { b -> ((b.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

private fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "invalid hex length" }
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
