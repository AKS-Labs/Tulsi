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
        const val KEY_CONTINUOUS_PROCESSING = "continuous_processing"
        const val KEY_PROCESS_ALL = "process_all"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL_PROCESSED = "total_processed"
        const val KEY_ERRORS = "errors"

        const val DEFAULT_BATCH_SIZE = 50
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
            val continuousProcessing = inputData.getBoolean(KEY_CONTINUOUS_PROCESSING, false)
            val processAll = inputData.getBoolean(KEY_PROCESS_ALL, false)

            return@withContext when {
                mediaId != -1L && mediaUri != null -> {
                    // Process single image
                    processSingleImage(mediaId, mediaUri)
                }
                processAll -> {
                    // Process all unprocessed images continuously
                    processContinuouslyUntilComplete(batchSize)
                }
                else -> {
                    // Process batch of unprocessed images
                    processBatchImages(batchSize, continuousProcessing)
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

                    // Mark failed image as processed with empty text to avoid infinite retry
                    val failedOcrEntity = OcrTextEntity(
                        mediaId = mediaId,
                        extractedText = "", // Empty text for failed OCR
                        extractionTimestamp = System.currentTimeMillis() / 1000,
                        confidenceScore = 0.0f,
                        textBlocksCount = 0,
                        processingTimeMs = 0L
                    )
                    database.ocrTextDao().insertOcrText(failedOcrEntity)

                    Log.d(TAG, "Marked failed image $mediaId as processed with empty text")

                    Result.success(workDataOf(
                        KEY_TOTAL_PROCESSED to 1,
                        KEY_PROGRESS to "Processed (failed) image $mediaId"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image $mediaId", e)
            Result.failure(workDataOf(KEY_ERRORS to e.message))
        }
    }
    
    /**
     * Process all unprocessed images continuously until complete
     */
    private suspend fun processContinuouslyUntilComplete(batchSize: Int): Result {
        Log.d(TAG, "Starting continuous processing with batch size: $batchSize")

        var totalProcessed = 0
        var totalErrors = 0
        var iterationCount = 0

        while (true) {
            iterationCount++
            Log.d(TAG, "Continuous processing iteration $iterationCount")

            // Get current progress
            val totalImages = getTotalImageCount()
            val processedMediaIds = database.ocrTextDao().getAllProcessedMediaIds().toSet()
            val remainingImages = totalImages - processedMediaIds.size

            Log.d(TAG, "Progress: ${processedMediaIds.size}/$totalImages processed, $remainingImages remaining")

            if (remainingImages <= 0) {
                Log.d(TAG, "All images processed! Stopping continuous processing.")
                updateProgressInDatabase(processedMediaIds.size, totalImages, false)
                break
            }

            // Process next batch
            val result = processBatchImages(minOf(batchSize, remainingImages), false)

            when (result) {
                is Result.Success -> {
                    val batchProcessed = result.outputData.getInt(KEY_TOTAL_PROCESSED, 0)
                    totalProcessed += batchProcessed
                    Log.d(TAG, "Batch completed: $batchProcessed processed in iteration $iterationCount")

                    // If no images were processed in this batch, we're done
                    if (batchProcessed == 0) {
                        Log.d(TAG, "No more images to process. Stopping.")
                        break
                    }
                }
                is Result.Failure -> {
                    totalErrors++
                    Log.e(TAG, "Batch failed in iteration $iterationCount")

                    // Continue processing even if one batch fails
                    if (totalErrors > 5) {
                        Log.e(TAG, "Too many batch failures ($totalErrors). Stopping continuous processing.")
                        break
                    }
                }
                else -> {
                    Log.w(TAG, "Unexpected result type: $result")
                    break
                }
            }

            // Small delay between batches to prevent overwhelming the system
            kotlinx.coroutines.delay(1000)
        }

        Log.d(TAG, "Continuous processing completed: $totalProcessed total processed, $totalErrors errors, $iterationCount iterations")

        return Result.success(workDataOf(
            KEY_TOTAL_PROCESSED to totalProcessed,
            KEY_PROGRESS to "Continuous processing completed: $totalProcessed images processed"
        ))
    }

    /**
     * Process a batch of unprocessed images
     */
    private suspend fun processBatchImages(batchSize: Int, scheduleNext: Boolean = false): Result {
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

                            // Mark failed image as processed with empty text to avoid infinite retry
                            val failedOcrEntity = OcrTextEntity(
                                mediaId = imageInfo.id,
                                extractedText = "", // Empty text for failed OCR
                                extractionTimestamp = System.currentTimeMillis() / 1000,
                                confidenceScore = 0.0f,
                                textBlocksCount = 0,
                                processingTimeMs = 0L
                            )
                            database.ocrTextDao().insertOcrText(failedOcrEntity)
                            processedCount++ // Count failed images as processed
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

                        // Mark failed image as processed with empty text to avoid infinite retry
                        val failedOcrEntity = OcrTextEntity(
                            mediaId = imageInfo.id,
                            extractedText = "", // Empty text for failed OCR
                            extractionTimestamp = System.currentTimeMillis() / 1000,
                            confidenceScore = 0.0f,
                            textBlocksCount = 0,
                            processingTimeMs = 0L
                        )
                        database.ocrTextDao().insertOcrText(failedOcrEntity)
                        processedCount++ // Count failed images as processed
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
     * Update progress in database with overall progress and timing information
     */
    private suspend fun updateProgressInDatabase(processed: Int, total: Int, isProcessing: Boolean) {
        try {
            // Get overall progress instead of just batch progress
            val totalImages = getTotalImageCount()
            val totalProcessedImages = database.ocrTextDao().getAllProcessedMediaIds().size

            val currentProgress = database.ocrProgressDao().getProgress()
            val currentTime = System.currentTimeMillis()

            // Calculate average processing time and estimated completion
            val averageProcessingTime = if (currentProgress != null && totalProcessedImages > 0) {
                val timeElapsed = currentTime - (currentProgress.lastUpdated * 1000)
                val imagesProcessedSinceLastUpdate = totalProcessedImages - currentProgress.processedImages
                if (imagesProcessedSinceLastUpdate > 0) {
                    timeElapsed / imagesProcessedSinceLastUpdate
                } else {
                    currentProgress.averageProcessingTimeMs
                }
            } else {
                0L
            }

            val estimatedCompletionTime = if (averageProcessingTime > 0 && totalProcessedImages < totalImages) {
                val remainingImages = totalImages - totalProcessedImages
                currentTime + (remainingImages * averageProcessingTime)
            } else {
                0L
            }

            if (currentProgress != null) {
                val updatedProgress = currentProgress.copy(
                    processedImages = totalProcessedImages,
                    totalImages = totalImages,
                    isProcessing = isProcessing,
                    lastUpdated = currentTime / 1000,
                    averageProcessingTimeMs = averageProcessingTime,
                    estimatedCompletionTime = estimatedCompletionTime
                )
                database.ocrProgressDao().updateProgress(updatedProgress)
            } else {
                // Initialize progress if it doesn't exist
                val initialProgress = com.aks_labs.tulsi.database.entities.OcrProgressEntity(
                    totalImages = totalImages,
                    processedImages = totalProcessedImages,
                    isProcessing = isProcessing,
                    lastUpdated = currentTime / 1000,
                    averageProcessingTimeMs = averageProcessingTime,
                    estimatedCompletionTime = estimatedCompletionTime
                )
                database.ocrProgressDao().insertProgress(initialProgress)
            }
            Log.d(TAG, "Updated overall progress: $totalProcessedImages/$totalImages (avg: ${averageProcessingTime}ms/image)")
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
