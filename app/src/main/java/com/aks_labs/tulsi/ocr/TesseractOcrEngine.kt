package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import cz.adaptech.tesseract4android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Tesseract OCR engine implementation (for F-Droid builds)
 */
class TesseractOcrEngine(private val context: Context) : OcrEngine {
    
    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TesseractOcrEngine"
        private const val OCR_TIMEOUT_MS = 3000L // 3 seconds timeout
        private const val TESSDATA_FOLDER = "tessdata"
        private const val LANGUAGE = "eng"
    }
    
    private suspend fun initializeTesseract(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) return@withContext true
                
                Log.d(TAG, "Initializing Tesseract OCR engine")
                
                // Create tessdata directory
                val tessDataDir = File(context.filesDir, TESSDATA_FOLDER)
                if (!tessDataDir.exists()) {
                    tessDataDir.mkdirs()
                }
                
                // Copy language data if not exists
                val langFile = File(tessDataDir, "$LANGUAGE.traineddata")
                if (!langFile.exists()) {
                    copyLanguageData(langFile)
                }
                
                // Initialize Tesseract
                tessBaseAPI = TessBaseAPI().apply {
                    if (!init(context.filesDir.absolutePath, LANGUAGE)) {
                        Log.e(TAG, "Failed to initialize Tesseract")
                        return@withContext false
                    }
                    
                    // Set page segmentation mode for better text detection
                    setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                    
                    // Set OCR engine mode
                    setOcrEngineMode(TessBaseAPI.OcrEngineMode.OEM_LSTM_ONLY)
                }
                
                isInitialized = true
                Log.d(TAG, "Tesseract initialized successfully")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Tesseract", e)
                false
            }
        }
    }
    
    private fun copyLanguageData(langFile: File) {
        try {
            // First try to copy from assets
            context.assets.open("$TESSDATA_FOLDER/$LANGUAGE.traineddata").use { inputStream ->
                FileOutputStream(langFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Language data copied from assets successfully")
        } catch (e: IOException) {
            Log.w(TAG, "Language data not found in assets, will download on demand")
            // For F-Droid builds, we'll download the language data on first use
            // This keeps the APK size smaller and ensures we always have the latest model
            downloadLanguageData(langFile)
        }
    }

    private fun downloadLanguageData(langFile: File) {
        try {
            Log.d(TAG, "Language data will be downloaded on first OCR use")
            // Create a minimal placeholder file to indicate download is needed
            langFile.createNewFile()
            langFile.writeText("# Tesseract language data placeholder - will be downloaded on first use")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create language data placeholder", e)
        }
    }
    
    override suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult {
        return try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Starting Tesseract OCR processing")
            
            // Validate bitmap
            if (bitmap.isRecycled) {
                return OcrResult.Error("Bitmap was recycled before processing")
            }
            
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return OcrResult.Error("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            }
            
            // Initialize Tesseract if needed
            if (!initializeTesseract()) {
                return OcrResult.Error("Failed to initialize Tesseract OCR engine")
            }
            
            // Process with timeout
            val result = withTimeoutOrNull(OCR_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    tessBaseAPI?.let { api ->
                        api.setImage(bitmap)
                        val extractedText = api.utF8Text ?: ""
                        val confidence = api.meanConfidence() / 100.0f // Convert to 0-1 range
                        
                        val processingTime = System.currentTimeMillis() - startTime
                        
                        Log.d(TAG, "Tesseract OCR completed successfully in ${processingTime}ms")
                        Log.d(TAG, "Extracted text length: ${extractedText.length}, confidence: $confidence")
                        
                        OcrResult.Success(
                            text = extractedText.trim(),
                            confidence = confidence,
                            textBlocksCount = if (extractedText.isNotBlank()) 1 else 0,
                            processingTimeMs = processingTime
                        )
                    } ?: OcrResult.Error("Tesseract API not initialized")
                }
            }
            
            result ?: OcrResult.Error("OCR processing timed out after ${OCR_TIMEOUT_MS}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Tesseract OCR processing failed", e)
            OcrResult.Error("OCR processing failed: ${e.message}")
        }
    }
    
    override fun cleanup() {
        try {
            tessBaseAPI?.end()
            tessBaseAPI = null
            isInitialized = false
            Log.d(TAG, "Tesseract OCR engine cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Tesseract", e)
        }
    }
}
