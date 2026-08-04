---
name: harness-work
description: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-en: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-ja: "HAR：负责从单个任务到全并行团队执行的Plans.md任务。实现、执行、全部完成、breezing、团队执行、并行、composer、作曲器、composer 2.5时启动。不用于：计划、审查、发布、设置。"
description-zh: "HAR：负责从单个任务到全并行团队执行的 Plans.md 任务。当用户提到实现、执行、全部完成、breezing、团队执行、并行、composer、作曲器、composer 2.5 时启动。不适用于：计划、审查、发布、设置。"
kind: workflow
purpose: "Execute Plans.md tasks end to end through Codex-native tools"
trigger: "implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5, composer mode, コンポーザー"
shape: workflow
role: executor
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Grep", "Glob", "Bash", "spawn_agent", "send_input", "wait_agent", "close_agent"]
argument-hint: "[all] [task-number|range] [--backend claude|codex|cursor] [--cursor] [--codex] [--parallel N] [--no-commit] [--resume id] [--breezing] [--auto-mode] [--tdd-bypass]"
effort: high
---

# Harness Work

Harness 的综合执行技能。
统合以下旧技能:

- `work` — Plans.md 任务实现（范围自动判断）
- `impl` — 功能实现（任务基础）
- `breezing` — 团队全自动执行
- `parallel-workflows` — 并行工作流优化
- `ci` — CI 失败时恢复

## Quick Reference

| 用户输入 | 模式 | 动作 |
|------------|--------|------|
| `harness-work` | **auto** | 按任务数自动判定（参考下述） |
| `harness-work all` | **auto** | 以自动模式执行所有未完成任务 |
| `harness-work 3` | solo | 仅执行任务 3 |
| `harness-work --parallel 5` | parallel | 以 5 worker 并行执行（强制） |
| `harness-work --codex` | codex | 委托给 Codex CLI（仅明确时） |
| `harness-work --breezing all` | breezing | 以 resolved backend 团队执行（分发默认是 claude，user/project default 可是 cursor） |
| `harness-work --breezing --backend cursor all` | breezing | 明确指定 Cursor worker backend 团队执行 |
| `harness-work --breezing --backend claude all` | breezing | 明确指定 Codex native subagent worker 团队执行 |
| `harness-work --breezing` | breezing | 强制团队执行 |
| `harness-work 3 --plan roadmap` | solo | 从 named Plans 的 `roadmap` 执行任务 3 |

## Execution Mode Auto Selection（无标志时的自动判定）

没有明确模式标志（`--parallel`, `--breezing`, `--codex`）时，
根据目标任务数自动选择最佳模式:

| 目标任务数 | 自动选择模式 | 理由 |
|-------------|---------------|------|
| **1 件** | Solo | overhead 最小。直接实现最快 |
| **2〜3 件** | Parallel（Task tool） | Worker 分离的好处开始出现的阈值 |
| **4 件以上** | Breezing | Lead 协调 + Worker 并行 + Reviewer 独立的三者分离有效 |

### 规则

1. **明确标志总是覆盖自动模式**
   - `--parallel N` → Parallel 模式（与任务数无关）
   - `--breezing` → Breezing 模式（与任务数无关）
   - `--codex` → Codex 模式（与任务数无关）
2. **`--codex` 仅在明确时启动**。因为有 Codex CLI 未安装的环境，不自动选择
3. `--codex` 可与其他模式组合: `--codex --breezing` → Codex + Breezing

## Execution Backend Selection（实现后端选择）

后端（哪个运行时**来实现**）与拓扑（执行模式: solo / parallel / breezing）正交。
拓扑决定"用几个 worker・如何分割运行"，后端决定"谁来动实现的手"。
此契约是 host-neutral（spec.md "Execution Backend Contract"），从 Codex host 驱动 harness 还是从 Claude Code 驱动都表现相同。

| backend | 实现承担者 | 委托命令 |
|---------|------------|------------|
| `claude`（global fallback） | Codex native subagent（`spawn_agent({message, fork_context})`） | 用 spawn_agent spawn worker |
| `codex` | Codex CLI | `bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write "<prompt>"` |
| `cursor` | cursor-agent（model `composer-2.5-fast`） | `bash "${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh" task --write --workspace <worktree> "<prompt>"` |

### 解决步骤

