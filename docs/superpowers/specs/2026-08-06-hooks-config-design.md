# Java Hooks 配置设计文档

**版本**: 1.0.0
**日期**: 2026-08-06
**作者**: dxf
**状态**: 设计阶段

---

## 1. 概述

### 1.1 目标

为 java-harness 项目创建正确的 hooks 配置，实现完整的 16 个 hooks 自动安全审查功能，避免之前错误的配置导致的进程爆炸和内存耗尽问题。

### 1.2 背景

**问题历史**：
- 之前使用了外部 Go 项目 "claude-code-harness" 的 hooks.json（64KB，65个钩子）
- 该配置尝试调用不存在的 `bin/harness` 命令
- 导致无限进程循环、内存耗尽、JVM 崩溃（28个 hs_err_pid*.log 文件）

**解决方案**：
- 创建专为 Java 项目设计的 hooks 配置
- 使用 Native Image 二进制实现 <10ms 响应时间
- 精细的匹配条件最大化性能
- 完整的错误处理和容错机制

---

## 2. 架构设计

### 2.1 文件结构

```
java-harness/
├── harness.toml                    # 主配置文件（手动维护）
├── hooks/
│   └── hooks.json                  # Hook 配置源文件（手动维护）
├── .claude-plugin/                 # 自动生成目录
│   ├── plugin.json                 # 由 PluginGenerator 生成
│   ├── settings.json               # 由 SettingsGenerator 生成
│   ├── hooks.json                  # 由 HooksSyncer 复制
│   └── marketplace.json            # 手动维护（marketplace 发布配置）
├── bin/
│   └── java-harness                # Native Image 二进制（构建后）
└── target/
    └── java-harness-cli.jar        # 编译后的 JAR
```

### 2.2 文件生成流程

```
1. harness.toml → ConfigReader.parse() → SyncConfig（内存对象）
2. SyncConfig → PluginGenerator.generate() → .claude-plugin/plugin.json
3. SyncConfig → SettingsGenerator.generate() → .claude-plugin/settings.json
4. hooks/hooks.json → HooksSyncer.sync() → .claude-plugin/hooks.json
5. marketplace.json → 手动维护
```

### 2.3 SyncSkill 集成

用户运行 `/harness-sync` skill 时：
1. 读取 harness.toml 配置
2. 自动生成 plugin.json、settings.json
3. 同步 hooks.json
4. 检测配置漂移（DriftDetector）
5. 返回 SyncResult 包含生成文件列表和警告

---

## 3. Hooks 配置详细设计

### 3.1 hooks.json 结构

```json
{
  "description": "java-harness: automation hooks",
  "hooks": {
    "PreToolUse": [...],
    "PostToolUse": [...],
    "PermissionRequest": [...],
    "SessionStart": [...],
    "SessionEnd": [...],
    "PostToolUseFailure": [...],
    "PermissionDenied": [...],
    "AskUserQuestion": [...],
    "SessionInit": [...],
    "SessionMonitor": [...],
    "PostCompact": [...],
    "Notification": [...],
    "CIStatus": [...],
    "SubagentStart": [...],
    "SubagentStop": [...]
  }
}
```

### 3.2 16 个 Hooks 分类

#### A. 核心安全 Hooks（5个）

| Hook Name | Event | 命令 | Matcher | Timeout | 描述 |
|-----------|-------|------|---------|---------|------|
| **pre-tool** | PreToolUse | `bin/java-harness hook pre-tool` | 所有工具 | 10s | 所有工具调用前的安全检查 |
| **post-tool** | PostToolUse | `bin/java-harness hook post-tool` | 所有工具 | 5s | 工具调用后的篡改检查 |
| **permission** | PermissionRequest | `bin/java-harness hook permission` | Edit\|Write\|Bash | 10s | 权限请求的自动批准逻辑 |
| **post-tool-failure** | PostToolUseFailure | `bin/java-harness hook post-tool-failure` | - | 5s | 工具失败时的计数和升级 |
| **permission-denied** | PermissionDenied | `bin/java-harness hook permission-denied` | - | 5s | 权限拒绝事件记录 |

#### B. 会话管理 Hooks（5个）

| Hook Name | Event | 命令 | Matcher | Timeout | 特殊选项 | 描述 |
|-----------|-------|------|---------|---------|----------|------|
| **session-init** | SessionInit | `bin/java-harness hook session-init` | init | 60s | once: true | 会话初始化 + Plans.md 总结 |
| **session-start** | SessionStart | `bin/java-harness hook session-start` | - | 30s | - | 会话开始时的环境设置 |
| **session-cleanup** | SessionEnd | `bin/java-harness hook session-cleanup` | - | 10s | - | 会话结束时的临时文件清理 |
| **session-monitor** | SessionMonitor | `bin/java-harness hook session-monitor` | - | 10s | - | 项目状态收集 + session.json |
| **session-summary** | SessionSummary | `bin/java-harness hook session-summary` | - | 10s | - | 会话总结到 session-log.md |

#### C. 工作流 Hooks（4个）

