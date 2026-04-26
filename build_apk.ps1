$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "e:\22buckup"

Write-Host "Building APK..."
Write-Host ""

& "$env:JAVA_HOME\bin\java.exe" -jar "$env:ANDROID_HOME\cmdline-tools\latest\lib\gradle-wrapper.jar" 2>$null
if (-not $?) {
    Write-Host "Creating Gradle wrapper..."
    & "$env:JAVA_HOME\bin\java.exe" -cp "$env:ANDROID_HOME\cmdline-tools\latest\lib\*" org.gradle.wrapper.GradleWrapperMain wrapper
}

Write-Host "Running gradle build..."
.\gradlew.bat assembleRelease --stacktrace

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "APK build successful!" -ForegroundColor Green
    Write-Host ""
    $apkPath = "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $apkPath) {
        $file = Get-Item $apkPath
        Write-Host "APK location: $apkPath"
        Write-Host "File size: $($file.Length) bytes"
        Write-Host "Modified: $($file.LastWriteTime)"
    }
} else {
    Write-Host ""
    Write-Host "APK build failed" -ForegroundColor Red
}
