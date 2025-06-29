package com.aks_labs.tulsi.ocr

import android.graphics.Bitmap
import android.net.Uri

/**
 * Interface for different OCR engine implementations
 */
interface OcrEngine {
    suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult
    fun cleanup()
}

/**
 * Factory for creating OCR engines based on build flavor
 */
object OcrEngineFactory {
    fun createEngine(context: android.content.Context): OcrEngine {
        return try {
            // Try to create ML Kit engine (for gplay flavor)
            Class.forName("com.google.mlkit.vision.text.TextRecognition")
            MLKitOcrEngine()
        } catch (e: ClassNotFoundException) {
            // Fall back to Tesseract engine (for fdroid flavor)
            TesseractOcrEngine(context)
        }
    }
}
