package com.aks_labs.tulsi.compose.text_selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.content.Intent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.ocr.SelectableOcrResult
import com.aks_labs.tulsi.ocr.SelectableTextBlock

/**
 * Dedicated text selection viewer with simplified coordinate system
 * This viewer displays a single image without zoom/pan functionality for accurate text selection
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun TextSelectionViewer(
    imageUri: String,
    ocrResult: SelectableOcrResult?,
    textSelectionState: TextSelectionState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val density = LocalDensity.current

    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    
    // State for container size
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    // Enable edge-to-edge display
    val view = LocalView.current
    val window = (view.context as ComponentActivity).window

    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemGestureExclusion()
    ) {
        // Simplified overlay with just close button for full-screen experience
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Close button
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close text selection",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Image with text selection overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    containerSize = Size(size.width.toFloat(), size.height.toFloat())
                }
        ) {
            // Background image
            GlideImage(
                model = imageUri,
                contentDescription = "Image for text selection",
                contentScale = ContentScale.Fit,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier.fillMaxSize()
            )
            
            // Text selection overlay
            if (ocrResult != null && containerSize != Size.Zero) {
                TextSelectionOverlaySimplified(
                    ocrResult = ocrResult,
                    containerSize = containerSize,
                    textSelectionState = textSelectionState,
                    showContextMenu = showContextMenu,
                    onShowContextMenu = { position ->
                        contextMenuPosition = position
                        showContextMenu = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                }
            )
        }
    }
}

/**
 * Simplified text selection overlay with accurate coordinate transformation
 */
@Composable
private fun TextSelectionOverlaySimplified(
    ocrResult: SelectableOcrResult,
    containerSize: Size,
    textSelectionState: TextSelectionState,
    showContextMenu: Boolean,
    onShowContextMenu: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Transform text blocks to screen coordinates using simplified transformation
    val screenTextBlocks = remember(ocrResult, containerSize) {
        ocrResult.textBlocks.map { block ->
            transformTextBlockToScreen(block, ocrResult.imageSize, containerSize)
        }
    }
    
    Box(modifier = modifier) {
        // Draw text block overlays
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            screenTextBlocks.forEach { textBlock ->
                drawTextBlockOverlaySimplified(
                    textBlock = textBlock,
                    isSelected = textBlock.isSelected
                )
            }
        }
        
        // Interactive text block areas
        screenTextBlocks.forEach { textBlock ->
            // Draw individual word overlays for granular selection
            textBlock.getAllElements().forEach { element ->
                WordInteractiveOverlay(
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
                        // Select entire line on long press
                        textSelectionState.selectLine(textBlock.lines.find { line ->
                            line.elements.contains(element)
                        }?.id ?: "")
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

        // Context menu is now handled in the main TextSelectionViewer
    }
}

/**
 * Simplified coordinate transformation without zoom/pan complexity
 */
private fun transformTextBlockToScreen(
    textBlock: SelectableTextBlock,
    originalImageSize: Size,
    containerSize: Size
): SelectableTextBlock {
    android.util.Log.d("TextSelectionSimple", "=== transformTextBlockToScreen START ===")
    android.util.Log.d("TextSelectionSimple", "Block ID: ${textBlock.id}, Text: ${textBlock.text.take(20)}...")
    android.util.Log.d("TextSelectionSimple", "Original boundingBox: ${textBlock.boundingBox}")
    android.util.Log.d("TextSelectionSimple", "Original image size: $originalImageSize")
    android.util.Log.d("TextSelectionSimple", "Container size: $containerSize")
    
    // Step 1: Calculate ContentScale.Fit scale factor
    val scaleX = containerSize.width / originalImageSize.width
    val scaleY = containerSize.height / originalImageSize.height
    val fitScale = minOf(scaleX, scaleY)
    android.util.Log.d("TextSelectionSimple", "ContentScale.Fit scale: $fitScale")
    
    // Step 2: Calculate displayed image dimensions
    val displayedImageWidth = originalImageSize.width * fitScale
    val displayedImageHeight = originalImageSize.height * fitScale
    android.util.Log.d("TextSelectionSimple", "Displayed image size: ${displayedImageWidth}x${displayedImageHeight}")
    
    // Step 3: Calculate centering offsets
    val centerOffsetX = (containerSize.width - displayedImageWidth) / 2f
    val centerOffsetY = (containerSize.height - displayedImageHeight) / 2f
    android.util.Log.d("TextSelectionSimple", "Center offsets: X=$centerOffsetX, Y=$centerOffsetY")
    
    // Step 4: Transform bounding box coordinates
    val originalBox = textBlock.boundingBox
    val screenLeft = originalBox.left * fitScale + centerOffsetX
    val screenTop = originalBox.top * fitScale + centerOffsetY
    val screenRight = originalBox.right * fitScale + centerOffsetX
    val screenBottom = originalBox.bottom * fitScale + centerOffsetY
    
    val screenBoundingBox = Rect(
        offset = Offset(screenLeft, screenTop),
        size = Size(
            width = screenRight - screenLeft,
            height = screenBottom - screenTop
        )
    )
    
    android.util.Log.d("TextSelectionSimple", "Screen bounding box: $screenBoundingBox")
    android.util.Log.d("TextSelectionSimple", "=== transformTextBlockToScreen END ===")
    
    return textBlock.copy(boundingBox = screenBoundingBox)
}

/**
 * Interactive area for text block selection
 */
@Composable
private fun TextBlockInteractiveAreaSimplified(
    textBlock: SelectableTextBlock,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    val density = LocalDensity.current
    val boundingBox = textBlock.boundingBox
    
    // Selection colors
    val selectionColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = boundingBox.left.toInt(),
                    y = boundingBox.top.toInt()
                )
            }
            .size(
                width = with(density) { boundingBox.width.toDp() },
                height = with(density) { boundingBox.height.toDp() }
            )
            .clip(RoundedCornerShape(4.dp))
            .background(selectionColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggleSelection()
            }
            .zIndex(1f)
    )
}

