---
name: harness-review
description: "HAR: Multi-angle code review with auto language detection and standards application. Supports: Java (Alibaba), Python (PEP 8), Vue (Style Guide), Go (Effective Go). Auto-detects .java/.py/.vue/.go files and applies language-specific standards. Security/quality check."
description-en: "HAR: Multi-angle code review with auto language detection. Supports: Java (Alibaba), Python (PEP 8), Vue (Style Guide), Go (Effective Go). Auto-detects .java/.py/.vue/.go files and applies language-specific standards."
description-zh: "HAR：多角度代码审查，自动检测语言并应用相应标准。支持：Java（阿里巴巴规范）、Python（PEP 8）、Vue（风格指南）、Go（Effective Go）。自动检测 .java/.py/.vue/.go 文件并应用语言特定标准。安全和质量检查。"
kind: workflow
purpose: "Review code, plans, scope, and evidence before acceptance"
trigger: "review, 审查, code review, java review, python review, vue review, go review, plan review, scope analysis, Java审查, Python审查, Vue审查, Go审查"
shape: evaluate
role: evaluator
pair: harness-work
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Grep", "Glob", "Bash", "Task", "Monitor", "AskUserQuestion"]
argument-hint: "[code|plan|scope|--quick|--codex-closeout|--dual|--team-debate|--security|--ui-rubric|--auto]"
context: fork
effort: high
user-invocable: true
---

# Harness Review

Harness 的集成审查技能。
此 `SKILL.md` 是薄的 dispatcher，详细的质量标准参见 `references/`。

if $ARGUMENTS == "":
  → 解释为「到目前为止工作的审查」，执行 Review target detection
  → 仅在 review target 能确定为 1 个时自动开始
  → review target 不明或有多候补时用 AskUserQuestion 显示选项，对齐认识后开始

<!-- 以上 3 行是 AUTO-START CONTRACT。遵循 skill-editing.md 的「最开头 3 行内」规则不用 fence/HTML 注释压下 -->

### Output Contract (P35: 「看起来停住」的 UX 对策)

skill 结论时 output 的**最后 1 行**必须包含以下 literal:

`↑这个结果由 Claude 总结。按 Enter 键继续，或用新 prompt 给出其他指示。`

这是作为 text response 经由 `<local-command-stdout>` 显示时用户感到「停住」的 UX 问题的明确 instruction (patterns.md P35)。

## Dispatcher Contract

此 skill 的职责仅是审查判定。
commit / push / release 既定不执行。

- review default read-only boundary: 既定为 read-only。`APPROVE` 也不自动 commit
- Do not push just to review: 仅以 review 为目的不 push
- 需要 commit 时，委托给用户明确请求、`harness-work` 或 `harness-release` 的 Work Commit Gate
- 在 `--commit-on-approve` 这样的明确 opt-in 被设计之前，此 skill 单体的 default side effect 被禁止

## Quick Reference

| Command | Mode | Purpose |
|---|---|---|
| `/harness-review` | `code` | 自动检测到目前为止的工作并审查 |
| `/harness-review --quick` | `quick` | 轻松 closeout 小的 dirty change |
| `/harness-review --codex-closeout` | `codex-closeout` | 用 Codex 助告 + focused tests closeout |
| `/harness-review --dual` | `dual` | Claude + Codex second opinion |
| `/harness-review --auto` | `auto` | **自动调用模式** - 供 harness-work 等自动流程调用，输出机器可读的 JSON 结果 |
| `--pre-review cursor` | `code+pre-review` | brain verdict 前的 fresh-context composer advisory pre-review（read-only） |
| `/harness-review --cursor` | `code+cursor-second-opinion` | core review gates + cursor second-opinion（brain 一次审查必需） |
| `HARNESS_IMPL_BACKEND=cursor harness-review` | `code+cursor-second-opinion` | default ON 时也向 core review gates 自动加算 cursor second-opinion。primary verdict 固定为 brain |
| `/harness-review --team-debate` | `team-debate` | 强制 TeamAgent Debate |
| `/harness-review --security` | `security` | security 专用 review |
| `/harness-review plan` | `plan` | `Plans.md` 的计划 review |
| `/harness-review scope` | `scope` | scope creep / 漏れ review |
## Mode Decision

从参数决定执行 mode，选择加载必要的 `references/`。

