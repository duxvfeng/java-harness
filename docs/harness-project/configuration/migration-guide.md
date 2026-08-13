# 配置迁移指南

面向从早期配置（草稿 `harness.toml.default`、旧 README 示例）迁移到**权威配置**
的用户。本指南列出 schema 差异、键名纠错与具体迁移步骤。

## 一、为什么要迁移

早期文档假设存在一个统一的 `[harness.*]` TOML schema（如 `[harness.review]`、
`[harness.session]`、`[harness.e2e_detection]` 等）。经源码核查，**这些嵌套段大多
没有 Java 解析器**，真正生效的配置分散在四种载体中：

| 配置载体 | 格式 | 读取者 |
|----------|------|--------|
| `harness.toml` | TOML | `ConfigReader`（同步 schema）、`ConfigSync`（后端校验） |
| `.claude/config/e2e-detection.config.json` | JSON | `HarnessConfigManager` |
| 会话配置（`.properties`） | Properties | `SessionConfigLoader` |
| 环境变量 | — | Java CLI / 技能脚本（分层） |

继续沿用旧 schema 会导致"改了不生效"的困惑。迁移后配置与实现一一对应。

## 二、迁移步骤

### 步骤 1：清理无效节

打开现有 `harness.toml`，**删除或注释**以下无解析器的段（它们目前不生效）：

```
[harness.review]          [harness.multilang]      [harness.severity]
[harness.branch_isolation] [harness.execution]     [harness.tdd]
[harness.model_selection] [harness.ci_integration] [harness.notifications]
[harness.reporting]       [harness.monitoring]     [harness.debug]
[harness.experimental]    [harness.compatibility]
```

> 这些能力的"现状替代方案"见第四节。

### 步骤 2：保留并核对已实现节

确认以下节存在且键名正确：

```toml
[harness]                 # 必须含 version + backend(∈ auto|codex|cursor)
[project]                 # name, version, description, author_*, homepage, ...
[agent]                   # default
[env]                     # 自由键值对
[safety.permissions]      # allow / deny / ask
[safety.sandbox]          # fail_if_unavailable
[safety.sandbox.network]  # denied_domains
[safety.sandbox.filesystem] # deny_read / allow_read
```

支持 `snake_case` 与 `camelCase` 两种风格（如 `author_name` ≡ `authorName`）。

### 步骤 3：迁移功能开关

把旧的 `[harness.review] enabled = true` 风格改为**顶层**开关（约定层）：

```toml
[plan]
enable = true
[work]
enable = true
default_effort = "medium"   # low | medium | high | xhigh
[review]
enable = true
[hooks]
enable = true
```

### 步骤 4：迁移 E2E 配置（TOML → JSON）

旧写法（**无效**，`loadFromToml` 是桩）：

```toml
[harness.e2e_detection]
enabled = true
mode = "strict"
```

新写法：在 `.claude/config/e2e-detection.config.json` 中以 **JSON** 配置：

```json
{ "enabled": true, "mode": "strict", "timeout": 120 }
```

完整字段见 [`config/e2e-detection/e2e-detection.config.json`](../config/e2e-detection/e2e-detection.config.json)
与 [最佳实践 · E2E 字段表](best-practices.md#四e2e-检测字段参考json)。

### 步骤 5：迁移会话配置（TOML → .properties）

旧写法（**无效**）：

```toml
[harness.session]
auto_save = true
token_threshold = 0.8
max_saves = 10
```

新写法：在会话 `.properties` 文件中用扁平点分键：

```properties
session.auto_save.enable = true
session.auto_save.thresholds = 80,90
session.auto_save.max_saves = 10
```

完整字段见 [`config/session/session.properties`](../config/session/session.properties)
与 [最佳实践 · 会话字段表](best-practices.md#五会话管理字段参考properties)。

## 三、键名 / 环境变量纠错表

| 旧（错误） | 新（正确） | 说明 |
|------------|-----------|------|
| `HARNESS_SESSION_TOKEN_THRESHOLD` | `HARNESS_SESSION_THRESHOLDS` | 复数，逗号分隔列表 |
| `[harness.e2e_detection]`（TOML） | `.claude/config/e2e-detection.config.json`（JSON） | TOML 桩未实现 |
| `[harness.session].*` | `session.*`（.properties） | 格式与载体都变了 |
| `HARNESS_MODEL_SELECTION_ENABLED` | （不存在） | 用 `ANTHROPIC_*` 环境变量 |
| `HARNESS_MODEL_SELECTION_STRATEGY` | （不存在） | 恒为 `effortBased` |
| `harness config show/path/test/diff/migrate` | `harness validate config [--all]` | 唯一配置命令 |
| `harness doctor --config` | `harness doctor analyze config` | `config` 是位置参数 |

## 四、被移除能力的现状替代

| 旧配置段 | 现状 | 替代方式 |
|----------|------|----------|
| `[harness.review].*` | Java 不解析 | 审查严格度等用 `HARNESS_REVIEW_MODE` 等技能层环境变量 |
| `[harness.multilang]` | 技能路由实现 | 编辑 `skills/harness-review/references/code-standards/` |
| `[harness.branch_isolation]` | 技能 + settings.json | 在 `.claude/settings.json` 配 `branchIsolation` |
| `[harness.execution]` | 自动判定 | 由 harness-work 依任务数选择 Solo/Parallel/Breezing |
| `[harness.tdd]` | 任务标签控制 | 用 `[tdd:required]` / `[tdd:skip:*]` 标签 |
| `[harness.model_selection]` | 恒用默认 | 用 `ANTHROPIC_*` 环境变量 |
| `[harness.notifications/.slack]` | 未实现 | 暂无；路线图项 |
| `[harness.debug]` / `HARNESS_DEBUG` | Java 不读取 | 用日志框架配置 |

## 五、校验迁移结果

```bash
# 1. 校验配置语法
harness validate config harness.toml --all

# 2. 同步生成插件文件，确认无报错
harness sync

# 3.（可选）健康检查
harness doctor analyze config
```

三项均通过即完成迁移。

## 六、相关文档

- [配置模板](../config/harness.toml.default)
- [环境变量参考](environment-variables.md)
- [配置最佳实践](best-practices.md)
