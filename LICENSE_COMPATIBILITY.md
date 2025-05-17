# License Compatibility Analysis

This document verifies that all dependencies used in Tulsi Photos have licenses compatible with the GNU General Public License v3.0 (GPLv3).

## License Compatibility with GPLv3

The following licenses are generally considered compatible with GPLv3 when used as dependencies:

- GNU General Public License (GPL) v2 or v3
- GNU Lesser General Public License (LGPL) v2.1 or v3
- MIT License
- Apache License 2.0
- BSD License (2-clause or 3-clause)
- Eclipse Public License (EPL)
- Mozilla Public License (MPL) v2.0

## Major Dependencies Analysis

| Dependency | License | Compatible with GPLv3 | Notes |
|------------|---------|------------------------|-------|
| AndroidX Libraries | Apache License 2.0 | ✅ Yes | All AndroidX components use Apache 2.0 |
| Jetpack Compose | Apache License 2.0 | ✅ Yes | All Compose components use Apache 2.0 |
| Kotlin Standard Library | Apache License 2.0 | ✅ Yes | |
| Glide | BSD, MIT and Apache 2.0 | ✅ Yes | Multi-licensed, all compatible |
| Room | Apache License 2.0 | ✅ Yes | |
| Material Components | Apache License 2.0 | ✅ Yes | |
| Coroutines | Apache License 2.0 | ✅ Yes | |
| Lifecycle Components | Apache License 2.0 | ✅ Yes | |
| Navigation Components | Apache License 2.0 | ✅ Yes | |

## Conclusion

All dependencies used in Tulsi Photos have licenses that are compatible with the GPLv3. This means:

1. We can legally distribute the app under the GPLv3 license
2. Users who receive the app can exercise all the freedoms granted by the GPLv3
3. There are no license conflicts that would prevent compliance with the GPLv3

## Ongoing Compliance

When adding new dependencies to the project:

1. Check the license of the dependency
2. Verify compatibility with GPLv3
3. Update this document to include the new dependency
4. Update the NOTICE.md file to properly attribute the dependency

This ensures continued compliance with the GPLv3 license requirements.
