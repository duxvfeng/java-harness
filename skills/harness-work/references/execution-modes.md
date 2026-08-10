# Execution Modes

`harness-work` chooses the lightest execution mode that still preserves review
and validation.

## Shared Preflight

1. Read `Plans.md` and identify the selected task set.
2. Stop if the task table lacks `Task`, `DoD`, `Depends`, or `Status`.
3. Check whether a project spec SSOT exists when product behavior can drift.
   Prefer existing project-level docs, then `docs/spec/00-project-spec.md`.
4. If the task changes product behavior, API, data model, permissions, billing,
   integrations, or tenant boundaries and no stable spec exists, create or
   update the spec before implementation.
5. Skip spec creation only for mechanical work such as typo, formatting,
   dependency bump, docs-only, or behavior-preserving refactor tasks. Record
   the skip reason in the task context or sprint contract.
6. Resolve helper scripts through `HARNESS_PLUGIN_ROOT`, not the caller
   project's `scripts/` directory.
7. Mark only the selected task as `cc:WIP`.
8. Generate and approve a sprint contract before implementation when the task
   needs reviewable DoD checks.

## Solo

Use for one task. The parent session implements directly, validates, runs the
review loop, commits unless `--no-commit` is set, and marks `Plans.md`
`cc:完了 [hash]`.

## Parallel

Use for two or three independent tasks, or when `--parallel N` is explicit.
Workers may use isolated worktrees when file ownership can conflict. The Lead
still owns final integration and status updates.

## Codex

Use only when `--codex` is explicit. Delegate implementation to the Codex
companion entrypoint:

```bash
```

Validate the result locally. Codex output is not accepted until the normal
review loop approves it.

## Breezing

Use for four or more tasks, or when `--breezing` is explicit. Lead coordinates
Workers, Advisor, and Reviewer while preserving the implementation/review
boundary.

Codex-native Breezing reads this flow through `spawn_agent`, `send_input`,
`wait_agent`, and `close_agent` rather than Claude Code `Agent` /
`SendMessage` pseudo-code.

## Lane and Stage Contract

Sprint contract generation passes `spec_path`, `lane`, `stage`, and evidence
fields to Worker / Reviewer. See `skills/harness-work/SKILL.md`「Sprint
Contract」for the full field list.

### Stage gate（5 个阶段）

| stage | 目的 |
|-------|------|
| `research` | 现状调查·evidence 收集。未获取数据报告为 `unknown` |
| `plan` | 将 scope / DoD / lane freeze 到 Plans |
| `impl` | TDD Red→Green 实现。`[tdd:required]` 需要 `tdd_red_log` |
| `review` | 将 `review_artifact`（`APPROVE` / `REQUEST_CHANGES`）载入 contract |
| `closeout` | 载入 `pr_closeout`（`base_ref` / `head_ref` / evidence pack） |

### Lane: 轻量化项目 vs 必须保留项目

| lane | 可轻量化项目 | 不可省略项目 |
|------|-------------|-------------|
| `fast` | full review（可仅 major-only 或 advisory）、PR body 详情、release preflight | `spec_path`、unknown data contract（`not_observed != absent`）、focused checks（`runtime_validation` / `checks`）、`tdd_red_log` 或 `skip_tdd_reason`（`[tdd:required]` 时） |
| `gate` | —（无轻量化） | spec alignment、required 时的 TDD、major-only 或 full review、clean 为止的 re-review、`research_evidence` |
| `release` | —（无轻量化） | version/tag/GitHub Release/CI 验证、`pr_closeout` + release preflight、完整 evidence pack |

`[tdd:required]` 任务无论 lane 如何，只要 sprint contract 中没有 `tdd_red_log` 或显式 `skip_tdd_reason` 就不视为完成。

## Solo — Detailed Steps

1. Read `Plans.md`, identify the target task.
   - If `Plans.md` doesn't exist: auto-invoke `harness-plan create --ci` to generate it.
   - If the header lacks DoD / Depends columns: stop and ask for `harness-plan create` regeneration.
   - Extract undocumented requirements from recent conversation into a `cc:TODO` row (action-verb detection: 追加/修正/実装).
