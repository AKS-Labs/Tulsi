package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for storing search history
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["search_query"]),
        Index(value = ["search_timestamp"]),
        Index(value = ["search_type"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "search_query")
    val searchQuery: String,
    
    @ColumnInfo(name = "search_timestamp")
    val searchTimestamp: Long,
    
    @ColumnInfo(name = "search_type")
    val searchType: String, // "metadata", "ocr", "combined"
    
    @ColumnInfo(name = "results_count")
    val resultsCount: Int = 0,
    
    @ColumnInfo(name = "frequency_count")
    val frequencyCount: Int = 1
)
