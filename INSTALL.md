# Java Harness - 快速安装指南

## 🚀 一键安装（推荐）

### Linux/macOS/Git Bash

```bash
# 方式 1：直接运行安装脚本
curl -fsSL https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.sh | bash

# 方式 2：下载后运行
wget https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.sh
chmod +x install.sh
./install.sh
```

### Windows

```cmd
REM 方式 1：直接下载运行
powershell -Command "Invoke-WebRequest -Uri 'https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.bat' -OutFile 'install.bat' && install.bat"

REM 方式 2：手动下载运行
REM 下载 https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.bat
REM 双击运行 install.bat
```

## 📦 手动安装

### 步骤 1：克隆仓库

```bash
# Linux/macOS/Git Bash
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness

# Windows CMD
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness
```

### 步骤 2：复制到插件目录

```bash
# 创建插件目录
mkdir -p ~/.claude/plugins/marketplaces/java-harness-marketplace

# 复制必要文件
cp -r .claude-plugin ~/.claude/plugins/marketplaces/java-harness-marketplace/
cp -r bin ~/.claude/plugins/marketplaces/java-harness-marketplace/
cp VERSION ~/.claude/plugins/marketplaces/java-harness-marketplace/ 2>/dev/null || true
cp README.md ~/.claude/plugins/marketplaces/java-harness-marketplace/ 2>/dev/null || true

# Windows CMD
mkdir "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace"
xcopy .claude-plugin "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\.claude-plugin\" /E /I /Y
xcopy bin "%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\bin\" /E /I /Y
```

### 步骤 3：设置权限

```bash
# Linux/macOS
chmod +x ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness
```

### 步骤 4：验证安装

```bash
# 测试启动
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version

# Windows
%USERPROFILE%\.claude\plugins\marketplaces\java-harness-marketplace\bin\harness.bat --version
```

## 🎯 在 Claude Code 中使用

### 重载插件

安装完成后，在 Claude Code 中运行：

```
/reload-plugins
```

### 查看插件列表

```
/plugin list
```

应该能看到 `java-harness-marketplace` 插件。

### 直接使用

```bash
# 查看版本
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version

# 查看帮助
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --help

# 计划管理
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan list
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan switch my-plan

# 工作流执行
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work solo
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work parallel

# 代码审查
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness review
```

## 🔧 系统要求

### 必需

- **Java**: JDK 17 或更高版本
- **Git**: 用于克隆仓库
- **磁盘空间**: 约 10MB

### 可选（用于编译 Native Image）

- **GraalVM JDK 17**: 用于编译 Native Image
- **Visual Studio 2022** (Windows): 用于 Native Image 编译
- **Xcode** (macOS): 用于 Native Image 编译

## 📋 安装验证

### 检查文件结构

```bash
ls -la ~/.claude/plugins/marketplaces/java-harness-marketplace/

# 应该看到：
# .claude-plugin/    # 配置目录
# bin/               # 可执行文件
# VERSION           # 版本文件
# README.md         # 文档
```

### 检查配置文件

```bash
ls -la ~/.claude/plugins/marketplaces/java-harness-marketplace/.claude-plugin/

# 必需文件：
# marketplace.json   # Marketplace 元数据
# plugin.json        # 插件信息
# settings.json      # 设置配置
# hooks.json         # Hook 配置
```

### 功能测试

```bash
# 测试 CLI 模式
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version

# 测试 Hook 模式
echo '{
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "echo hello"}
}' | ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness

# 预期输出：{"permissionDecision":"allow"}
```

## ⚡ 快速开始

安装完成后，立即体验：

### 1. 基本命令

```bash
# 查看所有可用命令
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --help

# 查看版本
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version
```

### 2. 计划管理

```bash
# 列出所有计划
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan list

# 创建新计划
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan create my-plan

# 切换计划
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan switch my-plan
```

### 3. 工作流执行

```bash
# Solo 模式
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work solo

# 并行模式
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work parallel
```

## 🐛 故障排除

### 问题 1：Java 版本过低

**错误**: `UnsupportedClassVersionError`

**解决**:
```bash
# 安装 Java 17+
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Windows
# 下载并安装 Oracle JDK 17 或 OpenJDK 17
```

### 问题 2：权限不足

**错误**: `Permission denied`

**解决**:
```bash
# Linux/macOS
chmod +x ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness

# Windows
# 以管理员身份运行命令提示符
```

### 问题 3：插件不被识别

**解决**:
```bash
# 重载插件
/reload-plugins

# 手动检查配置文件
cat ~/.claude/plugins/marketplaces/java-harness-marketplace/.claude-plugin/marketplace.json
```

### 问题 4：Hook 处理失败

**错误**: `Error processing hook`

**解决**:
```bash
# 检查 JAR 文件是否存在
ls -lh ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness.jar

# 检查日志
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version 2>&1
```

## 📞 获取帮助

- **文档**: [README.md](README.md)
- **问题反馈**: [Gitee Issues](https://gitee.com/duxvfeng/java-harness/issues)
- **详细指南**: [Claude插件打包指南.md](docs/Claude插件打包指南.md)

## 🔄 更新插件

```bash
# 进入插件目录
cd ~/.claude/plugins/marketplaces/java-harness-marketplace

# 拉取最新代码
git pull origin main

# 重新安装
./bin/harness --version
```

## 🗑️ 卸载插件

```bash
# 删除插件目录
rm -rf ~/.claude/plugins/marketplaces/java-harness-marketplace

# 在 Claude Code 中重载
/reload-plugins
```

---

**享受使用 Java Harness！** 🎉

如有任何问题，请通过 [Gitee Issues](https://gitee.com/chachamaru/java-harness/issues) 反馈。
