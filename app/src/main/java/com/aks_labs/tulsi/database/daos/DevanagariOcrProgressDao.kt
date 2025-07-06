package com.aks_labs.tulsi.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aks_labs.tulsi.database.entities.DevanagariOcrProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevanagariOcrProgressDao {
    
    @Query("SELECT * FROM devanagari_ocr_progress WHERE id = 1")
    suspend fun getProgress(): DevanagariOcrProgressEntity?
    
    @Query("SELECT * FROM devanagari_ocr_progress WHERE id = 1")
    fun getProgressFlow(): Flow<DevanagariOcrProgressEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DevanagariOcrProgressEntity)
    
    @Update
    suspend fun updateProgress(progress: DevanagariOcrProgressEntity)
    
    @Query("UPDATE devanagari_ocr_progress SET processed_images = :processedImages, last_updated = :timestamp WHERE id = 1")
    suspend fun updateProcessedCount(processedImages: Int, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET total_images = :totalImages, last_updated = :timestamp WHERE id = 1")
    suspend fun updateTotalCount(totalImages: Int, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET is_processing = :isProcessing, last_updated = :timestamp WHERE id = 1")
    suspend fun updateProcessingStatus(isProcessing: Boolean, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET is_paused = :isPaused, last_updated = :timestamp WHERE id = 1")
    suspend fun updatePausedStatus(isPaused: Boolean, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET progress_dismissed = :dismissed, last_updated = :timestamp WHERE id = 1")
    suspend fun updateDismissedStatus(dismissed: Boolean, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET average_processing_time_ms = :avgTime, estimated_completion_time = :estimatedTime, last_updated = :timestamp WHERE id = 1")
    suspend fun updateTimingInfo(avgTime: Long, estimatedTime: Long, timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("UPDATE devanagari_ocr_progress SET failed_images = failed_images + 1, last_updated = :timestamp WHERE id = 1")
    suspend fun incrementFailedCount(timestamp: Long = System.currentTimeMillis() / 1000)
    
    @Query("DELETE FROM devanagari_ocr_progress")
    suspend fun clearProgress()
    
    @Query("SELECT processed_images FROM devanagari_ocr_progress WHERE id = 1")
    suspend fun getProcessedCount(): Int?
    
    @Query("SELECT total_images FROM devanagari_ocr_progress WHERE id = 1")
    suspend fun getTotalCount(): Int?
    
    @Query("SELECT is_processing FROM devanagari_ocr_progress WHERE id = 1")
    suspend fun isProcessing(): Boolean?
    
    @Query("SELECT is_paused FROM devanagari_ocr_progress WHERE id = 1")
    suspend fun isPaused(): Boolean?
}
