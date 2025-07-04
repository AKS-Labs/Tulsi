package com.aks_labs.tulsi.compose.text_selection

import android.content.Context
// ActionMode and clipboard imports removed - all actions now in bottom panel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.aks_labs.tulsi.ocr.SelectableOcrResult
import com.aks_labs.tulsi.ocr.SelectableTextElement
import kotlin.math.max
import kotlin.math.min

/**
 * Native Android text selection overlay that provides drag selection with native context menu
 */
@Composable
fun NativeTextSelectionOverlay(
    ocrResult: SelectableOcrResult,
    containerSize: Size,
    modifier: Modifier = Modifier,
    onSelectionChanged: (String) -> Unit = {},
    externalSelectionControl: String? = null // External control for select all/deselect all
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    
    // Selection state
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var selectedElements by remember { mutableStateOf<List<SelectableTextElement>>(emptyList()) }
    // ActionMode removed to prevent unwanted top app bar
    
    // Transform text elements to screen coordinates
    val screenElements = remember(ocrResult, containerSize) {
        transformElementsToScreen(ocrResult, containerSize)
    }
    
    // Handle external selection control (Select All/Deselect All from bottom panel)
    LaunchedEffect(externalSelectionControl) {
        when (externalSelectionControl) {
            "SELECT_ALL" -> {
                selectedElements = screenElements
                selectionStart = null
                selectionEnd = null
            }
            "DESELECT_ALL" -> {
                selectedElements = emptyList()
                selectionStart = null
                selectionEnd = null
            }
        }
    }

    // Update selection when elements change - NO ActionMode to prevent top app bar
    LaunchedEffect(selectedElements) {
        // Sort selected elements by position (top to bottom, left to right) for sequential copying
        val sortedElements = selectedElements.sortedWith { a, b ->
            when {
                // First sort by vertical position (top to bottom)
                kotlin.math.abs(a.boundingBox.top - b.boundingBox.top) > 10f -> {
                    a.boundingBox.top.compareTo(b.boundingBox.top)
                }
                // If roughly same vertical position, sort by horizontal position (left to right)
                else -> a.boundingBox.left.compareTo(b.boundingBox.left)
            }
        }

        // Join with appropriate spacing for readability and multi-script support
        val selectedText = sortedElements.joinToString(" ") { element ->
            element.text
        }.let { text ->
            // Add line breaks between text blocks that are far apart vertically
            if (sortedElements.size > 1) {
                val result = StringBuilder()
                var previousElement: SelectableTextElement? = null

                sortedElements.forEach { element ->
                    if (previousElement != null) {
                        val verticalDistance = element.boundingBox.top - previousElement!!.boundingBox.bottom
                        val horizontalDistance = kotlin.math.abs(element.boundingBox.left - previousElement!!.boundingBox.right)

                        // Handle different script layouts
                        when {
                            // If elements are far apart vertically (likely different lines/paragraphs)
                            verticalDistance > 20f -> result.append("\n")
                            // For RTL scripts, check if we're moving right-to-left
                            isRtlText(element.text) && horizontalDistance > 50f -> result.append(" ")
                            // For complex scripts (Devanagari, etc.), use appropriate spacing
                            isComplexScript(element.text) -> result.append(" ")
                            // Default spacing for Latin and other scripts
                            else -> result.append(" ")
                        }
                    }
                    result.append(element.text)
                    previousElement = element
                }
                result.toString()
            } else {
                text
            }
        }

        onSelectionChanged(selectedText)
        // Note: ActionMode removed to prevent unwanted top app bar
        // All text actions are now handled through the bottom panel only
    }
    
    Box(modifier = modifier) {
        // Selection overlay canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            selectionStart = offset
                            selectionEnd = offset
                            
                            // Clear previous selection
                            selectedElements = emptyList()
                        },
                        onDragEnd = {
                            isDragging = false
                            
                            // Finalize selection
                            val start = selectionStart
                            val end = selectionEnd
                            if (start != null && end != null) {
                                selectedElements = getElementsInSelection(screenElements, start, end)
                            }
                        },
                        onDrag = { _, dragAmount ->
                            selectionEnd = selectionEnd?.plus(dragAmount)
                            
                            // Update selection in real-time during drag
                            val start = selectionStart
                            val end = selectionEnd
                            if (start != null && end != null) {
                                selectedElements = getElementsInSelection(screenElements, start, end)
                            }
                        }
                    )
                }
        ) {
            // Draw subtle highlighting for all selectable text regions
            drawSelectableTextIndicators(screenElements)

            // Draw selection highlighting
            drawSelectionHighlight(selectedElements, isDragging)

            // Draw selection handles
            val start = selectionStart
            val end = selectionEnd
            if (start != null && end != null && selectedElements.isNotEmpty()) {
                drawSelectionHandles(start, end)
            }
        }
    }
}

/**
 * Transform OCR elements to screen coordinates with pixel-perfect accuracy
 */
