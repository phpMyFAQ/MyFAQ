package app.myfaq.shared.ui

import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.MyFaqDatabase
import app.myfaq.shared.di.koinApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Creates a default CoroutineScope for use from iOS.
 * Kotlin default parameter values don't bridge to ObjC/Swift,
 * so iOS callers need this factory.
 */
fun createDefaultScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Koin's inline reified `get<T>()` can't be called from ObjC/Swift.
 * These explicit resolvers expose each dependency to iOS.
 */
object KoinIos {
    val database: MyFaqDatabase
        get() = koinApp.koin.get()

    val activeInstanceManager: ActiveInstanceManager
        get() = koinApp.koin.get()
}
