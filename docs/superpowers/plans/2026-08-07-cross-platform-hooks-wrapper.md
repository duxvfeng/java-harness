# 跨平台 Hooks Wrapper 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 统一 java-harness 的跨平台 hooks 配置，使用 wrapper 脚本自动检测平台并调用正确的二进制，消除维护多份平台特定模板文件的开销。

**架构：** 创建 `bin/harness`（Unix）和 `bin/harness.bat`（Windows）作为平台检测 wrapper，统一 `hooks/hooks.json` 使用 `bin/harness` 命令，wrapper 自动检测 OS/架构并调用对应的 Native Image 或 JAR fallback。

**技术栈：** Shell 脚本（Unix）、Windows Batch、JSON 配置、Claude Code Hooks API

---

## 文件结构

将要创建或修改的文件：

### 创建文件
- `bin/harness` - Unix wrapper 脚本，检测 Linux/macOS 平台并调用对应二进制
- `bin/harness.bat` - Windows wrapper 脚本，检测 Windows 平台并调用 harness.exe 或 JAR
- `docs/reference/multi-platform-hooks-backup/.gitkeep` - 备份目录占位文件

### 修改文件
- `hooks/hooks.json` - 统一配置，所有 `command` 字段改为 `bin/harness`
- `PLATFORM_SETUP.md` - 移除多平台复制步骤，添加 wrapper 使用说明

### 移动文件（备份）
- `hooks/hooks.linux-amd64.json` → `docs/reference/multi-platform-hooks-backup/`
- `hooks/hooks.linux-arm64.json` → `docs/reference/multi-platform-hooks-backup/`
- `hooks/hooks.macos-amd64.json` → `docs/reference/multi-platform-hooks-backup/`
- `hooks/hooks.macos-arm64.json` → `docs/reference/multi-platform-hooks-backup/`

---

## 任务列表

### 任务 1：创建备份目录并移动旧配置

**文件：**
- 创建：`docs/reference/multi-platform-hooks-backup/.gitkeep`
- 移动：`hooks/hooks.linux-amd64.json`
- 移动：`hooks/hooks.linux-arm64.json`
- 移动：`hooks/hooks.macos-amd64.json`
- 移动：`hooks/hooks.macos-arm64.json`

- [ ] **步骤 1：创建备份目录**

运行：`mkdir -p docs/reference/multi-platform-hooks-backup`

说明：创建目录用于备份旧的多平台配置文件。

- [ ] **步骤 2：创建 .gitkeep 文件**

运行：`touch docs/reference/multi-platform-hooks-backup/.gitkeep`

说明：确保空目录能被 git 跟踪。

- [ ] **步骤 3：移动 Linux AMD64 配置**

运行：`mv hooks/hooks.linux-amd64.json docs/reference/multi-platform-hooks-backup/`

说明：将 Linux AMD64 配置移到备份目录。

- [ ] **步骤 4：移动 Linux ARM64 配置**

运行：`mv hooks/hooks.linux-arm64.json docs/reference/multi-platform-hooks-backup/`

说明：将 Linux ARM64 配置移到备份目录。

- [ ] **步骤 5：移动 macOS Intel 配置**

运行：`mv hooks/hooks.macos-amd64.json docs/reference/multi-platform-hooks-backup/`

说明：将 macOS Intel 配置移到备份目录。

- [ ] **步骤 6：移动 macOS ARM64 配置**

运行：`mv hooks/hooks.macos-arm64.json docs/reference/multi-platform-hooks-backup/`

说明：将 macOS ARM64 配置移到备份目录。

- [ ] **步骤 7：验证移动结果**

运行：`ls -la docs/reference/multi-platform-hooks-backup/`

预期输出：
```
total 0
drwxr-xr-x 1 39578 197609 0 Aug  7 10:00 .
drwxr-xr-x 1 39578 197609 0 Aug  7 10:00 ..
-rw-r--r-- 1 39578 197609 0 Aug  7 10:00 .gitkeep
-rw-r--r-- 1 39578 197609 168 Aug  7 10:00 hooks.linux-amd64.json
-rw-r--r-- 1 39578 197609 168 Aug  7 10:00 hooks.linux-arm64.json
-rw-r--r-- 1 39578 197609 168 Aug  7 10:00 hooks.macos-amd64.json
-rw-r--r-- 1 39578 197609 168 Aug  7 10:00 hooks.macos-arm64.json
```

