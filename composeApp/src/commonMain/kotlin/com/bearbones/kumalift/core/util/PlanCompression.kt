package com.bearbones.kumalift.core.util

expect fun gzipCompress(bytes: ByteArray): ByteArray?
expect fun gzipDecompress(bytes: ByteArray): ByteArray?
