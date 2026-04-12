package app.myfaq.shared.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSMutableDictionary
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.NSObject

/**
 * iOS [SecureStore] backed by Keychain Services with
 * `kSecAttrAccessibleAfterFirstUnlock` so values survive reboots
 * but are unavailable until the user unlocks the device once.
 *
 * Service: `app.myfaq.ios`. Callers namespace their keys by
 * instance UUID. `clear()` enumerates by service tag so a user-
 * initiated "wipe" removes every per-instance secret atomically.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureStore {

    actual fun put(key: String, value: String) {
        remove(key)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return
        val add = baseQuery(key).apply {
            setObject(data, kSecValueData as NSObject)
            setObject(kSecAttrAccessibleAfterFirstUnlock as NSObject, kSecAttrAccessible as NSObject)
        }
        SecItemAdd(add, null)
    }

    actual fun get(key: String): String? {
        val query = baseQuery(key).apply {
            setObject(kSecMatchLimitOne as NSObject, kSecMatchLimit as NSObject)
            setObject(true as NSObject, kSecReturnData as NSObject)
        }
        val result = kotlinx.cinterop.memScoped {
            val ref = kotlinx.cinterop.alloc<kotlinx.cinterop.CPointerVar<platform.CoreFoundation.__CFType>>()
            val status = SecItemCopyMatching(query, ref.ptr)
            if (status != 0) return@memScoped null
            val data = ref.value ?: return@memScoped null
            @Suppress("UNCHECKED_CAST")
            (data as? NSData)
        } ?: return null
        return NSString.create(result, NSUTF8StringEncoding) as String?
    }

    actual fun remove(key: String) {
        SecItemDelete(baseQuery(key))
    }

    actual fun clear() {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword as NSObject, kSecClass as NSObject)
            setObject(SERVICE as NSObject, kSecAttrService as NSObject)
        }
        SecItemDelete(query)
    }

    private fun baseQuery(account: String): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword as NSObject, kSecClass as NSObject)
        setObject(SERVICE as NSObject, kSecAttrService as NSObject)
        setObject(account as NSObject, kSecAttrAccount as NSObject)
    }

    private companion object {
        const val SERVICE = "app.myfaq.ios"
    }
}
