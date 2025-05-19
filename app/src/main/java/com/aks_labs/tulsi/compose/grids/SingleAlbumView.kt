package com.aks_labs.tulsi.compose.grids

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.compose.ViewProperties
import com.aks_labs.tulsi.compose.app_bars.SingleAlbumViewBottomBar
import com.aks_labs.tulsi.compose.app_bars.SingleAlbumViewTopBar
import com.aks_labs.tulsi.compose.dialogs.SingleAlbumDialog
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.models.custom_album.CustomAlbumViewModel
import com.aks_labs.tulsi.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    viewModel: MultiAlbumViewModel
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    LaunchedEffect(mediaStoreData) {
        groupedMedia.value = mediaStoreData
    }

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        currentView = currentView,
        navController = navController
    ) {
        if (viewModel.albumInfo.paths.toSet() != albumInfo.paths.toSet()) {
            viewModel.reinitDataSource(
                context = context,
                album = albumInfo
            )
        }
    }
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    viewModel: CustomAlbumViewModel
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    LaunchedEffect(mediaStoreData) {
        groupedMedia.value = mediaStoreData
    }

    println("SINGLE_ALBUM_VIEW ${groupedMedia.value}")

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        currentView = currentView,
        navController = navController
    ) {
        if (viewModel.albumInfo != albumInfo) {
            viewModel.reinitDataSource(
                context = context,
                album = albumInfo
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    groupedMedia: MutableState<List<MediaStoreData>>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    navController: NavHostController,
    currentView: MutableState<BottomBarTab>,
    reinitDataSource: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    LaunchedEffect(albumInfo) {
        reinitDataSource()
    }

    Scaffold(
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                currentView = currentView
            ) {
                navController.popBackStack()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomSheet,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                SingleAlbumViewBottomBar(
                    albumInfo = albumInfo,
                    selectedItemsList = selectedItemsList
                )
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize(1f)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    start = padding.calculateLeftPadding(LocalLayoutDirection.current),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateRightPadding(LocalLayoutDirection.current),
                    bottom = 0.dp // Remove bottom padding to allow content to be visible behind the bottom bar
                )
                .fillMaxSize(1f)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Album,
                shouldPadUp = true
            )

            SingleAlbumDialog(
                showDialog = showDialog,
                album = albumInfo,
                navController = navController,
                selectedItemsList = selectedItemsList,
                itemCount = groupedMedia.value.size
            )
        }
    }
}




