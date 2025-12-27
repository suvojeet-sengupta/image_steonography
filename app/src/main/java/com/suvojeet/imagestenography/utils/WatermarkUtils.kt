package com.suvojeet.imagestenography.utils

import android.graphics.*
import kotlin.math.abs

object WatermarkUtils {

    /**
     * Applies an "Invisible Ink" watermark by slightly modifying the Blue channel.
     * This is robust (spatial) but invisible to the naked eye.
     */
    fun applyInvisibleWatermark(bitmap: Bitmap, text: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. Create a "Mask" of the text
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(maskBitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE // Text is White (on transparent background)
            textSize = width / 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Draw tiled text onto mask
        drawTiledText(canvas, text, paint, width, height)
        
        // 2. Embed Mask into Original Bitmap
        val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // We iterate pixels. Where Mask is WHITE, we add +4 to Blue channel of Original.
        // This small delta is invisible but detectable.
        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        
        watermarkedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        maskBitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            // Check if mask has text here (White)
            if (maskPixels[i] != 0) { // Not transparent/black
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                var b = Color.blue(pixel)
                
                // Add delta. Wrap around or clamp? Clamping is safer for visibility.
                // We add significant delta (e.g. 5) to ensure it survives compression better.
                // +5 is still very hard to see in complex images.
                b = (b + 5).coerceIn(0, 255)
                
                pixels[i] = Color.rgb(r, g, b)
            }
        }
        
        watermarkedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return watermarkedBitmap
    }

    /**
     * Reveals the invisible watermark by applying a high-pass / contrast filter.
     * It amplifies blue channel noise to make the text pop out.
     */
    fun revealWatermark(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Simple "Blue Boost" visualization. 
        // We highlight pixels that have "unusual" blue spikes compared to neighbors?
        // Or simpler: Just isolate Blue channel and maximize contrast.
        
        // Better Strategy: Spatial Differentiator.
        // But for this simple implementation, let's just map Blue Channel to Grayscale and Histogram Equalize it?
        // No, the delta is small.
        
        // Let's try error level analysis simulation.
        // Actually, since we know we added +5 to Blue...
        // We can visualze local blue variance.
        
        val newPixels = IntArray(width * height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val b = Color.blue(pixel)
            
            // Just visualize the Blue channel purely. 
            // The text (+5) will be slightly brighter than surroundings.
            // To make it POP, we multiply differences.
            
            // Visualization: Isolate Blue
            newPixels[i] = Color.rgb(0, 0, b)
        }
        
        // Apply a high contrast using ColorMatrix
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // High Contrast Matrix for Blue
        val contrast = 10f // Super high contrast
        val scale = 0f
        
        val colorMatrix = ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, contrast, 0f, -128f * contrast, // Boost Blue
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        output.setPixels(newPixels, 0, width, 0, 0, width, height) // set base pixels
        
        canvas.drawBitmap(output, 0f, 0f, paint)
        
        return result
    }

    private fun drawTiledText(canvas: Canvas, text: String, paint: Paint, width: Int, height: Int) {
        val textWidth = paint.measureText(text)
        val gap = textWidth * 1.5f
        
        canvas.save()
        canvas.rotate(-45f, width / 2f, height / 2f)
        
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
    }
}
