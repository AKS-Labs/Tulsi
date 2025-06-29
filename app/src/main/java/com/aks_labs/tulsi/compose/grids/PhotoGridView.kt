package com.aks_labs.tulsi.compose.grids

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.FolderIsEmpty
import com.aks_labs.tulsi.compose.ShowSelectedState
import com.aks_labs.tulsi.compose.ViewProperties
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.PhotoGrid
import com.aks_labs.tulsi.datastore.Storage

import com.aks_labs.tulsi.helpers.EncryptionManager
import com.aks_labs.tulsi.helpers.ImageFunctions
import com.aks_labs.tulsi.helpers.Screens
import com.aks_labs.tulsi.helpers.appSecureFolderDir
import com.aks_labs.tulsi.helpers.baseInternalStorageDirectory
import com.aks_labs.tulsi.helpers.checkHasFiles
import com.aks_labs.tulsi.helpers.getSecuredCacheImageForFile
import com.aks_labs.tulsi.helpers.rememberVibratorManager
import com.aks_labs.tulsi.helpers.selectAll
import com.aks_labs.tulsi.helpers.selectItem
import com.aks_labs.tulsi.helpers.selectSection
import com.aks_labs.tulsi.helpers.unselectAll
import com.aks_labs.tulsi.helpers.unselectItem
import com.aks_labs.tulsi.helpers.unselectSection
import com.aks_labs.tulsi.helpers.vibrateLong
import com.aks_labs.tulsi.helpers.vibrateShort
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.mediastore.getThumbnailIv
import com.aks_labs.tulsi.mediastore.signature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.math.roundToInt

private const val TAG = "PHOTO_GRID_VIEW"

