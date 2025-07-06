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
 * Handles notification actions for Devanagari OCR processing
 */
class DevanagariOcrNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DevanagariOcrNotificationReceiver"
        
        const val ACTION_PAUSE = "com.aks_labs.tulsi.ocr.devanagari.PAUSE"
        const val ACTION_RESUME = "com.aks_labs.tulsi.ocr.devanagari.RESUME"
        const val ACTION_CANCEL = "com.aks_labs.tulsi.ocr.devanagari.CANCEL"
        const val ACTION_VIEW_PROGRESS = "com.aks_labs.tulsi.ocr.devanagari.VIEW_PROGRESS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            ACTION_PAUSE -> handlePause(context)
            ACTION_RESUME -> handleResume(context)
            ACTION_CANCEL -> handleCancel(context)
            ACTION_VIEW_PROGRESS -> handleViewProgress(context)
        }
    }

    /**
     * Handle pause action
     */
    private fun handlePause(context: Context) {
        Log.d(TAG, "Handling Devanagari OCR pause action")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = getDatabase(context)
                val manager = DevanagariOcrManager(context, database)
                manager.pauseProcessing()
                Log.d(TAG, "Devanagari OCR processing paused successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause Devanagari OCR processing", e)
            }
        }
    }

    /**
     * Handle resume action
     */
    private fun handleResume(context: Context) {
        Log.d(TAG, "Handling Devanagari OCR resume action")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = getDatabase(context)
                val manager = DevanagariOcrManager(context, database)
                manager.resumeProcessing()
                Log.d(TAG, "Devanagari OCR processing resumed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume Devanagari OCR processing", e)
            }
        }
    }

    /**
     * Handle cancel action
     */
    private fun handleCancel(context: Context) {
        Log.d(TAG, "Handling Devanagari OCR cancel action")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = getDatabase(context)
                val manager = DevanagariOcrManager(context, database)
                manager.cancelAllOcr()
                
                // Update progress to not processing
                database.devanagariOcrProgressDao().updateProcessingStatus(false)
                database.devanagariOcrProgressDao().updatePausedStatus(false)
                
                // Hide notification
                val notificationManager = DevanagariOcrNotificationManager(context)
                notificationManager.hideNotification()
                
                Log.d(TAG, "Devanagari OCR processing cancelled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel Devanagari OCR processing", e)
            }
        }
    }

    /**
     * Handle view progress action
     */
    private fun handleViewProgress(context: Context) {
        Log.d(TAG, "Handling Devanagari OCR view progress action")
        
        try {
            // Launch main activity with intent to show progress
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("show_devanagari_ocr_progress", true)
                context.startActivity(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app for Devanagari OCR progress view", e)
        }
    }

    /**
     * Get database instance
     */
    private fun getDatabase(context: Context): MediaDatabase {
        return Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            "media-database"
        )
            .addMigrations(
                Migration3to4(context),
                Migration4to5(context),
                Migration5to6(context),
                Migration6to7(context),
                com.aks_labs.tulsi.database.migrations.Migration7to8
            )
            .build()
    }
}
