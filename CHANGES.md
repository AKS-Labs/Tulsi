# Changes from Original LavenderPhotos

This document outlines the major changes made to the original LavenderPhotos codebase in the Tulsi fork.

## Package Name Change
- Changed package name from `com.kaii.photos` to `com.aks_labs.tulsi`
- Updated all import statements and references accordingly

## UI Enhancements
- Added floating bottom app bar with rounded corners (35% radius)
- Customized horizontal padding in photo grid view
- Implemented transparent backgrounds in single photo view
- Adjusted dimensions (0.95f width, 76.dp height) for better visual appearance

## Feature Additions
- Integrated Google Lens functionality with multi-layered implementation:
  - Primary method: Direct integration with Google app using ACTION_SEND intent
  - Multiple fallback mechanisms for different device configurations
- Added toggle between grid view and date-grouped view
- Implemented persistent view mode preference between sessions

## Navigation Improvements
- Reordered navigation tabs to: Search, Screenshots, Albums, Secure
- Changed Photos tab to Screenshots tab for more distinctive purpose
- Made Search the default opening tab

## Bug Fixes
- Fixed selection issues in grid view mode
- Improved glide selection and drag selection functionality
- Enhanced performance for large photo libraries

## Other Changes
- Updated app icon and branding
- Improved error handling and user feedback
- Enhanced accessibility features
