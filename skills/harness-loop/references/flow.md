# harness-loop: wake-up 流程详细

`harness-loop` 各 wake-up 入口手续的详细版。
补全 SKILL.md 摘要的实现参考。

> **Java 版本边界**：本文件下方是 Go/Claude 宿主的 helper-script 参考
> 实现。Java 版本没有这些脚本或调度 runner；请使用宿主的 background
> task、resume/session 和 loop 能力，并使用 `harness sprint-contract`
> 与 `harness evidence` 完成契约和证据记录。不要逐条执行下方的
> `scripts/*.sh` 命令。

---

## 各 wake-up 的入口手续（详细）

### Step 0: plugin bundle root 解析

`harness-loop` 不调用 host project 的 cwd，而是调用 plugin bundle root 下的 helper script。
作业对象的 `Plans.md` 或 `.claude/state/...` 保留在 host project 侧，仅从 plugin bundle 读取相当于工具的 script。

```bash
resolve_harness_plugin_root() {
    if [ -n "${CLAUDE_PLUGIN_ROOT:-}" ] && [ -d "${CLAUDE_PLUGIN_ROOT}/scripts" ]; then
        (cd "${CLAUDE_PLUGIN_ROOT}" && pwd -P)
        return 0
    fi

    if [ -n "${CLAUDE_SKILL_DIR:-}" ]; then
        for candidate in "${CLAUDE_SKILL_DIR}/../.." "${CLAUDE_SKILL_DIR}/../../.."; do
            candidate_abs="$(cd "${candidate}" 2>/dev/null && pwd -P)" || continue
            if [ -f "${candidate_abs}/.claude-plugin/plugin.json" ] && [ -d "${candidate_abs}/scripts" ]; then
                printf '%s\n' "${candidate_abs}"
                return 0
            fi
        done
    fi

    echo "ERROR: cannot resolve Claude Harness plugin root. Set CLAUDE_PLUGIN_ROOT to the installed plugin bundle root." >&2
    return 1
}

HARNESS_PLUGIN_ROOT="$(resolve_harness_plugin_root)" || exit 1
```

- `CLAUDE_PLUGIN_ROOT` 有效时最优先使用
- 无 `CLAUDE_PLUGIN_ROOT` 时从 `CLAUDE_SKILL_DIR` 反推分发源
  - `skills/harness-loop` 分发则为 `${CLAUDE_SKILL_DIR}/../..`
  - `.agents/skills/harness-loop` mirror 分发则为 `${CLAUDE_SKILL_DIR}/../../..`
- 仅将有 `scripts/` 和 `.claude-plugin/plugin.json` 的候选用作 plugin root
- 不使用 host project cwd 的 `scripts/`

### Step 0.1: 多重启动防止锁（幂等性防护 (a)）

```bash
LOCK_DIR=".claude/state/locks/loop-session.lock.d"
mkdir -p ".claude/state/locks"

# 原子创建（已存在则立即失败 — 避免 TOCTOU 竞争）
if ! mkdir "${LOCK_DIR}" 2>/dev/null; then
    existing=$(cat "${LOCK_DIR}/meta.json" 2>/dev/null || echo '{}')
    echo "ERROR: harness-loop is already running (lock dir exists: ${LOCK_DIR})" >&2
    echo "Lock contents: ${existing}" >&2
    echo "To force-clear, run: rm -rf ${LOCK_DIR}" >&2
    exit 10
fi

# 将 lock 元数据写入 lock 目录内
SESSION_ID="${CLAUDE_SESSION_ID:-unknown}"
ARGS_STR="$*"
cat > "${LOCK_DIR}/meta.json" <<EOF
{
  "pid": $$,
  "session_id": "${SESSION_ID}",
  "started_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "args": "${ARGS_STR}"
}
EOF

# 结束时（正常・异常都删除）lock
cleanup_loop_lock() {
    rm -rf "${LOCK_DIR}" 2>/dev/null || true
}
trap cleanup_loop_lock EXIT INT TERM
```

