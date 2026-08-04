---
name: harness-sync
description: "HAR: Sync Plans.md with implementation. Drift detect, marker update, retrospective. Trigger: sync-status, where am I, check progress. --snapshot for snapshots. Do NOT load for: planning, implementation, review, release."
description-en: "HAR: Sync Plans.md with implementation. Drift detect, marker update, retrospective. Trigger: sync-status, where am I, check progress. --snapshot for snapshots. Do NOT load for: planning, implementation, review, release."
description-zh: "HAR：Plans.md 与实现的进度同步。差异检测、标记更新和回顾。当用户提到 sync-status、进度确认、当前位置、完成进度时启动。--snapshot 保存快照。不适用于：计划、实现、审查、发布。"
kind: workflow
purpose: "Reconcile Plans.md, git, and implementation state"
trigger: "sync-status, where am I, check progress"
shape: workflow
role: synchronizer
pair: harness-plan
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Edit", "Bash", "Grep", "Glob"]
argument-hint: "[--snapshot|--no-retro]"
user-invocable: true
effort: medium
---

# Harness Sync

对照 Plans.md 与实现状况，检测并更新差分。
旧 `sync-status` 及 `harness-plan sync` 子命令的独立版。

## Quick Reference

| 用户输入 | 操作 |
|------------|------|
| `harness-sync` | 进度同步 + 回顾（默认 ON） |
| `harness-sync --no-retro` | 仅进度同步（跳过回顾） |
| `harness-sync --snapshot` | 快照保存（记录当前进度时间点） |
| `harness-sync --plan roadmap` | 同步名为 `roadmap` 的 Plans |
| "现在在哪？" / "进度确认" | 同上 |

## 选项

| 选项 | 说明 | 默认值 |
|----------|------|----------|
| `--snapshot` | 将当前进度保存为快照 | false |
| `--no-retro` | 跳过回顾 | false（默认执行） |
| `--plan NAME` | 使用 `plans/manifest.json` 中的指定 plan | active/default |

## Step 0: Plans.md 验证

确认 Plans.md 的存在和格式。如有问题立即提示并停止。
在包含多个 Plans.md 的仓库中，通过 `scripts/plan-registry.sh list` 或 `--plan NAME` 确认目标 plan 后再读取。

| 状态 | 提示 |
|------|------|
| Plans.md 不存在 | `未找到 Plans.md。请使用 harness-plan create 创建。` → **停止** |
| 表头缺少 DoD / Depends 列（v1 格式） | `Plans.md 为旧格式（3列）。请使用 harness-plan create 重新生成 v2（5列）。现有任务将自动迁移。` → **停止** |
| v2 格式（5列） | 直接进入 Step 1 |

## Step 1: 现状收集（并行）

```bash
# Plans.md 状态
cat Plans.md

# Git 变更状态
git status
git diff --stat HEAD~3

# 最近提交历史
git log --oneline -10

# Agent 追踪（最近编辑的文件）
tail -20 .claude/state/agent-trace.jsonl 2>/dev/null | jq -r '.files[].path' | sort -u
```

## Step 1.5: Agent Trace 分析

从 Agent Trace 获取最近的编辑历史，并与 Plans.md 任务对照：

```bash
# 最近编辑文件列表
RECENT_FILES=$(tail -20 .claude/state/agent-trace.jsonl 2>/dev/null | \
  jq -r '.files[].path' | sort -u)

# 项目信息
PROJECT=$(tail -1 .claude/state/agent-trace.jsonl 2>/dev/null | \
  jq -r '.metadata.project')
```

**对照要点**：

| 检查项 | 检测方法 |
|------------|----------|
| 编辑了 Plans.md 中没有的文件 | Agent Trace vs 任务描述 |
| 与任务描述不同的文件 | 预期文件 vs 实际编辑 |
| 长时间未编辑的任务 | Agent Trace 时间序列 vs WIP 期间 |

## Step 2: 差分检测

| 检查项 | 检测方法 |
|------------|----------|
| 已完成却标记为 `cc:WIP` | 提交历史 vs 标记 |
| 已开始却标记为 `cc:TODO` | 变更文件 vs 标记 |
| 标记为 `cc:完了` 却未提交 | git status vs 标记 |

## Step 3: Plans.md 更新提案

检测到差分时，提出建议并执行：

```
Plans.md 需要更新

| Task | 当前 | 更改后 | 理由 |
|------|------|--------|------|
| XX   | cc:WIP | cc:完了 | 已提交 |
| YY   | cc:TODO | cc:WIP | 已编辑文件 |

是否更新？ (yes / no)
```

## Step 4: 进度摘要输出

