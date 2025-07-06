package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking Devanagari OCR processing progress
 */
@Entity(tableName = "devanagari_ocr_progress")
data class DevanagariOcrProgressEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for global progress
    
    @ColumnInfo(name = "total_images")
    val totalImages: Int = 0,
    
    @ColumnInfo(name = "processed_images")
    val processedImages: Int = 0,
    
    @ColumnInfo(name = "failed_images")
    val failedImages: Int = 0,
    
    @ColumnInfo(name = "is_processing")
    val isProcessing: Boolean = false,
    
    @ColumnInfo(name = "is_paused")
    val isPaused: Boolean = false,
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis() / 1000,
    
    @ColumnInfo(name = "estimated_completion_time")
    val estimatedCompletionTime: Long = 0,
    
    @ColumnInfo(name = "average_processing_time_ms")
    val averageProcessingTimeMs: Long = 0,
    
    @ColumnInfo(name = "current_batch_id")
    val currentBatchId: String? = null,
    
    @ColumnInfo(name = "progress_dismissed")
    val progressDismissed: Boolean = false
) {
    val progressPercentage: Int
        get() = if (totalImages > 0) ((processedImages * 100) / totalImages) else 0
    
    val remainingImages: Int
        get() = totalImages - processedImages
    
    val isComplete: Boolean
        get() = totalImages > 0 && processedImages >= totalImages
    
    val estimatedTimeRemainingMs: Long
        get() = if (averageProcessingTimeMs > 0 && remainingImages > 0) {
            remainingImages * averageProcessingTimeMs
        } else 0
}
