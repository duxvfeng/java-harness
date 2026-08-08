# Java Harness 安装指南

本指南详细介绍 Java Harness 的安装步骤、系统要求和配置方法。

## 📋 系统要求

### 基本要求

- **操作系统**: Windows 10+, macOS 10.15+, Linux (主流发行版)
- **JDK 版本**: JDK 17 或更高版本
- **内存**: 最少 4GB RAM（推荐 8GB+）
- **磁盘空间**: 
  - 基础安装: 500MB
  - 开发环境: 2GB+
  - 包含源码: 500MB+

### Java 环境

```bash
# 检查 Java 版本
java -version
# 应显示: openjdk version "17.x.x" 或更高版本

# 检查 JAVA_HOME
echo $JAVA_HOME
# 应指向 JDK 17+ 安装路径
```

### GraalVM (可选)

如果需要编译 Native Image，需要安装 GraalVM：

```bash
# 安装 GraalVM 23.1.0+
# 详见: https://www.graalvm.org/downloads/
```

## 🚀 安装方式

### 方式 1: 预编译二进制文件 (推荐)

#### Windows 安装

1. **下载二进制文件**:
```powershell
# 下载 Windows x64 版本
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-windows-amd64.exe -o harness.exe
```

2. **添加到 PATH**:
```powershell
# 将 harness.exe 移动到系统 PATH 中的目录
move harness.exe C:\Tools\bin\

# 或添加当前目录到 PATH
setx PATH "%PATH%;%CD%"
```

3. **验证安装**:
```powershell
harness --version
```

#### Linux 安装

1. **下载二进制文件**:
```bash
# AMD64 架构
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-linux-amd64 -o harness
chmod +x harness

# ARM64 架构
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-linux-arm64 -o harness
chmod +x harness
```

2. **安装到系统路径**:
```bash
sudo mv harness /usr/local/bin/
```

3. **验证安装**:
```bash
harness --version
```

#### macOS 安装

1. **下载二进制文件**:
```bash
# Intel Mac
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-amd64 -o harness
chmod +x harness

# Apple Silicon Mac
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-arm64 -o harness
chmod +x harness
```

2. **安装到系统路径**:
```bash
sudo mv harness /usr/local/bin/
```

3. **验证安装**:
```bash
harness --version
```

### 方式 2: JAR 文件安装

1. **下载 JAR 文件**:
```bash
curl -L https://github.com/your-org/java-harness/releases/latest/download/java-harness-cli-4.1.1.jar -o harness.jar
```

2. **创建启动脚本**:

**Linux/macOS**:
```bash
cat > harness << 'EOF'
#!/bin/bash
java -jar $(dirname $0)/harness.jar "$@"
EOF
chmod +x harness
```

**Windows**:
```powershell
@echo off
java -jar "%~dp0harness.jar" %*
```

3. **验证安装**:
```bash
./harness --version
```

### 方式 3: 从源码编译

1. **克隆仓库**:
```bash
git clone https://github.com/your-org/java-harness.git
cd java-harness
```

2. **编译项目**:
```bash
# 编译所有模块
mvn clean compile

# 打包 JAR 文件
mvn clean package

# 编译 Native Image (可选)
cd java-harness-cli
mvn -Pnative native:compile
```

3. **安装**:
```bash
# 安装 JAR 版本
cp java-harness-cli/target/java-harness-cli-4.1.1.jar /usr/local/lib/harness.jar

# 或安装 Native 版本
cp java-harness-cli/target/harness /usr/local/bin/
```

## ⚙️ 配置

### 基础配置

1. **初始化配置**:
```bash
harness init
```

这将在项目根目录创建 `.claude/harness.toml` 配置文件。

2. **配置文件结构**:
```toml
[harness]
version = "4.1.1"
mode = "standard"

[paths]
data = ".claude/data"
cache = ".claude/cache"
logs = ".claude/logs"

[security]
guardrails = true
strict_mode = false

[logging]
level = "INFO"
file = true
console = true
```

### Claude Code 集成

1. **安装 Claude Code Plugin**:
```bash
harness install-plugin --claude-code
```

2. **配置 Hooks**:
```bash
harness gen hooks
```

3. **验证安装**:
```bash
harness validate
```

## 🔍 验证安装

### 基本验证

```bash
# 检查版本
harness --version

# 查看帮助信息
harness --help

# 运行健康检查
harness doctor
```

### 功能验证

```bash
# 测试 Hook 功能
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"echo test"}}' | \
  harness hook pre-tool

# 测试配置文件
harness validate

# 查看系统状态
harness status
```

## 🛠️ 故障排除

### 常见问题

#### 1. Java 版本不兼容

**问题**: `UnsupportedClassVersionError`

**解决**:
```bash
# 更新到 JDK 17+
java -version  # 确认版本 >= 17
```

#### 2. 权限错误

**问题**: `Permission denied`

**解决**:
```bash
# 添加执行权限
chmod +x harness

# 或使用 Java 运行
java -jar harness.jar
```

#### 3. PATH 配置问题

**问题**: `command not found: harness`

**解决**:
```bash
# 检查 PATH
echo $PATH

# 添加到 PATH (临时)
export PATH="$PATH:/path/to/harness"

# 添加到 PATH (永久)
echo 'export PATH="$PATH:/path/to/harness"' >> ~/.bashrc
source ~/.bashrc
```

#### 4. 配置文件问题

**问题**: `Configuration file not found`

**解决**:
```bash
# 重新初始化配置
harness init

# 或手动创建配置目录
mkdir -p .claude
harness init --force
```

### 日志调试

启用详细日志以诊断问题：

```bash
# 启用调试日志
harness --debug --log-level=DEBUG <command>

# 查看日志文件
cat .claude/logs/harness.log
```

### 获取帮助

- **文档**: 查看 [README.md](../../README.md) 和 [故障排除指南](troubleshooting.md)
- **Issues**: [GitHub Issues](https://github.com/your-org/java-harness/issues)
- **讨论**: [GitHub Discussions](https://github.com/your-org/java-harness/discussions)

## 🔄 升级

### 升级到最新版本

```bash
# 下载最新版本
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness -o harness-new

# 替换旧版本
mv harness-new harness

# 验证新版本
harness --version
```

### 版本兼容性

查看 [CHANGELOG.md](../../CHANGELOG.md) 了解版本变更和兼容性信息。

## 📚 下一步

安装完成后，建议阅读：

- **[项目 README](../../README.md)** - 项目概述和快速开始
- **[架构文档](../developer-guide/architecture.md)** - 系统架构和设计决策
- **[开发指南](../developer-guide/development.md)** - 开发环境设置
- **[API 参考](../reference/api-reference.md)** - API 接口文档
- **[文档索引](../README.md)** - 完整文档导航

---

**安装指南版本**: 1.0  
**最后更新**: 2026-08-08  
**适用于版本**: 4.1.1
