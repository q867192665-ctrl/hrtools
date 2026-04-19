@echo off
chcp 65001 >nul
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\build-tools\34.0.0;%ANDROID_HOME%\cmdline-tools\latest\bin;%PATH%

echo Building Salary System APK...
echo.

cd /d "e:\22buckup"

rem Check if gradlew exists
if not exist "gradlew.bat" (
    echo Gradle wrapper not found, generating...
    gradle wrapper
)

rem Build APK
echo Starting APK build...
call gradlew.bat assembleDebug --stacktrace

if %ERRORLEVEL% equ 0 (
    echo.
    echo APK build successful!
    echo.
    
    rem Copy APK file
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        copy "app\build\outputs\apk\debug\app-debug.apk" "salary_system.apk" /Y
        echo APK file: e:\22buckup\salary_system.apk
        
        for %%F in ("salary_system.apk") do (
            echo File size: %%~zF bytes
        )
    )
) else (
    echo.
    echo APK build failed
)

pause
