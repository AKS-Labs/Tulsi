package com.aks_labs.tulsi.compose.single_photo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.Window
import android.view.WindowManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.MainActivity.Companion.applicationDatabase
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.compose.dialogs.ConfirmationDialog
import com.aks_labs.tulsi.compose.dialogs.ConfirmationDialogWithBody
import com.aks_labs.tulsi.compose.dialogs.DialogInfoText
import com.aks_labs.tulsi.compose.dialogs.LoadingDialog
import com.aks_labs.tulsi.helpers.appRestoredFilesDir
import com.aks_labs.tulsi.helpers.EncryptionManager
import com.aks_labs.tulsi.helpers.GetDirectoryPermissionAndRun
import com.aks_labs.tulsi.helpers.MediaData
import com.aks_labs.tulsi.helpers.MultiScreenViewType
import com.aks_labs.tulsi.helpers.brightenColor
import com.aks_labs.tulsi.helpers.getDecryptCacheForFile
import com.aks_labs.tulsi.helpers.getExifDataForMedia
import com.aks_labs.tulsi.helpers.moveImageOutOfLockedFolder
import com.aks_labs.tulsi.helpers.permanentlyDeleteSecureFolderImageList
import com.aks_labs.tulsi.helpers.shareSecuredImage
import com.aks_labs.tulsi.helpers.getSecureDecryptedVideoFile
import com.aks_labs.tulsi.helpers.getParentFromPath
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.mediastore.getIv
import com.aks_labs.tulsi.mediastore.getOriginalPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "SINGLE_HIDDEN_PHOTO_VIEW"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleHiddenPhotoView(
    mediaItemId: Long,
    window: Window
) {
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current

    var lastLifecycleState by rememberSaveable {
        mutableStateOf(Lifecycle.State.STARTED)
    }
    var hideSecureFolder by rememberSaveable {
        mutableStateOf(false)
    }
    val isGettingPermissions = rememberSaveable {
    	mutableStateOf(false)
    }

    LaunchedEffect(hideSecureFolder) {
        if (hideSecureFolder
            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SecureFolder.name
        ) {
            navController.navigate(MultiScreenViewType.MainScreen.name)
        }
    }

    DisposableEffect(key1 = lifecycleOwner.lifecycle.currentState, isGettingPermissions.value) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->

                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SecureFolder.name
                            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                            && !isGettingPermissions.value
                        ) {
                            lastLifecycleState = Lifecycle.State.DESTROYED
                        }
                    }

                    Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START, Lifecycle.Event.ON_CREATE -> {
                        if (lastLifecycleState == Lifecycle.State.DESTROYED && navController.currentBackStackEntry != null && !isGettingPermissions.value) {
                            lastLifecycleState = Lifecycle.State.STARTED

                            hideSecureFolder = true
                        }
                    }

                    else -> {}
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    if (hideSecureFolder) return

    val mainViewModel = mainViewModel

    // val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

    val holderGroupedMedia =
        mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

    val mediaItem by remember {
        derivedStateOf {
            holderGroupedMedia.find {
                it.id == mediaItemId
            }
        }
    }

    if (mediaItem == null) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    val appBarsVisible = remember { mutableStateOf(true) }
    val state = rememberPagerState {
        groupedMedia.value.size
    }
    val currentMediaItem by remember {
        derivedStateOf {
            val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
            if (index != groupedMedia.value.size) {
                groupedMedia.value[index]
            } else {
                MediaStoreData(
                    displayName = "Broken Media"
                )
            }
        }
    }

    val showInfoDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(currentMediaItem, appBarsVisible.value, showInfoDialog) {
                navController.popBackStack()
            }
        },
        bottomBar = {
            BottomBar(
                visible = appBarsVisible.value,
                item = currentMediaItem,
                groupedMedia = groupedMedia,
                state = state,
                isGettingPermissions = isGettingPermissions
            ) {
                navController.popBackStack()
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalImageList(
                currentMediaItem = currentMediaItem,
                groupedMedia = groupedMedia.value,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible,
                isHidden = true
            )
        }

        SingleSecuredPhotoInfoDialog(
            showDialog = showInfoDialog,
            currentMediaItem = currentMediaItem
        )
    }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = mediaItem) {
        coroutineScope.launch {
            state.scrollToPage(
                if (groupedMedia.value.indexOf(mediaItem) >= 0) groupedMedia.value.indexOf(mediaItem) else 0
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    mediaItem: MediaStoreData,
    visible: Boolean,
    showInfoDialog: MutableState<Boolean>,
    popBackStack: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeOut(),
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            navigationIcon = {
                IconButton(
                    onClick = { popBackStack() },
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
            title = {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = mediaItem.displayName,
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(160.dp)
                )
            },
            actions = {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                var showLoadingDialog by remember { mutableStateOf(false) }

                if (showLoadingDialog) {
                    LoadingDialog(
                        title = "Sharing Image",
                        body = "Hold on while this image is processed"
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            showLoadingDialog = true

                            val iv = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(mediaItem.absolutePath)
                            if (iv == null) {
                                Log.e(TAG, "IV for ${mediaItem.displayName} was null, aborting")
                                return@launch
                            }

                            val originalFile = File(mediaItem.absolutePath)

                            val cachedFile =
                                if (mediaItem.type == MediaType.Video) {
                                    getSecureDecryptedVideoFile(originalFile.name, context)
                                } else {
                                    getDecryptCacheForFile(originalFile, context)
                                }

                            if (!cachedFile.exists()) {
                                if (mediaItem.type == MediaType.Video) {
                                    EncryptionManager.decryptVideo(
                                        absolutePath = originalFile.absolutePath,
                                        context = context,
                                        iv = iv,
                                        progress = {}
                                    )
                                } else {
                                    EncryptionManager.decryptInputStream(
                                        inputStream = originalFile.inputStream(),
                                        outputStream = cachedFile.outputStream(),
                                        iv = iv
                                    )
                                }
                            }

                            showLoadingDialog = false

                            shareSecuredImage(
                                absolutePath = cachedFile.absolutePath,
                                context = context
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = "share this secured photo",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showInfoDialog.value = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_options),
                        contentDescription = "show more options",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomBar(
    visible: Boolean,
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: PagerState,
    isGettingPermissions: MutableState<Boolean>,
    popBackStack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val showRestoreDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val runRestoreAction = remember { mutableStateOf(false) }

    var showLoadingDialog by remember { mutableStateOf(false) }
    if (showLoadingDialog) {
        LoadingDialog(title = "Restoring Files", body = "Please wait while the media is processed")
    }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = listOf(item.bytes?.getOriginalPath()?.getParentFromPath() ?: context.appRestoredFilesDir),
        shouldRun = runRestoreAction,
        onGranted = { _ ->
	        mainViewModel.launch(Dispatchers.IO) {
	            moveImageOutOfLockedFolder(
	                list = listOf(item),
	                context = context
	            ) {
	                isGettingPermissions.value = false
	                showLoadingDialog = false
	            }

	            sortOutMediaMods(
	                item,
	                groupedMedia,
	                coroutineScope,
	                state
	            ) {
	                popBackStack()
	            }
	        }
        },
        onRejected = {
        	isGettingPermissions.value = false
        	showLoadingDialog = false
        }
    )

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = "Move item out of Secure Folder?",
        confirmButtonLabel = "Move"
    ) {
        isGettingPermissions.value = true
        runRestoreAction.value = true
        showLoadingDialog = true
    }

    ConfirmationDialogWithBody(
        showDialog = showDeleteDialog,
        dialogTitle = "Permanently delete this item?",
        dialogBody = "This action cannot be undone!",
        confirmButtonLabel = "Delete"
    ) {
        mainViewModel.launch(Dispatchers.IO) {
            permanentlyDeleteSecureFolderImageList(
                list = listOf(item.absolutePath),
                context = context
            )

            sortOutMediaMods(
                item,
                groupedMedia,
                coroutineScope,
                state
            ) {
                popBackStack()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeOut(),
    ) {
        // Outer Box container with transparent background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // Floating bottom bar container
            Box(
                modifier = Modifier
                    .height(76.dp)
                    .fillMaxWidth(0.95f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(percent = 35),
                        spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(percent = 35)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(12.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            showRestoreDialog.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.unlock),
                                contentDescription = "Restore Image Button",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(22.dp)
                            )

                            Spacer(
                                modifier = Modifier
                                    .width(8.dp)
                            )

                            Text(
                                text = "Restore",
                                fontSize = TextUnit(16f, TextUnitType.Sp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            showDeleteDialog.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = "Permanently Delete Image Button",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(22.dp)
                            )

                            Spacer(
                                modifier = Modifier
                                    .width(8.dp)
                            )

                            Text(
                                text = "Delete",
                                fontSize = TextUnit(16f, TextUnitType.Sp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleSecuredPhotoInfoDialog(
    showDialog: MutableState<Boolean>,
    currentMediaItem: MediaStoreData
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier.fillMaxWidth(0.85f)

    if (showDialog.value) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Column(
                modifier = Modifier
                    .then(modifier)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(MaterialTheme.colorScheme.surface, 0.1f))
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    Text(
                        text = "Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .wrapContentHeight()
                ) {
                    var mediaData by remember {
                        mutableStateOf(
                            emptyMap<MediaData, Any>()
                        )
                    }

                    var showLoadingDialog by remember { mutableStateOf(false) }
                    if (showLoadingDialog) {
                        LoadingDialog(
                            title = "Getting file info",
                            body = "Please wait..."
                        )
                    }

                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            showLoadingDialog = true

                            val file = if (currentMediaItem.type == MediaType.Video) {
                                val originalFile = File(currentMediaItem.absolutePath)
                                val cachedFile = getSecureDecryptedVideoFile(
                                    name = currentMediaItem.displayName,
                                    context = context
                                )

                                if (!cachedFile.exists()) {
                                    val iv = currentMediaItem.bytes?.getIv()

                                    if (iv == null) {
                                        Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
                                        return@withContext
                                    }
                                    EncryptionManager.decryptVideo(
                                        absolutePath = originalFile.absolutePath,
                                        iv = iv,
                                        context = context,
                                        progress = {}
                                    )
                                } else if (cachedFile.length() < originalFile.length()) {
                                    while (cachedFile.length() < originalFile.length()) {
                                        delay(100)
                                    }

                                    cachedFile
                                } else {
                                    cachedFile
                                }
                            } else {
                                val originalFile = File(currentMediaItem.absolutePath)
                                val cachedFile = getDecryptCacheForFile(
                                    file = originalFile,
                                    context = context
                                )

                                if (!cachedFile.exists()) {
                                    val iv = currentMediaItem.bytes?.getIv()

                                    if (iv == null) {
                                        Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
                                        return@withContext
                                    }
                                    EncryptionManager.decryptInputStream(
                                        inputStream = originalFile.inputStream(),
                                        outputStream = cachedFile.outputStream(),
                                        iv = iv
                                    )

                                    cachedFile
                                } else if (cachedFile.length() < originalFile.length()) {
                                    while (cachedFile.length() < originalFile.length()) {
                                        delay(100)
                                    }

                                    cachedFile
                                } else {
                                    cachedFile
                                }
                            }

                            getExifDataForMedia(file.absolutePath).collect {
                                mediaData = it
                            }
                            showLoadingDialog = false
                        }
                    }

                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                    ) {
                        for (key in mediaData.keys) {
                            val value = mediaData[key]

                            val splitBy = Regex("(?=[A-Z])")
                            val split = key.toString().split(splitBy)
                            // println("SPLIT IS $split")
                            val name = if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()

                            DialogInfoText(
                                firstText = name,
                                secondText = value.toString(),
                                iconResId = key.iconResInt,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


