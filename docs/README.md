# Java Harness Documentation Structure

本文档说明 Java Harness 项目的文档组织结构。

## 📁 文档目录结构

```
docs/
├── README.md                       # 文档导航和索引
├── reference/                      # 参考文档（已归档）
│   ├── backup/                    # 历史技术文档备份
│   ├── multi-platform-hooks-backup/  # 多平台 Hooks 配置备份
│   └── superpowers-archive/       # Superpowers 临时文档归档
├── user-guide/                     # 用户指南
│   ├── installation.md            # 详细安装指南
│   ├── quick-start.md             # 快速入门指南
│   ├── configuration.md           # 配置指南
│   └── troubleshooting.md         # 故障排除指南
├── developer-guide/                # 开发者指南
│   ├── architecture.md            # 架构设计文档
│   ├── development.md              # 开发环境设置
│   ├── testing.md                 # 测试指南
│   ├── contributing.md            # 贡献指南
│   └── release-process.md         # 发布流程
└── reference/                      # 参考文档
    ├── cli-commands.md            # CLI 命令参考
    ├── guardrail-rules.md         # 安全规则参考
    ├── hook-protocol.md           # Hook 协议规范
    └── api-reference.md           # API 参考文档
```

## 📖 文档分类说明

### 用户指南 (user-guide/)
面向最终用户的文档，包含如何安装、配置和使用 Java Harness。

- **installation.md**: 详细的安装步骤，支持多种平台和安装方式
- **quick-start.md**: 5分钟快速入门，包含基本使用示例
- **configuration.md**: 完整的配置选项说明和最佳实践
- **troubleshooting.md**: 常见问题和解决方案

### 开发者指南 (developer-guide/)
面向开发者的文档，包含架构设计、开发规范、贡献指南。

- **architecture.md**: 系统架构设计、模块关系、技术选型
- **development.md**: 开发环境设置、构建过程、代码结构
- **testing.md**: 测试策略、测试规范、如何编写测试
- **contributing.md**: 贡献流程、代码规范、提交规范
- **release-process.md**: 版本发布流程和规范

### 参考文档 (reference/)
技术参考文档，提供详细的 API、命令、规则说明。

- **cli-commands.md**: 86个 CLI 命令的完整参考
- **guardrail-rules.md**: 27个安全规则的详细说明
- **hook-protocol.md**: Hook 协议的技术规范
- **api-reference.md**: API 接口详细文档（预留）

### 归档文档 (reference/backup/)
历史文档和临时文档的归档，保留有价值的历史信息。

- **backup/**: 历史技术文档（BUILD_INSTRUCTIONS.md 等）
- **multi-platform-hooks-backup/**: 多平台 Hooks 配置备份
- **superpowers-archive/**: Superpowers 技能框架相关文档

## 🎯 文档设计原则

### 1. 用户导向
- 用户指南使用通俗语言，避免过度技术化
- 提供丰富的示例和截图
- 包含故障排除和常见问题

### 2. 开发者友好
- 开发者指南提供详细的技术说明
- 包含代码示例和架构图
- 明确开发规范和最佳实践

### 3. 维护性
- 文档结构清晰，易于更新
- 避免重复内容，使用交叉引用
- 版本化文档，与代码同步

### 4. 扩展性
- 预留未来文档扩展空间
- 模块化组织，便于添加新文档
- 支持多语言文档（如需要）

## 🔄 文档更新流程

1. **新建文档**: 在对应目录下创建 Markdown 文件
2. **更新索引**: 在 docs/README.md 中添加文档链接
3. **交叉引用**: 更新相关文档中的链接
4. **验证**: 检查所有链接和引用的正确性
5. **提交**: 使用规范的 commit message

## 📝 文档编写规范

### Markdown 规范
- 使用标准的 GitHub Flavored Markdown
- 代码块指定语言（```java, ```bash）
- 使用相对路径引用其他文档（../user-guide/installation.md）

### 标题层级
- 一级标题（#）用于文档标题
- 二级标题（##）用于主要章节
- 三级标题（###）用于子章节
- 四级标题（####）用于更细分的内容

### 代码示例
- 提供完整可运行的示例
- 包含必要的注释说明
- 显示预期输出

### 链接规范
- 内部文档使用相对路径
- 外部链接使用 HTTPS
- 提供有意义的链接文本

---

**文档结构版本**: 1.0  
**最后更新**: 2026-08-08  
**维护者**: Java Harness Team
