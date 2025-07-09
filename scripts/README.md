# Scripts Directory

This directory contains build scripts and utility tools for the Tulsi Gallery project.

## 📁 Scripts Overview

### 🏗️ Build Scripts

#### `build.sh`
**Purpose**: Main build script for creating signed APK files  
**Usage**: 
```bash
./scripts/build.sh [release|universal]
```
- `./scripts/build.sh` - Creates debug APK
- `./scripts/build.sh release` - Creates release APK  
- `./scripts/build.sh universal` - Creates universal APK

**Output**: Signed APK files (`Gallery_signed_debug.apk`, `Gallery_signed_release.apk`, etc.)

### 🔧 Development Utilities

#### `fix_packages.ps1`
**Purpose**: PowerShell script to fix package names during development  
**Usage**: 
```powershell
.\scripts\fix_packages.ps1
```
**Function**: 
- Fixes package declarations from old to new package names
- Updates import statements
- Fixes UI text references
- Updates file provider authorities

#### `update_package_names.ps1`
**Purpose**: PowerShell script to update package names in source files  
**Usage**: 
```powershell
.\scripts\update_package_names.ps1
```
**Function**: Updates package references in Kotlin source files

#### `fix_image.py`
**Purpose**: Python script to optimize and fix image assets  
**Usage**: 
```python
python scripts/fix_image.py
```
**Function**: 
- Optimizes PNG images
- Converts RGBA to RGB if needed
- Saves optimized images to temp directory

### 🚀 Deployment Scripts

#### `push_changes.sh`
**Purpose**: Git utility script for pushing changes  
**Usage**: 
```bash
./scripts/push_changes.sh
```
**Function**: Automates git operations for pushing changes

#### `rewrite_commit.sh`
**Purpose**: Git utility for rewriting commit history  
**Usage**: 
```bash
./scripts/rewrite_commit.sh
```
**Function**: Rewrites specific commits in git history

### 📊 Analysis Tools

#### `analyze-apk-size.gradle`
**Purpose**: Gradle script for analyzing APK size  
**Usage**: Include in build.gradle or run directly  
**Function**: Provides detailed APK size analysis and optimization suggestions

## 🚨 Prerequisites

### For Shell Scripts (.sh)
- Unix-like environment (Linux, macOS, WSL, Git Bash)
- Bash shell
- Java 17+ (for build scripts)
- Android SDK (for build scripts)

### For PowerShell Scripts (.ps1)
- Windows PowerShell or PowerShell Core
- Execution policy allowing script execution:
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  ```

### For Python Scripts (.py)
- Python 3.6+
- PIL (Pillow) library for image processing:
  ```bash
  pip install Pillow
  ```

## 🔐 Security Notes

### Build Scripts
- `build.sh` requires signing keys in the `keys/` directory
- Never commit signing keys to version control
- Use secure key storage for production builds

### PowerShell Scripts
- Review scripts before execution
- Run with appropriate execution policies
- Some scripts modify source files - ensure you have backups

## 📝 Usage Examples

### Building a Release APK
```bash
# Navigate to project root
cd /path/to/Tulsi

# Run build script
./scripts/build.sh release

# Output will be: Gallery_signed_release.apk
```

### Fixing Package Names During Development
```powershell
# Navigate to project root
cd C:\Users\AKS\StudioProjects\Tulsi

# Run package fix script
.\scripts\fix_packages.ps1
```

### Optimizing Images
```python
# Navigate to project root
cd /path/to/Tulsi

# Run image optimization
python scripts/fix_image.py
```

## 🛠️ Maintenance

### Adding New Scripts
1. Place script files in this `scripts/` directory
2. Update this README.md with script documentation
3. Ensure proper file permissions (executable for .sh files)
4. Test scripts in clean environment before committing

### Script Permissions
For Unix scripts, ensure executable permissions:
```bash
chmod +x scripts/*.sh
```

## 📞 Support

If you encounter issues with any scripts:
1. Check prerequisites are installed
2. Verify file permissions
3. Review script output for error messages
4. Check the main project documentation in `docs/`

---

**Note**: All scripts should be run from the project root directory unless otherwise specified.
