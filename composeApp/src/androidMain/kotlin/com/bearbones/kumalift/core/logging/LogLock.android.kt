package com.bearbones.kumalift.core.logging

internal actual fun createDiagnosticLogLock(): Any = Any()

internal actual fun <T> withDiagnosticLogLock(lock: Any, block: () -> T): T =
    synchronized(lock) { block() }
