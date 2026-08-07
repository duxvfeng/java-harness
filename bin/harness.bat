@echo off
REM java-harness Windows platform wrapper
REM Detects platform and delegates to the correct harness binary

setlocal enabledelayedexpansion

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM Platform detection for Windows
set "OS=windows"
set "ARCH=amd64"

REM Detect architecture
if "%PROCESSOR_ARCHITECTURE%"=="x86" (
    if "%PROCESSOR_ARCHITEW6432%"=="ARM64" (
        set "ARCH=arm64"
    ) else (
        set "ARCH=386"
    )
) else if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
    set "ARCH=arm64"
) else if "%PROCESSOR_ARCHITEW6432%"=="AMD64" (
    set "ARCH=amd64"
) else if "%PROCESSOR_ARCHITEW6432%"=="ARM64" (
    set "ARCH=arm64"
)

REM Build binary name (consistent with Go version)
set "BINARY_NAME=harness-%OS%-%ARCH%.exe"
set "BINARY_PATH=%SCRIPT_DIR%\%BINARY_NAME%"

REM Try to execute Native Image binary
if exist "%BINARY_PATH%" (
    "%BINARY_PATH%" %*
    exit /b !ERRORLEVEL!
)

REM Fallback: 尝试旧的子目录结构(向后兼容)
set "OLD_BINARY_PATH="
if "%OS%"=="windows" (
    if "%ARCH%"=="amd64" (
        set "OLD_BINARY_PATH=%SCRIPT_DIR%\windows\windows-amd64\harness.exe"
    ) else if "%ARCH%"=="arm64" (
        set "OLD_BINARY_PATH=%SCRIPT_DIR%\windows\windows-arm64\harness.exe"
    ) else if "%ARCH%"=="386" (
        set "OLD_BINARY_PATH=%SCRIPT_DIR%\windows\windows-386\harness.exe"
    )
)

if exist "!OLD_BINARY_PATH!" (
    echo [java-harness] Using legacy binary path ^(deprecated^) >&2
    "!OLD_BINARY_PATH!" %*
    exit /b !ERRORLEVEL!
)

REM Fallback to JAR
set "PROJECT_ROOT=%SCRIPT_DIR%\.."
set "JAR_PATTERN=%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli-*-shaded.jar"

REM 改进 for 循环，过滤 original- 文件
for %%F in ("%JAR_PATTERN%") do (
    echo %%F | findstr /v "original-" >nul
    if !ERRORLEVEL! equ 0 (
        set "JAR_FILE=%%F"
        goto :found_jar
    )
)

:found_jar
if defined JAR_FILE (
    if exist "!JAR_FILE!" (
        echo [java-harness] Using JAR fallback ^(slower performance^) >&2
        java -jar "!JAR_FILE!" %*
        exit /b !ERRORLEVEL!
    )
)

REM Nothing found
echo [java-harness] Error: No harness binary found for %OS%-%ARCH% >&2
echo [java-harness] Expected: %BINARY_PATH% >&2
echo [java-harness] Please run: scripts\build\build-all-native.bat >&2
exit /b 1
