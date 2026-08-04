---
name: harness-progress
description: "Generate a Progress Tracker HTML for non-engineer vibecoders to glance at session progress (cc:WIP / cc:TODO / cc:完了 counts, percentage, elapsed/estimated minutes, cost so far/estimate, drift alerts). Uses Plans.md as source of truth, renders a single-file HTML with auto-regeneration support. Use when user asks for progress overview, session status snapshot, dashboard, or says: progress tracker, 進捗確認, 進捗ボード, dashboard. Do NOT load for: actual implementation, code review, release work."
description-en: "Generate a Progress Tracker HTML for non-engineer vibecoders to glance at session progress (cc:WIP / cc:TODO / cc:完了 counts, percentage, elapsed/estimated minutes, cost so far/estimate, drift alerts). Uses Plans.md as source of truth, renders a single-file HTML with auto-regeneration support. Use when user asks for progress overview, session status snapshot, dashboard, or says: progress tracker, 進捗確認, 進捗ボード, dashboard. Do NOT load for: actual implementation, code review, release work."
description-zh: "生成进度追踪器 HTML。以 Plans.md 为唯一数据源，在单页 HTML 中显示 cc:WIP / cc:TODO / cc:已完成 数量、百分比、经过时间、成本和漂移告警。PostToolUse hook 每 60 秒自动重新生成。当用户提到：进度确认、进度面板、dashboard 时使用。不适用于：实现工作、代码审查、发布。"
allowed-tools: ["Read", "Write", "Bash"]
argument-hint: "[--out <path>] [--no-open]"
user-invocable: true
---

# Harness Progress Tracker

Phase 65.4 (Progress Tracker) — 3rd surface of the cognitive-load HTML triplet.
Plan Brief / Acceptance Demo 之后第三个 HTML surface，通过**单页纸掌握进行中会话的整体状况**。

## Quick Reference

| 输入 | 动作 |
|---|---|
| `/harness-progress` | 生成并打开当前项目的进度快照 HTML |
| `/harness-progress --no-open` | 仅生成（不开浏览器，用于 PostToolUse hook） |
| `/harness-progress --out <path>` | 指定输出路径（默认：`out/progress-snapshot.html`） |

## Mission

> "当前会话完成了多少任务、进展到何种程度、预计何时结束、已花费多少"，让非工程师 vibecoder 能够**在浏览器中 3 秒钟掌握**的 1 页 HTML。

**功能范围**:
- 统计 Plans.md 的 cc:TODO / cc:WIP / cc:已完成 数量
- 计算 progress_pct（完成率，cc:已完成 ÷ 总任务 × 100）
- 显示已用分钟数 / 预计总分钟数 / 已花费成本 / 预计成本
- 显示漂移告警（Phase 65.4.3 起填充数据）

**不在此范围** (本 cycle):
- WebSocket / SSE 实时更新（静态 HTML，通过重新生成更新）
- 与历史 session 的比较（Phase 65.4.4 另行处理）
- 其他项目的跨项目视图（与 Phase 65.3 独立）

## Schema: progress-snapshot.v1

詳細仕様: [schemas/progress-snapshot.v1.schema.json](${CLAUDE_SKILL_DIR}/schemas/progress-snapshot.v1.schema.json)

```yaml
schema:        progress-snapshot.v1
project:       <basename of git repo>
current_task:  <cc:WIP 的第一个任务 1 行摘要，否则空字符串>
progress_pct:  <0-100 的整数，cc:已完成 ÷ 总任务 × 100 的四舍五入>
todo_tasks:    [{number, title}]    ← 仅 cc:TODO
wip_tasks:     [{number, title}]    ← 仅 cc:WIP
done_tasks:    [{number, title, commit}]   ← 仅 cc:已完成 [hash]，hash 为 7 字符
elapsed_minutes:          <int, 从 state file 获取>
estimated_total_minutes:  <int, 从 state file 获取>
cost_so_far_usd:          <float, 从 state file 获取>
cost_estimate_usd:        <float, 从 state file 获取>
alerts:                    []   ← Phase 65.4.3 起填充数据
generated_at:             <ISO8601 UTC>
```

## Execution Flow

### Step 0: PROJECT_NAME を取得

```bash
PROJECT_NAME="$(basename "$(git rev-parse --show-toplevel)" 2>/dev/null || echo "current")"
```

### Step 1: 构建 snapshot

```bash
SNAPSHOT_JSON="$(mktemp /tmp/progress-snapshot-XXXX.json)"
bash scripts/progress-snapshot.sh \
  --plans Plans.md \
  --project "$PROJECT_NAME" \
  > "$SNAPSHOT_JSON"
```

`scripts/progress-snapshot.sh` (Phase 65.4.1 实现) 解析 Plans.md 并
输出符合 `progress-snapshot.v1` schema 的 JSON。

### Step 2: 渲染 HTML

```bash
OUT_PATH="${OUT_PATH:-out/progress-snapshot.html}"
mkdir -p "$(dirname "$OUT_PATH")"

bash scripts/render-html.sh \
  --template progress \
  --data "$SNAPSHOT_JSON" \
  --out "$OUT_PATH"
```

### Step 3: 在浏览器中打开

仅当**没有** `--no-open` flag 时执行（PostToolUse hook 的后台重新生成时跳过）：

```bash
bash scripts/plan-brief-open.sh --path "$OUT_PATH"
```

## Cross-project search (默认 OFF)

Phase 65.4.4 起将添加 `--cross-project-group <name>` flag。本 cycle (65.4.1) 默认为 OFF，仅统计当前项目。

## Failure modes

| 状态 | 动作 |
|---|---|
| Plans.md 不存在 | `progress-snapshot.sh` 退出码 1（清晰的错误消息） |
| Plans.md 没有任何任务 | `progress_pct: 0`，空数组生成 snapshot（HTML 显示"无任务"） |
| 缺少 state file（已用分钟数等） | fallback 为 `elapsed_minutes: 0`，`cost_so_far_usd: 0`（无警告） |
| 没有 `git` / 在 git repo 外 | fallback 为 `project: "current"` |

## Related

- `harness-plan-brief` (Phase 65.1.x) — 第 1 个 surface（实施前的说明会）
- `harness-accept` (Phase 65.2.x) — 第 2 个 surface（验收判断）
- `harness-progress` (本 skill, Phase 65.4.x) — 第 3 个 surface（进度仪表板）
- 65.4.2（PostToolUse 自动重新生成）、65.4.3（5 种漂移告警）、65.4.4（历史判断 lookup）扩展功能
