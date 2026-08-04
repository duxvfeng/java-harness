---
name: harness-loop
description: "HAR: Codex-native long-running loop runner. Uses a real background runner that executes one ready batch per cycle through Breezing by default, with status/stop controls. Trigger: long-running, loop, autonomous, background, Codex. Do NOT load for: one-shot implementation, normal review, release."
description-en: "HAR: Codex-native long-running loop runner. Uses a real background runner that executes one ready batch per cycle through Breezing by default, with status/stop controls. Trigger: long-running, loop, autonomous, background, Codex. Do NOT load for: one-shot implementation, normal review, release."
description-ja: "HAR：Codex 专用长时循环执行。实际后台 runner 通过 Breezing 推进 ready batch，可用 status/stop 监视。当用户提到长时、循环、自主、后台、Codex 时启动。"
description-zh: "HAR：Codex 原生长时循环执行器。使用实际的后台运行器，默认通过 Breezing 每个周期执行一个就绪批次，支持状态/停止控制。当用户提到长时间运行、循环、自主、后台、Codex 时启动。不适用于：一次性实现、普通审查、发布。"
kind: workflow
purpose: "Run long-lived Codex ready-batch execution loops"
trigger: "long-running, loop, autonomous, background, Codex"
shape: delegate
role: orchestrator
base: harness-work
pair: harness-sync
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Bash"]
argument-hint: "[all|TASK|START-END|START..END] [--max-cycles N] [--max-workers N|max] [--executor breezing|task] [--pacing worker|ci|plateau|night]"
disable-model-invocation: true
---

# Harness Loop

Codex 版的 `harness-loop` 不是仅作说明的伪循环，
而是启动**实际在后台运行的 runner**。

## 一句话来说

`$harness-loop` 不是一次性实现请求，
而是启动"将通过 Breezing 自动执行当前可执行的未完成任务群"的值班入口。

这里的 `ready batch` 指的是满足 Depends、当前可并行执行的 `cc:TODO` / `cc:WIP` 群。
1 cycle 不是 1 个 task，原则上处理 1 ready batch。

## 比喻来说

代替人在旁边一直监视，
让"找到可同时进行的工作 → 委托给 Breezing → 确认结果 → 进入下一群"的监督员常驻后台。

## Quick Reference

| 输入 | 动作 |
|------|------|
| `$harness-loop all` | 开始所有未完成任务的长时循环 |
| `$harness-loop 41.1-41.4` | 缩小范围开始 |
| `$harness-loop JLB3R-02..JLB3R-08` | 按 Plans.md 的 task ID 顺序缩小范围开始 |
| `$harness-loop all --max-cycles 3` | 最多 3 个周期后停止 |
| `$harness-loop all --max-workers 4` | 1 cycle 的 ready batch 限制为最多 4 个 worker |
| `$harness-loop all --max-workers max` | 将 ready batch 内可执行的任务数作为上限并行化 |
| `$harness-loop all --plan roadmap` | 对 named Plans 的 `roadmap` 进行循环执行 |
| `$harness-loop all --executor task` | 逃逸到传统 1 task per cycle local worker 执行 |
| `$harness-loop all --pacing night` | 加长周期间的等待 |
| `$harness-loop status` | 确认当前执行状况 |
| `$harness-loop stop` | 停止进行中的 job 并发出循环停止请求 |

## 执行命令

### 开始

```bash
harness codex-loop start all
```

范围指定:

```bash
harness codex-loop start 41.1-41.4 --max-cycles 5 --pacing worker
harness codex-loop start JLB3R-02..JLB3R-08 --max-cycles 5 --pacing worker
harness codex-loop start all --max-workers max --pacing worker
harness codex-loop start all --plan roadmap --max-cycles 5
harness codex-loop start all --executor task --max-cycles 5
```

`START..END` 是直接使用 `Plans.md` 中并列的 task ID 的范围指定。
包含英文字母或连字符的 task ID 优先使用 `..`。
像 `41.1-41.4` 这样的传统数值范围也可以继续使用。

