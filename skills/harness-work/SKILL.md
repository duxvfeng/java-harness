---
name: harness-work
description: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-en: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-zh: "HAR：负责从单个任务到全并行团队执行的 Plans.md 任务。当用户提到实现、执行、全部完成、breezing、团队执行、并行、composer、作曲器、composer 2.5 时启动。不适用于：计划、审查、发布、设置。"
kind: workflow
purpose: "Execute Plans.md tasks end to end"
trigger: "implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5, composer mode, 作曲器"
shape: workflow
role: executor
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Grep", "Glob", "Bash", "Task", "Monitor"]
argument-hint: "[all] [task-number|range] [--codex] [--parallel N] [--no-commit] [--resume id] [--breezing] [--auto-mode] [--tdd-bypass]"
user-invocable: true
effort: high
---

# Harness Work

Harness 的集成执行技能。

## Java 版本边界

本技能的完整自动编排段落来自 Go 版本。Java CLI 负责命令路由、契约、证据、健康检查和状态记录；实际的 worker、worktree、测试和 review 由宿主平台执行。本文中的 `scripts/*.sh`、`scripts/*.js`、`HARNESS_PLUGIN_ROOT`、companion、checkpoint 和 review runner 仅供 Go 版本参考，不应作为 Java 版本的可执行命令。

## 技能职责定义（实现执行者）

### 🎯 核心职责范围

**harness-work 是实现执行技能，负责从任务到代码实现的执行过程。**

**明确的职责边界**: 本技能负责代码实现、构建测试、工作区管理等实现相关工作。**不进行规划决策或任务定义修改**。规划和技术选型由 `harness-plan` 负责。

### ✅ 允许的操作

**代码实现**:
- ✅ **修改源代码文件** (`src/`、`app/`、`lib/`、`pkg/` 等)
- ✅ **执行构建命令** (`mvn`、`npm`、`gradle` 等)
- ✅ **运行测试** (`mvn test`、`npm test` 等)
- ✅ **修改配置文件** (`.env`、`config/`、`pom.xml`、`package.json` 等)
- ✅ **管理依赖** 和构建配置

**状态分析**:
- ✅ **Git 状态深度分析** (`git status`、`git log`、`git diff`)
- ✅ **Agent trace 分析** (实现历史和代码变更)
- ✅ **代码结构分析** 和依赖关系检查
- ✅ **测试结果分析** 和覆盖率检查

**状态同步**:
- ✅ **更新 Plans.md 任务状态** (基于实际完成情况)
- ✅ **分析实现进度** 和识别偏差
- ✅ **同步代码与规划** 状态
- ✅ **执行回顾分析** 和精度评估

**工作区管理**:
- ✅ **创建和管理 worktree** (隔离工作环境)
- ✅ **分支操作** (创建、切换、合并)
- ✅ **提交和推送** (git commit, git push)
- ✅ **PR 创建和管理** (代码审查流程)

### ❌ 禁止的操作

**规划决策**:
- ❌ **严禁修改任务定义** (Task 名称、内容、DoD)
- ❌ **严禁改变任务优先级** 或依赖关系
- ❌ **严禁添加新任务** (这是 `harness-plan` 的职责)
- ❌ **严禁修改产品规格** (`spec.md` 内容)

**架构决策**:
- ❌ **严禁改变技术选型** (未经规划批准)
- ❌ **严禁修改系统架构** (未经规划批准)
- ❌ **严禁重构项目结构** (未经规划批准)

### 🔵 灰色地带处理规则

**实现中的规划修正**:
- ✅ 允许: **发现技术债务时记录到 Plans.md** (添加注释或标记)
- ⚠️ 限制: **不能修改任务定义** (Task 名称、内容、DoD)
- ❌ 禁止: **改变任务优先级** 或删除任务

**紧急架构调整**:
- ✅ 允许: **记录架构问题** 到 Plans.md 或 issue
- ⚠️ 限制: **标记任务为 blocked** 并说明原因
- ❌ 禁止: **直接修改架构** (需要重新规划)

**状态更新权限**:
- ✅ 允许: **更新任务状态标记** (`cc:TODO` → `cc:WIP` → `cc:完成`)
- ⚠️ 限制: **仅基于实际完成情况**，不能主观判断
- ❌ 禁止: **修改任务的其他字段** (名称、内容、DoD)

### 🔄 规划→实现交接协议

**接收条件** (从 `harness-plan` 接收已批准的 Plans.md):
1. ✅ Plans.md 格式正确，包含完整的任务定义
2. ✅ 每个任务有明确的 DoD 和依赖关系
3. ✅ 任务状态标记为 `cc:TODO`
4. ✅ 用户已批准计划
5. ✅ 实现前置条件已识别

**交接验证**:
```bash
# 接收前检查
- Plans.md 格式验证
- 任务完整性检查
- 依赖关系验证
- DoD 可测试性验证
```

**实施过程中的状态更新**:
```bash
# 执行过程中可以更新
- 任务状态: cc:TODO → cc:WIP → cc:完成
- 添加实现注释（不修改任务定义）
- 记录发现的问题（标记为 blocked 或添加注释）
```

**完成后交接回 `harness-sync`**:
```bash
# 实现完成后
- 更新 Plans.md 状态标记
- 调用 harness-sync 进行完整状态同步
- 执行回顾分析（精度评估）
```

### 📋 职责分离表

| 活动 | harness-plan | harness-work | harness-sync |
|------|-------------|--------------|--------------|
| 任务定义和分解 | ✅ 主责 | ❌ 禁止 | ❌ 禁止 |
| 代码实现 | ❌ 禁止 | ✅ 主责 | ❌ 禁止 |
| 构建测试 | ❌ 禁止 | ✅ 主责 | ❌ 禁止 |
| Git 状态分析 | ❌ 禁止 | ✅ 主责 | ✅ 主责 |
| 状态标记更新 | ❌ 禁止 | ✅ 主责 | ✅ 主责 |
| 进度同步 | ❌ 禁止 | ⚠️ 执行中更新 | ✅ 主责 |
| 回顾分析 | ❌ 禁止 | ⚠️ 可选 | ✅ 主责 |

### 🚨 越界检测机制

**自动检测规则**:
- 修改 Plans.md 的 Task/内容/DoD 字段 → **违规**
- 修改 spec.md → **严重违规**
- 改变任务依赖关系 → **违规**
- 添加或删除任务 → **违规**

**违规处理**:
- 检测到违规操作时，警告并记录
- 如果影响规划完整性，建议重新规划
- 将违规记录到日志文件用于审计

### 📋 边界检查清单

每次执行实现后，自动检查：
- [ ] 没有修改任务定义（Task 名称、内容、DoD）
- [ ] 没有改变任务优先级或依赖关系
- [ ] 没有修改产品规格
- [ ] 代码修改符合任务 DoD
- [ ] 状态更新基于实际完成情况
- [ ] 发现的问题已正确记录或标记

**检查失败处理**: 如果检查失败，记录偏差并可能需要重新规划。

---

整合以下旧技能:

- `work` — Plans.md 任务的实现（范围自动判断）
- `impl` — 功能实现（基于任务）
- `breezing` — 团队全自动执行
- `parallel-workflows` — 并行工作流优化
- `ci` — CI 失败时的恢复

## Quick Reference

| 用户输入 | 模式 | 动作 |
|------------|--------|------|
| `/harness-work` | **auto** | 按任务数自动判定（参见下文） |
| `/harness-work all` | **auto** | 以自动模式执行所有未完成任务 |
| `/harness-work 3` | solo | 仅立即执行任务3 |
| `/harness-work --parallel 5` | parallel | 5工作器并行执行（强制） |
| `/harness-work --codex` | codex | 向 Codex CLI 委托（仅明确时） |
| `/harness-work --isolate-branch` | **branch-iso** | 在隔离分支执行，测试通过后合并 |
| Cursor host (adapter candidate) | cursor | Task/subagent routing via `.cursor/AGENTS.md`; not auto-selected |
| `/harness-work --breezing` | breezing | 强制团队执行 |
| `/harness-work --auto-mode` | **智能推荐** | 基于任务特征智能推荐模式，高置信度自动应用 |
| `/harness-work 3 --plan roadmap` | solo | 从名为 Plans 的 `roadmap` 执行任务3 |

## Execution Mode Auto Selection（无标志时的自动判定）

没有明确的模式标志（`--parallel`, `--breezing`, `--codex`）时，
根据对象任务数自动选择最优模式:

| 对象任务数 | 自动选择模式 | 理由 |
|-------------|---------------|------|
| **1 件** | Solo | 开销最小。直接实现最快 |
| **2-3 件** | Parallel（Task tool） | Worker 分离的效益开始显现的阈值 |
| **4 件以上** | Breezing | Lead 协调 + Worker 并行 + Reviewer 独立的三者分离有效 |

### 规则

