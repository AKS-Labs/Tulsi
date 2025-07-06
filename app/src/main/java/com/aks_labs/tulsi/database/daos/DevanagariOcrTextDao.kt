package com.aks_labs.tulsi.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aks_labs.tulsi.database.entities.DevanagariOcrTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevanagariOcrTextDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrText(ocrText: DevanagariOcrTextEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrTexts(ocrTexts: List<DevanagariOcrTextEntity>)
    
    @Update
    suspend fun updateOcrText(ocrText: DevanagariOcrTextEntity)
    
    @Query("SELECT * FROM devanagari_ocr_text WHERE media_id = :mediaId")
    suspend fun getOcrTextByMediaId(mediaId: Long): DevanagariOcrTextEntity?
    
    @Query("SELECT * FROM devanagari_ocr_text WHERE media_id IN (:mediaIds)")
    suspend fun getOcrTextsByMediaIds(mediaIds: List<Long>): List<DevanagariOcrTextEntity>
    
    @Query("SELECT * FROM devanagari_ocr_text WHERE extracted_text LIKE :searchQuery")
    suspend fun searchOcrText(searchQuery: String): List<DevanagariOcrTextEntity>

    // Fallback search for when FTS fails
    @Query("SELECT * FROM devanagari_ocr_text WHERE extracted_text LIKE '%' || :searchQuery || '%'")
    suspend fun searchOcrTextFallback(searchQuery: String): List<DevanagariOcrTextEntity>
    
    @Query("SELECT COUNT(*) FROM devanagari_ocr_text")
    suspend fun getOcrTextCount(): Int
    
    @Query("SELECT COUNT(*) FROM devanagari_ocr_text WHERE extraction_timestamp > :timestamp")
    suspend fun getOcrTextCountSince(timestamp: Long): Int
    
    @Query("SELECT * FROM devanagari_ocr_text WHERE extraction_timestamp > :timestamp")
    suspend fun getOcrTextsSince(timestamp: Long): List<DevanagariOcrTextEntity>
    
    @Query("DELETE FROM devanagari_ocr_text WHERE media_id = :mediaId")
    suspend fun deleteOcrTextByMediaId(mediaId: Long)
    
    @Query("DELETE FROM devanagari_ocr_text WHERE media_id IN (:mediaIds)")
    suspend fun deleteOcrTextsByMediaIds(mediaIds: List<Long>)
    
    @Query("DELETE FROM devanagari_ocr_text")
    suspend fun deleteAllOcrTexts()
    
    @Query("SELECT * FROM devanagari_ocr_text ORDER BY extraction_timestamp DESC LIMIT :limit")
    suspend fun getRecentOcrTexts(limit: Int): List<DevanagariOcrTextEntity>
    
    @Query("SELECT media_id FROM devanagari_ocr_text")
    suspend fun getAllProcessedMediaIds(): List<Long>
    
    @Query("SELECT AVG(confidence_score) FROM devanagari_ocr_text")
    suspend fun getAverageConfidenceScore(): Float?
    
    @Query("SELECT AVG(processing_time_ms) FROM devanagari_ocr_text")
    suspend fun getAverageProcessingTime(): Long?
}
