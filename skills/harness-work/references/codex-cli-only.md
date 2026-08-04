# Codex 插件策略

调用 Codex 时必须使用 **官方插件 `openai/codex-plugin-cc`**。

## 基本方针

禁止直接调用 raw `codex exec`。只能通过以下 2 种方式调用 Codex:

1. **`scripts/codex-companion.sh`** — 从 Harness 技能/代理中调用
2. **`/codex:*` 命令** — 用户交互中的临时使用

## 禁止事项

- 直接调用 `codex exec`（`skills-codex/` 内除外，参见下文例外）
- 使用 `mcp__codex__codex`（MCP 服务器已废弃）
- 通过 ToolSearch 搜索 Codex MCP
- 通过 `claude mcp add codex` 重新注册 MCP 服务器

## MCP 阻止（v2.1.78+）

settings.json 的 `deny` 规则阻止旧 MCP 工具（已预设）:

```json
{
  "permissions": {
    "deny": ["mcp__codex__*"]
  }
}
```

## 正确调用方式

### 任务委托（实现·调试·调查）

```bash
# 可写的任务委托
bash scripts/codex-companion.sh task --write "修复 bug"

# stdin 方式（用于大型提示）
cat "$PROMPT_FILE" | bash scripts/codex-companion.sh task --write

# 恢复上一次的线程
bash scripts/codex-companion.sh task --resume-last --write "继续完成"
```

### 评审

```bash
# 工作树的评审
bash scripts/codex-companion.sh review

# 从特定 base ref 的评审
bash scripts/codex-companion.sh review --base "${TASK_BASE_REF}"

# 对抗性评审（挑战设计判断）
bash scripts/codex-companion.sh adversarial-review
```

### 设置·作业管理

```bash
# 确认 Codex 可用性
bash scripts/codex-companion.sh setup --json

# 确认运行中的作业
bash scripts/codex-companion.sh status

# 获取作业结果
bash scripts/codex-companion.sh result <job-id>

# 取消作业
bash scripts/codex-companion.sh cancel <job-id>
```

### /codex:* 指令（用户交互）

```
/codex:setup              — Codex CLI 设置确认
/codex:rescue             — 任务委托（调查·实现·调试）
/codex:review             — 代码评审
/codex:adversarial-review — 对抗性评审
/codex:status             — 作业状态确认
/codex:result             — 获取作业结果
/codex:cancel             — 取消作业
```

## verdict 映射（官方插件 ↔ Harness）

官方插件的评审输出使用与 Harness 不同的架构。转换规则:

| 官方 plugin | Harness | 备注 |
|---|---|---|
| `approve` | `APPROVE` | |
| `needs-attention` | `REQUEST_CHANGES` | |
| `findings[].severity: critical` | `critical_issues[]` | 影响 verdict |
| `findings[].severity: high` | `major_issues[]` | 影响 verdict |
| `findings[].severity: medium/low` | `recommendations[]` | 不影响 verdict |

## 例外: Codex 原生技能

`skills-codex/` 内的技能**在 Codex CLI 内部运行**，
因此 `spawn_agent` / `wait_agent` / `send_input` / `close_agent` 等
Codex 原生 API 可继续使用。但建议评审调用通过 companion。

## 官方插件提供的功能

| 功能 | 说明 |
|------|------|
| 作业管理 | 线程的启动·恢复·取消·结果获取 |
| App Server Protocol | 通过 JSON-RPC over TCP 实现高可靠 Codex 通信 |
| 结构化输出 | 符合 `review-output.schema.json` 的结构化评审 |
| Stop Review Gate | 会话结束时的自动评审关卡 |
| GPT-5.4 Prompting | Codex 优化的提示指引 |