1. **明确标志始终覆盖自动模式**（`--parallel N` / `--breezing` / `--codex` 与任务数无关强制执行）
2. **`--codex` 仅在明确时触发**。因存在 Codex CLI 未安装的环境，不自动选择
3. `--codex` 可与其他模式组合: `--codex --breezing` → Codex + Breezing

## 智能执行模式推荐系统（Smart Mode Recommendation）

**Purpose**: 通过分析任务特征自动推荐最优执行模式，消除用户在 Solo/Parallel/Breezing 选择上的困惑。

### 核心价值

- **智能分析**: 基于任务数量、复杂度、依赖关系、审查需求四维度自动评估
- **透明决策**: 提供推荐理由和置信度评分，让用户理解选择依据
- **自动确认**: 高置信度推荐自动应用，减少决策负担
- **学习能力**: 记录用户反馈，持续优化推荐算法

### 工作原理（三步流水线）

```
任务描述 + 变更文件
       ↓
  [1. TaskAnalyzer] → TaskCharacteristics (taskCount, complexity, dependencies, reviewNeed)
       ↓
  [2. ModeScorer]   → ModeScores (soloScore, parallelScore, breezingScore)
       ↓
  [3. RecommendationGenerator] → ModeRecommendation (mode, confidence, reason, alternatives)
```

#### Step 1: 任务特征分析（TaskAnalyzer）

从任务描述和变更文件中提取四维特征：

| 特征 | 枚举值 | 说明 |
|------|--------|------|
| taskCount | int | 任务数量 |
| complexity | SIMPLE / MODERATE / COMPLEX / VERY_COMPLEX | 复杂度等级 |
| dependencies | INDEPENDENT / SEQUENTIAL / MIXED | 依赖关系类型 |
| reviewNeed | NONE / OPTIONAL / REQUIRED | 审查需求 |

#### Step 2: 加权评分（ModeScorer）

对三种模式分别计算匹配度评分（0.0-1.0），默认权重：

| 维度 | 权重 | 说明 |
|------|------|------|
| 任务数量 | 35% | 任务数越多，越倾向并行/团队模式 |
| 复杂度 | 35% | 复杂度越高，越倾向团队模式 |
| 依赖关系 | 20% | 依赖越复杂，越需要协调 |
| 审查需求 | 10% | 审查需求越高，越需要独立 Reviewer |

各模式的最优特征：
- **SOLO**: 1 任务、低复杂度、独立、无需审查
- **PARALLEL**: 2-4 任务、中等复杂度、独立、可选审查
- **BREEZING**: 6+ 任务、高复杂度、混合依赖、必须审查

#### Step 3: 推荐生成（RecommendationGenerator）

基于评分差异计算置信度：

| 评分差距 | 置信度范围 | 说明 |
|----------|-----------|------|
| ≥0.4 | 0.85-1.0 | 差异很大，高置信度 |
| ≥0.2 | 0.70-0.85 | 差异明显，中高置信度 |
| ≥0.05 | 0.55-0.70 | 差异较小，中等置信度 |
| <0.05 | 0.40-0.55 | 差异很小，低置信度 |

### 置信度与自动确认机制

| 置信度 | 行为 | 用户交互 |
|--------|------|----------|
| **≥80%** | 自动应用 | 显示推荐，自动执行 |
| **70%-80%** | 推荐确认 | 显示推荐，用户确认 [Y/n] |
| **<70%** | 多选项 | 显示选择菜单供用户决定 |

### 复杂度评分规则

TaskAnalyzer 从任务描述和变更文件中计算复杂度分数（0-10）：

| 因素 | 条件 | 分数 |
|------|------|------|
| 任务数量 | >=2 个 | +1 |
| 任务数量 | >=4 个 | +2 |
| 文件数量 | >=3 个 | +1 |
| 文件数量 | >=5 个 | +2 |
| 核心目录 | 路径包含 `core/` | +3 |
| 安全目录 | 路径包含 `security/`、`guardrails/` | +3 |
| 架构/迁移 | 包含 architecture、migration 关键字 | +8 |
| 超高风险 | 包含 refactor、database、schema | +5 |
| 高风险 | 包含 design、security | +2 |
| 失败历史 | agent memory 中有失败记录 | +3 |
| 显式指定 | `effort: low/medium/high/xhigh` | 直接映射 |

复杂度等级映射：>=7 VERY_COMPLEX, >=3 COMPLEX, >=1 MODERATE, else SIMPLE

### 用户交互（ModeAdvisor）

ModeAdvisor 提供丰富的交互体验：

```
╔═══════════════════════════════════════════════════════════════════╗
║            🤖 智能执行模式推荐                                    ║
╚═══════════════════════════════════════════════════════════════════╝

📊 推荐模式: BREEZING
🎯 置信度:   85.0% (0.85)

💡 推荐理由:
   推荐使用 BREEZING 模式执行，因为有 6 个任务需要团队协作，
   任务较为复杂，需要严格的代码审查。
   BREEZING 模式通过 Lead/Worker/Reviewer 角色分离，
   可以有效协调复杂任务，保证代码质量。

⭐ 强烈推荐 - 该模式最适合当前任务特征

🔄 备选方案: [PARALLEL]
```

置信度强度指示：
- ⭐ 强烈推荐（≥85%）— 高度匹配
- ✅ 推荐（70%-85%）— 较好选择
- 💭 建议（50%-70%）— 可考虑
- 🤔 可选（<50%）— 多种模式可行

### 学习与缓存

#### 推荐缓存（RecommendationCache）

- LRU 缓存，默认 100 条
- 以 (tasks, files) 为 key 缓存推荐结果
- 避免重复计算，提升响应速度

#### 自适应学习（AdaptiveLearner）

- 记录用户的接受/拒绝反馈
- 学习率衰减：<10 条反馈 0.3，<50 条 0.1，50+ 条 0.05
- `isWellLearned` 标记：50+ 条反馈时生效
- 存储路径：`.claude/mode-learning/user-feedback.dat`

#### 权重优化（WeightOptimizer）

- 根据用户偏好自动调整评分权重
- 偏好检测：5+ 次选择且某模式占比 >60% 时识别为强偏好
- 权重向用户偏好模式倾斜后归一化

### 配置方式

#### 权重自定义（代码方式）

```java
ScoringWeights weights = ScoringWeights.builder()
    .taskCountWeight(0.40)      // 提高任务数量权重
    .complexityWeight(0.30)     // 调整复杂度权重
    .dependencyWeight(0.20)     // 保持依赖关系权重
    .reviewRequirementWeight(0.10) // 保持审查需求权重
    .build();

ModeRecommender recommender = new ModeRecommender(weights);
```

#### 缓存配置

```java
RecommendationCache cache = new RecommendationCache(200); // 200 条缓存
```

#### 学习数据存储

```java
LearningPersistence persistence = new LearningPersistence(".claude/mode-learning");
```

### API 使用

```java
// 基本推荐
ModeRecommender recommender = new ModeRecommender();
ModeRecommendation rec = recommender.recommend(tasks, files);

// 带失败历史
ModeRecommendation rec = recommender.recommend(tasks, files, true);

// 完整 API（含显式 effort）
ModeRecommendation rec = recommender.recommend(tasks, files, false, "high");

// 带调试信息
RecommendationResult result = recommender.recommendWithDebugInfo(tasks, files);
// result.recommendation() — 推荐结果
// result.characteristics() — 任务特征
// result.scores() — 评分详情

// 快速推荐（仅返回模式）
ExecutionMode mode = recommender.quickRecommend(tasks, files);

// 自动确认判断
if (recommender.shouldAutoApply(rec)) {
    // 自动应用推荐
} else if (recommender.requiresUserConfirmation(rec)) {
    // 需要用户确认
}
```

## Branch Isolation Mode（分支隔离模式）

**Purpose**: 在任务执行前自动创建 git 分支隔离，确保主分支稳定性。测试通过后再合并回主分支。

### 核心流程

```
1. 检查当前分支（是否在主分支）
2. 自动创建 feature 分支（使用 git worktree）
3. 在隔离分支中执行任务实现
4. 运行测试验证
5. 测试通过：合并回主分支，清理 feature 分支
6. 测试失败：保留 feature 分支用于调试
```

### 基础用法

