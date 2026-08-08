# Java Harness CLI 命令结构设计文档

## 1. 概述

本文档描述了Java Harness CLI的命令结构设计，目标是完全复制Go版本`claude-code-harness`的命令结构，采用kebab-case命名格式，使用picocli框架实现。

## 2. 设计目标

### 2.1 主要目标
- 完全复制Go项目的命令结构
- 采用kebab-case命名格式（如`harness plan`、`harness sprint-contract`）
- 使用picocli框架实现
- 为每个命令编写完整的单元测试

### 2.2 次要目标
- 保持代码的可维护性
- 支持命令的扩展性
- 提供清晰的错误处理
- 支持命令的帮助信息

## 3. 架构设计

### 3.1 命令层次结构

```
harness (主命令)
├── hook (子命令组)
│   ├── pre-tool
│   ├── post-tool
│   ├── permission
│   ├── session-start
│   ├── post-tool-failure
│   ├── post-compact
│   ├── notification
│   ├── permission-denied
│   ├── ask-user-question-normalize
│   ├── session-init
│   ├── session-cleanup
│   ├── session-monitor
│   ├── session-summary
│   ├── ci-status
│   ├── subagent-start
│   └── subagent-stop
├── evidence
│   └── collect
├── sprint-contract
├── status
├── init
├── sync
├── validate
├── plans
│   └── check-deps
├── doctor
├── codex-loop
├── mem
├── channels-wake
├── inbox
├── session
├── self-audit
├── retired-alias
├── night-watch
├── failure-codifier
├── mirror
├── wt
├── impact-score
├── pre-compact
├── gen
├── work
├── plan
├── review
├── release
└── version
```

### 3.2 包结构设计

```
com.chachamaru.harness.cli
├── HarnessCLI.java (主入口)
├── command/
│   ├── hook/
│   │   ├── HookCommand.java (hook命令组主命令)
│   │   ├── PreToolCommand.java
│   │   ├── PostToolCommand.java
│   │   └── ... (其他hook子命令)
│   ├── evidence/
│   │   ├── EvidenceCommand.java
│   │   └── CollectCommand.java
│   ├── plan/
│   │   ├── PlanCommand.java
│   │   └── CheckDepsCommand.java
│   └── ... (其他命令组)
├── handler/
│   ├── HookHandler.java
│   └── ... (其他处理器)
└── util/
    └── ... (工具类)
```

## 4. 命令实现细节

### 4.1 主命令实现

```java
@Command(name = "harness",
         mixinStandardHelpOptions = true,
         version = "Java Harness CLI v4.1.0",
         subcommands = {
             HookCommand.class,
             EvidenceCommand.class,
             SprintContractCommand.class,
             StatusCommand.class,
             InitCommand.class,
             SyncCommand.class,
             ValidateCommand.class,
             PlansCommand.class,
             DoctorCommand.class,
             CodexLoopCommand.class,
             MemCommand.class,
             ChannelsWakeCommand.class,
             InboxCommand.class,
             SessionCommand.class,
             SelfAuditCommand.class,
             RetiredAliasCommand.class,
             NightWatchCommand.class,
             FailureCodifierCommand.class,
             MirrorCommand.class,
             WtCommand.class,
             ImpactScoreCommand.class,
             PreCompactCommand.class,
             GenCommand.class,
             WorkCommand.class,
             PlanCommand.class,
             ReviewCommand.class,
             ReleaseCommand.class,
             VersionCommand.class
         },
         description = "Java Harness CLI - Claude Code Harness v4")
public class HarnessCLI implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
```

### 4.2 命令命名规范

使用picocli的`@Command`注解的`name`属性来指定kebab-case格式的命令名：

```java
@Command(name = "sprint-contract",
         description = "Generate sprint-contract from Plans.md")
public class SprintContractCommand implements Runnable {
    // 命令实现
}
```

### 4.3 子命令实现示例

以`hook`命令组为例：