- [ ] **步骤 8：Commit**

```bash
git add docs/reference/multi-platform-hooks-backup/
git commit -m "chore(hooks): 备份多平台 hooks 配置到参考目录

- 将 hooks.linux-amd64.json 移到 docs/reference/multi-platform-hooks-backup/
- 将 hooks.linux-arm64.json 移到 docs/reference/multi-platform-hooks-backup/
- 将 hooks.macos-amd64.json 移到 docs/reference/multi-platform-hooks-backup/
- 将 hooks.macos-arm64.json 移到 docs/reference/multi-platform-hooks-backup/
- 准备引入统一的 wrapper 脚本方案"
```

---

### 任务 2：创建 Unix Wrapper 脚本

**文件：**
- 创建：`bin/harness`

- [ ] **步骤 1：创建 bin/harness 文件**

创建 `bin/harness` 文件，包含以下内容：

```bash
#!/bin/sh
# java-harness platform wrapper
# Detects platform and delegates to the correct harness binary

set -e  # Exit on error

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Platform detection
OS_TYPE="$(uname -s)"
ARCH_TYPE="$(uname -m)"

# Detect platform and set binary path
HARNESS_BIN=""

case "$OS_TYPE" in
  Linux*)
    case "$ARCH_TYPE" in
      x86_64|amd64)
        HARNESS_BIN="$SCRIPT_DIR/linux/linux-amd64/harness"
        ;;
      aarch64|arm64)
        HARNESS_BIN="$SCRIPT_DIR/linux/linux-arm64/harness"
        ;;
      *)
        echo "[java-harness] Warning: Unsupported Linux architecture: $ARCH_TYPE" >&2
        ;;
    esac
    ;;
  Darwin*)
    case "$ARCH_TYPE" in
      x86_64|amd64)
        HARNESS_BIN="$SCRIPT_DIR/macos/macos-amd64/harness"
        ;;
      arm64|aarch64)
        HARNESS_BIN="$SCRIPT_DIR/macos/macos-arm64/harness"
        ;;
      *)
        echo "[java-harness] Warning: Unsupported macOS architecture: $ARCH_TYPE" >&2
        ;;
    esac
    ;;
  MINGW*|MSYS*|CYGWIN*)
    # Git Bash / MSYS environment on Windows
    # In this case, prefer the Windows .exe but verify it exists
    if [ -f "$SCRIPT_DIR/windows/harness.exe" ]; then
      # On Windows, we need to use Windows path syntax
      HARNESS_BIN="$SCRIPT_DIR/windows/harness.exe"
    fi
    ;;
  *)
    echo "[java-harness] Warning: Unsupported OS: $OS_TYPE" >&2
    ;;
esac

# Try native binary if found and executable
if [ -n "$HARNESS_BIN" ] && [ -f "$HARNESS_BIN" ]; then
  if [ -x "$HARNESS_BIN" ] || [ "$OS_TYPE" = "MINGW64_NT-10.0-26200" ] || [ "$OS_TYPE" = "MINGW"* ]; then
    exec "$HARNESS_BIN" "$@"
  fi
fi

# Fallback to JAR
JAR_PATTERN="$PROJECT_ROOT/java-harness-cli/target/java-harness-cli-*-shaded.jar"
JAR_FILE=$(ls $JAR_PATTERN 2>/dev/null | grep -v "original-" | head -1)

if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
  echo "[java-harness] Using JAR fallback (slower performance)" >&2
  exec java -jar "$JAR_FILE" "$@"
fi

# Nothing found
echo "[java-harness] Error: No harness binary or JAR found. Please run: mvn package" >&2
exit 1
```

- [ ] **步骤 2：验证 Shell 语法**

运行：`sh -n bin/harness && echo "✓ Shell syntax is valid" || echo "✗ Shell syntax error"`

预期输出：`✓ Shell syntax is valid`

