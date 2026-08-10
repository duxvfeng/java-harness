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

## Phase 10: 智能分支隔离检测（核心功能增强）🆕

### 📋 需求说明

本 Phase 为 `harness-work` 添加智能分支检测功能，根据当前分支状态自动决定是否启用隔离：
- **主分支保护**：在 master/main 分支上自动启用隔离
- **Feature 分支灵活性**：在 feature 分支上可选隔离
- **智能跳过**：已隔离状态自动检测并跳过

### 事前确认章节

**Phase 10 涉及 Git 操作和状态文件创建，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: Git 状态读取（分支检测）
  - **理由**: Task 10.1-10.4 需要读取当前分支状态和 worktree 信息
  - **scope**: Phase 10 / Task 10.1-10.4

- **事项**: 文件系统写入（状态文件和配置）
  - **理由**: Task 10.4, 10.6, 10.7 需要创建 `.claude/state/` 和配置文件
  - **scope**: Phase 10 / Task 10.4, 10.6, 10.7

- **事项**: Git worktree 操作（分支隔离）
  - **理由**: Task 10.3 集成点需要支持创建和管理 worktree
  - **scope**: Phase 10 / Task 10.3

#### 说明
- 所有 Git 操作仅限于分支检测，不修改现有代码
- 状态文件记录用户决策和隔离状态，不包含敏感信息
- worktree 创建仅在用户同意或主分支强制保护时执行
- 与现有 `--isolate-branch` 功能向后兼容

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 10.1 | 实现分支检测核心函数 `[tdd:required]` | 实现 `detect_branch_isolation_strategy()` 函数，支持主分支/feature分支/worktree检测 | 函数能正确识别分支状态并返回策略，单元测试覆盖所有场景 | - | cc:TODO |
| 10.2 | 实现用户交互逻辑 `[tdd:required]` | 实现 `handle_branch_isolation()` 函数，处理强制隔离/可选隔离/已隔离三种策略 | 用户交互流程清晰，能正确处理用户选择和取消 | 10.1 | cc:TODO |
| 10.3 | 集成到 harness-work Phase A `[tdd:required]` | 修改 harness-work SKILL.md，在准备阶段添加智能分支检测步骤 | Phase A 流程包含分支检测，与现有执行模式兼容 | 10.2 | cc:TODO |
| 10.4 | 实现状态文件管理 `[tdd:required]` | 创建 `.claude/state/branch-isolation-decision.json` 记录用户决策和隔离状态 | 状态文件格式正确，包含所有必需字段 | 10.3 | cc:TODO |
| 10.5 | 添加配置支持 `[tdd:required]` | 在 `.claude/settings.json` 中添加 `branchIsolation` 配置项 | 配置项完整，支持策略配置和覆盖 | 10.4 | cc:TODO |
| 10.6 | 实现配置读取和解析 `[tdd:required]` | 实现配置文件读取逻辑，支持默认值和环境变量覆盖 | 能正确读取配置并应用策略，配置优先级正确 | 10.5 | cc:TODO |
| 10.7 | 实现策略配置逻辑 `[tdd:required]` | 实现主分支/feature分支策略配置，支持 force/ask/skip 三种模式 | 策略配置生效，用户可自定义行为 | 10.6 | cc:TODO |
| 10.8 | 实现 Git 检测失败处理 `[tdd:required]` | 添加 Git 命令失败的错误处理和用户提示 | Git 检测失败时提供清晰的错误信息和解决建议 | 10.3 | cc:TODO |
| 10.9 | 实现 Worktree 创建失败处理 `[tdd:required]` | 添加 worktree 创建失败的错误处理和回退机制 | Worktree 创建失败时提供具体错误信息和替代方案 | 10.3 | cc:TODO |
| 10.10 | 实现用户取消处理 `[tdd:required]` | 处理用户取消隔离的场景，记录决定并中止或继续 | 用户取消时正确记录决策，执行流程符合预期 | 10.2 | cc:TODO |
| 10.11 | 编写单元测试 `[tdd:required]` | 为检测函数、交互逻辑、配置解析编写单元测试 | 测试覆盖率达到 80%，所有核心逻辑有测试 | 10.10 | cc:TODO |
| 10.12 | 编写集成测试 `[tdd:required]` | 测试主分支/feature分支/已隔离三种场景的端到端流程 | 集成测试覆盖所有用户场景，测试通过 | 10.11 | cc:TODO |
| 10.13 | 更新用户文档 `[tdd:skip:docs-only]` | 更新 README.md 和相关文档，说明智能分支隔离功能 | 文档包含使用示例、配置说明、场景说明 | 10.12 | cc:TODO |

