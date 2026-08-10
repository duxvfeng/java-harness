# Cleanup Reference

`/maintenance` 各子命令的执行步骤・阈值・归档目标详情。

## 共通: 环境变量（与 auto-cleanup-hook 相同的 SSOT）

| 变量 | 默认值 | 来源 |
|------|---------|-------|
| `PLANS_MAX_LINES` | 200 | `scripts/auto-cleanup-hook.sh` |
| `SESSION_LOG_MAX_LINES` | 500 | 同上 |
| `CLAUDE_MD_MAX_LINES` | 100 | 同上 |
| `ARCHIVE_AFTER_DAYS` | 7 | Plans.md 已完成任务的天数阈值 |
| `LOGS_RETAIN_DAYS` | 30 | `.claude/logs/` 的保留天数 |

用户在自由格式中指定了其他阈值时优先使用该值。

---

## plans — Plans.md 归档

### 前提

1. `.claude/state/.ssot-synced-this-session` 标志不存在 → 提示执行 `/memory sync`
2. `cc:WIP`, `pm:依頼中`, `cursor:依頼中` 标记的行**绝对不能移动**

### 步骤

```bash
PLANS="Plans.md"
cp "$PLANS" "$PLANS.bak.$(date +%s)"

# 1. 测量现状
wc -l "$PLANS"
grep -c '\[x\].*pm:確認済\|cursor:確認済' "$PLANS" || true

# 2. 提取7天以上前完成的行（使用 Edit 工具逐个提取）
#    对象: `- [x] ... (YYYY-MM-DD) ... pm:確認済|cursor:確認済`
#    例外: 包含 cc:WIP / pm:依頼中 / cursor:依頼中 的行除外

# 3. 将提取的行 append 到「## 📦 归档」章节
#    如果没有归档章节则在末尾新建
```

### 归档章节格式

```markdown
## 📦 归档

### YYYY-MM (按月分组)

- [x] 旧任务 A (2026-04-05) pm:確認済
- [x] 旧任务 B (2026-04-07) cursor:確認済
```

### 无需处理时的输出

```
✅ Plans.md: 180行（上限 200）。完成任务 6件，其中7天以上前 0件。无需整理。
```

### 执行后的报告示例

```
✅ Plans.md 整理完成
- 行数: 250 → 178 (-72)
- 归档移动: 9件 (2026-03 分组)
- 备份: Plans.md.bak.1712900000
```

---

## session-log — session-log.md 按月分割

对象是 `.claude/memory/session-log.md`。超过500行时推荐分割。

### 步骤

```bash
LOG=".claude/memory/session-log.md"
ARCHIVE_DIR=".claude/memory/archive/sessions"
mkdir -p "$ARCHIVE_DIR"

# 1. 前提是条目用 `## YYYY-MM-DD` 标题分隔
# 2. 保留最近30天的内容，将更早的内容按月分割
#    输出: .claude/memory/archive/sessions/YYYY-MM.md (append)
# 3. 从原文件中删除已移动的内容
```

### 分割文件格式

在每个 `archive/sessions/YYYY-MM.md` 的开头写入以下内容：

```markdown
# Session Log — YYYY-MM

原文件: `.claude/memory/session-log.md` 移动N天以后的内容。
移动日: YYYY-MM-DD
```

### 执行后的报告示例

```
✅ session-log.md 分割完成
- 行数: 620 → 180
- 分割目标: archive/sessions/2026-03.md (+230行), 2026-02.md (+210行)
```

---

## logs — `.claude/logs/` 旧文件删除

### 步骤

```bash
LOGS_DIR=".claude/logs"
[ -d "$LOGS_DIR" ] || exit 0

# 用 dry-run 列出对象
find "$LOGS_DIR" -type f -mtime +${LOGS_RETAIN_DAYS:-30} -print

# 执行
find "$LOGS_DIR" -type f -mtime +${LOGS_RETAIN_DAYS:-30} -delete
```

### 报告示例

```
✅ logs/ 清理完成
- 删除: 12 文件 (30天以上前)
- 残存: 34 文件
```

---

## state — agent-trace / harness-usage 压缩

`.claude/state/agent-trace.jsonl` 和 `.claude/state/harness-usage.json`
是 append-only / growing JSON，放置的话可能达到数十MB。

### agent-trace.jsonl 压缩

```bash
TRACE=".claude/state/agent-trace.jsonl"
[ -f "$TRACE" ] || exit 0

# 只保留最后1000行
tail -1000 "$TRACE" > "$TRACE.tmp" && mv "$TRACE.tmp" "$TRACE"
```

### harness-usage.json 压缩

```bash
USAGE=".claude/state/harness-usage.json"
[ -f "$USAGE" ] || exit 0

# 删除60天以上前的条目（结构依存，用 jq 适当写条件）
# 实现前先用 Read 确认实际结构再处理
```

### 报告示例

```
✅ state 压缩完成
- agent-trace.jsonl: 8421行 → 1000行
- harness-usage.json: 删除2026-02以前的条目
```

---

## all — 全部执行

按 plans → session-log → logs → state 顺序执行。途中出现错误时停止并向用户报告。

### 执行流程

1. SSOT 同步检查（仅在 plans 包含在对象中时）
2. 顺序执行各子命令
3. 最后显示 Before/After 一览

### 报告示例

```
✅ 总维护完成

| 对象 | Before | After | 变化 |
|------|--------|-------|------|
| Plans.md | 250行 | 178行 | -72 (归档 9件) |
| session-log.md | 620行 | 180行 | -440 (2文件分割) |
| logs/ | 46 files | 34 files | -12 (超过30天) |
| agent-trace.jsonl | 8421行 | 1000行 | -7421 |

备份: Plans.md.bak.1712900000
```

---

## 常见追加指示处理示例

| 指示 | 处理 |
|------|------|
| 「旧归档也删除」 | 额外删除 `.claude/memory/archive/` 内超过N天的内容 |
| 「用 dry-run」 | 将所有删除・移动替换为 `echo`，仅列出要删除的内容 |
| 「保留这个文件」 | 从对象列表中排除该文件后执行 |
| 「将阈值提高到300行」 | 临时覆盖 `PLANS_MAX_LINES=300` 等环境变量 |

---

## 禁止事项

- ❌ `.claude/memory/decisions.md` / `patterns.md` 自动编辑（禁止直接改写 SSOT）
- ❌ `CHANGELOG.md` 压缩・归档（不删除历史）
- ❌ `.git/` 下的操作
- ❌ 无备份的行删除（超过200行的文件必须备份）
