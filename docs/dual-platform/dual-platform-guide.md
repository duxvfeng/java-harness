# Java Harness 双平台插件使用指南

## 🎯 概述

Java Harness 现在支持双平台：**Claude Code** 和 **OpenAI Codex**，提供统一的工作流自动化体验。

## 📦 安装指南

### Claude Code 安装

```bash
# 在 Claude Code 中安装
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git

# 验证安装
/harness-version
```

### Codex 安装

```bash
# 方法1: 使用 Codex CLI
codex plugin install https://gitee.com/duxvfeng/java-harness.git

# 方法2: 手动安装
cd ~/.codex/plugins/cache
git clone https://gitee.com/duxvfeng/java-harness.git java-harness

# 验证安装
harness-version
```

## 🔧 配置指南

### 自动平台检测

Java Harness 会自动检测运行环境，并优化配置：

```bash
# 在 Claude Code 中 - 自动检测并优化
harness-plan    # 自动使用 Claude 后端，高强度推理

# 在 Codex 中 - 自动检测并优化  
harness-plan    # 自动使用 Codex 后端，平衡性能
```

### 手动配置后端

```toml
# harness.toml.bak - 手动指定后端
[harness]
backend = "codex"  # 或 "claude", "auto"

# 为不同功能指定后端
[work]
backend = "claude"        # 工作执行用 Claude

[review]  
backend = "codex"        # 代码审查用 Codex
```

## 📋 功能对比

| 功能 | Claude Code | Codex | 说明 |
|------|-----------|-------|------|
| **后端选择** | ✅ 原生 | ✅ 桥接 | Codex 通过适配器支持 |
| **计划管理** | ✅ | ✅ | 完全兼容 |
| **工作执行** | ✅ | ✅ | 完全兼容 |
| **代码审查** | ✅ | ✅ | 完全兼容 |
| **Hook 系统** | ✅ 原生 | ⚠️ 桥接 | Codex 使用兼容层 |
| **技能系统** | ✅ 原生 | ✅ 桥接 | 通过 CodexSkillBridge |
| **性能优化** | ✅ 高强度 | ✅ 平衡 | 根据平台自动调整 |

## 🚀 快速开始

### 基础使用（两个平台相同）

```bash
# 1. 创建计划
harness-plan
# 输入："帮我制定用户认证模块的实现计划"

# 2. 执行任务
harness-work 3
# 自动使用检测到的平台后端

# 3. 代码审查
harness-review
# 自动使用平台优化的审查器

# 4. 发布管理
harness-release
# 统一的发布流程
```

### 高级使用

```bash
# 指定后端执行
harness-work 5 --backend claude   # 用 Claude 处理复杂任务
harness-work 6 --backend codex    # 用 Codex 处理标准任务

# 查看当前配置
harness-config show

# 验证平台支持
harness-doctor
```

## 🌟 平台优势

### Claude Code 优势

- 🔥 **原生集成**: 深度集成，性能最优
- 🧠 **智能推理**: Claude 3/4 系列，高质量输出
- ⚡ **实时反馈**: 支持实时交互和审查
- 🛡️ **安全第一**: 内置安全检查和防护

### Codex 优势

- 💰 **成本效益**: OpenAI 定价更有优势
- 🔄 **熟悉工具**: 如果已有 OpenAI 生态经验
- 🌐 **生态丰富**: OpenAI 插件和工具丰富
- ⚖️ **平衡性能**: 在质量和速度间取得平衡

## 📊 兼容性保证

### ✅ 完全兼容的功能

```bash
# 这些功能在两个平台上完全一致
harness-plan          # 计划管理
harness-work 3        # 任务执行
harness-review        # 代码审查
harness-release      # 发布管理
harness-sync         # 状态同步
```

### ⚠️ 部分兼容的功能

```bash
# Hook 系统 (Codex 使用桥接模式)
harness-hook pre-tool use   # Claude: 原生, Codex: 桥接

# Agent 协调 (Codex 使用适配器模式)
harness-work --breezing    # Claude: 原生, Codex: 适配
```

## 🎁 跨平台使用技巧

### 1. 项目级配置

```bash
# 项目 A: 主要用 Claude
cd /project/backend-api
echo "backend = 'claude'" >> harness.toml.bak

# 项目 B: 主要用 Codex  
cd /project/frontend-app
echo "backend = 'codex'" >> harness.toml.bak
```

### 2. 任务级选择

```bash
# 根据任务复杂度选择后端
harness-work 1 --backend claude   # 复杂架构设计
harness-work 2 --backend codex    # 简单 CRUD 实现
```

### 3. 环境变量控制

```bash
# 临时切换后端
export HARNESS_BACKEND=claude
harness-work 3

# 恢复默认
unset HARNESS_BACKEND
```

## 🔍 故障排查

### 检测当前平台

```bash
# 查看平台检测结果
harness-platform detect

# 查看当前配置
harness-config show
```

### 验证后端连接

```bash
# 测试 Claude 连接
harness-test --backend claude

# 测试 Codex 连接  
harness-test --backend codex
```

### 查看日志

```bash
# Claude Code 日志
cat .claude/logs/harness-claude.log

# Codex 日志
cat .claude/logs/harness-codex.log
```

## 💡 最佳实践

### 1. 选择合适的后端

```bash
# 复杂任务 → Claude (更好的推理)
harness-work --backend claude design-architecture

# 标准任务 → Codex (更好的性价比)
harness-work --backend codex implement-crud

# 安全审查 → Claude (更严格的安全检查)
harness-review --backend claude --strict

# 快速迭代 → Codex (更快的执行速度)
harness-work --backend codex hotfix
```

### 2. 项目配置推荐

```bash
# 新项目探索阶段
backend = "codex"    # 低成本快速迭代

# 生产环境稳定阶段  
backend = "claude"   # 高质量保证稳定
```

### 3. 混合使用策略

```bash
# 核心功能用 Claude
harness-work --backend claude core-module

# 辅助功能用 Codex
harness-work --backend codex helper-functions
```

## 🎯 总结

Java Harness 的双平台支持让你：

1. ✅ **一套技能，两个平台**: 学一次，两边使用
2. ✅ **智能平台检测**: 自动优化配置
3. ✅ **灵活后端选择**: 根据需求选择合适的 AI
4. ✅ **完全兼容性**: 核心功能在两边都能用
5. ✅ **成本效益优化**: 合理分配工作给不同后端

开始享受跨平台的工作流自动化吧！🚀