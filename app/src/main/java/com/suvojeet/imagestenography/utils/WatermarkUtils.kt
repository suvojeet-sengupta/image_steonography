package com.suvojeet.imagestenography.utils

import android.graphics.*

object WatermarkUtils {

    fun applyWatermark(bitmap: Bitmap, text: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Create a mutable bitmap to draw on
        val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(watermarkedBitmap)
        
        // Text Paint Configuration
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = 40 // 0-255, low opacity for subtle watermark
            textSize = width / 20f // Adaptive text size
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f, 2f, 2f, Color.LTGRAY) // Subtle shadow for visibility on white
        }
        
        // Rotate the canvas to draw diagonally
        // We actually want to tile the text.
        // Easier approach: Draw text at multiple points with rotation
        
        val textWidth = paint.measureText(text)
        val textHeight = paint.textSize
        
        val gap = textWidth * 1.5f // Gap between watermarks
        
        canvas.save()
        canvas.rotate(-45f, width / 2f, height / 2f)
        
        // Draw across the entire (rotated) canvas area
        // We need to cover a larger area because rotation exposes corners
        val diag = Math.sqrt((width * width + height * height).toDouble()).toFloat()
        
        var y = -diag / 2
        while (y < diag * 1.5) {
            var x = -diag / 2
            while (x < diag * 1.5) {
                canvas.drawText(text, x, y, paint)
                x += gap
            }
            y += gap
        }
        
        canvas.restore()
        
        return watermarkedBitmap
    }
}
