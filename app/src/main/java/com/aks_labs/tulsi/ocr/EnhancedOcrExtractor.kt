package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream

/**
 * Enhanced OCR text extractor that provides detailed text block information
 * for text selection functionality
 */
object EnhancedOcrExtractor {
    
    private const val TAG = "EnhancedOcrExtractor"
    private const val OCR_TIMEOUT_MS = 30000L // 30 seconds timeout
    
    // Initialize the text recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extract detailed text information from image URI
     * @param context Android context
     * @param imageUri URI of the image to process
     * @return SelectableOcrResult with detailed text blocks or null if failed
     */
    suspend fun extractSelectableTextFromImage(
        context: Context, 
        imageUri: Uri
    ): SelectableOcrResult? {
        return try {
            Log.d(TAG, "Starting enhanced OCR for image: $imageUri")
            
            // Load bitmap from URI
            val bitmap = loadBitmapFromUri(context, imageUri)
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap from URI: $imageUri")
                return null
            }
            
            val result = extractSelectableTextFromBitmap(bitmap)
            
            // Clean up bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced OCR failed for image: $imageUri", e)
            null
        }
    }
    
    /**
     * Extract detailed text information from bitmap
     * @param bitmap Bitmap to process
     * @return SelectableOcrResult with detailed text blocks or null if failed
     */
    suspend fun extractSelectableTextFromBitmap(bitmap: Bitmap): SelectableOcrResult? {
        return try {
            Log.d(TAG, "Starting enhanced OCR for bitmap: ${bitmap.width}x${bitmap.height}")
            
            if (!isValidSize(bitmap)) {
                Log.w(TAG, "Bitmap size is too small for OCR")
                return SelectableOcrResult(
                    textBlocks = emptyList(),
                    fullText = "",
                    imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                )
            }
            
            val startTime = System.currentTimeMillis()
            
            // Create InputImage from bitmap
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Process image with ML Kit with timeout
            val visionText = withTimeoutOrNull(OCR_TIMEOUT_MS) {
                textRecognizer.process(inputImage).await()
            }
            
            if (visionText == null) {
                Log.e(TAG, "OCR processing timed out after ${OCR_TIMEOUT_MS}ms")
                return null
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            val imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
            
            // Convert to selectable OCR result
            val selectableResult = visionText.toSelectableOcrResult(imageSize)
                .copy(processingTimeMs = processingTime)
            
            Log.d(TAG, "Enhanced OCR completed successfully in ${processingTime}ms")
            Log.d(TAG, "Extracted ${selectableResult.textBlocks.size} text blocks, " +
                    "${selectableResult.fullText.length} characters total")
            
            selectableResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced OCR failed for bitmap", e)
            null
        }
    }
    
    /**
     * Load bitmap from URI with proper error handling
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
        }
    }
    
    /**
     * Check if bitmap has valid size for OCR processing
     */
    private fun isValidSize(bitmap: Bitmap): Boolean {
        val minSize = 50 // Minimum size in pixels
        return bitmap.width >= minSize && bitmap.height >= minSize
    }
    
    /**
     * Get OCR result for an image that's already been processed and stored in database
     * This method retrieves existing OCR data and converts it to selectable format
     */
    suspend fun getSelectableOcrResultFromDatabase(
        context: Context,
        mediaId: Long,
        imageUri: Uri,
        database: com.aks_labs.tulsi.database.MediaDatabase
    ): SelectableOcrResult? {
        return try {
            // Check if OCR data exists in database
            val existingOcrText = database.ocrTextDao().getOcrTextByMediaId(mediaId)
            
            if (existingOcrText != null && existingOcrText.extractedText.isNotBlank()) {
                Log.d(TAG, "Found existing OCR data for media $mediaId, re-processing for detailed blocks")
                
                // Re-process the image to get detailed text blocks
                // This is necessary because the database only stores the plain text
                extractSelectableTextFromImage(context, imageUri)
            } else {
                Log.d(TAG, "No existing OCR data found for media $mediaId, processing fresh")
                
                // Process the image fresh
                val result = extractSelectableTextFromImage(context, imageUri)
                
                // Save the basic text to database for search functionality
                if (result != null && result.fullText.isNotBlank()) {
                    val ocrEntity = com.aks_labs.tulsi.database.entities.OcrTextEntity(
                        mediaId = mediaId,
                        extractedText = result.fullText,
                        extractionTimestamp = System.currentTimeMillis() / 1000,
                        confidenceScore = 1.0f,
                        textBlocksCount = result.textBlocks.size,
                        processingTimeMs = result.processingTimeMs
                    )
                    
                    database.ocrTextDao().insertOcrText(ocrEntity)
                    Log.d(TAG, "Saved OCR text to database for media $mediaId")
                }
                
                result
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get selectable OCR result for media $mediaId", e)
            null
        }
    }
    
    /**
     * Check if an image has been processed for OCR
     */
    suspend fun isImageProcessedForSelection(
        mediaId: Long,
        database: com.aks_labs.tulsi.database.MediaDatabase
    ): Boolean {
        return try {
            val existingOcrText = database.ocrTextDao().getOcrTextByMediaId(mediaId)
            existingOcrText != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if image is processed for media $mediaId", e)
            false
        }
    }
}
