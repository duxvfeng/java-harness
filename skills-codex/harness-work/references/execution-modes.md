# Codex Execution Modes

Codex `harness-work` 使用原生 Codex 工具，Claude Code 则使用 Agent/Task 工具用语。

## Shared Preflight

1. 读取 `Plans.md`。
2. 遇到缺少 `Task`、`DoD`、`Depends` 或 `Status` 的旧表格格式时停止。
3. 检查当产品行为可能偏离时是否存在项目规格 SSOT。
   优先使用现有项目级文档，然后是 `docs/spec/00-project-spec.md`。
4. 如果任务变更产品行为、API、数据模型、权限、计费、
   集成或租户边界且不存在稳定规格，则在实现前创建或
   更新规格。
5. 仅对机械性工作跳过规格创建，如拼写错误、格式化、
   依赖项升级、仅文档或保持行为不变的重构任务。在
   任务上下文或 sprint contract 中记录跳过理由。
6. 从 Harness 插件根目录解析帮助脚本。
7. 保持实现和审查分离。

## Solo

为单个任务使用当前 Codex 会话。本地验证并在完成前运行正常审查循环。

## Parallel / Breezing

使用 Codex 原生子代理:

- `spawn_agent`
- `send_input`
- `wait_agent`
- `close_agent`

默认 Breezing worker 数是 `max`，意味着依赖已满足的现成任务数量。
不是无限生成。

## Companion Delegation

仅通过解析的插件根目录使用 companion 脚本:

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/codex-companion.sh" task --write "任务"
```
