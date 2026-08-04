# 版本管理规则

Harness 的版本管理标准。遵循 SemVer（Semantic Versioning）。

## 版本判定标准

| 变更种类 | 版本 | 示例 |
|-----------|----------|-----|
| 技能定义（SKILL.md）的文案修正/追记 | **patch** (x.y.Z) | 模板微修正、说明文改善 |
| 文档/规则文件的更新 | **patch** (x.y.Z) | CHANGELOG 重写、rules/ 添加 |
| hooks/scripts 的 bug 修正 | **patch** (x.y.Z) | task-completed.sh 的转义修正 |
| 向现有技能添加新 flag/子命令 | **minor** (x.Y.0) | `--snapshot`、`--auto-mode` |
| 添加新技能/agent/hooks | **minor** (x.Y.0) | 新技能 `harness-foo` |
| TypeScript guardrail 引擎的变更 | **minor** (x.Y.0) | 添加新规则、变更现有规则 |
| Claude Code 新版本兼容对应 | **minor** (x.Y.0) | CC v2.1.72 对应 |
| 破坏性变更（废除旧技能、格式不兼容） | **major** (X.0.0) | 删除 Plans.md v1 支持 |

## 判断流程图

```
现有行为会破坏吗？
├─ Yes → major
└─ No → 用户能做新的事情吗？
    ├─ Yes → minor
    └─ No → patch
```

## 批量发布的推荐

- **同一天完成多个 Phase 时**: 汇总为 1 个 minor 发布
- **Phase 完成 + 文档修正**: Phase 部分为 minor，文档修正包含在内（不作为单独发布）
- **CC 兼容对应 + 功能添加**: 可以汇总为 1 个 minor

### 坏的示例

```
v3.6.0 (03/08 AM) — Phase 25
v3.7.0 (03/08 PM) — Phase 26    ← 同一天 2 个 minor 应避免
v3.7.1 (03/09)    — Auto Mode
```

### 好的示例

```
v3.6.0 (03/08) — Phase 25 + Phase 26    ← 汇总为 1 个 minor
v3.6.1 (03/09) — Auto Mode 准备         ← prep 是 patch
```

## 发布前检查

1. **列出从上次发布以来的变更**
2. **对照判定标准决定版本种类**
3. **同一天的多个变更考虑批量化**
4. **确认 version 面的同步** — 正本是 `./scripts/sync-version.sh`（2026-07-16 当前: VERSION / .claude-plugin/plugin.json / .codex-plugin/plugin.json / .cursor-plugin/plugin.json / .grok-plugin/plugin.json / marketplace.json×2 / harness.toml 的 7 个字符串 6 个文件 + CHANGELOG compare link。对象增加时更新 script 一侧，这行不重新计算数量）
5. **确认 git tag 无缺番连续**

## 禁止事项

- 删除・回滚标签（已发布版本不变）
- 同一天 2 次以上的 minor bump
- patch 级别变更的 minor bump

## Release Train Proposal

发布不是"每个 commit / PR"，而是在 `CHANGELOG.md` 的 `[Unreleased]` 中积累变更，
满足标准时**提议候选**，只有人类说 GO 时才发布（避免细粒度发布）。

- 积累层只触 `[Unreleased]`，不 bump VERSION / plugin.json / harness.toml。
- 提议器 `harness-release --check` 是 read-only。触发发火时只显示 `RELEASE_CANDIDATE`
  （带推定 bump），完全不重写 version 面。
- v1 触发（首先从 1 个规则开始）: 从最终 tag 经过 **7 天** OR `### Breaking` 存在于
  `[Unreleased]`。有 `### Security` 时缩短为 **2 天**。N 件计数等
  多阈值矩阵，在运行中 cadence 成为问题前不添加。
- 标题对照是 `### Breaking` 的 **prefix 匹配**（`skills/harness-release/references/bump-detection.md`
  的正记法 `### Breaking Changes` 也作为同一触发处理）。实现正本是
  `go/internal/releasetrain`（`harness release --check`）。对象 tag 仅限 `v[0-9]` 开头的
  semver tag（`claude-code-harness--v*` 的 plugin tag 不在对象内）。
- 这不是 gate 而是**提议**。忽略无成本，在下一个阈值再提议。Session Monitor
  以 tri-state（Candidate / None / NotApplicable）被动显示，
  遵循 `active-watching-test-policy.md` 的 3 状态命名（无候选是 silent）。
- 人类说 GO 后现有的 `harness-release` 照常运行（bump 检测 → sync-version.sh 的全 version 面同步 →
  CHANGELOG promote → PR → main → tag → GitHub Release）。批量化将 version 面同步
  集约为 1 发布 1 次，从结构上防止"同一天 2 minor"违反。

## Plan B Stage B Release Trigger

Plan B 工程表的 **stage b 完成**是 minor 发布候选。

- 达成条件: Phase 92.x（base 卫生 + 运行时 floor + 集约硬化 + Producer 层 + Mode 2 live-messaging）+
  Phase 93.x（/breezing MVP + 契约修正轮）+ Phase 95.x（Bridge Daemon + Decision Card 正式版 + mem 读出层）+
  Phase 96.1.1-96.1.4（Risk Gate Export + 3 CLI hook parity + auto-commit opt-in + deny baseline hardening）全部
  `cc:done`，且 93.3.6 / 95.5.1 / 96.1.5 的验证章节带 evidence 完跑判定。
- 发布判定: stage b 完成 = minor 发布候选的信号。实际 GO 判断与 `harness-release` 提议器的通常流程整合。
- 与 Release Train v1 trigger（7 天经过 / Breaking / Security）的关系: stage b 完成是**追加的候选信号**，
  不覆盖现有 v1 trigger。stage b 完成 + 现有 trigger 同时成立时汇总为 1 minor batch。
- Phase 94 (Release Train Proposal 实现) 的本体实现在此 trigger 之上 — stage b 是判断材料，Phase 94 是判定机制。
