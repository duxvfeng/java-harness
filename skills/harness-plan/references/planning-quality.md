# 计划质量契约 — harness-plan 标准流程

`harness-plan` 不将用户传递的信息原样转换为作业表。
计划制定或大 task 追加时，通过最新信息、既有规格、记忆、TeamAgent / 子代理的多视点讨论充分过滤，
仅将应纳入此产品的要素放入 Plans.md 的 task contract。

这不是独立子命令。是 `create` 和影响大的 `add` 的标准质量关卡。

## Step 0: 适用判断

符合以下任一情况时，使用此质量契约。

- 用 `create` 创建新 plan
- 用 `add` 添加影响 product behavior / API / data model / 权限 / 计费 / 外部联动 / 分发面的 task
- 用户提供外部产品、竞争、规格方案、改善方案、比较材料
- 可能与既有规格、Plans.md、记忆、过去 decision 冲突
- 用户要求"最大火力""彻底比较""中立採点""防止退化"等
- 非单发・轻微，影响多个 task / 多个 file / 多个 session / product behavior / API / data model / 权限 / 计费 / 外部联动 / 分发面 / 安全

`create` 和 product-impacting `add` 每次读取 root `spec.md`。
无 root `spec.md` 的 consumer repo 才 fallback 到既有 project spec / `docs/spec/00-project-spec.md`。
输出必须包含 `Spec delta` 或 `Spec skip reason`。
这是 co-required planning output 的契约，precedence 保持 `spec.md > sub-spec > Plans.md`。

non-trivial planning 以 TeamAgent 或子代理验证为前提。
可使用 Task tool 时必须运行独立视点。
不可用时明确显示 `未使用子代理`，单独分点评估相同观点。
输出必须包含 `team_validation_mode`。

| mode | 使用场景 |
|------|----------|
| `not_required_lightweight` | typo / format / README / CHANGELOG / marker 更新 / status sync 等轻量 task |
| `native` | 使用 TeamAgent 等 runtime native 多视点验证 |
| `subagent` | 使用 Task 子代理，每个 perspective 独立 |
| `manual-pass` | OpenCode 等 Task unavailable 的 runtime，单独分点评估相同观点 |
| `unavailable` | 无法验证。不得将 non-trivial work 设为 Required |

以下可轻量处理。

- 仅 marker 更新的 `update`
- 仅 status 对照的 `sync`
- 仅 typo / format / README / CHANGELOG
- 正解由既有 spec 和 test 固定的狭义更改

## Step 1: 输入分解

将用户提供的信息分为以下 4 类。

| 分类 | 例 |
|------|----|
| 评价对象 | 外部产品、竞争功能、规格方案、设计方针、运用方案 |
| 用户意图 | 想改善什么、想避免什么 |
| 不确定事实 | 最新性、价格、API、约束、竞争状况、既有 repo 状态 |
| 采用判断所需依据 | 官方 docs、实测、既有规格、记忆、测试结果 |

有不明点也不通过提问停止。先合理推测可评价的意图，仅在判断实在分歧时作为"判断分歧"输出。

## Step 2: 获取最新信息

包含外部事实时使用 WebSearch。优先级如下。

1. 官方文档、官方博客、发布笔记、GitHub repo
2. 标准规格、论文、接近一次信息的技术 source
3. 可信比较文章、导入案例、issue / discussion

重要事实尽可能通过 2 个以上 source 确认。
矛盾时整理哪个点矛盾，明确对采用判断的影响。

无法使用 WebSearch 或网络失败时，按以下处理。

- `最新信息: 未验证`
- 仅本地根据据暂定评价
- final 中明示"这里留有 Web 确认"

## Step 3: 本地正本确认

纳入产品的提案必须对照既有正本。

最低确认:

```bash
cat Plans.md
rg -n "関連キーワード" README.md README_ja.md CLAUDE.md docs skills scripts tests
rg -n "\"(lint|format)\"|eslint|prettier|biome|oxlint|dprint|ruff|black|isort|gofmt|go vet|cargo fmt|cargo clippy" package.json pyproject.toml go.mod Cargo.toml Makefile .github/workflows scripts docs 2>/dev/null
find docs -maxdepth 3 -type f | sort
git status --short --branch
```

見る観点:

- 既存の product promise と矛盾しないか
- 既存の skill role / trigger / allowed-tools と矛盾しないか
- Plans.md の未完了タスクと競合しないか
- 配布 mirror、Codex mirror、OpenCode mirror、i18n に影響しないか
- 仕様正本があるなら、Plans.md より先に spec SSOT を更新すべきか
- root `spec.md` の product contract と Plans.md の task contract が分離されているか
- source code changes を含む plan で lint / formatter baseline があるか。未設定なら implementation の前に setup task が必要か