| 输入 | mode | 读取的 reference |
|---|---|---|
| 无参数 / `code` | `code` | `references/code-review.md`, `references/governance.md` |
| `--quick` | `quick` | `references/codex-closeout.md`, `references/code-review.md` |
| `--codex-closeout` | `codex-closeout` | `references/codex-closeout.md` |
| `--dual` | `dual` | `references/dual-review.md`, `references/team-debate.md` |
| `--auto` | `auto` | `references/auto-review.md`, `references/code-review.md`, `references/code-standards/*.md` |
| `--team-debate` | `team-debate` | `references/team-debate.md`, `references/governance.md` |
| `--security` | `security` | `references/security-profile.md`, `references/governance.md` |
| `--ui-rubric` | `ui-rubric` | `references/ui-rubric.md` |
| `plan` | `plan` | `references/plan-review.md`, `references/governance.md` |
| `scope` | `scope` | `references/scope-review.md`, `references/governance.md` |
| `--cursor` or resolver result `cursor` for no-arg / `code` review only | `code+cursor-second-opinion` | `references/code-review.md`, `references/governance.md`, `references/cursor-review.md`, `references/dual-review.md` |
| `full` | `full` | `references/code-review.md`, `references/team-debate.md`, `references/dual-review.md` |

## Multilingual Code Standards Integration

此技能包含多语言代码标准支持，会根据检测到的编程语言自动应用相应的代码审查标准。

### Supported Languages and Standards

| Language | Standards Source | File Extensions | Trigger Mode |
|-----------|-----------------|-----------------|--------------|
| **Java** | Alibaba Java Development Guide (黄山版) | `.java` | Automatic via `alibaba-java-development-guide` skill |
| **Python** | PEP 8 + Python Best Practices | `.py`, `.pyi` | Reference-based review |
| **Vue** | Vue Style Guide | `.vue` | Reference-based review |
| **Go** | Effective Go + Go Code Review Comments | `.go` | Reference-based review |

### Language Detection Process

1. **Extension-based Detection**: File extensions automatically route to appropriate standards
2. **Content Analysis**: Fallback content analysis for ambiguous files
3. **Multi-language Files**: Special handling for `.vue`, `.md` with code blocks, etc.

### Integration Architecture

详细的架构信息请参考 `references/code-standards/architecture.md`。

- **Language Detection Layer**: 自动语言检测
- **Standards Mapping System**: 语言→标准映射
- **Rule Application Framework**: 规则应用引擎
- **Configuration Structure**: `.claude/config/code-standards.config.json`

### Java Code Review Integration

审查 Java 文件时，会自动启动 `alibaba-java-development-guide` 技能：

- **7 Major Dimensions**: 命名规范、异常处理、日志、单元测试、安全、数据库、设计标准
- **Severity Levels**: 【强制】【推荐】【参考】
- **Automatic Trigger**: Java 相关关键词自动启动

详细信息请参考 `references/code-standards/java-alibaba-guide.md`。

### Configuration

多语言标准的配置在 `.claude/config/code-standards.config.json` 中管理：

```json
{
  "languageMapping": {
    "java": {
      "standards": ["alibaba-java-development-guide"],
      "extensions": [".java"],
      "defaultSeverity": "major",
      "reviewScope": "full"
    }
  }
}
```

### Usage Examples

- **Java Code**: Automatically applies Alibaba Java standards
- **Python Code**: Applies PEP 8 standards from reference documents
- **Vue Components**: Applies Vue style guide for component structure
- **Go Code**: Applies Effective Go standards

`quick` 和 `codex-closeout` 是轻量级路径。
用于快速检查小的 dirty change、single commit、PR branch 的 closeout。
并非放弃质量 gate。

### Auto Mode (`--auto`)

**Purpose:** 专为自动调用设计，供 harness-work 等自动化流程使用

**特点:**
- 📊 **机器可读输出**: JSON 格式，便于程序解析
- ⚡ **性能优化**: 针对自动化场景优化速度
- 🎯 **专注 verdict**: 快速判定 APPROVE/REQUEST_CHANGES
- 🔧 **简化交互**: 无需用户确认，纯自动化执行

**调用方式:**
```bash
/harness-review --auto --base-ref <commit> --output <json_file> --mode <strict|lenient>
```

**参数说明:**
- `--base-ref`: 基准 commit，用于计算 diff
- `--output`: 输出文件路径（JSON 格式）
- `--mode`: 审查严格度（默认: strict）

