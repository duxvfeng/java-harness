# Java Harness 命令格式改造与功能对等实现设计文档

**项目**: Java Harness (java-harness)
**版本**: v4.1.0-java → v5.0.0-java
**日期**: 2026-08-03
**作者**: Chachamaru
**状态**: 设计阶段

---

## 文档元数据

- **设计目标**: 将 Java 项目完全对等 Go 项目（claude-code-harness）
- **当前状态**: 35-40% 功能覆盖率
- **目标状态**: 95%+ 功能覆盖率
- **实现策略**: 拆分为 3 个独立阶段
- **预计工期**: 10-13 周

---

## 1. 执行摘要

### 1.1 项目背景

Java Harness 是 claude-code-harness Go 项目的 Java 实现，当前实现了核心安全防护层（Guardrail 规则 100%），但在工作流编排、技能系统、代理协作等企业级功能上存在显著差距。

### 1.2 核心问题

1. **命令格式不一致**: 当前使用 `harness <subcommand>`，Go 项目使用 `/harness-*` 技能格式
2. **功能覆盖率低**: 约 35-40%，缺失工作流编排核心功能
3. **架构差异**: 使用 picocli 框架，与 Go 项目简单分发的架构不同

### 1.3 解决方案

**完全对等 Go 项目**:
- 移除 picocli 框架，采用类似 Go 的简单命令分发
- 所有命令改为 `/java-harness-*` 格式
- 分 3 个阶段实现所有缺失功能
- 保持命令名称、参数、行为与 Go 完全一致

---

## 2. 整体架构设计

### 2.1 Go 项目架构分析

**核心特征**:
1. 单一二进制文件 `harness`
2. 通过 `main()` switch 语句直接分发命令
3. 每个命令对应 `run*` 函数
4. 通过 `hooks.json` 与 Claude Code 集成
5. 通过 `.claude-plugin/skills/` 提供技能

### 2.2 Java 项目改造架构

```
┌─────────────────────────────────────────────────────────┐
│              Claude Code Integration Layer              │
│  ┌─────────────────────────────────────────────────┐   │
│  │  hooks.json (调用 java-harness 命令)             │   │
│  │  "java-harness hook pre-tool"                    │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────▼────────────┐
        │   Main.java             │
        │   (类似 Go main.go)      │
        └────────────┬────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌───▼────┐ ┌───▼────┐
    │ Hook    │ │ Core   │ │ Ext    │
    │Commands │ │Commands│ │Commands│
    └────┬────┘ └───┬────┘ └───┬────┘
         │          │          │
         └──────────┼──────────┘
                    │
         ┌──────────▼──────────┐
         │   7-Layer Modules   │
         │  shared→foundation  │
         │  →protocol→security │
         │  →workflow→collab   │
         └─────────────────────┘
```

**关键改造点**:
1. 移除 picocli 框架
2. 单一入口点 `Main.java`
3. 每个命令一个 `*Handler` 类
4. 技能文件在 `.claude-plugin/skills/`
5. 命令从 `harness <subcommand>` 改为 `java-harness <subcommand>`

---

## 3. 命令注册和分发机制

### 3.1 主入口点

**文件**: `Main.java`

```java
package com.chachamaru.harness;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
            case "hook":
                HookDispatcher.execute(commandArgs);
                break;
            case "plan":
                PlanHandler.execute(commandArgs);
                break;
            case "work":
                WorkHandler.execute(commandArgs);
                break;
            // ... 其他 83 个命令
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }
}
```

### 3.2 命令处理器接口

```java
public interface CommandHandler {
    void execute(String[] args);
}
```

### 3.3 命令注册表

```java
public class CommandRegistry {
    private static final Map<String, CommandHandler> handlers = new HashMap<>();

    static {
        handlers.put("hook", new HookDispatcher());
        handlers.put("plan", new PlanHandler());
        // ... 注册所有 86 个命令
    }
}
```

---

## 4. Claude Code 技能文件结构

### 4.1 目录结构

```
java-harness/
├── .claude-plugin/
│   ├── plugin.json
│   ├── hooks.json
│   └── skills/
│       ├── core/
│       │   ├── java-harness-plan.claude
│       │   ├── java-harness-work.claude
│       │   ├── java-harness-review.claude
│       │   ├── java-harness-sync.claude
│       │   └── java-harness-release.claude
│       ├── hook/
│       │   ├── java-harness-hook-pre-tool.claude
│       │   └── ...
│       └── extended/
│           ├── java-harness-init.claude
│           └── ...
```

