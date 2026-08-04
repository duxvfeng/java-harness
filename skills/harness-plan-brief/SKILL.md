---
name: harness-plan-brief
description: "Generate a Plan Brief HTML for non-engineer vibecoders before implementation starts. Searches harness-mem (project-only) for relevant past decisions, patterns, and Plans archive entries, then renders a single-file HTML artifact summarizing understanding, options, risks, acceptance criteria, and confidence. Use when the user requests a planning preview, a non-engineer-friendly summary before approval, or says: plan brief, planning preview, 計画概要, 計画レビュー. Do NOT load for: actual implementation, code review, release work."
description-en: "Generate a Plan Brief HTML for non-engineer vibecoders before implementation starts. Searches harness-mem (project-only) for relevant past decisions, patterns, and Plans archive entries, then renders a single-file HTML artifact summarizing understanding, options, risks, acceptance criteria, and confidence. Use when the user requests a planning preview, a non-engineer-friendly summary before approval, or says: plan brief, planning preview, 計画概要, 計画レビュー. Do NOT load for: actual implementation, code review, release work."
description-zh: "在实现开始前生成计划简报 HTML。仅在当前项目中搜索 harness-mem（`strict_project: true`），从过去的决策、模式和 Plans 存档中提取类似案例，整理为 `plan-brief-context.v1` schema，通过 `render-html.sh` 生成独立 HTML 并自动在浏览器中打开。当用户提到：计划概要、非工程师事前共享、提案前审查时使用。不适用于：实现工作、代码审查、发布。"
allowed-tools: ["Read", "Write", "Edit", "Bash"]
argument-hint: "[task-description]"
user-invocable: true
---

# harness-plan-brief

面向非工程师的委托方/制作人角色，以 **1 页 HTML** 展示 Claude 即将开始实施的计划。
在委托方认知负荷峰值 (1) 计划理解阶段使用。

## Quick Reference

- "**Plan Brief を作って**" → 此技能
- "**実装前にざっくり整理**" → 此技能
- "**非エンジニア向けに計画を見せて**" → 此技能

## 责任边界

| 范围 | 此技能的职责 |
|------|-----------------|
| 搜索 | **仅限当前项目** (`project: <current>`, `strict_project: true` 必须指定) |
| 跨项目 | **不做** (Phase 65.3 以后通过 `--cross-project-group <name>` flag opt-in 开放) |
| 写入 | 不做 (Plan Brief 批准后的 memory write 是 `plan-brief-record-decision.sh` 的职责) |
| plan_readiness 计算 | 委托给 `scripts/plan-brief-compile.sh`。兼容字段名 `confidence` 保留，但含义限定为 DoD 明确度 + 依赖解决率 |

## 输入

将用户的 request 传递给参数 `[task-description]`。
没有参数时以交互形式接收。

## 输出

| 输出 | 路径 | 形式 |
|------|------|------|
| Plan Brief HTML | `.claude/state/views/plan-brief-<timestamp>.html` | 可独立打开的 HTML (no server, no JS framework) |
| Plan Brief context JSON | `.claude/state/views/plan-brief-<timestamp>.context.json` | `plan-brief-context.v1` schema |

## Schema: `plan-brief-context.v1`

```json
{
  "schema": "plan-brief-context.v1",
  "user_request": "string (用户的 request 原文)",
  "my_understanding": "string (Claude 的理解用 1-3 段落表达)",
  "options": [
    { "name": "string", "summary": "string", "pros": ["string"], "cons": ["string"] }
  ],
  "risks": [
    { "kind": "string", "severity": "info|warn|critical", "description": "string", "mitigation": "string" }
  ],
  "acceptance_criteria": [
    { "id": "string", "description": "string", "verifiable_by": "string" }
  ],
  "tdd_required": "yes|no|skip:<reason>",
  "confidence": 0,
  "confidence_evidence": ["string (plan_readiness evidence: 仅限 DoD clarity + dependency resolution)"],
  "related_decisions": [
    { "id": "string", "title": "string", "relevance": "string" }
  ],
  "similar_past_plans": [
    { "archive_path": "string", "phase": "string", "outcome": "cc:完了|cc:WIP|cc:TODO|skipped", "relevance": "string" }
  ],
  "project": "string",
  "generated_at": "ISO8601"
}
```