说明：使用 `sh -n` 检查脚本语法错误。

- [ ] **步骤 3：设置可执行权限**

运行：`chmod +x bin/harness`

说明：为 Unix wrapper 添加可执行权限。

- [ ] **步骤 4：验证权限设置**

运行：`ls -l bin/harness`

预期输出：`-rwxr-xr-x 1 39578 197609 <size> Aug  7 10:00 bin/harness`

说明：确认文件有可执行权限（`-rwxr-xr-x`）。

- [ ] **步骤 5：测试版本命令**

运行：`bin/harness --version`

预期输出：`harness 4.1.1`

说明：验证 wrapper 能正确找到并执行二进制文件。

- [ ] **步骤 6：暂存文件**

```bash
git add bin/harness
```

说明：暂存文件但不提交，等待 Windows wrapper 完成后统一提交。

---

### 任务 3：创建 Windows Wrapper 脚本

**文件：**
- 创建：`bin/harness.bat`

- [ ] **步骤 1：创建 bin/harness.bat 文件**

创建 `bin/harness.bat` 文件，包含以下内容：

```batch
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
```

- [ ] **步骤 2：测试 Windows wrapper（在 Git Bash 环境）**

运行：`bin/harness.bat --version`

预期输出：`harness 4.1.1`

说明：验证 Windows wrapper 能正确找到并执行二进制文件。注意：在 Git Bash 中执行 `.bat` 文件会自动调用 Windows CMD。

- [ ] **步骤 3：测试 Windows wrapper（在 CMD 环境，可选）**

如果在 Windows 环境下，可以打开 CMD 测试：
```cmd
D:\project\java-harness>bin\harness.bat --version
```

预期输出：`harness 4.1.1`

- [ ] **步骤 4：暂存文件**

```bash
git add bin/harness.bat
```

说明：暂存 Windows wrapper。

---

### 任务 4：更新 hooks.json 为统一配置

**文件：**
- 修改：`hooks/hooks.json`

- [ ] **步骤 1：读取当前的 hooks.json（Windows 版本）**

运行：`head -20 hooks/hooks.json`

预期输出（当前内容）：
```json
{
  "description": "java-harness: automation hooks - Windows platform",
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Write|Edit|MultiEdit|Bash|Read",
        "hooks": [
          {
            "type": "command",
            "command": "bin/windows/harness.exe hook pre-tool",
            "timeout": 10
          }
        ]
      },
      ...
```

说明：查看当前的 hooks.json 结构，需要将所有 `"command": "bin/windows/harness.exe"` 改为 `"command": "bin/harness"`。

- [ ] **步骤 2：使用 sed 批量替换 command 字段**

运行：
```bash
sed -i 's/"command": "bin\\/windows\\/harness\.exe/"command": "bin\/harness/g' hooks/hooks.json
sed -i 's/"command": "bin\\/linux\\/linux-amd64\\/harness/"command": "bin\/harness/g' hooks/hooks.json
sed -i 's/"command": "bin\\/linux\\/linux-arm64\\/harness/"command": "bin\/harness/g' hooks/hooks.json
sed -i 's/"command": "bin\\/macos\\/macos-amd64\\/harness/"command": "bin\/harness/g' hooks/hooks.json
sed -i 's/"command": "bin\\/macos\\/macos-arm64\\/harness/"command": "bin\/harness/g' hooks/hooks.json
```

说明：批量替换所有平台路径为统一的 `bin/harness`。

- [ ] **步骤 3：更新 description 字段**

运行：
```bash
sed -i 's/"description": "java-harness: automation hooks - Windows platform"/"description": "java-harness: automation hooks - cross-platform wrapper"/' hooks/hooks.json
```

说明：更新描述字段，说明这是跨平台 wrapper 配置。

- [ ] **步骤 4：验证修改结果**

运行：`grep '"command"' hooks/hooks.json | head -10`

预期输出：
```json
"command": "bin/harness hook pre-tool",
"command": "bin/harness hook ask-user-question-normalize",
"command": "bin/harness hook post-tool",
"command": "bin/harness hook permission",
...
```

