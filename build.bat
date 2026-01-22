@echo off
REM Android Mouse Cursor - One-Click Build Script
REM This script builds the APK for the Android Mouse Cursor project

echo ========================================
echo Android Mouse Cursor Build Script
echo ========================================
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found!
    echo Please run this script from the project root directory.
    pause
    exit /b 1
)

REM Try to find Java in Android Studio
set "ANDROID_STUDIO_JBR=D:\software\AndroidStudio\jbr"
if exist "%ANDROID_STUDIO_JBR%\bin\java.exe" (
    echo Found Java in Android Studio JBR
    set "JAVA_HOME=%ANDROID_STUDIO_JBR%"
    set "PATH=%ANDROID_STUDIO_JBR%\bin;%PATH%"
    goto :java_found
)

REM Check if Java is available in PATH
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Java is not installed or not in PATH!
    echo ========================================
    echo.
    echo Please install JDK 17 or higher:
    echo 1. Download from: https://adoptium.net/
    echo 2. Install JDK
    echo 3. Set JAVA_HOME environment variable
    echo 4. Add %%JAVA_HOME%%\bin to PATH
    echo.
    echo Or install Android Studio which includes JDK
    echo.
    pause
    exit /b 1
)

:java_found
echo Java version detected:
java -version
echo.

echo [1/3] Cleaning previous build...
call gradlew.bat clean
if errorlevel 1 (
    echo.
    echo Error: Clean failed!
    pause
    exit /b 1
)

echo.
echo [2/3] Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo Error: Build failed!
    pause
    exit /b 1
)

echo.
echo [3/3] Build successful!
echo.
echo ========================================
echo APK Location:
echo app\build\outputs\apk\debug\app-debug.apk
echo ========================================
echo.
echo APK Size:
for %%A in (app\build\outputs\apk\debug\app-debug.apk) do echo %%~zA bytes
echo.
echo You can install it using:
echo adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

pause
