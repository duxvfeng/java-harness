---
name: breezing
description: "Team execution mode — backward-compatible alias for harness-work with team orchestration. Composer/composer 2.5 maps to the cursor backend."
description-ja: "チーム実行モード — harness-work のチーム協調エイリアス。breezing, チーム実行, 全部やって, composer, コンポーザー, composer 2.5 でトリガー。"
description-zh: "团队执行模式 — harness-work 的团队协调别名。当用户提到 breezing、团队执行、全部完成、composer、作曲器、composer 2.5 时触发。"
description-en: "Team execution mode — backward-compatible alias for harness-work with team orchestration. Composer/composer 2.5 maps to the cursor backend."
kind: workflow
purpose: "Wrap harness-work with team execution orchestration"
trigger: "breezing, team execution, do everything, composer, composer 2.5, composer mode, コンポーザー"
shape: wrap
role: orchestrator
base: harness-work
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Bash", "Grep", "Glob", "Task", "WebSearch", "Monitor"]
argument-hint: "[all|N-M|--codex|--cursor|--reviewer-only|--parallel N|--no-commit|--no-discuss|--no-review-gate|--auto-mode]"
user-invocable: true
---

# Breezing — Team Execution Mode

> **向后兼容别名**: 以团队执行模式运行 `harness-work`。

## Default Pipeline（一个命令完成 plan → work → review → report）

`/breezing` 在单次启动中完成「计划 → 实现 → 审查直到 OK → 报告」。
operator 无需单独指示 `/harness-plan` 或 `/harness-review`（operator 裁定 2026-07-24）。

1. **Plan gate**: 如果 Plans.md 中没有对应请求范围的 task，或 task 不足，先执行 `harness-plan` 生成 task 后再继续。已有 plan 时直接进入 Phase 0。plan 生成时的范围遵循 harness-plan 的「范围既定: 当前可进行的所有工作」。
2. **Work**: 现有的 Phase 0 → A → B（含 per-task review）。
3. **Integrated Review Gate（Phase D，默认 ON）**: Phase B 完成后，**在 Phase C 最终化（完成报告・run 完成声明）之前**，对 run 全体的 diff 执行 `harness-review`。
   - review target: 通常 run 为 `{base_ref}..HEAD`。`--no-commit` run 的 commit range 可能为空，因此**以 working tree（未 commit 变更 + untracked 文件）为对象**
   - fresh-context 的独立 reviewer subagent（与实现 Worker 不共享会话状态）与 `bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" review --base "${base_ref}"` 的 second opinion 并行
   - 任一为 REQUEST_CHANGES 相当 → 修正 → 再审查。**重复直到 APPROVE**（最多 3 次）。未收敛时将影响 task 的 marker 恢复为 `cc:WIP`，通过 human escalation 停止并报告 findings 和修正情况
   - primary verdict（`APPROVE | REQUEST_CHANGES`）由 brain（claude host）发出。维持 role-scoped 约束
4. **Finalize + Report（Phase C）**: 获得 gate 的 APPROVE 后确定 Plans.md 更新・commit・完成报告。不得在未通过 gate 时将 run 报告为「完成」。最终报告按 easy 作法输出（host session 有 `easy` skill 时 invoke 并遵循其作法，无则按 `harness-work` 的 Completion Report 模板）。

`--reviewer-only` / `--no-commit` 等现有标志作为仅运行该 pipeline 对应阶段的 per-run override。
低风险高速 run 想省略 Phase D 时传递 `--no-review-gate`（Phase B 的 per-task review 不会被省略。仅省略对 run 全体 diff 的集成审查）。

## Narration Rules (UX Contract)

敌人是 **冗长** 而非进度报告。**启动时简洁明确执行计划后开始执行**。欢迎易读的进度报告。仅禁止冗长重复・无实质内容的前言。

### 启动时必须输出的内容 (banner + plan，合计 5 行以内)

首次响应时，展示做什么・按什么顺序进行后进入 tool 执行:

```
🚀 cursor / composer-2.5-fast / feat/hah-11-golden-rule-lint / Reviewer
接下来:
1. resolve backend/model
2. 向 composer 委托 advisory findings (read-only)
3. brain 一次审查确定 verdict → 3-5 行摘要 → Plans.md 更新
```

banner 1 行 (`🚀 <backend> / <model> / <branch> / <task>`) + 计划 2-4 行。1 秒内输出，立即进入 Step 1。

