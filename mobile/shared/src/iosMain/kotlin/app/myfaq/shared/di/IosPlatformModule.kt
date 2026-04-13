package app.myfaq.shared.di

import app.myfaq.shared.platform.DatabaseDriverFactory
import app.myfaq.shared.platform.SecureStore
import org.koin.dsl.module

/**
 * iOS platform module providing [SecureStore] and [DatabaseDriverFactory].
 * Registered automatically via [initKoinIos].
 */
val iosPlatformModule = module {
    single { SecureStore() }
    single { DatabaseDriverFactory() }
}

/**
 * iOS-specific Koin initialization that includes the platform module.
 * Called from Swift AppDelegate/App init.
 */
fun initKoinIos() {
    initKoin {
        modules(iosPlatformModule)
    }
}