完整 schema 请参考 [`schemas/plan-brief-context.v1.schema.json`](${CLAUDE_SKILL_DIR}/schemas/plan-brief-context.v1.schema.json)。

## Execution Flow

技能启动时，Claude 按以下步骤运行。

### Step 1: 解析 project name

```bash
PROJECT_NAME="$(basename "$(git rev-parse --show-toplevel)")"
```

如果 `PROJECT_NAME` 为空 (git 外)，默认使用 `current`。

### Step 2: 以 **project-only** 搜索 harness-mem (默认)

参数中**没有** `--cross-project-group <name>` flag 的情况 (默认行为):

**必须** 用以下参数调用 `mcp__harness__harness_mem_search`:

```
project: <PROJECT_NAME>
strict_project: true
query: <user request>
expand_links: true
limit: 5
```

> **重要**: `project` 参数是**必须的**。不得传递空字符串或 `null`。
> 指定 `strict_project: true`，**绝不进行**跨项目搜索。
> 必要时可以用 `tags` filter 筛选 `decision` / `pattern`，但 `project` 固定。

从过去的 decision (D1-D41) / pattern (P1-P33) / Plans archive 28 件中获取最多 5 件类似案件。

### Step 2 (alt): cross-project search (Phase 65.3.5 opt-in)

参数中**有** `--cross-project-group <name>` flag 的情况:

按照 D43 Option α (MCP N-call)，以下列步骤进行跨项目搜索。

```bash
# (a) 解析 group → member projects (yaml SSOT)
MEMBERS_JSON="$(bash scripts/load-cross-project-groups.sh --group "<name>" 2>/dev/null)" || {
  echo "ERROR: cross-project group not found: <name>" >&2
  exit 1
}
# MEMBERS_JSON 是 ["proj1","proj2",...] 形式的 JSON 数组
```

如果 `MEMBERS_JSON` 为 `[]` (空数组)，输出 warning 并 fallback 到默认的单项目搜索。

如果 `MEMBERS_JSON` 非空，对**每个 member project 发起 1 次 MCP search**:

```
for each project in MEMBERS_JSON:
  mcp__harness__harness_mem_search(
    project: <member>,
    strict_project: true,
    query: <user request>,
    expand_links: true,
    limit: 5
  )
```

将各 search 结果在 **client 端合并・去重 (按 id)・按 relevance_score 降序排序**，筛选最多 5 件。
注意：总调用数会增加 (group 为 5 project 则 5 次)，延迟会增加。

> **D43 判断 1 的依据**: MCP tool schema 没有暴露 `projects: [array]` 也没有 `strict_project: false`，
> 因此横断搜索只能用 client 端 N-call。
> 详情请参考 `.claude/rules/cross-repo-handoff.md` 的「Phase 65.3 实施决定事项 (D43)」。

跨项目结果必须经过 Layer 2/3 (Phase 65.3.2-65.3.4) 的 redaction:
- HTML 渲染时使用 `bash scripts/render-html.sh ... --with-redaction`
- 通过词典 + NER + final scan 的 3 阶段防止固有名词泄露

### Step 3: 构建 context JSON

使用 `scripts/plan-brief-compile.sh`，从 mem search 结果构建符合
`plan-brief-context.v1` schema 的 JSON。

Phase 105.3 以后，Plan Brief 的 `confidence` 是向后兼容的字段名，
显示含义视为 `plan_readiness`。计算轴仅固定为以下 2 个。

- DoD 明确度: request / DoD 包含多少可机器验证的数值・条件
- 依赖解决率: 类似 Plans 中可作为已完成处理的依赖的占比

