package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color

object SteganographyUtils {


    // Result class to hold dual decoding results
    data class DecodeResult(
        val lsbMessage: String?,
        val dctMessage: String?
    )

    fun encodeMessage(bitmap: Bitmap, message: String, onProgress: ((Float) -> Unit)? = null): Bitmap? {
        // 1. First try DCT (Robust)
        var workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Notify start
        onProgress?.invoke(0f)
        
        // Try to encode with DCT. (Skipping detailed progress for DCT for now, assume fast or minimal impact on bar)
        val dctBitmap = DCTUtils.encodeMessage(workingBitmap, message)
        
        if (dctBitmap != null) {
             workingBitmap = dctBitmap 
        }
        
        // 2. Then apply LSB (Standard)
        // We'll map LSB progress to 0.1 -> 1.0 (assuming DCT took 10%)
        val finalBitmap = encodeLSB(workingBitmap, message) { progress ->
            onProgress?.invoke(0.1f + (progress * 0.9f))
        }
        
        onProgress?.invoke(1f)
        return finalBitmap
    }

    fun decodeMessage(bitmap: Bitmap, onProgress: ((Float) -> Unit)? = null): DecodeResult {
        onProgress?.invoke(0f)
        
        // Attempt to decode using both methods
        // LSB takes first 50%
        val lsbResult = decodeLSB(bitmap) { p ->
            onProgress?.invoke(p * 0.5f)
        }
        
        // DCT takes next 50%
        // (Assuming DCT decode is fast enough or we just jump to 100%)
        val dctResult = DCTUtils.decodeMessage(bitmap)
        
        onProgress?.invoke(1f)
        return DecodeResult(lsbResult, dctResult)
    }

    // --- Capability Checks ---
    fun getMaxLsbCapacity(width: Int, height: Int): Int {
        val totalPixels = width * height
        val maxBits = totalPixels * 3 // 3 channels (R, G, B)
        val maxChars = (maxBits / 8) - END_MESSAGE_CONSTANT.length
        return if (maxChars < 0) 0 else maxChars
    }

    // --- LSB Implementation (Private) ---

    private const val END_MESSAGE_CONSTANT = "$!@#END" 

    private fun encodeLSB(bitmap: Bitmap, message: String, onProgress: ((Float) -> Unit)? = null): Bitmap? {
        val fullMessage = message + END_MESSAGE_CONSTANT
        // Convert to UTF-8 bytes to support Emojis and special chars
        val messageBytes = fullMessage.toByteArray(Charsets.UTF_8)
        
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val width = mutableBitmap.width
        val height = mutableBitmap.height
        val totalPixels = width * height
        
        // Check capacity (bits needed vs available channels)
        if (messageBytes.size * 8 > totalPixels * 3) {
            return null // Message too long for LSB
        }

        var byteIndex = 0
        var bitIndex = 0
        var currentByteVal = messageBytes[0].toInt() and 0xFF

        for (y in 0 until height) {
            // Report progress per row
            onProgress?.invoke(y.toFloat() / height)
            
            for (x in 0 until width) {
                if (byteIndex >= messageBytes.size) {
                    return mutableBitmap
                }

                var pixel = mutableBitmap.getPixel(x, y)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)

                // Embed in Red
                if (byteIndex < messageBytes.size) {
                    r = embedBit(r, getBit(currentByteVal, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        byteIndex++
                        if (byteIndex < messageBytes.size) {
                            currentByteVal = messageBytes[byteIndex].toInt() and 0xFF
                        }
                    }
                }

                // Embed in Green
                if (byteIndex < messageBytes.size) {
                    g = embedBit(g, getBit(currentByteVal, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        byteIndex++
                        if (byteIndex < messageBytes.size) {
                            currentByteVal = messageBytes[byteIndex].toInt() and 0xFF
                        }
                    }
                }

                // Embed in Blue
                if (byteIndex < messageBytes.size) {
                    b = embedBit(b, getBit(currentByteVal, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        byteIndex++
                        if (byteIndex < messageBytes.size) {
                            currentByteVal = messageBytes[byteIndex].toInt() and 0xFF
                        }
                    }
                }

                mutableBitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return mutableBitmap
    }

    private fun decodeLSB(bitmap: Bitmap, onProgress: ((Float) -> Unit)? = null): String? {
        val width = bitmap.width
        val height = bitmap.height
        
        // Use ByteArrayOutputStream to collect raw bytes
        val baos = java.io.ByteArrayOutputStream()
        val endBytes = END_MESSAGE_CONSTANT.toByteArray(Charsets.UTF_8)
        val endLen = endBytes.size
        
        var currentByte = 0
        var bitIndex = 0

        for (y in 0 until height) {
            // Report progress per row
            onProgress?.invoke(y.toFloat() / height)
            
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Extract from Red
                currentByte = (currentByte shl 1) or (r and 1)
                bitIndex++
                if (bitIndex == 8) {
                    baos.write(currentByte)
                    if (checkEnd(baos, endBytes)) {
                        val allBytes = baos.toByteArray()
                        return String(allBytes, 0, allBytes.size - endLen, Charsets.UTF_8)
                    }
                    currentByte = 0
                    bitIndex = 0
                }

                // Extract from Green
                currentByte = (currentByte shl 1) or (g and 1)
                bitIndex++
                if (bitIndex == 8) {
                    baos.write(currentByte)
                    if (checkEnd(baos, endBytes)) {
                        val allBytes = baos.toByteArray()
                        return String(allBytes, 0, allBytes.size - endLen, Charsets.UTF_8)
                    }
                    currentByte = 0
                    bitIndex = 0
                }

                // Extract from Blue
                currentByte = (currentByte shl 1) or (b and 1)
                bitIndex++
                if (bitIndex == 8) {
                    baos.write(currentByte)
                    if (checkEnd(baos, endBytes)) {
                        val allBytes = baos.toByteArray()
                        return String(allBytes, 0, allBytes.size - endLen, Charsets.UTF_8)
                    }
                    currentByte = 0
                    bitIndex = 0
                }
            }
        }
        return null 
    }
    
    private fun checkEnd(baos: java.io.ByteArrayOutputStream, endBytes: ByteArray): Boolean {
        val size = baos.size()
        if (size < endBytes.size) return false
        
        val allBytes = baos.toByteArray() // Inefficient copying every byte? 
        // Optimized: Just check backing buffer via toByteArray? 
        // For now, this is safer. Optimizations can be done if slow.
        
        // Check last N bytes
        val start = size - endBytes.size
        for (i in endBytes.indices) {
            if (allBytes[start + i] != endBytes[i]) return false
        }
        return true
    }

    // --- LSB Implementation (Private) ---



    private fun getBit(value: Int, position: Int): Int {
        return (value shr position) and 1
    }

    private fun embedBit(value: Int, bit: Int): Int {
        return (value and 0xFE) or bit
    }
}