### Backend 既定和 per-run 的扁平判断（2026-07-24 operator 裁定）

既定 backend 为 **`claude`（Native subagent）**。resolver 的未设定 fallback 也是 `claude`，这是预期而非陷阱。
⚠️ 警告仅在 resolver 输出 **不正当值 fallback** 的 stderr 警告时在 banner 后立即输出 1 行（正常解析为 `claude` 时不输出。同一 run 内不重复）。

Lead 按 run 单位，从作业内容・量扁平选择 backend。选择时使用对 resolver 的明确 override（`--backend <v>` / `--codex` / `--cursor`）。继续禁止直接读取 env:

| 作业性质 | 推荐 backend | 理由 |
|---|---|---|
| 通常的实现・修正・测试（既定） | `claude` (native) | Worker 契约（`worker-report.v1` / self_review 5 件）全部有效 |
| 大规模高独立性批量实现、回避 Claude 侧 rate limit | `codex` | 可向 deep tier 的 xhigh 委托（model 由 `model-routing.sh` 解析） |
| UI 大量生成、lean 高速委托 | `cursor` | lean path（worktree 隔离 + Lead diff review） |

模型 ID 不写在 skill 中。`bash "${HARNESS_PLUGIN_ROOT}/scripts/model-routing.sh" --host <backend> --role worker` 是正本。

### 可以输出进度报告 (易读范围内)

- 各步骤的开始・完成以 1 行状态 (`✓ backend=cursor / model=composer-2.5-fast`)
- 判断所需的中間结果 (pre-check 要点、resolved model、检测出的 branch 等)
- 为何采取此分支的理由以 1 行 (例: 「仅委托 Reviewer: Worker 已在其他系統完成」)

### 禁止 (= 冗长)

- **同一事实的 2 次表述**: 不在后段重复说明已说过的事
- **无实质内容的前言**: 「确认用法」等仅行、tool call 自明的声明
- **3 行以上的経緯回顾**: 拉长结论的长前言。需要経緯时压缩为 1 行
- **启动序列中的 ★ Insight 块**: Insight 仅在最終 report 中出现 1 次

例 (违规 → 正常):
```
× 「composer 2.5 使用模式」= 以 cursor backend 委托 Composer（解释的重复、无实质内容的前言）
○ 🚀 cursor / composer-2.5-fast / feat/hah-11-golden-rule-lint / Reviewer
  接下来: backend resolve → 向 composer 委托 advisory findings (read-only) → brain 一次审查确定 verdict
```

## Quick Reference

```bash
/breezing                       # 询问范围（claude backend）
/breezing all                   # 完成所有任务（claude backend）
/breezing 3-6                   # 完成任务3-6
/breezing --codex all           # 通过 Codex CLI 委托所有任务
/breezing --cursor              # cursor backend lean path（默认 --no-discuss all）
/breezing --cursor --reviewer-only  # 仅向 cursor 委托 Reviewer（Worker 已在其他系統完成）
/breezing composer 2.5 all      # 自然语言 trigger: 作为 cursor backend 处理
/breezing --parallel 2 all      # 2并行完成所有任务
/breezing --no-discuss all      # 跳过计划讨论完成所有任务
/breezing --auto-mode all       # 在兼容的父会话中尝试 Auto Mode rollout
```

## Brief Composer v0

不匹配 argument-hint 任一项的自由文本输入通过 `bash scripts/breezing-brief.sh classify "<args>"` 判定为 `structured` / `free-text`。
`free-text` 分解为 3-7 个 subtasks 的 `brief-card.v1` 卡片并提示给用户，通过 `breezing-brief.sh confirm <yes|no> <card.json>` 确定。
分解逻辑・schema・`DISPATCH` 契约详情参见 [references/lean-path-detail.md](${CLAUDE_SKILL_DIR}/references/lean-path-detail.md)。

## Options

