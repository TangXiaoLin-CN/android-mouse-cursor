@echo off
REM Environment Check Script for Android Mouse Cursor Project

echo ========================================
echo Environment Check
echo ========================================
echo.

set ALL_OK=1

REM Check Java
echo [1/3] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [FAIL] Java is NOT installed or not in PATH
    echo        Please install JDK 17 or higher from: https://adoptium.net/
    set ALL_OK=0
) else (
    echo [OK] Java is installed:
    java -version 2>&1 | findstr "version"
)
echo.

REM Check Android SDK (optional)
echo [2/3] Checking Android SDK...
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
        echo [OK] Android SDK found at: %ANDROID_HOME%
    ) else (
        echo [WARN] ANDROID_HOME is set but SDK not found
        echo        Location: %ANDROID_HOME%
    )
) else (
    echo [WARN] ANDROID_HOME is not set
    echo        This is optional for building, but required for some features
)
echo.

REM Check ADB (optional)
echo [3/3] Checking ADB...
where adb >nul 2>&1
if errorlevel 1 (
    echo [WARN] ADB is not in PATH
    echo        This is optional, but needed for installing APK to device
) else (
    echo [OK] ADB is available:
    for /f "tokens=*" %%i in ('where adb') do echo        %%i
)
echo.

echo ========================================
if %ALL_OK%==1 (
    echo Result: Environment is ready for building!
    echo You can now run: build.bat
) else (
    echo Result: Please install missing requirements
    echo.
    echo Required:
    echo - JDK 17 or higher: https://adoptium.net/
    echo.
    echo Optional:
    echo - Android SDK: Install via Android Studio
    echo - ADB: Included in Android SDK platform-tools
)
echo ========================================
echo.

pause
