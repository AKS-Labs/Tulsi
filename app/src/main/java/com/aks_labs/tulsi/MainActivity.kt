/*
 * Copyright (C) 2023 kaii-lb (original author)
 * Copyright (C) 2025 AKS-Labs (modifications)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aks_labs.tulsi

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.aks_labs.tulsi.lavender_snackbars.LavenderSnackbarBox
import com.aks_labs.tulsi.lavender_snackbars.LavenderSnackbarController
import com.aks_labs.tulsi.lavender_snackbars.LavenderSnackbarEvents
import com.aks_labs.tulsi.lavender_snackbars.LavenderSnackbarHostState
import com.aks_labs.tulsi.compose.ErrorPage
import com.aks_labs.tulsi.compose.LockedFolderEntryView
import com.aks_labs.tulsi.compose.PermissionHandler
import com.aks_labs.tulsi.compose.ViewProperties
import com.aks_labs.tulsi.compose.app_bars.MainAppBottomBar
import com.aks_labs.tulsi.compose.app_bars.MainAppSelectingBottomBar
import com.aks_labs.tulsi.compose.app_bars.MainAppTopBar
import com.aks_labs.tulsi.compose.app_bars.getAppBarContentTransition
import com.aks_labs.tulsi.compose.app_bars.setBarVisibility
import com.aks_labs.tulsi.compose.dialogs.MainAppDialog
import com.aks_labs.tulsi.compose.grids.AlbumsGridView
import com.aks_labs.tulsi.compose.grids.FavouritesGridView
import com.aks_labs.tulsi.compose.grids.LockedFolderView
import com.aks_labs.tulsi.compose.grids.PhotoGrid
import com.aks_labs.tulsi.compose.grids.SearchPage
import com.aks_labs.tulsi.compose.grids.SingleAlbumView
import com.aks_labs.tulsi.compose.grids.TrashedPhotoGridView
import com.aks_labs.tulsi.compose.rememberDeviceOrientation
import com.aks_labs.tulsi.compose.settings.AboutPage
import com.aks_labs.tulsi.compose.settings.DataAndBackupPage
import com.aks_labs.tulsi.compose.settings.DebuggingSettingsPage
import com.aks_labs.tulsi.compose.settings.GeneralSettingsPage
import com.aks_labs.tulsi.compose.settings.LookAndFeelSettingsPage
import com.aks_labs.tulsi.compose.settings.MainSettingsPage
import com.aks_labs.tulsi.compose.settings.MemoryAndStorageSettingsPage
import com.aks_labs.tulsi.compose.settings.PrivacyAndSecurityPage
import com.aks_labs.tulsi.compose.settings.UpdatesPage
import com.aks_labs.tulsi.compose.single_photo.EditingView
import com.aks_labs.tulsi.compose.single_photo.SingleHiddenPhotoView
import com.aks_labs.tulsi.compose.single_photo.SinglePhotoView
import com.aks_labs.tulsi.compose.single_photo.SingleTrashedPhotoView
import com.aks_labs.tulsi.database.MediaDatabase
import com.aks_labs.tulsi.database.Migration3to4
import com.aks_labs.tulsi.database.Migration4to5
import com.aks_labs.tulsi.database.Migration5to6
import com.aks_labs.tulsi.database.Migration6to7
import com.aks_labs.tulsi.ocr.SimpleOcrService
import com.aks_labs.tulsi.ocr.OcrManager
import com.aks_labs.tulsi.ocr.MediaContentObserver
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers as CoroutineDispatchers
import kotlinx.coroutines.launch
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.AlbumInfoNavType
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.datastore.Debugging
import com.aks_labs.tulsi.datastore.DefaultTabs
import com.aks_labs.tulsi.datastore.Editing
import com.aks_labs.tulsi.datastore.LookAndFeel
import com.aks_labs.tulsi.datastore.MainGalleryView
import com.aks_labs.tulsi.datastore.PhotoGrid
import com.aks_labs.tulsi.datastore.Versions
import com.aks_labs.tulsi.helpers.BottomBarTabSaver
import com.aks_labs.tulsi.helpers.CheckUpdateState
import com.aks_labs.tulsi.helpers.LogManager
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.MultiScreenViewType
import com.aks_labs.tulsi.helpers.Screens
import com.aks_labs.tulsi.helpers.appStorageDir
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.models.multi_album.groupGalleryBy
import com.aks_labs.tulsi.models.custom_album.CustomAlbumViewModel
import com.aks_labs.tulsi.models.custom_album.CustomAlbumViewModelFactory
import com.aks_labs.tulsi.models.main_activity.MainViewModel
import com.aks_labs.tulsi.models.main_activity.MainViewModelFactory
import com.aks_labs.tulsi.models.multi_album.MultiAlbumViewModel
import com.aks_labs.tulsi.models.multi_album.MultiAlbumViewModelFactory
import com.aks_labs.tulsi.ui.theme.GalleryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

private const val TAG = "MAIN_ACTIVITY"

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

/**
 * Detects if the device is using gesture-based navigation or traditional button navigation
 */
