package com.aks_labs.tulsi.compose.text_selection

import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.aks_labs.tulsi.ocr.SelectableOcrResult
import com.aks_labs.tulsi.ocr.SelectableTextBlock
import kotlin.math.max
import kotlin.math.min

/**
 * State holder for text selection functionality
 */
class TextSelectionState {
    var ocrResult by mutableStateOf<SelectableOcrResult?>(null)
    var isTextSelectionMode by mutableStateOf(false)
    var isDragSelecting by mutableStateOf(false)
    var dragStartPoint by mutableStateOf(Offset.Zero)
    var dragEndPoint by mutableStateOf(Offset.Zero)
    
    /**
     * Toggle text selection mode
     */
    fun toggleTextSelectionMode() {
        isTextSelectionMode = !isTextSelectionMode
        if (!isTextSelectionMode) {
            clearAllSelections()
        }
    }
    
    /**
     * Update OCR result
     */
    fun updateOcrResult(result: SelectableOcrResult?) {
        ocrResult = result
    }
    
    /**
     * Toggle selection state of a text block
     */
    fun toggleTextBlockSelection(blockId: String) {
        ocrResult?.let { result ->
            val block = result.textBlocks.find { it.id == blockId }
            if (block != null) {
                ocrResult = result.updateBlockSelection(blockId, !block.isSelected)
            }
        }
    }
    
    /**
     * Select text block
     */
    fun selectTextBlock(blockId: String, isSelected: Boolean) {
        ocrResult?.let { result ->
            ocrResult = result.updateBlockSelection(blockId, isSelected)
        }
    }
    
    /**
     * Clear all selections
     */
    fun clearAllSelections() {
        ocrResult?.let { result ->
            ocrResult = result.clearSelections()
        }
    }
    
    /**
     * Get selected text
     */
    fun getSelectedText(): String {
        return ocrResult?.getSelectedText() ?: ""
    }
    
    /**
     * Check if any text is selected
     */
    fun hasSelectedText(): Boolean {
        return ocrResult?.getSelectedBlocks()?.isNotEmpty() == true
    }

    /**
     * Get selected text blocks
     */
    fun getSelectedTextBlocks(): List<SelectableTextBlock> {
        return ocrResult?.getSelectedBlocks() ?: emptyList()
    }

    /**
     * Clear selection (alias for clearAllSelections)
     */
    fun clearSelection() {
        clearAllSelections()
    }

    /**
     * Select all text blocks
     */
    fun selectAllTextBlocks() {
        ocrResult?.let { result ->
            val updatedBlocks = result.textBlocks.map { block ->
                block.copy(isSelected = true)
            }
            ocrResult = result.copy(textBlocks = updatedBlocks)
        }
    }
    
    /**
     * Start drag selection
     */
    fun startDragSelection(startPoint: Offset) {
        isDragSelecting = true
        dragStartPoint = startPoint
        dragEndPoint = startPoint
    }
    
    /**
     * Update drag selection
     */
    fun updateDragSelection(currentPoint: Offset) {
        if (isDragSelecting) {
            dragEndPoint = currentPoint
            updateSelectionInDragArea()
        }
    }
    
    /**
     * End drag selection
     */
    fun endDragSelection() {
        isDragSelecting = false
        dragStartPoint = Offset.Zero
        dragEndPoint = Offset.Zero
    }
    
    /**
     * Update selection based on drag area
     */
    private fun updateSelectionInDragArea() {
        ocrResult?.let { result ->
            val dragRect = createRectFromPoints(dragStartPoint, dragEndPoint)
            
            val updatedBlocks = result.textBlocks.map { block ->
                val blockRect = Rect(
                    offset = Offset(block.boundingBox.left, block.boundingBox.top),
                    size = Size(
                        width = block.boundingBox.right - block.boundingBox.left,
                        height = block.boundingBox.bottom - block.boundingBox.top
                    )
                )
                
                val isInDragArea = rectIntersects(dragRect, blockRect)
                block.copy(isSelected = isInDragArea)
            }
            
            ocrResult = result.copy(textBlocks = updatedBlocks)
        }
    }
    
    /**
     * Create rectangle from two points
     */
    private fun createRectFromPoints(start: Offset, end: Offset): Rect {
        return Rect(
            offset = Offset(min(start.x, end.x), min(start.y, end.y)),
            size = Size(
                width = kotlin.math.abs(end.x - start.x),
                height = kotlin.math.abs(end.y - start.y)
            )
        )
    }
    
    /**
     * Check if two rectangles intersect
     */
    private fun rectIntersects(rect1: Rect, rect2: Rect): Boolean {
        return rect1.left < rect2.right && 
               rect1.right > rect2.left && 
               rect1.top < rect2.bottom && 
               rect1.bottom > rect2.top
    }
}

/**
 * Remember text selection state
 */
@Composable
fun rememberTextSelectionState(): TextSelectionState {
    return remember { TextSelectionState() }
}

/**
 * Gesture handling for text selection
 */