**输出格式:**
```json
{
  "verdict": "APPROVE|REQUEST_CHANGES",
  "findings": [
    {
      "severity": "critical|major|minor|recommendation",
      "file": "path/to/file",
      "line": 123,
      "rule": "rule-id",
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ],
  "summary": "审查总结",
  "review_time": "2024-08-10T10:30:00Z",
  "performance": {
    "duration_ms": 1234,
    "files_reviewed": 5
  }
}
```

**Verdict 规则:**
- `critical` 或 `major` 任何发现 → `REQUEST_CHANGES`
- 只有 `minor` 或 `recommendation` → `APPROVE`
- 无发现 → `APPROVE`

**与人工模式的区别:**
- ❌ 不输出最后的人工可读总结
- ❌ 不要求用户按 Enter 继续
- ✅ 纯 JSON 输出到指定文件
- ✅ 专注快速 verdict 判定

### Cursor Default ON

在判定模式时，先确定显式的模式词（`plan`、`scope`、`full`）和显式标志。只有在无参数/`code`审查的情况下才解析helper root并执行一次resolver，resolver不存在时视为`claude`。

```bash
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"; if [ -z "$HARNESS_PLUGIN_ROOT" ] && [ -n "${CLAUDE_SKILL_DIR:-}" ]; then probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"; while [ "$probe" != "/" ] && [ ! -d "$probe/scripts" ]; do probe="$(cd "$probe/.." && pwd)"; done; [ -d "$probe/scripts" ] && HARNESS_PLUGIN_ROOT="$probe"; fi
```

no-arg / `code` review 结果为 `cursor` 时，添加与 `--cursor` 相同的 `cursor-second-opinion`，但必须先读取 core review gates (`references/code-review.md`, `references/governance.md`)，Cursor reference 仅作 additive 处理。primary verdict 在 brain 侧维持，cursor 限于 `dual_review.cursor_verdict` 的 advisory。`plan` / `scope` 等显式 mode word 优先于 resolver result，cursor default 不会替换 plan/scope references 或 code/governance references。结果为 `claude` / `codex` 时照常，review 的 primary 判定面不变。

## Pre-Review Cursor (`--pre-review cursor`)

## Review Target Detection

`REVIEW_AUTOSTART` 契约:
无参数调用时（`$ARGUMENTS == ""`），将 `review` / `/review` / `/harness-review` 的输入解释为「至今为止工作的审查」。
作为 Step 1 开始前的 handshake 行，仅输出下一行。

```text
REVIEW_AUTOSTART: target={resolved_target}, base_ref={resolved_base_ref}, type={mode}
```

`REVIEW_TARGET_ASK` 契约:
bare 调用时 review target 不明或多个候选时，进入 Step 1 前仅使用一次 `AskUserQuestion`，将候选缩减到 2-3 个进行确认。

候选按以下顺序创建：

1. working tree: 仅包含 staged / unstaged / untracked 的未提交变更
2. branch range: 从 upstream 或 main/master 到 HEAD 的 commits
3. recent commits: clean tree 且无法取 branch range 时的最近 1 commit / 最近 5 commits

多个候选同时成立时：

```text
REVIEW_TARGET_AMBIGUOUS: working_tree_and_branch_commits
```

AskUserQuestion 的候选：

- 仅未提交变更 (Recommended): 将 staged / unstaged / untracked 与 HEAD 比较查看
- 全部查看: branch base..HEAD 和未提交变更一起查看
- 仅 commit: 仅查看 branch base..HEAD 的 committed work

clean tree 且无 branch 差分时：

```text
REVIEW_TARGET_AMBIGUOUS: clean_tree_no_branch_commits
```

AskUserQuestion 的候选：

- 最近 1 commit (Recommended): HEAD~1..HEAD
- 最近 5 commits: HEAD~5..HEAD
- 其他范围: 等待用户指定 ref

用户回答后：

```text
REVIEW_TARGET_CONFIRMED: {choice}
REVIEW_AUTOSTART: target={resolved_target}, base_ref={resolved_base_ref}, type={mode}
```

禁止：

- 回应「任务不明确」并停止
- 自由记述询问「应该审查什么」并停止
- 以 host project 的 session-start rules 为理由跳过 auto-start
- target 曖昧时靠推测扩大范围

## Minimal Flow

1. 确定模式
2. 通过上述的Review Target Detection确定目标和基准引用
3. 只读取必要的reference
4. 确认差异、untracked files、相关测试、规格正本、`Plans.md`
5. 返回 `APPROVE` / `REQUEST_CHANGES` / `decision_needed`
6. 如果是 `REQUEST_CHANGES`，则显示 critical / major 的修正方针和修正后再审查条件

