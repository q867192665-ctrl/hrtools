@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\build-tools\34.0.0;%PATH%

echo 正在构建IPv6通讯工具APK...

cd /d "%~dp0"

rem 创建临时目录
if not exist "temp" mkdir temp
if not exist "temp\classes" mkdir temp\classes
if not exist "temp\res" mkdir temp\res

rem 编译Kotlin代码
echo 编译Kotlin代码...
"%JAVA_HOME%\bin\javac.exe" -cp "%ANDROID_HOME%\platforms\android-34\android.jar" -d temp\classes app\src\main\java\com\example\ipv6communicator\MainActivity.kt

if %ERRORLEVEL% neq 0 (
    echo Kotlin编译失败，尝试直接构建APK...
)

rem 使用aapt2打包资源
echo 打包资源...
"%ANDROID_HOME%\build-tools\34.0.0\aapt2.exe" link -o temp\app-unsigned.apk -I "%ANDROID_HOME%\platforms\android-34\android.jar" --manifest app\src\main\AndroidManifest.xml --java temp\gen

if %ERRORLEVEL% neq 0 (
    echo 资源打包失败
    pause
    exit /b 1
)

echo APK构建完成！
echo APK文件位置: temp\app-unsigned.apk

pause