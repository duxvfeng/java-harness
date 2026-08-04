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