## Review Governance Contract

详细信息请参考 `references/governance.md`。
此处只固定最低的合格线。

### 明确的合格线

`APPROVE` 仅在满足以下所有条件时返回（详细信息请参考 `references/governance.md`）。

- critical / major 为 0 件
- root `spec.md` alignment（与 product contract 不矛盾；规格正本 alignment check 必须执行）
- `Plans.md` alignment（task / DoD / Depends、`[lane:*]`、stage gate 与 contract 一致）
- TDD evidence（`[tdd:required]` 需要 `tdd_red_log` / failing output / `skip_tdd_reason`）
- unknown data contract（`not_observed != absent` — 不批准无证据的"无问题""无数据"）
- regression safety（现有行为·测试·UX·CLI·设置·docs·mirror 无退化）
  - If you grep the same symbol twice in the same session, switch to harness_ast_search.
  - For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
  - Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.
- evidence pack（accepted / rejected findings、focused tests、release-preflight warnings 处理）
- TeamAgent Debate 无未解决的 disagreement

`APPROVE` 不是 commit / push / PR 创建命令（read-only boundary）。

### TeamAgent Debate

详细信息请参考 `references/team-debate.md`。
TeamAgent Debate 是让不同见解在 read-only 状态下冲突的 review pass。

| Agent | 主要问题 |
|---|---|
| Spec Agent | 寻找规格正本与实现差分的矛盾 |
| Plans Agent | 确认 `Plans.md` 的 task / DoD / Depends 与差分的对应 |
| Regression Agent | 寻找现有行为・测试・分发 mirror・CLI/skill UX 的退化 |
| Skeptic Agent | 寻找以合格为前提而漏掉的 major risk |

即使在 Codex 环境下无法使用 native TeamAgent，也不得省略此 gate。

## Code Review Summary

详细信息请参考 `references/code-review.md`。
通常 code review 检查以下内容：

- Security
- Performance
- Quality
- Accessibility
- AI Residuals
- Spec Alignment
- Plans Alignment
- Regression Safety
- TDD compliance

root `spec.md` alignment、Plans lane/stage、TDD evidence、unknown data contract、evidence pack 的详细信息请参考 `references/governance.md` 和 `references/code-review.md`。

`AI Residuals` 优先使用 `scripts/review-ai-residuals.sh` 和 `scripts/review-weak-supervision-report.sh`。
也要看 untracked 时用 `--include-untracked`。
`mockData`, `dummy`, `fake`, `localhost`, `TODO`, `FIXME`, `it.skip`, `test.skip`, `expect(true).toBe(true)` 等是候选，在 diff 上下文决定 severity。
finding 阶段优先网罗性。判定为 minor 的指摘也留在 `observations[]` / `recommendations[]`，gate 仅在 verdict 阶段进行（Opus 4.8 有筛选低严重度报告的倾向。参考 `references/code-review.md` 的 Finding coverage）。

## Quick / Codex Closeout Summary

详细信息请参考 `references/codex-closeout.md`。

轻量级路径原则：

- 先固定 target selection
- Codex 指摘作为 advisory 处理，在实代码确认后决定采否
- final report 包含 review command / tests / accepted findings / rejected findings / clean result
- stop-on-clean: clean result 后不做仅为样子的追加 review
- Codex 不可用时 fallback 到 full manual pass，不把失败当作成功

helper:

```bash
bash scripts/harness-review-closeout.sh --dry-run --uncommitted
bash scripts/harness-review-closeout.sh --base origin/main --parallel-tests --test "bash tests/test-harness-review-governance.sh"
bash scripts/harness-review-closeout.sh --commit HEAD
```

## Plan Review Summary

详细信息请参考 `references/plan-review.md`。
Plan Review 查看 `Plans.md` 的 DoD / Depends / Status 和实现顺序。
需要规格正本的任务没有 `spec_path` 时，作为 `decision_needed` 停止。

## Scope Review Summary

详细信息请参考 `references/scope-review.md`。
Scope Review 查看要求・差分・测试・docs 的边界是否膨胀。
需要范围变更时，不靠推测推进，回到 `AskUserQuestion` 或 plan 更新。

## Security / UI / Dual

- Security: `references/security-profile.md`
- UI rubric: `references/ui-rubric.md`
- high-res vision flow: `references/vision-high-res-flow.md`
- Dual review: `references/dual-review.md`

`/ultrareview` 在 Harness flow 中默认不调用。
因为不替换 Harness flow 的 review-result.v1、commit guard、sprint-contract 的连接。
`claude ultrareview [target] --json` 仅作为 CI / script 的 second-opinion 处理。

