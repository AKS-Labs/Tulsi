package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.InputStream

/**
 * Helper class for extracting text from images using ML Kit Text Recognition
 * Based on ScreenshotGo's FirebaseVisionTextHelper approach
 */
object MLKitTextHelper {
    
    private const val TAG = "MLKitTextHelper"
    
    // Initialize the text recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extract text from image URI
     * @param context Android context
     * @param imageUri URI of the image to process
     * @return Extracted text or null if failed
     */
    suspend fun extractTextFromImage(context: Context, imageUri: Uri): String? {
        return try {
            Log.d(TAG, "Starting OCR for image: $imageUri")
            
            // Load bitmap from URI
            val bitmap = loadBitmapFromUri(context, imageUri)
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap from URI: $imageUri")
                return null
            }
            
            // Check if bitmap is valid size
            if (!isValidSize(bitmap)) {
                Log.w(TAG, "Bitmap size is too small for OCR: ${bitmap.width}x${bitmap.height}")
                return ""
            }
            
            // Create InputImage from bitmap
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Process image with ML Kit
            val visionText = textRecognizer.process(inputImage).await()

            val extractedText = visionText.text
            Log.d(TAG, "OCR completed. Extracted ${extractedText.length} characters")
            
            return extractedText
            
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for image: $imageUri", e)
            null
        }
    }
    
    /**
     * Extract text from bitmap directly
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String? {
        return try {
            Log.d(TAG, "Starting OCR for bitmap: ${bitmap.width}x${bitmap.height}")
            
            if (!isValidSize(bitmap)) {
                Log.w(TAG, "Bitmap size is too small for OCR")
                return ""
            }
            
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = textRecognizer.process(inputImage).await()

            val extractedText = visionText.text
            Log.d(TAG, "OCR completed. Extracted ${extractedText.length} characters")
            
            return extractedText
            
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for bitmap", e)
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
     * Check if bitmap is valid size for OCR processing
     * Based on ScreenshotGo's validation logic
     */
    private fun isValidSize(bitmap: Bitmap): Boolean {
        val minSize = 50 // Minimum dimension in pixels
        return bitmap.width >= minSize && bitmap.height >= minSize
    }
    
    /**
     * Process OCR for a media item and save to database
     * @param context Android context
     * @param mediaId Media ID from MediaStore
     * @param imageUri URI of the image
     * @param database Database instance
     */
    suspend fun processImageAndSaveText(
        context: Context,
        mediaId: Long,
        imageUri: Uri,
        database: com.aks_labs.tulsi.database.MediaDatabase
    ): Boolean {
        return try {
            Log.d(TAG, "Processing image for media ID: $mediaId")
            
            // Check if already processed
            val existingText = database.ocrTextDao().getOcrTextByMediaId(mediaId)
            if (existingText != null) {
                Log.d(TAG, "Image $mediaId already processed, skipping")
                return true
            }
            
            // Extract text
            val extractedText = extractTextFromImage(context, imageUri)
            if (extractedText == null) {
                Log.e(TAG, "Failed to extract text from image $mediaId")
                return false
            }
            
            // Save to database
            val ocrEntity = com.aks_labs.tulsi.database.entities.OcrTextEntity(
                mediaId = mediaId,
                extractedText = extractedText,
                extractionTimestamp = System.currentTimeMillis() / 1000,
                confidenceScore = 1.0f, // ML Kit doesn't provide confidence scores
                textBlocksCount = if (extractedText.isBlank()) 0 else 1,
                processingTimeMs = 0L // We don't track processing time in this simple approach
            )
            
            database.ocrTextDao().insertOcrText(ocrEntity)
            Log.d(TAG, "Saved OCR text for media $mediaId: ${extractedText.length} characters")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process and save OCR text for media $mediaId", e)
            false
        }
    }
    
    /**
     * Batch process multiple images
     * @param context Android context
     * @param imageInfoList List of images to process
     * @param database Database instance
     * @param onProgress Callback for progress updates (processed, total)
     */
    suspend fun batchProcessImages(
        context: Context,
        imageInfoList: List<ImageInfo>,
        database: com.aks_labs.tulsi.database.MediaDatabase,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        Log.d(TAG, "Starting batch processing of ${imageInfoList.size} images")
        
        var processedCount = 0
        for ((index, imageInfo) in imageInfoList.withIndex()) {
            val success = processImageAndSaveText(
                context = context,
                mediaId = imageInfo.mediaId,
                imageUri = imageInfo.uri,
                database = database
            )
            
            if (success) {
                processedCount++
            }
            
            // Report progress
            onProgress?.invoke(processedCount, imageInfoList.size)
            
            Log.d(TAG, "Processed ${index + 1}/${imageInfoList.size} images")
        }
        
        Log.d(TAG, "Batch processing completed: $processedCount/${imageInfoList.size} successful")
    }
    
    /**
     * Data class for image information
     */
    data class ImageInfo(
        val mediaId: Long,
        val uri: Uri,
        val displayName: String? = null
    )
}
