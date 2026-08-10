# Dual Review (--dual) / Triple Review (--cursor opt-in)

并行运行 Claude Reviewer 和 Codex Reviewer，通过不同模型视角提高审查质量。
`--dual` 不是单纯的双重检查，必要时结合 TeamAgent Debate，
从多视角填充规格正本、Plans.md、回归的合格线。

同时使用 `--cursor` flag（或 `--dual --cursor` 进行 triple）时，cursor (composer-2.5-fast) 可作为 **second-opinion only** 并行运行。详情请参考 `references/cursor-review.md`。

## 前提条件

- Codex 不可用时，回退到 Claude 单独审查
- `--cursor` 并用时 cursor-agent 已安装 (`setup-cursor.sh --check`)。不可用时 `cursor_verdict: unavailable` 降级

## 执行流程

1. 确认 Codex 可用性

   ```bash
   ```

2. 用 Task 工具启动 Claude Reviewer（通常 review 流程）


   ```bash
   # 如果传递了 BASE_REF，指定 --base。--json 获取结构化输出
   ```

4. 等待双方结果

5. 以下任一情况时执行 TeamAgent Debate
   - Claude 和 Codex 的 verdict 分歧
   - 规格正本、Plans.md、回归中存在不一致或未确认项
   - `critical` / `major` 候选有 1 件以上
   - 指定了 `--team-debate`

6. 固定合格线后合并 verdict

## TeamAgent Debate

TeamAgent Debate 作为故意让不同见解冲突的 read-only review pass 处理。

| Agent | 主要问题 |
|-------|----------|
| Spec Agent | 规格正本与实现是否存在矛盾 |
| Plans Agent | `Plans.md` 的 task / DoD / Depends 与证据是否一致 |
| Regression Agent | 现有行为、现有测试、分发 mirror、CLI/skill UX 是否存在回归 |
| Skeptic Agent | 在通过合格的前提下，是否遗漏了主要风险 |

在 Claude Code 中使用 Task tool。
在 Codex 环境中可能无法使用 native TeamAgent，因此
记录在 `team_agent_mode` 中。

## 合格线

最终 `APPROVE` 的条件是以下全部。

- `critical` / `major` 为 0 件
- 与规格正本或 `spec_skip_reason` 不矛盾
- 与 `Plans.md` 的 task / DoD / Depends 不矛盾
- 没有现有行为、现有测试、分发 mirror、CLI/skill UX 的回归证据
- Claude / Codex / TeamAgent 的分歧已解决，或作为 `minor` / `recommendation`` 附带理由降级

## Verdict 合并规则

按以下顺序评估：

   - 双方 APPROVE → `APPROVE`
   - 任一方为 REQUEST_CHANGES → `REQUEST_CHANGES`（采用较严的）
   - TeamAgent Debate 留下 `critical` / `major` 相当的分歧 → `REQUEST_CHANGES`
   - 规格正本 / Plans.md / 回归 gate fail → `REQUEST_CHANGES`
   - `critical_issues` 合并双方列表（不排除重复）
   - `major_issues` 合并双方列表（不排除重复）
   - `recommendations` 合并时排除重复

## 输出形式

在通常的 `review-result.v1` 架构中添加 `dual_review` 字段：

```json
{
  "schema_version": "review-result.v1",
  "verdict": "APPROVE | REQUEST_CHANGES",
  "dual_review": {
    "claude_verdict": "APPROVE | REQUEST_CHANGES",
    "codex_verdict": "APPROVE | REQUEST_CHANGES | unavailable | timeout",
    "merged_verdict": "APPROVE | REQUEST_CHANGES",
    "divergence_notes": "判定分岐时的理由。例: Claude 在 Performance 检测到 major，Codex 无问题"
  },
  "acceptance_bar": {
    "critical_major_zero": true,
    "spec_alignment": "pass | fail | not_applicable",
    "plans_alignment": "pass | fail | not_applicable",
    "regression_safety": "pass | fail | not_applicable",
    "verification_evidence": "pass | fail | not_applicable"
  },
  "team_debate": {
    "required": true,
    "agents": ["Spec Agent", "Plans Agent", "Regression Agent"],
    "disagreements": []
  },
  "critical_issues": [],
  "major_issues": [],
  "observations": [],
  "recommendations": []
}
```

### `codex_verdict` 的特殊值

| 值 | 意义 |
|----|------|
| `"unavailable"` | Codex CLI 未安装或不可用 |
| `"timeout"` | Codex 审查超时（120 秒内无响应） |

## 回退

- **Codex 不可用**: 单独执行 Claude，记录 `codex_verdict: "unavailable"`
- **Codex 超时**: 原样采用 Claude 的 verdict，记录 `codex_verdict: "timeout"`
- **Codex 审查输出无效**: 视为解析失败，记录 `codex_verdict: "unavailable"`
- **TeamAgent 不可用**: 记录 `team_debate.mode: "unavailable"` 和理由，至少执行 Spec / Plans / Regression 的 manual-pass

Codex unavailable / timeout 时，也不省略规格正本、Plans.md、回归的合格线。
TeamAgent unavailable 且无法 manual-pass 时，作为 `decision_needed` 而非 `REQUEST_CHANGES` 停止。

## Divergence Notes 的写法

判定一致时（`claude_verdict == codex_verdict`）将 `divergence_notes` 设为空字符串。

判定分岐时按以下格式记录：

```
Claude: REQUEST_CHANGES（Security - SQL注入风险）
Codex: APPROVE（判定同处无问题）
采用: REQUEST_CHANGES（优先采用较严的）
```
