# Harness 配置目录

本目录集中存放 Java Harness 的**配置模板与案例**，以及指向详细说明文档的入口。
所有内容都以 Java 源码为准绳：只描述真正被解析的配置项，未实现的项明确标注。

> ⚠️ 重要：Java Harness 的"配置"不是单一 schema。请先阅读
> [`harness.toml.default`](harness.toml.default) 顶部的"架构说明"，了解
> 哪些节由 Java CLI 解析、哪些是技能层约定、哪些尚属规划。

## 📁 目录结构

```
harness-project/config/
├── README.md                       # 本文件
├── harness.toml.default            # 完整配置模板（权威版，含状态标注）
├── harness-review-config.toml      # 审查相关参考配置
│
├── review/                         # 代码审查（技能层约定 + 环境变量）
│   └── review.config.toml
├── e2e-detection/                  # 端到端检测（实际为 JSON）
│   └── e2e-detection.config.json
├── session/                        # 会话管理（实际为 .properties）
│   └── session.properties
└── multilang/                      # 多语言规范说明（由技能路由，非 TOML）
    └── README.md
```

> 说明：早期草稿曾规划 `monitoring/`、`notifications/`、`ci_integration/` 等子目录。
> 经源码核查，这些功能目前**没有 TOML 配置解析器**，相关键名仅作为"规划中"项
> 保留在 `harness.toml.default` 的 🔴 区段，因此本目录不再为它们单独建案例文件。

## 🎯 快速开始

### 1. 生成项目配置

最稳妥的方式是用内置命令生成模板（会创建 `.claude/harness.toml.bak`）：

```bash
harness init
```

或手动复制权威模板：

```bash
cp docs/harness-project/config/harness.toml.default harness.toml
# 按需编辑 [project] / [agent] / [safety.*] 等已实现节
```

### 2. 校验配置

```bash
# 校验单个配置文件
harness validate config harness.toml

# 校验所有配置文件
harness validate config --all
```

> 注意：唯一的配置类 CLI 命令是 `harness validate config`。
> 不存在 `harness config show`、`harness config path`、`harness config test`、
> `harness config diff`、`harness config migrate`（这些在早期文档中出现过，但未实现）。

### 3. 同步生成插件文件

`harness sync` 读取 `harness.toml` 中的 `[project]/[agent]/[env]/[safety.*]`，
生成 `.claude-plugin/settings.json` 与 `plugin.json`：

```bash
harness sync
harness sync status     # 查看同步状态
harness sync validate   # 校验同步结果
```

## 🔌 技能 vs CLI 命令

| 技能（Claude Code 斜杠命令） | 对应 CLI 命令 |
|------------------------------|---------------|
| `/harness-work`              | `harness work` |
| `/harness-review`            | `harness review` |
| `/harness-sync`              | `harness sync` |
| `/harness-release`           | `harness release` |
| `/harness-plan`              | `harness plans`（仅 plans 组） |
| `/harness-session`           | 无 CLI 对应（仅 `session-register`/`session-unregister`） |
| `/harness-progress`          | 无 CLI 对应 |

斜杠命令是技能（由宿主 LLM 执行），并非 CLI 子命令。

## 📊 配置加载优先级

从高到低：

1. **环境变量**（临时覆盖，最高） —— 见 [环境变量参考](../configuration/environment-variables.md)
2. **平台特定配置**（`.claude/config.toml` 或 `.codex/config.toml`）
3. **标准配置文件**（项目根 `harness.toml`）
4. **代码内置默认值**

## 📚 详细文档

- [环境变量参考](../configuration/environment-variables.md)
- [配置最佳实践](../configuration/best-practices.md)
- [配置迁移指南](../configuration/migration-guide.md)

## 🧩 模块配置案例

需要某个功能模块的独立配置时，参考对应子目录：

| 模块 | 案例文件 | 实际格式 | 读取者 |
|------|----------|----------|--------|
| 代码审查 | [`review/review.config.toml`](review/review.config.toml) | TOML（约定层） | 技能 / 脚本 |
| 端到端检测 | [`e2e-detection/e2e-detection.config.json`](e2e-detection/e2e-detection.config.json) | JSON | `HarnessConfigManager` |
| 会话管理 | [`session/session.properties`](session/session.properties) | .properties | `SessionConfigLoader` |
| 多语言规范 | [`multilang/README.md`](multilang/README.md) | 说明 | 技能路由 |

> 模块案例的格式（JSON / .properties）与"配置模板"（TOML）不同，这是由实现决定的：
> E2E 与会话模块在 Java 中分别用 JSON 与 .properties 加载，详见各案例文件头注释。
