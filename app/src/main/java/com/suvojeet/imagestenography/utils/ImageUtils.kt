package com.suvojeet.imagestenography.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ImageUtils {

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Steganography")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                try {
                    val outputStream: OutputStream? = resolver.openOutputStream(it)
                    outputStream?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }
                    it
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(it, null, null)
                    null
                }
            }
        }
    }
}
