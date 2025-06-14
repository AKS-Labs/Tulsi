package com.aks_labs.tulsi.ocr

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.room.Room
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Simple OCR service for processing images in background
 * Based on ScreenshotGo's approach - simple and effective
 */
class SimpleOcrService(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleOcrService"
        private const val BATCH_SIZE = 10
        
        @Volatile
        private var INSTANCE: SimpleOcrService? = null
        
        fun getInstance(context: Context): SimpleOcrService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimpleOcrService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val database by lazy {
        Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(
                Migration3to4(context),
                Migration4to5(context),
                Migration5to6(context),
                Migration6to7(context)
            )
        }.build()
    }
    
    private var isProcessing = false
    
    /**
     * Start OCR processing for all unprocessed images
     */
    fun startOcrProcessing() {
        if (isProcessing) {
            Log.d(TAG, "OCR processing already in progress")
            return
        }
        
        Log.d(TAG, "Starting OCR processing...")
        isProcessing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processAllImages()
            } catch (e: Exception) {
                Log.e(TAG, "OCR processing failed", e)
            } finally {
                isProcessing = false
            }
        }
    }
    
    /**
     * Process a single image immediately
     */
    fun processImage(mediaId: Long, imageUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MLKitTextHelper.processImageAndSaveText(
                    context = context,
                    mediaId = mediaId,
                    imageUri = imageUri,
                    database = database
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process single image $mediaId", e)
            }
        }
    }
    
    /**
     * Get all unprocessed images and process them
     */
    private suspend fun processAllImages() {
        Log.d(TAG, "Getting unprocessed images...")
        
        // Get already processed media IDs
        val processedIds = try {
            database.ocrTextDao().getAllProcessedMediaIds().toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get processed IDs, assuming none processed", e)
            emptySet<Long>()
        }
        
        Log.d(TAG, "Found ${processedIds.size} already processed images")
        
        // Get all images from MediaStore
        val allImages = getAllImagesFromMediaStore()
        Log.d(TAG, "Found ${allImages.size} total images in MediaStore")
        
        // Filter unprocessed images
        val unprocessedImages = allImages.filter { !processedIds.contains(it.mediaId) }
        Log.d(TAG, "Found ${unprocessedImages.size} unprocessed images")
        
        if (unprocessedImages.isEmpty()) {
            Log.d(TAG, "No images to process")
            return
        }
        
        // Process in batches
        val batches = unprocessedImages.chunked(BATCH_SIZE)
        Log.d(TAG, "Processing ${batches.size} batches of up to $BATCH_SIZE images each")
        
        for ((batchIndex, batch) in batches.withIndex()) {
            Log.d(TAG, "Processing batch ${batchIndex + 1}/${batches.size}")
            
            MLKitTextHelper.batchProcessImages(
                context = context,
                imageInfoList = batch,
                database = database,
                onProgress = { processed, total ->
                    Log.d(TAG, "Batch ${batchIndex + 1} progress: $processed/$total")
                }
            )
        }
        
        Log.d(TAG, "OCR processing completed for all images")
    }
    
    /**
     * Get all images from MediaStore
     */
    private fun getAllImagesFromMediaStore(): List<MLKitTextHelper.ImageInfo> {
        val images = mutableListOf<MLKitTextHelper.ImageInfo>()
        
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME
                ),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    
                    images.add(MLKitTextHelper.ImageInfo(id, uri, name))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get images from MediaStore", e)
        }
        
        return images
    }
    
    /**
     * Search images by OCR text
     * Based on ScreenshotGo's search approach with FTS and fallback
     */
    suspend fun searchImagesByText(query: String): List<Long> {
        return try {
            if (query.isBlank()) {
                emptyList()
            } else {
                Log.d(TAG, "Searching for: '$query'")

                val results = mutableSetOf<Long>()

                // Use LIKE search (FTS temporarily disabled)
                try {
                    // Strategy 1: Exact phrase search
                    val exactResults = database.ocrTextDao().searchOcrTextFallback(query)
                    results.addAll(exactResults.map { it.mediaId })
                    Log.d(TAG, "Exact search found ${exactResults.size} results")
                } catch (e: Exception) {
                    Log.w(TAG, "Exact search failed: ${e.message}")
                }

                // Additional word-based search for better recall
                val words = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (words.size > 1) {
                    for (word in words) {
                        if (word.length >= 2) {
                            try {
                                val wordResults = database.ocrTextDao().searchOcrTextFallback(word)
                                results.addAll(wordResults.map { it.mediaId })
                            } catch (e: Exception) {
                                Log.w(TAG, "Word search failed for '$word': ${e.message}")
                            }
                        }
                    }
                }

                val mediaIds = results.toList()
                Log.d(TAG, "Found ${mediaIds.size} total images matching '$query'")
                mediaIds
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: $query", e)
            emptyList()
        }
    }

    /**
     * Process query for FTS search (temporarily disabled)
     * Based on ScreenshotGo's processQuery method
     */
    /*
    private fun processQueryForFts(query: String): String {
        // For FTS, we add wildcards to each word for prefix matching
        return query.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
    }
    */
    
    /**
     * Get OCR text for a specific image
     */
    suspend fun getOcrTextForImage(mediaId: Long): String? {
        return try {
            database.ocrTextDao().getOcrTextByMediaId(mediaId)?.extractedText
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get OCR text for media $mediaId", e)
            null
        }
    }
    
    /**
     * Check if an image has been processed
     */
    suspend fun isImageProcessed(mediaId: Long): Boolean {
        return try {
            database.ocrTextDao().getOcrTextByMediaId(mediaId) != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if image $mediaId is processed", e)
            false
        }
    }
    
    /**
     * Get processing statistics
     */
    suspend fun getProcessingStats(): ProcessingStats {
        return try {
            val totalProcessed = database.ocrTextDao().getOcrTextCount()
            val totalImages = getAllImagesFromMediaStore().size
            
            ProcessingStats(
                totalImages = totalImages,
                processedImages = totalProcessed,
                isComplete = totalProcessed >= totalImages
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get processing stats", e)
            ProcessingStats(0, 0, false)
        }
    }
    
    data class ProcessingStats(
        val totalImages: Int,
        val processedImages: Int,
        val isComplete: Boolean
    )
}
