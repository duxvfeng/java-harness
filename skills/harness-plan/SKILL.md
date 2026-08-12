---
name: harness-plan
description: "HAR: Research-backed, team-validated task planning with brainstorming integration, Plans.md management, progress sync. Uses creative exploration for complex features. Trigger: create a plan, add tasks, update Plans.md, mark complete, check progress."
description-zh: "HAR：带有创意探索的任务计划、Plans.md 管理和进度同步。在规划复杂功能时使用 brainstorming 进行创造性思考。当用户提到创建计划、添加任务、更新 Plans.md、标记完成、检查进度时启动。"
trigger: "create a plan, add tasks, update Plans.md, check progress, brainstorming, creative planning, explore options"
argument-hint: "[create] [add] [update] [sync] [list] [switch] [--ci] [--plan <name>]"

# Brainstorming Integration
brainstorming_enabled: true
integration_mode: "auto-trigger"
creative_exploration: true
---

# Harness Plan

Harness 的集成计划技能。

## Java 版本边界

Java CLI 的 `plan`、`add`、`update` 和 `sync` 命令负责路由到本技能文本，并不执行 Go 版本的 `plan-registry.sh`、`plans-issue-bridge.sh` 或其他 helper。Java 项目以 `Plans.md` 为任务正本，规格以 root `spec.md` 为产品正本，任务契约使用 `harness sprint-contract`，状态和健康检查使用 `harness status` 与 `harness doctor`。

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

## 无参数时的帮助提示

当用户输入 `/harness-plan` 不带任何子命令参数时，**必须**显示以下帮助提示：

```text
# Harness Plan - 计划管理技能

可用子命令：

📋 create — 创建新计划
  用途: Spec delta / skip reason → Plans.md task 生成
  触发: "制定计划" / `/harness-plan create`
  说明: 听取想法和需求，生成可执行的 Plans.md

➕ add — 添加任务
  用途: 向 Plans.md 添加新任务
  触发: "添加任务" / `/harness-plan add`
  说明: 对于 product-impacting 的添加，会同时输出 Spec delta

✅ update — 标记更新
  用途: 将任务标记改为 cc:完成、cc:WIP 或 blocked
  触发: "标记完成" / `/harness-plan update`
  示例: `/harness-plan update 12.13 完成`

🔄 sync — 进度同步
  用途: 对照实现与 Plans.md，检测并更新差异
  触发: "现在在哪？" / `/harness-plan sync` 或 `/harness-sync`
  说明: 自动分析 git 状态和 agent trace，更新任务状态

📑 list — 列出命名计划
  用途: 一览 plans/manifest.json 的 named Plans
  触发: `/harness-plan list`
  说明: 显示所有可用的命名计划

🔀 switch — 切换活动计划
  用途: 将 active plan 保存到 .claude/state/active-plan.json
  触发: `/harness-plan switch <name>`
  说明: 在多个 Plans.md 之间切换

---

下一步：
• 创建新计划 → /harness-plan create
• 查看当前进度 → /harness-plan sync
• 添加单个任务 → /harness-plan add
• 切换计划 → /harness-plan list 然后 /harness-plan switch <name>
```

**输出规则**:
- **必须**使用上述格式，包括 emoji 图标
- **必须**在用户仅输入 `/harness-plan` 时自动显示，无需额外确认
- 显示后等待用户选择下一步操作

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
`Spec delta` / `Spec skip reason` 由 Harness 生成，consumer 仅进行批准・修正。

**Non-trivial planning gate**:

非单发・轻微任务的 planning 以 TeamAgent 或子代理为前提处理。
这里的 non-trivial 指影响多个 task / 多个 file / 多个 session / 产品行为 / API / 数据模型 / 权限 / 计费 / 外部联动 / 分发面 / 安全的请求。
可使用 Task tool 时，运行 Product / Architecture / Security / QA / Skeptic 的独立视点。
不可使用时明确显示 `未使用子代理`，单独分点评估相同观点。

non-trivial planning 的输出必须包含以下验证。

- `team_validation_mode`: `not_required_lightweight` / `native` / `subagent` / `manual-pass` / `unavailable`
- `spec.md` / sub-spec / `Plans.md` 的整合性
- 通过 harness-mem / harness-recall / repo memory 防止重复发明轮子的确认
- 未脱离产品目的
- 安全、权限、秘密信息、供应链无问题
- 是否有 lint / formatter baseline。包含 source code changes 的计划中未设定时，在实现 task 前放置 setup task
- 是否能正常工作的计划。即 test / smoke / CI / review / release gate 是否落到 task DoD

