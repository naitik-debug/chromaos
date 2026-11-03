package com.example.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null,
    val isFavorite: Boolean = false
)

fun Drawable.toComposeImageBitmap(desiredSize: Int = 128): ImageBitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            bitmap.asImageBitmap()
        } else {
            val width = intrinsicWidth.takeIf { it > 0 } ?: desiredSize
            val height = intrinsicHeight.takeIf { it > 0 } ?: desiredSize
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}
