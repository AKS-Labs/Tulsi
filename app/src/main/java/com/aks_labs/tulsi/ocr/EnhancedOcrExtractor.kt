package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced OCR text extractor that provides detailed text block information
 * for text selection functionality
 */
object EnhancedOcrExtractor {
    
    private const val TAG = "EnhancedOcrExtractor"
    private const val OCR_TIMEOUT_MS = 30000L // 30 seconds timeout

    // Enhanced text recognizer with optimized options
    private val textRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder()
            .build()
    )

    // Image preprocessing constants
    private const val MIN_IMAGE_SIZE = 100
    private const val MAX_IMAGE_SIZE = 2048
    private const val OPTIMAL_IMAGE_SIZE = 1024
    
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
            val originalBitmap = loadBitmapFromUri(context, imageUri)
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to load bitmap from URI: $imageUri")
                return null
            }

            // Preprocess bitmap for better OCR accuracy
            val preprocessedBitmap = preprocessImageForOCR(originalBitmap)

            val result = extractSelectableTextFromBitmap(preprocessedBitmap)

            // Clean up bitmaps
            if (!originalBitmap.isRecycled && originalBitmap != preprocessedBitmap) {
                originalBitmap.recycle()
            }
            
            // Clean up preprocessed bitmap
            if (!preprocessedBitmap.isRecycled) {
                preprocessedBitmap.recycle()
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
        return bitmap.width >= MIN_IMAGE_SIZE && bitmap.height >= MIN_IMAGE_SIZE
    }

    /**
     * Preprocess image for better OCR accuracy
     * This includes resizing, contrast enhancement, and noise reduction
     */
    private fun preprocessImageForOCR(originalBitmap: Bitmap): Bitmap {
        try {
            Log.d(TAG, "Preprocessing image: ${originalBitmap.width}x${originalBitmap.height}")

            // Step 1: Resize image to optimal size for OCR
            val resizedBitmap = resizeImageForOCR(originalBitmap)

            // Step 2: Enhance contrast and brightness
            val enhancedBitmap = enhanceImageContrast(resizedBitmap)

            // Clean up intermediate bitmap if different
            if (resizedBitmap != originalBitmap && resizedBitmap != enhancedBitmap && !resizedBitmap.isRecycled) {
                resizedBitmap.recycle()
            }

            Log.d(TAG, "Image preprocessing completed: ${enhancedBitmap.width}x${enhancedBitmap.height}")
            return enhancedBitmap

        } catch (e: Exception) {
            Log.e(TAG, "Image preprocessing failed, using original", e)
            return originalBitmap
        }
    }

    /**
     * Resize image to optimal size for OCR processing
     */
    private fun resizeImageForOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // If image is already optimal size, return as-is
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE &&
            width >= MIN_IMAGE_SIZE && height >= MIN_IMAGE_SIZE) {
            return bitmap
        }

        // Calculate scale factor
        val scaleFactor = when {
            max(width, height) > MAX_IMAGE_SIZE -> {
                MAX_IMAGE_SIZE.toFloat() / max(width, height)
            }
            min(width, height) < MIN_IMAGE_SIZE -> {
                MIN_IMAGE_SIZE.toFloat() / min(width, height)
            }
            else -> {
                // Aim for optimal size
                OPTIMAL_IMAGE_SIZE.toFloat() / max(width, height)
            }
        }

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        Log.d(TAG, "Resizing image from ${width}x${height} to ${newWidth}x${newHeight} (scale: $scaleFactor)")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Enhance image contrast and brightness for better text recognition
     */
    private fun enhanceImageContrast(bitmap: Bitmap): Bitmap {
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint()

        // Create color matrix for contrast and brightness enhancement
        val colorMatrix = ColorMatrix()

        // Increase contrast (values > 1.0 increase contrast)
        val contrast = 1.2f
        // Adjust brightness (positive values brighten, negative darken)
        val brightness = 10f

        colorMatrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return enhancedBitmap
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
