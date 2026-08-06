# Java Harness Claude 插件 Marketplace 安装指南

## 🎯 概述

本指南介绍如何通过 Claude Code 插件 Marketplace 快速安装和配置 Java Harness 插件。

**Java Harness** 是 Claude Code 的完整 Java 实现，提供 21 个技能和 16 个 Hook 事件处理，支持 Plan→Work→Review→Release 完整工作流闭环。

## 📋 前置要求

### 系统要求
- **Claude Code**: 最新版本的 Claude Code 客户端
- **网络连接**: 用于从 marketplace 下载插件
- **磁盘空间**: 至少 500MB 可用空间
- **操作系统**: Windows/Linux/macOS

### Java 环境要求
- **JDK 17+**: 推荐使用 GraalVM JDK 17
- **Maven 3.8+**: 用于从源码构建（可选）
- **构建工具**: 用于 Native Image 编译（可选）

## 🚀 通过 Marketplace 安装

### 方法一：Claude Code 内置 Marketplace（推荐）

#### 步骤 1: 打开 Claude Code

启动 Claude Code 客户端应用程序。

#### 步骤 2: 进入插件管理界面

在 Claude Code 中访问插件管理：

**桌面客户端**:
```
1. 点击菜单栏的 "Tools" 或 "扩展"
2. 选择 "Plugin Marketplace" 或 "插件市场"
3. 在搜索框中输入 "java-harness"
```

**命令行方式**:
```bash
# 在 Claude Code REPL 中执行
/marketplace
```

#### 步骤 3: 搜索并安装插件

```
1. 在 Marketplace 搜索框中输入: "java-harness"
2. 找到 "Java Harness - Claude Code Implementation" 
3. 点击 "Install" 或 "安装" 按钮
4. 等待安装完成
```

#### 步骤 4: 验证安装

安装完成后，在 Claude Code 中测试：

```bash
# 在 Claude Code REPL 中测试
/harness-work --help
```

或查看已安装插件列表：

```bash
/list-plugins
```

### 方法二：通过 marketplace.json 文件安装

#### 步骤 1: 下载 marketplace 配置

从项目仓库下载 marketplace.json 文件：

```bash
# 直接下载
curl -o java-harness-marketplace.json \
  https://gitee.com/duxvfeng/java-harness/raw/main/.claude-plugin/marketplace.json

# 或使用 wget
wget https://gitee.com/duxvfeng/java-harness/raw/main/.claude-plugin/marketplace.json \
  -O java-harness-marketplace.json
```

#### 步骤 2: 导入到 Claude Code

在 Claude Code 中导入配置：

```bash
# 在 Claude Code REPL 中执行
/import-marketplace java-harness-marketplace.json
```

或在界面中：
```
1. 打开 "File" -> "Import Settings"
2. 选择下载的 marketplace.json 文件
3. 确认导入
```

#### 步骤 3: 自动安装

Claude Code 会自动：
- 解析 marketplace.json 配置
- 从指定的 Git 仓库下载插件
- 安装所有依赖和技能文件
- 配置可执行文件路径

## ⚙️ 插件配置

### 配置文件说明

安装后，Claude Code 会创建以下配置结构：

```
~/.claude/plugins/java-harness/
├── plugin.json              # 插件基本信息
├── marketplace.json         # Marketplace 配置
├── settings.json            # 环境和权限设置
├── java-harness             # 可执行文件
├── skills/                  # 21 个技能目录
│   ├── harness-plan/
│   ├── harness-work/
│   ├── harness-review/
│   ├── harness-release/
│   └── ...
└── workflows/              # 工作流定义
```

### settings.json 配置详解

默认的 settings.json 配置：

```json
{
  "agent": "claude-opus-5",
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "permissions": {
    "allow": [
      "Bash(git status:*)",
      "Bash(mvn -version)"
    ],
    "deny": [
      "Bash(rm -rf *)"
    ]
  },
  "sandbox": {
    "failIfUnavailable": true,
    "network": {
      "deniedDomains": [
        "169.254.169.254",
        "metadata.google.internal"
      ]
    }
  },
  "env": {
    "JAVA_HOME": "/usr/lib/jvm/java-17",
    "MAVEN_OPTS": "-Xmx1G -Xms512m -XX:MaxMetaspaceSize=512m"
  }
}
```

#### 自定义配置

你可以根据需要修改配置：

```json
{
  "permissions": {
    "allow": [
      "Bash(git status:*)",
      "Bash(mvn -version)",
      "Bash(java -version)",
      "Read(*)",
      "Write(*)"
    ]
  },
  "env": {
    "JAVA_HOME": "C:/Program Files/Java/graalvm-jdk-17",
    "HARNESS_LOG_LEVEL": "debug"
  }
}
```

## 🎯 插件功能验证

### 基本功能测试

安装完成后，测试插件的基本功能：

#### 1. 测试规划功能
```
/harness-plan
> Create a plan for implementing a new feature
```

#### 2. 测试执行功能
```
/harness-work task-1
> Execute task 1 from Plans.md
```

#### 3. 测试审查功能
```
/harness-review
> Review the completed work
```

