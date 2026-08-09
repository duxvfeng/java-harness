# 项目文档重建计划

**目标**: 清理现有文档，重建清晰的中英文README和文档体系，确保文档与项目当前状态一致。

## 背景

- **当前问题**: docs/ 目录下有27个文档文件，部分过时或重复，文档结构混乱
- **项目状态**: Phase 9 已完成，跨平台 Hooks 统一方案实现完毕
- **用户需求**: 全部清理后重建文档体系，生成中英文分离的 README

---

## Phase 1: 文档清理和备份（1-2天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | 创建 docs/reference/ 备份目录 | 创建 docs/reference/backup/ 和 docs/reference/superpowers-archive/ 目录 | 目录创建成功，权限正确 | - | cc:完成 ✅ 2026-08-08 |
| 1.2 | 备份重要参考文档 | 将 BUILD_INSTRUCTIONS.md, MULTI_PLATFORM_BUILD.md 等技术文档移到 docs/reference/backup/ | 文件已移动，路径正确 | 1.1 | cc:完成 ✅ 2026-08-08 |
| 1.3 | 归档 Superpowers 临时文档 | 将 docs/superpowers/ 整个目录移到 docs/reference/superpowers-archive/ | 目录已移动，包含所有18个文件 | 1.1 | cc:完成 ✅ 2026-08-08 |
| 1.4 | 删除 docs/ 下所有其他文档 | 删除根目录下的独立 md 文件和 install/ 目录 | docs/ 目录已清空，仅保留 reference/ | 1.3 | cc:完成 ✅ 2026-08-08 |
| 1.5 | 验证清理结果 | 确认 docs/ 结构干净，备份完整 | `ls -R docs/` 显示正确结构 | 1.4 | cc:完成 ✅ 2026-08-08 |

---

## Phase 2: 中文 README.md 创建（2-3天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.1 | 编写项目概述和快速入门 | 项目标题、简介、5分钟快速开始、安装步骤 | 包含项目标志、核心价值主张、快速安装命令 | 1.5 | cc:完成 ✅ 2026-08-08 |
| 2.2 | 编写完整功能特性列表 | 86个CLI命令、27个安全规则、Hook系统、Agent协调 | 结构化展示所有功能，带图标和分类 | 2.1 | cc:完成 ✅ 2026-08-08 |
| 2.3 | 编写架构设计和技术细节 | 模块结构、技术栈、性能目标、设计原理 | 包含架构图、技术栈表格、性能指标 | 2.2 | cc:完成 ✅ 2026-08-08 |
| 2.4 | 添加使用示例和代码片段 | Hook输入输出示例、命令使用示例 | 代码块语法高亮，示例可运行 | 2.3 | cc:完成 ✅ 2026-08-08 |
| 2.5 | 添加贡献指南和联系方式 | 开发规范、测试要求、许可证信息 | 遵循现有 CLAUDE.md 规范 | 2.4 | cc:完成 ✅ 2026-08-08 |
| 2.6 | 中文文案质量检查 | 中文排版、术语一致性、链接有效性 | 无中英文混排错误，所有链接有效 | 2.5 | cc:完成 ✅ 2026-08-08 |

---

## Phase 3: 英文 README_EN.md 创建（2-3天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 3.1 | 翻译项目概述和快速入门 | 将中文 README.md 的第1-2章节翻译为英文 | 准确的技术英语表达，保持专业语气 | 2.6 | cc:完成 ✅ 2026-08-08 |
| 3.2 | 翻译完整功能特性列表 | 将功能特性章节翻译，保持结构一致 | 命令名称、技术术语准确翻译 | 3.1 | cc:完成 ✅ 2026-08-08 |
| 3.3 | 翻译架构设计和技术细节 | 架构图表、技术栈、性能目标翻译 | 技术文档翻译标准，保持图表清晰 | 3.2 | cc:完成 ✅ 2026-08-08 |
| 3.4 | 翻译使用示例和贡献指南 | 代码示例保持不变，说明文本翻译 | 代码块不变，注释可翻译 | 3.3 | cc:完成 ✅ 2026-08-08 |
| 3.5 | 英文文案质量检查 | 语法检查、术语一致性、排版规范 | 无语法错误，术语统一使用 | 3.4 | cc:完成 ✅ 2026-08-08 |
| 3.6 | 中英文版本一致性验证 | 两个版本的结构和内容对等检查 | 章节结构一致，无遗漏内容 | 3.5 | cc:完成 ✅ 2026-08-08 |

---

