---
name: harness-plan
description: "HAR: Research-backed, team-validated task planning, Plans.md management, progress sync. Trigger: create a plan, add tasks, update Plans.md, mark complete, check progress. Do NOT load for: implementation, review, release."
description-en: "HAR: Research-backed, team-validated task planning, Plans.md management, progress sync. Trigger: create a plan, add tasks, update Plans.md, mark complete, check progress. Do NOT load for: implementation, review, release."
description-ja: "HAR:調査・採点・記憶確認・TeamAgent/サブエージェント検証つきのタスク計画、Plans.md管理、進捗同期を担当。計画作って、タスク追加、Plans.md更新、完了マーク、進捗確認で起動。実装・レビュー・リリースには使わない。"
description-zh: "HAR：带有调查、评分、记忆检查和 TeamAgent/子代理验证的任务计划、Plans.md 管理和进度同步。当用户提到创建计划、添加任务、更新 Plans.md、标记完成、检查进度时启动。不适用于：实现、审查、发布。"
kind: workflow
purpose: "Maintain co-required planning output for the spec.md product contract and Plans.md task contract"
trigger: "create a plan, add tasks, update Plans.md, check progress"
shape: workflow
role: generator
pair: harness-sync
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Bash", "Grep", "Glob", "WebSearch", "Task"]
argument-hint: "[create|add|update|sync|sync --no-retro|--ci]"
user-invocable: true
effort: medium
---

# Harness Plan

Harness 的集成计划技能。
整合以下3个旧技能:

- `planning` (plan-with-agent) — 构思 → Plans.md 落地
- `plans-management` — 任务状态管理・标记更新
- `sync-status` — Plans.md 与实现的同步确认

## Quick Reference

| 用户输入 | 子命令 | 动作 |
|------------|------------|------|
| "制定计划" / `/harness-plan create` | `create` | Spec delta / skip reason → Plans.md task 生成 |
| "添加任务" / `/harness-plan add` | `add` | 向 Plans.md 添加新任务 |
| "标记完成" / `/harness-plan update` | `update` | 将任务标记改为 cc:完成 |
| "现在在哪？" / `/harness-plan sync` | `sync` | 对照并同步实现与Plans.md |
| `/harness-sync` | `sync` | 进度确认（等同于独立 sync surface） |
| `/harness-plan create` | `create` | spec.md / Plans.md 二正本的计划创建 |
| `/harness-plan list` | `list` | 一览 `plans/manifest.json` 的 named Plans |
| `/harness-plan switch <name>` | `switch` | 将 active plan 保存到 `.claude/state/active-plan.json` |

## 范围既定: 当前可进行的所有工作（operator 裁定 2026-07-24）

计划请求（`create` / 无参数启动 / 「计划」）的既定解释为 **「当前可着手的全部工作」**。

- 除非用户明确范围，否则列出请求语境中包含的所有 open item（剩余 phase、未处理 follow-up、已知改善点、请求文中提及的所有问题）并纳入计划。不擅自缩成最小子集
- 即使件数多也不做筛选，而是将全量分类为 Required / Recommended / Optional / Reject 并提示。作为 Reject 明确排除理由（不默默遗漏）
- 判断「仅先做一部分」妥当时，不表现筛选后的计划，而是作为全量计划中的执行顺序（Phase 分割 / Depends）表现

## Literal companion commands（CC 2.1.108+）

- `/recap`: 久别回归时重新获取摘要后进入 `sync`
- `/undo`: `/rewind` 的别名。想立即撤回最近的 plan 更新时直接使用

## 子命令详情

### 标准的计划质量契约

See [references/planning-quality.md](${CLAUDE_SKILL_DIR}/references/planning-quality.md)

`harness-plan` 是创建 spec.md product contract and Plans.md task contract 的 co-required planning output 的 planning surface。
precedence 维持 `spec.md > sub-spec > Plans.md`。
Plans.md 是 task ledger，root `spec.md` 是 product contract，不破坏上下关系。
不将传入信息直接落到 Plans.md。
计划创建或大的 task 追加时，确认最新信息・现有规格・记忆・TeamAgent / 子代理的多视点讨论，
仅将应纳入此产品的要素转换为 task contract。
`/harness-plan create` 返回 `Spec delta` 或 `Spec skip reason` 与 `Plans.md` task 生成组合。
输出必须包含 `Spec delta` 或 `Spec skip reason`。
`Spec delta` / `Spec skip reason` は Harness が生成し、consumer は承認・修正だけ行う。

