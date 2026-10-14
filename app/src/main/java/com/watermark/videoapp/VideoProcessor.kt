package com.watermark.videoapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Video par watermark lagata hai. Poora offline, phone ke andar hi. Main thread se call karo. */
@androidx.annotation.OptIn(UnstableApi::class)
class VideoProcessor(private val context: Context) {

    private var transformer: Transformer? = null

    fun start(
        videoUri: Uri,
        icon: Bitmap?,
        text: String,
        opacity: Float,
        outputFile: File,
        onDone: (success: Boolean, error: String?) -> Unit
    ) {
        val (w, h) = readVideoSize(videoUri)
        val base = min(w, h)
        val margin = (base * 0.03f).toInt()

        val overlays = mutableListOf<TextureOverlay>()

        if (icon != null) {
            val target = (base * 0.14f).toInt().coerceAtLeast(32)
            val scale = target.toFloat() / max(icon.width, icon.height)
            val scaled = Bitmap.createScaledBitmap(
                icon,
                max(1, (icon.width * scale).toInt()),
                max(1, (icon.height * scale).toInt()),
                true
            )
            overlays += IconOverlay(scaled, w, h, margin)
        }

        if (text.isNotBlank()) {
            val textBitmap = renderTextBitmap(text.replace('\n', ' '), base, w)
            overlays += MovingTextOverlay(textBitmap, w, h, margin, opacity)
        }

        val videoEffects = listOf<Effect>(OverlayEffect(ImmutableList.copyOf(overlays)))
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        if (outputFile.exists()) outputFile.delete()

        val t = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onDone(true, null)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    onDone(false, exportException.message ?: "Unknown error")
                }
            })
            .build()
        transformer = t
        t.start(edited, outputFile.absolutePath)
    }

    /** 0..100, ya null agar abhi progress available nahi. */
    fun progressPercent(): Int? {
        val holder = ProgressHolder()
        val state = transformer?.getProgress(holder)
        return if (state == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress else null
    }

    fun cancel() {
        transformer?.cancel()
    }

    /** Video ki (rotation ke baad) width/height. */
    private fun readVideoSize(uri: Uri): Pair<Int, Int> {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (w == null || h == null) throw IllegalStateException("Could not read video size")
            if (rot == 90 || rot == 270) {
                val tmp = w; w = h; h = tmp
            }
            return w to h
        } finally {
            r.release()
        }
    }

    /** Text ko ek transparent bitmap mein likhta hai (white text + black outline, Hindi bhi chalega). */
    private fun renderTextBitmap(text: String, base: Int, videoWidth: Int): Bitmap {
        var size = base * 0.045f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = size
        }
        val maxWidth = videoWidth * 0.8f
        val measured = fill.measureText(text)
        if (measured > maxWidth) {
            size *= maxWidth / measured
            fill.textSize = size
        }
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = size * 0.12f
            strokeJoin = Paint.Join.ROUND
        }

        val pad = ceil(stroke.strokeWidth).toInt() + 2
        val textWidth = ceil(fill.measureText(text)).toInt()
        val fm = fill.fontMetrics
        val textHeight = ceil(fm.descent - fm.ascent).toInt()

        val bmp = Bitmap.createBitmap(textWidth + 2 * pad, textHeight + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val baseline = pad - fm.ascent
        canvas.drawText(text, pad.toFloat(), baseline, stroke)
        canvas.drawText(text, pad.toFloat(), baseline, fill)
        return bmp
    }
}