/**
 * Draw text block overlay on canvas
 */
private fun DrawScope.drawTextBlockOverlaySimplified(
    textBlock: SelectableTextBlock,
    isSelected: Boolean
) {
    val boundingBox = textBlock.boundingBox
    val rect = androidx.compose.ui.geometry.Rect(
        offset = Offset(boundingBox.left, boundingBox.top),
        size = Size(boundingBox.width, boundingBox.height)
    )
    
    // Draw selection highlight
    val color = if (isSelected) {
        Color.Blue.copy(alpha = 0.3f)
    } else {
        Color.Gray.copy(alpha = 0.2f)
    }
    
    drawRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size
    )
}

/**
 * Interactive overlay for individual words/elements with Google Lens-style highlighting
 */
@Composable
private fun WordInteractiveOverlay(
    element: com.aks_labs.tulsi.ocr.SelectableTextElement,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val density = LocalDensity.current

    // Animated highlight color for smooth transitions
    val highlightColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color(0xFF1976D2).copy(alpha = 0.3f) // Google-style blue highlight
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "highlight_color"
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
            .background(
                color = highlightColor,
                shape = RoundedCornerShape(2.dp) // Subtle rounded corners for natural look
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            )
    )
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
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
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
                        painter = painterResource(id = R.drawable.copy),
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

/**
 * Text selection overlay that can be added to any image viewer
 * This provides the text selection functionality without replacing the entire viewer
 */
@Composable
fun TextSelectionOverlay(
    ocrResult: SelectableOcrResult,
    containerSize: Size,
    textSelectionState: TextSelectionState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }

    // Transform text blocks to screen coordinates using simplified transformation
    val screenTextBlocks = remember(ocrResult, containerSize) {
        ocrResult.textBlocks.map { block ->
            transformTextBlockToScreen(block, ocrResult.imageSize, containerSize)
        }
    }

    Box(modifier = modifier) {
        // Draw text highlighting and interactive areas
        screenTextBlocks.forEach { textBlock ->
            // Draw individual word overlays for granular selection
            textBlock.getAllElements().forEach { element ->
                WordInteractiveOverlayImproved(
                    element = element,
                    isSelected = element.isSelected,
                    onTap = {
                        textSelectionState.toggleElementSelection(element.id)
                        // Show context menu if text is selected
                        if (textSelectionState.hasSelectedText()) {
                            contextMenuPosition = Offset(
                                element.boundingBox.center.x,
                                element.boundingBox.top - 50f
                            )
                            showContextMenu = true
                        }
                    },
                    onLongPress = {
                        // Select entire sentence on long press
                        selectSentence(textBlock, element, textSelectionState)
                        // Show context menu
                        contextMenuPosition = Offset(
                            element.boundingBox.center.x,
                            element.boundingBox.top - 50f
                        )
                        showContextMenu = true
                    }
                )
            }
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
                }
            )
        }
    }
}

/**
 * Improved word interactive overlay with better highlighting and word boundary detection
 */
@Composable
private fun WordInteractiveOverlayImproved(
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

    // Subtle border for text boundaries - always visible for better UX
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF1976D2).copy(alpha = 0.8f)
            isPressed -> Color(0xFF1976D2).copy(alpha = 0.4f)
            else -> Color.White.copy(alpha = 0.2f) // Very subtle border to show text boundaries
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
                shape = RoundedCornerShape(4.dp) // Rounded for natural Android look
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
 * Uses punctuation and line boundaries to determine sentence scope
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
