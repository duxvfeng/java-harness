# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.1.0-java] - 2026-08-11 (Phase 12: 智能模型选择系统)

### Added
- **🤖 智能模型选择系统**: 根据任务复杂度自动选择最优的 AI 大模型
  - 实现复杂度评分机制（基于文件数、目录、关键字、失败历史）
  - 创建四个模型等级（FAST/BALANCED/QUALITY/POWERFUL）精准匹配任务需求
  - 实现完整的降级机制，确保系统总能找到可用模型
  - 支持环境变量解析（`env:VAR_NAME` 格式）
  - 配置优先级：环境变量 > settings.json > harness.toml > 默认配置
- **📦 核心组件**:
  - `ModelTier`: 模型等级枚举（4 个等级：FAST/BALANCED/QUALITY/POWERFUL）
  - `TierConfig`: 单个等级配置类（包含降级链）
  - `ModelSelectionConfig`: 总配置类（管理所有等级）
  - `ModelSelectionConfigLoader`: 配置加载器（支持 JSON 和 TOML 格式）
  - `ModelAvailabilityChecker`: 增强的模型可用性检查器（支持网络连通性检查）
  - `SmartModelSelector`: 核心选择器（实现三层缓存和降级链）
  - `ModelSelectionLogger`: 结构化日志记录器（文件持久化）
  - `EffortRouter`: 集成智能模型选择（已完成）
  - `WorkerSpawnConfig`: Worker 启动配置（包含 effort tier 和选择的模型）
- **🎯 功能增强**:
  - 配置文件加载功能（支持实际 JSON 和 TOML 文件解析）
  - 增强的模型可用性检查（真实模型名称验证、网络连接检查）
  - 完整的缓存系统（配置缓存、可用性缓存、选择结果缓存）
  - 结构化日志记录（性能监控、错误追踪、文件持久化）
- **🧪 测试覆盖**:
  - 端到端集成测试（配置优先级、降级机制、异常处理）
  - 性能和压力测试（单次选择 < 100ms，支持 10+ 并发线程）
  - 并发测试（20线程，成功率 > 90%）
  - 内存稳定性测试（内存增长 < 50MB）
  - 缓存性能测试（缓存命中率 > 60%）
- **📖 文档更新**:
  - 更新 `skills/harness-work/SKILL.md` 添加智能模型选择完整章节
  - 创建用户配置指南（`docs/user-guides/smart-model-selection-configuration.md`）
  - 更新 README.md 添加智能模型选择功能说明（包含专门章节）
  - 更新 CHANGELOG.md 记录 Phase 12 完整变更
- **⚡ 性能指标**:
  - 单次选择时间：< 100ms（典型任务）
  - 缓存命中时间：< 10ms（缓存命中场景）
  - 并发支持：10+ 并发线程，高并发下成功率 > 90%
  - 内存占用：< 10MB（配置和缓存）
  - 选择成功率：> 98%（完整降级机制）

### Changed
- **EffortRouter 升级**: 从单纯的 effort tier 选择升级为 effort tier + 模型选择的完整路由
- **配置管理**: 新增智能模型选择配置支持（settings.json 和 harness.toml）
- **WorkerSpawnConfig**: 新增 selectedModel 字段，包含选择的模型名称
- **监控能力**: 新增结构化日志记录和性能监控

### Fixed
- 修复配置文件加载的优先级处理
- 修复模型可用性检查的网络超时处理
- 修复缓存并发访问的线程安全问题

### Technical Details
- **新增模块**:
  - `java-harness-workflow`: 添加智能模型选择相关类
  - `com.chachamaru.harness.model`: 模型选择数据模型
  - `com.chachamaru.harness.workflow.orchestration`: EffortRouter 集成
- **降级链机制**:
  1. 主要模型（如 env:ANTHROPIC_DEFAULT_HAIKU_MODEL）
  2. 默认模型（env:ANTHROPIC_MODEL）
  3. 安全模型（glm-4.7 硬编码兜底）
- **复杂度评分规则**:
  - 文件数：4 个文件以上 (+1)
  - 目录：包含 core/、guardrails/、security/ (+1)
  - 关键字：包含 architecture、security、design、migration (+1)
  - 失败历史：有同任务的失败记录 (+2)
  - 显式指定：PM 模板中记载 `effort: high` / `effort: xhigh` (+3)

### Configuration Examples
```json
{
  "modelSelection": {
    "enabled": true,
    "strategy": "effortBased",
    "tierMapping": {
      "fast": {
        "scoreRange": [0, 2],
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      }
    }
  }
}
```

### Environment Variables
```bash
export ANTHROPIC_MODEL="glm-4.7"
export ANTHROPIC_DEFAULT_FABLE_MODEL="claude-fable-5-20250514"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_DEFAULT_OPUS_MODEL="claude-opus-4-20250514"
```

## [5.0.0-java] - 2026-08-08 (Phase 7: 双平台支持)

### Added
- **🌐 双平台支持**: Java Harness 现在同时支持 Claude Code 和 Codex CLI 平台
  - 实现平台自动检测机制（`PlatformDetector`）
  - 创建配置兼容层（`ConfigCompatLayer`）统一配置解析
  - 支持平台特定的配置文件路径（`.claude/config.toml` 和 `.codex/config.toml`）
  - 实现统一的 TOML 解析器（`TomlParser`）用于配置文件