- `LOCK_DIR` 为 `.claude/state/locks/loop-session.lock.d`（目录）
- `mkdir` 是原子的，不会发生 TOCTOU 竞争（2 进程同时执行也只有一方成功）
- lock 元数据写入 `${LOCK_DIR}/meta.json`: `{"pid": <pid>, "session_id": <session>, "started_at": <ISO8601>, "args": "<args>"}` 的 JSON
- 已有 lock 时为 `already running` 错误（exit 10）立即停止
- `EXIT` / `INT` / `TERM` 任一都删除 lock（正常・异常都 cleanup）
- `rm -rf` 幂等（删除 2 次也安全）

### Step 0.5: state 完整性检查（幂等性防护 (b)）

```bash
# wake-up 开头以 --quick 模式执行轻量完整性检查
# 失败时立即停止循环（保护 Plans.md 损坏・未初始化环境）
if bash "${HARNESS_PLUGIN_ROOT}/tests/validate-plugin.sh" --quick; then
    : # OK — 继续
else
    echo "harness-loop: state 完整性检查失败 — 停止循环" >&2
    echo "详细: 请执行 bash \"${HARNESS_PLUGIN_ROOT}/tests/validate-plugin.sh\" --quick 确认" >&2
    exit 1
fi
```

- `${HARNESS_PLUGIN_ROOT}/tests/validate-plugin.sh --quick` 轻量且数秒内完成
- 检查内容: `.claude/state/` 的存在 / Plans.md 的存在+v2格式 / sprint-contract 的形式
- 不运行完整 validate（39 验证项）
- 若故意损坏 Plans.md 时此检查失败，循环立即停止

### Step 1: 先读取 Plans.md

```bash
# 抽取 cc:WIP / cc:TODO 任务，特定先头任务的 task_id
grep -E "cc:(WIP|TODO)" Plans.md | head -1
```

- 残留 `cc:WIP` 任务时: 前循环可能中断 → 取得 task_id 继续
- 有 `cc:TODO` 任务时: 作为下个目标任务取得 task_id
- 都无时: **全任务完成** → 循环正常结束

> **41.1.2 前提**: `plans-watcher.sh` 以 flock 保护 Plans.md 时，
> Plans.md 读取在该 flock 范围内执行。
> 41.1.2 发布前可无 flock 直接读取。

### Step 2: sprint-contract 存在确认 & 生成

```bash
CONTRACT_PATH=".claude/state/contracts/${task_id}.sprint-contract.json"

if [ ! -f "${CONTRACT_PATH}" ]; then
    # contract 未生成 → 生成
    node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" "${task_id}"

    # Step 2.5: draft → approved 升级（仅初回生成时）
    # generate-sprint-contract.js 以 review.status == "draft" 初始化，
    # ensure-sprint-contract-ready.sh（approved 要求）之前必须升级
    bash "${HARNESS_PLUGIN_ROOT}/scripts/enrich-sprint-contract.sh" "${CONTRACT_PATH}" \
      --check "wake-up 自动批准（harness-loop 用 DoD 以 reviewer 视点确认）" \
      --approve
fi
```

- 确认 `.claude/state/contracts/${task_id}.sprint-contract.json` 的有无
- 不存在时以 `node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" ${task_id}` 生成
  （※ 41.5.1 预定 .sh→.js 重命名，但现时点经 node 调用既有名）
- **生成后（仅首次）**: `enrich-sprint-contract.sh --approve` 升级 `draft` → `approved`
  - `generate-sprint-contract.js` 以 `review.status == "draft"` 初始化
  - `ensure-sprint-contract-ready.sh`（下个 Step 3）仅接受 `approved`
  - 放入 `if [ ! -f ... ]` 块，不适用于既有 contract（前循环已 approved）
- 生成后 `${CONTRACT_PATH}` 在后续步骤复用