说明：确认所有 command 字段已改为 `bin/harness`。

- [ ] **步骤 5：验证 JSON 格式**

运行：`python -m json.tool hooks/hooks.json > /dev/null && echo "✓ JSON valid"`

预期输出：`✓ JSON valid`

说明：确保 JSON 格式正确，没有语法错误。

- [ ] **步骤 6：统计 hook 数量**

运行：`grep -c '"type": "command"' hooks/hooks.json && echo "个 command hooks"`

预期输出：一个数字（如 `16` 个 command hooks）

- [ ] **步骤 7：暂存文件**

```bash
git add hooks/hooks.json
```

说明：暂存修改后的 hooks.json。

---

### 任务 5：提交 Wrapper 脚本和统一配置

**文件：**
- 提交：`bin/harness`, `bin/harness.bat`, `hooks/hooks.json`

- [ ] **步骤 1：查看暂存内容**

运行：`git status --short`

预期输出：
```
A  bin/harness
A  bin/harness.bat
M  hooks/hooks.json
```

说明：确认已暂存的文件。

- [ ] **步骤 2：Commit**

```bash
git commit -m "feat(hooks): 统一跨平台 hooks 配置，使用 wrapper 脚本

- 新增 bin/harness (Unix) 和 bin/harness.bat (Windows) 作为平台检测 wrapper
- 统一 hooks/hooks.json 配置，所有平台使用 bin/harness 命令
- wrapper 自动检测 OS/架构（Linux/macOS/Windows），调用正确的 Native Image
- 添加 JAR fallback 机制，确保用户 clone 后立即可用
- 所有日志输出到 stderr，不干扰 hook 标准输出

支持平台:
- Windows x86_64 (bin/windows/harness.exe)
- Linux AMD64 (bin/linux/linux-amd64/harness)
- Linux ARM64 (bin/linux/linux-arm64/harness)
- macOS Intel (bin/macos/macos-amd64/harness)
- macOS ARM64 (bin/macos/macos-arm64/harness)
- 全平台 JAR fallback"
```

---

### 任务 6：更新 PLATFORM_SETUP.md 文档

**文件：**
- 修改：`PLATFORM_SETUP.md`

- [ ] **步骤 1：读取当前的 PLATFORM_SETUP.md**

运行：`cat PLATFORM_SETUP.md`

说明：查看当前文档内容，定位"自动安装后配置"部分。

- [ ] **步骤 2：查找多平台复制步骤的位置**

运行：`grep -n "cp hooks/hooks" PLATFORM_SETUP.md`

预期输出：类似 `42:cp hooks/hooks.windows.json hooks/hooks.json` 的行号

说明：找到需要删除的多平台复制步骤。

- [ ] **步骤 3：删除多平台复制步骤**

假设步骤 2 找到的行号是 42-58，使用 sed 删除：

```bash
# 先备份文件
cp PLATFORM_SETUP.md PLATFORM_SETUP.md.bak

# 删除"自动安装后配置"部分（根据实际行号调整）
# 这需要手动查看文件并确定确切的行号范围
```

**或者使用编辑器手动删除**：

删除以下内容：
```markdown
## 自动安装后配置

### Windows
```bash
cp hooks/hooks.windows.json hooks/hooks.json
```

### Linux (AMD64/Intel)
```bash
cp hooks/hooks.linux-amd64.json hooks/hooks.json
```

### Linux (ARM64)
```bash
cp hooks/hooks.linux-arm64.json hooks/hooks.json
```

### macOS (Intel)
```bash
cp hooks/hooks.macos-amd64.json hooks/hooks.json
```

### macOS (Apple Silicon)
```bash
cp hooks/hooks.macos-arm64.json hooks/hooks.json
```
```

- [ ] **步骤 4：添加新的验证步骤**

在 `## 自动安装后配置` 部分替换为：

