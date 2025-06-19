package com.aks_labs.tulsi.helpers

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility object for extracting dominant colors from images
 */
object ColorExtractor {

    /**
     * Extracts the dominant color from a Drawable
     * @param drawable The drawable to extract color from
     * @return The dominant color, or Color.White as fallback
     */
    suspend fun getDominantColor(drawable: Drawable): Color = withContext(Dispatchers.IO) {
        try {
            val bitmap = drawable.toBitmap(width = 150, height = 150, config = Bitmap.Config.RGB_565)
            val palette = Palette.from(bitmap).generate()
            
            // Try different swatch types in order of preference
            val dominantColor = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: Color(0xFF6200EE).toArgb() // Material Purple as fallback instead of white

            Color(dominantColor)
        } catch (e: Exception) {
            // Fallback to white if extraction fails
            Color.White
        }
    }

    /**
     * Extracts the dominant color from a Bitmap
     * @param bitmap The bitmap to extract color from
     * @return The dominant color, or Color.White as fallback
     */
    suspend fun getDominantColor(bitmap: Bitmap): Color = withContext(Dispatchers.IO) {
        try {
            // Scale down bitmap for faster processing
            val scaledBitmap = if (bitmap.width > 150 || bitmap.height > 150) {
                Bitmap.createScaledBitmap(bitmap, 150, 150, false)
            } else {
                bitmap
            }
            
            val palette = Palette.from(scaledBitmap).generate()
            
            // Try different swatch types in order of preference
            val dominantColor = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: Color(0xFF6200EE).toArgb() // Material Purple as fallback instead of white

            Color(dominantColor)
        } catch (e: Exception) {
            // Fallback to white if extraction fails
            Color.White
        }
    }

    /**
     * Creates a gradient background color list from a dominant color
     * @param dominantColor The dominant color to base the gradient on
     * @param alpha The alpha value for the gradient (default 0.8f for center, 0.1f for edges)
     * @return List of colors for gradient background
     */
    fun createGradientColors(dominantColor: Color, alpha: Float = 0.8f): List<Color> {
        return listOf(
            dominantColor.copy(alpha = alpha),
            dominantColor.copy(alpha = alpha * 0.5f),
            dominantColor.copy(alpha = alpha * 0.1f)
        )
    }

    /**
     * Checks if a color is too light and adjusts it for better visibility
     * @param color The color to check
     * @return Adjusted color if needed
     */
    fun adjustColorForVisibility(color: Color): Color {
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        
        return if (luminance > 0.8f) {
            // If color is too light, darken it slightly
            Color(
                red = (color.red * 0.7f).coerceIn(0f, 1f),
                green = (color.green * 0.7f).coerceIn(0f, 1f),
                blue = (color.blue * 0.7f).coerceIn(0f, 1f),
                alpha = color.alpha
            )
        } else {
            color
        }
    }
}
