package com.watermark.videoapp

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var store: SettingsStore
    private lateinit var processor: VideoProcessor

    private lateinit var ivIcon: ImageView
    private lateinit var etText: TextInputEditText
    private lateinit var slider: Slider
    private lateinit var tvOpacity: TextView
    private lateinit var btnIcon: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnVideo: MaterialButton
    private lateinit var groupProgress: View
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancel: MaterialButton
    private lateinit var groupResult: View
    private lateinit var videoView: VideoView
    private lateinit var btnShare: MaterialButton
    private lateinit var adContainer: FrameLayout

    private var adView: AdView? = null
    private var resultUri: Uri? = null
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            processor.progressPercent()?.let {
                progressBar.progress = it
                tvStatus.text = getString(R.string.processing_percent, it)
            }
            handler.postDelayed(this, 300)
        }
    }

    private val pickIcon =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                if (store.saveIconFromUri(uri)) showIcon()
                else toast(getString(R.string.icon_failed))
            }
        }

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) startProcessing(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = SettingsStore(this)
        processor = VideoProcessor(this)

        ivIcon = findViewById(R.id.ivIcon)
        etText = findViewById(R.id.etText)
        slider = findViewById(R.id.slOpacity)
        tvOpacity = findViewById(R.id.tvOpacity)
        btnIcon = findViewById(R.id.btnIcon)
        btnSave = findViewById(R.id.btnSave)
        btnVideo = findViewById(R.id.btnVideo)
        groupProgress = findViewById(R.id.groupProgress)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        btnCancel = findViewById(R.id.btnCancel)
        groupResult = findViewById(R.id.groupResult)
        videoView = findViewById(R.id.videoView)
        btnShare = findViewById(R.id.btnShare)
        adContainer = findViewById(R.id.adContainer)

        // Pehle se saved form wapas bhar do
        etText.setText(store.loadText())
        val saved = ((store.loadOpacityPercent() / 5f).roundToInt() * 5).coerceIn(10, 100)
        slider.value = saved.toFloat()
        updateOpacityLabel(saved)
        showIcon()

        slider.addOnChangeListener { _, value, _ -> updateOpacityLabel(value.toInt()) }

        btnIcon.setOnClickListener { pickIcon.launch("image/*") }

        btnSave.setOnClickListener {
            saveForm()
            toast(getString(R.string.saved))
        }

        btnVideo.setOnClickListener {
            saveForm()
            val hasText = !etText.text.isNullOrBlank()
            if (!store.hasIcon() && !hasText) {
                toast(getString(R.string.need_icon_or_text))
            } else {
                pickVideo.launch("video/*")
            }
        }

        btnCancel.setOnClickListener {
            processor.cancel()
            handler.removeCallbacks(progressRunnable)
            setBusy(false)
        }

        btnShare.setOnClickListener {
            val uri = resultUri ?: return@setOnClickListener
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, getString(R.string.share)))
        }

        setupBanner()
    }

    private fun updateOpacityLabel(percent: Int) {
        tvOpacity.text = getString(R.string.opacity_label, percent)
    }

    private fun showIcon() {
        val bmp = store.loadIcon()
        if (bmp != null) ivIcon.setImageBitmap(bmp)
        else ivIcon.setImageResource(android.R.drawable.ic_menu_gallery)
    }

    private fun saveForm() {
        store.save(etText.text?.toString()?.trim().orEmpty(), slider.value.toInt())
    }

    private fun setBusy(busy: Boolean) {
        groupProgress.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) groupResult.visibility = View.GONE
        btnIcon.isEnabled = !busy
        btnSave.isEnabled = !busy
        btnVideo.isEnabled = !busy
        if (busy) {
            progressBar.progress = 0
            tvStatus.text = getString(R.string.processing)
        }
    }

    private fun startProcessing(videoUri: Uri) {
        val text = etText.text?.toString()?.trim().orEmpty()
        val icon = store.loadIcon()
        val opacity = slider.value / 100f
        val output = File(cacheDir, "wm_output.mp4")

        setBusy(true)
        handler.post(progressRunnable)
        try {
            processor.start(videoUri, icon, text, opacity, output) { ok, error ->
                handler.removeCallbacks(progressRunnable)
                if (ok) {
                    saveAndShow(output)
                } else {
                    setBusy(false)
                    toast(getString(R.string.failed, error ?: ""))
                }
            }
        } catch (e: Exception) {
            handler.removeCallbacks(progressRunnable)
            setBusy(false)
            toast(getString(R.string.failed, e.message ?: ""))
        }
    }

    private fun saveAndShow(file: File) {
        Thread {
            val uri = saveToGallery(file)
            runOnUiThread {
                setBusy(false)
                if (uri == null) {
                    toast(getString(R.string.save_failed))
                } else {
                    showResult(uri)
                }
            }
        }.start()
    }

    /** Video ko Movies/WatermarkApp folder mein save karta hai (gallery mein dikhega). */
    private fun saveToGallery(file: File): Uri? {
        val name = "WM_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/WatermarkApp")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun showResult(uri: Uri) {
        resultUri = uri
        groupResult.visibility = View.VISIBLE
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { it.isLooping = true; videoView.start() }
    }

    // ---------------------------- BANNER AD ----------------------------

    private fun setupBanner() {
        MobileAds.initialize(this) {}
        val view = AdView(this)
        view.setAdSize(AdSize.BANNER)
        view.adUnitId = BuildConfig.ADMOB_BANNER_ID
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adContainer.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                // Offline ho to ad ki jagah chhupa do, app normal chalega
                adContainer.visibility = View.GONE
            }
        }
        adContainer.addView(view)
        adView = view
        view.loadAd(AdRequest.Builder().build())
    }

    override fun onPause() {
        adView?.pause()
        videoView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressRunnable)
        adView?.destroy()
        super.onDestroy()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
