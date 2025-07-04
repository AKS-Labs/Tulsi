package com.aks_labs.tulsi.compose.text_selection

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.aks_labs.tulsi.ocr.SelectableOcrResult
import com.aks_labs.tulsi.ocr.SelectableTextBlock
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource

/**
 * Dedicated full-screen image viewer for text selection mode
 * This viewer provides accurate text positioning without complex zoom/pan transformations
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TextSelectionImageViewer(
    imageUri: String,
    ocrResult: SelectableOcrResult?,
    textSelectionState: TextSelectionState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val view = LocalView.current
    val window = (view.context as ComponentActivity).window
    
    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Container size for coordinate transformation
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    // Enable edge-to-edge display
    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemGestureExclusion()
    ) {
        // Main image display
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // Update container size when constraints change
            LaunchedEffect(constraints) {
                containerSize = Size(
                    width = constraints.maxWidth.toFloat(),
                    height = constraints.maxHeight.toFloat()
                )
            }
            
            // Display image with ContentScale.Fit (no zoom/pan transformations)
            GlideImage(
                model = imageUri,
                contentDescription = "Image for text selection",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Text selection overlay
            if (ocrResult != null && containerSize != Size.Zero) {
                TextSelectionOverlaySimplified(
                    ocrResult = ocrResult,
                    containerSize = containerSize,
                    textSelectionState = textSelectionState,
                    onShowContextMenu = { position ->
                        contextMenuPosition = position
                        showContextMenu = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Close button (top-right)
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close text selection",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Native-style context menu
        if (showContextMenu) {
            NativeTextSelectionContextMenu(
                position = contextMenuPosition,
                selectedText = textSelectionState.getSelectedText(),
                onDismiss = { showContextMenu = false },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(textSelectionState.getSelectedText()))
                    showContextMenu = false
                },
                onShare = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, textSelectionState.getSelectedText())
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share text"))
                    showContextMenu = false
                },
                onWebSearch = {
                    val searchIntent = Intent().apply {
                        action = Intent.ACTION_WEB_SEARCH
                        putExtra("query", textSelectionState.getSelectedText())
                    }
                    context.startActivity(searchIntent)
                    showContextMenu = false
                },
                onSelectAll = {
                    textSelectionState.selectAllTextBlocks()
                    showContextMenu = false
                }
            )
        }
        
        // Bottom selection panel
        if (textSelectionState.hasSelectedText()) {
            BottomSelectionPanel(
                selectedText = textSelectionState.getSelectedText(),
                onCopy = {
                    clipboardManager.setText(AnnotatedString(textSelectionState.getSelectedText()))
                },
                onSelectAll = {
                    textSelectionState.selectAllTextBlocks()
                },
                onClear = {
                    textSelectionState.clearAllSelections()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Simplified text selection overlay with accurate coordinate transformation
 * Uses only ContentScale.Fit transformation without complex zoom/pan
 */
