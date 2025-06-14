package com.aks_labs.tulsi.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aks_labs.tulsi.database.entities.OcrTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrTextDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrText(ocrText: OcrTextEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrTexts(ocrTexts: List<OcrTextEntity>)
    
    @Update
    suspend fun updateOcrText(ocrText: OcrTextEntity)
    
    @Query("SELECT * FROM ocr_text WHERE media_id = :mediaId")
    suspend fun getOcrTextByMediaId(mediaId: Long): OcrTextEntity?
    
    @Query("SELECT * FROM ocr_text WHERE media_id IN (:mediaIds)")
    suspend fun getOcrTextsByMediaIds(mediaIds: List<Long>): List<OcrTextEntity>
    
    @Query("SELECT * FROM ocr_text WHERE extracted_text LIKE :searchQuery")
    suspend fun searchOcrText(searchQuery: String): List<OcrTextEntity>

    // FTS search temporarily disabled - will be re-enabled later
    /*
    @Query("""
        SELECT ocr.* FROM ocr_text ocr
        JOIN ocr_text_fts fts ON ocr.rowid = fts.rowid
        WHERE ocr_text_fts MATCH :searchQuery
    """)
    suspend fun searchOcrTextFts(searchQuery: String): List<OcrTextEntity>
    */

    // Fallback search for when FTS fails
    @Query("SELECT * FROM ocr_text WHERE extracted_text LIKE '%' || :searchQuery || '%'")
    suspend fun searchOcrTextFallback(searchQuery: String): List<OcrTextEntity>
    
    @Query("SELECT COUNT(*) FROM ocr_text")
    suspend fun getOcrTextCount(): Int
    
    @Query("SELECT COUNT(*) FROM ocr_text WHERE extraction_timestamp > :timestamp")
    suspend fun getOcrTextCountSince(timestamp: Long): Int
    
    @Query("SELECT * FROM ocr_text WHERE extraction_timestamp > :timestamp")
    suspend fun getOcrTextsSince(timestamp: Long): List<OcrTextEntity>
    
    @Query("DELETE FROM ocr_text WHERE media_id = :mediaId")
    suspend fun deleteOcrTextByMediaId(mediaId: Long)
    
    @Query("DELETE FROM ocr_text WHERE media_id IN (:mediaIds)")
    suspend fun deleteOcrTextsByMediaIds(mediaIds: List<Long>)
    
    @Query("DELETE FROM ocr_text")
    suspend fun deleteAllOcrTexts()
    
    @Query("SELECT * FROM ocr_text ORDER BY extraction_timestamp DESC LIMIT :limit")
    suspend fun getRecentOcrTexts(limit: Int): List<OcrTextEntity>
    
    @Query("SELECT media_id FROM ocr_text")
    suspend fun getAllProcessedMediaIds(): List<Long>
    
    @Query("SELECT AVG(confidence_score) FROM ocr_text")
    suspend fun getAverageConfidenceScore(): Float?
    
    @Query("SELECT AVG(processing_time_ms) FROM ocr_text")
    suspend fun getAverageProcessingTime(): Long?
}
