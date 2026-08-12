---
name: harness-accept
description: "Generate an Acceptance Demo HTML for non-engineer vibecoders right before ship/wait/reject decision. Reads back the acceptance_criteria that were stored as personal-preference.v1 by harness-plan-brief (joined by user_request_hash), then renders a single-file HTML showing each criterion as verified or unverified along with a ship/wait/reject recommendation. Use when the user asks for an acceptance review, wants to decide whether to ship a delivered task, or says: acceptance demo, accept demo, 受け入れ判断, 受入レビュー, ship/wait/reject 判定, 検収レビュー. Do NOT load for: implementation, code review, release work."
description-en: "Generate an Acceptance Demo HTML for non-engineer vibecoders right before ship/wait/reject decision. Reads back the acceptance_criteria that were stored as personal-preference.v1 by harness-plan-brief (joined by user_request_hash), then renders a single-file HTML showing each criterion as verified or unverified along with a ship/wait/reject recommendation. Use when the user asks for an acceptance review, wants to decide whether to ship a delivered task, or says: acceptance demo, accept demo, 受け入れ判断, 受入レビュー, ship/wait/reject 判定, 検収レビュー. Do NOT load for: implementation, code review, release work."
description-zh: "在实现完成后的验收判断（ship / wait / reject）前生成验收演示 HTML。通过 `user_request_hash` 获取 harness-plan-brief 以 `personal-preference.v1` 写入的 acceptance_criteria，按标准显示 verified / unverified 状态。计算 ship / wait / reject 三项建议值，并在 HTML 上可视化依据。当用户提到：验收判断、验收审查、ship/wait/reject 判定、验收测试时使用。不适用于：实现工作、代码审查、发布。"
allowed-tools: ["Read", "Write", "Edit", "Bash"]
argument-hint: "[task-description]"
user-invocable: true
---

# harness-accept

为非工程师的订购者・制作人职业，用 **1 张 HTML** 提示实现完成任务后的验收判断 (ship / wait / reject) 的技能。
用于订购者的认知负荷峰值 (3) 验收判断阶段。

作为 Phase 65.1.x (`harness-plan-brief`) 的对构造运行，读取 Plan Brief 中批准的 `acceptance_criteria` 进行评价。

## Quick Reference

- "**制作 Acceptance Demo**" → 此技能
- "**想做验收判断**" → 此技能
- "**ship/wait/reject 判定**" → 此技能

## 责任边界

| 范围 | 此技能的职责 |
|------|----------------|
| 搜索 | **仅当前项目** (必须指定 `project: <current>`, `strict_project: true`) |
| 跨项目 | **不做** (Phase 65.3 以后用 `--cross-project-group <name>` flag opt-in 解禁) |
| Plan Brief 联动 | 以 `user_request_hash` 为 join key read `personal-preference.v1` (Phase 65.1.4) |
| 写入 | 不做 (验收批准后的 memory write 是 `accept-record-decision.sh` 的职责) |
| recommendation 计算 | verified / 全 criteria 的比率用 0.8 / 0.5 阈值判定。逻辑在 `scripts/render-html.sh` 直前计算 |

## 输入

向参数 `[task-description]` 传递用户的 request (与 Plan Brief 时相同文)。
无参数时用对话形式接收。

## 输出

| 输出 | 路径 | 格式 |
|------|------|------|
| Acceptance Demo HTML | `.claude/state/views/accept-<timestamp>.html` | 可单独打开的 HTML (无 server，无 JS framework) |
| Acceptance context JSON | `.claude/state/views/accept-<timestamp>.context.json` | `acceptance-context.v1` schema |

## Schema: `acceptance-context.v1`

```json
{
  "schema": "acceptance-context.v1",
  "user_request": "string",
  "user_request_hash": "sha256 hex (Plan Brief 侧的 personal-preference.v1 与 join)",
  "demo_artifacts": [
    { "kind": "video|screenshot|text", "path": "string" }
  ],
  "verified_criteria": [
    { "name": "string", "passed": true, "evidence": "string" }
  ],
  "tdd_verified": "yes|no|not-required|skip:<reason>",
  "unverified_caveats": ["string"],
  "past_issue_patterns": [
    { "pattern_id": "P5", "title": "string", "verified_in_current_task": true }
  ],
  "recommendation": "ship|wait|reject",
  "recommendation_evidence": ["string"],
  "project": "string",
  "generated_at": "ISO8601"
}
```

完整 schema 参照 [`schemas/acceptance-context.v1.schema.json`](${CLAUDE_SKILL_DIR}/schemas/acceptance-context.v1.schema.json)。

## Recommendation 计算逻辑

```
verified_count    = count of verified_criteria where passed=true
total_criteria    = count of verified_criteria
ratio             = verified_count / total_criteria  (total=0 时为 0)

  ratio >= 0.8 → "ship"
  ratio >= 0.5 → "wait"
  ratio <  0.5 → "reject"
  total = 0    → "reject" (criteria 0 件无法判定，安全侧 reject)
```

评价依据用 literal 数值留在 `recommendation_evidence`。
例: "verified 4 件 / 全 5 件 (80%) → ship 阈值以上"

## Execution Flow

技能启动时，Claude 按以下步骤运行。

### Step 1: 解析 project name 和 user_request_hash

```bash
PROJECT_NAME="$(basename "$(git rev-parse --show-toplevel)")"
USER_REQUEST_HASH="$(printf '%s' "$USER_REQUEST" | sha256sum | awk '{print $1}')"
```

`PROJECT_NAME` 为空 (git 外) 时默认用 `current`。

### Step 2: **project-only** 搜索 harness-mem，获取 Plan Brief 侧 record (default)

参数无 `--cross-project-group <name>` flag 时 (default behavior):

用以下参数调用 `mcp__harness__harness_mem_search`:

