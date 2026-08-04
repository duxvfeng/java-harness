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
rg -n "相关关键字" README.md README_ja.md CLAUDE.md docs skills scripts tests
rg -n "\"(lint|format)\"|eslint|prettier|biome|oxlint|dprint|ruff|black|isort|gofmt|go vet|cargo fmt|cargo clippy" package.json pyproject.toml go.mod Cargo.toml Makefile .github/workflows scripts docs 2>/dev/null
find docs -maxdepth 3 -type f | sort
git status --short --branch
```

查看视点:

- 是否与现有 product promise 矛盾
- 是否与现有 skill role / trigger / allowed-tools 矛盾
- 是否与 Plans.md 的未完成任务竞争
- 是否影响分发 mirror、Codex mirror、OpenCode mirror、i18n
- 若有规格正本，是否应先于 Plans.md 更新 spec SSOT
- root `spec.md` 的 product contract 和 Plans.md 的 task contract 是否分离
- 包含 source code changes 的 plan 是否有 lint / formatter baseline。未设定时实现前是否需要 setup task

## Step 4: 记忆确认

harness-mem、harness-recall、本地 memory file 可用时，以相关关键字确认过去判断。
可搜索时限定为当前的 project / repo。跨项目搜索仅在用户明确时使用。
此步骤是防止重复发明轮子的确认，non-trivial planning 不可省略。

确认对象示例:

- harness-mem / harness-recall 的搜索结果
- `.claude/agent-memory/`
- `.claude/state/memory-bridge-events.jsonl`
- `.harness-mem/` 的存在确认
- repo 内 docs / Plans.md 中残留的 prior decision

注意:

- 不前提直接读取 harness-mem 的 DB
- harness-mem 未设定、unhealthy、不可搜索时明确标注"记忆未确认"
- 记忆弱于当前 repo 状态。旧记忆与 git / docs 冲突时优先当前 repo 状态
- 不将 memory 或搜索中不可见的断定为 absent。`not_observed != absent`

## Step 5: 子代理讨论

non-trivial planning 以 TeamAgent 或 Task 子代理为前提。
可使用 Task tool 时，至少运行 3 个独立视点。为各 agent 指定"read-only""有根拠""先出结论"。
仅单发・轻微任务可明确跳过此步骤。
Product / Strategy、Architecture / Implementation、Security / Abuse、QA / Regression、Skeptic 是 perspective 名而非 agent_type 名。
传递给可用的 TeamAgent / Task 子代理作为 perspective。
不要求任意 agent spawn。

标准角色:

| Role | 目的 |
|------|------|
| Product / Strategy | 看采用价值、差异化、用户价值、机会费用 |
| Architecture / Implementation | 看实现可能性、与既有设计整合、维护负荷 |
| Security / Abuse | 看权限、秘密信息、prompt injection、供应链、外部发送风险 |
| QA / Regression | 看退化、测试、分发 mirror、兼容性、实际是否可动 |
| Skeptic | 攻击不采用理由、过度投资、模糊前提 |

对各 agent 输出的要求:

- 采用 / 条件采用 / 不采用
- 根拠
- 最大风险
- 应追加确认
- 与既有规格或记忆冲突
- 应落到 test / smoke / CI / review / release gate 的 DoD

讨论总结方法:

1. 提取合意点
2. 残留对立点
3. 输出自已判断
4. 分类为 Required / Recommended / Optional / Reject

子代理不可用时，单独明确分 5 视点评估，写明`未使用子代理`。

## Step 5.5: 实现计划验证关卡

实现计划在满足以下 5 个之前不设为 Required。

| Gate | 看什么 | 未满足时 |
|------|----------|------------|
| Spec / Plans Fit | 不与 root `spec.md`、sub-spec、`Plans.md` 的顺序矛盾 | 先输出 `Spec delta` 或 Reject |
| Memory / Wheel Check | harness-mem / harness-recall / repo memory 中是否有同类判断或现有 task | 重用现有案，仅将差分 task 化 |
| Product Fit | 是否直连产品目的和 primary user workflow | 逃到 docs / external workflow / Optional |
| Security Fit | 是否不弱化权限、秘密信息、外部发送、dependency、branch/release gate | spike / security task / Reject |
| Quality Baseline Fit | source code changes 是否可通过 lint / formatter / CI command 以 Yes/No 判定质量 | 先行 setup task，或保留 formatter_baseline 的 skip reason |
| Works In Practice | 是否可通过 test / smoke / CI / review / release closeout 以 Yes/No 判定 | 重做 DoD |

此关卡是"为减少返工的前工程"，非感想评审。
未满足的关卡必须反映到 Plans.md 的 DoD、Depends 或 `[needs-spike]`。
Quality Baseline Fit 不是杂乱添加 formatter 或 linter 的借口。
未设定且包含 source code changes 的 plan，在实现 task 之前放置 setup task。
setup task 的 DoD 包含 config、package script / CI command、validation command 3 点。
planning 中不进行 package install。导入由 harness-work 作为 setup task 执行。
广范围的一量 reformat 仅在用户明确时，或在其 setup task 的 scope 内执行。
Security Fit 不要求 secret 的实际读取。
需要读取 `.env`、tokens、private keys、customer data 等时作为 Risk Gate 停止。
通过不读秘密值的现有 guardrail、config shape、audit evidence、测试、GitHub / CI metadata 等确认。

## Step 6: 中立评分评审

评分为 5 分制。5 分为好状态，1 分为弱状态。

| 轴 | 5 分 | 3 分 | 1 分 |
|----|-----|-----|-----|
| Product Fit | 直连导入产品核心 | 方便但周边 | 别产品或运用足够 |
| Evidence Strength | 一次信息 + 实测 + 有既有根拠 | 仅确认一方 | 推测中心 |
| User Value | 判断质量和执行速度大升 | 部分 workflow 有效 | 体感价值薄 |
| Implementation Feasibility | 小且局地 | 中规模但可管理 | 大规模维护负荷大 |
| Regression Safety | 低风险可测试 | 有影响范围 | 易坏现有 flow |
| Strategic Leverage | 成长期差异化 | 止于便利功能 | 一过性 |
| Security Safety | 不弱化权限和秘密信息可验证 | 有注意点 | 有危险权限缓和或未验证外部发送 |
| Works In Practice | 可通过 smoke / CI / review 实证 | 手动确认中心 | 动作确认模糊 |

修正规则:

- Evidence Strength 2 以下禁止 Required
- Regression Safety 2 以下时，先放置 spike / spec / test
- Security Safety 2 以下禁止 Required
- Works In Practice 2 以下时，重做 DoD 或落到 spike
- Quality Baseline Fit 2 以下且包含 source code changes 时，将 formatter_baseline setup task 设为 Required dependency
- Implementation Feasibility 2 以下且 User Value 3 以下时偏向 Reject
- Product Fit 2 以下时，不放入此产品，逃到 docs / external workflow

## Step 7: `$easy` 报告

最终输出不直接输出困难的评价，转换为可判断的形式。

必须构成:

```markdown
一句话:
{{采用判断为 1 句}}