轻量 task 可以 `team_validation_mode: not_required_lightweight`。
non-trivial planning 使用 `native` / `subagent` / `manual-pass` 任一。
不能保持 `unavailable` 状态作为 Required。
Product / Architecture / Security / QA / Skeptic 是验证 perspective，而非 agent_type 名。
向可用的 TeamAgent / Task 子代理作为 perspective 请求，不要求任意 agent spawn。
Security gate 不要求秘密信息的实际读取。
需要读取 `.env` 或 secret 时作为 Risk Gate 停止，用允许的现有 guard / evidence 确认。

**适用场景**:

- 用 `create` 创建新计划
- 用 `add` 添加影响产品行为 / API / 权限 / 计费 / 外部联动 / 分发面的 task
- 用户提供了外部产品、竞争、规格方案、改善方案、比较材料
- 与现有规格或过去判断有冲突风险

**可轻量处理场景**:

- 仅 marker 更新的 `update`
- 仅 status 核对的 `sync`
- typo、format、仅 README/CHANGELOG
- 正解已由现有 spec 和 test 固定的狭义更改

**质量流程**:
1. 分解输入信息，明确评估对象・评分轴・不确定事实
2. 获取最新信息。外部事实优先 WebSearch / 官方文档 / 一次信息，重要点用多源交叉检查
3. 确认现有规格・root `spec.md`・Plans.md・README・docs・CLAUDE.md・相关 skill
4. 确认 harness-mem / harness-recall / `.claude/agent-memory/` / `.claude/state/` 等可用记忆面的 project-scoped
5. non-trivial planning 时使用 TeamAgent / Task 子代理，从 Product / Architecture / Security / QA / Skeptic 等不同视点进行独立评审
6. 包含 source code changes 的 plan 确认 lint / formatter baseline，未设定时先放置 setup task
7. 输出中立的评分评审，分类为 Required / Recommended / Optional / Reject
8. 以 `$easy` 形式报告提案内容、理由、会发生什么
9. 仅将采用的方案落实到 root `spec.md` / Plans.md / test task

### Lane Taxonomy + Stage Gate

Fast / Gate / Release 作为 **Plans metadata** 处理，而非新 skill。Plans.md 的 5 column 模板（Task / 内容 / DoD / Depends / Status）保持不变，
将 lane（`[lane:fast]` / `[lane:gate]` / `[lane:release]`）・stage（验证→计划→TDD实现→评审→PR closeout 的 5 个阶段）・unknown data contract（`not_observed != absent`，无法确认的事实明确标注为 `unknown`）
嵌入到 **内容（Content）或者 DoD 的开头**。标签列表、worked example、各阶段 DoD 示例请参考
[references/create.md](${CLAUDE_SKILL_DIR}/references/create.md)。

### create — 计划创建

See [references/create.md](${CLAUDE_SKILL_DIR}/references/create.md)

听取想法和需求，生成可执行的 Plans.md。

**流程**:
1. 确认对话上下文（从最近的讨论中提取 或 新的需求收集）
2. **创意探索（Brainstorming 集成）** - 对于复杂功能或新特性，启动 brainstorming 进行创意探索
   - 触发条件：新功能设计、架构决策、复杂问题解决、创新需求
   - 调用 `brainstorming` skill 进行多角度思考
   - 生成创意选项、潜在解决方案、风险评估
   - 为后续计划提供多样化的思路和选择
3. 询问要做什么（最多 3 个问题）- 可在 brainstorming 结果基础上进行更精准的需求收集
4. **计划质量检查**（最新信息、现有规格、记忆、TeamAgent / 子代理多视点评审、评分）
5. 技术调研（WebSearch）
6. 功能列表提取
7. **spec.md / Plans.md 双正本检查**（Spec delta 或 Spec skip reason + Plans.md task）
8. 优先级矩阵（Required / Recommended / Optional / Reject）
9. TDD 采用判断（测试设计）
10. Plans.md 生成（带 `cc:TODO` 标记）
11. **事前确认章节生成**（plan-time pre-approval）
11. 下一步行动指引

