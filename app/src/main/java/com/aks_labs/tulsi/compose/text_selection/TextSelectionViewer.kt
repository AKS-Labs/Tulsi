package com.aks_labs.tulsi.compose.text_selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    val density = LocalDensity.current
    
    // State for container size
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Text Selection") },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Copy selected text button
                IconButton(
                    onClick = {
                        val selectedText = textSelectionState.getSelectedText()
                        if (selectedText.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(selectedText))
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.copy),
                        contentDescription = "Copy"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        
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
                    modifier = Modifier.fillMaxSize()
                )
            }
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
            TextBlockInteractiveAreaSimplified(
                textBlock = textBlock,
                isSelected = textBlock.isSelected,
                onToggleSelection = {
                    textSelectionState.toggleTextBlockSelection(textBlock.id)
                }
            )
        }
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
