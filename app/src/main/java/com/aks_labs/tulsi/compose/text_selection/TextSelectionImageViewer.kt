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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.app.SearchManager
import android.net.Uri

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
    
    // Selection state
    var selectedText by remember { mutableStateOf("") }
    var showViewAllTextDialog by remember { mutableStateOf(false) }

    // External selection control for Select All/Deselect All
    var selectionControl by remember { mutableStateOf<String?>(null) }

    // Draggable panel state
    var panelPosition by remember { mutableStateOf(Alignment.BottomCenter) }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }

    // Container size for coordinate transformation
    var containerSize by remember { mutableStateOf(Size.Zero) }

    // Reset selection control after triggering
    LaunchedEffect(selectionControl) {
        if (selectionControl != null) {
            kotlinx.coroutines.delay(100)
            selectionControl = null
        }
    }
    
    // Enable edge-to-edge display
    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Respect current theme
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
            
            // Native text selection overlay
            if (ocrResult != null && containerSize != Size.Zero) {
                NativeTextSelectionOverlay(
                    ocrResult = ocrResult,
                    containerSize = containerSize,
                    onSelectionChanged = { text ->
                        selectedText = text
                        println("DEBUG: Selection changed: '$text'")
                    },
                    externalSelectionControl = selectionControl,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Close button (top-right) - theme-aware without 3D effects
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close text selection",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // View All Text Dialog
        if (showViewAllTextDialog && ocrResult != null) {
            ViewAllTextDialog(
                ocrResult = ocrResult,
                onDismiss = { showViewAllTextDialog = false }
            )
        }
        
        // Streamlined bottom panel with essential actions only
        DraggableBottomPanel(
            selectedText = selectedText,
            hasSelection = selectedText.isNotEmpty(),
            position = panelPosition,
            offset = panelOffset,
            onPositionChanged = { newPosition, newOffset ->
                panelPosition = newPosition
                panelOffset = newOffset
            },
            onCopy = {
                if (selectedText.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(selectedText))
                }
            },
            onSelectAll = {
                if (selectedText.isNotEmpty()) {
                    // Deselect all text
                    selectionControl = "DESELECT_ALL"
                } else {
                    // Select all text elements
                    selectionControl = "SELECT_ALL"
                }
            },
            onViewAllText = {
                showViewAllTextDialog = true
            }
        )
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
    // Use the current OCR result from textSelectionState (which contains selection updates)
    val currentOcrResult = textSelectionState.ocrResult ?: ocrResult

    // Transform text blocks to screen coordinates using simplified transformation
    val screenTextBlocks = remember(currentOcrResult, containerSize) {
        println("DEBUG: Recomputing screenTextBlocks - currentOcrResult blocks: ${currentOcrResult.textBlocks.size}")
        currentOcrResult.textBlocks.map { block ->
            transformTextBlockToScreen(block, currentOcrResult.imageSize, containerSize)
        }
    }
    
    Box(modifier = modifier) {
        // Draw individual word overlays for granular selection
        screenTextBlocks.forEach { textBlock ->
            textBlock.getAllElements().forEach { element ->
                println("DEBUG: Rendering element ${element.id} with text '${element.text}' - isSelected: ${element.isSelected}")
                println("DEBUG: Element bounding box: ${element.boundingBox}")

                WordInteractiveOverlayAccurate(
                    element = element,
                    isSelected = element.isSelected, // Use the element's own selection state
                    onTap = {
                        println("DEBUG: Tapping element ${element.id} with text '${element.text}'")
                        println("DEBUG: Before toggle - element.isSelected: ${element.isSelected}")

                        textSelectionState.toggleElementSelection(element.id)

                        println("DEBUG: After toggle, hasSelectedText: ${textSelectionState.hasSelectedText()}")
                        println("DEBUG: Selected text: '${textSelectionState.getSelectedText()}'")

                        // Show context menu if text is selected
                        if (textSelectionState.hasSelectedText()) {
                            println("DEBUG: Showing context menu at position: ${element.boundingBox.center}")
                            onShowContextMenu(
                                Offset(
                                    element.boundingBox.center.x,
                                    element.boundingBox.top - 50f
                                )
                            )
                        }
                    },
                    onLongPress = {
                        println("DEBUG: Long pressing element ${element.id} with text '${element.text}'")
                        // Select entire sentence on long press
                        selectSentence(textBlock, element, textSelectionState)

                        println("DEBUG: After sentence selection, hasSelectedText: ${textSelectionState.hasSelectedText()}")

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

    // High contrast highlight colors for visibility on all backgrounds
    val highlightColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF1976D2).copy(alpha = 0.6f) // Google Lens blue with higher opacity
            isPressed -> Color(0xFF1976D2).copy(alpha = 0.3f) // Pressed state
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "highlight_color"
    )

    // High contrast border for visibility on all image types
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF1976D2) // Solid blue border for selected text
            isPressed -> Color(0xFF1976D2).copy(alpha = 0.7f)
            else -> Color.White.copy(alpha = 0.8f) // High contrast white border for unselected
        },
        animationSpec = tween(durationMillis = 200),
        label = "border_color"
    )

    // Shadow/outline for better visibility on all backgrounds
    val shadowColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.Black.copy(alpha = 0.3f)
            isPressed -> Color.Black.copy(alpha = 0.2f)
            else -> Color.Black.copy(alpha = 0.1f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "shadow_color"
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
            // Add shadow/outline for better visibility
            .drawBehind {
                // Draw shadow for better contrast on all backgrounds
                drawRoundRect(
                    color = shadowColor,
                    topLeft = androidx.compose.ui.geometry.Offset(2f, 2f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
            }
            .background(
                color = highlightColor,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp, // Thicker border for better visibility
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(element.id) {
                detectTapGestures(
                    onPress = {
                        println("DEBUG: Press detected on element ${element.id}")
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        println("DEBUG: Press released on element ${element.id}")
                    },
                    onTap = { offset ->
                        println("DEBUG: Tap detected on element ${element.id} at offset $offset")
                        onTap()
                    },
                    onLongPress = { offset ->
                        println("DEBUG: Long press detected on element ${element.id} at offset $offset")
                        onLongPress()
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

/**
 * Enhanced bottom panel with all text selection functionality
 */
@Composable
private fun EnhancedBottomPanel(
    selectedText: String,
    hasSelection: Boolean,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onViewAllText: () -> Unit,
    onClose: () -> Unit,
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
            // Selected text preview (only show if text is selected)
            if (hasSelection) {
                Text(
                    text = "Selected: ${selectedText.take(100)}${if (selectedText.length > 100) "..." else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Action buttons - two rows for better layout
            Column {
                // First row: Copy and Select All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Copy button (only enabled when text is selected)
                    Button(
                        onClick = onCopy,
                        enabled = hasSelection,
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

                    // Select All button (always enabled)
                    OutlinedButton(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Second row: View All Text and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // View All Text button (always enabled)
                    OutlinedButton(
                        onClick = onViewAllText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View All Text")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close button (always enabled)
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Editable dialog to view and modify all extracted text
 */
@Composable
private fun ViewAllTextDialog(
    ocrResult: SelectableOcrResult,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    // Extract all text from OCR result with better formatting
    val initialText = remember(ocrResult) {
        ocrResult.textBlocks.joinToString("\n\n") { block ->
            block.lines.joinToString("\n") { line ->
                line.elements.joinToString(" ") { it.text }
            }
        }
    }

    // Editable text state
    var editableText by remember { mutableStateOf(initialText) }

    // Calculate responsive dialog height based on text length
    val dialogHeight = remember(editableText) {
        when {
            editableText.length < 200 -> 200.dp
            editableText.length < 500 -> 300.dp
            editableText.length < 1000 -> 400.dp
            else -> 500.dp
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "All Extracted Text",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Text extracted from this image:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dialogHeight),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Editable text field with scrolling
                        OutlinedTextField(
                            value = editableText,
                            onValueChange = { editableText = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.1 // Compact line spacing
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            placeholder = {
                                Text(
                                    text = "Edit extracted text...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(editableText))
                    onDismiss()
                }
            ) {
                Text("Copy All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Streamlined draggable bottom panel with essential text selection actions
 */
@Composable
private fun DraggableBottomPanel(
    selectedText: String,
    hasSelection: Boolean,
    position: Alignment,
    offset: Offset,
    onPositionChanged: (Alignment, Offset) -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onViewAllText: () -> Unit
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Use current offset for positioning
    val currentOffset = if (isDragging) dragOffset else offset

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .align(position)
                .offset {
                    IntOffset(
                        x = currentOffset.x.toInt(),
                        y = currentOffset.y.toInt()
                    )
                }
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            dragOffset = offset
                        },
                        onDragEnd = {
                            isDragging = false
                            // Snap to nearest position (top or bottom)
                            val screenHeight = size.height.toFloat()
                            val newPosition = if (dragOffset.y < screenHeight / 2) {
                                Alignment.TopCenter
                            } else {
                                Alignment.BottomCenter
                            }

                            val snappedOffset = when (newPosition) {
                                Alignment.TopCenter -> Offset(0f, 50f) // Small offset from top
                                else -> Offset.Zero // Bottom position
                            }

                            onPositionChanged(newPosition, snappedOffset)
                        },
                        onDrag = { change, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp) // Increased to 24dp for more pill-shaped design
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Visual drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selected text preview (only show if text is selected)
                if (hasSelection) {
                    Text(
                        text = "Selected: ${selectedText.take(100)}${if (selectedText.length > 100) "..." else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Streamlined single-row layout with essential actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Better spacing
                ) {
                    // Copy button (only enabled when text is selected)
                    Button(
                        onClick = onCopy,
                        enabled = hasSelection,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp) // More comfortable padding
                    ) {
                        Icon(
                            painter = painterResource(id = com.aks_labs.tulsi.R.drawable.copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold // Thick text
                            )
                        )
                    }

                    // Smart Select All/Deselect All toggle
                    OutlinedButton(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (hasSelection) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold // Thick text
                            )
                        )
                    }

                    // View All Text button (always enabled)
                    OutlinedButton(
                        onClick = onViewAllText,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold // Thick text
                            )
                        )
                    }
                }
            }
        }
    }
}