run 开始时只解决 1 次。后端判定**必须**通过 resolver，不要仅通过直接读取 `HARNESS_IMPL_BACKEND` env 判定:

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh"
```

precedence（从高到低）: `--backend <v>` / `--cursor` / `--codex` 标志 > `HARNESS_IMPL_BACKEND` 环境变量 > 项目 `env.local` 的同名行 > 用户 `~/.config/claude-harness/impl-backend.env` 的同名行 > call-site default。
明确标志（`--backend` / `--cursor` / `--codex`）总是覆盖 env / file / default。项目设置覆盖用户范围。

Codex host 的 `--breezing` / `breezing` 在分发插件中也不改变 call-site default。
无标志时遵循 `resolve-impl-backend.sh` 的结果，未设置时的 fallback 是 `claude`。
想将 Cursor 作为默认的环境，在 env / project `env.local` / user-scope config 设置 `HARNESS_IMPL_BACKEND=cursor`。
明确使用 Cursor 时传递 `--backend cursor` / `--cursor`，要回到 Codex native subagent worker 时传递 `--backend claude`。

> 模型名的正本是 `model-routing.sh`。本文档中的 `composer-2.5-fast` 是参照值，实际解决遵循 `bash "${HARNESS_PLUGIN_ROOT}/scripts/model-routing.sh" --host cursor --role worker --field model`（防止 drift）。

### 自然语言后端 trigger

用户说 `composer` / `コンポーザー` / `Composer で` / `composer 2.5` / `composer モード` 时，作为 `cursor backend` 指定处理。
这与 `--cursor` 是相同的 intent，但后端的确定值一定通过 `resolve-impl-backend.sh` 解决。
解决时作为明确 override 传递 `--backend cursor`，优先于 env / project / user file / default。
Lead 不将 `composer` 解释为 Codex native Worker 内的附加 agent，而是按照非 `claude` 后端的规约不挟 Worker agent 直接调用 `cursor-companion.sh`。

### role-scoped 约束

后端是 **role-scoped**。使用已解决后端的只是实现（worker）角色。
Reviewer 和 Advisor 两角色总是固定为 brain（`--host claude`，Opus）。
不将 Reviewer 路由到 cursor / codex 后端（实现后的后端不得审查自己的输出）。

### 非 `claude` 后端的 self_review 门

后端为 `codex` 或 `cursor` 时，`worker-report.v1` 和 `self_review` 数组都不生成。
因此 Lead **跳过** self_review 门，将 Lead 的 diff 审查作为唯一质量门（与现有 codex path 相同处理）。

### cursor 后端的 banner（委托前必需）

后端为 `cursor` 时，Lead 在委托前必须输出以下 1 行 banner:

```
⚠️ cursor backend: model=composer-2.5-fast / R01-R13 护栏不在 cursor-agent 内部适用 / 输出到 Lead 审查为止 untrusted
```

cursor 的 write 委托在持有专用 `.git` 的 worktree 内执行，Lead 将其 cherry-pick 到 main（在 cherry-pick 路径适用 R01-R13）。
治理细节参考 `.claude/rules/cursor-cli-only.md`。

## 选项

| 选项 | 说明 | 默认 |
|----------|------|----------|
| `all` | 所有未完成任务为目标 | - |
| `N` or `N-M` | 任务编号/范围指定 | - |
| `--parallel N` | 并行 worker 数 | auto |
| `--sequential` | 强制串行执行 | - |
| `--codex` | 实现委托给 Codex CLI（仅明确时，不自动选择） | false |
| `--backend <claude\|codex\|cursor>` | 明确后端选择（仅适用于 worker 角色，precedence 最高） | resolver result（未设置时是 claude） |
| `--cursor` | cursor 后端（`--backend cursor` 的别名） | false |
| `--plan NAME` | 使用 `plans/manifest.json` 的 named plan | active/default |
| `--no-commit` | 抑制自动 commit | false |
| `--resume <id\|latest>` | 重新开始上次会话 | - |
| `--breezing` | Lead/Worker/Reviewer 的团队执行 | false |
| `--no-tdd` | 跳过 TDD 阶段 | false |
| `--tdd-bypass` | 仅紧急时 bypass TDD 强制。在 audit 中留下 `HARNESS_TDD_BYPASS_REASON` 或明确理由 | false |
| `--no-simplify` | 跳过 Auto-Refinement | false |
| `--auto-mode` | 明确 Auto Mode rollout。仅在亲会话的 permission mode 兼容时考虑采用 | false |

## Progressive Disclosure

首先确认本文的入口、自动选择、停止条件。
详细内容只在必要时阅读。

| 详细 | 参考 |
|---|---|
| Codex native Solo / Parallel / Breezing 的具体步骤 | `references/execution-modes.md` |
| companion review、Reviewer fallback、AI Residuals、修正循环 | `references/review-loop.md` |
| 完成报告的生成 | `references/completion-report.md` |
| 测试/CI 失败时的再票决化 | `references/failure-reticketing.md` |
| 规格正本检查的标准 | `docs/plans/spec-ssot.md` |

### 重要停止条件

- `Plans.md` 是旧格式无法读取 DoD / Depends / Status 时停止。
- 规格影响实现判断但找不到 project spec SSOT 时，先制作/更新规格正本后再实现。
- sprint-contract 是 required 但不是 ready 时不进入实现。
- 剩余 critical / major review findings 时不完结。
- 不以减弱测试、skip 测试、将期待值配合实现放宽的形式解决。
- helper script 从 `${HARNESS_PLUGIN_ROOT}/scripts/` 调用，而不是 host project 的 `scripts/`。
- 有多个 Plans.md 时，在 1 run 中不切换 plan。必要时明确 `--plan NAME` 开始新 run。

> **Token Optimization (v2.1.69+)**: 在不伴随 git 操作的轻量任务中
> 启用 plugin settings 的 `includeGitInstructions: false` 可以
> 削减提示 token。

## 范围对话（无参数时）

```
harness-work
做到哪里?
1) 下一个任务: Plans.md 的下一个未完成任务 → Solo 执行
2) 全部（推荐）: 完成所有剩余任务 → 按任务数自动模式选择
3) 编号指定: 输入任务编号（例: 3, 5-7）→ 按件数自动模式选择
```

有参数则立即执行（跳过对话）:
- `harness-work all` → 全任务、自动模式选择
- `harness-work 3-6` → 4 件所以 Breezing 自动选择

## Effort 级别控制（Opus 4.8 / v2.1.111+）

effort 是选择模型推理强度的正式旋钮。`low(○)/medium(◐)/high(●)/xhigh` 的 4 阶段，
用 `/effort auto` 可以重置为默认（`max` 在 v2.1.72 废除，`xhigh` 是后继）。

Opus 4.8 thinking 默认 off，effort 是推理深度的主要 lever（比过去任何 Opus effort 影响都大）。
观测到"浅推理"不在 prompt 中回避，而是提高 effort。
因此复杂任务的强化**废除向 spawn prompt 注入 free-text marker（旧 `ultrathink`）方式**，
统一为从复杂度分数**选择 Worker spawn 的 effort tier**方式。

### 多要素评分

着手任务时合计以下分数。

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 文件以上 | +1 |
| 目录 | 包含 core/, guardrails/, security/ | +1 |
| 关键词 | 包含 architecture, security, design, migration | +1 |
| 失败履历 | agent memory 中有同任务的失败记录 | +2 |
| 明确指定 | PM 模板中有 `effort: high` / `effort: xhigh`（旧 `ultrathink` 也兼容受理）记载 | +3（自动采用） |

### effort tier 的决定方式（不注入）

从分数将 effort tier 作为 **escalation signal** 决定（不在 spawn prompt 中**写入** `ultrathink` 等标记字符串）。
适用的 lever 只有以下 2 个:

- **session `/effort`**: host 在进入复杂任务批次前设置 `/effort high` / `/effort xhigh`（在 session 单位有效的可靠 lever）。
- **worker frontmatter**: `agents/worker.md` 的 `effort`（默认 `medium`）是 floor。CC 的 Agent / Task spawn API 不公开 per-spawn 的 effort 指定，没有逐个提高 worker effort 的机制。分数记录在 `worker-report.v1` 的 `task_complexity_note` 中，Lead 作为提高 session effort 的判断材料。

| 分数 | code-risk（包含 core/guardrails/security/architecture/migration） | effort tier |
|--------|-----------------------------------|-------------|
| 0-2 | 不问 | `medium`（Worker frontmatter 默认） |
| ≥ 3 | 无 | `high` |
| ≥ 3 | 有 | `xhigh` |

breezing 模式也应用相同逻辑（harness-work 统一管理）。

## 执行模式详细

### Harness helper script root

Harness 附带的 helper script 必须从 plugin bundle root 调用，而不是作业对象项目的 `scripts/`。

```bash
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"
if [ -z "$HARNESS_PLUGIN_ROOT" ] && [ -n "${CLAUDE_SKILL_DIR:-}" ]; then
  probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"
  while [ "$probe" != "/" ] && [ ! -d "$probe/scripts" ]; do
    probe="$(cd "$probe/.." && pwd)"
  done
  [ -d "$probe/scripts" ] && HARNESS_PLUGIN_ROOT="$probe"
