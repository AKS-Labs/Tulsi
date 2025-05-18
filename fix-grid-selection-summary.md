# Fix for Selection Functionality in Grid View Mode

## Issue Description
In the Tulsi photo app, attempting to use either glide selection (swiping across multiple Gallery) or drag selection in grid view mode causes the app to crash. However, in date-grouped view mode, these selection methods work correctly without any crashes.

## Root Cause
The root cause of the issue is in how the selection functionality handles items in grid view mode versus date-grouped view mode:

1. In grid view mode, all items are assigned to a single section with `childCount` equal to the total number of items, but the section handling in the selection code doesn't properly account for this.

2. The `selectItem` and `selectAll` functions in `Selection.kt` have logic that assumes date-grouped sections, which causes issues when all items are in a single section in grid view mode.

3. The key issue is in the `selectItem` function where it checks if the number of already selected items in a section is equal to `childCount - 1`. In grid view mode, this can lead to an index out of bounds exception because all items are in one large section.

4. There are also bounds checking issues in the `getGridItemAtOffset` function and the drag selection handler in `PhotoGridView.kt`.

## Changes Made

### 1. Modified `Selection.kt` to handle grid view mode properly:

- Added grid view mode detection based on section date being 0L and no section items in the grouped media
- Updated all selection functions (`selectItem`, `selectAll`, `unselectItem`, `unselectAll`, `selectSection`, `unselectSection`) to handle grid view mode differently
- In grid view mode, we now bypass the section handling logic and directly add/remove items

### 2. Improved `PhotoGridView.kt` to be more robust:

- Added bounds checking in the drag selection handler to prevent index out of bounds exceptions
- Made the `getGridItemAtOffset` function more robust with better error handling
- Added safety checks to ensure indices are within valid ranges

## Testing
The changes have been tested and the app now allows both glide selection and drag selection to work properly in grid view mode without crashing.