### 4.2 技能文件示例

**文件**: `.claude-plugin/skills/core/java-harness-plan.claude`

```
---
name: java-harness-plan
description: Generate plan prompt for the host to execute
---

Please generate a plan prompt based on the current project context.

Use the java-harness CLI to generate the plan:
```bash
java-harness plan
```
```

### 4.3 命令映射

```
技能名称 (/java-harness-*)  → CLI 命令
--------------------------------------------------
/java-harness-plan         → java-harness plan
/java-harness-work         → java-harness work <taskID>
/java-harness-hook-pre-tool → java-harness hook pre-tool
```

---

## 5. Hook 协议处理层

### 5.1 Hook 输入格式

```json
{
  "session_id": "test-session",
  "transcript_path": "/path/to/transcript",
  "cwd": "/project",
  "permission_mode": "default",
  "hook_event_name": "PreToolUse",
  "tool_name": "Write",
  "tool_input": {
    "file_path": "/project/test.txt"
  },
  "plugin_root": "/plugin"
}
```

### 5.2 Hook 输出格式

```json
{
  "hookEventName": "PreToolUse",
  "permissionDecision": "allow",
  "permissionDecisionReason": null,
  "additionalContext": null
}
```

### 5.3 Hook 处理器架构

```java
public class HookDispatcher implements CommandHandler {
    @Override
    public void execute(String[] args) {
        String hookType = args[0];
        HookInput input = HookCodec.decode(System.in);
        HookOutput output = dispatch(hookType, input);
        System.out.println(HookCodec.encode(output));
    }
    
    private HookOutput dispatch(String hookType, HookInput input) {
        switch (hookType) {
            case "pre-tool":
                return new PreToolHookHandler().handle(input);
            // ... 其他 hook 类型
        }
    }
}
```

### 5.4 支持的 Hook 事件

1. PreToolUse
2. PostToolUse
3. PermissionRequest
4. SessionStart
5. SessionEnd
6. Stop
7. PreCompact
8. PostCompact
9. Notification
10. PermissionDenied
11. SubagentStart
12. SubagentStop
13. TaskCompleted
14. TaskCreated
15. WorktreeCreate
16. WorktreeRemove

---

## 6. 3 个阶段的实现范围

### 阶段 1: 命令格式改造 + 核心技能框架

**目标**: 从 35-40% 提升到 60-70% 功能覆盖率
**工期**: 3-4 周

**包含内容**:

**1.1 命令系统改造 (P0)**
- ✅ 移除 picocli 框架
- ✅ 实现 Main.java 入口点
- ✅ 实现 86 个命令 Handler
- ✅ 创建 .claude-plugin/ 结构
- ✅ 编写所有 /java-harness-* 技能文件
- ✅ 配置 hooks.json 和 plugin.json

**1.2 核心 5 个技能实现 (P0)**
- ✅ /java-harness-plan
- ✅ /java-harness-work
- ✅ /java-harness-review
- ✅ /java-harness-sync
- ✅ /java-harness-release

**1.3 Plans.md 解析器 (P0)**
- ✅ Plans.md 文件格式解析
- ✅ 任务依赖关系解析
- ✅ 任务状态跟踪
- ✅ plans check-deps 命令

**1.4 基础配置管理 (P1)**
- ✅ harness.toml 解析器
- ✅ init 命令生成配置模板
- ✅ sync 命令同步配置

**1.5 状态管理增强 (P1)**
- ✅ 会话状态管理
- ✅ 工作状态管理
- ✅ JSONL 持久化
- ✅ SQLite 数据库操作

**预期成果**: 86 个命令全部可用，核心闭环可用，功能覆盖率约 65-70%

---

### 阶段 2: 工作流编排 + 高级功能

**目标**: 提升到 85-90% 功能覆盖率
**工期**: 4-5 周

**包含内容**:

**2.1 并行编排系统 (P0)**
- ✅ 任务编排器
- ✅ 并行执行引擎
- ✅ 任务依赖图构建

