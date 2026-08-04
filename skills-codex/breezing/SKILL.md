---
name: breezing
description: "Team execution mode (Codex host) — backward-compatible alias for harness-work with backend selection, including opt-in Cursor worker delegation. Composer/composer 2.5 maps to the cursor backend."
description-en: "Team execution mode (Codex host) — backward-compatible alias for harness-work with backend selection, including opt-in Cursor worker delegation. Composer/composer 2.5 maps to the cursor backend."
description-ja: "团队执行模式（Codex 宿主版）— harness-work 的团队协调别名。可从 Codex 选择性委托 Cursor worker 后端。当用户提到 breezing、团队执行、全部完成、composer、作曲器、composer 2.5 时触发。"
description-zh: "团队执行模式（Codex 宿主版）— harness-work 的团队协调别名。可从 Codex 选择性委托 Cursor worker 后端。当用户提到 breezing、团队执行、全部完成、composer、作曲器、composer 2.5 时触发。"
kind: workflow
purpose: "Wrap harness-work with Codex-host team execution orchestration"
trigger: "breezing, team execution, do everything, cursor worker, composer, composer 2.5, composer mode, コンポーザー"
shape: wrap
role: orchestrator
base: harness-work
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Bash", "spawn_agent", "send_input", "wait_agent", "close_agent"]
argument-hint: "[all|N-M|--backend claude|codex|cursor|--cursor|--codex|--max-workers N|--no-discuss]"
user-invocable: true
effort: high
---

# Breezing — Team Execution Mode (Codex Host)

> **本 SKILL.md 是 Codex 宿主版。**
> Claude Code 版请参考 `skills/breezing/SKILL.md`。
> 后端通过 resolver 选择。分发插件的无标志默认保持 `claude` 兼容。
> 在设置了 `--cursor` / `--backend cursor` 或 `HARNESS_IMPL_BACKEND=cursor` 的环境中使用 Cursor worker 后端。
> frontmatter 中的 `allowed-tools` 也配合这 4 个 Codex 原生工具名称。

**向后兼容别名**: 以团队执行模式运行 `harness-work --breezing`。

## Default Pipeline（plan → work → review → report 一键完成）

与 Claude Code 版相同的契约（operator 裁定 2026-07-24。正本: `skills/breezing/SKILL.md` 的同名节）:

1. **Plan gate**: 如果请求范围的 task 不在 Plans.md 中/不足，先执行 `harness-plan` 后继续（范围默认是"当前可进行的所有工作"）
2. **Work**: 现有的团队执行流程（包括 per-task review）
3. **Integrated Review Gate（默认开启）**: 实现完成后、**最终化（完成报告・run 完成声明）之前**，对 run 全体 diff 执行 `harness-review`。review target 通常是 `{base_ref}..HEAD`，`--no-commit` run 则是 working tree（未提交变更 + untracked）。让 fresh-context 独立 reviewer 与 cross-CLI second opinion 并行，直到 APPROVE 为止反复修正 → 再审查（最多 3 次。未收敛时将相关 task 回退到 `cc:WIP` 并人工升级）
4. **Finalize + Report**: gate APPROVE 后确定 Plans.md 更新・完成报告。最终报告使用 easy 作法（宿主机有 `easy` skill 就用其作法，否则使用 Completion Report 模板）

若要在低风险的快速 run 中省略步骤 3，使用 `--no-review-gate`（保持 per-task review，仅跳过集成审查）。

## Quick Reference

```bash
breezing                        # 询问范围后执行
breezing all                    # 使用 resolved backend 完成所有 ready task（分发默认是 claude，当前环境可通过用户配置设为 cursor）
breezing 3-6                    # 使用 resolved backend 完成任务 3〜6
breezing composer 2.5 all       # 自然语言触发: 作为 cursor backend 处理
breezing --backend cursor all    # 明确指定 Cursor worker backend
breezing --backend claude all    # 明确指定 Codex native spawn_agent worker
breezing --codex all             # 明确指定 Codex CLI worker backend
breezing --cursor all            # 明确指定 Cursor worker backend
breezing --max-workers 2 all     # 将 ready task 的同时 spawn 上限设为 2
breezing --max-workers 1 all     # 回到传统的串行行为
breezing --no-discuss all       # 跳过计划讨论，完成所有任务
```

