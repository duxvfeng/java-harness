# Claude Code Harness - Java 实现版

Java 原生实现的 Claude Code Harness，提供 CLI Gateway 核心功能，包括 Hook 协议处理、Guardrail 安全引擎和快速响应机制。

## 🎯 项目概述

这是 [claude-code-harness](https://github.com/your-org/claude-code-harness) 的 Java 原生实现版本，通过 GraalVM 编译为 Native Image，实现 **<10ms 的 hook 响应时间**，为 Claude Code 提供实时安全策略执行。

### 核心价值

- **🚀 高性能**: GraalVM Native Image 编译，亚毫秒级响应时间
- **🔒 安全防护**: 27 个 Guardrail 规则（R01-R27）全覆盖
- **📡 Hook 协议**: 完整的 Claude Code Hook 事件处理（14 个 hook 子命令）
- **🎯 模块化设计**: 命令组 + 独立命令，与 Go 版本功能对等
- **📋 完整 CLI**: 86 个 CLI 命令，完全复制 Go 版本的命令结构
- **🌐 双平台**: 支持 Claude Code 和 Codex CLI 双平台（Beta）
- **💾 会话管理**: Token 感知的自动保存和智能恢复系统（Phase 11 新功能）
- **🤖 智能选择**: 根据任务复杂度自动选择最优 AI 模型（Phase 12 新功能）

### 双平台支持

**🎉 Phase 7 新功能**: Java Harness 现在支持双平台运行！

| 平台 | 状态 | 功能支持 | 安装方式 |
|------|------|----------|----------|
| **Claude Code** | ✅ 稳定 | 完整功能（21个技能 + 86个命令） | Marketplace / 手动 |
| **Codex CLI** | 🚧 Beta | 基础功能（配置兼容 + 核心技能） | 手动配置 |

**双平台特性**:
- ✅ **平台自动检测**: 自动识别当前运行环境
- ✅ **配置兼容层**: 统一的 `harness.toml` 配置文件
- ✅ **无缝切换**: 同一代码库支持两个平台
- ✅ **功能等价**: 核心功能在两个平台上保持一致

**安装限制**:
- Codex 平台支持为 **Beta 功能**，部分高级技能尚在迁移中
- 需要手动配置 `.codex/config.toml` 文件
- 建议优先使用 Claude Code 获得完整功能体验

### 当前状态

- **版本**: 4.1.1
- **Go 版本对应**: claude-code-harness v5.5.0
- **功能完成度**: Phase 12 已完成（智能模型选择系统）
- **文档状态**: 文档体系完整

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **操作系统**: Windows / macOS / Linux
- **内存**: 最少 4GB RAM
- **磁盘空间**: 最少 500MB

### 安装方式

#### 方式 1: 使用预编译二进制文件（推荐）

1. **下载对应平台的二进制文件**:

```bash
# Windows (x64)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-windows-amd64.exe -o harness.exe

# Linux (AMD64)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-linux-amd64 -o harness
chmod +x harness

# macOS (Intel)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-amd64 -o harness
chmod +x harness

# macOS (Apple Silicon)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-arm64 -o harness
chmod +x harness
```

2. **验证安装**:

```bash
./harness --version
# 输出: harness 4.1.1
```

#### 方式 2: 使用 JAR 文件

```bash
# 下载 JAR 文件
curl -L https://github.com/your-org/java-harness/releases/latest/download/java-harness-cli-4.1.1.jar -o harness.jar

# 运行
java -jar harness.jar --version
```

#### 方式 3: 从源码编译

```bash
# 克隆仓库
git clone https://github.com/your-org/java-harness.git
cd java-harness

# 编译项目
mvn clean package

# 运行
java -cp java-harness-cli/target/java-harness-cli-4.1.1.jar \
     com.chachamaru.harness.cli.HarnessCli --version
```

### 5分钟快速体验

```bash
# 1. 验证安装
harness --version

# 2. 查看帮助信息
harness --help

# 3. 测试 Hook 功能
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"ls -la"}}' | \
  harness hook pre-tool

# 4. 生成项目配置
harness init

# 5. 查看项目状态
harness status
```

### Claude Marketplace 安装

#### Claude Code 平台安装（推荐）

如果你使用 Claude Code，可以通过命令行方式从 Gitee 仓库安装：

```bash
# 1. 添加插件源
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git

# 2. 安装插件
/plugin install
```

**安装说明**：
- 第一步将 Java Harness 添加到 Claude Code 的插件市场源
- 第二步执行插件安装，自动下载并配置所需组件
- 安装完成后，重启 Claude Code 即可使用
- 如需更新插件，重新执行上述命令即可

#### Codex CLI 平台安装（Beta）

Codex 平台支持为 Beta 功能，需要手动配置：

```bash
# 1. 克隆仓库
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness

# 2. 配置 Codex 环境
mkdir -p .codex
cat > .codex/config.toml << EOF
[harness]
version = "5.0.0-java"
backend = "codex"

[plan]
enable = true
EOF

# 3. 设置环境变量
export CODEX_CLI=1

# 4. 验证安装
java -jar java-harness-cli/target/java-harness-cli-5.0.0-java.jar --version
```

**⚠️ Beta 限制**：
- Codex 平台支持为 **实验性功能**
- 部分高级技能可能不可用
- 建议优先使用 Claude Code 获得完整体验
- 遇到问题请提 Issue 反馈

## ⚙️ 配置说明

### 配置文件路径

Java Harness 使用统一的 TOML 配置文件，不同平台有各自的配置路径：

| 平台 | 配置文件路径 | 优先级 |
|------|-------------|--------|
| **Claude Code** | `.claude/config.toml` | 最高 |
| **Codex** | `.codex/config.toml` | 最高 |
| **通用** | `harness.toml` | 默认 |

### 配置优先级

配置加载优先级（从高到低）：
1. 平台特定配置（`.claude/config.toml` 或 `.codex/config.toml`）
2. 标准配置文件（`harness.toml`）
3. 平台默认值

### 基础配置示例

创建 `harness.toml` 文件：

```toml
[harness]
version = "5.0.0-java"
backend = "claude"  # 或 "codex"，或 "auto" 自动检测
project_root = "."

[plan]
enable = true
auto_save = true
backup_count = 5

[work]
enable = true
default_effort = "medium"  # low, medium, high, max
auto_review = true

[review]
enable = true
strict_mode = false
max_review_rounds = 3

[release]
enable = true
auto_version = true
changelog_enabled = true

[sync]
enable = true
auto_sync = false
sync_on_start = false

[hooks]
enable = true
pre_tool_use = true
post_tool_use = true
permission_request = true

[state]
persist = true
session_file = ".claude/state/session.jsonl"
work_file = ".claude/state/work.jsonl"

[logging]
level = "INFO"  # TRACE, DEBUG, INFO, WARN, ERROR
file = ".claude/logs/harness.log"
```

#### 分支隔离配置 🆕

在 `.claude/settings.json` 中配置智能分支隔离行为：

```json
{
  "branchIsolation": {
    "mainBranch": "force",
    "featureBranch": "ask"
  }
}
```

**配置说明**:
- `mainBranch`: 主分支策略（force/ask/skip）
- `featureBranch`: 功能分支策略（force/ask/skip）

**推荐配置**:
- 主分支使用 `force`（强制保护）
- 功能分支使用 `ask`（灵活决策）

### 平台特定配置

#### Claude Code 平台

`.claude/config.toml`:

```toml
[harness]
backend = "claude"
version = "5.0.0-java"

[work]
default_effort = "high"
```

#### Codex 平台

`.codex/config.toml`:

```toml
[harness]
backend = "codex"
version = "5.0.0-java"

[work]
default_effort = "max"
```

### 配置验证

验证配置文件是否正确：

```bash
# 验证配置文件语法
harness config validate

# 查看当前配置
harness config show

# 查看配置路径
harness config path
```

## 📖 核心功能

### Hook 系统

完整的 Claude Code Hook 协议实现，支持 14 个 hook 子命令：

| Hook 子命令 | 功能描述 |
|------------|---------|
| `hook pre-tool` | 工具使用前的安全检查 |
| `hook post-tool` | 工具使用后的篡改检测 |
| `hook permission` | 权限请求自动批准 |
| `hook session-start` | 会话开始环境设置 |
| `hook session-init` | 会话初始化 + Plans.md 摘要 |
| `hook session-cleanup` | 会话结束临时文件清理 |
| `hook session-monitor` | 项目状态收集 + session.json |
| `hook session-summary` | 会话总结到 session-log.md |
| `hook ci-status` | 推送/PR 后的 CI 状态检查 |
| `hook subagent-start` | Agent 生命周期跟踪开始 |
| `hook subagent-stop` | Agent 生命周期跟踪停止 |
| `hook notification` | 通知事件日志记录 |
| `hook permission-denied` | 权限拒绝事件日志记录 |

### CLI 命令

提供 86 个 CLI 命令，覆盖以下功能类别：

**核心工作流**:
- `plan` - 生成计划提示供主机执行
- `work <taskID>` - 生成工作提示 + 任务上下文  
- `review <taskID>` - 生成审查提示 + 任务上下文
- `release` - 生成发布提示供主机执行

**计划管理**:
- `plans check-deps` - 验证已完成任务仅依赖已关闭任务
- `sprint-contract` - 从 Plans.md 生成 sprint 契约

**证据收集**:
- `evidence collect` - 收集证据（测试结果、构建日志）

**系统管理**:
- `doctor` - 健康检查 + 迁移状态/报告
- `validate` - 验证 SKILL.md / agent frontmatter
- `sync [root]` - 从 harness.toml 生成 CC 文件
- `init [root]` - 在项目根创建 harness.toml 模板

### 安全规则（Guardrails）

27 个安全规则（R01-R27）全覆盖：

**系统安全**:
- R01: 阻止提权命令（sudo, su）
- R02: 保护敏感路径（/etc, /sys, /proc）
- R03: 阻止重定向绕过（|, nul）
- R04: 项目路径边界检查
- R05: 防止递归删除（rm -rf）

**Git 安全**:
- R06: 阻止强制推送（git push --force）
- R11: 硬重置防护（git reset --hard）
- R12: 主分支推送保护

**文件安全**:
- R07: Codex 写入监控
- R08: Breezing 写入监控
- R09: 密钥文件保护（*.pem, *.key）
- R13: 包文件监控（package.json, pom.xml）
- R18: 配置文件写入保护

**生产环境保护**:
- R15: 生产部署保护（kubectl, docker）
- R16: 数据库写入保护
- R17: 容器管理保护
- R19: 可执行文件下载保护
- R20: 网络暴露保护
- R25: 服务重启保护

### 会话管理系统 💾

**Phase 11 新功能**: 完善的会话保存和恢复系统，解决大型AI开发任务中的context满问题！

Java Harness 现在支持智能会话管理，提供无缝的开发体验：

#### 核心特性
- ✅ **Token监控**: 实时监控Token使用率，智能触发自动保存
- ✅ **自动保存**: 80%/90% Token阈值自动触发保存机制
- ✅ **智能恢复**: 新会话启动时自动检测并建议恢复工作状态
- ✅ **压缩存储**: GZIP压缩技术，节省70%+存储空间
- ✅ **完整集成**: 与Hook系统、任务管理、Git状态完全集成

#### 使用方式

**自动保存功能**:
```bash
# 系统自动监控Token使用率，超过阈值时自动保存
# 💾 [Token 80%] 自动保存: 20260809-173045-token-80
# 💾 [Token 90%] 强制保存: 20260809-180000-token-90
```

**手动保存功能**:
```bash
# 手动保存当前会话
/harness-save-session "完成Task 11.8实现"

# 强制保存（忽略间隔限制）
/harness-save-session --force
```

**会话恢复功能**:
```bash
# 查看可恢复的会话
/harness-list-sessions --recent 5

# 恢复到特定会话
/harness-restore-session 20260809-174530-abc123

# 完整恢复（包含所有对话历史）
/harness-restore-session 20260809-174530-abc123 --full
```

**存储管理功能**:
```bash
# 查看所有保存的会话
/harness-list-sessions --all

# 清理旧会话
/harness-cleanup-sessions --keep 10 --older-than 72

# 查看会话详情
/harness-show-session 20260809-174530-abc123
```

#### 性能指标
| 指标 | 目标值 | 实际值 | 状态 |
|------|-------|--------|------|
| 保存时间 | <3秒 | ~2.1秒 | ✅ 超额 |
| 恢复时间 | <5秒 | ~3.8秒 | ✅ 超额 |
| 压缩率 | >70% | ~78% | ✅ 超额 |
| 存储占用 | <10MB/会话 | ~7.2MB | ✅ 超额 |
| 保存成功率 | >99% | 99.8% | ✅ 达成 |
| 恢复成功率 | >98% | 98.9% | ✅ 达成 |

#### 配置示例
```toml
[session]
# 自动保存配置
autoSave = true              # 启用自动保存
tokenThreshold80 = true      # 80% Token时触发
tokenThreshold90 = true      # 90% Token时强制触发
saveIntervalMinutes = 30     # 最小保存间隔（分钟）

# 恢复提示配置
restorePrompt = true         # 启用恢复提示
autoShowPrompt = true        # 自动显示提示

# 存储配置
storageRoot = ".claude/state/session-saves"  # 存储目录
maxStorageMB = 100           # 最大存储空间（MB）
compressionEnabled = true    # 启用压缩
compressionLevel = 6          # 压缩级别（0-9）
maxHistoryAgeDays = 7        # 最大保存天数

# 清理配置
autoCleanup = true           # 自动清理过期会话
keepRecentSessions = 10      # 保留最近会话数量
```

📖 **完整用户指南**: [会话管理系统用户指南](docs/user-guide/session-management.md)
📊 **技术报告**: [Phase 11 完成报告](docs/superpowers/reports/PHASE_11_COMPLETION_REPORT.md)

### 智能模型选择系统 🆕

**Phase 12 新功能**: 根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现！

Java Harness 现在支持智能模型选择，为不同复杂度的任务匹配合适的模型能力：

#### 核心特性
- ✅ **复杂度评分**: 基于文件数、目录、关键字、失败历史的智能评分
- ✅ **模型等级**: 四个等级（FAST/BALANCED/QUALITY/POWERFUL）精准匹配
- ✅ **降级机制**: 完整的降级链，确保系统总能找到可用模型
- ✅ **配置优先级**: settings.json > harness.toml > 默认配置
- ✅ **性能优化**: 单次选择 < 100ms，支持 20+ 并发线程

#### 工作原理

**复杂度评分规则**:
| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 个文件以上 | +1 |
| 目录 | 包含 core/、guardrails/、security/ | +1 |
| 关键字 | 包含 architecture、security、design、migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |

**模型等级映射**:
| 复杂度分数 | 模型等级 | 主要模型 | 环境变量 |
|------------|----------|---------|---------|
| 0-2 | FAST (低复杂度) | FABLE | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED (中等复杂度) | HAIKU | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY (高复杂度) | SONNET | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL (超高复杂度) | OPUS | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

#### 使用方式

**自动启用（推荐）**:
```bash
# 系统会自动启用智能模型选择
/harness-work 3

# 系统自动：
# 1. 计算任务复杂度分数
# 2. 根据分数选择模型等级
# 3. 执行降级链找到可用模型
# 4. 返回 WorkerSpawnConfig
```

**环境变量配置（可选）**:
```bash
# 设置默认模型
export ANTHROPIC_MODEL="glm-4.7"

# 设置等级特定模型
export ANTHROPIC_DEFAULT_FABLE_MODEL="claude-fable-5-20250514"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_DEFAULT_OPUS_MODEL="claude-opus-4-20250514"
```

**项目配置（可选）**:
```json
{
  "modelSelection": {
    "enabled": true,
    "strategy": "effortBased",
    "tierMapping": {
      "fast": {
        "scoreRange": [0, 2],
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      }
    }
  }
}
```

#### 性能指标
| 指标 | 目标值 | 实际值 | 状态 |
|------|-------|--------|------|
| 单次选择时间 | <100ms | ~0ms | ✅ 超额 |
| 并发支持 | 10+ 线程 | 20+ 线程 | ✅ 超额 |
| 内存占用 | <10MB | ~0MB | ✅ 超额 |
| 选择成功率 | >98% | 100% | ✅ 超额 |
| 吞吐量 | 10K+ ops/s | 200K ops/s | ✅ 超额 |

#### 实际应用示例

**简单任务（格式化）**:
```bash
/harness-work format-code
# 自动选择 FAST 等级（FABLE）
# 复杂度分数：0-2
```

**中等复杂度任务（单元测试）**:
```bash
/harness-work add-unit-tests
# 自动选择 BALANCED 等级（HAIKU）
# 复杂度分数：3-4
```

**高复杂度任务（核心重构）**:
```bash
/harness-work refactor-core-module
# 自动选择 QUALITY 等级（SONNET）
# 复杂度分数：5-6
```

**超高复杂度任务（架构重构）**:
```bash
/harness-work architecture-refactor
# 自动选择 POWERFUL 等级（OPUS）
# 复杂度分数：≥7
```

📖 **完整用户指南**: [智能模型选择系统用户指南](docs/user-guides/smart-model-selection-guide.md)
📊 **技术报告**: [Phase 12 完成报告](docs/superpowers/reports/PHASE_12_COMPLETION_REPORT.md)

### 智能分支隔离检测 🆕

**Phase 10 新功能**: 自动分支保护系统，防止主分支意外提交！

**三大策略**:
- `force` - 强制隔离（主分支自动保护）
- `ask` - 用户选择（功能分支灵活决策）
- `skip` - 跳过隔离（已隔离环境无需重复）

**核心特性**:
- ✅ **自动检测**: 智能识别分支类型（main/feature/worktree）
- ✅ **智能配置**: 支持不同分支类型的自定义策略
- ✅ **用户友好**: 清晰的交互提示和错误处理
- ✅ **状态跟踪**: 记录所有决策用于审计和调试
- ✅ **完整诊断**: Git 环境健康检查和故障排除建议

**快速开始**:
```bash
# 自动检测并应用适当策略
bash scripts/branch-isolation/handle-isolation.sh --auto

# 检查当前分支状态
bash scripts/branch-isolation/detect-branch.sh --info

# 运行环境诊断
bash scripts/branch-isolation/git-error-handler.sh
```

**配置示例** (`.claude/settings.json`):
```json
{
  "branchIsolation": {
    "mainBranch": "force",      // 主分支强制隔离
    "featureBranch": "ask"      // 功能分支询问用户
  }
}
```

📖 **完整文档**: 参见 [BRANCH-ISOLATION.md](docs/BRANCH-ISOLATION.md)

## 🏗️ 架构设计

### 模块结构

```
java-harness/
├── java-harness-cli/              # CLI 模块（主入口）
│   └── command/                   # 86 个命令类
├── java-harness-shared/           # 共享模块
├── java-harness-foundation/       # 基础模块
├── java-harness-protocol/         # 协议模块
├── java-harness-security/         # 安全模块
├── java-harness-workflow/         # 工作流模块
├── java-harness-tools/            # 工具模块
├── java-harness-collaboration/    # 协作模块
├── java-harness-ci/               # CI 模块
├── java-harness-service/          # 服务模块
└── java-harness-distribution/     # 分发模块
```

### 技术栈

- **语言**: Java 17+
- **构建工具**: Maven
- **CLI 框架**: picocli 4.7
- **JSON 处理**: Jackson 2.15.2
- **YAML 处理**: SnakeYAML
- **日志**: SLF4J 2.0.9 + Logback 1.4.11
- **测试**: JUnit 5.10.0
- **Native 编译**: GraalVM 23.1.0+

### 性能目标

| 指标 | 目标值 | 实际值 |
|------|-------|--------|
| Hook 响应时间 | < 10ms (95th) | ~8ms |
| Workflow 启动 | < 100ms | ~85ms |
| 简单 Workflow 执行 | < 1s | ~0.9s |
| 内存占用 (Native) | < 50MB | ~42MB |
| 启动时间 (Native) | < 100ms | ~75ms |

## 💡 使用示例

### Hook 输入输出示例

**输入（stdin）**:
```json
{
  "session_id": "test-session-20260808",
  "transcript_path": "/project/.claude/transcript.jsonl",
  "cwd": "/project",
  "permission_mode": "default",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {
    "command": "sudo rm -rf /etc/passwd"
  },
  "plugin_root": "/plugin"
}
```

**输出（stdout）**:
```json
{
  "hookEventName": "PreToolUse",
  "permissionDecision": "block",
  "permissionDecisionReason": "R01: 阻止提权命令 - 检测到 sudo 使用",
  "additionalContext": {
    "ruleId": "R01",
    "ruleName": "阻止提权命令",
    "matched": true
  }
}
```

### 命令使用示例

```bash
# 1. 生成项目配置
harness init
# 创建 .claude/harness.toml.bak 配置文件

# 2. 验证配置
harness validate
# 验证 SKILL.md 和 agent frontmatter 格式

# 3. 检查依赖
harness plans check-deps
# 验证 Plans.md 中的任务依赖关系

# 4. 生成 sprint 契约
harness sprint-contract
# 从 Plans.md 生成 sprint-contract.json

# 5. 查看系统状态
harness status
# 显示所有 tracked agent 状态

# 6. 健康检查
harness doctor
# 运行完整的系统健康检查
```

## 🧪 测试

### 运行单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=HookCodecTest

# 运行特定测试方法
mvn test -Dtest=HookCodecTest#testDecodePreToolUse
```

### 集成测试

```bash
# 模拟 Hook 输入测试
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"sudo rm -rf /"}}' | \
  harness hook pre-tool

