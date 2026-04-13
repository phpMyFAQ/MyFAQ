import Shared

/// Swift-side mirror of the KMP UiState sealed interface.
/// Handles the ObjC interop type erasure.
enum UiStateWrapper<T> {
    case loading
    case success(T)
    case error(String)
}

/// Converts a KMP UiState (as Any?) to the Swift enum.
/// The caller must cast data from `UiStateSuccess.data` to the correct Swift type.
func unwrapUiState<T>(_ raw: Any?, cast: (Any?) -> T?) -> UiStateWrapper<T> {
    if raw is UiStateLoading {
        return .loading
    }
    if let success = raw as? UiStateSuccess<AnyObject> {
        if let data = cast(success.data) {
            return .success(data)
        }
        return .error("Unexpected data type")
    }
    if let err = raw as? UiStateError {
        return .error(err.message)
    }
    return .loading
}

/// Convenience for lists: casts NSArray to [T].
func castList<T>(_ value: Any?) -> [T]? {
    value as? [T]
}
