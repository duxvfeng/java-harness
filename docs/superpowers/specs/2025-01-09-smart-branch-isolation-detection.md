# 智能分支隔离检测设计文档

**创建日期:** 2025-01-09
**状态:** 设计阶段
**作者:** Harness Core Team
**优先级:** 高

---

## 目标

在 `harness-work` 执行时自动检测当前分支状态，智能决定是否启用分支隔离，提供：
- **主分支保护**：在 master/main 分支上自动启用隔离
- **Feature 分支灵活性**：在 feature 分支上可选隔离
- **智能跳过**：已隔离状态自动检测并跳过

---

## 背景

### 现状问题

当前 `harness-work` 已有 `--isolate-branch` 功能，但需要显式指定：
```bash
/harness-work 3 --isolate-branch
```

这存在以下问题：
1. **容易遗忘**：用户可能忘记指定标志，直接在主分支上工作
2. **无保护机制**：没有强制的主分支保护
3. **用户体验差**：每次需要手动指定

### 设计原则

- **智能判断**：根据分支状态自动决策
- **用户可控**：feature 分支允许用户选择
- **最小打扰**：已隔离状态静默跳过
- **向后兼容**：保留显式标志的优先级

---

## 架构设计

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   harness-work 开始                      │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
         ┌────────────────────────┐
         │  Phase A: 准备阶段      │
         └────────────────────────┘
                      │
                      ▼
    ┌──────────────────────────────────────┐
    │   🔍 智能分支检测（NEW）              │
    ├──────────────────────────────────────┤
    │ 1. 检测当前分支                       │
    │ 2. 检测是否已隔离                     │
    │ 3. 决定隔离策略                       │
    │ 4. 用户交互（如需要）                 │
    │ 5. 设置 ISOLATE_BRANCH 变量          │
    └──────────────────────────────────────┘
                      │
                      ▼
         ┌────────────────────────┐
         │  继续标准执行流程       │
         └────────────────────────┘
```

### 检测逻辑流程

```
START
  │
  ▼
检测当前分支状态
  │
  ├─→ 已在 worktree 中？
  │     │
  │     ├─ 是 → 跳过隔离创建，继续执行
  │     └─ 否 → 继续
  │
  ▼
是否主分支 (master/main)?
  │
  ├─ 是 → 强制启用隔离
  │     │
  │     ├─ 设置 ISOLATE_BRANCH=true
  │     └─ 通知用户"主分支已自动启用隔离"
  │
  └─ 否 (feature 分支) → 询问用户
        │
        ├─ 用户同意 → 启用隔离
        ├─ 用户拒绝 → 记录决定，继续执行
        └─ 用户取消 → 中止执行
```

---

## 技术实现

### 1. 核心检测函数

```bash
detect_branch_isolation_strategy() {
    # 检测当前分支
    local current_branch=$(git branch --show-current)
    if [ -z "$current_branch" ]; then
        current_branch="detached_head"
    fi

    # 检测主分支
    local main_branch=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's@^refs/remotes/origin/@@')
    if [ -z "$main_branch" ]; then
        main_branch="master"  # 默认值
    fi

    # 检查是否已经在 worktree 中
    local git_dir=$(cd "$(git rev-parse --git-dir)" 2>/dev/null && pwd -P)
    local git_common=$(cd "$(git rev-parse --git-common-dir)" 2>/dev/null && pwd -P)

    if [ "$git_dir" != "$git_common" ]; then
        # 已经在 worktree 中
        echo "already_isolated|$current_branch"
        return
    fi

    # 判断分支类型
    if [ "$current_branch" = "$main_branch" ]; then
        echo "force_isolate|$current_branch"
    else
        echo "optional_isolate|$current_branch"
    fi
}
```

### 2. 用户交互逻辑

```bash
handle_branch_isolation() {
    local detection_result=$(detect_branch_isolation_strategy)
    local strategy=$(echo "$detection_result" | cut -d'|' -f1)
    local branch=$(echo "$detection_result" | cut -d'|' -f2)

    case "$strategy" in
        already_isolated)
            echo "✅ 已在隔离环境中，跳过分支创建"
            ISOLATE_BRANCH="skip"
            ;;
        force_isolate)
            echo "🔒 主分支检测：自动启用分支隔离"
            echo "   当前分支: $branch"
            ISOLATE_BRANCH="true"
            ;;
        optional_isolate)
            echo "🌿 Feature 分支检测: $branch"
            echo ""
            echo "是否创建隔离分支？"
            echo "  [1] 是 - 创建隔离分支（推荐）"
            echo "  [2] 否 - 在当前分支直接工作"
            echo "  [3] 取消 - 中止执行"
            echo ""
            # 用户交互逻辑...
            ;;
    esac
}
```

### 3. 集成到 harness-work

在 `harness-work` SKILL.md 中修改执行流程：

```markdown
### Phase A: 准备阶段

