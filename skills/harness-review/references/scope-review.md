# Scope Review

## 概述

Scope Review 检查是否遗漏了应做的事情，或者是否做了多余的事情。

## 检查点

- 差异与用户请求一致
- 满足 task 的 DoD
- 未混入无关的重构
- docs / tests / mirror / changelog 的必要范围齐全
- 确认未增加新的 public surface
- 未擅自更改 migration / release / permission 边界

## Scope creep

scope creep 指"工作范围过度膨胀"。
例如在 docs 修复的 task 中开始更改 release script 是危险的。

发现 scope creep 后，分为以下两种情况：

- 本次 DoD 必需：在 plan 中明确记载并继续
- 本次 DoD 不必要：分离为另一个 task

## 判定

| 状态 | 判定 |
|---|---|
| 要求与差异一致 | APPROVE |
| DoD 未达成或混入不必要的更改 | REQUEST_CHANGES |
| 需要 scope 变更的业务判断 | decision_needed |
