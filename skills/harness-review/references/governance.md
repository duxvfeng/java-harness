# Review Governance

## 一句话总结

`APPROVE` 仅在有证据证明"没有重大问题"时返回。

## 明确的合格线

`APPROVE` 的条件：

- critical / major 为 0 件
- root `spec.md` alignment: 与上位 product contract 不矛盾。即使有 sub-spec (`spec_path`) 也优先确认 root contract。仅对不必要的 task 允许 `spec_skip_reason`
- `Plans.md` alignment: 与 task / DoD / Depends 不矛盾。`[lane:fast|gate|release]` 和 stage gate metadata 与 contract 一致
- TDD evidence: `[tdd:required]` 任务有 `tdd_red_log`、literal failing output 或显式 `skip_tdd_reason`
- unknown data contract: 不 `APPROVE` 无证据的"无问题""无数据"。`not_observed != absent` — 未观测的报告为 `unknown` / `not observed`
- regression safety: 既存行为、既存测试、既存 UX、既存 CLI、既存设置、既存 docs、配发 mirror 没有退化证据
- evidence pack: report 中有 accepted findings / rejected findings、focused tests、`release-preflight` warnings 的处理方针
- 没有 TeamAgent Debate 的未解决 disagreement

## Severity

| severity | 意味 | verdict |
|---|---|---|
| critical | 秘密信息泄露、数据破坏、权限破坏、直连 release 事故 | REQUEST_CHANGES |
| major | DoD 未达成、违反规格正本、lane/stage 不整合、TDD evidence 缺失、明确退化、测试未执行危险 | REQUEST_CHANGES |
| minor | 质量提升但不到停止出货程度 | 可 APPROVE |
| recommendation | 任意改善 | 可 APPROVE |

如果只有 minor / recommendation，不一定停止。
如果停止，具体说明为什么是 major。

## AskUserQuestion / decision_needed

靠推测判断会破坏的，作为 `decision_needed` 而非 `REQUEST_CHANGES`。

`decision_needed` 的例子：

- 需要改变规格正本
- 需要改变 `Plans.md` 的 DoD / Depends / lane / stage
- 需要用户选择 security 和 UX 的优先级
- 需要事业判断是否保留 backward compatibility

可以使用 AskUserQuestion 时使用。
Codex 环境等无法使用时将 `decision_needed.v1` 输出到 stdout，不靠推测推进。

## Side effects

review default read-only boundary:

- `APPROVE` でも自動 commit しない
- `APPROVE` は commit / push / PR 作成命令ではない
- Do not push just to review
- commit / push / release 是 `harness-work` / `harness-release` / 用户明确请求的职责

## Output evidence

必須:

- 对象范围
- 执行的 review command
- 执行的 tests
- accepted findings
- rejected findings
- release-preflight warnings 和处理方针
- clean result 或剩余课题
- root `spec.md` / `Plans.md` lane-stage / 退化 / TDD / unknown data 的合格线

`APPROVE` なのに evidence pack が空なら、その `APPROVE` は無効。
