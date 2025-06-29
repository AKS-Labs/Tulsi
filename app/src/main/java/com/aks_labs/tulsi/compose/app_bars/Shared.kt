package com.aks_labs.tulsi.compose.app_bars

import android.content.res.Resources
import android.os.Build
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat

/**
 * Detects if the device is using gesture-based navigation or traditional button navigation
 */
fun isGestureNavigationEnabled(resources: Resources): Boolean {
    return try {
        val resourceId = resources.getIdentifier(
            "config_navBarInteractionMode",
            "integer",
            "android"
        )
        if (resourceId > 0) {
            // 0 = 3-button navigation, 1 = 2-button navigation, 2 = gesture navigation
            val navBarInteractionMode = resources.getInteger(resourceId)
            navBarInteractionMode == 2
        } else {
            // Fallback: assume gesture navigation for Android 10+ if we can't detect
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    } catch (e: Exception) {
        // Fallback: assume gesture navigation for Android 10+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}

/**
 * Creates a floating bottom app bar with consistent styling across the app
 * @param content The content to display inside the floating bottom app bar
 */
@Composable
fun FloatingBottomAppBar(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isGestureNav = remember { isGestureNavigationEnabled(context.resources) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Calculate additional bottom padding for devices with traditional navigation buttons
    val additionalBottomPadding = if (!isGestureNav) {
        // Add extra padding to ensure the floating bottom app bar appears above the navigation bar
        navigationBarPadding.calculateBottomPadding() + 8.dp
    } else {
        16.dp // Default padding for gesture navigation
    }

    // Outer Box container with transparent background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 16.dp,
                bottom = additionalBottomPadding
            )
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Floating bottom bar container
        Box(
            modifier = Modifier
                .height(76.dp)
                .fillMaxWidth(0.95f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(percent = 35),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = RoundedCornerShape(percent = 35)
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** please only use dialogComposable for its intended purpose */
@Composable
fun BottomAppBarItem(
    text: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    buttonWidth: Dp = 64.dp,
    buttonHeight: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    textSize: Float = 14f,
    enabled: Boolean = true,
    showRipple: Boolean = true,
    cornerRadius: Dp = 1000.dp,
    color: Color = Color.Transparent,
    contentColor: Color = if (enabled) MaterialTheme.colorScheme.onBackground else (MaterialTheme.colorScheme.onBackground.copy(
        alpha = 0.6f
    )),
    action: (() -> Unit)? = null,
    dialogComposable: @Composable (() -> Unit)? = null
) {
    val clickModifier = if (action != null && enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = if (!showRipple) null else LocalIndication.current
        ) {
            action()
        }
    } else {
        Modifier
    }

    if (dialogComposable != null) dialogComposable()

    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(buttonHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .then(clickModifier)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier
                .height(iconSize + 8.dp)
                .width(iconSize * 2.25f)
                .clip(RoundedCornerShape(1000.dp))
                .align(Alignment.TopCenter)
                .background(color),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "button",
                tint = contentColor,
                modifier = Modifier
                    .size(iconSize)
            )
        }

        Text(
            text = text,
            fontSize = TextUnit(textSize, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
                .background(Color.Transparent)
        )
    }
}

fun getAppBarContentTransition(slideLeft: Boolean) = run {
    if (slideLeft) {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            ),
            initialAlpha = 0f
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> -width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                ),
                targetAlpha = 0f
            )
        )
    } else {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> -width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            ),
            initialAlpha = 0f
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                ),
                targetAlpha = 0f
            )
        )
    }
}



fun setBarVisibility(
    visible: Boolean,
    window: Window,
    onSetBarVisible: (isVisible: Boolean) -> Unit
) {
    onSetBarVisible(visible)

    window.insetsController?.apply {
        if (visible) {
            show(WindowInsetsCompat.Type.systemBars())
        } else {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // window.setDecorFitsSystemWindows(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualFunctionTopAppBar(
    alternated: Boolean,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    alternateTitle: @Composable () -> Unit,
    alternateActions: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = @Composable {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBarDefaults.topAppBarColors()
    TopAppBar(
        navigationIcon = navigationIcon,
        title = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateTitle()
                } else {
                    title()
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateActions()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        actions()
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