```bash
# 单个任务，自动创建分支并合并
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

### 适用场景

- **团队协作**: 使用 `--keep-branch` 保留分支用于 Code Review
- **CI/CD**: 使用 `--no-merge` 让 CI 控制合并时机
- **调试**: 测试失败时自动保留分支现场
- **安全**: 避免直接在主分支上工作

### 详细文档

分支隔离模式的完整实现细节、错误处理、状态文件格式、配置支持等详见：
**[references/branch-isolation.md](${CLAUDE_SKILL_DIR}/references/branch-isolation.md)**

## Execution Backend Selection（实现后端选择）

后端（哪个运行时**实现**）与执行模式（拓扑: solo / parallel / breezing）正交。

| backend | 实现承担者 | 委托命令 |
|---------|------------|------------|
| `claude`（默认） | Task subagent（`agents/worker.md`） | 用 Agent tool spawn worker |

Codex 调用的治理详情（禁止事项、verdict 映射等）
参见 [references/codex-cli-only.md](${CLAUDE_SKILL_DIR}/references/codex-cli-only.md)。

run 开始时用 resolver 仅解析一次。不得直接读取 `HARNESS_IMPL_BACKEND` env 来决定后端:

```bash
```

precedence（从高到低）: 明确标志（`--backend` / `--cursor` / `--codex`） > env > project file > user file > 默认 `claude`。项目设置覆盖用户范围。

### Backend 默认（`claude` 是意图的默认，警告仅在无效值 fallback 时）

默认 backend 是 `claude`（Native subagent）。resolver 的未设定 fallback 也是 `claude`，正常解析为 `claude` 时不输出警告（2026-07-24 operator 裁定。格式与 `breezing` 的 Narration Rules「Backend 默认和 per-run 的扁平判断」相同，cross-ref: `skills/breezing/SKILL.md`）。

- ⚠️ 警告仅在 resolver 输出 **无效值 fallback** 的 stderr 警告时输出。banner 后立即 1 行，同一 run 内不重复
- Lead 可以根据作业内容、量 per-run 扁平选择 backend。选择时使用对 resolver 的明确 override（`--backend <v>` / `--codex` / `--cursor`）
- `composer` / `作曲器` / `composer 2.5` 等自然语言表现作为 `--cursor` 相同 intent 处理，向 resolver 明示 override 传递 `--backend cursor`（自然语言 backend trigger）

后端是 **role-scoped**: 遵循已解决后端的仅是实现（worker）角色。Reviewer / Advisor 总是固定为 brain（`--host claude`）（不向 primary reviewer routing cursor/codex）。唯一例外是 **fresh-context advisory pre-review**: 不与会话状态共享生成 diff 的 session 的 cursor `review` tier 可以输出 advisory findings，primary verdict（`APPROVE | REQUEST_CHANGES`）仅由 brain 输出。

```bash
```

后端为 `codex` / `cursor` 时，Lead 不会 spawn Worker agent，而是直接调用 companion（无 Worker 介入的拓扑）。跳过 self_review 门控，Lead 的 diff 审查成为唯一的质量门控。在委托前输出 cursor backend banner，在 cherry-pick 前通过两道 contract grep 门控（`test-support-claim-wording.sh` / `check-consistency.sh` / `validate-plugin.sh`）。Mode 1 的 Producer → Sub-Lead → Composer 层级、review→iterate 循环详情参见
[references/backend-selection.md](${CLAUDE_SKILL_DIR}/references/backend-selection.md)。

## 选项

| 选项 | 说明 | 默认值 |
|----------|------|----------|
| `all` | 以全部未完成为对象 | - |
| `N` or `N-M` | 指定任务编号/范围 | - |
| `--parallel N` | 并行 Worker 数量 | auto |
| `--sequential` | 强制串行执行 | - |
| `--codex` | 委托给 Codex CLI 实现（仅在明确指定时，不自动选择） | false |
| `--backend <claude\|codex\|cursor>` | 明确选择后端（仅应用于 worker 角色，优先级最高） | claude |
| `--cursor` | cursor 后端（同 `--codex`，仅在明确指定时。因存在未安装 cursor-agent 的环境，不自动选择） | false |
| `--plan NAME` | 使用 `plans/manifest.json` 中的指定 plan | active/default |
| `--no-commit` | 抑制自动提交 | false |
| `--resume <id\|latest>` | 恢复上一次会话。在长时间中断后推荐与 `/recap` 一起使用 | - |
| `--breezing` | Lead/Worker/Reviewer 团队执行 | false |
| `--no-tdd` | 跳过 TDD 阶段 | false |
| `--tdd-bypass` | 仅在紧急情况下绕过 TDD 强制。将 `HARNESS_TDD_BYPASS_REASON` 或明确理由保留在 audit 中 | false |
| `--no-simplify` | 跳过 Auto-Refinement | false |
| `--auto-mode` | 启用智能执行模式推荐。基于任务特征自动推荐 Solo/Parallel/Breezing，高置信度时自动应用。同时也是 CC Auto Mode rollout 的 opt-in 标志 | false |
| `--isolate-branch` | 启用分支隔离模式。任务执行前自动创建 feature 分支，测试通过后合并回主分支 | false |
| `--no-merge` | 与 `--isolate-branch` 配合使用，完成测试后保留分支不自动合并 | false |
| `--branch-name <name>` | 自定义分支名称（默认：feature/task-<id>-<timestamp>） | auto |
| `--keep-branch` | 完成后保留 feature 分支（用于人工审查或创建 PR） | false |

## Progressive Disclosure

首先在此正文中确认入口、自动选择、停止条件。详细内容仅在必要时阅读。

| 详细 | 参照 |
|---|---|
| Solo / Breezing 的 1-17 步骤完整版、Phase A/B/C 完整版 | `references/execution-modes.md` |
| Backend role-scoped 限制、非 claude 拓扑、Mode 1 层级、review→iterate | `references/backend-selection.md` |
| **分支隔离模式、git worktree 管理、自动合并流程** | `references/branch-isolation.md` |
| Codex review、Reviewer fallback、verdict mapping、修正循环 | `references/review-loop.md` |
| Sprint Contract 字段一览、PR Closeout | `references/sprint-contract.md` |
| effort tier 的多要素得分详情 | `references/effort-routing.md` |
| Solo / Breezing 完成报告的生成 | `references/completion-report.md` |
| 测试/CI 失败时的重新票决命令 | `references/failure-reticketing.md` |
| 规格权威版本检查的基准 | `docs/harness-project/plans/spec-ssot.md` |
| **智能执行模式推荐、评分算法、学习机制** | 本文件「智能执行模式推荐系统」章节 |
| 智能推荐的设计规格 | `docs/harness-project/superpowers/specs/2026-08-11-mode-recommendation-docs-design.md` |

### 重要停止条件

- `Plans.md` 为旧格式无法读取 DoD / Depends / Status 时停止。
- 规格影响实现判断但找不到 project spec SSOT 时，先创建/更新规格权威版本后再实现。
- sprint-contract 为 required 但未 ready 时不进入实现。
- 残留 critical / major review findings 时不完成。
- 不能以减弱测试、skip 测试、将期待值配合实现放宽的形式解决。
- helper script 从 `${HARNESS_PLUGIN_ROOT}/scripts/` 而非 host project 的 `scripts/` 调用。
- 有多个 Plans.md 时，不在 1 run 中切换 plan。必要时明确 `--plan NAME` 并启动新的 run。

> **Token Optimization (v2.1.69+)**: 在不伴随 git 操作的轻量任务中
> 可以在 plugin settings 中启用 `includeGitInstructions: false` 来减少 prompt token。

> **Prompt Cache (CC 2.1.108+)**: 在较长的实现或频繁使用 `--resume` 的工作中
> 优先使用 `ENABLE_PROMPT_CACHING_1H=1`。

## 范围对话框（无参数时）

```
/harness-work
执行到什么程度？
1) 下一个任务: Plans.md 的下一个未完成任务 → Solo 执行
2) 全部（推荐）: 完成所有剩余任务 → 根据任务数自动选择模式
3) 指定编号: 输入任务编号（例: 3, 5-7）→ 根据件数自动选择模式
```

有参数时立即执行（跳过交互）:
- `/harness-work all` → 全部任务，自动模式选择
- `/harness-work 3-6` → 4 件所以自动选择 Breezing

## Effort 级别控制（Opus 4.8 / v2.1.111+）

effort 是选择模型推理强度的正式旋钮。分为 `low(○)/medium(◐)/high(●)/xhigh` 4 个级别，
可以用 `/effort auto` 重置为默认值。从复杂度得分（文件数·目标目录·关键字·失败历史·明确指定之合计）
决定 tier，不采用向 spawn prompt 注入 free-text marker（旧 `ultrathink`）的方式。

| 得分 | code-risk（包含 core/guardrails/security/architecture/migration） | effort tier |
|--------|-----------------------------------|-------------|
| 0-2 | 不问 | `medium`（Worker frontmatter 默认） |
| ≥ 3 | 无 | `high` |
| ≥ 3 | 有 | `xhigh` |

在 breezing 模式下也应用相同的逻辑（harness-work 统一管理）。得分详情·lever 详情参见
[references/effort-routing.md](${CLAUDE_SKILL_DIR}/references/effort-routing.md)。

## 执行模式详情

### Harness helper script root

Harness 捆绑的 helper script 必须从 plugin bundle root 调用，而不是工作目标项目的 `scripts/`。

```bash
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"
if [ -z "$HARNESS_PLUGIN_ROOT" ] && [ -n "${CLAUDE_SKILL_DIR:-}" ]; then
  probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"
  while [ "$probe" != "/" ] && [ ! -d "$probe/scripts" ]; do
    probe="$(cd "$probe/.." && pwd)"
  done
  [ -d "$probe/scripts" ] && HARNESS_PLUGIN_ROOT="$probe"