## Options

| Option | Description | Default |
|--------|-------------|---------|
| `all` | 所有未完成任务为目标 | - |
| `N` or `N-M` | 任务编号/范围指定 | - |
| `--backend <claude\|codex\|cursor>` | 明确选择 worker backend | resolver result（分发默认是 claude） |
| `--cursor` | `--backend cursor` 的别名 | false |
| `--codex` | `--backend codex` 的别名 | false |
| `--max-workers N` | ready task 的同时 spawn 数上限（breezing 固有选项）。`1` 时回到传统串行行为 | max |
| `--no-commit` | 不支持（Breezing 中 Worker 的临时 commit 和 Lead 的 cherry-pick 是必需的） | - |
| `--no-discuss` | 跳过计划讨论 | false |

## Execution

**此技能委托给 `harness-work --breezing`**。请使用以下配置执行：

1. **将参数传递给 `harness-work --breezing`**（`--max-workers N` 作为 breezing 固有选项解释，与 `harness-work` 的 `--parallel` 是不同概念）
2. **强制团队执行模式** — Lead → Worker spawn → 必要时 Advisor → companion review Reviewer 的四者分离
3. **Lead 专注于 delegate** — 不直接编写代码

### Execution Backend (persistent)

后端选择（worker 由 `claude` / `codex` / `cursor` 哪个来实现）的正本请参考
`harness-work` 的"Execution Backend Selection（实现后端选择）"。
那里定义了 precedence、role-scope（review / advisor 固定为 Opus）、self_review 跳过、cursor banner。
后端判定**必须**通过 resolver，不要直接读取 `HARNESS_IMPL_BACKEND` env。

Codex Breezing 在分发插件中也不改变 call-site default:

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh"
```

precedence 是 `--backend` / `--cursor` / `--codex` > `HARNESS_IMPL_BACKEND` env > project `env.local` >
user `~/.config/claude-harness/impl-backend.env` > call-site default `claude`。
也就是说分发插件的无标志 `breezing all` 为了兼容性保持 `claude`。
只有在此环境那样 user/project config 设置了 `HARNESS_IMPL_BACKEND=cursor` 的情况下，
无标志才会成为 Cursor worker backend。要明确使用 Codex native subagent worker 时指定 `--backend claude`。

`composer` / `コンポーザー` / `Composer で` / `composer 2.5` / `composer モード` 正式作为 `cursor backend` 的 trigger 处理。
这是相当于 `--cursor` 的 intent，Lead 通过 `resolve-impl-backend.sh` 确定 backend。
解决时作为明确 override 传递 `--backend cursor`，优先于 env / project / user file / default。
`composer` 不是在 Codex native Worker 内部 spawn 的附加 agent，而是按照非 `claude` backend 的规约，Lead 直接调用 `cursor-companion.sh`。

默认 worker 数是 **max**。
这里的 max 意味着"在目标范围内满足 Depends、当前可执行的 ready task 的最大数"。
并非无限制地 spawn Worker。
等待依赖的 task 在前段 task 完成变成 ready 之前不会 spawn。
要回到传统的一个个进行的串行行为时指定 `--max-workers 1`。

Worker 的实现可以并行化，但 review 和对 main 的 cherry-pick 是串行进行的。
这是为了避免对同一个 main worktree 的写入冲突。

### 与 `harness-work` 的不同

| 特征 | `harness-work` | `breezing` (此技能) |
|------|-----------------|------------------------|
| 默认模式 | Solo / Sequential | **Breezing（团队执行）** |
| 并行手段 | companion `task` Bash 并行 | **`spawn_agent` 的子代理委托** |
| Lead 的角色 | 协调+实现 | **delegate (专注协调)** |
| 审查 | Lead 自我审查 | **companion review 独立审查** |
| 默认范围 | 下一个任务 | **全部** |

### Team Composition（Codex Native）

| Role | 执行方式 | 权限 | 职责 |
|------|---------|------|------|
| Lead | (self) | 继承当前会话 | 协调・指挥・任务分配・cherry-pick |
| Worker ×N | resolver result: `spawn_agent` / `codex-companion.sh` / `cursor-companion.sh task --write --workspace <worktree>` | 继承会话权限 | 实现（git worktree 分离） |
| Advisor | `claude-code-harness:advisor` | 只读 | 方针建议 (`PLAN` / `CORRECTION` / `STOP`) |
| Reviewer | companion `review --base` | read-only | 独立审查 |

## Flow Summary

```
breezing [scope] [--backend claude|codex|cursor] [--max-workers N] [--no-discuss]
    │
    ↓ Load harness-work --breezing
    │
