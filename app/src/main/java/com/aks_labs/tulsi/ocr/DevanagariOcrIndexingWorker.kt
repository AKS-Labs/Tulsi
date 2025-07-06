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
import com.aks_labs.tulsi.database.entities.DevanagariOcrTextEntity
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import android.provider.MediaStore
import android.net.Uri
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Worker for processing Devanagari OCR in background
 */
class DevanagariOcrIndexingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DevanagariOcrIndexingWorker"
        
        // Input data keys
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_MEDIA_URI = "media_uri"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_CONTINUOUS_PROCESSING = "continuous_processing"
        const val KEY_PROCESS_ALL = "process_all"
        
        // Output data keys
        const val KEY_TOTAL_PROCESSED = "total_processed"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        
        // Default values
        const val DEFAULT_BATCH_SIZE = 50
        const val PROCESSING_DELAY_MS = 100L // Small delay between images to prevent overwhelming the system
    }

    private val textExtractor = DevanagariOcrTextExtractor(applicationContext)
    private val notificationManager = DevanagariOcrNotificationManager(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 === DEVANAGARI OCR WORKER STARTED ===")
        Log.d(TAG, "Worker ID: $id")
        Log.d(TAG, "Run attempt: $runAttemptCount")

        try {
            Log.d(TAG, "Creating database instance...")
            // Get database instance
            val database = Room.databaseBuilder(
                applicationContext,
                MediaDatabase::class.java,
                "media-database"
            )
                .addMigrations(
                    Migration3to4(applicationContext),
                    Migration4to5(applicationContext),
                    Migration5to6(applicationContext),
                    Migration6to7(applicationContext),
                    com.aks_labs.tulsi.database.migrations.Migration7to8
                )
                .build()
            Log.d(TAG, "✅ Database instance created successfully")

            Log.d(TAG, "Parsing input data...")
            val mediaId = inputData.getLong(KEY_MEDIA_ID, -1L)
            val mediaUri = inputData.getString(KEY_MEDIA_URI)
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
            val continuousProcessing = inputData.getBoolean(KEY_CONTINUOUS_PROCESSING, false)
            val processAll = inputData.getBoolean(KEY_PROCESS_ALL, false)

            Log.d(TAG, "Input parameters:")
            Log.d(TAG, "  mediaId: $mediaId")
            Log.d(TAG, "  mediaUri: $mediaUri")
            Log.d(TAG, "  batchSize: $batchSize")
            Log.d(TAG, "  continuousProcessing: $continuousProcessing")
            Log.d(TAG, "  processAll: $processAll")

            return@withContext when {
                mediaId != -1L && mediaUri != null -> {
                    Log.d(TAG, "📷 Processing single image: $mediaId")
                    // Process single image
                    processSingleImage(database, mediaId, Uri.parse(mediaUri))
                }
                continuousProcessing || processAll -> {
                    Log.d(TAG, "📚 Processing batch of images (batchSize: $batchSize, processAll: $processAll)")
                    // Process batch of images
                    processBatchImages(database, batchSize, processAll)
                }
                else -> {
                    Log.e(TAG, "❌ Invalid input parameters for Devanagari OCR worker")
                    Log.e(TAG, "mediaId: $mediaId, mediaUri: $mediaUri, continuous: $continuousProcessing, processAll: $processAll")
                    Result.failure(workDataOf(KEY_ERROR to "Invalid input parameters"))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Devanagari OCR worker failed with exception", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            return@withContext Result.failure(workDataOf(KEY_ERROR to e.message))
        } finally {
            Log.d(TAG, "🧹 Cleaning up text extractor...")
            textExtractor.cleanup()
            Log.d(TAG, "🏁 === DEVANAGARI OCR WORKER FINISHED ===")
        }
    }

    /**
     * Process a single image for OCR
     */
    private suspend fun processSingleImage(
        database: MediaDatabase,
        mediaId: Long,
        imageUri: Uri
    ): Result {
        Log.d(TAG, "Processing single Devanagari OCR for image: $mediaId")

        try {
            // Check if already processed
            val existingText = database.devanagariOcrTextDao().getOcrTextByMediaId(mediaId)
            if (existingText != null) {
                Log.d(TAG, "Image $mediaId already processed for Devanagari OCR, skipping")
                return Result.success(workDataOf(
                    KEY_TOTAL_PROCESSED to 0,
                    KEY_PROGRESS to "Image already processed"
                ))
            }

            // Extract text using Devanagari OCR
            val ocrResult = textExtractor.extractTextFromImage(imageUri)
            
            return when (ocrResult) {
                is DevanagariOcrResult.Success -> {
                    // Save OCR result to database
                    val ocrEntity = DevanagariOcrTextEntity(
                        mediaId = mediaId,
                        extractedText = ocrResult.extractedText,
                        extractionTimestamp = System.currentTimeMillis() / 1000,
                        confidenceScore = ocrResult.confidence,
                        textBlocksCount = ocrResult.textBlocksCount,
                        processingTimeMs = ocrResult.processingTimeMs
                    )

                    database.devanagariOcrTextDao().insertOcrText(ocrEntity)

                    Log.d(TAG, "Successfully processed Devanagari OCR for image $mediaId: ${ocrResult.extractedText.length} characters extracted")

                    Result.success(workDataOf(
                        KEY_TOTAL_PROCESSED to 1,
                        KEY_PROGRESS to "Processed image $mediaId"
                    ))
                }
                is DevanagariOcrResult.Error -> {
                    Log.e(TAG, "Devanagari OCR failed for image $mediaId: ${ocrResult.message}")

                    // Update failed count in progress
                    database.devanagariOcrProgressDao().incrementFailedCount()

                    Result.failure(workDataOf(
                        KEY_ERROR to "Devanagari OCR failed: ${ocrResult.message}"
                    ))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception processing Devanagari OCR for image $mediaId", e)
            
            // Update failed count in progress
            database.devanagariOcrProgressDao().incrementFailedCount()
            
            return Result.failure(workDataOf(
                KEY_ERROR to "Exception: ${e.message}"
            ))
        }
    }

    /**
     * Process batch of images for OCR
     */
    private suspend fun processBatchImages(
        database: MediaDatabase,
        batchSize: Int,
        processAll: Boolean
    ): Result {
        Log.d(TAG, "🔄 === STARTING DEVANAGARI BATCH PROCESSING ===")
        Log.d(TAG, "Batch size: $batchSize, Process all: $processAll")

        try {
            Log.d(TAG, "📱 Getting all images from MediaStore...")
            // Get all images from MediaStore
            val allImages = getAllImages()
            Log.d(TAG, "📊 Found ${allImages.size} total images for Devanagari OCR processing")

            if (allImages.isEmpty()) {
                Log.w(TAG, "⚠️ No images found for Devanagari OCR processing")
                return Result.success(workDataOf(
                    KEY_TOTAL_PROCESSED to 0,
                    KEY_PROGRESS to "No images found"
                ))
            }

            // Initialize or update progress tracking
            val totalImages = allImages.size
            Log.d(TAG, "🗃️ Checking existing progress in database...")
            val existingProgress = database.devanagariOcrProgressDao().getProgress()
            Log.d(TAG, "Existing progress: $existingProgress")

            if (existingProgress == null) {
                Log.d(TAG, "📝 Creating initial progress tracking...")
                val initialProgress = com.aks_labs.tulsi.database.entities.DevanagariOcrProgressEntity(
                    totalImages = totalImages,
                    processedImages = 0,
                    isProcessing = true,
                    isPaused = false,
                    lastUpdated = System.currentTimeMillis() / 1000
                )
                database.devanagariOcrProgressDao().insertProgress(initialProgress)
                Log.d(TAG, "✅ Created initial Devanagari progress tracking for $totalImages images")
            } else {
                Log.d(TAG, "📝 Updating existing progress tracking...")
                database.devanagariOcrProgressDao().updateTotalCount(totalImages)
                database.devanagariOcrProgressDao().updateProcessingStatus(true)
                Log.d(TAG, "✅ Updated existing Devanagari progress tracking: ${existingProgress.processedImages}/$totalImages")
            }

            // Get already processed images to avoid reprocessing
            val processedMediaIds = database.devanagariOcrTextDao().getAllProcessedMediaIds().toSet()
            val unprocessedImages = allImages.filter { it.id !in processedMediaIds }

            Log.d(TAG, "Found ${unprocessedImages.size} unprocessed images for Devanagari OCR (${processedMediaIds.size} already processed)")

            if (unprocessedImages.isEmpty()) {
                Log.d(TAG, "All images already processed for Devanagari OCR")
                database.devanagariOcrProgressDao().updateProcessingStatus(false)
                return Result.success(workDataOf(
                    KEY_TOTAL_PROCESSED to 0,
                    KEY_PROGRESS to "All images already processed"
                ))
            }

            // Process images in batches
            val imagesToProcess = if (processAll) unprocessedImages else unprocessedImages.take(batchSize)
            var processedCount = 0
            var failedCount = 0

            Log.d(TAG, "Processing ${imagesToProcess.size} images for Devanagari OCR")

            for ((index, imageInfo) in imagesToProcess.withIndex()) {
                try {
                    // Check if processing should be paused
                    val currentProgress = database.devanagariOcrProgressDao().getProgress()
                    if (currentProgress?.isPaused == true) {
                        Log.d(TAG, "Devanagari OCR processing paused, stopping worker")
                        break
                    }

                    Log.d(TAG, "Processing Devanagari OCR for image ${index + 1}/${imagesToProcess.size}: ${imageInfo.id}")

                    // Extract text using Devanagari OCR
                    val ocrResult = textExtractor.extractTextFromImage(imageInfo.uri)

                    when (ocrResult) {
                        is DevanagariOcrResult.Success -> {
                            // Save OCR result to database
                            val ocrEntity = DevanagariOcrTextEntity(
                                mediaId = imageInfo.id,
                                extractedText = ocrResult.extractedText,
                                extractionTimestamp = System.currentTimeMillis() / 1000,
                                confidenceScore = ocrResult.confidence,
                                textBlocksCount = ocrResult.textBlocksCount,
                                processingTimeMs = ocrResult.processingTimeMs
                            )

                            database.devanagariOcrTextDao().insertOcrText(ocrEntity)
                            processedCount++

                            Log.d(TAG, "Successfully processed Devanagari OCR for image ${imageInfo.id}: ${ocrResult.extractedText.length} characters extracted")
                        }
                        is DevanagariOcrResult.Error -> {
                            Log.e(TAG, "Devanagari OCR failed for image ${imageInfo.id}: ${ocrResult.message}")
                            failedCount++
                            database.devanagariOcrProgressDao().incrementFailedCount()
                        }
                    }

                    // Update progress
                    val totalProcessedImages = database.devanagariOcrTextDao().getAllProcessedMediaIds().size
                    database.devanagariOcrProgressDao().updateProcessedCount(totalProcessedImages)

                    // Small delay to prevent overwhelming the system
                    delay(PROCESSING_DELAY_MS)

                } catch (e: Exception) {
                    Log.e(TAG, "Exception processing Devanagari OCR for image ${imageInfo.id}", e)
                    failedCount++
                    database.devanagariOcrProgressDao().incrementFailedCount()
                }
            }

            // Update final processing status
            val finalProgress = database.devanagariOcrProgressDao().getProgress()
            val isComplete = finalProgress?.isComplete == true

            if (isComplete || !processAll) {
                database.devanagariOcrProgressDao().updateProcessingStatus(false)
                Log.d(TAG, "Devanagari OCR batch processing completed")
            }

            Log.d(TAG, "Devanagari OCR batch processing finished: $processedCount processed, $failedCount failed")

            return Result.success(workDataOf(
                KEY_TOTAL_PROCESSED to processedCount,
                KEY_PROGRESS to "Processed $processedCount images, $failedCount failed"
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Devanagari OCR batch processing failed", e)
            database.devanagariOcrProgressDao().updateProcessingStatus(false)
            return Result.failure(workDataOf(
                KEY_ERROR to "Batch processing failed: ${e.message}"
            ))
        }
    }

    /**
     * Get all images from MediaStore
     */
    private fun getAllImages(): List<ImageInfo> {
        val images = mutableListOf<ImageInfo>()

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME
            )

            val cursor = applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val data = it.getString(dataColumn)
                    val name = it.getString(nameColumn)

                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    images.add(ImageInfo(id, uri, name, data))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get images from MediaStore for Devanagari OCR", e)
        }

        return images
    }

    /**
     * Data class for image information
     */
    private data class ImageInfo(
        val id: Long,
        val uri: Uri,
        val name: String,
        val path: String
    )
}