### create — 事前确认章节（plan-time pre-approval）

使用 `create` 确定计划时，在输出 Plans.md tasks 后、批准之前，**必须**生成 **事前确认章节**。
目的不是通过常驻 allowlist 允许一切操作，而是在计划批准时一次性提前确认每个工作范围内"可能发生的 stop / ask"。

提取对象:

- 各 task 的目标文件、相关 path、预期变更范围
- DoD 中写的验证命令、PR closeout 命令、外部 API / CLI 调用
- `secret-read path`（`.env*`, `secrets/**`, `*.pem`, `*.key`, `.ssh/**`, `.aws/**`, `credentials` 等）
- 外部发送（`git push`, `gh pr create`, `gh api`, `curl` / API call, release / publish / deploy）
- 破坏性操作（`rm -rf`, migration destructive step, force push, production apply）

固定格式:

```text
## 事前确认
- 事项: <secret-read / external-send / destructive 的具体操作>
  理由: <DoD 或者 task 执行上必要的理由，1 行>
  scope: Phase <phase> / Task <task>
```

输出规则:

- 1 行的 `理由` 不得包含 secret 值。仅限于 path / 命令名 / 目标服务。
- 计划批准时，一揽子提示事前确认章节的所有事项，获得用户的批准 / 否决。
- 批准结果记录到 `.claude/state/plan-preapprovals.json`，格式为 `plan-preapproval.v2`。schema 参考 `templates/schemas/plan-preapproval.v2.json`。v1 仅用于现有记录的读取兼容。
- 记录保持 `事项 + 理由 1 行 + scope (phase/task)`。在 `operations` 中列举 `secret-read` / `external-send` / `destructive`。在 `paths` / `commands` / `targets` 中列举对象。包含 `decision`、`approved_at`、RFC3339 格式的 `expires_at`。
- `max_uses` 设置包含必要重试次数的上限。省略时为 10 次。`uses` 在新批准时设为 0。
- 确认仅在计划批准时进行 1 次。在 `harness-work` / `breezing` 执行过程中，不得仅以已声明事项为由使用 `AskUserQuestion`。
- 记录中未列出的未计划 secret-read / 外部发送 / 破坏性操作，照旧通过 runtime floor / ask 停止。不缩小安全网。
- secret-read 的批准不是显示 secret 值的许可。声明必要的最小 path，作为 work 开始时向 project config 的 `runtimefloor.secretAllow` 反映 per-run 的输入。

### spec.md / Plans.md 双正本检查（默认）

Plans.md 作为"要做什么"的 task contract，root `spec.md` 作为"什么是正确的"的 product contract。
co-required planning output 意味着必须输出两者，precedence 仍维持 `spec.md > sub-spec > Plans.md`。
当实现可能产生偏差时，在生成 Plans.md 之前更新 root `spec.md`。
`create` 和 product-impacting `add` 每次都要读取 root `spec.md`。

优先保存位置:

1. root `spec.md`
2. 仅在 consumer repo 没有 root `spec.md` 时，使用现有的 project spec / architecture / product compass
3. 仅在 consumer repo 没有 root `spec.md` 时，使用 `docs/spec/00-project-spec.md`
4. 有现有规约的 repo 中，遵循该规约的 spec path

需要创建/更新的条件:

- 决定用户可见行为、API、数据模型、权限、计费、外部联动的 task
- 有多个实现方针，选择方式会影响 product behavior 的 task
- 在过去或本次对话中有"规格模糊导致实现偏差"迹象的 task
- Plans.md 中有作业内容，但 project 正解条件没有稳定文档的 task

不必要的条件:

- typo、format、dependency bump、仅 README/CHANGELOG
- 无行为变更的狭义 refactor
- 正解已由现有 spec 和 test 充分固定的修正

输出契约:

- `Spec delta`: 更新 product contract 时，写明目标 spec path 和变更点
- `Spec skip reason`: 不更新 product contract 时，写明理由
- `Spec delta` / `Spec skip reason` 由 Harness 生成，consumer 仅进行批准・修正
- docs-only / mechanical task 也要在 task context / sprint contract 中保留 `Spec skip reason`
- 不要将 missing search result、unavailable memory、未读文件断定为 absent。`not_observed != absent`
- 不让用户从零开始写 spec。agent 根据现有 spec 和输入创建最小 delta，仅在模糊时输出判断分支