## Step 4: 記憶確認

harness-mem、harness-recall、ローカル memory file が使える場合は、関連キーワードで過去判断を確認する。
検索できる場合は現在の project / repo に絞る。cross-project 検索は、ユーザーが明示した場合だけ使う。
この step は車輪の再発明防止確認であり、non-trivial planning では省略しない。

確認対象の例:

- harness-mem / harness-recall の検索結果
- `.claude/agent-memory/`
- `.claude/state/memory-bridge-events.jsonl`
- `.harness-mem/` の存在確認
- repo 内 docs / Plans.md に残っている prior decision

注意:

- harness-mem の DB を直接読む前提にしない
- harness-mem が未セットアップ、unhealthy、検索不可なら「記憶未確認」と明示する
- 記憶は現在の repo 状態より弱い。古い記憶と git / docs が衝突したら、現在の repo 状態を優先する
- memory や検索で見えないものを absent と断定しない。`not_observed != absent`

## Step 5: サブエージェント議論

non-trivial planning では、TeamAgent または Task サブエージェントを前提にする。
Task tool が使える場合は、最低 3 つの独立視点を走らせる。各 agent には「read-only」「根拠付き」「結論先出し」を指定する。
単発・軽微タスクだけは、この step を明示的に skip してよい。
Product / Strategy、Architecture / Implementation、Security / Abuse、QA / Regression、Skeptic は perspective 名であり、agent_type 名ではない。
利用可能な TeamAgent / Task サブエージェントに perspective として渡す。
任意 agent spawn を要求しない。

標準ロール:

| Role | 目的 |
|------|------|
| Product / Strategy | 採用価値、差別化、ユーザー価値、機会費用を見る |
| Architecture / Implementation | 実装可能性、既存設計との整合、保守負荷を見る |
| Security / Abuse | 権限、秘密情報、prompt injection、サプライチェーン、外部送信リスクを見る |
| QA / Regression | デグレ、テスト、配布 mirror、互換性、実際に動くかを見る |
| Skeptic | 採用しない理由、過剰投資、曖昧な前提を攻撃する |

各 agent の出力に求めるもの:

- 採用 / 条件付き採用 / 不採用
- 根拠
- 最大のリスク
- 追加で確認すべきこと
- 既存仕様や記憶との衝突
- test / smoke / CI / review / release gate に落とすべき DoD

議論のまとめ方:

1. 合意点を抽出する
2. 対立点を残す
3. 自分の判断を出す
4. Required / Recommended / Optional / Reject に分類する

サブエージェントが使えない場合は、単独で同じ 5 視点を明示的に分けて評価し、`サブエージェント未使用` と書く。

## Step 5.5: 実装プラン検証ゲート

実装プランは、次の 5 つをすべて満たすまで Required にしない。

| Gate | 見ること | 落ちた場合 |
|------|----------|------------|
| Spec / Plans Fit | root `spec.md`、sub-spec、`Plans.md` の順序と矛盾しない | `Spec delta` を先に出すか Reject |
| Memory / Wheel Check | harness-mem / harness-recall / repo memory に同種判断や既存 task がないか | 既存案を再利用、差分だけ task 化 |
| Product Fit | プロダクト目的と primary user workflow に直結するか | docs / external workflow / Optional へ逃がす |
| Security Fit | 権限、秘密情報、外部送信、dependency、branch/release gate を弱めないか | spike / security task / Reject |
| Quality Baseline Fit | source code changes に対して lint / formatter / CI command で品質を Yes/No 判定できるか | setup task を先行、または formatter_baseline の skip reason を残す |
| Works In Practice | test / smoke / CI / review / release closeout で Yes/No 判定できるか | DoD を作り直す |

この gate は「手戻りを減らすための前工程」であり、感想レビューではない。
落ちた gate は必ず Plans.md の DoD、Depends、または `[needs-spike]` に反映する。
Quality Baseline Fit は、formatter や linter を雑に追加するための口実ではない。
未設定かつ source code changes を含む plan では、実装 task の前に setup task を置く。
setup task の DoD は config、package script / CI command、validation command の 3 点を含める。
planning では package install しない。導入は harness-work が setup task として行う。
広範囲の一括 reformat は、ユーザーが明示した場合か、その setup task の scope に入っている場合だけ実行する。
Security Fit は secret の実読取を要求しない。
`.env`、tokens、private keys、customer data などの read が必要になる場合は Risk Gate として止める。
既存の guardrail、config shape、audit evidence、テスト、GitHub / CI metadata など、秘密値を読まない surface で確認する。

