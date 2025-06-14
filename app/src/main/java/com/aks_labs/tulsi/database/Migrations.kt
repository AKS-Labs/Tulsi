package com.aks_labs.tulsi.database

import android.content.ContentValues
import android.content.Context
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aks_labs.tulsi.database.entities.FavouritedItemEntity
import com.aks_labs.tulsi.mediastore.toMediaType
import com.aks_labs.tulsi.mediastore.getUriFromAbsolutePath

class Migration3to4(val context: Context) : Migration(startVersion = 3, endVersion = 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val favItems = db.query("SELECT * FROM favouriteditementity")
        val newFavItems = emptyList<FavouritedItemEntity>().toMutableList()

        val idNum = favItems.getColumnIndexOrThrow("id")
        val dateTakenNum = favItems.getColumnIndexOrThrow("date_taken")
        val mimeTypeNum = favItems.getColumnIndexOrThrow("mime_type")
        val displayNameNum = favItems.getColumnIndexOrThrow("display_name")
        val absolutePathNum = favItems.getColumnIndexOrThrow("absolute_path")
        val typeNum = favItems.getColumnIndexOrThrow("type")
        val dateModifiedNum = favItems.getColumnIndexOrThrow("date_modified")

        while(favItems.moveToNext()) {
            val id = favItems.getLong(idNum)
            val dateTaken = favItems.getLong(dateTakenNum)
            val mimeType = favItems.getString(mimeTypeNum)
            val displayName = favItems.getString(displayNameNum)
            val absolutePath = favItems.getString(absolutePathNum)
            val type = favItems.getString(typeNum).toMediaType()
            val dateModified = favItems.getLong(dateModifiedNum)

            val uri = context.contentResolver.getUriFromAbsolutePath(absolutePath = absolutePath, type = type).toString()

            newFavItems.add(
                FavouritedItemEntity(
                    id = id,
                    dateTaken = dateTaken,
                    mimeType = mimeType,
                    displayName = displayName,
                    absolutePath = absolutePath,
                    type = type,
                    dateModified = dateModified,
                    uri = uri
                )
            )
        }

        db.execSQL("DROP TABLE favouriteditementity")
        db.execSQL("CREATE TABLE `favouriteditementity` (`id` INTEGER NOT NULL, `date_taken` INTEGER NOT NULL, `mime_type` TEXT NOT NULL, `display_name` TEXT NOT NULL, `absolute_path` TEXT NOT NULL, `type` TEXT NOT NULL, `date_modified` INTEGER NOT NULL, `uri` TEXT NOT NULL, PRIMARY KEY(`id`))")

        newFavItems.forEach { item ->
            db.insert(
                "favouriteditementity",
                OnConflictStrategy.REPLACE,
                ContentValues().apply {
                    put("id", item.id)
                    put("date_taken", item.dateTaken)
                    put("mime_type", item.mimeType)
                    put("display_name", item.displayName)
                    put("absolute_path", item.absolutePath)
                    put("type", item.type.toString())
                    put("date_modified", item.dateModified)
                    put("uri", item.uri)
                }
            )
        }
    }
}

class Migration4to5(val context: Context) : Migration(startVersion = 4, endVersion = 5) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("DROP TABLE secureditementity")
		db.execSQL("CREATE TABLE IF NOT EXISTS `secureditementity` (`originalPath` TEXT NOT NULL, `secured_path` TEXT NOT NULL, `iv` BLOB NOT NULL, PRIMARY KEY(`originalPath`))")
	}
}

