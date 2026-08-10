---
name: maintenance
description: "File cleanup and archiving. Tidies up bloated Plans.md, session-log.md, old logs, and state files. Trigger: /maintenance, cleanup, archive, organize, split session-log. Do NOT load for: implementation, review, release, new feature development."
description-en: "File cleanup and archiving. Tidies up bloated Plans.md, session-log.md, old logs, and state files. Trigger: /maintenance, cleanup, archive, organize, split session-log. Do NOT load for: implementation, review, release, new feature development."
description-zh: "负责文件整理、归档和日志压缩。清理杂乱的 Plans.md / session-log.md / 旧日志 / 状态文件。当用户提到 `/maintenance`、维护、整理、归档、移动旧任务、分割 session-log、清理日志时启动。不适用于：实现、审查、发布、新功能开发。"
allowed-tools: ["Read", "Write", "Edit", "Grep", "Glob", "Bash"]
argument-hint: "[plans|session-log|logs|state|all] [--dry-run]"
user-invocable: true
effort: low
---

# Maintenance

整理散乱文件的单目的技能。auto-cleanup-hook 发出警告时，
或作为定期家务调用。

> **前提**: 破坏性操作（归档移动・行删除）前确认 Plans.md / session-log.md 的
> 重要信息是否已提升到 SSOT (decisions.md / patterns.md)。
> 未同步则先运行 `/memory sync`。

## Quick Reference

| 子命令 | 对象 | 典型触发器 |
|------------|------|-------------|
| `maintenance plans` | Plans.md 已完成任务归档移动 | 「Plans.md 整理」「移动旧任务」 |
| `maintenance session-log` | session-log.md 按月分割 | 「session-log 分割」「日志过长」 |
| `maintenance logs` | `.claude/logs/` 旧文件删除 | 「日志清理」「删除30天前的日志」 |
| `maintenance state` | `agent-trace.jsonl` / `harness-usage.json` 压缩 | 「trace 肥大」「state 压缩」 |
| `maintenance all` | 按顺序执行以上4个操作 | 「全部整理」「大扫除」 |

`--dry-run` 仅列出要执行的操作而不实际执行。自由格式的指示（例:
「删除旧归档」「只保留这个 session-log」）在 Step 1 中
接收并反映到 Step 2 之后的处理参数中。

## 执行步骤

1. **解析用户指示**: 子命令 + 自由格式（排除对象、保存目标、天数阈值）
2. **SSOT 同步检查**: 如果不存在 `.claude/state/.ssot-synced-this-session`
   则提示执行 `/memory sync`（仅在操作 Plans.md 时必需）
3. **打开参考文件**: 读取 `${CLAUDE_SKILL_DIR}/references/cleanup.md` 并执行对应章节
4. **报告 Before/After**: 显示行数和删除数量并完成

## 子命令详情

各对象的执行步骤・阈值・归档目标详见 [cleanup.md](./references/cleanup.md)。

## 与 auto-cleanup-hook 的联动

PostToolUse hook (`scripts/auto-cleanup-hook.sh` / Go 版 `auto_cleanup_hook.go`)
在检测到 Plans.md・session-log.md・CLAUDE.md 的行数超限时
会返回 `/maintenance 推荐归档旧任务` 的反馈。
看到此警告时请执行相应的子命令。

## 注意事项

- **不移动进行中的任务**: `cc:WIP`, `pm:依頼中`, `cursor:依頼中` 不在归档范围内
- **归档目标目录固定**: `.claude/memory/archive/` — 移动到其他位置时
  需要向用户确认
- **备份**: 编辑超过200行的文件前使用 `cp <file> <file>.bak.$(date +%s)` 创建
  本地备份
- **CLAUDE.md 仅警告**: 不自动编辑。仅提供分割建议

## 相关技能

- `memory` — Plans.md 整理前的 SSOT 晋升（decisions.md / patterns.md 更新）
- `harness-setup` — 设置刚完成后的定期维护也可通过 `harness-setup` 调用
- `session-init` — 控制会话开始时的维护推荐通知