`--max-workers` 是 Breezing 在 1 cycle 中同时运行的 worker 数上限。
`max` 意味着将选择范围内满足 Depends 的 ready task 数作为上限。
`--executor task` 是兼容用的逃逸手段，只向 local worker 传递 1 task。
用于问题切分，或不想并行执行的危险作业。
有多个 Plans.md 的 repo，在启动长时 run 时需要明确 `--plan NAME`。
runner 在开始时解析的 Plans file 在 cycle 间保持，因此中途不切换 active plan。

### 状态确认

```bash
harness codex-loop status
harness codex-loop status --json
```

### 停止

```bash
harness codex-loop stop
```

## 如何运作

1. 将 Harness loop 的执行状态写到 project root 的 `.claude/state/codex-loop/`
2. 从 Plans.md 正则化接收到的 selection
3. 从 Plans.md 收集满足 Depends 的 `cc:TODO` / `cc:WIP`，创建 ready batch
4. 用 `--max-workers` 限制 ready batch 的同时执行数
5. 默认 Breezing executor 以 Lead / Worker / Reviewer 分离执行 ready batch
6. 仅在 `--executor task` 时，兼容用 local worker 以 1 task per cycle 启动 `codex exec`（仅在 `CODEX_LOOP_TASK_DRIVER=companion` 时使用 `bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --background --write ...`）
7. 在高风险 task / 2 次失败 / plateau 前插入 advisor consult
8. ready batch 完成后进行 review / checkpoint / plateau 判定
9. 如果还有剩余目标任务，等待后进入下一周期

## Realtime Handoff / Silence Policy

Codex `0.123.0` 以后的 background agent 可以通过 realtime handoff 接收 transcript delta。
此 delta 是"情况掌握用的追记"，不是每次都要向用户回答的信号。

一句话来说：background agent 只在必要时报告，当没有任何判断变化时明确沉默。

比喻来说，监视员不是在走廊一直实况转播，而是只在异常・完成・等待判断时通知的形式。

可以报告的时机：

- loop 开始、停止、`already running`、`stop` 受理等与用户操作相关的 lifecycle 边界
- 1 ready batch cycle 的最终结果、commit、`RESULT: APPROVED` / `RESULT: BLOCKED`
- Breezing Lead 将 task 完成作为 progress feed 汇总输出时
- task 被 blocked、validation failure、review `REQUEST_CHANGES`、plateau、advisor `STOP` 停止时
- 用户执行 `status` 时，或明确询问中途状况时
- advisor / reviewer drift、contract readiness failure 等，放置会导致质量判定偏离时

沉默的时机：

- 仅接收了 transcript delta、task / review / advisor 状态没有变化时
- 仅增加了已留在 `runner.log` / `jobs/*.log` 的细微 stdout
- `pacing` 等待中，到下一个 cycle 没有新判断材料时

中途报告的频率：

- 默认是"1 ready batch cycle 仅最终报告 1 次"。
- Breezing 的 task-level progress feed 仅在 batch 内完成数变化时输出。
- 即使是长 cycle，只要没有 material state change 就不输出 heartbeat。
- 详细流程交给 `harness codex-loop status --json` 和 project root 的 `.claude/state/codex-loop/runner.log`，会话侧只输出要点。

Advisor / Reviewer drift 的关系：

- silence policy 不是为了减弱 drift 检测。
- `advisor-request.v1` 没有响应、`review-result.v1` 不返回、contract 未批准等异常必须留在 state / log 中，必要时向用户报告。
- Advisor 保持 `PLAN` / `CORRECTION` / `STOP` 的咨询角色、Reviewer 保持最终质量判定角色的分离。

## pacing

| 值 | 用途 | 等待秒数 |
|----|------|---------|
| `worker` | 通常开发循环 | 270 |
| `ci` | 想短一点确认时 | 270 |
| `plateau` | 卡住感的重试 | 1200 |
| `night` | 长放置执行 | 3600 |

