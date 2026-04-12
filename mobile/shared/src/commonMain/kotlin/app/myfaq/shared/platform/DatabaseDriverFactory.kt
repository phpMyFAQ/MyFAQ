package app.myfaq.shared.platform

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates a platform-specific SQLDelight driver for `myfaq.db`.
 *
 * - Android: SQLCipher-backed driver. The [passphrase] is derived
 *   from a hardware-backed key in the Android Keystore and never
 *   leaves [SecureStore].
 * - iOS: `NativeSqliteDriver` with file-level encryption delegated
 *   to Data Protection Class B
 *   (`NSFileProtectionCompleteUntilFirstUserAuthentication`). The
 *   [passphrase] is ignored on iOS in Phase 0 — it is accepted to
 *   keep the `expect`/`actual` signatures identical across
 *   platforms.
 */
expect class DatabaseDriverFactory {
    fun create(passphrase: ByteArray): SqlDriver
}
