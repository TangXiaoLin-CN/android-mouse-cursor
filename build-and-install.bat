@echo off
REM Android Mouse Cursor - Build and Install Script
REM This script builds and installs the APK to a connected Android device

echo ========================================
echo Android Mouse Cursor Build and Install
echo ========================================
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found!
    echo Please run this script from the project root directory.
    pause
    exit /b 1
)

REM Check if adb is available
where adb >nul 2>nul
if errorlevel 1 (
    echo Warning: adb not found in PATH!
    echo Please make sure Android SDK platform-tools is in your PATH.
    echo.
    set SKIP_INSTALL=1
) else (
    set SKIP_INSTALL=0
)

echo [1/4] Cleaning previous build...
call gradlew.bat clean
if errorlevel 1 (
    echo.
    echo Error: Clean failed!
    pause
    exit /b 1
)

echo.
echo [2/4] Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo Error: Build failed!
    pause
    exit /b 1
)

echo.
echo [3/4] Build successful!

if "%SKIP_INSTALL%"=="1" (
    echo.
    echo Skipping installation (adb not found)
    goto :end
)

echo.
echo [4/4] Installing APK to device...
adb devices
echo.
adb install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo.
    echo Error: Installation failed!
    echo Make sure:
    echo - Your device is connected via USB
    echo - USB debugging is enabled
    echo - Device is authorized
    pause
    exit /b 1
)

echo.
echo ========================================
echo Installation successful!
echo ========================================
echo.
echo To use the app:
echo 1. Open the app on your device
echo 2. Enable accessibility service for Mouse Cursor
echo 3. Connect from QtScrcpy on your PC
echo.

:end
pause