class Migration5to6(val context: Context) : Migration(startVersion = 5, endVersion = 6) {
	override fun migrate(db: SupportSQLiteDatabase) {
		// Create OCR text table
		db.execSQL("""
			CREATE TABLE IF NOT EXISTS `ocr_text` (
				`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
				`media_id` INTEGER NOT NULL,
				`extracted_text` TEXT NOT NULL,
				`extraction_timestamp` INTEGER NOT NULL,
				`confidence_score` REAL NOT NULL DEFAULT 0.0,
				`text_blocks_count` INTEGER NOT NULL DEFAULT 0,
				`processing_time_ms` INTEGER NOT NULL DEFAULT 0
			)
		""")

		// Create indexes for OCR text table
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ocr_text_media_id` ON `ocr_text` (`media_id`)")
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_text_extracted_text` ON `ocr_text` (`extracted_text`)")
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_text_extraction_timestamp` ON `ocr_text` (`extraction_timestamp`)")

		// Create FTS virtual table for OCR text search
		// Temporarily disabled due to FTS validation issues - will implement in next version
		/*
		db.execSQL("""
			CREATE VIRTUAL TABLE IF NOT EXISTS `ocr_text_fts` USING fts4(
				content=`ocr_text`,
				extracted_text
			)
		""")
		*/

		// Create search history table
		db.execSQL("""
			CREATE TABLE IF NOT EXISTS `search_history` (
				`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
				`search_query` TEXT NOT NULL,
				`search_timestamp` INTEGER NOT NULL,
				`search_type` TEXT NOT NULL,
				`results_count` INTEGER NOT NULL DEFAULT 0,
				`frequency_count` INTEGER NOT NULL DEFAULT 1
			)
		""")

		// Create indexes for search history table
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_query` ON `search_history` (`search_query`)")
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_timestamp` ON `search_history` (`search_timestamp`)")
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_type` ON `search_history` (`search_type`)")

	}
}

class Migration6to7(val context: Context) : Migration(startVersion = 6, endVersion = 7) {
	override fun migrate(db: SupportSQLiteDatabase) {
		// Create OCR progress table
		db.execSQL("""
			CREATE TABLE IF NOT EXISTS `ocr_progress` (
				`id` INTEGER PRIMARY KEY NOT NULL,
				`total_images` INTEGER NOT NULL DEFAULT 0,
				`processed_images` INTEGER NOT NULL DEFAULT 0,
				`failed_images` INTEGER NOT NULL DEFAULT 0,
				`is_processing` INTEGER NOT NULL DEFAULT 0,
				`is_paused` INTEGER NOT NULL DEFAULT 0,
				`last_updated` INTEGER NOT NULL DEFAULT 0,
				`estimated_completion_time` INTEGER NOT NULL DEFAULT 0,
				`average_processing_time_ms` INTEGER NOT NULL DEFAULT 0,
				`current_batch_id` TEXT,
				`progress_dismissed` INTEGER NOT NULL DEFAULT 0
			)
		""")

		// Insert initial progress row
		db.execSQL("""
			INSERT OR IGNORE INTO `ocr_progress`
			(`id`, `total_images`, `processed_images`, `last_updated`)
			VALUES (1, 0, 0, ${System.currentTimeMillis() / 1000})
		""")
	}
}

class Migration7to8(val context: Context) : Migration(startVersion = 7, endVersion = 8) {
	override fun migrate(db: SupportSQLiteDatabase) {
		try {
			// Create FTS virtual table for OCR text search
			db.execSQL("""
				CREATE VIRTUAL TABLE IF NOT EXISTS `ocr_text_fts` USING fts4(
					content=`ocr_text`,
					extracted_text
				)
			""")

			// Populate FTS table with existing OCR data
			db.execSQL("""
				INSERT INTO `ocr_text_fts`(`docid`, `extracted_text`)
				SELECT `rowid`, `extracted_text` FROM `ocr_text`
			""")

			// Create triggers to keep FTS table in sync
			db.execSQL("""
				CREATE TRIGGER IF NOT EXISTS ocr_text_fts_insert AFTER INSERT ON ocr_text BEGIN
					INSERT INTO ocr_text_fts(docid, extracted_text) VALUES (new.rowid, new.extracted_text);
				END
			""")

			db.execSQL("""
				CREATE TRIGGER IF NOT EXISTS ocr_text_fts_delete AFTER DELETE ON ocr_text BEGIN
					INSERT INTO ocr_text_fts(ocr_text_fts) VALUES('delete-all');
					INSERT INTO ocr_text_fts(docid, extracted_text) SELECT rowid, extracted_text FROM ocr_text;
				END
			""")

			db.execSQL("""
				CREATE TRIGGER IF NOT EXISTS ocr_text_fts_update AFTER UPDATE ON ocr_text BEGIN
					INSERT INTO ocr_text_fts(ocr_text_fts) VALUES('delete-all');
					INSERT INTO ocr_text_fts(docid, extracted_text) SELECT rowid, extracted_text FROM ocr_text;
				END
			""")

		} catch (e: Exception) {
			// If FTS creation fails, we'll continue without it
			// The search will fall back to LIKE queries
		}
	}
}


