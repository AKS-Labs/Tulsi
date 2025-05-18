# Google Lens Integration Guide for Android Apps

This document provides a comprehensive guide for implementing the "double-tap to search with Google Lens" feature in Android applications, based on the implementation in PixelFlow. This feature allows users to quickly search for visual information in screenshots using Google Lens with a simple double-tap gesture.

## Table of Contents

1. [Required Permissions](#1-required-permissions)
2. [FileProvider Configuration](#2-fileprovider-configuration)
3. [Gesture Detection Implementation](#3-gesture-detection-implementation)
4. [Creating Content URIs](#4-creating-content-uris)
5. [Multi-Approach Google Lens Launch Strategy](#5-multi-approach-google-lens-launch-strategy)
6. [Error Handling and Fallbacks](#6-error-handling-and-fallbacks)
7. [Haptic Feedback](#7-haptic-feedback)
8. [Android Version Considerations](#8-android-version-considerations)
9. [Complete Implementation Example](#9-complete-implementation-example)

## 1. Required Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<!-- Storage permissions for accessing images -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" /> <!-- For Android 13+ -->

<!-- For haptic feedback -->
<uses-permission android:name="android.permission.VIBRATE" />
```

## 2. FileProvider Configuration

### Step 1: Add FileProvider to AndroidManifest.xml

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider" />
</provider>
```

### Step 2: Create file_provider.xml

Create a file at `res/xml/file_provider.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path
        name="external_files"
        path="." />
</paths>
```

This configuration allows your app to share files from external storage with other apps, which is necessary for Google Lens integration.

## 3. Gesture Detection Implementation

### View-Based Implementation (Traditional Android Views)

For traditional Android Views, use `GestureDetectorCompat` to detect double-tap gestures:

```kotlin
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat

class MyTouchListener(private val context: Context) : View.OnTouchListener {
    // Gesture detector for double-tap detection
    private val gestureDetector = GestureDetectorCompat(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Launch Google Lens with the current image
                launchGoogleLens()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Handle single tap if needed
                return false
            }
        })

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            gestureDetector.onTouchEvent(it)
        }
        return true
    }
}

// Apply the touch listener to your view
imageView.setOnTouchListener(MyTouchListener(context))
```

### Jetpack Compose Implementation

For Jetpack Compose UI, use the `pointerInput` modifier with `detectTapGestures`:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    // Launch Google Lens with the current image
                    launchGoogleLens(imageFile)
                },
                onTap = {
                    // Handle single tap if needed
                }
            )
        }
) {
    // Your image content here
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageFile)
            .crossfade(true)
            .build(),
        contentDescription = "Image",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize()
    )
}
```

## 4. Creating Content URIs

To share an image with Google Lens, you need to create a content URI using FileProvider:

```kotlin
private fun createImageUri(imageFile: File): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
```

## 5. Multi-Approach Google Lens Launch Strategy

The key to reliable Google Lens integration is implementing multiple approaches with fallbacks:

```kotlin
fun launchGoogleLens(imageFile: File) {
    try {
        if (!imageFile.exists()) {
            // Show error message
            return
        }

        // Create content URI
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        // Try different approaches to launch Google Lens
        var success = false

        // Approach 1: Direct Google Lens launch with ACTION_SEND
        try {
            val lensIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                setPackage("com.google.android.googlequicksearchbox")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(lensIntent)
            provideHapticFeedback()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Lens directly: ${e.message}")
        }

        // Approach 2: Google Gallery with ACTION_SEND
        if (!success) {
            try {
                val GalleryIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    setPackage("com.google.android.apps.Gallery")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(GalleryIntent)
                provideHapticFeedback()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Google Gallery: ${e.message}")
            }
        }

        // Approach 3: Chooser dialog with ACTION_SEND
        if (!success) {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, "Search with Google Lens")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                provideHapticFeedback()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch chooser: ${e.message}")
            }
        }

        // Approach 4: Google app with ACTION_VIEW
        if (!success) {
            try {
                val googleIntent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.google.android.googlequicksearchbox")
                    data = imageUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(googleIntent)
                provideHapticFeedback()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Google app with ACTION_VIEW: ${e.message}")
            }
        }

        // Approach 5: Web browser fallback
        if (!success) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
                provideHapticFeedback()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch web Google Lens: ${e.message}")
            }
        }

        // If all approaches failed
        if (!success) {
            // Show error message
            Toast.makeText(context, "Google Lens not available", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens", e)
        Toast.makeText(context, "Error launching Google Lens", Toast.LENGTH_SHORT).show()
    }
}
```

## 6. Error Handling and Fallbacks

The implementation includes comprehensive error handling:

1. **File Existence Check**: Verifies the image file exists before attempting to share
2. **Multiple Launch Approaches**: Tries different methods to launch Google Lens
3. **Exception Handling**: Catches and logs exceptions for each approach
4. **User Feedback**: Provides toast messages for errors
5. **Web Fallback**: Falls back to the web version of Google Lens if all else fails

## 7. Haptic Feedback

Provide haptic feedback when launching Google Lens for better user experience:

```kotlin
private fun provideHapticFeedback() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error providing haptic feedback", e)
    }
}
```

## 8. Android Version Considerations

The implementation handles different Android versions:

1. **Storage Permissions**: Different permissions for Android 10+ (API 29+) and Android 13+ (API 33+)
2. **Vibration API**: Different approaches for Android 12+ (API 31+), Android 8+ (API 26+), and older versions
3. **FileProvider**: Works across all supported Android versions (API 26+)
4. **Intent Flags**: Proper flags for sharing content across apps

## 9. Complete Implementation Example

Here's a complete implementation class that you can adapt to your application:

```kotlin
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for Google Lens integration
 */
