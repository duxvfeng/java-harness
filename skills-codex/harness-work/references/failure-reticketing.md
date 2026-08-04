# Codex Failure Reticketing

Codex 绝不能通过减弱测试将红色验证变成绿色结果。

验证失败时:

1. 如果失败属于当前任务，在范围内修复。
2. 如果任务已标记为完成，创建待定修正提案。
3. 同一 CI 原因重复 3 次后，停止并升级。

待定修正提案属于 `.claude/state/pending-fix-proposals.jsonl`
直到用户批准添加到 `Plans.md`。