## PR Host Boundary

GitHub-first。
PR host 上的 review 事实以 GitHub 为正，local diff 作为辅助证据处理。
但 local uncommitted review 不会 push 到 GitHub。

## Output Contract

User-facing prose 遵循显式的会话或项目语言。
如未配置语言，使用英语。仅在 `i18n.language: ja`、`CLAUDE_CODE_HARNESS_LANG=ja` 或显式会话指令要求日语输出时使用日语。
Machine-readable values 保持英语。

Start with the result summary.

~~~markdown
## Review Result

### {APPROVE | REQUEST_CHANGES | decision_needed} - {one-line conclusion}

Target: `{BASE_REF}..HEAD` or `{target}`
Verification: {commands run}

Strengths:
- ...

Findings:
- [severity] file:line - issue and evidence

Next Actions:
- ...

**⚠️ 端到端检测提示 (v2.2.0+)**:
- 审查通过（APPROVE）后将自动触发端到端检测
- 如需禁用，设置 `e2e_detection.enabled = false` 或环境变量 `HARNESS_E2E_ENABLED=false`
- 检测失败时将自动回到 harness-work 继续修改

Details:
```json
{
  "schema_version": "review-result.v1",
  "verdict": "APPROVE | REQUEST_CHANGES",
  "decision_needed": {
    "required": false,
    "ask_tool": "AskUserQuestion"
  },
  "accepted_findings": [],
  "rejected_findings": [],
  "acceptance_bar": {
    "critical_major_zero": true,
    "spec_alignment": "pass | fail | not_applicable",
    "plans_alignment": "pass | fail | not_applicable",
    "regression_safety": "pass | fail | not_applicable",
    "verification_evidence": "pass | fail | not_applicable"
  },
  "team_debate": {
    "required": false,
    "agents": [],
    "disagreements": []
  },
  "critical_issues": [],
  "major_issues": [],
  "observations": [],
  "recommendations": []
}
```
~~~

## 端到端检测触发 (v2.2.0+)

### 审查通过后的自动流程

当代码审查通过（verdict == "APPROVE"）时，系统会自动触发端到端检测：

```python
# 在 harness-work 中的集成点
if review_result.verdict == "APPROVE":
    # 加载端到端检测配置
    e2e_config = load_e2e_detection_config()
    
    if e2e_config.enabled:
        # 自动触发端到端检测
        detection_result = run_e2e_detection(e2e_config)
        
        # 处理检测结果
        if detection_result.status == "PASS":
            # 继续正常流程
            continue_flow()
        elif detection_result.status == "FAIL":
            # 回到 harness-work 继续修改
            escalate_to_harness_work(detection_result)
```

### 配置检查

审查通过前，系统会检查以下配置：

- ✅ 端到端检测是否启用（`e2e_detection.enabled`）
- ✅ 是否在支持的分支上
- ✅ 工作空间是否干净
- ✅ 是否为草稿 PR 或 WIP 分支

### 临时禁用

如果需要临时禁用端到端检测：

```bash
# 设置环境变量
export HARNESS_E2E_ENABLED=false

# 或在 harness.toml 中设置
[e2e_detection]
enabled = false
```

### 检测失败处理

端到端检测失败时的处理流程：

1. **自动修复尝试**（如果配置启用）：
   - 依赖更新
   - 敏感文件保护
   - 代码修复

2. **回到 harness-work**：
   - 自动修复失败时
   - 达到最大重试次数时

3. **升级到用户**：
   - 检测出错时
   - 配置错误时

### 相关文档

- **完整流程**: `skills/harness-work/SKILL.md#端到端检测集成`
- **架构设计**: `docs/architecture/e2e-detection-architecture.md`
- **配置参考**: `java-harness-cli/harness.toml`

## Codex Environment

Codex 环境下可用工具不同，但合格线、规格正本、`Plans.md`、退化、修正后再审查、AskUserQuestion / `decision_needed.v1` 契约相同。

| 通常环境 | Codex fallback |
|---|---|
| AskUserQuestion | 不可用时将 `decision_needed.v1` 输出到 stdout，不靠推测推进 |
| TaskList | 直接读 `Plans.md` |
## Related Skills

- `harness-work`: `REQUEST_CHANGES` 後の修正実行
- `harness-plan`: plan / scope / spec 的更新
- `harness-release`: review 完毕的 work 的 commit / release
