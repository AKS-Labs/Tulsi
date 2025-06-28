package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service class for extracting text from images using ML Kit OCR
 */
class OcrTextExtractor(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        private const val TAG = "OcrTextExtractor"
        private const val MAX_IMAGE_SIZE = 1024 // Max dimension for OCR processing
        private const val OCR_TIMEOUT_MS = 3000L // 3 seconds timeout for OCR processing
    }
    
    /**
     * Extract text from an image URI with enhanced device compatibility
     */
    suspend fun extractTextFromImage(imageUri: Uri): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()

            Log.d(TAG, "Starting OCR processing for image: $imageUri")

            // Load and optimize bitmap for OCR with fallback mechanisms
            val bitmap = loadOptimizedBitmap(imageUri)
                ?: return OcrResult.Error("Failed to load image from URI")

            Log.d(TAG, "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

            // Validate bitmap before processing
            if (bitmap.isRecycled) {
                return OcrResult.Error("Bitmap was recycled before processing")
            }

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return OcrResult.Error("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            }

            // Create InputImage for ML Kit with error handling
            val inputImage = try {
                InputImage.fromBitmap(bitmap, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create InputImage from bitmap", e)
                if (!bitmap.isRecycled) bitmap.recycle()
                return OcrResult.Error("Failed to create input image: ${e.message}")
            }

            // Perform OCR with timeout and enhanced error handling
            val result = try {
                performOcrWithTimeout(inputImage)
            } catch (e: Exception) {
                Log.e(TAG, "OCR processing failed", e)
                if (!bitmap.isRecycled) bitmap.recycle()
                return OcrResult.Error("OCR processing failed: ${e.message}")
            }

            val processingTime = System.currentTimeMillis() - startTime

            // Clean up bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            Log.d(TAG, "OCR completed successfully in ${processingTime}ms for image: $imageUri")
            Log.d(TAG, "Extracted text length: ${result.text.length}, blocks: ${result.textBlocks.size}")

            OcrResult.Success(
                extractedText = result.text,
                confidence = calculateAverageConfidence(result.textBlocks.map { 1.0f }), // ML Kit doesn't provide confidence per block
                textBlocksCount = result.textBlocks.size,
                processingTimeMs = processingTime
            )

        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for image: $imageUri", e)
            OcrResult.Error("OCR processing failed: ${e.message}")
        }
    }
    
    /**
     * Extract text from a bitmap directly with enhanced error handling
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()

            Log.d(TAG, "Starting OCR processing for bitmap: ${bitmap.width}x${bitmap.height}")

            // Validate input bitmap
            if (bitmap.isRecycled) {
                return OcrResult.Error("Input bitmap is recycled")
            }

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return OcrResult.Error("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            }

            // Optimize bitmap for OCR if needed with error handling
            val optimizedBitmap = try {
                optimizeBitmapForOcr(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to optimize bitmap for OCR", e)
                return OcrResult.Error("Failed to optimize bitmap: ${e.message}")
            }

            // Create InputImage for ML Kit with error handling
            val inputImage = try {
                InputImage.fromBitmap(optimizedBitmap, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create InputImage from bitmap", e)
                if (optimizedBitmap != bitmap && !optimizedBitmap.isRecycled) {
                    optimizedBitmap.recycle()
                }
                return OcrResult.Error("Failed to create input image: ${e.message}")
            }

            // Perform OCR with timeout and enhanced error handling
            val result = try {
                performOcrWithTimeout(inputImage)
            } catch (e: Exception) {
                Log.e(TAG, "OCR processing failed for bitmap", e)
                if (optimizedBitmap != bitmap && !optimizedBitmap.isRecycled) {
                    optimizedBitmap.recycle()
                }
                return OcrResult.Error("OCR processing failed: ${e.message}")
            }

            val processingTime = System.currentTimeMillis() - startTime

            // Clean up optimized bitmap if it's different from original
            if (optimizedBitmap != bitmap && !optimizedBitmap.isRecycled) {
                optimizedBitmap.recycle()
            }

            Log.d(TAG, "OCR completed successfully in ${processingTime}ms for bitmap")
            Log.d(TAG, "Extracted text length: ${result.text.length}, blocks: ${result.textBlocks.size}")

            OcrResult.Success(
                extractedText = result.text,
                confidence = calculateAverageConfidence(result.textBlocks.map { 1.0f }), // ML Kit doesn't provide confidence per block
                textBlocksCount = result.textBlocks.size,
                processingTimeMs = processingTime
            )

        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for bitmap", e)
            OcrResult.Error("OCR processing failed: ${e.message}")
        }
    }
    
    /**
     * Perform OCR using ML Kit Text Recognition with timeout
     */
    private suspend fun performOcrWithTimeout(inputImage: InputImage) = withTimeoutOrNull(OCR_TIMEOUT_MS) {
        performOcr(inputImage)
    } ?: throw Exception("OCR processing timed out after ${OCR_TIMEOUT_MS}ms")

    /**
     * Perform OCR using ML Kit Text Recognition
     */
    private suspend fun performOcr(inputImage: InputImage) = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
    
    /**
     * Load and optimize bitmap from URI for OCR processing with enhanced compatibility
     */
    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        return try {
            // Try multiple approaches for better device compatibility
            loadBitmapWithFallback(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
        }
    }

    /**
     * Load bitmap with fallback mechanisms for better device compatibility
     */
    private fun loadBitmapWithFallback(uri: Uri): Bitmap? {
        // First attempt: Standard approach with sample size
        try {
            return loadBitmapStandard(uri)
        } catch (e: Exception) {
            Log.w(TAG, "Standard bitmap loading failed, trying fallback approach", e)
        }

        // Second attempt: Fallback with different config
        try {
            return loadBitmapFallback(uri)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback bitmap loading failed, trying minimal approach", e)
        }

        // Third attempt: Minimal approach for problematic devices
        try {
            return loadBitmapMinimal(uri)
        } catch (e: Exception) {
            Log.e(TAG, "All bitmap loading approaches failed", e)
            return null
        }
    }

    /**
     * Standard bitmap loading approach
     */
    private fun loadBitmapStandard(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // First, get image dimensions without loading the full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            // Calculate sample size to reduce memory usage
            val sampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)

            // Load the bitmap with sample size
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                    inTempStorage = ByteArray(16 * 1024) // 16KB temp storage
                }
                BitmapFactory.decodeStream(stream, null, loadOptions)
            }
        }
    }

    /**
     * Fallback bitmap loading with ARGB_8888 config
     */
    private fun loadBitmapFallback(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            // Use larger sample size for problematic devices
            val sampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE / 2, MAX_IMAGE_SIZE / 2)

            context.contentResolver.openInputStream(uri)?.use { stream ->
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = maxOf(sampleSize, 2) // Ensure at least 2x downsampling
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inDither = false
                    inPurgeable = true
                    inInputShareable = true
                }
                BitmapFactory.decodeStream(stream, null, loadOptions)
            }
        }
    }

    /**
     * Minimal bitmap loading for very problematic devices
     */
    private fun loadBitmapMinimal(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Heavy downsampling
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = false
                inScaled = false
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }
    
    /**
     * Optimize bitmap for OCR processing
     */
    private fun optimizeBitmapForOcr(bitmap: Bitmap): Bitmap {
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        
        return if (maxDimension > MAX_IMAGE_SIZE) {
            val scale = MAX_IMAGE_SIZE.toFloat() / maxDimension
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Calculate sample size for bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Calculate average confidence score
     */
    private fun calculateAverageConfidence(confidences: List<Float>): Float {
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0.0f
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        textRecognizer.close()
    }
}

/**
 * Sealed class representing OCR operation results
 */
sealed class OcrResult {
    data class Success(
        val extractedText: String,
        val confidence: Float,
        val textBlocksCount: Int,
        val processingTimeMs: Long
    ) : OcrResult()
    
    data class Error(val message: String) : OcrResult()
}