fi
```

以后的 `node "${HARNESS_PLUGIN_ROOT}/scripts/..."` / `bash "${HARNESS_PLUGIN_ROOT}/scripts/..."` 以此解决后的 root 为前提。

### Backend-resolved executor path (Solo / Parallel / Breezing)

Solo / Parallel / Breezing 从同一个 resolver result 选择实现 executor。
`harness-work 3 --cursor` 和 user/project `HARNESS_IMPL_BACKEND=cursor` 即使在 1 件任务时也不能 fall through 到 local Read/Write/Edit/Bash。

```
resolver_backend_arg = ""
if explicit_backend_value in ["claude", "codex", "cursor"]:
    resolver_backend_arg = "--backend {explicit_backend_value}"
backend = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh\" {resolver_backend_arg}")
if explicit_flag == "--cursor":
    backend = "cursor"
if explicit_flag == "--codex":
    backend = "codex"

if topology in ["solo", "parallel"] and backend in ["cursor", "codex"]:
    BASE_REF = git("rev-parse", "HEAD")
    WT_ID = "{task.number}-$(date +%Y%m%d-%H%M%S)-$$"
    worktree_path = ".claude/worktrees/{backend}-{WT_ID}"
    worktree_branch = "{backend}-work/{WT_ID}"
    bash("mkdir -p .claude/worktrees && git worktree add -b {worktree_branch} {worktree_path} {BASE_REF}")
    companion_prompt = "{task prompt}\n\n完成更改后，在此 worktree 中创建恰好一个 git commit 然后返回。"
    if backend == "cursor":
        companion_output = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh\" task --write --workspace {worktree_path} \"{companion_prompt}\"")
    else:
        companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
        companion_output = bash("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE={companion_state_file} bash \"${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh\" task --write -C {worktree_path} \"{companion_prompt}\"")
    latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if backend == "cursor" and git("-C", worktree_path, "status", "--porcelain") != "":
        git("-C", worktree_path, "add", "-A")
        git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: delegated change")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if latest_commit == BASE_REF:
        raise EscalationError("{backend} companion produced no commit")
    worker_result = {type: "companion-result.v1", baseCommit: BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: worktree_branch, files_changed: git("-C", worktree_path, "diff", "--name-only", "{BASE_REF}..HEAD"), summary: companion_output}
    enter_shared_review_loop(worker_result)
else:
    run_native_solo_or_parallel()