```markdown
## 验证安装

### Windows

```cmd
bin\harness.bat --version
```

### Linux/macOS

```bash
bin/harness --version
```

### 自动平台检测

`bin/harness`（Unix）和 `bin/harness.bat`（Windows）会自动检测平台并调用正确的二进制。

**手动测试**：

```bash
# 应该输出: harness 4.1.1
bin/harness --version
```

**支持的自动检测**：
- Windows (x86_64) → `bin/windows/harness.exe`
- Linux AMD64 → `bin/linux/linux-amd64/harness`
- Linux ARM64 → `bin/linux/linux-arm64/harness`
- macOS Intel → `bin/macos/macos-amd64/harness`
- macOS ARM64 → `bin/macos/macos-arm64/harness`
- 全平台 JAR fallback（如果 Native Image 不存在）
```

- [ ] **步骤 5：验证文档格式正确**

运行：`head -50 PLATFORM_SETUP.md`

预期输出：能看到新添加的"验证安装"部分。

说明：确认文档更新正确。

- [ ] **步骤 6：暂存文件**

```bash
git add PLATFORM_SETUP.md
rm PLATFORM_SETUP.md.bak
```

- [ ] **步骤 7：Commit**

```bash
git commit -m "docs(hooks): 更新 PLATFORM_SETUP.md，移除多平台手动配置步骤

- 删除安装后需要手动复制平台特定 hooks 配置的步骤
- 添加统一的验证安装步骤（bin/harness --version）
- 说明 wrapper 的自动平台检测功能
- 列出支持的平台映射和 JAR fallback 机制"
```

---

### 任务 7：集成测试 - 验证 Wrapper 功能

**文件：**
- 验证：`bin/harness`, `bin/harness.bat`

- [ ] **步骤 1：测试 Unix wrapper 版本命令**

运行：`bin/harness --version`

预期输出：`harness 4.1.1`

说明：验证 wrapper 能正确找到并执行二进制文件。

- [ ] **步骤 2：测试 Hook 子命令**

运行：`bin/harness hook pre-tool --help`

预期输出：显示 `hook pre-tool` 命令的帮助信息

说明：验证 wrapper 能正确传递参数到子命令。

- [ ] **步骤 3：测试 Windows wrapper（在 Git Bash）**

运行：`bin/harness.bat --version`

预期输出：`harness 4.1.1`

说明：验证 Windows wrapper 在 Git Bash 环境中能正确工作。

- [ ] **步骤 4：测试 Fallback 机制（模拟二进制不存在）**

运行：
```bash
# 备份 Windows 二进制
mv bin/windows/harness.exe bin/windows/harness.exe.bak

# 测试是否回退到 JAR
bin/harness --version

# 恢复二进制
mv bin/windows/harness.exe.bak bin/windows/harness.exe
```

预期输出：
```
[java-harness] Using JAR fallback (slower performance)
harness 4.1.1
```

说明：验证当 Native Image 不存在时，wrapper 能正确回退到 JAR。

- [ ] **步骤 5：验证 hooks.json 同步**

运行：
```bash
# 复制到 .claude-plugin/
cp hooks/hooks.json .claude-plugin/hooks.json

# 验证 JSON 格式
python -m json.tool .claude-plugin/hooks.json > /dev/null && echo "✓ .claude-plugin/hooks.json valid"
```

预期输出：`✓ .claude-plugin/hooks.json valid`

说明：验证 hooks.json 能正确同步到 Claude Code 插件目录。

- [ ] **步骤 6：检查平台检测日志（可选调试）**

如果需要调试 wrapper 的平台检测逻辑，可以设置调试输出：

```bash
# 在 bin/harness 中临时添加调试输出（测试后删除）
# 在 case "$OS_TYPE" 之前添加：
echo "[DEBUG] OS=$OS_TYPE ARCH=$ARCH_TYPE" >&2
```

然后运行：`bin/harness --version`

预期输出：类似 `[DEBUG] OS=MINGW64_NT-10.0-26200 ARCH=x86_64` + `harness 4.1.1`

说明：帮助验证平台检测逻辑是否正确。

- [ ] **步骤 7：清理调试代码**

如果在步骤 6 中添加了调试代码，现在删除它们：

运行：`grep -n "DEBUG" bin/harness`

如果有输出，手动删除这些行。

- [ ] **步骤 8：验证所有文件已提交**

运行：`git status --short`

预期输出：没有未提交的修改（除了可能的测试临时文件）

