<p align="center">
  <img src="assets/images/tulsi.png" alt="Tulsi App Icon" width="180"/>
</p>

<h1 align="center">📸 Tulsi Gallery</h1>

<img src="assets/images/tulsi.png" alt="Tulsi App Icon" width="120" align="right"/>

> *Because your Gallery deserve a gallery app with personality!*

Tulsi is a fork of [LavenderGallery](https://github.com/kaii-lb/LavenderPhotos) that dares to be different. While LavenderGallery is already a fantastic gallery app, Tulsi adds that extra bit of flair and functionality that makes your photo browsing experience just *chef's kiss* 👌

## 🌟 Why Tulsi Exists

Let's be honest—we spend hours scrolling through our Gallery, so why not make the experience as delightful as possible? Tulsi was born because:

1. **I'm picky about UI details** - Those rounded corners and floating elements aren't just pretty, they make the app feel more premium
2. **Google Lens should be one tap away** - Why switch apps when you want to know "what's that building?" or "what kind of dog is this?"
3. **Navigation should match how humans think** - Search first, then Gallery, Albums, and Secure stuff tucked away at the end
4. **The devil is in the details** - Subtle animations, proper elevations, and thoughtful spacing make all the difference

All credit for the original codebase goes to the amazing LavenderGallery project. Tulsi just adds a bit of spice to that already delicious recipe! 🌶️

## Features

### Core Features
- **Smart Photo Browsing** - Browse all your photos and videos smoothly, separated by date
- **Flexible Album Management** - Add and remove albums as you wish, no arbitrary or forced selections
- **Powerful Search** - Search by image name, date (in many formats), or text content within photos using OCR
- **Intelligent Trash Management** - Trash Bin that's sorted by recently trashed items
- **Advanced Favoriting** - Full-fledged favoriting system with easy organization
- **Intuitive Selection** - A selection system that actually works properly
- **Built-in Photo Editor** - Edit and personalize any photo, any time, without an internet connection (even works in landscape mode!)
- **Secure Storage** - Keep sensitive photos in an encrypted medium for safe keeping
- **Detailed Photo Information** - Find all the relevant information for a photo from one button click
- **Easy Organization** - Copy and move photos to albums with simple gestures
- **Clean Design** - Beautiful UI with smooth UX and thoughtful animations
- **Privacy-First** - No data collection, all processing happens on your device

### ✨ Tulsi's Special Sauce

What makes Tulsi stand out from the original LavenderGallery? Here's the secret recipe:

#### 🔍 Google Lens Superpowers
* **One-tap visual search** - See a cool landmark? Weird plant? Mysterious gadget? Tap the Lens icon and get answers instantly
* **Multi-layered implementation** - Works reliably across different Android versions and device configurations
* **Fallback mechanisms** - Even if your device configuration is unusual, we've got you covered with 5 different ways to make Lens work

#### 🎨 UI That Sparks Joy
* **Floating bottom app bar** - With perfectly rounded corners (35% radius), proper elevation, and buttery-smooth animations
* **Thoughtful spacing** - Customized horizontal padding gives your Gallery room to breathe
* **Transparent backgrounds** - In single photo view, with carefully calibrated dimensions (0.95f width, 76.dp height)

#### 🧭 Navigation That Makes Sense
* **Reordered tabs** - Search, Gallery, Albums, Secure - in that order, because that's how most people use a gallery app
* **Consistent three-column grid** - With an optional toggle to switch to date-grouped view
* **Memory for your preferences** - The app remembers your preferred view mode between sessions

#### 🔄 Selection That Actually Works
* **Fixed selection issues** - Glide selection and drag selection work correctly in all view modes
* **Intuitive controls** - Select multiple Gallery with natural gestures

#### 🔍 Smart OCR Text Search
* **AI-powered text recognition** - Find photos by the text content within them using Google's ML Kit
* **Offline functionality** - All OCR processing happens on your device, no internet required
* **Real-time search** - Search through extracted text from all your photos instantly
* **Background processing** - OCR runs automatically in the background with progress indicators
* **Interactive text selection** - Copy, share, or search extracted text with intuitive gestures
* **Multi-language support** - Recognizes text in 100+ languages automatically

## 🔍 Google Lens Integration: The Details

Tulsi's Google Lens integration is like having a visual search superpower built right into your gallery app. The implementation is robust with multiple fallback options:

1. **Primary method**: Direct integration with Google app using ACTION_SEND intent
2. **Fallback #1**: Integration with Google Gallery app
3. **Fallback #2**: Generic chooser dialog for maximum compatibility
4. **Fallback #3**: Direct ACTION_VIEW with Google app
5. **Last resort**: Web fallback to lens.google.com

This multi-layered approach ensures that no matter what device or Android version you're using, you'll always be able to identify that strange bug you photographed or find out what mountain that is in the background of your vacation photo.

## 🧠 OCR Text Search: Your Photos, Searchable

Tulsi Gallery's OCR (Optical Character Recognition) feature transforms your photo collection into a searchable database. Using Google's advanced ML Kit technology, every photo with text becomes instantly findable.

### How It Works
1. **Automatic Processing** - When you open the app, OCR quietly processes your photos in the background
2. **Smart Recognition** - Detects text in photos, screenshots, documents, signs, and more
3. **Instant Search** - Type any word and find photos containing that text immediately
4. **Privacy First** - All processing happens on your device; no data leaves your phone

### Perfect For
* **Screenshots** - Find that important screenshot by searching for text within it
* **Documents** - Locate photos of receipts, business cards, or handwritten notes
* **Signs & Menus** - Search for restaurant names, street signs, or any text you've photographed
* **Memes & Social Media** - Find that funny meme by searching for the text in it
* **Study Materials** - Quickly locate photos of whiteboards, textbooks, or handwritten notes

### Technical Excellence
* **Offline Operation** - Works without internet connection
* **Real-time Progress** - See processing status with detailed progress indicators
* **Optimized Performance** - Efficient background processing that doesn't slow down your device
* **Multi-language Support** - Recognizes text in over 100 languages automatically
* **Interactive Results** - Copy, share, or search extracted text with simple gestures

## 📥 Download

Get Tulsi Gallery from your preferred app store:

<p align="center">
  <a href="https://f-droid.org/packages/com.aks_labs.tulsi/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
         alt="Get it on F-Droid"
         height="80">
  </a>
  <a href="https://play.google.com/store/apps/details?id=com.aks_labs.tulsi">
    <img alt='Get it on Google Play'
         src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'
         height="80" />
  </a>
  <a href="https://github.com/AKS-Labs/Tulsi/releases/latest">
    <img alt='Get it on GitHub'
         src='https://img.shields.io/badge/GitHub-Download%20APK-181717?style=for-the-badge&logo=github&logoColor=white'
         height="80" />
  </a>
</p>

### 🏪 Store Availability
- **F-Droid**: Open-source app repository with reproducible builds
- **Google Play Store**: Official Android app store with automatic updates
- **GitHub Releases**: Direct APK downloads with release notes and changelogs

### 📋 System Requirements
- **Android Version**: 7.0 (API level 24) or higher
- **Storage**: ~30MB for app installation (optimized for minimal size)
- **RAM**: 2GB recommended for optimal performance
- **Architecture**: ARM devices (armeabi-v7a, arm64-v8a)
- **Permissions**: Storage access for photo management, camera access for photo capture

### ⚡ Performance & Optimization
- **Lightweight Design** - Heavily optimized APK size without compromising functionality
- **Efficient OCR** - Smart background processing that doesn't drain your battery
- **Memory Optimized** - Careful resource management for smooth performance on older devices
- **Fast Startup** - Quick app launch with progressive feature loading

## 📱 App Screenshots

Experience Tulsi Gallery's beautiful interface and thoughtful design. These screenshots showcase the app's elegant UI, featuring the floating bottom app bar, intuitive navigation, and visually pleasing layout.

<!-- First row of screenshots -->
<p align="center">
  <img src="/assets/images/1.png" width="30%" alt="Tulsi Gallery Screenshot 1" />
  <img src="/assets/images/2.png" width="30%" alt="Tulsi Gallery Screenshot 2" />
  <img src="/assets/images/3.png" width="30%" alt="Tulsi Gallery Screenshot 3" />
</p>

<!-- Second row of screenshots -->
<p align="center">
  <img src="/assets/images/4.png" width="30%" alt="Tulsi Gallery Screenshot 4" />
  <img src="/assets/images/5.png" width="30%" alt="Tulsi Gallery Screenshot 5" />
  <img src="/assets/images/6.png" width="30%" alt="Tulsi Gallery Screenshot 6" />
</p>

## 🚀 Try Tulsi Today!

If you're tired of gallery apps that prioritize simplicity over functionality or aesthetics over usability, Tulsi might be just what you're looking for. It's the gallery app for people who:

* **Care about the little UI details** that make daily use more pleasant
* **Want powerful features** like OCR text search without sacrificing performance
* **Appreciate thoughtful design choices** that enhance the user experience
* **Need Google Lens integration** without switching between apps
* **Value privacy** with offline OCR processing and no data collection
* **Want to find photos faster** using AI-powered text recognition

### 🎯 Perfect For
- **Students** - Find photos of notes, whiteboards, and study materials instantly
- **Professionals** - Locate business cards, receipts, and document photos quickly
- **Content Creators** - Search through memes, screenshots, and social media content
- **Travelers** - Find photos by searching for place names, signs, or text in images
- **Anyone** who takes photos of text and wants to find them later!

Tulsi is maintained with love and an obsessive attention to detail. Because life's too short for boring gallery apps! 📱✨

## License

Tulsi Gallery is licensed under the [GNU General Public License v3.0](LICENSE.md).

As a fork of [LavenderGallery](https://github.com/kaii-lb/LavenderPhotos), Tulsi Gallery maintains the same GPLv3 license. This means:

- You are free to use, modify, and distribute this software
- If you distribute this software or derivatives, you must:
  - Make the source code available
  - Include the original copyright notices
  - License your modifications under GPLv3
  - Clearly mark what changes you have made

For the full license text, see the [LICENSE.md](LICENSE.md) file.

## Source Code Availability

Tulsi Gallery is open source software licensed under the GNU General Public License v3.0.

### How to Access the Source Code:
- **GitHub Repository**: The complete source code is available at [https://github.com/AKS-Labs/Tulsi](https://github.com/AKS-Labs/Tulsi)
- **Direct Request**: You can request the source code by emailing [akslabs.tech@gmail.com](mailto:akslabs.tech@gmail.com)
- **In-App Access**: The app includes an option to view the source code repository in the About section

### Building from Source:
For detailed build instructions, see the [BUILDING.md](BUILDING.md) file.

## 🙏 Acknowledgments

Tulsi Gallery stands on the shoulders of the excellent [LavenderGallery](https://github.com/kaii-lb/LavenderPhotos) project created by [kaii-lb](https://github.com/kaii-lb). All core functionality credit goes to the original developers.

Tulsi Gallery is a fork that adds additional features and UI enhancements while respecting the original project's GPLv3 license. We are grateful to the original author for creating such a fantastic foundation.

### Third-Party Libraries

Tulsi Gallery uses several open-source libraries and technologies:

- [Google ML Kit](https://developers.google.com/ml-kit) - Text recognition and OCR functionality
- [Glide](https://github.com/bumptech/glide) - Image loading and caching
- [Room](https://developer.android.com/jetpack/androidx/releases/room) - Database management with FTS5 search
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Background OCR processing
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI framework
- [Material Components](https://github.com/material-components/material-components-android) - Material Design components
- [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines) - Asynchronous programming

For a complete list of dependencies, see the [app/build.gradle.kts](app/build.gradle.kts) file.

---

*"Good design is obvious. Great design is transparent."*