fun isGestureNavigationEnabled(resources: Resources): Boolean {
    return try {
        val resourceId = resources.getIdentifier(
            "config_navBarInteractionMode",
            "integer",
            "android"
        )
        if (resourceId > 0) {
            // 0 = 3-button navigation, 1 = 2-button navigation, 2 = gesture navigation
            val navBarInteractionMode = resources.getInteger(resourceId)
            navBarInteractionMode == 2
        } else {
            // Fallback: assume gesture navigation for Android 10+ if we can't detect
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    } catch (e: Exception) {
        // Fallback: assume gesture navigation for Android 10+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var applicationDatabase: MediaDatabase
        lateinit var mainViewModel: MainViewModel
    }

    private lateinit var mediaContentObserver: MediaContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        // Configure window for better navigation bar handling
        configureSystemUI()

        val mediaDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(
                Migration3to4(applicationContext),
                Migration4to5(applicationContext),
                Migration5to6(applicationContext),
                Migration6to7(applicationContext)
            )
        }.build()
        applicationDatabase = mediaDatabase

        // Initialize OCR functionality
        initializeOcrSystem()

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        setContent {
            mainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext)
            )

            val continueToApp = remember {
                // Manifest.permission.MANAGE_MEDIA is optional
                mainViewModel.startupPermissionCheck(applicationContext)
                mutableStateOf(
                    mainViewModel.checkCanPass()
                )
            }

            val initial =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }
            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
                    initialValue = initial
                )

            GalleryTheme(
                darkTheme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                AnimatedContent(
                    targetState = continueToApp.value,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                            .using(
                                SizeTransform(clip = false)
                            )
                    },
                    label = "PermissionHandlerToMainViewAnimatedContent"
                ) { stateValue ->
                    if (!stateValue) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle =
                                if (!isSystemInDarkTheme()) {
                                    SystemBarStyle.light(
                                        MaterialTheme.colorScheme.background.toArgb(),
                                        MaterialTheme.colorScheme.background.toArgb()
                                    )
                                } else {
                                    SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
                                }
                        )

                        PermissionHandler(continueToApp)
                    } else {
                        SetContentForActivity()
                    }
                }
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun SetContentForActivity() {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        val navControllerLocal = rememberNavController()

        val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.search)
        val currentView = rememberSaveable(
            inputs = arrayOf(defaultTab),
            stateSaver = BottomBarTabSaver
        ) { mutableStateOf(defaultTab) }

        val context = LocalContext.current
        val showDialog = remember { mutableStateOf(false) }

        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val logPath = "${context.appStorageDir}/log.txt"
        Log.d(TAG, "Log save path is $logPath")

        val canRecordLogs by mainViewModel.settings.Debugging.getRecordLogs()
            .collectAsStateWithLifecycle(initialValue = false)

        LaunchedEffect(canRecordLogs) {
            if (canRecordLogs) {
                val logManager = LogManager(context = context)
                logManager.startRecording()
            }
        }

