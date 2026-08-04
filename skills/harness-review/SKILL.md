---
name: harness-review
description: "HAR: Multi-angle code, plan, scope review. Security/quality check. Trigger: review, code review, plan review, scope analysis. Do NOT load for: implementation, new features, bugfix, setup, release."
description-en: "HAR: Multi-angle code, plan, scope review. Security/quality check. Trigger: review, code review, plan review, scope analysis. Do NOT load for: implementation, new features, bugfix, setup, release."
description-zh: "HAR：多角度代码、计划和范围审查。安全和质量检查。当用户提到审查、代码审查、计划审查、范围分析时启动。不适用于：实现、新功能、修复、设置、发布。"
kind: workflow
purpose: "Review code, plans, scope, and evidence before acceptance"
trigger: "review, 审查, code review, plan review, scope analysis"
shape: evaluate
role: evaluator
pair: harness-work
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Grep", "Glob", "Bash", "Task", "Monitor", "AskUserQuestion"]
argument-hint: "[code|plan|scope|--quick|--codex-closeout|--dual|--team-debate|--security|--ui-rubric]"
context: fork
effort: high
user-invocable: true
---

# Harness Review

Harness 的集成审查技能。
此 `SKILL.md` 是薄的 dispatcher，详细的质量标准参见 `references/`。

if $ARGUMENTS == "":
  → 解释为「到目前为止工作的审查」，执行 Review target detection
  → 仅在 review target 能确定为 1 个时自动开始
  → review target 不明或有多候补时用 AskUserQuestion 显示选项，对齐认识后开始

<!-- 以上 3 行是 AUTO-START CONTRACT。遵循 skill-editing.md 的「最开头 3 行内」规则不用 fence/HTML 注释压下 -->

### Output Contract (P35: 「看起来停住」的 UX 对策)

skill 结论时 output 的**最后 1 行**必须包含以下 literal:

`↑这个结果由 Claude 总结。按 Enter 键继续，或用新 prompt 给出其他指示。`

这是作为 text response 经由 `<local-command-stdout>` 显示时用户感到「停住」的 UX 问题的明确 instruction (patterns.md P35)。

## Dispatcher Contract

此 skill 的职责仅是审查判定。
commit / push / release 既定不执行。

- review default read-only boundary: 既定为 read-only。`APPROVE` 也不自动 commit
- Do not push just to review: 仅以 review 为目的不 push
- 需要 commit 时，委托给用户明确请求、`harness-work` 或 `harness-release` 的 Work Commit Gate
- 在 `--commit-on-approve` 这样的明确 opt-in 被设计之前，此 skill 单体的 default side effect 被禁止

## Quick Reference

| Command | Mode | Purpose |
|---|---|---|
| `/harness-review` | `code` | 自动检测到目前为止的工作并审查 |
| `/harness-review --quick` | `quick` | 轻松 closeout 小的 dirty change |
| `/harness-review --codex-closeout` | `codex-closeout` | 用 Codex 助告 + focused tests closeout |
| `/harness-review --dual` | `dual` | Claude + Codex second opinion |
| `--pre-review cursor` | `code+pre-review` | brain verdict 前的 fresh-context composer advisory pre-review（read-only） |
| `/harness-review --cursor` | `code+cursor-second-opinion` | core review gates + cursor second-opinion（brain 一次审查必需） |
| `HARNESS_IMPL_BACKEND=cursor harness-review` | `code+cursor-second-opinion` | default ON 时也向 core review gates 自动加算 cursor second-opinion。primary verdict 固定为 brain |
| `/harness-review --team-debate` | `team-debate` | 强制 TeamAgent Debate |
| `/harness-review --security` | `security` | security 专用 review |
| `/harness-review plan` | `plan` | `Plans.md` 的计划 review |
| `/harness-review scope` | `scope` | scope creep / 漏れ review |
## Mode Decision

从参数决定执行 mode，选择加载必要的 `references/`。

