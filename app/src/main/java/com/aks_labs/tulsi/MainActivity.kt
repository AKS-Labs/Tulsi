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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import com.aks_labs.tulsi.compose.app_bars.AnimatedBottomNavigationBar
import com.aks_labs.tulsi.compose.app_bars.MainAppBottomBar
import com.aks_labs.tulsi.compose.app_bars.MainAppSelectingBottomBar
import com.aks_labs.tulsi.compose.app_bars.MainAppTopBar
import com.aks_labs.tulsi.compose.app_bars.getAppBarContentTransition
import com.aks_labs.tulsi.compose.app_bars.setBarVisibility
import com.aks_labs.tulsi.compose.utils.DynamicStatusBarController
import com.aks_labs.tulsi.compose.utils.ScrollVisibilityState
import com.aks_labs.tulsi.compose.utils.handleScrollVisibilityChange
import com.aks_labs.tulsi.compose.utils.handleBottomBarScrollVisibilityChange
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
import com.aks_labs.tulsi.ocr.DevanagariOcrManager
import com.aks_labs.tulsi.ocr.MediaContentObserver
import com.aks_labs.tulsi.datastore.Settings
import com.aks_labs.tulsi.datastore.Ocr
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers as CoroutineDispatchers
import kotlinx.coroutines.flow.first
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
        private const val TAG = "BOTTOM_BAR_ANIMATION"
    }

    private lateinit var mediaContentObserver: MediaContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        // Configure window for better navigation bar handling
        configureSystemUI()

        Log.d(TAG, "Creating database instance with migrations...")
        val mediaDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(
                Migration3to4(applicationContext),
                Migration4to5(applicationContext),
                Migration5to6(applicationContext),
                Migration6to7(applicationContext),
                com.aks_labs.tulsi.database.migrations.Migration7to8
            )
        }.build()
        applicationDatabase = mediaDatabase

        // Verify database version and Devanagari tables
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val version = mediaDatabase.openHelper.readableDatabase.version
                Log.d(TAG, "📊 Current database version: $version")

                // Test Devanagari OCR tables accessibility
                val devanagariTextCount = mediaDatabase.devanagariOcrTextDao().getOcrTextCount()
                val devanagariProgress = mediaDatabase.devanagariOcrProgressDao().getProgress()
                Log.d(TAG, "✅ Devanagari OCR tables accessible - Text count: $devanagariTextCount, Progress: $devanagariProgress")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to access Devanagari OCR tables", e)
                Log.e(TAG, "This indicates the database migration did not run properly")
            }
        }

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
                        Log.d(TAG, "Transitioning to main app content")
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

        // Initialize OCR system when entering main app after permissions are granted
        LaunchedEffect(Unit) {
            Log.d(TAG, "Main app content loaded - initializing OCR system")
            // Small delay to ensure permission state is updated after user grants permissions
            kotlinx.coroutines.delay(500)
            ensureOcrSystemInitialized()
        }

        // Handle notification navigation intents
        LaunchedEffect(Unit) {
            val navigateToOcrSettings = intent?.getBooleanExtra("navigate_to_ocr_settings", false) ?: false
            if (navigateToOcrSettings) {
                Log.d(TAG, "Navigation intent received - navigating to OCR Language Models page")
                navControllerLocal.navigate(MultiScreenViewType.OcrLanguageModelsView.name)
            }
        }

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

                    composable(MultiScreenViewType.OcrLanguageModelsView.name) {
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

                        com.aks_labs.tulsi.compose.settings.OcrLanguageModelsPage(
                            onNavigateUp = { navControllerLocal.navigateUp() }
                        )
                    }
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
    }

    @OptIn(ExperimentalMaterial3Api::class)
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

        // Scroll visibility state for photos page
        var photosScrollVisibilityState by remember { mutableStateOf(ScrollVisibilityState()) }
        var photosLastScrollIndex by remember { mutableStateOf(0) }

        // Scroll visibility state for custom album pages
        var albumScrollVisibilityState by remember { mutableStateOf(ScrollVisibilityState()) }
        var albumLastScrollIndex by remember { mutableStateOf(0) }

        // Bottom bar visibility state for scroll-based animations
        var isBottomBarVisible by remember { mutableStateOf(true) }

        // Log initial state
        Log.d(TAG, "MainActivity: Initial isBottomBarVisible = $isBottomBarVisible")

        // Log state changes
        LaunchedEffect(isBottomBarVisible) {
            Log.d(TAG, "MainActivity: isBottomBarVisible changed to $isBottomBarVisible")
        }

        // Reset bottom bar visibility when switching to secure screen
        LaunchedEffect(currentView.value) {
            Log.d(TAG, "MainActivity: Current view changed to ${currentView.value.name}")
            if (currentView.value == DefaultTabs.TabTypes.secure) {
                Log.d(TAG, "MainActivity: Switching to secure screen - forcing bottom bar visible")
                isBottomBarVisible = true
            }
        }

        // Create scroll behavior for smooth top app bar animations
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        // Create shared grid state for photo grids
        val sharedGridState = rememberLazyGridState()

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

        // Dynamic status bar controller for photos and album pages
        when (currentView.value) {
            DefaultTabs.TabTypes.Gallery -> {
                DynamicStatusBarController(isVisible = photosScrollVisibilityState.isStatusBarVisible)
            }
            DefaultTabs.TabTypes.search -> {
                // Search page handles its own status bar controller
            }
            else -> {
                // For custom album tabs that use PhotoGrid
                if (currentView.value.albumPaths.isNotEmpty()) {
                    DynamicStatusBarController(isVisible = albumScrollVisibilityState.isStatusBarVisible)
                } else {
                    // For other tabs, ensure status bar is visible
                    DynamicStatusBarController(isVisible = true)
                }
            }
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
                    isVisible = isTopBarVisible,
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                BottomBar(
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabList,
                    isBottomBarVisible = isBottomBarVisible
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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

                                // Monitor scroll state for custom album page auto-hide functionality
                                LaunchedEffect(sharedGridState.firstVisibleItemIndex) {
                                    val currentIndex = sharedGridState.firstVisibleItemIndex
                                    Log.d(TAG, "Custom Album: LaunchedEffect triggered - firstVisibleItemIndex=$currentIndex")
                                    Log.d(TAG, "Custom Album: Scroll detected - currentIndex=$currentIndex, lastIndex=$albumLastScrollIndex")

                                    // Handle scroll visibility changes for both app bar and status bar
                                    // Use immediate response for album pages like Photos screen
                                    handleScrollVisibilityChange(
                                        currentIndex = currentIndex,
                                        lastScrollIndex = albumLastScrollIndex,
                                        scrollThreshold = 0, // Immediate hiding for album pages
                                        onVisibilityChange = { newState ->
                                            albumScrollVisibilityState = newState
                                            isTopBarVisible = newState.isAppBarVisible
                                        }
                                    )

                                    // Handle bottom bar scroll visibility for album screen
                                    Log.d(TAG, "Custom Album: Calling handleBottomBarScrollVisibilityChange")
                                    handleBottomBarScrollVisibilityChange(
                                        currentIndex = currentIndex,
                                        lastScrollIndex = albumLastScrollIndex,
                                        scrollThreshold = 0, // Immediate hiding for smooth animation
                                        onBottomBarVisibilityChange = { visible ->
                                            Log.d(TAG, "Custom Album: Bottom bar visibility callback - visible=$visible, previous=$isBottomBarVisible")
                                            isBottomBarVisible = visible
                                        }
                                    )

                                    albumLastScrollIndex = currentIndex
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
                                    state = sharedGridState
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

                                // Monitor scroll state for photos page auto-hide functionality
                                LaunchedEffect(sharedGridState.firstVisibleItemIndex) {
                                    val currentIndex = sharedGridState.firstVisibleItemIndex
                                    Log.d(TAG, "Gallery: LaunchedEffect triggered - firstVisibleItemIndex=$currentIndex")
                                    Log.d(TAG, "Gallery: Scroll detected - currentIndex=$currentIndex, lastIndex=$photosLastScrollIndex")

                                    // Handle scroll visibility changes for both app bar and status bar
                                    // Use immediate response (threshold = 0) for Photos screen since it has no search bar
                                    handleScrollVisibilityChange(
                                        currentIndex = currentIndex,
                                        lastScrollIndex = photosLastScrollIndex,
                                        scrollThreshold = 0, // Immediate hiding for Photos screen
                                        onVisibilityChange = { newState ->
                                            photosScrollVisibilityState = newState
                                            isTopBarVisible = newState.isAppBarVisible
                                        }
                                    )

                                    // Handle bottom bar scroll visibility for gallery screen
                                    Log.d(TAG, "Gallery: Calling handleBottomBarScrollVisibilityChange")
                                    handleBottomBarScrollVisibilityChange(
                                        currentIndex = currentIndex,
                                        lastScrollIndex = photosLastScrollIndex,
                                        scrollThreshold = 0, // Immediate hiding for smooth animation
                                        onBottomBarVisibilityChange = { visible ->
                                            Log.d(TAG, "Gallery: Bottom bar visibility callback - visible=$visible, previous=$isBottomBarVisible")
                                            isBottomBarVisible = visible
                                        }
                                    )

                                    photosLastScrollIndex = currentIndex
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = multiAlbumViewModel.albumInfo,
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                    state = sharedGridState
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.secure -> LockedFolderEntryView(
                                currentView
                            )

                            stateValue == DefaultTabs.TabTypes.albums -> {
                                AlbumsGridView(
                                    currentView = currentView,
                                    onBottomBarVisibilityChange = { visible ->
                                        isBottomBarVisible = visible
                                    }
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.search -> {
                                selectedItemsList.clear()

                                SearchPage(
                                    selectedItemsList = selectedItemsList,
                                    currentView = currentView,
                                    onTopBarVisibilityChange = { visible ->
                                        isTopBarVisible = visible
                                    },
                                    onBottomBarVisibilityChange = { visible ->
                                        isBottomBarVisible = visible
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        currentView: MutableState<BottomBarTab>,
        isVisible: Boolean = true,
        scrollBehavior: TopAppBarScrollBehavior? = null
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
                currentView = currentView,
                scrollBehavior = scrollBehavior
            )
        }
    }

    @Composable
    private fun BottomBar(
        currentView: MutableState<BottomBarTab>,
        tabs: List<BottomBarTab>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        isBottomBarVisible: Boolean = true
    ) {
        Log.d(TAG, "BottomBar: isBottomBarVisible=$isBottomBarVisible")

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
                AnimatedBottomNavigationBar(
                    currentView = currentView,
                    tabs = tabs,
                    selectedItemsList = selectedItemsList,
                    isVisible = isBottomBarVisible
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
        Log.d(TAG, "Initializing OCR system...")
        CoroutineScope(CoroutineDispatchers.IO).launch {
            try {
                // Check if we have necessary permissions before initializing
                if (!hasRequiredPermissions()) {
                    Log.d(TAG, "Required permissions not granted, skipping OCR initialization")
                    return@launch
                }

                val ocrManager = OcrManager(applicationContext, applicationDatabase)

                // Initialize progress tracking
                val totalImages = getTotalImageCount()
                Log.d(TAG, "Found $totalImages total images")
                ocrManager.initializeProgress(totalImages)

                // Set up content observer for new images if not already registered
                if (!::mediaContentObserver.isInitialized) {
                    mediaContentObserver = MediaContentObserver(applicationContext)
                    contentResolver.registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true,
                        mediaContentObserver
                    )
                    Log.d(TAG, "Content observer registered")
                }

                // Check current progress
                val processedCount = applicationDatabase.ocrProgressDao().getProcessedCount() ?: 0
                Log.d(TAG, "Already processed: $processedCount images")

                // Start automatic OCR processing if needed
                if (processedCount < totalImages) {
                    Log.d(TAG, "Starting automatic OCR processing for ${totalImages - processedCount} remaining images")

                    // Ensure progress status is properly set before starting
                    applicationDatabase.ocrProgressDao().updateProcessingStatus(true)
                    applicationDatabase.ocrProgressDao().updatePausedStatus(false)

                    ocrManager.startContinuousProcessing(batchSize = 50) // Use continuous processing for background operation
                } else {
                    Log.d(TAG, "All images already processed")
                    // Mark as complete if all images are processed
                    applicationDatabase.ocrProgressDao().updateProcessingStatus(false)
                }

                // Ensure progress monitoring is active
                ocrManager.ensureProgressMonitoring()

                // Reset dismissed state on app restart to allow progress monitoring
                Log.d(TAG, "Resetting Latin OCR progress bar dismissed state on app restart")
                ocrManager.showProgressBar()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OCR system", e)
            }
        }

        // Initialize Devanagari OCR system if enabled
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if Devanagari OCR is enabled in settings
                val settings = Settings(applicationContext, CoroutineScope(Dispatchers.IO))
                val isDevanagariOcrEnabled = settings.Ocr.devanagariOcrEnabled.first()

                Log.d(TAG, "Devanagari OCR enabled in settings: $isDevanagariOcrEnabled")

                if (isDevanagariOcrEnabled) {
                    Log.d(TAG, "Initializing Devanagari OCR system...")

                    val devanagariOcrManager = DevanagariOcrManager(applicationContext, applicationDatabase)
                    val totalImages = getTotalImageCount()

                    // Initialize progress if needed
                    val devanagariProgress = applicationDatabase.devanagariOcrProgressDao().getProgress()
                    if (devanagariProgress == null && totalImages > 0) {
                        Log.d(TAG, "Devanagari OCR system not initialized, initializing now...")
                        devanagariOcrManager.initializeProgress(totalImages)
                    }

                    // Check current progress
                    val processedCount = applicationDatabase.devanagariOcrProgressDao().getProcessedCount() ?: 0
                    Log.d(TAG, "Devanagari OCR - Already processed: $processedCount images")

                    // Start automatic Devanagari OCR processing if needed
                    if (processedCount < totalImages) {
                        Log.d(TAG, "Starting automatic Devanagari OCR processing for ${totalImages - processedCount} remaining images")

                        // Ensure progress status is properly set before starting
                        applicationDatabase.devanagariOcrProgressDao().updateProcessingStatus(true)
                        applicationDatabase.devanagariOcrProgressDao().updatePausedStatus(false)

                        devanagariOcrManager.startContinuousProcessing(batchSize = 50)
                    } else {
                        Log.d(TAG, "All Devanagari OCR images already processed")
                        // Mark as complete if all images are processed
                        applicationDatabase.devanagariOcrProgressDao().updateProcessingStatus(false)
                    }

                    // Ensure progress monitoring is active
                    devanagariOcrManager.ensureProgressMonitoring()
                } else {
                    Log.d(TAG, "Devanagari OCR is disabled in settings, skipping initialization")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Devanagari OCR system", e)
            }
        }
    }

    /**
     * Public method to trigger OCR initialization from external components
     */
    fun triggerOcrInitialization() {
        Log.d(TAG, "External trigger for OCR initialization received")
        CoroutineScope(CoroutineDispatchers.Main).launch {
            // Small delay to ensure the UI transition is complete
            kotlinx.coroutines.delay(1000)
            ensureOcrSystemInitialized()

            // Also force start progress monitoring to ensure UI updates
            kotlinx.coroutines.delay(500)
            val ocrManager = OcrManager(applicationContext, applicationDatabase)
            ocrManager.forceStartProgressMonitoring()
            Log.d(TAG, "Triggered progress monitoring from external call")
        }
    }

    /**
     * Ensure OCR system is initialized after permissions are granted
     */
    private fun ensureOcrSystemInitialized() {
        Log.d(TAG, "Ensuring OCR system is initialized...")
        CoroutineScope(CoroutineDispatchers.IO).launch {
            try {
                // More aggressive permission checking with longer retry period
                var permissionCheckAttempts = 0
                val maxAttempts = 5
                while (!hasRequiredPermissions() && permissionCheckAttempts < maxAttempts) {
                    Log.d(TAG, "Required permissions not yet available, waiting... (attempt ${permissionCheckAttempts + 1}/$maxAttempts)")
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds before retry
                    permissionCheckAttempts++
                }

                if (!hasRequiredPermissions()) {
                    Log.w(TAG, "Required permissions not granted after $maxAttempts retries, cannot initialize OCR")
                    // Try one more time with direct system check
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    } else {
                        applicationContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }

                    if (!hasPermission) {
                        Log.e(TAG, "Direct permission check also failed, aborting OCR initialization")
                        return@launch
                    } else {
                        Log.d(TAG, "Direct permission check succeeded, continuing with OCR initialization")
                    }
                }

                Log.d(TAG, "Permissions confirmed, proceeding with OCR initialization")

                // Check if OCR system needs initialization
                val progress = applicationDatabase.ocrProgressDao().getProgress()
                val totalImages = getTotalImageCount()

                Log.d(TAG, "OCR status check: progress=$progress, totalImages=$totalImages")

                if (progress == null && totalImages > 0) {
                    Log.d(TAG, "OCR system not initialized, initializing now...")
                    initializeOcrSystem()

                    // Force start progress monitoring to ensure UI updates on first launch
                    kotlinx.coroutines.delay(1000) // Wait for initialization to complete
                    val ocrManager = OcrManager(applicationContext, applicationDatabase)
                    ocrManager.forceStartProgressMonitoring()
                    Log.d(TAG, "Forced progress monitoring start for first launch")
                } else if (progress != null) {
                    Log.d(TAG, "OCR system already initialized, ensuring monitoring is active")
                    val ocrManager = OcrManager(applicationContext, applicationDatabase)
                    ocrManager.ensureProgressMonitoring()

                    // Check if we need to resume processing
                    val processedCount = applicationDatabase.ocrProgressDao().getProcessedCount() ?: 0
                    Log.d(TAG, "OCR progress: $processedCount/$totalImages processed")

                    if (processedCount < totalImages && !progress.isProcessing && !progress.isPaused) {
                        Log.d(TAG, "Resuming OCR processing for remaining images")
                        applicationDatabase.ocrProgressDao().updateProcessingStatus(true)
                        ocrManager.startContinuousProcessing(batchSize = 50)
                    } else if (progress.isProcessing) {
                        Log.d(TAG, "OCR processing already in progress")
                    } else if (progress.isPaused) {
                        Log.d(TAG, "OCR processing is paused")
                    } else {
                        Log.d(TAG, "OCR processing appears to be complete")
                    }
                } else if (totalImages == 0) {
                    Log.d(TAG, "No images found to process")
                } else {
                    Log.d(TAG, "OCR system already fully processed")
                }

                // Also check Devanagari OCR resume logic
                val settings = Settings(applicationContext, CoroutineScope(Dispatchers.IO))
                val isDevanagariOcrEnabled = settings.Ocr.devanagariOcrEnabled.first()

                if (isDevanagariOcrEnabled) {
                    Log.d(TAG, "Checking Devanagari OCR resume status...")
                    val devanagariOcrManager = DevanagariOcrManager(applicationContext, applicationDatabase)
                    val devanagariProgress = applicationDatabase.devanagariOcrProgressDao().getProgress()

                    if (devanagariProgress != null) {
                        val devanagariProcessedCount = applicationDatabase.devanagariOcrProgressDao().getProcessedCount() ?: 0
                        Log.d(TAG, "Devanagari OCR progress: $devanagariProcessedCount/$totalImages processed")

                        if (devanagariProcessedCount < totalImages && !devanagariProgress.isProcessing && !devanagariProgress.isPaused) {
                            Log.d(TAG, "Resuming Devanagari OCR processing for remaining images")
                            applicationDatabase.devanagariOcrProgressDao().updateProcessingStatus(true)
                            devanagariOcrManager.startContinuousProcessing(batchSize = 50)
                        } else if (devanagariProgress.isProcessing) {
                            Log.d(TAG, "Devanagari OCR processing already in progress")
                        } else if (devanagariProgress.isPaused) {
                            Log.d(TAG, "Devanagari OCR processing is paused")
                        } else {
                            Log.d(TAG, "Devanagari OCR processing appears to be complete")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure OCR system initialization", e)
            }
        }
    }

    /**
     * Check if required permissions are granted
     */
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
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


