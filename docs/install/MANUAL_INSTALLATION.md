# Java Harness Claude 插件完整安装指南

## 🎯 概述

Java Harness 是 Claude Code 的 Java 实现，提供完整的 Plan→Work→Review→Release 工作流闭环，支持 21 个技能和 16 个 Hook 事件处理。

## 📋 系统要求

### 必要条件
- **Claude Code**: 最新版本的 Claude Code 客户端
- **Java 环境**: JDK 17+ (推荐使用 GraalVM JDK 17)
- **Maven**: 3.8+ 用于构建项目
- **操作系统**: Windows/Linux/macOS
- **内存**: 至少 8GB RAM
- **磁盘空间**: 至少 2GB 可用空间

### 可选条件（用于 Native Image 构建）
- **GraalVM**: 用于编译原生可执行文件
- **Visual Studio Build Tools** (Windows) 或 Xcode (macOS) 或 GCC (Linux)

## 🔧 安装步骤

### 步骤 1: 克隆项目

```bash
# 使用 Git 克隆项目
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness

# 或者使用 GitHub 镜像
git clone https://github.com/your-username/java-harness.git
cd java-harness
```

### 步骤 2: 构建项目

#### 选项 A: 使用预构建的 JAR 文件（推荐快速安装）

```bash
# 构建项目
cd java-harness-cli
mvn clean package -DskipTests

# 生成的文件位置
# java-harness-cli/target/java-harness-cli-4.0.0-java-SNAPSHOT.jar
```

#### 选项 B: 构建 Native Image（推荐生产环境）

```bash
# 构建 Native Image
cd java-harness-cli
mvn -Pnative package -DskipTests

# 生成的文件位置
# java-harness-cli/target/harness (Linux/macOS)
# java-harness-cli/target/harness.exe (Windows)
```

### 步骤 3: 配置 Claude Code 插件

#### 3.1 创建插件目录

```bash
# 在用户主目录下创建 Claude 插件目录
mkdir -p ~/.claude/plugins/java-harness
```

Windows 用户:
```powershell
# 在用户主目录下创建 Claude 插件目录
mkdir %USERPROFILE%\.claude\plugins\java-harness
```

#### 3.2 复制插件文件

```bash
# 复制插件配置文件
cp -r .claude-plugin/* ~/.claude/plugins/java-harness/

# 复制技能文件
cp -r skills/* ~/.claude/plugins/java-harness/skills/

# 复制工作流文件（如果有）
cp -r workflows/* ~/.claude/plugins/java-harness/workflows/
```

Windows 用户:
```powershell
# 复制插件配置文件
xcopy /E /I .claude-plugin %USERPROFILE%\.claude\plugins\java-harness

# 复制技能文件
xcopy /E /I skills %USERPROFILE%\.claude\plugins\java-harness\skills

# 复制工作流文件
xcopy /E /I workflows %USERPROFILE%\.claude\plugins\java-harness\workflows
```

#### 3.3 配置可执行文件路径

```bash
# 如果使用 Native Image
cd java-harness-cli
cp target/harness ~/.claude/plugins/java-harness/java-harness
chmod +x ~/.claude/plugins/java-harness/java-harness

# 或者使用 JAR 文件
cd java-harness-cli
cp target/java-harness-cli-4.0.0-java-SNAPSHOT.jar ~/.claude/plugins/java-harness/java-harness.jar
```

Windows 用户:
```powershell
# 如果使用 Native Image
copy java-harness-cli\target\harness.exe %USERPROFILE%\.claude\plugins\java-harness\java-harness.exe

# 或者使用 JAR 文件
copy java-harness-cli\target\java-harness-cli-4.0.0-java-SNAPSHOT.jar %USERPROFILE%\.claude\plugins\java-harness\java-harness.jar
```

### 步骤 4: 配置环境变量

```bash
# 设置 JAVA_HOME（如果需要）
export JAVA_HOME=/path/to/graalvm-jdk-17

# 添加到 PATH（可选）
export PATH=$PATH:~/.claude/plugins/java-harness
```

Windows 用户:
```powershell
# 设置 JAVA_HOME
set JAVA_HOME=C:\Path\To\GraalVM-JDK-17

# 或者通过系统设置永久配置
# 系统属性 -> 高级系统设置 -> 环境变量
```

### 步骤 5: 验证安装

```bash
# 测试可执行文件
~/.claude/plugins/java-harness/java-harness --version

# 或者使用 JAR 文件
java -jar ~/.claude/plugins/java-harness/java-harness.jar --version
```

Windows 用户:
```powershell
# 测试可执行文件
%USERPROFILE%\.claude\plugins\java-harness\java-harness.exe --version

# 或者使用 JAR 文件
java -jar %USERPROFILE%\.claude\plugins\java-harness\java-harness.jar --version
```

### 步骤 6: 重启 Claude Code

关闭并重新启动 Claude Code 客户端以加载新插件。

## 🔌 插件配置

