package com.aks_labs.tulsi.compose.text_selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aks_labs.tulsi.ocr.MultiLanguageOcrExtractor

/**
 * Dialog for selecting OCR language preference
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: MultiLanguageOcrExtractor.Language,
    onLanguageSelected: (MultiLanguageOcrExtractor.Language) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = "Select OCR Language",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Description
                Text(
                    text = "Choose the primary language for text recognition. Auto-detect works well for most images.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // Language options
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(MultiLanguageOcrExtractor.getSupportedLanguages()) { language ->
                        LanguageOption(
                            language = language,
                            isSelected = language == currentLanguage,
                            onSelected = { onLanguageSelected(language) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/**
 * Individual language option in the selection dialog
 */
@Composable
private fun LanguageOption(
    language: MultiLanguageOcrExtractor.Language,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null // Handled by selectable modifier
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            
            if (language == MultiLanguageOcrExtractor.Language.AUTO_DETECT) {
                Text(
                    text = "Automatically detects the best language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Language code: ${language.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact language selector for the text selection interface
 */
@Composable
fun LanguageSelector(
    currentLanguage: MultiLanguageOcrExtractor.Language,
    onLanguageChanged: (MultiLanguageOcrExtractor.Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 21.dp, vertical = 8.dp)
    ) {
        Text(
            text = when (currentLanguage) {
                MultiLanguageOcrExtractor.Language.AUTO_DETECT -> "Auto"
                MultiLanguageOcrExtractor.Language.DEVANAGARI -> "हिं"
                MultiLanguageOcrExtractor.Language.LATIN -> "En"
                // Additional languages can be added when dependencies are included
                // MultiLanguageOcrExtractor.Language.CHINESE -> "中"
                // MultiLanguageOcrExtractor.Language.JAPANESE -> "日"
                // MultiLanguageOcrExtractor.Language.KOREAN -> "한"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
    
    if (showDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                onLanguageChanged(language)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Language detection indicator that shows detected language
 */
@Composable
fun LanguageDetectionIndicator(
    detectedLanguage: MultiLanguageOcrExtractor.Language?,
    modifier: Modifier = Modifier
) {
    if (detectedLanguage != null && detectedLanguage != MultiLanguageOcrExtractor.Language.AUTO_DETECT) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Detected: ${detectedLanguage.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