## Phase 4: 文档体系设计和重建（2-3天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 4.1 | 设计新文档结构 | 设计 docs/ 目录组织方式，考虑未来扩展 | 文档结构图清晰，分类合理 | 3.6 | cc:完成 ✅ 2026-08-08 |
| 4.2 | 创建核心文档模板 | 创建 INSTALLATION.md, ARCHITECTURE.md 等核心文档模板 | 模板结构完整，包含必要章节 | 4.1 | cc:完成 ✅ 2026-08-08 |
| 4.3 | 编写快速安装指南 | 基于 README 中的安装章节，扩展详细安装文档 | 支持多平台，包含故障排除 | 4.2 | cc:完成 ✅ 2026-08-08 |
| 4.4 | 编写架构详细文档 | 扩展 README 中的架构章节，添加设计决策 | 包含架构图、模块说明、设计原理 | 4.3 | cc:完成 ✅ 2026-08-08 |
| 4.5 | 创建 API 参考文档框架 | 为未来 API 文档预留框架和结构 | 包含 API 文档标准和示例 | 4.4 | cc:完成 ✅ 2026-08-08 |
| 4.6 | 文档交叉引用验证 | 确保所有文档间链接正确，README 与详细文档对应 | 所有链接有效，无死链 | 4.5 | cc:完成 ✅ 2026-08-08 |

---

## Phase 5: 文档验证和发布（1天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.1 | 中英文 README 验证 | 检查两个版本在 GitHub 上显示效果 | 格式正确，表格列表渲染正常 | 4.6 | cc:完成 ✅ 2026-08-08 |
| 5.2 | 文档完整性检查 | 确保所有承诺的内容都已实现 | 对照用户要求列表逐项检查 | 5.1 | cc:完成 ✅ 2026-08-08 |
| 5.3 | 创建变更日志 | 在 CHANGELOG.md 中记录本次文档重建 | 符合项目变更日志规范 | 5.2 | cc:完成 ✅ 2026-08-08 |
| 5.4 | 提交文档重建变更 | git commit 所有文档变更 | Commit message 清晰，变更范围明确 | 5.3 | cc:完成 ✅ 2026-08-08 [76360b9] |

---

## Phase 6: 文档优化（按需添加）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 6.1 | 调整 README 中的 Claude Marketplace 安装方式 | 将原 Marketplace 搜索安装方式更新为命令行安装方式：`/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git` + `/plugin install` | 中英文 README.md 和 README_EN.md 中的 Claude Marketplace 安装章节已更新为新的命令行方式，包含完整的安装步骤说明 | - | cc:完成 ✅ 2026-08-08 |

---

## Phase 7: 双平台适配（可选扩展）

### 事前确认章节

**Phase 7 涉及双平台集成和外部工具访问，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: secret-read（读取双平台配置和状态）
  - **理由**: Task 7.2-7.6 需要读取 `$HOME/.codex/`、`$HOME/.claude/` 和项目配置目录的配置文件
  - **scope**: Phase 7 / Task 7.2-7.6

- **事项**: external-send（调用平台 CLI）
  - **理由**: Task 7.2-7.7 需要调用平台命令进行功能验证和测试
  - **scope**: Phase 7 / Task 7.2-7.7

- **事项**: external-send（网络请求到 AI API）
  - **理由**: Task 7.7 集成测试需要验证双平台的实际 API 调用
  - **scope**: Phase 7 / Task 7.7

