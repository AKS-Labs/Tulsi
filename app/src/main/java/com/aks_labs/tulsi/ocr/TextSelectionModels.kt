package com.aks_labs.tulsi.ocr

import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.text.Text
import android.graphics.Rect as AndroidRect

/**
 * Data class representing a selectable text block with its bounding box and content
 */
@Stable
data class SelectableTextBlock(
    val id: String,
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1.0f,
    val isSelected: Boolean = false,
    val lines: List<SelectableTextLine> = emptyList()
) {
    /**
     * Convert to screen coordinates based on image transformation
     * This matches the coordinate system used by HorizontalImageList's graphicsLayer transformation
     */
    fun toScreenCoordinates(
        originalImageSize: Size,
        containerSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextBlock {
        android.util.Log.d("TextBlockDebug", "=== SelectableTextBlock.toScreenCoordinates START ===")
        android.util.Log.d("TextBlockDebug", "Block ID: $id, Text: ${text.take(20)}...")
        android.util.Log.d("TextBlockDebug", "Original boundingBox: $boundingBox")
        android.util.Log.d("TextBlockDebug", "Input originalImageSize: $originalImageSize")
        android.util.Log.d("TextBlockDebug", "Input containerSize: $containerSize")
        android.util.Log.d("TextBlockDebug", "Input scale: $scale")
        android.util.Log.d("TextBlockDebug", "Input offset: $offset")

        // Step 1: Calculate ContentScale.Fit scale factor
        val scaleX = containerSize.width / originalImageSize.width
        val scaleY = containerSize.height / originalImageSize.height
        val fitScale = minOf(scaleX, scaleY)
        android.util.Log.d("TextBlockDebug", "ContentScale.Fit - scaleX: $scaleX, scaleY: $scaleY, fitScale: $fitScale")

        // Step 2: Transform bounding box from original image space to displayed image space
        val displayedLeft = boundingBox.left * fitScale
        val displayedTop = boundingBox.top * fitScale
        val displayedRight = boundingBox.right * fitScale
        val displayedBottom = boundingBox.bottom * fitScale
        android.util.Log.d("TextBlockDebug", "Displayed bounding box: ($displayedLeft, $displayedTop, $displayedRight, $displayedBottom)")

        // Step 3: Calculate centering offsets
        val displayedImageWidth = originalImageSize.width * fitScale
        val displayedImageHeight = originalImageSize.height * fitScale
        val centerOffsetX = (containerSize.width - displayedImageWidth) / 2f
        val centerOffsetY = (containerSize.height - displayedImageHeight) / 2f
        android.util.Log.d("TextBlockDebug", "Center offsets: X=$centerOffsetX, Y=$centerOffsetY")

        // Step 4: Apply centering to get base screen coordinates
        val baseLeft = displayedLeft + centerOffsetX
        val baseTop = displayedTop + centerOffsetY
        val baseRight = displayedRight + centerOffsetX
        val baseBottom = displayedBottom + centerOffsetY
        android.util.Log.d("TextBlockDebug", "Base screen coordinates: ($baseLeft, $baseTop, $baseRight, $baseBottom)")

        // Step 5: Apply user zoom and pan transformations
        val containerCenterX = containerSize.width / 2f
        val containerCenterY = containerSize.height / 2f

        // Apply zoom around center
        val zoomedLeft = containerCenterX + (baseLeft - containerCenterX) * scale
        val zoomedTop = containerCenterY + (baseTop - containerCenterY) * scale
        val zoomedRight = containerCenterX + (baseRight - containerCenterX) * scale
        val zoomedBottom = containerCenterY + (baseBottom - containerCenterY) * scale

        // Apply pan offset
        val finalLeft = zoomedLeft - offset.x
        val finalTop = zoomedTop - offset.y
        val finalRight = zoomedRight - offset.x
        val finalBottom = zoomedBottom - offset.y

        val scaledBoundingBox = Rect(
            offset = Offset(finalLeft, finalTop),
            size = Size(
                width = finalRight - finalLeft,
                height = finalBottom - finalTop
            )
        )

        android.util.Log.d("TextBlockDebug", "Final scaledBoundingBox: $scaledBoundingBox")
        android.util.Log.d("TextBlockDebug", "=== SelectableTextBlock.toScreenCoordinates END ===")

        val scaledLines = lines.map { line ->
            line.toScreenCoordinates(originalImageSize, containerSize, scale, offset)
        }

        return copy(
            boundingBox = scaledBoundingBox,
            lines = scaledLines
        )
    }
}

/**
 * Data class representing a selectable text line within a text block
 */
@Stable
data class SelectableTextLine(
    val id: String,
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1.0f,
    val isSelected: Boolean = false,
    val elements: List<SelectableTextElement> = emptyList()
) {
    /**
     * Convert to screen coordinates based on image transformation
     * This matches the coordinate system used by HorizontalImageList's graphicsLayer transformation
     */
    fun toScreenCoordinates(
        originalImageSize: Size,
        containerSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextLine {
        // Step 1: Calculate ContentScale.Fit scale factor
        val scaleX = containerSize.width / originalImageSize.width
        val scaleY = containerSize.height / originalImageSize.height
        val fitScale = minOf(scaleX, scaleY)

        // Step 2: Transform bounding box from original image space to displayed image space
        val displayedLeft = boundingBox.left * fitScale
        val displayedTop = boundingBox.top * fitScale
        val displayedRight = boundingBox.right * fitScale
        val displayedBottom = boundingBox.bottom * fitScale

        // Step 3: Calculate centering offsets
        val displayedImageWidth = originalImageSize.width * fitScale
        val displayedImageHeight = originalImageSize.height * fitScale
        val centerOffsetX = (containerSize.width - displayedImageWidth) / 2f
        val centerOffsetY = (containerSize.height - displayedImageHeight) / 2f

        // Step 4: Apply centering to get base screen coordinates
        val baseLeft = displayedLeft + centerOffsetX
        val baseTop = displayedTop + centerOffsetY
        val baseRight = displayedRight + centerOffsetX
        val baseBottom = displayedBottom + centerOffsetY

        // Step 5: Apply user zoom and pan transformations
        val containerCenterX = containerSize.width / 2f
        val containerCenterY = containerSize.height / 2f

        // Apply zoom around center
        val zoomedLeft = containerCenterX + (baseLeft - containerCenterX) * scale
        val zoomedTop = containerCenterY + (baseTop - containerCenterY) * scale
        val zoomedRight = containerCenterX + (baseRight - containerCenterX) * scale
        val zoomedBottom = containerCenterY + (baseBottom - containerCenterY) * scale

        // Apply pan offset
        val finalLeft = zoomedLeft - offset.x
        val finalTop = zoomedTop - offset.y
        val finalRight = zoomedRight - offset.x
        val finalBottom = zoomedBottom - offset.y

        val scaledBoundingBox = Rect(
            offset = Offset(finalLeft, finalTop),
            size = Size(
                width = finalRight - finalLeft,
                height = finalBottom - finalTop
            )
        )

        val scaledElements = elements.map { element ->
            element.toScreenCoordinates(originalImageSize, containerSize, scale, offset)
        }

        return copy(
            boundingBox = scaledBoundingBox,
            elements = scaledElements
        )
    }
}

/**
 * Data class representing a selectable text element (word) within a text line
 */
@Stable
data class SelectableTextElement(
    val id: String,
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1.0f,
    val isSelected: Boolean = false
) {
    /**
     * Convert to screen coordinates based on image transformation
     * This matches the coordinate system used by HorizontalImageList's graphicsLayer transformation
     */
    fun toScreenCoordinates(
        originalImageSize: Size,
        containerSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextElement {
        // Step 1: Calculate ContentScale.Fit scale factor
        val scaleX = containerSize.width / originalImageSize.width
        val scaleY = containerSize.height / originalImageSize.height
        val fitScale = minOf(scaleX, scaleY)

        // Step 2: Transform bounding box from original image space to displayed image space
        val displayedLeft = boundingBox.left * fitScale
        val displayedTop = boundingBox.top * fitScale
        val displayedRight = boundingBox.right * fitScale
        val displayedBottom = boundingBox.bottom * fitScale

        // Step 3: Calculate centering offsets
        val displayedImageWidth = originalImageSize.width * fitScale
        val displayedImageHeight = originalImageSize.height * fitScale
        val centerOffsetX = (containerSize.width - displayedImageWidth) / 2f
        val centerOffsetY = (containerSize.height - displayedImageHeight) / 2f

        // Step 4: Apply centering to get base screen coordinates
        val baseLeft = displayedLeft + centerOffsetX
        val baseTop = displayedTop + centerOffsetY
        val baseRight = displayedRight + centerOffsetX
        val baseBottom = displayedBottom + centerOffsetY

        // Step 5: Apply user zoom and pan transformations
        val containerCenterX = containerSize.width / 2f
        val containerCenterY = containerSize.height / 2f

        // Apply zoom around center
        val zoomedLeft = containerCenterX + (baseLeft - containerCenterX) * scale
        val zoomedTop = containerCenterY + (baseTop - containerCenterY) * scale
        val zoomedRight = containerCenterX + (baseRight - containerCenterX) * scale
        val zoomedBottom = containerCenterY + (baseBottom - containerCenterY) * scale

        // Apply pan offset
        val finalLeft = zoomedLeft - offset.x
        val finalTop = zoomedTop - offset.y
        val finalRight = zoomedRight - offset.x
        val finalBottom = zoomedBottom - offset.y

        val scaledBoundingBox = Rect(
            offset = Offset(finalLeft, finalTop),
            size = Size(
                width = finalRight - finalLeft,
                height = finalBottom - finalTop
            )
        )

        return copy(boundingBox = scaledBoundingBox)
    }
}

/**
 * Data class representing the complete OCR result with selectable text blocks
 */
@Stable
data class SelectableOcrResult(
    val textBlocks: List<SelectableTextBlock>,
    val fullText: String,
    val imageSize: Size,
    val processingTimeMs: Long = 0L
) {
    /**
     * Get all selected text concatenated
     */
    fun getSelectedText(): String {
        return textBlocks
            .filter { it.isSelected }
            .joinToString("\n") { it.text }
    }
    
    /**
     * Get selected text blocks
     */
    fun getSelectedBlocks(): List<SelectableTextBlock> {
        return textBlocks.filter { it.isSelected }
    }
    
    /**
     * Update selection state of a text block
     */
    fun updateBlockSelection(blockId: String, isSelected: Boolean): SelectableOcrResult {
        val updatedBlocks = textBlocks.map { block ->
            if (block.id == blockId) {
                block.copy(isSelected = isSelected)
            } else {
                block
            }
        }
        return copy(textBlocks = updatedBlocks)
    }
    
    /**
     * Clear all selections
     */
    fun clearSelections(): SelectableOcrResult {
        val clearedBlocks = textBlocks.map { block ->
            block.copy(
                isSelected = false,
                lines = block.lines.map { line ->
                    line.copy(
                        isSelected = false,
                        elements = line.elements.map { element ->
                            element.copy(isSelected = false)
                        }
                    )
                }
            )
        }
        return copy(textBlocks = clearedBlocks)
    }
}

/**
 * Convert Android Rect to Compose Rect
 */
private fun AndroidRect?.toComposeRect(): Rect {
    return if (this != null) {
        Rect(
            offset = Offset(left.toFloat(), top.toFloat()),
            size = Size(
                width = (right - left).toFloat(),
                height = (bottom - top).toFloat()
            )
        )
    } else {
        Rect.Zero
    }
}

/**
 * Extension functions to convert ML Kit Text objects to selectable models
 */
fun Text.toSelectableOcrResult(imageSize: Size): SelectableOcrResult {
    val selectableBlocks = textBlocks.mapIndexed { blockIndex, textBlock ->
        textBlock.toSelectableTextBlock("block_$blockIndex")
    }

    return SelectableOcrResult(
        textBlocks = selectableBlocks,
        fullText = text,
        imageSize = imageSize
    )
}

fun Text.TextBlock.toSelectableTextBlock(id: String): SelectableTextBlock {
    val selectableLines = lines.mapIndexed { lineIndex, line ->
        line.toSelectableTextLine("${id}_line_$lineIndex")
    }

    return SelectableTextBlock(
        id = id,
        text = text,
        boundingBox = boundingBox.toComposeRect(),
        lines = selectableLines
    )
}

fun Text.Line.toSelectableTextLine(id: String): SelectableTextLine {
    val selectableElements = elements.mapIndexed { elementIndex, element ->
        element.toSelectableTextElement("${id}_element_$elementIndex")
    }

    return SelectableTextLine(
        id = id,
        text = text,
        boundingBox = boundingBox.toComposeRect(),
        elements = selectableElements
    )
}

fun Text.Element.toSelectableTextElement(id: String): SelectableTextElement {
    return SelectableTextElement(
        id = id,
        text = text,
        boundingBox = boundingBox.toComposeRect()
    )
}