# 预期输出：{"permissionDecision":"block","permissionDecisionReason":"R01: 阻止提权命令"}
```

## 🔧 开发指南

### 添加新的 Guardrail 规则

1. 在 `java-harness-security/src/main/java/com/chachamaru/harness/security/guardrail/rules/` 创建新规则类
2. 实现 `Rule` 接口
3. 在 `GuardrailRegistry` 中注册

```java
package com.chachamaru.harness.security.guardrail.rules;

public class R28CustomRule implements Rule {
    @Override
    public String getId() {
        return "R28";
    }

    @Override
    public String getName() {
        return "自定义规则";
    }

    @Override
    public boolean matches(HookInput input) {
        // 匹配条件
        return input.getToolName().equals("CustomTool");
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        // 评估逻辑
        return GuardrailResult.blocked("R28: 自定义规则阻止");
    }
}
```

### 添加新的 CLI 命令

1. 在 `java-harness-cli/src/main/java/com/chachamaru/harness/cli/command/` 创建命令类
2. 使用 picocli 注解定义命令
3. 在 `CommandRegistry` 中注册

```java
package com.chachamaru.harness.cli.command;

@Command(name = "my-command", mixinStandardHelpOptions = true,
        description = "我的自定义命令")
public class MyCommand implements Runnable {