suspend fun PointerInputScope.handleTextSelectionGestures(
    textSelectionState: TextSelectionState,
    imageSize: Size,
    screenSize: Size,
    scale: Float,
    offset: Offset,
    onHapticFeedback: () -> Unit = {}
) {
    if (!textSelectionState.isTextSelectionMode) return
    
    detectTapGestures(
        onTap = { tapPosition ->
            // Convert screen position to image position
            val imagePosition = TextSelectionUtils.transformScreenToImage(
                screenCoordinate = tapPosition,
                imageSize = imageSize,
                screenSize = screenSize,
                scale = scale,
                offset = offset
            )

            // Find text block at tap position in image space
            val tappedBlock = findTextBlockAtPosition(
                position = imagePosition,
                textBlocks = textSelectionState.ocrResult?.textBlocks ?: emptyList()
            )

            tappedBlock?.let { block ->
                textSelectionState.toggleTextBlockSelection(block.id)
                onHapticFeedback()
            }
        }
    )
    
    detectDragGestures(
        onDragStart = { startPosition ->
            textSelectionState.startDragSelection(startPosition)
            onHapticFeedback()
        },
        onDrag = { _, _ ->
            // Drag handling is done in onDragEnd for performance
        },
        onDragEnd = {
            textSelectionState.endDragSelection()
        }
    )
}

/**
 * Find text block at given position (in image coordinates)
 */
private fun findTextBlockAtPosition(
    position: Offset,
    textBlocks: List<SelectableTextBlock>
): SelectableTextBlock? {
    return textBlocks.find { block ->
        val boundingBox = block.boundingBox
        position.x >= boundingBox.left &&
        position.x <= boundingBox.right &&
        position.y >= boundingBox.top &&
        position.y <= boundingBox.bottom
    }
}

/**
 * Text selection utilities
 */
object TextSelectionUtils {

    /**
     * Calculate selection area from drag points
     */
    fun calculateSelectionArea(startPoint: Offset, endPoint: Offset): Rect {
        return Rect(
            offset = Offset(min(startPoint.x, endPoint.x), min(startPoint.y, endPoint.y)),
            size = Size(
                width = kotlin.math.abs(endPoint.x - startPoint.x),
                height = kotlin.math.abs(endPoint.y - startPoint.y)
            )
        )
    }

    /**
     * Check if a text block is within selection area
     */
    fun isTextBlockInSelectionArea(
        textBlock: SelectableTextBlock,
        selectionArea: Rect
    ): Boolean {
        val blockRect = textBlock.boundingBox
        return selectionArea.overlaps(blockRect)
    }

    /**
     * Transform coordinates from image space to screen space
     */
    fun transformImageToScreen(
        imageCoordinate: Offset,
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): Offset {
        // Calculate the scale factor to fit image in screen
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val fitScale = minOf(scaleX, scaleY)

        // Apply fit scale and user zoom scale
        val totalScale = fitScale * scale

        // Calculate centered position
        val scaledImageWidth = imageSize.width * totalScale
        val scaledImageHeight = imageSize.height * totalScale

        val centerOffsetX = (screenSize.width - scaledImageWidth) / 2f
        val centerOffsetY = (screenSize.height - scaledImageHeight) / 2f

        // Transform coordinate
        val screenX = imageCoordinate.x * totalScale + centerOffsetX + offset.x
        val screenY = imageCoordinate.y * totalScale + centerOffsetY + offset.y

        return Offset(screenX, screenY)
    }

    /**
     * Transform coordinates from screen space to image space
     */
    fun transformScreenToImage(
        screenCoordinate: Offset,
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): Offset {
        // Calculate the scale factor to fit image in screen
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val fitScale = minOf(scaleX, scaleY)

        // Apply fit scale and user zoom scale
        val totalScale = fitScale * scale

        // Calculate centered position
        val scaledImageWidth = imageSize.width * totalScale
        val scaledImageHeight = imageSize.height * totalScale

        val centerOffsetX = (screenSize.width - scaledImageWidth) / 2f
        val centerOffsetY = (screenSize.height - scaledImageHeight) / 2f

        // Reverse transform coordinate
        val imageX = (screenCoordinate.x - centerOffsetX - offset.x) / totalScale
        val imageY = (screenCoordinate.y - centerOffsetY - offset.y) / totalScale

        return Offset(imageX, imageY)
    }

    /**
     * Transform a rectangle from image space to screen space
     */
    fun transformRectImageToScreen(
        imageRect: Rect,
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): Rect {
        val topLeft = transformImageToScreen(
            Offset(imageRect.left.toFloat(), imageRect.top.toFloat()),
            imageSize, screenSize, scale, offset
        )
        val bottomRight = transformImageToScreen(
            Offset(imageRect.right.toFloat(), imageRect.bottom.toFloat()),
            imageSize, screenSize, scale, offset
        )

        return Rect(
            offset = topLeft,
            size = Size(
                width = bottomRight.x - topLeft.x,
                height = bottomRight.y - topLeft.y
            )
        )
    }

    /**
     * Check if a point is within image bounds
     */
    fun isPointInImageBounds(
        point: Offset,
        imageSize: Size
    ): Boolean {
        return point.x >= 0 && point.x <= imageSize.width &&
               point.y >= 0 && point.y <= imageSize.height
    }

    /**
     * Clamp coordinates to image bounds
     */
    fun clampToImageBounds(
        point: Offset,
        imageSize: Size
    ): Offset {
        return Offset(
            x = point.x.coerceIn(0f, imageSize.width),
            y = point.y.coerceIn(0f, imageSize.height)
        )
    }

    /**
     * Get text blocks within selection area
     */
    fun getTextBlocksInArea(
        textBlocks: List<SelectableTextBlock>,
        selectionArea: Rect
    ): List<SelectableTextBlock> {
        return textBlocks.filter { block ->
            isTextBlockInSelectionArea(block, selectionArea)
        }
    }
}




