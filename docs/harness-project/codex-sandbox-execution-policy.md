# Codex Sandbox 与执行策略

最后更新：2026-08-12

本文记录 Java Harness 如何看待 Codex sandbox、`remote_sandbox_config` 和 `codex exec` 的 shared flags。

## 核心原则

组织级、按主机区分的 sandbox 约束属于 Codex requirements；用户级偏好属于 Codex config 或显式 CLI 参数。Harness 不把组织的 host policy 写入普通项目默认配置。

Java 版本当前没有 Go 版本中的 `scripts/codex-companion.sh` 或 `scripts/codex/codex-exec-wrapper.sh`。因此本文只记录适用的策略，不声称 Java 仓库已经提供这些 wrapper。

## `remote_sandbox_config`

`remote_sandbox_config` 应放在管理员维护的 `requirements.toml` 中，用于按主机类别选择允许的 sandbox mode：

```toml
allowed_sandbox_modes = ["read-only"]

[[remote_sandbox_config]]
hostname_patterns = ["devbox-*.corp.example.com"]
allowed_sandbox_modes = ["read-only", "workspace-write"]
```

典型判断：

| 环境 | 策略 |
|------|------|
| 本地开发机 | 使用组织默认的 `allowed_sandbox_modes` |
| Remote devbox | 允许 `workspace-write`，避免放开整个主机 |
| 临时 CI runner | 仅在隔离充分时允许更宽模式，主机匹配保持窄范围 |
| 高风险共享主机 | 在 requirements 中强制 `read-only` |
| 未匹配主机 | 回退到 requirements 顶层规则 |

主机名匹配只是便利的分类，不是强设备认证。高风险环境不要使用宽泛 wildcard。不要把这些条目复制到 Java 项目的 `.codex/config.toml`。

## `codex exec` shared flags

当 Codex 支持 root-level shared flags 继承到 `exec` 时，调用方应避免重复添加 `--approval-policy` 和 `--sandbox`：

- 每次调用尽量只有一个 sandbox/approval 来源；
- 只有表达 Harness workflow 意图时，wrapper 才可以增加明确的参数；
- model 选择不应由通用 wrapper 固定；
- 最终权限以实际 Codex runtime 和管理员 requirements 为准。

Java Harness 的 `install-codex.sh` 只创建基础 `.codex/config.toml`，不写入 organization-specific sandbox policy。未来增加执行 wrapper 时，必须单独添加 approval/sandbox 行为回归测试。

## 验证与责任边界

修改 requirements 或用户 config 后，检查实际选择的 environment、sandbox mode 和配置来源；不要在日志中记录 secret。安全边界必须能由用户、管理员和 Codex runtime 各自解释，不能由 Harness 静默改写。

参考：

- [Codex 配置参考](https://developers.openai.com/codex/config-reference)
- [Codex releases](https://github.com/openai/codex/releases)