过去类似案件的成功率、相关 Decision / Pattern 件数作为 context-only 依据显示，
不向 readiness 点数另轴加算。这是为了避免被误读为「AI 的理解度」「成功概率」。

`options` / `risks` / `acceptance_criteria` 始终生成至少 1 件。
即使 mem search 为空，也至少填充以下内容。

- `options`: 推荐案至少 1 件。必要时添加替代案，附带 pros / cons
- `risks`: 本次计划特有风险至少 1 件 (readness 误读、scope creep、未观测数据等)
- `acceptance_criteria`: 可机器验证或目视确认的条件至少 1 件

例:

```bash
jq -n \
  --arg req "$USER_REQUEST" \
  --arg proj "$PROJECT_NAME" \
  --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '{
    schema: "plan-brief-context.v1",
    user_request: $req,
    my_understanding: "(尚未着手)",
    options: [{name:"Option A: 最小验证前进", summary:"先确认 DoD 和依赖再实现", pros:["影响小"], cons:["大的重新设计需要另外任务化"]}],
    risks: [{kind:"readiness-misread", severity:"warn", description:"将 plan_readiness 误读为 AI 理解度的风险", mitigation:"在 evidence 中明确记载仅为 DoD 明确度 + 依赖解决率的指标"}],
    acceptance_criteria: [{id:"AC-1", description:"Plan Brief context 包含非空的 options / risks / acceptance_criteria", verifiable_by:"tests/test-plan-brief-compile.sh"}],
    confidence: 0,
    confidence_evidence: ["plan_readiness DoD 明确度: 0/60", "plan_readiness 依赖解决率: 0/40"],
    tdd_required: "no",
    related_decisions: [],
    similar_past_plans: [],
    project: $proj,
    generated_at: $ts
  }' > "$CONTEXT_JSON"
```

### Step 4: 生成 HTML

用 `templates/html/plan-brief.html.template` 调用 `scripts/render-html.sh` (Phase 65.1.1):

HTML 以 1 行显示 TDD 判定。
形式为 `tdd_required: yes`、`tdd_required: no` 或 `tdd_required: skip:<reason>` 之一。

```bash
bash scripts/render-html.sh \
  --template plan-brief \
  --data "$CONTEXT_JSON" \
  --out "$HTML_OUT"
```

### Step 5: 自动在浏览器中 open

通过 `scripts/plan-brief-open.sh` 进行 OS 别 dispatch:

```bash
bash scripts/plan-brief-open.sh "$HTML_OUT"
```

如果设置了 `BROWSER=true` 的 env (CI 环境)，open 被 **skip**，仅用 `printf` 输出 path。

### Step 6: 等待用户批准

确认「按此理解是否可以前进到实现」。
批准后的 memory write 是其他技能 (Phase 65.1.4 的 `plan-brief-record-decision.sh`) 的职责。

## 失败时的行为

| 失败 | 行为 |
|------|------|
| `mcp__harness__harness_mem_search` 不达 | 显示警告，以空数组继续 `related_decisions` / `similar_past_plans` |
| `git rev-parse --show-toplevel` 失败 | 以 `PROJECT_NAME=current` 继续 |
| `render-html.sh` 失败 | 向 stderr 输出错误并 exit 1 |
| `plan-brief-open.sh` 失败 | 仅向 stdout 输出 HTML path 并 exit 0 (browser open 是 best-effort) |

## Related

- `scripts/render-html.sh` (Phase 65.1.1) — HTML 模板引擎
- `scripts/plan-brief-compile.sh` (Phase 65.1.3) — context compilation
- `scripts/plan-brief-record-decision.sh` (Phase 65.1.4) — 批准 memory write
- `harness-accept` skill (Phase 65.2.1) — 验收判断技能 (对结构)
- `harness-progress` skill (Phase 65.4.1) — 进度管理技能 (对结构)
