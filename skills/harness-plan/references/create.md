# create 子命令 — 计划创建流程

听取想法和需求，生成可执行的 Plans.md。

**Precedence**: root `spec.md` > sub-spec > Plans.md（product contract 优先于 task ledger）。

## Step 0: 确认对话上下文

如果能从最近的对话中提取需求则进行确认:

> 请选择计划创建方式:
> 1. 从最近的对话 — 基于讨论内容创建计划
> 2. 从零开始 — 从需求收集开始

选择"从最近的对话"时: 提取需求・想法・决定事项并请用户确认。
确认后，跳转到 Step 3（技术调研）。

## Step 1: 询问要做什么

若无用户输入则询问:

> 要构建什么？
>
> 示例: 预约管理系统 / 博客网站 / 任务管理应用 / API服务器
>
> 粗略的想法即可！

## Step 2: 提升清晰度（最多3问）

> 请再告诉我一些信息:
>
> 1. 谁会使用？（仅自己？团队？公开发布？）
> 2. 有参考服务吗？
> 3. 要做多少？（MVP？完整功能？）

## Step 3: 计划质量检查

详细说明: `references/planning-quality.md`

不要将用户提供的信息直接落到 Plans.md。
包含外部产品、竞争、规格方案、改善方案、比较材料时，通过最新信息・现有规格・记忆・TeamAgent / 子代理多视点评审进行确认，仅将应采用的要素转换为 task contract。
非单发・轻微的 planning 以 TeamAgent 或子代理为前提处理。

最低限度的确认:

- 最新信息: 优先 WebSearch / 公式 docs / 一次信息，重要点用多个源确认
- 现有规格: 确认 Plans.md、README、docs、CLAUDE.md、相关 skill、tests
- 记忆: 如果能使用 harness-mem / harness-recall / `.claude/agent-memory/` / `.claude/state/` 则按 project-scoped 确认，避免重复发明轮子
- 讨论: 从 Product / Architecture / Security / QA / Skeptic 视点区分采用价值和风险
- 质量基础: 包含 source code changes 的 plan 确认 `formatter_baseline`，lint / formatter 未设定时先放置 setup task
- 实现计划验证: 确认 product fit、security fit、works in practice，将 test / smoke / CI / review / release gate 落到 DoD
- 评分: 以 5 分制评价 Product Fit、Evidence Strength、User Value、Implementation Feasibility、Regression Safety、Strategic Leverage、Security Safety、Works In Practice

不直接读取 `harness-mem` 的 DB。无法使用搜索或 documented memory surface 时明确标注"记忆未确认"。
无法使用 Task tool 时明确标注`未使用子代理`，单独分点评估相同观点。
`team_validation_mode` 输出 `not_required_lightweight` / `native` / `subagent` / `manual-pass` / `unavailable` 中的任一。
non-trivial planning 使用 `native` / `subagent` / `manual-pass` 中的任一，不保持 `unavailable` 状态作为 Required。
Product / Architecture / Security / QA / Skeptic 是 perspective 名而非 agent_type 名。
Security gate 不要求实际读取 `.env` 或 secret。

仅小的 typo、format、README/CHANGELOG、marker 更新可轻量处理此步骤。

## Lane taxonomy / stage gate / unknown data contract

Fast / Gate / Release 与 kickoff / understand / tasking / pair / showcase / respond 都不是新 skill，而是 **Plans metadata**（Content 或 DoD 开头的标签）。
5 column 模板不变。

### Lane taxonomy

| 标签 | 何时使用 |
|------|---------|
| `[lane:fast]` | low-risk local work（refactor / docs / typo） |
| `[lane:gate]` | spec / workflow / mirror / guardrail / 大部分功能实现 |
| `[lane:release]` | public artifact / version / tag / GitHub Release |

### Stage taxonomy

