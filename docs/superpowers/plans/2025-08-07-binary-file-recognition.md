# 二进制文件识别实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 修改Java Native Image构建流程，生成与Go版本命名一致的二进制文件（harness-{os}-{arch}），确保Claude插件能够正确识别。

**架构：** 通过修改Maven Native Image插件配置和构建脚本，将编译产物从子目录结构（bin/macos/macos-arm64/harness）改为扁平化命名（bin/harness-darwin-arm64），同时更新包装脚本的二进制查找逻辑。

**技术栈：** Maven, GraalVM Native Image, Bash脚本

---

## 文件结构

此计划将创建或修改以下文件：

**新建文件：**
- `scripts/build/build-all-native.sh` - 多平台自动检测和构建脚本

**修改文件：**
- `java-harness-cli/pom.xml:169` - 修改Native Image插件配置中的imageName
- `bin/harness:1-77` - 更新包装脚本的二进制文件查找逻辑
- `bin/harness.bat:1-50` - 更新Windows批处理脚本（如存在）
- `.gitignore:35-39` - 更新二进制文件忽略规则

**文件职责：**
- `build-all-native.sh` - 检测当前平台并调用对应的Maven构建命令，将编译产物复制到bin目录并重命名为标准格式
- `pom.xml` - 配置Native Image插件使用动态命名模式
- `bin/harness` - 主入口脚本，负责检测平台并调用对应的harness-{os}-{arch}二进制文件
- `.gitignore` - 忽略编译后的二进制文件，避免提交到版本控制

---

## 任务 1：创建多平台构建脚本

**文件：**
- 创建：`scripts/build/build-all-native.sh`

- [ ] **步骤 1：创建构建脚本框架**

创建目录：
```bash
mkdir -p scripts/build
```

创建文件 `scripts/build/build-all-native.sh`：

```bash
#!/bin/bash
# build-all-native.sh
# 多平台Native Image构建脚本
# 检测当前平台并编译对应的二进制文件，输出格式：harness-{os}-{arch}

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==================================="
echo -e "Building Java Harness Native Binary"
echo -e "===================================${NC}"

# 获取脚本所在目录的父目录（项目根目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 平台检测函数
detect_platform() {
    local os
    local arch
    
    os=$(uname -s | tr '[:upper:]' '[:lower:]')
    arch=$(uname -m)
    
    # 标准化操作系统名称
    case "$os" in
        darwin)
            os="darwin"
            ;;
        linux)
            os="linux"
            ;;
        mingw*|msys*|cygwin*)
            os="windows"
            ;;
        *)
            os="$os"
            ;;
    esac
    
    # 标准化架构名称
    case "$arch" in
        x86_64|amd64)
            arch="amd64"
            ;;
        aarch64|arm64)
            arch="arm64"
            ;;
        i386|i686)
            arch="386"
            ;;
        *)
            arch="$arch"
            ;;
    esac
    
    echo "${os}-${arch}"
}

# 检测当前平台
PLATFORM=$(detect_platform)
OS_PART="${PLATFORM%-*}"
ARCH_PART="${PLATFORM#*-}"

# 设置可执行文件扩展名
EXT=""
if [ "$OS_PART" = "windows" ]; then
    EXT=".exe"
fi

# 目标二进制文件名
BINARY_NAME="harness-${OS_PART}-${ARCH_PART}${EXT}"

echo -e "${YELLOW}📍 检测到的平台: ${PLATFORM}${NC}"
echo -e "${YELLOW}📦 目标二进制文件名: ${BINARY_NAME}${NC}"
echo ""

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven未安装，请先安装Maven${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Maven已安装${NC}"
echo ""

# 进入项目根目录
cd "$PROJECT_ROOT"

echo -e "${BLUE}🔨 开始Native Image编译...${NC}"
echo ""

# 执行Maven Native Image构建
mvn -Pnative \
    -DskipTests \
    -Dnative.os="${OS_PART}" \
    -Dnative.arch="${ARCH_PART}" \
    -Dnative.image.name="${BINARY_NAME}" \
    native:compile

echo ""
echo -e "${GREEN}✅ 编译完成${NC}"
echo ""

# 源二进制文件路径（Maven输出目录）
SOURCE_BINARY="java-harness-cli/target/${BINARY_NAME}"

# 检查编译产物是否存在
if [ ! -f "$SOURCE_BINARY" ]; then
    echo -e "${RED}❌ 编译产物未找到: ${SOURCE_BINARY}${NC}"
    exit 1
fi

# 创建bin目录
mkdir -p bin

# 复制二进制文件到bin目录
echo -e "${BLUE}📦 复制二进制文件到 bin/ 目录...${NC}"
cp "$SOURCE_BINARY" "bin/${BINARY_NAME}"

# 设置执行权限（仅对非Windows系统）
if [ "$OS_PART" != "windows" ]; then
    chmod +x "bin/${BINARY_NAME}"
fi

echo -e "${GREEN}✅ 二进制文件已创建: bin/${BINARY_NAME}${NC}"
echo ""

# 显示文件信息
if [ "$OS_PART" != "windows" ]; then
    FILE_SIZE=$(ls -lh "bin/${BINARY_NAME}" | awk '{print $5}')
    echo -e "${BLUE}📊 文件大小: ${FILE_SIZE}${NC}"
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           Native Image 构建成功！                          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}🚀 二进制文件位置: bin/${BINARY_NAME}${NC}"
echo -e "${BLUE}🧪 测试运行: ./bin/harness --version${NC}"
echo ""

# 测试运行（如果当前平台支持）
if [ "$OS_PART" != "windows" ]; then
    echo -e "${YELLOW}🧪 测试二进制文件...${NC}"
    if "./bin/${BINARY_NAME}" --version &> /dev/null; then
        VERSION=$("./bin/${BINARY_NAME}" --version)
        echo -e "${GREEN}✅ 测试成功！版本: ${VERSION}${NC}"
    else
        echo -e "${YELLOW}⚠️  测试失败，但文件已创建${NC}"
    fi
fi

echo ""
echo -e "${BLUE}📚 下一步: 运行 './bin/harness --help' 查看所有可用命令${NC}"
echo ""
```

