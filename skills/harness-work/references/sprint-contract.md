# Sprint Contract & PR Closeout

## Sprint Contract

`sprint-contract` 是一个小契约文件，将"如何判定此任务合格"以人类和机器都能以相同含义理解的形式表示。默认保存位置为 `.claude/state/contracts/<task-id>.sprint-contract.json`。

```bash
node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" 32.1.1
```

生成物包含以下内容：

- `checks`: 分解 DoD 的确认项目
- `non_goals`: 本次不做的事项
- `runtime_validation`: test、lint、typecheck 等验证命令
  - 同一符号在同一会话 grep 2 次后切换到 `harness_ast_search`。
  - 有多模块相同实现的 bug 修复，编辑前用 `harness_ast_search` 洗出全部实现。
  - 变更文件包含 `.ts`/`.tsx` 时才将 `harness_lsp_diagnostics` 的新错误 0 件作为 DoD。harness MCP 未连接或对象外文件型时为 not-configured 处理、non-blocking。
- `browser_validation`: browser 评审应保留的 UI 流程验证项目
- `browser_mode`: `scripted` 或 `exploratory`
- `route`: browser 评审使用 `playwright` / `agent-browser` / `chrome-devtools` 中的哪一个
- `risk_flags`: `needs-spike`、`security-sensitive`、`ux-regression` 等
- `reviewer_profile`: `static`、`runtime`、`browser`

**必需元数据（lane / stage / evidence）** — 传递给 Worker / Scaffolder / Reviewer 的 sprint contract 输入:

| 字段 | 意义 | 例 |
|-----------|------|-----|
| `spec_path` | root `spec.md`（或最近的 sub-spec）路径 | `spec.md`、`docs/spec/00-project-spec.md` |
| `lane` | 任务的 lane taxonomy | `fast`、`gate`、`release` |
| `stage` | 5-stage gate 的当前阶段 | `research`、`plan`、`impl`、`review`、`closeout` |
| `research_evidence` | research 结果的 link / commit / file | `docs/research/phase-72-evidence.md`、commit hash |
| `tdd_red_log` | `[tdd:required]` 任务的 RED 证据（commit hash 或 log path） | `.claude/state/tdd-red-log/72.1.3.jsonl`、`abc1234` |
| `review_artifact` | review verdict 和 findings | `{ verdict: "APPROVE", findings: [...] }` |
| `pr_closeout` | closeout artifact（base/head refs + evidence pack） | `{ base_ref, head_ref, evidence_pack }` |

执行 `generate-sprint-contract.js` 时，Lead 将 `spec_path` / `lane` / `stage` 从 Plans metadata 载入 contract，research 完成后追加 `research_evidence`。TDD Red 后载入 `tdd_red_log`，review 后载入 `review_artifact`，PR closeout 后载入 `pr_closeout`。

**TDD 完成关卡**: `[tdd:required]` 任务只要 sprint contract 中没有 `tdd_red_log` 或显式 `skip_tdd_reason` 就不视为完成（`cc:完了` 更新·cherry-pick·PR closeout 全部适用）。

## PR Closeout（review APPROVE 后）

review APPROVE 后的 PR title/body 用 `bash "${HARNESS_PLUGIN_ROOT}/scripts/harness-pr-closeout.sh"` 从 evidence pack 构建。**默认为 `dry-run` preview**（`git push` / `gh pr create` 仅在 `push` 子命令 + 确认或 `--yes` 时）。

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/harness-pr-closeout.sh" build \
  --base origin/main --head "$(git branch --show-current)" \
  --evidence .claude/state/evidence-pack.json \
  --out .claude/state/pr-payload.json
bash "${HARNESS_PLUGIN_ROOT}/scripts/harness-pr-closeout.sh" dry-run --payload .claude/state/pr-payload.json
# 仅显式 push（必须确认、可用 --yes 跳过）:
bash "${HARNESS_PLUGIN_ROOT}/scripts/harness-pr-closeout.sh" push --payload .claude/state/pr-payload.json
```

禁止通过 `harness-review` 路径自动 push / PR / merge（read-only boundary）。detached HEAD 下 `push` 前需要创建 branch。

**Fast lane 轻量化边界**: `lane: fast` 可省略 full review，但不可省略 `not_observed != absent` 的 unknown data contract 和 focused checks（`runtime_validation` / `checks` 的 DoD 分解）。