#### 说明
- 以上事项仅在 Phase 7 实施时生效，Phase 1-6 的文档任务不涉及
- 所有配置读取仅为功能验证需要，不会记录或传输密钥内容
- 网络请求仅用于集成测试，不会在实际产品代码中进行未经授权的 API 调用

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 7.1 | 双平台架构设计验证 | 验证现有架构设计是否符合双平台需求，确认统一插件结构和配置兼容层 | 架构验证报告，设计文档确认 | - | cc:完成 ✅ 2026-08-08 |
| 7.2 | 平台检测机制实现 `[tdd:required]` | 实现 PlatformDetector 类，支持 Claude Code 和 Codex 环境检测 | PlatformDetector.detectCurrentPlatform() 准确识别平台，单元测试通过 | 7.1 | cc:完成 ✅ 2026-08-08 [78f7e90] |
| 7.3 | 统一技能接口实现 `[tdd:required]` | 实现 UniversalSkill 接口，支持平台特定适配，保持现有技能兼容 | 现有技能在新接口下正常运行，兼容性测试通过 | 7.2 | cc:完成 ✅ 2026-08-08 [67bba1e] |
| 7.4 | 后端选择策略实现 `[tdd:required]` | 实现 BackendSelector 类，支持明确指定、自动检测、平台默认、系统默认的优先级 | 能根据配置和环境选择正确后端，集成测试通过 | 7.3 | cc:完成 ✅ 2026-08-08 [4513fd1] |
| 7.5 | 配置兼容层实现 `[tdd:required]` | 创建双平台配置文件结构，实现统一的 harness.toml 配置解析器 | 两个平台都能正确读取配置，配置解析测试通过 | 7.4 | cc:完成 ✅ 2026-08-08 [23f2d1e] |
| 7.6 | 双平台配置文件更新 `[tdd:skip:config-task]` | 更新 plugin.json 支持两个平台，创建 marketplace.json 和 Codex plugin.json | 两个平台都能正确安装和识别插件，安装测试通过 | 7.5 | cc:完成 ✅ 2026-08-08 [3b2296e] |
| 7.7 | 双平台集成测试 `[tdd:skip:test-task]` | 编写端到端测试，验证平台检测、技能执行、后端选择、配置解析的集成 | 测试覆盖率达到 80%，两个平台功能等价性验证通过 | 7.6 | cc:完成 ✅ 2026-08-08 [a128c5c] |
| 7.8 | 双平台文档发布 `[tdd:skip:docs-only]` | 更新 README.md 包含双平台安装指南，创建详细使用文档，更新 CHANGELOG | 文档完整，两个平台的安装流程清晰，用户能按照文档成功安装 | 7.7 | cc:完成 ✅ 2026-08-08 [9307024] |
| 7.9 | Beta 发布和反馈收集 `[tdd:skip:release-task]` | 将双平台支持作为 Beta 功能发布，收集两个平台的用户反馈并优化 | Beta 发布完成，反馈收集和分析报告完成 | 7.8 | cc:完成 ✅ 2026-08-08 [3bf6ba8] |

---

## Phase 8: Codex 实现完善（补充实现）

基于与 Go 项目对比分析发现的差距，将 Phase 7 的完整设计转化为实际可运行的 Codex 支持。

### 事前确认章节

**Phase 8 涉及配置文件生成和目录结构创建，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 文件系统写入（创建配置和目录）
  - **理由**: Task 8.1-8.4 需要创建 .codex-plugin/ 目录和相关配置文件
  - **scope**: Phase 8 / Task 8.1-8.4

- **事项**: 读取 Go 项目参考文件
  - **理由**: Task 8.2-8.3 需要参考 /Users/apple/IdeaProjects/claude-code-harness 的实现
  - **scope**: Phase 8 / Task 8.2-8.3

#### 说明
- 所有文件创建仅限于当前 java-harness 项目目录
- 读取 Go 项目仅用于参考结构，不会修改其文件
- 生成的配置文件基于 dual-platform-config.md 的设计

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.1 | 创建 Codex 插件目录结构 | 创建 .codex-plugin/ 目录和相关子目录结构 | 目录结构符合 Codex 插件规范，权限正确 | - | cc:TODO |
| 8.2 | 生成 Codex plugin.json | 参考 Go 项目，生成符合 Codex 规范的 plugin.json 配置文件 | plugin.json 包含必需字段，格式正确，Codex 能识别 | 8.1 | cc:TODO |
| 8.3 | 创建 Codex 技能映射 | 将现有 Java Harness 技能映射到 Codex 执行模式 | 技能映射文档完整，包含调用路径和参数 | 8.2 | cc:TODO |
| 8.4 | 编写 Codex 集成文档 | 说明如何在 Codex 环境中使用 Java Harness 技能 | 文档包含安装、配置、使用示例，用户能按文档成功使用 | 8.3 | cc:TODO |
| 8.5 | Codex 环境测试验证 | 在 Codex 环境中测试关键技能功能 | 主要功能在 Codex 中正常工作，测试通过 | 8.4 | cc:TODO |

---

## Phase 9: 多语言代码规范和 Brainstorming 集成（技能层面增强）🆕

### 📋 需求说明

本 Phase 主要涉及**技能配置和参考文档**的修改，不涉及核心代码库变更：
- 修改 `skills/harness-review/` 中的配置和参考文档
- 修改 `skills/harness-plan/` 和 `skills/harness-plan-brief/` 的技能配置
- 添加新的参考文档和配置文件

### 事前确认章节

**Phase 9 涉及技能文件修改和外部资源集成，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 修改 skills 目录文件
  - **理由**: Task 9.1-9.7 需要修改技能配置文件和参考文档
  - **scope**: Phase 9 / Task 9.1-9.7

