package com.aks_labs.tulsi.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.CheckBoxButtonRow
import com.aks_labs.tulsi.compose.PreferencesRow
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.compose.PreferencesSwitchRow
import com.aks_labs.tulsi.compose.dialogs.DefaultTabSelectorDialog
import com.aks_labs.tulsi.compose.dialogs.SelectableButtonListDialog
import com.aks_labs.tulsi.compose.dialogs.SortModeSelectorDialog
import com.aks_labs.tulsi.compose.dialogs.TabCustomizationDialog
import com.aks_labs.tulsi.datastore.AlbumsList
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.datastore.DefaultTabs
import com.aks_labs.tulsi.datastore.Editing
import com.aks_labs.tulsi.datastore.MainGalleryView
import com.aks_labs.tulsi.datastore.PhotoGrid
import com.aks_labs.tulsi.datastore.Versions
import com.aks_labs.tulsi.datastore.Video
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.RowPosition
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsPage(currentTab: MutableState<BottomBarTab>) {
    Scaffold(
        topBar = {
            GeneralSettingsTopBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText("Video")
            }

            item {
                val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay()
                    .collectAsStateWithLifecycle(initialValue = true)
                val muteOnStart by mainViewModel.settings.Video.getMuteOnStart()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Auto Play Videos",
                    summary = "Start playing videos as soon as they appear on screen",
                    iconResID = R.drawable.auto_play,
                    checked = shouldAutoPlay,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setShouldAutoPlay(checked)
                    }
                )

                PreferencesSwitchRow(
                    title = "Videos Start Muted",
                    summary = "Don't play audio when first starting video playback",
                    iconResID = R.drawable.volume_mute,
                    checked = muteOnStart,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setMuteOnStart(checked)
                    }
                )
            }


            item {
                PreferencesSeparatorText("Editing")
            }

            item {
                val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Overwrite on save",
                    summary = "Default to overwriting instead of saving a copy when editing media",
                    iconResID = R.drawable.storage,
                    checked = overwriteByDefault,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setOverwriteByDefault(checked)
                    }
                )
            }

            item {
                val exitOnSave by mainViewModel.settings.Editing.getExitOnSave()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Exit on save",
                    summary = "Automatically exit the editing view when you save the changes",
                    iconResID = R.drawable.exit,
                    checked = exitOnSave,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setExitOnSave(checked)
                    }
                )
            }

            item {
                PreferencesSeparatorText("Albums")
            }

            item {
                val mainGalleryAlbums by mainViewModel.settings.MainGalleryView.getAlbums()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val allAlbums by mainViewModel.settings.AlbumsList.getAlbumsList()
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                val showAlbumsSelectionDialog = remember { mutableStateOf(false) }
                val selectedAlbums = remember { mutableStateListOf<String>() }

                PreferencesRow(
                    title = "Main Albums List",
                    iconResID = R.drawable.photogrid,
                    position = RowPosition.Single,
                    showBackground = false,
                    summary = "Select albums that will have their Gallery displayed in the main photo view"
                ) {
                    selectedAlbums.clear()
                    selectedAlbums.addAll(
                        mainGalleryAlbums.map {
                            it.apply {
                                removeSuffix("/")
                            }
                        }
                    )

                    showAlbumsSelectionDialog.value = true
                }

                if (showAlbumsSelectionDialog.value) {
                    SelectableButtonListDialog(
                        title = "Selected Albums",
                        body = "Albums selected here will show up in the main photo view",
                        showDialog = showAlbumsSelectionDialog,
                        onConfirm = {
                            mainViewModel.settings.MainGalleryView.clear()

                            selectedAlbums.forEach { album ->
                                mainViewModel.settings.MainGalleryView.addAlbum(album.removeSuffix("/"))
                            }
                        },
                        buttons = {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .height(384.dp)
                            ) {
                                items(
                                    count = allAlbums.size
                                ) { index ->
                                    val associatedAlbum = allAlbums[index]

                                    CheckBoxButtonRow(
                                        text = associatedAlbum.name,
                                        checked = selectedAlbums.contains(associatedAlbum.mainPath)
                                    ) {
                                        if (selectedAlbums.contains(associatedAlbum.mainPath) && selectedAlbums.size > 1) {
                                            selectedAlbums.remove(associatedAlbum.mainPath)
                                        } else {
                                            selectedAlbums.add(associatedAlbum.mainPath)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect()
                    .collectAsStateWithLifecycle(initialValue = false)
                val isAlreadyLoading = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                PreferencesSwitchRow(
                    title = "Automatically detect albums",
                    summary = "Detects all the folders with media on the device and adds them to the album list",
                    iconResID = R.drawable.albums_search,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = autoDetectAlbums
                ) { checked ->
                    // as to keep the MutableState alive even if the user leaves the screen
                    mainViewModel.launch {
                        if (!isAlreadyLoading.value) {
                            isAlreadyLoading.value = true

                            if (checked) {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.LoadingEvent(
                                        message = if (isAlreadyLoading.value) "Finding albums..." else "Found all albums!",
                                        iconResId = R.drawable.albums_search,
                                        isLoading = isAlreadyLoading
                                    )
                                )
                                val albums =
                                    mainViewModel.settings.AlbumsList.getAllAlbumsOnDevice()
                                albums.collectLatest {
                                    mainViewModel.settings.AlbumsList.addToAlbumsList(it)
                                }
                            }
                            mainViewModel.settings.AlbumsList.setAutoDetect(checked)

                            isAlreadyLoading.value = false
                        }
                    }
                }

                PreferencesRow(
                    title = "Clear album list",
                    summary = "Remove all albums except for Camera and Download",
                    iconResID = R.drawable.albums_clear,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        mainViewModel.settings.AlbumsList.setAlbumsList(
                            mainViewModel.settings.AlbumsList.defaultAlbumsList
                        )
                        mainViewModel.settings.AlbumsList.setAutoDetect(false)

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = "Cleared album list",
                                duration = SnackbarDuration.Short,
                                iconResId = R.drawable.albums
                            )
                        )
                    }
                }
            }

            item {
                val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode()
                    .collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)
                var showSortModeSelectorDialog by remember { mutableStateOf(false) }

                if (showSortModeSelectorDialog) {
                    SortModeSelectorDialog(
                        currentSortMode = currentSortMode,
                        dismiss = {
                            showSortModeSelectorDialog = false
                        }
                    )
                }

                PreferencesSwitchRow(
                    title = "Media Sorting",
                    summary = "Sets the sorting of Gallery and videos in grids",
                    iconResID = R.drawable.sorting,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = currentSortMode != MediaItemSortMode.Disabled,
                    onRowClick = {
                        showSortModeSelectorDialog = true
                    },
                    onSwitchClick = { checked ->
                        if (checked) mainViewModel.settings.PhotoGrid.setSortMode(MediaItemSortMode.DateTaken)
                        else mainViewModel.settings.PhotoGrid.setSortMode(MediaItemSortMode.Disabled)
                    }
                )
            }

            item {
                PreferencesSeparatorText(
                    text = "Tabs"
                )
            }

            item {
                var showDefaultTabAlbumDialog by remember { mutableStateOf(false) }
                val tabList by mainViewModel.settings.DefaultTabs.getTabList()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab()
                    .collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.search)

                if (showDefaultTabAlbumDialog) {
                    DefaultTabSelectorDialog(
                        tabList = tabList,
                        defaultTab = defaultTab
                    ) {
                        showDefaultTabAlbumDialog = false
                    }
                }

                PreferencesRow(
                    title = "Default Tab",
                    summary = "Tulsi Gallery will auto-open this tab at startup",
                    iconResID = R.drawable.folder_open,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showDefaultTabAlbumDialog = true
                }
            }

            item {
                var showDialog by remember { mutableStateOf(false) }

                if (showDialog) {
                    TabCustomizationDialog(
                        currentTab = currentTab,
                        closeDialog = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = "Customize Tabs",
                    summary = "Change what tabs are available in the bottom bar",
                    iconResID = R.drawable.edit,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showDialog = true
                }
            }



        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = "General",
                fontSize = TextUnit(22f, TextUnitType.Sp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}