| 标签 | 何时使用 | Skill 映射 |
|------|---------|------------|
| `[stage:kickoff]` | 需求刚进入，需对齐目标、用户、边界、Story Card / freeze gate | `harness-plan create`、`harness-plan-brief` |
| `[stage:understand]` | 需要调研 spec、repo、memory、unknown data 或比较方案 | `harness-plan create`、`memory`、`harness-plan-brief` |
| `[stage:tasking]` | 需要拆任务、确定 DoD / Depends、确认 formatter baseline、整理 Plans.md | `harness-plan create`、`harness-sync` |
| `[stage:pair]` | 进入实现、TDD、测试修复、evidence collect / handoff | `harness-work`、`test-driven-development` |
| `[stage:showcase]` | 需要 review、acceptance、演示、Quality Quadrants 分类 | `harness-review`、`harness-accept`、`requesting-code-review` |
| `[stage:respond]` | 需要交付总结、状态同步、release / closeout、下一步建议 | `harness-work`、`harness-sync`、`harness-release` |

`[stage:*]` 描述任务在 evidence-driven delivery loop 中的位置；`[lane:*]` 描述执行路径/风险。两者必须可以并存，不能用 stage 替代 lane。
### Stage gate

planning output 按以下 5 个阶段结构化:

1. 验证・调查 — research evidence、`unknown` 数据明示
2. 实现计划确定 — lane 标签 + DoD 确定
3. 实现(TDD) — `[tdd:required]` / `[tdd:skip:<reason>]`
4. 评审 — 将 review artifact 落到 DoD
5. PR closeout — evidence pack → PR body

### Unknown data contract

`not_observed != absent`。failed search / 未读 file / missing fixture / API unavailable 为 **`unknown`**。
仅在 repo evidence 可确认时断定不存在。

### Stage + lane examples（并存示例）

```markdown
| 1.1 | `[Contract]` `[stage:kickoff]` `[lane:gate]` `[tdd:skip:docs-contract]` Story Card freeze gate 定义 | spec.md 含 Story Card 边界与 freeze 条件 | - | cc:TODO |
| 1.2 | `[Implementation]` `[stage:pair]` `[lane:gate]` `[tdd:required]` evidence writer 実装 | writer tests PASS、evidence.v1 JSON valid | 1.1 | cc:TODO |
| 1.3 | `[Review]` `[stage:showcase]` `[lane:fast]` `[tdd:skip:review-only]` Quality Quadrants review | review artifact 含 Q1-Q4 分类 | 1.2 | cc:TODO |
```
### Lane examples（最小示例）

`[lane:fast]`:

```markdown
| 1.1 | `[Docs]` `[lane:fast]` `[tdd:skip:docs-only]` CHANGELOG typo 修正 | diff 確認、validate-plugin PASS | - | cc:TODO |
| 1.2 | `[Refactor]` `[lane:fast]` `[tdd:skip:behavior-unchanged]` 関数名を rename（挙動不変） | 既存テスト全 PASS | - | cc:TODO |
| 1.3 | `[Format]` `[lane:fast]` `[tdd:skip:style-only]` markdown 表の列揃え | git diff --check PASS | - | cc:TODO |
```

`[lane:gate]`:

```markdown
| 2.1 | `[Contract]` `[lane:gate]` `[tdd:skip:docs-contract]` spec.md に API 契約を追加 | spec.md に rule 記載、git diff --check PASS | - | cc:TODO |
| 2.2 | `[Feature]` `[lane:gate]` `[tdd:required]` status marker writer 実装 | writer tests PASS、legacy read 互換 | 2.1 | cc:TODO |
| 2.3 | `[Guardrail]` `[lane:gate]` `[tdd:required]` protected path policy 更新 | guardrail tests PASS | 2.1 | cc:TODO |
```

`[lane:release]`:

```markdown
| 3.1 | `[Version]` `[lane:release]` `[tdd:skip:release-prep]` VERSION / plugin.json 同期 | sync-version.sh --check PASS | 2.2, 2.3 | cc:TODO |
| 3.2 | `[Release]` `[lane:release]` `[tdd:skip:release-automation]` tag + GitHub Release | harness-release 完了、Release URL 記録 | 3.1 | cc:TODO |
| 3.3 | `[Dependency]` `[lane:release]` `[tdd:skip:dependency-bump]` Dependabot major merge + main CI green | merge commit + main validate-plugin PASS | - | cc:TODO |
```