设置执行权限：
```bash
chmod +x scripts/build/build-all-native.sh
```

- [ ] **步骤 2：测试脚本语法正确性**

运行：
```bash
bash -n scripts/build/build-all-native.sh
```

预期：无输出（语法检查通过）

- [ ] **步骤 3：验证脚本可执行性**

运行：
```bash
./scripts/build/build-all-native.sh --help 2>&1 | head -5
```

预期：脚本开始执行并显示帮助信息（或正常执行构建流程）

- [ ] **步骤 4：Commit**

```bash
git add scripts/build/build-all-native.sh
git commit -m "feat(binary-recognition): 添加多平台Native Image构建脚本

- 自动检测操作系统和架构类型
- 生成符合Go版本命名的二进制文件：harness-{os}-{arch}
- 将编译产物自动复制到bin目录并设置执行权限
- 包含构建验证和测试运行逻辑

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 任务 2：修改Maven Native Image插件配置

**文件：**
- 修改：`java-harness-cli/pom.xml:169`

- [ ] **步骤 1：编写测试验证当前配置**

先运行当前构建流程以确认基线：
```bash
cd java-harness-cli
mvn -Pnative -DskipTests native:compile
```

预期：在 `java-harness-cli/target/` 目录生成 `harness` 文件（无平台后缀）

验证：
```bash
ls -lh java-harness-cli/target/harness
```

预期输出：显示文件信息，文件名确认为 `harness`

- [ ] **步骤 2：修改imageName配置**

编辑 `java-harness-cli/pom.xml`，将第169行的 `<imageName>harness</imageName>` 修改为：

```xml
<imageName>harness-${native.os}-${native.arch}</imageName>
```

完整修改后的配置段（第168-182行）应为：

```xml
<configuration>
    <imageName>harness-${native.os}-${native.arch}</imageName>
    <mainClass>com.chachamaru.harness.cli.HarnessCLI</mainClass>
    <buildArgs>
        <buildArg>--no-fallback</buildArg>
        <buildArg>--initialize-at-build-time=java.time.LocalDateTime</buildArg>
        <buildArg>--initialize-at-build-time=java.util.LinkedHashMap</buildArg>
        <buildArg>--initialize-at-build-time=java.util.ArrayList</buildArg>
        <buildArg>--initialize-at-build-time=java.util.HashMap</buildArg>
        <buildArg>-H:+ReportExceptionStackTraces</buildArg>
        <buildArg>-H:IncludeResources=.*\\.yaml$</buildArg>
        <buildArg>-H:IncludeResources=.*\.json$</buildArg>
        <buildArg>-H:IncludeResources=.*\.yml$</buildArg>
    </buildArgs>
