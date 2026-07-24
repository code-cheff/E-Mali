package com.sambhav.plantdisease

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.Closeable

/** A single class prediction. */
data class Prediction(val label: String, val rawLabel: String, val confidence: Float)

/**
 * Loads the TensorFlow Lite plant-disease model from assets and runs on-device
 * inference. No network connection is required — everything happens on the phone.
 *
 * Note: the .tflite model already contains MobileNetV2's preprocess_input step
 * (pixel scaling to [-1, 1]) inside its graph, so here we only resize the image
 * and feed raw 0-255 float RGB values.
 */
class PlantClassifier(context: Context) : Closeable {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize: Int
    private val imageProcessor: ImageProcessor

    companion object {
        private const val MODEL_FILE = "plant_disease_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        const val CONFIDENCE_THRESHOLD = 0.60f  // below this, warn the user
    }

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_FILE)
        interpreter = Interpreter(model, Interpreter.Options().apply { numThreads = 4 })
        labels = FileUtil.loadLabels(context, LABELS_FILE)

        // Input tensor shape is [1, height, width, 3]; read the side length.
        inputSize = interpreter.getInputTensor(0).shape()[1]
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    /** Classify a leaf bitmap and return the top-3 predictions, highest first. */
    fun classify(bitmap: Bitmap): List<Prediction> {
        // 0. TensorImage.load() only accepts ARGB_8888 software bitmaps. Camera
        //    captures (and gallery images on Android 8+) can arrive as HARDWARE
        //    or other configs, which would crash, so normalise the config first.
        val safeBitmap =
            if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
            else bitmap.copy(Bitmap.Config.ARGB_8888, false)

        // 1. Wrap + resize the bitmap into a float tensor.
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(safeBitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Allocate the output buffer [1, numClasses] and run inference.
        val outputShape = interpreter.getOutputTensor(0).shape()
        val output = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
        interpreter.run(tensorImage.buffer, output.buffer.rewind())

        // 3. Map probabilities to labels and return the top 3.
        val probs = output.floatArray
        return probs.indices
            .map { Prediction(prettify(labels[it]), labels[it], probs[it]) }
            .sortedByDescending { it.confidence }
            .take(3)
    }

    /** "Tomato___Late_blight" -> "Tomato - Late blight" for display. */
    private fun prettify(raw: String): String =
        raw.replace("___", " - ").replace("_", " ").trim()

    override fun close() = interpreter.close()
}