## Step 4: 技术调研（WebSearch）

不询问用户，由 Claude Code 调研・提案。

```
WebSearch:
- "{{项目类型}} tech stack 2025"
- "{{类似服务}} architecture"
```

## Step 4.4: spec.md / Plans.md 双正本检查

Plans.md 是固定"要做什么"的 task contract。
root `spec.md` 是固定"什么是正确的"的 product contract。
不混淆这二者。`/harness-plan create` 不是 Plans.md 生成命令，而是返回 spec.md product contract and Plans.md task contract 的 co-required planning output 的 surface。
precedence 仍维持 `spec.md > sub-spec > Plans.md`。

`/harness-plan create` 的输出必须包含以下 2 点:

1. `Spec delta` 或 `Spec skip reason`
2. `Plans.md` task 生成

每次读取 root `spec.md`。实现判断可能产生偏差时，在创建 Plans.md 之前更新 root `spec.md`。
不让用户从零开始写 spec。`Spec delta` / `Spec skip reason` 由 Harness 生成，consumer 仅进行批准・修正。
agent 根据现有 spec、repo evidence、记忆、测试、输入需求 draft 最小 delta，仅在判断分歧时输出选择。

### 创建/更新规格正本的条件

- 增加或改变用户可见行为
- 决定 API、数据模型、权限、计费、外部联动、tenant boundary
- 有多个实现方案，选择方式会影响 product behavior
- 在过去或本次对话中，看到规格模糊导致的实现 drift
- Plans.md 中有 task，但 project 的正解条件未文档化

### 可跳过的条件

- 仅 typo / format / lint
- 仅 dependency bump
- 仅 README / CHANGELOG
- docs-only / mechanical task
- 无行为变更的狭义 refactor
- 现有 spec 和测试已明确正解

跳过时也不省略 `Spec skip reason`。
docs-only / mechanical task 也在 task context / sprint contract 中保留 skip reason。

### 保存位置

最优先 root `spec.md`。
仅在 consumer repo 没有 root `spec.md` 时，更新现有的 project-level spec 作为 fallback。
既无 root `spec.md` 也无现有 project spec 时创建:

```text
docs/spec/00-project-spec.md
```

最初的 spec 可以简短。最低包含 Purpose、Users And Workflows、Core Rules、Data And Contracts、Non-Goals、Open Decisions、Links。

详细说明: `docs/plans/spec-ssot.md`

## Step 4.6: lint / formatter baseline 检查

包含 source code changes 的 plan，在创建实现 task 之前确认 lint / formatter baseline。
这不是"清理工作"，而是预先创建能在实现后以 Yes/No 确认质量的基础。

确认内容:

- JavaScript / TypeScript: `package.json` 的 `lint` / `format` scripts、ESLint / Prettier / Biome / Oxlint / dprint 的 config 或 dependency
- Python: `pyproject.toml` 的 Ruff / Black / isort / mypy 等 config
- Go: `gofmt` / `go test` / `go vet` / 相当于 lint 的 CI command
- Rust: `cargo fmt` / `cargo clippy` / `cargo test`
- 现有 CI: `.github/workflows`、`scripts/ci/*`、`Makefile` 等质量 command

输出需保留 `formatter_baseline`:

```text
formatter_baseline: configured | missing | not_applicable | unknown
formatter_baseline_evidence: [看到的 file / command]
formatter_baseline_action: none | add_setup_task | skip_with_reason | spike
```

未设定且包含 source code changes 时，在 Plans.md 的实现 task 之前添加 setup task。
setup task 的 DoD 为"config / script / validation command 齐备，明确将广范围的一批量 reformat 排除在 scope 外"。
planning 中不进行 package install。导入作业由 harness-work 作为 setup task 执行。

