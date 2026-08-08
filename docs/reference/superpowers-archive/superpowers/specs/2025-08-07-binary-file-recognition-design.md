# 二进制文件识别设计方案

**日期：** 2025-08-07  
**状态：** 待用户审查  
**相关项目：** java-harness 与 claude-code-harness (Go版本) 对齐

## 概述

将Java版本的二进制文件组织方式调整为与Go版本完全一致，确保Claude插件安装时能够正确识别二进制文件。

## 问题分析

### 当前状况

| 方面 | Go版本 | Java版本（当前） |
|------|--------|------------------|
| **二进制文件位置** | 直接在 `bin/` 目录下 | 在子目录中：`bin/macos/macos-arm64/harness` |
| **文件命名** | `harness-darwin-amd64`, `harness-linux-amd64` | 统一命名为 `harness` |
| **Claude插件识别** | ✅ 能够正确识别 | ❌ 识别失败 |

### 根本原因

1. **文件结构不一致**：Java版本使用子目录组织，Go版本使用扁平化命名
2. **命名约定不匹配**：插件系统期望 `harness-{os}-{arch}` 格式
3. **构建产物位置**：Native Image编译后输出到子目录而非顶层

## 解决方案设计

### 架构设计

**目标结构：**
```
bin/
├── harness                    # 主入口脚本（更新逻辑）
├── harness-darwin-amd64      # macOS Intel
├── harness-darwin-arm64      # macOS Apple Silicon  
├── harness-linux-amd64       # Linux x86_64
├── harness-linux-arm64       # Linux ARM64
├── harness-windows-amd64.exe # Windows x86_64
└── harness-win32-amd64.exe   # Windows 32-bit (可选)
```

### 命名约定对应关系

| Go版本 | Java版本 | 一致性 |
|--------|----------|--------|
| `darwin` | `darwin` | ✅ |
| `linux` | `linux` | ✅ |
| `windows` | `windows` | ✅ |
| `amd64` | `amd64` | ✅ |
| `arm64` | `arm64` | ✅ |
| 格式：`harness-{os}-{arch}` | 格式：`harness-{os}-{arch}` | ✅ |

## 实施计划

### 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `java-harness-cli/pom.xml` | 修改 | 更新 native-maven-plugin 配置 |
| `bin/harness` | 替换 | 更新二进制文件查找逻辑 |
| `bin/harness.bat` | 替换 | Windows 批处理脚本 |
| `scripts/build/build-all-native.sh` | 新建 | 多平台构建脚本 |
| `scripts/build/build.sh` | 修改 | 更新构建流程 |
| `.gitignore` | 更新 | 调整二进制文件忽略规则 |

### 构建流程修改

#### 1. Maven配置更新

在 `java-harness-cli/pom.xml` 中修改 native-maven-plugin 配置：

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>${native.maven.plugin.version}</version>
    <configuration>
        <!-- 使用动态命名：harness-{os}-{arch} -->
        <imageName>harness-${os}-${arch}</imageName>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>-H:+ReportExceptionStackTraces</buildArg>
        </buildArgs>
        <mainClass>com.chachamaru.harness.cli.HarnessCli</mainClass>
    </configuration>
</plugin>
```

#### 2. 多平台构建脚本

创建 `scripts/build/build-all-native.sh`：

```bash
#!/bin/bash
# scripts/build/build-all-native.sh
set -e

detect_platform() {
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case "$OS" in
        Darwin) OS="darwin";;
        Linux)  OS="linux";;
        MINGW*|MSYS*|CYGWIN*) OS="windows";;
    esac
    
    case "$ARCH" in
        x86_64)  ARCH="amd64";;
        aarch64|arm64) ARCH="arm64";;
        i386|i686) ARCH="386";;
    esac
    
    echo "${OS}-${ARCH}"
}

PLATFORM=$(detect_platform)
OS_PART="${PLATFORM%-*}"
ARCH_PART="${PLATFORM#*-}"
EXT=""
[ "$OS_PART" = "windows" ] && EXT=".exe"

echo "Building for: ${PLATFORM}"
mvn -Pnative \
    -Dnative.os="${OS_PART}" \
    -Dnative.arch="${ARCH_PART}" \
    -Dnative.image.name="harness-${OS_PART}-${ARCH_PART}${EXT}" \
    native:compile