class GoogleLensIntegration(private val context: Context) {

    private val TAG = "GoogleLensIntegration"

    /**
     * Launch Google Lens with the provided image file
     *
     * @param imageFile The image file to search with Google Lens
     */
    fun searchWithGoogleLens(imageFile: File) {
        Log.d(TAG, "Launching Google Lens with image: ${imageFile.absolutePath}")

        try {
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: ${imageFile.absolutePath}")
                showToast("Image file not found")
                return
            }

            // Create a content URI for the image file using FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            Log.d(TAG, "Image URI: $imageUri")

            // Try different approaches to launch Google Lens
            var success = false

            // Approach 1: Use Google Lens directly with ACTION_SEND
            try {
                val lensIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    setPackage("com.google.android.googlequicksearchbox")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(lensIntent)
                vibrateDevice() // Provide haptic feedback
                Log.d(TAG, "Google Lens launched with ACTION_SEND")
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Google Lens with ACTION_SEND: ${e.message}")
            }

            // Approach 2: Use Google Gallery with ACTION_SEND
            if (!success) {
                try {
                    val GalleryIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        setPackage("com.google.android.apps.Gallery")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(GalleryIntent)
                    vibrateDevice() // Provide haptic feedback
                    Log.d(TAG, "Google Gallery launched with ACTION_SEND")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch Google Gallery: ${e.message}")
                }
            }

            // Approach 3: Use a chooser with ACTION_SEND
            if (!success) {
                try {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooser = Intent.createChooser(sendIntent, "Search with Google Lens")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    context.startActivity(chooser)
                    vibrateDevice() // Provide haptic feedback
                    Log.d(TAG, "Chooser launched with ACTION_SEND")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch chooser: ${e.message}")
                }
            }

            // Approach 4: Use Google app with ACTION_VIEW
            if (!success) {
                try {
                    val googleIntent = Intent(Intent.ACTION_VIEW).apply {
                        setPackage("com.google.android.googlequicksearchbox")
                        data = imageUri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(googleIntent)
                    vibrateDevice() // Provide haptic feedback
                    Log.d(TAG, "Google app launched with ACTION_VIEW")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch Google app with ACTION_VIEW: ${e.message}")
                }
            }

            // Approach 5: Use web browser with lens.google.com
            if (!success) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(webIntent)
                    vibrateDevice() // Provide haptic feedback
                    Log.d(TAG, "Web Google Lens launched")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch web Google Lens: ${e.message}")
                }
            }

            // If all approaches failed
            if (!success) {
                showToast("Google Lens not available")
                Log.e(TAG, "Google Lens not available - all approaches failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Google Lens", e)
            showToast("Error launching Google Lens")
        }
    }

    /**
     * Vibrates the device to provide haptic feedback
     */
    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0+
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device", e)
        }
    }

    /**
     * Shows a toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```

### Usage Examples

#### In a Traditional Activity/Fragment:

```kotlin
// Initialize the integration class
val googleLensIntegration = GoogleLensIntegration(context)

// Set up double-tap detection on an ImageView
imageView.setOnTouchListener(object : View.OnTouchListener {
    private val gestureDetector = GestureDetectorCompat(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Get the image file
                val imageFile = File(imagePath)

                // Launch Google Lens
                googleLensIntegration.searchWithGoogleLens(imageFile)
                return true
            }
        })

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let { gestureDetector.onTouchEvent(it) }
        return true
    }
})
```

#### In Jetpack Compose:

```kotlin
// Initialize the integration class
val googleLensIntegration = remember { GoogleLensIntegration(context) }

// Image with double-tap detection
Box(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    // Get the image file
                    val imageFile = File(screenshot.filePath)

                    // Launch Google Lens
                    googleLensIntegration.searchWithGoogleLens(imageFile)
                }
            )
        }
) {
    // Your image content here
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(File(screenshot.filePath))
            .crossfade(true)
            .build(),
        contentDescription = "Image",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize()
    )
}
```

## 10. Conclusion

Implementing the "double-tap to search with Google Lens" feature in your Android application provides users with a powerful and intuitive way to search for visual information. This implementation guide covers all aspects of the feature, from permissions and FileProvider configuration to gesture detection and multi-approach launch strategies.

Key takeaways:

1. **Robust Implementation**: The multi-approach strategy ensures the feature works across different device configurations and installed apps.

2. **User Experience**: Haptic feedback and proper error handling enhance the user experience.

3. **Compatibility**: The implementation works across Android versions from API 26 (Android 8.0) onwards.

4. **Adaptability**: The code can be easily adapted to different UI frameworks (traditional Views or Jetpack Compose).

By following this guide, you can implement a reliable and user-friendly Google Lens integration in your Android applications, allowing users to quickly search for information about screenshots or images with a simple double-tap gesture.