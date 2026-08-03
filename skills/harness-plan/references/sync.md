# sync 子命令 — 进度同步流程

对照实现状况与 Plans.md，检测・更新差异。

## Step 0: Plans.md 验证

确认 Plans.md 的存在和格式。有问题时立即引导并停止。

| 状态 | 引导 |
|------|------|
| Plans.md 不存在 | `Plans.md 未找到。请用 /harness-plan create 创建。` → **停止** |
| 表头无 DoD / Depends 列（v1 格式） | `Plans.md 为旧格式（3列）。请用 /harness-plan create 再生成 v2（5列）。现有任务自动继承。` → **停止** |
| v2 格式（5列）| 照样进入 Step 1 |

## Step 1: 现状收集（并行）

```bash
# Plans.md 状态
cat Plans.md

# Git 更改状态
git status
git diff --stat HEAD~3

# 最近提交履历
git log --oneline -10

# 代理轨迹（最近编辑文件）
tail -20 .claude/state/agent-trace.jsonl 2>/dev/null | jq -r '.files[].path' | sort -u
```

## Step 1.5: Agent Trace 分析

从 Agent Trace 获取最近的编辑履历，与 Plans.md 的任务对照:

```bash
# 最近编辑文件列表
RECENT_FILES=$(tail -20 .claude/state/agent-trace.jsonl 2>/dev/null | \
  jq -r '.files[].path' | sort -u)

# 项目信息
PROJECT=$(tail -1 .claude/state/agent-trace.jsonl 2>/dev/null | \
  jq -r '.metadata.project')
```

**对照点**:

| 检查项 | 检测方法 |
|------------|----------|
| 编辑 Plans.md 不存在的文件 | Agent Trace vs 任务描述 |
| 与任务描述不同的文件 | 预想文件 vs 实际编辑 |
| 长时间无编辑的任务 | Agent Trace 时系列 vs WIP 期间 |

## Step 2: 差异检测

| 检查项 | 检测方法 |
|------------|----------|
| 已完成却 `cc:WIP` | 提交履历 vs 标记 |
| 已着手却 `cc:TODO` | 更改文件 vs 标记 |
| `cc:完了` 却未提交 | git status vs 标记 |

### Artifact Hash 后方互兼容

同时识别 `cc:完了 [a1b2c3d]` 格式（带 commit hash）和 `cc:完了`（无 hash）。

**匹配规则**:
- `cc:完了` → 作为无 hash 完成处理
- `cc:完了 [xxxxxxx]` → 作为带 hash 完成处理。保持 7 字短缩 hash
- 带 hash 时，可与 `git log --oneline` 对照确认提交存在

> **后方兼容**: 无 hash 格式继续有效。不破坏既有 Plans.md。

## Step 3: Plans.md 更新提案

检测到差异时，提案并执行:

```
需要更新 Plans.md

| Task | 当前 | 更新后 | 理由 |
|------|------|--------|------|
| XX   | cc:WIP | cc:完了 | 已提交 |
| YY   | cc:TODO | cc:WIP | 文件已编辑 |

更新吗？ (yes / no)
```

## Step 4: 进度摘要输出

```markdown
## 进度摘要

**项目**: {{project_name}}

| 状态 | 件数 |
|----------|------|
| 未着手 (cc:TODO) | {{count}} |
| 作业中 (cc:WIP) | {{count}} |
| 完成 (cc:完了) | {{count}} |
| PM已确认 (pm:確認済) | {{count}} |

**进度率**: {{percent}}%

### 最近编辑文件 (Agent Trace)
- {{file1}}
- {{file2}}
```

## Step 5: 下一步行动提案

```
接下来做

**优先 1**: {{任务}}
- 理由: {{请求中 / 等待解除}}

**推荐**: harness-work, harness-review
```

## 异常检测

| 状况 | 警告 |
|------|------|
| 多个 `cc:WIP` | 多任务同时进行中 |
| `pm:依頼中` 未处理 | 先处理 PM 请求 |
| 大偏差 | 任务管理追赶不上 |
| WIP 超过 3 天无更新 | 确认是否被阻塞 |

## Step 6: 回顾（默认 ON）

`sync` 执行时，`cc:完了` 任务 1 件以上自动执行回顾。
`--no-retro` 可明确跳过。

### Step R1: 完成任务收集

```bash
# 从 Plans.md 抽取 cc:完了 / pm:確認済 任务
grep -E 'cc:完了|pm:確認済' Plans.md

# 最近完成提交履历
git log --oneline --since="7 days ago"

# 更改规模
git diff --stat HEAD~10
```

### Step R2: 回顾 4 项目

| 项目 | 分析方法 |
|------|---------|
| **估算精度** | 从 Plans.md 任务描述推想文件数 → 与 `git diff --stat` 实际更改文件数比较 |
| **阻塞原因** | 汇总带 `blocked` 标记任务的理由模式（技术/外部依赖/规格不明确） |
| **质量标记命中率** | 带 `[feature:security]` 等的任务实际是否出现相关问题 |
| **范围变动** | Plans.md 首次提交时任务数 vs 当前任务数（追加/删除件数） |

### Step R3: 回顾摘要输出

```markdown
## 回顾摘要

**期间**: {{start_date}} 〜 {{end_date}}

| 指标 | 值 |
|------|-----|
| 完成任务 | {{count}} 件 |
| 阻塞发生 | {{blocked_count}} 件 |
| 范围变动 | +{{added}} / -{{removed}} 件 |
| 估算精度 | 预想 {{est}} 文件 → 实际 {{actual}} 文件 |

### 学到
- {{1-2 行学到}}

### 下次活用
- {{1-2 行改善行动}}
```

### Step R4: 记录到 harness-mem

将回顾结果记录到 harness-mem，下次 `create` 时可参照。
记录目标: `.claude/agent-memory/` 下的对应代理内存。
