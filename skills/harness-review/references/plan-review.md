# Plan Review

## 概述

Plan Review 检查 `Plans.md` 是否具有可实现的粒度和顺序。

## 检查点

- task 是否为一个完成单位
- DoD 是否可验证
- Depends 是否无循环
- Status 是否与现实一致
- 需要规格正本的 task 是否有 `spec_path` 或创建任务
- 实现顺序是否未将高风险部分延后
- review / release / mirror / docs 的收尾是否遗漏

## 判定

| 状态 | 判定 |
|---|---|
| DoD 可测量、Depends 妥当、scope 明确 | APPROVE |
| DoD 模糊、依赖损坏、需要规格正本却没有 | REQUEST_CHANGES |
| 需要更改 scope 且无需用户判断 | decision_needed |

## 输出

Plan Review 优先使用 file:line。
以 `Plans.md` 的相应行、文档、规格正本为依据。
