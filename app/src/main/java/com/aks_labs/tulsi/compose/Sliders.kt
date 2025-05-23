package com.aks_labs.tulsi.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.helpers.getColorFromLinearGradientList
import com.aks_labs.tulsi.helpers.gradientColorList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.ColorRangeSlider(
    sliderValue: MutableFloatState
) {
    val localDensity = LocalDensity.current
    val max = maxWidth
    val clearAreaWidth = max / 11f - 4.dp
    val thumbRadius = 16.dp

    val resolvedColor by remember {
        derivedStateOf {
            getColorFromLinearGradientList(
                value = (sliderValue.floatValue * 0.5f + 0.5f).coerceIn(0f, gradientColorList.size - 1f),
                colorList = gradientColorList
            )
        }
    }

    val sliderAnimatedValue by animateFloatAsState(
    	targetValue = sliderValue.floatValue,
    	animationSpec = tween(
    		durationMillis = if (sliderValue.floatValue == -1.2f) 200 else 0
    	),
    	label = "animate editing adjustment color tint value change"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(18.dp)
            .padding(16.dp, 0.dp)
            .clip(CircleShape)
            .align(Alignment.Center)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(clearAreaWidth)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.secondary)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.trash),
                contentDescription = "Clear current color tint",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .size(clearAreaWidth - 4.dp)
                    .align(Alignment.Center)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = gradientColorList
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
        ) {
            val sliderLeft = sliderValue.floatValue * 0.5f + 0.5f
            val sliderRight = 1f - sliderLeft

            val trackWidth = maxWidth - thumbRadius
            val spaceBetween = 6.dp

            Box(
                modifier = Modifier
                    .width(trackWidth * sliderLeft - spaceBetween)
                    .height(18.dp)
                    .clip(RoundedCornerShape(0.dp, 2.dp, 2.dp, 0.dp))
                    .background(Color.White)
                    .align(Alignment.CenterStart)
            )

            Box(
                modifier = Modifier
                    .width(trackWidth * sliderRight - spaceBetween)
                    .height(18.dp)
                    .clip(
                        if (sliderValue.floatValue != -1.2f) {
                            RoundedCornerShape(2.dp, 0.dp, 0.dp, 2.dp)
                        } else {
                            RoundedCornerShape(0.dp)
                        }
                    )
                    .background(Color.White)
                    .align(Alignment.CenterEnd)
            )
        }

    }

    Row(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp, 0.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Slider(
            value = sliderAnimatedValue,
            valueRange = -1.2f..1f,
            onValueChange = {
                sliderValue.floatValue =
                    if (it <= -1.1f) {
                        -1.2f
                    } else {
                        it.coerceAtLeast(-1f)
                    }
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(18.dp)
                )
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                with(localDensity) {
                                    if (sliderValue.floatValue == -1.2f) {
                                        (thumbRadius / 2)
                                            .toPx()
                                            .toInt()
                                    } else {
                                        0
                                    }
                                },
                                0
                            )
                        }
                        .size(thumbRadius)
                        .clip(CircleShape)
                        .background(resolvedColor)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            },
            modifier = Modifier
                .fillMaxWidth(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.PopupPillSlider(
    sliderValue: MutableFloatState,
    changesSize: MutableIntState
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isDraggingSlider by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource.interactions) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    isDraggingSlider = true
                    changesSize.intValue += 1
                }

                is DragInteraction.Cancel -> {
                    isDraggingSlider = false
                }

                is DragInteraction.Stop -> {
                    isDraggingSlider = false
                }

                else -> {}
            }
        }
    }

    val animatedPillHeight by animateDpAsState(
        targetValue = if (isDraggingSlider) 32.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill height"
    )

    val animatedPillHeightOffset by animateDpAsState(
        targetValue = if (isDraggingSlider) 0.dp else 32.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill height"
    )

    val animatedPillWidth by animateDpAsState(
        targetValue = if (isDraggingSlider) 48.dp else 4.dp,
        animationSpec = tween(
            durationMillis = 150,
            delayMillis = if (!isDraggingSlider) 0 else 100
        ),
        label = "Animate editing view bottom bar slider pill width"
    )

    val localDensity = LocalDensity.current
    val multiplier = sliderValue.floatValue * 0.5f + 0.5f
    val neededOffset = with(localDensity) {
        val position = multiplier * maxWidth.toPx() - (24.dp.toPx() * sliderValue.floatValue) - (animatedPillWidth / 2).toPx() // offset by the opposite of the movement so the pill stays in the same place, then subtract half the width to center it
        position.coerceIn(16.dp.toPx(), (maxWidth - animatedPillWidth - 16.dp).toPx()) // -width - 16.dp because width of pill + padding
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    neededOffset.toInt(),
                    with(localDensity) {
                        ((-24).dp + animatedPillHeightOffset)
                            .toPx()
                            .toInt()
                    })
            }
            .height(animatedPillHeight)
            .width(animatedPillWidth)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = (sliderValue.floatValue * 100).toInt().toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center)
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp, 0.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Slider(
            value = sliderValue.floatValue * 100f,
            onValueChange = {
                sliderValue.floatValue = it / 100f
            },
            valueRange = -100f..100f,
            steps = 199,
            thumb = {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.Center)
                    )
                }
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    drawTick = { _, _ -> },
                    drawStopIndicator = {}
                )
            },
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth(1f)
        )
    }
}