## State Path Policy

Codex 版 `harness-loop` 分开处理 Codex 原生的会话・执行缓存和 Harness 共享的 project state。

- **Harness 共通 state**: 放在 project root 的 `.claude/state/` 下。与 Claude 侧的 advisor / review / checkpoint 共享，`harness codex-loop status` 也读取这里。
- **Codex loop runner state**: 放在 project root 的 `.claude/state/codex-loop/` 下。这不是"Codex 全体的正本"，而是 Harness loop runner 的 job / cycle / log 用 state。
- **Codex native state**: 留在 `${CODEX_HOME:-~/.codex}` 下的 Codex 自身的 thread / transcript / cache。不作为 Harness loop 的 task status、advisor history、review result 的正本。
- **禁止**: 不将 `.Codex/` 或 `~/.Codex` 作为正本 path 指引。大写 `Codex` 目录视为 historical drift。

也就是说，`.claude/state/codex-loop/` 是"此 project 的 Harness loop state"，不是 Codex native state 全体的固定保存处。

## 状态文件

以下都是以 project root 为基准。

- `.claude/state/codex-loop/run.json`
- `.claude/state/codex-loop/cycles.jsonl`
- `.claude/state/codex-loop/runner.log`
- `.claude/state/codex-loop/current-job.json`
- `.claude/state/codex-loop/jobs/*.json`
- `.claude/state/codex-loop/jobs/*.log`
- `.claude/state/codex-loop/jobs/*.out`
- `.claude/state/advisor/history.jsonl`
- `.claude/state/advisor/last-request.json`
- `.claude/state/advisor/last-response.json`
- `.claude/state/locks/codex-loop.lock.d`

## Advisor Consult

Advisor 不是"代替实现的角色"，而是"只返回下一步的咨询角色"。
loop 中只在以下 3 个地方调用。

| 时机 | reason_code | 做什么 |
|-----------|-------------|-----------|
| 高风险 task 的首次执行前 | `high-risk-preflight` | 先听取要固定的观点 |
| 同样原因的 2 次失败后 | `retry-threshold` | 听取方针变更还是局部修正 |
| 因 plateau 停止前 | `plateau-pre-escalation` | 听取是否真的应该停止 |

decision 只有 3 种。

| decision | loop 的处理 |
|----------|-------------|
| `PLAN` | 将 advice 加到下一个 executor prompt 开头重新执行 |
| `CORRECTION` | 作为局部修正指示重新执行 |
| `STOP` | 停止 loop，将理由留在 state 和 runner.log |

同一个 trigger 用 `trigger_hash = task_id + reason_code + normalized_error_signature` 只咨询 1 次。
每个 task 的咨询次数最多 3 次，以上则提升到用户判断。

## 注意点

- 这是 **真正在后台运行**。不是仅返回说明的技能。
- 不能同时启动 2 个。已在运行时以 `already running` 停止。
- 默认 executor 是 Breezing。只在需要传统 1 task per cycle 行为时使用 `--executor task`。
- 基本不在当场停止并将理由留在那里，而不是强行跳过失败的任务进入下一个。
- 通过看 `status` 和 `runner.log`，容易追踪当前停在哪里。

## 具体例子

想在"今天自动运行 Phase 41 的剩余任务"的话：

```bash
harness codex-loop start 41.1-41.4 --max-cycles 8 --max-workers max --pacing worker
```

中途查看状况：

```bash
harness codex-loop status
```

到晚上想停止时：

```bash
harness codex-loop stop
```

## 为什么采用这个形式

Codex 无法直接使用 Claude 的 `/loop` 相同的 wake-up 机制。
取而代之，以 **Codex loop runner** 为基础，
Harness 侧持有状态管理和再入控制，实际作业委托给 Breezing 的 batch 执行。
这样，即使是长时任务，"停止"、"重新开始"、"看当前状态"也能变得自然，
只安全地汇总推进满足依赖关系的工作。
