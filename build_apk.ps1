# PowerShell script to build APK with Chinese path handling

# Set environment variables
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"

# Create temporary directory with English path
$tempDir = Join-Path $env:TEMP "salary_build"
if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
New-Item -ItemType Directory -Path $tempDir -Force

# Copy project files to temp directory
Write-Host "Copying project files to temporary directory..."
Copy-Item "e:\程序\webtool\22\*" $tempDir -Recurse -Force

# Set Gradle properties for Chinese path support
$gradleProps = @"
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.overridePathCheck=true
"@
Set-Content -Path "$tempDir\gradle.properties" -Value $gradleProps -Encoding UTF8

# Change to temp directory and build
Set-Location $tempDir

Write-Host "Building APK..."
$buildResult = .\gradlew.bat assembleDebug --no-daemon --console=plain 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ APK build successful!" -ForegroundColor Green
    
    # Copy APK back to original location
    $apkPath = "$tempDir\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $destPath = "e:\程序\webtool\22\salary_system.apk"
        Copy-Item $apkPath $destPath -Force
        
        $apkInfo = Get-Item $destPath
        Write-Host "📱 APK file: $destPath"
        Write-Host "📊 File size: $([math]::Round($apkInfo.Length/1KB, 2)) KB"
        Write-Host "🕒 Modified: $($apkInfo.LastWriteTime)"
        
        Write-Host ""
        Write-Host "🎯 Features:" -ForegroundColor Cyan
        Write-Host "   • IPv6 server address configuration"
        Write-Host "   • User login interface"
        Write-Host "   • Salary information display"
        Write-Host "   • Handwritten signature function"
        Write-Host "   • Signature image upload"
        Write-Host ""
        Write-Host "🔗 Backend service: Run backend/run_backend.bat" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ APK build failed" -ForegroundColor Red
    Write-Host $buildResult
}

# Clean up
Set-Location "e:\程序\webtool\22"
Remove-Item $tempDir -Recurse -Force