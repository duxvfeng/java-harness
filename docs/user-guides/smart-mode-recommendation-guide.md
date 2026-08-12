# 智能执行模式推荐系统用户指南

## 概述

智能执行模式推荐系统是 Java Harness Phase 13 引入的核心功能，旨在帮助用户在 Solo/Parallel/Breezing 三种执行模式之间做出最优选择。

## 为什么需要智能推荐？

传统的模式选择依赖用户经验：
- **Solo**: 单任务直接执行
- **Parallel**: 2-3 个任务并行执行
- **Breezing**: 4+ 个任务团队协作

但实际场景中，任务数量只是决策因素之一。任务复杂度、依赖关系、审查需求等都会影响模式选择。智能推荐系统通过四维度分析，自动给出最优建议。

## 快速开始

### 启用智能推荐

```bash
# 使用 --auto-mode 参数
/harness-work --auto-mode

# 结合任务范围使用
/harness-work --auto-mode all
/harness-work --auto-mode 3-6
/harness-work --auto-mode 5
```

### 理解推荐结果

推荐结果包含四个关键信息：

1. **推荐模式**: 系统建议的执行模式（SOLO/PARALLEL/BREEZING）
2. **置信度**: 推荐的可信程度（0%-100%）
3. **推荐理由**: 基于哪些特征做出的判断
4. **备选方案**: 其他可行的执行模式

## 置信度详解

置信度是推荐系统的核心指标，决定了系统的交互行为：

| 置信度范围 | 系统行为 | 用户操作 |
|-----------|---------|---------|
| **≥80%** | 自动应用推荐 | 无需操作，系统自动执行 |
| **70%-80%** | 显示推荐并询问 | 输入 `y` 接受，`n` 拒绝 |
| **<70%** | 显示选择菜单 | 输入数字选择模式 |

### 置信度强度指示

- ⭐ **强烈推荐**（≥85%）— 推荐模式与任务特征高度匹配
- ✅ **推荐**（70%-85%）— 推荐模式适合当前任务
- 💭 **建议**（50%-70%）— 可以考虑，也有其他可行选择
- 🤔 **可选**（<50%）— 多种模式都可行，请根据具体情况选择

## 评分维度

系统从四个维度分析任务特征：

### 1. 任务数量（权重 35%）

| 任务数 | 倾向模式 | 说明 |
|--------|---------|------|
| 1 个 | SOLO | 开销最小，直接执行 |
| 2-4 个 | PARALLEL | 并行处理提升效率 |
| 5+ 个 | BREEZING | 需要团队协调管理 |

### 2. 复杂度（权重 35%）

复杂度通过以下因素计算：

| 因素 | 分数 | 说明 |
|------|------|------|
| 文件数 >=3 | +1 | 中等规模变更 |
| 文件数 >=5 | +2 | 大规模变更 |
| 核心目录 `core/` | +3 | 核心模块修改 |
| 安全目录 `security/` | +3 | 安全相关修改 |
| 架构关键字 | +8 | 架构级变更 |
| 失败历史 | +3 | 曾失败的任务 |

复杂度等级：
- **SIMPLE**（0 分）: 简单任务
- **MODERATE**（1-2 分）: 中等复杂度
- **COMPLEX**（3-6 分）: 复杂任务
- **VERY_COMPLEX**（7+ 分）: 非常复杂

### 3. 依赖关系（权重 20%）

| 依赖类型 | 说明 | 倾向模式 |
|---------|------|---------|
| INDEPENDENT | 任务相互独立 | PARALLEL |
| SEQUENTIAL | 有顺序依赖 | 需要协调 |
| MIXED | 混合依赖 | BREEZING |

### 4. 审查需求（权重 10%）

| 审查需求 | 说明 | 倾向模式 |
|---------|------|---------|
| NONE | 无需审查 | SOLO/PARALLEL |
| OPTIONAL | 可选审查 | 灵活选择 |
| REQUIRED | 必须审查 | BREEZING（有独立 Reviewer） |

## 典型场景

### 场景 1: 简单文档更新

```bash
/harness-work --auto-mode update-readme
```

**分析**:
- 任务数: 1 → 倾向 SOLO
- 复杂度: SIMPLE（文档任务）→ 倾向 SOLO
- 依赖: INDEPENDENT → 倾向 SOLO
- 审查: NONE → 倾向 SOLO

**推荐**: SOLO（置信度: 92%）

### 场景 2: 添加单元测试

```bash
/harness-work --auto-mode 10.11
```

**分析**:
- 任务数: 1 → 倾向 SOLO
- 复杂度: MODERATE（测试任务）→ 中等
- 依赖: INDEPENDENT → 倾向 SOLO
- 审查: OPTIONAL → 灵活

**推荐**: SOLO（置信度: 78%）

### 场景 3: 多个独立功能

```bash
/harness-work --auto-mode 5-7
```