| Option | Description | Default |
|--------|-------------|---------|
| `all` | 以所有未完成为对象 | - |
| `N` or `N-M` | 指定任务编号/范围 | - |
| `--codex` | 向 Codex CLI 委托实现 | false |
| `--cursor` | cursor backend lean path（per-run 明确 override。与 resolver 输出为 `cursor` 时同等）。跳过 Worker 介在 / self_review / sprint-contract 3段链 / Phase 0，3 秒内开始启动 → 委托 | false |
| `--reviewer-only` | 仅向 Reviewer 委托独立系統（以 Worker 实现已完成前提）。与 `--cursor` 并用向 Composer 逃逸 | false |
| `--parallel N` | Implementer 并行数 | auto |
| `--no-commit` | 抑制自动提交 | false |
| `--no-discuss` | 跳过计划讨论 | `--cursor` 时默认 true |
| `--no-review-gate` | 跳过 Phase D（Integrated Review Gate）。维持 Phase B 的 per-task review | false |
| `--auto-mode` | 明确 Harness 侧的 Auto Mode rollout。与 CC 2.1.111 不再需要的 `--enable-auto-mode` 不同 | false |

## Natural Language Backend Triggers

`composer` / `コンポーザー` / `Composer で` / `composer 2.5` / `composer モード` 正式作为 `cursor backend` 的 trigger 处理。
这是相当于 `--cursor` 的 intent，Lead 通过 `resolve-impl-backend.sh` 确定 backend。
解析时作为明确 override 传递 `--backend cursor`，优先于 env / project / user file / default。

| 输入例 | 解释 | 执行路径 |
|---|---|---|
| `composer 2.5 で` | `cursor backend` | Lead → `cursor-companion.sh task --write --workspace <wt>` |
| `コンポーザーで全部` | `cursor backend` | Lead → `cursor-companion.sh task --write --workspace <wt>` |
| `composer モード` | `cursor backend` | Lead → `cursor-companion.sh task --write --workspace <wt>` |

`composer` は Claude Worker の内側に spawn する追加 agent ではない。
非 `claude` backend のトポロジーに従い、Lead が Worker agent を挟まずに `cursor-companion.sh` を直接呼ぶ。

> **CC 2.1.111 note**:
> Opus 4.7 では literal に `/effort xhigh` が使える。
> built-in `/ultrareview` は明示要求時だけ追加で使い、既定レビューは置き換えない。

> **長時間セッション推奨 (CC 2.1.108+)**:
> セッション長が 30 分を超える見込みの場合、plugin bundle root 解決後に
> `bash "${HARNESS_PLUGIN_ROOT}/scripts/enable-1h-cache.sh"` を実行して 1 時間 prompt cache を opt-in すること。
> このスクリプトは `env.local` に `export ENABLE_PROMPT_CACHING_1H=1` を追記する (冪等)。
> 5 分 TTL の既定キャッシュでは breezing の 1 時間超セッションで cache miss が累積し
> input token コストが最大 12 倍になりうるため、長時間 team 実行では明示的に opt-in する。
> Codex CLI 子プロセス (`scripts/codex-companion.sh task --write` 等) は通常 env 継承で
> `ENABLE_PROMPT_CACHING_1H` を読むが、`CLAUDE_CODE_SUBPROCESS_ENV_SCRUB=1` が有効な場合は
> 明示的に export を維持する shell wrapper が必要。詳細は
> [`docs/long-running-harness.md`](../../docs/long-running-harness.md) を参照。

## Execution

**このスキルは `harness-work` に委譲します。** 以下の設定で `harness-work` を実行してください:

1. **引数をそのまま `harness-work` に渡す**
2. **チーム実行モードを強制** — Lead → Worker spawn → Reviewer spawn の三者分離
3. **Lead は delegate 専念** — コードを直接書かない
4. **Auto Mode は opt-in 扱い** — `--auto-mode` は互換な親セッションでの rollout 用フラグとして受け付ける
5. **Advisor は必要時のみ** — Worker が `advisor-request.v1` を返した時だけ Lead が advisor を呼ぶ

### Plan-time 事前確認の扱い

Breezing run 開始時は、Lead が `harness-work` と同じ preapproval preflight を実行する。

- 各 task の開始時、task worktree の `.claude/state/active-task.json` に `{"phase":"<phase>","task":"<task>"}` を原子的に書く。task 終了時は成功、失敗、停止の全経路で削除する。
- `.claude/state/plan-preapprovals.json` があれば `scripts/plan-preapproval.sh validate` で v2 を validate する。v1 は既存記録の読み取り互換として受け付ける。
- 実行対象 task の `decision: approved` 事項だけを宣言済みとして扱い、Worker briefing に渡す。
- `secret-read` は `bash "${HARNESS_PLUGIN_ROOT}/scripts/plan-preapproval.sh" apply-secret-allow "$PROJECT_ROOT"` で project config `.claude-code-harness.config.json` の `runtimefloor.secretAllow` に per-run 反映し、108.2 の project config floor と接続する。
- R12 の `ask` は、同じ phase/task、期限内、使用回数内で、`external-send` と実行コマンドが一致する v2 承認だけが抑制する。明示 `deny` と runtime floor は抑制しない。
- 宣言済み事項では途中停止せず、work 中の宣言済み事項起因 `AskUserQuestion` はゼロにする。確認は plan 承認時の 1 回のみ。
- 記録に無い未計画の secret-read / 外部送信 / 破壊的操作は従来どおり runtime floor / ask で停止する。安全網を狭めない。