**2.2 Breezing 团队支持 (P0)**
- ✅ Planner 代理
- ✅ Critic 代理
- ✅ Worker 代理
- ✅ 并行协作编排

**2.3 高级状态管理 (P1)**
- ✅ 4 阶段恢复机制
- ✅ TTL 管理
- ✅ 状态迁移工具

**2.4 Worktree 隔离 (P1)**
- ✅ Worktree 创建/移除
- ✅ 指纹捕获和对比
- ✅ 逃逸检测

**2.5 扩展 Hook 处理 (P1)**
- ✅ TaskCompleted/TaskCreated 事件
- ✅ ConfigChange/FileChanged 事件
- ✅ InstructionsLoaded 事件

**预期成果**: 完整工作流编排能力，功能覆盖率约 85-90%

---

### 阶段 3: 企业级功能 + 多工具支持

**目标**: 达到 95%+ 功能对等
**工期**: 3-4 周

**包含内容**:

**3.1 监控和可观测性 (P1)**
- ✅ OpenTelemetry 追踪集成
- ✅ 审计日志系统
- ✅ 会话追踪

**3.2 多工具支持 (P1)**
- ✅ Codex CLI 集成
- ✅ Cursor 集成
- ✅ Grok 集成
- ✅ OpenCode 兼容层

**3.3 内存和知识管理 (P2)**
- ✅ harness-mem 集成
- ✅ MCP 服务器实现
- ✅ 决策卡片系统

**3.4 技能镜像 (P2)**
- ✅ OpenCode/Codex 技能同步
- ✅ 镜像偏差检测

**3.5 高级监控 (P2)**
- ✅ 夜巡报告
- ✅ 失败编码器
- ✅ 影响评分

**预期成果**: 完全对等 Go 项目，功能覆盖率 95%+

---

## 7. 数据流和错误处理

### 7.1 Hook 事件处理流程

```
Claude Code
    ↓
hooks.json
    ↓
Main.java (命令分发)
    ↓
HookDispatcher
    ↓
HookCodec.decode(stdin)
    ↓
具体 HookHandler
    ↓
GuardrailEngine (R01-R27)
    ↓
HookOutput 构建
    ↓
HookCodec.encode(stdout)
    ↓
返回 Claude Code
```

### 7.2 错误处理策略

**分层错误处理**:
- 命令错误 (1000-1999)
- Hook 错误 (2000-2999)
- Guardrail 错误 (3000-3999)
- 配置错误 (4000-4999)
- 状态错误 (5000-5999)

**Hook 错误处理原则**: fail-open（默认允许，避免阻断用户工作）

```java
public class HookDispatcher {
    public HookOutput dispatch(String hookType, HookInput input) {
        try {
            return executeHook(hookType, input);
        } catch (HookJsonParseException e) {
            // JSON 解析错误：返回 allow
            logger.error("Hook JSON parse error, allowing: {}", e.getMessage());
            return HookOutput.allow();
        }
    }
}
```

---

## 8. 测试策略

### 8.1 测试层次

```
端到端测试 (E2E)
    ↓
集成测试 (Integration)
    ↓
单元测试 (Unit)
```

### 8.2 测试覆盖率目标

- **单元测试覆盖率**: ≥ 80%
- **Guardrail 规则覆盖率**: 100% (27 个规则)
- **Hook 处理器覆盖率**: 100% (16 个 Hook)
- **命令覆盖率**: 100% (86 个命令)

### 8.3 性能测试目标

- **Hook 响应时间**: P95 < 10ms
- **内存占用**: < 50MB (Native Image)
- **启动时间**: < 100ms

---

## 9. 部署和分发

### 9.1 Native Image 编译

```bash
cd java-harness-cli
mvn -Pnative native:compile
```

### 9.2 交叉编译

支持 Linux、macOS、Windows 平台

### 9.3 安装脚本

```bash
scripts/install-java-harness.sh
```

### 9.4 Claude Code Marketplace 发布

通过 `marketplace.json` 配置发布

---