**Non-trivial planning gate**:

単発・軽微タスクでない planning は、TeamAgent またはサブエージェント前提で扱う。
ここでの non-trivial は、複数 task / 複数 file / 複数 session / product behavior / API / data model / 権限 / 課金 / 外部連携 / 配布面 / セキュリティに影響する依頼を指す。
Task tool が使える場合は Product / Architecture / Security / QA / Skeptic の独立視点を走らせる。
使えない場合は `サブエージェント未使用` と明示し、同じ観点を単独で分けて評価する。

non-trivial planning の出力には、次の検証を必ず含める。

- `team_validation_mode`: `not_required_lightweight` / `native` / `subagent` / `manual-pass` / `unavailable`
- `spec.md` / sub-spec / `Plans.md` の整合性
- harness-mem / harness-recall / repo memory による車輪の再発明防止確認
- プロダクト目的から外れていないか
- セキュリティ、権限、秘密情報、サプライチェーンに問題がないか
- lint / formatter baseline があるか。source code changes を含む plan で未設定なら、実装 task の前に setup task を置く
- ちゃんと動く計画か。つまり test / smoke / CI / review / release gate が task DoD に落ちているか

軽量 task は `team_validation_mode: not_required_lightweight` でよい。
non-trivial planning は `native` / `subagent` / `manual-pass` のいずれかを使う。
`unavailable` のまま Required にしてはいけない。
Product / Architecture / Security / QA / Skeptic は検証 perspective であり、agent_type 名ではない。
利用可能な TeamAgent / Task サブエージェントに perspective として依頼し、任意 agent spawn を要求しない。
Security gate は秘密情報の実読取を要求しない。
`.env` や secret の read が必要になる場合は Risk Gate として止め、許可された既存 guard / evidence で確認する。

**適用する場面**:

- `create` で新しい計画を作る
- `add` で product behavior / API / 権限 / 課金 / 外部連携 / 配布面に影響する task を足す
- ユーザーが外部プロダクト、競合、仕様案、改善案、比較材料を渡した
- 既存仕様や過去判断との衝突リスクがある

**軽く扱ってよい場面**:

- marker 更新だけの `update`
- status 照合だけの `sync`
- typo、format、README/CHANGELOG のみ
- 既存 spec とテストで正解が固定されている狭い変更

**品質フロー**:
1. 入力情報を分解し、評価対象・採点軸・不確かな事実を明示する
2. 最新情報を取得する。外部事実は WebSearch / 公式ドキュメント / 一次情報を優先し、重要点は複数ソースでクロスチェックする
3. 既存仕様・root `spec.md`・Plans.md・README・docs・CLAUDE.md・関連 skill を確認する
4. harness-mem / harness-recall / `.claude/agent-memory/` / `.claude/state/` など、利用可能な記憶面を project-scoped で確認する
5. non-trivial planning では TeamAgent / Task サブエージェントを使い、Product / Architecture / Security / QA / Skeptic など異なる視点で独立レビューする
6. source code changes を含む plan では lint / formatter baseline を確認し、未設定なら setup task を先行させる
7. 中立的な採点レビューを出し、Required / Recommended / Optional / Reject に分類する
8. `$easy` 形式で、提案内容・理由・どうなるのかを報告する
9. 採用する案だけを root `spec.md` / Plans.md / test task へ落とし込む

### Lane Taxonomy + Stage Gate

Fast / Gate / Release は **新 skill ではなく Plans metadata** として扱う。Plans.md の 5 column テンプレート（Task / 内容 / DoD / Depends / Status）は変更せず、
lane（`[lane:fast]` / `[lane:gate]` / `[lane:release]`）・stage（検証→計画→TDD実装→レビュー→PR closeout の 5 段階）・unknown data contract（`not_observed != absent`、確認できない事実は `unknown` と明示）を
**内容（Content）または DoD の先頭**に埋め込む。タグ一覧・worked example・stage 別 DoD 例は
[references/create.md](${CLAUDE_SKILL_DIR}/references/create.md) を参照。

### create — 計画作成

See [references/create.md](${CLAUDE_SKILL_DIR}/references/create.md)

