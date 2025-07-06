package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for storing Devanagari OCR extracted text from images
 */
@Entity(
    tableName = "devanagari_ocr_text",
    indices = [
        Index(value = ["media_id"], unique = true),
        Index(value = ["extracted_text"]),
        Index(value = ["extraction_timestamp"])
    ]
)
data class DevanagariOcrTextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    
    @ColumnInfo(name = "extracted_text")
    val extractedText: String,
    
    @ColumnInfo(name = "extraction_timestamp")
    val extractionTimestamp: Long,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float = 0.0f,
    
    @ColumnInfo(name = "text_blocks_count")
    val textBlocksCount: Int = 0,
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long = 0
)