可跳过的条件:

- docs-only / markdown-only / changelog-only
- 有现有 lint / formatter / CI command，充分覆盖本次变更涉及的语言
- consumer repo 约束导致无法导入。此时保留 `formatter_baseline_action: spike` 或 skip reason

## Step 5: 功能列表提取

从需求提取具体功能列表。

示例: 预约管理系统的情况
- 用户注册/登录
- 预约日历显示
- 预约的创建/编辑/取消
- 管理员仪表板
- 邮件通知
- 支付功能

## Step 5.5: optional brief 生成

仅在必要时附加 brief。brief 不替代 Plans.md，是简短固定实现前提的辅助资料。

- 包含 UI 的 task 使用 `design brief`
- 包含 API 的 task 使用 `contract brief`
- UI 和 API 混在时分开 brief

### design brief

UI task 的 brief 最低包含:

- 想达成什么
- 谁使用
- 重要画面状态
- 外观和操作感的约束
- 完成条件

### contract brief

API task 的 brief 最低包含:

- 接收什么 / 返回什么
- 输入验证条件
- 失败时的行为
- 外部依赖
- 完成条件

## Step 6: 优先度矩阵创建（2轴评估）

以 **Impact（影响度）× Risk（风险/不确定性）** 的 2 轴评估各功能:

- **Impact**: 用户价值 × 目标用户数（高/低）
- **Risk**: 技术未知 × 外部依赖（高/低）

| Impact＼Risk | 低风险 | 高风险 |
|-------------|---------|---------|
| **高 Impact** | ★ **Required** — 最优先（确实有价值） | ▲ **Required + [needs-spike]** — 需要早期验证 |
| **低 Impact** | ○ **Recommended** — 有余力时处理 | ✕ **Optional** — 延后 or 缩小范围 |

### `[needs-spike]` 标记

高 Impact × 高 Risk 的任务自动附加 `[needs-spike]` 标记。
附加 `[needs-spike]` 的任务，自动生成 **spike（技术验证）任务** 并优先执行:

```markdown
| N.X-spike | [spike] {{任务名}} 的技术验证 | 生成验证结果报告 | - | cc:TODO |
| N.X       | {{任务名}} [needs-spike] | {{DoD}} | N.X-spike | cc:TODO |
```

spike 任务的完成条件为"保留验证结果报告（可实现/不可实现/需设计变更）"。

## Step 6.5: TDD 跳过判断（默认启用）

TDD 默认启用。仅对符合以下任一条件的任务附加 `[skip:tdd]` 标记跳过:

| 跳过条件 | 理由 |
|-------------|------|
| 仅文档/注释 | 不影响执行代码 |
| 仅配置文件（JSON, YAML, .env） | 无测试逻辑 |
| 1 行以下简单修正（typo） | 测试成本超过效果 |
| 仅样式/格式变更 | 不影响行为 |
| 仅依赖更新 | 无实现逻辑变更 |
| README/CHANGELOG 更新 | 仅文档 |
| 重构（无行为变更） | 已被现有测试覆盖 |

不符合上述条件的任务自动应用 TDD（推荐测试先行）。

## Step 6.7: Plans.md v3 格式规范

Plans.md v3 包含以下格式扩展:

### Phase 头部的 Purpose 行（可选）

各 Phase 的头部可记载 1 行 Purpose（目的）。无输入时省略:

```markdown
### Phase N.X: [阶段名] [Px]

Purpose: [此阶段解决的课题，1 行]
```

- **默认**: 不要求输入（空时省略）
- **记载时的效果**: 在 breezing Phase 0 的 scope 确认时显示
- **生成规则**: 仅在用户明确陈述阶段目的时自动记载

### Artifact 记法（Status 列）

任务完成时在 Status 附加 commit hash:

```markdown
| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1  | ... | ... | - | cc:完成 [a1b2c3d] |
| 1.2  | ... | ... | 1.1 | cc:TODO |
```