```

Parallel 对每个 task 应用此 resolver path。
backend=`cursor` / `codex` 时不用 native Worker spawn，为每个 task 创建 isolated companion worktree 并正规化到 `companion-result.v1` 后进入共同 review / cherry-pick loop。

### Solo 模式（1 件时自动选择）

1. 读取 Plans.md，确定目标任务
   - **Plans.md 不存在时**: 自动调用 `harness-plan create --ci` → 生成 Plans.md 后继续
   - 标题没有 DoD / Depends 栏时: `Plans.md 是旧格式。请用 harness-plan create 重新生成。` → **停止**
   - **会话中有未记载任务时**: 从最近会话上下文提取要求，作为 `cc:TODO` 自动追加到 Plans.md
     - 提取逻辑: 从用户发言检测动作动词（"添加〜"、"修正〜"、"实现〜"）
     - 追加时遵循 v2 格式（Task / 内容 / DoD / Depends / Status）
     - 追加后向用户显示"已向 Plans.md 追加以下内容"（附带 5 秒超时提示，默认: 继续）
1.5. **任务背景确认**（30 秒）:
   - 从任务的"内容"和"DoD" 推论显示 **目的**（此任务解决的课题）为 1 行
   - 用 `git grep` / `Glob` 推论显示 **影响范围**（变更涉及的文件/模块）
   - 对推论有自信时: 直接进入实现（不延迟流程）
   - 对推论没有自信时: 向用户确认仅 1 问（"这个理解正确吗？"）
1.6. **规格正本 preflight**:
   - 寻找现有的 project spec SSOT（例: `docs/spec/00-project-spec.md`, `docs/ARCHITECTURE.md`, `docs/HANDOFF.md`, `docs/oem/PROJECT_COMPASS.md`, `docs/specs/`）
   - task 变更 product behavior / API / data model / permission / billing / integration / tenant boundary 时，如果没有 spec 则创建 `docs/spec/00-project-spec.md`
   - spec 过旧，或与 task 矛盾时，实现前更新 spec
   - typo / format / dependency bump / docs-only / 无行为变更 refactor 时记录 skip 理由后继续
   - 向 Worker / Reviewer 传递的 context 包含 `spec_path` 或 `spec_skip_reason`
2. タスクを `cc:WIP` に更新
3. **TDD フェーズ**（`[skip:tdd]` なし & テストFW存在時）:
   a. テストファイルを先に作成（Red）
   b. 失敗を確認
   c. `bash "${HARNESS_PLUGIN_ROOT}/scripts/log-tdd-red.sh"` で `.claude/state/tdd-red-log/<task-id>.jsonl` に FAIL 証跡を残す。script が利用できない環境では、literal な failing test output を worker-report の `self_review` evidence に添付する
   d. `--tdd-bypass` を使う場合は、`HARNESS_TDD_BYPASS=1` と `HARNESS_TDD_BYPASS_REASON="<理由>"` を明示し、TDD を省略した理由を sprint-contract / worker-report に残す
4. `node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" <task-id>` で `sprint-contract.json` を生成
5. Reviewer 観点の追記を `bash "${HARNESS_PLUGIN_ROOT}/scripts/enrich-sprint-contract.sh"` で加え、`bash "${HARNESS_PLUGIN_ROOT}/scripts/ensure-sprint-contract-ready.sh"` で approved を確認
6. **Advisor consult（必要時のみ）**:
   - 高リスク task（`needs-spike` / `security-sensitive` / `state-migration`）は、初回実行前に 1 回だけ相談する
   - 同じ原因の失敗が 2 回続いたら、3 回目に入る前に相談する
   - plateau（行き詰まり検知）が `PIVOT_REQUIRED` を返した時は、ユーザーへ止めて投げる前に 1 回だけ相談する
   - 相談結果は `advisor-response.v1` で受け取り、`PLAN` は進め方の組み替え、`CORRECTION` は局所修正、`STOP` は即エスカレーションとして扱う
   - 同じ `trigger_hash` では 1 回しか相談しない。task ごとの相談回数は最大 3 回
7. backend-resolved executor path でコードを実装（Green）
   - backend=`claude`: local / native Read/Write/Edit/Bash path で実装
   - backend=`cursor` / `codex`: 上記 companion worktree path で実装し、`companion-result.v1` を共通 review loop に渡す
8. `/simplify` で Auto-Refinement（`--no-simplify` で省略可）
9. **自動レビューステージ**（「レビューループ」参照）:
   - Codex exec 優先でレビュー実行 → フォールバックで内部 Reviewer agent
   - `sprint-contract.json` の `reviewer_profile` が `runtime` の場合は `bash "${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh"` を実行
   - REQUEST_CHANGES の場合: 指摘を元に修正→再レビュー（`MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3`）
   - APPROVE で次ステップへ。self-check だけでは完了を確定しない
10. `bash "${HARNESS_PLUGIN_ROOT}/scripts/write-review-result.sh"` で review artifact を正規化して保存（browser profile は `--browser-result` を渡し、`browser_verdict == PENDING_BROWSER` の時は static verdict を採用）
11. `git commit` で自動コミット（`--no-commit` で省略可）
12. タスクを `cc:完了` に更新（commit hash 付与）
   - `git log --oneline -1` で直近の commit hash（短縮形 7 文字）を取得
   - Plans.md の Status を `cc:完了 [a1b2c3d]` 形式で更新
   - commit がない場合（`--no-commit` 時）は hash なしで `cc:完了` のみ
13. **リッチ完了報告**（`Completion Report Output Contract` と `references/completion-report.md` を参照）
14. **失敗時の自動再計画**（テスト/CI 失敗時のみ）:
    - テスト実行結果を確認
    - 失敗した場合: 修正タスク案を state に保存し、承認コマンド経由で Plans.md に追加（「失敗タスクの自動再チケット化」参照）
    - 成功した場合: 次タスクへ進む

### Parallel モード（2〜3 件時の自動選択 / `--parallel N` で強制）

`[P]` マーク付きタスクを N ワーカーで並列実行。
`--parallel N` で明示指定した場合は、タスク数に関係なくこのモードを使用。
同一ファイルへの書き込みが競合する場合は git worktree で分離。
各 task の実装 executor は Backend-resolved executor path に従う。
`--parallel N --cursor`、`--backend cursor`、または default `HARNESS_IMPL_BACKEND=cursor` の場合、Parallel でも native Worker spawn ではなく task ごとの Cursor companion worktree を使う。

### Codex モード（`--codex` 明示時のみ）

公式プラグイン `codex-plugin-cc` の companion 経由で Codex CLI にタスクを委託する。

```bash
# タスク委託（書き込み可能・worktree 分離）
BASE_REF="$(git rev-parse HEAD)"
WT_ID="codex-$(date +%Y%m%d-%H%M%S)-$$"
WORKTREE_PATH=".claude/worktrees/${WT_ID}"
git worktree add -b "codex-work/${WT_ID}" "$WORKTREE_PATH" "$BASE_REF"
HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
  bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write -C "$WORKTREE_PATH" \
  "タスク内容。完了前にこの worktree で exactly one git commit を作成してください。"

# stdin 経由（大きなプロンプト向け）
CODEX_PROMPT=$(mktemp /tmp/codex-prompt-XXXXXX.md)
# タスク内容を書き出し
cat "$CODEX_PROMPT" | HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
  bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write -C "$WORKTREE_PATH"
rm -f "$CODEX_PROMPT"

# Lead review 後に承認されたら range を取り込む
git -C "$WORKTREE_PATH" diff "$BASE_REF..HEAD"
WORKTREE_HEAD="$(git -C "$WORKTREE_PATH" rev-parse HEAD)"
git cherry-pick --no-commit "$BASE_REF..$WORKTREE_HEAD"
```

