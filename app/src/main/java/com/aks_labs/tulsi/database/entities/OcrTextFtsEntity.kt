package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 virtual table for fast full-text search of OCR extracted text
 */
@Entity(tableName = "ocr_text_fts")
@Fts4(contentEntity = OcrTextEntity::class)
data class OcrTextFtsEntity(
    @ColumnInfo(name = "extracted_text")
    val extractedText: String
)