### Brainstorming 集成（创意探索）

**目的**: 在计划制定初期进行创意探索，为复杂功能和新特性提供多样化的思路和解决方案。

**触发条件**:
- 新功能设计：需要从多个角度思考功能实现方式
- 架构决策：涉及技术选型、系统架构变化
- 复杂问题解决：需要创新解决方案的技术挑战
- 创新需求：用户明确提出需要创意和探索的需求

**Brainstorming 流程**:
1. **问题定义**: 明确需要 brainstorming 的核心问题或挑战
2. **调用 skill**: 使用 `Skill` 工具调用 `brainstorming` skill
3. **创意生成**: 生成多个解决方案、技术选项、实现路径
4. **选项评估**: 评估各创意的可行性、风险、资源需求
5. **整合结果**: 将 brainstorming 结果整合到后续计划流程

**输出整合**:
- Brainstorming 结果作为步骤 3"询问要做什么"的输入
- 创意选项可作为优先级矩阵的候选方案
- 选定的创意路径体现在 Plans.md 任务分解中
- 被拒绝的创意可作为 Optional 任务或记录备查

**质量保证**:
- Brainstorming 不替代技术调研和 TeamAgent 评审
- 创意方案仍需通过质量检查和验证流程
- 保持与现有规格和架构的一致性
- 确保创意可实现且符合项目目标

**示例场景**:
```
用户: "我们需要设计一个实时的协作编辑功能"

计划流程:
1. 确认对话上下文：实时协作编辑需求
2. 创意探索：
   - 调用 brainstorming skill
   - 探索 OT vs CRDT 算法选择
   - 考虑 WebSocket vs WebRTC 通信
   - 评估冲突解决策略
3. 基于创意结果询问具体需求
4. 继续标准计划质量检查...
```

**注意事项**:
- 仅为真正需要创意的场景调用 brainstorming
- 避免过度工程化，保持实用性
- Brainstorming 结果需与现有架构兼容
- 时间控制：创意探索不应阻碍计划进程

参照:

- `docs/harness-project/plans/spec-ssot.md`

### create 完成时的会话启动指引（必須）

`create` 完成后，不要仅以说明结束，**必须**同时提供 **新会话的启动命令** 和
**启动后可直接输入的第一条指示提示**。

优先顺序如下:

1. 仅有 1 个未完成任务，或自然地只开始第 1 个任务
   - 启动命令: `claude`
   - 首次输入: `/harness-work <task编号>`
2. 有多个依赖薄弱的任务，适合一起推进
   - 启动命令: `claude`
   - 首次输入: `/breezing all`
   - 替代: `/harness-work all`
3. 前提是长时间运行或重新进入
   - 启动命令: `ENABLE_PROMPT_CACHING_1H=1 claude`
   - 首次输入: `/harness-loop all`
   - 替代: `/breezing all`

至少包含以下 3 行:

- `新会话的启动命令:`
- `启动后的首次输入:`
- `适用场景:`

示例:

```text
新会话的启动命令: claude
启动后的首次输入: /breezing all
适用场景: Phase 1 有多个 task，适合一起推进
```

推荐长时间运行时，也并记 Claude Code 会话启动命令:

```text
新会话的启动命令: ENABLE_PROMPT_CACHING_1H=1 claude
启动后的首次输入: /harness-loop all
适用场景: 超过 5 分钟的等待或跨越 resume 的长时任务
```

补充:

- `scripts/claude-longrun.sh` 是此 repo 的开发辅助脚本，plugin install 后不向 consumer 环境分发
- 因此，向 consumer 的指引始终优先 `ENABLE_PROMPT_CACHING_1H=1 claude` 单行命令
- 仅在 repo 开发中想使用同等 wrapper 时，可在本地 checkout 上使用 `bash scripts/claude-longrun.sh`

**CI 模式** (`--ci`):
无需需求收集。直接使用现有 Plans.md，仅进行任务分解。

### add — 添加任务

向 Plans.md 添加新任务。
对于 product-impacting 的添加，遵循上述"spec.md / Plans.md 双正本检查"，同时输出 `Spec delta` 或 `Spec skip reason`。

```
/harness-plan add 任务名: 详细说明 [--phase 阶段编号]
```

任务以 `cc:TODO` 标记添加。

