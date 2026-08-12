# Claude Code 设置：MCP、Telemetry 与 Provider 策略

最后更新：2026-08-12

本文说明 Java Harness 如何引导 Claude Code 的 MCP、telemetry、provider、Windows Shell 和 deferred tools 设置。Harness 只提供使用建议，不替换 Claude Code 本体的配置语义。

## 核心原则

Claude Code 是工具运行时，Harness 是工作流程和安全边界的说明层。默认配置应保持小而明确，按需启用能力，不把特定团队或 provider 的设置写进通用项目模板。

## 设置清单

| 项目 | Java Harness 处理方式 |
|------|----------------------|
| `${CLAUDE_EFFORT}` | 只用于读取当前调用方的 effort，不由 skill 强制修改；变量为空时也不应导致流程失败 |
| MCP `alwaysLoad` | 只有每个 turn 都必需的小型 server 才设为 `true`；大型 server 保持 deferred |
| `claude plugin prune` | plugin 卸载后的依赖清理；先使用 `--dry-run` |
| `claude project purge` | 删除 project state 的强清理；先使用 `--dry-run` 或 `--interactive` |
| `ANTHROPIC_BEDROCK_SERVICE_TIER` | 仅由 Bedrock 用户在 provider 环境中设置，不写入 Harness 默认配置 |
| `claude_code.skill_activated.invocation_trigger` | telemetry 中区分用户显式调用、Claude 主动调用和嵌套 skill 调用 |
| Windows Shell | Windows 优先按 PowerShell 说明，同时避免只给 Bash 固定示例 |
| forked skill / subagent | 首个 turn 需要的 deferred tool 必须在 workflow 中明确发现和验证 |

## effort 读取

`${CLAUDE_EFFORT}` 表示当前调用环境的 effort level。它用于解释当前运行强度或做轻量分支，不代表 skill 可以自行提升 effort。

可以这样写：

```md
Current effort: `${CLAUDE_EFFORT}`.
If effort is low, report confirmed blockers only.
If effort is xhigh, include adversarial checks.
```

不要在 skill 中要求调用方必须切换到某个 effort，也不要在变量为空时把它当成配置错误。

## MCP `alwaysLoad`

MCP tool search 会延迟加载 tool schema，以减少上下文占用。`alwaysLoad: true` 会让 server 在 session 开始时始终可见。

适合启用的场景：

- 每个 turn 都使用的小型核心 server；
- workflow 第一阶段必需的 server；
- 延迟发现会明显影响可靠性的少数 server。

不适合启用的场景：

- tool 数量很多的 server；
- 很少使用的 integration；
- schema 很大的数据库或 observability server。

```json
{
  "mcpServers": {
    "core-tools": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "alwaysLoad": true
    }
  }
}
```

## Plugin 与 project 清理

`claude plugin prune` 用于清理由 plugin 依赖自动安装、现在已不再需要的 plugin，不应用来无提示删除用户直接安装的 plugin：

```bash
claude plugin prune --dry-run
claude plugin prune -y
```

Java Harness 的 setup 只在卸载后的清理场景提示该命令，不在初始化或 release 流程中无条件执行。

`claude project purge [path]` 会删除 project 的 transcripts、tasks、file history 和配置记录，是更强的清理操作：

```bash
claude project purge . --dry-run
claude project purge . --interactive
```

进行中的任务、需要保留的 transcript 或未确认删除范围时，不应使用该命令。

## Provider 与 telemetry

`ANTHROPIC_BEDROCK_SERVICE_TIER`、`CLAUDE_CODE_USE_BEDROCK` 和其他 `ANTHROPIC_*` 变量属于 Claude Code / Anthropic provider 边界。不同账号、区域和组织可能需要不同值，因此不放入 Java Harness 的 plugin default、模板或共享项目配置。

`claude_code.skill_activated.invocation_trigger` 可用于区分：

| 值 | 含义 |
|----|------|
| `user-slash` | 用户通过 slash command 显式启动 |
| `claude-proactive` | Claude 根据上下文主动启动 |
| `nested-skill` | 由其他 skill 或 workflow 内部启动 |

telemetry 只记录排障所需的低基数元数据，不写入用户数据、客户数据、API key、provider credential 或私有 URL。

## Windows Shell 与 deferred tools

Windows 环境优先使用 PowerShell 示例：

- 同时给出 `pwsh` / PowerShell 语法，不能只给 POSIX `export`；
- 注意路径分隔符、引号和环境变量语法差异；
- 不把 Git Bash 假定为唯一可用 shell。

`context: fork` skill 和 subagent 同样可能遇到 deferred tools。workflow 应明确：

- WebFetch 等工具是否需要加入允许列表；
- MCP server 名称和使用目的；
- 首个 turn 看不到工具时的发现、诊断和重试步骤。

## 参考

- [Claude Code 更新日志](https://code.claude.com/docs/en/changelog)
- [Claude Code MCP](https://code.claude.com/docs/en/mcp)
- [Claude Code Plugins](https://code.claude.com/docs/en/plugins-reference)
