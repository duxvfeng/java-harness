---
name: harness-loop
description: "Long-running task loop using /loop (Claude Code dynamic mode) and ScheduleWakeup to re-enter with fresh context on each wake-up. Internally invokes harness-work through Agent. Trigger: long-running, loop, wake-up, autonomous. Do NOT load for: one-shot task execution, review, release, planning."
description-en: "Long-running task loop using /loop (Claude Code dynamic mode) and ScheduleWakeup to re-enter with fresh context on each wake-up. Internally invokes harness-work through Agent. Trigger: long-running, loop, wake-up, autonomous. Do NOT load for: one-shot task execution, review, release, planning."
description-ja: "使用 /loop 和 ScheduleWakeup 在每次唤醒时以全新上下文重新执行长时间任务。内部通过 Agent 调用 harness-work。支持：长时间运行、循环、loop、唤醒、自主执行。"
description-zh: "使用 /loop 和 ScheduleWakeup 在每次唤醒时以全新上下文重新执行长时间任务。内部通过 Agent 调用 harness-work。支持：长时间运行、循环、loop、唤醒、自主执行。"
kind: workflow
purpose: "Re-enter long-running Plans.md execution with fresh context"
trigger: "long-running, loop, wake-up, autonomous"
shape: delegate
role: orchestrator
base: harness-work
pair: harness-sync
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Edit", "Bash", "Task", "ScheduleWakeup", "mcp__harness__harness_mem_resume_pack", "mcp__harness__harness_mem_record_checkpoint"]
argument-hint: "[all|N-M] [--max-cycles N] [--pacing worker|ci|plateau|night]"
user-invocable: true
---

# harness-loop

将 `/loop`（CC dynamic mode）与 `ScheduleWakeup` 结合，
对长时间任务 **在每次 wake-up 时以 fresh context 重新执行** 的元技能。

在各 wake-up 通过 Agent 调用 `harness-work --breezing`，
构成 1 cycle = 1 任务完结的可重新进入循环。

> **Long-session helpers (CC 2.1.108+)**:
> 人回来时用 `/recap` 重新获取摘要后再看 `/harness-loop status`。
> 较长的离席和频繁重新进入的操作优先使用 `ENABLE_PROMPT_CACHING_1H=1`。

> **长时会话推荐 (CC 2.1.108+)**:
> 会话长度预计超过 30 分钟时，在 plugin bundle root 解决后执行 `bash "${HARNESS_PLUGIN_ROOT}/scripts/enable-1h-cache.sh"` 以 opt-in 1 小时 prompt cache。

> **Codex 0.123.0 automatic bug fix inheritance**:
> manual shell follow-up queue 和 `/copy` after rollback 作为 Codex 本体的 TUI 修正自动继承。
> loop runner 不追加追加输入 queue、copy wrapper、rollback workaround。
> 长时间作业中向 manual shell 投递 follow-up 时的 queueing 委托给 Codex runtime。

## Quick Reference

| 输入 | 动作 |
|------|------|
| `/harness-loop all` | 循环执行所有未完成任务（default: max 8 cycles） |
| `/harness-loop all --max-cycles 3` | 3 cycles 后停止 |
| `/harness-loop 41.1-41.3 --pacing ci` | 以 CI pacing 执行任务范围 |
| `/harness-loop all --plan roadmap` | 对 named Plans 的 `roadmap` 进行循环执行 |
| `/harness-loop all --pacing night` | 深夜批处理（3600s 间隔） |
| `/harness-loop status` | 确认进行中 runner 的状态 |
| `/harness-loop stop` | 停止进行中 runner 的请求 |

## 选项

| 选项 | 说明 | 默认 |
|----------|------|----------|
| `all` | 所有未完成任务为目标 | - |
| `N-M` | 任务编号范围指定 | - |
| `--plan NAME` | 使用 `plans/manifest.json` 的 named plan | active/default |
| `--max-cycles N` | 最大 cycle 数 | `8` |
| `--pacing <mode>` | wake-up 间隔模式 | `worker`（270s） |

### pacing 值映射

| pacing | delaySeconds | 用途 |
|--------|-------------|------|
| `worker` | 270 | Worker 完成后（5 min 内 cache warm） |
| `ci` | 270 | CI 短时间作业等待 |
| `plateau` | 1200 | 20 min（plateau 检测后的重试间隔） |
| `night` | 3600 | 深夜长时间放置 |