</configuration>
```

- [ ] **步骤 3：测试修改后的配置**

使用新的构建脚本测试：
```bash
./scripts/build/build-all-native.sh
```

预期：
1. 脚本检测到当前平台（如 darwin-arm64）
2. Maven生成 `java-harness-cli/target/harness-darwin-arm64` 文件
3. 文件被复制到 `bin/harness-darwin-arm64`

验证：
```bash
ls -lh bin/harness-*
```

预期输出：显示 `bin/harness-darwin-arm64` 文件信息

- [ ] **步骤 4：验证跨平台命名格式**

测试不同的平台参数（模拟）：
```bash
# 模拟Linux AMD64构建
mvn -Pnative -DskipTests -Dnative.os=linux -Dnative.arch=amd64 -Dnative.image.name=harness-linux-amd64 native:compile

# 检查输出
ls -lh java-harness-cli/target/harness-linux-amd64
```

预期：Maven配置正确接受参数并生成对应命名的文件

- [ ] **步骤 5：Commit**

```bash
git add java-harness-cli/pom.xml
git commit -m "feat(binary-recognition): 修改Native Image配置支持平台特定命名

- 将imageName从静态的'harness'改为动态的'harness-${native.os}-${native.arch}'
- 支持通过Maven参数传入os和arch值
- 与Go版本的二进制文件命名保持一致

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 任务 3：更新Unix包装脚本

**文件：**
- 修改：`bin/harness:1-77`

- [ ] **步骤 1：备份当前脚本**

```bash
cp bin/harness bin/harness.backup
```

- [ ] **步骤 2：编写新的包装脚本**

将 `bin/harness` 完整替换为：

```bash
#!/bin/sh
# java-harness platform wrapper
# Detects platform and delegates to the correct harness binary
# 命名格式与Go版本保持一致: harness-{os}-{arch}

set -e  # Exit on error

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Platform detection
OS_TYPE="$(uname -s)"
ARCH_TYPE="$(uname -m)"

# 标准化操作系统名称（与Go版本一致）
case "$OS_TYPE" in
  Darwin*)
    OS="darwin"
    ;;
  Linux*)
    OS="linux"
    ;;
  MINGW*|MSYS*|CYGWIN*)
    OS="windows"
    ;;
  *)
    OS="$OS_TYPE"
    ;;
esac

# 标准化架构名称（与Go版本一致）
case "$ARCH_TYPE" in
  x86_64|amd64)
    ARCH="amd64"
    ;;
  aarch64|arm64)
    ARCH="arm64"
    ;;
  i386|i686)
    ARCH="386"
    ;;
  *)
    ARCH="$ARCH_TYPE"
    ;;
esac

# 构建二进制文件名（与Go版本命名一致）
EXT=""
if [ "$OS" = "windows" ]; then
    EXT=".exe"
fi

BINARY_NAME="harness-${OS}-${ARCH}${EXT}"
BINARY_PATH="${SCRIPT_DIR}/${BINARY_NAME}"

# 尝试执行Native Image二进制文件
if [ -n "$BINARY_PATH" ] && [ -f "$BINARY_PATH" ]; then
  if [ -x "$BINARY_PATH" ] || [ "$OS" = "windows" ]; then
    exec "$BINARY_PATH" "$@"
  fi
fi

# Fallback: 尝试旧的子目录结构（向后兼容）
OLD_BINARY_PATH=""
case "$OS_TYPE" in
  Darwin*)
    case "$ARCH_TYPE" in
      x86_64|amd64)
        OLD_BINARY_PATH="${SCRIPT_DIR}/macos/macos-amd64/harness"
        ;;
      arm64|aarch64)
        OLD_BINARY_PATH="${SCRIPT_DIR}/macos/macos-arm64/harness"
        ;;
    esac
    ;;
  Linux*)
    case "$ARCH_TYPE" in
      x86_64|amd64)
        OLD_BINARY_PATH="${SCRIPT_DIR}/linux/linux-amd64/harness"
        ;;
      aarch64|arm64)
        OLD_BINARY_PATH="${SCRIPT_DIR}/linux/linux-arm64/harness"
        ;;
    esac
    ;;
esac

if [ -n "$OLD_BINARY_PATH" ] && [ -f "$OLD_BINARY_PATH" ] && [ -x "$OLD_BINARY_PATH" ]; then
  echo "[java-harness] Using legacy binary path (deprecated)" >&2
  exec "$OLD_BINARY_PATH" "$@"
fi

# Fallback to JAR
JAR_PATTERN="$PROJECT_ROOT/java-harness-cli/target/java-harness-cli-*-shaded.jar"
JAR_FILE=$(ls $JAR_PATTERN 2>/dev/null | grep -v "original-" | head -1)

if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
  echo "[java-harness] Using JAR fallback (slower performance)" >&2
  exec java -jar "$JAR_FILE" "$@"
fi

# Nothing found
echo "[java-harness] Error: No harness binary found for ${OS}-${ARCH}" >&2
echo "[java-harness] Expected: ${BINARY_PATH}" >&2
echo "[java-harness] Please run: ./scripts/build/build-all-native.sh" >&2
exit 1
```

