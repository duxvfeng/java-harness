# Harness Setup Reference: codex

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

### codex — Codex CLI 设置

```bash
# 安装确认（Codex CLI 是基于 Node.js。与 Harness 本体是不同的工具）
which codex || npm install -g @openai/codex

# 超时命令确认（macOS）
TIMEOUT=$(command -v timeout || command -v gtimeout || echo "")
# macOS 的情况: brew install coreutils
```

> **注意**: Harness v4.0 本体（`harness` 命令）是不需要 Node.js 的 Go 二进制文件。
> Codex CLI（`codex` 命令）是不同的工具，仍然需要 Node.js。

### Codex provider / model metadata policy (0.123.0+ / 0.130.0)

Codex `0.123.0` 以降の provider / model guidance と、Codex `0.130.0` stable の Bedrock `aws login` guidance は
`docs/codex-provider-setup-policy.md` を正本として扱う。

要点:

- 使用 Bedrock 时，使用 Codex built-in provider 的 `amazon-bedrock`。
- AWS profile 在 user / project 的 Codex config 中放在 `[model_providers.amazon-bedrock.aws]`。
- 来自 `aws login` profiles 的 AWS console-login credentials 作为 AWS 侧的 profile material 处理。
- Harness 不写入 AWS credential、console-login cache、provider endpoint。
- Harness 的分发用 Codex config 不将 `model = "gpt-5.4"` 固定为 setup default。
- Harness 的分发用 Codex config 也不将 `model_provider = "amazon-bedrock"` 固定为 setup default。
- `gpt-5.4` 作为 Codex 本体的 current model metadata 处理，不保留旧的 `gpt-5.2-codex` 等作为推荐 sample。
- Claude Code 侧的 `CLAUDE_CODE_USE_BEDROCK` / `ANTHROPIC_DEFAULT_*` / `modelOverrides` guidance 与 Codex 的 `model_provider = "amazon-bedrock"` 不混用。

只有使用 Bedrock 的 user / project，根据需要添加以下内容：

```toml
model_provider = "amazon-bedrock"

[model_providers.amazon-bedrock.aws]
profile = "codex-bedrock"
```

Claude Code 侧的 provider / MCP / telemetry guidance 请参考
`docs/claude-code-setup-mcp-telemetry-provider.md`。
特别是 `ANTHROPIC_BEDROCK_SERVICE_TIER` 只在 Bedrock 使用者的 provider 环境中处理，
不放入 Harness 的 plugin default / template / shared project settings。

### Codex app-server / plugin workflow policy (0.130.0)

Codex `0.130.0` stable (`rust-v0.130.0`, published `2026-05-08T23:09:55Z`) の app-server / plugin workflow guidance は
`docs/codex-plugin-workflows-policy.md` を正本として扱う。

要点:

- `codex remote-control` 是 headless remotely controllable app-server 的显式启动 entrypoint。Harness setup 不在 config 中写入 remote-control defaults。
- App-server clients can page large threads。长的 loop / Breezing transcript 确认必要的 page 范围。
- `view_image` 可以通过 multi-environment session 的 selected environments 解析 file。artifact report 附上 environment / workdir。
- Live app-server threads 无需重启即可获取 config 更改。但是 secret / provider / hook policy 的更改通过 diff 和 verification 处理。
- Turn diffs 在 `apply_patch` 包括部分失败时保持准确。最终判断通过 `git diff` 和 tests 确认。
- Plugin details 现在显示 bundled hooks。在 install / share 前确认 bundled hooks，Harness bundled hooks 保持 opt-in。
- Plugin sharing 暴露 link metadata 和 discoverability controls。公开范围和 metadata 作为 release surface 确认。
- Configurable OpenTelemetry trace metadata 限定为 debugging / triage 辅助，不放入个人信息、顾客信息、secret。
- Built-in MCPs first-class runtime servers 作为 Codex runtime owned surface 处理，不与 plugin-provided MCP 和所有者混用。
- `CODEX_HOME` environments TOML provider 是 user-level environment source。报告选择的 environment，write turn 固定为 one primary environment。
- 不依赖 Remove skills list extra roots，明确 Harness mirror install 或 `[[skills.config]]` path-based loading。

### Codex MCP diagnostics / plugin loading (0.123.0+)

Codex `0.123.0` 以降の MCP diagnostics / plugin MCP loading guidance は
`docs/codex-mcp-diagnostics.md` を正本として扱う。

要点:

- 在 Codex TUI 中，平时用 `/mcp` 轻量确认 server 状态。
- 只在 MCP server 不可见、resources 不输出、resource templates 无法读取时使用 `/mcp verbose`。
- 在 `/mcp verbose` 中确认 diagnostics / resources / resource templates。
- plugin 内 `.mcp.json` 以可接受 `mcpServers` 形式和 top-level server map 形式两者为前提进行说明。
- 在新 plugin 中优先使用易于共享的 `mcpServers` 形式。
- 如果现有 plugin 是 top-level server map 形式，利用 Codex 侧的 loading 改善，避免不必要的重写。
- 不与 Claude Code 侧的 `claude mcp ...`、`.claude/mcp.json`、hook `type: "mcp_tool"` guidance 混用。

`mcpServers` 形式:

```json
{
  "mcpServers": {
    "docs": {
      "command": "node",
      "args": ["server.js"]
    }
  }
}
```

top-level server map 形式:

```json
{
  "docs": {
    "command": "node",
    "args": ["server.js"]
  }
}
```

### Codex sandbox / execution policy (0.123.0+)

Codex `0.123.0` 以降の `remote_sandbox_config` と `codex exec` shared flags guidance は
`docs/codex-sandbox-execution-policy.md` を正本として扱う。

要点:

- `remote_sandbox_config` 作为 `requirements.toml` 的 host-specific sandbox policy 进行说明。
- 像 remote devbox / ephemeral CI runner / shared host 那样，比较并决定每个 remote environment 的 `allowed_sandbox_modes`。
- host matching 是便利的分类，但不是强的 device authentication。在高风险环境中避免 broad wildcard。
- Harness 的分发用 `codex/.codex/config.toml` 不写入 organization-specific 的 `remote_sandbox_config`。
- Codex `0.123.0` 以后 `codex exec` 继承 root-level shared flags，因此 wrapper 侧不添加重复的 `--approval-policy` / `--sandbox` pairs。
- `scripts/codex-companion.sh task --write` 附带 `--sandbox workspace-write` 是将 Harness 的"写入任务"意图转换为 exec-local，不是 root shared flags 的重复转发。
- `scripts/codex/codex-exec-wrapper.sh` 的 `--full-auto` 在 53.2.4 中维持。如果要更改，在另外的 task 中添加 approval / sandbox behavior 的回归测试。

requirements example:

```toml
allowed_sandbox_modes = ["read-only"]

[[remote_sandbox_config]]
hostname_patterns = ["devbox-*.corp.example.com"]
allowed_sandbox_modes = ["read-only", "workspace-write"]
```

**使用模式**（通过官方插件）:
```bash
bash scripts/codex-companion.sh task --write "任务内容"
# 或者通过 stdin
cat /tmp/prompt.md | bash scripts/codex-companion.sh task --write
```

