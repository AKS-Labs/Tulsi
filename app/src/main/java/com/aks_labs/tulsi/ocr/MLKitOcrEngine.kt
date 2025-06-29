package com.aks_labs.tulsi.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit OCR engine implementation (for Google Play builds)
 */
class MLKitOcrEngine : OcrEngine {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        private const val TAG = "MLKitOcrEngine"
        private const val OCR_TIMEOUT_MS = 3000L // 3 seconds timeout
    }
    
    override suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Starting ML Kit OCR processing")
            
            // Validate bitmap
            if (bitmap.isRecycled) {
                return OcrResult.Error("Bitmap was recycled before processing")
            }
            
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return OcrResult.Error("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            }
            
            // Create InputImage from bitmap
            val inputImage = try {
                InputImage.fromBitmap(bitmap, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create InputImage from bitmap", e)
                return OcrResult.Error("Failed to create InputImage: ${e.message}")
            }
            
            // Process with timeout
            val result = withTimeoutOrNull(OCR_TIMEOUT_MS) {
                suspendCancellableCoroutine<OcrResult> { continuation ->
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val processingTime = System.currentTimeMillis() - startTime
                            val extractedText = visionText.text
                            val textBlocks = visionText.textBlocks.size
                            val confidence = if (visionText.textBlocks.isNotEmpty()) {
                                visionText.textBlocks.map { it.confidence ?: 0.0f }.average().toFloat()
                            } else 0.0f
                            
                            Log.d(TAG, "ML Kit OCR completed successfully in ${processingTime}ms")
                            Log.d(TAG, "Extracted text length: ${extractedText.length}, blocks: $textBlocks, confidence: $confidence")
                            
                            val result = OcrResult.Success(
                                text = extractedText,
                                confidence = confidence,
                                textBlocksCount = textBlocks,
                                processingTimeMs = processingTime
                            )
                            continuation.resume(result)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "ML Kit OCR failed", exception)
                            continuation.resumeWithException(exception)
                        }
                }
            }
            
            result ?: OcrResult.Error("OCR processing timed out after ${OCR_TIMEOUT_MS}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit OCR processing failed", e)
            OcrResult.Error("OCR processing failed: ${e.message}")
        }
    }
    
    override fun cleanup() {
        try {
            textRecognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ML Kit text recognizer", e)
        }
    }
}
