# Gallery v1.1.0 Release Notes

## 🎉 Major New Features

### 🔍 Offline AI-Powered Text Search
- **Complete OCR Integration**: Find photos by text content using Google's ML Kit
- **Multi-Language Support**:
  - **Latin Scripts**: English, Spanish, French, German, Italian, Portuguese (enabled by default)
  - **Devanagari Scripts**: Hindi, Marathi, Nepali, Sanskrit (optional toggle in settings)
- **Background Processing**: OCR runs continuously in the background even when app is closed
- **Real-time Progress**: Live progress updates with dismissible notifications for Latin OCR
- **Full-Text Search**: Lightning-fast search through extracted text using SQLite FTS
- **Smart Search Hints**: Creative engaging hints when 'text in images' chip is selected

### ✨ Google Lens-Style Text Selection
- **OCR-Detected Regions**: Text regions automatically detected and made selectable
- **Native Selection Experience**: Word boundary detection and sentence selection on long-press
- **Drag/Tap Selection**: Intuitive selection with semi-transparent blue highlighting
- **Multi-Row Selection**: Intelligent text selection across multiple rows following reading order
- **Context Actions**: Copy, Select All/Deselect, View All Text, and Web Search
- **Draggable Bottom Panel**: Modern pill-shaped design with 28dp-32dp corner radius

### 🎨 Enhanced User Interface
- **Dynamic Status Bar**: Automatically hides during photo browsing and text selection for immersive experience
- **Smooth Shrink Animations**: Bottom navigation bar shrinks during scrolling with FastOutLinearInEasing curves
- **Enhanced Floating App Bar**: Improved animations with Material Design patterns (enterAlwaysCollapsed, exitUntilCollapsed)
- **Minimum Alpha Transparency**: Bottom navigation maintains 0.7-0.8 alpha when shrunk for better visibility

## 🔧 Technical Improvements

### Database Enhancements
- **Dual OCR Systems**: Completely independent Latin and Devanagari OCR with separate database tables
- **Migration Support**: Seamless database migrations (Migration7to8) for FTS setup
- **Optimized Storage**: Efficient text storage with confidence scoring and processing metadata

### Background Processing
- **WorkManager Integration**: Robust background processing that survives force-close
- **Foreground Service**: Persistent OCR processing with proper notification management
- **Progress Tracking**: Real-time progress updates for both OCR systems
- **Smart Resource Management**: Battery-friendly processing with configurable settings

### Performance Optimizations
- **Efficient OCR Pipeline**: Optimized text extraction with timeout handling
- **Memory Management**: Proper bitmap recycling and memory cleanup
- **Threading**: Coroutine-based async processing for smooth UI experience

## 🛠️ Developer Features

### Code Architecture
- **Modular OCR System**: Separate managers for Latin and Devanagari OCR
- **Enhanced Text Selection**: Comprehensive text selection framework with state management
- **System UI Controller**: Centralized status bar and navigation management
- **Multi-Language OCR Extractor**: Unified interface for different OCR engines

### Testing & Quality
- **Comprehensive Error Handling**: Robust error handling for OCR failures and edge cases
- **Logging & Debugging**: Detailed logging for OCR processing and text selection
- **Configuration Options**: Extensive settings for OCR behavior and UI preferences

## 📱 User Experience Improvements

### Search Experience
- **Instant Search**: Real-time search results as you type
- **Search Filters**: Filter by text content with visual indicators
- **Search History**: Smart suggestions based on previous searches
- **Visual Feedback**: Clear indication when OCR is processing or complete

### Text Selection Mode
- **Dual Access Methods**: Available in three-dot menu and as dedicated icon button
- **Immersive Mode**: Status bar automatically hides in text selection mode
- **Native Feel**: Consistent with Android's native text selection patterns
- **Multi-Language Support**: Works seamlessly with both Latin and Devanagari text

### Settings & Configuration
- **OCR Settings**: Toggle Devanagari OCR on/off (Latin enabled by default)
- **Progress Controls**: Dismissible progress for Latin OCR, non-dismissible for Devanagari
- **Language Detection**: Automatic language detection with manual override options

## 🔄 Migration & Compatibility

### Version Migration
- **Semantic Versioning**: Proper minor version bump (1.0.0 → 1.1.0) for new features
- **Database Migration**: Automatic migration from previous versions
- **Settings Preservation**: All user preferences maintained during upgrade

### Platform Support
- **Android 11+**: Optimized for modern Android versions
- **Wide Device Support**: Works across different screen sizes and hardware configurations
- **Accessibility**: Improved accessibility with proper content descriptions and navigation

## 🐛 Bug Fixes & Stability

### UI Fixes
- **Status Bar Consistency**: Fixed status bar behavior across different screens
- **Animation Smoothness**: Resolved animation glitches and improved performance
- **Selection Mode**: Enhanced selection behavior in grid view mode

### OCR Stability
- **Error Recovery**: Improved error handling for OCR processing failures
- **Memory Leaks**: Fixed potential memory leaks in OCR processing
- **Background Processing**: Resolved issues with background OCR continuation

## 📊 Performance Metrics

### OCR Performance
- **Processing Speed**: Optimized OCR processing with configurable timeouts
- **Accuracy**: High-quality text recognition using Google's ML Kit
- **Resource Usage**: Efficient memory and CPU usage during OCR processing

### UI Performance
- **Smooth Scrolling**: 60fps scrolling with optimized animations
- **Fast Search**: Sub-second search results through extracted text
- **Responsive UI**: Immediate feedback for all user interactions

---

**Full Changelog**: https://github.com/AKS-Labs/Tulsi/compare/v1.0.0...v1.1.0
**Download**: Available on GitHub Releases and F-Droid
**Source Code**: https://github.com/AKS-Labs/Tulsi
