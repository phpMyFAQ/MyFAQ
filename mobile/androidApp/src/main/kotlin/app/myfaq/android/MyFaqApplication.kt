package app.myfaq.android

import android.app.Application
import app.myfaq.shared.di.initKoin
import app.myfaq.shared.platform.DatabaseDriverFactory
import app.myfaq.shared.platform.SecureStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class MyFaqApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val androidPlatformModule =
            module {
                single { SecureStore(androidContext()) }
                single { DatabaseDriverFactory(androidContext()) }
            }

        initKoin {
            androidContext(this@MyFaqApplication)
            modules(androidPlatformModule)
        }
    }
}
