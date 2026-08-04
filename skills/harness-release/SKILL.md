---
name: harness-release
description: "Generic release automation for projects using Keep a Changelog + GitHub. Single confirmation gate then end-to-end automation: bump detection, CHANGELOG promotion, PR/main merge, tag, GitHub Release. Trigger: release, version bump, publish. Do NOT load for: implementation, review, planning, setup."
description-en: "Generic release automation for projects using Keep a Changelog + GitHub. Single confirmation gate then end-to-end automation: bump detection, CHANGELOG promotion, PR/main merge, tag, GitHub Release. Trigger: release, version bump, publish. Do NOT load for: implementation, review, planning, setup."
description-zh: "通用发布自动化技能。适用于使用 Keep a Changelog 和 GitHub 的任何项目。通过单一确认门控，自动完成 bump 判定、CHANGELOG 提升、PR/main 合并、标签和 GitHub Release 全流程。当用户提到发布、版本升级、创建标签、公开时启动。不适用于：实现、代码审查、计划、设置。"
kind: workflow
purpose: "Release projects through changelog, version, PR/main merge, tag, and GitHub Release gates"
trigger: "release, version bump, publish"
shape: workflow
role: orchestrator
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Bash", "AskUserQuestion", "Skill"]
argument-hint: "[patch|minor|major|--dry-run]"
context: fork
effort: high
user-invocable: true
---

# Harness Release（通用）

面向使用 Keep a Changelog + GitHub 的**所有项目**的通用发布自动化技能。

**设计原则**：单一确认门控。用户只需查看整体计划并批准一次。批准后，将无中断地执行文件重写 → commit → branch push → PR 创建/更新 → 合并到 default branch → 在 default branch 上打标签 → GitHub Release。

**Release complete 的定义**：release 仅"创建标签和 GitHub Release"并未完成。目标工作与 release bump 已合并到 default branch（通常为 `main`），release tag 指向 default branch 可达的 commit，且 GitHub Release 已公开该标签，此状态方为完成。

## PR ready vs release ready

在 Harness V2 中，不要混淆 PR closeout 与 release closeout。

| Gate | 含义 | 必要条件 | 停止 lane |
|------|------|----------|-----------|
| **PR ready** | 分支可进行 review 且可判断是否可合并 | `harness-review` `APPROVE`、focused tests PASS、evidence pack 完备（accepted/rejected findings、tests、release-preflight warnings 处理、residual risk） | `[lane:fast]` / `[lane:gate]` 可在此停止 |
| **release ready** | 公开发布路径通过 preflight | PR ready 条件 + version surface sync + tag + GitHub Release + CI/public artifact 验证 | 仅 `[lane:release]` |

- PR ready 通过 `harness-review` APPROVE + evidence pack 判定。不从 `harness-review` 进行 push / PR / merge。
- release ready 仅由 `harness-release` 的 Preflight / Post-Gate 判定。version bump / tag / GitHub Release 专用于 release lane。
- 仅 local tests passed 既非 PR ready 也非 release ready（`not_observed != absent`）。

> **Literal invocation note**：本技能的入口直接使用 `harness-release`、`/release`、`/release patch`、`/release --dry-run` 等字面命令。

## 与 CC runtime hard floor 的关系

Claude Code 2.1.183+ 的 runtime hard floor 在结构上拒绝 GitHub CLI release publish 系命令（Anthropic 产品规格，无法通过 `settings.json` 的 `permissions.ask` 覆盖）。本技能不执行 publish 本身，而是委托给 `.github/workflows/release.yml`（tag push trigger）。技能的责任在 tag push 时完成，之后通过 `scripts/release-verify-publish.sh` 验证 workflow 的发布。

**Revert 条件**：当 CC 在 runtime hard floor 上提供用户明确批准路径时，考虑将直接 publish step 返回 Post-Gate。

## Bare invocation contract

if $ARGUMENTS == "":
  → 解读为「提交迄今为止的工作，完成 PR/main 反映后进行发布」，执行 Review Gate 检测
  → 仅在目标工作可确定为单个的情况下自动进入 Step 0 (Review Gate)
  → 目标不明或无 review state 时，通过 AskUserQuestion 提供选项后再进行

无参数调用时，首次响应必须输出以下字面标记：

`RELEASE_AUTOSTART: target=<work-summary>, base_ref=<ref>, mode=<patch|minor|major|auto>`

禁止以下行为：「任务不明确」「等待指示」「无任务」「等待额外指示」。

<!-- 上述块遵循 AUTO-START CONTRACT。skill-editing.md「前 3 行内」规则。patterns.md P27 解决方案三点集（机器可读条件 + 禁止行为字面量 + AUTOSTART marker） -->

