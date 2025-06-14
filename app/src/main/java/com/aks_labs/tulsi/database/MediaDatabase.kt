package com.aks_labs.tulsi.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.aks_labs.tulsi.database.daos.FavouritedItemEntityDao
import com.aks_labs.tulsi.database.daos.MediaEntityDao
import com.aks_labs.tulsi.database.daos.OcrProgressDao
import com.aks_labs.tulsi.database.daos.OcrTextDao
import com.aks_labs.tulsi.database.daos.SearchHistoryDao
import com.aks_labs.tulsi.database.daos.SecuredMediaItemEntityDao
import com.aks_labs.tulsi.database.daos.TrashedItemEntityDao
import com.aks_labs.tulsi.database.entities.FavouritedItemEntity
import com.aks_labs.tulsi.database.entities.MediaEntity
import com.aks_labs.tulsi.database.entities.OcrProgressEntity
import com.aks_labs.tulsi.database.entities.OcrTextEntity
import com.aks_labs.tulsi.database.entities.OcrTextFtsEntity
import com.aks_labs.tulsi.database.entities.SearchHistoryEntity
import com.aks_labs.tulsi.database.entities.SecuredItemEntity
import com.aks_labs.tulsi.database.entities.TrashedItemEntity

@Database(entities =
    [
        MediaEntity::class,
        TrashedItemEntity::class,
        FavouritedItemEntity::class,
        SecuredItemEntity::class,
        OcrTextEntity::class,
        OcrProgressEntity::class,
        // OcrTextFtsEntity::class, // Temporarily disabled for build compatibility
        SearchHistoryEntity::class
    ],
    version = 7, // Keep at version 7 for now
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        // AutoMigration(from = 4, to = 5) - Manual migration needed
        // AutoMigration(from = 5, to = 6) - Manual migration needed for FTS
    ]
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun favouritedItemEntityDao(): FavouritedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao
    abstract fun ocrTextDao(): OcrTextDao
    abstract fun ocrProgressDao(): OcrProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}


