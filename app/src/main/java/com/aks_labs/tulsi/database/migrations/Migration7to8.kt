package com.aks_labs.tulsi.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 7 to 8: Add Devanagari OCR tables
 */
val Migration7to8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        android.util.Log.d("Migration7to8", "🔄 === STARTING MIGRATION 7 TO 8 ===")
        android.util.Log.d("Migration7to8", "Adding Devanagari OCR tables...")

        try {
            // Create Devanagari OCR text table
            android.util.Log.d("Migration7to8", "Creating devanagari_ocr_text table...")
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `devanagari_ocr_text` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `media_id` INTEGER NOT NULL,
                    `extracted_text` TEXT NOT NULL,
                    `extraction_timestamp` INTEGER NOT NULL,
                    `confidence_score` REAL NOT NULL DEFAULT 0.0,
                    `text_blocks_count` INTEGER NOT NULL DEFAULT 0,
                    `processing_time_ms` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            android.util.Log.d("Migration7to8", "✅ devanagari_ocr_text table created")

            // Create indices for Devanagari OCR text table
            android.util.Log.d("Migration7to8", "Creating indices for devanagari_ocr_text...")
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS `index_devanagari_ocr_text_media_id`
                ON `devanagari_ocr_text` (`media_id`)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_devanagari_ocr_text_extracted_text`
                ON `devanagari_ocr_text` (`extracted_text`)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_devanagari_ocr_text_extraction_timestamp`
                ON `devanagari_ocr_text` (`extraction_timestamp`)
            """.trimIndent())
            android.util.Log.d("Migration7to8", "✅ Indices created for devanagari_ocr_text")

            // Create Devanagari OCR progress table
            android.util.Log.d("Migration7to8", "Creating devanagari_ocr_progress table...")
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `devanagari_ocr_progress` (
                    `id` INTEGER PRIMARY KEY NOT NULL,
                    `total_images` INTEGER NOT NULL DEFAULT 0,
                    `processed_images` INTEGER NOT NULL DEFAULT 0,
                    `failed_images` INTEGER NOT NULL DEFAULT 0,
                    `is_processing` INTEGER NOT NULL DEFAULT 0,
                    `is_paused` INTEGER NOT NULL DEFAULT 0,
                    `last_updated` INTEGER NOT NULL,
                    `estimated_completion_time` INTEGER NOT NULL DEFAULT 0,
                    `average_processing_time_ms` INTEGER NOT NULL DEFAULT 0,
                    `current_batch_id` TEXT,
                    `progress_dismissed` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            android.util.Log.d("Migration7to8", "✅ devanagari_ocr_progress table created")

            android.util.Log.d("Migration7to8", "✅ === MIGRATION 7 TO 8 COMPLETED SUCCESSFULLY ===")
        } catch (e: Exception) {
            android.util.Log.e("Migration7to8", "❌ Migration 7 to 8 failed", e)
            throw e
        }
    }
}
