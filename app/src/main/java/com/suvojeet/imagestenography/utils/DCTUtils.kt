package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DCTUtils {

    private const val N = 8 // Block size 8x8
    private const val END_MESSAGE_CONSTANT = "$!#" // Short end marker
    
    // Improved Robustness Constants
    private const val ALPHA = 50.0 // Increased from 20 to 50 for stronger watermark
    private const val REPETITION = 3 // Write each bit 3 times and vote (Majority Rule)

    // Lower-Mid frequency coefficients (Stronger against compression than High-Mid)
    // (3,2) and (2,3) are good choices
    private val P1 = Pair(3, 2)
    private val P2 = Pair(2, 3)

    fun encodeMessage(bitmap: Bitmap, message: String): Bitmap? {
        val fullMessage = message + END_MESSAGE_CONSTANT
        val bitArray = toBitArray(fullMessage)
        
        val width = bitmap.width
        val height = bitmap.height
        
        // Capacity Check: (Blocks) / Repetition
        val totalBlocks = (width / N) * (height / N)
        val maxBits = totalBlocks / REPETITION
        
        if (bitArray.size > maxBits) {
            return null
        }

        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        var bitIndex = 0
        var repetitionCount = 0
        
        // Iterate blocks
        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (bitIndex >= bitArray.size) return newBitmap
                
                if (x + N <= width && y + N <= height) {
                    // Encode the current bit (repeatedly)
                    processBlockEncode(newBitmap, x, y, bitArray[bitIndex])
                    
                    repetitionCount++
                    if (repetitionCount >= REPETITION) {
                        repetitionCount = 0
                        bitIndex++
                    }
                }
            }
        }
        
        return newBitmap
    }

    fun decodeMessage(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        
        val recoveredBits = ArrayList<Boolean>()
        var blockVotes = 0 // Count of '1's found in the repetition set
        var blocksRead = 0
        
        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (x + N <= width && y + N <= height) {
                    val bitVal = processBlockDecode(bitmap, x, y)
                    if (bitVal) blockVotes++
                    blocksRead++

                    if (blocksRead >= REPETITION) {
                        // Majority Vote
                        val limit = REPETITION / 2
                        recoveredBits.add(blockVotes > limit)
                        
                        // Reset for next bit
                        blockVotes = 0
                        blocksRead = 0
                    }
                }
            }
        }
        
        return fromBitArray(recoveredBits)
    }

    // --- Core DCT Logic ---

    private fun processBlockEncode(bitmap: Bitmap, startX: Int, startY: Int, bit: Boolean) {
        val block = Array(N) { DoubleArray(N) }
        
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                // Use GREEN channel. Green has highest contribution to Luminance (71%) 
                // and is less subsampled than Blue.
                block[i][j] = Color.green(pixel).toDouble() 
            }
        }

        // DCT
        val dctBlock = applyDCT(block)

        // Embed
        val u1 = P1.first; val v1 = P1.second
        val u2 = P2.first; val v2 = P2.second
        
        val val1 = dctBlock[u1][v1]
        val val2 = dctBlock[u2][v2]
        
        // Swapping/Adapting logic
        if (bit) {
            // Encode 1: Ensure val1 > val2 + ALPHA
            if (val1 <= val2 + ALPHA) {
                val avg = (val1 + val2) / 2
                dctBlock[u1][v1] = avg + (ALPHA / 2) + 2.0 // +2 for safety margin
                dctBlock[u2][v2] = avg - (ALPHA / 2) - 2.0
            }
        } else {
            // Encode 0: Ensure val2 > val1 + ALPHA
             if (val2 <= val1 + ALPHA) {
                val avg = (val1 + val2) / 2
                dctBlock[u1][v1] = avg - (ALPHA / 2) - 2.0
                dctBlock[u2][v2] = avg + (ALPHA / 2) + 2.0
            }
        }

        // Inverse DCT
        val idctBlock = applyIDCT(dctBlock)

        // Replace pixels
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                var newGreen = idctBlock[i][j].roundToInt()
                newGreen = newGreen.coerceIn(0, 255)
                
                // Keep R and B, update G
                val newPixel = Color.rgb(Color.red(pixel), newGreen, Color.blue(pixel))
                bitmap.setPixel(startX + j, startY + i, newPixel)
            }
        }
    }

    private fun processBlockDecode(bitmap: Bitmap, startX: Int, startY: Int): Boolean {
        val block = Array(N) { DoubleArray(N) }
        
        for (i in 0 until N) {
            for (j in 0 until N) {
                val pixel = bitmap.getPixel(startX + j, startY + i)
                block[i][j] = Color.green(pixel).toDouble() // Use GREEN
            }
        }

        val dctBlock = applyDCT(block)
        
        val val1 = dctBlock[P1.first][P1.second]
        val val2 = dctBlock[P2.first][P2.second]
        
        return val1 > val2
    }

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
                result[u][v] = 0.25 * cu * cv * sum
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
                if (currentByte == 0) break // Null terminator check if needed (not here)
                
                sb.append(currentByte.toChar())
                currentByte = 0
                bitCount = 0
                
                if (sb.endsWith(END_MESSAGE_CONSTANT)) {
                    return sb.substring(0, sb.length - END_MESSAGE_CONSTANT.length)
                }
                // Safety break for garbage data
                if (sb.length > 500) return null 
            }
        }
        return null
    }
}
