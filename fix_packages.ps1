# This script will fix package names in the codebase

# 1. Fix package declarations
Get-ChildItem -Path app/src/main/java/com/aks_labs/tulsi -Recurse -Include *.kt | ForEach-Object {
    $file = $_
    $content = Get-Content -Path $file -Raw
    
    # Fix package declarations
    if ($content -match "package com\.kaii\.photos") {
        Write-Host "Fixing package declaration in $($file.FullName)"
        $newContent = $content -replace "package com\.kaii\.photos", "package com.aks_labs.tulsi"
        Set-Content -Path $file -Value $newContent
    }
    
    # Fix import statements
    if ($content -match "import com\.kaii\.photos") {
        Write-Host "Fixing imports in $($file.FullName)"
        $newContent = $content -replace "import com\.kaii\.photos", "import com.aks_labs.tulsi"
        Set-Content -Path $file -Value $newContent
    }
}

# 2. Fix specific references to Lavender in UI text
Get-ChildItem -Path app/src/main/java/com/aks_labs/tulsi -Recurse -Include *.kt | ForEach-Object {
    $file = $_
    $content = Get-Content -Path $file -Raw
    
    # Fix Lavender Photos references
    if ($content -match "Lavender Photos") {
        Write-Host "Fixing 'Lavender Photos' references in $($file.FullName)"
        $newContent = $content -replace "Lavender Photos", "Tulsi Photos"
        Set-Content -Path $file -Value $newContent
    }
    
    # Fix LavenderPhotos references
    if ($content -match "LavenderPhotos") {
        Write-Host "Fixing 'LavenderPhotos' references in $($file.FullName)"
        $newContent = $content -replace "LavenderPhotos", "TulsiPhotos"
        Set-Content -Path $file -Value $newContent
    }
}

# 3. Fix LAVENDER_FILE_PROVIDER_AUTHORITY
Get-ChildItem -Path app/src/main/java/com/aks_labs/tulsi -Recurse -Include *.kt | ForEach-Object {
    $file = $_
    $content = Get-Content -Path $file -Raw
    
    # Fix LAVENDER_FILE_PROVIDER_AUTHORITY
    if ($content -match "LAVENDER_FILE_PROVIDER_AUTHORITY") {
        Write-Host "Fixing LAVENDER_FILE_PROVIDER_AUTHORITY in $($file.FullName)"
        $newContent = $content -replace "com\.kaii\.photos\.fileprovider", "com.aks_labs.tulsi.fileprovider"
        Set-Content -Path $file -Value $newContent
    }
}

Write-Host "Done fixing package names"
