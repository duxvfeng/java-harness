# Branch Isolation Mode

## Purpose

在任务执行前询问是否创建 git 分支隔离。用户选择隔离后执行，测试通过后再合并回主分支，测试失败时保留分支用于调试。

## 概述

分支隔离流程适用于所有执行模式（Solo / Parallel / Breezing）。执行 `/harness-work` 时，系统会先询问用户选择“隔离”或“不隔离”；只有选择“隔离”时才创建隔离分支。`--isolate-branch` 作为兼容标志保留。

### 核心流程

```
1. 检查当前分支状态
2. 如果在主分支，创建 feature 分支（使用 git worktree）
3. 在隔离分支中执行任务实现
4. 运行测试验证
5. 测试通过：合并回主分支，清理 feature 分支
6. 测试失败：保留 feature 分支和 worktree 用于调试
```

## 命令行选项

| 选项 | 说明 | 默认值 |
|----------|------|----------|
| `--isolate-branch` | 兼容旧用法，启用分支隔离流程 | true |
| `--no-merge` | 完成测试后不自动合并，保留 feature 分支 | false |
| `--branch-name <name>` | 自定义分支名称（格式：feature/task-<id>-<timestamp>） | auto |
| `--keep-branch` | 完成后保留 feature 分支（用于人工审查或创建 PR） | false |

## 使用示例

### 基础用法

```bash
# 单个任务，先询问隔离，再按用户选择创建分支并合并
/harness-work 3 --isolate-branch

# 全部任务，每个任务独立分支
/harness-work all --isolate-branch

# Breezing 模式 + 分支隔离
/harness-work --breezing --isolate-branch
```

### 高级用法

```bash
# 执行后保留分支，手动创建 PR
/harness-work 3 --isolate-branch --keep-branch

# 自定义分支名称
/harness-work 5 --isolate-branch --branch-name feature/add-user-auth

# 测试后不自动合并，等待人工审查
/harness-work 3-6 --isolate-branch --no-merge

# 与其他模式组合
/harness-work --parallel 3 --isolate-branch --no-merge
```

## 执行流程详解

### Phase A: 分支准备

在标准 Phase A 准备阶段之后，执行分支隔离检查：

```bash
# 1. 检测当前分支
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
MAIN_BRANCH=$(git symbolic-ref refs/remotes/origin/HEAD | sed 's@^refs/remotes/origin/@@')

# 2. 如果在主分支且启用了分支隔离
if [ "$CURRENT_BRANCH" = "$MAIN_BRANCH" ] && [ "$ISOLATE_BRANCH" = "true" ]; then
    # 3. 准备分支信息
    TASK_ID="<task-id>"
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    BRANCH_NAME="${CUSTOM_BRANCH_NAME:-feature/task-${TASK_ID}-${TIMESTAMP}}"
    WORKTREE_PATH=".claude/worktrees/${BRANCH_NAME}"
    BASE_REF=$(git rev-parse HEAD)

    # 4. 创建 worktree 和分支
    mkdir -p .claude/worktrees
    git worktree add -b "$BRANCH_NAME" "$WORKTREE_PATH" "$BASE_REF"

    # 5. 切换到 worktree
    cd "$WORKTREE_PATH"

    # 6. 记录分支信息到状态文件
    # 状态由 harness-work 的 v2 状态管理入口写入，不在此处创建旧格式文件。
    # 唯一状态文件：.claude/state/branch-isolation-decision.json
    cat > .claude/state/branch-isolation-decision.json <<EOF
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": null,
  "codeStatus": null,
  "resetTriggers": {
    "autoResetCondition": "branch_clean_and_no_uncommitted_changes",
    "autoResetAfterHours": 4,
    "manualResetAvailable": true,
    "taskSeriesComplete": false,
    "autoResetEnabled": true
  },
  "decisionHistory": [],
  "metadata": {
    "createdAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "updatedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "version": "2.0"
  }
}
EOF

    echo "🔀 Branch isolation enabled"
    echo "   Feature branch: $BRANCH_NAME"
    echo "   Worktree path: $WORKTREE_PATH"
fi
```

### Phase B: 任务实现（在隔离分支）

在 worktree 中正常执行任务实现，所有变更都在隔离分支进行：

```bash
# 在 $WORKTREE_PATH 中执行
# - 编写代码
# - 运行测试
# - 代码审查
# - 创建 commit

# 所有 git 操作都在 worktree 中
git add .
git commit -m "feat: complete task ${TASK_ID}"
```

