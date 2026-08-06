# Java Harness Claude 插件安装指南

欢迎安装 Java Harness Claude 插件！本页面提供了不同安装方式的详细指南。

## 🎯 Java Harness 简介

**Java Harness** 是 Claude Code 的完整 Java 实现，提供：

- **🧠 21 个技能** - 覆盖 Plan→Work→Review→Release 完整工作流
- **🔌 16 个 Hook 事件** - 全面的 Claude Code 事件处理
- **⚡ 高性能** - Native Image 支持，60ms 启动时间
- **🎯 完全对齐** - 与 Go 版本功能完全一致

## 📋 安装前准备

### 系统要求
- **Claude Code**: 最新版本
- **操作系统**: Windows/Linux/macOS
- **磁盘空间**: 至少 500MB
- **网络连接**: 用于下载插件

### Java 环境要求（可选）
- **JDK 17+**: 推荐使用 GraalVM JDK 17
- **Maven 3.8+**: 用于从源码构建（可选）

## 🚀 安装方式选择

根据你的需求和环境，选择合适的安装方式：

### 方式一：Marketplace 安装（推荐）

**适合用户**：
- ✅ 希望快速安装的用户
- ✅ 不需要自定义编译的用户
- ✅ 希望获得自动更新支持的用户

**优势**：
- 🚀 一键安装，简单快速
- 🔄 自动更新支持
- 🎯 预配置完成，即装即用
- 📦 包含预编译的 Native Image

**安装时间**：约 2-5 分钟

📖 **详细指南**: [MARKETPLACE_INSTALLATION.md](./MARKETPLACE_INSTALLATION.md)

```bash
# 在 Claude Code Marketplace 中搜索 "java-harness"
# 点击安装即可
```

---

### 方式二：手动安装

**适合用户**：
- 🔧 希望自定义配置的用户
- 💻 需要从源码编译的用户
- 🌐 离线环境或受限网络环境
- 🎓 希望了解插件结构的用户

**优势**：
- 🛠️ 完全控制安装过程
- 🔍 可以查看和修改源码
- 📚 学习插件结构
- 🌐 适合离线环境

**安装时间**：约 10-30 分钟（取决于是否编译 Native Image）

📖 **详细指南**: [MANUAL_INSTALLATION.md](./MANUAL_INSTALLATION.md)

```bash
# 克隆项目
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness

# 构建并安装
cd java-harness-cli
mvn clean package -DskipTests
```

---

## 📊 安装方式对比

| 特性 | Marketplace 安装 | 手动安装 |
|------|-----------------|----------|
| **安装难度** | ⭐ 简单 | ⭐⭐⭐ 中等 |
| **安装时间** | 2-5 分钟 | 10-30 分钟 |
| **自定义** | ⭐⭐ 有限 | ⭐⭐⭐⭐⭐ 完全 |
| **更新维护** | ⭐⭐⭐⭐ 自动 | ⭐⭐ 手动 |
| **离线支持** | ❌ 不支持 | ✅ 支持 |
| **源码访问** | ❌ 预编译 | ✅ 完整源码 |
| **网络依赖** | ✅ 需要 | ❌ 可选 |

## 🎯 快速开始

### 推荐安装路径

对于大多数用户，我们推荐以下路径：

1. **首次安装** → 使用 Marketplace 安装
2. **测试功能** → 验证基本功能
3. **高级使用** → 需要时切换到手动安装

### 验证安装

无论使用哪种安装方式，都可以通过以下方式验证安装：

```bash
# 在 Claude Code REPL 中测试
/harness-work --help

# 或查看已安装技能
/list-skills | grep harness
```

## 🔧 安装后配置

### 基础配置

安装完成后，建议进行以下配置：

1. **环境变量设置**
   ```bash
   export JAVA_HOME=/path/to/java-17
   export PATH=$PATH:$HOME/.claude/plugins/java-harness
   ```

2. **权限配置**
   - 根据需要调整 Bash 命令权限
   - 配置文件读写权限
   - 设置网络访问权限

3. **性能优化**
   - 选择使用 Native Image 版本
   - 配置合适的 JVM 参数
   - 启用适当的缓存设置

详细配置说明请查看对应的安装指南。

## 📚 功能概览

安装完成后，你可以使用以下功能：

### 🧠 规划阶段
- **harness-plan** - 创建详细实现计划
- **harness-plan-brief** - 快速规划

### ⚡ 执行阶段
- **harness-work** - 执行 Plans.md 任务
- **harness-loop** - 长期工作循环
- **harness-accept** - 验证实现
- **breezing** - 团队全自动执行

### 🔍 审查阶段
- **harness-review** - 代码和工作成果审查

### 🚀 发布阶段
- **harness-release** - 发布和版本管理

### 🛠️ 工具技能
- **harness-setup** - 初始化和配置
- **harness-sync** - 状态同步
- **harness-progress** - 进度跟踪
- **memory** - 知识管理
- **failure-codifier** - 故障分析

### 🎨 Cursor 集成
- **cursor-ask** - Cursor 问答模式
- **cursor-do** - Cursor 执行模式
- **cursor-review** - Cursor 审查模式
- **cursor-setup** - Cursor 配置

## 🆘 需要帮助？

### 常见问题

查看 [FAQ](../faq/) 或直接访问对应的安装指南：

- **Marketplace 安装问题** → [MARKETPLACE_INSTALLATION.md](./MARKETPLACE_INSTALLATION.md)
- **手动安装问题** → [MANUAL_INSTALLATION.md](./MANUAL_INSTALLATION.md)

### 获取支持

- **项目主页**: https://gitee.com/duxvfeng/java-harness/
- **问题反馈**: 在项目仓库中提交 Issue
- **文档**: 查看 `docs/` 目录下的详细文档

## 🎉 开始使用

选择适合你的安装方式，开始使用 Java Harness：

- 🚀 **快速安装** → [MARKETPLACE_INSTALLATION.md](./MARKETPLACE_INSTALLATION.md)
- 🔧 **手动安装** → [MANUAL_INSTALLATION.md](./MANUAL_INSTALLATION.md)

**祝安装顺利！如有问题，请查看对应的详细指南或寻求社区支持。** 🎊
