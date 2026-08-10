# Effort Routing 详细说明

`harness-work` 的 effort tier 判定逻辑的完整规格（本体 SKILL.md 仅保留决策表）。

## 背景

Opus 4.8 中 thinking 默认为 off，effort 是推理深度的主要杠杆（比过去的任何 Opus effort 影响都更大）。
观测到"浅推理"时不在 prompt 中回避而是提高 effort。
强化复杂任务废止了向 spawn prompt 注入 free-text marker（旧 `ultrathink`）的方式，统一为从复杂度分数选择 Worker spawn 的 effort tier 的方式。

## 多要素评分

着手任务时合算以下分数。

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 个文件以上 | +1 |
| 目录 | 包含 core/、guardrails/、security/ | +1 |
| 关键字 | 包含 architecture、security、design、migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |
| 显式指定 | PM 模板中记载 `effort: high` / `effort: xhigh`（旧 `ultrathink` 也兼容受理） | +3（自动采用） |

## effort tier 的决定方式（不注入）

从分数将 effort tier 作为 **escalation signal** 决定（不在 spawn prompt 中**写入** `ultrathink` 等标记字符串）。
仅应用以下 2 个 lever：

- **session `/effort`**：host 在进入复杂任务批次前设置 `/effort high` / `/effort xhigh`（session 单位有效的可靠 lever）。
- **worker frontmatter**：`agents/worker.md` 的 `effort`（默认 `medium`）为 floor。CC 的 Agent / Task spawn API 不公开 per-spawn 的 effort 指定，因此没有逐个 worker 提高 effort 的机制。分数记录在 `worker-report.v1` 的 `task_complexity_note`，Lead 作为提高 session effort 的判断材料。

| 分数 | code-risk（包含 core/guardrails/security/architecture/migration） | effort tier |
|--------|-----------------------------------|-------------|
| 0-2 | 不问 | `medium`（Worker frontmatter 默认） |
| ≥ 3 | 无 | `high` |
| ≥ 3 | 有 | `xhigh` |

breezing 模式也应用相同逻辑（harness-work 一体化管理）。
Worker 因使用 Sonnet 4.6，`xhigh` 实际降级为 `high`，但 tier 提升本身有效（`docs/effort-level-policy.md`）。
