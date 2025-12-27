package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color

object SteganographyUtils {

    private const val END_MESSAGE_CONSTANT = "$!@#END" 

    fun encodeMessage(bitmap: Bitmap, message: String): Bitmap? {
        // LSB implementation directly
        val fullMessage = message + END_MESSAGE_CONSTANT
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val width = mutableBitmap.width
        val height = mutableBitmap.height
        val totalPixels = width * height
        
        if (fullMessage.length * 8 > totalPixels * 3) {
            return null // Message too long
        }

        var messageIndex = 0
        var bitIndex = 0
        var currentAscii = fullMessage[0].code

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (messageIndex >= fullMessage.length) {
                    return mutableBitmap
                }

                var pixel = mutableBitmap.getPixel(x, y)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)

                // Embed in Red
                if (messageIndex < fullMessage.length) {
                    r = embedBit(r, getBit(currentAscii, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        messageIndex++
                        if (messageIndex < fullMessage.length) {
                            currentAscii = fullMessage[messageIndex].code
                        }
                    }
                }

                // Embed in Green
                if (messageIndex < fullMessage.length) {
                    g = embedBit(g, getBit(currentAscii, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        messageIndex++
                        if (messageIndex < fullMessage.length) {
                            currentAscii = fullMessage[messageIndex].code
                        }
                    }
                }

                // Embed in Blue
                if (messageIndex < fullMessage.length) {
                    b = embedBit(b, getBit(currentAscii, 7 - bitIndex))
                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        messageIndex++
                        if (messageIndex < fullMessage.length) {
                            currentAscii = fullMessage[messageIndex].code
                        }
                    }
                }

                mutableBitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return mutableBitmap
    }

    fun decodeMessage(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        
        val sb = StringBuilder()
        var currentByte = 0
        var bitIndex = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Extract from Red
                currentByte = (currentByte shl 1) or (r and 1)
                bitIndex++
                if (bitIndex == 8) {
                    sb.append(currentByte.toChar())
                    if (sb.endsWith(END_MESSAGE_CONSTANT)) {
                        return sb.substring(0, sb.length - END_MESSAGE_CONSTANT.length)
                    }
                    currentByte = 0
                    bitIndex = 0
                }

                // Extract from Green
                currentByte = (currentByte shl 1) or (g and 1)
                bitIndex++
                if (bitIndex == 8) {
                    sb.append(currentByte.toChar())
                    if (sb.endsWith(END_MESSAGE_CONSTANT)) {
                        return sb.substring(0, sb.length - END_MESSAGE_CONSTANT.length)
                    }
                    currentByte = 0
                    bitIndex = 0
                }

                // Extract from Blue
                currentByte = (currentByte shl 1) or (b and 1)
                bitIndex++
                if (bitIndex == 8) {
                    sb.append(currentByte.toChar())
                    if (sb.endsWith(END_MESSAGE_CONSTANT)) {
                        return sb.substring(0, sb.length - END_MESSAGE_CONSTANT.length)
                    }
                    currentByte = 0
                    bitIndex = 0
                }
            }
        }
        return null 
    }

    // --- LSB Implementation (Private) ---



    private fun getBit(value: Int, position: Int): Int {
        return (value shr position) and 1
    }

    private fun embedBit(value: Int, bit: Int): Int {
        return (value and 0xFE) or bit
    }
}
