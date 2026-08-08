# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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