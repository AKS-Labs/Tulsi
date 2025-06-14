package com.aks_labs.tulsi.ocr

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to initialize OCR functionality
 */
object OcrInitializer {
    
    private const val TAG = "OcrInitializer"
    
    /**
     * Initialize OCR functionality for the app
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing OCR functionality")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = Room.databaseBuilder(
                    context,
                    MediaDatabase::class.java,
                    "media-database"
                ).apply {
                    addMigrations(
                        Migration3to4(context),
                        Migration4to5(context),
                        Migration5to6(context)
                    )
                }.build()
                
                val ocrManager = OcrManager(context, database)
                
                // Check if we need to start background OCR processing
                val ocrStats = ocrManager.getOcrStats()
                Log.d(TAG, "OCR Stats - Total processed: ${ocrStats.totalProcessed}, Avg confidence: ${ocrStats.averageConfidence}")
                
                // Optionally start background processing for unprocessed images
                // This could be made configurable via settings
                if (ocrStats.totalProcessed == 0) {
                    Log.d(TAG, "Starting initial OCR batch processing")
                    ocrManager.processBatch(batchSize = 5) // Start with small batch
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OCR", e)
            }
        }
    }
    
    /**
     * Start OCR processing for a specific image
     */
    fun processImage(context: Context, mediaId: Long, mediaUri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = Room.databaseBuilder(
                    context,
                    MediaDatabase::class.java,
                    "media-database"
                ).apply {
                    addMigrations(
                        Migration3to4(context),
                        Migration4to5(context),
                        Migration5to6(context)
                    )
                }.build()
                
                val ocrManager = OcrManager(context, database)
                
                // Check if already processed
                if (!ocrManager.isImageProcessed(mediaId)) {
                    Log.d(TAG, "Starting OCR processing for image: $mediaId")
                    // This would need a MediaStoreData object, simplified for now
                    // ocrManager.processImage(mediaItem)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image for OCR", e)
            }
        }
    }
}
