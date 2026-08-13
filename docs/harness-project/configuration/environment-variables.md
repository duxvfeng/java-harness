# 环境变量参考

本文档列出 Java Harness 真正会读取的环境变量，并按"读取者"分层，避免把
**技能/脚本层**的变量误当成 **Java CLI** 读取的变量。

> 核查方法：以下结论基于对全部 `java-harness-*` 模块 `System.getenv` /
> `System.getProperty` 调用的逐一代码审查（2026-08-13）。

## 读取者优先

| 层 | 读取者 | 说明 |
|----|--------|------|
| **Java CLI** | `harness` 二进制（picocli `HarnessCLI`） | 进程启动时读取，机器强制 |
| **技能/脚本层** | `skills/*.md`、`scripts/**.sh`、`.claude-plugin/hooks.json` | 由宿主 LLM / shell 执行，Java 进程不读取 |
| **宿主平台** | Claude Code / Codex CLI 自身 | 平台运行时变量 |

## 配置加载优先级（从高到低）

1. **环境变量**（本文档）
2. **平台特定配置**（`.claude/config.toml` 或 `.codex/config.toml`）
3. **标准配置文件**（项目根 `harness.toml`）
4. **代码内置默认值**

---

## 一、Java CLI 读取的环境变量

### 1.1 端到端检测（E2E）

读取处：`HarnessConfigManager.applyEnvironmentOverrides`。覆盖
`.claude/config/e2e-detection.config.json` 中的对应字段。

| 环境变量 | 作用 | 取值 | 覆盖字段 |
|----------|------|------|----------|
| `HARNESS_E2E_ENABLED` | E2E 检测总开关 | `true`/`false` | `enabled` |
| `HARNESS_E2E_MODE` | 检测严格度 | `strict`/`lenient` | `mode` |
| `HARNESS_E2E_TIMEOUT` | 单次测试超时（秒） | 整数 | `timeout` |
| `HARNESS_E2E_FRONTEND` | 是否启用前端检测 | `true`/`false` | `test_types.frontend.enabled` |
| `HARNESS_E2E_BACKEND` | 是否启用后端检测 | `true`/`false` | `test_types.backend.enabled` |

### 1.2 会话管理

读取处：`SessionConfigLoader.loadEnvironmentVariables`。覆盖 `.properties` 文件值。

| 环境变量 | 作用 | 取值 | 覆盖键 |
|----------|------|------|--------|
| `HARNESS_SESSION_AUTO_SAVE` | 自动保存开关 | `true`/`false` | `session.auto_save.enable` |
| `HARNESS_SESSION_THRESHOLDS` | 触发保存的 Token 阈值 | 逗号分隔整数，如 `80,90` | `session.auto_save.thresholds` |
| `HARNESS_SESSION_MAX_SAVES` | 保留的会话保存数 | 整数 | `session.auto_save.max_saves` |

> ⚠️ **命名纠错**：不存在 `HARNESS_SESSION_TOKEN_THRESHOLD`。正确名称是
> `HARNESS_SESSION_THRESHOLDS`（复数，逗号分隔列表）。

### 1.3 模型选择（经 `env:` 前缀动态读取）

`ModelSelectionConfig` 加载器返回 null → 恒用默认四级配置；各级别通过 `env:` 前缀
解析下列变量，最终回退到硬编码的 `glm-4.7`。读取处：`SmartModelSelector.resolveModelReference`。

| 环境变量 | 复杂度等级（分数） | 角色 |
|----------|-------------------|------|
| `ANTHROPIC_DEFAULT_FABLE_MODEL` | FAST（0–2） | 主模型 |
| `ANTHROPIC_DEFAULT_HAIKU_MODEL` | BALANCED（3–4） | 主模型 |
| `ANTHROPIC_DEFAULT_SONNET_MODEL` | QUALITY（5–6） | 主模型 |
| `ANTHROPIC_DEFAULT_OPUS_MODEL` | POWERFUL（7+） | 主模型 |
| `ANTHROPIC_MODEL` | 所有等级 | 各级共享回退 |
| —（硬编码）`glm-4.7` | 所有等级 | 最终兜底 |

> 注：不存在 `HARNESS_MODEL_SELECTION_ENABLED` / `HARNESS_MODEL_SELECTION_STRATEGY`。
> `enabled` 恒为 `true`，`strategy` 恒为 `effortBased`（代码默认）。

### 1.4 平台检测

读取处：`PlatformDetector`。

| 环境变量 | 作用 | 默认 |
|----------|------|------|
| `CODEX_CLI` | 检测 Codex CLI 平台（**优先级最高**，非空即判定 Codex） | 未设 |
| `CLAUDE_CODE_HARNESS` | 检测 Claude Code 平台（非空即判定） | 两者皆未设时默认 Claude Code |

> 注意：`CLAUDE_CODE_HARNESS`（平台检测）与下文技能层的 `CLAUDE_CODE_HARNESS_LANG`
>（语言/区域）是**不同**的变量。

### 1.5 Token 监控 / 数据路径