### Output Contract (P35: 「看似停止」的 UX 对策)

技能结论时的 output **最后一行**必须包含以下字面量：

`↑此结果将由 Claude 汇总。按 Enter 键继续，或通过新 prompt 给出其他指示。`

这是针对通过 `<local-command-stdout>` 作为文本响应显示时用户感觉「停止」的 UX 问题的明确指示（patterns.md P35）。

当仅输入 `harness-release` / `/release` 时，将其视为
**「提交迄今为止的工作，完成 PR/main 反映后进行发布」**。
旧表达 **「提交迄今为止的工作后发布」** 意图相同，但完成条件必须包含 PR/main 反映。
不应因「无任务」「等待指示」而停止。

在 bare release 中，通常的 release preflight 之前执行 **Review Gate** 和 **Work Commit Gate**。

Review Gate 面向 **release ready**。对于仅需 `[lane:fast]` / `[lane:gate]` 的 PR ready 的工作，除非用户明确要求 release，否则不启动 `harness-release`。

1. 检查 `git status --porcelain` 和 `git log @{upstream}..HEAD` / `main..HEAD`，确定「迄今为止的工作」目标
2. 检查 `.claude/state/review-result.json` 和 `.claude/state/review-approved.json`，确认目标工作是否有 `APPROVE` 的 review 和 evidence pack
3. 若无 APPROVE review，则通过 `AskUserQuestion` 确认
4. 用户选择「从 review 开始」时，启动 `harness-review`，在 `APPROVE` 之前不进入 release
5. 若 `harness-review` 返回 `REQUEST_CHANGES`，则保留 release，通过 `harness-work` 修正后重新执行 `harness-review`。循环至 `APPROVE`
6. `harness-review` 返回 `APPROVE` 后，创建 working tree 的工作 commit
7. working tree clean 后进入通常的 release preflight / confirmation gate / PR merge / tag / GitHub Release

### Review Gate AskUserQuestion

执行 `harness-release` 时若无法确认 review approval，不推测进行 release。
输出以下 Ask。

```text
question: "harness-release 将提交迄今为止的工作并发布，但未找到此工作的 APPROVE review。如何继续？"
options:
  - label: "从 review 开始（推荐）"
    description: "执行 harness-review，仅在 APPROVE 后进入 commit/release。"
  - label: "release dry-run"
    description: "不重写文件，仅确认 release 计划和缺失的 gate。"
  - label: "中止"
    description: "不进行 review 和 release，停止。"
```

用户选择「从 review 开始」时，在同一会话内从 `harness-review` 开始。
`harness-review` 的目标判定遵循 `harness-review` 侧的 bare review contract。
若 review 为 `APPROVE`，直接返回 `harness-release` 的 Work Commit Gate。
若 review 为 `REQUEST_CHANGES`，则保留 release，通过 `harness-work` 修正后重新执行 `harness-review`。
此修正后重新 review loop 持续至 `APPROVE`。

仅在以下情况可返回给用户。

1. 修正需要规格正本 / Plans.md / API / permission / migration / billing 等决策，需要 `AskUserQuestion`
2. 修正方针有多个，采用不同方针会影响用户价值或兼容性
3. 用户在 Ask 中选择 `release dry-run` 或 `中止`

不应仅将 `REQUEST_CHANGES` 作为最终停止理由。

### Work Commit Gate

在 bare release 中，若 working tree 有未提交变更，在 release version bump commit 之前，
先创建已 review 的工作 commit。

```bash
git status --short
git diff --stat
git add <reviewed files>
git commit -m "<type>: <summary>"
```

commit message 从 review summary / Plans.md task / branch name 简短生成。
无法判断时通过 `AskUserQuestion` 提供 2-3 个 commit message 候选。
创建 work commit 后，确认或更新 `.claude/state/review-result.json` 的 `commit_hash`，
进入 release preflight。

进入通常的 release preflight 后，与之前一样将 working tree dirty 视为失败。
不保持 dirty tree 进入 version bump / tag / GitHub Release。

## Quick Reference

```bash
/release              # 将迄今为止的工作通过 review gate → commit → PR/main merge → release
/release patch        # 明确指定 bump 为 patch
/release minor        # 明确指定 bump 为 minor
/release major        # 明确指定 bump 为 major
/release --dry-run    # 仅显示计划，不执行
```

## 前提条件

使用本技能的项目需满足以下条件：