Phase 0: Planning Discussion (--no-discuss 时跳过)
Phase A: Pre-delegate（团队初始化 + worktree 准备）
Phase B: Delegate（resolver-selected worker + 必要时 Advisor + companion review 审查）
Phase C: Post-delegate（集成验证 + Plans.md 更新 + commit）
```

## Advisor Protocol

Worker 不增加通用的 subagent。
迷路时只返回结构化 JSON 的咨询请求，Lead 调用 advisor。

1. Worker → `advisor-request.v1`
2. Lead → Advisor
3. Advisor → `advisor-response.v1`
4. Lead → 向同一个 Worker 返回 advice 继续
5. Reviewer 只看最后的成果物

咨询条件与 loop / solo 保持一致。

- 高风险 task（`needs-spike` / `security-sensitive` / `state-migration`）的首次执行前
- 同样原因失败连续 2 次后
- 因 plateau 返回 `PIVOT_REQUIRED` 前
- 同一个 `trigger_hash` 只 1 次。每个 task 的咨询次数最多 3 次

## Realtime Handoff / Silence Policy

Codex `0.123.0` 以后，background agent 可以接收 realtime handoff 的 transcript delta。
Breezing 将此机制作为"不是增加多余通知的入口，而是仅在必要时更新判断的输入"处理。

一句话来说：Worker / Advisor / Reviewer 对状态不变的 transcript delta 不反应，对 Lead 的报告仅限于 material state change。

比喻来说，不是在多人的作业房间所有人都自言自语地实况转播，而是只在负责的作业完成时、卡住时、等待判断时才出声的形式。

报告内容：

- Worker 的完成 JSON、blocked 理由、必要的 `advisor-request.v1`
- Advisor 的 `PLAN` / `CORRECTION` / `STOP`
- Reviewer 的 `APPROVE` / `REQUEST_CHANGES`
- validation failure、contract readiness failure、plateau、drift 检测
- Lead 输出的 task 完成单位的 progress feed

可以沉默的内容：

- 仅接收了 transcript delta、task status、review verdict、advisor decision 没有变化的情况
- tool stdout 的细微增量，仅留在 job log 中就足够的内容
- parallel spawn 中的等待 heartbeat。等待交给 `wait_agent` / job status

中途报告的频率：

- Lead 的 progress feed 基本上每个 task 完成 1 次。
- Worker / Reviewer 只返回"完成・回退・阻塞"的结果，避免每个 delta 的小报告。
- 只在用户明确请求 status 时，Lead 总结返回当前位置。

Advisor / Reviewer drift 的关系：

- silence policy 不是让 Advisor / Reviewer 沉默的免责。
- 发送 `advisor-request.v1` 后没有返回 response、reviewer profile 中缺少必要的 result、review loop plateau 的情况作为 drift 处理。
- Advisor 保持方针建议、Reviewer 保持质量判定的角色分离，沉默仅用于"不发出不必要通知"。

### Phase 0: Planning Discussion（结构化 3 问检查）

在执行所有任务之前，用以下 3 问确认计划的健全性。
指定 `--no-discuss` 时全部跳过。

**Q1. 范围确认**:
> 「将执行 {{N}} 个任务。范围合适吗？」

**Q2. 依赖关系确认**（仅当 Plans.md 有 Depends 栏时）:
> 「任务 {{X}} 依赖于 {{Y}}。执行顺序正确吗？」

**Q3. 风险标志**（仅当有 `[needs-spike]` 任务时）:
> 「任务 {{Z}} 是 [needs-spike]。要先做 spike 吗？」

### Phase A: Pre-delegate

1. 读取 Plans.md，确定目标任务
2. 分析依赖图，决定执行顺序
3. 为每个任务创建 git worktree

### Phase B: Delegate（Codex Host 编排）

- If you grep the same symbol twice in the same session, switch to harness_ast_search.
- For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
- Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.

```
for task in execution_order:
    # B-0. 作业目录分离
    worktree_path = "/tmp/worker-{task.number}-$$"
    branch_name = "worker-{task.number}-$$"
    git worktree add -b {branch_name} {worktree_path}
    TASK_BASE_REF = git rev-parse HEAD

    # B-1. 生成 sprint-contract
    contract_path = bash("node \"${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js\" {task.number}")
    contract_path = bash("scripts/enrich-sprint-contract.sh {contract_path} --check \"从 reviewer 角度确认 DoD\" --approve")
    bash("scripts/ensure-sprint-contract-ready.sh {contract_path}")

    # B-2. Worker 委托
    Plans.md: task.status = "cc:WIP"

    resolver_backend_arg = ""
    if explicit_backend_value in ["claude", "codex", "cursor"]:
        resolver_backend_arg = "--backend {explicit_backend_value}"
    backend = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh\" {resolver_backend_arg}")
    if explicit_flag == "--cursor":
        backend = "cursor"
    if explicit_flag == "--codex":
        backend = "codex"

    if backend == "cursor":
        print("🚀 cursor / $(bash \"${HARNESS_PLUGIN_ROOT}/scripts/model-routing.sh\" --host cursor --role worker --field model) / {branch_name} / {task.ID}")
        companion_prompt = "{task prompt}\n\n完成更改后，在此 worktree 中创建恰好一个 git commit 然后返回。"
        companion_output = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh\" task --write --workspace {worktree_path} \"{companion_prompt}\"")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if git("-C", worktree_path, "status", "--porcelain") != "":
            git("-C", worktree_path, "add", "-A")
            git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: breezing delegated change")
            latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if latest_commit == TASK_BASE_REF:
            raise EscalationError("cursor companion produced no commit")
        worker_result = {type: "companion-result.v1", baseCommit: TASK_BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: branch_name, files_changed: git("-C", worktree_path, "diff", "--name-only", "{TASK_BASE_REF}..HEAD"), summary: companion_output}
        worker_id = null
    elif backend == "codex":
        companion_prompt = "{task prompt}\n\n完成更改后，在此 worktree 中创建恰好一个 git commit 然后返回。"
        companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
        companion_output = bash("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE={companion_state_file} bash \"${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh\" task --write -C {worktree_path} \"{companion_prompt}\"")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if latest_commit == TASK_BASE_REF:
            raise EscalationError("codex companion produced no commit")
        worker_result = {type: "companion-result.v1", baseCommit: TASK_BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: branch_name, files_changed: git("-C", worktree_path, "diff", "--name-only", "{TASK_BASE_REF}..HEAD"), summary: companion_output}
        worker_id = null
    else:
        print("🚀 claude / native-subagent / {branch_name} / {task.ID}")
        worker_id = spawn_agent({
            message: "请在作业目录: {worktree_path} 工作。\n\n任务: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\n\n请实现。完成后请 git commit。\n\n完成时请返回以下 JSON:\n{\"commit\": \"<hash>\", \"files_changed\": [...], \"summary\": \"...\"}",
            fork_context: true
        })
        worker_result = wait_agent({ targets: [worker_id] })

    # B-3. 仅当 Worker 返回 advice request 时，Lead 调用 Advisor
    if backend == "claude" and worker_result.type == "advisor-request.v1":
        advisor_id = spawn_agent({
            agent_type: "default",
            message: worker_result.request_json,
            fork_context: true
        })
        advisor_result = wait_agent({ targets: [advisor_id] })
        close_agent({ target: advisor_id })
        send_input({
            target: worker_id,
            message: "advisor-response.v1: {advisor_result}"
        })
        worker_result = wait_agent({ targets: [worker_id] })

    # B-4. Lead 执行审查（以 TASK_BASE_REF 为起点）
    # 使用官方插件 companion review（参考 harness-work 的"审查循环"）:
    #   bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" review --base {TASK_BASE_REF}
    #   → verdict 映射: approve→APPROVE, needs-attention→REQUEST_CHANGES
    VERDICT = review_task(worktree_path, TASK_BASE_REF)  # static review（参考 harness-work）
    PROFILE = jq(contract_path, ".review.reviewer_profile")
    BROWSER_MODE = jq(contract_path, ".review.browser_mode // \"scripted\"")
    REVIEW_INPUT = "review-output.json"
    if PROFILE == "runtime":
        # 在 worktree 内执行 runtime checks
        REVIEW_INPUT = bash("cd {worktree_path} && scripts/run-contract-review-checks.sh {contract_path}")
        RUNTIME_VERDICT = jq(REVIEW_INPUT, ".verdict")
        if RUNTIME_VERDICT == "REQUEST_CHANGES":
            VERDICT = "REQUEST_CHANGES"
        elif RUNTIME_VERDICT == "DOWNGRADE_TO_STATIC":
            REVIEW_INPUT = "review-output.json"  # 回退到 static review
    if PROFILE == "browser":
        # browser artifact 是 PENDING_BROWSER scaffold。reviewer agent 在后续执行。
        BROWSER_ARTIFACT = bash("scripts/generate-browser-review-artifact.sh {contract_path}")
        # REVIEW_INPUT 保持 static review
    if REVIEW_INPUT != "review-output.json" and jq(REVIEW_INPUT, ".verdict") == "DOWNGRADE_TO_STATIC":
        REVIEW_INPUT = "review-output.json"
    bash("scripts/write-review-result.sh {REVIEW_INPUT} {commit_hash}")

    # B-5. 修正循环（REQUEST_CHANGES 时，直到 contract 的 max_iterations）
    review_count = 0
    # 只在 sprint-contract 存在时读取 max_iterations。不存在则为 3（向后兼容）
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3
    while VERDICT == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
        if backend == "claude":
            send_input({
                target: worker_id,
                message: "指出内容: {issues}\n请修正并 git commit --amend。修正后再次输出 JSON。"
            })
            wait_agent({ targets: [worker_id] })
        elif backend == "cursor":
            previous_commit = git("-C", worktree_path, "rev-parse", "HEAD")
            bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh\" task --write --workspace {worktree_path} \"Review findings:\n{issues}\n\n修正 findings 并在返回前创建一个新的 git commit。\"")
            latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
            if git("-C", worktree_path, "status", "--porcelain") != "":
                git("-C", worktree_path, "add", "-A")
                git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: breezing review fix")
                latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
            if latest_commit == previous_commit:
                raise EscalationError("cursor companion retry produced no new commit")
        else:
            previous_commit = git("-C", worktree_path, "rev-parse", "HEAD")
            companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
            bash("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE={companion_state_file} bash \"${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh\" task --write -C {worktree_path} \"Review findings:\n{issues}\n\n修正 findings 并在返回前 commit 结果。\"")
            latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
            if latest_commit == previous_commit:
                raise EscalationError("codex companion retry produced no new commit")
        VERDICT = review_task(worktree_path, TASK_BASE_REF)
        review_count++

    # B-6. Worker 结束
    if backend == "claude":
        close_agent({ target: worker_id })

    # B-7. 结果处理
    if VERDICT == "APPROVE":
        commit_hash = git("-C", worktree_path, "rev-parse", "HEAD")
        git cherry-pick --no-commit {TASK_BASE_REF}..{commit_hash}
        git commit -m "{task.内容}"
        Plans.md: task.status = "cc:完结 [{short_hash}]"
    else:
        → 向用户升级（Plans.md 保持 cc:WIP）
        → 后续任务也停止

    # B-8. Worktree 清理
    git worktree remove {worktree_path}
    git branch -D {branch_name}

    # B-9. Progress feed
    print("📊 Progress: Task {completed}/{total} 完成 — {task.内容}")
