# Effort Routing Detail

`harness-work` 的 effort tier 判定逻辑完整规格（本体 SKILL.md 只保留决策表）。

## 背景

Opus 4.8 的 thinking 默认关闭，effort 是推理深度的主要杠杆（比过去的任何 Opus 都受 effort 影响更大）。
观察到"浅层推理"时，不通过 prompt 规避，而是提高 effort。
复杂任务的强化废除向 spawn prompt 注入 free-text marker（旧 `ultrathink`）的方式，统一为从复杂度分数选择 Worker spawn 的 effort tier 的方式。
这与 `docs/model-routing-policy.md`（不从 free-text 推测 effort）和 `.claude/rules/claude-5-prompt-standard.md`"维持的纪律 5"（`xhigh` 由调用方选择）一致。

## 多要素评分

着手任务时合计以下分数。

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 更改对象 4 文件以上 | +1 |
| 目录 | 包含 core/, guardrails/, security/ | +1 |
| 关键词 | 包含 architecture, security, design, migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |
| 明确指定 | PM 模板记载 `effort: high` / `effort: xhigh`（旧 `ultrathink` 也兼容受理） | +3（自动采用） |

## effort tier 的决定方式（不注入）

从分数将 effort tier 决定为 **escalation signal**（spawn prompt 中 **不写入** `ultrathink` 等 marker 字符串）。
适用的 lever 仅以下 2 个：

- **session `/effort`**: 进入复杂任务批次前 host 设置 `/effort high` / `/effort xhigh`（session 单位有效的可靠 lever）。
- **worker frontmatter**: `agents/worker.md` 的 `effort`（默认 `medium`）为 floor。CC 的 Agent / Task spawn API 不公开 per-spawn 的 effort 指定，因此没有逐个 Worker 提高 effort 的机制。分数记录在 `worker-report.v1` 的 `task_complexity_note`，作为 Lead 判断提高 session effort 的材料。

| 分数 | code-risk（包含 core/guardrails/security/architecture/migration） | effort tier |
|--------|-----------------------------------|-------------|
| 0-2 | 不问 | `medium`（保持 Worker frontmatter 默认） |
| ≥ 3 | 无 | `high` |
| ≥ 3 | 有 | `xhigh` |

breezing 模式也适用相同逻辑（harness-work 统一管理）。
Worker 使用 Sonnet 4.6，`xhigh` 降级为实际 `high`，但 tier 提升本身有效（`docs/effort-level-policy.md`）。
