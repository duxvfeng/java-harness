# Lean Path & Phase Detail

Supplementary detail for `breezing`'s free-text brief composer, the Cursor lean
path savings breakdown, Phase 0 planning discussion, and dependency-graph task
assignment. The main `SKILL.md` keeps only the operational summary; read this
file when you need the full rationale.

## Brief Composer v0

`/breezing` 的 argument-hint（`all|N-M|--codex|--cursor|--reviewer-only|--parallel N|--no-commit|--no-discuss|--auto-mode`）的**都不匹配的自由文本输入**用的分解・确认流程。

1. **分类** — Lead 执行 `bash scripts/breezing-brief.sh classify "<args>"`。
   - 输出 `structured` → 现有的 structured 参数路径（Quick Reference）直接进入。
   - 输出 `free-text` → 进入下一步。
2. **分解** — Lead 的 LLM 将自由文本分解为 **3〜7 个 subtasks**，构建 `brief-card.v1` JSON 卡片（schema: `templates/schemas/brief-card.v1.json`）。v0 中 `breezing-brief.sh` 不调用 LLM。
3. **提示** — 向用户提示卡片（goal / subtasks[id,title,dod] / scope_files / risk_notes / confidence）。`confidence` 是 `high` | `medium` | `low` 中的任一个。
4. **确定** — 用户 Yes/No 后，执行 `bash scripts/breezing-brief.sh confirm <yes|no> <card.json>`。
   - `yes` → 输出 `DISPATCH: <subtask 数>`，传递给现有的 team 路径（worktree-per-task）。
   - `no` → `DISPATCH: 0`（执行 0 件的 dry 契约）。

仅需验证时: `bash scripts/breezing-brief.sh validate <card.json>`（exit 0 = valid）。

## Cursor Fast Path — 被删除的 step（相比 claude backend 节省）

| Step | 删除理由 | 节省秒数 |
|---|---|---|
| `claude-code-harness:worker` agent spawn | cursor backend 不经过 Worker | 5-30s |
| self_review 5 件 gate | cursor 不生成 `worker-report.v1`，因此不需要 | 10-60s × retry |
| sprint-contract 3 段链条 (generate→enrich→ensure) | 不需要 Worker 契约就不需要 contract | 2-5s × N |
| Phase 0 Q1-Q3 interactive | `--no-discuss all` 默认 (Plans/Depends 由 Lead 直接读取) | 15-30s |
| Effort 评分 | cursor backend 不需要 ultrathink 注入 | 0.5-1s × N |
| Plans.md re-parse (per task) | session 内缓存 (mtime+hash 短路) | 3-8s |

总计 baseline `15-35s` → target `3-7s`，缩短到开始第 1 个任务的 cursor 委托。

## Reviewer-only mode 的用途

- Anthropic 侧 server rate limit 导致 Reviewer 停止时，提前收集 advisory findings（不能代替 brain verdict — verdict 在 brain 复活后确定）
- Worker 已完成，只有 Reviewer 分散到其他系统
- Codex review auth 失败时的 manual fallback

## Phase 0: Planning Discussion（结构化 3 问检查・详细）

在执行所有任务前，通过以下 3 个问题确认计划的健全性。指定 `--no-discuss` 时全部跳过。设计为 30 秒内完成。

**Q1. 范围确认**: 「将执行 {{N}} 个任务。范围合适吗？」
太多时按优先度（Required > Recommended > Optional）提议缩小。

**Q2. 依赖关系确认**（仅当 Plans.md 有 Depends 列时）: 「任务 {{X}} 依赖于 {{Y}}。执行顺序正确吗？」
读取 Depends 列，显示依赖链。有循环依赖时报错。

**Q3. 风险标志**（仅当有 `[needs-spike]` 任务时）: 「任务 {{Z}} 是 [needs-spike]。先做 spike 吗？」
有未完成的 `[needs-spike]` 任务时，确认是否先行执行 spike。

3 个问题都没问题时，进入 Phase A。

## 基于依赖图的任务分配（详细）

当 Plans.md 有 Depends 列时（v2 格式），按依赖图执行任务:

1. 先执行 **Depends 为 `-` 的任务**。有多个独立任务时可并行 spawn
2. 各 Worker 完成后，Lead 进行 review→cherry-pick（参照 `harness-work` Phase B）
3. 依赖源任务被 cherry-pick 到 main 后，执行依赖于该任务的任务
4. 重复直到所有任务完成

各任务的"Worker 完成→review→cherry-pick"是逐次处理。只能并行化独立任务（Depends 为 `-`）的 Worker spawn 部分。
