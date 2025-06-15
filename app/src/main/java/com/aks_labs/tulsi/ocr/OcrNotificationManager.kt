package com.aks_labs.tulsi.ocr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aks_labs.tulsi.MainActivity
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.database.entities.OcrProgressEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manages OCR processing notifications
 */
class OcrNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "ocr_processing"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_PAUSE = "com.aks_labs.tulsi.OCR_PAUSE"
        private const val ACTION_RESUME = "com.aks_labs.tulsi.OCR_RESUME"
        private const val ACTION_VIEW_PROGRESS = "com.aks_labs.tulsi.OCR_VIEW_PROGRESS"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for OCR processing
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OCR Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of image text processing"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show OCR processing notification
     */
    fun showProcessingNotification(progress: OcrProgressEntity) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_search", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseResumeIntent = if (progress.isPaused) {
            createActionIntent(ACTION_RESUME)
        } else {
            createActionIntent(ACTION_PAUSE)
        }
        
        val viewProgressIntent = createActionIntent(ACTION_VIEW_PROGRESS)
        
        val pauseResumeText = if (progress.isPaused) "Resume" else "Pause"
        val statusText = if (progress.isPaused) "Paused" else "Processing"

        // Calculate estimated time remaining
        val timeRemainingText = if (progress.estimatedTimeRemainingMs > 0 && !progress.isPaused) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(progress.estimatedTimeRemainingMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(progress.estimatedTimeRemainingMs) % 60
            when {
                minutes > 0 -> " • ~${minutes}m ${seconds}s remaining"
                seconds > 0 -> " • ~${seconds}s remaining"
                else -> " • Almost done"
            }
        } else ""

        val progressText = "${progress.processedImages}/${progress.totalImages} (${progress.progressPercentage}%)"
        val detailText = "$statusText: $progressText$timeRemainingText"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Tulsi Gallery - OCR Processing")
            .setContentText(detailText)
            .setSubText("Tap to view detailed progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You may want to create a specific OCR icon
            .setProgress(progress.totalImages, progress.processedImages, false)
            .setContentIntent(pendingIntent)
            .setOngoing(!progress.isComplete)
            .setAutoCancel(progress.isComplete)
            .addAction(
                android.R.drawable.ic_media_pause,
                pauseResumeText,
                pauseResumeIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Progress",
                viewProgressIntent
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true) // Make notification completely silent
            .setOnlyAlertOnce(true) // Only alert once, not on every update
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }
    
    /**
     * Show completion notification
     */
    fun showCompletionNotification(totalProcessed: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_search", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Text Search Ready!")
            .setContentText("$totalProcessed images processed and ready for text search")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }
    
    /**
     * Hide OCR notification
     */
    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Hide progress notification (alias for hideNotification)
     */
    fun hideProgress() {
        hideNotification()
    }
    
    /**
     * Create action intent for notification buttons
     */
    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Update notification with new progress
     */
    fun updateProgress(progress: OcrProgressEntity) {
        if (progress.isComplete) {
            showCompletionNotification(progress.processedImages)
        } else if (progress.isProcessing || progress.isPaused) {
            showProcessingNotification(progress)
        }
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}