- **形式**: `cc:完成 [7字符hash]`
- **附加时机**: 在 `harness-work` Solo Step 7 自动附加
- **向后兼容**: 无 hash 的 `cc:完成` 继续有效

### 影响文件列表

与 v3 格式相关的文件:

| 文件 | 影响 |
|---------|------|
| `skills/harness-plan/references/create.md` | Step 6 模板添加 Purpose 行 |
| `skills/harness-plan/references/sync.md` | 差异检测识别 `cc:完成 [hash]` 格式 |
| `skills/harness-work/SKILL.md` | Solo Step 7 附加 hash，失败时重新票据化 |
| `skills/harness-sync/SKILL.md` | --snapshot 保存快照 |
| `skills/breezing/SKILL.md` | Progress Feed 显示进度 |

## Step 7: Plans.md 生成

先输出 `Spec delta` 或 `Spec skip reason`，之后自动生成质量标记 + DoD + Depends 生成 Plans.md。

### Spec result 输出

`Spec delta` / `Spec skip reason` 由 Harness 生成，consumer 仅进行批准・修正。

```markdown
Spec delta:
- path: spec.md
- change: [追加/变更的 product rule]
- why: [作为此 task contract 前提必要的理由]

Plans.md:
| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
```

```markdown
Spec skip reason:
- path checked: spec.md
- reason: [docs-only / mechanical task / 现有 spec 和测试已固定正解]
- preserve in: task context or sprint contract

Plans.md:
| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
```

### 质量标记附加逻辑
```
分析任务内容
    ↓
├── "auth" "login" "API" → [feature:security]
├── "component" "UI" "screen" → [feature:a11y]
├── "fix" "bug" → [bugfix:reproduce-first]
├── "docs" "comment" "README" "CHANGELOG" → [skip:tdd]
├── "config" "json" "yaml" "env" → [skip:tdd]
├── "style" "format" "lint" → [skip:tdd]
├── "refactor" (无行为变更) → [skip:tdd]
├── "payment" "billing" → [feature:security]
└── 其他 → 无标记（TDD 默认启用）
```

### DoD 自动推理逻辑

根据任务的"内容"以关键字为基础推理 DoD，自动填充:

| 任务内容关键字 | DoD 推理 |
|---------------------|---------|
| "创建" "新建" "添加" | 文件存在，具有预期结构 |
| "测试" "test" | 测试通过（`npm test` / `pytest` 等） |
| "修正" "fix" "bug" | 问题不再重现 |
| "UI" "画面" "组件" | 显示确认（截图或浏览器） |
| "API" "端点" | curl/httpie 确认响应 |
| "设置" "config" | 设置值生效 |
| "文档" "docs" | 文件存在，无链接损坏 |
| "迁移" "DB" | 可执行迁移 |
| "重构" | 现有测试全部通过 + lint 错误 0 |

推理结果仅为默认值。用户指定具体验收条件时优先。

### Depends 自动推理逻辑

按以下规则推理阶段内任务间的依赖关系:

1. **DB/模式系任务** → 被其他实现任务依赖（先行任务）
2. **UI 任务** → 依赖 API/逻辑 任务（后续任务）
3. **测试/验证任务** → 依赖实现任务（最后）
4. **设置/环境任务** → 被其他任务依赖（先行任务）
5. **无明确依赖的任务** → `-`（可并行执行）

推理无自信时设为 `-`，请求用户确认。

**生成模板**:

```markdown
# [项目名] Plans.md

创建日: YYYY-MM-DD

---

## Phase 1: [阶段名]

Purpose: [阶段的目的（可省略）]

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1  | [任务说明] [feature:security] | [可验证的完成条件] | - | cc:TODO |
| 1.2  | [任务说明] | [可验证的完成条件] | 1.1 | cc:TODO |
```

**Purpose 行**:
- 仅在用户陈述阶段目的时自动记载
- 无输入时省略 Purpose 行（不空行）
- 以 1 行完成（禁止多行）

