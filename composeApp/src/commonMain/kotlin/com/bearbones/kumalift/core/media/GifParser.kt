package com.bearbones.kumalift.core.media

/**
 * Pure Kotlin GIF89a / GIF87a parser and LZW decompressor.
 * Extracts individual frames, per-frame delays, and computes cumulative durations.
 */
object GifParser {

    fun parse(bytes: ByteArray, assetPath: String = ""): DecodedGif {
        var pos = 0

        fun hasBytes(count: Int) = pos + count <= bytes.size

        fun readByte(): Int {
            if (pos >= bytes.size) return -1
            return bytes[pos++].toInt() and 0xFF
        }

        fun readShort(): Int {
            val b1 = readByte()
            val b2 = readByte()
            if (b1 == -1 || b2 == -1) return 0
            return b1 or (b2 shl 8)
        }

        fun skip(count: Int) {
            pos = (pos + count).coerceAtMost(bytes.size)
        }

        fun skipSubBlocks() {
            while (pos < bytes.size) {
                val len = readByte()
                if (len <= 0) break
                skip(len)
            }
        }

        // 1. Read Header (6 bytes: "GIF87a" or "GIF89a")
        if (!hasBytes(6)) throw IllegalArgumentException("Invalid GIF: too short")
        val header = ByteArray(6)
        for (i in 0 until 6) header[i] = bytes[pos++]
        val headerStr = header.decodeToString()
        if (!headerStr.startsWith("GIF")) {
            throw IllegalArgumentException("Not a valid GIF file: $headerStr")
        }

        // 2. Logical Screen Descriptor (7 bytes)
        val screenWidth = readShort()
        val screenHeight = readShort()
        val packed = readByte()
        val gctFlag = (packed and 0x80) != 0
        val gctSize = if (gctFlag) 2 shl (packed and 0x07) else 0
        val bgColorIndex = readByte()
        val pixelAspect = readByte()

        // 3. Global Color Table
        val globalColorTable = if (gctFlag && gctSize > 0) {
            val table = IntArray(gctSize)
            for (i in 0 until gctSize) {
                val r = readByte()
                val g = readByte()
                val b = readByte()
                table[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            table
        } else {
            IntArray(0)
        }

        val frames = mutableListOf<GifFrame>()
        var cumulativeTimeMs = 0

        // Graphic Control Extension state
        var disposalMethod = 0
        var delayMs = 100
        var hasTransparency = false
        var transparentColorIndex = -1

        var currentCanvas = IntArray(screenWidth * screenHeight) { 0xFFFFFFFF.toInt() } // default opaque white
        var previousCanvas: IntArray? = null

        while (pos < bytes.size) {
            val block = readByte()
            if (block == -1 || block == 0x3B) { // EOF or Trailer
                break
            }

            when (block) {
                0x21 -> { // Extension Block
                    val extLabel = readByte()
                    if (extLabel == 0xF9) { // Graphic Control Extension
                        val blockSize = readByte() // Expected 4
                        val gcePacked = readByte()
                        disposalMethod = (gcePacked shr 2) and 0x07
                        hasTransparency = (gcePacked and 0x01) != 0

                        val rawDelay = readShort() // in 1/100s
                        var delay = rawDelay * 10
                        if (delay <= 10) {
                            delay = 100 // Fallback rule for delay 0 or too small (Requirement A)
                        }
                        delayMs = delay
                        transparentColorIndex = readByte()
                        readByte() // Block Terminator (0x00)
                    } else {
                        skipSubBlocks()
                    }
                }
                0x2C -> { // Image Descriptor
                    val ix = readShort()
                    val iy = readShort()
                    val iw = readShort()
                    val ih = readShort()
                    val imgPacked = readByte()
                    val hasLct = (imgPacked and 0x80) != 0
                    val interlace = (imgPacked and 0x40) != 0
                    val lctSize = if (hasLct) 2 shl (imgPacked and 0x07) else 0

                    val colorTable = if (hasLct && lctSize > 0) {
                        val table = IntArray(lctSize)
                        for (i in 0 until lctSize) {
                            val r = readByte()
                            val g = readByte()
                            val b = readByte()
                            table[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        }
                        table
                    } else {
                        globalColorTable
                    }

                    val lzwMinCodeSize = readByte()

                    // Gather sub-blocks
                    val compressedBaos = mutableListOf<Byte>()
                    while (pos < bytes.size) {
                        val len = readByte()
                        if (len <= 0) break
                        for (i in 0 until len) {
                            if (pos < bytes.size) {
                                compressedBaos.add(bytes[pos++])
                            }
                        }
                    }
                    val compressedData = compressedBaos.toByteArray()
                    val pixelIndices = decompressLzw(compressedData, iw * ih, lzwMinCodeSize)

                    if (disposalMethod == 3 && previousCanvas == null) {
                        previousCanvas = currentCanvas.copyOf()
                    }

                    val nextCanvas = currentCanvas.copyOf()
                    renderImageToCanvas(
                        canvas = nextCanvas,
                        canvasWidth = screenWidth,
                        canvasHeight = screenHeight,
                        ix = ix,
                        iy = iy,
                        iw = iw,
                        ih = ih,
                        interlace = interlace,
                        pixelIndices = pixelIndices,
                        colorTable = colorTable,
                        hasTransparency = hasTransparency,
                        transparentIndex = transparentColorIndex
                    )

                    frames.add(
                        GifFrame(
                            pixels = nextCanvas.copyOf(),
                            width = screenWidth,
                            height = screenHeight,
                            delayMs = delayMs,
                            cumulativeTimeMs = cumulativeTimeMs
                        )
                    )
                    cumulativeTimeMs += delayMs

                    when (disposalMethod) {
                        2 -> { // Restore to background
                            currentCanvas = IntArray(screenWidth * screenHeight) { 0xFFFFFFFF.toInt() }
                        }
                        3 -> { // Restore to previous
                            if (previousCanvas != null) {
                                currentCanvas = previousCanvas.copyOf()
                            }
                        }
                        else -> { // Disposal 0 or 1: Keep current
                            currentCanvas = nextCanvas
                            previousCanvas = null
                        }
                    }

                    // Reset GCE state
                    disposalMethod = 0
                    delayMs = 100
                    hasTransparency = false
                    transparentColorIndex = -1
                }
                else -> {
                    skipSubBlocks()
                }
            }
        }

        return DecodedGif(
            assetPath = assetPath,
            width = screenWidth,
            height = screenHeight,
            frames = frames,
            totalDurationMs = cumulativeTimeMs
        )
    }

    private fun decompressLzw(compressedData: ByteArray, pixelCount: Int, minCodeSize: Int): ByteArray {
        val output = ByteArray(pixelCount)
        if (compressedData.isEmpty() || pixelCount == 0) return output

        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1
        val maxTableSize = 4096

        val prefix = IntArray(maxTableSize)
        val suffix = ByteArray(maxTableSize)
        val pixelStack = ByteArray(maxTableSize + 1)

        var codeSize = minCodeSize + 1
        var codeMask = (1 shl codeSize) - 1
        var available = clearCode + 2
        var oldCode = -1
        var firstChar = 0

        for (i in 0 until clearCode) {
            prefix[i] = -1
            suffix[i] = i.toByte()
        }

        var bitPos = 0
        var outPos = 0
        var top = 0

        fun readCode(): Int {
            val bytePos = bitPos shr 3
            if (bytePos >= compressedData.size) return eoiCode

            var buffer = (compressedData[bytePos].toInt() and 0xFF)
            if (bytePos + 1 < compressedData.size) {
                buffer = buffer or ((compressedData[bytePos + 1].toInt() and 0xFF) shl 8)
            }
            if (bytePos + 2 < compressedData.size) {
                buffer = buffer or ((compressedData[bytePos + 2].toInt() and 0xFF) shl 16)
            }

            val code = (buffer ushr (bitPos and 7)) and codeMask
            bitPos += codeSize
            return code
        }

        while (outPos < pixelCount) {
            val code = readCode()
            if (code == eoiCode) break
            if (code == clearCode) {
                codeSize = minCodeSize + 1
                codeMask = (1 shl codeSize) - 1
                available = clearCode + 2
                oldCode = -1
                continue
            }

            if (oldCode == -1) {
                if (code < available) {
                    output[outPos++] = suffix[code]
                    oldCode = code
                    firstChar = (suffix[code].toInt() and 0xFF)
                }
                continue
            }

            var inCode = code
            if (code >= available) {
                pixelStack[top++] = firstChar.toByte()
                inCode = oldCode
            }

            while (inCode >= clearCode && inCode < maxTableSize) {
                pixelStack[top++] = suffix[inCode]
                inCode = prefix[inCode]
            }
            firstChar = (suffix[inCode].toInt() and 0xFF)
            pixelStack[top++] = firstChar.toByte()

            if (available < maxTableSize) {
                prefix[available] = oldCode
                suffix[available] = firstChar.toByte()
                available++
                if (available == (1 shl codeSize) && codeSize < 12) {
                    codeSize++
                    codeMask = (1 shl codeSize) - 1
                }
            }
            oldCode = code

            while (top > 0 && outPos < pixelCount) {
                output[outPos++] = pixelStack[--top]
            }
        }

        return output
    }

    private fun renderImageToCanvas(
        canvas: IntArray,
        canvasWidth: Int,
        canvasHeight: Int,
        ix: Int,
        iy: Int,
        iw: Int,
        ih: Int,
        interlace: Boolean,
        pixelIndices: ByteArray,
        colorTable: IntArray,
        hasTransparency: Boolean,
        transparentIndex: Int
    ) {
        var srcIndex = 0

        val passSteps = if (interlace) intArrayOf(8, 8, 4, 2) else intArrayOf(1)
        val passStarts = if (interlace) intArrayOf(0, 4, 2, 1) else intArrayOf(0)

        for (pass in passSteps.indices) {
            val step = passSteps[pass]
            val start = passStarts[pass]
            var y = start
            while (y < ih) {
                val targetY = iy + y
                if (targetY in 0 until canvasHeight) {
                    val rowOffset = targetY * canvasWidth
                    for (x in 0 until iw) {
                        if (srcIndex >= pixelIndices.size) break
                        val targetX = ix + x
                        if (targetX in 0 until canvasWidth) {
                            val colorIdx = pixelIndices[srcIndex].toInt() and 0xFF
                            if (!hasTransparency || colorIdx != transparentIndex) {
                                if (colorIdx < colorTable.size) {
                                    canvas[rowOffset + targetX] = colorTable[colorIdx]
                                }
                            }
                        }
                        srcIndex++
                    }
                } else {
                    srcIndex += iw
                }
                y += step
            }
        }
    }
}
