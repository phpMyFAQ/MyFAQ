package app.myfaq.shared.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.myfaq.shared.data.MyFaqDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * SQLCipher-backed driver. On first call, [System.loadLibrary]
 * bootstraps the native `sqlcipher` library shipped with
 * `net.zetetic:sqlcipher-android`. The passphrase is whatever the
 * caller supplies — never derived in place.
 */
actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun create(passphrase: ByteArray): SqlDriver {
        ensureSqlCipherLoaded()
        val helperFactory = SupportOpenHelperFactory(passphrase)
        return AndroidSqliteDriver(
            schema = MyFaqDatabase.Schema.synchronous(),
            context = context.applicationContext,
            name = DB_NAME,
            factory = helperFactory,
        )
    }

    private fun ensureSqlCipherLoaded() {
        if (sqlCipherLoaded) return
        synchronized(DatabaseDriverFactory::class) {
            if (!sqlCipherLoaded) {
                System.loadLibrary("sqlcipher")
                sqlCipherLoaded = true
            }
        }
    }

    private companion object {
        const val DB_NAME = "myfaq.db"

        @Volatile
        var sqlCipherLoaded: Boolean = false
    }
}

/**
 * SQLDelight 2.x exposes schemas as `SqlSchema<QueryResult.AsyncValue<Unit>>`
 * for KMP compatibility. The Android driver expects the synchronous
 * variant, so we unwrap it here.
 */
private fun app.cash.sqldelight.db.SqlSchema<app.cash.sqldelight.db.QueryResult.Value<Unit>>.synchronous() = this