private fun transformElementsToScreen(
    ocrResult: SelectableOcrResult,
    containerSize: Size
): List<SelectableTextElement> {
    // Calculate ContentScale.Fit scale factor with high precision
    val scaleX = containerSize.width / ocrResult.imageSize.width
    val scaleY = containerSize.height / ocrResult.imageSize.height
    val scale = minOf(scaleX, scaleY)

    // Calculate displayed image dimensions with precise floating point arithmetic
    val displayedWidth = ocrResult.imageSize.width * scale
    val displayedHeight = ocrResult.imageSize.height * scale

    // Calculate centering offsets for letterboxing/pillarboxing
    val offsetX = (containerSize.width - displayedWidth) / 2f
    val offsetY = (containerSize.height - displayedHeight) / 2f

    println("DEBUG: Container size: $containerSize")
    println("DEBUG: Original image size: ${ocrResult.imageSize}")
    println("DEBUG: Scale factor: $scale")
    println("DEBUG: Displayed dimensions: ${displayedWidth}x${displayedHeight}")
    println("DEBUG: Offset: ($offsetX, $offsetY)")

    return ocrResult.textBlocks.flatMap { block ->
        block.getAllElements().map { element ->
            // Apply transformation with pixel-perfect precision
            val transformedBoundingBox = Rect(
                left = element.boundingBox.left * scale + offsetX,
                top = element.boundingBox.top * scale + offsetY,
                right = element.boundingBox.right * scale + offsetX,
                bottom = element.boundingBox.bottom * scale + offsetY
            )

            println("DEBUG: Element '${element.text}' - Original: ${element.boundingBox}, Transformed: $transformedBoundingBox")

            element.copy(boundingBox = transformedBoundingBox)
        }
    }
}

/**
 * Get elements within selection rectangle
 */
private fun getElementsInSelection(
    elements: List<SelectableTextElement>,
    start: Offset,
    end: Offset
): List<SelectableTextElement> {
    val selectionRect = Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y)
    )
    
    return elements.filter { element ->
        element.boundingBox.overlaps(selectionRect)
    }
}

/**
 * Draw subtle indicators for all selectable text regions
 */
private fun DrawScope.drawSelectableTextIndicators(
    allElements: List<SelectableTextElement>
) {
    val indicatorColor = Color(0xFF9E9E9E).copy(alpha = 0.15f) // Light gray, very subtle

    allElements.forEach { element ->
        // Draw very subtle rounded rectangle to indicate selectable text
        drawRoundRect(
            color = indicatorColor,
            topLeft = Offset(element.boundingBox.left, element.boundingBox.top),
            size = Size(element.boundingBox.width, element.boundingBox.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

/**
 * Draw selection highlighting with pixel-perfect accuracy
 */
private fun DrawScope.drawSelectionHighlight(
    selectedElements: List<SelectableTextElement>,
    isDragging: Boolean
) {
    val highlightColor = Color(0xFF1976D2).copy(
        alpha = if (isDragging) 0.4f else 0.5f // Google Lens-style highlighting
    )

    selectedElements.forEach { element ->
        // Draw rounded rectangle for better visual appearance
        drawRoundRect(
            color = highlightColor,
            topLeft = Offset(element.boundingBox.left, element.boundingBox.top),
            size = Size(element.boundingBox.width, element.boundingBox.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Add subtle border for better definition
        drawRoundRect(
            color = Color(0xFF1976D2).copy(alpha = 0.8f),
            topLeft = Offset(element.boundingBox.left, element.boundingBox.top),
            size = Size(element.boundingBox.width, element.boundingBox.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Draw selection handles
 */
private fun DrawScope.drawSelectionHandles(start: Offset, end: Offset) {
    val handleColor = Color(0xFF1976D2)
    val handleRadius = 12.dp.toPx()
    
    // Start handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = start
    )
    
    // End handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = end
    )
}

// All text action helper functions removed - actions now handled through bottom panel only

/**
 * Check if text contains RTL (Right-to-Left) characters
 */
private fun isRtlText(text: String): Boolean {
    val arabicPattern = Regex("[\u0600-\u06FF\u0750-\u077F]+") // Arabic
    val hebrewPattern = Regex("[\u0590-\u05FF]+") // Hebrew
    return arabicPattern.containsMatchIn(text) || hebrewPattern.containsMatchIn(text)
}

/**
 * Check if text contains complex script characters (Devanagari, Tamil, etc.)
 */
private fun isComplexScript(text: String): Boolean {
    val devanagariPattern = Regex("[\u0900-\u097F]+") // Hindi/Devanagari
    val tamilPattern = Regex("[\u0B80-\u0BFF]+") // Tamil
    val bengaliPattern = Regex("[\u0980-\u09FF]+") // Bengali
    val gujaratiPattern = Regex("[\u0A80-\u0AFF]+") // Gujarati
    val kannadaPattern = Regex("[\u0C80-\u0CFF]+") // Kannada
    val malayalamPattern = Regex("[\u0D00-\u0D7F]+") // Malayalam
    val oriyaPattern = Regex("[\u0B00-\u0B7F]+") // Oriya
    val punjabiPattern = Regex("[\u0A00-\u0A7F]+") // Punjabi
    val teluguPattern = Regex("[\u0C00-\u0C7F]+") // Telugu

    return devanagariPattern.containsMatchIn(text) ||
           tamilPattern.containsMatchIn(text) ||
           bengaliPattern.containsMatchIn(text) ||
           gujaratiPattern.containsMatchIn(text) ||
           kannadaPattern.containsMatchIn(text) ||
           malayalamPattern.containsMatchIn(text) ||
           oriyaPattern.containsMatchIn(text) ||
           punjabiPattern.containsMatchIn(text) ||
           teluguPattern.containsMatchIn(text)
}
