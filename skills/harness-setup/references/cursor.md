# Harness Setup Reference: cursor

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

## Cursor 实现后端引入（脑 Opus / 体 composer）

将 Cursor 作为 Harness 的实现（worker）后端的使用步骤。
审查 / advisor 角色固定为 Opus，不切换到 cursor 后端（`.claude/rules/cursor-cli-only.md` 的 Role scope）。

### 1. AI 可执行（后端选择的持久化）

使用 `set-impl-backend.sh` 持久化后端。Harness / AI 可以执行此步骤。

```bash
# 项目范围（写入此项目的 env.local）
bash "${HARNESS_PLUGIN_ROOT}/scripts/set-impl-backend.sh" cursor

# 用户范围（所有项目通用: ${HOME}/.config/claude-harness/impl-backend.env）
bash "${HARNESS_PLUGIN_ROOT}/scripts/set-impl-backend.sh" --user cursor

# 显示当前解析的后端以确认
bash "${HARNESS_PLUGIN_ROOT}/scripts/set-impl-backend.sh" --show
```

解析优先级：项目 env.local 优先于用户范围。

### 2. 用户手动（AI 无法编辑。protected path + sandbox）

以下 3 个文件由于 `Edit/Write(.claude/settings*)` deny 和 self-audit guard，以及 `~/.cursor/*` 不在
plugin 写入范围内，因此 Harness / AI 无法编辑。用户需要自己在终端 / 编辑器中设置。

- **`~/.cursor/permissions.json`**: 添加 `terminalAllowlist` / `mcpAllowlist`。
  使用 `.claude/rules/cursor-cli-only.md` 的 `~/.cursor/permissions.json` 模板。
  不使用 `--force` / Run Everything（`--yolo`）（Cursor 官方表示"永不使用"）。
- **`.cursorignore`**: 列出 secrets（`.env`, `*.pem`, `*.key`, `.ssh`, `.aws`, `.git`）。
  使用 `.claude/rules/cursor-cli-only.md` 的 `.cursorignore` 模板。
- **`~/.claude/settings.json` 的 sandbox（2 点）**: (1) `network.allowedDomains` 添加 `*.cursor.sh`，
  (2) 官方键 **`sandbox.filesystem.allowWrite`** 添加 `~/.cursor`（因为 cursor-agent 运行时
  需要向 `~/.cursor/projects/...` 和 `~/.cursor/cli-config.json.tmp` 写入状态，未授权会
  因 `EPERM` 失败）。⚠️ **键名为 `allowWrite`**: 如果命名为 `write` 会被作为未知键
  忽略，设置无效（实测确认）。`~/` 由 sandbox 侧展开（官方示例 `["~/.kube"]`）。
  两者配置完成后，无需每次禁用 sandbox 即可运行。步骤按 `docs/sandbox-allowlist-recipe.md` 的 jq merge 配方和
  `.claude/rules/cursor-cli-only.md` 的「Sandbox 要求」。CC 完全重启后生效。

### 3. 边界（cursor 保持 candidate 状态）

cursor 后端定位为 candidate。安全性不依赖 Cursor 的 allowlist（best-effort，可绕过），
而是通过**专用 `.git` worktree 隔离执行 + Lead diff 审查 + cherry-pick 接入主流**来保证。
cursor-agent 的输出在 Lead 审查前视为 untrusted。详情见 `.claude/rules/cursor-cli-only.md`。

