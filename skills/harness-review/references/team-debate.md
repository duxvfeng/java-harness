# TeamAgent Debate

## 概述

TeamAgent Debate 是以不同视点读取同一更改，减少遗漏的 read-only review pass。

## 执行时机

满足以下任一条件时执行。

- 更改跨越多个模块
- 触及 security / auth / release / distribution / mirror
- 与规格正本或 `Plans.md` 的对应暧昧
- regression risk 高
- Claude 和 Codex 的 verdict 分歧
- reviewer 中视点别评价分歧
- 同一 issue 修正后再审查连续 2 次未通过

## Agents

| Agent | 主要提问 |
|---|---|
| Spec Agent | 探索规格正本与实现差异的矛盾 |
| Plans Agent | 确认 `Plans.md` 的 task / DoD / Depends 与差异的对应 |
| Regression Agent | 探索既存行为・测试・分发 mirror・CLI/skill UX 的退化 |
| Skeptic Agent | 探索想合格的前提下遗漏的 major risk |

最低 2 视点，必要时 4 视点。
全员 read-only。

## Codex fallback

Codex 环境无法使用 native TeamAgent 时也不省略。

可用的 fallback:

- `codex-companion.sh review`
- reviewer subagent
- 明确分开的 manual-pass

`team_agent_mode` 记录以下任一。

- `native`
- `codex-companion`
- `manual-pass`
- `unavailable`

`unavailable` 状态下 manual-pass 也无法进行时，作为 `decision_needed` 停止。

## Output

```json
{
  "team_debate": {
    "required": true,
    "mode": "manual-pass",
    "team_agent_mode": "manual-pass",
    "agents": ["Spec Agent", "Plans Agent", "Regression Agent"],
    "disagreements": [],
    "acceptance_bar": {
      "spec_alignment": "pass",
      "plans_alignment": "pass",
      "regression_safety": "pass"
    }
  }
}
```

## 合格线

TeamAgent Debate 的 disagreement 若相当 critical / major 则 `REQUEST_CHANGES`。
降级为 minor / recommendation 时，写理由并附 evidence。
