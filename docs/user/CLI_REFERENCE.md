# Java Harness CLI 命令参考手册

> **版本**: 4.1.0-java
> **更新时间**: 2026-08-03
> **命令总数**: 86个（与Go版本完全一致）

---

## 📖 目录

- [概述](#概述)
- [Hook命令组](#hook命令组)
- [Evidence命令组](#evidence命令组)
- [Plans命令组](#plans命令组)
- [核心工作流命令](#核心工作流命令)
- [维护命令](#维护命令)
- [会话管理](#会话管理)
- [审计命令](#审计命令)
- [监控命令](#监控命令)
- [工作树命令](#工作树命令)
- [CI命令](#ci命令)
- [内存命令](#内存命令)
- [收件箱命令](#收件箱命令)
- [其他命令](#其他命令)

---

## 概述

Java Harness CLI 是 Claude Code Harness 的 Java 实现，采用 picocli 框架，所有命令使用 kebab-case 命名格式。

### 基本用法

```bash
# 查看帮助
harness --help

# 查看版本
harness version

# 查看命令帮助
harness <command> --help
```

---

## Hook命令组

Hook命令组包含16个子命令，用于处理 Claude Code Hook 事件。

### 命令列表

| 命令 | 描述 |
|------|------|
| `harness hook pre-tool` | PreToolUse guardrail evaluation |
| `harness hook post-tool` | PostToolUse tampering/security checks |
| `harness hook permission` | PermissionRequest auto-approval |
| `harness hook session-start` | SessionStart env setup |
| `harness hook post-tool-failure` | PostToolUseFailure counter & escalation |
| `harness hook post-compact` | PostCompact WIP context re-injection |
| `harness hook notification` | Notification event logging |
| `harness hook permission-denied` | PermissionDenied event logging |
| `harness hook ask-user-question-normalize` | AskUserQuestion answer bridge |
| `harness hook session-init` | Session initialization + Plans.md summary |
| `harness hook session-cleanup` | SessionEnd temp file cleanup |
| `harness hook session-monitor` | Project state collection + session.json |
| `harness hook session-summary` | Session summary to session-log.md |
| `harness hook ci-status` | CI status check after push/PR |
| `harness hook subagent-start` | Track agent lifecycle start |
| `harness hook subagent-stop` | Track agent lifecycle stop |

### 使用示例

```bash
# PreTool hook - 读取stdin中的JSON输入
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"ls"}}' | harness hook pre-tool

# PostTool hook
echo '{"hook_event_name":"PostToolUse","tool_name":"Write"}' | harness hook post-tool

# 查看hook帮助
harness hook --help
```

---

## Evidence命令组

### 命令列表

| 命令 | 描述 |
|------|------|
| `harness evidence collect` | Collect evidence (test results, build logs) |

### 使用示例

```bash
# 从stdin收集证据
echo "Test passed" | harness evidence collect

# 指定标签
echo "Build log" | harness evidence collect --label build

# 从文件收集
harness evidence collect --file test-results.txt --label test
```

### 选项

| 选项 | 描述 | 默认值 |
|------|------|--------|
| `--label` | 证据标签 | `general` |
| `--file` | 从文件读取内容 | stdin |

---

## Plans命令组

### 命令列表

| 命令 | 描述 |
|------|------|
| `harness plans check-deps` | Verify done tasks only depend on closed tasks |

### 使用示例

```bash
# 检查默认Plans.md文件的依赖
harness plans check-deps

# 检查指定文件
harness plans check-deps my-plans.md
```

---

## 核心工作流命令

### plan

生成计划提示，供主机执行。

```bash
# 生成计划
harness plan
```

### work

生成工作提示和任务上下文，供主机执行（不调用LLM）。

```bash
# 执行任务
harness work <taskID>

# 示例
harness work 1
```

### review

生成审查提示和任务上下文，供主机执行。

```bash
# 审查任务
harness review <taskID>

# 示例
harness review 1
```

### release

生成发布提示，供主机执行。

```bash
# 生成发布提示
harness release

# 检查模式
harness release --check
```

### gen

从 hosts.toml 生成每个主机的 hooks.json。

```bash
# 生成hooks
harness gen hooks

# 检查模式（与golden文件对比）
harness gen hooks --check

# 指定根目录
harness gen hooks /path/to/project
```

### sprint-contract

从 Plans.md 生成 sprint-contract JSON。

```bash
# 生成sprint-contract
harness sprint-contract <task-id>

# 指定plans文件
harness sprint-contract <task-id> my-plans.md

# 指定输出文件
harness sprint-contract <task-id> Plans.md output.json
```

---

## 维护命令

### init

在项目根目录创建 harness.toml 模板。

```bash
# 在当前目录初始化
harness init

# 指定根目录
harness init /path/to/project
```

### sync

从 harness.toml 生成 CC 文件。

```bash
# 同步文件
harness sync

# 指定根目录
harness sync /path/to/project
```

### validate

验证 SKILL.md / agent frontmatter。

```bash
# 验证所有
harness validate all

# 只验证skills
harness validate skills

# 只验证agents
harness validate agents

# 指定根目录
harness validate all /path/to/project
```

### doctor

健康检查加上迁移状态/报告。

```bash
# 基本健康检查
harness doctor

# 显示迁移状态
harness doctor --migration

# 显示迁移报告
harness doctor --migration-report

# 指定根目录
harness doctor /path/to/project
```

### codex-loop

运行 Codex 原生的长时间运行循环。

```bash
# 启动循环
harness codex-loop start

# 查看状态
harness codex-loop status

# 停止循环
harness codex-loop stop
```

---

## 会话管理

### session

会话管理命令。

```bash
# 会话管理
harness session
```

### session-register

注册会话。

```bash
harness session-register
```

### session-unregister

注销会话。

```bash
harness session-unregister
```

---

## 审计命令

### self-audit

审计 settings.local.json 命令钩子。

```bash
# 审计hooks
harness self-audit hooks --file <path>

# 验证baseline
harness self-audit baseline --settings <path> --baseline <path>
```

### retired-alias

扫描仓库中的已废弃别名残留。

```bash
# 扫描已废弃别名
harness retired-alias scan

# 指定根目录
harness retired-alias scan /path/to/project
```

---

## 监控命令

### night-watch

生成夜巡报告。

```bash
# 生成报告
harness night-watch report

# 干运行模式
harness night-watch report --dry-run
```

### mirror

报告 skills/ 镜像漂移。

```bash
# 查看状态
harness mirror status

# 验证镜像
harness mirror verify

# JSON输出
harness mirror status --json

# 指定根目录
harness mirror status /path/to/project
```

### plans-watcher

监视 Plans.md 文件变化。

```bash
harness plans-watcher
```

---

## 工作树命令

### wt

工作树指纹操作。

```bash
# 捕获指纹
harness wt fingerprint capture --output <path>

# 对比指纹
harness wt fingerprint diff --before <path1> --after <path2>
```

### worktree-create

创建 git 工作树。

```bash
harness worktree-create
```

### worktree-remove

删除 git 工作树。

```bash
harness worktree-remove
```

---

## CI命令

### ci-check

CI 检查命令。

```bash
harness ci-check
```

### ci-status

CI 状态检查（在 push/PR 后）。

```bash
harness ci-status
```

---

## 内存命令

### mem

管理 harness-mem 伴侣。

```bash
# 查看状态
harness mem status

# 设置
harness mem setup

# 更新
harness mem update

# 健康检查
harness mem doctor

# 关闭
harness mem off

# 清除
harness mem purge

# 健康状态
harness mem health
```

### memory-bridge

内存桥接操作。

```bash
harness memory-bridge
```

---

## 收件箱命令

### inbox

收件箱管理。

```bash
# 收件箱管理
harness inbox
```

### inbox-check

读取 livemsg 收件箱（fail-open）。

```bash
# 检查收件箱
harness inbox check --team <team> --agent <agent> --db <path>

# 监视收件箱
harness inbox monitor --team <team> --agent <agent> --db <path>
```

---

## 其他命令

### status

显示所有追踪的代理状态。

```bash
harness status
```

### breezing-signal

处理 Breezing 模式信号。

```bash
harness breezing-signal
```

### failure-codifier

生成 failure-rule.v1 提案（人工审批门控）。

```bash
# 干运行模式
harness failure-codifier propose --dry-run
```

### impact-score

计算 judgment-card impact_score。

```bash
# 计算影响分数
harness impact-score --files-changed 10 --lines-changed 100

# 指定最低类别
harness impact-score --files-changed 10 --lines-changed 100 --floor-category CAT
```

### pre-compact

评估是否应阻止 PreCompact。

```bash
harness pre-compact
```

### channels-wake

桥接通道健康检查。

```bash
# 检查通道健康
harness channels-wake check
```

### version

打印版本。

```bash
harness version
# 输出: 4.1.0-java (Hokage)
```

---

## 事件处理命令

这些命令用于处理各种事件：

| 命令 | 描述 |
|------|------|
| `harness auto-broadcast` | 自动广播消息 |
| `harness auto-cleanup` | 自动清理临时文件 |
| `harness auto-test` | 自动运行测试 |
| `harness config-change` | 处理配置变更 |
| `harness elicitation` | Elicitation handler |
| `harness elicitation-result` | Elicitation result handler |
| `harness emit-trace` | 发出跟踪事件 |
| `harness notification-ext` | 扩展通知处理 |
| `harness permission-denied-ext` | 扩展权限拒绝处理 |
| `harness task-completed-ext` | 扩展任务完成处理 |
| `harness runtime-reactive` | 运行时响应处理 |

---

## 文件租约命令

| 命令 | 描述 |
|------|------|
| `harness post-tool-use-file-lease` | Post tool use file lease handler |
| `harness pre-tool-use-file-lease` | Pre tool use file lease handler |

---

## 设置命令

| 命令 | 描述 |
|------|------|
| `harness setup-init` | 初始化设置 |
| `harness setup-maintenance` | 设置维护任务 |
| `harness skill-mirror-drift` | 检测技能镜像漂移 |

---

## 追踪命令

| 命令 | 描述 |
|------|------|
| `harness track-changes` | 跟踪文件变更 |
| `harness track-command` | 跟踪命令使用 |
| `harness usage-tracker` | 跟踪使用统计 |
| `harness todo-sync` | 同步待办事项 |
| `harness tdd-check` | 检查TDD合规性 |

---

## 其他命令

| 命令 | 描述 |
|------|------|
| `harness browser-guide` | 打开浏览器指南 |
| `harness clear-pending` | 清除待处理任务 |
| `harness commit-cleanup` | 提交后清理 |
| `harness fix-proposal` | 生成修复提案 |
| `harness inject-policy` | 注入策略规则 |
| `harness instructions-loaded` | 处理指令加载事件 |
| `harness log-toolname` | 记录工具名称使用 |
| `harness stop-evaluator` | 停止评估器 |
| `harness stop-failure` | 处理停止失败 |
| `harness teammate-idle` | 处理队友空闲事件 |

---

## 配置

### harness.toml

项目配置文件，位于项目根目录。

```toml
[project]
name = "my-project"
version = "1.0.0"

[hooks]
pre-tool = true
post-tool = true
```

### 环境变量

| 变量 | 描述 |
|------|------|
| `CLAUDE_PLUGIN_DATA` | 插件数据目录 |
| `HARNESS_ROOT` | Harness 根目录 |

---

## 故障排查

### 常见问题

1. **命令找不到**
   ```bash
   # 检查PATH
   which harness
   
   # 或使用完整路径
   ./harness version
   ```

2. **编译失败**
   ```bash
   # 清理并重新编译
   mvn clean compile
   ```

3. **测试失败**
   ```bash
   # 运行特定测试
   mvn test -Dtest=HarnessCLIIntegrationTest
   ```

---

## 相关文档

- [安装指南](../installation.md)
- [配置指南](../configuration.md)
- [API文档](../api/)
- [开发者指南](../developer/)