### `harness-work` との違い

| 特徴 | `harness-work` | `breezing` (このスキル) |
|------|-----------------|------------------------|
| 並列手段 | 必要数に応じた自動分割 | **Lead/Worker/Reviewer の役割分離** |
| Lead の役割 | 調整+実装 | **delegate (調整専念)** |
| レビュー | Lead 自己レビュー | **独立 Reviewer** |
| デフォルトスコープ | 次のタスク | **全部** |

### Team Composition

| Role | Agent Type | Mode | 責務 |
|------|-----------|------|------|
| Lead | (self) | - | 調整・指揮・タスク分配 |
| Worker ×N | `claude-code-harness:worker` | `bypassPermissions`（現行） / Auto Mode（follow-up）* | 実装 |
| Advisor | `claude-code-harness:advisor` | 読み取り専用 | 方針助言 (`PLAN` / `CORRECTION` / `STOP`) |
| Reviewer | `claude-code-harness:reviewer` | `bypassPermissions`（現行） / Auto Mode（follow-up）* | 独立レビュー |

> *親セッションまたは frontmatter が `bypassPermissions` の場合はそちらが優先される。配布テンプレートは現在も `bypassPermissions` を使うため、Auto Mode は follow-up の rollout 対象であり、既定挙動ではない。

### Mode 1 — Sub-Lead 階層と review→iterate（opt-in）

Go orchestrator 経路（`harness work --team`）では、Breezing の Lead/Worker/Reviewer 三者分離に加えて **Producer → Sub-Lead → Composer** の Mode 1 階層を opt-in で重ねられる。Lead（Producer = Claude Code 固定）が lane を Sub-Lead に委譲し、Sub-Lead は orchestrator-spawned headless CLI（Lead と同一 backend）で mini-plan を組み、実装は Composer 2.5（cursor backend）が `companion-result.v1` で lane 単位に集約する。`HARNESS_TEAM_HIERARCHY=sublead` で有効化（default OFF）。

品質面では `HARNESS_REVIEW_ITERATE=on` で worker 出力を review→iterate ループで wrap できる。fresh-context 並列 advisory + cross-CLI review のあと **brain-only verdict** を経て、DoD 未達なら同 worktree へ精緻化タスクを再投入し、**OK まで反復**する（`HARNESS_REVIEW_ITERATE_MAX` で上限、未収束は human escalation）。配線・契約の詳細は `harness-work` の「Mode 1 — Producer → Sub-Lead → Composer 階層」「review→iterate ループ」節を正本とする。

- If you grep the same symbol twice in the same session, switch to harness_ast_search.
- For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
- Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.

### Codex Mode (`--codex`)

公式プラグイン `codex-plugin-cc` 経由で Codex CLI にすべての実装を委託するモード:

```bash
# タスク委託（書き込み可能）
bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write "タスク内容"

# stdin 経由（大きなプロンプト向け）
CODEX_PROMPT=$(mktemp /tmp/codex-prompt-XXXXXX.md)
# タスク内容を書き出し
cat "$CODEX_PROMPT" | bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write
rm -f "$CODEX_PROMPT"
```

### Execution Backend (persistent)

backend 判定は **必ず resolver 経由**。`HARNESS_IMPL_BACKEND` env を直接読んで backend を決めてはならない。
env / `--cursor` per-run flag / project `env.local` / user file を
`bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh"` で precedence 解決し、その出力を backend として使う
（env unset でも project / user file から拾える）。永続 default を変えたい場合は
`bash "${HARNESS_PLUGIN_ROOT}/scripts/set-impl-backend.sh" <claude|codex|cursor> [--user]` で project / user file に書き込み、
run 開始時に resolver が解決する（現行の operator 既定はユーザースコープで `claude`）。review / advisor ロールは brain に固定したまま。
バックエンド選択の正本（precedence、role-scope、self_review スキップ、cursor banner）は
`harness-work` の「Execution Backend Selection（実装バックエンド選択）」を参照する。

