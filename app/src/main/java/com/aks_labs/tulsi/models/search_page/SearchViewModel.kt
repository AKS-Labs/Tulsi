package com.aks_labs.tulsi.models.search_page

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import com.aks_labs.tulsi.database.entities.SearchHistoryEntity
import com.aks_labs.tulsi.datastore.SQLiteQuery
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MultiAlbumDataSource
import com.aks_labs.tulsi.ocr.SimpleOcrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(context: Context, sortBy: MediaItemSortMode) : ViewModel() {
	private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
				    MultiAlbumDataSource(
				    	context = context,
				    	queryString = SQLiteQuery(query = "", paths = null),
				    	sortBy = sortBy,
				    	cancellationSignal = cancellationSignal
				    )

    // Simple OCR service
    private val ocrService = SimpleOcrService.getInstance(context)

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    // Search suggestions flow
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Search history flow
    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntity>> = _searchHistory.asStateFlow()

    // OCR search results flow
    private val _ocrSearchResults = MutableStateFlow<List<MediaStoreData>>(emptyList())
    val ocrSearchResults: StateFlow<List<MediaStoreData>> = _ocrSearchResults.asStateFlow()

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> = mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    fun cancelMediaFlow() = cancellationSignal.cancel()

    /**
     * Search images by OCR text content
     */
    suspend fun searchByOcrText(query: String): List<Long> {
        if (query.isBlank()) {
            _ocrSearchResults.value = emptyList()
            return emptyList()
        }

        return try {
            val mediaIds = ocrService.searchImagesByText(query)

            // Filter current media items by OCR results
            val currentMedia = mediaFlow.value
            val filteredMedia = currentMedia.filter { mediaItem ->
                mediaIds.contains(mediaItem.id)
            }

            _ocrSearchResults.value = filteredMedia

            // Save search to history
            saveSearchToHistory(query, "ocr", filteredMedia.size)

            mediaIds
        } catch (e: Exception) {
            _ocrSearchResults.value = emptyList()
            emptyList()
        }
    }

    /**
     * Get search suggestions based on input
     */
    fun getSearchSuggestions(input: String) {
        if (input.length < 2) {
            _searchSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // For now, we'll skip search suggestions since we removed the database dependency
                // You can implement this later if needed
                _searchSuggestions.value = emptyList()
            } catch (e: Exception) {
                _searchSuggestions.value = emptyList()
            }
        }
    }

    /**
     * Load recent search history
     */
    fun loadSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // For now, we'll skip search history since we simplified the approach
                // You can implement this later if needed
                _searchHistory.value = emptyList()
            } catch (e: Exception) {
                _searchHistory.value = emptyList()
            }
        }
    }

    /**
     * Save search query to history
     */
    private suspend fun saveSearchToHistory(query: String, type: String, resultsCount: Int) {
        try {
            // For now, we'll skip saving search history since we simplified the approach
            // You can implement this later if needed
        } catch (e: Exception) {
            // Ignore history save errors
        }
    }

    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // For now, we'll skip clearing search history since we simplified the approach
                _searchHistory.value = emptyList()
            } catch (e: Exception) {
                // Ignore clear errors
            }
        }
    }

    /**
     * Start OCR processing for an image
     */
    fun processImageForOcr(mediaItem: MediaStoreData) {
        ocrService.processImage(mediaItem.id, mediaItem.uri)
    }

    /**
     * Start batch OCR processing
     */
    fun startBatchOcrProcessing() {
        ocrService.startOcrProcessing()
    }

    /**
     * Get OCR processing statistics
     */
    fun getOcrStats() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val stats = ocrService.getProcessingStats()
            // You can expose this via StateFlow if needed
        } catch (e: Exception) {
            // Handle error
        }
    }
}


