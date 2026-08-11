# 智能模型选择配置指南

## 快速开始

### 1. 默认使用（无需配置）

智能模型选择系统默认启用，会自动根据任务复杂度选择最优模型：

```bash
# 直接使用，无需任何配置
/harness-work 3
```

系统会：
- 计算任务复杂度分数（0-2分→FAST，3-4分→BALANCED，5-6分→QUALITY，≥7分→POWERFUL）
- 自动选择对应的模型等级
- 执行降级链确保找到可用模型

### 2. 基础环境变量配置

设置默认模型和等级特定模型：

```bash
# 设置默认模型（兜底）
export ANTHROPIC_MODEL="glm-4.7"

# 设置各等级模型
export ANTHROPIC_DEFAULT_FABLE_MODEL="claude-fable-5-20250514"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_DEFAULT_OPUS_MODEL="claude-opus-4-20250514"
```

## 配置详解

### 配置优先级

配置加载优先级（从高到低）：

1. **环境变量** - 最高优先级，适合动态配置
2. `.claude/settings.json` - 项目配置，推荐使用
3. `harness.toml` - 传统项目配置
4. **默认配置** - 内置兜底配置

### JSON 配置（推荐）

在项目根目录创建 `.claude/settings.json`：

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

### TOML 配置

在项目根目录创建 `harness.toml`：

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
max_score = 999
model_env = "ANTHROPIC_DEFAULT_OPUS_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]
```

## 配置参数说明

### 全局参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 是否启用智能模型选择 |
| `strategy` | string | `"effortBased"` | 选择策略，当前仅支持 effortBased |
| `fallback.maxAttempts` | integer | `3` | 降级链最大尝试次数 |
| `fallback.timeoutMs` | integer | `5000` | 每次尝试的超时时间（毫秒） |
| `fallback.validateApiCall` | boolean | `false` | 是否验证模型API调用 |

### 等级配置

每个等级（fast/balanced/quality/powerful）支持以下参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| `scoreRange` | [int, int] | 复杂度分数范围 |
| `modelEnv` | string | 主要模型的环境变量名 |
| `fallbackModels` | string[] | 降级链模型列表 |

### 降级链格式

降级链支持三种模型引用格式：

1. **环境变量引用**: `env:VARIABLE_NAME`
2. **直接模型名**: `glm-4.7`
3. **硬编码兜底**: `glm-4.7`（最后一个）

## 使用场景

### 场景1: 成本优化

为简单任务使用更便宜的模型：

```json
{
  "modelSelection": {
    "tierMapping": {
      "fast": {
        "fallbackModels": ["claude-3.5-haiku", "glm-4.7"]
      },
      "balanced": {
        "fallbackModels": ["claude-3.5-haiku", "glm-4.7"]
      }
    }
  }
}
```

### 场景2: 性能优先

为所有任务使用最强大的模型：

```json
{
  "modelSelection": {
    "tierMapping": {
      "fast": {
        "fallbackModels": ["claude-opus-4-20250514", "glm-4.7"]
      },
      "balanced": {
        "fallbackModels": ["claude-opus-4-20250514", "glm-4.7"]
      },
      "quality": {
        "fallbackModels": ["claude-opus-4-20250514", "glm-4.7"]
      },
      "powerful": {
        "fallbackModels": ["claude-opus-4-20250514", "glm-4.7"]
      }
    }
  }
}
```

### 场景3: 混合策略

为不同复杂度使用不同的模型组合：

```json
{
  "modelSelection": {
    "tierMapping": {
      "fast": {
        "fallbackModels": ["claude-3.5-haiku", "glm-4.7"]
      },
      "balanced": {
        "fallbackModels": ["claude-sonnet-4-20250514", "claude-3.5-haiku", "glm-4.7"]
      },
      "quality": {
        "fallbackModels": ["claude-sonnet-4-20250514", "claude-opus-4-20250514", "glm-4.7"]
      },
      "powerful": {
        "fallbackModels": ["claude-opus-4-20250514", "claude-sonnet-4-20250514", "glm-4.7"]
      }
    }
  }
}
```

## 故障排除

### 问题1: 所有模型都不可用

**症状**: 系统报告 "No models available"

**解决方案**:
1. 检查环境变量是否正确设置
2. 确保至少有一个硬编码兜底模型（如 `glm-4.7`）
3. 验证网络连接和模型可用性

```bash
# 验证环境变量
echo $ANTHROPIC_MODEL
echo $ANTHROPIC_DEFAULT_SONNET_MODEL

