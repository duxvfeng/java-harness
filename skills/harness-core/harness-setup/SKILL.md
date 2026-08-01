---
name: harness-setup
description: "HAR: Project init, tool setup, agent config, memory setup, skill mirror sync. Trigger: setup, init, new project, CI/Codex setup, harness-mem, mirror. Do NOT load for: implementation, review, release, planning."
description-en: "HAR: Project init, tool setup, agent config, memory setup, skill mirror sync. Trigger: setup, init, new project, CI/Codex setup, harness-mem, mirror. Do NOT load for: implementation, review, release, planning."
description-ja: "HAR:プロジェクト初期化・ツール設定・エージェント構成・メモリ設定・skill mirror 同期を担当。セットアップ、初期化、新規プロジェクト、CI/Codex CLI セットアップ、harness-mem、mirror で起動。実装・レビュー・リリース・プランニングには使わない。"
description-zh: "HAR：负责项目初始化、工具配置、代理设置、内存配置和技能镜像同步。当用户提到设置、初始化、新项目、CI/Codex CLI 设置、harness-mem、镜像时启动。不适用于：实现、审查、发布、计划。"
kind: workflow
purpose: "Initialize and repair Harness project configuration"
trigger: "setup, init, new project, CI/Codex setup, harness-mem, mirror"
shape: workflow
role: generator
pair: harness-sync
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Grep", "Glob", "Bash"]
argument-hint: "[init|ci|codex|harness-mem|mirrors|agents|localize]"
user-invocable: true
effort: medium
---

# Harness Setup

Harness 的集成设置技能。
整合以下旧技能:

- `setup` — 集成设置中心
- `harness-init` — 项目初始化
- `harness-update` — Harness 更新
- `maintenance` — 文件整理・清理

## Quick Reference

| 子命令 | 动作 | 详情 |
|------------|------|------|
| `/harness-setup init` | 新项目初始化（CLAUDE.md + Plans.md + hooks + sync + doctor）| `${CLAUDE_SKILL_DIR}/references/init.md` |
| `/harness-setup ci` | CI/CD 管道设置 | `${CLAUDE_SKILL_DIR}/references/ci.md` |
| `/harness-setup codex` | Codex CLI 安装・设置 | `${CLAUDE_SKILL_DIR}/references/codex.md` |
| `/harness-setup harness-mem` | harness-mem 集成・内存设置 | `${CLAUDE_SKILL_DIR}/references/harness-mem.md` |
| `/harness-setup mirrors` | skills/ → 公开 mirror bundle 更新 | `${CLAUDE_SKILL_DIR}/references/mirrors-agents-localize.md` |
| `/harness-setup agents` | agents/ 代理设置 | `${CLAUDE_SKILL_DIR}/references/mirrors-agents-localize.md` |
| `/harness-setup localize` | CLAUDE.md 规则的本地化 | `${CLAUDE_SKILL_DIR}/references/mirrors-agents-localize.md` |
| marketplace / update | Plugin install, update, dependency policy | `${CLAUDE_SKILL_DIR}/references/marketplace.md` |
| maintenance | 文件整理・清理 | `${CLAUDE_SKILL_DIR}/references/maintenance.md` |

> **Built-in slash discovery (CC 2.1.108+)**:
> `/init` 这样的 built-in slash command 也会被发现。
> 仅在需要 Harness 特有的 bootstrap 时区分使用 `/harness-setup init`。

> **Claude Code setup guidance (CC 2.1.120+)**:
> MCP `alwaysLoad`、`${CLAUDE_EFFORT}`、`claude plugin prune`、`claude project purge`、
> `ANTHROPIC_BEDROCK_SERVICE_TIER`、`claude_code.skill_activated.invocation_trigger`、
> Windows PowerShell primary shell、forked skills / subagents 的 deferred tools 以
> `docs/claude-code-setup-mcp-telemetry-provider.md` 为正本。

> **Codex plugin workflows**:
> 不双重管理 Codex `/goal` 和 `Plans.md`。
> plugin-bundled hooks 为 opt-in、external agent import 明记 ownership、
> MultiAgentV2 / `agents.max_threads = 8` 作为上限处理、
> sticky environments / app-server artifacts 优先 safe default。
> Codex `0.130.0` stable 的 `codex remote-control`、large thread pagination、
> selected-environment `view_image`、live app-server config refresh、
> accurate turn diffs、plugin details bundled hooks、sharing discoverability controls 以
> `docs/codex-plugin-workflows-policy.md` 为正本。
> 详情参见 `docs/codex-plugin-workflows-policy.md` 和 `${CLAUDE_SKILL_DIR}/references/codex.md`。

## Execution

1. 选择对应用户目的的 Quick Reference 行。
2. 读取对应的 `${CLAUDE_SKILL_DIR}/references/*.md`。
3. 遵循参照文件的步骤，有 dry-run 的操作先执行 dry-run。
4. setup 后根据需要通过 `harness doctor`、`bash scripts/sync-skill-mirrors.sh --check`、`bash scripts/ci/check-consistency.sh` 确认。

## Upstream Policy Anchors

- `docs/plugin-managed-settings-policy.md` — plugin managed settings policy; normal defaults must not inherit managed marketplace restrictions.
- `docs/codex-provider-setup-policy.md` — Codex provider setup policy, including `amazon-bedrock` and `model_provider = "amazon-bedrock"` examples.
- Codex `0.123.0` 以降的 MCP diagnostics / plugin MCP loading guidance: use `/mcp verbose` and `docs/codex-mcp-diagnostics.md` for diagnostics.
- Codex sandbox / execution policy (0.123.0+): see `docs/codex-sandbox-execution-policy.md` for `remote_sandbox_config` and execution constraints.

## Cursor 实现后端引入

詳細手順は `${CLAUDE_SKILL_DIR}/references/cursor.md` を読む。
契約アンカー: `set-impl-backend.sh` は AI 実行可。`permissions.json`、`.cursorignore`、`*.cursor.sh` allowlist はユーザー手動で、AI が編集できない protected path として扱う。
根拠ルールは `.claude/rules/cursor-cli-only.md` と `docs/sandbox-allowlist-recipe.md`。

## Reference Index

- `${CLAUDE_SKILL_DIR}/references/init.md` — init, Go binary verification, plugin sync, doctor.
- `${CLAUDE_SKILL_DIR}/references/ci.md` — GitHub Actions CI setup.
- `${CLAUDE_SKILL_DIR}/references/codex.md` — Codex CLI, provider/model metadata, app-server, MCP diagnostics, sandbox policy.
- `${CLAUDE_SKILL_DIR}/references/cursor.md` — Cursor implementation backend setup and boundaries.
- `${CLAUDE_SKILL_DIR}/references/harness-mem.md` — memory directory and template setup.
- `${CLAUDE_SKILL_DIR}/references/mirrors-agents-localize.md` — mirror sync, agent setup, localization.
- `${CLAUDE_SKILL_DIR}/references/marketplace.md` — plugin marketplace install/update and managed dependency policy.
- `${CLAUDE_SKILL_DIR}/references/maintenance.md` — cleanup commands and related skills.

## 関連スキル

- `harness-sync` — 設定/Plans/git 状態の同期確認
- `harness-work` — 実装タスク実行
- `harness-review` — 品質レビュー
- `maintenance` — ファイル整理（統合済み）
