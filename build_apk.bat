@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\build-tools\34.0.0;%PATH%

echo Building Salary System APK (handling Chinese path issue)...
echo.

rem Create temp directory with English path
set TEMP_DIR=%TEMP%\salary_system_build
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
mkdir "%TEMP_DIR%"

rem Copy project files to temp directory
robocopy "e:\程序\webtool\22" "%TEMP_DIR%" /E /XF *.apk /XD build gradle .gradle /R:3 /W:10

rem Change to temp directory
cd /d "%TEMP_DIR%"

rem Set Gradle properties to support Chinese path
echo org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 > gradle.properties
echo android.useAndroidX=true >> gradle.properties
echo kotlin.code.style=official >> gradle.properties
echo android.nonTransitiveRClass=true >> gradle.properties
echo android.overridePathCheck=true >> gradle.properties

rem Build APK
echo Starting APK build...
call gradlew.bat assembleDebug --stacktrace

if %ERRORLEVEL% equ 0 (
    echo.
    echo APK build successful!
    echo.
    
    rem Copy APK file back to original directory
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        copy "app\build\outputs\apk\debug\app-debug.apk" "e:\程序\webtool\22\salary_system.apk"
        echo APK file copied to: e:\程序\webtool\22\salary_system.apk
        
        rem Show file info
        for %%F in ("e:\程序\webtool\22\salary_system.apk") do (
            echo File size: %%~zF bytes
            echo Modified: %%~tF
        )
    )
) else (
    echo.
    echo APK build failed
)

rem Clean up temp directory
cd /d "e:\程序\webtool\22"
rmdir /s /q "%TEMP_DIR%"

pause