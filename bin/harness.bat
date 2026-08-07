@echo off
REM java-harness platform wrapper for Windows
REM Detects platform and delegates to the correct harness binary

setlocal enabledelayedexpansion

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM Get project root (parent of bin directory)
for %%i in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fi"

REM Try Windows native binary first
set "HARNESS_BIN=%SCRIPT_DIR%\windows\harness.exe"
if exist "%HARNESS_BIN%" (
  "%HARNESS_BIN%" %*
  set "EXIT_CODE=!ERRORLEVEL!"
  endlocal
  exit /b !EXIT_CODE!
)

REM Fallback to JAR
REM Use dir to find the JAR, excluding original- files
for /f "delims=" %%f in ('dir /b /s "%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli*-shaded.jar" 2^>nul ^| findstr /v "original-"') do (
  set "JAR_FILE=%%f"
  goto :jar_found
)

REM Try alternative: just check target directory
if exist "%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli*-shaded.jar" (
  for %%f in ("%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli*-shaded.jar") do (
    set "JAR_FILE=%%f"
    goto :jar_found
  )
)

:jar_found
if defined JAR_FILE (
  echo [java-harness] Using JAR fallback ^(slower performance^) >&2
  java -jar "!JAR_FILE!" %*
  set "EXIT_CODE=!ERRORLEVEL!"
  endlocal
  exit /b !EXIT_CODE!
)

REM Nothing found
echo [java-harness] Error: No harness binary or JAR found. Please run: mvn package >&2
endlocal
exit /b 1
