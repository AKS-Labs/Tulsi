package com.aks_labs.tulsi.ocr

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.entities.OcrProgressEntity
import com.aks_labs.tulsi.database.entities.OcrTextEntity
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.ocr.MediaContentObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manager class for coordinating OCR operations
 */
class OcrManager(
    private val context: Context,
    private val database: MediaDatabase
) {

    companion object {
        private const val TAG = "OcrManager"
        private const val WORK_NAME_BATCH_OCR = "batch_ocr_indexing"
        private const val WORK_NAME_SINGLE_OCR = "single_ocr_"
    }

    private val workManager = WorkManager.getInstance(context)
    private val notificationManager = OcrNotificationManager(context)
    
    /**
     * Start OCR processing for a single image
     */
    fun processImage(mediaItem: MediaStoreData): UUID {
        Log.d(TAG, "Starting OCR for image: ${mediaItem.id}")

        val inputData = workDataOf(
            OcrIndexingWorker.KEY_MEDIA_ID to mediaItem.id,
            OcrIndexingWorker.KEY_MEDIA_URI to mediaItem.uri.toString()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("ocr_single")
            .addTag("media_${mediaItem.id}")
            .build()

        workManager.enqueueUniqueWork(
            "${WORK_NAME_SINGLE_OCR}${mediaItem.id}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        return workRequest.id
    }

    /**
     * Process image from MediaContentObserver
     */
    fun processImage(imageDetails: MediaContentObserver.ImageDetails): UUID {
        Log.d(TAG, "Starting OCR for new image: ${imageDetails.id}")

        val inputData = workDataOf(
            OcrIndexingWorker.KEY_MEDIA_ID to imageDetails.id,
            OcrIndexingWorker.KEY_MEDIA_URI to imageDetails.uri.toString()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("ocr_single")
            .addTag("media_${imageDetails.id}")
            .build()

        workManager.enqueueUniqueWork(
            "${WORK_NAME_SINGLE_OCR}${imageDetails.id}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        return workRequest.id
    }
    
    /**
     * Start batch OCR processing for multiple images
     */
    fun processBatch(batchSize: Int = OcrIndexingWorker.DEFAULT_BATCH_SIZE): UUID {
        Log.d(TAG, "Starting batch OCR processing with batch size: $batchSize")

        // Update processing status
        CoroutineScope(Dispatchers.IO).launch {
            database.ocrProgressDao().updateProcessingStatus(true)
            Log.d(TAG, "Updated processing status to true")
        }

        val inputData = workDataOf(
            OcrIndexingWorker.KEY_BATCH_SIZE to batchSize
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("ocr_batch")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_BATCH_OCR,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Enqueued OCR batch work with ID: ${workRequest.id}")
        return workRequest.id
    }
    
    /**
     * Get OCR work progress for a specific work ID
     */
    fun getWorkProgress(workId: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(workId)
    }
    
    /**
     * Get all OCR work progress
     */
    fun getAllOcrWorkProgress(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("ocr_single")
    }
    
    /**
     * Cancel OCR work for a specific image
     */
    fun cancelImageOcr(mediaId: Long) {
        workManager.cancelUniqueWork("${WORK_NAME_SINGLE_OCR}$mediaId")
    }
    
    /**
     * Cancel all OCR work
     */
    fun cancelAllOcr() {
        workManager.cancelAllWorkByTag("ocr_single")
        workManager.cancelAllWorkByTag("ocr_batch")
    }
    
    /**
     * Check if an image has been processed for OCR
     */
    suspend fun isImageProcessed(mediaId: Long): Boolean {
        return database.ocrTextDao().getOcrTextByMediaId(mediaId) != null
    }
    
    /**
     * Get OCR text for a specific image
     */
    suspend fun getOcrText(mediaId: Long): OcrTextEntity? {
        return database.ocrTextDao().getOcrTextByMediaId(mediaId)
    }
    
    /**
     * Search images by OCR text
     */
    suspend fun searchByOcrText(query: String, useFts: Boolean = false): List<OcrTextEntity> {
        // FTS temporarily disabled, using LIKE search
        return database.ocrTextDao().searchOcrText(query)
    }
    
    /**
     * Get OCR statistics
     */
    suspend fun getOcrStats(): OcrStats {
        val totalProcessed = database.ocrTextDao().getOcrTextCount()
        val averageConfidence = database.ocrTextDao().getAverageConfidenceScore() ?: 0.0f
        val averageProcessingTime = database.ocrTextDao().getAverageProcessingTime() ?: 0L
        
        return OcrStats(
            totalProcessed = totalProcessed,
            averageConfidence = averageConfidence,
            averageProcessingTime = averageProcessingTime
        )
    }
    
    /**
     * Clean up old OCR data
     */
    suspend fun cleanupOldOcrData(olderThanTimestamp: Long) {
        // This would remove OCR data for images that no longer exist
        // Implementation would depend on your media management logic
        Log.d(TAG, "Cleaning up OCR data older than $olderThanTimestamp")
    }

    /**
     * Get OCR progress flow for real-time updates
     */
    fun getProgressFlow(): Flow<OcrProgressEntity?> {
        return database.ocrProgressDao().getProgressFlow()
    }

    /**
     * Initialize progress tracking
     */
    suspend fun initializeProgress(totalImages: Int) {
        Log.d(TAG, "Initializing progress tracking with $totalImages total images")
        val existingProgress = database.ocrProgressDao().getProgress()
        if (existingProgress == null) {
            val initialProgress = OcrProgressEntity(
                totalImages = totalImages,
                processedImages = 0,
                isProcessing = false,
                lastUpdated = System.currentTimeMillis() / 1000
            )
            database.ocrProgressDao().insertProgress(initialProgress)
            Log.d(TAG, "Created initial progress tracking")
        } else {
            database.ocrProgressDao().updateTotalCount(totalImages)
            Log.d(TAG, "Updated existing progress tracking: ${existingProgress.processedImages}/$totalImages")
        }
    }

    /**
     * Update progress when an image is processed
     */
    suspend fun updateProgress(processedCount: Int, avgProcessingTime: Long = 0) {
        database.ocrProgressDao().updateProcessedCount(processedCount)

        if (avgProcessingTime > 0) {
            val progress = database.ocrProgressDao().getProgress()
            if (progress != null) {
                val estimatedCompletion = System.currentTimeMillis() / 1000 +
                    ((progress.totalImages - processedCount) * avgProcessingTime / 1000)
                database.ocrProgressDao().updateTimingInfo(avgProcessingTime, estimatedCompletion)
            }
        }

        // Update notification
        val currentProgress = database.ocrProgressDao().getProgress()
        if (currentProgress != null) {
            notificationManager.updateProgress(currentProgress)
        }
    }

    /**
     * Pause OCR processing
     */
    suspend fun pauseProcessing() {
        database.ocrProgressDao().updatePausedStatus(true)
        database.ocrProgressDao().updateProcessingStatus(false)
        cancelAllOcr()

        val progress = database.ocrProgressDao().getProgress()
        if (progress != null) {
            notificationManager.updateProgress(progress.copy(isPaused = true, isProcessing = false))
        }
    }

    /**
     * Resume OCR processing
     */
    suspend fun resumeProcessing() {
        database.ocrProgressDao().updatePausedStatus(false)
        database.ocrProgressDao().updateProcessingStatus(true)
        processBatch()

        val progress = database.ocrProgressDao().getProgress()
        if (progress != null) {
            notificationManager.updateProgress(progress.copy(isPaused = false, isProcessing = true))
        }
    }

    /**
     * Dismiss progress bar
     */
    suspend fun dismissProgress() {
        database.ocrProgressDao().updateDismissedStatus(true)
    }

    /**
     * Show progress bar again
     */
    suspend fun showProgress() {
        database.ocrProgressDao().updateDismissedStatus(false)
    }

    /**
     * Force restart OCR processing (for debugging)
     */
    suspend fun forceRestartOcr() {
        Log.d(TAG, "Force restarting OCR processing...")

        // Clear progress
        database.ocrProgressDao().clearProgress()

        // Reinitialize
        val totalImages = getTotalImageCount()
        initializeProgress(totalImages)

        // Start processing
        processBatch(batchSize = 3)

        Log.d(TAG, "Force restart completed")
    }

    /**
     * Get total image count from MediaStore
     */
    private fun getTotalImageCount(): Int {
        return try {
            val cursor = context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.Images.Media._ID),
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
}

/**
 * Data class for OCR statistics
 */
data class OcrStats(
    val totalProcessed: Int,
    val averageConfidence: Float,
    val averageProcessingTime: Long
)