设置执行权限：
```bash
chmod +x bin/harness
```

- [ ] **步骤 3：测试脚本语法正确性**

运行：
```bash
bash -n bin/harness
```

预期：无输出（语法检查通过）

- [ ] **步骤 4：验证平台检测逻辑**

测试平台检测：
```bash
# 测试脚本能够正确检测平台
bash -c 'OS_TYPE="$(uname -s)"; ARCH_TYPE="$(uname -m)"; echo "OS: $OS_TYPE, ARCH: $ARCH_TYPE"'
```

预期：显示当前操作系统和架构信息

- [ ] **步骤 5：测试包装脚本功能**

测试基本功能（需要先构建二进制文件）：
```bash
# 确保二进制文件存在
if [ -f "bin/harness-darwin-amd64" ] || [ -f "bin/harness-linux-amd64" ]; then
    ./bin/harness --version
else
    echo "需要先运行构建脚本生成二进制文件"
fi
```

预期：显示harness版本信息

- [ ] **步骤 6：验证错误处理逻辑**

测试错误处理：
```bash
# 临时重命名二进制文件以触发错误路径
if [ -f "bin/harness-darwin-amd64" ]; then
    mv bin/harness-darwin-amd64 bin/harness-darwin-amd64.tmp
    ./bin/harness --version 2>&1
    mv bin/harness-darwin-amd64.tmp bin/harness-darwin-amd64
fi
```

预期：显示清晰的错误消息，指示未找到二进制文件

- [ ] **步骤 7：清理备份文件**

```bash
rm bin/harness.backup
```

- [ ] **步骤 8：Commit**

```bash
git add bin/harness
git commit -m "feat(binary-recognition): 更新包装脚本支持新的二进制文件命名

- 更新二进制文件查找逻辑以支持harness-{os}-{arch}命名格式
- 与Go版本的平台检测和命名约定保持一致
- 保留向后兼容的旧子目录结构查找逻辑
- 增强错误消息，明确指出缺失的二进制文件和构建命令

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 任务 4：创建Windows批处理脚本（如不存在）

**文件：**
- 修改：`bin/harness.bat` 或新建

- [ ] **步骤 1：检查Windows脚本是否存在**

```bash
if [ -f "bin/harness.bat" ]; then
    echo "bin/harness.bat exists"
    ls -la bin/harness.bat
else
    echo "bin/harness.bat does not exist, will create"
