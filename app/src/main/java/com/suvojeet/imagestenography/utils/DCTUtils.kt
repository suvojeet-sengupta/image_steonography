package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DCTUtils {
    // 8x8 block size
    private const val N = 8
    
    // Standard JPEG Quantization Matrix (Luminance) - simplified for steganography
    // We only need to preserve low-mid frequencies well.
    private val QUANTIZATION_TABLE = arrayOf(
        intArrayOf(16, 11, 10, 16, 24, 40, 51, 61),
        intArrayOf(12, 12, 14, 19, 26, 58, 60, 55),
        intArrayOf(14, 13, 16, 24, 40, 57, 69, 56),
        intArrayOf(14, 17, 22, 29, 51, 87, 80, 62),
        intArrayOf(18, 22, 37, 56, 68, 109, 103, 77),
        intArrayOf(24, 35, 55, 64, 81, 104, 113, 92),
        intArrayOf(49, 64, 78, 87, 103, 121, 120, 101),
        intArrayOf(72, 92, 95, 98, 112, 100, 103, 99)
    )

    private const val END_MESSAGE_CONSTANT = "$!@#END"

    fun encodeMessage(bitmap: Bitmap, message: String): Bitmap? {
        val fullMessage = message + END_MESSAGE_CONSTANT
        val binaryMessage = toBinary(fullMessage)
        
        // We embed 1 bit per 8x8 block to be robust
        // This is low capacity but high robustness.
        // Capacity = (W/8) * (H/8) bits
        val width = bitmap.width
        val height = bitmap.height
        val blocksX = width / N
        val blocksY = height / N
        val maxBits = blocksX * blocksY
        
        if (binaryMessage.length > maxBits) {
             return null // Message too long
        }

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        var messageIndex = 0
        
        for (blockY in 0 until blocksY) {
            for (blockX in 0 until blocksX) {
                 if (messageIndex >= binaryMessage.length) return mutableBitmap
                 
                 val currentBit = binaryMessage[messageIndex] == '1'
                 processBlockEncode(mutableBitmap, blockX * N, blockY * N, currentBit)
                 messageIndex++
            }
        }
        
        return mutableBitmap
    }
    
    fun decodeMessage(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val blocksX = width / N
        val blocksY = height / N
        
        val binaryBuilder = StringBuilder()
        
        for (blockY in 0 until blocksY) {
            for (blockX in 0 until blocksX) {
                val bit = processBlockDecode(bitmap, blockX * N, blockY * N)
                binaryBuilder.append(if(bit) '1' else '0')
            }
        }
        
        // Convert binary to string and look for terminator
        val fullString = fromBinary(binaryBuilder.toString())
        if (fullString.contains(END_MESSAGE_CONSTANT)) {
            return fullString.substringBefore(END_MESSAGE_CONSTANT)
        }
        
        return null // Failed or not found
    }
    
    private fun processBlockEncode(bitmap: Bitmap, startX: Int, startY: Int, bit: Boolean) {
        val yChannel = Array(N) { DoubleArray(N) }
        val cbChannel = Array(N) { DoubleArray(N) }
        val crChannel = Array(N) { DoubleArray(N) }
        
        // 1. RGB to YCbCr extraction
        for (y in 0 until N) {
            for (x in 0 until N) {
                val pixel = bitmap.getPixel(startX + x, startY + y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // RGB to YCbCr conversion
                yChannel[y][x] = 0.299 * r + 0.587 * g + 0.114 * b
                cbChannel[y][x] = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b
                crChannel[y][x] = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b
            }
        }
        
        // 2. DCT on Y Channel (or Cr/Cb)
        // Embedding in Mid-Frequency of Y for robustness
        val dctY = applyDCT(yChannel)
        
        // 3. Embed bit in (4, 3) and (3, 4) coefficients (Mid-frequency)
        // Persistence approach: make sure coeff1 > coeff2 if bit=1, else coeff1 < coeff2
        val c1 = dctY[4][3]
        val c2 = dctY[3][4]
        val strength = 50.0 // Embedding strength (higher = more robust but more visible)
        
        if (bit) {
            if (c1 <= c2 + strength) {
                 dctY[4][3] = c2 + strength / 2
                 dctY[3][4] = c1 - strength / 2
            }
        } else {
             if (c1 + strength >= c2) {
                 dctY[4][3] = c2 - strength / 2
                 dctY[3][4] = c1 + strength / 2
             }
        }
        
        // 4. IDCT
        val idctY = applyIDCT(dctY)
        
        // 5. YCbCr back to RGB and save
         for (y in 0 until N) {
            for (x in 0 until N) {
                val Y = idctY[y][x]
                val Cb = cbChannel[y][x]
                val Cr = crChannel[y][x]
                
                var r = (Y + 1.402 * (Cr - 128)).toInt()
                var g = (Y - 0.344136 * (Cb - 128) - 0.714136 * (Cr - 128)).toInt()
                var b = (Y + 1.772 * (Cb - 128)).toInt()
                
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)
                
                bitmap.setPixel(startX + x, startY + y, Color.rgb(r, g, b))
            }
        }
    }

    private fun processBlockDecode(bitmap: Bitmap, startX: Int, startY: Int): Boolean {
         val yChannel = Array(N) { DoubleArray(N) }

         for (y in 0 until N) {
            for (x in 0 until N) {
                val pixel = bitmap.getPixel(startX + x, startY + y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // RGB to Y (Luminance only needed)
                yChannel[y][x] = 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        
        val dctY = applyDCT(yChannel)
        val c1 = dctY[4][3]
        val c2 = dctY[3][4]
        
        return c1 > c2 
    }
    
    private fun applyDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val result = Array(N) { DoubleArray(N) }
        val piOverN = PI / N
        
        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                
                for (x in 0 until N) {
                    for (y in 0 until N) {
                        sum += matrix[x][y] * 
                               cos((2 * x + 1) * u * piOverN / 2) * 
                               cos((2 * y + 1) * v * piOverN / 2)
                    }
                }
                result[u][v] = 0.25 * cu * cv * sum
            }
        }
        return result
    }

    private fun applyIDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val result = Array(N) { DoubleArray(N) }
        val piOverN = PI / N

        for (x in 0 until N) {
            for (y in 0 until N) {
                var sum = 0.0
                for (u in 0 until N) {
                    for (v in 0 until N) {
                        val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                        val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                        sum += cu * cv * matrix[u][v] * 
                               cos((2 * x + 1) * u * piOverN / 2) * 
                               cos((2 * y + 1) * v * piOverN / 2)
                    }
                }
                result[x][y] = 0.25 * sum
            }
        }
        return result
    }

    private fun toBinary(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            var binary = Integer.toBinaryString(char.code)
            while (binary.length < 8) {
                binary = "0$binary"
            }
            sb.append(binary)
        }
        return sb.toString()
    }
    
    private fun fromBinary(binary: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 8 <= binary.length) {
            val byteStr = binary.substring(i, i + 8)
            val charCode = Integer.parseInt(byteStr, 2)
            sb.append(charCode.toChar())
            i += 8
        }
        return sb.toString()
    }
}