- **📦 配置文件系统**:
  - 更新 `plugin.json` 添加双平台支持信息
  - 创建 `marketplace.json` 用于 Claude Marketplace 发布
  - 创建 `.codex-plugin/plugin.json` 专用于 Codex 平台
- **🧪 集成测试**: 添加22个端到端集成测试验证双平台功能
  - `ConfigCompatLayerTest`: 11个配置兼容层测试
  - `DualPlatformIntegrationTest`: 11个双平台集成测试
  - 测试覆盖率达到目标要求（100%通过率）
- **📖 文档更新**:
  - 更新 README.md 添加双平台安装指南
  - 添加配置说明章节和配置优先级说明
  - 创建平台特定的配置示例
- **🔧 配置优先级链**:
  - 平台特定配置 > 标准harness.toml > 平台默认值
  - 自动回退机制确保配置健壮性

### Changed
- **平台检测逻辑**: 从单一平台支持升级为双平台自动检测
- **配置解析**: 从单一配置路径升级为多路径优先级加载
- **文档结构**: 添加双平台支持说明和配置指南
- **JVM 参数修正**: 修复 pom.xml 中的 `CodeCacheSize` 参数为 `ReservedCodeCacheSize`

### Beta Features
- **🚧 Codex CLI 平台支持**（实验性）:
  - 平台检测：✅ 支持
  - 配置兼容：✅ 支持
  - 技能执行：🚧 部分支持
  - 高级工作流：🚧 有限支持
  - 建议优先使用 Claude Code 获得完整功能体验

### Technical Details
- **新增模块**:
  - `java-harness-collaboration`: 平台检测和配置兼容层
  - `com.chachamaru.harness.collaboration.platform`: 平台枚举和检测器
  - `com.chachamaru.harness.collaboration.config`: 配置兼容层和解析器
- **配置文件**:
  - `plugin.json`: 添加 `platforms` 字段支持双平台
  - `.codex-plugin/plugin.json`: Codex 专用配置
  - `marketplace.json`: Marketplace 发布配置
- **环境变量**:
  - `CLAUDE_CODE_HARNESS`: Claude Code 环境标识
  - `CODEX_CLI`: Codex CLI 环境标识

### Migration Notes
- **从 Claude Code 迁移到 Codex**:
  - 复制 `.claude/config.toml` 到 `.codex/config.toml`
  - 设置 `CODEX_CLI=1` 环境变量
  - 验证配置：`harness config validate`
- **配置文件兼容**:
  - 现有 `harness.toml` 文件继续有效
  - 平台特定配置会覆盖通用配置

### Documentation
- 完整的双平台安装指南
- 配置文件说明和优先级文档
- 平台特定配置示例
- Beta 功能限制说明

## [4.2.0] - 2026-08-08

### Added
- **文档系统重建**: 建立完整的分层文档体系
  - 创建中文 README.md 和英文 README_EN.md 独立文档
  - 新增 docs/README.md 文档导航索引
  - 新增 docs/user-guide/installation.md 详细安装指南
  - 新增 docs/developer-guide/architecture.md 架构设计文档
  - 新增 docs/reference/api-reference.md API 参考框架
- **文档结构优化**: 建立三层文档分类（用户指南、开发者指南、参考文档）
- **历史文档归档**: 将22个历史文档归档到 docs/reference/ 目录
- **多平台Native Image构建脚本**，自动检测平台并生成对应二进制文件
- **与Go版本完全一致的二进制文件命名格式**（harness-{os}-{arch}）
- **支持平台**：macOS（Intel/Apple Silicon）、Linux（AMD64/ARM64）、Windows（AMD64）

### Changed
- **文档组织**: 从27个独立文档重构为结构化的文档体系
- **README 内容**: 扩展为包含完整功能特性和架构设计的综合文档
- **中英文支持**: 采用独立文件结构，便于双语维护
- **更新Maven Native Image插件配置**支持动态二进制文件命名
- **重构包装脚本**以支持新的文件结构，保持向后兼容性
- **优化构建流程**，支持自动平台检测

### Fixed
- **中文排版**: 修正数字与中文之间的空格问题
- **文档链接**: 更新所有文档间的交叉引用链接
- **Claude插件二进制文件识别问题**，现在能够正确识别和调用
- **Windows平台包装脚本向后兼容性问题**
- **二进制文件权限设置问题**

### Documentation
- 完整的项目文档重建，包含用户指南和开发者指南
- 建立清晰的文档维护和更新流程
- 提供26个文档文件，覆盖项目各个方面
- 文档验证清单确保所有用户需求满足

### Technical Details
- **二进制文件**现在直接生成在bin目录下，采用扁平化结构
- **文档体系**: 采用三层结构（user-guide、developer-guide、reference）
- **构建脚本**：`scripts/build/build-all-native.sh`
- **包装脚本**：`bin/harness`（Unix）/ `bin/harness.bat`（Windows）
- **保留旧的子目录结构**作为向后兼容的回退机制
- **归档文档**: 历史文档保存在 docs/reference/backup/ 和 docs/reference/superpowers-archive/

## [4.1.0] - Previous Release
- Initial version changes from previous releases