1. `CHANGELOG.md` 为 [Keep a Changelog](https://keepachangelog.com/) 格式
2. 存在 `[Unreleased]` 章节
3. 拥有以下任一 version file：
   - `VERSION`（独立文件）
   - `package.json`（npm）
   - `pyproject.toml`（Python，`[project]` 或 `[tool.poetry]`）
   - `Cargo.toml`（Rust，`[package]`）
4. 已安装并认证 `gh` CLI
5. git 远程 `origin` 指向 GitHub
6. 若为 Claude Code plugin 项目，`claude` CLI 需支持 `plugin tag`

若不满足这些条件，Preflight 将检测并中止。

虽然通过 `prUrlTemplate` 的 multi-host review URL 作为将来候补被识别，
但本技能的 release automation 仍以 `gh` CLI 和 GitHub remote 为主要路径。
因为 owner / branch / release asset / CI metadata 的自动获取因 host 而异，Phase 56.2.3 中仅保留为 docs-only。

## 单一门控流程

Bare release（0. Review Gate → 0.5 Work Commit Gate）→
Pre-Gate（1. Preflight → 2. Version file 检测 → 3. 版本读取 → 4. plugin tag preflight → 5. bump 推定 → 6. 新版本计算 → 7. CHANGELOG 草案 → 8. Release notes 草案）→
**单一确认门控**（参见下述「Confirmation Gate」，`yes` / `<修正指示>` / `cancel` 三选一）→
Post-Gate（9. Version file 重写 → 10. CHANGELOG 提升 → 11. commit → 12. branch push → 13. PR 创建/更新 → 14. default branch merge → 15. 可达性确认 → 16. plugin tag → 17. semver tag → 18. tag push → 19. workflow publish verify → 20. 完成报告）
共 3 个阶段。各阶段详情参见「Pre-Gate 详情」「Confirmation Gate」「Post-Gate 详情」。

## Pre-Gate 详情

### 1. Preflight

release ready gate：除 PR ready 条件外，确认 version / tag / GitHub Release / CI artifact 路径。

```bash
# 必要工具
command -v gh >/dev/null || { echo "无 gh CLI"; exit 1; }
command -v python3 >/dev/null || { echo "需要 python3"; exit 1; }

# working tree
if [ -n "$(git status --porcelain)" ]; then
  echo "working tree 有未提交变更"; exit 1;
fi

# CHANGELOG
[ -f CHANGELOG.md ] || { echo "无 CHANGELOG.md"; exit 1; }
grep -q "^## \[Unreleased\]" CHANGELOG.md || { echo "无 [Unreleased] 章节"; exit 1; }

# plugin/mirror 项目
scripts/release-preflight.sh
```

此 working tree clean check 通常是 release preflight 的门控。
在 bare release 中要提交「迄今为止的工作」时，需在此 check 之前完成 Review Gate 和 Work Commit Gate。
不应仅通过此 check 将未 review 的 dirty tree 中止结束。

`scripts/release-preflight.sh` 在 tag 创建前也检测 `opencode/`、`skills-codex/`、`codex/.codex/skills/` 的 mirror drift。若 `node scripts/build-opencode.js` 生成差分，则停止 release，提交该差分后再进入 tag。

release preflight 对所有 dist host 以 `REQUIRED=1`（fail-closed）执行 host workflow smoke。任一 host FAIL 则停止 release。这是 multi-host bar H7（release-preflight consumes host gates fail-closed）的充分配线。参见 `scripts/release-preflight-host-smoke.sh`。fail-closed 的正本是 operator 机器的 preflight，在 GitHub runner（`GITHUB_ACTIONS=true`）中，对于未 provision CLI 的 host，明确以 SKIP 行跳过（避免 tag-triggered workflow 重新运行阻塞整个 release。v5.3.0 run 29679591686 的 regression 对应）。

### 2. Version File 自动检测

以 `VERSION` → `package.json` → `pyproject.toml`（`[project]` / `[tool.poetry]`）→ `Cargo.toml` 的优先级搜索，将首个找到的作为正本。
检测片段・读取逻辑详情：[version-files.md](${CLAUDE_SKILL_DIR}/references/version-files.md)

### 3. Claude Plugin Tag Preflight

在存在 `.claude-plugin/plugin.json` 的项目中，除通常的 GitHub Release tag 外，也创建 Claude plugin release tag。

简而言之，在手动组装 `git tag -a` 之前，先通过 Claude Code 本体的 plugin validation，然后创建 `{plugin-name}--v{version}` tag。

Pre-Gate 中不重写文件，确认以下内容。
version sync 不通过 `grep` / `sed` 获取，JSON 使用结构化解析器读取：

```bash
command -v claude >/dev/null || { echo "无 claude CLI"; exit 1; }
claude plugin validate .claude-plugin/plugin.json

HARNESS_PLUGIN_ROOT="${CLAUDE_PLUGIN_ROOT:-.}"
python3 "${HARNESS_PLUGIN_ROOT}/scripts/check-release-version-sync.py" --root .

claude plugin tag .claude-plugin --dry-run
```

`${HARNESS_PLUGIN_ROOT}/scripts/check-release-version-sync.py` 读取所有存在的 release surface，canonical 按 `VERSION > package.json > .claude-plugin/plugin.json > .codex-plugin/plugin.json` 顺序决定。
在此基础上，若有任何以下不一致・缺失，则不进入 tag / release：

- `VERSION`
- `package.json` 的 `.version`
- `.claude-plugin/plugin.json` 的 `.version`
- `.codex-plugin/plugin.json` 的 `.version`
- `.claude-plugin/marketplace.json` 的 `.metadata.version`
- `.claude-plugin/marketplace.json` 的 `.plugins[].version`（数组内各 plugin entry）

不一致时，显示哪个 surface 与 canonical 不同，或哪个 field 缺失 / invalid。
机器处理或 CI 读取时使用 `--json`：

```bash
python3 "${HARNESS_PLUGIN_ROOT}/scripts/check-release-version-sync.py" --root . --json
```

此 check 旨在防止 3 类事故：

- `VERSION` 与 `.claude-plugin/plugin.json` 的 version 不一致状态下打 tag 的事故
- `package.json` / marketplace entry 的 version 陈旧状态下进入 release workflow 的事故
- 未通过 plugin manifest / marketplace entry 的 validation，之后在 plugin install / update 侧卡住的事故

在 `--dry-run` 中，`claude plugin tag` 将显示实际创建的 tag 名和内部的 `git tag -a` / push 相当命令。
将此处看到的命令包含在 Confirmation Gate 的 plan 中。

### 4. Bump 自动推定

解析 `[Unreleased]` 直接下的标题（`### Breaking Changes`/`### Removed` → major、`### Added` → minor、`### Fixed`/`### Changed`/`### Security` 仅 → patch、空章节 → error）决定 bump level。
用户通过 `/release patch|minor|major` 明确指定时优先。
详情：[bump-detection.md](${CLAUDE_SKILL_DIR}/references/bump-detection.md)

### 5. CHANGELOG 草案创建（内存中）

切出 `[Unreleased]` 的内容，组装 `[<new>] - YYYY-MM-DD` 章节和 compare link（暂不写入）。
详情：[release-notes.md](${CLAUDE_SKILL_DIR}/references/release-notes.md#changelog-草案创建内存中pre-gate-步骤-7)

### 6. Release Notes 草案创建（内存中）

基于 `## [<new>]` 章节内容，生成 GitHub Release 用的 markdown（What's Changed / Before-After / Added-Changed-Fixed / 页脚）。
必需要素・生成方法・验证 check 详情：[release-notes.md](${CLAUDE_SKILL_DIR}/references/release-notes.md)

## Confirmation Gate

所有草案齐备后，仅向用户展示一次：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Release Plan: v<old> → v<new> (<bump>)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 Version file: <detected file>
 Bump reason:  <why this level was chosen>

 CHANGELOG changes:
   在 [Unreleased] 中检测到 <N> 项变更
   作为 [<new>] - YYYY-MM-DD 确定
   添加 Compare link

 GitHub Release notes preview:
   <前 10 行>
   ...

 Files to modify:
   - <version file>
   - CHANGELOG.md

 Final actions:
   - git commit -m "chore: release v<new>"
   - git push origin <release-branch>
   - gh pr create/update + gh pr merge into <default-branch>
   - git fetch origin <default-branch> && git checkout <default-branch>
   - claude plugin tag .claude-plugin --push --remote origin  # plugin 项目的情况。在 default branch 上执行
   - git tag -a v<new>                                        # 需要 GitHub Release 用 semver tag 的情况。在 default branch 上创建
   - git push origin <default-branch> --tags
   - (tag push 后 GitHub Actions release workflow 自动公开 release)

Proceed? [yes / cancel / <修正指示>]
```

## Post-Gate 详情

批准后无中断执行。失败时的方针：

| 失败位置 | 恢复 |
|---------|------|
| 文件重写失败 | 此处中止，local 保持 dirty 状态由人类判断 |
| commit 失败 | hook 拒绝等。向用户提示原因并促使其修正 |
| PR 创建/merge 失败 | 停止并将 release 视为未完成。不进入 tag / GitHub Release |
| plugin tag validation 失败 | 修正 `VERSION` / `.claude-plugin/plugin.json` / marketplace entry 的不一致，不进入 tag 创建 |
| push 失败 | 远程侧问题。保留 local commit/tag |

### PR / Main Merge Gate、plugin tag、Verify Publish

Post-Gate 的 release commit 后，创建 tag 之前将 GitHub PR merge 到 default branch（`gh pr create` → `gh pr merge --merge` → default branch fetch/checkout 确认 release commit 的可达性）。不应创建指向仅存在于 release branch 的 commit 的 tag 进行 GitHub Release。
在存在 `.claude-plugin/plugin.json` 的项目中，merge 后在 default branch 上重新确认 version sync，然后通过 `claude plugin tag .claude-plugin --push --remote origin` 创建 plugin tag（`{plugin-name}--v{version}` 形式）。
tag push 后通过 `bash scripts/release-verify-publish.sh` 验证 `.github/workflows/release.yml` 的发布结果（5 秒间隔 × 60 次 polling，exit 0=PASS / 2=WARN(timeout) / 3=ERROR）。
命令全文・失败时的判断基准参见 [post-gate-detail.md](${CLAUDE_SKILL_DIR}/references/post-gate-detail.md)。

## `--dry-run` 模式

执行 Pre-Gate 全部，显示直至 Confirmation Gate 的内容，但**在门控处停止，不进入 Post-Gate**。

Claude plugin 项目的情况，dry-run 中也执行 `python3 "${HARNESS_PLUGIN_ROOT}/scripts/check-release-version-sync.py" --root .` 和 `claude plugin tag .claude-plugin --dry-run`，显示实际创建的 plugin tag 名和 push 对象。此时若 `VERSION` / `package.json` / `.claude-plugin/plugin.json` / `.codex-plugin/plugin.json` / `.claude-plugin/marketplace.json` 的 version surface 不一致或缺失，则在 dry-run 阶段停止。

## 环境变量

用于各项目调整：

| 变量 | 说明 |
|------|------|
| `HARNESS_RELEASE_PROJECT_ROOT` | 仓库根目录（默认：`$(pwd)`） |
| `HARNESS_RELEASE_BRANCH` | push 对象分支（默认：当前分支） |
| `HARNESS_RELEASE_DEFAULT_BRANCH` | PR merge 目标 default branch（默认：`main`） |
| `HARNESS_RELEASE_HEALTHCHECK_CMD` | Preflight 中额外执行的命令 |
| `HARNESS_RELEASE_SKIP_GH` | 为 `1` 时跳过 GitHub Release 创建 |

## CHANGELOG 书写规则

`[Unreleased]` 章节需要持有 KaCL 标准子章节（`### Added`=minor / `### Changed`・`### Fixed`・`### Security`=patch / `### Deprecated`=minor / `### Removed`・`### Breaking Changes`=major）之一。
本技能机械解析这些标题，无法识别表述差异（`### Fix` / `### Bug Fixes` 等）。

GitHub Release notes 的必要格式・CHANGELOG 的「过去/未来」记法・merge 方式（不采用 squash）的详情参见
[github-release.md](${CLAUDE_SKILL_DIR}/references/github-release.md)。
SemVer 判定基准・批量发布方针・Release Train Proposal 的详情参见
[versioning.md](${CLAUDE_SKILL_DIR}/references/versioning.md)。

## 出货前的验收判断（非工程师面向）

在 release 确定之前提议 `harness-accept`。将各合格条件是否满足与 ship/wait/reject 的
推荐汇总到一张 HTML 的「验收判断」画面，订购者无需专业知识即可判断出货可否。

## 相关技能

- `harness-release-internal` - 本体 claude-code-harness 发布时额外执行的 harness 固有 preflight/finalization（非分发对象）
- `harness-plan` - Plans.md 管理
- `harness-review` - 发布前的代码审查
- `harness-accept` - 验收判断 HTML（非工程师面向，发布前提议）

## 设计思想

- **PR ready / release ready 分离**：PR ready 是 review + evidence pack。release ready 是到 version/tag/GitHub Release/CI。lane:fast / lane:gate 可在 PR ready 停止
- **单一门控**：用户判断时机仅一次。夹入 mini-confirmation 会流于形式化失去意义
- **事先描绘全部**：禁止进入 Post-Gate 后的「重新思考」。Gate 前齐备所有草案
- **main 反映为完成条件**：release tag / GitHub Release 仅在 default branch 反映后创建。branch-only release 视为未完成
- **失败保持透明**：中途失败时不尝试自动回滚，向用户提示现状由其判断
- **不依赖项目**：VERSION file 格式、mirror、residue check 等不预设特定环境。本体 harness 固有处理分离到 `harness-release-internal`
