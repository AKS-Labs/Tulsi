# 📸 Tulsi Photos

> *Because your photos deserve a gallery app with personality!*

Tulsi is a fork of [LavenderPhotos](https://github.com/KaustubhPatange/LavenderPhotos) that dares to be different. While LavenderPhotos is already a fantastic gallery app, Tulsi adds that extra bit of flair and functionality that makes your photo browsing experience just *chef's kiss* 👌

## 🌟 Why Tulsi Exists

Let's be honest—we spend hours scrolling through our photos, so why not make the experience as delightful as possible? Tulsi was born because:

1. **I'm picky about UI details** - Those rounded corners and floating elements aren't just pretty, they make the app feel more premium
2. **Google Lens should be one tap away** - Why switch apps when you want to know "what's that building?" or "what kind of dog is this?"
3. **Navigation should match how humans think** - Search first, then Photos, Albums, and Secure stuff tucked away at the end
4. **The devil is in the details** - Subtle animations, proper elevations, and thoughtful spacing make all the difference

All credit for the original codebase goes to the amazing LavenderPhotos project. Tulsi just adds a bit of spice to that already delicious recipe! 🌶️

## Features

### Original Features from LavenderPhotos
- Browse all your photos and videos smoothly, separated by date
- Add and remove albums as you wish, no arbitrary or forced selections
- Search for an image by its name or date (in many formats!)
- Trash Bin that's sorted by recently trashed
- Full-fledged favoriting system
- A selection system that doesn't suck
- Edit and personalize any photo, any time, without an internet connection (even works in landscape mode!)
- Secure sensitive photos in an encrypted medium, for safe keeping
- Find all the relevant information for a photo from one button click
- Copy and Move photos to albums easily
- Clean UI and smooth UX
- Privacy-focused design, no chance of anything happening without your permission

### ✨ Tulsi's Special Sauce

What makes Tulsi stand out from the original LavenderPhotos? Here's the secret recipe:

#### 🔍 Google Lens Superpowers
* **One-tap visual search** - See a cool landmark? Weird plant? Mysterious gadget? Tap the Lens icon and get answers instantly
* **Multi-layered implementation** - Works reliably across different Android versions and device configurations
* **Fallback mechanisms** - Even if your device configuration is unusual, we've got you covered with 5 different ways to make Lens work

#### 🎨 UI That Sparks Joy
* **Floating bottom app bar** - With perfectly rounded corners (35% radius), proper elevation, and buttery-smooth animations
* **Thoughtful spacing** - Customized horizontal padding gives your photos room to breathe
* **Transparent backgrounds** - In single photo view, with carefully calibrated dimensions (0.95f width, 76.dp height)

#### 🧭 Navigation That Makes Sense
* **Reordered tabs** - Search, Photos, Albums, Secure - in that order, because that's how most people use a gallery app
* **Consistent three-column grid** - With an optional toggle to switch to date-grouped view
* **Memory for your preferences** - The app remembers your preferred view mode between sessions

#### 🔄 Selection That Actually Works
* **Fixed selection issues** - Glide selection and drag selection work correctly in all view modes
* **Intuitive controls** - Select multiple photos with natural gestures

## 🔍 Google Lens Integration: The Details

Tulsi's Google Lens integration is like having a visual search superpower built right into your gallery app. The implementation is robust with multiple fallback options:

1. **Primary method**: Direct integration with Google app using ACTION_SEND intent
2. **Fallback #1**: Integration with Google Photos app
3. **Fallback #2**: Generic chooser dialog for maximum compatibility
4. **Fallback #3**: Direct ACTION_VIEW with Google app
5. **Last resort**: Web fallback to lens.google.com

This multi-layered approach ensures that no matter what device or Android version you're using, you'll always be able to identify that strange bug you photographed or find out what mountain that is in the background of your vacation photo.

## Screenshots
  Main View                 |  Albums                   |
:--------------------------:|:-------------------------:|
![](/assets/images/main.png)|![](/assets/images/albums.png)

  Secure Folder            |  Favourites & Trash       |  Search                  |
:-------------------------:|:-------------------------:|:-------------------------:
  ![](/assets/images/locked.png)|![](/assets/images/favtrash.png)|![](/assets/images/search.png)

## 🚀 Try Tulsi Today!

If you're tired of gallery apps that prioritize simplicity over functionality or aesthetics over usability, Tulsi might be just what you're looking for. It's the gallery app for people who:

* Care about the little UI details that make daily use more pleasant
* Want powerful features without sacrificing performance
* Appreciate thoughtful design choices that enhance the user experience
* Need Google Lens integration without switching between apps

Tulsi is maintained with love and an obsessive attention to detail. Because life's too short for boring gallery apps! 📱✨

## 🙏 Acknowledgments

Tulsi stands on the shoulders of the excellent [LavenderPhotos](https://github.com/KaustubhPatange/LavenderPhotos) project. All core functionality credit goes to the original developers. Tulsi simply adds a personal touch to an already fantastic foundation.

---

*"Good design is obvious. Great design is transparent." *