**分析**:
- 任务数: 3 → 倾向 PARALLEL
- 复杂度: 取决于具体任务
- 依赖: 可能独立 → 倾向 PARALLEL
- 审查: 可选 → 灵活

**推荐**: PARALLEL（置信度: 72%）

### 场景 4: 核心模块重构

```bash
/harness-work --auto-mode all
```

**分析**:
- 任务数: 6+ → 倾向 BREEZING
- 复杂度: VERY_COMPLEX（核心模块）→ 倾向 BREEZING
- 依赖: MIXED → 倾向 BREEZING
- 审查: REQUIRED → 倾向 BREEZING

**推荐**: BREEZING（置信度: 88%）

## 学习与优化

### 反馈机制

系统会记录用户对推荐的反馈：
- **接受**: 用户接受推荐模式
- **拒绝**: 用户选择其他模式
- **修改**: 用户手动指定模式

### 权重优化

根据用户偏好自动调整评分权重：
- 如果用户经常拒绝 SOLO 推荐，SOLO 权重会降低
- 如果用户经常接受 BREEZING 推荐，BREEZING 权重会提高

### 缓存机制

- LRU 缓存，默认 100 条
- 相同任务特征的推荐结果会缓存
- 避免重复计算，提升响应速度

## 配置选项

### 权重自定义（高级）

通过代码自定义评分权重：

```java
ScoringWeights weights = ScoringWeights.builder()
    .taskCountWeight(0.40)      // 提高任务数量权重
    .complexityWeight(0.30)     // 调整复杂度权重
    .dependencyWeight(0.20)     // 保持依赖关系权重
    .reviewRequirementWeight(0.10) // 保持审查需求权重
    .build();

ModeRecommender recommender = new ModeRecommender(weights);
```

### 缓存配置

```java
RecommendationCache cache = new RecommendationCache(200); // 200 条缓存
```

### 学习数据存储

```java
LearningPersistence persistence = new LearningPersistence(".claude/mode-learning");
```

## 故障排除

### 问题: 推荐结果不符合预期

**可能原因**:
- 任务描述不够清晰
- 文件路径没有正确反映任务特征
- 复杂度评分规则需要调整

**解决方法**:
- 使用 `recommendWithDebugInfo()` 查看详细分析
- 检查任务特征是否被正确识别
- 考虑使用显式 `effort` 参数覆盖

### 问题: 置信度总是很低

**可能原因**:
- 任务特征不明显
- 多种模式评分接近

**解决方法**:
- 这是正常情况，说明多种模式都可行
- 根据实际情况手动选择
- 提供更明确的任务描述

### 问题: 推荐速度慢

**可能原因**:
- 缓存未命中
- 任务特征分析复杂

**解决方法**:
- 检查缓存配置
- 简化任务描述
- 使用 `quickRecommend()` 获取快速结果

## API 参考

### ModeRecommender

核心推荐引擎。

```java
// 创建推荐器
ModeRecommender recommender = new ModeRecommender();

// 基本推荐
ModeRecommendation rec = recommender.recommend(tasks, files);

// 带失败历史
ModeRecommendation rec = recommender.recommend(tasks, files, true);

// 完整 API
ModeRecommendation rec = recommender.recommend(tasks, files, false, "high");

// 带调试信息
RecommendationResult result = recommender.recommendWithDebugInfo(tasks, files);

// 快速推荐
ExecutionMode mode = recommender.quickRecommend(tasks, files);

// 自动确认判断
boolean shouldAuto = recommender.shouldAutoApply(rec);
boolean needsConfirm = recommender.requiresUserConfirmation(rec);
```

### ModeRecommendation

推荐结果数据类。

```java
rec.recommendedMode()    // 推荐的执行模式
rec.confidence()         // 置信度 (0.0-1.0)
rec.reason()             // 推荐理由
rec.alternativeModes()   // 备选方案列表
rec.isHighConfidence()   // 是否高置信度 (>0.8)
rec.isMediumConfidence() // 是否中等置信度 (0.6-0.8)
rec.isLowConfidence()    // 是否低置信度 (<0.6)
```

### ModeAdvisor

用户交互顾问。

```java
ModeAdvisor advisor = new ModeAdvisor();

// 格式化显示
String display = advisor.formatRecommendation(rec);

// 生成确认提示
String prompt = advisor.generateConfirmationPrompt(rec);

// 生成详细报告
String report = advisor.generateDetailedReport(rec);

// 生成对比表格
String table = advisor.generateComparisonTable(rec, scores);

// 解析用户选择
ExecutionMode mode = advisor.parseModeSelection(input);
```

## 相关文档

- **SKILL.md**: [智能执行模式推荐系统](../skills/harness-work/SKILL.md#智能执行模式推荐系统smart-mode-recommendation)
- **设计规格**: [设计文档](../superpowers/specs/2026-08-11-mode-recommendation-docs-design.md)
- **README**: [功能介绍](../README.md#智能执行模式推荐系统-🆕)
