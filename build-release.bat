@echo off
setlocal enabledelayedexpansion
REM Android Mouse Cursor - Release Build Script
REM This script builds the release APK for the Android Mouse Cursor project

echo ========================================
echo Android Mouse Cursor Release Build
echo ========================================
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found!
    echo Please run this script from the project root directory.
    pause
    exit /b 1
)

REM Try to find JAVA_HOME if not set
if not defined JAVA_HOME (
    echo JAVA_HOME not set, trying to find Java...

    REM Check common Java installation locations
    for %%J in (
        "D:\software\AndroidStudio\jbr"
        "C:\Program Files\Java\jdk-17"
        "C:\Program Files\Java\jdk-21"
        "C:\Program Files\Eclipse Adoptium\jdk-17"
        "C:\Program Files\Eclipse Adoptium\jdk-21"
        "D:\software\Java\jdk-17"
        "D:\software\Java\jdk-21"
        "D:\personSpace\newDownload\androidstudioCache\jbr"
    ) do (
        if exist %%J\bin\java.exe (
            set "JAVA_HOME=%%~J"
            echo Found Java at: !JAVA_HOME!
            goto :java_found
        )
    )

    REM Try to find java in PATH
    where java >nul 2>&1
    if not errorlevel 1 (
        echo Java found in PATH
        goto :java_found
    )

    echo Error: Java is not installed or not in PATH
    echo Please install JDK 17 or higher from: https://adoptium.net/
    echo Or set JAVA_HOME environment variable manually
    pause
    exit /b 1
)

:java_found
if defined JAVA_HOME (
    echo Using JAVA_HOME: %JAVA_HOME%
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
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
echo [2/3] Building release APK...
call gradlew.bat assembleRelease
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
echo app\build\outputs\apk\release\app-release.apk
echo ========================================
echo.
echo Note: This APK is signed with debug keystore and ready to install.
echo.

pause
