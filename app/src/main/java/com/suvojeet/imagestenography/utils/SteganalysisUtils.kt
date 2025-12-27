package com.suvojeet.imagestenography.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.ln

object SteganalysisUtils {

    data class AnalysisResult(
        val entropy: Double,
        val isSuspicious: Boolean,
        val noiseMap: Bitmap // A visualization of the LSBs
    )

    // Threshold for LSB Entropy (0.0 to 1.0)
    // Natural images usually have LSB entropy < 0.9 depending on complexity.
    // Encrypted steganography fills LSB with uniform noise -> Entropy ~ 1.0.
    private const val ENTROPY_THRESHOLD = 0.92

    fun analyzeImage(bitmap: Bitmap): AnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        
        val noiseMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val lsbCounts = IntArray(2) // 0s and 1s
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val blue = Color.blue(pixel)
                
                // Extract LSB
                val lsb = blue and 1
                lsbCounts[lsb]++
                
                // Visualization: 0 -> Black, 1 -> White (High contrast noise)
                val visualPixel = if (lsb == 1) Color.WHITE else Color.BLACK
                noiseMap.setPixel(x, y, visualPixel)
            }
        }
        
        // Calculate Entropy
        // H(X) = - sum(p(x) * log2(p(x)))
        var entropy = 0.0
        for (count in lsbCounts) {
            if (count > 0) {
                val p = count.toDouble() / totalPixels
                entropy -= p * (ln(p) / ln(2.0))
            }
        }
        
        val isSuspicious = entropy > ENTROPY_THRESHOLD
        
        return AnalysisResult(entropy, isSuspicious, noiseMap)
    }
}