下の Cursor Backend Fast Path は per-run フラグ (`--cursor`) を resolver への明示 override として lean path を有効化する別軸であり、本節と併読する。

### Cursor Backend Fast Path (`--cursor` / lean mode)

`bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh"` の出力が `cursor` のときに有効
（`--cursor` は resolver への明示 override として precedence 最上位）。Worker 層を介在させず Lead が直接 `cursor-companion.sh` を呼ぶ（Phase 85 SSOT、`.claude/rules/cursor-cli-only.md` Topology 節）。

cursor backend は Worker agent spawn / self_review 5 件ゲート / sprint-contract 3 段チェーン / Phase 0 interactive / effort スコアリングを省略し、baseline `15-35s` → target `3-7s` で 1 タスク目の委譲を開始する。節約内訳の全表は
[references/lean-path-detail.md](${CLAUDE_SKILL_DIR}/references/lean-path-detail.md) を参照。

#### 既定 flow（cursor backend）

1. **banner + 実行計画** (`🚀 cursor / <model> / <branch> / <task>` + これから進める 2-4 step、合計 5 行以内、1 秒以内)
2. **1 bash で並列 pre-check**: `git branch --show-current` + `cat VERSION` + `Plans.md tail` + `cursor-agent --version`
3. **1 bash で resolve**: `bash "${HARNESS_PLUGIN_ROOT}/scripts/resolve-impl-backend.sh"` + `bash "${HARNESS_PLUGIN_ROOT}/scripts/model-routing.sh" --host cursor --role worker --field model`
4. **即 委譲**: `bash "${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh" task --write --workspace <wt> "<task>"`
   - 委譲開始時に `bin/harness session declare --task <task-id>` で共有 presence に作業宣言（他セッションから task 番号で逆引き可能になる）
5. cursor 出力を Lead が diff レビュー → cherry-pick → Plans.md `cc:done [hash]` 更新
   - 更新後 `bin/harness session declare --clear` で presence の task 宣言を解除

#### Reviewer-only mode (`--cursor --reviewer-only`) — read = lean

Worker 実装は既完了（別系統 = claude / Codex で済んだ）、advisory pre-review を Composer に出してもらう lean path。read-only 委譲なので **worktree 不要・cherry-pick 不要・cursor 出力の取り込みレビュー不要**（cursor は新たな diff を生まないため）。ただし対象 diff への **brain 一次レビュー（primary verdict）は省略しない**:

1. banner + 計画: `🚀 cursor / composer-2.5-fast / review` + 「これから: composer に advisory findings を委譲 → brain 一次レビューで verdict 確定」
2. `bash "${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh" task "diff レビュー: <base_ref>..HEAD"` — **`--write` も `--workspace` も付けない**
   - companion は `--write` 未指定で default `--mode ask` (hard read-only stop) になる (cursor-companion.sh の workspace guard は `--write` 時のみ発火)
   - cursor 側はファイル書込・コマンド実行が disabled、worktree 隔離不要
3. cursor 出力 (REQUEST_CHANGES / APPROVE 相当) を Lead が解釈し、`dual_review.cursor_verdict` に advisory として格納
4. **primary verdict は brain reviewer から取る**。cursor 単独では APPROVE を確定しない (spec.md Execution Backend Contract の self-review scope 契約 = 「diff を生成した同一コンテキストは自分の出力をレビューしない」と整合)。この lean path 自体が fresh-context advisory pre-review であり、委譲先 cursor session は実装 worker と会話状態を共有しないこと
5. **brain 一次レビュー**: Lead が cursor advisory findings を入力として対象 diff を自ら検分し、verdict（`APPROVE | REQUEST_CHANGES`）を出す。brain reviewer が利用不能（rate limit 等）の間は verdict を確定せず、タスクを `cc:wip` のままユーザー判断へ渡す（cursor advisory のみで `cc:done` にしない）
6. brain の APPROVE 後に Plans.md `cc:done [hash]` を Lead が更新