@Composable
private fun TextSelectionOverlaySimplified(
    ocrResult: SelectableOcrResult,
    containerSize: Size,
    textSelectionState: TextSelectionState,
    onShowContextMenu: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Transform text blocks to screen coordinates using simplified transformation
    val screenTextBlocks = remember(ocrResult, containerSize) {
        ocrResult.textBlocks.map { block ->
            transformTextBlockToScreen(block, ocrResult.imageSize, containerSize)
        }
    }
    
    Box(modifier = modifier) {
        // Draw individual word overlays for granular selection
        screenTextBlocks.forEach { textBlock ->
            textBlock.getAllElements().forEach { element ->
                WordInteractiveOverlayAccurate(
                    element = element,
                    isSelected = element.isSelected,
                    onTap = { 
                        textSelectionState.toggleElementSelection(element.id)
                        // Show context menu if text is selected
                        if (textSelectionState.hasSelectedText()) {
                            onShowContextMenu(
                                Offset(
                                    element.boundingBox.center.x,
                                    element.boundingBox.top - 50f
                                )
                            )
                        }
                    },
                    onLongPress = {
                        // Select entire sentence on long press
                        selectSentence(textBlock, element, textSelectionState)
                        // Show context menu
                        onShowContextMenu(
                            Offset(
                                element.boundingBox.center.x,
                                element.boundingBox.top - 50f
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Transform text block coordinates from image space to screen space
 * Uses simplified ContentScale.Fit transformation for accurate positioning
 */
private fun transformTextBlockToScreen(
    textBlock: SelectableTextBlock,
    originalImageSize: Size,
    containerSize: Size
): SelectableTextBlock {
    // Calculate ContentScale.Fit scale factor
    val scaleX = containerSize.width / originalImageSize.width
    val scaleY = containerSize.height / originalImageSize.height
    val scale = minOf(scaleX, scaleY)
    
    // Calculate displayed image dimensions
    val displayedWidth = originalImageSize.width * scale
    val displayedHeight = originalImageSize.height * scale
    
    // Calculate centering offsets for letterboxing/pillarboxing
    val offsetX = (containerSize.width - displayedWidth) / 2f
    val offsetY = (containerSize.height - displayedHeight) / 2f
    
    // Transform all elements in the text block
    val transformedLines = textBlock.lines.map { line ->
        val transformedElements = line.elements.map { element ->
            val transformedBoundingBox = androidx.compose.ui.geometry.Rect(
                left = element.boundingBox.left * scale + offsetX,
                top = element.boundingBox.top * scale + offsetY,
                right = element.boundingBox.right * scale + offsetX,
                bottom = element.boundingBox.bottom * scale + offsetY
            )
            
            element.copy(boundingBox = transformedBoundingBox)
        }
        
        // Transform line bounding box
        val transformedLineBoundingBox = androidx.compose.ui.geometry.Rect(
            left = line.boundingBox.left * scale + offsetX,
            top = line.boundingBox.top * scale + offsetY,
            right = line.boundingBox.right * scale + offsetX,
            bottom = line.boundingBox.bottom * scale + offsetY
        )
        
        line.copy(
            boundingBox = transformedLineBoundingBox,
            elements = transformedElements
        )
    }
    
    // Transform block bounding box
    val transformedBlockBoundingBox = androidx.compose.ui.geometry.Rect(
        left = textBlock.boundingBox.left * scale + offsetX,
        top = textBlock.boundingBox.top * scale + offsetY,
        right = textBlock.boundingBox.right * scale + offsetX,
        bottom = textBlock.boundingBox.bottom * scale + offsetY
    )
    
    return textBlock.copy(
        boundingBox = transformedBlockBoundingBox,
        lines = transformedLines
    )
}

/**
 * Accurate word interactive overlay with precise positioning
 */
@Composable
private fun WordInteractiveOverlayAccurate(
    element: com.aks_labs.tulsi.ocr.SelectableTextElement,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }

    // Animated highlight color for smooth transitions
    val highlightColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF1976D2).copy(alpha = 0.5f) // Google-style blue highlight
            isPressed -> Color(0xFF1976D2).copy(alpha = 0.2f) // Pressed state
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "highlight_color"
    )

    // Subtle border for text boundaries
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF1976D2).copy(alpha = 0.8f)
            isPressed -> Color(0xFF1976D2).copy(alpha = 0.4f)
            else -> Color.White.copy(alpha = 0.2f) // Subtle border to show text boundaries
        },
        animationSpec = tween(durationMillis = 200),
        label = "border_color"
    )

    // Scale animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = element.boundingBox.left.toInt(),
                    y = element.boundingBox.top.toInt()
                )
            }
            .size(
                width = with(density) { element.boundingBox.width.toDp() },
                height = with(density) { element.boundingBox.height.toDp() }
            )
            .scale(scale)
            .background(
                color = highlightColor,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress,
                onClickLabel = "Select word: ${element.text}",
                onLongClickLabel = "Select sentence containing: ${element.text}"
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    )
}

/**
 * Select entire sentence containing the given element
 */
private fun selectSentence(
    textBlock: SelectableTextBlock,
    element: com.aks_labs.tulsi.ocr.SelectableTextElement,
    textSelectionState: TextSelectionState
) {
    // Find the line containing this element
    val containingLine = textBlock.lines.find { line ->
        line.elements.any { it.id == element.id }
    } ?: return

    // Get all elements in the line
    val lineElements = containingLine.elements
    val elementIndex = lineElements.indexOfFirst { it.id == element.id }

    if (elementIndex == -1) return

    // Find sentence boundaries using punctuation
    val sentenceEndPunctuation = setOf(".", "!", "?", ":", ";")
    val sentenceStartPunctuation = setOf(".", "!", "?")

    // Find start of sentence (look backwards for sentence-ending punctuation)
    var startIndex = 0
    for (i in elementIndex - 1 downTo 0) {
        val elementText = lineElements[i].text.trim()
        if (elementText.isNotEmpty() && sentenceStartPunctuation.any { elementText.endsWith(it) }) {
            startIndex = i + 1
            break
        }
    }

    // Find end of sentence (look forwards for sentence-ending punctuation)
    var endIndex = lineElements.size - 1
    for (i in elementIndex until lineElements.size) {
        val elementText = lineElements[i].text.trim()
        if (elementText.isNotEmpty() && sentenceEndPunctuation.any { elementText.endsWith(it) }) {
            endIndex = i
            break
        }
    }

    // Select all elements in the sentence range
    for (i in startIndex..endIndex) {
        if (i < lineElements.size) {
            textSelectionState.toggleElementSelection(lineElements[i].id)
        }
    }
}

/**
 * Native-style text selection context menu
 */
@Composable
private fun NativeTextSelectionContextMenu(
    position: Offset,
    selectedText: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onWebSearch: () -> Unit,
    onSelectAll: () -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = with(density) { position.x.toDp().roundToPx() },
                    y = with(density) { position.y.toDp().roundToPx() }
                )
            }
    ) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // Copy option
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = onCopy,
                enabled = selectedText.isNotEmpty()
            )

            // Share option
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = onShare,
                enabled = selectedText.isNotEmpty()
            )

            // Web search option
            DropdownMenuItem(
                text = { Text("Web search") },
                onClick = onWebSearch,
                enabled = selectedText.isNotEmpty()
            )

            // Select all option
            DropdownMenuItem(
                text = { Text("Select all") },
                onClick = onSelectAll
            )
        }
    }
}

/**
 * Bottom selection panel with text preview and action buttons
 */
@Composable
private fun BottomSelectionPanel(
    selectedText: String,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Selected text preview
            Text(
                text = "Selected: ${selectedText.take(100)}${if (selectedText.length > 100) "..." else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Copy button
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = com.aks_labs.tulsi.R.drawable.copy),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Select All button
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Clear button
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}