fi
```

以后的 `node "${HARNESS_PLUGIN_ROOT}/scripts/..."` / `bash "${HARNESS_PLUGIN_ROOT}/scripts/..."` 以此已解析的 root 为前提。

### Auto Review Integration (v2.1.0+)

从 v2.1.0 开始，harness-work 使用 harness-review 的 --auto 模式进行代码审查，实现职责清晰分离。

**调用函数:**

```python
def call_harness_review_auto(base_ref: str, worktree_path: str, mode: str = "strict") -> dict:
    """
    调用 harness-review --auto 模式进行自动代码审查

    Args:
        base_ref: 基准 commit SHA 或分支名
        worktree_path: 工作树路径
        mode: 审查模式 (strict|lenient)

    Returns:
        包含审查结果的字典
        {
            "success": bool,
            "result": {
                "verdict": "APPROVE|REQUEST_CHANGES",
                "findings": [...],
                "summary": "...",
                "performance": {...}
            },
            "stdout": str,
            "stderr": str
        }
    """
    import subprocess
    import json
    import os

    # 确定脚本路径（项目统一的 scripts/ 目录）
    auto_review_script = os.path.join(
        HARNESS_PLUGIN_ROOT,
        "scripts", "review", "auto-review.sh"
    )

    # 准备输出文件
    output_file = f"/tmp/harness-review-{os.getpid()}.json"

    # 构建命令
    cmd = [
        auto_review_script,
        "--auto",
        "--base-ref", base_ref,
        "--output", output_file,
        "--mode", mode
    ]

    try:
        # 执行审查
        result = subprocess.run(
            cmd,
            cwd=worktree_path,
            timeout=30,
            capture_output=True,
            text=True
        )

        # 读取结果
        if os.path.exists(output_file):
            with open(output_file, 'r') as f:
                review_result = json.load(f)

            # 清理临时文件
            os.remove(output_file)

            return {
                "success": True,
                "result": review_result,
                "stdout": result.stdout,
                "stderr": result.stderr
            }
        else:
            return {
                "success": False,
                "error": "输出文件不存在",
                "stdout": result.stdout,
                "stderr": result.stderr
            }

    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error": "审查超时（超过 30 秒）"
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def lightweight_review_fallback(base_ref: str, worktree_path: str) -> str:
    """
    轻量级审查降级方案
    当 harness-review --auto 不可用时使用

    Args:
        base_ref: 基准 commit SHA
        worktree_path: 工作树路径

    Returns:
        "APPROVE" 或 "REQUEST_CHANGES"
    """
    import subprocess

    try:
        # 基础检查：确保代码可以编译/运行
        result = subprocess.run(
            ["git", "-C", worktree_path, "diff", "--name-only", f"{base_ref}..HEAD"],
            capture_output=True,
            text=True,
            timeout=10
        )

        changed_files = result.stdout.strip().split('\n') if result.stdout.strip() else []

        # 如果有变更文件，进行基本检查
        if changed_files:
            # 检查是否有明显的错误（如编译错误）
            # 这里可以添加更多基础检查
            pass

        # 保守策略：如果有任何变更，要求人工审查
        if changed_files:
            return "REQUEST_CHANGES"
        else:
            return "APPROVE"

    except Exception:
        # 出错时采用保守策略
        return "REQUEST_CHANGES"

class AutoFixReviewLoop:
    """自动修复审查循环 (v2.1.1+)"""

    def __init__(self, max_iterations: int = 3):
        """
        初始化自动修复循环

        Args:
            max_iterations: 最大修复尝试次数
        """
        self.max_iterations = max_iterations
        self.iteration_count = 0

    def auto_fix_issues(self, issues: list, worktree_path: str, worker_context: dict) -> dict:
        """
        自动修复代码问题

        Args:
            issues: 需要修复的问题列表
            worktree_path: 工作树路径
            worker_context: Worker 上下文

        Returns:
            修复结果字典
        """
        import subprocess

        fixed_count = 0
        failed_count = 0

        for issue in issues:
            try:
                fix_command = self.generate_fix_command(issue)
                result = subprocess.run(
                    fix_command,
                    cwd=worktree_path,
                    capture_output=True,
                    text=True,
                    timeout=10
                )

                if result.returncode == 0:
                    print(f"✅ 修复: {issue['file']}:{issue['line']} - {issue['message']}")
                    fixed_count += 1
                else:
                    print(f"⚠️  修复失败: {issue['file']}:{issue['line']}")
                    failed_count += 1

            except Exception as e:
                print(f"❌ 修复异常: {issue['file']}:{issue['line']} - {e}")
                failed_count += 1

        return {
            "success": fixed_count > 0,
            "fixed_count": fixed_count,
            "failed_count": failed_count
        }

    def generate_fix_command(self, issue: dict) -> list:
        """根据问题生成修复命令"""
        file_path = issue["file"]
        line_number = issue["line"]
        rule = issue.get("rule", "")
        suggestion = issue.get("suggestion", "")

        # 根据不同的规则生成不同的修复命令
        if rule == "no-system-out":
            return [
                "sed", "-i",
                f"{line_number}s/System.out.println/LOGGER.info/",
                file_path
            ]
        elif rule == "no-print-statements":
            return [
                "sed", "-i",
                f"{line_number}s/print(/logging.info/",
                file_path
            ]
        else:
            # 通用修复策略
            return [
                "sed", "-i",
                f"{line_number}i// TODO: 需要修复: {issue['message']}",
                file_path
            ]

    def commit_fixes(self, worktree_path: str, commit_message: str) -> dict:
        """提交修复"""
        import subprocess

        try:
            subprocess.run(
                ["git", "add", "-A"],
                cwd=worktree_path,
                check=True,
                capture_output=True
            )

            result = subprocess.run(
                ["git", "diff", "--staged", "--name-only"],
                cwd=worktree_path,
                capture_output=True,
                text=True
            )

            if not result.stdout.strip():
                return {
                    "success": False,
                    "error": "没有需要提交的变更"
                }

            subprocess.run(
                ["git", "commit", "-m", commit_message],
                cwd=worktree_path,
                check=True,
                capture_output=True
            )

            return {
                "success": True,
                "commit_message": commit_message
            }

        except subprocess.CalledProcessError as e:
            return {
                "success": False,
                "error": f"Git 命令执行失败: {e}"
            }
        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }
```

### 强制审查集成 (v2.3.0+)

**Purpose**: 确保所有 harness-work 执行都强制经过代码审查，实现质量门控。

#### 核心原则

1. **强制审查**: 所有执行模式（Solo/Parallel/Breezing）完成后必须通过审查
2. **统一标准**: 所有后端（claude/cursor/codex）使用相同的审查流程
3. **完成门控**: 审查不通过时阻止任务标记为完成
4. **自动修复**: 尝试自动修复审查问题，达到最大重试次数后升级到用户

#### 集成点

**Solo 模式集成**:
```python
# 在任务完成后，标记 cc:完了 之前
def complete_task_solo(task_id, worktree_path):
    # 1. 执行任务实现
    implementation_result = implement_task(task_id)

    # 2. 强制代码审查
    review_result = forced_review_gate(
        base_ref=implementation_result.base_commit,
        worktree_path=worktree_path,
        mode="strict"
    )

    # 3. 审查不通过时阻止完成
    if review_result["verdict"] != "APPROVE":
        handle_review_failure(review_result, task_id)
        return  # 不标记为完成

    # 4. 审查通过后才标记完成
    mark_task_completed(task_id)
```

**Parallel 模式集成**:
```python
# 在每个任务完成后，汇总结果之前
def complete_task_parallel(task_id, worktree_path):
    # 每个任务独立审查
    review_result = forced_review_gate(
        base_ref=task_base_commit,
        worktree_path=worktree_path,
        mode="strict"
    )

    # 记录审查结果到任务状态
    task_statuses[task_id]["review"] = review_result

    # 汇总时检查所有任务是否都通过审查
    if all(t["review"]["verdict"] == "APPROVE" for t in task_statuses.values()):
        aggregate_and_complete()
```

**Breezing 模式集成**:
```python
# 在每个 Worker 任务完成后，cherry-pick 之前
def handle_worker_completion(worker_result):
    # Worker 完成后立即审查
    review_result = forced_review_gate(
        base_ref=worker_result.baseCommit,
        worktree_path=worker_result.worktreePath,
        mode="strict"
    )

    # 只有审查通过才 cherry-pick 到 trunk
    if review_result["verdict"] == "APPROVE":
        git_cherry_pick_to_trunk(worker_result)
    else:
        # 审查失败，进入修复循环
        enter_review_fix_loop(worker_result, review_result)
