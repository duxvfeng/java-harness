---
name: cc-update-review
description: "Quality guardrail for Claude/Codex update integration. Detects doc-only Feature Table additions and requires implementation or explicit planning. Internal use only."
description-en: "Quality guardrail for Claude/Codex update integration. Detects doc-only Feature Table additions and requires implementation or explicit planning. Internal use only."
description-zh: "Claude/Codex 上游更新集成的质量护栏。检测 Feature Table 中仅文档添加的情况，强制要求实现或明确规划。仅限内部使用。"
user-invocable: false
disable-model-invocation: true
allowed-tools: ["Read", "Grep", "Glob", "Bash"]
---

# Claude/Codex Update Review 质量护栏

防止 Claude Code / OpenAI Codex 更新集成时"仅在 Feature Table 书写"的质量护栏。
分类 Feature Table 的添加是否伴随实现・验证・明确的将来任务化，不足则强制输出实现方案。

## Quick Reference

以下情况触发此技能:

- Claude Code / Codex upstream update 集成 PR 审查时
- 检测到 `docs/CLAUDE-feature-table.md` 有新增行的 diff 时
- `/harness-review` 判定为 upstream update 集成 PR 时的内部调用
- `claude-codex-upstream-update` 技能更新审查时

不触发的情况:

- 通常实现作业
- 与 Feature Table / upstream 追随无关的更改
- 设置・初始化作业

## 获取差分输入

此技能专用于 diff-aware review，必须通过以下任一确定审查对象差分。

1. 调用方的 `/harness-review` 传递 PR diff / changed files / Feature Table 新增行
2. 此技能自身通过 read-only Bash 执行 `git status --short`, `git diff --name-only`, `git diff -- docs/CLAUDE-feature-table.md`, `git show --stat --name-only` 等确认

Bash 仅用于 read-only git inspection。不执行测试、format、生成、网络访问、文件更改的命令。
无法获取 diff 时，不推测为 `B: 仅书写 0 件`，而是作为"未提供差分无法分类"停止审查。

## 前提检查

审查开头必须确认:

- diff source 是否由调用方提供或 read-only git inspection 确定
- 编辑 `skills/` 或 `hooks/` 的 PR，是否紧接执行 `/reload-plugins` 更新 runtime cache（`{skills,hooks}/**` 准则）
- 是否有 upstream 版本分解表
- Claude Code 一次信息 URL 是否为 `anthropics/claude-code` 或官方 docs
- Codex 一次信息 URL 是否为 `openai/codex/releases` 或 OpenAI 官方文章
- 是否残留 `B: 仅书写`
- 触及 skill mirror 时，`skills/`, `codex/.codex/skills/`, `.agents/skills/` 差分是否符合预期

禁止的旧参照:

- 旧 TypeScript guardrail path
- 旧 TypeScript implementation glob
- 旧 Codex feature-table path
- 旧 Codex plugin directory
- 旧 Codex state directory 作为现行正本的记述
- 不存在的 Anthropic 侧 Codex repo URL

## A/B/C/P 分类

将 Feature Table 新增的各项目分类为以下 A/B/C/P 任一。

### (A) 有实现

定义: Feature Table 新增对应的 hooks / settings / Go / scripts / agents / skills / tests 更改包含在同一 PR 中。

判定条件:

- Feature Table 行中提到的功能相关文件被更改
- `hooks/hooks.json`, `.claude-plugin/hooks.json`, `.claude-plugin/settings.json`, `go/internal/guardrail/`, `go/internal/hookhandler/`, `scripts/`, `agents/`, `skills/`, `tests/` 任一有实际差分
- 由对象测试或验证脚本固定

例:

| Feature Table 新增 | 对应实现更改 | 判定 |
|-------------------|----------------|------|
| `AskUserQuestion updatedInput` | Go handler + hooks wiring + upstream integration test | A |
| `sandbox.network.deniedDomains` | `.claude-plugin/settings.json` + jq test | A |
| `find -delete hardening` | `go/internal/guardrail/` + unit test | A |

结果: OK。无需额外操作。

---

### (B) 仅书写

定义: 仅在 Feature Table 新增行，Harness 侧无实现更改也无 Plans 化。且不属于 upstream 自动继承。

判定条件:

- Feature Table 有新行
- 同一 PR 内无相关实现 / test / skill / Plans 更改
- Harness 应提供独自附加价值的功能

例:

| Feature Table 新增 | 对应实现更改 | 判定 |
|-------------------|----------------|------|
| `PreCompact hook` | 无 | B |
| `permission hardening` | 无 settings / guardrail / tests 确认 | B |
| `Codex marketplace` | 无 Plans 分离 | B |

