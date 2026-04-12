package app.myfaq.shared.platform

/**
 * Per-platform encrypted key/value store.
 *
 * - Android: `androidx.security.crypto.EncryptedSharedPreferences`
 *   backed by an AES master key in the Android Keystore.
 * - iOS: Keychain Services with
 *   `kSecAttrAccessibleAfterFirstUnlock`.
 *
 * Every instance UUID namespaces its own secrets via the key prefix
 * the caller supplies. Nothing written here ever touches SQLite.
 */
expect class SecureStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun clear()
}