```

### ready task 的并行 spawn（默认 max / `--max-workers N` 指定时）

当有多个满足 Depends 的 ready task 时，默认同时 spawn 到 ready task 的数量。
指定 `--max-workers N` 时，将同时 spawn 数限制到 N 件。
`--max-workers 1` 是回到传统串行行为的 escape hatch。

> **`wait_agent` 的语义**: `wait_agent({targets: [a, b]})` 返回第一个完成的（不是等待全部完成）。
> 因此，要等待所有 Worker 的完成需要循环分别调用 `wait_agent`。

```
# 并行 spawn 独立任务 A, B（各自已分离 worktree）
worker_a = spawn_agent({ message: "作业目录: /tmp/worker-a-$$ ...", fork_context: true })
worker_b = spawn_agent({ message: "作业目录: /tmp/worker-b-$$ ...", fork_context: true })

# 分别等待每个 Worker 的完成 → 审查 → cherry-pick（串行）
# wait_agent 返回第一个，所以其余 Worker 还在运行中
for worker_id in [worker_a, worker_b]:
    wait_agent({ targets: [worker_id] })    # 等待此 Worker 的完成
    VERDICT = review_task(worktree_path, TASK_BASE_REF)  # 参考 harness-work
    # 修正循环（如需要）...
    close_agent({ target: worker_id })
    if VERDICT == "APPROVE":
        cherry-pick → Plans.md 更新
