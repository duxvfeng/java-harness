@echo off
REM Java Harness - Windows 包装脚本 (bin/java-harness.bat)

setlocal

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM Windows 直接使用 Windows 版本的二进制文件
set "BINARY=%SCRIPT_DIR%windows\harness.exe"

REM 检查二进制文件是否存在
if not exist "%BINARY%" (
    echo {"error": "Binary not found: %BINARY%"}
    exit /b 1
)

REM 执行二进制文件，传递所有参数
"%BINARY%" %*