1. **读取 Plans.md** - 解析任务、依赖、状态
2. **🔥 智能分支检测** - 检测分支状态并决定隔离策略
   - 检测当前分支状态
   - 根据 strategy 设置 `ISOLATE_BRANCH` 变量
   - 记录决定到状态文件
3. **事前确认应用** - 应用 plan-time 预批准事项
4. **Effort 评分** - 决定模型推理强度
5. **Sprint Contract 生成** - 生成任务契约
```

### 4. 状态文件管理

创建 `.claude/state/branch-isolation-decision.json`：

```json
{
  "timestamp": "2025-01-09T10:30:00Z",
  "original_branch": "master",
  "detection_result": "force_isolate",
  "user_decision": "auto_enabled",
  "isolate_branch": true,
  "reason": "main_branch_protection"
}
```

---

## 配置选项

在 `.claude/settings.json` 中添加配置：

```json
{
  "harness-work": {
    "branchIsolation": {
      "mainBranchStrategy": "force",        // force | ask | skip
      "featureBranchStrategy": "ask",       // ask | skip | force
      "skipIfAlreadyIsolated": true,         // boolean
      "autoMergeOnSuccess": true,            // boolean
      "keepBranchOnFailure": true            // boolean
    }
  }
}
```

### 配置说明

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `mainBranchStrategy` | `force` | 主分支策略：force(强制) / ask(询问) / skip(跳过) |
| `featureBranchStrategy` | `ask` | Feature 分支策略：ask(询问) / skip(跳过) / force(强制) |
| `skipIfAlreadyIsolated` | `true` | 已隔离时跳过检测 |
| `autoMergeOnSuccess` | `true` | 测试通过后自动合并 |
| `keepBranchOnFailure` | `true` | 测试失败时保留分支 |

---

## 用户交互

### 场景 1: 主分支工作

```bash
$ /harness-work 3

🔒 主分支检测：自动启用分支隔离
   当前分支: master
   工作模式: Solo
   任务: 3 - Add user authentication

✅ 分支隔离已启用
   Feature 分支: feature/task-3-20250109-103000
   Worktree: .claude/worktrees/feature/task-3-20250109-103000

[继续正常执行流程...]
```

### 场景 2: Feature 分支工作

```bash
$ /harness-work 5

🌿 Feature 分支检测: feature/add-payment

是否创建隔离分支？
  [1] 是 - 创建隔离分支（推荐）
  [2] 否 - 在当前分支直接工作
  [3] 取消 - 中止执行

请选择 [1/2/3]: 1

✅ 创建隔离分支
   Feature 分支: feature/task-5-20250109-103100
   Worktree: .claude/worktrees/feature/task-5-20250109-103100

[继续正常执行流程...]
```

### 场景 3: 已隔离状态

```bash
$ /harness-work 7

✅ 已在隔离环境中，跳过分支创建
   当前分支: feature/task-3-20250109-103000

[继续正常执行流程...]
```

---

## 错误处理

### Git 检测失败

```bash
⚠️  Git 检测失败，无法确定分支状态
   错误: [git command] failed

建议:
  1. 检查 git 是否正确安装
  2. 检查是否在 git 仓库中
  3. 使用 --isolate-branch 显式指定

是否继续在当前分支工作？[y/N]: _
```

### Worktree 创建失败

```bash
❌ Worktree 创建失败
   路径: .claude/worktrees/feature/task-3-20250109-103000
   错误: Permission denied

建议:
  1. 检查目录权限
  2. 清理已存在的 worktree: git worktree prune
  3. 手动创建 worktree

是否继续在当前分支工作？[y/N]: _
```

### 用户取消

```bash
$ /harness-work 5

🌿 Feature 分支检测: feature/add-payment

是否创建隔离分支？
  [1] 是 - 创建隔离分支（推荐）
  [2] 否 - 在当前分支直接工作
  [3] 取消 - 中止执行