---

## 实施说明

### 🎯 主要特点

1. **智能检测**：根据分支状态自动决策，无需手动指定
2. **主分支保护**：防止意外在主分支上工作造成不稳定
3. **灵活性**：feature 分支允许用户选择隔离策略
4. **向后兼容**：保留显式 `--isolate-branch` 标志的优先级
5. **配置驱动**：支持通过配置文件自定义默认行为

### 📊 工作范围

- **修改文件**：`skills/harness-work/SKILL.md`, `skills/harness-work/references/execution-modes.md`
- **新增文件**：状态文件 `.claude/state/branch-isolation-decision.json`
- **配置文件**：`.claude/settings.json` 添加 `branchIsolation` 配置项
- **测试验证**：单元测试、集成测试、跨平台测试

### 🔄 与现有功能集成

- **与 `--isolate-branch` 兼容**：显式标志优先级最高
- **与 `using-git-worktrees` 兼容**：遵循现有 worktree 管理最佳实践
- **与分支隔离模式兼容**：扩展现有 branch-isolation 功能

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

## Phase 11: 会话保存和恢复系统（Context 满问题解决）🆕

### 📋 需求说明

本 Phase 为 Java Harness 添加会话持久化和恢复功能，解决大型任务中 context 满的问题：
- **Token 自动检测**：当对话接近 token 限制时（80%/90%）自动触发保存
- **完整状态保存**：保存对话历史、任务状态、文件修改、Git 状态到本地存储
- **智能恢复提示**：新会话启动时自动显示恢复选项和 AI 决策
- **技能命令接口**：提供手动保存、恢复和管理的命令接口
- **存储管理**：自动清理、压缩存储、健康检查

### 事前确认章节

**Phase 11 涉及文件系统写入和配置修改，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 文件系统写入（创建会话保存目录和文件）
  - **理由**: Task 11.1-11.13 需要创建 `.claude/state/session-saves/` 目录和相关文件
  - **scope**: Phase 11 / Task 11.1-11.13

- **事项**: 读取对话历史和状态信息
  - **理由**: Task 11.4-11.7 需要读取对话历史、任务状态、Git 状态用于保存
  - **scope**: Phase 11 / Task 11.4-11.7

- **事项**: 环境变量读取（Token 检测）
  - **理由**: Task 11.4 需要读取 `CLAUDE_TOKEN_COUNT` 等环境变量进行 token 检测
  - **scope**: Phase 11 / Task 11.4