### plugin.json 配置说明

插件配置文件 `~/.claude/plugins/java-harness/plugin.json` 包含以下关键配置：

```json
{
  "name": "java-harness",
  "version": "5.0.0-java",
  "capabilities": {
    "commands": {
      "java-harness": {
        "binary": "java-harness",
        "description": "Java Harness CLI - 统一命令入口，支持86个命令"
      }
    },
    "hooks": {
      "supported": ["PreToolUse", "PostToolUse", "PermissionRequest", ...],
      "total": 16
    },
    "skills": {
      "total": 21,
      "categories": {
        "planning": ["harness-plan", "harness-plan-brief"],
        "execution": ["harness-work", "harness-loop", "harness-accept"],
        "review": ["harness-review"],
        "release": ["harness-release"]
      }
    }
  }
}
```

### settings.json 配置

```json
{
  "agent": "claude-opus-5",
  "permissions": {
    "allow": ["Bash(git status:*)", "Bash(mvn -version)"],
    "deny": ["Bash(rm -rf *)"]
  },
  "env": {
    "JAVA_HOME": "/usr/lib/jvm/java-17",
    "MAVEN_OPTS": "-Xmx1G -Xms512m"
  }
}
```

## 🚀 使用插件

### 基本使用

安装完成后，在 Claude Code 中可以直接使用以下技能：

#### 规划阶段
```
/harness-plan - 创建详细的实现计划
/harness-plan-brief - 快速规划
```

#### 执行阶段
```
/harness-work - 执行 Plans.md 任务
/harness-loop - 长期运行的工作循环
/harness-accept - 验证和接受实现
```

#### 审查阶段
```
/harness-review - 代码和工作成果审查
```

#### 发布阶段
```
/harness-release - 发布和版本管理
```

### 高级功能

#### Hooks 事件处理
插件支持 16 个 Claude Code Hook 事件：
- PreToolUse - 工具使用前的安全检查
- PostToolUse - 工具使用后的验证
- PermissionRequest - 权限请求自动批准
- SessionStart/End - 会话管理
- 等等...

#### 并行执行
```
/harness-work all --parallel 4 - 并行执行所有任务
/breezing - 全自动团队执行模式
```

## 🛠️ 故障排除

### 常见问题

#### 1. 插件未加载
```bash
# 检查插件文件是否完整
ls -la ~/.claude/plugins/java-harness/

# 检查权限
chmod +x ~/.claude/plugins/java-harness/java-harness

# 重启 Claude Code
```

#### 2. 可执行文件无法运行
```bash
# 检查 Java 环境
java -version
mvn -version

# 重新构建
cd java-harness-cli
mvn clean package -DskipTests
```

#### 3. Native Image 构建失败
```bash
# 确保安装了必要的构建工具
# Windows: 安装 Visual Studio Build Tools
# macOS: 安装 Xcode 命令行工具
# Linux: 安装 GCC 和构建依赖

# 使用 GraalVM
export JAVA_HOME=/path/to/graalvm-jdk-17
mvn -Pnative package -DskipTests
```

#### 4. 技能文件缺失
```bash
# 确保所有技能文件都已复制
find ~/.claude/plugins/java-harness/skills -name "SKILL.md" | wc -l

# 应该显示 21 个技能文件
```

### 日志和调试

```bash
# 启用详细日志
export CLAUDE_DEBUG=1

# 测试插件
~/.claude/plugins/java-harness/java-harness --help
```

## 📊 性能优化

### Native Image 优势

使用 Native Image 版本的性能对比：

| 特性 | JAR 版本 | Native Image |
|------|----------|---------------|
| 启动时间 | ~2-3秒 | ~60ms ⚡ |
| 内存占用 | ~200MB | ~50MB |
| 运行时性能 | 良好 | 优秀 |
| 部署复杂度 | 需要 JDK | 无依赖 |

### 推荐配置

对于生产环境，推荐使用 Native Image 版本以获得最佳性能。

## 🔄 更新和升级

### 更新插件

```bash
# 拉取最新代码
cd java-harness
git pull origin main

# 重新构建
cd java-harness-cli
mvn clean package -DskipTests

# 更新插件文件
cp -r .claude-plugin/* ~/.claude/plugins/java-harness/
cp -r skills/* ~/.claude/plugins/java-harness/skills/
```

### 版本回退

```bash
# 查看可用版本
git tag

# 切换到特定版本
git checkout v5.0.0-java

# 重新构建和安装
cd java-harness-cli
mvn clean package -DskipTests
```

## 🤝 社区和支持

- **仓库**: https://gitee.com/duxvfeng/java-harness/
- **问题反馈**: 在项目仓库中提交 Issue
- **文档**: 查看 `docs/` 目录获取详细文档

## 📝 许可证

MIT License - 详见项目根目录的 LICENSE 文件。

---

**安装完成后，你就可以在 Claude Code 中使用 Java Harness 的完整功能了！** 🎉