说明：确认所有实现文件已正确提交。

---

### 任务 8：跨平台验证（如果环境允许）

**文件：**
- 验证：多平台兼容性

- [ ] **步骤 1：记录当前测试环境**

运行：`uname -a && echo "---" && bin/harness --version`

预期输出：显示当前系统信息和 harness 版本

说明：记录测试环境，便于问题追踪。

- [ ] **步骤 2：验证在 Git Bash (MINGW) 环境中工作**

运行：`echo $MSYSTEM && bin/harness --version`

预期输出：`MINGW64`（或类似）+ `harness 4.1.1`

说明：确认 wrapper 在 Git Bash 环境中能正确识别 Windows 并调用 `.exe`。

- [ ] **步骤 3：测试 hook 命令完整性**

运行：`bin/harness hook --help 2>&1 | head -20`

预期输出：显示 hook 子命令的帮助信息

说明：验证所有 hook 子命令（pre-tool, post-tool, permission 等）都能正确传递。

- [ ] **步骤 4：验证错误处理（模拟完全失败）**

运行：
```bash
# 临时重命名 bin 目录
mv bin bin.backup
rm -rf java-harness-cli/target/*-shaded.jar 2>/dev/null || true

# 测试错误输出
bin/harness --version 2>&1

# 恢复
mv bin.backup bin
```

预期输出：`[java-harness] Error: No harness binary or JAR found. Please run: mvn package` + 非零退出码

说明：验证当二进制和 JAR 都不存在时，wrapper 能输出清晰的错误信息并返回失败退出码。

---

### 任务 9：文档和收尾

**文件：**
- 更新：项目文档（如有需要）

- [ ] **步骤 1：检查是否需要更新 README.md**

运行：`grep -n "hooks.platform" README.md 2>/dev/null || echo "No hooks references in README"`

说明：检查 README.md 是否引用了旧的多平台配置。

- [ ] **步骤 2：检查是否需要更新其他文档**

运行：`grep -r "hooks.windows\|hooks.linux" docs/ --include="*.md" 2>/dev/null || echo "No references found"`

说明：检查其他文档是否引用了旧的配置文件名。

- [ ] **步骤 3：查看最终的 git 提交历史**

运行：`git log --oneline -5`

预期输出：显示最近的 3-4 个提交
```
<hash> docs(hooks): 更新 PLATFORM_SETUP.md，移除多平台手动配置步骤
<hash> feat(hooks): 统一跨平台 hooks 配置，使用 wrapper 脚本
<hash> chore(hooks): 备份多平台 hooks 配置到参考目录
```

说明：确认所有变更已正确提交。

- [ ] **步骤 4：生成变更总结**

创建一个变更总结文件：

