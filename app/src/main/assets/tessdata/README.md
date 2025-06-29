# Tesseract Language Data

This directory contains Tesseract OCR language data files for the F-Droid build.

## Required Files

For English OCR support, the following file is required:
- `eng.traineddata` - English language model for Tesseract 4.x

## Download Instructions

The language data file can be downloaded from:
https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

## Build Process

During the F-Droid build process, the language data will be automatically downloaded
and included in the APK to ensure the OCR functionality works properly.

## File Size

The English language model is approximately 10MB, which is significantly smaller
than the Google ML Kit dependency (~40MB) used in the Google Play version.