```

#### 强制审查门控函数

```python
def forced_review_gate(base_ref: str, worktree_path: str, mode: str = "strict") -> dict:
    """
    强制代码审查门控 - 所有完成路径必须经过此门控

    Args:
        base_ref: 基准 commit SHA
        worktree_path: 工作树路径
        mode: 审查模式 (strict|lenient)

    Returns:
        审查结果字典，必须包含 verdict 字段
    """
    # 检查是否启用了跳过审查（仅用于紧急情况）
    if should_skip_review():
        logger.warning("⚠️  审查被跳过（紧急模式）")
        return {
            "verdict": "APPROVE",
            "skip_reason": "emergency_skip",
            "findings": []
        }

    # 调用 harness-review --auto
    verdict_result = call_harness_review_auto(
        base_ref=base_ref,
        worktree_path=worktree_path,
        mode=mode
    )

    if not verdict_result["success"]:
        # 如果自动审查失败，使用降级方案
        logger.warning(f"harness-review --auto 失败: {verdict_result.get('error')}")
        return {
            "verdict": "REQUEST_CHANGES",
            "error": verdict_result.get("error"),
            "findings": []
        }

    return verdict_result["result"]

def should_skip_review() -> bool:
    """
    检查是否应该跳过审查（仅用于紧急情况）

    跳过条件（必须全部满足）：
    1. 环境变量 HARNESS_SKIP_REVIEW=true
    2. 或 harness.toml 中 skip_review=true
    3. 或用户显式确认跳过
    """
    import os

    # 检查环境变量
    if os.getenv("HARNESS_SKIP_REVIEW", "false").lower() == "true":
        return True

    # 检查配置文件
    try:
        import toml
        config = toml.load("harness.toml")
        if config.get("review", {}).get("skip_review", False):
            return True
    except:
        pass

    return False
```

#### 配置支持

**环境变量控制**:
```bash
# 仅在紧急情况下使用
export HARNESS_SKIP_REVIEW=true
export HARNESS_REVIEW_MODE=lenient  # strict | lenient
export HARNESS_MAX_REVIEW_ITERATIONS=5  # 最大审查重试次数
```

**harness.toml 配置**:
```toml
[review]
# 强制审查配置
enabled = true  # 是否启用强制审查（默认: true）
mode = "strict"  # 审查模式: strict | lenient
skip_review = false  # 紧急跳过审查（不推荐）
max_iterations = 3  # 最大审查重试次数

# 自动修复配置
auto_fix = true  # 是否启用自动修复（默认: true）
fix_critical_only = true  # 仅修复 critical/major 问题

# 审查失败处理
on_failure = "escalate"  # escalate | continue | stop
escalation_message = "代码审查未通过，需要人工介入"
```

#### 审查失败处理流程

```python
def handle_review_failure(review_result, task_id, worktree_path):
    """
    处理审查失败的情况
    """
    findings = review_result.get("findings", [])

    # 1. 输出审查结果
    print_review_findings(findings)

    # 2. 尝试自动修复
    if should_auto_fix(review_result):
        fix_result = attempt_auto_fix(findings, worktree_path)

        if fix_result["success"]:
            # 重新审查
            new_review = forced_review_gate(
                base_ref=get_current_base(),
                worktree_path=worktree_path,
                mode="strict"
            )

            if new_review["verdict"] == "APPROVE":
                print("✅ 自动修复后审查通过")
                return True

    # 3. 自动修复失败，升级到用户
    print(f"❌ 任务 {task_id} 审查未通过，需要人工介入")
    escalate_to_user(review_result, task_id)

    return False

def print_review_findings(findings):
    """输出审查结果"""
    print(f"\n📋 审查发现 {len(findings)} 个问题:\n")

    for finding in findings:
        severity = finding.get("severity", "unknown")
        file_path = finding.get("file", "unknown")
        line = finding.get("line", 0)
        message = finding.get("message", "")

        severity_emoji = {
            "critical": "🔴",
            "major": "🟠",
            "minor": "🟡",
            "recommendation": "💡"
        }.get(severity, "⚪")

        print(f"{severity_emoji} [{severity.upper()}] {file_path}:{line}")
        print(f"   {message}\n")
```

#### 完成报告集成

在所有模式的完成报告中，包含审查结果：

```markdown
## 完成报告

### 审查结果
- **Verdict**: ✅ APPROVE / ❌ REQUEST_CHANGES
- **审查模式**: strict / lenient
- **发现的问题**: critical(0) major(0) minor(3) recommendation(5)
- **审查时间**: 2024-08-13T10:30:00Z
- **审查性能**: duration_ms=1234, files_reviewed=5

### 问题详情（如有）
[详细的审查发现列表]
```

#### 监控和日志

强制审查集成会记录详细日志：

```python
# 审查调用日志
logger.info(f"Forced review triggered: task_id={task_id}, mode={mode}")

# 审查结果日志
logger.info(f"Review verdict: {verdict}, findings={len(findings)}")

# 自动修复日志
logger.info(f"Auto-fix attempted: fixed={fixed_count}, failed={failed_count}")

# 升级日志
logger.warning(f"Review escalation: task_id={task_id}, reason={reason}")
```

#### 相关文档

- **审查标准**: `skills/harness-review/references/code-review.md`
- **多语言标准**: `skills/harness-review/references/code-standards/`
- **治理规则**: `skills/harness-review/references/governance.md`

### Backend-resolved executor path (Solo / Parallel / Breezing)

Solo / Parallel / Breezing 从相同的 resolver result 选择实现 executor。
`harness-work 3 --cursor` 或 resolver 输出为 `cursor` 的 run（包含通过 project / user file 的 default），
即使是 1 件任务也不能 fall through 到 local Read/Write/Edit/Bash。

```
resolver_backend_arg = ""
if explicit_backend_value in ["claude", "codex", "cursor"]:
    resolver_backend_arg = "--backend {explicit_backend_value}"
if explicit_flag == "--cursor":
    backend = "cursor"
if explicit_flag == "--codex":
    backend = "codex"

if topology in ["solo", "parallel"] and backend in ["cursor", "codex"]:
    BASE_REF = git("rev-parse", "HEAD")
    WT_ID = "{task.number}-$(date +%Y%m%d-%H%M%S)-$$"
    worktree_path = ".claude/worktrees/{backend}-{WT_ID}"
    worktree_branch = "{backend}-work/{WT_ID}"
    bash("mkdir -p .claude/worktrees && git worktree add -b {worktree_branch} {worktree_path} {BASE_REF}")
    companion_prompt = "{task prompt}\n\nAfter making changes, create exactly one git commit in this worktree before returning."
    if backend == "cursor":
    else:
        companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
    latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if backend == "cursor" and git("-C", worktree_path, "status", "--porcelain") != "":
        git("-C", worktree_path, "add", "-A")
        git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: delegated change")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if latest_commit == BASE_REF:
        raise EscalationError("{backend} companion produced no commit")
    worker_result = {type: "companion-result.v1", baseCommit: BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: worktree_branch, files_changed: git("-C", worktree_path, "diff", "--name-only", "{BASE_REF}..HEAD"), summary: companion_output}
    enter_non_claude_companion_review_loop(worker_result)
else:
    run_native_solo_or_parallel()