アイデア・要件をヒアリングし、実行可能な Plans.md を生成する。

**フロー**:
1. 会話コンテキスト確認（直前の議論から抽出 or 新規ヒアリング）
2. 何を作るか聞く（max 3問）
3. **計画品質チェック**（最新情報、既存仕様、記憶、TeamAgent / サブエージェント複数視点レビュー、採点）
4. 技術調査（WebSearch）
5. 機能リスト抽出
6. **spec.md / Plans.md 二正本チェック**（Spec delta または Spec skip reason + Plans.md task）
7. 優先度マトリクス（Required / Recommended / Optional / Reject）
8. TDD 採用判断（テスト設計）
9. Plans.md 生成（`cc:TODO` マーカー付き）
10. **事前確認セクション生成**（plan-time pre-approval）
11. 次のアクション案内

### create — 事前確認セクション（plan-time pre-approval）

`create` で計画を確定する時は、Plans.md task を出したあと、承認前に **事前確認セクション**を必ず生成する。
目的は、常設 allowlist で何でも許可するのではなく、作業スコープごとに「発生しそうな stop / ask」を plan 承認時に 1 回だけ前倒しで確認すること。

抽出対象:

- 各 task の対象ファイル、関連 path、想定変更範囲
- DoD に書いた検証コマンド、PR closeout コマンド、外部 API / CLI 呼び出し
- `secret-read path`（`.env*`, `secrets/**`, `*.pem`, `*.key`, `.ssh/**`, `.aws/**`, `credentials` など）
- 外部送信（`git push`, `gh pr create`, `gh api`, `curl` / API call, release / publish / deploy）
- 破壊的操作（`rm -rf`, migration destructive step, force push, production apply）

固定 format:

```text
## 事前確認
- 事項: <secret-read / external-send / destructive の具体操作>
  理由: <DoD または task 実行上必要な理由を 1 行>
  scope: Phase <phase> / Task <task>
```

出力ルール:

- 1 行の `理由` は secret 値を含めない。path / コマンド名 / 対象サービスまでに留める。
- plan 承認時に、事前確認セクションの全事項を一括提示し、ユーザーから承認 / 否認を得る。
- 承認結果は `.claude/state/plan-preapprovals.json` に `plan-preapproval.v2` として記録する。schema は `templates/schemas/plan-preapproval.v2.json`。v1 は既存記録の読み取り互換に限る。
- 記録は `事項 + 理由 1 行 + scope (phase/task)` を維持する。`operations` には `secret-read` / `external-send` / `destructive` を列挙する。`paths` / `commands` / `targets` には対象を列挙する。`decision`、`approved_at`、RFC3339 の `expires_at` を入れる。
- `max_uses` は必要な再試行回数を含む上限を設定する。省略時は 10 回。`uses` は新規承認時に 0 とする。
- 確認は plan 承認時の 1 回のみ。`harness-work` / `breezing` 実行中、宣言済み事項だけを理由に `AskUserQuestion` を出してはいけない。
- 記録に無い未計画の secret-read / 外部送信 / 破壊的操作は、従来どおり runtime floor / ask で停止する。安全網を狭めない。
- secret-read の承認は secret 値の表示許可ではない。必要最小の path を宣言し、work 開始時に project config の `runtimefloor.secretAllow` へ per-run 反映するための入力として扱う。

### spec.md / Plans.md 二正本チェック（デフォルト）

Plans.md は「やるべきこと」の task contract、root `spec.md` は「何が正しいか」の product contract として扱う。
co-required planning output は両方の出力を必須にするという意味であり、precedence は `spec.md > sub-spec > Plans.md` のまま維持する。
実装がぶれる可能性がある時は、Plans.md 生成前に root `spec.md` を更新する。
`create` と product-impacting `add` は毎回 root `spec.md` を読む。

優先する保存先:

1. root `spec.md`
2. consumer repo に root `spec.md` がない時だけ、既存の project spec / architecture / product compass
3. consumer repo に root `spec.md` がない時だけ、`docs/spec/00-project-spec.md`
4. 既存規約がある repo では、その規約に沿った spec path

作成/更新が必要な条件:

