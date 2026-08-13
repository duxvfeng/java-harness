# 配置最佳实践

本文档给出 Java Harness 配置的推荐做法、常见场景与故障排除。所有建议都基于
"已实现"的配置项——请勿依赖尚未实现（🔴 规划中）的节。

## 一、最小可用配置

绝大多数项目只需要"插件同步 schema"。最小 `harness.toml`：

```toml
[harness]
version = "4.1.1"
backend = "auto"

[project]
name = "my-plugin"
version = "1.0.0"

[agent]
default = "claude-sonnet-5"

[safety.permissions]
ask = ["Bash"]
```

用以下命令校验并生成插件文件：

```bash
harness validate config harness.toml
harness sync
```

## 二、按项目类型选择配置

### 纯后端项目（无前端）

- 在 `.claude/config/e2e-detection.config.json` 中关闭前端检测，或用环境变量：

```bash
export HARNESS_E2E_FRONTEND=false
```

- 启用 `smart_skip` 让系统自动识别"无前端"并跳过（见下方字段表）。

### 多语言仓库

- 多语言审查由 `harness-review` 技能路由处理，**无需** `[harness.multilang]` 段。
- 各语言标准参考 `skills/harness-review/references/code-standards/`。

### 团队协作（需要会话保存）

- 部署 `.claude/config/session.properties`（见 [`config/session/session.properties`](../config/session/session.properties)）。
- 推荐保留默认阈值 `80,90`，仅在磁盘紧张时调小 `session.auto_save.max_saves`。

## 三、配置分层原则

1. **优先用最小配置**：只写你真正需要覆盖的键，其余走默认值。
2. **临时值用环境变量**：CI 一次性覆盖用 `HARNESS_*`，不污染 `harness.toml`。
3. **不复制未实现节**：`harness.toml.default` 中 🔴 段仅供未来参考，写入项目配置
   不会生效，反而误导维护者。
4. **格式别搞混**：
   - 插件同步 / 后端声明 / 功能开关 → `harness.toml`（TOML）
   - 端到端检测 → `.claude/config/e2e-detection.config.json`（**JSON**）
   - 会话管理 → `.properties`（**不是 TOML**）

## 四、E2E 检测字段参考（JSON）

> JSON 不支持注释，故字段说明集中在此。文件位置：
> `.claude/config/e2e-detection.config.json`，由 `HarnessConfigManager.loadFromJson` 读取。

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `enabled` | bool | `true` | E2E 检测总开关 |
| `mode` | string | `"strict"` | `strict`（任一失败即阻止）/ `lenient`（仅关键失败阻止） |
| `timeout` | int | `120` | 单次测试超时（秒） |
| `retry_on_failure` | bool | `true` | 失败是否重试 |
| `max_retries` | int | `3` | 最大重试次数 |
| `auto_fix.enabled` | bool | `true` | 是否自动修复 |
| `auto_fix.max_iterations` | int | `3` | 自动修复最大轮次 |
| `auto_fix.fix_timeout` | int | `60` | 单次修复超时（秒） |
| `auto_fix.commit_on_fix` | bool | `true` | 修复后是否自动提交 |
| `test_types.<type>.enabled` | bool | 见下 | 是否启用该类型测试 |
| `test_types.<type>.framework` | string | `"auto"` | 测试框架（前端默认 `playwright`） |
| `test_types.<type>.test_paths` | array | `[]` | 测试路径 |
| `test_types.<type>.test_scenarios` | array | `[]` | 测试场景 |
| `triggers.auto_trigger_on_review_pass` | bool | `true` | 审查通过后自动触发 |
| `triggers.require_clean_workspace` | bool | `true` | 要求干净工作区 |
| `triggers.skip_on_draft_pr` | bool | `true` | 草稿 PR 跳过 |
| `triggers.skip_on_wip_branch` | bool | `true` | WIP 分支跳过 |
| `triggers.branch_patterns.include` | array | `["feature/*","bugfix/*","hotfix/*"]` | 触发分支包含模式 |
| `triggers.branch_patterns.exclude` | array | `["draft/*","wip/*","experimental/*"]` | 触发分支排除模式 |
| `smart_skip.enabled` | bool | `true` | 智能跳过开关 |
| `smart_skip.detect_project_type` | bool | `true` | 自动识别项目类型 |
| `smart_skip.skip_frontend_if_missing` | bool | `true` | 无前端时跳过前端检测 |
| `smart_skip.skip_backend_if_missing` | bool | `false` | 无后端时跳过后端检测 |
| `smart_skip.min_confidence_threshold` | number | `0.7` | 识别置信度阈值 |
| `smart_skip.fallback_strategy` | string | `"run_backend"` | `run_backend`/`run_all`/`skip_all` |

`test_types` 的 `<type>`：`frontend`（默认开）、`backend`（默认开）、`integration`（默认开）、
`performance`（默认**关**）、`security`（默认开）。

## 五、会话管理字段参考（.properties）

> 文件由 `SessionConfigLoader` 读取，扁平点分键。

| 键 | 类型 | 默认 | 说明 |
|----|------|------|------|
| `session.auto_save.enable` | bool | `true` | 自动保存开关 |
| `session.auto_save.thresholds` | int[] | `80,90` | Token 触发阈值（逗号分隔） |
| `session.auto_save.max_saves` | int | `10` | 保留的保存数量 |
| `session.auto_save.compression` | bool | `true` | 是否压缩 |
| `session.auto_save.save_interval_minutes` | int | `5` | 最小保存间隔（分钟） |
| `session.restore.auto_prompt` | bool | `true` | 启动时显示恢复提示 |
| `session.restore.max_history_age_days` | int | `7` | 历史保留天数 |
| `session.storage.max_total_size` | size | `500MB` | 最大总存储（支持 `NNMB`/`NNGB`/字节数） |
| `session.storage.max_single_save` | size | `50MB` | 单次保存上限 |
| `session.storage.async_save` | bool | `true` | 异步保存 |
| `session.storage.incremental_save` | bool | `true` | 增量保存 |

## 六、故障排除

| 现象 | 排查 |
|------|------|
| `harness sync` 报 backend 非法 | `[harness].backend` 必须是 `auto`/`codex`/`cursor` 之一 |
| `harness config show` 不存在 | 正常——唯一配置命令是 `harness validate config` |
| 改了 `[harness.review].mode` 没效果 | 该节未被解析；审查严格度用 `HARNESS_REVIEW_MODE`（技能层） |
| 改了 `[harness.session].*` 没效果 | 会话配置是 `.properties` 文件，不是 TOML；见上表 |
| E2E 配置不生效 | 确认改的是 `.claude/config/e2e-detection.config.json`（JSON），而非 TOML |
| 模型选择不生效 | 不存在文件配置；用 `ANTHROPIC_*` 环境变量 |
| `harness validate config` 提示找不到文件 | 用 `--all` 走查，或给出绝对路径 |

## 七、版本控制建议

- `harness.toml`、`.claude/config/*.json`、`.properties` 一并纳入版本控制。
- 含敏感信息的 `[env]` 键值改用真实环境变量注入，不要提交明文。
- 团队统一一份 `harness.toml.default` 派生配置，避免每人各异。

## 八、相关文档

- [配置模板](../config/harness.toml.default)
- [环境变量参考](environment-variables.md)
- [配置迁移指南](migration-guide.md)
