package com.aks_labs.tulsi.ocr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.WorkManager
import com.aks_labs.tulsi.MainActivity
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import com.aks_labs.tulsi.datastore.Settings
import com.aks_labs.tulsi.datastore.Ocr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that ensures OCR processing continues even when the app is force-closed
 */
class OcrForegroundService : Service() {

    companion object {
        private const val TAG = "OcrForegroundService"
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "ocr_foreground_service"
        
        // Service actions
        const val ACTION_START_LATIN_OCR = "start_latin_ocr"
        const val ACTION_START_DEVANAGARI_OCR = "start_devanagari_ocr"
        const val ACTION_STOP_LATIN_OCR = "stop_latin_ocr"
        const val ACTION_STOP_DEVANAGARI_OCR = "stop_devanagari_ocr"
        const val ACTION_STOP_SERVICE = "stop_service"
        
        // Service state
        private var isServiceRunning = false
        
        fun isRunning(): Boolean = isServiceRunning
        
        /**
         * Start the foreground service for Latin OCR
         */
        fun startLatinOcr(context: Context) {
            val intent = Intent(context, OcrForegroundService::class.java).apply {
                action = ACTION_START_LATIN_OCR
            }
            context.startForegroundService(intent)
        }
        
        /**
         * Start the foreground service for Devanagari OCR
         */
        fun startDevanagariOcr(context: Context) {
            val intent = Intent(context, OcrForegroundService::class.java).apply {
                action = ACTION_START_DEVANAGARI_OCR
            }
            context.startForegroundService(intent)
        }
        
        /**
         * Stop Latin OCR processing
         */
        fun stopLatinOcr(context: Context) {
            val intent = Intent(context, OcrForegroundService::class.java).apply {
                action = ACTION_STOP_LATIN_OCR
            }
            context.startService(intent)
        }
        
        /**
         * Stop Devanagari OCR processing
         */
        fun stopDevanagariOcr(context: Context) {
            val intent = Intent(context, OcrForegroundService::class.java).apply {
                action = ACTION_STOP_DEVANAGARI_OCR
            }
            context.startService(intent)
        }
        
        /**
         * Stop the entire service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, OcrForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var database: MediaDatabase
    private lateinit var settings: Settings
    private lateinit var workManager: WorkManager
    
    private var latinOcrManager: OcrManager? = null
    private var devanagariOcrManager: DevanagariOcrManager? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var latinOcrMonitorJob: Job? = null
    private var devanagariOcrMonitorJob: Job? = null
    
    private var isLatinOcrActive = false
    private var isDevanagariOcrActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 OCR Foreground Service created")
        
        isServiceRunning = true
        
        // Initialize components
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        workManager = WorkManager.getInstance(this)
        
        // Initialize database
        database = Room.databaseBuilder(
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
        
        // Initialize settings
        settings = Settings(applicationContext, serviceScope)
        
        // Create notification channel
        createNotificationChannel()
        
        // Acquire wake lock to prevent device from sleeping during OCR processing
        acquireWakeLock()
        
        Log.d(TAG, "✅ OCR Foreground Service initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📨 Service command received: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_LATIN_OCR -> startLatinOcrProcessing()
            ACTION_START_DEVANAGARI_OCR -> startDevanagariOcrProcessing()
            ACTION_STOP_LATIN_OCR -> stopLatinOcrProcessing()
            ACTION_STOP_DEVANAGARI_OCR -> stopDevanagariOcrProcessing()
            ACTION_STOP_SERVICE -> stopSelf()
            else -> {
                // Default: start with a basic notification
                startForeground(NOTIFICATION_ID, createServiceNotification())
            }
        }
        
        // Return START_STICKY to ensure service restarts if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "🛑 OCR Foreground Service destroyed")
        
        isServiceRunning = false
        
        // Cancel all monitoring jobs
        latinOcrMonitorJob?.cancel()
        devanagariOcrMonitorJob?.cancel()
        
        // Cancel service scope
        serviceScope.cancel()
        
        // Release wake lock
        releaseWakeLock()
        
        super.onDestroy()
    }

    /**
     * Start Latin OCR processing
     */
    private fun startLatinOcrProcessing() {
        Log.d(TAG, "🔤 Starting Latin OCR processing in foreground service")
        
        if (isLatinOcrActive) {
            Log.d(TAG, "Latin OCR already active")
            return
        }
        
        isLatinOcrActive = true
        
        serviceScope.launch {
            try {
                // Check if Latin OCR is enabled
                val isEnabled = settings.Ocr.latinOcrEnabled.first()
                if (!isEnabled) {
                    Log.d(TAG, "Latin OCR is disabled in settings")
                    isLatinOcrActive = false
                    checkIfServiceShouldStop()
                    return@launch
                }
                
                // Initialize Latin OCR manager
                latinOcrManager = OcrManager(applicationContext, database)
                
                // Start foreground with notification
                startForeground(NOTIFICATION_ID, createServiceNotification())
                
                // Start continuous processing
                latinOcrManager?.startContinuousProcessing()
                
                // Start monitoring Latin OCR progress
                startLatinOcrMonitoring()
                
                Log.d(TAG, "✅ Latin OCR processing started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start Latin OCR processing", e)
                isLatinOcrActive = false
                checkIfServiceShouldStop()
            }
        }
    }

    /**
     * Start Devanagari OCR processing
     */
    private fun startDevanagariOcrProcessing() {
        Log.d(TAG, "🔤 Starting Devanagari OCR processing in foreground service")
        
        if (isDevanagariOcrActive) {
            Log.d(TAG, "Devanagari OCR already active")
            return
        }
        
        isDevanagariOcrActive = true
        
        serviceScope.launch {
            try {
                // Check if Devanagari OCR is enabled
                val isEnabled = settings.Ocr.devanagariOcrEnabled.first()
                if (!isEnabled) {
                    Log.d(TAG, "Devanagari OCR is disabled in settings")
                    isDevanagariOcrActive = false
                    checkIfServiceShouldStop()
                    return@launch
                }
                
                // Initialize Devanagari OCR manager
                devanagariOcrManager = DevanagariOcrManager(applicationContext, database)
                
                // Start foreground with notification if not already started
                if (!isLatinOcrActive) {
                    startForeground(NOTIFICATION_ID, createServiceNotification())
                }
                
                // Start continuous processing
                devanagariOcrManager?.startContinuousProcessing()
                
                // Start monitoring Devanagari OCR progress
                startDevanagariOcrMonitoring()
                
                Log.d(TAG, "✅ Devanagari OCR processing started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start Devanagari OCR processing", e)
                isDevanagariOcrActive = false
                checkIfServiceShouldStop()
            }
        }
    }

    /**
     * Stop Latin OCR processing
     */
    private fun stopLatinOcrProcessing() {
        Log.d(TAG, "🛑 Stopping Latin OCR processing")

        isLatinOcrActive = false
        latinOcrMonitorJob?.cancel()

        serviceScope.launch {
            try {
                latinOcrManager?.cancelAllOcr()
                latinOcrManager = null
                Log.d(TAG, "✅ Latin OCR processing stopped")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error stopping Latin OCR", e)
            }

            checkIfServiceShouldStop()
        }
    }

    /**
     * Stop Devanagari OCR processing
     */
    private fun stopDevanagariOcrProcessing() {
        Log.d(TAG, "🛑 Stopping Devanagari OCR processing")

        isDevanagariOcrActive = false
        devanagariOcrMonitorJob?.cancel()

        serviceScope.launch {
            try {
                devanagariOcrManager?.cancelAllOcr()
                devanagariOcrManager = null
                Log.d(TAG, "✅ Devanagari OCR processing stopped")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error stopping Devanagari OCR", e)
            }

            checkIfServiceShouldStop()
        }
    }

    /**
     * Check if service should stop (when no OCR processing is active)
     */
    private fun checkIfServiceShouldStop() {
        if (!isLatinOcrActive && !isDevanagariOcrActive) {
            Log.d(TAG, "🛑 No OCR processing active, stopping service")
            stopSelf()
        } else {
            // Update notification to reflect current state
            updateServiceNotification()
        }
    }

    /**
     * Start monitoring Latin OCR progress
     */
    private fun startLatinOcrMonitoring() {
        latinOcrMonitorJob?.cancel()
        latinOcrMonitorJob = serviceScope.launch {
            try {
                while (isLatinOcrActive) {
                    val progress = database.ocrProgressDao().getProgress()
                    if (progress != null && progress.isProcessing) {
                        updateServiceNotification()
                    } else if (progress != null && progress.isComplete) {
                        Log.d(TAG, "Latin OCR processing completed")
                        isLatinOcrActive = false
                        break
                    }
                    delay(2000) // Update every 2 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Latin OCR monitoring", e)
            }

            checkIfServiceShouldStop()
        }
    }

    /**
     * Start monitoring Devanagari OCR progress
     */
    private fun startDevanagariOcrMonitoring() {
        devanagariOcrMonitorJob?.cancel()
        devanagariOcrMonitorJob = serviceScope.launch {
            try {
                while (isDevanagariOcrActive) {
                    val progress = database.devanagariOcrProgressDao().getProgress()
                    if (progress != null && progress.isProcessing) {
                        updateServiceNotification()
                    } else if (progress != null && progress.isComplete) {
                        Log.d(TAG, "Devanagari OCR processing completed")
                        isDevanagariOcrActive = false
                        break
                    }
                    delay(2000) // Update every 2 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Devanagari OCR monitoring", e)
            }

            checkIfServiceShouldStop()
        }
    }

    /**
     * Create notification channel for the foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OCR Background Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps OCR processing running in background"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create the service notification
     */
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_ocr_settings", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isLatinOcrActive && isDevanagariOcrActive -> "OCR Processing (Latin + Devanagari)"
            isLatinOcrActive -> "OCR Processing (Latin)"
            isDevanagariOcrActive -> "OCR Processing (Devanagari)"
            else -> "OCR Service Running"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Processing images in background")
            .setSmallIcon(R.drawable.ocr)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()
    }

    /**
     * Update the service notification with current progress
     */
    private fun updateServiceNotification() {
        serviceScope.launch {
            try {
                val notification = createServiceNotification()
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update service notification", e)
            }
        }
    }

    /**
     * Acquire wake lock to prevent device from sleeping during processing
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TulsiGallery::OcrForegroundService"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout
            }
            Log.d(TAG, "🔋 Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "🔋 Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
}
