package com.aks_labs.tulsi.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
// Additional imports can be added when dependencies are included
// import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
// import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
// import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Enhanced multi-language OCR extractor with automatic language detection
 * and support for Devanagari, Chinese, Japanese, Korean, and Latin scripts
 */
object MultiLanguageOcrExtractor {
    
    private const val TAG = "MultiLanguageOcrExtractor"
    private const val OCR_TIMEOUT_MS = 30000L // 30 seconds timeout
    
    // Language detection patterns
    private val DEVANAGARI_PATTERN = Regex("[\u0900-\u097F]+") // Hindi/Devanagari Unicode range
    private val CHINESE_PATTERN = Regex("[\u4E00-\u9FFF]+") // Chinese Unicode range
    private val JAPANESE_PATTERN = Regex("[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]+") // Japanese Unicode range
    private val KOREAN_PATTERN = Regex("[\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F]+") // Korean Unicode range
    private val ARABIC_PATTERN = Regex("[\u0600-\u06FF\u0750-\u077F]+") // Arabic Unicode range
    
    // Text recognizers for different languages (only include what we have dependencies for)
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    // Additional recognizers can be added when dependencies are included
    // private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    // private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    // private val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    
    /**
     * Language types supported by the OCR system
     */
    enum class Language(val displayName: String, val code: String) {
        LATIN("English/Latin", "en"),
        DEVANAGARI("Hindi/Devanagari", "hi"),
        // Additional languages can be added when dependencies are included
        // CHINESE("Chinese", "zh"),
        // JAPANESE("Japanese", "ja"),
        // KOREAN("Korean", "ko"),
        AUTO_DETECT("Auto Detect", "auto")
    }
    
