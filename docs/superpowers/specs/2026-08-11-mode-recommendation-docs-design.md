---
name: 智能执行模式推荐系统 SKILL.md 文档更新设计
description: 更新 harness-work SKILL.md 文档，记录 Phase 13 智能执行模式推荐系统
metadata:
  type: project
---

# 智能执行模式推荐系统 — SKILL.md 文档更新设计

**日期**: 2026-08-11
**范围**: Task 13.11 — 更新 harness-work SKILL.md 文档
**类型**: 纯文档更新（`[tdd:skip:docs-only]`）

## 目标

将 Phase 13 实现的智能执行模式推荐系统功能文档化到 `skills/harness-work/SKILL.md` 中，让用户理解推荐机制、使用方式和配置选项。

## 更新内容

### 1. 新增章节："智能执行模式推荐系统"

在现有 "Execution Mode Auto Selection" 和 "Branch Isolation Mode" 之间插入新章节。

**章节结构**:

1. **概述** — 核心价值和解决的问题
2. **工作原理** — 三步流水线（Analyze → Score → Recommend）
3. **置信度与自动确认机制** — 三级置信度对应的行为
4. **复杂度评分规则** — 各因素的分数加成
5. **评分权重配置** — 默认权重和自定义方式
6. **用户交互** — ModeAdvisor 的展示和确认流程
7. **学习与缓存** — AdaptiveLearner 和 RecommendationCache
8. **配置方式** — harness.toml 和 settings.json 配置
9. **使用示例** — 典型场景的推荐结果

### 2. 更新 Quick Reference 表格

添加 `--auto-mode` 行的智能推荐说明。

### 3. 更新 Options 表格

在 `--auto-mode` 选项说明中添加智能模式推荐的描述。

## 关键实现类参考

| 类 | 职责 |
|---|---|
| `ModeRecommender` | 核心引擎，整合分析→评分→推荐 |
| `TaskAnalyzer` | 任务特征提取（数量、复杂度、依赖、审查需求） |
| `ModeScorer` | 加权评分计算 |
| `RecommendationGenerator` | 推荐结果生成（模式、置信度、理由、备选） |
| `ModeAdvisor` | 用户交互层（展示、确认、选择菜单） |
| `AdaptiveLearner` | 历史学习和权重优化 |
| `RecommendationCache` | LRU 缓存 |

## 置信度阈值

| 阈值 | 来源 |
|------|------|
| ≥0.8 自动应用 | `ModeRecommender.shouldAutoApply()` |
| <0.7 需确认 | `ModeRecommender.requiresUserConfirmation()` |
| ≥0.85 强烈推荐 | `ModeAdvisor.formatRecommendation()` |

## 规格自检

1. **占位符扫描**: 无 TODO/待定内容
2. **内部一致性**: 文档内容与代码实现一致
3. **范围检查**: 聚焦于 SKILL.md 更新，无需拆分
4. **模糊性检查**: 置信度阈值、评分规则均有明确数值