#### 说明
- 所有文件操作仅限于项目 `.claude/state/` 目录内
- Token 检测仅用于判断保存时机，不读取敏感对话内容
- 会话数据存储在本地项目目录，不上传到外部服务
- 配置修改仅限 `harness.toml` 和技能文件

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 11.1 | 创建会话数据模型 `[tdd:required]` | 实现 SessionMetadata, SessionSummary, SessionSaveResult, TokenUsageInfo, RestoreSuggestion 数据类 | 所有数据类包含必要字段，JSON 序列化正常，单元测试通过 | - | cc:完成 ✅ 2026-08-09 |
| 11.2 | 实现存储层 `[tdd:required]` | 创建 SessionStorage 接口和 FileSystemStorage 实现，支持保存、加载、列表、删除、清理、健康检查 | 存储功能完整，文件结构正确，压缩功能正常，测试通过 | 11.1 | cc:完成 ✅ 2026-08-09 |
| 11.3 | 实现 SessionSaveManager `[tdd:required]` | 创建核心保存管理器，支持自动保存、手动保存、并发控制、间隔限制、空间检查 | 保存管理逻辑正确，并发控制有效，空间检查正常，测试通过 | 11.2 | cc:完成 ✅ 2026-08-09 |
| 11.4 | 实现 Token 监控器 `[tdd:required]` | 创建 TokenMonitor 类，支持环境变量检测、降级策略、阈值判断 | Token 检测准确，降级策略有效，阈值判断正确，测试通过 | 11.3 | cc:完成 ✅ 2026-08-09 |
| 11.5 | 实现 session-monitor Hook 扩展 `[tdd:required]` | 扩展现有 session-monitor hook，添加 token 自动检测和保存触发逻辑 | Hook 扩展正常工作，自动保存触发准确，与现有功能兼容，测试通过 | 11.4 | cc:TODO |
| 11.6 | 实现配置管理 `[tdd:required]` | 创建 SessionConfigLoader，支持 harness.toml 配置、环境变量、默认值 | 配置加载正确，优先级正确，默认值合理，测试通过 | 11.5 | cc:完成 ✅ 2026-08-09 |
| 11.7 | 实现 SessionRestoreManager `[tdd:required]` | 创建恢复管理器，支持恢复检测、摘要生成、AI 决策、完整性验证 | 恢复逻辑正确，摘要生成准确，AI 决策合理，测试通过 | 11.6 | cc:完成 ✅ 2026-08-09 |
| 11.8 | 实现 session-init Hook 扩展 `[tdd:required]` | 扩展现有 session-init hook，添加恢复提示生成和显示逻辑 | Hook 扩展正常工作，恢复提示显示正确，与现有功能兼容，测试通过 | 11.7 | cc:TODO |
| 11.9 | 创建会话管理技能 `[tdd:skip:docs-only]` | 创建 skills/harness-session/SKILL.md 和参考文档，定义技能命令接口 | 技能定义完整，命令格式正确，参考文档详细，符合技能规范 | 11.8 | cc:TODO |
| 11.10 | 注册会话管理命令 `[tdd:required]` | 在 CommandRegistry 中注册会话管理命令，实现 save, restore, list, show, cleanup 命令 | 命令注册正确，功能完整，参数解析正常，测试通过 | 11.9 | cc:TODO |
| 11.11 | 端到端集成测试 `[tdd:required]` | 编写完整的端到端测试，验证保存、恢复、清理的完整流程 | 端到端测试覆盖所有场景，性能指标达标，测试通过 | 11.10 | cc:TODO |
| 11.12 | 编写用户指南 `[tdd:skip:docs-only]` | 创建 docs/user-guide/session-management.md，包含快速开始、配置说明、故障排除 | 用户指南完整，示例清楚，故障排除有效，文档验证通过 | 11.11 | cc:TODO |
| 11.13 | 系统集成和验收测试 `[tdd:required]` | 运行完整测试套件，功能验证清单，性能验证，兼容性测试 | 所有测试通过，功能完整性达标，性能指标满足，兼容性正常 | 11.12 | cc:TODO |

---

## 实施说明

### 🎯 主要特点

1. **轻量级 Hook 集成**：扩展现有 Hook 系统，无需修改核心架构
2. **Token 智能检测**：基于 token 使用率自动触发保存，避免 context 满
3. **完整状态保存**：保存对话历史、任务状态、文件修改、Git 状态
4. **智能恢复提示**：新会话启动时自动显示恢复选项和 AI 决策
5. **技能命令接口**：提供 `/harness-*` 格式的命令，符合项目规范
6. **存储优化**：GZIP 压缩、自动清理、健康检查，节省存储空间

### 📊 工作范围

- **新增文件**：
  - `skills/harness-session/SKILL.md` 及参考文档
  - `java-harness-workflow/src/main/java/com/chachamaru/harness/session/` 完整包结构
  - `java-harness-workflow/src/main/java/com/chachamaru/harness/hook/extensions/` Hook 扩展
  - `docs/user-guide/session-management.md` 用户指南
  
- **修改文件**：
  - `java-harness-workflow/src/main/java/com/chachamaru/harness/hook/HookRegistry.java`
  - `java-harness-cli/src/main/java/com/chachamaru/harness/cli/CommandRegistry.java`
  - `skills/routing-rules.md` 路由规则
  - `harness.toml.bak` 配置模板

### 🔧 技术架构

- **存储位置**：`.claude/state/session-saves/<timestamp>/`
- **Hook 集成**：session-monitor (token 检测), session-init (恢复提示)
- **命令接口**：`/harness-save-session`, `/harness-restore-session`, `/harness-list-sessions` 等
- **配置方式**：`harness.toml` 中的 `[session.*]` 节

### 🚀 性能目标

- 保存时间：**< 3 秒**（典型会话）
- 恢复时间：**< 5 秒**（典型会话）
- 压缩率：**> 70%**（文本内容）
- 存储占用：**< 10MB/会话**（压缩后）
- 可靠性：保存成功率 > 99%，恢复成功率 > 98%