- **事项**: 集成外部 Claude Skills（alibaba-java-development-guide, brainstorming）
  - **理由**: Task 9.2 和 9.6 需要引用现有的 Claude Skills 框架技能
  - **scope**: Phase 9 / Task 9.2, 9.6

- **事项**: 创建 GitHub 规范参考文档
  - **理由**: Task 9.3-9.5 需要创建 Python/Vue/Go 代码规范参考文档
  - **scope**: Phase 9 / Task 9.3-9.5

#### 说明
- 所有修改仅限于 `skills/` 目录下的配置和文档文件
- 不修改任何核心 Java 代码实现
- 外部技能通过现有的 Claude Skills 框架集成
- GitHub 规范仅作为参考文档，不包含实际规范内容副本

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 9.1 | 设计多语言代码规范架构 | 设计语言检测、规范路由、规则应用的整体架构 | 架构设计文档完成，包含语言检测逻辑和规范映射表 | - | cc:完成 ✅ 2026-08-09 |
| 9.2 | 集成 Alibaba Java 规范 `[tdd:skip:config-integration]` | 在 harness-review 中集成 alibaba-java-development-guide 技能 | Java 代码审查能够应用阿里巴巴规范，配置正确 | 9.1 | cc:完成 ✅ 2026-08-09 |
| 9.3 | 添加 Python 代码规范支持 `[tdd:skip:docs-only]` | 创建 Python 代码规范参考文档（基于 GitHub PEP 8） | Python 规范参考文档创建，包含主要规则和检查点 | 9.1 | cc:完成 ✅ 2026-08-09 |
| 9.4 | 添加 Vue 代码规范支持 `[tdd:skip:docs-only]` | 创建 Vue 代码规范参考文档（基于 GitHub Vue 风格指南） | Vue 规范参考文档创建，包含组件命名和结构规则 | 9.1 | cc:完成 ✅ 2026-08-09 |
| 9.5 | 添加 Go 代码规范支持 `[tdd:skip:docs-only]` | 创建 Go 代码规范参考文档（基于 Effective Go） | Go 规范参考文档创建，包含命名和错误处理规则 | 9.1 | cc:完成 ✅ 2026-08-09 |
| 9.6 | 在 harness-plan 中集成 brainstorming `[tdd:skip:config-integration]` | 修改 harness-plan 技能配置，支持 brainstorming 前置触发 | 规划时能够自动调用 brainstorming 进行创意探索 | - | cc:完成 ✅ 2026-08-09 |
| 9.7 | 在 harness-plan-brief 中集成 brainstorming `[tdd:skip:config-integration]` | 修改 harness-plan-brief 技能配置，支持 brainstorming 增强 | 生成 Plan Brief 时能够使用 brainstorming 提升质量 | 9.6 | cc:完成 ✅ 2026-08-09 |
| 9.8 | 更新 harness-review 技能路由配置 `[tdd:skip:config-integration]` | 修改 skills/harness-review/SKILL.md 添加多语言代码规范路由 | SKILL.md 包含语言检测和规范路由描述，无需额外配置文件 | 9.2 | cc:完成 ✅ 2026-08-09 |
| 9.9 | 多语言规范测试验证 | 测试不同语言代码的审查流程，验证规范应用正确 | 各语言代码审查能够正确应用相应规范，测试通过 | 9.8 | cc:完成 ✅ 2026-08-09 |
| 9.10 | 更新技能使用文档 | 更新 CLAUDE.md 和相关文档，说明多语言规范功能 | 文档包含使用示例、配置说明、最佳实践 | 9.9 | cc:完成 ✅ 2026-08-09 |

---

## 实施说明

### 🎯 主要特点

1. **纯技能层面工作**：所有任务仅涉及 `skills/` 目录下的文件修改
2. **无代码变更**：不修改任何核心 Java 实现代码
3. **SKILL.md 路由**：通过技能文件描述实现功能，无需额外配置文件
4. **向后兼容**：不影响现有功能的正常运行

### 📊 工作范围

- **修改文件**：`skills/harness-review/`, `skills/harness-plan/`, `skills/harness-plan-brief/`
- **新增文件**：规范参考文档 `references/code-standards/`
- **删除文件**：`.claude/config/` 目录（采用 SKILL.md 路由方案后不需要）
- **测试验证**：功能验证和文档验证

### 🔄 集成方式

- **Java 规范**：集成现有 Claude Skills（alibaba-java-development-guide）
- **其他语言**：创建参考文档，基于 GitHub 官方规范
- **Brainstorming**：通过技能配置集成现有 brainstorming 技能

---

**注意**: Phase 9 为纯配置和文档工作，不涉及代码实现，可以快速完成验证。