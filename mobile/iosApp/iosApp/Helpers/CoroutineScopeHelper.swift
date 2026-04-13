import Shared

/// Creates a default CoroutineScope for KMP ViewModels called from iOS.
/// Kotlin default params don't bridge to ObjC, so we provide one explicitly.
func defaultScope() -> Kotlinx_coroutines_coreCoroutineScope {
    IosHelpersKt.createDefaultScope()
}