请选择 [1/2/3]: 3

⚠️  用户取消执行
   分支: feature/add-payment

记录决定到: .claude/state/branch-isolation-decision.json

执行中止。
```

---

## 向后兼容

### 1. 显式标志优先级

```bash
# 显式指定 --isolate-branch 优先级最高
/harness-work 3 --isolate-branch

# 无论当前分支状态，都会创建隔离分支
# 跳过智能检测逻辑
```

### 2. 显式跳过

```bash
# 用户可以选择跳过自动检测
/harness-work 3 --skip-branch-detection

# 在当前分支直接工作，不进行任何检测
```

### 3. 配置覆盖

```bash
# 通过环境变量覆盖默认配置
HARNESS_BRANCH_ISOLATION_FORCE=true /harness-work 3

# 强制启用分支隔离，无论分支状态
```

---

## 测试计划

### 单元测试

```bash
# 测试检测函数
test_detect_branch_isolation_strategy() {
    # Mock git commands
    alias git='echo_git'

    # Test 1: 主分支检测
    assert_equals "force_isolate|master" \
        "$(detect_branch_isolation_strategy)"

    # Test 2: Feature 分支检测
    assert_equals "optional_isolate|feature/test" \
        "$(detect_branch_isolation_strategy)"

    # Test 3: 已隔离检测
    assert_equals "already_isolated|feature/test" \
        "$(detect_branch_isolation_strategy)"
}
```

### 集成测试

```bash
# Test 1: 主分支自动隔离
cd /tmp/test_repo_main
git checkout master
/harness-work 1
# 验证: 自动创建 worktree

# Test 2: Feature 分支询问
cd /tmp/test_repo_feature
git checkout feature/test
echo "1" | /harness-work 1
# 验证: 创建 worktree

# Test 3: 已隔离跳过
cd /tmp/test_repo_worktree/.claude/worktrees/feature/*
/harness-work 1
# 验证: 跳过检测
```

### 跨平台测试

- **Linux**: Ubuntu 22.04, CentOS 8
- **macOS**: Intel, Apple Silicon
- **Windows**: Git Bash, WSL2

---

## 实现优先级

### Phase 1: 核心功能 (P0)

1. ✅ 检测函数实现
2. ✅ 用户交互逻辑
3. ✅ 集成到 harness-work Phase A
4. ✅ 状态文件管理

### Phase 2: 配置支持 (P1)

1. ⏳ `.claude/settings.json` 配置读取
2. ⏳ 策略配置实现
3. ⏳ 环境变量覆盖

### Phase 3: 错误处理 (P1)

1. ⏳ Git 检测失败处理
2. ⏳ Worktree 创建失败处理
3. ⏳ 用户取消处理

### Phase 4: 测试和文档 (P2)

1. ⏳ 单元测试
2. ⏳ 集成测试
3. ⏳ 用户文档更新

---

## 风险和限制

### 风险

1. **Git 版本兼容性**: 某些旧版本 Git 可能不支持某些命令
2. **Worktree 清理**: 长期使用可能残留大量 worktree
3. **权限问题**: 某些环境下可能无法创建 worktree

### 限制

1. **非 Git 仓库**: 无法在非 Git 仓库中使用
2. **Detached HEAD**: 无法正确检测 detached HEAD 状态
3. **Submodule**: 需要特殊处理 submodule 中的工作

---

## 未来改进

### 短期 (1-2个月)

- 添加 `--dry-run` 选项预览隔离策略
- 支持自定义分支命名规则
- 添加 worktree 自动清理机制

### 长期 (3-6个月)

- 集成 CI/CD 流程
- 支持团队协作的分支管理
- 可视化分支隔离状态

---

## 相关文档

- [Branch Isolation Mode](../references/branch-isolation.md)
- [Using Git Worktrees](../../superpowers/6.1.1/skills/using-git-worktrees/SKILL.md)
- [Execution Modes Details](../references/execution-modes.md)

---

**变更历史:**

| 日期 | 版本 | 作者 | 变更说明 |
|------|------|------|---------|
| 2025-01-09 | 1.0 | Harness Team | 初始设计文档 |

---

**审查状态:**

- [ ] 技术审查
- [ ] 安全审查
- [ ] 产品审查
- [ ] 用户测试

---

本文档遵循 [Markdown 规范](docs/markdown-style-guide.md)
