# Codex MCP 诊断与 Plugin 加载

最后更新：2026-08-12

本文只讨论 Codex TUI 的 `/mcp`、`/mcp verbose` 和 plugin 内 `.mcp.json` 加载。不要把它与 Claude Code 的 `claude mcp`、`.claude/mcp.json` 或 `hooks.json` 的 `mcp_tool` 类型混用。

## `/mcp` 与 `/mcp verbose`

| 情况 | 使用方式 | 检查内容 |
|------|----------|----------|
| 只确认 server 是否注册 | `/mcp` | server 名称和连接状态 |
| server 不显示 | `/mcp verbose` | 启动、认证和配置解析错误 |
| tool 可见但 resource 不见 | `/mcp verbose` | resources 和 resource templates |
| 怀疑 plugin `.mcp.json` 未加载 | `/mcp verbose` | plugin MCP loading 和 diagnostics |

日常检查先执行 `/mcp`。只有出现不可见、认证失败、resource 缺失或配置解析异常时，再执行 `/mcp verbose`。

## 诊断步骤

1. 在 Codex TUI 执行 `/mcp`。
2. 记录期望的 server 是否出现以及状态。
3. 需要深入排查时执行 `/mcp verbose`。
4. 依次查看 diagnostics、resources 和 resource templates。
5. 只有 plugin 来源的 server 异常时，再检查 plugin 内 `.mcp.json` 的结构。

## `.mcp.json` 形式

新 plugin 优先使用便于跨工具共享的 `mcpServers` 形式：

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

Codex 也可能加载 top-level server map：

```json
{
  "docs": {
    "command": "node",
    "args": ["server.js"]
  }
}
```

已有 plugin 使用第二种形式时，不要仅为格式统一而进行无风险之外的重写；先利用 Codex 的兼容加载能力。

## Java Harness 适配

当前 `.codex-plugin/plugin.json` 没有声明 MCP server。将来新增 `.mcp.json` 时，应在 manifest、文档和 `/mcp verbose` 验证记录中说明 server 的所有者和配置来源。

不要把 secret 直接写入 `.mcp.json`，应使用环境变量或受管理的 credential store。resources 或 resource templates 为空不一定是错误，有些 server 只提供 tools。
