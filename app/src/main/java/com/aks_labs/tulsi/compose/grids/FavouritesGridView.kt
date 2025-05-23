package com.aks_labs.tulsi.compose.grids

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.compose.ViewProperties
import com.aks_labs.tulsi.compose.app_bars.FavouritesViewBottomAppBar
import com.aks_labs.tulsi.compose.app_bars.FavouritesViewTopAppBar
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.models.favourites_grid.FavouritesViewModel
import com.aks_labs.tulsi.models.favourites_grid.FavouritesViewModelFactory
import com.aks_labs.tulsi.models.multi_album.groupGalleryBy
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesGridView(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    val favouritesViewModel: FavouritesViewModel = viewModel(
        factory = FavouritesViewModelFactory()
    )

    val mediaStoreData =
        favouritesViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember {
        mutableStateOf(
            groupGalleryBy(
                mediaStoreData.value,
                MediaItemSortMode.LastModified
            )
        )
    }

    LaunchedEffect(mediaStoreData.value) {
        groupedMedia.value = groupGalleryBy(mediaStoreData.value, MediaItemSortMode.LastModified)
    }

    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            FavouritesViewTopAppBar(
                selectedItemsList = selectedItemsList,
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
                FavouritesViewBottomAppBar(
                    selectedItemsList = selectedItemsList,
                    groupedMedia = groupedMedia
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
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Favourites,
                shouldPadUp = true
            )
        }
    }
}