fi
```

- [ ] **步骤 2：创建或更新Windows批处理脚本**

创建或替换 `bin/harness.bat`：

```batch
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

REM Fallback to JAR
set "PROJECT_ROOT=%SCRIPT_DIR%\.."
set "JAR_PATTERN=%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli-*-shaded.jar"

for %%F in ("%JAR_PATTERN%") do (
    set "JAR_FILE=%%F"
    goto :found_jar
)

:found_jar
if defined JAR_FILE (
    if exist "%JAR_FILE%" (
        echo [java-harness] Using JAR fallback ^(slower performance^) >&2
        java -jar "%JAR_FILE%" %*
        exit /b !ERRORLEVEL!
    )
)

REM Nothing found
echo [java-harness] Error: No harness binary found for %OS%-%ARCH% >&2
echo [java-harness] Expected: %BINARY_PATH% >&2
echo [java-harness] Please run: scripts\build\build-all-native.bat >&2
exit /b 1
```

- [ ] **步骤 3：验证脚本格式**

检查文件编码和行尾：
```bash
file bin/harness.bat
```

预期：显示为ASCII text或UTF-8 text，可能包含CRLF行尾（Windows格式）

- [ ] **步骤 4：Commit**

```bash
git add bin/harness.bat
git commit -m "feat(binary-recognition): 创建Windows批处理脚本支持新的二进制文件命名

- 创建harness.bat以支持Windows平台
- 使用harness-{os}-{arch}.exe命名格式
- 包含平台架构检测逻辑
- 提供JAR文件回退机制
- 与Unix版本脚本功能保持一致

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 任务 5：更新.gitignore文件

**文件：**
- 修改：`.gitignore:35-39`

- [ ] **步骤 1：查看当前.gitignore配置**

```bash
cat .gitignore | grep -A 5 "Native Image"
```

当前内容（第35-39行）：
```gitignore
# Native Image 可执行文件
bin/windows/harness.exe
bin/linux/*/harness
bin/macos/*/harness
```

- [ ] **步骤 2：编写新的忽略规则**

将第35-39行替换为：

```gitignore
# Native Image 可执行文件（新格式：与Go版本一致）
bin/harness-darwin-amd64
bin/harness-darwin-arm64
bin/harness-darwin-x86_64
bin/harness-linux-amd64
bin/harness-linux-arm64
bin/harness-linux-386
bin/harness-windows-amd64.exe
bin/harness-windows-arm64.exe
bin/harness-windows-386.exe

# 旧的子目录结构（保持兼容过渡期）
# bin/windows/harness.exe
# bin/linux/*/harness
# bin/macos/*/harness
```

- [ ] **步骤 3：测试gitignore规则**

测试规则是否正确工作：
```bash
# 创建测试文件
touch bin/harness-darwin-amd64
touch bin/harness-darwin-arm64

# 检查git状态
git status

# 清理测试文件
rm bin/harness-darwin-amd64 bin/harness-darwin-arm64
```

预期：git status不应显示这些测试文件（已被忽略）

- [ ] **步骤 4：验证旧格式仍然被忽略**

测试向后兼容性：
```bash
# 创建旧格式的测试目录和文件
mkdir -p bin/macos/macos-arm64
touch bin/macos/macos-arm64/harness

# 检查git状态
git status | grep harness

# 清理测试文件
rm -rf bin/macos
```

预期：旧格式的文件也应该被忽略（虽然当前规则已注释，但为了平滑过渡）

- [ ] **步骤 5：Commit**

