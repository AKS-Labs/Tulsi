package com.aks_labs.tulsi.ocr

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.entities.DevanagariOcrProgressEntity
import com.aks_labs.tulsi.database.entities.DevanagariOcrTextEntity
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.ocr.MediaContentObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Manager class for coordinating Devanagari OCR operations
 */
class DevanagariOcrManager(
    private val context: Context,
    private val database: MediaDatabase
) {

    companion object {
        private const val TAG = "DevanagariOcrManager"
        private const val WORK_NAME_BATCH_OCR = "devanagari_batch_ocr_indexing"
        private const val WORK_NAME_SINGLE_OCR = "devanagari_single_ocr_"
    }

    private val workManager = WorkManager.getInstance(context)
    private val notificationManager = DevanagariOcrNotificationManager(context)
    private var progressMonitorJob: Job? = null
    
    /**
     * Start OCR processing for a single image
     */
    fun processImage(mediaItem: MediaStoreData): UUID {
        Log.d(TAG, "Starting Devanagari OCR for image: ${mediaItem.id}")

        val inputData = workDataOf(
            DevanagariOcrIndexingWorker.KEY_MEDIA_ID to mediaItem.id,
            DevanagariOcrIndexingWorker.KEY_MEDIA_URI to mediaItem.uri.toString()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow processing even with low battery for better compatibility
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DevanagariOcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("devanagari_ocr_single")
            .addTag("devanagari_media_${mediaItem.id}")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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
        Log.d(TAG, "Starting Devanagari OCR for new image: ${imageDetails.id}")

        val inputData = workDataOf(
            DevanagariOcrIndexingWorker.KEY_MEDIA_ID to imageDetails.id,
            DevanagariOcrIndexingWorker.KEY_MEDIA_URI to imageDetails.uri.toString()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow processing even with low battery for better compatibility
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DevanagariOcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("devanagari_ocr_single")
            .addTag("devanagari_media_${imageDetails.id}")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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
    fun processBatch(batchSize: Int = DevanagariOcrIndexingWorker.DEFAULT_BATCH_SIZE): UUID {
        Log.d(TAG, "Starting Devanagari batch OCR processing with batch size: $batchSize")

        // Start foreground service to ensure processing continues in background
        OcrForegroundService.startDevanagariOcr(context)

        // Update processing status
        CoroutineScope(Dispatchers.IO).launch {
            database.devanagariOcrProgressDao().updateProcessingStatus(true)
            Log.d(TAG, "Updated Devanagari processing status to true")
        }

        val inputData = workDataOf(
            DevanagariOcrIndexingWorker.KEY_BATCH_SIZE to batchSize,
            DevanagariOcrIndexingWorker.KEY_CONTINUOUS_PROCESSING to true
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow processing even with low battery for better compatibility
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DevanagariOcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("devanagari_ocr_batch")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_BATCH_OCR,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Start real-time progress monitoring for batch processing too
        startProgressMonitoring()

        Log.d(TAG, "Enqueued Devanagari OCR batch work with ID: ${workRequest.id}")
        return workRequest.id
    }

    /**
     * Start continuous OCR processing for all images
     */
    fun startContinuousProcessing(batchSize: Int = 50): UUID {
        Log.d(TAG, "=== STARTING CONTINUOUS DEVANAGARI OCR PROCESSING ===")
        Log.d(TAG, "Batch size: $batchSize")

        // Start foreground service to ensure processing continues in background
        OcrForegroundService.startDevanagariOcr(context)

        // Update processing status and ensure not paused
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Verifying database tables exist...")
                verifyDatabaseTables()

                Log.d(TAG, "Updating Devanagari OCR database status...")
                database.devanagariOcrProgressDao().updateProcessingStatus(true)
                database.devanagariOcrProgressDao().updatePausedStatus(false)
                Log.d(TAG, "Successfully updated Devanagari processing status to true and paused status to false")

                // Verify the update worked
                val progress = database.devanagariOcrProgressDao().getProgress()
                Log.d(TAG, "Current progress after update: $progress")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update Devanagari OCR database status", e)
            }
        }

        Log.d(TAG, "Creating work request for Devanagari OCR...")
        val inputData = workDataOf(
            DevanagariOcrIndexingWorker.KEY_BATCH_SIZE to batchSize,
            DevanagariOcrIndexingWorker.KEY_CONTINUOUS_PROCESSING to true,
            DevanagariOcrIndexingWorker.KEY_PROCESS_ALL to true
        )
        Log.d(TAG, "Input data created: batchSize=$batchSize, continuous=true, processAll=true")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow processing even with low battery for better compatibility
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()
        Log.d(TAG, "Work constraints created (no network, no battery/charging/idle requirements)")

        val workRequest = OneTimeWorkRequestBuilder<DevanagariOcrIndexingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("devanagari_ocr_continuous")
            .addTag("devanagari_ocr_batch")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        Log.d(TAG, "Work request created with ID: ${workRequest.id}")

        val workName = "${WORK_NAME_BATCH_OCR}_continuous"
        Log.d(TAG, "Enqueueing work with name: $workName, policy: REPLACE")

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "✅ Successfully enqueued continuous Devanagari OCR work with ID: ${workRequest.id}")

        // Verify work was enqueued
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // Wait a bit for WorkManager to process
            try {
                val workInfo = workManager.getWorkInfoById(workRequest.id).get()
                if (workInfo != null) {
                    Log.d(TAG, "📊 Work status after enqueue: ${workInfo.state}")
                    Log.d(TAG, "📊 Work tags: ${workInfo.tags}")
                    Log.d(TAG, "📊 Work progress: ${workInfo.progress}")
                } else {
                    Log.w(TAG, "⚠️ WorkInfo is null for work ID: ${workRequest.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get work info", e)
            }
        }

        // Start real-time progress monitoring
        Log.d(TAG, "Starting progress monitoring for Devanagari OCR...")
        startProgressMonitoring()

        Log.d(TAG, "=== DEVANAGARI OCR PROCESSING SETUP COMPLETE ===")
        return workRequest.id
    }

    /**
     * Check if monitoring should be active and start if needed
     */
    suspend fun ensureProgressMonitoring() {
        val progress = database.devanagariOcrProgressDao().getProgress()
        Log.d(TAG, "Checking Devanagari progress monitoring: progress=$progress, job=${progressMonitorJob != null}")

        if (progress != null && (progress.isProcessing || progress.isPaused)) {
            // Force refresh the progress data to ensure accurate display
            val totalImages = getTotalImageCount()
            val totalProcessedImages = database.devanagariOcrTextDao().getAllProcessedMediaIds().size

            Log.d(TAG, "Refreshing Devanagari progress data: $totalProcessedImages/$totalImages (stored: ${progress.processedImages}/${progress.totalImages})")

            // Update database with current counts
            database.devanagariOcrProgressDao().updateTotalCount(totalImages)
            database.devanagariOcrProgressDao().updateProcessedCount(totalProcessedImages)

            // Start monitoring if not already active
            if (progressMonitorJob == null || progressMonitorJob?.isActive != true) {
                Log.d(TAG, "Starting Devanagari progress monitoring...")
                startProgressMonitoring()
            } else {
                Log.d(TAG, "Devanagari progress monitoring already active")
            }
        } else {
            Log.d(TAG, "No active Devanagari OCR processing, monitoring not needed")
        }
    }

    /**
     * Start real-time progress monitoring
     */
    private fun startProgressMonitoring() {
        Log.d(TAG, "=== STARTING DEVANAGARI PROGRESS MONITORING ===")

        // Cancel existing monitoring
        progressMonitorJob?.cancel()
        Log.d(TAG, "Cancelled any existing progress monitoring job")

        progressMonitorJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "🔄 Devanagari progress monitoring coroutine started")
            var lastProgressCount = -1
            var stuckProgressCounter = 0
            val maxStuckIterations = 30 // 30 iterations * 2 seconds = 60 seconds timeout

            while (true) {
                try {
                    Log.d(TAG, "📊 Progress monitoring iteration - checking database...")
                    val progress = database.devanagariOcrProgressDao().getProgress()
                    Log.d(TAG, "Retrieved progress from database: $progress")

                    if (progress != null) {
                        // Refresh progress with current database state
                        Log.d(TAG, "Getting total image count...")
                        val totalImages = getTotalImageCount()
                        Log.d(TAG, "Total images in MediaStore: $totalImages")

                        Log.d(TAG, "Getting processed media IDs...")
                        val totalProcessedImages = database.devanagariOcrTextDao().getAllProcessedMediaIds().size
                        Log.d(TAG, "Total processed images in Devanagari OCR: $totalProcessedImages")

                        Log.d(TAG, "📈 Devanagari progress check: $totalProcessedImages/$totalImages (stored: ${progress.processedImages}/${progress.totalImages})")

                        // Check for stuck progress (no change in processed count while processing)
                        if (progress.isProcessing && !progress.isPaused) {
                            if (lastProgressCount == totalProcessedImages) {
                                stuckProgressCounter++
                                Log.d(TAG, "Devanagari progress appears stuck: $stuckProgressCounter/$maxStuckIterations iterations")

                                if (stuckProgressCounter >= maxStuckIterations) {
                                    Log.w(TAG, "Devanagari progress stuck for too long, marking as not processing")
                                    database.devanagariOcrProgressDao().updateProcessingStatus(false)
                                    stuckProgressCounter = 0
                                }
                            } else {
                                stuckProgressCounter = 0 // Reset counter if progress is made
                            }
                            lastProgressCount = totalProcessedImages
                        } else {
                            stuckProgressCounter = 0 // Reset counter if not processing
                        }

                        // Always update progress to ensure Flow emission and real-time updates
                        val currentTime = System.currentTimeMillis()
                        val updatedProgress = progress.copy(
                            processedImages = totalProcessedImages,
                            totalImages = totalImages,
                            lastUpdated = currentTime / 1000
                            // Preserve isPaused and isProcessing states
                        )

                        // Update database to trigger Flow emission
                        Log.d(TAG, "Updating progress in database...")
                        database.devanagariOcrProgressDao().updateProgress(updatedProgress)
                        Log.d(TAG, "✅ Database updated successfully")
                        Log.d(TAG, "📊 Devanagari real-time progress: ${updatedProgress.processedImages}/${updatedProgress.totalImages} (${updatedProgress.progressPercentage}%)")

                        // Update notification if progress bar is dismissed or if processing
                        Log.d(TAG, "Checking if notification should be updated...")
                        Log.d(TAG, "Progress dismissed: ${updatedProgress.progressDismissed}, Is processing: ${updatedProgress.isProcessing}")
                        if (updatedProgress.progressDismissed || updatedProgress.isProcessing) {
                            Log.d(TAG, "🔔 Updating Devanagari OCR notification...")
                            notificationManager.updateProgress(updatedProgress)
                            Log.d(TAG, "✅ Notification update called")
                        } else {
                            Log.d(TAG, "⏭️ Skipping notification update (not dismissed and not processing)")
                        }

                        // Stop monitoring if processing is complete
                        if (updatedProgress.isComplete || (!updatedProgress.isProcessing && !updatedProgress.isPaused)) {
                            Log.d(TAG, "Devanagari progress monitoring stopped - processing complete or inactive")
                            break
                        }
                    } else {
                        Log.w(TAG, "No Devanagari progress entity found in database")
                    }

                    // Update every 2 seconds for real-time feedback
                    delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Devanagari progress monitoring", e)
                    delay(5000) // Wait longer on error
                }
            }
            Log.d(TAG, "Devanagari progress monitoring ended")
        }
    }

    /**
     * Get total number of images in MediaStore
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
            Log.e(TAG, "Failed to get total image count for Devanagari", e)
            0
        }
    }

    /**
     * Get OCR progress flow for real-time updates
     */
    fun getProgressFlow(): Flow<DevanagariOcrProgressEntity?> {
        return database.devanagariOcrProgressDao().getProgressFlow()
    }

    /**
     * Initialize progress tracking
     */
    suspend fun initializeProgress(totalImages: Int) {
        Log.d(TAG, "Initializing Devanagari progress tracking with $totalImages total images")
        val existingProgress = database.devanagariOcrProgressDao().getProgress()
        if (existingProgress == null) {
            val initialProgress = DevanagariOcrProgressEntity(
                totalImages = totalImages,
                processedImages = 0,
                isProcessing = false,
                isPaused = false,
                lastUpdated = System.currentTimeMillis() / 1000
            )
            database.devanagariOcrProgressDao().insertProgress(initialProgress)
            Log.d(TAG, "Created initial Devanagari progress tracking")
        } else {
            database.devanagariOcrProgressDao().updateTotalCount(totalImages)
            Log.d(TAG, "Updated existing Devanagari progress tracking: ${existingProgress.processedImages}/$totalImages")
        }
    }

    /**
     * Check if an image has been processed for OCR
     */
    suspend fun isImageProcessed(mediaId: Long): Boolean {
        return database.devanagariOcrTextDao().getOcrTextByMediaId(mediaId) != null
    }

    /**
     * Get OCR text for a specific image
     */
    suspend fun getOcrText(mediaId: Long): DevanagariOcrTextEntity? {
        return database.devanagariOcrTextDao().getOcrTextByMediaId(mediaId)
    }

    /**
     * Search images by OCR text
     */
    suspend fun searchByOcrText(query: String): List<DevanagariOcrTextEntity> {
        return database.devanagariOcrTextDao().searchOcrTextFallback(query)
    }

    /**
     * Cancel all OCR work
     */
    fun cancelAllOcr() {
        // Stop foreground service for Devanagari OCR
        OcrForegroundService.stopDevanagariOcr(context)

        workManager.cancelAllWorkByTag("devanagari_ocr_single")
        workManager.cancelAllWorkByTag("devanagari_ocr_batch")
        workManager.cancelAllWorkByTag("devanagari_ocr_continuous")

        // Cancel progress monitoring
        progressMonitorJob?.cancel()
        progressMonitorJob = null

        // Hide notification
        notificationManager.hideNotification()

        Log.d(TAG, "All Devanagari OCR processing cancelled")
    }

    /**
     * Pause OCR processing
     */
    suspend fun pauseProcessing() {
        database.devanagariOcrProgressDao().updatePausedStatus(true)
        database.devanagariOcrProgressDao().updateProcessingStatus(false)
        cancelAllOcr()

        val progress = database.devanagariOcrProgressDao().getProgress()
        if (progress != null) {
            notificationManager.updateProgress(progress.copy(isPaused = true, isProcessing = false))
        }

        Log.d(TAG, "Devanagari OCR processing paused")
    }

    /**
     * Resume OCR processing
     */
    suspend fun resumeProcessing() {
        database.devanagariOcrProgressDao().updatePausedStatus(false)
        database.devanagariOcrProgressDao().updateProcessingStatus(true)
        startContinuousProcessing(batchSize = 50)

        val progress = database.devanagariOcrProgressDao().getProgress()
        if (progress != null) {
            notificationManager.updateProgress(progress.copy(isPaused = false, isProcessing = true))
        }

        Log.d(TAG, "Devanagari OCR processing resumed")
    }

    /**
     * Mark progress bar as dismissed and show notification
     */
    suspend fun dismissProgressBar() {
        Log.d(TAG, "Devanagari progress bar dismissed - showing notification")
        database.devanagariOcrProgressDao().updateDismissedStatus(true)

        val progress = database.devanagariOcrProgressDao().getProgress()
        if (progress != null && (progress.isProcessing || progress.isPaused)) {
            notificationManager.updateProgress(progress.copy(progressDismissed = true))
        }
    }

    /**
     * Verify that database tables exist and are accessible
     */
    private suspend fun verifyDatabaseTables() {
        try {
            Log.d(TAG, "🔍 Verifying Devanagari OCR database tables...")

            // Test devanagari_ocr_text table
            val textCount = database.devanagariOcrTextDao().getOcrTextCount()
            Log.d(TAG, "✅ devanagari_ocr_text table accessible, current count: $textCount")

            // Test devanagari_ocr_progress table
            val progress = database.devanagariOcrProgressDao().getProgress()
            Log.d(TAG, "✅ devanagari_ocr_progress table accessible, current progress: $progress")

            Log.d(TAG, "✅ All Devanagari OCR database tables verified successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database table verification failed", e)
            throw e
        }
    }
}

/**
 * Data class for Devanagari OCR statistics
 */
data class DevanagariOcrStats(
    val totalProcessed: Int,
    val averageConfidence: Float,
    val averageProcessingTime: Long
)