| 环境变量 | 读取处 | 作用 |
|----------|--------|------|
| `CLAUDE_TOKEN_COUNT` | `TokenMonitor` | 当前会话 Token 用量（整数），用于触发会话保存 |
| `CLAUDE_PLUGIN_DATA` | `InboxCheckCommand` | 插件数据目录 → livemsg.db 位置（回退顺序见下） |
| `CLAUDE_PROJECT_DIR` | `InboxCheckCommand` | 项目目录 → `.harness/livemsg.db`（`CLAUDE_PLUGIN_DATA` 未设时回退到此，再到 `user.dir`） |
| `CLAUDE_CODE_HARNESS_MEM_CONFIRM_PURGE` | `MemCommand` | 设为允许值时 `harness mem purge` 可不带 `--confirm-purge` |

### 1.6 协作子系统（LiveMsg / Mem / NightWatch）

| 环境变量 | 读取处 | 作用 / 默认 |
|----------|--------|-------------|
| `HARNESS_LIVEMSG_TEAM` | `InboxCheckCommand` | LiveMsg 团队标识（默认 `default`） |
| `HARNESS_LIVEMSG_AGENT` | `InboxCheckCommand` | LiveMsg 代理标识（默认空） |
| `HARNESS_MEM_HOME` | `MemCommand` | harness-mem 数据目录（默认 `~/.harness-mem`） |
| `HARNESS_MEM_HOST` | `MemCommand` | mem 守护进程主机（默认 `127.0.0.1`） |
| `HARNESS_MEM_PORT` | `MemCommand` | mem 守护进程端口（默认 `37888`） |
| `HARNESS_BRIDGE_HOME` | `NightWatchReport` | channels-bridge 主目录（默认 `~/.harness-bridge`）|
| `HARNESS_NIGHT_WATCH_HOME` | `NightWatchReport` | night-watch 主目录（默认 `~/.harness-night-watch`）|

> `HARNESS_BRIDGE_HOME` / `HARNESS_NIGHT_WATCH_HOME` / `NIGHT_WATCH_ENABLED`
> 既可作环境变量，也可作 **JVM 系统属性**（`System.getProperty` 优先于 `getenv`）。

### 1.7 JVM 系统属性（非环境变量，但影响配置）

| 属性 | 读取处 | 作用 / 默认 |
|------|--------|-------------|
| `java.harness.work.dir` | `PlanHandler`/`WorkHandler`/`SyncHandler`/`ReleaseHandler`/`ReviewHandler` | CLI 处理器使用的项目工作目录（默认 `.`） |

---

## 二、技能 / 脚本层读取的环境变量（Java CLI 不读取）

这些变量作用于宿主 LLM 执行的技能与 shell 脚本，**不被 Java 进程读取**。
列出以便正确区分层级；详见对应技能文档。

| 环境变量 | 消费者 | 作用 |
|----------|--------|------|
| `HARNESS_SKIP_REVIEW` | `scripts/review/forced-review-gate.sh`、`.claude-plugin/hooks.json`、harness-review 技能 | 紧急跳过审查（仅限紧急，会记录审计） |
| `HARNESS_REVIEW_MODE` | harness-review 技能 / 脚本 | 审查严格度 `strict`/`lenient` |
| `HARNESS_MAX_REVIEW_ITERATIONS` | harness-review 技能 / 脚本 | 自动修复最大轮次 |
| `HARNESS_REVIEW_TIMEOUT` | harness-review 技能 / 脚本 | 单次审查超时（秒） |
| `HARNESS_DEBUG` / `HARNESS_DEBUG_LOG_LEVEL` | 技能文档约定 | 调试开关与日志级别（Java 不读取） |
| `HARNESS_TDD_BYPASS_REASON` | harness-work 技能 | 紧急绕过 TDD 的理由（审计留存） |
| `HARNESS_IMPL_BACKEND` | harness-work / breezing 技能 | 实现后端选择（优先级低于显式 `--backend`） |
| `HARNESS_ACTIVE_PHASE` / `HARNESS_ACTIVE_TASK` | harness-work 技能 | 当前执行 scope（state 文件不存在时的 fallback） |
| `HARNESS_CODEX_PRIMARY_ENV_STATE_FILE` | breezing / harness-work 技能 | Codex 伴随后端状态文件路径 |
| `HARNESS_PLUGIN_ROOT` / `CLAUDE_PLUGIN_ROOT` / `CLAUDE_SKILL_DIR` | 技能 / 安装脚本 | 插件根目录与技能目录定位 |
| `ENABLE_PROMPT_CACHING_1H` | harness-work 技能（文档约定） | 启用 1 小时 prompt 缓存 |
| `CLAUDE_CODE_HARNESS_LANG` | 技能文档（完成报告本地化） | 输出语言（`en`/`ja` 等） |

> 这些变量在早期 `harness.toml.default` 草稿中被误标为"Java 读取"，已更正。

---

## 三、常用配置示例

```bash
# 临时关闭端到端检测
export HARNESS_E2E_ENABLED=false

# 调整会话保存阈值
export HARNESS_SESSION_THRESHOLDS=75,90
export HARNESS_SESSION_MAX_SAVES=20

# 指定各级别模型（智能模型选择）
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_MODEL="glm-4.7"          # 各级共享回退

# 强制识别为 Codex 平台
export CODEX_CLI=1

# 紧急跳过审查（技能层；仅紧急使用）
export HARNESS_SKIP_REVIEW=true
```

## 四、相关文档

- [配置模板](../config/harness.toml.default)
- [配置最佳实践](best-practices.md)
- [配置迁移指南](migration-guide.md)