read mode で省略できるもの: 専用 `.git` worktree / cursor 出力の取り込みレビュー / cherry-pick / `worker-report.v1` / self_review 5 件。**省略不可**: 対象 diff への brain 一次レビュー（verdict 確定）。
read mode でも保持必要: `.cursorignore` / egress allowlist (`*.cursor.sh`) / permissions.json (best-effort)。詳細は `.claude/rules/cursor-cli-only.md` 「Read mode delegation (lean path)」節を参照。

**用途**（rate limit 時の前倒し集約 / Reviewer だけ別系統に分散 / Codex review auth 失敗時の fallback、詳細は [references/lean-path-detail.md](${CLAUDE_SKILL_DIR}/references/lean-path-detail.md)）。

#### Cursor adapter support claim

Cursor は `supported` tier（H8 pin: live H4 2026-07-17 + H7 release-preflight fail-closed）。FS jail なし — containment は harness-side（`docs/CURSOR_INTEGRATION.md`）。`--cursor` lean path 自体は tier を昇格させない。

Bootstrap route: `.cursor/AGENTS.md` + `.cursor-plugin/plugin.json`。

Verification:

```bash
bash tests/test-cursor-adapter-candidate.sh
bash tests/test-support-claim-wording.sh
```

## Flow Summary

```
breezing [scope] [--codex] [--parallel N] [--no-discuss] [--auto-mode]
    │
    ↓ Load harness-work with team mode
    │
Phase 0: Planning Discussion (--no-discuss でスキップ)
Phase A: Pre-delegate（チーム初期化）
Phase B: Delegate（Worker 実装 + 必要時 Advisor + Reviewer レビュー）
Phase C: Post-delegate（統合検証 + Plans.md 更新 + commit）
```

## Advisor Protocol

Worker は generic な subagent を増やさない。
迷った時は構造化 JSON で相談要求だけ返し、Lead が advisor を呼ぶ。

1. Worker → `advisor-request.v1`
2. Lead → Advisor
3. Advisor → `advisor-response.v1`
4. Lead → 同じ Worker に advice を返して続行
5. Reviewer は最後の成果物だけを見る

相談条件は loop / solo とそろえる。

- 高リスク task（`needs-spike` / `security-sensitive` / `state-migration`）の初回実行前
- 同じ原因の失敗が 2 回続いた後
- plateau により `PIVOT_REQUIRED` を返す直前
- 同じ `trigger_hash` は 1 回だけ。task ごとの相談回数は最大 3 回

### Progress Feed（Phase B 中の進捗通知）

Lead は Worker のタスク完了ごとに、以下のフォーマットで進捗を出力する:

```
📊 Progress: Task {completed}/{total} 完了 — "{task_subject}"
```

**出力例**:
```
📊 Progress: Task 1/5 完了 — "harness-work に失敗再チケット化を追加"
📊 Progress: Task 2/5 完了 — "harness-sync に --snapshot を追加"
📊 Progress: Task 3/5 完了 — "breezing にプログレスフィードを追加"
```

> **設計意図**: breezing は長時間実行になることが多い。
> ユーザーがターミナルをチラ見した時に「今どこまで進んでいるか」が一目で分かるようにする。
> task-completed.sh フックが systemMessage で同等の情報を出力するため、Lead の出力と補完し合う。

### Silence Policy（長時間実行の通知整理）

Codex `0.123.0` の realtime handoff では、background agent が transcript delta を受け取り、必要ない時は明示的に沈黙できる。
Breezing の progress feed はこの前提に合わせ、通知を「作業の節目」に絞る。

報告するもの:

- task 完了、blocked、validation failure、review `REQUEST_CHANGES`
- Advisor の `PLAN` / `CORRECTION` / `STOP`
- Reviewer の `APPROVE` / `REQUEST_CHANGES`
- advisor / reviewer drift、plateau、contract readiness failure
- user が明示的に status を求めた時の要約

沈黙してよいもの:

- transcript delta を受け取っただけで、判定や status が変わっていない時
- tool stdout の細かな増分で、log に残っていれば十分な時
- 並列 Worker の待機中 heartbeat

頻度は「task 完了ごとに 1 回」を基本にする。
heartbeat を増やして安心感を作るのではなく、status / log / drift 検知に責務を分ける。
ただし Advisor request 未応答、Reviewer result 未到着、plateau 直前の警告は silence 対象にしない。

### Monitor ツール活用ガイド (CC 2.1.98+)