#### 4. 测试发布功能
```
/harness-release
> Prepare release documentation
```

### 高级功能测试

#### 测试 Hook 事件处理
```bash
# 在 Claude Code 中执行一些操作，观察 Hook 响应
/write test.txt
/ls
/git-status
```

#### 测试并行执行
```
/harness-work all --parallel 4
> Execute all tasks with 4 parallel workers
```

## 🔄 更新和维护

### 自动更新

Claude Code Marketplace 支持自动更新：

```bash
# 检查插件更新
/check-updates

# 更新插件
/update-plugin java-harness
```

### 手动更新

如果需要手动更新到最新版本：

```bash
# 在 Claude Code REPL 中
/reinstall-plugin java-harness
```

或通过 Marketplace 界面重新安装。

## 🛠️ 故障排除

### 常见问题

#### 1. 插件无法从 Marketplace 安装

**问题**: 无法在 Marketplace 中找到 java-harness

**解决方案**:
```bash
# 确认网络连接正常
ping gitee.com

# 手动导入 marketplace.json
curl https://gitee.com/duxvfeng/java-harness/raw/main/.claude-plugin/marketplace.json
```

#### 2. 安装后插件无法加载

**问题**: 安装成功但插件不工作

**解决方案**:
```bash
# 检查插件文件完整性
ls -la ~/.claude/plugins/java-harness/

# 检查可执行文件权限
chmod +x ~/.claude/plugins/java-harness/java-harness

# 重启 Claude Code
```

#### 3. 技能文件缺失

**问题**: 某些技能无法使用

**解决方案**:
```bash
# 检查技能文件数量
find ~/.claude/plugins/java-harness/skills -name "SKILL.md" | wc -l

# 应该显示 21 个技能文件
# 如果缺少，重新安装插件
```

#### 4. Java 环境问题

**问题**: 插件运行时报 Java 错误

**解决方案**:
```bash
# 检查 Java 版本
java -version

# 检查 JAVA_HOME 配置
echo $JAVA_HOME

# 更新 settings.json 中的环境变量
```

### 调试模式

启用调试模式获取详细日志：

```bash
# 设置调试环境变量
export CLAUDE_DEBUG=1
export HARNESS_DEBUG=1

# 在 Claude Code 中查看日志
/view-logs
```

## 📊 性能优化建议

### 使用 Native Image 版本

Marketplace 默认提供预编译的 Native Image 版本：

**优势**:
- 启动时间: ~60ms (vs JAR ~2-3s)
- 内存占用: ~50MB (vs JAR ~200MB)
- 无需 JDK 依赖

**确认使用 Native Image**:
```bash
# 检查可执行文件类型
file ~/.claude/plugins/java-harness/java-harness

# 应该显示为可执行文件而不是 JAR
```

### 配置 JVM 参数

如果使用 JAR 版本，优化 JVM 参数：

```json
{
  "env": {
    "MAVEN_OPTS": "-Xmx2G -Xms1G -XX:+UseG1GC",
    "JAVA_OPTS": "-server -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
  }
}
```

## 🌐 网络配置

### 代理设置

如果需要通过代理访问 Marketplace：

```bash
# 设置代理环境变量
export HTTP_PROXY=http://proxy.example.com:8080
export HTTPS_PROXY=http://proxy.example.com:8080

# 或在 Claude Code 配置中设置
```

### 镜像源配置

对于国内用户，可以使用镜像源加速：

```json
{
  "marketplace": {
    "mirror": "https://mirror.claude-code.com/marketplace"
  }
}
```

## 🔐 安全配置

### 权限管理

根据需要调整插件权限：

```json
{
  "permissions": {
    "allow": [
      "Bash(git status:*)",
      "Read(*)",
      "Write(*)",
      "Edit(*)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(sudo *)",
      "Bash(admin *)"
    ]
  }
}
```

### 沙箱配置

启用沙箱隔离：

```json
{
  "sandbox": {
    "failIfUnavailable": true,
    "network": {
      "allowedDomains": ["*.example.com"],
      "deniedDomains": ["metadata.google.internal"]
    }
  }
}
```

## 📚 使用资源

### 官方文档

- **项目主页**: https://gitee.com/duxvfeng/java-harness/
- **源码仓库**: https://gitee.com/duxvfeng/java-harness.git
- **问题反馈**: 在项目仓库中提交 Issue

### 相关文档

- **手动安装指南**: `docs/install/MANUAL_INSTALLATION.md`
- **完整文档**: `docs/` 目录下的各种指南
- **API 文档**: 查看项目中的 Javadoc 和技能说明

## 🎉 安装完成

安装完成后，你就可以在 Claude Code 中使用 Java Harness 的完整功能：

### 🧠 规划阶段
- `/harness-plan` - 详细规划
- `/harness-plan-brief` - 快速规划

### ⚡ 执行阶段
- `/harness-work` - 任务执行
- `/harness-loop` - 工作循环
- `/breezing` - 团队执行

### 🔍 审查阶段
- `/harness-review` - 代码审查

### 🚀 发布阶段
- `/harness-release` - 版本发布

**恭喜！你现在拥有了完整的 Java Harness 工作流支持！** 🎊
