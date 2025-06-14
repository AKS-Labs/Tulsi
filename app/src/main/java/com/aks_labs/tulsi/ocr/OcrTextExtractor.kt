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
    }
    
    /**
     * Extract text from an image URI
     */
    suspend fun extractTextFromImage(imageUri: Uri): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Load and optimize bitmap for OCR
            val bitmap = loadOptimizedBitmap(imageUri)
                ?: return OcrResult.Error("Failed to load image")
            
            // Create InputImage for ML Kit
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Perform OCR
            val result = performOcr(inputImage)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Clean up bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            
            Log.d(TAG, "OCR completed in ${processingTime}ms for image: $imageUri")
            
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
     * Extract text from a bitmap directly
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Optimize bitmap for OCR if needed
            val optimizedBitmap = optimizeBitmapForOcr(bitmap)
            
            // Create InputImage for ML Kit
            val inputImage = InputImage.fromBitmap(optimizedBitmap, 0)
            
            // Perform OCR
            val result = performOcr(inputImage)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Clean up optimized bitmap if it's different from original
            if (optimizedBitmap != bitmap && !optimizedBitmap.isRecycled) {
                optimizedBitmap.recycle()
            }
            
            Log.d(TAG, "OCR completed in ${processingTime}ms for bitmap")
            
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
     * Load and optimize bitmap from URI for OCR processing
     */
    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
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
                    }
                    BitmapFactory.decodeStream(stream, null, loadOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
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
