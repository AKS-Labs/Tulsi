package com.aks_labs.tulsi.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aks_labs.tulsi.database.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistoryEntity): Long
    
    @Update
    suspend fun updateSearchHistory(searchHistory: SearchHistoryEntity)
    
    @Query("SELECT * FROM search_history WHERE search_query = :query AND search_type = :type")
    suspend fun getSearchHistory(query: String, type: String): SearchHistoryEntity?
    
    @Query("SELECT * FROM search_history ORDER BY search_timestamp DESC LIMIT :limit")
    suspend fun getRecentSearchHistory(limit: Int): List<SearchHistoryEntity>
    
    @Query("SELECT * FROM search_history ORDER BY frequency_count DESC, search_timestamp DESC LIMIT :limit")
    suspend fun getPopularSearchHistory(limit: Int): List<SearchHistoryEntity>
    
    @Query("SELECT DISTINCT search_query FROM search_history WHERE search_query LIKE :prefix || '%' ORDER BY frequency_count DESC, search_timestamp DESC LIMIT :limit")
    suspend fun getSearchSuggestions(prefix: String, limit: Int): List<String>
    
    @Query("SELECT * FROM search_history WHERE search_type = :type ORDER BY search_timestamp DESC LIMIT :limit")
    suspend fun getSearchHistoryByType(type: String, limit: Int): List<SearchHistoryEntity>
    
    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistory(id: Long)
    
    @Query("DELETE FROM search_history WHERE search_timestamp < :timestamp")
    suspend fun deleteOldSearchHistory(timestamp: Long)
    
    @Query("DELETE FROM search_history")
    suspend fun deleteAllSearchHistory()
    
    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getSearchHistoryCount(): Int
    
    @Query("UPDATE search_history SET frequency_count = frequency_count + 1, search_timestamp = :timestamp WHERE search_query = :query AND search_type = :type")
    suspend fun incrementSearchFrequency(query: String, type: String, timestamp: Long): Int
}