## 10. 关键技术决策总结

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 命令框架 | 移除 picocli，使用简单 switch | 与 Go 项目对等，降低复杂度 |
| 技能格式 | .claude-plugin/skills/*.claude | Claude Code 标准格式 |
| Hook 协议 | JSON stdin/stdout | 与 Go 完全一致 |
| 状态管理 | SQLite + JSONL | 与 Go 对等 |
| 部署方式 | GraalVM Native Image | 单一可执行文件，快速启动 |

---

## 11. 风险和缓解措施

### 11.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| GraalVM 兼容性问题 | 高 | 提前验证所有依赖库的 GraalVM 兼容性 |
| 性能目标无法达成 | 中 | 建立 POC 验证 <10ms 响应时间 |
| 技能文件格式变化 | 中 | 紧跟 Claude Code 文档更新 |

### 11.2 项目风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 工期估算偏差 | 中 | 采用敏捷迭代，每阶段可独立交付 |
| 功能范围蔓延 | 高 | 严格按 3 阶段执行，不在阶段间添加功能 |
| Go 项目功能变化 | 低 | 定期同步 Go 项目更新 |

---

## 12. 成功标准

### 12.1 功能标准

- ✅ 86 个命令全部实现
- ✅ 27 个 Guardrail 规则全部通过测试
- ✅ 16 个 Hook 事件全部处理
- ✅ 核心 5 技能闭环可用

### 12.2 质量标准

- ✅ 单元测试覆盖率 ≥ 80%
- ✅ 集成测试全部通过
- ✅ E2E 测试全部通过
- ✅ 性能测试目标达成

### 12.3 对等标准

- ✅ 命令名称与 Go 项目 100% 一致
- ✅ 命令行为与 Go 项目 100% 一致
- ✅ Hook 协议与 Go 项目 100% 一致
- ✅ 功能覆盖率 ≥ 95%

---

## 附录 A: 完整命令列表

### Hook 命令 (16 个)

```
java-harness hook pre-tool
java-harness hook post-tool
java-harness hook permission
java-harness hook session-start
java-harness hook post-tool-failure
java-harness hook post-compact
java-harness hook notification
java-harness hook permission-denied
java-harness hook ask-user-question-normalize
java-harness hook session-init
java-harness hook session-cleanup
java-harness hook session-monitor
java-harness hook session-summary
java-harness hook ci-status
java-harness hook subagent-start
java-harness hook subagent-stop
```

### 核心命令 (12 个)

```
java-harness plan
java-harness work <taskID>
java-harness review <taskID>
java-harness release [--check]
java-harness plans check-deps
java-harness evidence collect
java-harness sprint-contract <task-id>
java-harness status
java-harness init [root]
java-harness sync [root]
java-harness validate [skills|agents|all] [root]
java-harness doctor [--migration] [--migration-report]
```

### 扩展命令 (58 个)

```
java-harness gen [hooks] [--check] [root]
java-harness codex-loop
java-harness mem
java-harness channels-wake
java-harness inbox
java-harness session
java-harness self-audit
java-harness retired-alias
java-harness night-watch
java-harness mirror
java-harness failure-codifier
java-harness wt
java-harness impact-score
java-harness pre-compact
java-harness version
... (其他命令)
```

---

## 附录 B: 功能对比矩阵

| 功能模块 | Go 项目 | Java 项目 (当前) | Java 项目 (目标) |
|---------|--------|----------------|----------------|
| Hook 协议处理 | ✅ 16 个 | ⚠️ 7 个 | ✅ 16 个 |
| Guardrail 规则 | ✅ 27 个 | ✅ 27 个 | ✅ 27 个 |
| 技能系统 | ✅ 21 个 | ❌ 0 个 | ✅ 21 个 |
| 代理系统 | ✅ 3 个 | ❌ 0 个 | ✅ 3 个 |
| 工作流编排 | ✅ 完整 | ⚠️ 15% | ✅ 完整 |
| 配置管理 | ✅ 完整 | ❌ 0% | ✅ 完整 |
| 状态管理 | ✅ 完整 | ⚠️ 50% | ✅ 完整 |
| 监控可观测性 | ✅ 完整 | ⚠️ 20% | ✅ 完整 |
| 多工具支持 | ✅ 4 个 | ⚠️ 1 个 | ✅ 4 个 |
| 内存管理 | ✅ 完整 | ❌ 0% | ✅ 完整 |

---

**文档版本**: 1.0
**最后更新**: 2026-08-03
**审核状态**: 待审核
