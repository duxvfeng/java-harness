# Java Harness 双平台插件 - 完整安装和使用手册

## 🎯 项目愿景

**一个插件，两个平台，统一体验**

Java Harness 现在同时支持 **Claude Code** 和 **OpenAI Codex**，提供跨平台的完整工作流自动化。

## 📦 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                  Java Harness 双平台插件                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         统一的用户界面和命令接口                        │  │
│  │                                                         │  │
│  │  /harness-plan   /harness-work   /harness-review         │  │
│  │                                                         │  │
│  └────────────────────────┬────────────────────────────┘  │
│                             │                              │
│              ┌──────────────┴───────────────┐              │
│              │                             │              │
│        ┌─────▼──────────┐        ┌──────▼─────────┐        │
│        │  Claude Code   │        │   Codex CLI    │        │
│        │   (原生)      │        │   (桥接)      │        │
│        │               │        │               │        │
│        │ - 计划管理    │        │ - 计划管理    │        │
│        │ - 工作执行    │        │ - 工作执行    │        │
│        │ - 代码审查    │        │ - 代码审查    │        │
│        │ - Agent 协调  │        │ - 适配器协调   │        │
│        └────────────────┘        └────────────────┘        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 安装指南

### 方法 1: Claude Code 安装

```bash
# 1. 在 Claude Code 中执行
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git

# 2. 等待安装完成
# ✅ 安装位置：/Users/apple/.claude/plugins/cache/java-harness-market/java-harness/

# 3. 验证安装
/harness-version
# 输出：Java Harness v4.1.1-java for Claude Code

# 4. 查看帮助
/harness --help
```

### 方法 2: Codex CLI 安装

#### 方式 A: 使用安装脚本（推荐）

```bash
# 1. 克隆仓库
cd ~/.codex/plugins/cache
git clone https://gitee.com/duxvfeng/java-harness.git java-harness
cd java-harness

# 2. 运行 Codex 安装脚本
./install-codex.sh

# 3. 验证安装
harness-version
# 输出：Java Harness v4.1.1-java for Codex CLI
```

#### 方式 B: 手动安装

```bash
# 1. 在系统终端执行
cd ~/.codex/plugins/cache
git clone https://gitee.com/duxvfeng/java-harness.git java-harness
cd java-harness

# 2. 验证插件文件
ls -la .codex-plugin/plugin.json  # 应该存在
ls -la harness.toml.bak               # 应该存在

# 3. 验证安装
cd ~/your-project
harness-version
# 输出：Java Harness v4.1.1-java for Codex CLI
```

## 🔧 配置设置

### 实际可用的配置文件

Java Harness 现已包含完整的双平台配置文件：

```bash
# 项目根目录下的实际文件
harness.toml.bak              # ✅ 完整的跨平台配置（15个配置节段）
.codex-plugin/plugin.json # ✅ Codex 插件定义
.claude-plugin/plugin.json # ✅ Claude 插件定义
install-claude.sh         # ✅ Claude 安装脚本
install-codex.sh          # ✅ Codex 安装脚本
```

### 自动配置（推荐）

```bash
# Java Harness 会自动检测运行环境
# 在 Claude Code 中：自动配置为 Claude 模式
# 在 Codex 中：自动配置为 Codex 模式

# 无需手动配置，开箱即用！
```

### 手动配置（高级用户）

#### 1. 创建项目级配置

```bash
cd ~/your-project

# 创建配置文件
cat > harness.toml.bak << 'EOF'
[harness]
# 自动检测运行环境
auto_detect_platform = true

# Claude Code 平台配置
[platform.claude]
default_backend = "claude"
effort = "high"
review_rounds = 5

# Codex 平台配置
[platform.codex]
default_backend = "codex"
effort = "medium"
review_rounds = 3
EOF
```

#### 2. 设置默认后端

```toml
# harness.toml.bak
[harness]
# 方法1: 指定默认后端
backend = "codex"  # 或 "claude"

# 方法2: 自动检测
auto_detect = true
```

#### 3. 针对不同功能指定后端

```toml
# harness.toml.bak
# 为不同功能指定最合适的后端
[plan]
backend = "claude"        # 计划用 Claude 的推理能力

[work]
backend = "auto"         # 工作执行自动选择

[review]
backend = "claude"        # 审查用 Claude 的严格模式
```

## 📋 核心功能使用

### 1. 计划管理（两个平台一致）

```bash
# 在 Claude Code 中
/harness-plan
# 输入："为微服务架构设计一个完整的实施计划"

# 在 Codex 中
harness-plan
# 输入："设计一个RESTful API的最佳实践指南"
```

### 2. 工作执行（后端自动选择）

```bash
# 智能后端选择
harness-work 3           # 自动选择最优后端

# 手动指定后端
harness-work 3 --backend claude   # 强制使用 Claude
harness-work 3 --backend codex    # 强制使用 Codex
```

### 3. 代码审查（平台优化）

```bash
# 自动使用平台优化的审查器
harness-review

# Claude 审查：更严格，注重安全
/harness-review --backend claude --strict

# Codex 审查：更快速，注重效率
/harness-review --backend codex
```

### 4. 发布管理（统一流程）