### Phase C: 分支合并或保留

任务完成后，根据测试结果决定合并或保留分支：

#### 场景 1: 测试通过且未指定 --no-merge

```bash
# 1. 确认在 worktree 中
cd "$WORKTREE_PATH"

# 2. 最终测试验证
mvn test
# 或
npm test

# 3. 如果测试通过
if [ $? -eq 0 ] && [ "$NO_MERGE" != "true" ]; then
    # 4. 切换回主分支
    cd "$PROJECT_ROOT"
    git checkout "$MAIN_BRANCH"

    # 5. 合并 feature 分支
    git merge --no-ff "$BRANCH_NAME" -m "Complete task ${TASK_ID}: ${TASK_TITLE}"

    # 6. 清理 worktree 和分支
    if [ "$KEEP_BRANCH" != "true" ]; then
        git worktree remove "$WORKTREE_PATH"
        git branch -d "$BRANCH_NAME"
        echo "✅ Branch merged and cleaned up"
    else
        echo "✅ Branch merged. Feature branch '$BRANCH_NAME' preserved for review"
    fi
fi
```

#### 场景 2: 测试失败

```bash
# 测试失败时保留分支用于调试
cd "$WORKTREE_PATH"

if mvn test; then
    # 测试通过，继续合并流程
else
    # 测试失败
    echo "❌ Tests failed in isolated branch"
    echo "📁 Worktree preserved at: $WORKTREE_PATH"
    echo "🔀 Fix branch: $BRANCH_NAME"
    echo ""
    echo "To debug:"
    echo "  cd $WORKTREE_PATH"
    echo "  git log --oneline -5"
    echo "  mvn test"
    echo ""
    echo "To fix and retry:"
    echo "  cd $WORKTREE_PATH"
    echo "  # make fixes"
    echo "  git commit -am 'fix: test failures'"
    echo "  cd $PROJECT_ROOT"
    echo "  git checkout $MAIN_BRANCH"
    echo "  git merge $BRANCH_NAME"

    # 保留分支状态信息
    cat > .claude/state/failed-branch.json <<EOF
{
  "branch": "$BRANCH_NAME",
  "worktree": "$WORKTREE_PATH",
  "task_id": "$TASK_ID",
  "failed_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "reason": "tests_failed"
}
EOF

    exit 1
fi
```

#### 场景 3: 指定 --no-merge（手动合并）

```bash
# 完成任务但不自动合并
cd "$WORKTREE_PATH"

# 运行测试
mvn test

echo "✅ Task completed and tests passed in isolated branch"
echo "🔀 Feature branch: $BRANCH_NAME"
echo "📁 Worktree: $WORKTREE_PATH"
echo ""
echo "Next steps:"
echo "  1. Review changes: cd $WORKTREE_PATH && git diff $MAIN_BRANCH"
echo "  2. Create PR: gh pr create --base $MAIN_BRANCH"
echo "  3. Or merge manually: cd $PROJECT_ROOT && git merge $BRANCH_NAME"
```

## 与不同执行模式的组合

### Solo 模式

```bash
# 单个任务，隔离分支
/harness-work 3 --isolate-branch

# 流程：
# 1. 创建 feature/task-3-20260807-143022 分支
# 2. 在 worktree 中实现任务
# 3. 测试通过后合并回 master
# 4. 清理分支
```

### Parallel 模式

```bash
# 并行执行多个任务，每个任务独立分支
/harness-work 3-5 --parallel 3 --isolate-branch

# 流程：
# 1. 为每个任务创建独立分支：
#    - feature/task-3-20260807-143022
#    - feature/task-4-20260807-143023
#    - feature/task-5-20260807-143024
# 2. 并行执行（通过 worktree 避免文件冲突）
# 3. 各任务测试通过后依次合并
# 4. 清理所有分支
```

### Breezing 模式

```bash
# 团队执行 + 分支隔离
/harness-work --breezing --isolate-branch

# 流程：
# 1. Lead 创建 feature 分支
# 2. Worker 在隔离分支中实现
# 3. Reviewer 在隔离分支中审查
# 4. 测试通过后合并回主分支
# 5. Lead 生成完成报告
```

## 错误处理

### 合并冲突