- ユーザーに見える振る舞い、API、データモデル、権限、課金、外部連携を決める task
- 複数の実装方針があり、選び方で product behavior が変わる task
- 過去または今回の会話で「仕様が曖昧で実装がぶれた」兆候がある task
- Plans.md には作業内容があるが、project としての正解条件が安定文書にない task

不要な条件:

- typo、format、dependency bump、README/CHANGELOG のみ
- 動作変更なしの狭い refactor
- 既存 spec とテストで正解が十分に固定されている修正

出力契約:

- `Spec delta`: product contract を更新する時に、対象 spec path と変更点を書く
- `Spec skip reason`: product contract を更新しない時に、理由を書く
- `Spec delta` / `Spec skip reason` は Harness が生成し、consumer は承認・修正だけ行う
- docs-only / mechanical task でも `Spec skip reason` を task context / sprint contract に残す
- missing search result、unavailable memory、未読ファイルを absent と断定しない。`not_observed != absent`
- ユーザーに spec を一から書かせない。agent が既存 spec と入力から最小 delta を作り、曖昧な時だけ判断分岐を出す

参照:

- `docs/plans/spec-ssot.md`

### create 完了時のセッション起動案内（必須）

`create` が終わったら、説明だけで終わらせず、**新しいセッションの起動コマンド** と
**起動後にそのまま入れる最初の指示プロンプト** をセットで案内する。

優先順位は次の通り:

1. 未完了タスクが 1 件だけ、または最初の 1 件だけ始めるのが自然
   - 起動コマンド: `claude`
   - 最初の入力: `/harness-work <task番号>`
2. 依存の薄いタスクが複数あり、まとめて進めるのが自然
   - 起動コマンド: `claude`
   - 最初の入力: `/breezing all`
   - 代替: `/harness-work all`
3. 長時間実行や再入が前提
   - 起動コマンド: `ENABLE_PROMPT_CACHING_1H=1 claude`
   - 最初の入力: `/harness-loop all`
   - 代替: `/breezing all`

最低でも次の 3 行を含める:

- `新しいセッションの起動コマンド:`
- `起動後の最初の入力:`
- `向いている場面:`

例:

```text
新しいセッションの起動コマンド: claude
起動後の最初の入力: /breezing all
向いている場面: Phase 1 の task が複数あり、まとめて進めるほうが自然なため
```

長時間系を勧める場合は、Claude Code セッション起動コマンドも併記する:

```text
新しいセッションの起動コマンド: ENABLE_PROMPT_CACHING_1H=1 claude
起動後の最初の入力: /harness-loop all
向いている場面: 5 分を超える待機や resume をまたぐ長時間タスクのため
```

補足:

- `scripts/claude-longrun.sh` はこのリポジトリの開発補助スクリプトで、plugin install 後の consumer 環境には配布されない
- そのため、consumer 向け案内では常に `ENABLE_PROMPT_CACHING_1H=1 claude` の 1 行コマンドを優先する
- リポジトリ開発中だけ同等のラッパーを使いたい場合、`bash scripts/claude-longrun.sh` はローカル checkout 上では利用してよい

**CI モード** (`--ci`):
ヒアリングなし。既存の Plans.md をそのまま利用してタスク分解のみ行う。

### add — タスク追加

Plans.md に新しいタスクを追加する。
product-impacting な追加では、上の「spec.md / Plans.md 二正本チェック」に従い `Spec delta` または `Spec skip reason` も出力する。

```
/harness-plan add タスク名: 詳細説明 [--phase フェーズ番号]
```

タスクは `cc:TODO` マーカーで追加される。

### update — マーカー更新

タスクのステータスマーカーを変更する。

```
/harness-plan update [タスク名|タスク番号] [WIP|完了|blocked]
```

マーカー対応表:

| コマンド | マーカー |
|---------|---------|
| `WIP` | `cc:WIP` |
| `完了` / `done` | `cc:完了` |
| `blocked` | `blocked` |
| `TODO` | `cc:TODO` |

### sync — 進捗同期

実装状況と Plans.md を照合し、差分を検出・更新する（Plans.md 現状取得 → フォーマット検出 → git 状況取得 → agent trace 分析 → 差分検出 → マーカー修正提案 → 次アクション提示）。
`cc:完了` タスクが 1 件以上あれば、見積もり精度・ブロック原因・スコープ変動を分析するレトロスペクティブをデフォルト ON で実行する（`sync --no-retro` でスキップ）。
Step 0-6 の完全版・harness-mem への記録手順は [references/sync.md](${CLAUDE_SKILL_DIR}/references/sync.md) を参照。

