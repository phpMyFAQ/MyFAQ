package app.myfaq.shared.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.myfaq.shared.data.MyFaqDatabase

/**
 * iOS SQLDelight driver. Phase 0 relies on Data Protection Class B
 * (`NSFileProtectionCompleteUntilFirstUserAuthentication`) rather
 * than SQLCipher — see `plans/phase-0-foundations.md` §"Database
 * layer → Encryption".
 *
 * The [passphrase] parameter is accepted so the expect/actual
 * signatures line up with the Android driver, but it is ignored on
 * iOS in Phase 0. Phase 1 may revisit if Data Protection proves
 * insufficient.
 */
actual class DatabaseDriverFactory {
    actual fun create(passphrase: ByteArray): SqlDriver {
        @Suppress("UNUSED_PARAMETER")
        return NativeSqliteDriver(
            schema = MyFaqDatabase.Schema,
            name = DB_NAME,
        )
    }

    private companion object {
        const val DB_NAME = "myfaq.db"
    }
}
