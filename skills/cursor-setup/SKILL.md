---
name: cursor-setup
description: "Configure and verify the Cursor backend for Claude Code Harness. Use when user invokes cursor:setup, wants Cursor as the local default implementation backend, or asks to check Cursor plugin/agent readiness. Distribution default remains opt-in; only local env/user settings are changed when explicitly requested."
description-en: "Configure and verify the Cursor backend for Claude Code Harness. Use when user invokes cursor:setup, wants Cursor as the local default implementation backend, or asks to check Cursor plugin/agent readiness. Distribution default remains opt-in; only local env/user settings are changed when explicitly requested."
description-zh: "配置和验证 Claude Code Harness 的 Cursor 后端的技能。当用户提到 cursor:setup、想将 Cursor 设为当前环境的默认实现后端、或想检查 Cursor 插件/代理就绪状态时使用。分发插件的默认值保持 opt-in，仅在明确请求时更改本地环境/用户设置。"
allowed-tools: ["Read", "Bash"]
argument-hint: "[--check | --user-default | --project-default | --unset]"
user-invocable: true
---

# cursor:setup - Cursor Backend Setup

Cursor 后端的 setup/verification 技能。分发插件的 fallback 保持为 `claude`，仅在将当前 repo/user 环境设为 `cursor` 默认值时使用。

## Quick Reference

```bash
cursor:setup --check
cursor:setup --user-default
cursor:setup --project-default
cursor:setup --unset
```

## Rules

- Do not change distribution defaults or plugin manifests to make Cursor the shipped fallback.
- Treat `~/.cursor/permissions.json`, `.cursorignore`, and Claude sandbox allowlists as manual/user-owned setup surfaces unless the user explicitly asks to edit a local file.
- Java 版本不提供 Go 版本的 `setup-cursor.sh`、`set-impl-backend.sh` 或 companion wrapper。不要执行这些路径。
- Java 版本只记录项目配置，不会因为设置 `backend = "cursor"` 就自动启动 `cursor-agent`。

## Flow

在 Java 版本中，先确认 CLI 和 Cursor 本身可用：

```bash
harness --version
harness doctor
cursor-agent --version
```

1. For `--check`, run:

   ```bash
   harness doctor
   cursor-agent --version
   ```

2. For `--user-default`, run:

   ```bash
   # Edit the user-owned Harness configuration and set:
   # [harness]
   # backend = "cursor"
   ```

3. For `--project-default`, run:

   ```bash
   # Edit the project harness.toml and set:
   # [harness]
   # backend = "cursor"
   harness doctor
   ```

4. For `--unset`, ask whether the target is user or project scope if not clear from the request, then run exactly one matching unset command:

   Project scope:

   ```bash
   # Remove the project-level [harness].backend value from harness.toml.
   ```

   User scope:

   ```bash
   # Remove the user-level backend override from the user-owned config.
   ```

## Output

Report three facts only:

- resolved backend (`claude` / `codex` / `cursor`)
- Cursor package readiness (`cursor-agent --version`)
- next manual step if Cursor is not ready