> **约束**: `ScheduleWakeup` 的 `delaySeconds` 在运行时被 clamp 到 **[60, 3600]**。
> `worker` / `ci` 的 270s 以及 `night` 的 3600s 在此范围内。
> `plateau` 的 1200s 也在范围内。直接指定值时务必在 60 以上 3600 以下。

## 启动流程（每次 wake-up 的入口）

详细版: [`${CLAUDE_SKILL_DIR}/references/flow.md`](${CLAUDE_SKILL_DIR}/references/flow.md)

### plugin bundle root 解决

`harness-loop` 不调用 host project 的 cwd，而是调用 plugin bundle root 下的 helper script。
比喻来说，分开处理作业桌（host project）和工具箱（plugin bundle）。

各 wake-up 的开头按以下顺序决定 `HARNESS_PLUGIN_ROOT`:

1. 如果存在 `CLAUDE_PLUGIN_ROOT` 且包含 `scripts/` 则使用它
2. 如果没有 `CLAUDE_PLUGIN_ROOT`，则从 `CLAUDE_SKILL_DIR` 反推 plugin bundle root
   - `skills/harness-loop` 分发则为 `${CLAUDE_SKILL_DIR}/../..`
   - `.agents/skills/harness-loop` mirror 分发则为 `${CLAUDE_SKILL_DIR}/../../..`
3. 都无法解决时停止，设置 `CLAUDE_PLUGIN_ROOT` 为 plugin bundle root 后重新执行

`Plans.md` 和 `.claude/state/...` 放在 host project 侧。
仅从 `${HARNESS_PLUGIN_ROOT}/scripts/...` 调用 helper script。

有多个 Plans.md 的 repo，在启动长时 run 时明确 `--plan NAME`。
runner 在开始时解析的 Plans file 在 cycle 间保持，因此中途不切换 active plan。

```
wake-up
  │
  ▼
[Step 0] 将 plugin bundle root 解析为 HARNESS_PLUGIN_ROOT
  如果 CLAUDE_PLUGIN_ROOT 有效则使用
  否则从 CLAUDE_SKILL_DIR 反推 plugin bundle root
  ※ 不参照 host project cwd 的 scripts/
  │
  ▼
[Step 1] 先读取 Plans.md
  特定 cc:WIP / cc:TODO 的先头任务（获得 task_id）
  ※ 无未完成任务 → 循环结束（正常完成）
  │
  ▼
[Step 2] sprint-contract 存在确认 & 生成
  确认 .claude/state/contracts/${task_id}.sprint-contract.json 的有无
  无则通过 node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" ${task_id} 生成
  生成后（仅首次）: bash "${HARNESS_PLUGIN_ROOT}/scripts/enrich-sprint-contract.sh" <contract-path> \
    --check "wake-up 自动批准（harness-loop 用 DoD 以 reviewer 视点确认）" \
    --approve  ← draft → approved 升级
  （既有 contract 已 approved 因此跳过）
  │
  ▼
[Step 3] contract readiness 检查
  bash "${HARNESS_PLUGIN_ROOT}/scripts/ensure-sprint-contract-ready.sh" <contract-path>
  │
  ▼
[Step 4] Resume pack 重新读取
  harness-mem resume-pack（重新注入上下文）
  │
  ▼
[Step 4.5] Advisor 咨询（仅必要时）
  高风险任务首次执行前 / 同一原因 2 次失败后 / plateau 前
  组装 `advisor-request.v1` 进行咨询
  │
  ├── PLAN        → 下次 executor prompt prepend advice
  ├── CORRECTION  → 作为局部修正指示重新执行
  └── STOP        → 当场停止 loop，留下理由
  │
  ▼
[Step 5] 1 任务循环执行
  worker_result = Agent(
      subagent_type="claude-code-harness:worker",  # worker agent（非 harness-work）
      prompt="任务: ${task_id}\nDoD: <从 Plans.md 抽取>\ncontract_path: ${CONTRACT_PATH}\nmode: breezing",
      isolation="worktree",
      run_in_background=false
  )
  # worker_result: { commit, branch, worktreePath, files_changed, summary }
  │
  ▼
[Step 5.5] Lead 评审执行
  diff_text = git show worker_result.commit
  verdict = codex_exec_review(diff_text) or reviewer_agent_review(diff_text)
  ※ 详细参照 flow.md
  │
  ▼
[Step 5.6] APPROVE → cherry-pick 到 main / REQUEST_CHANGES → 修正循环（contract 的 max_iterations 次，默认 3）
  APPROVE: git cherry-pick → 将 Plans.md 更新为 cc:完成 [{hash}] → 删除 feature branch
  REQUEST_CHANGES x MAX_REVIEWS 后仍否决: 升级处理
  ※ 详细参照 flow.md
  │
  ▼
[Step 6] plateau 判定
  bash "${HARNESS_PLUGIN_ROOT}/scripts/detect-review-plateau.sh" ${current_task_id}
  │
  ├── PIVOT_REQUIRED（exit 2）  → 循环停止 + 用户升级
  ├── INSUFFICIENT_DATA（exit 1）→ 继续
  └── PIVOT_NOT_REQUIRED（exit 0）→ 继续
  │
  ▼
[Step 7] 循环数检查
  │
  ├── cycles >= max_cycles → 循环停止（到达上限）
  │
  ▼
[Step 8] checkpoint 记录
  harness_mem_record_checkpoint(
      session_id, title, content=循环结果摘要
  )
  │
  ▼
[Step 9] 下次 wake-up 预约
  ScheduleWakeup(
      delaySeconds=<pacing值>,
      prompt="/harness-loop <相同参数>",
      reason="循环 {N}/{max} 完成 — 下一任务"
  )
```

