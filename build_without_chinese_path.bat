@echo off
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\build-tools\34.0.0;%PATH%

echo 正在构建工资签收系统APK（处理中文路径问题）...
echo.

rem 创建临时工作目录（使用英文路径）
set TEMP_DIR=%TEMP%\salary_system_build
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
mkdir "%TEMP_DIR%"
mkdir "%TEMP_DIR%\app"
mkdir "%TEMP_DIR%\app\src"
mkdir "%TEMP_DIR%\app\src\main"
mkdir "%TEMP_DIR%\app\src\main\java"
mkdir "%TEMP_DIR%\app\src\main\res"
mkdir "%TEMP_DIR%\app\src\main\res\drawable"
mkdir "%TEMP_DIR%\app\src\main\res\layout"
mkdir "%TEMP_DIR%\app\src\main\res\values"
mkdir "%TEMP_DIR%\app\src\main\res\mipmap-anydpi-v26"
mkdir "%TEMP_DIR%\app\src\main\res\xml"

rem 复制项目文件到临时目录
xcopy "e:\程序\webtool\22\settings.gradle.kts" "%TEMP_DIR%" /Y
xcopy "e:\程序\webtool\22\build.gradle.kts" "%TEMP_DIR%" /Y
xcopy "e:\程序\webtool\22\gradle.properties" "%TEMP_DIR%" /Y
xcopy "e:\程序\webtool\22\gradlew.bat" "%TEMP_DIR%" /Y
xcopy "e:\程序\webtool\22\gradle" "%TEMP_DIR%\gradle" /E /Y

xcopy "e:\程序\webtool\22\app\build.gradle.kts" "%TEMP_DIR%\app" /Y
xcopy "e:\程序\webtool\22\app\src\main\AndroidManifest.xml" "%TEMP_DIR%\app\src\main" /Y

rem 复制Java/Kotlin文件
xcopy "e:\程序\webtool\22\app\src\main\java\com" "%TEMP_DIR%\app\src\main\java\com" /E /Y

rem 复制资源文件
xcopy "e:\程序\webtool\22\app\src\main\res\drawable" "%TEMP_DIR%\app\src\main\res\drawable" /E /Y
xcopy "e:\程序\webtool\22\app\src\main\res\layout" "%TEMP_DIR%\app\src\main\res\layout" /E /Y
xcopy "e:\程序\webtool\22\app\src\main\res\values" "%TEMP_DIR%\app\src\main\res\values" /E /Y
xcopy "e:\程序\webtool\22\app\src\main\res\mipmap-anydpi-v26" "%TEMP_DIR%\app\src\main\res\mipmap-anydpi-v26" /E /Y
xcopy "e:\程序\webtool\22\app\src\main\res\xml" "%TEMP_DIR%\app\src\main\res\xml" /E /Y

rem 切换到临时目录构建
cd /d "%TEMP_DIR%"

rem 设置Gradle属性以支持中文路径
echo org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 > gradle.properties
echo android.useAndroidX=true >> gradle.properties
echo kotlin.code.style=official >> gradle.properties
echo android.nonTransitiveRClass=true >> gradle.properties
echo android.overridePathCheck=true >> gradle.properties

rem 构建APK
echo 开始构建APK...
call gradlew.bat assembleDebug --stacktrace

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ APK构建成功！
    echo.
    
    rem 复制APK文件回原目录
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        copy "app\build\outputs\apk\debug\app-debug.apk" "e:\程序\webtool\22\app-debug.apk"
        echo 📱 APK文件已复制到: e:\程序\webtool\22\app-debug.apk
        
        rem 显示文件信息
        for %%F in ("e:\程序\webtool\22\app-debug.apk") do (
            echo 📊 文件大小: %%~zF 字节
            echo 🕒 修改时间: %%~tF
        )
    )
) else (
    echo.
    echo ❌ APK构建失败
)

rem 清理临时目录
cd /d "e:\程序\webtool\22"
rmdir /s /q "%TEMP_DIR%"

pause