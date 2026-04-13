package app.myfaq.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
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

/**
 * iOS [SecureStore] backed by Keychain Services with
 * `kSecAttrAccessibleAfterFirstUnlock` so values survive reboots
 * but are unavailable until the user unlocks the device once.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SecureStore {
    actual fun put(
        key: String,
        value: String,
    ) {
        remove(key)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query =
            cfMutableDict(6).apply {
                cfSet(kSecClass, kSecClassGenericPassword)
                cfSet(kSecAttrService, CFBridgingRetain(SERVICE))
                cfSet(kSecAttrAccount, CFBridgingRetain(key))
                cfSet(kSecValueData, CFBridgingRetain(data))
                cfSet(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
            }
        SecItemAdd(query as CFDictionaryRef, null)
    }

    actual fun get(key: String): String? {
        val query =
            cfMutableDict(5).apply {
                cfSet(kSecClass, kSecClassGenericPassword)
                cfSet(kSecAttrService, CFBridgingRetain(SERVICE))
                cfSet(kSecAttrAccount, CFBridgingRetain(key))
                cfSet(kSecMatchLimit, kSecMatchLimitOne)
                cfSet(kSecReturnData, CFBridgingRetain(true))
            }

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status != errSecSuccess) return null
            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            return NSString.create(data, NSUTF8StringEncoding) as? String
        }
    }

    actual fun remove(key: String) {
        val query =
            cfMutableDict(3).apply {
                cfSet(kSecClass, kSecClassGenericPassword)
                cfSet(kSecAttrService, CFBridgingRetain(SERVICE))
                cfSet(kSecAttrAccount, CFBridgingRetain(key))
            }
        SecItemDelete(query as CFDictionaryRef)
    }

    actual fun clear() {
        val query =
            cfMutableDict(2).apply {
                cfSet(kSecClass, kSecClassGenericPassword)
                cfSet(kSecAttrService, CFBridgingRetain(SERVICE))
            }
        SecItemDelete(query as CFDictionaryRef)
    }

    private fun cfMutableDict(capacity: Int): CFMutableDictionaryRef =
        CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            capacity.toLong(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!

    private fun CFMutableDictionaryRef.cfSet(
        key: Any?,
        value: Any?,
    ) {
        CFDictionarySetValue(this, CFBridgingRetain(key), CFBridgingRetain(value))
    }

    private companion object {
        const val SERVICE = "app.myfaq.ios"
    }
}
