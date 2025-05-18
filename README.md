<p align="center">
  <img src="assets/images/tulsi.png" alt="Tulsi App Icon" width="180"/>
</p>

<h1 align="center">📸 Tulsi Gallery</h1>

<img src="assets/images/tulsi.png" alt="Tulsi App Icon" width="120" align="right"/>

> *Because your Gallery deserve a gallery app with personality!*

Tulsi is a fork of [LavenderGallery](https://github.com/kaii-lb/LavenderGallery) that dares to be different. While LavenderGallery is already a fantastic gallery app, Tulsi adds that extra bit of flair and functionality that makes your photo browsing experience just *chef's kiss* 👌

## 🌟 Why Tulsi Exists

Let's be honest—we spend hours scrolling through our Gallery, so why not make the experience as delightful as possible? Tulsi was born because:

1. **I'm picky about UI details** - Those rounded corners and floating elements aren't just pretty, they make the app feel more premium
2. **Google Lens should be one tap away** - Why switch apps when you want to know "what's that building?" or "what kind of dog is this?"
3. **Navigation should match how humans think** - Search first, then Gallery, Albums, and Secure stuff tucked away at the end
4. **The devil is in the details** - Subtle animations, proper elevations, and thoughtful spacing make all the difference

All credit for the original codebase goes to the amazing LavenderGallery project. Tulsi just adds a bit of spice to that already delicious recipe! 🌶️

## Features

### Original Features from LavenderGallery
- Browse all your Gallery and videos smoothly, separated by date
- Add and remove albums as you wish, no arbitrary or forced selections
- Search for an image by its name or date (in many formats!)
- Trash Bin that's sorted by recently trashed
- Full-fledged favoriting system
- A selection system that doesn't suck
- Edit and personalize any photo, any time, without an internet connection (even works in landscape mode!)
- Secure sensitive Gallery in an encrypted medium, for safe keeping
- Find all the relevant information for a photo from one button click
- Copy and Move Gallery to albums easily
- Clean UI and smooth UX
- Privacy-focused design, no chance of anything happening without your permission

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

## 🔍 Google Lens Integration: The Details

Tulsi's Google Lens integration is like having a visual search superpower built right into your gallery app. The implementation is robust with multiple fallback options:

1. **Primary method**: Direct integration with Google app using ACTION_SEND intent
2. **Fallback #1**: Integration with Google Gallery app
3. **Fallback #2**: Generic chooser dialog for maximum compatibility
4. **Fallback #3**: Direct ACTION_VIEW with Google app
5. **Last resort**: Web fallback to lens.google.com

This multi-layered approach ensures that no matter what device or Android version you're using, you'll always be able to identify that strange bug you photographed or find out what mountain that is in the background of your vacation photo.

## 📱 App Screenshots

Experience Tulsi Gallery's beautiful interface and thoughtful design. These screenshots showcase the app's elegant UI, featuring the floating bottom app bar, intuitive navigation, and visually pleasing layout.

<div align="center">
  <img src="/assets/images/1.png" width="250" alt="Tulsi Gallery Screenshot 1" style="margin-right: 10px;" />
  <img src="/assets/images/2.png" width="300" alt="Tulsi Gallery Screenshot 2" style="margin: 0 10px;" />
  <img src="/assets/images/3.png" width="250" alt="Tulsi Gallery Screenshot 3" style="margin-left: 10px;" />
</div>

<br />

<div align="center">
  <img src="/assets/images/4.png" width="250" alt="Tulsi Gallery Screenshot 4" style="margin-right: 10px;" />
  <img src="/assets/images/5.png" width="300" alt="Tulsi Gallery Screenshot 5" style="margin: 0 10px;" />
  <img src="/assets/images/6.png" width="250" alt="Tulsi Gallery Screenshot 6" style="margin-left: 10px;" />
</div>

## 🚀 Try Tulsi Today!

If you're tired of gallery apps that prioritize simplicity over functionality or aesthetics over usability, Tulsi might be just what you're looking for. It's the gallery app for people who:

* Care about the little UI details that make daily use more pleasant
* Want powerful features without sacrificing performance
* Appreciate thoughtful design choices that enhance the user experience
* Need Google Lens integration without switching between apps

Tulsi is maintained with love and an obsessive attention to detail. Because life's too short for boring gallery apps! 📱✨

## License

Tulsi Gallery is licensed under the [GNU General Public License v3.0](LICENSE.md).

As a fork of [LavenderGallery](https://github.com/kaii-lb/LavenderGallery), Tulsi Gallery maintains the same GPLv3 license. This means:

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

Tulsi Gallery stands on the shoulders of the excellent [LavenderGallery](https://github.com/kaii-lb/LavenderGallery) project created by [kaii-lb](https://github.com/kaii-lb). All core functionality credit goes to the original developers.

Tulsi Gallery is a fork that adds additional features and UI enhancements while respecting the original project's GPLv3 license. We are grateful to the original author for creating such a fantastic foundation.

### Third-Party Libraries

Tulsi Gallery uses several open-source libraries:

- [Glide](https://github.com/bumptech/glide) - Image loading and caching
- [Room](https://developer.android.com/jetpack/androidx/releases/room) - Database management
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework
- [Material Components](https://github.com/material-components/material-components-android) - UI components

For a complete list of dependencies, see the [app/build.gradle.kts](app/build.gradle.kts) file.

---

*"Good design is obvious. Great design is transparent."*
