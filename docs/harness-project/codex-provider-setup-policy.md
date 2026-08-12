# Codex Provider 设置策略

最后更新：2026-08-12

本文说明 Codex provider、model 和 AWS Bedrock 配置在 Java Harness 中的责任边界。Harness 可以给出配置示例，但不在分发配置中固定用户的模型、provider 或凭据。

## 默认原则

Java Harness 的 `.codex/config.toml` 只承载项目级 Harness/Codex 基础设置。不要因为 plugin 安装就自动加入：

- `model = "..."`；
- `model_provider = "amazon-bedrock"`；
- AWS access key、secret key、session token 或 console-login cache；
- provider endpoint、组织账号或区域特定值。

需要复现性时，由用户在 user/project Codex config 中显式固定 model；需要 Bedrock 时，由用户显式选择 provider。

## Bedrock 示例

Codex 使用 Bedrock 时，示例配置可以是：

```toml
model_provider = "amazon-bedrock"

[model_providers.amazon-bedrock.aws]
profile = "codex-bedrock"
```

`profile` 只引用 AWS 侧已有的 profile 名称。Harness 不创建、复制或保存 profile 内容及任何 credential material。

Claude Code 的 `CLAUDE_CODE_USE_BEDROCK`、`ANTHROPIC_DEFAULT_*`、`ANTHROPIC_BEDROCK_SERVICE_TIER` 和 `modelOverrides` 属于另一个 runtime surface，不能与 Codex 的 `model_provider` 混用。

## 配置层级

| 层级 | 责任 |
|------|------|
| Java Harness plugin / `.codex-plugin/plugin.json` | 分发技能和 manifest，不固定用户 provider |
| 项目 `.codex/config.toml` | 用户选择的项目级 Codex 配置 |
| 用户 `~/.codex/config.toml` | 用户级 model/provider 和 credential 引用 |
| AWS profile / credential store | AWS 认证材料，由 AWS 工具链管理 |

如果项目配置和用户配置冲突，应报告 Codex 实际选择的 provider/model，而不是由 Harness 静默覆盖。

## 验证

排查时检查配置来源和实际选择值，但不要输出 credential 内容：

```bash
codex --version
codex --help
```

需要固定模型时，先确认当前 Codex 版本支持的 model metadata，再由用户在自己的配置中添加值。Java Harness 文档不把旧 model slug 作为推荐默认值。

## 安全边界

Harness 不写入 AWS access key、secret key、session token、temporary token、console-login cache，也不把它们放入日志、Plans 或 telemetry。provider 配置的正确性由用户、组织和 Codex runtime 共同负责。
