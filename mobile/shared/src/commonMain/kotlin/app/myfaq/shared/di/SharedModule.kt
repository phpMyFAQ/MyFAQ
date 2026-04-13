package app.myfaq.shared.di

import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.CacheStore
import app.myfaq.shared.data.DatabaseFactory
import app.myfaq.shared.data.MyFaqDatabase
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Shared Koin module. Phase 1 wires the real HTTP engines,
 * repository pattern, cache store, and active-instance manager.
 */
val sharedModule =
    module {
        single { DatabaseFactory(get(), get()) }
        single<MyFaqDatabase> { get<DatabaseFactory>().create() }
        single { CacheStore(get()) }
        single { ActiveInstanceManager(get(), get()) }
    }

/**
 * Exposed so iOS Swift code can resolve dependencies without
 * importing Koin types directly.
 */
lateinit var koinApp: org.koin.core.KoinApplication
    private set

/**
 * Hosts call this on startup with their platform module.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    koinApp =
        startKoin {
            appDeclaration()
            modules(sharedModule)
        }
}
