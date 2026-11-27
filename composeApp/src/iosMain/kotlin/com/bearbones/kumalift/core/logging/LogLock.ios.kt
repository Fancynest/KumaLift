package com.bearbones.kumalift.core.logging

import platform.Foundation.NSRecursiveLock

internal actual fun createDiagnosticLogLock(): Any = NSRecursiveLock()

internal actual fun <T> withDiagnosticLogLock(lock: Any, block: () -> T): T {
    val nsLock = lock as NSRecursiveLock
    nsLock.lock()
    return try {
        block()
    } finally {
        nsLock.unlock()
    }
}
