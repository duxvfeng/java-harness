# Skill Routing Rules (Reference)

技能间路由规则的参考文档。

> **SSOT 位置**: 每个技能的 `description` 字段是路由的 SSOT。
> 此文件是提供详细说明和示例的参考文档，实际路由依赖于各技能的 description。
>
> **重要**: 每个技能的 description 和本文的「Do NOT Load For」表必须完全一致。

## Codex 相关路由

### harness-review（包含 Codex 审查功能）

**目的**: 通过 Codex CLI (`codex exec`) 提供第二意见审查（在 v3 中从 `codex-review` 整合）

**触发关键词**（description 引用）:
- "review", "code review", "plan review"
- "scope analysis", "security", "performance"
- "quality checks", "PRs", "diffs"
- "/harness-review"

**排除关键词**（description 引用）:
- "implementation", "new features", "bug fixes"
- "setup", "release"

### harness-work --codex（包含 Codex 实现功能）

**目的**: 使用 Codex 作为实现引擎（在 v3 中整合）

**触发关键词**:
- "implement", "execute", "/work"
- "breezing", "team run"
- "--codex", "--parallel"

**排除关键词**（description 引用）:
- "planning", "code review", "release"
- "setup", "initialization"

**对应**: 通过 `/harness-work --codex` 执行

## 阶段标签路由（参考）

`[stage:*]` 是 Plans metadata，不是新的 skill 触发词；实际 skill 加载仍以各技能 `description` 为 SSOT。阶段标签用于标明任务处于交付闭环的哪个位置，便于 `harness-plan`、`harness-work`、`harness-review` 等已有 skill 分工。

| 标签 | 阶段目标 | 主要 skill / surface |
|------|---------|----------------------|
| `[stage:kickoff]` | 对齐用户意图、产出 Story Card、冻结进入理解阶段的边界 | `harness-plan create`、`harness-plan-brief` |
| `[stage:understand]` | 调研现有规格、repo evidence、unknown data，并区分已观察与未观察 | `harness-plan create`、`memory`、`harness-plan-brief` |
| `[stage:tasking]` | 将理解结果拆为 Plans.md task contract，确定 DoD、Depends、baseline | `harness-plan create`、`harness-sync` |
| `[stage:pair]` | 执行实现或测试任务，收集 evidence，保持 TDD / review gate | `harness-work`、`test-driven-development` |
| `[stage:showcase]` | 展示可验证结果，执行 review / acceptance / Quality Quadrants 分类 | `harness-review`、`harness-accept`、`requesting-code-review` |
| `[stage:respond]` | 汇总交付、同步状态、给出后续行动或 release / closeout 指引 | `harness-work`、`harness-sync`、`harness-release` |

### 与 lane 标签并存

`[stage:*]` 表示流程位置，`[lane:*]` 表示执行风险/路径；两者可以同时出现在 Content 或 DoD 中，不互相替代。

```markdown
| 13.2 | 定义 6-stage 标签词汇表 `[stage:tasking]` `[lane:gate]` `[tdd:skip:docs-only]` | routing-rules.md 与 create.md 含阶段表、skill 映射、并存示例 | - | cc:TODO |
```
## 路由判定流程（参考）

> 此部分是对 Claude Code 内部行为的说明，不是额外的关键词定义。
> 实际路由仅根据各技能的 description 中记录的关键词判定。

```
用户输入
    │
    ├── description 的触发关键词匹配 → 加载对应技能
    ├── description 的排除关键词匹配 → 排除对应技能
    └── 都不匹配 → 通常的技能匹配
```

## 优先级规则（参考）

关键词匹配多个技能时的优先级:

1. **排除最优先**: 匹配排除关键词的技能绝对不加载
2. **具体关键词优先**: 完全匹配 > 部分匹配

> **注**: 不使用「上下文判定」因为会产生歧义。通过 description 的关键词确定性地判定。

## 更新规则

1. **description = SSOT**: 每个技能的 `description` 字段是路由的正式定义
2. **与本文一致**: 每个技能的「Do NOT Load For」表必须与 description 完全一致
3. **此文件的作用**: 提供详细说明和判定流程的参考（不是 SSOT）
4. **维护完整列表**: 不使用通用表达（"〜全般"），而是列举具体的关键词
