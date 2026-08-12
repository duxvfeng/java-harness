# Codex Plugin 与 Workflow 策略

最后更新：2026-08-12

本文规定 Java Harness 如何处理 Codex plugin、workflow、`/goal`、app-server、MCP、sandbox 和外部 agent 的所有权边界。

## 单一事实来源

`Plans.md` 是 Harness 的任务 SSOT。Codex `/goal` 和 Claude Code 原生 `/goal` 都只能作为当前 turn 的 continuation memo，不能替代计划、验收标准或完成定义。

| 内容 | 正确归属 |
|------|----------|
| 长期任务、任务状态、验收标准、DoD | `Plans.md` |
| 当前 turn 的临时聚焦目标 | `/goal` |
| 最终完成判断 | `Plans.md` + `git diff` + 测试/验证结果 |

禁止：只在 `/goal` 中保存 acceptance criteria；维护两套独立 task list；worker 未读取 `Plans.md` 就宣布完成；用 `/goal` 的完成条件覆盖 `Plans.md` 的 DoD。

## Java Harness 的分发边界

Java 版本的 Codex plugin 入口是 `.codex-plugin/plugin.json`，技能目录是 `skills-codex/`，项目级 Codex 配置是 `.codex/config.toml`。当前 manifest 的 `hooks` 为空对象，Codex project hooks 由兼容层和项目配置管理，不应把 Go 版本的脚本路径直接复制到 Java 文档或 wrapper 中。

## Codex workflow surface

Codex 新增的能力应作为显式的观察和连接手段处理，不改变 Harness 的基本边界：

| 能力 | Java Harness 规则 |
|------|-----------------|
| `codex remote-control` | 由用户或 operator 显式启动；不在项目默认配置中写入 remote-control defaults |
| app-server thread pagination | 长 transcript 分页读取，并记录读取的 page；不假设一次加载全部内容 |
| selected-environment `view_image` | artifact 记录 environment 和 workdir；不能把其他环境的文件当作主仓库证据 |
| live config refresh | 可用于确认配置变化，但 secret、权限和 provider 变更仍需 diff 与验证 |
| turn diff | 仅作 review 辅助；最终以 `git diff`、测试和包检查为准 |
| bundled hooks | 安装或分享前检查；默认保持 opt-in，破坏性操作、push、deploy 和外部发送默认关闭 |
| sharing metadata | 将版本、用途、风险 hook 和 discoverability 作为 release 信息明确记录 |
| OTel trace metadata | 只记录低基数排障信息，不记录凭据和个人/客户数据 |
| built-in MCP | 与 plugin-provided MCP 区分所有者和配置来源 |
| `CODEX_HOME` environment | 报告实际选择的 environment；写入操作固定在一个 primary environment |

## Plugin hooks 与外部 agent

plugin 只要包含 hooks，就必须明确 opt-in 入口、权限范围、stdout JSON contract 和副作用。安装 plugin 不应自动改变项目的强行为。

外部 agent 的所有权如下：

| 类型 | 所有者 | Java Harness 处理 |
|------|--------|------------------|
| Harness 自带 agent | Harness | 在本仓库内 review、测试和同步 |
| 用户本地 agent | 用户 | setup 不覆盖 |
| 第三方 plugin agent | 第三方 plugin | 作为依赖使用，不静默 fork |
| 已复制的外部 agent | Harness fork | 记录来源、修改理由和后续更新责任 |

## 并行与环境安全

`agents.max_threads = 8` 是上限，不是默认并发数。独立文档调查可以并行；共享文件的写入、review、集成和最终验证应串行。

sticky 或 remote environment 复用时：

- 每个 write turn 只选择一个 primary environment；
- 开始任务先检查 `git status --short`；
- 确认 app-server 的 port、pid 和 health；
- 不把环境变量和 secret 写入日志；
- 不擅自删除不属于当前任务的 stale artifact。

## 证据与完成判断

Java Harness 的完成判断不能依赖 Codex 或 cloud 侧的 task state 自动同步。最终必须回到本地仓库，核对 `Plans.md`、`git diff`、测试结果和必要的 `harness validate` / `harness doctor` 输出。
