# Sandbox Allowlist 配置配方

最后更新：2026-08-12

本文用于 Claude Code sandbox 下的外部 API、Firecrawl、Web scraping 和 Cursor backend 连接问题。它不是 Java Harness 自动配置脚本，涉及全局安全边界的修改必须由用户手动完成。

## 常见症状

```text
HTTP/2 403
x-deny-reason: host_not_allowed
```

或：

```text
curl: (6) Could not resolve host: api.firecrawl.dev
```

原因通常是 Claude Code sandbox 的网络 allowlist 未包含目标域名。allowlist 为空时，外向通信可能全部被拒绝。

## 修改前的安全要求

- 先备份 `~/.claude/settings.json`；
- 只增加明确需要的域名，不要使用无边界的 `*`；
- 保留既有 `failIfUnavailable`、`filesystem`、`denyRead` 和 `deniedDomains`；
- 不要用整个 `sandbox` 对象覆盖已有配置；
- AI 只提供变更建议，用户在终端或编辑器中手动应用；
- 配置文件可能包含 token，修改后保持原文件权限。

## Cursor backend 的最低配置

Java Harness 的 Cursor 规则把 Cursor 当作实现 worker，输出在 Lead review 前视为不可信。Cursor 的 allowlist 只是 best-effort convenience，不是安全边界。

通常需要用户在 Claude Code 全局 sandbox 中保留两项授权：

```json
{
  "sandbox": {
    "network": {
      "allowedDomains": ["*.cursor.sh"]
    },
    "filesystem": {
      "allowWrite": ["~/.cursor"]
    }
  }
}
```

关键名是 `allowWrite`，不是 `write`。错误的键名会被忽略并导致 `~/.cursor/projects` 或 `~/.cursor/cli-config.json.tmp` 出现 `EPERM`。已有 `sandbox` 时只对内部数组做 union，不能替换整个对象。

## 外部 API / Firecrawl 配置

按任务需要加入明确域名。常见分类如下：

| 类别 | 示例 |
|------|------|
| 开发依赖 | `github.com`、`api.github.com`、`registry.npmjs.org`、`pypi.org`、`repo.maven.apache.org` |
| Claude / Firecrawl | `api.anthropic.com`、`api.firecrawl.dev`、`firecrawl.dev` |
| 目标站点 | 只添加当前任务实际访问的站点，例如 `zenn.dev`、`qiita.com`、`dev.to` |

同时保留对云 metadata endpoint 和常见外传站点的 deny，例如 `169.254.169.254`、`metadata.google.internal`、`pastebin.com`、`transfer.sh`。具体列表应由组织安全策略决定，不应从示例无审查地全量复制。

## 手动 union 示例

在 Linux/macOS 上，使用 `jq` 时应先备份并合并数组；在 Windows PowerShell 上，请使用等价的 JSON 解析和写回操作，不要通过字符串替换 JSON：

```bash
SETTINGS=~/.claude/settings.json
cp -p "$SETTINGS" "${SETTINGS}.bak.$(date +%Y%m%d-%H%M%S)"

jq '
  .sandbox.enabled = true |
  .sandbox.network.allowedDomains =
    (((.sandbox.network.allowedDomains // []) + ["*.cursor.sh", "api.firecrawl.dev"]) | unique) |
  .sandbox.network.deniedDomains =
    (((.sandbox.network.deniedDomains // []) + ["169.254.169.254", "metadata.google.internal"]) | unique) |
  .sandbox.filesystem.allowWrite =
    (((.sandbox.filesystem.allowWrite // []) + ["~/.cursor"]) | unique)
' "$SETTINGS" > "${SETTINGS}.tmp" && mv "${SETTINGS}.tmp" "$SETTINGS"
```

该示例只展示合并原则。生产环境还应保留原文件 mode，并在写回前检查临时文件权限。Windows 用户应先复制备份，再用 `ConvertFrom-Json` / `ConvertTo-Json` 修改对象属性。

## 验证

```bash
jq -e '.' ~/.claude/settings.json > /dev/null && echo "VALID JSON"
jq '.sandbox.network.allowedDomains' ~/.claude/settings.json
jq '.sandbox.network.deniedDomains' ~/.claude/settings.json
jq '.sandbox.filesystem.allowWrite' ~/.claude/settings.json
```

修改后完全重启 Claude Code，使 session start 重新加载 sandbox 配置。先做最小范围的网络 smoke test，再决定是否增加域名。

## 不应自动化的部分

`~/.claude/settings.json` 是限制 Claude Code 自身的安全边界。Java Harness、skill 或 AI 不应为了让某个请求成功而自动放宽网络、文件系统或 secret-read 权限。需要扩大边界时，应由用户明确批准、记录原因并在任务结束后复查。