companion は App Server Protocol 経由で Codex と通信し、
Job 管理・thread resume・構造化出力を提供する。
結果を検証し、品質基準を満たさない場合は自力で修正。

### Breezing モード（4 件以上で自動選択 / `--breezing` で強制）

Lead / Worker / Advisor / Reviewer の役割分離でチーム実行する。
Codex host の Breezing は resolver result に従う。配布 plugin のフラグなし fallback は `claude` なので、
`spawn_agent`, `wait_agent`, `send_input`, `close_agent` を使った Codex native subagent orchestration が互換既定。
`--backend cursor` / `--cursor`、または user/project config の `HARNESS_IMPL_BACKEND=cursor` がある時だけ
`cursor-companion.sh` で Cursor worker に委託する。
古い TeamCreate / TaskCreate ベースの説明は採らない。

**権限ポリシー**:
- 現行の shipped default は `bypassPermissions`
- `--auto-mode` は互換な親セッション向けの opt-in rollout フラグとして扱う
- `permissions.defaultMode` や agent frontmatter の `permissionMode` には未文書化の `autoMode` 値を書かない

> **CC v2.1.69+**: nested teammates はプラットフォーム側で禁止されるため、
> Worker/Reviewer プロンプトには冗長な nested 防止文言を追加しない。

```
Lead (this agent)
├── Worker (resolver result: Codex native spawn_agent / codex-companion / cursor-companion) — 実装担当
├── Advisor (claude-code-harness:advisor) — 方針助言
└── Reviewer (code-reviewer agent) — レビュー担当
```

**Phase A: Pre-delegate（準備）**:
1. Plans.md を読み込み、対象タスクを特定
2. 依存グラフを解析し、実行順序を決定（Depends カラム）
3. 各タスクの仕様正本 preflight を行い、必要なら `docs/spec/00-project-spec.md` または既存 spec を実装前に更新
4. 各タスクの effort スコアリング（effort tier 判定 — high/xhigh）
5. `node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js"` で `sprint-contract.json` を生成
6. `bash "${HARNESS_PLUGIN_ROOT}/scripts/enrich-sprint-contract.sh"` で Reviewer 観点を加え、`bash "${HARNESS_PLUGIN_ROOT}/scripts/ensure-sprint-contract-ready.sh"` で未承認なら停止

**Phase B: Delegate（Worker spawn → 必要時 Advisor → レビュー → cherry-pick）**:

各タスクについて以下を**逐次**実行する（依存順）:

> **API 注記**: 以下は Codex native の subagent API 構文で記述する。
> backend=`claude` の時だけ `spawn_agent(...)`, `send_input(...)`, `wait_agent(...)`, `close_agent(...)` をそのまま使う。
> backend=`cursor` / `codex` の時は Worker agent を spawn せず、Lead が companion を直接呼ぶ。
> Claude Code 向けのサブエージェント構文やメッセージ送信構文は混ぜない。

