package com.aks_labs.tulsi.ocr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.database.entities.DevanagariOcrProgressEntity

/**
 * Manages notifications for Devanagari OCR processing
 */
class DevanagariOcrNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "devanagari_ocr_progress"
        private const val NOTIFICATION_ID = 2002 // Different from Latin OCR (2001)
        private const val TAG = "DevanagariOcrNotificationManager"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        Log.d(TAG, "🔔 Initializing DevanagariOcrNotificationManager")
        createNotificationChannel()
        Log.d(TAG, "✅ DevanagariOcrNotificationManager initialized")
    }

    /**
     * Create notification channel for Devanagari OCR progress
     */
    private fun createNotificationChannel() {
        Log.d(TAG, "📱 Creating notification channel for Devanagari OCR...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Android O+ detected, creating notification channel")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Devanagari OCR Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of Devanagari OCR text extraction"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ Notification channel '$CHANNEL_ID' created successfully")
        } else {
            Log.d(TAG, "Android < O, no need to create notification channel")
        }
    }

    /**
     * Show or update progress notification
     */
    fun updateProgress(progress: DevanagariOcrProgressEntity) {
        Log.d(TAG, "🔔 === UPDATING DEVANAGARI OCR NOTIFICATION ===")
        Log.d(TAG, "Progress: ${progress.processedImages}/${progress.totalImages} (${progress.progressPercentage}%)")
        Log.d(TAG, "Is processing: ${progress.isProcessing}, Is paused: ${progress.isPaused}")
        Log.d(TAG, "Progress dismissed: ${progress.progressDismissed}")

        if (!progress.isProcessing && !progress.isPaused) {
            Log.d(TAG, "❌ Not processing and not paused - hiding notification")
            // Hide notification when not processing
            hideNotification()
            return
        }

        Log.d(TAG, "📝 Creating progress notification...")
        val notification = createProgressNotification(progress)
        Log.d(TAG, "📤 Showing notification with ID: $NOTIFICATION_ID")
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Notification displayed successfully")
    }

    /**
     * Create progress notification
     */
    private fun createProgressNotification(progress: DevanagariOcrProgressEntity): Notification {
        Log.d(TAG, "🏗️ Building notification content...")
        val progressPercentage = progress.progressPercentage
        val statusText = when {
            progress.isPaused -> "Paused"
            progress.isProcessing -> "Processing"
            else -> "Completed"
        }

        val title = "Devanagari OCR $statusText"
        val text = "${progress.processedImages}/${progress.totalImages} images processed ($progressPercentage%)"

        Log.d(TAG, "Notification content:")
        Log.d(TAG, "  Title: $title")
        Log.d(TAG, "  Text: $text")
        Log.d(TAG, "  Progress: $progressPercentage%")

        Log.d(TAG, "🔧 Building notification with NotificationCompat.Builder...")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ocr)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, progressPercentage, false)
            .setOngoing(progress.isProcessing)
            .setAutoCancel(!progress.isProcessing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        Log.d(TAG, "Notification properties:")
        Log.d(TAG, "  Channel: $CHANNEL_ID")
        Log.d(TAG, "  Icon: R.drawable.ocr")
        Log.d(TAG, "  Ongoing: ${progress.isProcessing}")
        Log.d(TAG, "  Auto-cancel: ${!progress.isProcessing}")

        // Add estimated time remaining if available
        if (progress.estimatedTimeRemainingMs > 0 && progress.isProcessing) {
            val timeRemaining = formatTimeRemaining(progress.estimatedTimeRemainingMs)
            builder.setSubText("$timeRemaining remaining")
            Log.d(TAG, "Added time remaining: $timeRemaining")
        }

        // Add action buttons
        Log.d(TAG, "Adding notification actions...")
        addNotificationActions(builder, progress)

        Log.d(TAG, "🏗️ Building final notification...")
        val notification = builder.build()
        Log.d(TAG, "✅ Notification built successfully")
        return notification
    }

    /**
     * Add action buttons to notification
     */
    private fun addNotificationActions(
        builder: NotificationCompat.Builder,
        progress: DevanagariOcrProgressEntity
    ) {
        // Pause/Resume action
        if (progress.isProcessing) {
            val pauseIntent = Intent(context, DevanagariOcrNotificationReceiver::class.java).apply {
                action = DevanagariOcrNotificationReceiver.ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.pause, "Pause", pausePendingIntent)
        } else if (progress.isPaused) {
            val resumeIntent = Intent(context, DevanagariOcrNotificationReceiver::class.java).apply {
                action = DevanagariOcrNotificationReceiver.ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.play_arrow, "Resume", resumePendingIntent)
        }

        // View Progress action
        val viewProgressIntent = Intent(context, DevanagariOcrNotificationReceiver::class.java).apply {
            action = DevanagariOcrNotificationReceiver.ACTION_VIEW_PROGRESS
        }
        val viewProgressPendingIntent = PendingIntent.getBroadcast(
            context, 
            3, 
            viewProgressIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.view_day, "View Progress", viewProgressPendingIntent)
    }

    /**
     * Hide the notification
     */
    fun hideNotification() {
        Log.d(TAG, "🚫 Hiding Devanagari OCR notification (ID: $NOTIFICATION_ID)")
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "✅ Notification hidden")
    }

    /**
     * Format time remaining in human-readable format
     */
    private fun formatTimeRemaining(timeMs: Long): String {
        val seconds = timeMs / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    /**
     * Show completion notification
     */
    fun showCompletionNotification(totalProcessed: Int, totalFailed: Int) {
        val title = "Devanagari OCR Complete"
        val text = "Processed $totalProcessed images" + 
                   if (totalFailed > 0) ", $totalFailed failed" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ocr)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Show error notification
     */
    fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.error)
            .setContentTitle("Devanagari OCR Error")
            .setContentText(error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