### Step 3: contract readiness 检查

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/ensure-sprint-contract-ready.sh" "${CONTRACT_PATH}"
```

- 确认 sprint-contract 的 `review.status == "approved"`
- 残留未批准 contract 时错误停止

### Step 4: Resume pack 重新读取

```
Step 4. harness-mem resume-pack 重新读取:
  调用 mcp__harness__harness_mem_resume_pack 工具。
  必须参数:
    - project: 当前项目名（仿现有 session-init 技能实现例。
              例: 以 `basename $(git rev-parse --show-toplevel)` 取得 repository root 并传递）
  可选: session_id（从前会话恢复时）

  例（伪代码）:
    resume_pack = mcp__harness__harness_mem_resume_pack(
      project="claude-code-harness",
      session_id=<上次 checkpoint 的 session_id>
    )
```

fresh context 的 wake-up 后失去前循环的记忆。
以相当于 `harness-mem resume-pack` 的操作重新注入以下:

- `decisions.md` — 架构决定事项
- `patterns.md` — 可复用模式
- `session-state` — 上次的作业状态
- 直前循环的 `checkpoint` — 完成了什么

> **注意**: resume pack 重新读取在 Step 3（contract readiness 检查）之后执行。
> 跳过时有重复实现前循环成果物的风险。

### Step 4.5: Advisor 咨询（仅必要时）

loop 以 executor 为主推进，advisor 仅在必要时调用。
咨询时机固定为以下 3 个:

1. 高风险任务首次执行前
2. 同一原因失败连续 2 次后
3. `PIVOT_REQUIRED` 停止前

```bash
TRIGGER_HASH="${task_id}:${reason_code}:$(normalize_error_signature "${summary_or_risk}")"

if ! advisor_trigger_seen "${TRIGGER_HASH}"; then
    RESPONSE_FILE=$(
        bash "${HARNESS_PLUGIN_ROOT}/scripts/run-advisor-consultation.sh" \
          --request-file ".claude/state/codex-loop/${task_id}.${reason_code}.advisor-request.json" \
          --response-file ".claude/state/codex-loop/${task_id}.${reason_code}.advisor-response.json"
    )
    DECISION=$(jq -r '.decision' "${RESPONSE_FILE}")
fi
```

- `PLAN` / `CORRECTION` 下次 executor prompt 头部放入 advice 再执行
- `STOP` 停止 loop，在 `run.json` 的 `last_decision`, `last_trigger`, `last_model` 记录
- 同一 `trigger_hash` 仅咨询 1 次
- 每任务咨询次数最多 3 次

### Step 5: 1 任务循环执行

经 Agent tool spawn `claude-code-harness:worker`:

> **重要**: `subagent_type` 应指定 `"claude-code-harness:worker"` 而非 `"harness-work"`。
> `harness-work` 是技能而非 agent。实际存在的 agent 是 `worker` / `reviewer`。
> 指定 `"harness-work"` 会导致 Agent spawn 失败，循环在首次 Worker 启动时停止。

```python
worker_result = Agent(
    subagent_type="claude-code-harness:worker",  # ← worker agent（非技能）
    prompt="""
    任务: ${task_id}
    DoD: <从 Plans.md 抽取>
    contract_path: ${CONTRACT_PATH}
    mode: breezing
    完成后请返回 commit hash・branch・变更摘要。
    """,
    isolation="worktree",
    run_in_background=False  # 前台执行（等待完成）
)
# worker_result: { commit, branch, worktreePath, files_changed, summary }
```

Worker 以 `mode: breezing` 动作:
- 仅在 feature branch 提交，不碰 main
- 变更内容存储在 `worktreePath`
- Lead（harness-loop）在 Step 5.5/5.6 负责评审 → cherry-pick

> **Codex loop 实现差分**: Codex 版 `${HARNESS_PLUGIN_ROOT}/scripts/codex-loop.sh` 启动 background task，
> 将 advisor 返回的 guidance prepend 到下次 prompt 重新执行同一 task。

> **实现注意**: `Bash("harness-work --breezing")` 也可替代，
> 但经 Agent tool 上下文分离明确且易调试。

### Step 5.5: Lead 评审执行

Lead 对 Worker 返回的 commit 执行评审:

```bash
# 取得 diff（以 worktree 内的 commit 为对象）
diff_text=$(git -C "${worker_result.worktreePath}" show "${worker_result.commit}")

