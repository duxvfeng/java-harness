---
name: memory
description: "Manage SSOT, memory, and cross-tool memory search. Guardian of decisions.md and patterns.md. Use when user mentions memory, SSOT, decisions.md, patterns.md, merging, migration, SSOT promotion, sync memory, save learnings, memory search, harness-mem, past decisions, or record this. Do NOT load for: implementation work, reviews, ad-hoc notes, or in-session logging."
description-en: "Manage SSOT, memory, and cross-tool memory search. Guardian of decisions.md and patterns.md. Use when user mentions memory, SSOT, decisions.md, patterns.md, merging, migration, SSOT promotion, sync memory, save learnings, memory search, harness-mem, past decisions, or record this. Do NOT load for: implementation work, reviews, ad-hoc notes, or in-session logging."
description-zh: "管理 SSOT 和内存，提供跨工具的内存搜索。decisions.md 和 patterns.md 的守护者。当用户提到内存、SSOT、decisions.md、patterns.md、合并、迁移、SSOT 提升、同步内存、保存学习、内存搜索、harness-mem、过去决策或记录此项时使用。不适用于：实现工作、审查、临时笔记或会话内日志。"
allowed-tools: ["Read", "Write", "Edit", "Bash", "mcp__harness__harness_mem_*"]
argument-hint: "[ssot|sync|migrate|search|record]"
user-invocable: true
context: fork
---

# Memory Skills

负责内存和 SSOT 管理的技能群。

## 功能详情

| 功能 | 详情 |
|------|------|
| **SSOT初始化** | See [references/ssot-initialization.md](${CLAUDE_SKILL_DIR}/references/ssot-initialization.md) |
| **Plans.md合并** | See [references/plans-merging.md](${CLAUDE_SKILL_DIR}/references/plans-merging.md) |
| **迁移处理** | See [references/workflow-migration.md](${CLAUDE_SKILL_DIR}/references/workflow-migration.md) |
| **项目规格同步** | See [references/sync-project-specs.md](${CLAUDE_SKILL_DIR}/references/sync-project-specs.md) |
| **内存→SSOT提升** | See [references/sync-ssot-from-memory.md](${CLAUDE_SKILL_DIR}/references/sync-ssot-from-memory.md) |

## Unified Harness Memory（共通DB）

Claude Code / Codex / OpenCode 共通的记录・搜索优先使用 `harness_mem_*` MCP。

- 搜索: `harness_mem_search`, `harness_mem_timeline`, `harness_mem_get_observations`
- 注入: `harness_mem_resume_pack`
- 记录: `harness_mem_record_checkpoint`, `harness_mem_finalize_session`, `harness_mem_record_event`

## 与 Claude Code 自动内存的关系（D22）

Harness 的 SSOT 内存（Layer 2）与 Claude Code 的自动内存（Layer 1）共存。
自动内存隐式记录通用学习，SSOT 显式管理项目固有决策。
Layer 1 的见解对项目整体重要时，请用 `/memory ssot` 提升到 Layer 2。

详情: 项目运行时的 `.claude/memory/decisions.md` 中的 D22（3 层内存架构）。该文件由项目会话初始化创建，不作为插件仓库固定文件。

## 执行步骤

1. 分类用户请求
2. 从上述"功能详情"读取适当的参考文件
3. 按其内容执行

## SSOT提升

将内存系统（Claude-mem / Serena）的重要学习持久化到 SSOT。

- "**Save what we learned**" → [references/sync-ssot-from-memory.md](${CLAUDE_SKILL_DIR}/references/sync-ssot-from-memory.md)
- "**Promote decisions to SSOT**" → [references/sync-ssot-from-memory.md](${CLAUDE_SKILL_DIR}/references/sync-ssot-from-memory.md)
