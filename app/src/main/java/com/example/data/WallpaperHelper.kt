package com.example.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class WallpaperHelper(private val context: Context) {

    data class PickedWallpaperResult(
        val message: String,
        val previewDataUri: String
    )

    suspend fun applyWallpaper(
        imageUri: Uri,
        whichScreen: WallpaperTarget = WallpaperTarget.BOTH
    ): Result<String> = applyWallpaperWithPreview(imageUri, whichScreen).map { it.message }

    suspend fun applyWallpaperWithPreview(
        imageUri: Uri,
        whichScreen: WallpaperTarget = WallpaperTarget.BOTH
    ): Result<PickedWallpaperResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open selected image stream."))

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            val maxDimension = 2560
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

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

            applyBitmapToTarget(wallpaperManager, bitmap, whichScreen)

            val previewDataUri = createCompactPreviewDataUri(bitmap)
            bitmap.recycle()

            val targetLabel = when (whichScreen) {
                WallpaperTarget.HOME -> "Home Screen"
                WallpaperTarget.LOCK -> "Lock Screen"
                WallpaperTarget.BOTH -> "Home & Lock Screens"
            }
            Result.success(PickedWallpaperResult("Custom photo wallpaper applied to $targetLabel!", previewDataUri))
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied by system: Some devices restrict wallpaper setting by third-party apps."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to apply wallpaper: ${e.localizedMessage ?: "Device restriction"}"))
        }
    }

    suspend fun applyThemeWallpaperBitmap(
        colors: List<Int>,
        wallpaperMode: String,
        whichScreen: WallpaperTarget,
        width: Int,
        height: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val c1 = colors.getOrElse(0) { Color.parseColor("#291C0E") }
            val c2 = colors.getOrElse(1) { Color.parseColor("#6E473B") }
            val c3 = colors.getOrElse(2) { Color.parseColor("#A78D78") }
            val c4 = colors.getOrElse(3) { Color.parseColor("#BEB5A9") }
            val c5 = colors.getOrElse(4) { Color.parseColor("#E1D4C2") }

            val w = width.coerceIn(720, 1440)
            val h = height.coerceIn(1280, 3200)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            when (wallpaperMode.lowercase()) {
                "oled" -> {
                    canvas.drawColor(Color.parseColor("#050506"))
                }
                "botanical" -> {
                    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, w * 0.4f, h.toFloat(),
                            c1, Color.parseColor("#091410"),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

                    val glowTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.2f, h * 0.2f, w * 0.65f,
                            withAlpha(c3, 110), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(w * 0.2f, h * 0.2f, w * 0.65f, glowTop)

                    val glowBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.8f, h * 0.75f, w * 0.7f,
                            withAlpha(c5, 120), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(w * 0.8f, h * 0.75f, w * 0.7f, glowBottom)
                }
                else -> {
                    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, w * 0.35f, h.toFloat(),
                            intArrayOf(c1, c2, c5),
                            floatArrayOf(0f, 0.55f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

                    val orbTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.8f, h * 0.18f, w * 0.68f,
                            withAlpha(c3, 135), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(w * 0.8f, h * 0.18f, w * 0.68f, orbTop)

                    val orbBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.18f, h * 0.82f, w * 0.72f,
                            withAlpha(c4, 110), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(w * 0.18f, h * 0.82f, w * 0.72f, orbBottom)
                }
            }

            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isWallpaperSupported) {
                bitmap.recycle()
                return@withContext Result.failure(Exception("Wallpapers are not supported on this device profile."))
            }

            applyBitmapToTarget(wallpaperManager, bitmap, whichScreen)
            bitmap.recycle()

            val targetLabel = when (whichScreen) {
                WallpaperTarget.HOME -> "Home Screen"
                WallpaperTarget.LOCK -> "Lock Screen"
                WallpaperTarget.BOTH -> "Home & Lock"
            }
            Result.success("System wallpaper updated ($targetLabel)")
        } catch (e: SecurityException) {
            Result.failure(Exception("Wallpaper permission denied by system."))
        } catch (e: Exception) {
            Result.failure(Exception("Wallpaper error: ${e.localizedMessage ?: "Device restriction"}"))
        }
    }

    private fun applyBitmapToTarget(
        wallpaperManager: WallpaperManager,
        bitmap: Bitmap,
        whichScreen: WallpaperTarget
    ) {
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
    }

    private fun createCompactPreviewDataUri(source: Bitmap): String {
        val maxPreviewDim = 540
        val scale = minOf(
            maxPreviewDim.toFloat() / source.width.coerceAtLeast(1),
            maxPreviewDim.toFloat() / source.height.coerceAtLeast(1),
            1f
        )
        val previewW = (source.width * scale).toInt().coerceAtLeast(1)
        val previewH = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, previewW, previewH, true)
        } else {
            source
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
        if (scaled !== source) {
            scaled.recycle()
        }
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH;

    companion object {
        fun fromString(raw: String?): WallpaperTarget {
            return when (raw?.trim()?.lowercase()) {
                "home" -> HOME
                "lock" -> LOCK
                else -> BOTH
            }
        }
    }
}