2. Task background confirmation (30s): infer the purpose from DoD, infer blast radius via `git grep`/`Glob`. Low confidence → ask one question; high confidence → proceed without delay.
3. **规格正本 preflight**：查找现有的项目规格 SSOT（`docs/spec/00-project-spec.md`、`docs/ARCHITECTURE.md`、`docs/HANDOFF.md`、`docs/oem/PROJECT_COMPASS.md`、`docs/specs/`）。如果任务变更产品行为/API/数据模型/权限/计费/集成/租户边界且没有规格，先创建 `docs/spec/00-project-spec.md`。如果规格过期或与任务矛盾，先更新。仅在 typo/format/dependency-bump/docs-only/behavior-preserving refactor 时跳过，记录跳过原因。将 `spec_path` 或 `spec_skip_reason` 传递到 Worker/Reviewer 上下文。
4. **Active scope + plan-time preapproval read**：preapproval 前，将 `{"phase":"<phase>","task":"<task>"}` 原子写入该任务 worktree 的 `.claude/state/active-task.json`；注册清理，在所有成功、失败或停止路径上删除它。如果存在则读取 `.claude/state/plan-preapprovals.json` 并用 `bash "${HARNESS_PLUGIN_ROOT}/scripts/plan-preapproval.sh" validate .claude/state/plan-preapprovals.json` 验证（写入架构: `templates/schemas/plan-preapproval.v2.json`；v1 仅读取兼容）。只有匹配此任务 `scope.phase`/`scope.task` 且 `decision: approved` 的条目才计为已声明。通过 `bash "${HARNESS_PLUGIN_ROOT}/scripts/plan-preapproval.sh" apply-secret-allow "$PROJECT_ROOT"` 应用已声明的 `secret-read`（在项目配置中写入 `runtimefloor.secretAllow`）。R12 可消费匹配的、未过期的、使用受限制的 v2 `external-send` 批准。它从不压制显式 R12 拒绝或任何 runtime-floor 类别。将已声明的 external-send/destructive 项目作为"plan approved"传递给 worker 简报/sprint-contract。确认仅在 plan 批准时 1 次。工作起因于已声明项目的 `AskUserQuestion` 为零。未声明的项目仍在 runtime floor / ask 停止 — 从不默默地将其加入白名单。
5. Mark the task `cc:WIP`; declare presence with `bin/harness session declare --task <task-id>` (clear with `--clear` on completion).
6. TDD phase (unless `[skip:tdd]` or no test framework): write the failing test first (Red), confirm the failure, log it with `bash "${HARNESS_PLUGIN_ROOT}/scripts/log-tdd-red.sh"` into `.claude/state/tdd-red-log/<task-id>.jsonl` (or attach literal failing output to the `worker-report`'s `self_review` evidence if the script is unavailable). `--tdd-bypass` requires `HARNESS_TDD_BYPASS=1` and `HARNESS_TDD_BYPASS_REASON="<reason>"` recorded in the sprint contract.
7. Generate `sprint-contract.json` with `node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" <task-id>`.
8. Enrich with reviewer perspective (`enrich-sprint-contract.sh`) and confirm approval (`ensure-sprint-contract-ready.sh`).
9. Advisor consult (only when needed): a high-risk task (`needs-spike`/`security-sensitive`/`state-migration`) gets one consult before the first attempt; the same failure cause twice in a row triggers a consult before the third attempt; a plateau `PIVOT_REQUIRED` verdict triggers one consult before escalating. The response is `advisor-response.v1`: `PLAN` reshapes the approach, `CORRECTION` is a local fix, `STOP` escalates immediately. The same `trigger_hash` is consulted at most once; max 3 consults per task.
10. Implement via the backend-resolved executor path (Green).
11. `/simplify` for Auto-Refinement (skip with `--no-simplify`).
12. Run the automatic review stage — see [review-loop.md](review-loop.md). When `sprint-contract.json`'s `reviewer_profile` is `runtime`, also run `bash "${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh"`.
13. Normalize the review artifact with `bash "${HARNESS_PLUGIN_ROOT}/scripts/write-review-result.sh"` (pass `--browser-result` for the browser profile; `browser_verdict == PENDING_BROWSER` keeps the static verdict).
14. `git commit` (skip with `--no-commit`).
15. Mark `cc:完了 [hash]`, clear the presence declaration, record the short commit hash.
16. Render the rich completion report — see the `Completion Report Output Contract` in the main `SKILL.md` and [completion-report.md](completion-report.md).
17. On test/CI failure after completion, see [failure-reticketing.md](failure-reticketing.md).

## Breezing — Phase Detail

```
Lead (this agent)
├── Worker (task-worker agent) — implementation
├── Advisor (claude-code-harness:advisor) — guidance
└── Reviewer (code-reviewer agent) — review
```

### Phase A — Pre-delegate

1. Read `Plans.md`, identify target tasks, and resolve execution order from the `Depends` column.
2. For each task, atomically write its phase/task to the task worktree's `.claude/state/active-task.json` before preapproval. Remove it on every task exit path.
3. Read `.claude/state/plan-preapprovals.json` if present; validate v2 or read-compatible v1 with `plan-preapproval.sh validate`.
4. Pass this task's `decision: approved` items to the worker briefing; apply declared `secret-read` via `plan-preapproval.sh apply-secret-allow "$PROJECT_ROOT"`. R12 may consume only a matching v2 `external-send` approval; explicit deny and runtime floor remain unchanged. Do not stop mid-work or ask for declared items; undeclared secret-read/external-send/destructive operations still stop on runtime floor / ask.
5. Score each task's effort tier — see [effort-routing.md](effort-routing.md).
6. Generate and enrich `sprint-contract.json` for each task (same scripts as Solo); stop if `ensure-sprint-contract-ready.sh` reports not-ready.

### Phase B — Delegate (per task, sequential in dependency order)

> API note: the pseudo-code below uses Claude Code syntax. On Codex, read `Agent(...)` as `spawn_agent(...)` and `SendMessage(...)` as `send_input(...)` — see `team-composition.md`'s API mapping table.

```
for task in execution_order:
    contract_path = generate + enrich + ensure-ready sprint-contract for task.number

    Plans.md: task.status = "cc:WIP"
    worker_result = Agent(
        subagent_type="claude-code-harness:worker",
        prompt=briefing_header + "任务: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\nmode: breezing",
        isolation="worktree",
        run_in_background=false
    )
    worker_id = worker_result.agentId

    if worker_result.type == "advisor-request.v1":
        advisor_result = Advisor(prompt=worker_result.request_json)
        worker_result = SendMessage(to=worker_id, message="advisor-response.v1: {advisor_result}")

    # self_review gate (Lead checks mechanically before spawning Reviewer)
    self_review_failures = 0
    MAX_SELF_REVIEW_RETRIES = 2  # a 3rd failure escalates
    while True:
        unverified = [r for r in worker_result.self_review if not r.get("verified") or not r.get("evidence")]
        if not unverified:
            break
        self_review_failures += 1
        if self_review_failures > MAX_SELF_REVIEW_RETRIES:
            Plans.md: task.status = "cc:TODO"
            raise EscalationError(f"self_review unresolved after 3 return trips: {[u['rule'] for u in unverified]}")
        SendMessage(to=worker_id, message=f"self_review 中有未确认 rule: {[u['rule'] for u in unverified]}。请用实际命令输出或 literal 测试结果填充 evidence，verified=true 后 amend")
        worker_result = wait_for_response(worker_id)

    # review (see review-loop.md for verdict priority/thresholds)
    diff_text = git("-C", worker_result.worktreePath, "show", worker_result.commit)
    verdict = codex_exec_review(diff_text) or reviewer_agent_review(diff_text)
    profile = jq(contract_path, ".review.reviewer_profile")
    if profile == "runtime":
        review_input = run-contract-review-checks.sh output; DOWNGRADE_TO_STATIC falls back to the static verdict
    if profile == "browser":
        browser artifact -> browser-review-runner.sh; REQUEST_CHANGES/APPROVE override the static verdict, PENDING_BROWSER keeps it
    write-review-result.sh normalizes the artifact

    review_count = 0
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3
    latest_commit = worker_result.commit
    while verdict == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
        SendMessage(to=worker_id, message="指摘内容: {issues}\n修正して amend してください")
        updated_result = wait_for_response(worker_id)
        latest_commit = updated_result.commit
        verdict = codex_exec_review(...) or reviewer_agent_review(...)
        review_count++

    if verdict == "APPROVE":
        cherry-pick latest_commit onto trunk (如果已是祖先则跳过 — 重入保护)
        remove the worker's worktree; delete the feature branch
        Plans.md: task.status = "cc:完了 [{hash}]"
        bash "${HARNESS_PLUGIN_ROOT}/scripts/auto-checkpoint.sh" "${task.number}" "${HASH}" "${contract_path}" "${REVIEW_RESULT_PATH}" || true  # fail-open
    else:
        escalate to the user

    print("📊 Progress: Task {completed}/{total} 完成 — {task.内容}")
```

### Phase C — Post-delegate

1. Aggregate the commit log for all tasks.
2. Render the rich completion report (Breezing template in [completion-report.md](completion-report.md)).
3. Confirm every task in `Plans.md` reached `cc:完了`.
