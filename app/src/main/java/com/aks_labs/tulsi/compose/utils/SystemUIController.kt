package com.aks_labs.tulsi.compose.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controller for managing system UI visibility and styling
 */
class SystemUIController(private val activity: ComponentActivity) {
    
    private val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    
    /**
     * Hide the status bar with smooth animation
     */
    fun hideStatusBar() {
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    /**
     * Show the status bar with smooth animation
     */
    fun showStatusBar() {
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    }
    
    /**
     * Set status bar style for immersive mode (when hidden)
     */
    fun setImmersiveStatusBarStyle(isDarkTheme: Boolean, backgroundColor: Int) {
        activity.enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(backgroundColor),
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(backgroundColor)
            } else {
                SystemBarStyle.light(backgroundColor, backgroundColor)
            }
        )
    }
    
    /**
     * Set normal status bar style (when visible)
     */
    fun setNormalStatusBarStyle(isDarkTheme: Boolean, backgroundColor: Int, surfaceContainerColor: Int) {
        activity.enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(surfaceContainerColor),
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(backgroundColor)
            } else {
                SystemBarStyle.light(backgroundColor, backgroundColor)
            }
        )
    }
    
    /**
     * Check if status bar is currently visible
     */
    fun isStatusBarVisible(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.decorView.rootWindowInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        } else {
            // For older versions, check system UI visibility flags
            @Suppress("DEPRECATION")
            (activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
        }
    }
}

/**
 * Composable function to create and remember a SystemUIController
 */
@Composable
fun rememberSystemUIController(): SystemUIController {
    val context = LocalContext.current
    return remember(context) {
        SystemUIController(context as ComponentActivity)
    }
}

/**
 * Composable function to manage status bar visibility based on scroll state
 */
@Composable
fun DynamicStatusBarController(
    isVisible: Boolean,
    systemUIController: SystemUIController = rememberSystemUIController()
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()

    LaunchedEffect(isVisible, isDarkTheme, backgroundColor, surfaceContainerColor) {
        try {
            if (isVisible) {
                // Show status bar and set normal styling
                systemUIController.showStatusBar()
                systemUIController.setNormalStatusBarStyle(isDarkTheme, backgroundColor, surfaceContainerColor)
            } else {
                // Hide status bar and set immersive styling
                systemUIController.hideStatusBar()
                systemUIController.setImmersiveStatusBarStyle(isDarkTheme, backgroundColor)
            }
        } catch (e: Exception) {
            // Gracefully handle any system UI exceptions
            // This can happen on some devices or Android versions
        }
    }
}

/**
 * Enhanced scroll behavior that tracks both app bar and status bar visibility
 */
data class ScrollVisibilityState(
    val isAppBarVisible: Boolean = true,
    val isStatusBarVisible: Boolean = true
) {
    /**
     * Create a new state with updated app bar visibility
     * Status bar follows app bar visibility
     */
    fun withAppBarVisibility(visible: Boolean): ScrollVisibilityState {
        return copy(
            isAppBarVisible = visible,
            isStatusBarVisible = visible
        )
    }
    
    /**
     * Create a new state for immersive mode (both bars hidden)
     */
    fun toImmersiveMode(): ScrollVisibilityState {
        return copy(
            isAppBarVisible = false,
            isStatusBarVisible = false
        )
    }
    
    /**
     * Create a new state for normal mode (both bars visible)
     */
    fun toNormalMode(): ScrollVisibilityState {
        return copy(
            isAppBarVisible = true,
            isStatusBarVisible = true
        )
    }
}

/**
 * Utility function to handle scroll-based visibility changes
 */
fun handleScrollVisibilityChange(
    currentIndex: Int,
    lastScrollIndex: Int,
    onVisibilityChange: (ScrollVisibilityState) -> Unit,
    scrollThreshold: Int = 2
) {
    when {
        // At top - show everything
        currentIndex == 0 -> {
            onVisibilityChange(ScrollVisibilityState().toNormalMode())
        }
        // Scrolling up - show everything
        currentIndex < lastScrollIndex -> {
            onVisibilityChange(ScrollVisibilityState().toNormalMode())
        }
        // Scrolling down significantly - hide everything
        currentIndex > lastScrollIndex + scrollThreshold -> {
            onVisibilityChange(ScrollVisibilityState().toImmersiveMode())
        }
        // No significant change - maintain current state
    }
}
