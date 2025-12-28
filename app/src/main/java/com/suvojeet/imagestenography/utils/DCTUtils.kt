package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DCTUtils {

    private const val N = 8 // Block size 8x8
    private const val END_MESSAGE_CONSTANT = "$!#"
    
    // Robustness Constants
    private const val ALPHA = 70.0 
    private const val REPETITION = 5 

    // Coefficients for embedding (Lower-frequency for WhatsApp survival)
    // (2,1) and (1,2) are preserved better than (3,2)/(2,3) during JPEG compression
    private val P1 = Pair(2, 1)
    private val P2 = Pair(1, 2)

    // Precomputed DCT Matrix T and its Transpose Tt
    // DCT = T * Block * Tt
    // IDCT = Tt * Coeffs * T
    private val T = Array(N) { DoubleArray(N) }
    private val Tt = Array(N) { DoubleArray(N) }

    init {
        // Precompute Cosine Transform Matrix
        for (i in 0 until N) {
            val c = if (i == 0) 1.0 / sqrt(N.toDouble()) else sqrt(2.0 / N.toDouble())
            for (j in 0 until N) {
                T[i][j] = c * cos(((2 * j + 1) * i * PI) / (2 * N))
                Tt[j][i] = T[i][j] // Transpose
            }
        }
    }

    fun encodeMessage(bitmap: Bitmap, message: String): Bitmap? {
        val fullMessage = message + END_MESSAGE_CONSTANT
        val bitArray = toBitArray(fullMessage)
        
        val width = bitmap.width
        val height = bitmap.height
        
        // Capacity Check
        val maxBits = getMaxBitsCapacity(width, height)
        if (bitArray.size > maxBits) return null

        // Convert to mutable bitmap
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Get all pixels into an array for fast processing
        val pixels = IntArray(width * height)
        newBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var bitIndex = 0
        var repetitionCount = 0
        
        // Reusable buffers
        val block = Array(N) { DoubleArray(N) }
        val dctBlock = Array(N) { DoubleArray(N) }
        val idctBlock = Array(N) { DoubleArray(N) }
        val tempBlock = Array(N) { DoubleArray(N) } // For matrix mult intermediate

        // Iterate blocks
        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (bitIndex >= bitArray.size) break // Done

                if (x + N <= width && y + N <= height) {
                    processBlockEncodeFast(pixels, width, x, y, bitArray[bitIndex], block, dctBlock, idctBlock, tempBlock)
                    
                    repetitionCount++
                    if (repetitionCount >= REPETITION) {
                        repetitionCount = 0
                        bitIndex++
                    }
                }
            }
            if (bitIndex >= bitArray.size) break
        }
        
        // Write pixels back
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return newBitmap
    }

    fun decodeMessage(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val recoveredBits = ArrayList<Boolean>()
        var blockVotes = 0
        var blocksRead = 0
        
        // Reusable buffers
        val block = Array(N) { DoubleArray(N) }
        val dctBlock = Array(N) { DoubleArray(N) }
        val tempBlock = Array(N) { DoubleArray(N) }

        for (y in 0 until height step N) {
            for (x in 0 until width step N) {
                if (x + N <= width && y + N <= height) {
                    val bitVal = processBlockDecodeFast(pixels, width, x, y, block, dctBlock, tempBlock)
                    if (bitVal) blockVotes++
                    blocksRead++

                    if (blocksRead >= REPETITION) {
                        // Majority Vote
                        recoveredBits.add(blockVotes > (REPETITION / 2))
                        
                        blockVotes = 0
                        blocksRead = 0
                    }
                }
            }
        }
        
        return fromBitArray(recoveredBits)
    }

    fun getMaxMessageLength(width: Int, height: Int): Int {
        val maxBits = getMaxBitsCapacity(width, height)
        val maxChars = (maxBits / 8) - END_MESSAGE_CONSTANT.length
        return if (maxChars < 0) 0 else maxChars
    }
    
    private fun getMaxBitsCapacity(width: Int, height: Int): Int {
        val totalBlocks = (width / N) * (height / N)
        return totalBlocks / REPETITION
    }

    // --- Fast Matrix DCT Logic ---

    // DCT = T * Block * Tt
    private fun fastDCT(input: Array<DoubleArray>, output: Array<DoubleArray>, temp: Array<DoubleArray>) {
        multiplyMatrix(T, input, temp)     // temp = T * input
        multiplyMatrix(temp, Tt, output)   // output = temp * Tt
    }

    // IDCT = Tt * Coeffs * T
    private fun fastIDCT(input: Array<DoubleArray>, output: Array<DoubleArray>, temp: Array<DoubleArray>) {
        multiplyMatrix(Tt, input, temp)    // temp = Tt * input
        multiplyMatrix(temp, T, output)    // output = temp * T
    }
    
    // C = A * B
    private fun multiplyMatrix(A: Array<DoubleArray>, B: Array<DoubleArray>, C: Array<DoubleArray>) {
        // Unrolled or standard loop. N=8 is small.
        for (i in 0 until N) {
            for (j in 0 until N) {
                var sum = 0.0
                for (k in 0 until N) {
                    sum += A[i][k] * B[k][j]
                }
                C[i][j] = sum
            }
        }
    }

    private fun processBlockEncodeFast(
        pixels: IntArray, width: Int, startX: Int, startY: Int, bit: Boolean,
        block: Array<DoubleArray>, dctBlock: Array<DoubleArray>, idctBlock: Array<DoubleArray>, temp: Array<DoubleArray>
    ) {
        // 1. Extract Green Channel
        for (i in 0 until N) {
            val rowOffset = (startY + i) * width + startX
            for (j in 0 until N) {
                val pixel = pixels[rowOffset + j]
                block[i][j] = ((pixel shr 8) and 0xFF).toDouble() // Fast Green extract
            }
        }

        // 2. DCT
        fastDCT(block, dctBlock, temp)

        // 3. Embed
        val u1 = P1.first; val v1 = P1.second
        val u2 = P2.first; val v2 = P2.second
        
        val val1 = dctBlock[u1][v1]
        val val2 = dctBlock[u2][v2]
        
        if (bit) {
            // Encode 1: Ensure val1 > val2 + ALPHA
            if (val1 <= val2 + ALPHA) {
                val avg = (val1 + val2) / 2
                dctBlock[u1][v1] = avg + (ALPHA / 2) + 2.0
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

        // 4. IDCT
        fastIDCT(dctBlock, idctBlock, temp)

        // 5. Replace Pixels
        for (i in 0 until N) {
            val rowOffset = (startY + i) * width + startX
            for (j in 0 until N) {
                val pixel = pixels[rowOffset + j]
                
                var newGreen = idctBlock[i][j].roundToInt()
                newGreen = newGreen.coerceIn(0, 255)
                
                // Keep R and B, replace G
                // pixel is ARGB. 
                // A = (pixel >> 24) & 0xFF
                // R = (pixel >> 16) & 0xFF
                // B = pixel & 0xFF
                // We reconstruct carefully to avoid sign extension issues with alpha
                
                val alpha = pixel.toInt() and -0x1000000 // Mask alpha 0xFF000000
                val red = (pixel shr 16) and 0xFF
                val blue = pixel and 0xFF
                
                pixels[rowOffset + j] = alpha or (red shl 16) or (newGreen shl 8) or blue
            }
        }
    }

    private fun processBlockDecodeFast(
        pixels: IntArray, width: Int, startX: Int, startY: Int,
        block: Array<DoubleArray>, dctBlock: Array<DoubleArray>, temp: Array<DoubleArray>
    ): Boolean {
        // 1. Extract Green
        for (i in 0 until N) {
            val rowOffset = (startY + i) * width + startX
            for (j in 0 until N) {
                val pixel = pixels[rowOffset + j]
                block[i][j] = ((pixel shr 8) and 0xFF).toDouble()
            }
        }

        // 2. DCT
        fastDCT(block, dctBlock, temp)
        
        val val1 = dctBlock[P1.first][P1.second]
        val val2 = dctBlock[P2.first][P2.second]
        
        return val1 > val2
    }

    private fun toBitArray(cString: String): BooleanArray {
        val bytes = cString.toByteArray(Charsets.UTF_8)
        val bits = BooleanArray(bytes.size * 8)
        for (i in bytes.indices) {
            val byteVal = bytes[i].toInt() and 0xFF
            for (j in 0 until 8) {
                bits[i * 8 + j] = ((byteVal shr (7 - j)) and 1) == 1
            }
        }
        return bits
    }

    private fun fromBitArray(bits: ArrayList<Boolean>): String? {
        val baos = java.io.ByteArrayOutputStream()
        val endBytes = END_MESSAGE_CONSTANT.toByteArray(Charsets.UTF_8)
        val endLen = endBytes.size
        
        var currentByte = 0
        var bitCount = 0
        
        for (bit in bits) {
            currentByte = (currentByte shl 1) or (if (bit) 1 else 0)
            bitCount++
            
            if (bitCount == 8) {
                baos.write(currentByte)
                currentByte = 0
                bitCount = 0
                
                if (baos.size() >= endLen) {
                    val allBytes = baos.toByteArray()
                    // Check last endLen bytes
                    var match = true
                    for (k in 0 until endLen) {
                        if (allBytes[allBytes.size - endLen + k] != endBytes[k]) {
                            match = false
                            break
                        }
                    }
                    if (match) {
                        return String(allBytes, 0, allBytes.size - endLen, Charsets.UTF_8)
                    }
                }
                
                if (baos.size() > 5000) return null // Increased limit
            }
        }
        return null
    }
}
