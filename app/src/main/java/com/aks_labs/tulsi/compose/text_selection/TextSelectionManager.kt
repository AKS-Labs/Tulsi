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
     * Toggle selection state of a text element (word)
     */
    fun toggleElementSelection(elementId: String) {
        ocrResult?.let { result ->
            val updatedBlocks = result.textBlocks.map { block ->
                block.updateElementSelection(elementId, !isElementSelected(elementId))
            }
            ocrResult = result.copy(textBlocks = updatedBlocks)
        }
    }

    /**
     * Select a line by line ID
     */
    fun selectLine(lineId: String) {
        ocrResult?.let { result ->
            val updatedBlocks = result.textBlocks.map { block ->
                val updatedLines = block.lines.map { line ->
                    if (line.id == lineId) {
                        val updatedElements = line.elements.map { element ->
                            element.copy(isSelected = true)
                        }
                        line.copy(elements = updatedElements, isSelected = true)
                    } else {
                        line
                    }
                }
                block.copy(lines = updatedLines)
            }
            ocrResult = result.copy(textBlocks = updatedBlocks)
        }
    }

    /**
     * Check if a specific element is selected
     */
    private fun isElementSelected(elementId: String): Boolean {
        return ocrResult?.textBlocks?.any { block ->
            block.getAllElements().any { element ->
                element.id == elementId && element.isSelected
            }
        } ?: false
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
     * This properly handles ContentScale.Fit with different aspect ratios and letterboxing/pillarboxing
     */
    fun transformImageToScreen(
        imageCoordinate: Offset,
        originalImageSize: Size,
        containerSize: Size,
        scale: Float,
        offset: Offset
    ): Offset {
        android.util.Log.d("TextSelectionDebug", "=== transformImageToScreen START ===")
        android.util.Log.d("TextSelectionDebug", "Input imageCoordinate: $imageCoordinate")
        android.util.Log.d("TextSelectionDebug", "Input originalImageSize: $originalImageSize")
        android.util.Log.d("TextSelectionDebug", "Input containerSize: $containerSize")
        android.util.Log.d("TextSelectionDebug", "Input scale: $scale")
        android.util.Log.d("TextSelectionDebug", "Input offset: $offset")

        // Step 1: Calculate ContentScale.Fit scale factor
        val scaleX = containerSize.width / originalImageSize.width
        val scaleY = containerSize.height / originalImageSize.height
        val fitScale = minOf(scaleX, scaleY)
        android.util.Log.d("TextSelectionDebug", "ContentScale.Fit - scaleX: $scaleX, scaleY: $scaleY, fitScale: $fitScale")

        // Step 2: Transform from original image space to displayed image space
        val displayedX = imageCoordinate.x * fitScale
        val displayedY = imageCoordinate.y * fitScale
        android.util.Log.d("TextSelectionDebug", "Displayed image coordinate: ($displayedX, $displayedY)")

        // Step 3: Calculate displayed image dimensions and centering offsets
        val displayedImageWidth = originalImageSize.width * fitScale
        val displayedImageHeight = originalImageSize.height * fitScale
        val centerOffsetX = (containerSize.width - displayedImageWidth) / 2f
        val centerOffsetY = (containerSize.height - displayedImageHeight) / 2f
        android.util.Log.d("TextSelectionDebug", "Displayed image size: ${displayedImageWidth}x${displayedImageHeight}")
        android.util.Log.d("TextSelectionDebug", "Center offsets: X=$centerOffsetX, Y=$centerOffsetY")

        // Step 4: Apply centering to get base screen coordinates
        val baseScreenX = displayedX + centerOffsetX
        val baseScreenY = displayedY + centerOffsetY
        android.util.Log.d("TextSelectionDebug", "Base screen coordinate: ($baseScreenX, $baseScreenY)")

        // Step 5: Apply user zoom and pan transformations
        // The zoom is applied around the center of the displayed image
        val displayedCenterX = containerSize.width / 2f
        val displayedCenterY = containerSize.height / 2f

        // Apply zoom around center
        val zoomedX = displayedCenterX + (baseScreenX - displayedCenterX) * scale
        val zoomedY = displayedCenterY + (baseScreenY - displayedCenterY) * scale
        android.util.Log.d("TextSelectionDebug", "After zoom: ($zoomedX, $zoomedY)")

        // Apply pan offset (note: HorizontalImageList uses negative offset in graphicsLayer)
        val finalX = zoomedX - offset.x
        val finalY = zoomedY - offset.y

        val result = Offset(finalX, finalY)
        android.util.Log.d("TextSelectionDebug", "Final screen coordinate: $result")
        android.util.Log.d("TextSelectionDebug", "=== transformImageToScreen END ===")

        return result
    }

    /**
     * Transform coordinates from screen space to image space
     * This properly handles ContentScale.Fit with different aspect ratios and letterboxing/pillarboxing
     */
    fun transformScreenToImage(
        screenCoordinate: Offset,
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): Offset {
        // Step 1: Reverse pan offset
        val afterPanX = screenCoordinate.x + offset.x
        val afterPanY = screenCoordinate.y + offset.y

        // Step 2: Reverse zoom transformation
        val screenCenterX = screenSize.width / 2f
        val screenCenterY = screenSize.height / 2f
        val baseScreenX = screenCenterX + (afterPanX - screenCenterX) / scale
        val baseScreenY = screenCenterY + (afterPanY - screenCenterY) / scale

        // Step 3: Calculate ContentScale.Fit parameters
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val fitScale = minOf(scaleX, scaleY)

        // Step 4: Calculate centering offsets
        val displayedImageWidth = imageSize.width * fitScale
        val displayedImageHeight = imageSize.height * fitScale
        val centerOffsetX = (screenSize.width - displayedImageWidth) / 2f
        val centerOffsetY = (screenSize.height - displayedImageHeight) / 2f

        // Step 5: Remove centering to get displayed image coordinates
        val displayedX = baseScreenX - centerOffsetX
        val displayedY = baseScreenY - centerOffsetY

        // Step 6: Transform from displayed image space to original image space
        val imageX = displayedX / fitScale
        val imageY = displayedY / fitScale

        return Offset(imageX, imageY)
    }

    /**
     * Transform a rectangle from image space to screen space
     */
    fun transformRectImageToScreen(
        imageRect: Rect,
        originalImageSize: Size,
        containerSize: Size,
        scale: Float,
        offset: Offset
    ): Rect {
        val topLeft = transformImageToScreen(
            Offset(imageRect.left.toFloat(), imageRect.top.toFloat()),
            originalImageSize, containerSize, scale, offset
        )
        val bottomRight = transformImageToScreen(
            Offset(imageRect.right.toFloat(), imageRect.bottom.toFloat()),
            originalImageSize, containerSize, scale, offset
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




