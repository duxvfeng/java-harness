# 智能模型选择系统设计文档

**项目**: Java Harness v4.1.1
**设计日期**: 2026-08-10
**状态**: 设计阶段
**作者**: Java Harness Team

---

## 1. 项目概述

### 1.1 目标

为 Java Harness 添加智能模型选择功能，根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现。

### 1.2 核心价值

- **成本优化**: 简单任务使用快速/便宜模型，复杂任务使用强大模型
- **性能优化**: 根据任务需求匹配合适的模型能力
- **可靠性**: 完整的降级机制，确保系统总能找到可用模型
- **灵活性**: 配置驱动，支持运行时策略调整

### 1.3 设计原则

1. **最小侵入**: 基于现有 Effort Routing 系统，不破坏现有架构
2. **配置驱动**: 通过配置文件管理策略，避免硬编码
3. **向后兼容**: 不影响现有功能的正常运行
4. **可观测性**: 完整的日志和监控，便于调试

---

## 2. 系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    智能模型选择系统                          │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ↓                     ↓                     ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Effort Routing│    │ Config Loader│    │Model Selector│
│   (现有)     │    │  (新增组件)  │    │  (新增组件)  │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ↓
                    ┌──────────────┐
                    │ Fallback    │
                    │ Chain       │
                    │ Executor    │
                    └──────────────┘
                              │
                              ↓
                    ┌──────────────┐
                    │ Availability │
                    │ Checker     │
                    └──────────────┘
                              │
                              ↓
                    ┌──────────────┐
                    │ Final Model  │
                    │ Assignment  │
                    └──────────────┘
```

### 2.2 核心组件

#### 2.2.1 Config Loader 组件

**职责**: 加载和管理模型选择配置

**优先级顺序**:
```
1. .claude/settings.json (项目级别，最高优先级)
2. harness.toml (项目配置)
3. 默认配置 (内置兜底)
```

**接口设计**:
```java
public class ModelSelectionConfigLoader {
    
    public static ModelSelectionConfig load() {
        // 按优先级尝试加载配置
        return loadFromSettingsJson()
            .orElseGet(() -> loadFromHarnessToml()
            .orElseGet(() -> getDefaultConfig()));
    }
    
    private static Optional<ModelSelectionConfig> loadFromSettingsJson() {
        Path settingsPath = Paths.get(".claude/settings.json");
        if (Files.exists(settingsPath)) {
            return Optional.of(parseSettingsJson(settingsPath));
        }
        return Optional.empty();
    }
    
    private static Optional<ModelSelectionConfig> loadFromHarnessToml() {
        Path tomlPath = Paths.get("harness.toml");
        if (Files.exists(tomlPath)) {
            return Optional.of(parseHarnessToml(tomlPath));
        }
        return Optional.empty();
    }
}
```

#### 2.2.2 Model Selector 组件

**职责**: 根据复杂度分数和配置选择最优模型

**核心算法**:
```java
public class SmartModelSelector {
    
    private final ModelSelectionConfig config;
    private final ModelAvailabilityChecker availabilityChecker;
    
    public String selectModel(int complexityScore) {
        // 1. 确定模型等级
        ModelTier tier = determineTier(complexityScore);
        
        // 2. 获取该等级的配置
        TierConfig tierConfig = config.getTierConfig(tier);
        
        // 3. 执行降级链
        return executeFallbackChain(tierConfig);
    }
    
    private String executeFallbackChain(TierConfig tierConfig) {
        for (String candidate : tierConfig.getFallbackChain()) {
            String resolvedModel = resolveModelReference(candidate);
            
            if (availabilityChecker.isAvailable(resolvedModel, config.getTimeout())) {
                logModelSelection(tierConfig, resolvedModel, candidate);
                return resolvedModel;
            }
        }
        
        throw new ModelUnavailableException(
            "No models available for tier: " + tierConfig.getTierName());
    }
    
