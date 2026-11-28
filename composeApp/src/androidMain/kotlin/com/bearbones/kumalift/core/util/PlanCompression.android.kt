package com.bearbones.kumalift.core.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun gzipCompress(bytes: ByteArray): ByteArray? = try {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).use { it.write(bytes) }
    bos.toByteArray()
} catch (_: Exception) {
    null
}

actual fun gzipDecompress(bytes: ByteArray): ByteArray? = try {
    GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > 2 * 1024 * 1024) return null
            out.write(buf, 0, n)
        }
        out.toByteArray()
    }
} catch (_: Exception) {
    null
}
