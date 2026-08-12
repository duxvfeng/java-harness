# Java 与 Go 能力边界

本文档说明 `D:\go-project\claude-code-harness` 与本 Java 版本的能力差异。
Java 版本可以复用同一套 Skills 和文档契约，但不能直接执行 Go 版本的 helper script。

## Java 版本可直接使用

以下命令由 Java CLI 提供，前提是 `harness` 已安装并在 `PATH` 中：

```bash
harness init
harness doctor
harness validate all
harness status --json
harness gen
harness sprint-contract generate --task <task-id>
harness sprint-contract validate --contract <contract-path> --strict
harness sprint-contract list
harness evidence collect --task <task-id>
harness evidence report --task <task-id> --format json
harness evidence list
```

本地从源码运行时，可将 `harness` 替换为：

```bash
java -jar java-harness-cli/target/java-harness-cli-4.1.1.jar <command> [args...]
```

会话命令使用 `harness-save-session`、`harness-restore-session`、
`harness-list-sessions`、`harness-show-session` 和
`harness-cleanup-sessions`。项目级健康检查优先使用 `harness doctor` 和
`harness validate all`。

## 当前仅 Go 版本提供

以下能力在 Go 版本中由 `scripts/` 下的脚本实现，Java 版本当前没有等价的
可执行入口：

| Go helper | Java 版本状态 | Java 侧处理方式 |
|---|---|---|
| `codex-companion.sh`、`cursor-companion.sh` | 不提供 | 由宿主 Codex/Claude 工具直接委托 worker；不要执行同名脚本 |
| `resolve-impl-backend.sh`、`set-impl-backend.sh`、`model-routing.sh` | 不提供 | 在 `harness.toml` 的 `[harness].backend` 中记录项目配置；Java CLI 不自动启动外部 backend |
| `setup-cursor.sh` | 不提供 | 手动检查 `cursor-agent --version`，并按 sandbox 文档配置用户级权限 |
| `generate-sprint-contract.js` | 不提供 | 使用 `harness sprint-contract generate --task <task-id>` |
| `enrich-sprint-contract.sh`、`ensure-sprint-contract-ready.sh` | 不提供 | 使用 `harness sprint-contract validate --contract <path> --strict`，人工补充 reviewer 信息 |
| `enable-1h-cache.sh`、`claude-longrun.sh` | 不提供 | 使用宿主平台的 loop、resume 或 session 功能；完成标准不变 |
| `review-ai-residuals.sh`、`write-review-result.sh` | 不提供 | 使用 `harness review` skill 和 `harness evidence` 收集结果 |
| `sync-skill-mirrors.sh` | 不提供 | 手动维护 `skills/` 与 `skills-codex/`，再运行 `harness validate all` |

## 使用规则

1. `skills/` 和 `skills-codex/` 中出现的 Go helper，只能作为 Go 版本实现说明，不能视为 Java 版本命令。
2. Java 版本的文档示例必须优先使用上面的 CLI 命令；没有等价物时必须明确写出“不提供”。
3. Java CLI 的核心 workflow 命令会路由到 skill 文本。实际的 agent、worktree、review 和 cherry-pick 仍由宿主平台执行。
4. 不要为了消除文档中的路径检查而在 Java 版本创建空壳脚本；这会掩盖真实能力差异。
