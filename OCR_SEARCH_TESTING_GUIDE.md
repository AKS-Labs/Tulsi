# OCR Search Testing Guide

## 🚀 Quick Start Testing

### Step 1: Build and Install
```bash
# Build the app with the new OCR search functionality
./gradlew assembleDebug
# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Initial Setup
1. **Launch Tulsi Gallery**
2. **Grant Permissions**: Allow media access when prompted
3. **Navigate to Search Tab**: Should be the default tab
4. **Wait for Initial Load**: Let the app load your media library

### Step 3: Start OCR Processing
1. **Select OCR Search**: Tap "Text in Images" filter chip
2. **Start Processing**: Use "Debug: Force Restart OCR" button to begin processing
3. **Monitor Progress**: Watch the progress bar (if visible)
4. **Check Logs**: Look for OCR processing messages in logcat

## 🧪 Test Scenarios

### Test 1: Basic OCR Search
**Objective**: Verify OCR text extraction and search works

**Steps**:
1. Take a screenshot with clear text (e.g., a webpage, document, or app with text)
2. Wait for OCR processing to complete
3. Go to Search tab
4. Select "Text in Images" filter
5. Type a word that appears in your screenshot
6. Verify the screenshot appears in results

**Expected Result**: Screenshot with matching text should appear in search results

### Test 2: Combined Search
**Objective**: Test metadata + OCR search combination

**Steps**:
1. Search for a term that appears in both:
   - A filename (e.g., "IMG_2024")
   - Text within an image
2. Select "Both" filter
3. Enter search term
4. Verify results include both types

**Expected Result**: Results should include images matching filename AND images with matching text content

### Test 3: Multi-word Search
**Objective**: Test complex search queries

**Steps**:
1. Search for a phrase with multiple words
2. Try partial word matches
3. Test with different word orders

**Expected Result**: Should find images containing any or all of the words

### Test 4: Performance Test
**Objective**: Verify search performance with large libraries

**Steps**:
1. Ensure you have 100+ images
2. Let OCR processing complete
3. Perform various searches
4. Measure response time

**Expected Result**: Search should complete within 1-2 seconds

## 🔍 Debug Tools

### Debug Buttons (Temporary)
Located in Search tab when OCR/Combined filter is selected:

1. **"Debug: Force Restart OCR"**
   - Clears all OCR progress
   - Restarts OCR processing from scratch
   - Use when OCR seems stuck

2. **"Debug: Test Search"**
   - Performs a test search for "test"
   - Prints results to console
   - Shows total OCR texts in database

### Logcat Monitoring
```bash
# Monitor OCR-related logs
adb logcat | grep -E "(OcrManager|OcrIndexingWorker|SimpleOcrService|MLKitTextHelper)"

# Monitor search-related logs
adb logcat | grep -E "(SEARCH_PARAMETERS|SearchViewModel)"

# Monitor database-related logs
adb logcat | grep -E "(Migration|Database)"
```

### Key Log Messages to Look For
- `"Starting batch OCR processing"` - OCR processing started
- `"Successfully processed image"` - Individual image processed
- `"Found X images matching 'query'"` - Search results found
- `"FTS search found X results"` - FTS search working
- `"Fallback search found X results"` - FTS failed, using fallback

## 🐛 Common Issues and Solutions

### Issue: No Search Results
**Symptoms**: OCR search returns empty results

**Possible Causes**:
1. OCR processing not completed
2. Images don't contain recognizable text
3. Search term doesn't match extracted text

**Solutions**:
1. Wait for OCR processing to complete
2. Try searching for simpler, clearer text
3. Use debug buttons to check OCR status
4. Check logcat for processing errors

### Issue: OCR Processing Stuck
**Symptoms**: Progress bar doesn't advance, no new OCR logs

**Solutions**:
1. Use "Debug: Force Restart OCR" button
2. Check device storage space
3. Restart the app
4. Check for ML Kit errors in logs

### Issue: Search is Slow
**Symptoms**: Search takes >5 seconds to return results

**Possible Causes**:
1. FTS table not properly created
2. Large number of images
3. Complex search query

**Solutions**:
1. Check if FTS migration completed successfully
2. Try simpler search terms
3. Monitor database performance in logs

### Issue: App Crashes During Search
**Symptoms**: App crashes when performing OCR search

**Solutions**:
1. Check for database migration errors
2. Verify all dependencies are properly included
3. Check for memory issues with large images
4. Review crash logs for specific errors

## 📊 Performance Benchmarks

### Expected Performance Metrics
- **OCR Processing**: 1-3 seconds per image (depending on size/complexity)
- **Search Response**: <1 second for FTS, <3 seconds for fallback
- **Database Size**: ~1KB per processed image (text storage)
- **Memory Usage**: Should remain stable during processing

### Monitoring Performance
```bash
# Monitor memory usage
adb shell dumpsys meminfo com.aks_labs.tulsi

# Monitor CPU usage
adb shell top | grep tulsi

# Monitor database size
adb shell ls -la /data/data/com.aks_labs.tulsi/databases/
```

## ✅ Success Criteria

### Basic Functionality
- [ ] OCR processing completes without errors
- [ ] Search finds images with matching text
- [ ] Combined search works correctly
- [ ] No app crashes during normal usage

### Performance
- [ ] Search responds within 2 seconds
- [ ] OCR processing doesn't block UI
- [ ] Memory usage remains stable
- [ ] Battery usage is reasonable

### User Experience
- [ ] Search filters work intuitively
- [ ] Progress indication is clear
- [ ] Results are relevant and accurate
- [ ] App remains responsive during processing

## 🎯 Next Steps After Testing

### If Tests Pass
1. Remove debug buttons from production build
2. Consider adding user-facing progress indicators
3. Implement search suggestions
4. Add search history functionality

### If Tests Fail
1. Review logs for specific error messages
2. Check database migration status
3. Verify ML Kit dependencies
4. Test with different image types
5. Consider fallback-only mode if FTS issues persist

## 📞 Support

If you encounter issues during testing:

1. **Collect Logs**: Save relevant logcat output
2. **Document Steps**: Note exact steps that cause issues
3. **Environment Info**: Device model, Android version, app version
4. **Sample Data**: If possible, identify specific images that cause problems

The implementation follows ScreenshotGo's proven approach and should work reliably across different Android devices and image types.
