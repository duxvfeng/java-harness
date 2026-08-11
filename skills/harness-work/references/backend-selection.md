# Execution Backend Selection — Full Detail

Role-scoped constraints, non-`claude` topology, self_review gate exception, cursor banner,
cherry-pick gate, natural-language trigger, and the two Mode 1 opt-in configurations for
`harness-work` / `breezing`.

## Role-scoped 约束

后端是 **role-scoped**。只有实现（worker）角色使用已解析的后端。
Reviewer 和 Advisor 两个角色始终固定为 brain（`--host claude`）。
不要将 primary reviewer 路由到 cursor / codex 后端（生成 diff 的同一上下文不得评审自己的输出 — spec.md Execution Backend Contract 的 self-review scope 约定）。
例外仅限 **fresh-context advisory pre-review**：不与生成 diff 的会话共享对话状态的 cursor `review` 层（composer-2.5-fast、read-only）允许在 brain 主评审之前输出 advisory findings。只有 brain 才能输出 primary verdict（`APPROVE | REQUEST_CHANGES`）。

```bash
```


## 非 `claude` 后端的拓扑结构（不经过 Worker）

当 backend 为 `codex` 或 `cursor` 时，**Lead 不会 spawn Worker agent (`claude-code-harness:worker`)**。

| backend | 路径 |
|---------|------|
| `claude`（默认） | Lead → Worker (`claude-code-harness:worker` agent) → … → Lead review → cherry-pick |

非 claude 后端在中间夹 Worker 会导致 Lead → Worker → companion → composer/codex 的二段委托，使 Worker 的存在意义（通过 agent 约定的 self_review 5 个关卡）落空（非 claude 不生成 `worker-report.v1` 也不生成 `self_review`）。Lead 跳过 Worker 直接调用 companion。


## 非 `claude` 后端的 self_review 关卡

当 backend 为 `codex` 或 `cursor` 时，不会生成 `worker-report.v1` 也不生成 `self_review` 数组。
因此 Lead **跳过** self_review 关卡，将 Lead 的 diff 评审作为唯一的质量关卡（与现有的 codex path 相同处理）。

## cursor 后端的 banner（委托前必须）

当 backend 为 `cursor` 时，Lead 在委托前必须输出以下 1 行 banner:

```
⚠️ cursor backend: model=composer-2.5-fast / R01-R13 防护规则不在 cursor-agent 内部应用 / 输出在 Lead 评审之前为 untrusted
```

cursor 的 write 委托在拥有专用 `.git` 的 worktree 内执行，Lead 将其 cherry-pick 到 main（在 cherry-pick 路径中应用 R01-R13）。
治理详情请参考 `.claude/rules/cursor-cli-only.md`。

## Lead 的 cherry-pick 前关卡（必须执行 contract grep）

将非 claude 后端 (cursor / codex) 的输出合并到 main 之前，Lead 必须通过**目视 diff + contract grep 的二段关卡**。不要仅通过目视 diff 就 APPROVE。

| 关卡 | 指令 | 可检测内容 |
|--------|----------|----------------|
| diff 目视 | `git show <sha>` | 变更是否符合预期·是否 touch 了其他文件·support tier 标注是否不变 |
| contract grep | `bash tests/test-support-claim-wording.sh` | 公开 support 标注的破坏 |
| contract grep | `bash scripts/ci/check-consistency.sh` | i18n / locale / mirror / capability matrix 的固定字符串约定破坏 |
| contract grep | `bash tests/validate-plugin.sh` | plugin 分发约定·hook 配线 |

**仅在全部 PASS 时才 cherry-pick**。如果有 1 个失败则 revert 或重新委托给 composer（明确要求保持同一字符串约定）。

理由: docs / README / locale / capability-matrix / spec.md 有 grep 监控的**固定字符串约定**（例: `README_ja.md` 的 `5动词工作流`）。composer 倾向于机械地减少表面的语言重复，即使目视 diff 看起来是"干净的 dedup"，也可能破坏固定句。

## 自然语言 backend trigger

当用户说 `composer` / `编辑器` / `在 Composer 中` / `composer 2.5` / `composer 模式` 时，作为 `cursor backend` 指定处理。
解析时作为显式 override 传递 `--backend cursor`，优先于 env / project / user file / default。

## Mode 1 — Producer → Sub-Lead → Composer 层级

通过 `harness work --team`（Breezing 的 Go orchestrator 路径）启用 **Mode 1 producer hierarchy** 的 opt-in 配线。正本在 `spec.md`「Mode 1 — orchestrated Producer hierarchy」节。实现: `go/internal/sublead/sublead.go`、`go/cmd/harness/work_team.go`。

| 层 | 角色 | 备注 |
|----|------|------|
| **Producer（Lead）** | 固定为 Claude Code。按 lane 委托给 Sub-Lead，并集约 `companion-result.v1` | 人类对话的 CLI = Lead |
| **Sub-Lead** | 将 1 个 lane 分解为 mini-plan，并并行 fan-out subtask | orchestrator-spawned **headless CLI**（与 Lead 同一 CLI backend） |

**仅 hub-spoke**：subWorker 之间不接受 peer results 或 channel。Sub-Lead 通过 inner `breezing.Orchestrator` fan-out，并将 lane 结果折叠为 1 个 `companion-result.v1`。

**启用**: `HARNESS_TEAM_HIERARCHY=sublead`（**默认 OFF**）。未设置时为 flat companion worker（Lead 对每个任务直接调用 companion 的传统路径）。

## review→iterate 循环

将 cross-CLI 的质量关卡包装到 worker 输出的 opt-in 配线。实现: `go/internal/reviewiterate/run.go`、`go/cmd/harness/work_team_reviewiterate.go`。

**启用**: `HARNESS_REVIEW_ITERATE=on`（**默认 OFF**）。`teamWorkerFactory` 用 `wrapWorkerWithReviewIterate` 包装 inner worker（flat companion 或 Sub-Lead 下的 subWorker）。

| 阶段 | 行为 |
|------|------|
| 1. advisory fan-out | 用多个 lens（例: correctness / security / scope）并行启动 **fresh-context** headless reviewer CLI（与 producing session 不共享对话状态） |
| 2. brain primary verdict | **primary verdict（`APPROVE` / `REQUEST_CHANGES`）仅由 brain（claude host / Lead）** 输出。advisory reviewer 仅输出 findings |
| 3. refinement re-dispatch | brain 如果 `REQUEST_CHANGES`，将 findings 折叠为精炼提示，用 inner `WorkerFunc` 重新投入**同一 worktree** |
| 4. 迭代上限 | 到达 `MaxIters` 未收敛 → `Outcome.Escalated=true` + `EscalationNote` 进行 human escalation |

**迭代上限 env**: `HARNESS_REVIEW_ITERATE_MAX`（未设置时 default `3`）。传递到 `reviewiterate.Config.MaxIters`。

cross-CLI review **迭代直到 OK**（DoD 未达标则将精炼任务重新投入同一 worktree，N 次未收敛则 human escalation）。