```java
@Command(name = "hook",
         subcommands = {
             PreToolCommand.class,
             PostToolCommand.class,
             // ... 其他子命令
         },
         description = "Hook subcommands")
public class HookCommand implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}

@Command(name = "pre-tool",
         description = "Evaluate PreToolUse guardrails")
public class PreToolCommand implements Runnable {
    @Override
    public void run() {
        // 实现pre-tool功能
    }
}
```

## 5. 测试策略

### 5.1 测试结构

```
src/test/java/com/chachamaru/harness/cli/
├── command/
│   ├── hook/
│   │   ├── PreToolCommandTest.java
│   │   ├── PostToolCommandTest.java
│   │   └── ... (其他hook命令测试)
│   ├── evidence/
│   │   └── CollectCommandTest.java
│   └── ... (其他命令测试)
├── handler/
│   ├── HookHandlerTest.java
│   └── ... (其他处理器测试)
└── integration/
    ├── HarnessCLIIntegrationTest.java
    └── ... (其他集成测试)
```

### 5.2 单元测试示例

```java
class PreToolCommandTest {
    @Test
    void testPreToolCommandExecution() {
        PreToolCommand command = new PreToolCommand();
        // 模拟输入和验证输出
    }
    
    @Test
    void testPreToolCommandWithInvalidInput() {
        // 测试无效输入的处理
    }
}
```

### 5.3 集成测试示例

```java
class HarnessCLIIntegrationTest {
    @Test
    void testFullCommandExecution() {
        String[] args = {"hook", "pre-tool"};
        HarnessCLI.main(args);
        // 验证执行结果
    }
}
```

### 5.4 测试覆盖率目标

- **单元测试覆盖率**：80%以上
- **集成测试覆盖率**：核心命令100%
- **边界条件测试**：所有命令都包含边界条件测试

## 6. 实现计划

### 6.1 实现阶段

**阶段一：基础架构搭建（第1-2天）**
- 创建主命令`HarnessCLI`
- 搭建命令组结构
- 实现核心工具类

**阶段二：核心命令实现（第3-5天）**
- 实现`hook`命令组（所有子命令）
- 实现`evidence`命令
- 实现`plan`、`plans`命令
- 实现`version`、`status`命令

**阶段三：扩展命令实现（第6-8天）**
- 实现`doctor`、`validate`命令
- 实现`sync`、`init`命令
- 实现`codex-loop`、`mem`命令
- 实现`channels-wake`、`inbox`命令

**阶段四：高级命令实现（第9-10天）**
- 实现`session`、`self-audit`命令
- 实现`retired-alias`、`night-watch`命令
- 实现`failure-codifier`、`mirror`命令
- 实现`wt`、`impact-score`命令
- 实现`pre-compact`、`gen`命令
- 实现`work`、`review`、`release`命令

**阶段五：测试和优化（第11-12天）**
- 编写单元测试
- 编写集成测试
- 性能优化和bug修复

### 6.2 里程碑

- **里程碑1**：基础架构完成，核心命令可用
- **里程碑2**：所有命令实现完成
- **里程碑3**：测试覆盖率达到目标
- **里程碑4**：项目交付

## 7. 风险评估

### 7.1 技术风险
- picocli框架的学习曲线
- 命令参数解析的复杂性
- 与Go项目的功能一致性

### 7.2 时间风险
- 命令数量较多（30+个）
- 每个命令都需要完整实现
- 测试工作量较大

### 7.3 缓解措施
- 参考Go项目的实现
- 使用picocli的最佳实践
- 分阶段实现，逐步验证

## 8. 附录

### 8.1 Go项目命令列表

