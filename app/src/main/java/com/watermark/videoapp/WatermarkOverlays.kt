package com.watermark.videoapp

import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings

/**
 * Icon: video ke BOTTOM-RIGHT corner mein fixed.
 * Anchor values NDC mein hote hain: (-1,-1) = bottom-left, (1,1) = top-right.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class IconOverlay(
    private val bitmap: Bitmap,
    videoWidth: Int,
    videoHeight: Int,
    marginPx: Int
) : BitmapOverlay() {

    private val settings: OverlaySettings = OverlaySettings.Builder()
        .setOverlayFrameAnchor(1f, -1f) // icon ka apna bottom-right point
        .setBackgroundFrameAnchor(
            1f - 2f * marginPx / videoWidth,
            -1f + 2f * marginPx / videoHeight
        )
        .build()

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = settings
}

/**
 * Text: screen par is fixed pattern mein ghoomta hai (har baar repeat):
 *   1) Top-Right    -> Bottom-Right   (seedha neeche)
 *   2) Bottom-Right -> Top-Left       (tirchha, upar left tak)
 *   3) Top-Left     -> Bottom-Left    (seedha neeche)
 *   4) Bottom-Left  -> Top-Right      (tirchha, upar right tak)
 * Phir wapas step 1 se.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class MovingTextOverlay(
    private val bitmap: Bitmap,
    videoWidth: Int,
    videoHeight: Int,
    marginPx: Int,
    private val opacity: Float,
    private val secondsPerSegment: Float = SECONDS_PER_SEGMENT
) : BitmapOverlay() {

    private val xs: FloatArray
    private val ys: FloatArray

    init {
        // Text ka aadha size (NDC mein) + margin, taaki text frame se bahar na jaye
        val halfW = bitmap.width.toFloat() / videoWidth
        val halfH = bitmap.height.toFloat() / videoHeight
        val marginX = 2f * marginPx / videoWidth
        val marginY = 2f * marginPx / videoHeight
        val xMax = (1f - halfW - marginX).coerceAtLeast(0f)
        val yMax = (1f - halfH - marginY).coerceAtLeast(0f)

        //            TR     BR      TL      BL      TR
        xs = floatArrayOf(xMax, xMax, -xMax, -xMax, xMax)
        ys = floatArrayOf(yMax, -yMax, yMax, -yMax, yMax)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val seconds = presentationTimeUs / 1_000_000.0
        val cycle = secondsPerSegment * 4.0
        val pos = (seconds % cycle) / secondsPerSegment // 0.0 .. 4.0
        val idx = pos.toInt().coerceIn(0, 3)
        val frac = (pos - idx).toFloat()

        val x = xs[idx] + (xs[idx + 1] - xs[idx]) * frac
        val y = ys[idx] + (ys[idx + 1] - ys[idx]) * frac

        return OverlaySettings.Builder()
            .setOverlayFrameAnchor(0f, 0f)     // text ka center
            .setBackgroundFrameAnchor(x, y)
            .setAlphaScale(opacity)            // user ki opacity
            .build()
    }

    companion object {
        /** Har ek side tak pahunchne mein itne second lagte hain. Speed badalni ho to ye badlo. */
        const val SECONDS_PER_SEGMENT = 4f
    }
}
