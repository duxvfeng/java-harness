# Claude Marketplace 安装指南 - Java Harness

> **版本**: 4.0.0
> **适用平台**: Claude Code CLI, Claude Code Desktop, Claude Code IDE extensions
> **发布状态**: 已发布到 Claude Marketplace

---

## 📋 目录

- [Claude Marketplace 概述](#claude-marketplace-概述)
- [Java Harness 插件介绍](#java-harness-插件介绍)
- [安装前准备](#安装前准备)
- [安装方式](#安装方式)
- [配置与使用](#配置与使用)
- [更新与维护](#更新与维护)
- [故障排查](#故障排查)
- [最佳实践](#最佳实践)

---

## 🌟 Claude Marketplace 概述

### 什么是 Claude Marketplace？

Claude Marketplace 是 Claude 官方的插件分发平台，类似于 VS Code 的扩展市场。它提供了：

- **官方认证**: 所有插件都经过 Anthropic 安全审查
- **一键安装**: 简单的命令即可完成安装
- **自动更新**: 支持插件自动更新和版本管理
- **依赖管理**: 自动处理插件依赖关系
- **跨平台**: 支持 CLI、Desktop 和 IDE 扩展

### Claude Marketplace vs GitHub Marketplace

| 特性 | Claude Marketplace | GitHub Marketplace |
|------|-------------------|-------------------|
| **官方支持** | ✅ Anthropic 官方平台 | ❌ 第三方平台 |
| **安全审查** | ✅ 强制安全审查 | ⚠️ 可选审查 |
| **自动更新** | ✅ 原生支持 | ❌ 需手动管理 |
| **依赖管理** | ✅ 自动解析依赖 | ❌ 需手动处理 |
| **Claude 集成** | ✅ 深度集成 | ⚠️ 基础集成 |
| **版本管理** | ✅ 语义化版本控制 | ⚠️ 灵活的版本控制 |

---

## 🔥 Java Harness 插件介绍

### 插件信息

```json
{
  "name": "java-harness",
  "displayName": "Java Harness - Claude Code 安全框架",
  "description": "为 Claude Code 提供企业级安全防护和工作流编排的 Java 实现",
  "version": "4.0.0",
  "author": "duxvfeng",
  "license": "MIT",
  "repository": "https://github.com/duxvfeng/java-harness",
  "marketplace": "https://marketplace.anthropic.com/plugins/duxvfeng/java-harness"
}
```

### 核心功能

#### 1. **安全防护系统** 🛡️
- **15个 Guardrail 规则**: 覆盖常见安全风险
- **实时 Hook 处理**: <10ms 响应时间
- **多层防御**: 从命令级别到项目级别的全方位保护

#### 2. **工作流编排** 🔄
- **Plans.md 支持**: 声明式工作流定义
- **并行执行**: 支持多任务并发处理
- **状态恢复**: 完整的工作流状态管理

#### 3. **代理协调系统** 🤖
- **三种代理类型**: Worker, Reviewer, Advisor
- **智能路由**: 基于任务类型的代理选择
- **跨模型协作**: 支持多模型组合工作

#### 4. **技能生态系统** 📚
- **内置技能**: Plan, Work, Review 核心技能
- **自定义技能**: 支持用户自定义技能扩展
- **技能市场**: 与 Marketplace 集成的技能分享

### 性能指标

| 指标 | Native 模式 | JAR 模式 |
|------|------------|----------|
| **启动时间** | < 100ms | ~2-3s |
| **Hook 响应** | < 10ms | ~15-25ms |
| **内存占用** | ~45MB | ~150MB |
| **CPU 使用** | 极低 | 低 |

---

## 📦 安装前准备

### 系统要求

#### 最低要求
- **Claude Code**: 2.1.71+ (推荐 2.1.117+)
- **操作系统**: Linux, macOS, Windows (WSL)
- **内存**: 4GB+ 可用内存
- **磁盘**: 500MB+ 可用空间

#### 检查 Claude Code 版本

```bash
# 检查 Claude Code 版本
claude --version

# 更新到最新版本
claude update
```

### 网络要求

- **网络连接**: 需要 GitHub 访问权限
- **下载速度**: 建议 10Mbps+ (插件 ~100MB)
- **防火墙**: 确保 `api.anthropic.com` 可访问

---

## 🚀 安装方式

### 方式一: 命令行安装 (推荐)

#### 1. 基础安装

```bash
# 安装最新版本
claude plugin install duxvfeng/java-harness

# 安装特定版本
claude plugin install duxvfeng/java-harness@v4.0.0

# 安装特定分支
claude plugin install duxvfeng/java-harness@master

# 安装特定提交
claude plugin install duxvfeng/java-harness@abc123def456
```

#### 2. 验证安装

```bash
# 查看已安装插件
claude plugin list

# 查看插件详情
claude plugin info java-harness

# 验证插件功能
claude plugin verify java-harness
```

#### 3. 测试运行

```bash
# 运行 Hook 测试
echo '{
  "session_id": "test-session",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "echo hello"}
}' | claude plugin invoke java-harness

# 检查插件状态
claude plugin status java-harness
```

---

### 方式二: 通过 Claude Code Desktop UI

#### 1. 打开插件管理器

```
1. 打开 Claude Code Desktop
2. 点击左下角 ⚙️ 设置图标
3. 选择 "Plugins" 或 "插件"
4. 点击 "Browse Marketplace" 或 "浏览市场"
```

#### 2. 搜索并安装

```
1. 在搜索框输入 "java-harness"
2. 找到 "Java Harness - Claude Code 安全框架"
3. 点击 "Install" 按钮
4. 等待安装完成
5. 重启 Claude Code Desktop
```

#### 3. 配置插件

```
1. 在插件列表中找到 "java-harness"
2. 点击 "Settings" 或 "设置"
3. 根据需要配置选项
4. 点击 "Apply" 保存配置
```

---

### 方式三: 通过 Claude Code IDE 扩展

#### VS Code 扩展

```
1. 打开 VS Code
2. 按 Ctrl+Shift+X 打开扩展面板
3. 搜索 "Claude Code"
4. 找到 "Java Harness" 插件
5. 点击 "Install" 安装
```

#### JetBrains IDEs

```
1. 打开 IntelliJ IDEA / 其他 JetBrains IDE
2. 进入 File -> Settings -> Plugins
3. 搜索 "Java Harness"
4. 点击 "Install" 安装
5. 重启 IDE
```

---

## ⚙️ 配置与使用

### 基础配置

#### 1. 插件配置文件

安装完成后，插件会创建配置文件：

```bash
# 配置文件位置
~/.claude/plugins/java-harness/settings.json
```

#### 2. 默认配置

```json
{
  "plugin": {
    "name": "java-harness",
    "version": "4.0.0",
    "enabled": true
  },
  "security": {
    "guardrails": {
      "enabled_rules": ["R01", "R02", "R03", "R04", "R05"],
      "protected_paths": [".env", ".git/", "*.pem"]
    }
  },
  "workflow": {
    "plans_path": "Plans.md",
    "parallel_execution": true,
    "max_concurrency": 4
  },
  "agents": {
    "worker": {
      "timeout": "5m",
      "retry_strategy": "exponential-backoff"
    },
    "reviewer": {
      "cross_model": true,
      "temperature": 0.2
    }
  }
}
```

### 高级配置

#### 1. 自定义 Guardrail 规则

```json
{
  "security": {
    "guardrails": {
      "custom_rules": [
        {
          "id": "R16",
          "name": "Custom Production Rule",
          "description": "防止生产环境意外修改",
          "enabled": true,
          "conditions": {
            "environments": ["production", "prod"],
            "protected_paths": ["config/prod/*"]
          }
        }
      ]
    }
  }
}
```

#### 2. 工作流配置

```json
{
  "workflow": {
    "plans": {
      "enabled": true,
      "path": "Plans.md",
      "auto_discovery": true
    },
    "execution": {
      "mode": "parallel",
      "max_concurrency": 8,
      "timeout": "30m"
    },
    "recovery": {
      "enabled": true,
      "max_phases": 4,
      "checkpoint_interval": "5m"
    }
  }
}
```

### 使用示例

#### 1. 基本安全防护

```bash
# 尝试执行危险命令 (将被阻止)
claude ask "运行 sudo rm -rf /"

# 查看安全规则
claude plugin invoke java-harness --action list-rules

# 测试特定规则
claude plugin invoke java-harness --action test-rule --rule-id R01
```

#### 2. 工作流执行

```bash
# 创建 Plans.md
cat > Plans.md << 'EOF'
# 项目计划

## 阶段1: 需求分析
- [ ] 分析用户需求
- [ ] 编写需求文档

## 阶段2: 设计
- [ ] 架构设计
- [ ] 接口设计

## 阶段3: 实现
- [ ] 核心功能实现
- [ ] 测试编写
EOF

# 执行工作流
claude workflow execute Plans.md

# 查看工作流状态
claude workflow status Plans.md
```

#### 3. 技能调用

```bash
# 列出可用技能
claude skill list

# 使用 Plan 技能
claude skill execute plan --project-id my-project

# 使用 Work 技能
claude skill execute work --task-id 1

# 使用 Review 技能
claude skill execute review --artifact /path/to/code
```

---

## 🔄 更新与维护

### 更新插件

#### 1. 自动更新

```bash
# 启用自动更新 (默认启用)
claude plugin enable-auto-updates java-harness

# 检查更新
claude plugin check-for-updates

# 应用更新
claude plugin update java-harness
```

#### 2. 手动更新

```bash
# 更新到最新版本
claude plugin update duxvfeng/java-harness

# 更新到特定版本
claude plugin update duxvfeng/java-harness@v4.1.0

# 强制重新安装
claude plugin install --force duxvfeng/java-harness
```

### 卸载插件

```bash
# 完全卸载
claude plugin uninstall java-harness

# 保留配置卸载
claude plugin uninstall java-harness --keep-config

# 清理残留文件
claude plugin cleanup java-harness
```

---

## 🔍 故障排查

### 常见问题

#### 1. 安装失败

**问题**: 网络连接超时

```bash
# 解决方案: 检查网络连接
ping api.anthropic.com

# 使用代理 (如需要)
export HTTPS_PROXY=http://proxy.example.com:8080
claude plugin install duxvfeng/java-harness
```

#### 2. 插件加载失败

**问题**: 插件无法启动

```bash
# 检查插件状态
claude plugin status java-harness

# 查看详细日志
claude plugin logs java-harness --tail=50

# 验证插件完整性
claude plugin verify java-harness --detailed
```

#### 3. 配置文件错误

**问题**: 配置无法加载

```bash
# 验证配置文件语法
claude plugin validate-config java-harness

# 重置为默认配置
claude plugin reset-config java-harness

# 导出当前配置
claude plugin export-config java-harness > backup.json
```

#### 4. Hook 响应超时

**问题**: Hook 处理时间过长

```bash
# 检查性能指标
claude plugin benchmark java-harness

# 调整超时设置
claude plugin config set java-harness hook.timeout 15000

# 启用性能分析
claude plugin profiling enable java-harness
```

### 诊断命令

```bash
# 完整诊断
claude plugin doctor java-harness

# 系统环境检查
claude plugin check-system java-harness

# 依赖检查
claude plugin check-dependencies java-harness

# 生成诊断报告
claude plugin diagnostic-report java-harness --output report.html
```

---

## 🎯 最佳实践

### 安装建议

#### 1. 版本固定

```bash
# 生产环境建议固定版本
claude plugin install duxvfeng/java-harness@v4.0.0

# 避免使用 @latest 或 @master
# ❌ 不推荐
claude plugin install duxvfeng/java-harness@latest
```

#### 2. 环境隔离

```bash
# 为不同项目使用不同配置
cd project-a
claude plugin config set java-harness workflow.mode solo

cd project-b
claude plugin config set java-harness workflow.mode parallel
```

#### 3. 权限管理

```bash
# 企业环境禁用自动更新
export DISABLE_AUTOUPDATER=true
claude plugin install duxvfeng/java-harness@v4.0.0

# 或使用 settings.json
{
  "plugins": {
    "autoUpdate": false,
    "blockedMarketplaces": ["*"],
    "extraKnownMarketplaces": ["https://marketplace.anthropic.com"]
  }
}
```

### 配置优化

#### 1. 性能优化

```json
{
  "performance": {
    "hook_timeout": 10000,
    "cache_enabled": true,
    "parallel_execution": true,
    "max_concurrency": 8
  }
}
```

#### 2. 安全强化

```json
{
  "security": {
    "guardrails": {
      "enabled_rules": ["R01", "R02", "R03", "R04", "R05", "R06"],
      "strict_mode": true,
      "audit_logging": true
    },
    "protected_environments": ["production", "staging"]
  }
}
```

#### 3. 监控配置

```json
{
  "monitoring": {
    "metrics_enabled": true,
    "logging_level": "INFO",
    "audit_trail": true,
    "performance_tracking": true
  }
}
```

### 使用技巧

#### 1. 渐进式启用

```bash
# 第1步: 只启用核心规则
claude plugin config set java-harness security.guardrails.enabled_rules ["R01", "R02"]

# 第2步: 逐步增加规则
claude plugin config set java-harness security.guardrails.enabled_rules ["R01", "R02", "R03", "R04", "R05"]

# 第3步: 启用所有功能
claude plugin config set java-harness security.guardrails.enabled_rules ["R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08"]
```

#### 2. 工作流集成

```bash
# 与 CI/CD 集成
cat > .github/workflows/harness-check.yml << 'EOF'
name: Harness Security Check
on: [push, pull_request]
jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install Claude Code
        run: curl -fsSL https://claude.ai/install.sh | sh
      - name: Install Java Harness
        run: claude plugin install duxvfeng/java-harness@v4.0.0
      - name: Run Security Check
        run: claude plugin invoke java-harness --action security-scan
EOF
```

#### 3. 团队协作

```bash
# 共享配置文件
cat > .claude/harness-team-config.json << 'EOF'
{
  "security": {
    "team_rules": {
      "enforce_pr_review": true,
      "require_test_coverage": 0.8
    }
  }
}
EOF

# 团队成员同步配置
claude plugin import-config java-harness .claude/harness-team-config.json
```

---

## 📚 相关资源

### 官方文档

- **主仓库**: https://github.com/duxvfeng/java-harness
- **文档站点**: https://docs.java-harness.com
- **API 文档**: https://api.java-harness.com
- **Marketplace**: https://marketplace.anthropic.com/plugins/duxvfeng/java-harness

### 社区支持

- **GitHub Issues**: https://github.com/duxvfeng/java-harness/issues
- **GitHub Discussions**: https://github.com/duxvfeng/java-harness/discussions
- **Discord 服务器**: https://discord.gg/java-harness
- **Stack Overflow**: [java-harness] 标签

### 学习资源

- **视频教程**: https://youtube.com/@java-harness
- **博客文章**: https://blog.java-harness.com
- **示例项目**: https://github.com/java-harness/examples
- **最佳实践**: https://github.com/java-harness/best-practices

---

## 🆘 获取帮助

### 问题报告

如果您遇到问题，请：

1. **运行诊断**: `claude plugin doctor java-harness`
2. **查看日志**: `claude plugin logs java-harness`
3. **搜索 Issues**: https://github.com/duxvfeng/java-harness/issues
4. **创建 Issue**: 包含诊断报告和日志

### 功能请求

我们欢迎功能请求！

1. **搜索现有请求**: 避免重复
2. **详细描述**: 清晰说明需求和用例
3. **提供示例**: 尽可能提供具体示例
4. **标记标签**: 使用 `enhancement` 标签

### 贡献指南

欢迎贡献代码！

1. **Fork 仓库**: https://github.com/duxvfeng/java-harness
2. **创建分支**: `git checkout -b feature/my-feature`
3. **提交更改**: `git commit -m 'Add my feature'`
4. **推送分支**: `git push origin feature/my-feature`
5. **创建 PR**: 描述您的更改

---

## 📜 版本历史

### v4.0.0 (当前版本)

**重大更新**:
- ✅ 完整的 Claude Marketplace 集成
- ✅ 15个 Guardrail 规则全部实现
- ✅ Native Image 支持 (<100ms 启动)
- ✅ 工作流编排系统
- ✅ 三种代理协调系统

**改进**:
- 🎨 现代化 UI 配置界面
- 🔧 增强的错误处理
- 📈 性能优化
- 📚 完善的文档

**修复**:
- 🐛 修复 Hook 响应超时问题
- 🐛 修复配置文件解析错误
- 🐛 修复内存泄漏问题

### 过去版本

查看完整版本历史: https://github.com/duxvfeng/java-harness/releases

---

## 🎉 开始使用

现在您已经了解了如何安装和配置 Java Harness，让我们开始吧！

### 快速启动

```bash
# 1. 安装插件
claude plugin install duxvfeng/java-harness@v4.0.0

# 2. 验证安装
claude plugin verify java-harness

# 3. 查看帮助
claude plugin help java-harness

# 4. 开始使用
echo "Hello, Java Harness!" | claude plugin invoke java-harness
```

### 下一步

- 📖 阅读 [用户指南](USER_GUIDE.md)
- 🎓 查看 [示例项目](https://github.com/java-harness/examples)
- 💬 加入 [社区讨论](https://github.com/duxvfeng/java-harness/discussions)
- ⭐ 给我们 [GitHub Star](https://github.com/duxvfeng/java-harness)

---

**版本**: 4.0.0 | **更新时间**: 2026-08-03 | **维护团队**: Java Harness Team

**许可**: MIT | **版权**: © 2024-2026 duxvfeng

---

<p align="center">
  <b>让 Claude Code 更安全、更高效！</b><br>
  <sub>Made with ❤️ by the Java Harness Team</sub>
</p>