| 输入 | mode | 读取的 reference |
|---|---|---|
| 无参数 / `code` | `code` | `references/code-review.md`, `references/governance.md` |
| `--quick` | `quick` | `references/codex-closeout.md`, `references/code-review.md` |
| `--codex-closeout` | `codex-closeout` | `references/codex-closeout.md` |
| `--dual` | `dual` | `references/dual-review.md`, `references/team-debate.md` |
| `--team-debate` | `team-debate` | `references/team-debate.md`, `references/governance.md` |
| `--security` | `security` | `references/security-profile.md`, `references/governance.md` |
| `--ui-rubric` | `ui-rubric` | `references/ui-rubric.md` |
| `plan` | `plan` | `references/plan-review.md`, `references/governance.md` |
| `scope` | `scope` | `references/scope-review.md`, `references/governance.md` |
| `--cursor` or resolver result `cursor` for no-arg / `code` review only | `code+cursor-second-opinion` | `references/code-review.md`, `references/governance.md`, `references/cursor-review.md`, `references/dual-review.md` |
| `full` | `full` | `references/code-review.md`, `references/team-debate.md`, `references/dual-review.md` |

`quick` 和 `codex-closeout` 是轻量级路径。
用于快速检查小的 dirty change、single commit、PR branch 的 closeout。
并非放弃质量 gate。

### Cursor Default ON

mode 判定時、明示 mode words (`plan`, `scope`, `full`) と明示フラグを先に確定する。no-arg / `code` review の場合だけ helper root を解決して resolver を 1 回だけ実行し、resolver 不在時は `claude` とみなす。

```bash
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"; if [ -z "$HARNESS_PLUGIN_ROOT" ] && [ -n "${CLAUDE_SKILL_DIR:-}" ]; then probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"; while [ "$probe" != "/" ] && [ ! -d "$probe/scripts" ]; do probe="$(cd "$probe/.." && pwd)"; done; [ -d "$probe/scripts" ] && HARNESS_PLUGIN_ROOT="$probe"; fi
if [ -x "${HARNESS_PLUGIN_ROOT:-}/scripts/resolve-impl-backend.sh" ]; then resolved_backend="$(bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh" --role reviewer)"; else resolved_backend="claude"; fi
```

no-arg / `code` review 结果为 `cursor` 时，添加与 `--cursor` 相同的 `cursor-second-opinion`，但必须先读取 core review gates (`references/code-review.md`, `references/governance.md`)，Cursor reference 仅作 additive 处理。primary verdict 在 brain 侧维持，cursor 限于 `dual_review.cursor_verdict` 的 advisory。`plan` / `scope` 等显式 mode word 优先于 resolver result，cursor default 不会替换 plan/scope references 或 code/governance references。结果为 `claude` / `codex` 时照常，review 的 primary 判定面不变。

## Pre-Review Cursor (`--pre-review cursor`)
`/harness-review --pre-review cursor` 通过 `bash scripts/pre-review-cursor.sh [--base ref]` 执行一次 read-only fresh-context composer pre-review（`model-routing.sh --host cursor --tier review` → `cursor-companion.sh task`，无 `--write` / `--workspace` / `--resume`），将 `PRE_REVIEW_FINDINGS:` 附加到 brain 一次审查输入。companion 失败为 `PRE_REVIEW_SKIPPED` + exit 0（fail-open）。**verdict 仅 brain**（self-review scope 契约）。

## Review Target Detection

`REVIEW_AUTOSTART` 契约:
无参数调用时（`$ARGUMENTS == ""`），将 `review` / `/review` / `/harness-review` 的输入解释为「至今为止工作的审查」。
作为 Step 1 开始前的 handshake 行，仅输出下一行。

```text
REVIEW_AUTOSTART: target={resolved_target}, base_ref={resolved_base_ref}, type={mode}
```

`REVIEW_TARGET_ASK` 契约:
bare 调用时 review target 不明或多个候选时，进入 Step 1 前仅使用一次 `AskUserQuestion`，将候选缩减到 2-3 个进行确认。

候选按以下顺序创建：

1. working tree: 仅包含 staged / unstaged / untracked 的未提交变更
2. branch range: 从 upstream 或 main/master 到 HEAD 的 commits
3. recent commits: clean tree 且无法取 branch range 时的最近 1 commit / 最近 5 commits

多个候选同时成立时：

```text
REVIEW_TARGET_AMBIGUOUS: working_tree_and_branch_commits
```

AskUserQuestion 的候选：

- 仅未提交变更 (Recommended): 将 staged / unstaged / untracked 与 HEAD 比较查看
- 全部查看: branch base..HEAD 和未提交变更一起查看
- 仅 commit: 仅查看 branch base..HEAD 的 committed work

