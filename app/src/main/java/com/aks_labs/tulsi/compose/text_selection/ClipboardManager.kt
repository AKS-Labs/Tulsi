package com.aks_labs.tulsi.compose.text_selection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility class for handling clipboard operations with user feedback
 */
class TextClipboardManager(
    private val context: Context,
    private val snackbarHostState: SnackbarHostState? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    /**
     * Copy text to clipboard with user feedback
     */
    fun copyTextToClipboard(
        text: String,
        label: String = "Selected Text",
        showToast: Boolean = true,
        showSnackbar: Boolean = false
    ) {
        if (text.isBlank()) {
            if (showToast) {
                Toast.makeText(context, "No text selected to copy", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        try {
            val clipData = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clipData)
            
            // Show user feedback
            val message = "Copied ${text.length} characters to clipboard"
            
            when {
                showSnackbar && snackbarHostState != null && coroutineScope != null -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
                showToast -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            val errorMessage = "Failed to copy text to clipboard"
            
            when {
                showSnackbar && snackbarHostState != null && coroutineScope != null -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
                showToast -> {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Copy selected text from text selection state
     */
    fun copySelectedText(
        textSelectionState: TextSelectionState,
        showToast: Boolean = true,
        showSnackbar: Boolean = false
    ) {
        val selectedText = textSelectionState.getSelectedText()
        val selectedBlocksCount = textSelectionState.ocrResult?.getSelectedBlocks()?.size ?: 0
        
        if (selectedText.isBlank()) {
            val message = "No text selected to copy"
            
            when {
                showSnackbar && snackbarHostState != null && coroutineScope != null -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
                showToast -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            return
        }
        
        val label = if (selectedBlocksCount == 1) {
            "Selected Text Block"
        } else {
            "Selected Text ($selectedBlocksCount blocks)"
        }
        
        copyTextToClipboard(
            text = selectedText,
            label = label,
            showToast = showToast,
            showSnackbar = showSnackbar
        )
    }
    
    /**
     * Get text from clipboard
     */
    fun getTextFromClipboard(): String? {
        return try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                clipData.getItemAt(0).text?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if clipboard has text
     */
    fun hasTextInClipboard(): Boolean {
        return try {
            val clipData = clipboardManager.primaryClip
            clipData != null && clipData.itemCount > 0 && 
            clipData.getItemAt(0).text?.isNotBlank() == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear clipboard
     */
    fun clearClipboard() {
        try {
            val clipData = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clipData)
        } catch (e: Exception) {
            // Ignore errors when clearing clipboard
        }
    }
}

/**
 * Composable function to remember clipboard manager
 */
@Composable
fun rememberTextClipboardManager(
    snackbarHostState: SnackbarHostState? = null
): TextClipboardManager {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    return remember(context, snackbarHostState, coroutineScope) {
        TextClipboardManager(
            context = context,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * Extension functions for easier clipboard operations
 */
fun TextSelectionState.copySelectedTextToClipboard(
    clipboardManager: TextClipboardManager,
    showToast: Boolean = true,
    showSnackbar: Boolean = false
) {
    clipboardManager.copySelectedText(
        textSelectionState = this,
        showToast = showToast,
        showSnackbar = showSnackbar
    )
}

/**
 * Utility functions for text formatting
 */
object TextFormattingUtils {
    
    /**
     * Format selected text for clipboard with proper spacing
     */
    fun formatTextForClipboard(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("\\n\\s*\\n"), "\n\n") // Normalize line breaks
    }
    
    /**
     * Get text preview for user feedback
     */
    fun getTextPreview(text: String, maxLength: Int = 50): String {
        val cleanText = formatTextForClipboard(text)
        return if (cleanText.length <= maxLength) {
            cleanText
        } else {
            "${cleanText.take(maxLength)}..."
        }
    }
    
    /**
     * Count words in text
     */
    fun countWords(text: String): Int {
        return text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .size
    }
    
    /**
     * Count lines in text
     */
    fun countLines(text: String): Int {
        return text.split('\n').size
    }
}