## 循环停止条件

| 条件 | 停止种类 | 对应 |
|------|---------|------|
| `cycles >= max_cycles` | 正常停止（到达上限） | 向用户报告 |
| `PIVOT_REQUIRED`（exit 2） | 异常停止（升级处理） | 请示用户判断 |
| 无未完成任务 | 正常停止（全部完成） | 输出完成报告 |

指定 `--max-cycles 3` 时在 3 循环完成后停止。
default（`--max-cycles 8`）时在 8 循环停止。

## 途中报告 / Silence Policy

长时 loop 中，途中报告不是"为安心的 heartbeat"而是"判断变化时的通知"。
Codex `0.123.0` 的 background agent 可接收 transcript delta 的环境中，不因仅 delta 到达而回应，不需要时明确沉默。

应报告的:

- cycle 完成、到达上限、全完成、blocked
- validation failure、review `REQUEST_CHANGES`、plateau、advisor `STOP`
- advisor / reviewer drift、contract readiness failure
- 用户求 `status` 时的摘要

可沉默的:

- 仅 transcript delta 增加，task / review / advisor 状态未变时
- 仅 log 中残留的细小 stdout 增加时
- 等待下次 wake-up 的 pacing 期间

default 为"1 cycle 最终报告 1 次"。
但 Advisor request 未响应、Reviewer result 未到达、plateau 前的警告优先于 silence policy 报告。

## 与 /loop 的协作

此技能与 CC 的 `/loop`（dynamic mode）组合使用。

有效 `/loop` 后 CC 持续自律重新进入执行，
各循环末尾调用 `ScheduleWakeup` 预约下次 wake-up。

`/loop` 的哨兵: `<<autonomous-loop-dynamic>>`

各 wake-up 以 **fresh context** 开始，防止前循环的上下文污染。
`harness-mem resume-pack` 的 resume pack 重新读取必须（Step 2）。

## checkpoint 记录

`harness_mem_record_checkpoint` schema:

```json
{
  "session_id": "<会话 ID>",
  "title": "harness-loop cycle {N}/{max}: {任务名}",
  "content": "cycle_result 的 1 行摘要 + commit hash"
}
```

## Advisor Strategy

此技能主角是 executor，advisor 仅在必要时调用。
打个比方，负责人平时自走，仅难点咨询老手。

咨询条件固定，不使用自然语言的"自信低"判定。

| 条件 | 是否咨询 |
|------|-----------|
| `needs-spike` / `security-sensitive` / `state-migration` | 咨询 |
| `<!-- advisor:required -->` | 咨询 |
| 同一原因 2 次失败 | 咨询 |
| plateau 停止前 | 咨询 |

同一 trigger 仅咨询 1 次。
该判定使用 `trigger_hash = task_id + reason_code + normalized_error_signature`。

## 相关技能

- `harness-work` — 各循环执行的任务实现技能
- `harness-plan` — 循环对象任务的计划
- `harness-review` — 个别任务的评审
- `session-control` — 会话状态管理
