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
     */
    fun toScreenCoordinates(
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextBlock {
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val actualScale = minOf(scaleX, scaleY) * scale
        
        val scaledBoundingBox = Rect(
            offset = Offset(
                boundingBox.left * actualScale + offset.x,
                boundingBox.top * actualScale + offset.y
            ),
            size = Size(
                width = (boundingBox.right - boundingBox.left) * actualScale,
                height = (boundingBox.bottom - boundingBox.top) * actualScale
            )
        )
        
        val scaledLines = lines.map { line ->
            line.toScreenCoordinates(imageSize, screenSize, scale, offset)
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
     */
    fun toScreenCoordinates(
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextLine {
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val actualScale = minOf(scaleX, scaleY) * scale
        
        val scaledBoundingBox = Rect(
            offset = Offset(
                boundingBox.left * actualScale + offset.x,
                boundingBox.top * actualScale + offset.y
            ),
            size = Size(
                width = (boundingBox.right - boundingBox.left) * actualScale,
                height = (boundingBox.bottom - boundingBox.top) * actualScale
            )
        )
        
        val scaledElements = elements.map { element ->
            element.toScreenCoordinates(imageSize, screenSize, scale, offset)
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
     */
    fun toScreenCoordinates(
        imageSize: Size,
        screenSize: Size,
        scale: Float,
        offset: Offset
    ): SelectableTextElement {
        val scaleX = screenSize.width / imageSize.width
        val scaleY = screenSize.height / imageSize.height
        val actualScale = minOf(scaleX, scaleY) * scale
        
        val scaledBoundingBox = Rect(
            offset = Offset(
                boundingBox.left * actualScale + offset.x,
                boundingBox.top * actualScale + offset.y
            ),
            size = Size(
                width = (boundingBox.right - boundingBox.left) * actualScale,
                height = (boundingBox.bottom - boundingBox.top) * actualScale
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