@Composable
fun PhotoGrid(
    groupedMedia: MutableState<List<MediaStoreData>>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties,
    shouldPadUp: Boolean = false,
    state: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    var hasFiles: Boolean? by remember { mutableStateOf(null) }

    LaunchedEffect(groupedMedia.value) {
        withContext(Dispatchers.IO) {
            hasFiles = when {
                viewProperties == ViewProperties.SecureFolder -> {
                    val basePath = context.appSecureFolderDir

                    File(basePath).listFiles()?.isNotEmpty() == true
                }

                viewProperties == ViewProperties.Album && !albumInfo.isCustomAlbum -> {
                    var result: Boolean? = null

                    albumInfo.paths.any { path ->
                        result = Path("$baseInternalStorageDirectory$path").checkHasFiles()
                        result == true
                    }

                    result
                }

                else -> {
                    groupedMedia.value.isNotEmpty()
                }
            }
        }
    }

    when (hasFiles) {
        null -> {
            Row(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .then(modifier)
            ) {
                // TODO: show loading spinner for 5 seconds
                // if no change after 5 seconds hide and show album doesn't exist
            }
        }

        true -> {
            Row(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .then(modifier)
            ) {
                DeviceMedia(
                    groupedMedia = groupedMedia,
                    selectedItemsList = selectedItemsList,
                    viewProperties = viewProperties,
                    shouldPadUp = shouldPadUp,
                    gridState = state,
                    albumInfo = albumInfo
                )
            }
        }

        false -> {
            FolderIsEmpty(viewProperties.emptyText, viewProperties.emptyIconResId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMedia(
    groupedMedia: MutableState<List<MediaStoreData>>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    viewProperties: ViewProperties,
    shouldPadUp: Boolean,
    gridState: LazyGridState,
    albumInfo: AlbumInfo
) {
    var showLoadingSpinner by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    BackHandler(
        enabled = selectedItemsList.isNotEmpty()
    ) {
        selectedItemsList.clear()
    }

    if (groupedMedia.value.isNotEmpty()) {
        showLoadingSpinner = false
    }

    LaunchedEffect(groupedMedia.value) {
        mainViewModel.setGroupedMedia(groupedMedia.value)
    }

    val spacerHeight by animateDpAsState(
        targetValue = if (selectedItemsList.isNotEmpty() && shouldPadUp) 80.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = if (selectedItemsList.isNotEmpty() && shouldPadUp) 350 else 0
        ),
        label = "animate spacer on bottom of photo grid"
    )

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(this.maxHeight - spacerHeight)
                .align(Alignment.TopCenter)
        ) {
            val cacheThumbnails by mainViewModel.settings.Storage.getCacheThumbnails()
                .collectAsStateWithLifecycle(initialValue = false)
            val thumbnailSize by mainViewModel.settings.Storage.getThumbnailSize()
                .collectAsStateWithLifecycle(initialValue = 0)

            val scrollSpeed = remember { mutableFloatStateOf(0f) }
            val isDragSelecting = remember { mutableStateOf(false) }
            val localDensity = LocalDensity.current

            LaunchedEffect(scrollSpeed.floatValue) {
                if (scrollSpeed.floatValue != 0f) {
                    while (isActive) {
                        gridState.scrollBy(scrollSpeed.floatValue)
                        delay(10)
                    }
                }
            }

            // Get column count from preferences
            val portraitColumns by mainViewModel.settings.PhotoGrid.getGridColumnCountPortrait()
                .collectAsStateWithLifecycle(initialValue = 4)
            val landscapeColumns by mainViewModel.settings.PhotoGrid.getGridColumnCountLandscape()
                .collectAsStateWithLifecycle(initialValue = 6)

            // Get grid item styling preferences
            val gridItemCornerRadius by mainViewModel.settings.PhotoGrid.getGridItemCornerRadius()
                .collectAsStateWithLifecycle(initialValue = 16)
            val gridItemPadding by mainViewModel.settings.PhotoGrid.getGridItemPadding()
                .collectAsStateWithLifecycle(initialValue = 3)

            // Get drag selection preference
            val dragSelectionEnabled by mainViewModel.settings.PhotoGrid.getDragSelectionEnabled()
                .collectAsStateWithLifecycle(initialValue = true)

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(
                    if (!isLandscape) {
                        portraitColumns
                    } else {
                        landscapeColumns
                    }
                ),
                userScrollEnabled = !isDragSelecting.value,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(horizontal = 6.dp)
                    .align(Alignment.TopCenter)
                    .then(
                        // Apply drag selection if enabled OR already in selection mode
                        if (dragSelectionEnabled || selectedItemsList.isNotEmpty()) {
                            Modifier.dragSelectionHandler(
                                state = gridState,
                                selectedItemsList = selectedItemsList,
                                groupedMedia = groupedMedia.value,
                                scrollSpeed = scrollSpeed,
                                scrollThreshold = with(localDensity) {
                                    40.dp.toPx()
                                },
                                isDragSelecting = isDragSelecting
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                items(
                    count = groupedMedia.value.size,
                    key = {
                        groupedMedia.value[it].uri.toString()
                    },
                    span = { index ->
                        if (index < groupedMedia.value.size) {
                            val item = groupedMedia.value[index]
                            if (item.type == MediaType.Section) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                GridItemSpan(1)
                            }
                        } else {
                            GridItemSpan(1)
                        }
                    }
                ) { i ->
                    if (groupedMedia.value.isEmpty()) return@items
                    val mediaStoreItem = groupedMedia.value[i]

                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .animateItem()
                    ) {
                        val navController = LocalNavController.current

                        MediaStoreItem(
                            item = mediaStoreItem,
                            groupedMedia = groupedMedia,
                            viewProperties = viewProperties,
                            selectedItemsList = selectedItemsList,
                            thumbnailSettings = Pair(cacheThumbnails, thumbnailSize),
                            isDragSelecting = isDragSelecting,
                            onClick = {
                            when (viewProperties.operation) {
                                ImageFunctions.LoadNormalImage -> {
                                    // mainViewModel.setGroupedMedia(groupedMedia.value)

                                    navController.navigate(
                                        Screens.SinglePhotoView(
                                            albumInfo = albumInfo,
                                            mediaItemId = mediaStoreItem.id,
                                            loadsFromMainViewModel = viewProperties == ViewProperties.SearchLoading || viewProperties == ViewProperties.SearchNotFound || viewProperties == ViewProperties.Favourites
                                        )
                                    )
                                }

                                ImageFunctions.LoadTrashedImage -> {
                                    // mainViewModel.setGroupedMedia(groupedMedia.value)
                                    navController.navigate(
                                        Screens.SingleTrashedPhotoView(
                                            mediaItemId = mediaStoreItem.id
                                        )
                                    )
                                }

                                ImageFunctions.LoadSecuredImage -> {
                                    mainViewModel.setGroupedMedia(groupedMedia.value)

                                    navController.navigate(
                                        Screens.SingleHiddenPhotoView(
                                            mediaItemId = mediaStoreItem.id
                                        )
                                    )
                                }
                            }
                        },
                        cornerRadius = gridItemCornerRadius,
                        itemPadding = gridItemPadding
                    )
                }
            }
            }

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

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(1f)
                    .width(48.dp)
            ) {
                var showHandle by remember { mutableStateOf(false) }
                var isScrollingByHandle by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }

                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is DragInteraction.Start -> {
                                isScrollingByHandle = true
                            }

                            is DragInteraction.Cancel -> {
                                isScrollingByHandle = false
                            }

                            is DragInteraction.Stop -> {
                                isScrollingByHandle = false
                            }

                            else -> {}
                        }
                    }
                }

                LaunchedEffect(key1 = gridState.isScrollInProgress, key2 = isScrollingByHandle) {
                    if (gridState.isScrollInProgress || isScrollingByHandle) {
                        showHandle = true
                    } else {
                        delay(
                            if (selectedItemsList.isNotEmpty()) 1000 else 3000
                        )
                        showHandle = false
                    }
                }

                val listSize by remember {
                    derivedStateOf {
                        groupedMedia.value.size - 1
                    }
                }
                val totalLeftOverItems by remember {
                    derivedStateOf {
                        (listSize - gridState.layoutInfo.visibleItemsInfo.size).toFloat()
                    }
                }

                AnimatedVisibility(
                    visible = showHandle && !showLoadingSpinner && totalLeftOverItems > 50f,
                    modifier = Modifier.fillMaxHeight(1f),
                    enter =
                        slideInHorizontally { width -> width },
                    exit =
                        slideOutHorizontally { width -> width }
                ) {
                    val visibleItemIndex =
                        remember { derivedStateOf { gridState.firstVisibleItemIndex } }
                    val percentScrolled by remember {
                        derivedStateOf {
                            visibleItemIndex.value / totalLeftOverItems
                        }
                    }
                    var chosenItemIndex by remember { mutableIntStateOf(0) }

                    Slider(
                        value = percentScrolled,
                        interactionSource = interactionSource,
                        onValueChange = {
                            coroutineScope.launch {
                                if (isScrollingByHandle) {
                                    chosenItemIndex =
                                        (it * (groupedMedia.value.size - 1)).roundToInt()
                                    gridState.scrollToItem(
                                        chosenItemIndex
                                    )
                                }
                            }
                        },
                        valueRange = 0f..1f,
                        thumb = { _ ->
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(96.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(0.dp, 0.dp, 1000.dp, 1000.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .align(Alignment.Center)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.code),
                                        contentDescription = "scrollbar handle",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )

                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-90f)
                                        .graphicsLayer {
                                            translationX = -220f
                                        }
                                ) {
                                    AnimatedVisibility(
                                        visible = isScrollingByHandle,
                                        enter =
                                            slideInHorizontally { width -> width / 4 } + fadeIn(),
                                        exit =
                                            slideOutHorizontally { width -> width / 4 } + fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .height(32.dp)
                                            .wrapContentWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .wrapContentWidth()
                                                .clip(RoundedCornerShape(1000.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(8.dp, 4.dp)
                                        ) {
                                            // last index to "reach" even the last items
                                            val item by remember {
                                                derivedStateOf {
                                                    groupedMedia.value[chosenItemIndex]
                                                }
                                            }

                                            val format =
                                                remember { DateTimeFormatter.ofPattern("MMM yyyy") }
                                            val formatted = remember(item) {
                                                Instant.ofEpochSecond(item.dateTaken)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDateTime().format(format)
                                            }

                                            Text(
                                                text = formatted,
                                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        track = {
                            val colors = SliderDefaults.colors()
                            SliderDefaults.Track(
                                sliderState = it,
                                trackInsideCornerSize = 8.dp,
                                colors = colors.copy(
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                    disabledActiveTickColor = Color.Transparent,
                                    disabledInactiveTickColor = Color.Transparent,

                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                thumbTrackGapSize = 4.dp,
                                drawTick = { _, _ -> },
                                modifier = Modifier
                                    .height(16.dp)
                            )
                        },
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight(1f)
                            .graphicsLayer {
                                rotationZ = 90f
                                translationX = 30f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints(
                                        minWidth = constraints.minHeight,
                                        minHeight = constraints.minWidth,
                                        maxWidth = constraints.maxHeight,
                                        maxHeight = constraints.maxWidth
                                    )
                                )

                                layout(placeable.height, placeable.width) {
                                    placeable.place(0, -placeable.height)
                                }
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaStoreItem(
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    viewProperties: ViewProperties,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    thumbnailSettings: Pair<Boolean, Int>,
    isDragSelecting: MutableState<Boolean>,
    onClick: () -> Unit,
    cornerRadius: Int = 16,
    itemPadding: Int = 3
) {
    val vibratorManager = rememberVibratorManager()

    if (item.type == MediaType.Section) {
        val isSectionSelected by remember {
            derivedStateOf {
                selectedItemsList.contains(item)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (isSectionSelected) {
                        selectedItemsList.unselectSection(
                            section = item.section,
                            groupedMedia = groupedMedia.value
                        )
                    } else {
                        if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData.dummyItem) selectedItemsList.clear()

                        selectedItemsList.selectSection(
                            section = item.section,
                            groupedMedia = groupedMedia.value
                        )
                    }

                    vibratorManager.vibrateLong()
                }
                .padding(16.dp, 8.dp),
        ) {
            Text(
                text = "${viewProperties.prefix}${item.displayName}",
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )

            ShowSelectedState(
                isSelected = isSectionSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    } else {
        val isSelected by remember {
            derivedStateOf {
                selectedItemsList.contains(item)
            }
        }


        val animatedItemScale by animateFloatAsState(
            targetValue = if (isSelected) 0.8f else 1f,
            animationSpec = tween(
                durationMillis = 150
            ),
            label = "animate scale of selected item"
        )

        val onSingleClick: () -> Unit = {
            vibratorManager.vibrateShort()
            if (selectedItemsList.isNotEmpty()) {
                if (isSelected) {
                    selectedItemsList.unselectItem(item, groupedMedia.value)
                } else {
                    if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData.dummyItem) selectedItemsList.clear()

                    selectedItemsList.selectItem(item, groupedMedia.value)
                }
            } else {
                onClick()
            }
        }

        val onLongClick: () -> Unit = {
            isDragSelecting.value = true

            vibratorManager.vibrateLong()
            if (isSelected) {
                selectedItemsList.unselectItem(item, groupedMedia.value)
            } else {
                if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()

                selectedItemsList.selectItem(item, groupedMedia.value)
            }
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(itemPadding.dp) // Customizable padding between grid items
                .clip(RoundedCornerShape(cornerRadius.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(cornerRadius.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (selectedItemsList.isNotEmpty()) {
                        Modifier.clickable {
                            onSingleClick()
                        }
                    } else {
                        Modifier.combinedClickable(
                            onClick = onSingleClick,

                            onLongClick = onLongClick
                        )
                    }
                )
        ) {
            val context = LocalContext.current

            var model by remember { mutableStateOf<Any?>(null) }
            val isSecureMedia =
                remember(viewProperties) { viewProperties == ViewProperties.SecureFolder }

            LaunchedEffect(isSecureMedia) {
                if (!isSecureMedia || model != null) return@LaunchedEffect

                model =
                    withContext(Dispatchers.IO) {
                        try {
                            val thumbnailIv =
                                item.bytes!!.getThumbnailIv() // get thumbnail iv from video

                            EncryptionManager.decryptBytes(
                                bytes = getSecuredCacheImageForFile(
                                    fileName = item.displayName,
                                    context = context
                                ).readBytes(),
                                iv = thumbnailIv
                            )
                        } catch (e: Throwable) {
                            android.util.Log.d(TAG, e.toString())
                            e.printStackTrace()

                            item.uri.path
                        }
                    }
            }

            GlideImage(
                model = if (isSecureMedia) model else item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .fillMaxSize(1f) // Full size for clean, borderless appearance
                    .align(Alignment.Center)
                    .scale(animatedItemScale)
                    .clip(RoundedCornerShape(cornerRadius.dp))
            ) {
                if (isSecureMedia) {
                    it.signature(item.signature())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                } else if (thumbnailSettings.second == 0) {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                } else {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                        .override(thumbnailSettings.second)
                }
            }

            if (item.type == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.movie_filled),
                        contentDescription = "file is video indicator",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            ShowSelectedState(
                isSelected = isSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Preview
@Composable
fun PhotoGridPreview() {
    val groupedMedia = remember { mutableStateOf(emptyList<MediaStoreData>()) }
    val albumInfo = remember { AlbumInfo(id = 0, name = "Sample Album", paths = emptyList()) }
    val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
    val viewProperties = remember { ViewProperties.Album }

    PhotoGrid(
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        viewProperties = viewProperties
    )
}

@Preview
@Composable
fun PhotoGridWithItemsPreview() {
    val sampleMedia = remember {
        mutableStateOf(
            listOf(
                MediaStoreData(id = 1, displayName = "Image 1"),
                MediaStoreData(id = 2, displayName = "Image 2"),
                MediaStoreData(type = MediaType.Section, displayName = "Section 1"),
                MediaStoreData(id = 3, displayName = "Image 3"),
                MediaStoreData(id = 4, displayName = "Video 1", type = MediaType.Video),
            )
        )
    }
    val albumInfo = remember { AlbumInfo(id = 0, name = "Sample Album", paths = emptyList()) }
    val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
    val viewProperties = remember { ViewProperties.Album }

    PhotoGrid(
        groupedMedia = sampleMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        viewProperties = viewProperties
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DeviceMediaPreview() {
    val sampleMedia = remember {
        mutableStateOf(
            listOf(
                MediaStoreData(id = 1, displayName = "Image 1"),
                MediaStoreData(id = 2, displayName = "Image 2"),
                MediaStoreData(type = MediaType.Section, displayName = "Section 1"),
                MediaStoreData(id = 3, displayName = "Image 3"),
                MediaStoreData(id = 4, displayName = "Video 1", type = MediaType.Video),
            )
        )
    }
    val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
    val viewProperties = remember { ViewProperties.Album }
    val albumInfo = remember { AlbumInfo(id = 0, name = "Sample Album", paths = emptyList()) }
    val gridState = rememberLazyGridState()

    DeviceMedia(
        groupedMedia = sampleMedia,
        selectedItemsList = selectedItemsList,
        viewProperties = viewProperties,
        shouldPadUp = false,
        gridState = gridState,
        albumInfo = albumInfo
    )
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun MediaStoreItemPreview() {
    val item = remember { MediaStoreData(id = 1, displayName = "Image 1") }
    val groupedMedia = remember { mutableStateOf(emptyList<MediaStoreData>()) }
    val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
    val viewProperties = remember { ViewProperties.Album }
    val isDragSelecting = remember { mutableStateOf(false) }

    MediaStoreItem(
        item = item,
        groupedMedia = groupedMedia,
        viewProperties = viewProperties,
        selectedItemsList = selectedItemsList,
        thumbnailSettings = Pair(false, 0),
        isDragSelecting = isDragSelecting,
        onClick = {}
    )
}

fun Modifier.dragSelectionHandler(
    state: LazyGridState,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>,
    scrollSpeed: MutableFloatState,
    scrollThreshold: Float,
    isDragSelecting: MutableState<Boolean>
) = pointerInput(Unit) {
    var initialKey: Int? = null
    var currentKey: Int? = null

    if (groupedMedia.isEmpty()) return@pointerInput

    val itemWidth = state.layoutInfo.visibleItemsInfo.firstOrNull {
        if (it.index in groupedMedia.indices) groupedMedia[it.index].type != MediaType.Section else false
    }?.size?.width

    val numberOfHorizontalItems = itemWidth?.let { state.layoutInfo.viewportSize.width / it } ?: 1

    android.util.Log.d(TAG, "grid displays $numberOfHorizontalItems horizontal items")

    detectDragGestures(
        onDragStart = { offset ->
            isDragSelecting.value = true

            if (selectedItemsList.size == 1 && selectedItemsList[0] != MediaStoreData()) {
                initialKey = groupedMedia.indexOf(selectedItemsList[0])
                currentKey = initialKey
            } else {
                state.getGridItemAtOffset(
                    offset,
                    groupedMedia.map { it.uri.toString() },
                    numberOfHorizontalItems
                )?.let { key ->
                    if (key < groupedMedia.size) {  // Add bounds check
                        val item = groupedMedia[key]

                        if (item.type != MediaType.Section) {
                            initialKey = key
                            currentKey = key
                            if (!selectedItemsList.contains(item)) selectedItemsList.selectItem(
                                item,
                                groupedMedia
                            )
                        }
                    }
                }
            }
        },

        onDragCancel = {
            initialKey = null
            scrollSpeed.floatValue = 0f
            isDragSelecting.value = false
        },

        onDragEnd = {
            initialKey = null
            scrollSpeed.floatValue = 0f
            isDragSelecting.value = false
        },

        onDrag = { change, _ ->
            if (initialKey != null) {
                val distanceFromBottom = state.layoutInfo.viewportSize.height - change.position.y
                val distanceFromTop = change.position.y // for clarity

                scrollSpeed.floatValue = when {
                    distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
                    distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
                    else -> 0f
                }

                state.getGridItemAtOffset(
                    change.position,
                    groupedMedia.map { it.uri.toString() },
                    numberOfHorizontalItems
                )?.let { key ->
                    // Add bounds check
                    if (key < groupedMedia.size && currentKey != key && initialKey != null && currentKey != null) {
                        // Ensure all indices are within bounds
                        val safeInitialKey = initialKey!!.coerceIn(0, groupedMedia.size - 1)
                        val safeCurrentKey = currentKey!!.coerceIn(0, groupedMedia.size - 1)
                        val safeKey = key.coerceIn(0, groupedMedia.size - 1)

                        selectedItemsList.apply {
                            val toBeRemoved =
                                if (safeInitialKey <= safeCurrentKey) groupedMedia.subList(
                                    safeInitialKey,
                                    safeCurrentKey + 1
                                )
                                else groupedMedia.subList(safeCurrentKey, safeInitialKey + 1)

                            unselectAll(
                                toBeRemoved.filter {
                                    it.type != MediaType.Section
                                },
                                groupedMedia
                            )

                            val toBeAdded =
                                if (safeInitialKey <= safeKey) groupedMedia.subList(safeInitialKey, safeKey + 1)
                                else groupedMedia.subList(safeKey, safeInitialKey + 1)

                            selectAll(
                                toBeAdded.filter {
                                    it.type != MediaType.Section
                                },
                                groupedMedia
                            )
                        }

                        currentKey = key
                    }
                }
            }
        }
    )
}

@Suppress("UNCHECKED_CAST")
        /** make sure [T] is the same type as state keys */
fun <T : Any> LazyGridState.getGridItemAtOffset(
    offset: Offset,
    keys: List<T>,
    numberOfHorizontalItems: Int
): Int? {
    if (keys.isEmpty()) return null

    var key: T? = null

    // scan the entire row for this item
    // if theres only one or two items on a row and user drag selects to the empty space they get selected
    for (i in 1..numberOfHorizontalItems) {
        val possibleItem = layoutInfo.visibleItemsInfo.find { item ->
            val stretched = item.size.toIntRect().let {
                IntRect(
                    top = it.top,
                    bottom = it.bottom,
                    left = it.left,
                    right = it.right * i
                )
            }

            stretched.contains(offset.round() - item.offset)
        }

        if (possibleItem != null) {
            try {
                key = possibleItem.key as? T
                if (key != null) break
            } catch (e: Exception) {
                // If casting fails, continue to the next item
                continue
            }
        }
    }

    if (key == null) return null

    val index = keys.indexOf(key)
    return if (index >= 0) index else null
}