```
for task in execution_order:
    # B-0. backend 解決（配布 fallback は claude、user/project default で cursor 可）
    resolver_backend_arg = ""
    if explicit_backend_value in ["claude", "codex", "cursor"]:
        resolver_backend_arg = "--backend {explicit_backend_value}"
    backend = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh\" {resolver_backend_arg}")
    if explicit_flag == "--cursor":
        backend = "cursor"
    if explicit_flag == "--codex":
        backend = "codex"

    # B-1. sprint-contract を生成
    contract_path = bash("node \"${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js\" {task.number}")
    contract_path = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/enrich-sprint-contract.sh\" {contract_path} --check \"DoD を reviewer 観点で確認\" --approve")
    bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/ensure-sprint-contract-ready.sh\" {contract_path}")

    # B-2. Worker 委託（worktree 分離）
    Plans.md: task.status = "cc:WIP"  # 着手時に更新（未着手タスクは cc:TODO のまま）

    if backend == "cursor":
        BASE_REF = git("rev-parse", "HEAD")
        WT_ID = "{task.number}-$(date +%Y%m%d-%H%M%S)-$$"
        worktree_path = ".claude/worktrees/cursor-{WT_ID}"
        worktree_branch = "cursor-work/{WT_ID}"
        bash("mkdir -p .claude/worktrees && git worktree add -b {worktree_branch} {worktree_path} {BASE_REF}")
        print("🚀 cursor / $(bash \"${HARNESS_PLUGIN_ROOT}/scripts/model-routing.sh\" --host cursor --role worker --field model) / {branch} / {task.number}")
        companion_prompt = "{task prompt}\n\nAfter making changes, create exactly one git commit in this worktree before returning."
        companion_output = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh\" task --write --workspace {worktree_path} \"{companion_prompt}\"")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if git("-C", worktree_path, "status", "--porcelain") != "":
            git("-C", worktree_path, "add", "-A")
            git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: delegated change")
            latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if latest_commit == BASE_REF:
            raise EscalationError("cursor companion produced no commit")
        worker_result = {type: "companion-result.v1", baseCommit: BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: worktree_branch, files_changed: git("-C", worktree_path, "diff", "--name-only", "{BASE_REF}..HEAD"), summary: companion_output}
        worker_id = null
    elif backend == "codex":
        BASE_REF = git("rev-parse", "HEAD")
        WT_ID = "{task.number}-$(date +%Y%m%d-%H%M%S)-$$"
        worktree_path = ".claude/worktrees/codex-{WT_ID}"
        worktree_branch = "codex-work/{WT_ID}"
        bash("mkdir -p .claude/worktrees && git worktree add -b {worktree_branch} {worktree_path} {BASE_REF}")
        companion_prompt = "{task prompt}\n\nAfter making changes, create exactly one git commit in this worktree before returning."
        companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
        companion_output = bash("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE={companion_state_file} bash \"${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh\" task --write -C {worktree_path} \"{companion_prompt}\"")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
        if latest_commit == BASE_REF:
            raise EscalationError("codex companion produced no commit")
        worker_result = {type: "companion-result.v1", baseCommit: BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: worktree_branch, files_changed: git("-C", worktree_path, "diff", "--name-only", "{BASE_REF}..HEAD"), summary: companion_output}
        worker_id = null
    else:
        print("🚀 claude / native-subagent / {branch} / {task.number}")
        worker_id = spawn_agent({
            message: "タスク: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\nspec_path: {spec_path}\nspec_skip_reason: {spec_skip_reason}\nmode: breezing\n\n作業は分離 worktree で行い、完了後に git commit してください。\n完了時は {commit, worktreePath, branch, files_changed, summary} を返してください。",
            fork_context: true
        })
        worker_result = wait_agent({ targets: [worker_id] })
    # worker_result には {commit, worktreePath, branch, files_changed, summary} が含まれる
    # backend=cursor/codex では Lead が companion stdout を companion-result.v1 に正規化する。

    # B-3. Worker が advice request を返した時だけ、Lead が Advisor を呼ぶ
    if backend == "claude" and worker_result.type == "advisor-request.v1":
        advisor_id = spawn_agent({
            message: worker_result.request_json,
            agent_type: "default",
            fork_context: true
        })
        advisor_result = wait_agent({ targets: [advisor_id] })
        close_agent({ target: advisor_id })
        send_input({
            target: worker_id,
            message: "advisor-response.v1: {advisor_result}"
        })
        worker_result = wait_agent({ targets: [worker_id] })

    # B-4. Lead がレビュー実行（Codex exec 優先）
    if backend == "claude":
        diff_text = git("-C", worker_result.worktreePath, "show", worker_result.commit)
    else:
        diff_text = git("-C", worker_result.worktreePath, "diff", "{worker_result.baseCommit}..HEAD")
    verdict = codex_exec_review(diff_text) or reviewer_agent_review(diff_text)
    profile = jq(contract_path, ".review.reviewer_profile")
    review_input = "review-output.json"
    if profile == "runtime":
        review_input = bash("cd {worker_result.worktreePath} && bash \"${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh\" {contract_path}")
        runtime_verdict = jq(review_input, ".verdict")
        if runtime_verdict == "REQUEST_CHANGES":
            verdict = "REQUEST_CHANGES"
        elif runtime_verdict == "DOWNGRADE_TO_STATIC":
            pass  # runtime 検証コマンドなし → static verdict をそのまま使う
    browser_result = ""
    if profile == "browser":
        # browser artifact から route / browser_mode / execution_instructions を再利用して browser runner を起動する。
        browser_artifact = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/generate-browser-review-artifact.sh\" {contract_path}")
        browser_result = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/browser-review-runner.sh\" {browser_artifact}")
        browser_verdict = jq(browser_result, ".browser_verdict")
        if browser_verdict == "REQUEST_CHANGES":
            verdict = "REQUEST_CHANGES"
        elif browser_verdict == "APPROVE" and verdict != "REQUEST_CHANGES":
            verdict = "APPROVE"
        # browser_verdict == PENDING_BROWSER のときは static verdict を維持する
    # review_input が DOWNGRADE_TO_STATIC の場合は static review 結果を使う
    if review_input != "review-output.json" and jq(review_input, ".verdict") == "DOWNGRADE_TO_STATIC":
        review_input = "review-output.json"  # static review の結果にフォールバック
    bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/write-review-result.sh\" {review_input} {latest_commit} --browser-result {browser_result}")

    # B-5. 修正ループ（REQUEST_CHANGES 時、contract の max_iterations まで）
    review_count = 0
    # sprint-contract が存在するときのみ max_iterations を読む。存在しない場合は 3（後方互換）
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3
    latest_commit = worker_result.commit
    while verdict == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
        if backend == "claude":
            send_input({
                target: worker_id,
                message: "指摘内容: {issues}\n修正して amend してください"
            })
            # Worker が修正 → amend → 更新された commit hash を返す
            updated_result = wait_agent({ targets: [worker_id] })
            latest_commit = updated_result.commit
        elif backend == "cursor":
            previous_commit = latest_commit
            companion_output = bash("bash \"${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh\" task --write --workspace {worker_result.worktreePath} \"Review findings:\n{issues}\n\nFix the findings and commit the result.\"")
            latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
            if git("-C", worker_result.worktreePath, "status", "--porcelain") != "":
                git("-C", worker_result.worktreePath, "add", "-A")
                git("-C", worker_result.worktreePath, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: review fix")
                latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
            if latest_commit == previous_commit:
                raise EscalationError("cursor companion retry produced no new commit")
            worker_result.commit = latest_commit
            worker_result.summary = companion_output
        else:
            previous_commit = latest_commit
            companion_state_file = "{worker_result.worktreePath}/.claude/state/codex-primary-environment.json"
            companion_output = bash("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE={companion_state_file} bash \"${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh\" task --write -C {worker_result.worktreePath} \"Review findings:\n{issues}\n\nFix the findings and commit the result.\"")
            latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
            if latest_commit == previous_commit:
                raise EscalationError("codex companion retry produced no new commit")
            worker_result.commit = latest_commit
            worker_result.summary = companion_output
        if backend == "claude":
            diff_text = git("-C", worker_result.worktreePath, "show", latest_commit)
        else:
            diff_text = git("-C", worker_result.worktreePath, "diff", "{worker_result.baseCommit}..HEAD")
        verdict = codex_exec_review(diff_text) or reviewer_agent_review(diff_text)
        review_count++

    # B-6. Worker 終了
    if backend == "claude":
        close_agent({ target: worker_id })

    # B-7. APPROVE → trunk に cherry-pick（feature ブランチ経由）
    # Worker の Branch Guard により trunk HEAD は動かず、commit は feature ブランチ上にある想定
    if verdict == "APPROVE":
        TRUNK=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's|refs/remotes/origin/||' || echo "main")
        git checkout "$TRUNK"  # safety: 既に trunk なら no-op
        # feature ブランチの commit が既に trunk にある（Branch Guard 失敗時のフォールバック）か確認
        if git("merge-base", "--is-ancestor", latest_commit, "HEAD"):
            pass  # 既に trunk 上 — cherry-pick 不要（再入防止）
        else:
            if backend == "claude":
                git cherry-pick --no-commit {latest_commit}  # feature branch → trunk
            else:
                git cherry-pick --no-commit {worker_result.baseCommit}..{latest_commit}  # companion range → trunk
            git commit -m "{task.内容}"
        # Worker の worktree を remove してから feature ブランチを削除
        if worker_result.worktreePath:
            git worktree remove {worker_result.worktreePath} --force
        if worker_result.branch and worker_result.branch not in ["main", "master"] and worker_result.branch != TRUNK:
            git branch -D {worker_result.branch}
        Plans.md: task.status = "cc:完了 [{hash}]"
        # auto-checkpoint 記録（冪等性ガード (c)）
        # Plans.md 書き換え直後に呼ぶ。失敗しても fail-open（|| true）でループを止めない
        HASH=$(git rev-parse --short HEAD)
        REVIEW_RESULT_PATH=".claude/state/review-results/${task.number}.review-result.json"
        bash "${HARNESS_PLUGIN_ROOT}/scripts/auto-checkpoint.sh" \
            "${task.number}" "${HASH}" "${contract_path}" "${REVIEW_RESULT_PATH}" \
            || true  # fail-open: harness-mem 未起動環境でも継続
    else:
        → ユーザーにエスカレーション

    # B-8. Progress feed
    print("📊 Progress: Task {completed}/{total} 完了 — {task.内容}")
```