    @Override
    public void run() {
        // 命令实现
        System.out.println("Hello from my command!");
    }
}
```

## 📚 文档

### 核心文档

- **[安装指南](docs/user-guide/installation.md)** - 详细的安装步骤和系统要求
- **[架构文档](docs/developer-guide/architecture.md)** - 完整的架构设计和模块说明
- **[API 参考](docs/reference/api-reference.md)** - API 接口详细说明
- **[文档索引](docs/README.md)** - 完整文档导航

### 参考文档

- **[技术文档备份](docs/reference/backup/)** - 历史技术文档归档
- **[Superpowers 文档](docs/reference/superpowers-archive/)** - 临时文档归档

## 🤝 贡献指南

### 开发规范

1. **遵循项目规范**: 参考 CLAUDE.md 中的开发指南
2. **代码风格**: 遵循 Java 代码规范
3. **测试覆盖**: 确保新功能有相应的单元测试
4. **文档更新**: 更新相关文档以反映新功能

### 提交流程

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某个功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### Commit 规范

遵循 Conventional Commits 规范：

- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档更新
- `style:` 代码格式调整
- `refactor:` 代码重构
- `test:` 测试相关
- `chore:` 构建过程或辅助工具的变动

## 📄 许可证

本项目与 claude-code-harness 主项目保持相同的许可证。

## 📞 联系方式

- **主项目**: https://github.com/your-org/claude-code-harness
- **问题反馈**: [GitHub Issues](https://github.com/your-org/java-harness/issues)
- **讨论区**: [GitHub Discussions](https://github.com/your-org/java-harness/discussions)

## 🙏 致谢

感谢 claude-code-harness 主项目提供的设计规范和技术指导。

---

**版本**: 4.1.1  
**最后更新**: 2026-08-10  
**维护者**: Java Harness Team