# ── (a) Codex companion review: 在 Worker 的 worktree 目录执行 ──────────────
# Lead 在 main repo dir 时 diff 为空（无条件 APPROVE 的危险）。
# cd 到 Worker 的 worktreePath 后调用 review 传递正确差分。
#
# worktreePath 为空或与 main repo 同一（worktree isolation 不生效的环境）时
# 在 Lead dir 执行（与既有行为同等的 fallback）。
MAIN_REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
WORKER_PATH="${worker_result.worktreePath:-}"

if [ -n "${WORKER_PATH}" ] && [ "${WORKER_PATH}" != "${MAIN_REPO_ROOT}" ]; then
    # 在 Worker 的 worktree 内执行 review → 看 Worker feature branch 的实际差分
    REVIEW_EXIT=$?
    # review-output.json 在 Worker worktree dir 创建，因此以绝对路径管理
    REVIEW_OUTPUT_PATH="${WORKER_PATH}/review-output.json"
else
    # fallback: 在 Lead dir 执行（worktree isolation 不生效的环境）
    REVIEW_EXIT=$?
    REVIEW_OUTPUT_PATH="$(pwd)/review-output.json"
fi
# → REVIEW_OUTPUT_PATH 指示的文件中写入 verdict
# 后续全部使用 $REVIEW_OUTPUT_PATH（不直接参照相对路径 "review-output.json"）

# ── (b) reviewer_profile 分歧（确认 sprint-contract 的 review.reviewer_profile）──
# CONTRACT_PATH 使用 Step 2/3 确定的值（不在此覆盖）
if command -v jq >/dev/null 2>&1; then
    REVIEWER_PROFILE=$(jq -r '.review.reviewer_profile // "static"' "${CONTRACT_PATH}" 2>/dev/null || echo "static")
else
    REVIEWER_PROFILE="static"
fi

