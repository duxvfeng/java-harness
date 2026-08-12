---
name: failure-codifier
description: "Extract recurring failure patterns from breezing orchestration logs and Judgment Ledger, emit failure-rule.v1 proposals with confidence scores. SSOT promotion to patterns.md or decisions.md is proposal-only — human-approval-required. Use when user mentions failure codifier, failure patterns, self-learning loop, codify failures, or failure-rule proposals. Do NOT load for: direct SSOT edits, auto-promotion, or implementation unrelated to failure analysis."
description-en: "Extract recurring failure patterns from breezing orchestration logs and Judgment Ledger, emit failure-rule.v1 proposals with confidence scores. SSOT promotion to patterns.md or decisions.md is proposal-only — human-approval-required. Use when user mentions failure codifier, failure patterns, self-learning loop, codify failures, or failure-rule proposals. Do NOT load for: direct SSOT edits, auto-promotion, or implementation unrelated to failure analysis."
description-zh: "从 breezing 日志和 Judgment Ledger 中提取可复现的失败模式，提出带有置信度评分的 failure-rule.v1 建议。向 patterns.md / decisions.md 的提升需要人工审批（禁止自动提升）。当用户提到 failure codifier、失败模式、self-learning loop、codify failures 时使用。不适用于：SSOT 直接编辑、自动提升。"
allowed-tools: ["Read", "Bash", "Grep"]
argument-hint: "[propose|explain]"
user-invocable: true
---

# Failure Codifier

从 breezing orchestration ledger + Judgment Ledger 中 **只读**提取重现失败，附带 confidence score 提议 `failure-rule.v1` 候选。

## 核心契约

- **human-approval-required**: codifier 仅执行 dry-run 提案。`patterns.md` / `decisions.md` 的自动提升在结构上被禁止。
- Confidence 阈值: occurrence **count ≥ 3 → medium**，**count ≥ 5 → high**（`go/internal/failurecodifier/confidence.go`）。
- 提升目标 heuristic: 通过 `proposed_ssot_target` 字段**仅提议** `patterns.md` 或 `decisions.md`。

## 使用方法

### Dry-run 提案（推荐）

```bash
./scripts/failure-codifier-propose.sh --dry-run
```

stdout 为 JSON 数组（`failure-rule.v1` 候选）。不向 SSOT 文件进行任何写入。

### Go 测试

```bash
cd go && go test ./internal/failurecodifier/... -count=1
```

## 参考

- 提升工作流程: [references/promotion-workflow.md](${CLAUDE_SKILL_DIR}/references/promotion-workflow.md)
- Schema: `scripts/templates/schemas/failure-rule.v1.json`
- Core: `go/internal/failurecodifier/`

## 禁止事项

- 对 `patterns.md` / `decisions.md` 的 Write / Edit（人工批准后也不能通过 codifier 进行）
- `AutoPromote` / 无人值守 SSOT 更新
- Plans.md 的 `cc:*` 标记变更
