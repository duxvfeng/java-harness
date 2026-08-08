# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.2.0] - 2026-08-08

### Added
- 多平台Native Image构建脚本，自动检测平台并生成对应二进制文件
- 与Go版本完全一致的二进制文件命名格式（harness-{os}-{arch}）
- 支持平台：macOS（Intel/Apple Silicon）、Linux（AMD64/ARM64）、Windows（AMD64）

### Changed
- 更新Maven Native Image插件配置支持动态二进制文件命名
- 重构包装脚本以支持新的文件结构，保持向后兼容性
- 优化构建流程，支持自动平台检测

### Fixed
- Claude插件二进制文件识别问题，现在能够正确识别和调用
- Windows平台包装脚本向后兼容性问题
- 二进制文件权限设置问题

### Technical Details
- 二进制文件现在直接生成在bin目录下，采用扁平化结构
- 构建脚本：`scripts/build/build-all-native.sh`
- 包装脚本：`bin/harness`（Unix）/ `bin/harness.bat`（Windows）
- 保留旧的子目录结构作为向后兼容的回退机制

## [4.1.0] - Previous Release
- Initial version changes from previous releases