case "${REVIEWER_PROFILE}" in
    runtime)
        # 执行 runtime 验证命令，可能覆盖 verdict
        # run-contract-review-checks.sh 在 Worker 的 worktree 内执行（测试环境在 worktree 内）
        # 重要: run-contract-review-checks.sh 的 stdout 是 artifact 的"文件路径"（非 JSON payload）
        if [ -n "${WORKER_PATH}" ] && [ "${WORKER_PATH}" != "${MAIN_REPO_ROOT}" ]; then
            RUNTIME_ARTIFACT_PATH=$(
                cd "${WORKER_PATH}" && bash "${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh" "${CONTRACT_PATH}" 2>/dev/null
            ) || RUNTIME_ARTIFACT_PATH=""
        else
            RUNTIME_ARTIFACT_PATH=$(
                bash "${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh" "${CONTRACT_PATH}" 2>/dev/null
            ) || RUNTIME_ARTIFACT_PATH=""
        fi

        # 空（脚本失败）时作为 DOWNGRADE_TO_STATIC 处理
        if [ -z "${RUNTIME_ARTIFACT_PATH}" ]; then
            RUNTIME_ARTIFACT_PATH=""
            RUNTIME_VERDICT="DOWNGRADE_TO_STATIC"
        else
            # 相对路径时以 WORKER_PATH（或 Lead dir）为基点绝对路径化
            if [[ "${RUNTIME_ARTIFACT_PATH}" != /* ]]; then
                if [ -n "${WORKER_PATH}" ] && [ "${WORKER_PATH}" != "${MAIN_REPO_ROOT}" ]; then
                    RUNTIME_ARTIFACT_PATH="${WORKER_PATH}/${RUNTIME_ARTIFACT_PATH}"
                else
                    RUNTIME_ARTIFACT_PATH="$(pwd)/${RUNTIME_ARTIFACT_PATH}"
                fi
            fi

            # artifact ファイルから verdict を読む
            if command -v jq >/dev/null 2>&1; then
                RUNTIME_VERDICT=$(jq -r '.verdict // "DOWNGRADE_TO_STATIC"' "${RUNTIME_ARTIFACT_PATH}" 2>/dev/null || echo "DOWNGRADE_TO_STATIC")
            else
                RUNTIME_VERDICT=$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get('verdict','DOWNGRADE_TO_STATIC'))" "${RUNTIME_ARTIFACT_PATH}" 2>/dev/null || echo "DOWNGRADE_TO_STATIC")
            fi
        fi

        if [ "${RUNTIME_VERDICT}" = "REQUEST_CHANGES" ]; then
            # runtime 検証が失敗 → verdict を REQUEST_CHANGES に上書き
            # write-review-result.sh には runtime artifact を渡す（static review-output.json を使わない）
            EFFECTIVE_VERDICT="REQUEST_CHANGES"
            REVIEW_RESULT_INPUT="${RUNTIME_ARTIFACT_PATH}"
        elif [ "${RUNTIME_VERDICT}" = "DOWNGRADE_TO_STATIC" ]; then
            # runtime 検証コマンドなし → static verdict をそのまま使う
            EFFECTIVE_VERDICT=""  # → REVIEW_OUTPUT_PATH から読む
            REVIEW_RESULT_INPUT="${REVIEW_OUTPUT_PATH}"
        else
            EFFECTIVE_VERDICT="${RUNTIME_VERDICT}"
            REVIEW_RESULT_INPUT="${RUNTIME_ARTIFACT_PATH}"
        fi
        ;;
    browser)
        # browser reviewer が後続で使う artifact を生成
        # browser artifact は PENDING_BROWSER scaffold。実際の browser 実行は reviewer agent が担当。
        # review-result の verdict は static のまま（PENDING_BROWSER ではない）。
        bash "${HARNESS_PLUGIN_ROOT}/scripts/generate-browser-review-artifact.sh" "${CONTRACT_PATH}" 2>/dev/null || true
        EFFECTIVE_VERDICT=""  # → REVIEW_OUTPUT_PATH から読む（static verdict を使用）
        REVIEW_RESULT_INPUT="${REVIEW_OUTPUT_PATH}"
        ;;
    *)
        # static（デフォルト）: Codex companion review の verdict をそのまま使う
        EFFECTIVE_VERDICT=""
        REVIEW_RESULT_INPUT="${REVIEW_OUTPUT_PATH}"
        ;;
esac

# EFFECTIVE_VERDICT が設定されていない場合は REVIEW_OUTPUT_PATH（絶対パス）から読む
if [ -z "${EFFECTIVE_VERDICT}" ]; then
    if command -v jq >/dev/null 2>&1; then
        EFFECTIVE_VERDICT=$(jq -r '.verdict // "REQUEST_CHANGES"' "${REVIEW_OUTPUT_PATH}" 2>/dev/null || echo "REQUEST_CHANGES")
    else
        EFFECTIVE_VERDICT=$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get('verdict','REQUEST_CHANGES'))" "${REVIEW_OUTPUT_PATH}" 2>/dev/null || echo "REQUEST_CHANGES")
    fi
fi

# review-result を正規化して保存
# REVIEW_RESULT_INPUT は runtime REQUEST_CHANGES 時は runtime artifact パス、それ以外は REVIEW_OUTPUT_PATH
# これにより runtime REQUEST_CHANGES が pretooluse-guard まで正しく伝わる（指摘 4 対応）
bash "${HARNESS_PLUGIN_ROOT}/scripts/write-review-result.sh" "${REVIEW_RESULT_INPUT}" "${worker_result.commit}"
```

**verdict 判定**:

| verdict | アクション |
|---------|----------|
| `APPROVE` | Step 5.6 へ（cherry-pick） |
| `REQUEST_CHANGES` | 修正ループへ（最大 3 回） |

**修正ループ（REQUEST_CHANGES 時）**:

```python
review_count = 0
latest_commit = worker_result.commit
worker_id = worker_result.agentId
# sprint-contract が存在するときのみ max_iterations を読む。存在しない場合は 3（後方互換）
MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3

while verdict == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
    # Worker に修正を指示（SendMessage で再開）
    SendMessage(to=worker_id, message=f"指摘内容: {issues}\n修正して amend してください")
    updated_result = wait_for_response(worker_id)
    latest_commit = updated_result.commit
    diff_text = git("-C", worker_result.worktreePath, "show", latest_commit)
    verdict = codex_exec_review(diff_text) or reviewer_agent_review(diff_text)
    review_count += 1

if review_count >= MAX_REVIEWS and verdict != "APPROVE":
    # エスカレーション
    raise PivotRequired(f"{MAX_REVIEWS} 回修正後も REQUEST_CHANGES: {issues}")
```

### Step 5.6: APPROVE → cherry-pick 到 main

```bash
# trunk ブランチに戻る（Worker は feature branch で作業）
TRUNK=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's|refs/remotes/origin/||' || echo "main")
git checkout "${TRUNK}"

# feature branch の commit が trunk に未マージかを確認（再入防止）
if ! git merge-base --is-ancestor "${latest_commit}" HEAD; then
    git cherry-pick --no-commit "${latest_commit}"
    git commit -m "${task_title}"
fi

# ── (c) cleanup 順序: worktree remove → branch -D ────────────────────────────────
# feature branch が worktree に checkout されている状態では
# `git branch -D` が "branch is checked out at <path>" エラーになる。
# worktree remove を先に実行することで branch -D が安全に動作する。
#
# 順序:
#   1. cherry-pick → main に取り込み（上記 git commit 済み）
#   2. worktree remove（feature branch が checked out されていた worktree を削除）
#   3. branch -D（worktree が remove されたので削除可能になる）

MAIN_REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
WORKER_PATH="${worker_result.worktreePath:-}"

# Step 2: worktree remove
if [ -n "${WORKER_PATH}" ] && [ "${WORKER_PATH}" != "${MAIN_REPO_ROOT}" ]; then
    git worktree remove "${WORKER_PATH}" --force 2>/dev/null || true
fi

# Step 3: branch -D（worktree remove 後なので安全）
if [ -n "${worker_result.branch}" ] && \
   [ "${worker_result.branch}" != "main" ] && \
   [ "${worker_result.branch}" != "master" ] && \
   [ "${worker_result.branch}" != "${TRUNK}" ]; then
    git branch -D "${worker_result.branch}" 2>/dev/null || true
fi
```

Plans.md を更新:

```bash
# cc:WIP → cc:完了 [{hash}] に更新
HASH=$(git rev-parse --short HEAD)
# Plans.md の該当タスク行を更新
```

### Step 6: plateau 判定

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/detect-review-plateau.sh" ${current_task_id}
PLATEAU_EXIT=$?
# ※ current_task_id は Step 1 で特定した task_id
```

| exit code | 意味 | アクション |
|-----------|------|----------|
| `0` | `PIVOT_NOT_REQUIRED` | 続行 |
| `1` | `INSUFFICIENT_DATA` | 続行（データ不足） |
| `2` | `PIVOT_REQUIRED` | advisor を 1 回だけ挟む。`STOP` か相談枠切れのときだけ **ループ停止** + エスカレーション |

**PIVOT_REQUIRED 時のエスカレーションメッセージ**:

```
harness-loop: plateau 検知により停止（サイクル {N}/{max}）

検知された問題:
  {plateau の詳細: detect-review-plateau.sh の出力}

対応案:
  1. 手動でタスク内容を見直す
  2. `--pacing plateau` で間隔を延ばして再実行
  3. 問題タスクをスキップして `/harness-loop` を再起動

現在の Plans.md 状態を確認してください。
```

### Step 7: 循环数检查

```
cycles_completed += 1
if cycles_completed >= max_cycles:
    ループ停止
    print(f"harness-loop: {max_cycles} サイクル完了で停止")
    return
```

- default `max_cycles = 8`
- `--max-cycles N` 指定時は N サイクルで停止

**サイクルカウントの永続化**:
- `ScheduleWakeup` の `prompt` 引数にカウントを埋め込む:
  ```
  /harness-loop all --max-cycles 8 --cycles-done {N} --pacing worker
  ```
- wake-up 時に `--cycles-done N` を読み取り、カウントを復元する

### Step 8: checkpoint 记录

```json
{
  "session_id": "<現在のセッション ID>",
  "title": "harness-loop cycle {N}/{max}: {task_completed}",
  "content": "cycle {N} 完了。commit: {commit}。変更: {files_changed}。次: {next_task}"
}
```

`harness_mem_record_checkpoint` ツールでメモリに記録する。
次の wake-up の resume pack に自動的に含まれる。

### Step 9: 下次 wake-up 预约

```
ScheduleWakeup(
    delaySeconds=<pacing に対応する値>,
    prompt="/harness-loop <同じ引数> --cycles-done {N}",
    reason="サイクル {N}/{max} 完了: {task_completed}"
)
```

**pacing に対応する delaySeconds**:

| pacing | delaySeconds | 選定理由 |
|--------|-------------|---------|
| `worker` | 270 | Worker 完了直後の再入（5 min cache warm 以内） |
| `ci` | 270 | CI ジョブの最短完了を想定した待機 |
| `plateau` | 1200 | 20 min 冷却期間（plateau 回避） |
| `night` | 3600 | 深夜バッチ（最大 clamp 値） |

> **clamp 制約**: `ScheduleWakeup` は `delaySeconds` を `[60, 3600]` にランタイムで clamp する。
> 60 未満を指定すると 60 に切り上げ、3600 超を指定すると 3600 に切り下げられる。
> 设计值都在范围内，但将来变更时要注意。

---

## 循环停止条件矩阵

| 条件 | 循环数 | exit | 停止理由 | 用户通知 |
|------|-----------|------|---------|------------|
| `cycles >= max_cycles` | N (上限) | 0 | 正常上限 | 「{N} 循环完成停止」 |
| `PIVOT_REQUIRED` | 任意 | 2 | plateau 检测 | 升级详细 |
| 无未完成任务 | 任意 | 0 | 全任务完成 | 完成报告 |
| 用户取消 | 任意 | - | 手动中断 | - |

---

## pacing 选择指南

### 应使用哪个 pacing

```
任务性质是？
│
├── Worker 完成后想立即重新进入
│     → worker（270s）
│
├── 需要等待 CI / 测试完成
│     → ci（270s）
│     ※ CI 超过 270s 时手动调整 --pacing
│
├── 检测到 plateau 想空开间隔
│     → plateau（1200s）
│
└── 深夜放置翌晨确认
      → night（3600s）
```

### pacing 变更时机

- **首次启动时**: 通常 `worker`（默认）即可
- **CI 等待多时**: 切换到 `--pacing ci`
- **plateau 检测后**: 考虑 `--pacing plateau` 自动切换（参照 Step 5）
- **夜间放置**: `--pacing night` 启动后直接就寝

---

## ScheduleWakeup 约束详细

### delaySeconds 的运行时约束

```
ScheduleWakeup(delaySeconds=X)
  → X < 60  → clamp to 60
  → X > 3600 → clamp to 3600
  → 60 <= X <= 3600 → そのまま使用
```

### 与 cache TTL 的关系

ScheduleWakeup 的 cache TTL 为 **5 min（300s）**。

- `worker` / `ci` 的 270s 在 5 min 以内 → cache warm 状态下 wake-up
- `plateau` 的 1200s、`night` 的 3600s 在 cache 失效后 wake-up
  → Step 2（resume pack 重新读取）特别重要

### prompt 参数继承

将循环计数继承到下次 wake-up 的方法:

```bash
# 将当前 cycle count 嵌入 prompt
NEXT_PROMPT="/harness-loop ${SCOPE} --max-cycles ${MAX_CYCLES} --cycles-done ${CYCLES_DONE} --pacing ${PACING}"

ScheduleWakeup(
    delaySeconds=${DELAY},
    prompt="${NEXT_PROMPT}",
    reason="循环 ${CYCLES_DONE}/${MAX_CYCLES} 完成"
)
```

---

## 参考: spike 41.0.0 的验证结果

此设计基于 spike 41.0.0 的实证结果:

- `ScheduleWakeup`: 确认作为内部工具存在。delay [60, 3600] clamp、cache 5min TTL
- `/loop`: 确认作为 CC dynamic mode 存在。sentinel `<<autonomous-loop-dynamic>>`
- `harness_mem_record_checkpoint`: 确认存在（schema: session_id / title / content 必需）

这些前提变化时请更新本文件。