### 🔄 与现有系统集成

- **与 Hook 系统集成**：扩展现有 session-monitor 和 session-init hooks
- **与技能系统集成**：新增 harness-session 技能，遵循技能命名规范
- **与配置系统集成**：扩展 harness.toml 配置文件
- **向后兼容**：不影响现有功能的正常运行

---

## Phase 12: 智能模型选择系统（核心功能增强）🆕

### 📋 需求说明

本 Phase 为 Java Harness 添加智能模型选择功能，根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现。

### 事前确认章节

**Phase 12 涉及配置文件读取和环境变量访问，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 环境变量读取（模型配置）
  - **理由**: Task 12.1-12.8 需要读取 ANTHROPIC_DEFAULT_*_MODEL 等环境变量
  - **scope**: Phase 12 / Task 12.1-12.8

- **事项**: 配置文件读取（.claude/settings.json, harness.toml）
  - **理由**: Task 12.4-12.5 需要读取项目配置文件
  - **scope**: Phase 12 / Task 12.4-12.5

- **事项**: 文件系统写入（创建新的 Java 类和测试文件）
  - **理由**: Task 12.1-12.7 需要在 java-harness-workflow 模块中创建新文件
  - **scope**: Phase 12 / Task 12.1-12.7

#### 说明
- 所有环境变量读取仅用于模型配置，不包含敏感认证信息
- 配置文件读取遵循现有权限规范
- 新文件创建仅在项目目录内，不涉及系统目录
- 与现有 Effort Routing 系统集成，不破坏现有功能

---

### 实现任务

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 12.1 | 创建 ModelTier 枚举 `[tdd:required]` | 实现基于复杂度分数的模型等级映射（FAST/BALANCED/QUALITY/POWERFUL），支持环境变量映射 | 单元测试通过，包含所有等级的分数范围验证 | - | cc:完成 ✅ 2026-08-10 [1775f53] |
| 12.2 | 创建 TierConfig 数据类 `[tdd:required]` | 实现单个等级的配置类，包含降级链列表和验证逻辑 | 单元测试通过，正确处理空降级链异常 | 12.1 | cc:完成 ✅ 2026-08-10 [1775f53] |
| 12.3 | 创建 ModelSelectionConfig 总配置类 `[tdd:required]` | 实现总配置类，管理所有等级配置和全局设置 | 单元测试通过，支持启用/禁用功能 | 12.2 | cc:完成 ✅ 2026-08-10 [1775f53] |
| 12.4 | 创建 ModelReferenceResolver 解析器 `[tdd:required]` | 实现环境变量引用解析器，支持 `env:MODEL_NAME` 格式 | 单元测试通过，正确处理缺失和空环境变量 | 12.3 | cc:完成 ✅ 2026-08-10 [6d25da4] |
| 12.5 | 创建 ModelAvailabilityChecker 检查器 `[tdd:required]` | 实现模型可用性检查器，包含格式验证和可选 API 调用 | 单元测试通过，超时设置正确 | 12.4 | cc:完成 ✅ 2026-08-10 [6d25da4] |
| 12.6 | 创建 ModelSelectionConfigLoader 加载器 `[tdd:required]` | 实现配置加载器，支持 settings.json 和 harness.toml 双格式优先级加载 | 单元测试通过，默认配置正确加载 | 12.5 | cc:完成 ✅ 2026-08-10 [6d25da4] |
| 12.7 | 创建 SmartModelSelector 核心选择器 `[tdd:required]` | 实现核心模型选择逻辑，包含完整降级链执行 | 单元测试通过，所有等级正确选择 | 12.6 | cc:完成 ✅ 2026-08-10 [6d25da4] |
| 12.8 | 集成到 EffortRouter 系统 `[tdd:required]` | 在现有 EffortRouter 中集成智能模型选择，返回 WorkerSpawnConfig | 集成测试通过，不破坏现有功能 | 12.7 | cc:TODO |
| 12.9 | 更新技能文档 `[tdd:skip:docs-only]` | 在 harness-work SKILL.md 中添加智能模型选择章节说明 | 文档完整，包含配置示例和使用场景 | 12.8 | cc:TODO |
| 12.10 | 编写端到端集成测试 `[tdd:required]` | 创建完整的端到端测试，覆盖配置优先级、降级机制、异常处理 | 端到端测试覆盖率 > 80%，所有场景通过 | 12.9 | cc:TODO |
| 12.11 | 编写性能和压力测试 `[tdd:required]` | 创建性能测试，验证单次选择 < 100ms，支持 10+ 并发线程 | 性能测试通过，无内存泄漏 | 12.10 | cc:TODO |
| 12.12 | 创建用户配置指南 `[tdd:skip:docs-only]` | 编写详细的用户配置文档，包含快速开始、配置详解、故障排除 | 文档完整，示例可运行 | 12.11 | cc:TODO |
| 12.13 | 更新 README 和 CHANGELOG `[tdd:skip:docs-only]` | 在主 README 中添加功能介绍，在 CHANGELOG 中记录版本变更 | README 功能完整，CHANGELOG 格式正确 | 12.12 | cc:TODO |