| Go命令 | Java命令类 | 功能描述 |
|--------|-----------|----------|
| `harness hook pre-tool` | `PreToolCommand` | PreToolUse guardrail evaluation |
| `harness hook post-tool` | `PostToolCommand` | PostToolUse tampering/security checks |
| `harness hook permission` | `PermissionCommand` | PermissionRequest auto-approval |
| `harness hook session-start` | `SessionStartCommand` | SessionStart env setup |
| `harness hook post-tool-failure` | `PostToolFailureCommand` | PostToolUseFailure counter & escalation |
| `harness hook post-compact` | `PostCompactCommand` | PostCompact WIP context re-injection |
| `harness hook notification` | `NotificationCommand` | Notification event logging |
| `harness hook permission-denied` | `PermissionDeniedCommand` | PermissionDenied event logging |
| `harness hook ask-user-question-normalize` | `AskUserQuestionNormalizeCommand` | PreToolUse AskUserQuestion answer bridge |
| `harness hook session-init` | `SessionInitCommand` | SessionStart: session initialization + Plans.md summary |
| `harness hook session-cleanup` | `SessionCleanupCommand` | SessionEnd: temp file cleanup |
| `harness hook session-monitor` | `SessionMonitorCommand` | SessionStart: project state collection + session.json |
| `harness hook session-summary` | `SessionSummaryCommand` | Stop: session summary to session-log.md |
| `harness hook ci-status` | `CiStatusCommand` | PostToolUse: CI status check after push/PR |
| `harness hook subagent-start` | `SubagentStartCommand` | SubagentStart: track agent lifecycle start |
| `harness hook subagent-stop` | `SubagentStopCommand` | SubagentStop: track agent lifecycle stop |
| `harness evidence collect` | `CollectCommand` | Collect evidence (test results, build logs) |
| `harness sprint-contract` | `SprintContractCommand` | Generate sprint-contract from Plans.md |
| `harness status` | `StatusCommand` | Show all tracked agent states |
| `harness init` | `InitCommand` | Create harness.toml template in project root |
| `harness sync` | `SyncCommand` | Generate CC files from harness.toml |
| `harness validate` | `ValidateCommand` | Validate SKILL.md / agent frontmatter |
| `harness plans check-deps` | `CheckDepsCommand` | Verify done tasks only depend on closed tasks |
| `harness doctor` | `DoctorCommand` | Health check plus migration status/report |
| `harness codex-loop` | `CodexLoopCommand` | Run the Codex-native long-running loop |
| `harness mem` | `MemCommand` | Manage harness-mem companion |
| `harness channels-wake` | `ChannelsWakeCommand` | Bridge channel health check |
| `harness inbox` | `InboxCommand` | Read livemsg inbox |
| `harness session` | `SessionCommand` | Session management |
| `harness self-audit` | `SelfAuditCommand` | Audit settings.local.json command hooks |
| `harness retired-alias` | `RetiredAliasCommand` | Scan repo for retired alias residue |
| `harness night-watch` | `NightWatchCommand` | Emit night-watch patrol report |
| `harness failure-codifier` | `FailureCodifierCommand` | Emit failure-rule.v1 proposals |
| `harness mirror` | `MirrorCommand` | Report skills/ mirror drift |
| `harness wt` | `WtCommand` | Worktree fingerprint operations |
| `harness impact-score` | `ImpactScoreCommand` | Compute judgment-card impact_score |
| `harness pre-compact` | `PreCompactCommand` | Evaluate whether PreCompact should be blocked |
| `harness gen` | `GenCommand` | Generate per-host hooks.json from hosts.toml |
| `harness work` | `WorkCommand` | Emit the work prompt + task context |
| `harness plan` | `PlanCommand` | Emit the plan prompt for the host to execute |
| `harness review` | `ReviewCommand` | Emit the review prompt + task context |
| `harness release` | `ReleaseCommand` | Emit the release prompt for the host to execute |
| `harness version` | `VersionCommand` | Print version |

### 8.2 参考资料

- Go项目源码：`D:\go-project\claude-code-harness`
- picocli官方文档：https://picocli.info/
- Java Harness CLI现有代码：`D:\project\java-harness\java-harness-cli`
