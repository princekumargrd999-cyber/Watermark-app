package com.watermark.videoapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Form ki settings (text, opacity, icon) phone ke local storage mein save hoti hain,
 * taaki user ko har baar form na bharna pade. Sab kuch offline.
 */
class SettingsStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("watermark_prefs", Context.MODE_PRIVATE)
    private val iconFile: File get() = File(context.filesDir, "watermark_icon.png")

    fun loadText(): String = prefs.getString(KEY_TEXT, "") ?: ""

    fun loadOpacityPercent(): Int = prefs.getInt(KEY_OPACITY, 60)

    fun save(text: String, opacityPercent: Int) {
        prefs.edit().putString(KEY_TEXT, text).putInt(KEY_OPACITY, opacityPercent).apply()
    }

    fun hasIcon(): Boolean = iconFile.exists()

    fun loadIcon(): Bitmap? =
        if (iconFile.exists()) BitmapFactory.decodeFile(iconFile.absolutePath) else null

    /** Gallery se chuni hui image ko chhota karke app ke andar copy kar deta hai. */
    fun saveIconFromUri(uri: Uri): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false

            val maxSide = 512
            val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
            val out = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else bmp

            iconFile.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_TEXT = "text"
        private const val KEY_OPACITY = "opacity"
    }
}