### add 完成时的下一步指引（必須）

`add` 完成后，不要仅报告"已添加"，**必须**同时提示 **接下来可直接输入的命令**。
与 `create` 的会话启动指引同源，区别在于 `add` 通常在当前会话内继续，因此以
**当前会话的下一条输入** 为主，长时任务才并记新会话启动命令。

至少包含以下 2 行:

- `下一步输入:`
- `适用场景:`

优先顺序如下:

1. 新任务的 Depends 已全部满足，且只添加了 1 个任务
   - 下一步输入: `/harness-work <新task编号>`
   - 适用场景: 依赖已解除，可立即实现
2. 一次添加了多个依赖薄弱的任务
   - 下一步输入: `/breezing all`
   - 替代: `/harness-work all`
3. 新任务的 Depends 尚未完成
   - 下一步输入: `/harness-plan sync`
   - 适用场景: 先确认阻塞任务的实际状态，再决定实现顺序
   - 同时明示阻塞任务编号，不让用户自己去表里找
4. 长时间运行或跨 resume 前提
   - 新会话的启动命令: `ENABLE_PROMPT_CACHING_1H=1 claude`
   - 下一步输入: `/harness-loop all`

示例:

```text
已添加: Task 12.13 [lane:gate] cc:TODO (Depends: 12.6)
下一步输入: /harness-work 12.13
适用场景: Depends 的 12.6 已 cc:完成，可立即开始实现
```

```text
已添加: Task 10.14, 10.15 cc:TODO (Depends: 10.13)
下一步输入: /harness-plan sync
适用场景: Depends 的 10.13 仍为 cc:TODO，先确认 Phase 10 的实际进度
```

追加规则:

- product-impacting 的 `add` 中，`Spec delta` 未获批准前不要提示 `/harness-work`。
  此时下一步输入固定为"批准 Spec delta"，实现命令在批准后再提示
- 事前确认章节有未批准事项时同样处理。先获得批准，再提示实现命令
- 影响非工程师判断的追加，一并提议 `/harness-plan-brief`

### update — 标记更新

更改任务的状态标记。

```
/harness-plan update [任务名|任务编号] [WIP|完成|blocked]
```

标记对应表:

| 命令 | 标记 |
|---------|---------|
| `WIP` | `cc:WIP` |
| `完成` / `done` | `cc:完成` |
| `blocked` | `blocked` |
| `TODO` | `cc:TODO` |

### list — 列出命名计划

一览 `plans/manifest.json` 或 `.claude/plans/registry.json` 的所有命名计划。

```
/harness-plan list
```

**实现步骤**:
1. 检查计划注册表文件是否存在 (`.claude/plans/registry.json` 或 `plans/manifest.json`)
2. 如果文件不存在，显示"没有注册的计划"并提示如何创建
3. 读取并解析注册表 JSON 文件
4. 格式化显示所有已注册的计划，包括：
   - 计划名称 (name)
   - 计划 ID (id)
   - 计划文件路径 (file)
   - 状态 (status)
   - 创建时间 (created_at)
   - 更新时间 (updated_at)
5. 显示当前激活的计划 (active_plan)
6. 提供下一步操作提示

**输出格式**:

```text
## Registered Plans
===================
- main (ID: 20250111-main, Status: active)
  File: Plans.md
  Created: 2025-01-11T10:30:00Z

- roadmap (ID: 20250110-roadmap, Status: active)
  File: plans/roadmap.md
  Created: 2025-01-10T15:20:00Z

Active plan: 20250111-main
```

**错误处理**:
- 如果注册表文件不存在：提示用户创建第一个计划或检查项目配置
- 如果 JSON 解析失败：显示原始文件内容并提示格式错误
- 如果 jq 工具不可用：提供原始 JSON 输出

**可选操作**:
- 使用 `scripts/plan/plan-registry.sh list` 作为底层实现
- 支持过滤和排序选项（如 `--status active`、`--sort updated`）

### sync — 进度同步

对照实现与 Plans.md，检测并更新差异（Plans.md 现状获取 → 格式检测 → git 状态获取 → agent trace 分析 → 差异检测 → 标记修正提案 → 下一步提示）。
当有 1 件以上 `cc:完成` 任务时，默认 ON 执行分析估算精度・阻塞原因・范围变动的回顾（`sync --no-retro` 跳过）。
Step 0-6 完整版及 harness-mem 记录步骤请参考 [references/sync.md](${CLAUDE_SKILL_DIR}/references/sync.md)。

