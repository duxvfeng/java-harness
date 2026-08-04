# GitHub Release Notes Rules

Formatting rules applied when creating GitHub Release notes.

## Required Format

### Structure

```markdown
## What's Changed

**One-line description of the change's value**

### Before / After

| Before | After |
|--------|-------|
| Previous state | New state |
| ... | ... |

---

## Added

- **Feature name**: Description
  - Detail 1
  - Detail 2

## Changed

- **Change**: Description

## Fixed

- **Fix**: Description

## Requirements (if applicable)

- **Claude Code vX.X.X+** (recommended)
- Link: [Documentation](URL)

---

Generated with [Claude Code](https://claude.com/claude-code)
```

### Required Elements

| Element | Required | Description |
|---------|----------|-------------|
| `## What's Changed` | Yes | Section heading |
| **Bold summary** | Yes | One-line value description |
| `Before / After` table | Yes | User-facing changes |
| `Added/Changed/Fixed` | When applicable | Detailed changes |
| Footer | Yes | `Generated with [Claude Code](...)` |

### Language

- **GitHub Release**: English required（公开仓库用）
- **CHANGELOG.md**: **中文**采用详细的 Before/After 格式（后述）
- Keep descriptions user-focused

## CHANGELOG 格式（中文・详细 Before/After）

CHANGELOG 将各功能以"迄今为止 → 今后"格式具体记述:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### 主题: [用一句话概括整体变更]

**[用1~2句话描述对用户的价值]**

---

#### 1. [功能名称]

**迄今为止**: [旧行为。具体描述用户经历的不便之处]

**今后**: [新行为。解决了什么 + 具体示例]

```输出示例或命令示例```

#### 2. [下一个功能名称]

**迄今为止**: ...
**今后**: ...
```

**书写规则**:
- 各功能用 `#### N. 功能名` 设为独立章节
- "迄今为止"是**问题描述**（"〜需要〜"的形式）
- "今后"是**解决的具体形象**（包含命令示例/输出示例）
- 长也可以。可读性最优先
- 技术细节（文件名、步骤编号）在"今后"的补充中保持最少

## Prohibited

- No skipping the Before / After (CHANGELOG) or Before / After table (GitHub Release)
- No skipping the footer (GitHub Release)
- No technical-only descriptions (user perspective required)
- No bare change lists without value explanation

## Good Example (GitHub Release — English)

```markdown
## What's Changed

**`/work --full` now automates implement -> self-review -> improve -> commit in parallel**

### Before / After

| Before | After |
|--------|-------|
| `/work` executes tasks one at a time | `/work --full --parallel 3` runs in parallel |
| Reviews required separate manual step | Each task-worker self-reviews autonomously |
```

## Good Example (CHANGELOG — Japanese)

```markdown
#### 1. 失败任务的自动重新生成票据

**迄今为止**: 测试/CI 失败时只会重试 3 次然后停止。
停止后需要自己调查"原因是什么"，手动将修正任务添加到 Plans.md。

**今后**: 3 次失败停止时，Harness 会分类失败原因，自动生成修正任务方案。
批准后作为 `.fix` 任务自动添加到 Plans.md。
```

## Bad Example

```markdown
## What's New

### Added
- Added task-worker.md
- Added --full option
```

-> Doesn't communicate user value

## 合并方式（固定 merge commit / 不采用 squash）

release PR 以及合并到 main 的 PR 使用 **merge commit** (`gh pr merge --merge` /
`git merge --no-ff`)。不采用 squash / rebase（Phase 114 preamble 裁定 2026-07-14）。

**原因**: Plans.md 在 task 的 Status 列中埋入 commit hash 作为台账
（例: 113.1 `cc:done [fa2b9c37]`）。squash 会将这些 hash 从 main 的 ancestry 中
移除，破坏台账与历史的核对（`scripts/ci/check-branch-alignment-ledger.sh`、AR-16）。
squash 技术上可行（3 种方式允许 + binary 用 `-buildvcs=false` 实现
SHA 非依赖），但只要 Plans.md 存在 hash 台账方式就不采用。

- 机器 gate: merge 前确认 `bash scripts/ci/check-branch-alignment-ledger.sh` exit 0
- 先例: v5.0.0 的 #235 / #236 也是 merge commit
- 重新审视条件: 只有 Plans.md 停止 hash 台账方式时再探讨

## Release evidence 的保存（v5.1.0 审计指出的 codify）

upgrade smoke（旧版本 → 新版本的实测）和 release gate 的执行日志，
不仅是声明，还要作为 artifact **保存到 `.claude/state/release-evidence/<version>/`**。
Plans.md 的 cc:done 标记声称"实测了"的项目，必须有对应的日志文件或
命令输出的记录（SA-13 completeness 审计 2026-07-16 的指出）。

## Release Creation Command

```bash
gh release create vX.X.X \
  --title "vX.X.X - Title" \
  --notes "$(cat <<'EOF'
## What's Changed
...
EOF
)"
```

## Editing Past Releases

```bash
gh release edit vX.X.X --notes "$(cat <<'EOF'
...
EOF
)"
```

## CC 版本集成时的 CHANGELOG 模式

包含 Claude Code 新版本集成的 release，不使用通常的"迄今为止 / 今后"格式，
而是使用**"CC 的更新 → Harness 中的应用"**格式。
从上游（CC）的变更理由开始说明，让读者从语境中理解"为什么这个变更与自己有关"。

### 判定条件

符合以下任一情况时，应用此模式:

- Feature Table 的版本表记已更新
- hooks.json 中添加了 CC 来源的新事件
- skills 中追记了 CC 新功能的应用指南

### 结构

```markdown
#### N. Claude Code X.Y.Z 集成

（1 行概括整体）

##### N-1. 功能名称

**CC 的更新**: Claude Code 中有什么变化。从用户视角说明，让用户理解该功能的用途。

**Harness 中的应用**: Harness 如何利用该变更。包含具体的机制（脚本名、流程）。

##### N-2. 下一个功能名称

**CC 的更新**: ...
**Harness 中的应用**: ...
```

### 书写规则

- 各功能用 `##### N-X.` 设为独立章节
- "CC 的更新"不写文件变更，而是写**用户体验的变化**
- "Harness 中的应用"写**具体机制**（什么会运行、什么会被防止）
- 避免文件名罗列。不写"更新了 hooks.json"，而是写"防止 Worker 的冻结"
- 仅文档的变更（Feature Table 更新、添加详细章节）不作为单独条目，包含在开头的概述 1 行中

### Good Example

```markdown
##### 5-1. 对 MCP Elicitation 的自动应对

**CC 的更新**: MCP 服务器可以在任务执行期间向用户"提问"（Elicitation）。
例如会被要求表单输入，比如"要 push 到哪个仓库？"。

**Harness 中的应用**: Breezing 的 Worker 是后台执行，无法回答提问表单。
放置不管会导致 Worker 冻结。新创建 elicitation-handler.sh，
实现 Breezing 会话中自动跳过、通常会话中原样通过让用户回答的机制。
```

### Bad Example

```markdown
#### CC 2.1.76 集成

- 向 hooks.json 添加 Elicitation
- 创建 elicitation-handler.sh
- 更新 CLAUDE.md
```

→ 文件变更的罗列，没有传达为什么需要该变更，对用户来说有什么变化

## Reference

- Good examples: v2.8.0, v2.8.2, v2.9.1, v3.10.3 (CC集成模式)
- Keep consistent with CHANGELOG
