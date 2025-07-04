package com.aks_labs.tulsi.compose.text_selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aks_labs.tulsi.ocr.SelectableOcrResult
import com.aks_labs.tulsi.ocr.SelectableTextBlock

/**
 * Overlay composable that displays selectable text regions on top of images
 */
@Composable
fun TextSelectionOverlay(
    ocrResult: SelectableOcrResult?,
    isTextSelectionMode: Boolean,
    imageSize: Size,
    screenSize: Size,
    scale: Float,
    offset: Offset,
    onTextBlockSelected: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ocrResult == null || !isTextSelectionMode) return
    
    val density = LocalDensity.current
    
    // Transform text blocks to screen coordinates
    val screenTextBlocks = remember(ocrResult, imageSize, screenSize, scale, offset) {
        ocrResult.textBlocks.map { block ->
            block.toScreenCoordinates(imageSize, screenSize, scale, offset)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Draw text block overlays
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            screenTextBlocks.forEach { textBlock ->
                drawTextBlockOverlay(
                    textBlock = textBlock,
                    isSelected = textBlock.isSelected
                )
            }
        }
        
        // Interactive text block areas
        screenTextBlocks.forEach { textBlock ->
            TextBlockInteractiveArea(
                textBlock = textBlock,
                imageSize = imageSize,
                screenSize = screenSize,
                scale = scale,
                offset = offset,
                onSelected = { isSelected ->
                    onTextBlockSelected(textBlock.id, isSelected)
                }
            )
        }
    }
}

/**
 * Interactive area for a text block that handles selection
 */
@Composable
private fun TextBlockInteractiveArea(
    textBlock: SelectableTextBlock,
    imageSize: Size,
    screenSize: Size,
    scale: Float,
    offset: Offset,
    onSelected: (Boolean) -> Unit
) {
    val density = LocalDensity.current

    // Transform bounding box from image space to screen space
    val screenBoundingBox = TextSelectionUtils.transformRectImageToScreen(
        imageRect = textBlock.boundingBox,
        imageSize = imageSize,
        screenSize = screenSize,
        scale = scale,
        offset = offset
    )

    // Animate selection state with spring animations
    val selectionAlpha by animateFloatAsState(
        targetValue = if (textBlock.isSelected) 0.3f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_alpha"
    )

    val selectionColor by animateColorAsState(
        targetValue = if (textBlock.isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_color"
    )

    // Scale animation for selection feedback
    val scale by animateFloatAsState(
        targetValue = if (textBlock.isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "selection_scale"
    )

    // Border width animation
    val borderWidth by animateFloatAsState(
        targetValue = if (textBlock.isSelected) 2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "border_width"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = screenBoundingBox.left.toInt(),
                    y = screenBoundingBox.top.toInt()
                )
            }
            .size(
                width = with(density) { screenBoundingBox.width.toDp() },
                height = with(density) { screenBoundingBox.height.toDp() }
            )
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(selectionColor.copy(alpha = selectionAlpha))
            .border(
                width = with(density) { borderWidth.toDp() },
                color = if (textBlock.isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onSelected(!textBlock.isSelected)
            }
    )
}

/**
 * Draw text block overlay on canvas
 */
private fun DrawScope.drawTextBlockOverlay(
    textBlock: SelectableTextBlock,
    isSelected: Boolean
) {
    val boundingBox = textBlock.boundingBox
    val rect = Rect(
        offset = Offset(boundingBox.left.toFloat(), boundingBox.top.toFloat()),
        size = Size(boundingBox.width.toFloat(), boundingBox.height.toFloat())
    )
    
    // Draw selection highlight
    if (isSelected) {
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    cornerRadius = CornerRadius(8f, 8f)
                )
            )
        }
        
        drawPath(
            path = path,
            color = Color.Blue.copy(alpha = 0.2f)
        )
        
        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 4f)
        )
    } else {
        // Draw subtle border for unselected blocks
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    cornerRadius = CornerRadius(4f, 4f)
                )
            )
        }
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.7f),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Preview composable for text selection overlay
 */
@Composable
fun TextSelectionOverlayPreview(
    modifier: Modifier = Modifier
) {
    // This would be used for preview/testing purposes
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.1f))
    ) {
        // Preview content would go here
    }
}
