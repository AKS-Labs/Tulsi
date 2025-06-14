package com.aks_labs.tulsi.ocr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Handles notification action buttons for OCR processing
 */
class OcrNotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "OcrNotificationReceiver"
        private const val ACTION_PAUSE = "com.aks_labs.tulsi.OCR_PAUSE"
        private const val ACTION_RESUME = "com.aks_labs.tulsi.OCR_RESUME"
        private const val ACTION_VIEW_PROGRESS = "com.aks_labs.tulsi.OCR_VIEW_PROGRESS"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_PAUSE -> {
                pauseOcrProcessing(context)
            }
            ACTION_RESUME -> {
                resumeOcrProcessing(context)
            }
            ACTION_VIEW_PROGRESS -> {
                openAppToSearchScreen(context)
            }
        }
    }
    
    /**
     * Pause OCR processing
     */
    private fun pauseOcrProcessing(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = getDatabase(context)
                val ocrManager = OcrManager(context, database)
                
                // Update pause status in database
                database.ocrProgressDao().updatePausedStatus(true)
                
                // Cancel current work
                ocrManager.cancelAllOcr()
                
                Log.d(TAG, "OCR processing paused")
                
                // Update notification
                val progress = database.ocrProgressDao().getProgress()
                if (progress != null) {
                    val notificationManager = OcrNotificationManager(context)
                    notificationManager.updateProgress(progress.copy(isPaused = true, isProcessing = false))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause OCR processing", e)
            }
        }
    }
    
    /**
     * Resume OCR processing
     */
    private fun resumeOcrProcessing(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = getDatabase(context)
                val ocrManager = OcrManager(context, database)
                
                // Update pause status in database
                database.ocrProgressDao().updatePausedStatus(false)
                database.ocrProgressDao().updateProcessingStatus(true)
                
                // Resume processing
                ocrManager.processBatch()
                
                Log.d(TAG, "OCR processing resumed")
                
                // Update notification
                val progress = database.ocrProgressDao().getProgress()
                if (progress != null) {
                    val notificationManager = OcrNotificationManager(context)
                    notificationManager.updateProgress(progress.copy(isPaused = false, isProcessing = true))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume OCR processing", e)
            }
        }
    }
    
    /**
     * Open app to search screen
     */
    private fun openAppToSearchScreen(context: Context) {
        val intent = Intent(context, com.aks_labs.tulsi.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_search", true)
        }
        context.startActivity(intent)
    }
    
    /**
     * Get database instance
     */
    private fun getDatabase(context: Context): MediaDatabase {
        return Room.databaseBuilder(
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
}
