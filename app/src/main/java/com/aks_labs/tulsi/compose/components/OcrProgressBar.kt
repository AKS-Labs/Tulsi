package com.aks_labs.tulsi.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.database.entities.OcrProgressEntity
import java.util.concurrent.TimeUnit

/**
 * Progress bar component for OCR processing in search screen
 */
@Composable
fun OcrProgressBar(
    progress: OcrProgressEntity?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPauseResume: () -> Unit,
    modifier: Modifier = Modifier,
    showDismissButton: Boolean = true,
    horizontalPadding: Dp = 12.dp,
    title: String? = null
) {
    AnimatedVisibility(
        visible = isVisible && progress != null && !progress.isComplete,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        progress?.let { progressData ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Header row with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title ?: "Getting Search-Ready...   ${progressData.processedImages}/${progressData.totalImages} images",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Row {
                            // Pause/Resume button
                            IconButton(
                                onClick = onPauseResume,
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (progressData.isPaused) {
                                    // Show play icon when paused (to resume)
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume processing",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    // Show pause icon when processing (to pause)
                                    Icon(
                                        painter = painterResource(id = R.drawable.pause),
                                        contentDescription = "Pause processing",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Close button (conditionally shown)
                            if (showDismissButton) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss progress",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(3.dp))

                    // Progress bar - ultra-thin design
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LinearProgressIndicator(
                            progress = if (progressData.totalImages > 0) {
                                progressData.processedImages.toFloat() / progressData.totalImages.toFloat()
                            } else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = if (progressData.isPaused) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    
                    // Status and time information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status text
                        Text(
                            text = when {
                                progressData.isPaused -> "Paused"
                                progressData.isProcessing -> "Processing..."
                                else -> "Waiting..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Percentage and time remaining
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${progressData.progressPercentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            
                            if (!progressData.isPaused && progressData.estimatedTimeRemainingMs > 0) {
                                Text(
                                    text = " • ${formatTimeRemaining(progressData.estimatedTimeRemainingMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format time remaining in a human-readable format
 */
@Composable
private fun formatTimeRemaining(timeMs: Long): String {
    return remember(timeMs) {
        when {
            timeMs < TimeUnit.MINUTES.toMillis(1) -> {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
                "${seconds}s remaining"
            }
            timeMs < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
                "${minutes}m remaining"
            }
            else -> {
                val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
                "${hours}h ${minutes}m remaining"
            }
        }
    }
}