```
project: <PROJECT_NAME>
strict_project: true
tags: ["personal-preference", "plan-brief-approval"]
limit: 10
```

> **重要**: `project` 参数**必需**。指定 `strict_project: true`，跨项目搜索**绝对不做**。

获取的 record 用 `data.user_request_hash == <USER_REQUEST_HASH>` 过滤，选择最新的 1 件。
这保持 Plan Brief 时的批准内容 (chosen_option / acceptance_criteria 等)。

### Step 2 (alt): cross-project search (Phase 65.3.5 opt-in)

参数有 `--cross-project-group <name>` flag 时，获取横断 group 内其他项目的
类似 plan-brief-approval / acceptance-decision 履历 (D43 Option α):

```bash
MEMBERS_JSON="$(bash scripts/load-cross-project-groups.sh --group "<name>" 2>/dev/null)" || {
  echo "ERROR: cross-project group not found: <name>" >&2
  exit 1
}
```

`MEMBERS_JSON` 为 `[]` 时 fallback 到 default 单一 project search。

`MEMBERS_JSON` 非空时，各 member project 执行 1 次 MCP search:

```
for each project in MEMBERS_JSON:
  mcp__harness__harness_mem_search(
    project: <member>,
    strict_project: true,
    tags: ["personal-preference", "plan-brief-approval"],
    limit: 10
  )
```

结果在 client 侧合并，用 `data.user_request_hash == <USER_REQUEST_HASH>` 过滤。
hash 一致基本因为同一 user request 由来，多项目的重复稀少，但为保险起见 id 单位 dedupe。

采用 cross-project 由来的 record 时，可能混入过去其他案件的 chosen_option / acceptance_criteria，
因此 HTML 输出时**必须使用 `--with-redaction` flag**:

```bash
bash scripts/render-html.sh --template accept ... --with-redaction
```

详情参照 Go 版本的 `.claude/rules/cross-repo-handoff.md`；Java 版本不提供该
宿主规则文件，跨仓库交接以 `harness evidence` 和宿主平台的 review 记录为准。

### Step 3: 获取过去的问题模式 (Phase 65.2.2 委托)

```bash
bash scripts/accept-past-issues.sh --project "$PROJECT_NAME" --task "$USER_REQUEST" > "$PAST_ISSUES_JSON"
```

此脚本对 patterns.md (P1-P33) 和过去的 `acceptance-context.v1` record 进行 semantic search，
返回最多 3 件 `past-issue.v1`。各自附带 `verified_in_current_task: bool`。

### Step 4: 组装 verified_criteria

对 Plan Brief 时的 acceptance_criteria 各项，评价当前任务状态。
用户 (或 Claude) 提示"验证过的 evidence"，填写 `evidence` 字符串。

`evidence` 为空字符串时，HTML 上警告显示 (DoD c)。

需要 TDD 的 task，Acceptance Demo 必须输出 `TDD verified: yes|no` 的 1 行。
TDD 不需或 skip 时显示 `TDD verified: not-required` 或 `TDD verified: skip:<reason>`。
能 `yes` 限于确认 `.claude/state/tdd-red-log/<task-id>.jsonl` 的 Red 迹迹，或 literal failing test output 时。

### Step 5: 计算 recommendation

按上述"Recommendation 计算逻辑"决定 ship / wait / reject。

### Step 6: 生成 HTML

用 `templates/html/accept.html.template` 调用 `scripts/render-html.sh` (Phase 65.1.1):

```bash
bash scripts/render-html.sh \
  --template accept \
  --data "$CONTEXT_JSON" \
  --out "$HTML_OUT"
```

### Step 7: 浏览器自动 open

再利用 `scripts/plan-brief-open.sh` (Phase 65.1.2 导入的**通用 OS dispatcher**):

```bash
bash scripts/plan-brief-open.sh "$HTML_OUT"
```

> **注**: 脚本名包含"plan-brief"，但实体是 OS 别 browser open dispatcher，kind 中立。
> Phase 65.1.2 先导因此 historical name。Layer 3 (HTML 直前最终 scan) 等其他用途也再利用。
> `BROWSER=true` env 设定时 (CI 环境)，open **skip** 仅 `printf` 输出 path。

### Step 8: 等待用户判断

确认"是否采用 ship / wait / reject 的 recommendation，或 override"。
判断后的 memory write 是其他技能 (`accept-record-decision.sh`，Phase 65.2.3) 的职责。

## 失败时的行为

| 失败 | 行为 |
|------|------|
| `mcp__harness__harness_mem_search` 不达 | 显示警告，`verified_criteria` 空数组继续 (recommendation = reject) |
| 未找到 Plan Brief 侧 record | 输出 warning，`verified_criteria` 空数组继续 |
| `git rev-parse --show-toplevel` 失败 | `PROJECT_NAME=current` 继续 |
| `accept-past-issues.sh` 失败 | `past_issue_patterns: []` 继续 (best-effort) |
| `render-html.sh` 失败 | 错误输出到 stderr 并 exit 1 |

## Related

- `harness-plan-brief` (Phase 65.1.2) — 计划阶段的对构造技能。本技能用 `user_request_hash` join Plan Brief 时的 `personal-preference.v1` 进行 read
- `scripts/accept-past-issues.sh` (Phase 65.2.2) — 获取过去问题模式 (read side)
- `scripts/accept-record-decision.sh` (Phase 65.2.3) — 批准 memory write (`acceptance-decision.v1`)
- `scripts/render-html.sh` (Phase 65.1.1) — HTML 模板引擎
- `scripts/plan-brief-open.sh` (Phase 65.1.2) — 通用 OS browser dispatcher
- `harness-progress` skill (Phase 65.4.1) — 进度管理技能 (3 surface 中的正中)