### Advisor Protocol（全モード共通）

Advisor は「実装者」でも「レビュー担当」でもない。
迷った時だけ、実行役が次の一歩を決めるための相談役として入る。

1. Worker は generic な subagent を増やさず、必要時だけ `advisor-request.v1` を返す
2. Lead が advisor を 1 回だけ呼ぶ
3. Advisor は `PLAN` / `CORRECTION` / `STOP` のどれかを返す
4. Lead はその advice を同じ Worker に返して続行させる
5. Reviewer は最後の成果物だけを見る。advisor の返答に APPROVE / REQUEST_CHANGES を出さない

### Solo モードでの Advisor

solo 実行では親セッション自身が Lead を兼ねる。
つまり「自分で実装し、自分で advisor に相談し、最後は独立レビューに回す」形になる。

- 相談条件は loop / breezing と同じ
- 相談 budget も task ごとに最大 3 回で同じ
- `STOP` はその場で止まり、ユーザー判断へ上げる
- review artifact の gate は飛ばさない

### Sprint Contract

`sprint-contract` は「このタスクを何で合格にするか」を機械でも人でも同じ意味で読める形にする小さな契約ファイルです。
既定の保存先は `.claude/state/contracts/<task-id>.sprint-contract.json` です。

```bash
node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" 32.1.1
```

生成物には次を含めます。

- `checks`: DoD を分解した確認項目
- `non_goals`: 今回やらないこと
- `runtime_validation`: test, lint, typecheck などの検証コマンド
  - If you grep the same symbol twice in the same session, switch to harness_ast_search.
  - For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
  - Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.
- `browser_validation`: browser reviewer が残すべき UI フロー検証項目
- `browser_mode`: `scripted` または `exploratory`
- `route`: browser reviewer が `playwright` / `agent-browser` / `chrome-devtools` のどれを使うか
- `risk_flags`: `needs-spike`, `security-sensitive`, `ux-regression` など
- `reviewer_profile`: `static`, `runtime`, `browser`

**Phase C: Post-delegate（統合・報告）**:
1. 全タスクの commit log を集計
2. **リッチ完了報告**（`Completion Report Output Contract` と `references/completion-report.md` の Breezing テンプレート）を出力
3. Plans.md の最終確認（全タスク cc:完了 になっているか）

## CI 失敗時の対応

CI が失敗した場合:

1. ログを確認してエラーを特定
2. 修正を実施
3. 同一原因で 3 回失敗したら自動修正ループを停止
4. 失敗ログ・試みた修正・残る論点をまとめてエスカレーション

## 失敗タスクの自動再チケット化

タスク完了後にテスト/CI が失敗した場合、修正タスク案を自動生成し、承認後に Plans.md へ反映する:

### トリガー条件

| 条件 | アクション |
|------|----------|
| `cc:完了` 後にテスト失敗 | 修正タスク案を state に保存し、承認を待つ |
| CI 失敗（3回未満） | 修正を実施し、失敗カウントをインクリメント |
| CI 失敗（3回目） | 修正タスク案を提示 + エスカレーション |

### 修正タスクの自動生成

1. 失敗原因を分類（syntax_error / import_error / type_error / assertion_error / timeout / runtime_error）
2. `.claude/state/pending-fix-proposals.jsonl` に修正タスク案を保存:
   - 番号: 元タスク番号 + `.fix` サフィックス（例: `26.1.fix`）
   - 内容: `fix: [元タスク名] - [失敗原因カテゴリ]`
   - DoD: テスト/CI が通ること
   - Depends: 元タスク番号
3. ユーザーが `approve fix <task_id>` を送ると Plans.md に `cc:TODO` で追加
4. `reject fix <task_id>` で提案を破棄。pending が1件だけのときは `yes` / `no` でも応答可能

## レビューループ

実装完了後（ステップ 5 の後）に自動実行される品質検証ステージ。
**全モード共通**（Solo / Parallel / Breezing）で統一的に適用される。
Parallel モードでは各 Worker が step 10（外部レビュー受付）として同じループを実行する。

### レビュー実行の優先順位

```
1. Codex exec（優先）
   ↓ codex コマンドが存在しない or タイムアウト（120s）
2. 内部 Reviewer agent（フォールバック）
```

