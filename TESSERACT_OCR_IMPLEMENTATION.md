# Tesseract4Android OCR Implementation Guide

## 📋 Overview
This guide provides step-by-step instructions to replace ML Kit with Tesseract4Android for significant APK size reduction while maintaining OCR functionality.

## 🎯 Benefits
- **Size Reduction**: ~30MB smaller than ML Kit
- **Offline Capability**: Fully offline OCR processing
- **Customizable**: Support for multiple languages and custom training
- **Open Source**: No vendor lock-in

## 📦 Dependencies

### Replace ML Kit Dependencies
```gradle
// Remove these ML Kit dependencies
// implementation("com.google.mlkit:text-recognition:16.0.1")
// implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

// Add Tesseract4Android
implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'

// Keep existing dependencies
implementation("androidx.work:work-runtime-ktx:2.10.0")
implementation("androidx.lifecycle:lifecycle-service:2.8.7")
```

## 🔧 Implementation Steps

### 1. Create TesseractOcrHelper.kt
```kotlin
package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import cz.adaptech.tesseract4android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TesseractOcrHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "TesseractOcrHelper"
        private const val TESSDATA_PATH = "tessdata"
        private const val LANGUAGE = "eng" // English language data
    }
    
    private var tessBaseAPI: TessBaseAPI? = null
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Copy tessdata files to internal storage if needed
            prepareTessData()
            
            tessBaseAPI = TessBaseAPI().apply {
                val dataPath = File(context.filesDir, TESSDATA_PATH).parent
                if (!init(dataPath, LANGUAGE)) {
                    Log.e(TAG, "Failed to initialize Tesseract")
                    return@withContext false
                }
            }
            
            Log.d(TAG, "Tesseract initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Tesseract", e)
            false
        }
    }
    
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            tessBaseAPI?.let { api ->
                api.setImage(bitmap)
                val extractedText = api.utF8Text
                Log.d(TAG, "OCR completed. Extracted ${extractedText.length} characters")
                return@withContext extractedText
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for bitmap", e)
            null
        }
    }
    
    private fun prepareTessData() {
        val tessDataDir = File(context.filesDir, TESSDATA_PATH)
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }
        
        val tessDataFile = File(tessDataDir, "$LANGUAGE.traineddata")
        if (!tessDataFile.exists()) {
            // Copy from assets
            try {
                context.assets.open("tessdata/$LANGUAGE.traineddata").use { input ->
                    FileOutputStream(tessDataFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Tessdata file copied successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy tessdata file", e)
            }
        }
    }
    
    fun cleanup() {
        tessBaseAPI?.end()
        tessBaseAPI = null
    }
}
```

### 2. Update MLKitTextHelper.kt
```kotlin
// Replace ML Kit implementation with Tesseract
class TesseractTextHelper(private val context: Context) {
    
    private val tesseractHelper = TesseractOcrHelper(context)
    
    suspend fun initialize(): Boolean {
        return tesseractHelper.initialize()
    }
    
    suspend fun extractTextFromImage(context: Context, imageUri: Uri): String? {
        return try {
            val bitmap = loadOptimizedBitmap(imageUri) ?: return null
            tesseractHelper.extractTextFromBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text from image", e)
            null
        }
    }
    
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String? {
        return tesseractHelper.extractTextFromBitmap(bitmap)
    }
    
    // Keep existing bitmap loading and optimization methods
    private fun loadOptimizedBitmap(uri: Uri): Bitmap? {
        // Existing implementation
    }
    
    fun cleanup() {
        tesseractHelper.cleanup()
    }
}
```

### 3. Add Tessdata Assets
Create directory structure:
```
app/src/main/assets/
└── tessdata/
    └── eng.traineddata  // Download from GitHub
```

Download link: https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

### 4. Update OcrManager.kt
```kotlin
class OcrManager(
    private val context: Context,
    private val database: MediaDatabase
) {
    // Replace ML Kit helper with Tesseract helper
    private val textHelper = TesseractTextHelper(context)
    
    suspend fun initialize(): Boolean {
        return textHelper.initialize()
    }
    
    // Keep existing methods, update implementation to use textHelper
    // ... rest of the implementation remains the same
}
```

### 5. Update MainActivity.kt
```kotlin
// In initializeOcrSystem()
private fun initializeOcrSystem() {
    Log.d("MainActivity", "Initializing Tesseract OCR system...")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val ocrManager = OcrManager(applicationContext, applicationDatabase)
            
            // Initialize Tesseract
            if (!ocrManager.initialize()) {
                Log.e("MainActivity", "Failed to initialize Tesseract OCR")
                return@launch
            }
            
            // Rest of initialization remains the same
            // ...
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize OCR system", e)
        }
    }
}
```

## 📁 Asset Management

### Tessdata File Size Optimization
- **eng.traineddata**: ~10MB (English only)
- **Alternative**: Use compact models (~3-5MB) for basic text recognition
- **Multi-language**: Add only required languages

### Download Strategy
```kotlin
// Optional: Download tessdata on first run to reduce APK size
class TessdataDownloader(private val context: Context) {
    
    suspend fun downloadTessdata(language: String = "eng"): Boolean {
        // Download from GitHub releases or your server
        // Save to internal storage
        // Return success/failure
    }
}
```

## 🔧 Configuration Options

### Tesseract Configuration
```kotlin
// In TesseractOcrHelper initialization
tessBaseAPI?.apply {
    // Optimize for speed vs accuracy
    setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ")
    
    // Set page segmentation mode
    setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
    
    // Set OCR engine mode
    setOcrEngineMode(TessBaseAPI.OcrEngineMode.OEM_LSTM_ONLY)
}
```

## 📊 Performance Comparison

| Metric | ML Kit | Tesseract4Android |
|--------|--------|-------------------|
| APK Size | +40MB | +8MB |
| Accuracy | 95-98% | 85-92% |
| Speed | Fast | Medium |
| Languages | 100+ | 100+ |
| Offline | ✅ | ✅ |

## 🧪 Testing Checklist

- [ ] OCR accuracy with various image types
- [ ] Processing speed benchmarks
- [ ] Memory usage optimization
- [ ] Error handling and edge cases
- [ ] Multi-language support (if needed)
- [ ] Device compatibility testing

## 🚀 Migration Steps

1. **Backup**: Create branch with current ML Kit implementation
2. **Dependencies**: Update build.gradle.kts
3. **Assets**: Add tessdata files
4. **Code**: Implement Tesseract classes
5. **Integration**: Update existing OCR calls
6. **Testing**: Validate functionality
7. **Optimization**: Fine-tune performance
8. **Release**: Deploy with size reduction

## 📝 Notes

- **Accuracy Trade-off**: Tesseract may have slightly lower accuracy than ML Kit
- **Initialization Time**: First-time setup may take longer due to tessdata loading
- **Memory Usage**: Monitor memory consumption during OCR processing
- **Language Support**: Add only required language packs to minimize size