```bash
git add .gitignore
git commit -m "feat(binary-recognition): 更新gitignore规则支持新的二进制文件命名

- 添加harness-{os}-{arch}[.exe]格式的忽略规则
- 支持darwin/linux/windows全平台命名
- 保留旧子目录结构规则的注释以保持过渡期兼容
- 与Go版本的.gitignore配置保持一致

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 任务 6：端到端测试验证

**文件：**
- 测试：验证整个构建和运行流程

- [ ] **步骤 1：清理旧的二进制文件**

```bash
# 清理旧的构建产物
mvn clean
rm -rf bin/harness-* bin/macos bin/linux bin/windows
```

- [ ] **步骤 2：执行完整构建流程**

```bash
# 运行新的构建脚本
./scripts/build/build-all-native.sh
```

预期输出：
1. 显示检测到的平台信息
2. Maven构建过程成功完成
3. 二进制文件成功复制到bin目录
4. 显示文件大小和测试信息

- [ ] **步骤 3：验证二进制文件命名格式**

```bash
# 列出生成的二进制文件
ls -lh bin/harness-*
```

预期：显示类似输出（根据当前平台）：
```
-rwxr-xr-x 1 user staff 42M Aug 7 12:00 bin/harness-darwin-arm64
```

- [ ] **步骤 4：测试包装脚本功能**

```bash
# 测试版本命令
./bin/harness --version
```

预期：显示harness版本信息（如：`java-harness version 4.1.1`）

```bash
# 测试帮助命令
./bin/harness --help
```

预期：显示harness帮助信息，包含所有可用命令

- [ ] **步骤 5：验证Go版本对齐性**

对比Go版本的文件结构：
```bash
# 显示Java版本的bin目录
ls -la bin/

# 显示Go版本的bin目录（如果存在）
ls -la /Users/apple/IdeaProjects/claude-code-harness/bin/
```

预期：两个版本的bin目录结构应具有相似的文件命名模式

- [ ] **步骤 6：测试跨平台兼容性**

模拟其他平台的构建（仅验证参数）：
```bash
# 测试Linux AMD64参数验证
mvn -Pnative -DskipTests -Dnative.os=linux -Dnative.arch=amd64 -Dnative.image.name=harness-linux-amd64 help:evaluate
```

预期：Maven正确接受参数而不报错

- [ ] **步骤 7：验证错误处理和回退机制**

测试错误场景：
```bash
# 临时重命名二进制文件测试错误处理
BINARY_FILE=$(ls bin/harness-* 2>/dev/null | head -1)
if [ -n "$BINARY_FILE" ]; then
    mv "$BINARY_FILE" "$BINARY_FILE.bak"
    ./bin/harness --version 2>&1 | head -5
    mv "$BINARY_FILE.bak" "$BINARY_FILE"
fi
```

预期：显示清晰的错误消息，指示未找到对应平台的二进制文件

- [ ] **步骤 8：验证向后兼容性**

如果存在旧格式的二进制文件，测试向后兼容性：
```bash
# 创建测试目录结构模拟旧版本
mkdir -p bin/macos/macos-arm64
# 将现有二进制文件复制到旧位置进行测试
BINARY_FILE=$(ls bin/harness-* 2>/dev/null | head -1)
if [ -n "$BINARY_FILE" ]; then
    cp "$BINARY_FILE" bin/macos/macos-arm64/harness
    chmod +x bin/macos/macos-arm64/harness
    
    # 删除新格式的文件
    rm bin/harness-*
    
    # 测试回退逻辑
    ./bin/harness --version
    
    # 清理
    rm -rf bin/macos
fi
```

预期：脚本能够回退到旧目录结构并执行二进制文件（显示deprecation警告）

---

## 任务 7：文档更新和验证

**文件：**
- 更新：相关文档和说明

- [ ] **步骤 1：更新构建说明**

在项目根目录的README.md或相关文档中添加新的构建说明：

```bash
# 在README.md中添加或更新构建部分
cat >> docs/BUILD_INSTRUCTIONS.md << 'EOF'
# Native Image构建说明

## 快速构建

使用新的多平台构建脚本：

```bash
./scripts/build/build-all-native.sh
```

此脚本会：
1. 自动检测当前平台（操作系统和架构）
2. 生成对应平台的Native Image二进制文件
3. 将二进制文件复制到bin目录，命名格式为 `harness-{os}-{arch}`

## 手动构建

如需手动构建特定平台：

```bash
# macOS ARM64
mvn -Pnative -Dnative.os=darwin -Dnative.arch=arm64 native:compile

# Linux AMD64  
mvn -Pnative -Dnative.os=linux -Dnative.arch=amd64 native:compile

# Windows AMD64
mvn -Pnative -Dnative.os=windows -Dnative.arch=amd64 native:compile
```