### team mode / issue bridge

Plans.md は正本のまま維持し、GitHub Issue 連携は opt-in の team mode だけで使う。

- solo 開発では bridge を使わない
- team mode は tracking issue を 1 つ作り、その配下に task ごとの sub-issue payload を dry-run で生成する
- `scripts/plans-issue-bridge.sh` は実際に GitHub を更新せず、常に dry-run の payload を返す
- Plans.md への変更はこの bridge では行わない

参照:

- `docs/plans/team-mode.md`

### named Plans

複数の Plans.md を使う場合は `plans/manifest.json` を正本にして、名前で選択する（1 run では 1 つの named plan だけを使う。long-running / CI / issue bridge では active pointer に頼らず `--plan <name>` を渡す。manifest path は project root 相対のみ）。

```bash
scripts/plan-registry.sh list
scripts/plan-registry.sh switch roadmap
scripts/plans-issue-bridge.sh --plan roadmap --format markdown
node scripts/generate-sprint-contract.js --plan roadmap 9.1.1
```

参照: `docs/plans/named-plans.md`

## Plans.md フォーマット規約

### フォーマット

5 カラム（Task / 内容 / DoD / Depends / Status）の Markdown table。DoD は Yes/No 判定できる検証可能な 1 行（「いい感じ」「ちゃんと動く」は禁止）。
Depends は `-`（依存なし）/ タスク番号 / カンマ区切り複数 / フェーズ依存のいずれか。生成テンプレート全文（Purpose 行含む）は
[references/create.md](${CLAUDE_SKILL_DIR}/references/create.md) を参照。

### TDD tags

Plans.md の task には、TDD 判定を明示するタグを内容または DoD に書ける。

| タグ | 意味 | `tdd_required` 推論 |
|------|------|--------------------|
| `[tdd:required]` | この task は先に失敗テストを書く必要がある | `true` |
| `[tdd:skip:<reason>]` | この task は理由つきで TDD を省略する | `false`, `skip_tdd_reason=<reason>` |

`<reason>` は空にしない。
例: `[tdd:skip:docs-only]`、`[tdd:skip:no-test-framework-detected]`。

タグがない場合の `tdd_required` は次の順で推論する。

1. Plans.md tag: `[tdd:required]` / `[tdd:skip:<reason>]`
2. files: `src/`, `app/`, `cmd/`, `lib/`, `pkg/`, `internal/`, `go/` など source 実装を含むなら required
3. TDD 推論: docs-only や test framework なしなら skip reason を付けて not required

### optional briefs / manifest

`harness-plan create` は、必要なときだけ brief を付ける。

- project spec SSOT は project 全体の正解条件を固定する文書で、必要時だけ作る
- UI を含むタスクでは `design brief`
- API を含むタスクでは `contract brief`
- brief は「何を作るか」を短く固定する補助資料で、Plans.md や spec SSOT を置き換えない
- skill frontmatter の一覧は `scripts/generate-skill-manifest.sh` で machine-readable JSON にできる

参照:

- `docs/plans/briefs-manifest.md`
- `docs/plans/spec-ssot.md`

### マーカー一覧

| マーカー | 意味 |
|---------|------|
| `pm:依頼中` | PM から依頼済み |
| `cc:TODO` | 未着手 |
| `cc:WIP` | 作業中 |
| `cc:完了` | Worker 作業完了 |
| `pm:確認済` | PM レビュー完了 |
| `blocked` | ブロック中（理由を必ず記載） |

### 計画確定後の導線（非エンジニア向け計画概要）

Plans.md への task append が完了したら、非エンジニアの発注者が計画を判断できるよう
`harness-plan-brief` を提案する。これは理解・選択肢・リスク・合格条件を 1 枚の HTML に
まとめた「計画概要」画面で、専門知識なしで読める。実装に入る前の合意形成に使う。

## 関連スキル

- `harness-sync` — 実装と Plans.md を同期する
- `harness-work` — 計画したタスクを実装する
- `harness-plan-brief` — 計画概要 HTML（非エンジニア向け、計画確定時に提案）
- `harness-review` — 実装のレビュー
- `harness-setup` — プロジェクト初期化