### APPROVE / REQUEST_CHANGES の判定基準

レビュアーには以下の閾値基準を渡し、**この基準のみ**で verdict を判定させる。
基準外の改善提案は `recommendations` として返すが、verdict には影響しない。

| 重要度 | 定義 | verdict への影響 |
|--------|------|-----------------|
| **critical** | セキュリティ脆弱性、データ損失リスク、本番障害の可能性 | 1 件でも → REQUEST_CHANGES |
| **major** | 既存機能の破壊、仕様との明確な矛盾、テスト不通過 | 1 件でも → REQUEST_CHANGES |
| **minor** | 命名改善、コメント不足、スタイル不統一 | verdict に影響しない |
| **recommendation** | ベストプラクティス提案、将来の改善案 | verdict に影響しない |

> **重要**: minor / recommendation のみの場合は **必ず APPROVE** を返すこと。
> 「あったほうが良い改善」は REQUEST_CHANGES の理由にならない。

### Codex exec レビュー（公式プラグイン経由）

タスク開始時の HEAD を `BASE_REF` として保持し、その ref との差分をレビュー対象にする。
公式プラグイン `codex-plugin-cc` の companion review を使用する。

```bash
# タスク開始時に base ref を記録（Step 2 の cc:WIP 更新前に実行）
BASE_REF=$(git rev-parse HEAD)

# ... 実装完了後 ...

# 公式プラグインの構造化レビューを実行
bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" review --base "${BASE_REF}"
REVIEW_EXIT=$?
```

**verdict マッピング**（公式プラグイン → Harness 形式）:

公式プラグインは `review-output.schema.json` 準拠の構造化出力を返す。
Harness の verdict 形式への変換ルール:

| 公式 plugin | Harness | verdict 影響 |
|---|---|---|
| `approve` | `APPROVE` | - |
| `needs-attention` | `REQUEST_CHANGES` | - |
| `findings[].severity: critical` | `critical_issues[]` | 1件でも → REQUEST_CHANGES |
| `findings[].severity: high` | `major_issues[]` | 1件でも → REQUEST_CHANGES |
| `findings[].severity: medium/low` | `recommendations[]` | verdict に影響しない |

AI Residuals スキャンは引き続き `bash "${HARNESS_PLUGIN_ROOT}/scripts/review-ai-residuals.sh"` で実行し、
companion review の結果と合わせて最終 verdict を判定する。

```bash
# AI Residuals スキャン（companion review と並行実行可能）
AI_RESIDUALS_JSON="$(bash "${HARNESS_PLUGIN_ROOT}/scripts/review-ai-residuals.sh" --base-ref "${BASE_REF}" 2>/dev/null || echo '{"tool":"review-ai-residuals","scan_mode":"diff","base_ref":null,"files_scanned":[],"summary":{"verdict":"APPROVE","major":0,"minor":0,"recommendation":0,"total":0},"observations":[]}')"
```

### 内部 Reviewer agent フォールバック

Codex exec が使えない場合（`command -v codex` が失敗、または exit code ≠ 0）:

```
Agent tool: subagent_type="reviewer"
prompt: "以下の変更をレビューしてください。判定基準: critical/major → REQUEST_CHANGES、minor/recommendation のみ → APPROVE。diff: {git diff ${BASE_REF}}"
```

Reviewer agent は Read-only（Write/Edit/Bash 無効）で安全にレビューを実行する。

### 修正ループ（REQUEST_CHANGES 時）

```
review_count = 0
# sprint-contract が存在するときのみ max_iterations を読む。存在しない場合は 3（後方互換）
contract_path = get_sprint_contract_path()  # 例: .claude/state/contracts/<task-id>.sprint-contract.json
MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3

while verdict == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
    1. レビュー指摘を解析（critical / major のみ対象）
    2. 各指摘に対して修正を実装
    3. 再度レビューを実行（同じ判定基準・同じ優先順位）
    review_count++

if review_count >= MAX_REVIEWS and verdict != "APPROVE":
    → ユーザーにエスカレーション
    → 「MAX_REVIEWS 回修正しましたが以下の critical/major 指摘が残っています」+ 指摘一覧を表示
    → ユーザー判断を待つ（続行 / 中断）
```

### Breezing モードでの適用

Breezing モードでは **Lead** がレビューループを実行する（上記 Phase B 参照）:

1. Worker が worktree 内で実装・commit → Lead に結果返却
2. Lead が Codex exec でレビュー（優先）/ Reviewer agent（フォールバック）
3. REQUEST_CHANGES → Lead が `send_input` で Worker に修正指示し、`wait_agent` で再応答を待つ → Worker が amend
4. 修正後、再レビュー（`MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3` 回まで）
5. APPROVE → Lead が trunk（デフォルトブランチ）に cherry-pick → Plans.md を `cc:完了 [{hash}]` に更新

## Completion Report Output Contract

<!-- harness-work-completion-output-contract:start -->
Before rendering a Solo, forced single-task Parallel, or Breezing completion
report:

1. Resolve the active locale with the shared `get_harness_locale` function from
   `${HARNESS_PLUGIN_ROOT}/scripts/config-utils.sh`. Pass an explicit session or
   user language as its optional argument; otherwise keep the resolver priority
   of project `i18n.language`, `CLAUDE_CODE_HARNESS_LANG`, then default `en`.
2. Unset, invalid, and resolved `en` render the English template.
3. Only resolved `ja` renders the Japanese template.
4. Japanese input alone does not select the Japanese template.
5. Read `references/completion-report.md` and render exactly one template for
   the selected mode and locale.
6. Keep machine-readable status and review values in English, and never mix
   English and Japanese labels in one report.
<!-- harness-work-completion-output-contract:end -->

## 関連スキル

- `harness-plan` — 実行するタスクを計画する
- `harness-sync` — 実装と Plans.md を同期する
- `harness-review` — 実装のレビュー
- `harness-release` — バージョンバンプ・リリース
