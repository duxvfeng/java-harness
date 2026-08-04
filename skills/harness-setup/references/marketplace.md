# Harness Setup Reference: marketplace

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

## Plugin 安装 (v2.1.71+ Marketplace)

v2.1.71 大幅改善了 Marketplace 的稳定性。
Claude Code 2.1.117-2.1.118 以后的 plugin / managed settings 方针以
`docs/plugin-managed-settings-policy.md` 为正本。

### 推荐安装方式

```bash
# @ref 形式固定版本（推荐）
claude plugin install owner/repo@v4.0.0

# 最新版
claude plugin install owner/repo
```

推荐使用 `owner/repo@vX.X.X` 形式。通过 `@ref` 解析器修正，无论是标签、分支、提交哈希都能准确解析。

### 更新

```bash
claude plugin update owner/repo
```

在 v2.1.71 修复了 update 时的 merge conflict，可以进行稳定的更新。

### 其他改善点

- MCP server 重复排除：自动防止同一 MCP 服务器的多重注册
- `/plugin uninstall` 使用 `settings.local.json`：准确反映到用户本地设置

### Managed marketplace / dependency policy (v2.1.117+)

企業利用で plugin marketplace を制御する場合は、Claude Code 本体の managed settings を使う。
Harness は独自の marketplace resolver や dependency resolver を重ねない。

| 項目 | 用途 | Harness 的处理 |
|------|------|----------------|
| `extraKnownMarketplaces` | 向团队推荐 marketplace 并引导、注册 | 在通常的 onboarding 中优先使用这个 |
| `blockedMarketplaces` | 阻止特定的 marketplace source | 专用於 managed settings。不放入面向通常用户的 default |
| `strictKnownMarketplaces` | 只能添加允许的 marketplace source | 专用於 managed settings。不放入面向通常用户的 default |
| plugin dependency auto-resolve | `dependencies` 的自动 install / missing dependency hints | 交给 Claude Code 本体。不添加 Harness 独自的 resolver |
| plugin `themes/` directory | plugin 分发 theme | 本次是 P: 将来任务。Harness 不附带 theme |

`DISABLE_AUTOUPDATER` 停止自动更新。
`DISABLE_UPDATES` 连手动的 `claude update` 也停止，面向企业的固定版本运营。
Harness 的 project default 都不放入两者，需要的组织通过 managed settings 或端点管理设置。

缺少依赖关系时，首先确认 Claude Code 的 `/plugin` Errors、`/doctor`、`claude plugin list --json`。
如果是因未注册 marketplace，通过 `/plugin marketplace add` 或 `claude plugin marketplace add` 注册，交给本体的 auto-resolve。