# 复制到 bin 目录
TARGET_BINARY="java-harness-cli/target/harness-${OS_PART}-${ARCH_PART}${EXT}"
if [ -f "$TARGET_BINARY" ]; then
    mkdir -p bin
    cp "$TARGET_BINARY" "bin/"
    chmod +x "bin/harness-${OS_PART}-${ARCH_PART}${EXT}"
    echo "✅ Binary created: bin/harness-${OS_PART}-${ARCH_PART}${EXT}"
fi
```

#### 3. 包装脚本更新

更新 `bin/harness` 脚本：

```bash
#!/bin/sh
# bin/harness - 更新后的版本
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 平台检测
OS_TYPE="$(uname -s)"
ARCH_TYPE="$(uname -m)"

# 标准化OS和架构名称
case "$OS_TYPE" in
    Darwin*) OS="darwin";;
    Linux*)  OS="linux";;
    MINGW*|MSYS*|CYGWIN*) OS="windows";;
    *)       OS="$OS_TYPE";;
esac

case "$ARCH_TYPE" in
    x86_64|amd64)  ARCH="amd64";;
    aarch64|arm64) ARCH="arm64";;
    i386|i686)     ARCH="386";;
    *)              ARCH="$ARCH_TYPE";;
esac

# 构建二进制文件名（与Go版本一致）
EXT=""
[ "$OS" = "windows" ] && EXT=".exe"
BINARY_NAME="harness-${OS}-${ARCH}${EXT}"
BINARY_PATH="${SCRIPT_DIR}/${BINARY_NAME}"

# 执行对应的二进制文件
if [ -x "$BINARY_PATH" ]; then
    exec "$BINARY_PATH" "$@"
fi

# 错误处理
echo "Error: No binary found for ${OS}-${ARCH}" >&2
echo "Expected: ${BINARY_PATH}" >&2
exit 1
```

### .gitignore 更新

```gitignore
# Native Image 可执行文件（新格式）
bin/harness-darwin-amd64
bin/harness-darwin-arm64
bin/harness-linux-amd64
bin/harness-linux-arm64
bin/harness-windows-amd64.exe
bin/harness-win32-amd64.exe

# 旧的子目录结构（保持兼容过渡期）
# bin/windows/harness.exe
# bin/linux/*/harness
# bin/macos/*/harness
```

## 测试验证

### 构建测试

```bash
# macOS ARM64
./scripts/build/build-all-native.sh
# 验证输出：bin/harness-darwin-arm64

# 测试执行
./bin/harness --version
```

### 跨平台验证

```bash
# 验证包装脚本逻辑
./bin/harness doctor
./bin/harness --help
```

### Claude 插件识别测试

```bash
# 确认文件命名与Go版本一致
ls -la bin/harness-*
# 应该看到类似输出：
# -rwxr-xr-x harness-darwin-arm64
# -rwxr-xr-x harness-darwin-amd64
```

## 兼容性保证

- ✅ **向后兼容**：旧的子目录结构在过渡期内仍然有效
- ✅ **Go 版本对齐**：二进制文件命名完全一致
- ✅ **跨平台支持**：macOS、Linux、Windows 全覆盖
- ✅ **CI/CD 友好**：构建脚本自动化平台检测

## 实施优先级

1. **高优先级**：核心构建流程修改（pom.xml, build-all-native.sh）
2. **中优先级**：脚本逻辑更新（bin/harness, bin/harness.bat）
3. **低优先级**：清理旧的子目录结构（可在后续版本中移除）

## 成功标准

- ✅ 所有平台的二进制文件直接生成在 `bin/` 目录下
- ✅ 文件命名格式为 `harness-{os}-{arch}[.exe]`
- ✅ Claude 插件能够正确识别并调用二进制文件
- ✅ 构建流程自动化，无需手动重命名
- ✅ 向后兼容性得到保证

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 构建脚本兼容性问题 | 中 | 充分测试多平台构建 |
| 旧版本用户升级 | 低 | 保持向后兼容过渡期 |
| CI/CD 集成 | 低 | 更新相关配置文件 |
| Windows符号链接限制 | 低 | 使用文件复制替代 |

## 后续步骤

1. 用户审查本设计文档
2. 根据反馈调整设计细节
3. 调用 `writing-plans` skill 创建详细实现计划
4. 开始实施