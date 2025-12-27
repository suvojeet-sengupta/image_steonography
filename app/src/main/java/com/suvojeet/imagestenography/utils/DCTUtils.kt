package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DCTUtils {

    private const val N = 8 // Block size 8x8
    private const val END_MESSAGE_CONSTANT = "$!#" // Short end marker to save space

    // Mid-frequency coefficients to embed in (Zig-Zag order approximate)
    // Using (5,2) and (4,3) - mid band
    private val P1 = Pair(5, 2)
    private val P2 = Pair(4, 3)
    
    // Persistence alpha - strength of watermark
    // Higher = more robust but more visible artifacts
    private const val ALPHA = 20.0 

    fun encodeMessage(bitmap: Bitmap, message: String): Bitmap? {
        val fullMessage = message + END_MESSAGE_CONSTANT
        val bitArray = toBitArray(fullMessage)
        
        val width = bitmap.width
        val height = bitmap.height
        
        // Ensure image can hold the message
        // 1 bit per 8x8 block
        val maxBits = (width / N) * (height / N)
        if (bitArray.size > maxBits) {
            return null // Message too long for DCT
        }

        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        var bitIndex = 0
        
        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (bitIndex >= bitArray.size) return newBitmap
                
                // Process 8x8 block
                if (x + N <= width && y + N <= height) {
                    processBlockEncode(newBitmap, x, y, bitArray[bitIndex])
                    bitIndex++
                }
            }
        }
        
        return newBitmap
    }

    fun decodeMessage(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        
        val bitList = ArrayList<Boolean>()
        
        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (x + N <= width && y + N <= height) {
                    bitList.add(processBlockDecode(bitmap, x, y))
                }
            }
        }
        
        return fromBitArray(bitList)
    }

    // --- Core DCT Logic ---

    private fun processBlockEncode(bitmap: Bitmap, startX: Int, startY: Int, bit: Boolean) {
        // 1. Extract Blue channel (Y channel in YCbCr is better, but Blue is simplest for robust demo)
        // Ideally should convert RGB -> YUB and embed in Y
        // For simplicity and speed in this demo, we embed in Blue component as it's less sensitive.
        val block = Array(N) { DoubleArray(N) }
        
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                block[i][j] = Color.blue(pixel).toDouble() 
            }
        }

        // 2. DCT
        val dctBlock = applyDCT(block)

        // 3. Embed Logic
        // If bit is 1, ensure Coeff(P1) > Coeff(P2) + ALPHA
        // If bit is 0, ensure Coeff(P2) > Coeff(P1) + ALPHA
        val u1 = P1.first; val v1 = P1.second
        val u2 = P2.first; val v2 = P2.second
        
        val val1 = dctBlock[u1][v1]
        val val2 = dctBlock[u2][v2]
        
        if (bit) {
            // Encode 1
            if (val1 <= val2 + ALPHA) {
                val avg = (val1 + val2) / 2
                dctBlock[u1][v1] = avg + (ALPHA / 2) + 1.0
                dctBlock[u2][v2] = avg - (ALPHA / 2) - 1.0
            }
        } else {
            // Encode 0
             if (val2 <= val1 + ALPHA) {
                val avg = (val1 + val2) / 2
                dctBlock[u1][v1] = avg - (ALPHA / 2) - 1.0
                dctBlock[u2][v2] = avg + (ALPHA / 2) + 1.0
            }
        }

        // 4. Inverse DCT
        val idctBlock = applyIDCT(dctBlock)

        // 5. Replace pixels
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                var newBlue = idctBlock[i][j].roundToInt()
                newBlue = newBlue.coerceIn(0, 255)
                
                // Reconstruct ARGB (Keep R and G same, update B)
                val newPixel = Color.rgb(Color.red(pixel), Color.green(pixel), newBlue)
                bitmap.setPixel(startX + j, startY + i, newPixel)
            }
        }
    }

    private fun processBlockDecode(bitmap: Bitmap, startX: Int, startY: Int): Boolean {
        val block = Array(N) { DoubleArray(N) }
        
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                block[i][j] = Color.blue(pixel).toDouble()
            }
        }

        val dctBlock = applyDCT(block)
        
        val val1 = dctBlock[P1.first][P1.second]
        val val2 = dctBlock[P2.first][P2.second]
        
        // Returns true (1) if val1 > val2, else false (0)
        return val1 > val2
    }

    // --- Math Helpers ---

    private fun applyDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val result = Array(N) { DoubleArray(N) }
        val piN = PI / N.toDouble()

        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0

                for (x in 0 until N) {
                    for (y in 0 until N) {
                        sum += matrix[x][y] * 
                               cos((2 * x + 1) * u * piN / 2.0) * 
                               cos((2 * y + 1) * v * piN / 2.0)
                    }
                }
                result[u][v] = 0.25 * cu * cv * sum // 2/N * cu * cv * sum, where N=8, 2/8 = 0.25
            }
        }
        return result
    }

    private fun applyIDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val result = Array(N) { DoubleArray(N) }
        val piN = PI / N.toDouble()

        for (x in 0 until N) {
            for (y in 0 until N) {
                var sum = 0.0
                for (u in 0 until N) {
                    for (v in 0 until N) {
                         val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                         val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                         
                         sum += cu * cv * matrix[u][v] * 
                                cos((2 * x + 1) * u * piN / 2.0) * 
                                cos((2 * y + 1) * v * piN / 2.0)
                    }
                }
                result[x][y] = 0.25 * sum
            }
        }
        return result
    }

    private fun toBitArray(cString: String): BooleanArray {
        val bits = BooleanArray(cString.length * 8)
        for (i in cString.indices) {
            val charCode = cString[i].code
            for (j in 0 until 8) {
                bits[i * 8 + j] = ((charCode shr (7 - j)) and 1) == 1
            }
        }
        return bits
    }

    private fun fromBitArray(bits: ArrayList<Boolean>): String? {
        val sb = StringBuilder()
        var currentByte = 0
        var bitCount = 0
        
        for (bit in bits) {
            currentByte = (currentByte shl 1) or (if (bit) 1 else 0)
            bitCount++
            
            if (bitCount == 8) {
                sb.append(currentByte.toChar())
                currentByte = 0
                bitCount = 0
                
                if (sb.endsWith(END_MESSAGE_CONSTANT)) {
                    return sb.substring(0, sb.length - END_MESSAGE_CONSTANT.length)
                }
            }
        }
        return null // End marker not found
    }
}