| Hook Name | Event | 命令 | Matcher | Timeout | 描述 |
|-----------|-------|------|---------|---------|------|
| **ask-user-question-normalize** | AskUserQuestion | `bin/java-harness hook ask-user-question-normalize` | AskUserQuestion | 5s | 问题答案桥接 |
| **post-compact** | PostCompact | `bin/java-harness hook post-compact` | - | 10s | WIP 上下文重新注入 |
| **ci-status** | CIStatus | `bin/java-harness hook ci-status` | `Bash(git push*)\|Bash(git merge*)` | 30s | push/PR 后的 CI 状态检查 |

#### D. 监控 Hooks（2个）

| Hook Name | Event | 命令 | Matcher | Timeout | 描述 |
|-----------|-------|------|---------|---------|------|
| **notification** | Notification | `bin/java-harness hook notification` | - | 5s | 通知事件记录 |
| **subagent-start** | SubagentStart | `bin/java-harness hook subagent-start` | - | 5s | Agent 生命周期跟踪（启动） |
| **subagent-stop** | SubagentStop | `bin/java-harness hook subagent-stop` | - | 5s | Agent 生命周期跟踪（停止） |

### 3.3 调用机制

**统一调用模式**：
```bash
bin/java-harness hook <hook-subcommand> [stdin-input]
```

**示例**：
```json
{
  "type": "command",
  "command": "bin/java-harness hook pre-tool",
  "timeout": 10
}
```

**超时策略**：
- 核心安全 hooks: 5-10秒（快速响应）
- 会话管理 hooks: 10-60秒（允许更长时间）
- 一次性 hooks (session-init): 60秒

---

## 4. 错误处理和容错机制

### 4.1 Hook 调用失败处理

| 失败类型 | 处理策略 | 描述 |
|---------|---------|------|
| **超时** | Fail-open | timeout 到达后终止 hook，允许原操作继续 |
| **命令不存在** | 静默跳过 | `bin/java-harness` 不存在时，不阻止用户操作 |
| **非零退出码** | 记录警告 | 记录到 stderr，不中断流程 |

### 4.2 配置验证

- **JSON 语法检查**：HooksSyncer 在复制前验证格式
- **二进制文件检查**：`bin/java-harness` 不存在时输出警告
- **DriftDetector 集成**：检测手动修改的 .claude-plugin/* 文件

### 4.3 日志和监控

- **警告输出**：hook 失败时输出到 stderr，不干扰 stdout
- **失败计数**：PostToolUseFailure hook 跟踪失败次数
- **性能监控**：执行时间超过 1秒 时记录警告

### 4.4 降级策略

1. **Native Image 不可用**：尝试回退到 `java -jar java-harness-cli.jar`
2. **完全不可用**：禁用所有 hooks，允许基本功能运行

---

## 5. 测试策略

### 5.1 单元测试

- **HooksSyncerTest** - 验证 hooks.json 复制逻辑
- **PluginGeneratorTest** - 验证 plugin.json 生成
- **SettingsGeneratorTest** - 验证 settings.json 生成
- **DriftDetectorTest** - 验证配置漂移检测

### 5.2 集成测试

- **SyncSkillTest** - 端到端测试：harness.toml → 所有生成文件
- **HooksValidationTest** - 验证生成的 hooks.json 语法和字段
- **CommandInvocationTest** - 验证 hook 命令调用格式

### 5.3 手动验证步骤

1. 构建 Native Image：`mvn -Pnative package`
2. 验证二进制存在：`ls -la bin/java-harness`
3. 运行 `/harness-sync` skill
4. 检查生成的 `.claude-plugin/hooks.json`
5. 触发测试操作（Write、Bash、AskUserQuestion）
6. 验证 hook 被调用（检查日志）
7. 确认无进程爆炸（监控进程数量）

### 5.4 性能测试

- 验证 hook 响应时间 <10ms（Native Image）
- 验证内存使用稳定（无泄漏）
- 验证无无限循环启动

---

## 6. 实现计划

### 6.1 创建 hooks/hooks.json

编写包含所有 16 个 hooks 的配置文件，遵循以下原则：
- 精细的 matcher 条件
- 合理的 timeout 设置
- 统一的调用格式

### 6.2 更新 .gitignore

添加崩溃日志忽略规则：
```
# Java - 崩溃日志
hs_err_pid*.log
```

**注意**：`hooks/hooks.json` 应该提交到 git，因为它是项目的正确配置。之前的错误配置已删除。

### 6.3 验证和测试

1. 运行现有单元测试
2. 手动验证 hook 调用
3. 性能测试
4. 监控内存和进程数量

---

## 7. 风险和缓解措施

### 7.1 风险

| 风险 | 影响 | 可能性 | 缓解措施 |
|-----|------|-------|---------|
| Native Image 构建失败 | 高 | 中 | 提供回退到 JAR 的机制 |
| Hook 超时导致用户体验下降 | 中 | 低 | 优化 hook 性能，设置合理 timeout |
| 配置错误导致进程爆炸 | 高 | 低 | 严格测试，添加 DriftDetector |

### 7.2 已知问题

- **当前状态**：项目中不存在 `bin/java-harness` Native Image
- **解决方案**：第一阶段先使用 JAR 方式，后续添加 Native Image 构建

---

## 8. 后续工作

1. 实现 Native Image 构建配置
2. 优化 hook 性能达到 <10ms 目标
3. 添加更详细的监控和日志
4. 编写用户文档

---

**批准**：[待批准]
**下一步**：调用 writing-plans 技能创建实现计划