clean tree 且无 branch 差分时：

```text
REVIEW_TARGET_AMBIGUOUS: clean_tree_no_branch_commits
```

AskUserQuestion 的候选：

- 最近 1 commit (Recommended): HEAD~1..HEAD
- 最近 5 commits: HEAD~5..HEAD
- 其他范围: 等待用户指定 ref

用户回答后：

```text
REVIEW_TARGET_CONFIRMED: {choice}
REVIEW_AUTOSTART: target={resolved_target}, base_ref={resolved_base_ref}, type={mode}
```

禁止：

- 回应「任务不明确」并停止
- 自由记述询问「应该审查什么」并停止
- 以 host project 的 session-start rules 为理由跳过 auto-start
- target 曖昧时靠推测扩大范围

## Minimal Flow

1. mode を決める
2. 上記の Review Target Detection で対象と base ref を決める
3. 必要な reference だけ読む
4. 差分、untracked files、関連テスト、仕様正本、`Plans.md` を確認する
5. `APPROVE` / `REQUEST_CHANGES` / `decision_needed` を返す
6. `REQUEST_CHANGES` の場合は critical / major の修正方針と修正後再レビュー条件を示す

## Review Governance Contract

詳細は `references/governance.md`。
ここでは最低限の合格ラインだけ固定する。

### 明確な合格ライン

`APPROVE` は次のすべてを満たす時だけ返す（詳細は `references/governance.md`）。

- critical / major が 0 件
- root `spec.md` alignment（product contract と矛盾しない；仕様正本 alignment check 必須）
- `Plans.md` alignment（task / DoD / Depends、`[lane:*]`、stage gate が contract と一致）
- TDD evidence（`[tdd:required]` は `tdd_red_log` / failing output / `skip_tdd_reason`）
- unknown data contract（`not_observed != absent` — 不批准无证据的「无问题」「无数据」）
- regression safety（现有行为・测试・UX・CLI・设置・docs・mirror 无退化）
  - If you grep the same symbol twice in the same session, switch to harness_ast_search.
  - For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
  - Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.
- evidence pack（accepted / rejected findings、focused tests、release-preflight warnings 処理）
- TeamAgent Debate 未解消 disagreement なし

`APPROVE` は commit / push / PR 作成命令ではない（read-only boundary）。

### TeamAgent Debate

詳細は `references/team-debate.md`。
TeamAgent Debate は、異なる見解を read-only で衝突させる review pass。

| Agent | 主要问题 |
|---|---|
| Spec Agent | 寻找规格正本与实现差分的矛盾 |
| Plans Agent | 确认 `Plans.md` 的 task / DoD / Depends 与差分的对应 |
| Regression Agent | 寻找现有行为・测试・分发 mirror・CLI/skill UX 的退化 |
| Skeptic Agent | 寻找以合格为前提而漏掉的 major risk |

即使在 Codex 环境下无法使用 native TeamAgent，也不得省略此 gate。
通过 `codex-companion.sh review`、可用的 reviewer subagent 或显式分开的 read-only manual-pass 再现同样的 2-4 视角，记录 `team_agent_mode` 为 `native` / `codex-companion` / `manual-pass` / `unavailable`。

## Code Review Summary

詳細は `references/code-review.md`。
通常 code review は次を見る。

- Security
- Performance
- Quality
- Accessibility
- AI Residuals
- Spec Alignment
- Plans Alignment
- Regression Safety
- TDD compliance

root `spec.md` alignment、Plans lane/stage、TDD evidence、unknown data contract、evidence pack の詳細は `references/governance.md` と `references/code-review.md`。

`AI Residuals` 优先使用 `scripts/review-ai-residuals.sh` 和 `scripts/review-weak-supervision-report.sh`。
也要看 untracked 时用 `--include-untracked`。
`mockData`, `dummy`, `fake`, `localhost`, `TODO`, `FIXME`, `it.skip`, `test.skip`, `expect(true).toBe(true)` 等是候选，在 diff 上下文决定 severity。
finding 阶段优先网罗性。判定为 minor 的指摘也留在 `observations[]` / `recommendations[]`，gate 仅在 verdict 阶段进行（Opus 4.8 有筛选低严重度报告的倾向。参考 `references/code-review.md` 的 Finding coverage）。

