@echo off
REM Java Harness - 一键安装脚本 (Windows)
REM 适用于 Windows CMD/PowerShell

setlocal enabledelayedexpansion

echo ========================================
echo   Java Harness 插件安装程序
echo ========================================
echo.

REM 配置
set REPO_URL=https://gitee.com/duxvfeng/java-harness.git
set PLUGIN_DIR=%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace
set TEMP_DIR=%TEMP%\java-harness-install

echo [INFO] 插件将安装到：%PLUGIN_DIR%
echo [INFO] 从以下地址下载：%REPO_URL%
echo.

REM 检查 Java
echo [INFO] 检查 Java 环境...
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请先安装 Java 17 或更高版本
    pause
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1') do (
    set JAVA_VERSION=%%i
    goto :got_version
)
:got_version
echo [OK] 发现 Java 版本：%JAVA_VERSION%
echo.

REM 检查 git
echo [INFO] 检查 git...
where git >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 git，请先安装 git
    pause
    exit /b 1
)
echo [OK] git 已安装
echo.

REM 清理并创建临时目录
echo [INFO] 创建临时目录...
if exist "%TEMP_DIR%" rd /s /q "%TEMP_DIR%"
mkdir "%TEMP_DIR%"

REM 克隆仓库
echo [INFO] 克隆仓库...
git clone --depth 1 %REPO_URL% "%TEMP_DIR%"
if errorlevel 1 (
    echo [ERROR] 克隆失败，请检查网络连接或仓库地址
    pause
    exit /b 1
)

REM 安装插件
echo [INFO] 安装插件...
if exist "%PLUGIN_DIR%" rd /s /q "%PLUGIN_DIR%"
mkdir "%PLUGIN_DIR%"

REM 复制文件
xcopy "%TEMP_DIR%\.claude-plugin" "%PLUGIN_DIR%\.claude-plugin\" /E /I /Y
xcopy "%TEMP_DIR%\bin" "%PLUGIN_DIR%\bin\" /E /I /Y
if exist "%TEMP_DIR%\VERSION" copy "%TEMP_DIR%\VERSION" "%PLUGIN_DIR%\" >nul
if exist "%TEMP_DIR%\README.md" copy "%TEMP_DIR%\README.md" "%PLUGIN_DIR%\" >nul

REM 设置执行权限
echo [INFO] 设置执行权限...
copy "%TEMP_DIR%\bin\harness.bat" "%PLUGIN_DIR%\bin\" >nul

REM 清理
echo [INFO] 清理临时文件...
if exist "%TEMP_DIR%" rd /s /q "%TEMP_DIR%"

REM 验证
echo [INFO] 验证安装...
if exist "%PLUGIN_DIR%\bin\harness.jar" (
    echo [OK] 插件文件安装成功！
) else (
    echo [ERROR] 插件文件缺失，安装可能失败
    pause
    exit /b 1
)

REM 测试
echo [INFO] 测试插件启动...
"%PLUGIN_DIR%\bin\harness.bat" --version >nul 2>&1
if errorlevel 1 (
    echo [WARNING] 插件启动测试失败，但文件已安装
) else (
    echo [OK] 插件启动成功！
)

echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo [INFO] 安装位置：%PLUGIN_DIR%
echo.
echo [INFO] 使用方式：
echo    直接调用：
echo    %PLUGIN_DIR%\bin\harness.bat --version
echo    %PLUGIN_DIR%\bin\harness.bat --help
echo.
echo [INFO] 下一步：
echo    1. 在 Claude Code 中运行：/reload-plugins
echo    2. 查看插件列表：/plugin list
echo    3. 查看完整文档：%PLUGIN_DIR%\README.md
echo.
echo [TIP] 如果遇到问题，请检查：
echo    - Java 版本是否 ≥ 17
echo    - 网络连接是否正常
echo    - 插件目录权限是否正确
echo.
pause