# 检查配置文件
cat .claude/settings.json
```

### 问题2: 选择的模型不符合预期

**症状**: 简单任务使用了昂贵的模型

**解决方案**:
1. 检查任务复杂度评分是否正确
2. 验证 `scoreRange` 配置
3. 查看日志了解选择过程

```bash
# 查看日志
cat .claude/logs/model-selection.log
```

### 问题3: 降级链总是失败

**症状**: 每次都尝试多个模型才成功

**解决方案**:
1. 检查网络连接和模型可用性
2. 增加 `timeoutMs` 值
3. 调整降级链顺序，将最可靠的模型放在前面

```json
{
  "fallback": {
    "timeoutMs": 10000,
    "priority": ["defaultModel", "tierModel", "safeModel"]
  }
}
```

### 问题4: 配置文件不生效

**症状**: 修改配置后行为没有变化

**解决方案**:
1. 确认配置文件路径正确
2. 检查配置格式是否正确（JSON/TOML语法）
3. 验证配置优先级（环境变量 > settings.json > harness.toml）
4. 清除缓存重试

## 性能调优

### 缓存优化

系统使用三层缓存机制：

1. **配置缓存**: 10分钟有效期
2. **可用性缓存**: 5分钟有效期
3. **选择结果缓存**: 1分钟有效期

查看缓存统计：

```bash
# 通过日志查看缓存状态
cat .claude/logs/model-selection.log | grep "Cache"
```

### 并发性能

系统支持高并发模型选择：

- **单次选择时间**: < 100ms（典型任务）
- **并发支持**: 支持 10+ 并发线程
- **内存占用**: < 10MB（配置和缓存）

## 监控和日志

### 日志位置

- **日志目录**: `.claude/logs/`
- **日志文件**: `model-selection.log`

### 日志级别

- `DEBUG`: 详细的调试信息
- `INFO`: 一般信息性消息
- `WARN`: 警告消息（可恢复的异常）
- `ERROR`: 错误消息（影响功能的异常）

### 监控指标

系统自动记录以下指标：

- 总选择次数
- 成功率
- 缓存命中率
- 平均响应时间
- 错误类型分布

## 最佳实践

### 1. 渐进式配置

从默认配置开始，逐步调整：

1. 先使用默认配置验证功能
2. 根据实际需求调整模型映射
3. 优化降级链和超时设置
4. 监控性能和成本

### 2. 环境隔离

为不同环境使用不同配置：

```bash
# 开发环境：使用快速模型
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"

# 生产环境：使用稳定模型
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
```

### 3. 成本监控

定期检查模型使用情况：

```bash
# 查看使用统计
cat .claude/logs/model-selection.log | grep "Model selected"
```

### 4. 容错设计

始终配置完整的降级链：

```json
{
  "fallbackModels": [
    "env:PRIMARY_MODEL",      // 主要模型
    "env:BACKUP_MODEL",        // 备用模型
    "glm-4.7"                 // 硬编码兜底
  ]
}
```

## 高级配置

### 自定义复杂度评分

可以通过修改任务上下文来影响复杂度评分：

- **文件数**: 变更对象 4 个文件以上 (+1)
- **目录**: 包含 core/、guardrails/、security/ (+1)
- **关键字**: 包含 architecture、security、design、migration (+1)
- **失败历史**: agent memory 中有同任务的失败记录 (+2)
- **显式指定**: PM 模板中记载 `effort: high` / `effort: xhigh` (+3)

### 与 Effort Routing 集成

智能模型选择与 Effort Routing 无缝集成：

```java
EffortRouter router = new EffortRouter();
TaskContext context = new TaskContext(5, 2, true, false);
WorkerSpawnConfig config = router.determineWorkerConfig(context);

// config.getEffortTier() 返回 "xhigh"
// config.getSelectedModel() 返回选择的模型
```

## 参考资源

- **设计文档**: `docs/superpowers/specs/2026-08-10-smart-model-selection-design.md`
- **实施计划**: `docs/superpowers/plans/2026-08-10-smart-model-selection.md`
- **技能文档**: `skills/harness-work/SKILL.md`
- **API文档**: `java-harness-workflow/src/main/java/com/chachamaru/harness/model/`