package com.aks_labs.tulsi.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.compose.PreferencesThreeStateSwitchRow
import com.aks_labs.tulsi.datastore.LookAndFeel
import com.aks_labs.tulsi.datastore.PhotoGrid
import com.aks_labs.tulsi.helpers.RowPosition
import kotlin.math.roundToInt

@Composable
fun LookAndFeelSettingsPage() {
	Scaffold (
		topBar = {
			DebuggingSettingsTopBar()
		}
	) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        	item {
        		PreferencesSeparatorText("Theme")
        	}

        	item {
                val followDarkMode by mainViewModel.settings.LookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = 0)

                PreferencesThreeStateSwitchRow(
                    title = "Dark Theme",
                    summary = DarkThemeSetting.entries[followDarkMode].description,
                    iconResID = R.drawable.palette,
                    currentPosition = followDarkMode,
                    trackIcons = listOf(
                        R.drawable.theme_auto,
                        R.drawable.theme_dark,
                        R.drawable.theme_light
                    ),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
					mainViewModel.settings.LookAndFeel.setFollowDarkMode(it)
                }
        	}

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PreferencesSeparatorText("Grid Layout")
            }

            item {
                // Grid Column Count Settings
                val portraitColumns by mainViewModel.settings.PhotoGrid.getGridColumnCountPortrait()
                    .collectAsStateWithLifecycle(initialValue = 3)
                val landscapeColumns by mainViewModel.settings.PhotoGrid.getGridColumnCountLandscape()
                    .collectAsStateWithLifecycle(initialValue = 6)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.grid_view),
                                contentDescription = "Grid columns",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Column Count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Portrait columns slider
                        var portraitSliderValue by remember { mutableFloatStateOf(portraitColumns.toFloat()) }

                        // Update slider value when preference changes
                        LaunchedEffect(portraitColumns) {
                            portraitSliderValue = portraitColumns.toFloat()
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Portrait Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${portraitSliderValue.roundToInt()} columns",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = portraitSliderValue,
                                onValueChange = { portraitSliderValue = it },
                                onValueChangeFinished = {
                                    mainViewModel.settings.PhotoGrid.setGridColumnCountPortrait(portraitSliderValue.roundToInt())
                                },
                                valueRange = 2f..8f,
                                steps = 5, // 2, 3, 4, 5, 6, 7, 8
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Landscape columns slider
                        var landscapeSliderValue by remember { mutableFloatStateOf(landscapeColumns.toFloat()) }

                        // Update slider value when preference changes
                        LaunchedEffect(landscapeColumns) {
                            landscapeSliderValue = landscapeColumns.toFloat()
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Landscape Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${landscapeSliderValue.roundToInt()} columns",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = landscapeSliderValue,
                                onValueChange = { landscapeSliderValue = it },
                                onValueChangeFinished = {
                                    mainViewModel.settings.PhotoGrid.setGridColumnCountLandscape(landscapeSliderValue.roundToInt())
                                },
                                valueRange = 3f..12f,
                                steps = 8, // 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Corner roundness slider
                        val cornerRadius by mainViewModel.settings.PhotoGrid.getGridItemCornerRadius()
                            .collectAsStateWithLifecycle(initialValue = 16)
                        var cornerRadiusSliderValue by remember { mutableFloatStateOf(cornerRadius.toFloat()) }

                        // Update slider value when preference changes
                        LaunchedEffect(cornerRadius) {
                            cornerRadiusSliderValue = cornerRadius.toFloat()
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.style),
                                        contentDescription = "Corner roundness",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Corner Roundness",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cornerRadiusSliderValue.roundToInt()}dp",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            cornerRadiusSliderValue = 16f
                                            mainViewModel.settings.PhotoGrid.setGridItemCornerRadius(16)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.reset),
                                            contentDescription = "Reset to default",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Slider(
                                value = cornerRadiusSliderValue,
                                onValueChange = { cornerRadiusSliderValue = it },
                                onValueChangeFinished = {
                                    mainViewModel.settings.PhotoGrid.setGridItemCornerRadius(cornerRadiusSliderValue.roundToInt())
                                },
                                valueRange = 0f..24f,
                                steps = 23, // 0, 1, 2, ..., 24
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Item padding slider
                        val itemPadding by mainViewModel.settings.PhotoGrid.getGridItemPadding()
                            .collectAsStateWithLifecycle(initialValue = 3)
                        var itemPaddingSliderValue by remember { mutableFloatStateOf(itemPadding.toFloat()) }

                        // Update slider value when preference changes
                        LaunchedEffect(itemPadding) {
                            itemPaddingSliderValue = itemPadding.toFloat()
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.reorderable),
                                        contentDescription = "Item spacing",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Item Spacing",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${itemPaddingSliderValue.roundToInt()}dp",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            itemPaddingSliderValue = 3f
                                            mainViewModel.settings.PhotoGrid.setGridItemPadding(3)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.reset),
                                            contentDescription = "Reset to default",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Slider(
                                value = itemPaddingSliderValue,
                                onValueChange = { itemPaddingSliderValue = it },
                                onValueChangeFinished = {
                                    mainViewModel.settings.PhotoGrid.setGridItemPadding(itemPaddingSliderValue.roundToInt())
                                },
                                valueRange = 1f..8f,
                                steps = 6, // 1, 2, 3, 4, 5, 6, 7, 8
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Drag selection toggle
                        val dragSelectionEnabled by mainViewModel.settings.PhotoGrid.getDragSelectionEnabled()
                            .collectAsStateWithLifecycle(initialValue = false)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Enable Drag Selection",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Allow dragging across grid to select multiple items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = dragSelectionEnabled,
                                onCheckedChange = { enabled ->
                                    mainViewModel.settings.PhotoGrid.setDragSelectionEnabled(enabled)
                                }
                            )
                        }
                    }
                }
            }
        }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebuggingSettingsTopBar() {
	val navController = LocalNavController.current

	TopAppBar(
        title = {
            Text(
                text = "Look & Feel",
                fontSize = TextUnit(22f, TextUnitType.Sp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
					navController.popBackStack()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

enum class DarkThemeSetting(val description: String) {
    FollowSystem("App follows the system theme"),
    ForceDark("App is always in dark theme"),
    ForceLight("App is always in light theme")
}


