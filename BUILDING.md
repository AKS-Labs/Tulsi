# Building Tulsi Gallery from Source

This document provides instructions for building Tulsi Gallery from source code.

## Prerequisites

- Android Studio Arctic Fox (2021.3.1) or newer
- JDK 17 or newer
- Git

## Steps to Build

1. Clone the repository:
   ```
   git clone https://github.com/AKS-Labs/Tulsi.git
   ```

2. Open the project in Android Studio:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository and select it

3. Install dependencies:
   Android Studio should automatically sync and download the required dependencies.

4. Build the app:
   - For a debug build: `./gradlew assembleDebug`
   - For a release build: `./gradlew assembleRelease`

5. Install on a device:
   - Connect an Android device via USB with USB debugging enabled
   - Run `./gradlew installDebug` to install the debug version

## Troubleshooting

If you encounter any issues during the build process:

1. Make sure you have the latest Android Studio and JDK installed
2. Try cleaning the project with `./gradlew clean`
3. Ensure all dependencies are properly resolved
4. Check that your device is running Android 10 (API level 30) or higher

## Contact

If you need assistance building from source, please email [akslabs.tech@gmail.com](mailto:akslabs.tech@gmail.com).
