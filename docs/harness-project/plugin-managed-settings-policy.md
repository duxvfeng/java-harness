# Plugin Managed Settings 策略

最后更新：2026-08-12

本文固定 Claude Code plugin、Marketplace、managed settings 和 managed sandbox 的责任边界。Harness 负责说明使用方式，不替换 Claude Code 本体的 resolver 和 managed settings enforcement。

## 核心原则

Harness 是入口说明，不是第二个 marketplace 或 dependency resolver。企业约束应由 Claude Code 的 managed settings 或终端管理系统施加，不能通过 Java Harness 的普通项目默认值绕过配置优先级。

## 设置判断

| 设置 | 用途 | Java Harness 处理 |
|------|------|-----------------|
| `extraKnownMarketplaces` | 引导并注册团队推荐的 marketplace | onboarding 可说明；由 Claude Code 执行注册 |
| `blockedMarketplaces` | 阻止指定 marketplace source | 仅 managed settings；不放入普通默认配置 |
| `strictKnownMarketplaces` | 只允许白名单 marketplace source | 仅 managed settings；不放入普通默认配置 |
| `DISABLE_AUTOUPDATER` | 停止自动更新 | 由用户或组织按需设置，Harness 不默认写入 |
| `DISABLE_UPDATES` | 停止自动和手动更新 | 只用于有独立版本分发流程的管理环境 |
| plugin dependency auto-resolve | 解析 plugin 依赖 | 交给 Claude Code 本体，不实现第二套 resolver |
| plugin `themes/` | 分发终端主题 | Java Harness 当前不附带主题 |
| `wslInheritsWindowsSettings` | WSL 继承 Windows managed settings | 企业候选配置，不进入 Harness default |
| `allowManagedDomainsOnly` / `allowManagedReadPathsOnly` | 将 sandbox 边界交给管理员 | 仅 managed settings，不写入普通模板或 `harness.toml` |

## 更新控制

`DISABLE_AUTOUPDATER` 只停止自动更新；`DISABLE_UPDATES` 还会停止手动 `claude update`。后者启用后，组织必须提供已验证版本的分发和更新办法。

Java Harness 不在 `.claude-plugin/settings.json`、项目模板或普通 `harness.toml` 中默认设置这两个变量。即使禁用自动更新，也要保留 Harness 的版本同步、校验和 release 流程。

## Marketplace

`strictKnownMarketplaces` 是策略门，不等于自动注册；需要同时给团队注册推荐源时，再由管理员组合 `extraKnownMarketplaces`。

```json
{
  "strictKnownMarketplaces": [
    { "source": "github", "repo": "acme-corp/approved-plugins" }
  ],
  "extraKnownMarketplaces": {
    "acme-tools": {
      "source": {
        "source": "github",
        "repo": "acme-corp/approved-plugins"
      }
    }
  }
}
```

普通用户遇到依赖错误时，先查看 Claude Code 的 `/plugin` 错误、`/doctor` 和 `claude plugin list --json`，再确认 marketplace 是否已注册。不要在 Java Harness 中增加独立的依赖解析器。

## Hooks、主题与 sandbox

plugin hooks 默认应为 opt-in，破坏性操作、push、deploy 和外部发送默认关闭。Hook stdout 必须保持约定的 JSON 格式；如果改写 tool output，还必须遵循本仓库既有的 output governance 规则。

Java Harness 当前是工作流和安全规则 plugin，不随普通安装分发 `themes/`。主题涉及品牌、终端兼容性和可访问性，应单独评审。

`allowManagedDomainsOnly`、`allowManagedReadPathsOnly` 等键属于管理员约束。Java Harness 可以在文档中说明其意义，但不能通过项目模板降低或覆盖 Claude Code 的 managed settings 优先级。

## 参考

- [Claude Code 更新日志](https://code.claude.com/docs/en/changelog)
- [Claude Code Settings](https://code.claude.com/docs/en/settings)
- [Plugin Dependencies](https://code.claude.com/docs/en/plugin-dependencies)
- [Discover Plugins](https://code.claude.com/docs/en/discover-plugins)