def enter_non_claude_companion_review_loop(worker_result):
    # companion-result.v1 has no worker_id and no worker_result.self_review.
    # Do not use the Worker-only SendMessage/self_review loop for cursor/codex.
    latest_commit = worker_result.commit

    # 调用 harness-review --auto 模式进行自动审查
    verdict_result = call_harness_review_auto(
        base_ref=worker_result.baseCommit,
        worktree_path=worker_result.worktreePath,
        mode="strict"
    )

    if not verdict_result["success"]:
        # 如果自动审查失败，降级到基础检查
        logger.warning(f"harness-review --auto 失败，使用 fallback: {verdict_result.get('error')}")
        verdict = lightweight_review_fallback(
            base_ref=worker_result.baseCommit,
            worktree_path=worker_result.worktreePath
        )
        findings = []
    else:
        verdict = verdict_result["result"]["verdict"]
        findings = verdict_result["result"].get("findings", [])

    review_count = 0
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3

    # 集成自动修复循环
    auto_fix_loop = AutoFixReviewLoop(max_iterations=MAX_REVIEWS)

    while review_count < MAX_REVIEWS:
        # 调用自动审查
        verdict_result = call_harness_review_auto(
            base_ref=BASE_REF,
            worktree_path=worker_result.worktreePath,
            mode="strict"
        )

        if not verdict_result["success"]:
            # 如果自动审查失败，降级到基础检查
            logger.warning(f"harness-review --auto 失败，使用 fallback: {verdict_result.get('error')}")
            verdict = lightweight_review_fallback(
                base_ref=BASE_REF,
                worktree_path=worker_result.worktreePath
            )
            # 如果 fallback 也不通过，直接跳过循环
            if verdict == "REQUEST_CHANGES":
                logger.warning("Fallback 审查也不通过，跳过自动修复循环")
                break
        else:
            verdict = verdict_result["result"]["verdict"]
            findings = verdict_result["result"].get("findings", [])

        # 如果审查通过，退出循环
        if verdict == "APPROVE":
            print("✅ 审查通过，继续流程")
            break

        # 审查不通过，执行自动修复循环
        print(f"❌ 审查未通过 (第 {review_count + 1} 次)")

        # 分析问题并自动修复
        critical_major_issues = [
            f for f in findings
            if f.get("severity") in ["critical", "major"]
        ]

        if not critical_major_issues:
            print("⚠️  没有 critical/major 问题，可能是判定问题")
            break

        print(f"🔧 发现 {len(critical_major_issues)} 个需要修复的问题")

        # 调用自动修复
        fix_result = auto_fix_loop.auto_fix_issues(
            critical_major_issues,
            worker_result.worktreePath,
            worker_context
        )

        if not fix_result["success"]:
            print(f"❌ 自动修复失败: {fix_result.get('error')}")
            break

        # 提交修复
        commit_result = auto_fix_loop.commit_fixes(
            worker_result.worktreePath,
            f"fix: 审查问题修复 (第 {review_count + 1} 次尝试)"
        )

        if not commit_result["success"]:
            print(f"❌ 提交修复失败: {commit_result.get('error')}")
            break

        review_count += 1
        print(f"✅ 修复已提交，准备第 {review_count + 1} 次审查...")

    # 检查是否达到最大重试次数
    if review_count >= MAX_REVIEWS and verdict != "APPROVE":
        print(f"⚠️  达到最大重试次数 ({MAX_REVIEWS})，需要人工介入")
        # 可以选择继续或停止
        user_input = input("继续尝试？(y/n): ")
        if user_input.lower() != 'y':
            raise EscalationError("达到最大重试次数，用户选择停止")
        previous_commit = latest_commit
        if backend == "cursor":
        else:
            companion_state_file = "{worker_result.worktreePath}/.claude/state/codex-primary-environment.json"
        latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
        if backend == "cursor" and git("-C", worker_result.worktreePath, "status", "--porcelain") != "":
            git("-C", worker_result.worktreePath, "add", "-A")
            git("-C", worker_result.worktreePath, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: review fix")
            latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
        if latest_commit == previous_commit:
            raise EscalationError("{backend} companion retry produced no new commit")
        worker_result.commit = latest_commit
        worker_result.summary = companion_output

        # 重新调用 harness-review --auto 进行审查
        verdict_result = call_harness_review_auto(
            base_ref=worker_result.baseCommit,
            worktree_path=worker_result.worktreePath,
            mode="strict"
        )

        if verdict_result["success"]:
            verdict = verdict_result["result"]["verdict"]
            findings = verdict_result["result"].get("findings", [])
        else:
            # 如果自动审查失败，降级处理
            logger.warning(f"harness-review --auto 重试失败，使用 fallback: {verdict_result.get('error')}")
            verdict = "REQUEST_CHANGES"  # 保守策略
            findings = []

        review_count++
    if verdict == "APPROVE":
        git cherry-pick --no-commit {worker_result.baseCommit}..{worker_result.commit}
```

Parallel 对每个 task 应用这个 resolver path。
backend=`cursor` / `codex` 时不使用 native Worker spawn，而是为每个 task 创建 isolated companion worktree，规范化为 `companion-result.v1` 后进入 non-Claude companion 专用的 range review / cherry-pick 循环。

### Solo 模式（1 件时的自动选择）

从 Plans.md 读入到 `cc:完了 [hash]` 的 1-17 步骤完整版参见
[references/execution-modes.md#solo-detailed-steps](${CLAUDE_SKILL_DIR}/references/execution-modes.md)。
要点：在**规格权威版本 preflight** 确认 spec SSOT 的有无并将 `spec_path` 传递给 Worker/Reviewer。应用 plan-time 事前确认，
将工作中因已声明事项引起的 `AskUserQuestion` 降为零。按 TDD Red → sprint-contract → 实现 → 审查循环 → commit → `cc:完了` 的顺序进行。

### Parallel 模式（2-3 件时的自动选择 / `--parallel N` 强制）

用 N 个 Worker 并行执行带 `[P]` 标记的任务。
用 `--parallel N` 明确指定时，无论任务数如何都使用此模式。
向同一文件的写入发生冲突时用 git worktree 分离。
各 task 的实现 executor 遵循 Backend-resolved executor path。
`--parallel N --cursor`、`--backend cursor`、或 resolver 输出为 `cursor` 时，Parallel 也不使用 native Worker spawn，而是使用每个 task 的 Cursor companion worktree。

### Codex 模式（`--codex` 仅在明确指定时）

通过官方插件 `codex-plugin-cc` 的 companion 将任务委托给 Codex CLI。

```bash
# 任务委托（可写入·worktree 分离）
BASE_REF="$(git rev-parse HEAD)"
WT_ID="codex-$(date +%Y%m%d-%H%M%S)-$$"
WORKTREE_PATH=".claude/worktrees/${WT_ID}"
git worktree add -b "codex-work/${WT_ID}" "$WORKTREE_PATH" "$BASE_REF"
HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
  "任务内容。请在此 worktree 中创建 exactly one git commit 后再完成。"

# 通过 stdin（面向较大的 prompt）
CODEX_PROMPT=$(mktemp /tmp/codex-prompt-XXXXXX.md)
# 写出任务内容
cat "$CODEX_PROMPT" | HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
rm -f "$CODEX_PROMPT"

# Lead review 后批准后取入 range
git -C "$WORKTREE_PATH" diff "$BASE_REF..HEAD"
WORKTREE_HEAD="$(git -C "$WORKTREE_PATH" rev-parse HEAD)"
git cherry-pick --no-commit "$BASE_REF..$WORKTREE_HEAD"
```

companion 通过 App Server Protocol 与 Codex 通信，
提供 Job 管理·thread resume·结构化输出。
验证结果，不满足质量标准时自己修正。

### Cursor 模式（adapter candidate，不自动选择）

Cursor host 中 `.cursor/AGENTS.md` 和 `.cursor-plugin/plugin.json` 是
bootstrap route。Cursor 保持 `candidate` — 禁止 supported claim。

- **Solo / Parallel**: Task tool 或 `.cursor/agents/worker.md` subagent
- **Breezing**: Worker 并行仅限 non-overlapping file groups;
  Reviewer / cherry-pick / Advisor 按 core 规则串行
- **Multitask / background agents**: 仅限 smoke target。不主张 Claude Agent Teams parity

```bash
bash tests/test-cursor-adapter-candidate.sh
```

Explicit Task/subagent `model` 优先于 routed default。

### Breezing 模式（4 件以上时自动选择 / `--breezing` 强制）

通过 Lead / Worker / Advisor / Reviewer 的角色分离进行团队执行。
Codex 假定使用 `spawn_agent`, `wait`, `send_input`, `resume_agent`, `close_agent`
的 native subagent orchestration。
Cursor 向 Task/subagent/background agents mapping，但
review/cherry-pick 的串行责任留在 core 侧（adapter smoke target）。

**权限策略**: 当前 shipped default 为 `bypassPermissions`。`--auto-mode` 是兼容亲会话的 opt-in rollout 标志。
不要在 `permissions.defaultMode` 或 agent frontmatter 的 `permissionMode` 中写入未文档化的 `autoMode` 值。

```
Lead (this agent)
├── Worker (task-worker agent) — 实现担当
├── Advisor (claude-code-harness:advisor) — 方针建议
└── Reviewer (code-reviewer agent) — 审查担当
```

Phase A（准备: 智能分支检测·Plans.md 读入·依赖解决·plan-preapproval 应用·effort 得分·sprint-contract 生成）→
Phase B（各任务: Worker spawn → 必要时 Advisor → self_review 门控 → 审查循环 → APPROVE 时向 trunk cherry-pick）→
Phase C（整合: commit log 集计·丰富完成报告·Plans.md 最终确认）的 3 段构成。
完整版 pseudocode（包含 B-1-B-7 的逐次顺序）参见
[references/execution-modes.md#breezing-phase-detail](${CLAUDE_SKILL_DIR}/references/execution-modes.md)。

### 智能分支隔离检测（Phase A 集成）

**Purpose**: 在任务执行开始前自动检测分支状态并应用适当的隔离策略

**触发时机**: Phase A 准备阶段，在 Plans.md 读入之后，任务执行之前

**检测逻辑**:
1. 自动检测当前分支类型（main/feature/worktree）
2. 根据分支类型和配置文件确定隔离策略（force/ask/skip）
3. 处理用户交互决策
4. 记录决策到状态文件

**策略类型**:
- `force`: 强制隔离（主分支保护）- 自动创建隔离分支，无需用户确认
- `ask`: 可选隔离（功能分支）- 提示用户选择是否隔离
- `skip`: 跳过隔离（已隔离状态）- 当前已在 worktree 中，无需额外隔离

**优先级**:
1. 显式 `--isolate-branch` 标志优先级最高
2. 智能检测次之（根据分支类型和配置）
3. 配置文件可覆盖默认策略

**配置支持**:
```json
// .claude/settings.json
{
  "branchIsolation": {
    "mainBranch": "force",     // 主分支策略：force/ask/skip
    "featureBranch": "ask"     // 功能分支策略：force/ask/skip
  }
}
```

**状态文件**: `.claude/state/branch-isolation-decision.json`
- 唯一的 v2 状态文件，Java `IsolationStateManager` 与 Shell 兼容入口共同使用
- 顶层必须包含 `version: "2.0"`、`schemaType: "branch-isolation-state-v2"` 和 `decisionHistory`
- 派生状态（例如 `isReadyForReset`）不写入 JSON；所有更新必须原子替换

**执行脚本**:
```bash
# 智能检测（推荐 - 自动根据分支类型决定）
bash scripts/branch-isolation/handle-isolation.sh --auto

# 强制特定策略
bash scripts/branch-isolation/handle-isolation.sh --strategy force

# 仅检测不执行
bash scripts/branch-isolation/detect-branch.sh --strategy
```

Phase A 的嵌入式执行入口为
`com.chachamaru.harness.isolation.integration.HarnessWorkIsolationIntegration#handlePhaseABranchIsolation`。
Shell 脚本是兼容适配入口，只能更新同一份 v2 状态文件，不得创建或写入其他分支隔离状态格式。

Phase A 调用约定（嵌入式宿主）:
```java
var isolation = new HarnessWorkIsolationIntegration();
var decision = isolation.handlePhaseABranchIsolation(taskId, taskTitle, worktreePath);
if (!decision.shouldProceed()) {
    return;
}
```

### Active task scope

在各任务的 preapproval preflight 之前，向目标 worktree 的
`.claude/state/active-task.json` 原子性写入 `{"phase":"<phase>","task":"<task>"}`。
Go guardrail 将此文件作为当前 scope 的权威版本读取。
任务结束时，无论成功、失败、停止的哪条路径都删除。环境变量
`HARNESS_ACTIVE_PHASE` / `HARNESS_ACTIVE_TASK` 仅用于
state 文件不存在的 host 的 fallback。

Parallel / Breezing 在每个任务的 worktree 中写入。不在多个任务间共享
同一 worktree 的 `active-task.json`。

### Advisor Protocol（所有模式通用）

Advisor 既不是"实现者"也不是"审查担当"。仅在迷惘时，作为执行者决定下一步的协商对象介入。

1. Worker 不增加 generic subagent，仅在必要时返回 `advisor-request.v1`
2. Lead 仅调用 advisor 一次
3. Advisor 返回 `PLAN` / `CORRECTION` / `STOP` 之一
4. Lead 将其 advice（`advisor-response.v1`）返回给同一 Worker 继续执行
5. Reviewer 只看最后的成果物。不对 advisor 的返答输出 APPROVE / REQUEST_CHANGES

solo 执行时亲会话自身兼任 Lead（自己实现，自己向 advisor 协商，最后交给独立审查）。
协商条件·budget 与 breezing 相同，每个任务的协商次数最多 3 次。`STOP` 在该处停止，上升到用户判断。review artifact 的门控不跳过。

### Sprint Contract 与 PR Closeout

`sprint-contract` 是将"此任务用什么来合格"机械可读化的契约文件（默认: `.claude/state/contracts/<task-id>.sprint-contract.json`，由 `generate-sprint-contract.js` 生成）。
`runtime_validation` 的 LSP/AST 工作流方针:

- If you grep the same symbol twice in the same session, switch to harness_ast_search.
- For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
- Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.

`spec_path` / `lane` / `stage` / `research_evidence` / `tdd_red_log` / `review_artifact` / `pr_closeout` 的字段规格，
以及 review APPROVE 后的 PR title/body 组装（`harness-pr-closeout.sh`，默认 `dry-run`）详情参见
[references/sprint-contract.md](${CLAUDE_SKILL_DIR}/references/sprint-contract.md)。

## CI 失败时的对应

CI 失败时:

1. 确认日志并特定错误
2. 实施修正
3. 同一原因失败 3 次后停止自动修正循环
4. 汇总失败日志·尝试的修正·残留论点并升级

## 失败任务的自动重新票决

任务完成后测试/CI 失败时，自动生成修正任务案，批准后反映到 Plans.md。
触发条件·生成格式·批准命令（`approve fix <task_id>` / `reject fix <task_id>`）详情参见
[references/failure-reticketing.md](${CLAUDE_SKILL_DIR}/references/failure-reticketing.md)。

## 审查循环

实现完成后自动执行的质量验证阶段。**全模式通用**（Solo / Parallel / Breezing）统一应用。
优先级（Codex exec → 内部 Reviewer agent fallback）、APPROVE / REQUEST_CHANGES 判定基准（仅 critical/major 影响 verdict）、
verdict 映射、修正循环（`MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3`）的完整版参见
[references/review-loop.md](${CLAUDE_SKILL_DIR}/references/review-loop.md)。

## 智能模型选择（Smart Model Selection）

**功能**: 根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现。

### 核心价值

- **成本优化**: 简单任务使用快速/便宜模型，复杂任务使用强大模型
- **性能优化**: 根据任务需求匹配合适的模型能力
- **可靠性**: 完整的降级机制，确保系统总能找到可用模型
- **灵活性**: 配置驱动，支持运行时策略调整

### 工作原理

#### 1. 复杂度评分

基于以下因素计算任务复杂度分数：

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 个文件以上 | +1 |
| 目录 | 包含 core/、guardrails/、security/ | +1 |
| 关键字 | 包含 architecture、security、design、migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |
| 显式指定 | PM 模板中记载 `effort: high` / `effort: xhigh` | +3（自动采用） |

#### 2. 模型等级映射

| 复杂度分数 | 模型等级 | 主要模型 | 环境变量 |
|------------|----------|---------|---------|
| 0-2 | FAST (低复杂度) | FABLE | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED (中等复杂度) | HAIKU | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY (高复杂度) | SONNET | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL (超高复杂度) | OPUS | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

#### 3. 降级机制

每个模型等级都有独立的降级链，按顺序尝试直到找到可用模型：

```
1. 主要模型 (如 env:ANTHROPIC_DEFAULT_HAIKU_MODEL)
2. 默认模型 (env:ANTHROPIC_MODEL)
3. 安全模型 (glm-4.7 硬编码兜底)
```

### 配置方式

#### 默认配置（自动启用）

系统会自动加载默认配置，无需手动设置。默认配置包含完整的四个等级配置和合理的降级链。

#### 项目配置（可选）

**.claude/settings.json** (优先级最高):
```json
{
  "modelSelection": {
    "enabled": true,
    "strategy": "effortBased",
    "fallback": {
      "priority": ["tierModel", "defaultModel", "safeModel"],
      "maxAttempts": 3,
      "timeoutMs": 5000,
      "validateApiCall": false
    },
    "tierMapping": {
      "fast": {
        "scoreRange": [0, 2],
        "modelEnv": "ANTHROPIC_DEFAULT_FABLE_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "balanced": {
        "scoreRange": [3, 4],
        "modelEnv": "ANTHROPIC_DEFAULT_HAIKU_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "quality": {
        "scoreRange": [5, 6],
        "modelEnv": "ANTHROPIC_DEFAULT_SONNET_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_SONNET_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "powerful": {
        "scoreRange": [7, 999],
        "modelEnv": "ANTHROPIC_DEFAULT_OPUS_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      }
    }
  }
}
```

**harness.toml** (项目配置):
```toml
[model_selection]
enable_smart_selection = true
strategy = "effort_based"

[model_selection.fallback]
priority = ["tier_model", "default_model", "safe_model"]
max_attempts = 3
timeout_ms = 5000
validate_api_call = false

[model_selection.tiers.fast]
min_score = 0
max_score = 2
model_env = "ANTHROPIC_DEFAULT_FABLE_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]
```

### 与 Effort Routing 集成

智能模型选择与现有的 Effort Routing 系统无缝集成：

1. **复杂度评分**: 使用现有的多要素评分机制
2. **Effort Tier 决定**: 根据分数和 code-risk 决定 effort tier
3. **模型选择**: 集成 SmartModelSelector 返回选择的模型

**集成示例**:
```java
EffortRouter router = new EffortRouter();
TaskContext context = new TaskContext(5, 2, true, false);
WorkerSpawnConfig config = router.determineWorkerConfig(context);
// config.getEffortTier() 返回 "xhigh"
// config.getSelectedModel() 返回选择的模型
```

### 使用示例

#### 基本使用

```bash
# 自动启用智能模型选择（默认）
/harness-work 3

# 系统会自动：
# 1. 计算任务复杂度分数
# 2. 根据分数选择模型等级
# 3. 执行降级链找到可用模型
# 4. 返回 WorkerSpawnConfig
```

#### 环境变量配置

```bash
# 设置默认模型（可选）
export ANTHROPIC_MODEL="glm-4.7"

# 设置等级特定模型
export ANTHROPIC_DEFAULT_FABLE_MODEL="claude-fable-5-20250514"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_DEFAULT_OPUS_MODEL="claude-opus-4-20250514"
```

### 性能目标

- **单次选择时间**: < 100ms（典型任务）
- **并发支持**: 支持 10+ 并发线程
- **内存占用**: < 10MB（配置和缓存）
- **可用性**: 选择成功率 > 98%（完整降级机制）

### 监控和日志

智能模型选择会输出详细日志，便于调试和监控：

- 模型选择日志：记录分数、等级、选择的模型
- 降级链日志：记录降级过程和失败原因
- 性能日志：记录选择耗时和并发统计

### 故障排除

**问题**: 所有模型都不可用
- **解决**: 检查环境变量配置，确保至少有一个兜底模型（glm-4.7）

**问题**: 选择的模型不符合预期
- **解决**: 检查任务复杂度评分，确认选择的等级正确

**问题**: 降级链总是失败
- **解决**: 检查网络连接和模型可用性，调整超时设置

### 相关文档

- **设计文档**: `docs/harness-project/superpowers/specs/2026-08-10-smart-model-selection-design.md`
- **实施计划**: `docs/harness-project/superpowers/plans/2026-08-10-smart-model-selection.md`
- **参考文档**: `skills/harness-work/references/effort-routing.md`

## Completion Report Output Contract

<!-- harness-work-completion-output-contract:start -->
Before rendering a Solo, forced single-task Parallel, or Breezing completion
report:

1. Resolve the active locale with the shared `get_harness_locale` function from
   `${HARNESS_PLUGIN_ROOT}/scripts/config-utils.sh`. Pass an explicit session or
   user language as its optional argument; otherwise keep the resolver priority
   of project `i18n.language`, `CLAUDE_CODE_HARNESS_LANG`, then default `en`.
2. Unset, invalid, and resolved `en` render the English template.
3. Only resolved `ja` renders the Japanese template.
4. Japanese input alone does not select the Japanese template.
5. Read `references/completion-report.md` and render exactly one template for
   the selected mode and locale.
6. Keep machine-readable status and review values in English, and never mix
   English and Japanese labels in one report.
<!-- harness-work-completion-output-contract:end -->

## 进度的可视化（面向非工程师）

任务执行中 `harness-progress` 将进度的件数与 drift alert 汇总到一张 HTML。
由 PostToolUse hook 自动再生成，订货人无需记住调用方式就能看到最新的进度板
（`posttool-progress-regen.sh` 最多每 1 分钟再生成 1 次）。

## 端到端检测集成 (v2.2.0+)

### 概述

从 v2.2.0 开始，harness-work 集成了端到端检测功能，在代码审查通过后自动触发前后端一起检查，确保代码质量不仅限于代码层面，还包括功能层面的完整性。

### 核心流程

```
代码实现 → 审查通过 → 端到端检测 → 结果处理
                                    ├─ PASS → cherry-pick → 完成
                                    ├─ FAIL → 自动修复/harness-work继续修改
                                    ├─ SKIPPED → 继续（配置禁用）
                                    └─ ERROR → 升级到用户
```

### 自动触发机制

端到端检测会在以下情况**自动运行**：

- ✅ 代码审查通过后（verdict == "APPROVE"）
- ✅ 配置启用的项目中（e2e_detection.enabled = true）
- ✅ 非草稿分支上
- ✅ 工作空间干净时

### 配置管理

所有端到端检测配置统一在 `harness.toml` 中管理：

```toml
# 端到端检测配置
[e2e_detection]
enabled = true  # 启用端到端检测
mode = "strict"  # 模式: strict | lenient
timeout = 120  # 单次测试超时时间（秒）

# 前端功能测试 - 🎭 默认使用 Playwright
[e2e_detection.test_types.frontend]
enabled = true  # 启用前端测试
framework = "playwright"  # 测试框架

[e2e_detection.test_types.frontend.playwright]
browsers = ["chromium", "firefox", "webkit"]  # 多浏览器支持
headless = true  # 无头模式
retries = 1  # 失败重试

# 后端API测试
[e2e_detection.test_types.backend]
enabled = true  # 启用后端测试
framework = "auto"  # 自动检测框架
```

### Playwright 默认支持

系统默认启用 Playwright 作为前端测试框架，提供以下特性：

- 🎭 **多浏览器测试**：Chromium、Firefox、WebKit
- 🚀 **自动等待机制**：减少不稳定测试
- 📸 **失败时自动截图**：便于调试
- 🎥 **失败时自动录制视频**：重现问题
- 🔍 **失败时保存追踪**：详细调试信息
- ⚡ **并行测试执行**：提高测试效率
- 📊 **HTML 报告生成**：美观的测试报告

### 失败处理策略

当端到端检测失败时，系统会根据配置自动处理：

1. **自动修复尝试**（如果启用）：
   - 依赖更新：自动更新缺失的依赖包
   - 敏感文件保护：自动添加到 .gitignore
   - 代码修复：尝试自动修复常见问题

2. **回到 harness-work**：
   - 如果自动修复失败或达到最大重试次数
   - 系统会自动将任务交还给 harness-work 继续修改
   - 避免内部修复循环，利用现有的工作流

3. **升级到用户**：
   - 如果检测出错或配置错误
   - 系统会停止并提示用户检查配置

### 配置优先级

```
harness.toml > JSON 配置 > 默认配置 > 环境变量
```

### 临时覆盖配置

```bash
# 临时禁用端到端检测
export HARNESS_E2E_ENABLED=false

# 临时切换到宽松模式
export HARNESS_E2E_MODE=lenient

# 临时禁用前端测试
export HARNESS_E2E_FRONTEND=false
```

### 检测类型

系统支持以下端到端检测类型：

- ✅ **前端测试**：Playwright（默认）、Cypress、Selenium
- ✅ **后端测试**：自动检测（Node.js/Java/Python/Go）
- ✅ **集成测试**：用户登录、数据流转、错误处理
- ⚪ **性能测试**：可选（默认关闭）
- ✅ **安全测试**：漏洞扫描、依赖检查

### 报告和诊断

端到端检测会生成详细的报告：

- 📋 多格式报告（Markdown/JSON/HTML/Console）
- 🔍 详细的问题分析和修复建议
- 📊 性能指标收集
- 📁 测试产物保存（截图、视频、追踪）

### 集成点

端到端检测集成在Breezing模式的Phase B（各任务执行）和Solo/Parallel模式中的审查通过后：

```python
# 在审查通过后自动触发
if verdict == "APPROVE":
    # 加载端到端检测配置
    e2e_config = load_e2e_detection_config()
    
    if e2e_config.enabled:
        # 执行端到端检测
        detection_result = run_e2e_detection(e2e_config)
        
        if detection_result.status == "PASS":
            # 继续正常流程
            cherry_pick_to_trunk()
        elif detection_result.status == "FAIL":
            # 回到 harness-work 继续修改
            escalate_to_harness_work(detection_result)
        elif detection_result.status == "ERROR":
            # 升级到用户
            escalate_to_user(detection_result)
```

### 监控和日志

端到端检测会输出详细日志，便于调试和监控：

- 检测执行日志：记录每个测试类型的执行情况
- 失败分析日志：记录失败原因和修复建议
- 性能日志：记录检测耗时和资源使用

### 故障排除

**问题**: 端到端检测总是失败
- **解决**: 检查测试是否完整，配置是否正确

**问题**: Playwright 浏览器无法启动
- **解决**: 检查 Playwright 是否正确安装，运行 `npx playwright install`

**问题**: 检测超时
- **解决**: 增加 `timeout` 配置或优化测试性能

### 相关文档

- **架构设计**: `docs/harness-project/architecture/e2e-detection-architecture.md`
- **分析报告**: `docs/harness-project/analysis/e2e-detection-analysis-report.md`
- **Playwright指南**: `docs/harness-project/playwright-testing-guide.md`
- **配置参考**: `docs/harness-project/playwright-default-config.md`

## 相关技能

- `harness-plan` — 计划要执行的任务
- `harness-sync` — 同步实现与 Plans.md
- `harness-review` — 审查实现
- `harness-release` — 版本 bump·发布
- `harness-progress` — 进度板 HTML（面向非工程师，执行中自动再生成）
