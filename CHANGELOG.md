# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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