### team mode / issue bridge

Plans.md 保持为正本，GitHub Issue 联动仅在 opt-in 的 team mode 中使用。

- solo 开发时不使用 bridge
- team mode 创建 1 个 tracking issue，在其下为每个 task 以 dry-run 生成 sub-issue payload
- `scripts/plans-issue-bridge.sh` 不实际更新 GitHub，始终返回 dry-run payload
- 此 bridge 不进行 Plans.md 的更改

参考:

- `docs/harness-project/plans/team-mode.md`

### named Plans

使用多个 Plans.md 时，以 `plans/manifest.json` 为正本，按名称选择（1 run 中仅使用 1 个 named plan。long-running / CI / issue bridge 中不依赖 active pointer，而是传递 `--plan <name>`。manifest path 仅限 project root 相对路径）。

```bash
scripts/plan-registry.sh list
scripts/plan-registry.sh switch roadmap
scripts/plans-issue-bridge.sh --plan roadmap --format markdown
node scripts/generate-sprint-contract.js --plan roadmap 9.1.1
```

参考: `docs/harness-project/plans/named-plans.md`

## Plans.md 格式规约

### 格式

5 列（Task / 内容 / DoD / Depends / Status）的 Markdown table。DoD 是可验证的 1 行，可 Yes/No 判断（禁止"感觉良好""正常运行"等表述）。
Depends 为 `-`（无依赖）/ 任务编号 / 逗号分隔多个 / 阶段依赖中的任一。生成模板全文（含 Purpose 行）请参考
[references/create.md](${CLAUDE_SKILL_DIR}/references/create.md)。

### TDD tags

在 Plans.md 的 task 中，可在内容或 DoD 中写入 TDD 判定标签。

| 标签 | 意义 | `tdd_required` 推断 |
|------|------|--------------------|
| `[tdd:required]` | 此 task 必须先写失败测试 | `true` |
| `[tdd:skip:<reason>]` | 此 task 因故跳过 TDD | `false`, `skip_tdd_reason=<reason>` |

`<reason>` 不可为空。
示例: `[tdd:skip:docs-only]`、`[tdd:skip:no-test-framework-detected]`。

无标签时的 `tdd_required` 按以下顺序推断:

1. Plans.md tag: `[tdd:required]` / `[tdd:skip:<reason>]`
2. files: 包含 `src/`, `app/`, `cmd/`, `lib/`, `pkg/`, `internal/`, `go/` 等 source 实现则为 required
3. TDD 推断: docs-only 或无 test framework 则附带 skip reason，not required

### optional briefs / manifest

`harness-plan create` 仅在必要时附加 brief。

- project spec SSOT 是固定整个项目正解条件的文档，仅在必要时创建
- 包含 UI 的 task 使用 `design brief`
- 包含 API 的 task 使用 `contract brief`
- brief 是简短固定"做什么"的辅助资料，不替代 Plans.md 或 spec SSOT
- skill frontmatter 列表可通过 `scripts/generate-skill-manifest.sh` 转换为 machine-readable JSON

参考:

- `docs/harness-project/plans/briefs-manifest.md`
- `docs/harness-project/plans/spec-ssot.md`

### 标记一览

| 标记 | 意义 |
|---------|------|
| `pm:依頼中` | PM 已委托 |
| `cc:TODO` | 未着手 |
| `cc:WIP` | 进行中 |
| `cc:完成` | Worker 已完成 |
| `pm:確認済` | PM 已审核 |
| `blocked` | 已阻塞（必须记载理由） |

### 计划确定后的引导（面向非工程师的计划概要）

向 Plans.md 追加 task 完成后，为让非工程师的委托方能够判断计划，
提议 `harness-plan-brief`。这是将理解・选择・风险・合格条件汇总到 1 张 HTML 的
"计划概要"画面，无需专业知识即可阅读。用于进入实现前的共识形成。

## 相关技能

- `harness-sync` — 同步实现与 Plans.md
- `harness-work` — 实现计划中的 task
- `harness-plan-brief` — 计划概要 HTML（面向非工程师，计划确定时提议）
- `harness-review` — 实现的评审
- `harness-setup` — 项目初始化