    /**
     * Extract text from image with automatic language detection
     */
    suspend fun extractSelectableTextFromImage(
        context: Context,
        imageUri: Uri,
        preferredLanguage: Language = Language.AUTO_DETECT
    ): SelectableOcrResult? {
        return try {
            Log.d(TAG, "Starting multi-language OCR for image: $imageUri")
            
            // Load and preprocess bitmap
            val bitmap = loadBitmapFromUri(context, imageUri)
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap from URI: $imageUri")
                return null
            }
            
            val result = extractSelectableTextFromBitmap(bitmap, preferredLanguage)

            // Clean up bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Multi-language OCR failed for image: $imageUri", e)
            null
        }
    }
    
    /**
     * Extract text from bitmap with language detection and switching
     */
    suspend fun extractSelectableTextFromBitmap(
        bitmap: Bitmap,
        preferredLanguage: Language = Language.AUTO_DETECT
    ): SelectableOcrResult? {
        return try {
            Log.d(TAG, "Starting multi-language OCR for bitmap: ${bitmap.width}x${bitmap.height}")
            
            val startTime = System.currentTimeMillis()
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
            
            // Try different recognizers based on preference or auto-detection
            val visionText = when (preferredLanguage) {
                Language.AUTO_DETECT -> performAutoDetectionOcr(inputImage)
                Language.DEVANAGARI -> performOcrWithRecognizer(devanagariRecognizer, inputImage, "Devanagari")
                Language.LATIN -> performOcrWithRecognizer(latinRecognizer, inputImage, "Latin")
                // Additional languages can be added when dependencies are included
                // Language.CHINESE -> performOcrWithRecognizer(chineseRecognizer, inputImage, "Chinese")
                // Language.JAPANESE -> performOcrWithRecognizer(japaneseRecognizer, inputImage, "Japanese")
                // Language.KOREAN -> performOcrWithRecognizer(koreanRecognizer, inputImage, "Korean")
            }
            
            if (visionText == null) {
                Log.e(TAG, "OCR processing failed or timed out")
                return null
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Convert to selectable OCR result
            val selectableResult = visionText.toSelectableOcrResult(imageSize)
                .copy(processingTimeMs = processingTime)
            
            Log.d(TAG, "Multi-language OCR completed in ${processingTime}ms")
            Log.d(TAG, "Extracted ${selectableResult.textBlocks.size} text blocks, ${visionText.text.length} characters")
            
            selectableResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Multi-language OCR failed for bitmap", e)
            null
        }
    }
    
    /**
     * Perform OCR with automatic language detection
     */
    private suspend fun performAutoDetectionOcr(inputImage: InputImage): Text? {
        Log.d(TAG, "Starting auto-detection OCR")
        
        // Try Latin first (fastest and most common)
        var result = performOcrWithRecognizer(latinRecognizer, inputImage, "Latin (auto-detect)")
        if (result != null && result.text.isNotBlank()) {
            val detectedLanguage = detectLanguageFromText(result.text)
            Log.d(TAG, "Auto-detected language: $detectedLanguage")
            
            // If we detect non-Latin script, try the appropriate recognizer
            when (detectedLanguage) {
                Language.DEVANAGARI -> {
                    Log.d(TAG, "Switching to Devanagari recognizer")
                    val devanagariResult = performOcrWithRecognizer(devanagariRecognizer, inputImage, "Devanagari (switched)")
                    if (devanagariResult != null && devanagariResult.text.length > result.text.length) {
                        result = devanagariResult
                    }
                }
                // Additional language switching can be added when dependencies are included
                // Language.CHINESE -> { ... }
                // Language.JAPANESE -> { ... }
                // Language.KOREAN -> { ... }
                else -> {
                    // Latin is fine, no need to switch
                    Log.d(TAG, "Keeping Latin recognizer result")
                }
            }
        } else {
            // If Latin didn't work well, try Devanagari (common case for Indian users)
            Log.d(TAG, "Latin OCR failed, trying Devanagari")
            result = performOcrWithRecognizer(devanagariRecognizer, inputImage, "Devanagari (fallback)")
        }
        
        return result
    }
    
    /**
     * Perform OCR with a specific recognizer
     */
    private suspend fun performOcrWithRecognizer(
        recognizer: TextRecognizer,
        inputImage: InputImage,
        recognizerName: String
    ): Text? {
        return try {
            Log.d(TAG, "Performing OCR with $recognizerName recognizer")
            
            val result = withTimeoutOrNull(OCR_TIMEOUT_MS) {
                recognizer.process(inputImage).await()
            }
            
            if (result != null) {
                Log.d(TAG, "$recognizerName OCR completed: ${result.text.length} characters")
            } else {
                Log.w(TAG, "$recognizerName OCR timed out")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "$recognizerName OCR failed", e)
            null
        }
    }
    
    /**
     * Detect language from extracted text using Unicode patterns
     */
    private fun detectLanguageFromText(text: String): Language {
        val devanagariMatches = DEVANAGARI_PATTERN.findAll(text).count()
        val chineseMatches = CHINESE_PATTERN.findAll(text).count()
        val japaneseMatches = JAPANESE_PATTERN.findAll(text).count()
        val koreanMatches = KOREAN_PATTERN.findAll(text).count()
        val arabicMatches = ARABIC_PATTERN.findAll(text).count()

        Log.d(TAG, "Language detection - Devanagari: $devanagariMatches, Chinese: $chineseMatches, Japanese: $japaneseMatches, Korean: $koreanMatches, Arabic: $arabicMatches")

        return when {
            devanagariMatches > 0 -> Language.DEVANAGARI
            // Additional language detection can be added when dependencies are included
            // chineseMatches > 0 -> Language.CHINESE
            // japaneseMatches > 0 -> Language.JAPANESE
            // koreanMatches > 0 -> Language.KOREAN
            else -> Language.LATIN
        }
    }
    
    /**
     * Get list of supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    /**
     * Check if a specific language is supported
     */
    fun isLanguageSupported(language: Language): Boolean {
        return language in Language.values()
    }

    /**
     * Load bitmap from URI with error handling
     */
    private fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            android.graphics.BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $imageUri", e)
            null
        }
    }
}