```bash
# 自动合并时检测到冲突
git merge --no-ff "$BRANCH_NAME"

if [ $? -ne 0 ]; then
    echo "⚠️  Merge conflict detected"
    echo "🔀 Conflict branch: $BRANCH_NAME"
    echo ""
    echo "To resolve:"
    echo "  1. Review conflicts: git status"
    echo "  2. Fix conflicts: # edit files"
    echo "  3. Mark resolved: git add <files>"
    echo "  4. Complete merge: git commit"
    echo "  5. Cleanup: git worktree remove $WORKTREE_PATH"

    # 保留分支用于冲突解决
    exit 1
fi
```

### Worktree 已存在

```bash
# 检查 worktree 是否已存在
if [ -d "$WORKTREE_PATH" ]; then
    echo "⚠️  Worktree already exists: $WORKTREE_PATH"
    echo "Removing old worktree..."
    git worktree remove "$WORKTREE_PATH" || true
    git branch -D "$BRANCH_NAME" || true
fi
```

## 状态文件

### `.claude/state/branch-isolation-decision.json`

记录分支隔离的完整 v2 状态和决策历史：

```json
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": null,
  "codeStatus": null,
  "resetTriggers": {},
  "decisionHistory": [],
  "metadata": {}
}
```

### `.claude/state/failed-branch.json`

记录失败的分支用于后续修复：

```json
{
  "branch": "feature/task-3-20260807-143022",
  "worktree": ".claude/worktrees/feature/task-3-20260807-143022",
  "task_id": "3",
  "failed_at": "2026-08-07T15:45:10Z",
  "reason": "tests_failed"
}
```

## 配置文件支持

在 `.claude/settings.json` 中配置默认行为：

```json
{
  "harness-work": {
    "branchIsolation": {
      "enabled": false,
      "autoMerge": true,
      "branchPrefix": "feature/",
      "keepBranchOnFailure": true,
      "cleanupWorktree": true,
      "mergeStrategy": "no-ff"
    }
  }
}
```

## 最佳实践

### 1. 团队协作

```bash
# 推荐团队使用 --keep-branch + PR 工作流
/harness-work 3 --isolate-branch --keep-branch

# 完成后自动创建 PR
cd .claude/worktrees/feature/task-3-*
gh pr create --base master --title "Complete task 3" --body "Task 3 implementation"
```

### 2. CI/CD 集成

```bash
# CI 环境中使用 --no-merge，由 CI 系统控制合并
/harness-work all --isolate-branch --no-merge

# CI 测试通过后合并
if [ $CI_STATUS = "success" ]; then
    git merge feature/task-*
fi
```

### 3. 调试和修复

```bash
# 失败后快速修复
cd .claude/worktrees/feature/task-3-*
# 修复代码
mvn test
git commit -am "fix: resolve test failures"
cd ../../../
git merge feature/task-3-*
```

## 注意事项

### 分支命名规范

- 默认格式：`feature/task-<id>-<timestamp>`
- 推荐使用描述性名称：`--branch-name feature/add-user-login`
- 避免特殊字符和空格
- 保持分支名简洁但有意义

### Worktree 清理

```bash
# 手动清理所有 worktrees
git worktree list
git worktree remove <path>

# 清理已删除分支的 worktrees
git worktree prune
```

### 主分支检测

```bash
# 自动检测主分支名称
MAIN_BRANCH=$(git symbolic-ref refs/remotes/origin/HEAD | sed 's@^refs/remotes/origin/@@')
# 支持 master, main, develop 等
```

## 故障排查

### 问题：worktree 创建失败

```bash
# 检查是否有未提交的变更
git status

# 检查 .git/worktrees 目录
ls -la .git/worktrees

# 手动清理
git worktree prune
```

### 问题：合并后分支未删除

```bash
# 手动删除分支
git branch -d feature/task-3-*

# 强制删除（如果已合并）
git branch -D feature/task-3-*

# 删除 worktree
git worktree remove .claude/worktrees/feature/task-3-*
```

### 问题：测试通过但合并失败

```bash
# 检查远程更新
git fetch origin

# 检查分支状态
git log HEAD..origin/master

# 变基后重试
git checkout feature/task-3-*
git rebase master
git checkout master
git merge feature/task-3-*
```

## 相关文档

- [Execution Modes Details](execution-modes.md) - Solo/Parallel/Breezing 完整流程
- [Backend Selection](backend-selection.md) - Cursor/Codex 后端选择
- [Sprint Contract](sprint-contract.md) - 任务契约和 PR Closeout

---

**版本**: 1.0.0
**最后更新**: 2026-08-07
**维护者**: Harness Core Team