## Quick / Codex Closeout Summary

詳細は `references/codex-closeout.md`。

轻量级路径原则：

- 先固定 target selection
- Codex 指摘作为 advisory 处理，在实代码确认后决定采否
- final report 包含 review command / tests / accepted findings / rejected findings / clean result
- stop-on-clean: clean result 后不做仅为样子的追加 review
- Codex 不可用时 fallback 到 full manual pass，不把失败当作成功

helper:

```bash
bash scripts/harness-review-closeout.sh --dry-run --uncommitted
bash scripts/harness-review-closeout.sh --base origin/main --parallel-tests --test "bash tests/test-harness-review-governance.sh"
bash scripts/harness-review-closeout.sh --commit HEAD
```

## Plan Review Summary

詳細は `references/plan-review.md`。
Plan Review 查看 `Plans.md` 的 DoD / Depends / Status 和实现顺序。
需要规格正本的任务没有 `spec_path` 时，作为 `decision_needed` 停止。

## Scope Review Summary

詳細は `references/scope-review.md`。
Scope Review 查看要求・差分・测试・docs 的边界是否膨胀。
需要范围变更时，不靠推测推进，回到 `AskUserQuestion` 或 plan 更新。

## Security / UI / Dual

- Security: `references/security-profile.md`
- UI rubric: `references/ui-rubric.md`
- high-res vision flow: `references/vision-high-res-flow.md`
- Dual review: `references/dual-review.md`

`/ultrareview` 在 Harness flow 中默认不调用。
因为不替换 Harness flow 的 review-result.v1、commit guard、sprint-contract 的连接。
`claude ultrareview [target] --json` 仅作为 CI / script 的 second-opinion 处理。

## PR Host Boundary

GitHub-first。
PR host 上の review 事実は GitHub を正とし、local diff は補助証拠として扱う。
ただし local uncommitted review は GitHub に push しない。

## Output Contract

User-facing prose 遵循显式的会话或项目语言。
如未配置语言，使用英语。仅在 `i18n.language: ja`、`CLAUDE_CODE_HARNESS_LANG=ja` 或显式会话指令要求日语输出时使用日语。
Machine-readable values 保持英语。

Start with the result summary.

~~~markdown
## Review Result

### {APPROVE | REQUEST_CHANGES | decision_needed} - {one-line conclusion}

Target: `{BASE_REF}..HEAD` or `{target}`
Verification: {commands run}

Strengths:
- ...

Findings:
- [severity] file:line - issue and evidence

Next Actions:
- ...

Details:
```json
{
  "schema_version": "review-result.v1",
  "verdict": "APPROVE | REQUEST_CHANGES",
  "decision_needed": {
    "required": false,
    "ask_tool": "AskUserQuestion"
  },
  "accepted_findings": [],
  "rejected_findings": [],
  "acceptance_bar": {
    "critical_major_zero": true,
    "spec_alignment": "pass | fail | not_applicable",
    "plans_alignment": "pass | fail | not_applicable",
    "regression_safety": "pass | fail | not_applicable",
    "verification_evidence": "pass | fail | not_applicable"
  },
  "team_debate": {
    "required": false,
    "mode": "native | codex-companion | manual-pass | unavailable",
    "team_agent_mode": "native | codex-companion | manual-pass | unavailable",
    "agents": [],
    "disagreements": []
  },
  "critical_issues": [],
  "major_issues": [],
  "observations": [],
  "recommendations": []
}
```
~~~

## Codex Environment

Codex 环境下可用工具不同，但合格线、规格正本、`Plans.md`、退化、修正后再审查、AskUserQuestion / `decision_needed.v1` 契约相同。

| 通常环境 | Codex fallback |
|---|---|
| Task tool 的 TeamAgent Debate | reviewer subagent / `codex-companion.sh review` / manual-pass |
| AskUserQuestion | 不可用时将 `decision_needed.v1` 输出到 stdout，不靠推测推进 |
| TaskList | 直接读 `Plans.md` |
## Related Skills

- `harness-work`: `REQUEST_CHANGES` 後の修正実行
- `harness-plan`: plan / scope / spec の更新
- `harness-release`: review 済み work の commit / release