`run_in_background: true` で投げた長時間 shell process（`gh run watch`、build --watch 等）は、ポーリングではなく **Monitor ツール**で stdout を逐次通知として拾う。Agent (Worker/Reviewer) の完了監視や短時間の一発コマンドには不要。
使い分け表・典型パターンは [references/monitor-and-learning.md](${CLAUDE_SKILL_DIR}/references/monitor-and-learning.md) を参照。

### Review Policy（全モード統一）

Breezing モードでもレビューは **Codex exec 優先 → 内部 Reviewer フォールバック** の統一ポリシーに従う。
詳細は `harness-work` の「レビューループ」セクションを参照。

- Worker が worktree 内で実装・commit → `worker-report.v1` (self_review 5 件) を Lead に返却
- **self_review ゲート (Reviewer spawn 前)**: Lead が `self_review[].verified` と `evidence` を機械検証。1 件でも `verified:false` or `evidence:""` なら Reviewer を spawn せず Worker に自動差し戻し（同一セッション内 最大 2 回、3 回目で escalate）
- Lead が Codex exec でレビュー（120s タイムアウト、フォールバック: Reviewer agent）
- REQUEST_CHANGES → Lead が SendMessage で Worker に修正指示、Worker が amend（最大 `MAX_REVIEWS` 回。`MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3`）
- APPROVE → **Lead** が main に cherry-pick → Plans.md を `cc:完了 [{hash}]` に更新

### 完了報告（Phase C — Lead が生成）

全タスク完了後、**Lead** が以下の手順でリッチ完了報告を生成する:

1. `git log --oneline {base_ref}..HEAD` で全 cherry-pick コミットを収集
2. `git diff --stat {base_ref}..HEAD` で全体の変更規模を取得
3. Plans.md の `cc:TODO` / `cc:WIP` 残タスクを抽出
4. `harness-work` の `Completion Report Output Contract` と `references/completion-report.md` の Breezing テンプレートに従い出力

> **生成者は Lead**。Worker や hook ではない。Lead が Phase C で git + Plans.md を読んで生成する。

### Phase 0: Planning Discussion（構造化 3 問チェック）

全タスク実行前に、スコープ（Q1）・依存関係（Q2、Depends カラムがある時のみ）・リスクフラグ（Q3、`[needs-spike]` がある時のみ）の 3 問で計画の健全性を確認する（合計 30 秒設計）。
`--no-discuss` 指定時は全スキップ。3 問の具体文言と判定ロジックは [references/lean-path-detail.md](${CLAUDE_SKILL_DIR}/references/lean-path-detail.md) を参照。

### Universal Violations Injection（セッション内 Worker 間の学習伝播）

同一 `/breezing` 起動内で蓄積された Reviewer の universal gotchas を次 Worker の briefing 冒頭に自動注入する。**同一セッション内のみ有効**（セッション終了で破棄、`session-memory` には書かない）。実装（in-memory 配列 + briefing 注入コード）は
[references/monitor-and-learning.md](${CLAUDE_SKILL_DIR}/references/monitor-and-learning.md) を参照。

### 依存グラフに基づくタスク割り当て

Plans.md に Depends カラムがある場合（v2 フォーマット）、`Depends` が `-` の独立タスクを先に並列 spawn し、各 Worker 完了後に Lead がレビュー→cherry-pick する（harness-work Phase B 参照）。依存元が main に入ったら、それに依存していたタスクを次に実行し、全タスク完了まで繰り返す。逐次処理なのは「Worker 完了→レビュー→cherry-pick」で、並列化できるのは独立タスクの Worker spawn 部分のみ。詳細は [references/lean-path-detail.md](${CLAUDE_SKILL_DIR}/references/lean-path-detail.md) を参照。

## Codex Native Orchestration

Codex では native subagent を使う。
代表的な制御面は `spawn_agent`, `wait`, `send_input`, `resume_agent`, `close_agent`。

> **Claude Code vs Codex の通信 API**（SSOT: `team-composition.md` の API マッピング表）:
> - Claude Code: `SendMessage(to: agentId, message: "...")` で Worker に修正指示
> - Codex: `resume_agent(agent_id)` で Worker を再開 → `send_input(agent_id, "...")` で指示送信
>
> harness-work の擬似コードは Claude Code 構文で記述。Codex 環境では上記に読み替えること。

## Related Skills

- `harness-work` — 単一タスクからチーム実行まで（本体）
- `harness-sync` — 進捗同期
- `harness-review` — コードレビュー（breezing 内で自動起動）
