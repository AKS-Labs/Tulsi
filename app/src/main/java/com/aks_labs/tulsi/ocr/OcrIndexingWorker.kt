package com.aks_labs.tulsi.ocr

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.workDataOf
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import com.aks_labs.tulsi.database.entities.OcrTextEntity
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import android.provider.MediaStore
import android.net.Uri
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for background OCR text extraction and indexing
 */
class OcrIndexingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "OcrIndexingWorker"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_MEDIA_URI = "media_uri"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL_PROCESSED = "total_processed"
        const val KEY_ERRORS = "errors"
        
        const val DEFAULT_BATCH_SIZE = 10
    }
    
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(
                Migration3to4(applicationContext),
                Migration4to5(applicationContext),
                Migration5to6(applicationContext),
                Migration6to7(applicationContext)
            )
        }.build()
    }
    
    private val ocrExtractor by lazy {
        OcrTextExtractor(applicationContext)
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val mediaId = inputData.getLong(KEY_MEDIA_ID, -1L)
            val mediaUri = inputData.getString(KEY_MEDIA_URI)
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
            
            return@withContext when {
                mediaId != -1L && mediaUri != null -> {
                    // Process single image
                    processSingleImage(mediaId, mediaUri)
                }
                else -> {
                    // Process batch of unprocessed images
                    processBatchImages(batchSize)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR indexing worker failed", e)
            Result.failure(workDataOf(KEY_ERRORS to e.message))
        } finally {
            ocrExtractor.cleanup()
        }
    }
    
    /**
     * Process a single image for OCR
     */
    private suspend fun processSingleImage(mediaId: Long, mediaUri: String): Result {
        return try {
            Log.d(TAG, "Processing single image: $mediaId")
            
            // Check if already processed
            val existingOcr = database.ocrTextDao().getOcrTextByMediaId(mediaId)
            if (existingOcr != null) {
                Log.d(TAG, "Image $mediaId already processed, skipping")
                return Result.success()
            }
            
            // Extract text from image
            val uri = android.net.Uri.parse(mediaUri)
            val ocrResult = ocrExtractor.extractTextFromImage(uri)
            
            when (ocrResult) {
                is OcrResult.Success -> {
                    // Save OCR result to database
                    val ocrEntity = OcrTextEntity(
                        mediaId = mediaId,
                        extractedText = ocrResult.extractedText,
                        extractionTimestamp = System.currentTimeMillis() / 1000,
                        confidenceScore = ocrResult.confidence,
                        textBlocksCount = ocrResult.textBlocksCount,
                        processingTimeMs = ocrResult.processingTimeMs
                    )
                    
                    database.ocrTextDao().insertOcrText(ocrEntity)
                    
                    Log.d(TAG, "Successfully processed image $mediaId: ${ocrResult.extractedText.length} characters extracted")
                    
                    Result.success(workDataOf(
                        KEY_TOTAL_PROCESSED to 1,
                        KEY_PROGRESS to "Processed image $mediaId"
                    ))
                }
                is OcrResult.Error -> {
                    Log.e(TAG, "OCR failed for image $mediaId: ${ocrResult.message}")
                    Result.failure(workDataOf(KEY_ERRORS to ocrResult.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image $mediaId", e)
            Result.failure(workDataOf(KEY_ERRORS to e.message))
        }
    }
    
    /**
     * Process a batch of unprocessed images
     */
    private suspend fun processBatchImages(batchSize: Int): Result {
        return try {
            Log.d(TAG, "Processing batch of $batchSize images")

            // Get list of already processed media IDs
            val processedMediaIds = try {
                database.ocrTextDao().getAllProcessedMediaIds().toSet()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get processed media IDs, assuming none processed", e)
                emptySet<Long>()
            }
            Log.d(TAG, "Found ${processedMediaIds.size} already processed images")

            // Get total images available
            val totalAvailable = getTotalImageCount()
            Log.d(TAG, "Total images available in MediaStore: $totalAvailable")

            // Get unprocessed images from MediaStore
            val unprocessedImages = getUnprocessedImages(processedMediaIds, batchSize)
            Log.d(TAG, "Found ${unprocessedImages.size} unprocessed images to process")

            if (unprocessedImages.isEmpty()) {
                Log.d(TAG, "No unprocessed images found using normal method")

                // Fallback: try to get some images anyway for debugging
                if (processedMediaIds.isEmpty() && totalAvailable > 0) {
                    Log.d(TAG, "No processed images in database but images exist - trying fallback method")
                    val fallbackImages = getUnprocessedImages(emptySet(), minOf(batchSize, 3))
                    if (fallbackImages.isNotEmpty()) {
                        Log.d(TAG, "Fallback found ${fallbackImages.size} images to process")
                        // Continue with fallback images
                        return processFallbackImages(fallbackImages)
                    }
                }

                Log.d(TAG, "Truly no images to process")
                return Result.success(workDataOf(
                    KEY_TOTAL_PROCESSED to 0,
                    KEY_PROGRESS to "No images to process"
                ))
            }

            var processedCount = 0
            var errorCount = 0

            // Update progress tracking
            updateProgressInDatabase(0, unprocessedImages.size, true)

            for ((index, imageInfo) in unprocessedImages.withIndex()) {
                try {
                    Log.d(TAG, "Processing image ${index + 1}/${unprocessedImages.size}: ${imageInfo.id}")

                    // Check if already processed (double-check)
                    val existingOcr = database.ocrTextDao().getOcrTextByMediaId(imageInfo.id)
                    if (existingOcr != null) {
                        Log.d(TAG, "Image ${imageInfo.id} already processed, skipping")
                        processedCount++
                        continue
                    }

                    // Extract text from image
                    val ocrResult = ocrExtractor.extractTextFromImage(imageInfo.uri)

                    when (ocrResult) {
                        is OcrResult.Success -> {
                            // Save OCR result to database
                            val ocrEntity = OcrTextEntity(
                                mediaId = imageInfo.id,
                                extractedText = ocrResult.extractedText,
                                extractionTimestamp = System.currentTimeMillis() / 1000,
                                confidenceScore = ocrResult.confidence,
                                textBlocksCount = ocrResult.textBlocksCount,
                                processingTimeMs = ocrResult.processingTimeMs
                            )

                            database.ocrTextDao().insertOcrText(ocrEntity)
                            processedCount++

                            Log.d(TAG, "Successfully processed image ${imageInfo.id}: ${ocrResult.extractedText.length} characters extracted")
                        }
                        is OcrResult.Error -> {
                            Log.e(TAG, "OCR failed for image ${imageInfo.id}: ${ocrResult.message}")
                            errorCount++
                        }
                    }

                    // Update progress
                    updateProgressInDatabase(processedCount, unprocessedImages.size, true)
                    updateProgress(processedCount, unprocessedImages.size, "Image ${imageInfo.id}")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process image ${imageInfo.id}", e)
                    errorCount++
                }
            }

            // Mark processing as complete if we processed all available images
            val totalImages = getTotalImageCount()
            val totalProcessed = database.ocrTextDao().getAllProcessedMediaIds().size
            if (totalProcessed >= totalImages) {
                updateProgressInDatabase(totalProcessed, totalImages, false)
                Log.d(TAG, "All images processed! Total: $totalProcessed")
            }

            Log.d(TAG, "Batch processing completed: $processedCount processed, $errorCount errors")

            Result.success(workDataOf(
                KEY_TOTAL_PROCESSED to processedCount,
                KEY_PROGRESS to "Processed $processedCount images with $errorCount errors"
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process batch", e)
            Result.failure(workDataOf(KEY_ERRORS to e.message))
        }
    }

    /**
     * Get unprocessed images from MediaStore
     */
    private fun getUnprocessedImages(processedIds: Set<Long>, batchSize: Int): List<ImageInfo> {
        val unprocessedImages = mutableListOf<ImageInfo>()

        try {
            Log.d(TAG, "Querying MediaStore for images...")
            val cursor = applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
                ),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                Log.d(TAG, "MediaStore cursor has ${it.count} total images")
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                var checkedCount = 0
                while (it.moveToNext() && unprocessedImages.size < batchSize) {
                    val id = it.getLong(idColumn)
                    checkedCount++

                    if (!processedIds.contains(id)) {
                        val name = it.getString(nameColumn) ?: "unknown"
                        val path = it.getString(pathColumn) ?: ""
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

                        unprocessedImages.add(ImageInfo(id, name, path, uri))
                        Log.d(TAG, "Added unprocessed image: $id ($name)")
                    } else {
                        Log.v(TAG, "Skipping already processed image: $id")
                    }
                }
                Log.d(TAG, "Checked $checkedCount images, found ${unprocessedImages.size} unprocessed")
            } ?: run {
                Log.w(TAG, "MediaStore cursor is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unprocessed images", e)
        }

        return unprocessedImages
    }

    /**
     * Process fallback images for debugging
     */
    private suspend fun processFallbackImages(images: List<ImageInfo>): Result {
        Log.d(TAG, "Processing ${images.size} fallback images")

        var processedCount = 0
        for ((index, imageInfo) in images.withIndex()) {
            try {
                Log.d(TAG, "Processing fallback image ${index + 1}/${images.size}: ${imageInfo.id}")

                // Extract text from image
                val ocrResult = ocrExtractor.extractTextFromImage(imageInfo.uri)

                when (ocrResult) {
                    is OcrResult.Success -> {
                        // Save OCR result to database
                        val ocrEntity = OcrTextEntity(
                            mediaId = imageInfo.id,
                            extractedText = ocrResult.extractedText,
                            extractionTimestamp = System.currentTimeMillis() / 1000,
                            confidenceScore = ocrResult.confidence,
                            textBlocksCount = ocrResult.textBlocksCount,
                            processingTimeMs = ocrResult.processingTimeMs
                        )

                        database.ocrTextDao().insertOcrText(ocrEntity)
                        processedCount++

                        Log.d(TAG, "Successfully processed fallback image ${imageInfo.id}: ${ocrResult.extractedText.length} characters extracted")
                    }
                    is OcrResult.Error -> {
                        Log.e(TAG, "OCR failed for fallback image ${imageInfo.id}: ${ocrResult.message}")
                    }
                }

                // Update progress
                updateProgressInDatabase(processedCount, images.size, true)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to process fallback image ${imageInfo.id}", e)
            }
        }

        return Result.success(workDataOf(
            KEY_TOTAL_PROCESSED to processedCount,
            KEY_PROGRESS to "Processed $processedCount fallback images"
        ))
    }

    /**
     * Get total number of images in MediaStore
     */
    private fun getTotalImageCount(): Int {
        return try {
            val cursor = applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )
            cursor?.use { it.count } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total image count", e)
            0
        }
    }

    /**
     * Update progress in database
     */
    private suspend fun updateProgressInDatabase(processed: Int, total: Int, isProcessing: Boolean) {
        try {
            val currentProgress = database.ocrProgressDao().getProgress()
            if (currentProgress != null) {
                val updatedProgress = currentProgress.copy(
                    processedImages = processed,
                    totalImages = total,
                    isProcessing = isProcessing,
                    lastUpdated = System.currentTimeMillis() / 1000
                )
                database.ocrProgressDao().updateProgress(updatedProgress)
            } else {
                // Initialize progress if it doesn't exist
                val initialProgress = com.aks_labs.tulsi.database.entities.OcrProgressEntity(
                    totalImages = total,
                    processedImages = processed,
                    isProcessing = isProcessing,
                    lastUpdated = System.currentTimeMillis() / 1000
                )
                database.ocrProgressDao().insertProgress(initialProgress)
            }
            Log.d(TAG, "Updated progress: $processed/$total")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress in database", e)
        }
    }

    /**
     * Set progress for the worker
     */
    private suspend fun updateProgress(processed: Int, total: Int, currentItem: String) {
        val progress = if (total > 0) (processed * 100) / total else 0
        setProgress(workDataOf(
            KEY_PROGRESS to "Processing: $currentItem ($processed/$total)",
            KEY_TOTAL_PROCESSED to processed
        ))
    }

    /**
     * Data class for image information
     */
    data class ImageInfo(
        val id: Long,
        val name: String,
        val path: String,
        val uri: Uri
    )
}