```bash
# 统一的发布流程
harness-release
# 自动：版本递增、变更日志生成、Git 标签
```

## 🌟 平台特性对比

### Claude Code 特有优势

```bash
# 1. 原生集成（性能最优）
/harness-work --backend claude
# - 无进程通信开销
# - 原生 Agent 协调
# - 实时审查反馈

# 2. 高级推理能力
harness-plan --backend claude
# - Claude 3.5/4 系列
# - 更强的代码理解能力
# - 更好的架构设计建议

# 3. 安全第一
harness-review --backend claude --strict
# - 内置安全检查
# - 严格的代码规范
# - 深度的漏洞分析
```

### Codex 特有优势

```bash
# 1. 成本效益
harness-work --backend codex
# - OpenAI 定价优势
# - 更高的吞吐量
# - 适合批量任务

# 2. 生态丰富
harness-work --backend codex
# - OpenAI 插件生态
# - 丰富的工具集成
# - 灵活的定制选项

# 3. 平衡性能
harness-work --backend codex
# - 质量与速度平衡
# - 可调节的性能参数
# - 适合敏捷开发
```

## 💡 使用场景推荐

### 场景 1: 新项目快速迭代

```bash
# 项目初期：用 Codex 快速迭代
harness-plan --backend codex    # 快速生成基础代码
harness-work 1-5 --backend codex   # 批量实现基础功能
harness-review --backend codex    # 快速代码审查
```

### 场景 2: 核心功能深度设计

```bash
# 核心模块：用 Claude 深度设计
harness-plan --backend claude    # 深度架构设计
harness-work "核心模块" --backend claude  # 高质量实现
harness-review --backend claude --strict  # 严格安全审查
```

### 场景 3: 混合使用策略

```bash
# 计划和设计用 Claude
harness-plan --backend claude
harness-work "架构设计" --backend claude

# 实现和测试用 Codex
harness-work "CRUD接口" --backend codex
harness-work "单元测试" --backend codex

# 审查用 Claude
harness-review --backend claude --strict
```

## 🔍 故障排查

### 问题 1: 插件无法识别

```bash
# 检查插件安装
# Claude Code
ls ~/.claude/plugins/cache/java-harness-market/

# Codex
ls ~/.codex/plugins/cache/java-harness/

# 验证版本
harness-version
```

### 问题 2: 后端连接失败

```bash
# 测试后端连接
harness-test --backend claude
harness-test --backend codex

# 查看后端状态
harness-doctor
```

### 问题 3: 配置不生效

```bash
# 查看当前配置
harness-config show

# 验证配置文件
cat harness.toml.bak

# 重置配置
harness-config reset
```

## 📊 性能优化建议

### Claude Code 性能优化

```toml
[platform.claude]
# 高强度推理获得最佳质量
effort = "high"        # 或 "max"（最慢但最佳）
review_rounds = 5        # 更多审查轮数
timeout = 900            # 更长超时时间
```

### Codex 性能优化

```toml
[platform.codex]
# 平衡质量和速度
effort = "medium"      # 平衡模式
review_rounds = 3        # 标准轮数
timeout = 600            # 标准超时
```

## 🎁 高级技巧

### 1. 环境变量控制

```bash
# 临时切换后端
export HARNESS_BACKEND=claude
harness-work 3

# 设置全局默认
echo 'export HARNESS_BACKEND=claude' >> ~/.zshrc
```

### 2. 项目级配置

```bash
# 不同项目使用不同后端
cd /project/backend-api
echo "backend = 'claude'" >> harness.toml.bak

cd /project/frontend-app
echo "backend = 'codex'" >> harness.toml.bak
```

### 3. 混合后端策略

```bash
# 复杂任务用 Claude，简单任务用 Codex
alias hw-claude='harness-work --backend claude'
alias hw-codex='harness-work --backend codex'

hw-claude "设计复杂的架构"
hw-codex "实现简单的接口"
```

## 📚 更多资源

- 📖 [架构设计文档](docs/dual-platform-compatibility.md)
- 📖 [配置指南](docs/dual-platform-config.md)
- 🔗 [Gitee 仓库](https://gitee.com/duxvfeng/java-harness)

## 🎉 实现状态

### Phase 8 实现完成（2026-08-08）

✅ **已完成的关键实现：**
- ✅ .codex-plugin/ 目录结构完整
- ✅ Codex plugin.json 配置就绪
- ✅ harness.toml 跨平台配置文件创建
- ✅ 双平台安装脚本完成
- ✅ 集成测试验证通过
- ✅ 与 Go 项目结构对等

**实际可用的文件：**
```bash
# 验证实际实现
ls -la .codex-plugin/plugin.json  # ✅ 存在
ls -la harness.toml.bak              # ✅ 存在 (15个配置节段)
ls -la install-*.sh             # ✅ 两个脚本都可执行
ls -la skills-codex/             # ✅ 包含3个技能
```

**从设计到实现的转化：**
- Phase 7: ✅ 完整的双平台设计（文档和架构）
- Phase 8: ✅ 实际的可运行实现（文件和脚本）
- **结果**: Java Harness 现在拥有与 Go 项目对等的双平台支持

开始享受跨平台的 AI 驱动开发体验！🚀