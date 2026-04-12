package app.myfaq.shared.di

import app.myfaq.shared.api.HttpClientFactory
import app.myfaq.shared.api.MetaLoader
import app.myfaq.shared.api.MyFaqApi
import app.myfaq.shared.api.MyFaqApiImpl
import app.myfaq.shared.data.DatabaseFactory
import app.myfaq.shared.data.MyFaqDatabase
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Shared Koin module. Phase 0 default wiring points the API client
 * at the in-process [app.myfaq.shared.api.HttpClientFactory.phase0MockClient]
 * so both host apps can render the `/meta` payload without touching
 * the network.
 */
val sharedModule = module {
    single { HttpClientFactory.phase0MockClient() }
    single<MyFaqApi> { MyFaqApiImpl(get(), baseUrl = "https://demo.myfaq.app") }
    factory { MetaLoader(get()) }
    single { DatabaseFactory(get(), get()) }
    single<MyFaqDatabase> { get<DatabaseFactory>().create() }
}

/**
 * Exposed so iOS Swift code can resolve dependencies without
 * importing Koin types directly.
 */
lateinit var koinApp: org.koin.core.KoinApplication
    private set

/**
 * Hosts call this on startup with their platform module (Android
 * provides a `Context`-bound `SecureStore` and
 * `DatabaseDriverFactory`; iOS provides Keychain + NativeSqlite
 * implementations).
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    koinApp = startKoin {
        appDeclaration()
        modules(sharedModule)
    }
}