```

> **约束**: 只能并行化满足 Depends 的 ready task。
> max 是 ready task 数的上限，不是无限制 spawn。
> 审查 → cherry-pick 串行执行（因为对 main 的写入会冲突）。

### Worker 的输出契约

在 Worker 提示中，明确完成时返回以下 JSON：

```json
{
  "commit": "a1b2c3d",
  "files_changed": ["src/foo.ts", "tests/foo.test.ts"],
  "summary": "向 foo 模块添加 bar 功能"
}
```

Lead 解析此 JSON 获取 commit hash 和文件列表。

### Progress Feed（Phase B 中的进度通知）

```
📊 Progress: Task 1/5 完成 — "向 harness-work 添加失败再票决化"
📊 Progress: Task 2/5 完成 — "向 harness-sync 添加 --snapshot"
```

### 完成报告（Phase C）

全部任务完成后，Lead 通过以下步骤生成丰富的完成报告：

1. `git log --oneline {session_base_ref}..HEAD` 收集所有 cherry-pick commit
2. `git diff --stat {session_base_ref}..HEAD` 获取全体变更规模
3. 从 Plans.md 提取剩余任务
4. 按照 Breezing 模板输出

## 与 Claude Code 版的差异

| 项目 | Claude Code 版 | Codex 原生版（本文件） |
|------|---------------|-------------------------------|
| Worker spawn | Claude Code Agent tool + worktree isolation | resolver result: `spawn_agent`, `codex-companion.sh`, or `cursor-companion.sh` + `git worktree add` |
| 完成等待 | `Agent` 的返回值 | `wait_agent({targets: [id]})` |
| 修正指示 | Claude Code message tool | `send_input({target, message})` |
| Worker 结束 | 自动 | `close_agent({target})` |
| 审查 | Codex exec → Reviewer agent fallback | companion `review --base`（结构化输出） |
| 权限 | `bypassPermissions` + hooks | companion `task --write` / `spawn_agent`: 继承会话权限 |
| Agent Teams | `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS` 环境变量 | Codex native（标准功能） |
| Worktree | `isolation="worktree"` 自动管理 | `git worktree add/remove` 手动管理 |
| 模式升级 | 4 个任务以上自动 | 仅 `--breezing` 明确时 |

## Related Skills

- `harness-work` — 从单个任务到团队执行（本体）
- `harness-sync` — 进度同步
- `harness-review` — 代码审查
