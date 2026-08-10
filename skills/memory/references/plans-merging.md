---
name: merge-plans
description: "执行 Plans.md 合并更新的技能（保持用户任务）。在需要整合多个 Plans.md 时使用。"
allowed-tools: ["Read", "Write", "Edit"]
---

# Merge Plans Skill

在更新现有 Plans.md 时，保持用户任务数据的同时
应用模板结构的技能。

---

## 目的

- 保持用户的任务（🔴🟡🟢📦章节）
- 更新模板的结构・标记定义
- 更新最终更新信息

---

## Plans.md 的结构

```markdown
# Plans.md - 任务管理

> **项目**: {{PROJECT_NAME}}
> **最终更新**: {{DATE}}
> **更新者**: Claude Code

---

## 🔴 进行中的任务        ← 用户数据（保持）

## 🟡 未开始的任务        ← 用户数据（保持）

## 🟢 已完成任务            ← 用户数据（保持）

## 📦 归档            ← 用户数据（保持）

## 标记图例             ← 从模板更新

## 最终更新信息             ← 更新日期
```

---

## 合并算法

### Step 1: 章节分割

```
将现有 Plans.md 分割为以下章节:

1. 标题部分（# Plans.md ... ---）
2. 🔴 进行中的任务（到下一章节）
3. 🟡 未开始的任务（到下一章节）
4. 🟢 已完成任务（到下一章节）
5. 📦 归档（到下一章节）
6. 标记图例（到下一章节）
7. 最终更新信息（到文件末尾）
```

### Step 2: 任务章节提取

```bash
extract_section() {
  local file="$1"
  local start_marker="$2"
  local end_markers="$3"  # 管道分隔的结束标记

  awk -v start="$start_marker" -v ends="$end_markers" '
    BEGIN { in_section = 0; split(ends, end_arr, "|") }
    $0 ~ start { in_section = 1; next }
    in_section {
      for (i in end_arr) {
        if ($0 ~ end_arr[i]) { in_section = 0; exit }
      }
      if (in_section) print
    }
  ' "$file"
}

# 提取各章节
TASKS_WIP=$(extract_section "$PLANS_FILE" "## 🔴" "## 🟡|## 🟢|## 📦|## 标记|---")
TASKS_TODO=$(extract_section "$PLANS_FILE" "## 🟡" "## 🔴|## 🟢|## 📦|## 标记|---")
TASKS_DONE=$(extract_section "$PLANS_FILE" "## 🟢" "## 🔴|## 🟡|## 📦|## 标记|---")
TASKS_ARCHIVE=$(extract_section "$PLANS_FILE" "## 📦" "## 🔴|## 🟡|## 🟢|## 标记|---")
```

### Step 3: 任务验证

```bash
# 确认非空
count_tasks() {
  echo "$1" | grep -c "^\s*- \[" || echo "0"
}

WIP_COUNT=$(count_tasks "$TASKS_WIP")
TODO_COUNT=$(count_tasks "$TASKS_TODO")
DONE_COUNT=$(count_tasks "$TASKS_DONE")
ARCHIVE_COUNT=$(count_tasks "$TASKS_ARCHIVE")

echo "保持的任务:"
echo "  进行中: $WIP_COUNT"
echo "  未开始: $TODO_COUNT"
echo "  已完成: $DONE_COUNT"
echo "  归档: $ARCHIVE_COUNT"
```

### Step 4: 生成新的 Plans.md

```markdown
# Plans.md - 任务管理

> **项目**: {{PROJECT_NAME}}
> **最终更新**: {{DATE}}
> **更新者**: Claude Code

---

## 🔴 进行中的任务

<!-- cc:WIP 的任务在这里记载 -->

{{TASKS_WIP}}

---

## 🟡 未开始的任务

<!-- cc:TODO, pm:依頼中（兼容: cursor:依頼中） 的任务在这里记载 -->

{{TASKS_TODO}}

---

## 🟢 已完成任务

<!-- cc:已完成, pm:確認済（兼容: cursor:確認済） 的任务在这里记载 -->

{{TASKS_DONE}}

---

## 📦 归档

<!-- 旧的已完成任务移动到这里 -->

{{TASKS_ARCHIVE}}

---

## 标记图例

| 标记 | 意义 |
|---------|------|
| `pm:依頼中` | PM 委托的任务（兼容: cursor:依頼中） |
| `cc:TODO` | Claude Code 未开始 |
| `cc:WIP` | Claude Code 进行中 |
| `cc:已完成` | Claude Code 已完成（等待确认） |
| `pm:確認済` | PM 确认完成（兼容: cursor:確認済） |
| `cursor:依頼中` | （兼容）与 pm:依頼中 同义 |
| `cursor:確認済` | （兼容）与 pm:確認済 同义 |
| `blocked` | 阻塞中（并记理由） |

---

## 最终更新信息

- **更新日期**: {{DATE}}
- **最后会话担当**: Claude Code
- **分支**: main
- **更新种类**: 插件更新
```

---

## 空章节的处理

任务为空时，插入默认文本：

```markdown
## 🔴 进行中的任务

<!-- cc:WIP 的任务在这里记载 -->

（当前无）
```

---

## 错误处理

### 无法解析 Plans.md 的情况

```bash
if ! validate_plans_structure "$PLANS_FILE"; then
  echo "⚠️ 无法解析 Plans.md 的结构"
  echo "保持备份，使用新模板"

  # 备份
  cp "$PLANS_FILE" "${PLANS_FILE}.bak.$(date +%Y%m%d%H%M%S)"

  # 使用模板
  use_template_instead=true
fi
```

### 缺少必需章节的情况

用模板的默认值补全缺失的章节。

---

## 输出

| 项目 | 说明 |
|------|------|
| `merge_successful` | 合并成功标志 |
| `tasks_wip_count` | 进行中任务数 |
| `tasks_todo_count` | 未开始任务数 |
| `tasks_done_count` | 已完成任务数 |
| `tasks_archive_count` | 归档任务数 |
| `backup_created` | 备份创建有无 |

---

## 使用示例

```bash
# 调用技能
merge_plans \
  --existing "./Plans.md" \
  --template "$PLUGIN_PATH/templates/Plans.md.template" \
  --output "./Plans.md" \
  --project-name "my-project" \
  --date "$(date +%Y-%m-%d)"
```

---

## 相关技能

- `update-2agent-files` - 更新流程全体
- `generate-workflow-files` - 新建生成