评分评审:
| 案 | 分数 | 判定 | 根据 | 未验证 |
|----|------|------|------|--------|

应采用的提案:
| 优先 | 提案内容 | 理由 | 会怎样 |
|------|----------|------|--------------|

退化确认:
- team_validation_mode:
- 规格:
- Plans.md:
- harness-mem / 记忆:
- TeamAgent / 子代理:
- product fit:
- security:
- works in practice:
- formatter_baseline:
- mirror / 分发:
- test:

下一步做:
1. ...
2. ...
3. ...
```

文体规则:

- 先出结论
- 专业术语立即简短翻译
- 不以"厉害""革新"等氛围判断
- 提案限制为 1〜3 个。不过多并列候选
- 区分事实、推测、未验证

## Step 8: 落到 Plans.md / spec 时

仅将采用的方案转换为 task contract。

顺序:

1. 读取 root `spec.md`，必要时先作为 `Spec delta` 更新 product contract
2. 有 source code changes 且 lint / formatter baseline 未设定时，先将 formatter_baseline setup task 作为 Required dependency 放置
3. 向 Plans.md 仅添加 Required task
4. 对高风险方案附加 `[needs-spike]`
5. 为各 task 放置可验证的 DoD
6. 对需要 TDD 的 task 附加 `[tdd:required]`
7. 影响到 mirror / i18n / package surface 时，另外放置验证 task
8. 不需 spec 更新时，在 task context / sprint contract 中保留 `Spec skip reason`
9. non-trivial planning 中保留 TeamAgent / 子代理验证结果，或 `未使用子代理` fallback 和 5 gate 的结果到 task context
10. `team_validation_mode: unavailable` 的 plan 不设为 Required。仅轻量 task 允许 `not_required_lightweight`

`Spec delta` 由 agent draft。不前提用户从零开始写 spec。
`Spec delta` / `Spec skip reason` 由 Harness 生成，consumer 仅进行批准・修正。

禁止:

- 规格正解条件摇摆时仅创建实现 task
- 不将退化确认 task 化而以"注意"了结
- 包含 source code changes 时忽视 lint / formatter baseline 不在而仅创建实现 task
- 省略 docs-only / mechanical task 的 `Spec skip reason`
