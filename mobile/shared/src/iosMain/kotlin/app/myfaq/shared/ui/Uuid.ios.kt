package app.myfaq.shared.ui

import platform.Foundation.NSUUID

internal actual fun generateUuid(): String = NSUUID().UUIDString()