## Step 6: 中立採点レビュー

採点は 5 点満点。5 点は良い状態、1 点は弱い状態として扱う。

| 軸 | 5 点 | 3 点 | 1 点 |
|----|-----|-----|-----|
| Product Fit | 導入先プロダクトの核に直結 | 便利だが周辺的 | 別製品や運用で足りる |
| Evidence Strength | 一次情報 + 実測 + 既存根拠あり | 片方だけ確認 | 推測中心 |
| User Value | 判断品質や実行速度が大きく上がる | 一部 workflow で有効 | 体感価値が薄い |
| Implementation Feasibility | 小さく局所的 | 中規模だが管理可能 | 大規模で保守負荷大 |
| Regression Safety | 低リスクでテスト可能 | 影響範囲あり | 既存 flow を壊しやすい |
| Strategic Leverage | 長期の差別化になる | 便利機能止まり | 一過性 |
| Security Safety | 権限や秘密情報を弱めず検証可能 | 注意点あり | 危険な権限緩和や未検証外部送信がある |
| Works In Practice | smoke / CI / review で実証できる | 手動確認中心 | 動作確認が曖昧 |

補正ルール:

- Evidence Strength が 2 以下なら Required 禁止
- Regression Safety が 2 以下なら、先に spike / spec / test を置く
- Security Safety が 2 以下なら Required 禁止
- Works In Practice が 2 以下なら、DoD を作り直すか spike に落とす
- Quality Baseline Fit が 2 以下で source code changes を含むなら、formatter_baseline setup task を Required dependency にする
- Implementation Feasibility が 2 以下で User Value が 3 以下なら Reject 寄り
- Product Fit が 2 以下なら、このプロダクトに入れず docs / external workflow に逃がす

## Step 7: `$easy` 報告

最終出力は、難しい評価をそのまま出さず、判断できる形に変換する。

必須構成:

```markdown
ひとことで:
{{採用判断を 1 文}}

採点レビュー:
| 案 | 点数 | 判定 | 根拠 | 未検証 |
|----|------|------|------|--------|

取り入れるべき提案:
| 優先 | 提案内容 | 理由 | どうなるのか |
|------|----------|------|--------------|

デグレ確認:
- team_validation_mode:
- 仕様:
- Plans.md:
- harness-mem / 記憶:
- TeamAgent / サブエージェント:
- product fit:
- security:
- works in practice:
- formatter_baseline:
- mirror / 配布:
- test:

次にやること:
1. ...
2. ...
3. ...
```

文体ルール:

- 結論を先に出す
- 専門語はすぐ短く訳す
- 「すごい」「革新的」などの空気で判断しない
- 提案は 1〜3 個に絞る。候補を並べすぎない
- 事実、推測、未検証を分ける

## Step 8: Plans.md / spec へ落とす時

採用する案だけを task contract に変換する。

順序:

1. root `spec.md` を読み、必要なら先に `Spec delta` として product contract を更新する
2. source code changes があり lint / formatter baseline が未設定なら、formatter_baseline setup task を Required dependency として先に置く
3. Plans.md に Required task だけを追加する
4. 高リスク案には `[needs-spike]` を付ける
5. 各 task に検証可能な DoD を置く
6. TDD が必要な task には `[tdd:required]` を付ける
7. mirror / i18n / package surface に影響する場合は、検証 task を別に置く
8. spec 更新が不要なら `Spec skip reason` を task context / sprint contract に残す
9. non-trivial planning では TeamAgent / サブエージェント検証結果、または `サブエージェント未使用` fallback と 5 gate の結果を task context に残す
10. `team_validation_mode: unavailable` の plan は Required にしない。軽量 task だけ `not_required_lightweight` を許可する

`Spec delta` は agent が draft する。ユーザーに spec を一から書かせる前提にしない。
`Spec delta` / `Spec skip reason` は Harness が生成し、consumer は承認・修正だけ行う。

禁止:

- 仕様の正解条件が揺れているのに実装 task だけ作る
- デグレ確認を task 化せずに「注意」で済ませる
- source code changes を含むのに lint / formatter baseline 不在を無視して実装 task だけ作る
- docs-only / mechanical task の `Spec skip reason` を省略する