结果: NG。阻止 PR，要求实现方案或 Plans 化。

---

### (C) upstream 自动继承

定义: Claude Code / Codex 本体的性能改善・bug 修复・内部优化等，Harness 侧无需更改的项目。

判定条件:

- upstream 本体修正，Harness 无包层・扩展余地
- 不影响 Harness 的 settings / hooks / guardrail / workflow / tests
- Feature Table 明记"upstream 自动继承"或"CC 自动继承 / Codex 侧自动继承"

注意:

- permission / sandbox / security / Bash allowlist / MCP trust boundary 不轻易定为 C
- 确认该项目不影响 Harness 独自 guardrail 或 settings 后再定为 C
- Claude Code 2.1.113 的 hardening，未确认 `sandbox.network.deniedDomains`, wrapper Bash deny, `find -exec/-delete`, macOS dangerous rm paths 前不判定 C

例:

| Feature Table 新增 | 理由 | 判定 |
|-------------------|------|------|
| `Agent Teams permission dialog crash fix` | CC 本体 crash fix。Harness 侧无需更改 | C |
| `Codex Guardian timeout wording` | Codex 侧 UX 修正。无 Harness surface | C |

结果: OK。但需明记理由。

---

### (P) Plans 化

定义: 本次不直接实现，但值得纳入 Harness，作为 `Plans.md` 明确任务保留。

判定条件:

- Feature Table 附加价值列可读为 `A: 将来任务化` 或 `P: Plans 化`
- `Plans.md` 有对应任务，明记 setup / guardrails / memory / Codex workflow 等实现面
- 记录不立即实现理由，如 alpha release 或大规模设计变更

例:

| Feature Table 新增 | Plans 分离 | 判定 |
|-------------------|-------------------|------|
| `Codex marketplace / MCP Apps` | Codex workflow 比较轴任务 | P |
| `Codex 0.122.0-alpha` | stable 化后的 compare 调查任务 | P |

结果: OK。下次 cycle 可拾取。

## Upstream update PR 检查清单

```markdown
## Claude/Codex update 集成检查清单

### 1. 一次信息与分解表
- [ ] diff source 由调用方提供或 read-only git inspection 确定
- [ ] 确认 Claude / Codex 官方 URL
- [ ] 有 Version / Upstream item / Category / Harness surface / Action 表
- [ ] 有 alpha / stable / docs-only 区别

### 2. Feature Table 差分
- [ ] 列举 `docs/CLAUDE-feature-table.md` 新增行
- [ ] 各行附带 A / C / P 任一
- [ ] B 为 0 件

### 3. 分类别确认
- [ ] (A) 有实现: 有对应实现文件和测试
- [ ] (B) 仅书写: 0 件。残留则阻止 PR
- [ ] (C) 自动继承: 确认 permission / sandbox / security / workflow 影响
- [ ] (P) Plans 化: `Plans.md` 有将来任务

### 4. Mirror 与 stale path
- [ ] `skills/` 与 `codex/.codex/skills/` 无意外 drift
- [ ] `.agents/skills/` 存在时，Claude/Codex 表记未破损
- [ ] 无旧 TypeScript guardrail path、旧 Codex plugin directory、旧 Codex feature-table path 等旧参照

### 5. CHANGELOG / tests
- [ ] CHANGELOG 有"迄今为止 / 今后"或相当的 user-facing 说明
- [ ] upstream integration test 或对象 unit test 已追加/更新
```

## 分类 B 检测时的输出格式

检测到分类 B 1 件以上时，以下格式输出实现方案。
此格式输出必需，省略不被允许。

```markdown
## 分类 B 检测: 实现方案

### B-{编号}. {Feature Table 项目名}

**现状**: 仅记载于 Feature Table。无 Harness 侧实现 / 验证 / Plans 化。

**Harness 独有附加价值**:
{Harness 应如何活用此功能的具体说明}

**实现方案**:

| 对象文件 | 更改内容 |
|------------|---------|
| `{文件路径}` | {具体更改内容} |
| `{文件路径}` | {具体更改内容} |

**用户体验改善**:
- 迄今: {当前用户体验}
- 今后: {实现后用户体验}

**实现优先级**: {高 / 中 / 低}
**预估工数**: {小 / 中 / 大}
```

## 相关技能

- `claude-codex-upstream-update` - upstream 差分调查与实现 cycle
- `harness-review` - 代码审查
- `harness-work` - 分类 B / P 的实现
- `memory` - 分类标准的 SSOT 化