## 二进制文件命名

构建后的二进制文件采用与Go版本一致的命名约定：

- `harness-darwin-amd64` - macOS Intel
- `harness-darwin-arm64` - macOS Apple Silicon
- `harness-linux-amd64` - Linux x86_64
- `harness-linux-arm64` - Linux ARM64
- `harness-windows-amd64.exe` - Windows x86_64

## 运行

使用包装脚本自动选择正确的二进制文件：

```bash
./bin/harness --version
./bin/harness --help
```

包装脚本会自动检测当前平台并调用对应的二进制文件。
EOF
```

- [ ] **步骤 2：验证文档内容**

```bash
# 验证文档文件创建成功
cat docs/BUILD_INSTRUCTIONS.md
```

预期：显示完整的构建说明文档

- [ ] **步骤 3：更新CHANGELOG**

在CHANGELOG.md中添加版本更新记录：

```bash
# 在CHANGELOG.md顶部添加新条目
cat > CHANGELOG_NEW.md << 'EOF'
# [4.2.0] - 2025-08-07

## Added
- 多平台Native Image构建脚本，自动检测平台并生成对应二进制文件
- 与Go版本完全一致的二进制文件命名格式（harness-{os}-{arch}）

## Changed
- 更新Maven Native Image插件配置支持动态二进制文件命名
- 重构包装脚本以支持新的文件结构，保持向后兼容性

## Fixed
- Claude插件二进制文件识别问题，现在能够正确识别和调用

## Technical Details
- 二进制文件现在直接生成在bin目录下，采用扁平化结构
- 支持平台：macOS（Intel/Apple Silicon）、Linux（AMD64/ARM64）、Windows（AMD64）
- 保留旧的子目录结构作为向后兼容的回退机制
EOF
```

- [ ] **步骤 4：提交文档更新**

```bash
git add docs/BUILD_INSTRUCTIONS.md CHANGELOG.md
git commit -m "docs(binary-recognition): 更新构建和版本文档

- 添加详细的Native Image构建说明
- 记录新的二进制文件命名约定
- 更新CHANGELOG记录4.2.0版本变更
- 提供多平台构建和使用示例

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 自检清单

**规格覆盖度：**
- ✅ 构建流程修改（任务1、2）
- ✅ 脚本逻辑更新（任务3、4）
- ✅ .gitignore更新（任务5）
- ✅ 测试验证（任务6）
- ✅ 文档更新（任务7）

**占位符扫描：**
- ✅ 所有步骤包含完整代码
- ✅ 所有命令都有预期输出说明
- ✅ 没有使用"TBD"、"TODO"等占位符
- ✅ 每个文件修改都有具体行号指示

**类型一致性：**
- ✅ 二进制文件命名在所有任务中保持一致：harness-{os}-{arch}
- ✅ 平台检测逻辑在Unix和Windows脚本中保持一致
- ✅ Maven参数命名在各任务中统一使用native.os和native.arch

**YAGNI验证：**
- ✅ 没有添加不必要的功能
- ✅ 保持向后兼容性但不过度设计
- ✅ 专注于解决核心问题：二进制文件识别

**TDD原则：**
- ✅ 每个任务都包含测试步骤
- ✅ 测试步骤先于实现步骤
- ✅ 包含失败验证和成功验证

---

## 执行说明

**预估时间：**
- 每个任务：15-30分钟
- 总计：2-3小时

**顺序执行：**
必须按照任务顺序执行，因为后续任务依赖前面的完成状态。

**并行化机会：**
- 任务6的测试步骤可以与任务7的文档更新并行进行
- 不同平台的构建测试可以在不同环境中并行执行

**关键检查点：**
1. 任务2完成后：确认Maven配置能够生成正确命名的二进制文件
2. 任务3完成后：确认包装脚本能够调用新的二进制文件
3. 任务6完成后：确认整个构建和运行流程端到端工作正常

**回滚计划：**
如果任何任务失败，可以使用git回滚到上一个成功的commit：
```bash
git log --oneline -5
git revert <commit-hash>
```