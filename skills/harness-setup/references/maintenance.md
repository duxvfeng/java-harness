# Harness Setup Reference: maintenance

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

## Maintenance — 文件整理

定期维护任务:

| 任务 | 命令 |
|--------|---------|
| 删除旧日志 | `find .claude/logs -mtime +30 -delete` |
| Plans.md 压缩 | 将完成任务移至归档章节 |
| 删除旧跟踪 | `tail -1000 .claude/state/agent-trace.jsonl > /tmp/trace && mv /tmp/trace .claude/state/agent-trace.jsonl` |

## 相关技能

- `harness-plan` — 设置后创建项目计划
- `harness-work` — 设置后执行任务
- `harness-review` — 审查设置配置