```markdown
## 进度摘要

**项目**: {{project_name}}

| 状态 | 数量 |
|----------|------|
| 未开始 (cc:TODO) | {{count}} |
| 进行中 (cc:WIP) | {{count}} |
| 已完成 (cc:完了) | {{count}} |
| PM已确认 (pm:已确认) | {{count}} |

**进度率**: {{percent}}%

### 最近编辑的文件 (Agent Trace)
- {{file1}}
- {{file2}}
```

## Step 4.5: 快照保存（指定 `--snapshot` 时）

当指定 `--snapshot` 时，将当前进度状态保存为带时间戳的快照。

### 保存位置

以 JSON 格式保存到 `.claude/state/snapshots/` 目录：

```bash
SNAPSHOT_DIR="${PROJECT_ROOT}/.claude/state/snapshots"
mkdir -p "${SNAPSHOT_DIR}"
SNAPSHOT_FILE="${SNAPSHOT_DIR}/progress-$(date -u +%Y%m%dT%H%M%SZ).json"
```

### 快照内容

```json
{
  "timestamp": "2026-03-08T10:30:00Z",
  "phase": "Phase 26",
  "progress": {
    "total": 16,
    "todo": 5,
    "wip": 3,
    "done": 6,
    "confirmed": 2
  },
  "progress_rate": 50,
  "recent_commits": ["abc1234 feat: ...", "def5678 fix: ..."],
  "recent_files": ["skills/harness-work/SKILL.md", "..."],
  "notes": ""
}
```

### 差分比较

如果存在上次快照，显示差分：

```markdown
## 快照差分

| 指标 | 上次 ({{prev_time}}) | 本次 | 变化 |
|------|---------------------|------|------|
| 进度率 | {{prev}}% | {{current}}% | +{{diff}}%pt |
| 已完成任务 | {{prev_done}} | {{current_done}} | +{{diff_done}} |
| WIP 任务 | {{prev_wip}} | {{current_wip}} | {{diff_wip}} |
```

> **设计意图**: snapshot 是用户在"想记录当前状态"时手动使用的功能。
> 与 breezing 期间的自动进度反馈（26.2.3）是不同的功能。

## Step 5: 下一步行动建议

```
下一步行动

**优先 1**: {{任务}}
- 理由: {{依赖中 / 等待解除阻塞}}

**推荐**: harness-work, harness-review
```

## 异常检测

| 情况 | 警告 |
|------|------|
| 多个 `cc:WIP` | 多个任务同时进行中 |
| `pm:请求中` 未处理 | 优先处理 PM 的请求 |
| 严重偏离 | 任务管理跟不上进度 |
| WIP 超过3天未更新 | 确认是否被阻塞 |

## Step 6: 回顾（默认 ON）

当有 1 个以上 `cc:完了` 任务时自动执行回顾。
可通过 `--no-retro` 显式跳过。

### Step R1: 完成任务收集

```bash
# 从 Plans.md 提取 cc:完了 / pm:已确认 的任务
grep -E 'cc:完了|pm:已确认' Plans.md

# 最近的完成提交历史
git log --oneline --since="7 days ago"

# 变更规模
git diff --stat HEAD~10
```

### Step R2: 回顾四项

| 项目 | 分析方法 |
|------|---------|
| **估算精度** | 从 Plans.md 任务描述推断预期文件数 → 与 `git diff --stat` 的实际变更文件数比较 |
| **阻塞原因** | 统计带有 `blocked` 标记的任务的理由模式（技术/外部依赖/规格不明确） |
| **质量标记命中率** | 带有 `[feature:security]` 等标记的任务实际是否出现相关问题 |
| **范围变动** | Plans.md 首次提交时的任务数 vs 当前任务数（添加/删除件数） |

### Step R3: 回顾摘要输出

```markdown
## 回顾摘要

**期间**: {{start_date}} 〜 {{end_date}}

| 指标 | 值 |
|------|-----|
| 完成任务 | {{count}} 件 |
| 发生阻塞 | {{blocked_count}} 件 |
| 范围变动 | +{{added}} / -{{removed}} 件 |
| 估算精度 | 预期 {{est}} 文件 → 实际 {{actual}} 文件 |

### 学到的经验
- {{1-2 行的学到的经验}}

### 下次改进
- {{1-2 行的改进行动}}
```

### Step R4: 记录到 harness-mem

将回顾结果记录到 harness-mem，以便在下次 `create` 时可以参考。
记录位置: `.claude/agent-memory/` 下相应的 agent memory。

## 相关技能

- `harness-plan` — 计划创建·任务管理
- `harness-work` — 任务实现
- `harness-review` — 代码审查
