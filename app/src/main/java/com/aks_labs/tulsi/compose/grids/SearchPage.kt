package com.aks_labs.tulsi.compose.grids

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.room.Room
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.compose.SearchTextField
import com.aks_labs.tulsi.compose.ViewProperties
import com.aks_labs.tulsi.compose.components.OcrProgressBar
import com.aks_labs.tulsi.compose.components.SearchBar
import com.aks_labs.tulsi.compose.utils.DynamicStatusBarController
import com.aks_labs.tulsi.compose.utils.ScrollVisibilityState
import com.aks_labs.tulsi.compose.utils.handleScrollVisibilityChange
import com.aks_labs.tulsi.compose.utils.handleBottomBarScrollVisibilityChange
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import com.aks_labs.tulsi.ocr.OcrManager
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.datastore.DefaultTabs
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.MultiScreenViewType
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.models.multi_album.groupGalleryBy
import com.aks_labs.tulsi.models.search_page.SearchViewModel
import com.aks_labs.tulsi.models.search_page.SearchViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "BOTTOM_BAR_ANIMATION"

@Composable
fun SearchPage(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    onTopBarVisibilityChange: (Boolean) -> Unit = {},
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
) {
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(LocalContext.current, MediaItemSortMode.DateTaken)
    )
    val mediaStoreDataHolder =
        searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val originalGroupedMedia = remember { derivedStateOf { mediaStoreDataHolder.value } }

    val groupedMedia = remember { mutableStateOf(originalGroupedMedia.value) }

    LaunchedEffect(groupedMedia.value) {
        mainViewModel.setGroupedMedia(groupedMedia.value)
    }

    val gridState = rememberLazyGridState()
    val navController = LocalNavController.current

    // Auto-hide search bar and status bar on scroll
    var scrollVisibilityState by remember { mutableStateOf(ScrollVisibilityState()) }
    var lastScrollIndex by remember { mutableStateOf(0) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    // Track immersive mode for proper window insets handling
    val isImmersiveMode = !scrollVisibilityState.isAppBarVisible

    // Dynamic status bar controller
    DynamicStatusBarController(isVisible = scrollVisibilityState.isStatusBarVisible)

    // Auto-collapse filter chips after 10 seconds of inactivity
    LaunchedEffect(showFilterDropdown) {
        if (showFilterDropdown) {
            delay(10000) // 10 seconds
            showFilterDropdown = false
        }
    }

    // Monitor scroll state for auto-hide functionality
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        val currentIndex = gridState.firstVisibleItemIndex
        Log.d(TAG, "Search: LaunchedEffect triggered - firstVisibleItemIndex=$currentIndex")
        Log.d(TAG, "Search: Scroll detected - currentIndex=$currentIndex, lastIndex=$lastScrollIndex")

        // Handle scroll visibility changes for both app bar and status bar
        handleScrollVisibilityChange(
            currentIndex = currentIndex,
            lastScrollIndex = lastScrollIndex,
            onVisibilityChange = { newState ->
                scrollVisibilityState = newState
                onTopBarVisibilityChange(newState.isAppBarVisible)

                // Hide filter chips when scrolling or when bars are hidden
                if (!newState.isAppBarVisible || currentIndex != lastScrollIndex) {
                    showFilterDropdown = false
                }
            }
        )

        lastScrollIndex = currentIndex
    }

    // Monitor scroll state for bottom bar animations (separate from index changes)
    LaunchedEffect(gridState.isScrollInProgress) {
        val isScrolling = gridState.isScrollInProgress
        Log.d(TAG, "Search: Scroll state changed - isScrollInProgress=$isScrolling")

        // Bottom bar logic: hide while scrolling, show when stopped
        if (isScrolling) {
            Log.d(TAG, "Search: Scrolling started - hiding bottom bar")
            onBottomBarVisibilityChange(false)
        } else {
            Log.d(TAG, "Search: Scrolling stopped - showing bottom bar")
            onBottomBarVisibilityChange(true)
        }
    }

    // Observe grid view mode changes to update the UI immediately
    val isGridView by mainViewModel.isGridViewMode.collectAsStateWithLifecycle(initialValue = true)

    // React to grid view mode changes
    LaunchedEffect(isGridView) {
        val mediaItems = originalGroupedMedia.value.filter { it.type != MediaType.Section }
        if (mediaItems.isNotEmpty()) {
            groupedMedia.value = groupGalleryBy(mediaItems, MediaItemSortMode.DateTaken, isGridView)
        }
    }

    BackHandler(
        enabled = currentView.value == DefaultTabs.TabTypes.search && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        searchViewModel.cancelMediaFlow()
        // Since Search is now the default tab, we don't need to change the tab when pressing back
        // But we'll keep the handler to cancel the media flow
    }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val coroutineScope = rememberCoroutineScope()
        val scrollBackToTop = {
            coroutineScope.launch {
                gridState.animateScrollToItem(0)
            }
        }

        val searchedForText = rememberSaveable { mutableStateOf("") }
        var searchNow by rememberSaveable { mutableStateOf(false) }

        // Search type state: "metadata", "ocr", "combined" - Default to OCR for text search
        var searchType by rememberSaveable { mutableStateOf("ocr") }

        // OCR progress tracking
        val context = LocalContext.current
        val database = remember {
            Room.databaseBuilder(
                context,
                MediaDatabase::class.java,
                "media-database"
            ).apply {
                addMigrations(
                    Migration3to4(context),
                    Migration4to5(context),
                    Migration5to6(context),
                    Migration6to7(context)
                )
            }.build()
        }
        val ocrManager = remember { OcrManager(context, database) }
        val ocrProgress by ocrManager.getProgressFlow().collectAsStateWithLifecycle(initialValue = null)

        // Ensure progress monitoring is active when page loads
        LaunchedEffect(Unit) {
            Log.d("SearchPage", "Initializing OCR progress monitoring")
            ocrManager.ensureProgressMonitoring()
        }

        // Additional monitoring to handle first launch scenario
        LaunchedEffect(ocrProgress) {
            if (ocrProgress?.isProcessing == true) {
                Log.d("SearchPage", "OCR processing detected, ensuring monitoring is active")
                ocrManager.ensureProgressMonitoring()

                // Progress bar visibility is now controlled by database state
                // No need to manually reset dismissed state
            }
        }

        // Periodic check to ensure progress monitoring is active during first launch
        LaunchedEffect(Unit) {
            var checkCount = 0
            while (checkCount < 10) { // Check for up to 20 seconds
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
                checkCount++

                val currentProgress = ocrManager.getProgressFlow().first()
                if (currentProgress?.isProcessing == true) {
                    Log.d("SearchPage", "Periodic check: OCR processing active, ensuring monitoring")
                    ocrManager.forceStartProgressMonitoring()
                    break // Stop checking once we've ensured monitoring is active
                }
            }
        }

        var hideLoadingSpinner by remember { mutableStateOf(false) }
        val showLoadingSpinner by remember {
            derivedStateOf {
                if (groupedMedia.value.isEmpty()) true else !hideLoadingSpinner
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated search bar container - smooth fade animations
            AnimatedVisibility(
                visible = scrollVisibilityState.isAppBarVisible,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Search bar container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(56.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
            val placeholdersList = remember {
                val month = months.random().replaceFirstChar {
                    it.uppercase()
                }
                val day = days.random().replaceFirstChar {
                    it.uppercase()
                }
                val date = (1..31).random()
                val year = (2016..2024).random()

                listOf(
                    "Search for a photo's name",
                    "Search for a specific date",
                    "$month $date $year",
                    "$month $year",
                    "Search by day",
                    "$day $month $year",
                    "$date $month $year"
                )
            }
            val placeholder = remember { placeholdersList.random() }

                SearchBar(
                    query = searchedForText,
                    placeholder = placeholder,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(horizontal = 6.dp),
                    onSearch = {
                        if (!showLoadingSpinner) {
                            searchNow = true
                            scrollBackToTop()
                        }
                    },
                    onClear = {
                        searchedForText.value = ""
                        searchNow = true
                        scrollBackToTop()
                    },
                    onFilterClick = {
                        showFilterDropdown = !showFilterDropdown
                    }
                )
            }

                    // Compact filter chips - show when filter button is clicked
                    AnimatedVisibility(
                visible = showFilterDropdown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        onClick = {
                            searchType = "metadata"
                            showFilterDropdown = false // Auto-hide after selection
                            if (searchedForText.value.isNotEmpty()) {
                                searchNow = true
                            }
                        },
                        label = {
                            Text(
                                "Filename & Date",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        selected = searchType == "metadata",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Metadata search",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    FilterChip(
                        onClick = {
                            searchType = "ocr"
                            showFilterDropdown = false // Auto-hide after selection
                            if (searchedForText.value.isNotEmpty()) {
                                searchNow = true
                            }
                        },
                        label = {
                            Text(
                                "Text in Images",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        selected = searchType == "ocr",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "OCR search",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    FilterChip(
                        onClick = {
                            searchType = "combined"
                            showFilterDropdown = false // Auto-hide after selection
                            if (searchedForText.value.isNotEmpty()) {
                                searchNow = true
                            }
                        },
                        label = {
                            Text(
                                "Both",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        selected = searchType == "combined",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Combined search",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

                    // Add spacing between filter chips and progress bar
                    Spacer(modifier = Modifier.height(4.dp))

                    // OCR Progress Bar - show by default when OCR processing is active
            val currentProgress = ocrProgress
            OcrProgressBar(
                progress = currentProgress,
                isVisible = currentProgress != null &&
                           !currentProgress.isComplete &&
                           !currentProgress.progressDismissed && // Respect database dismissed state
                           (currentProgress.isProcessing || currentProgress.isPaused ||
                            (currentProgress.processedImages < currentProgress.totalImages && currentProgress.totalImages > 0)),
                onDismiss = {
                    // Remove local state management, rely on database state
                    coroutineScope.launch {
                        ocrManager.dismissProgressBar()
                    }
                },
                onPauseResume = {
                    coroutineScope.launch {
                        val progressState = ocrProgress
                        if (progressState?.isPaused == true) {
                            ocrManager.resumeProcessing()
                        } else {
                            ocrManager.pauseProcessing()
                        }
                    }
                }
            )

                    // Note: Removed automatic reset of dismissed state to prevent instant reappearance
                    // Progress bar will only reappear when user manually enables OCR or app restarts
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LaunchedEffect(hideLoadingSpinner) {
            if (!hideLoadingSpinner) {
                delay(10000)
                hideLoadingSpinner = true
            }
        }

        LaunchedEffect(searchedForText.value, originalGroupedMedia.value, searchType) {
            println("SEARCH PARAMETERS CHANGED - Query: '${searchedForText.value}', Type: $searchType")
            if (searchedForText.value == "") {
                // Get the current grid view mode from MainViewModel
                val isGridView = mainViewModel.isGridViewMode.value
                // Filter out section items and regroup with current grid view mode
                val mediaItems = originalGroupedMedia.value.filter { it.type != MediaType.Section }
                groupedMedia.value = if (mediaItems.isNotEmpty()) {
                    groupGalleryBy(mediaItems, MediaItemSortMode.DateTaken, isGridView)
                } else {
                    originalGroupedMedia.value
                }
                hideLoadingSpinner = true
                return@LaunchedEffect
            }

            hideLoadingSpinner = false

            coroutineScope.launch {
                when (searchType) {
                    "metadata" -> performMetadataSearch(searchedForText.value, originalGroupedMedia.value, groupedMedia) { hideLoadingSpinner = it }
                    "ocr" -> performOcrSearch(searchedForText.value, originalGroupedMedia.value, groupedMedia, searchViewModel) { hideLoadingSpinner = it }
                    "combined" -> performCombinedSearch(searchedForText.value, originalGroupedMedia.value, groupedMedia, searchViewModel) { hideLoadingSpinner = it }
                }
            }
        }



        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = if (searchedForText.value == "") ViewProperties.SearchLoading else ViewProperties.SearchNotFound,
                state = gridState,
                modifier = Modifier.align(Alignment.Center)
            )

            if (showLoadingSpinner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(48.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(1000.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private val months = listOf(
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december"
)

private val days = listOf(
    "monday",
    "tuesday",
    "wednesday",
    "thursday",
    "friday",
    "saturday",
    "sunday"
)

private fun String.toDateListOrNull(): List<Date?> {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    dateFormat.isLenient = true

    val year = run {
        val split = this.split(" ")
        if (split.size == 3) {
            if (split[2].contains(Regex("[0-9]{4}"))) split[2].toIntOrNull()
            else null
        } else null
    }

    val month = months.firstOrNull {
        this.lowercase().split(" ").getOrElse(1) { "definitely is not a month" } in it
    }?.let {
        months.indexOf(it) + 1
    }

    if (year != null && month != null) {
        days.firstOrNull {
            this.lowercase().split(" ").getOrElse(0) { "definitely is not a day" } in it
        }?.let { weekDay ->
            var localDate = kotlinx.datetime.LocalDate(year, month, 1)

            val list = emptyList<Date?>().toMutableList()
            while (localDate.dayOfWeek != kotlinx.datetime.DayOfWeek.of(days.indexOf(weekDay) + 1) && localDate.month == kotlinx.datetime.Month.of(
                    month
                ) && localDate.year == year
            ) {
                localDate = localDate.plus(DatePeriod.parse("P0Y1D"))
            }
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 7}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 14}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 21}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )

            return list
        }
    }

    val formats = listOf(
        "dd/MM/yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd MM yyyy",
        "dd MMM yyyy",
        "dd MMMM yyyy",
        "MM/dd/yyyy",
        "MM-dd-yyyy",
        "MM dd yyyy",
        "MMM dd yyyy",
        "MMMM dd yyyy"
    )

    for (format in formats) {
        val dateFormatter = SimpleDateFormat(format, Locale.getDefault())
        try {
            return listOf(dateFormatter.parse(this))
        } catch (_: Throwable) {
        }
    }
    return listOf(null)
}

private fun Date.toDayLong(): Long {
    val millis = this.time
    val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return calendar.timeInMillis / 1000
}

/**
 * Perform metadata-based search (filename and date)
 */
private suspend fun performMetadataSearch(
    searchQuery: String,
    originalMedia: List<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>,
    setLoadingSpinner: (Boolean) -> Unit
) {
    val possibleDate = searchQuery.trim().toDateListOrNull()

    if (possibleDate.component1() != null) {
        val local = originalMedia.filter {
            it.type != MediaType.Section &&
                    (possibleDate.getOrNull(0)?.toDayLong()
                        ?.let { date -> it.getDateTakenDay() == date } == true ||
                            possibleDate.getOrNull(1)?.toDayLong()
                                ?.let { date -> it.getDateTakenDay() == date } == true ||
                            possibleDate.getOrNull(2)?.toDayLong()
                                ?.let { date -> it.getDateTakenDay() == date } == true ||
                            possibleDate.getOrNull(3)?.toDayLong()
                                ?.let { date -> it.getDateTakenDay() == date } == true)
        }

        // Get the current grid view mode from MainViewModel
        val isGridView = mainViewModel.isGridViewMode.value
        groupedMedia.value = groupGalleryBy(local, MediaItemSortMode.DateTaken, isGridView)
        setLoadingSpinner(true)
        return
    }

    val onlyMonthYearSplit = searchQuery.trim().split(" ")
    if (onlyMonthYearSplit.size == 2) {
        val month = months.firstOrNull { onlyMonthYearSplit[0] in it }
        val year = onlyMonthYearSplit[1]

        if (year.contains(Regex("[0-9]{4}")) && month != null && year.toIntOrNull() != null) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year.toIntOrNull()!!)
                set(Calendar.MONTH, months.indexOf(month))
                set(Calendar.DAY_OF_MONTH, 0)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val local = originalMedia.filter {
                it.type != MediaType.Section &&
                        it.getDateTakenMonth() == calendar.timeInMillis / 1000
            }

            // Get the current grid view mode from MainViewModel
            val isGridView = mainViewModel.isGridViewMode.value
            groupedMedia.value = groupGalleryBy(local, MediaItemSortMode.DateTaken, isGridView)
            setLoadingSpinner(true)
            return
        }
    }

    val groupedMediaLocal = originalMedia.filter {
        val isMedia = it.type != MediaType.Section
        val matchesFilter = it.displayName.contains(searchQuery.trim(), true)
        isMedia && matchesFilter
    }

    // Get the current grid view mode from MainViewModel
    val isGridView = mainViewModel.isGridViewMode.value
    groupedMedia.value = groupGalleryBy(groupedMediaLocal, MediaItemSortMode.DateTaken, isGridView)
    setLoadingSpinner(true)
}

/**
 * Perform OCR-based search (text content in images)
 */
private suspend fun performOcrSearch(
    searchQuery: String,
    originalMedia: List<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>,
    searchViewModel: SearchViewModel,
    setLoadingSpinner: (Boolean) -> Unit
) {
    try {
        // Get OCR search results (media IDs that contain the search text)
        val ocrMediaIds = searchViewModel.searchByOcrText(searchQuery)

        // Filter original media to only include items found by OCR search
        val filteredMedia = originalMedia.filter { mediaItem ->
            mediaItem.type != MediaType.Section && ocrMediaIds.contains(mediaItem.id)
        }

        // Apply current grid view mode
        val isGridView = mainViewModel.isGridViewMode.value
        groupedMedia.value = groupGalleryBy(filteredMedia, MediaItemSortMode.DateTaken, isGridView)

    } catch (e: Exception) {
        // On error, show empty results
        groupedMedia.value = emptyList()
    } finally {
        setLoadingSpinner(true)
    }
}

/**
 * Perform combined search (both metadata and OCR)
 */
private suspend fun performCombinedSearch(
    searchQuery: String,
    originalMedia: List<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>,
    searchViewModel: SearchViewModel,
    setLoadingSpinner: (Boolean) -> Unit
) {
    try {
        // Get metadata results (filename search)
        val metadataResults = originalMedia.filter {
            val isMedia = it.type != MediaType.Section
            val matchesFilter = it.displayName.contains(searchQuery.trim(), true)
            isMedia && matchesFilter
        }

        // Get OCR results (text content search)
        val ocrMediaIds = searchViewModel.searchByOcrText(searchQuery)
        val ocrResults = originalMedia.filter { mediaItem ->
            mediaItem.type != MediaType.Section && ocrMediaIds.contains(mediaItem.id)
        }

        // Combine results and remove duplicates
        val combinedResults = (metadataResults + ocrResults).distinctBy { it.id }

        // Apply current grid view mode
        val isGridView = mainViewModel.isGridViewMode.value
        groupedMedia.value = groupGalleryBy(combinedResults, MediaItemSortMode.DateTaken, isGridView)

    } catch (e: Exception) {
        // On error, show empty results
        groupedMedia.value = emptyList()
    } finally {
        setLoadingSpinner(true)
    }
}