**DoD（Definition of Done）记法**:
- 以可验证的 1 行书写（例: "测试通过""可执行迁移""lint 错误 0"）
- 禁止"感觉良好""正常运行"。以 Yes/No 可判定的形式

**Depends 记法**:
- 无依赖: `-`
- 单一依赖: 任务编号（例: `1.1`）
- 多重依赖: 逗号分隔（例: `1.1, 1.2`）
- 阶段依赖: 阶段编号（例: `Phase 1`）

### Team mode output

仅用户明确 team mode 时，另行提示 issue bridge 的 dry-run。

- tracking issue 仅 1 个
- 并排每个 task 的 sub-issue payload
- Plans.md 保持为正本
- 提示可直接使用 `scripts/plans-issue-bridge.sh --team-mode` 的 dry-run 形式

## Step 8: 必须提示会话启动命令和首次输入

Plans.md 输出后，为使用户不迷失下一步，
**必须**成套提示 **新会话启动命令** 和
**启动后可直接输入的首次输入**。

### 提示规则

1. 至少写 1 组具体启动命令 + 首次输入
2. 可能的话限制为"最有力的 1 组 + 替代 1 组"
3. 不仅命令，还以 1 行附加为何此组合
4. 长时任务优先提示 `bash scripts/claude-longrun.sh`

### 推荐映射

| 情况 | 启动命令 | 首次输入 |
|------|--------------|------------|
| 从第 1 个任务开始 | `claude` | `/harness-work 1.1` 等单任务执行 |
| 多任务一起推进 | `claude` | `/breezing all` |
| 直列全部推进 | `claude` | `/harness-work all` |
| 长时・再入前提 | `bash scripts/claude-longrun.sh` | `/harness-loop all` |

### 输出示例

```text
下一步:
- 新会话启动命令: claude
- 启动后首次输入: /breezing all
- 适用场景: 本次 Plans.md 是多任务一起推进的构成，团队执行最自然
```

```text
下一步:
- 新会话启动命令: bash scripts/claude-longrun.sh
- 启动后首次输入: /harness-loop all
- 适用场景: 长时任务，容易发生超过 5 分钟的等待或重开
```

## Step 9: 下一步行动指引

> Plans.md 完成！
>
> 下一步:
> - `harness-work` 开始实现
> - 或说"从 Phase 1 开始"
> - 添加功能: `harness-plan add [功能名]`
> - 延后功能: `harness-plan update [任务] blocked`

## CI 模式（--ci）

无需需求收集。直接使用现有 Plans.md，仅进行任务分解。

1. 读取 Plans.md
2. cc:TODO 任务按优先级列表化
3. 可并行任务附加 `[P]` 标记
4. 提示下个执行任务

## Story Card / Freeze Gate

Before `harness-plan create` enters `understand`, it must produce a Story Card and explicitly decide whether the card is frozen. The freeze gate prevents implementation tasks from being split while the target, user, boundary, or acceptance evidence is still ambiguous.

### Story Card template

```markdown
## Story Card

- Story: [one-sentence user outcome]
- User/Actor: [primary user, system, or operator]
- Boundary: [included scope, excluded scope, and integration boundary]
- Acceptance: [Given/When/Then criteria or other yes/no checks]
- Evidence: [test, artifact, metric, or observation that proves acceptance]
- Open Questions: [unresolved decisions; use `none` when empty]
- Freeze Decision: `frozen` | `not frozen`
- Next Stage: `understand` | `clarify`
```

### Freeze rules

1. Align the Story, User/Actor, Boundary, Acceptance, and Evidence fields with the user before freezing.
2. Record every unresolved decision in Open Questions; do not hide ambiguity inside implementation tasks.
3. Enter `understand` only when `Freeze Decision: frozen` and `Next Stage: understand` are both explicit.
4. When the card is not frozen, keep `Next Stage: clarify` and do not split implementation tasks in `Plans.md`.
5. A later scope change reopens the gate and requires a new freeze decision before task decomposition continues.
