package com.example.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperHelper(private val context: Context) {

    suspend fun applyWallpaper(
        imageUri: Uri,
        whichScreen: WallpaperTarget = WallpaperTarget.BOTH
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open selected image stream."))

            // Decode dimensions first
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate appropriate inSampleSize to avoid OOM
            var inSampleSize = 1
            val maxDimension = 2560
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            // Decode actual bitmap
            val decodeStream = contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot read image stream for decoding."))
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image data into valid bitmap."))
            }

            val wallpaperManager = WallpaperManager.getInstance(context)

            if (!wallpaperManager.isWallpaperSupported) {
                return@withContext Result.failure(Exception("Wallpapers are not supported on this device/ROM profile."))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flag = when (whichScreen) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, flag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }

            Result.success("Wallpaper successfully applied!")
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied by system: Some devices restrict wallpaper setting by third-party apps."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to apply wallpaper: ${e.localizedMessage ?: "Device restriction"}"))
        }
    }
}

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}
