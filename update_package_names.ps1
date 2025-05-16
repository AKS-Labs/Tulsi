$files = Get-ChildItem -Path app/src/main/java/ -Recurse -Include *.kt | Select-String -Pattern "com.aks_labs.tulsi" | Select-Object Path -Unique | ForEach-Object { $_.Path }

foreach ($file in $files) {
    Write-Host "Processing $file"
    $content = Get-Content -Path $file -Raw
    $newContent = $content -replace "com\.kaii\.photos", "com.aks_labs.tulsi"
    Set-Content -Path $file -Value $newContent
}

Write-Host "Done updating package names"