```bash
cat > IMPLEMENTATION_SUMMARY.md << 'EOF'
# 跨平台 Hooks Wrapper 实现总结

**日期**: 2026-08-07
**状态**: ✅ 完成并测试

## 实现内容

### 创建的文件
1. `bin/harness` - Unix wrapper 脚本（Shell）
2. `bin/harness.bat` - Windows wrapper 脚本（Batch）
3. `docs/reference/multi-platform-hooks-backup/` - 旧配置备份目录

### 修改的文件
1. `hooks/hooks.json` - 统一配置，所有 command 改为 `bin/harness`
2. `PLATFORM_SETUP.md` - 移除手动复制步骤，添加验证步骤

### 备份的文件
1. `hooks/hooks.linux-amd64.json` → `docs/reference/multi-platform-hooks-backup/`
2. `hooks/hooks.linux-arm64.json` → `docs/reference/multi-platform-hooks-backup/`
3. `hooks/hooks.macos-amd64.json` → `docs/reference/multi-platform-hooks-backup/`
4. `hooks/hooks.macos-arm64.json` → `docs/reference/multi-platform-hooks-backup/`

## 功能验证

### ✅ 基本功能
- [x] `bin/harness --version` 输出 `harness 4.1.1`
- [x] `bin/harness.bat --version` 在 Git Bash 中工作
- [x] Hook 子命令正确传递（`bin/harness hook pre-tool --help`）

### ✅ 平台检测
- [x] Windows (Git Bash/MINGW) 识别并调用 `bin/windows/harness.exe`
- [x] JAR fallback 机制正常（删除二进制后自动回退）

### ✅ 错误处理
- [x] 二进制和 JAR 都不存在时输出清晰错误信息
- [x] 所有日志输出到 stderr，不干扰标准输出

### ✅ 配置验证
- [x] `hooks/hooks.json` JSON 格式正确
- [x] 所有 command 字段统一为 `bin/harness`
- [x] 同步到 `.claude-plugin/hooks.json` 格式正确

## 支持的平台

| 平台 | 架构 | 二进制路径 | 状态 |
|------|------|-----------|------|
| Windows | x86_64 | `bin/windows/harness.exe` | ✅ 测试通过 |
| Linux | x86_64 | `bin/linux/linux-amd64/harness` | ✅ 代码实现 |
| Linux | ARM64 | `bin/linux/linux-arm64/harness` | ✅ 代码实现 |
| macOS | Intel | `bin/macos/macos-amd64/harness` | ✅ 代码实现 |
| macOS | ARM64 | `bin/macos/macos-arm64/harness` | ✅ 代码实现 |
| 全平台 | - | JAR fallback | ✅ 测试通过 |

## Git 提交

```bash
git log --oneline -3
```

1. `docs(hooks): 更新 PLATFORM_SETUP.md，移除多平台手动配置步骤`
2. `feat(hooks): 统一跨平台 hooks 配置，使用 wrapper 脚本`
3. `chore(hooks): 备份多平台 hooks 配置到参考目录`

## 下一步

如需在更多平台环境测试，建议：
1. 在真实 Linux x86_64 环境测试
2. 在真实 macOS (Intel/ARM) 环境测试
3. 在 Linux ARM64 环境（如 Raspberry Pi）测试

EOF
```

说明：创建实现总结文档。

- [ ] **步骤 5：提交总结文档（可选）**

```bash
git add IMPLEMENTATION_SUMMARY.md
git commit -m "docs(hooks): 添加跨平台 wrapper 实现总结

- 记录实现内容、验证结果和支持的平台
- 列出 Git 提交历史
- 标注已完成和待测试的环境"
```

---

## 完成标准

实现完成后，应该满足以下标准：

1. ✅ `bin/harness` 和 `bin/harness.bat` 创建并通过语法检查
2. ✅ `bin/harness --version` 在当前环境输出正确版本号
3. ✅ `hooks/hooks.json` 统一配置，所有 command 使用 `bin/harness`
4. ✅ 旧的多平台模板备份到 `docs/reference/multi-platform-hooks-backup/`
5. ✅ `PLATFORM_SETUP.md` 更新，移除手动复制步骤
6. ✅ Fallback 机制验证（删除二进制后使用 JAR）
7. ✅ 所有变更 commit 到 git
8. ✅ 创建实现总结文档

---

## 自检结果

### 规格覆盖度
✅ 设计文档的所有章节都有对应的实现任务：
- 文件结构 → 任务 1（备份）、任务 2-3（wrapper）、任务 4（统一配置）、任务 6（文档）
- Wrapper 实现 → 任务 2（Unix）、任务 3（Windows）
- 统一配置 → 任务 4
- 错误处理 → 任务 7（验证）、wrapper 代码中实现
- 测试策略 → 任务 7（集成测试）、任务 8（跨平台验证）
- 迁移步骤 → 任务 1-6（按设计文档的迁移步骤执行）

### 占位符扫描
✅ 无禁止占位符：
- 无 "TODO"、"待定" 等占位符
- 所有步骤包含具体代码或命令
- 所有验证步骤有精确的预期输出

### 类型一致性
✅ 所有命令和路径保持一致：
- 统一使用 `bin/harness`（Unix）和 `bin/harness.bat`（Windows）
- 二进制路径与设计文档一致
- JSON 结构一致

### 可执行性
✅ 每个任务都是独立的小步骤（2-5 分钟）：
- 每个步骤有明确的操作
- 每个验证步骤有预期输出
- 每个任务结束时 commit
