# 🚀 Java Harness - 快速安装指南

## 一键安装（推荐）

### Linux/macOS/Git Bash

```bash
# 方式 1：直接运行（最快）
curl -fsSL https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.sh | bash

# 方式 2：下载后运行
wget https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.sh
chmod +x install.sh
./install.sh
```

### Windows

```cmd
# 方式 1：PowerShell 一键安装
powershell -Command "Invoke-WebRequest -Uri 'https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.bat' -OutFile 'install.bat' && install.bat"

# 方式 2：手动下载
# 下载 https://gitee.com/chachamaru/java-harness/raw/main/scripts/install.bat
# 双击运行 install.bat
```

## 📦 手动安装

### 1. 克隆仓库

```bash
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness
```

### 2. 复制到插件目录

```bash
# Linux/macOS
mkdir -p ~/.claude/plugins/marketplaces/java-harness-marketplace
cp -r .claude-plugin ~/.claude/plugins/marketplaces/java-harness-marketplace/
cp -r bin ~/.claude/plugins/marketplaces/java-harness-marketplace/
cp VERSION ~/.claude/plugins/marketplaces/java-harness-marketplace/ 2>/dev/null || true

# Windows
mkdir "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace"
xcopy .claude-plugin "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\.claude-plugin\" /E /I /Y
xcopy bin "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\bin\" /E /I /Y
```

### 3. 设置权限并验证

```bash
# Linux/macOS：设置执行权限
chmod +x ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness

# 验证安装
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version
# 应该输出：4.0.0-java-SNAPSHOT

# Windows
%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\bin\harness.bat --version
```

## 🎯 在 Claude Code 中使用

### 重载插件

```
/reload-plugins
```

### 查看插件

```
/plugin list
```

应该能看到 `java-harness-marketplace` 插件。

## ✨ 快速开始

### 基本命令

```bash
# 查看版本
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version

# 查看帮助
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --help

# 计划管理
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan list
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan create my-plan

# 工作流执行
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work solo
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work parallel

# 代码审查
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness review
```

### 测试安全规则

```bash
# 测试安全命令（应允许）
echo '{
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "echo hello"}
}' | ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness
# 输出：{"permissionDecision":"allow"}

# 测试危险命令（应拒绝）
echo '{
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "sudo rm -rf /"}
}' | ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness
# 输出：{"permissionDecision":"deny"}
```

## ⚙️ 系统要求

### 必需
- **Java**: JDK 17 或更高版本
- **Git**: 用于克隆仓库
- **磁盘空间**: 约 10MB

### 可选（用于编译 Native Image）
- **GraalVM JDK 17**
- **Visual Studio 2022** (Windows)

## 🐛 故障排除

### Java 版本问题
```bash
# 检查 Java 版本
java -version

# 安装 Java 17+
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17
```

### 权限问题
```bash
# Linux/macOS
chmod +x ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness

# Windows - 以管理员身份运行
```

### 插件不被识别
```bash
# 重载插件
/reload-plugins

# 检查配置文件
cat ~/.claude/plugins/marketplaces/java-harness-marketplace/.claude-plugin/marketplace.json
```

## 📚 更多信息

- [完整文档](README.md)
- [打包指南](docs/Claude插件打包指南.md)
- [问题反馈](https://gitee.com/duxvfeng/java-harness/issues)

---

**安装完成后即可使用！** 🎉