//        mainViewModel.settings.AlbumsList.addToAlbumsList("DCIM/Camera")

        val albumsList by mainViewModel.settings.MainGalleryView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode()
            .collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)
        val isGridView by mainViewModel.isGridViewMode
            .collectAsStateWithLifecycle(initialValue = true)

        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortBy = currentSortMode
            )
        )

        val customAlbumViewModel: CustomAlbumViewModel = viewModel(
            factory = CustomAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                sortBy = currentSortMode
            )
        )

        // update main Gallery view albums list
        LaunchedEffect(albumsList) {
            if (navControllerLocal.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                || multiAlbumViewModel.albumInfo.paths.toSet() == albumsList
            ) return@LaunchedEffect

            Log.d(TAG, "Refreshing main Gallery view")
            Log.d(TAG, "In view model: ${multiAlbumViewModel.albumInfo.paths} new: $albumsList")
            multiAlbumViewModel.reinitDataSource(
                context = context,
                album = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortMode = currentSortMode,
                gridView = isGridView
            )
        }

        // update grid view mode when it changes
        LaunchedEffect(isGridView) {
            if (navControllerLocal.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name) {
                return@LaunchedEffect
            }

            // First, update the grouped media immediately to reflect the new view mode
            val currentMedia = multiAlbumViewModel.mediaFlow.value
            val filteredMedia = currentMedia.filter { it.type != MediaType.Section }
            if (filteredMedia.isNotEmpty()) {
                val regroupedMedia = groupGalleryBy(filteredMedia, multiAlbumViewModel.sortBy, isGridView)
                multiAlbumViewModel.setGroupedMedia(regroupedMedia)
            }

            // Then update the view mode in the view model (which will reload data in the background)
            multiAlbumViewModel.setGridViewMode(context, isGridView)
        }

        LaunchedEffect(currentSortMode) {
            if (multiAlbumViewModel.sortBy == currentSortMode) return@LaunchedEffect

            Log.d(
                TAG,
                "Changing sort mode from: ${multiAlbumViewModel.sortBy} to: $currentSortMode"
            )
            multiAlbumViewModel.changeSortMode(context = context, sortMode = currentSortMode)
            customAlbumViewModel.changeSortMode(context = context, sortMode = currentSortMode)
        }

        val snackbarHostState = remember {
            LavenderSnackbarHostState()
        }

        CompositionLocalProvider(LocalNavController provides navControllerLocal) {
            LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
                NavHost(
                    navController = navControllerLocal,
                    startDestination = MultiScreenViewType.MainScreen.name,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .background(MaterialTheme.colorScheme.background),
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> width } + fadeIn()
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> -width } + fadeOut()
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> width } + fadeOut()
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> -width } + fadeIn()
                    }
                ) {
                    composable(MultiScreenViewType.MainScreen.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle =
                                if (!isSystemInDarkTheme()) {
                                    SystemBarStyle.light(
                                        MaterialTheme.colorScheme.background.toArgb(),
                                        MaterialTheme.colorScheme.background.toArgb()
                                    )
                                } else {
                                    SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
                                }
                        )
                        setupNextScreen(
                            selectedItemsList = selectedItemsList,
                            window = window
                        )

                        Content(currentView, showDialog, selectedItemsList, multiAlbumViewModel)
                    }

                    composable<Screens.SinglePhotoView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfoNavType,
                            typeOf<List<String>>() to NavType.StringListType
                        )
                    ) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(
                                MaterialTheme.colorScheme.surfaceContainer.copy(
                                    alpha = 0.2f
                                ).toArgb()
                            ),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.SinglePhotoView = it.toRoute()

                        if (!screen.hasSameAlbumsAs(other = multiAlbumViewModel.albumInfo.paths)) {
                            multiAlbumViewModel.reinitDataSource(
                                context = context,
                                album = screen.albumInfo,
                                sortMode = multiAlbumViewModel.sortBy
                            )
                        }

                        SinglePhotoView(
                            navController = navControllerLocal,
                            window = window,
                            viewModel = multiAlbumViewModel,
                            mediaItemId = screen.mediaItemId,
                            loadsFromMainViewModel = screen.loadsFromMainViewModel
                        )
                    }

                    composable<Screens.SingleAlbumView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfoNavType,
                            typeOf<List<String>>() to NavType.StringListType
                        )
                    ) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                MaterialTheme.colorScheme.surface.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.SingleAlbumView = it.toRoute()

                        if (!screen.albumInfo.isCustomAlbum) {
                            if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                                multiAlbumViewModel.reinitDataSource(
                                    context = context,
                                    album = screen.albumInfo,
                                    sortMode = multiAlbumViewModel.sortBy
                                )
                            }

                            SingleAlbumView(
                                albumInfo = screen.albumInfo,
                                selectedItemsList = selectedItemsList,
                                currentView = currentView,
                                viewModel = multiAlbumViewModel
                            )
                        } else {
                            if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                                customAlbumViewModel.reinitDataSource(
                                    context = context,
                                    album = screen.albumInfo,
                                    sortMode = customAlbumViewModel.sortBy
                                )
                            }

                            SingleAlbumView(
                                albumInfo = screen.albumInfo,
                                selectedItemsList = selectedItemsList,
                                currentView = currentView,
                                viewModel = customAlbumViewModel
                            )
                        }
                    }

                    composable<Screens.SingleTrashedPhotoView> {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                MaterialTheme.colorScheme.surface.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.SingleTrashedPhotoView = it.toRoute()

                        SingleTrashedPhotoView(
                            window = window,
                            mediaItemId = screen.mediaItemId
                        )
                    }

                    composable(MultiScreenViewType.TrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                MaterialTheme.colorScheme.surface.toArgb()
                            )
                        )

                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        TrashedPhotoGridView(
                            selectedItemsList = selectedItemsList,
                            currentView = currentView
                        )
                    }

                    composable(MultiScreenViewType.SecureFolder.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                MaterialTheme.colorScheme.surface.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        LockedFolderView(window = window, currentView = currentView)
                    }

                    composable<Screens.SingleHiddenPhotoView> {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                MaterialTheme.colorScheme.surface.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.SingleHiddenPhotoView = it.toRoute()

                        SingleHiddenPhotoView(
                            mediaItemId = screen.mediaItemId,
                            window = window
                        )
                    }

                    composable(MultiScreenViewType.AboutAndUpdateView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        AboutPage {
                            navControllerLocal.popBackStack()
                        }
                    }

                    composable(MultiScreenViewType.FavouritesGridView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        FavouritesGridView(
                            selectedItemsList = selectedItemsList,
                            currentView = currentView
                        )
                    }

                    composable<Screens.EditingScreen>(
                        enterTransition = {
                            slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            ) { height -> height } + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            )
                        },
                        exitTransition = {
                            slideOutVertically(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            ) { height -> height } + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            )
                        },
                        popEnterTransition = {
                            slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            ) { height -> height } + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            )
                        },
                        popExitTransition = {
                            slideOutVertically(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            ) { height -> height } + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 600
                                )
                            )
                        }
                    ) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.surfaceContainer.toArgb(),
                                MaterialTheme.colorScheme.surfaceContainer.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.EditingScreen = it.toRoute()
                        val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault()
                            .collectAsStateWithLifecycle(initialValue = false)

                        EditingView(
                            absolutePath = screen.absolutePath,
                            dateTaken = screen.dateTaken,
                            uri = screen.uri.toUri(),
                            window = window,
                            overwriteByDefault = overwriteByDefault
                        )
                    }

                    composable(MultiScreenViewType.SettingsMainView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        MainSettingsPage()
                    }

                    composable(MultiScreenViewType.SettingsDebuggingView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        DebuggingSettingsPage()
                    }

                    composable(MultiScreenViewType.SettingsGeneralView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        GeneralSettingsPage(currentTab = currentView)
                    }

                    composable(MultiScreenViewType.SettingsMemoryAndStorageView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        MemoryAndStorageSettingsPage()
                    }

                    composable(MultiScreenViewType.SettingsLookAndFeelView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        LookAndFeelSettingsPage()
                    }

                    composable(MultiScreenViewType.UpdatesPage.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        UpdatesPage()
                    }

                    composable(MultiScreenViewType.DataAndBackup.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        DataAndBackupPage()
                    }

                    composable(MultiScreenViewType.PrivacyAndSecurity.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(
                                MaterialTheme.colorScheme.background.toArgb(),
                                MaterialTheme.colorScheme.background.toArgb()
                            )
                        )
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        PrivacyAndSecurityPage()
                    }
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
    }

    @Composable
    private fun Content(
        currentView: MutableState<BottomBarTab>,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        multiAlbumViewModel: MultiAlbumViewModel,
    ) {
        val context = LocalContext.current
        val albumsList by mainViewModel.settings.MainGalleryView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
        var isTopBarVisible by remember { mutableStateOf(true) }

        val tabList by mainViewModel.settings.DefaultTabs.getTabList()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

        // Extract theme colors outside LaunchedEffect to avoid Composable context issues
        val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        val isDarkTheme = isSystemInDarkTheme()

        // Set normal status bar style based on theme
        LaunchedEffect(Unit) {
            enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(surfaceContainerColor),
                statusBarStyle = if (!isDarkTheme) {
                    SystemBarStyle.light(backgroundColor, backgroundColor)
                } else {
                    SystemBarStyle.dark(backgroundColor)
                }
            )
        }

        // faster loading if no custom tabs are present
        LaunchedEffect(tabList) {
            if (!tabList.any { it.isCustom } && currentView.value.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                multiAlbumViewModel.reinitDataSource(
                    context = context,
                    album = AlbumInfo(
                        id = currentView.value.id,
                        name = currentView.value.name,
                        paths = currentView.value.albumPaths,
                        isCustomAlbum = currentView.value.isCustom
                    ),
                    sortMode = multiAlbumViewModel.sortBy
                )

                groupedMedia.value = mediaStoreData.value
            }
        }

        Scaffold(
            topBar = {
                TopBar(
                    showDialog = showDialog,
                    selectedItemsList = selectedItemsList,
                    currentView = currentView,
                    isVisible = isTopBarVisible
                )
            },
            bottomBar = {
                BottomBar(
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabList
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize(1f)
        ) { padding ->
            val isLandscape by rememberDeviceOrientation()

            val safeDrawingPadding = if (isLandscape) {
                val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

                val layoutDirection = LocalLayoutDirection.current
                val left = safeDrawing.calculateStartPadding(layoutDirection)
                val right = safeDrawing.calculateEndPadding(layoutDirection)

                Pair(left, right)
            } else {
                Pair(0.dp, 0.dp)
            }

            Column(
                modifier = Modifier
                    .padding(
                        safeDrawingPadding.first,
                        padding.calculateTopPadding(),
                        safeDrawingPadding.second,
                        0.dp // Remove bottom padding to allow content to be visible behind the bottom bar
                    )
                    .fillMaxSize()
            ) {
                MainAppDialog(showDialog, currentView, selectedItemsList)

                AnimatedContent(
                    targetState = currentView.value,
                    transitionSpec = {
                        if (targetState.index > initialState.index) {
                            (slideInHorizontally { width -> width } + fadeIn(initialAlpha = 0f)).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(targetAlpha = 0f))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(initialAlpha = 0f)).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(targetAlpha = 0f))
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "MainAnimatedContentView",
                    modifier = Modifier.background(Color.Transparent)
                ) { stateValue ->
                    if (stateValue in tabList || stateValue == DefaultTabs.TabTypes.secure) {
                        Log.d(TAG, "Tab needed is $stateValue")
                        when {
                            stateValue.isCustom -> {
                                if (stateValue.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = stateValue.albumPaths,
                                            isCustomAlbum = true
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy
                                    )
                                }

                                LaunchedEffect(mediaStoreData.value) {
                                    groupedMedia.value = mediaStoreData.value
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = AlbumInfo(
                                        id = stateValue.id,
                                        name = stateValue.name,
                                        paths = stateValue.albumPaths,
                                        isCustomAlbum = true
                                    ),
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.Gallery -> {
                                if (albumsList.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = albumsList,
                                            isCustomAlbum = false
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy
                                    )
                                }

                                selectedItemsList.clear()

                                LaunchedEffect(mediaStoreData.value) {
                                    groupedMedia.value = mediaStoreData.value
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = multiAlbumViewModel.albumInfo,
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.secure -> LockedFolderEntryView(
                                currentView
                            )

                            stateValue == DefaultTabs.TabTypes.albums -> {
                                AlbumsGridView(currentView)
                            }

                            stateValue == DefaultTabs.TabTypes.search -> {
                                selectedItemsList.clear()

                                SearchPage(
                                    selectedItemsList = selectedItemsList,
                                    currentView = currentView,
                                    onTopBarVisibilityChange = { visible ->
                                        isTopBarVisible = visible
                                    }
                                )
                            }
                        }
                    } else {
                        ErrorPage(
                            message = "This tab doesn't exist!",
                            iconResId = R.drawable.error
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        currentView: MutableState<BottomBarTab>,
        isVisible: Boolean = true
    ) {
        val show by remember {
            derivedStateOf {
                selectedItemsList.isNotEmpty()
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            MainAppTopBar(
                alternate = show,
                showDialog = showDialog,
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        }
    }

    @Composable
    private fun BottomBar(
        currentView: MutableState<BottomBarTab>,
        tabs: List<BottomBarTab>,
        selectedItemsList: SnapshotStateList<MediaStoreData>
    ) {
        val navController = LocalNavController.current
        val show by remember {
            derivedStateOf {
                selectedItemsList.isNotEmpty()
            }
        }

        AnimatedContent(
            targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
            transitionSpec = {
                getAppBarContentTransition(show)
            },
            label = "MainBottomBarAnimatedContentView",
            modifier = Modifier.background(Color.Transparent)
        ) { state ->
            if (!state) {
                MainAppBottomBar(
                    currentView = currentView,
                    tabs = tabs,
                    selectedItemsList = selectedItemsList
                )
            } else {
                MainAppSelectingBottomBar(selectedItemsList)
            }
        }
    }

    /**
     * Configures system UI for better navigation bar handling
     */
    private fun configureSystemUI() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Configure window insets controller
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // For devices with traditional navigation buttons, make navigation bar translucent
        if (!isGestureNavigationEnabled(resources)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            // Make navigation bar translucent
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /**
     * Initialize OCR system with content observer and progress tracking
     */
    private fun initializeOcrSystem() {
        Log.d("MainActivity", "Initializing OCR system...")
        CoroutineScope(CoroutineDispatchers.IO).launch {
            try {
                val ocrManager = OcrManager(applicationContext, applicationDatabase)

                // Initialize progress tracking
                val totalImages = getTotalImageCount()
                Log.d("MainActivity", "Found $totalImages total images")
                ocrManager.initializeProgress(totalImages)

                // Set up content observer for new images
                mediaContentObserver = MediaContentObserver(applicationContext)
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mediaContentObserver
                )
                Log.d("MainActivity", "Content observer registered")

                // Check current progress
                val processedCount = applicationDatabase.ocrProgressDao().getProcessedCount() ?: 0
                Log.d("MainActivity", "Already processed: $processedCount images")

                // Start automatic OCR processing if needed
                if (processedCount < totalImages) {
                    Log.d("MainActivity", "Starting automatic OCR processing for ${totalImages - processedCount} remaining images")

                    // Ensure progress status is properly set before starting
                    applicationDatabase.ocrProgressDao().updateProcessingStatus(true)
                    applicationDatabase.ocrProgressDao().updatePausedStatus(false)

                    ocrManager.startContinuousProcessing(batchSize = 50) // Use continuous processing for background operation
                } else {
                    Log.d("MainActivity", "All images already processed")
                    // Mark as complete if all images are processed
                    applicationDatabase.ocrProgressDao().updateProcessingStatus(false)
                }

                // Ensure progress monitoring is active
                ocrManager.ensureProgressMonitoring()

            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to initialize OCR system", e)
            }
        }
    }

    /**
     * Get total number of images in MediaStore
     */
    private fun getTotalImageCount(): Int {
        return try {
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )
            cursor?.use { it.count } ?: 0
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to get total image count", e)
            0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister content observer
        if (::mediaContentObserver.isInitialized) {
            contentResolver.unregisterContentObserver(mediaContentObserver)
        }
    }
}

private fun setupNextScreen(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    window: Window
) {
    selectedItemsList.clear()
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    // window.setDecorFitsSystemWindows(false)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}


