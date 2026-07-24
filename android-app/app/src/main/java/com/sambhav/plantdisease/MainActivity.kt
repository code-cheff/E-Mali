package com.sambhav.plantdisease

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sambhav.plantdisease.databinding.ActivityMainBinding
import com.sambhav.plantdisease.databinding.ItemPredictionBinding
import com.sambhav.plantdisease.databinding.ItemHistoryBinding
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Single-screen app: pick a leaf photo (camera/gallery) -> classify on-device ->
 * show a rich result card (disease, confidence, description, treatment, top-3)
 * plus a session scan history. Inference runs off the UI thread.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var classifier: PlantClassifier

    // Run inference off the main thread so the UI stays responsive.
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Cached for the Share action.
    private var lastTop: Prediction? = null
    private var lastInfo: DiseaseInfo.Info? = null

    private val MAX_HISTORY = 5

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            bitmap?.let { analyze(it) }
        }

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { analyze(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            classifier = PlantClassifier(this)
        } catch (e: Exception) {
            Log.e("PlantDisease", "Model load failed", e)
            Toast.makeText(this, "Model not found. Run train.py and rebuild.", Toast.LENGTH_LONG).show()
        }

        binding.btnCamera.setOnClickListener { takePhoto.launch(null) }
        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.btnShare.setOnClickListener { shareResult() }
        binding.btnScanAnother.setOnClickListener { resetToStart() }
    }

    /** Show the image, then classify it on a background thread. */
    private fun analyze(bitmap: Bitmap) {
        binding.imagePreview.setImageBitmap(bitmap)
        binding.imagePreview.visibility = View.VISIBLE
        binding.placeholderGroup.visibility = View.GONE
        binding.resultCard.visibility = View.GONE

        if (!::classifier.isInitialized) {
            Toast.makeText(this, "Model not loaded.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingGroup.visibility = View.VISIBLE
        executor.execute {
            try {
                val start = SystemClock.elapsedRealtime()
                val results = classifier.classify(bitmap)
                val elapsed = SystemClock.elapsedRealtime() - start
                mainHandler.post { showResult(bitmap, results, elapsed) }
            } catch (e: Exception) {
                Log.e("PlantDisease", "Classification failed", e)
                mainHandler.post {
                    binding.loadingGroup.visibility = View.GONE
                    Toast.makeText(this, "Could not analyse this image: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showResult(bitmap: Bitmap, results: List<Prediction>, elapsedMs: Long) {
        binding.loadingGroup.visibility = View.GONE
        val top = results.first()
        val info = DiseaseInfo.lookup(top.rawLabel)
        lastTop = top
        lastInfo = info

        val healthyColor = ContextCompat.getColor(this, R.color.healthy)
        val dangerColor = ContextCompat.getColor(this, R.color.danger)
        val warnColor = ContextCompat.getColor(this, R.color.warn)
        val uncertain = top.confidence < PlantClassifier.CONFIDENCE_THRESHOLD
        val statusColor = if (info.healthy) healthyColor else dangerColor

        // Status chip
        binding.statusChip.text = if (info.healthy) "Healthy" else "Diseased"
        binding.statusChip.chipBackgroundColor = ColorStateList.valueOf(statusColor)
        binding.statusChip.setChipIconResource(if (info.healthy) R.drawable.ic_eco else R.drawable.ic_warning)
        binding.statusChip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))

        // Heading — show the disease only (no crop/plant name).
        binding.diseaseName.text = if (info.healthy) "Healthy" else info.condition

        // Confidence
        val pct = top.confidence * 100f
        binding.confidencePct.text = String.format(Locale.US, "Confidence: %.1f%%", pct)
        binding.confidenceBar.setIndicatorColor(if (uncertain) warnColor else healthyColor)
        binding.confidenceBar.setProgressCompat(pct.toInt(), true)
        binding.inferenceTime.text =
            String.format(Locale.US, "Analyzed on-device in %d ms", elapsedMs)

        binding.warningGroup.visibility = if (uncertain) View.VISIBLE else View.GONE

        // Knowledge base — show bundled offline advice immediately...
        binding.descText.text = info.description
        binding.treatmentText.text = info.treatment

        // ...then optionally enrich it with AI advice from OpenRouter.
        if (RecommendationService.isConfigured()) {
            binding.aiSource.visibility = View.VISIBLE
            binding.aiSource.text = "✦ Getting AI-tailored advice…"
            RecommendationService.fetch(info) { advice ->
                mainHandler.post {
                    // Ignore if the user has already scanned something newer.
                    if (lastTop !== top) return@post
                    if (advice != null) {
                        binding.treatmentText.text = advice
                        binding.aiSource.text = "✦ AI advice via OpenRouter"
                    } else {
                        binding.aiSource.text = "✦ Offline advice (AI unavailable)"
                    }
                }
            }
        } else {
            binding.aiSource.visibility = View.GONE
        }

        // Top-3 bars
        binding.top3Container.removeAllViews()
        results.forEach { p ->
            val row = ItemPredictionBinding.inflate(layoutInflater, binding.top3Container, false)
            row.predLabel.text = DiseaseInfo.conditionLabel(p.rawLabel)
            val pPct = p.confidence * 100f
            row.predPct.text = String.format(Locale.US, "%.1f%%", pPct)
            row.predBar.setIndicatorColor(ContextCompat.getColor(this, R.color.green))
            row.predBar.setProgressCompat(pPct.toInt(), true)
            binding.top3Container.addView(row.root)
        }

        binding.resultCard.visibility = View.VISIBLE
        addToHistory(bitmap, top, info)
    }

    /** Prepend a small thumbnail entry to the session history (capped). */
    private fun addToHistory(bitmap: Bitmap, top: Prediction, info: DiseaseInfo.Info) {
        val item = ItemHistoryBinding.inflate(layoutInflater, binding.historyContainer, false)
        val thumb = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        item.histThumb.setImageBitmap(thumb)
        item.histLabel.text = DiseaseInfo.conditionLabel(top.rawLabel)
        item.histConfidence.text = String.format(
            Locale.US, "%s · %.1f%%", if (info.healthy) "Healthy" else "Diseased", top.confidence * 100f
        )
        binding.historyContainer.addView(item.root, 0)
        while (binding.historyContainer.childCount > MAX_HISTORY) {
            binding.historyContainer.removeViewAt(binding.historyContainer.childCount - 1)
        }
        binding.historyHeading.visibility = View.VISIBLE
    }

    /** Reset the preview area so the user can scan a new leaf (history is kept). */
    private fun resetToStart() {
        binding.resultCard.visibility = View.GONE
        binding.imagePreview.visibility = View.GONE
        binding.imagePreview.setImageDrawable(null)
        binding.placeholderGroup.visibility = View.VISIBLE
    }

    private fun shareResult() {
        val top = lastTop ?: return
        val info = lastInfo ?: return
        val text = buildString {
            append("🌿 E-Mali — Plant Disease Detection\n\n")
            append("Result: ${if (info.healthy) "Healthy" else info.condition}\n")
            append(String.format(Locale.US, "Confidence: %.1f%%\n\n", top.confidence * 100f))
            append("About: ${info.description}\n\n")
            // Share whatever advice is currently displayed (AI-enriched or offline).
            append("Recommended action: ${binding.treatmentText.text}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share result"))
    }

    override fun onDestroy() {
        executor.shutdown()
        if (::classifier.isInitialized) classifier.close()
        super.onDestroy()
    }
}