    private String resolveModelReference(String reference) {
        if (reference.startsWith("env:")) {
            String envVar = reference.substring(4);
            String value = System.getenv(envVar);
            if (value == null || value.isEmpty()) {
                throw new ConfigException(
                    "Environment variable not found: " + envVar);
            }
            return value;
        }
        return reference; // 直接模型名称
    }
    
    private ModelTier determineTier(int score) {
        if (score <= 2) return ModelTier.FAST;
        if (score <= 4) return ModelTier.BALANCED;
        if (score <= 6) return ModelTier.QUALITY;
        return ModelTier.POWERFUL;
    }
}
```

#### 2.2.3 Model Availability Checker 组件

**职责**: 检查模型是否可用

**实现策略**:
```java
public class ModelAvailabilityChecker {
    
    public boolean isAvailable(String model, int timeoutMs) {
        try {
            // 1. 检查模型名称格式
            if (!isValidModelName(model)) {
                return false;
            }
            
            // 2. 检查网络连通性（如需要）
            if (isRemoteModel(model) && !checkNetworkConnectivity(timeoutMs)) {
                return false;
            }
            
            // 3. 尝试轻量级API调用（可选）
            if (config.isValidateApiCall()) {
                return tryLightweightApiCall(model, timeoutMs);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warn("Model availability check failed for: {}", model, e);
            return false;
        }
    }
    
    private boolean isValidModelName(String model) {
        // 基本格式验证
        return model != null && !model.trim().isEmpty() && model.length() <= 100;
    }
}
```

---

## 3. 模型映射策略

### 3.1 复杂度评分

基于现有的 Effort Routing 系统评分机制：

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 个文件以上 | +1 |
| 目录 | 包含 core/、guardrails/、security/ | +1 |
| 关键字 | 包含 architecture、security、design、migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |
| 显式指定 | PM 模板中记载 `effort: high` / `effort: xhigh` | +3（自动采用） |

### 3.2 模型等级映射

| Effort 分数 | 复杂度等级 | 主要模型 | 环境变量 |
|------------|----------|---------|---------|
| 0-2 | 低复杂度 | FABLE | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | 中等复杂度 | HAIKU | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | 高复杂度 | SONNET | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | 超高复杂度 | OPUS | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

### 3.3 降级策略

每个模型等级都有独立的降级链，按顺序尝试直到找到可用模型：

**示例降级链**:
```
1. 主要模型 (如 ANTHROPIC_DEFAULT_HAIKU_MODEL)
2. 默认模型 (ANTHROPIC_MODEL)  
3. 安全模型 (glm-4.7 硬编码兜底)
```

---

## 4. 配置文件格式

### 4.1 .claude/settings.json 格式

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

### 4.2 harness.toml 格式

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

[model_selection.tiers.balanced]
min_score = 3
max_score = 4
model_env = "ANTHROPIC_DEFAULT_HAIKU_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]

[model_selection.tiers.quality]
min_score = 5
max_score = 6
model_env = "ANTHROPIC_DEFAULT_SONNET_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_SONNET_MODEL", 
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]

[model_selection.tiers.powerful]
min_score = 7
model_env = "ANTHROPIC_DEFAULT_OPUS_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]
```

---

## 5. 集成方案

### 5.1 与现有系统集成

**在 Effort Routing 中的集成点**:

```java
// 在现有的 effort routing 逻辑后添加
public class EffortRouter {
    
    public WorkerSpawnConfig determineWorkerConfig(TaskContext context) {
        // 1. 现有的复杂度评分逻辑
        int complexityScore = calculateComplexityScore(context);
        
        // 2. 现有的 effort tier 决定
        EffortTier effortTier = determineEffortTier(complexityScore);
        
        // 3. 新增：智能模型选择
        String selectedModel = SmartModelSelector.getInstance()
            .selectModel(complexityScore);
        
        return new WorkerSpawnConfig(effortTier, selectedModel);
    }
}
```

### 5.2 代码结构

```
java-harness-workflow/
├── src/main/java/com/chachamaru/harness/model/
│   ├── ModelSelectionConfigLoader.java
│   ├── SmartModelSelector.java
│   ├── ModelAvailabilityChecker.java
│   ├── ModelTier.java
│   ├── TierConfig.java
│   └── ModelSelectionConfig.java
├── src/test/java/com/chachamaru/harness/model/
│   ├── SmartModelSelectorTest.java
│   ├── ConfigLoaderTest.java
│   └── AvailabilityCheckerTest.java
```

### 5.3 技能文件更新

**harness-work SKILL.md** 新增章节:

```markdown
## 智能模型选择（新增）

当 effort tier 确定后，自动选择对应的 AI 模型：

1. 读取当前环境变量中的模型配置
2. 根据复杂度分数映射到模型等级  
3. 按配置的降级链尝试模型
4. 在 Worker Agent 启动时使用选定模型

**配置方式**: 
- 项目配置: `.claude/settings.json` (优先) 或 `harness.toml`
- 环境变量: `ANTHROPIC_DEFAULT_*_MODEL`
- 降级策略: 可配置的 fallback chain
```

---

## 6. 数据流示例

**场景**: 中等复杂度任务（分数=4）

```
1. Effort Routing 评分: 4分
   ↓
2. Model Selector 确定等级: balanced (3-4分)
   ↓
3. Config Loader 获取配置: 
   - modelEnv: "ANTHROPIC_DEFAULT_HAIKU_MODEL"
   - fallbackModels: ["env:ANTHROPIC_DEFAULT_HAIKU_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"]
   ↓
4. Fallback Chain 执行:
   ├─ 尝试 env:ANTHROPIC_DEFAULT_HAIKU_MODEL → "mimo-v2.5-pro"
   │  └─ 检查可用性... 假设失败 ❌
   │
   ├─ 尝试 env:ANTHROPIC_MODEL → "glm-4.7" 
   │  └─ 检查可用性... 成功 ✅
   │
   └─ 返回: "glm-4.7"
   ↓
5. 最终模型: glm-4.7
```

---

## 7. 错误处理

### 7.1 配置错误处理

- **配置文件不存在**: 使用默认配置
- **配置格式错误**: 记录错误日志，使用默认配置
- **环境变量缺失**: 抛出 ConfigException，跳过该候选模型

### 7.2 模型可用性错误

- **所有模型不可用**: 抛出 ModelUnavailableException
- **网络超时**: 记录日志，继续下一个候选模型
- **API 验证失败**: 根据配置决定是否继续尝试

### 7.3 降级失败处理

```java
try {
    return smartModelSelector.selectModel(complexityScore);
} catch (ModelUnavailableException e) {
    logger.error("All models unavailable, using hardcoded fallback");
    return "glm-4.7"; // 最终兜底
}
```

---

## 8. 性能考虑

### 8.1 缓存策略

- **配置缓存**: 配置文件变更后重新加载
- **可用性缓存**: 模型可用性检查结果缓存 5 分钟
- **选择结果缓存**: 相同复杂度分数的选择结果缓存 1 分钟

### 8.2 性能目标

- **配置加载时间**: < 100ms
- **模型选择时间**: < 50ms
- **可用性检查时间**: < 5s (可配置)
- **内存占用**: < 10MB (配置和缓存)

---

## 9. 测试策略

### 9.1 单元测试

- **配置加载测试**: 各种配置格式和优先级
- **模型选择测试**: 不同复杂度分数的选择逻辑
- **降级链测试**: 各种失败场景的降级行为
- **可用性检查测试**: 网络异常、超时等场景

### 9.2 集成测试

- **端到端流程测试**: 从复杂度评分到最终模型选择
- **多配置源测试**: settings.json 和 harness.toml 切换
- **实际环境变量测试**: 使用真实环境变量

### 9.3 性能测试

- **并发选择测试**: 多线程同时调用模型选择
- **缓存效率测试**: 验证缓存命中率
- **内存占用测试**: 长时间运行的内存稳定性

---

## 10. 监控和日志

### 10.1 关键日志

```java
logger.info("Model selection started - complexity score: {}", score);
logger.debug("Trying model candidate: {}", candidate);
logger.info("Model selected: {} for tier: {}", selectedModel, tier);
logger.warn("Model unavailable: {}, trying next", candidate);
logger.error("All models exhausted for tier: {}", tier);
```

### 10.2 性能监控

- **模型选择耗时**: 记录每次选择的时间
- **降级次数**: 统计每个等级的降级频率
- **模型使用分布**: 统计各模型的使用情况

---

## 11. 安全考虑

### 11.1 配置安全

- **配置文件权限**: 确保配置文件权限正确
- **环境变量保护**: 不在日志中记录敏感信息
- **输入验证**: 严格验证所有配置输入

### 11.2 API 安全

- **认证信息**: 使用现有的 ANTHROPIC_AUTH_TOKEN
- **网络安全**: 支持 HTTPS 和代理配置
- **超时保护**: 防止长时间阻塞

---

## 12. 实施计划

### 12.1 开发阶段

**Phase 1: 核心组件开发** (2-3 天)
- Config Loader 实现
- Model Selector 实现  
- Availability Checker 实现

**Phase 2: 配置和集成** (1-2 天)
- 配置文件格式实现
- 与 Effort Routing 集成
- 技能文件更新

**Phase 3: 测试和验证** (2-3 天)
- 单元测试编写
- 集成测试验证
- 性能测试优化

**Phase 4: 文档和发布** (1 天)
- 用户文档编写
- 配置示例创建
- 发布说明准备

### 12.2 验收标准

- ✅ 所有单元测试通过
- ✅ 集成测试覆盖主要场景
- ✅ 性能指标达标
- ✅ 文档完整清晰
- ✅ 向后兼容验证

---

## 13. 风险和挑战

### 13.1 技术风险

- **模型可用性判断**: 可能需要 API 调用验证
- **配置复杂性**: 用户配置错误可能导致意外行为
- **性能影响**: 额外的模型选择步骤可能影响性能

### 13.2 缓解措施

- **可选验证**: API 验证设为可选，避免性能损失
- **配置验证**: 提供配置验证工具，帮助用户检查配置
- **性能优化**: 使用缓存和异步检查减少延迟
- **降级兜底**: 确保在最坏情况下仍能工作

---

## 14. 未来扩展

### 14.1 可能的增强

- **学习机制**: 根据历史结果自动优化模型选择策略
- **成本感知**: 考虑模型价格，优化成本效益
- **A/B测试**: 支持对比不同模型选择策略的效果
- **自定义评分**: 支持用户自定义复杂度评分规则

### 14.2 扩展点

- **插件化模型检查器**: 支持自定义可用性检查逻辑
- **配置热更新**: 运行时更新配置而无需重启
- **多区域支持**: 根据地理位置选择最优模型

---

## 15. 总结

智能模型选择系统通过以下特点为 Java Harness 提供了显著价值：

✅ **成本优化**: 根据任务复杂度选择合适模型，避免过度配置
✅ **性能优化**: 简单任务使用快速模型，提高响应速度
✅ **可靠性**: 完整的降级机制确保系统稳定运行
✅ **灵活性**: 配置驱动，支持运行时策略调整
✅ **可维护性**: 清晰的架构和完整的测试覆盖

该系统将与现有 Effort Routing 无缝集成，为 Java Harness 用户提供更智能、更经济的模型使用体验。

---

**文档版本**: 1.0
**最后更新**: 2026-08-10
**下一步**: 调用 writing-plans 技能创建实现计划