import Shared

/// Resolves Koin dependencies via the KoinIos helper.
/// Koin's inline reified `get<T>()` can't be called from Swift,
/// so we use explicit Kotlin-side resolvers instead.
enum KoinHelper {
    static var database: MyFaqDatabase {
        KoinIos.shared.database
    }

    static var activeInstanceManager: ActiveInstanceManager {
        KoinIos.shared.activeInstanceManager
    }
}