---

### 实施说明

#### 🎯 主要特点

1. **基于现有系统**: 完全集成现有 Effort Routing 评分系统
2. **配置驱动**: 支持两种配置格式，自动优先级选择
3. **完整降级**: 多层降级机制，确保总能找到可用模型
4. **性能优化**: 单次选择 < 100ms，支持高并发
5. **向后兼容**: 不影响现有功能，可选启用

#### 📊 技术架构

- **核心组件**: 8 个 Java 类，按职责分层设计
- **测试覆盖**: 11 个测试类，覆盖率 > 80%
- **文档支持**: 设计文档、实现计划、用户指南
- **性能目标**: 单次 < 100ms，并发 10+ 线程，内存 < 10MB

#### 🔧 模型映射策略

| 复杂度分数 | 模型等级 | 环境变量 |
|-----------|---------|---------|
| 0-2 | FAST | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

#### 🔄 降级机制

每个等级都有独立的降级链：
```
1. 主要模型 (如 ANTHROPIC_DEFAULT_HAIKU_MODEL)
2. 默认模型 (ANTHROPIC_MODEL)
3. 安全模型 (glm-4.7 硬编码兜底)
```

---

### 工作范围

- **新建文件**: 8 个 Java 类 + 11 个测试类 + 3 个配置文件 + 5 个文档文件
- **修改文件**: EffortRouter.java, harness-work SKILL.md, README.md, CHANGELOG.md
- **代码行数**: ~3000 行（含测试）
- **预计工作量**: 5-7 天

---

### 集成方式

- **与 Effort Routing 集成**: 在评分后添加模型选择逻辑
- **与技能系统集成**: 更新 harness-work 技能文档
- **与配置系统集成**: 扩展现有配置加载机制
- **向后兼容**: 不破坏现有功能，平滑升级

---

### 质量标准

- ✅ 所有单元测试通过 (7 个测试类)
- ✅ 所有集成测试通过 (2 个测试类)
- ✅ 所有端到端测试通过 (1 个测试类)
- ✅ 所有性能测试通过 (1 个测试类)
- ✅ 代码覆盖率 > 80%
- ✅ 文档完整清晰

---

## 实现计划总览

### 完成状态
- **已完成 Phases**: 1-7, 9, 11 (文档重建 + 双平台支持 + 会话管理)
- **进行中**: Phase 12 (智能模型选择系统)
- **待开始**: Phase 8 (Codex 实现完善), Phase 10 (智能分支隔离检测)

### 优先级建议
1. **Phase 12** (智能模型选择系统) - 高优先级，核心功能增强
2. **Phase 10** (智能分支隔离检测) - 中优先级，改进安全性
3. **Phase 8** (Codex 实现完善) - 低优先级，可选功能

### 预估工作量
- **Phase 12**: 5-7 天 (智能模型选择系统，包含 13 个任务)
- **Phase 10**: 4-5 天 (分支检测、用户交互、集成测试)
- **Phase 8**: 2-3 天 (配置生成和集成测试)

---

## 下一步行动

**推荐执行顺序**:
1. 完成 **Phase 12** (智能模型选择系统) - 核心功能增强
2. 完成 **Phase 10** (智能分支隔离检测) - 安全性改进
3. 可选完成 **Phase 8** (Codex 实现完善) - 扩展平台支持

**启动方式**: 使用 `/harness-work` 或 `/breezing` 开始实施 Phase 12
