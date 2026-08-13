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
| 10.1 | 实现分支检测核心函数 `[tdd:required]` | 实现 `detect_branch_isolation_strategy()` 函数，支持主分支/feature分支/worktree检测 | 函数能正确识别分支状态并返回策略，单元测试覆盖所有场景 | - | cc:完成 ✅ 2026-08-09 |
| 10.2 | 实现用户交互逻辑 `[tdd:required]` | 实现 `handle_branch_isolation()` 函数，处理强制隔离/可选隔离/已隔离三种策略 | 用户交互流程清晰，能正确处理用户选择和取消 | 10.1 | cc:完成 ✅ 2026-08-09 |
| 10.3 | 集成到 harness-work Phase A `[tdd:required]` | 修改 harness-work SKILL.md，在准备阶段添加智能分支检测步骤 | Phase A 流程包含分支检测，与现有执行模式兼容 | 10.2 | cc:完成 ✅ 2026-08-09 |
| 10.4 | 实现状态文件管理 `[tdd:required]` | 创建 `.claude/state/branch-isolation-decision.json` 记录用户决策和隔离状态 | 状态文件格式正确，包含所有必需字段 | 10.3 | cc:完成 ✅ 2026-08-09 |
| 10.5 | 添加配置支持 `[tdd:required]` | 在 `.claude/settings.json` 中添加 `branchIsolation` 配置项 | 配置项完整，支持策略配置和覆盖 | 10.4 | cc:完成 ✅ 2026-08-09 |
| 10.6 | 实现配置读取和解析 `[tdd:required]` | 实现配置文件读取逻辑，支持默认值和环境变量覆盖 | 能正确读取配置并应用策略，配置优先级正确 | 10.5 | cc:完成 ✅ 2026-08-09 |
| 10.7 | 实现策略配置逻辑 `[tdd:required]` | 实现主分支/feature分支策略配置，支持 force/ask/skip 三种模式 | 策略配置生效，用户可自定义行为 | 10.6 | cc:完成 ✅ 2026-08-09 |
| 10.8 | 实现 Git 检测失败处理 `[tdd:required]` | 添加 Git 命令失败的错误处理和用户提示 | Git 检测失败时提供清晰的错误信息和解决建议 | 10.3 | cc:完成 ✅ 2026-08-09 |
| 10.9 | 实现 Worktree 创建失败处理 `[tdd:required]` | 添加 worktree 创建失败的错误处理和回退机制 | Worktree 创建失败时提供具体错误信息和替代方案 | 10.3 | cc:完成 ✅ 2026-08-09 |
| 10.10 | 实现用户取消处理 `[tdd:required]` | 处理用户取消隔离的场景，记录决定并中止或继续 | 用户取消时正确记录决策，执行流程符合预期 | 10.2 | cc:完成 ✅ 2026-08-09 |
| 10.11 | 编写单元测试 `[tdd:required]` | 为检测函数、交互逻辑、配置解析编写单元测试 | 测试覆盖率达到 80%，所有核心逻辑有测试 | 10.10 | cc:完成 ✅ 2026-08-09 |
| 10.12 | 编写集成测试 `[tdd:required]` | 测试主分支/feature分支/已隔离三种场景的端到端流程 | 集成测试覆盖所有用户场景，测试通过 | 10.11 | cc:完成 ✅ 2026-08-09 |
| 10.13 | 更新用户文档 `[tdd:skip:docs-only]` | 更新 README.md 和相关文档，说明智能分支隔离功能 | 文档包含使用示例、配置说明、场景说明 | 10.12 | cc:完成 ✅ 2026-08-09 |

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
| 8.1 | 创建 Codex 插件目录结构 | 创建 .codex-plugin/ 目录和相关子目录结构 | 目录结构符合 Codex 插件规范，权限正确 | - | cc:完成 ✅ 2026-08-10 |
| 8.2 | 生成 Codex plugin.json | 参考 Go 项目，生成符合 Codex 规范的 plugin.json 配置文件 | plugin.json 包含必需字段，格式正确，Codex 能识别 | 8.1 | cc:完成 ✅ 2026-08-10 |
| 8.3 | 创建 Codex 技能映射 | 将现有 Java Harness 技能映射到 Codex 执行模式 | 技能映射文档完整，包含调用路径和参数 | 8.2 | cc:完成 ✅ 2026-08-10 |
| 8.4 | 编写 Codex 集成文档 | 说明如何在 Codex 环境中使用 Java Harness 技能 | 文档包含安装、配置、使用示例，用户能按文档成功使用 | 8.3 | cc:完成 ✅ 2026-08-10 |
| 8.5 | Codex 环境测试验证 | 在 Codex 环境中测试关键技能功能 | 主要功能在 Codex 中正常工作，测试通过 | 8.4 | cc:完成 ✅ 2026-08-10 |

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
| 11.5 | 实现 session-monitor Hook 扩展 `[tdd:required]` | 扩展现有 session-monitor hook，添加 token 自动检测和保存触发逻辑 | Hook 扩展正常工作，自动保存触发准确，与现有功能兼容，测试通过 | 11.4 | cc:完成 ✅ 2026-08-09 |
| 11.6 | 实现配置管理 `[tdd:required]` | 创建 SessionConfigLoader，支持 harness.toml 配置、环境变量、默认值 | 配置加载正确，优先级正确，默认值合理，测试通过 | 11.5 | cc:完成 ✅ 2026-08-09 |
| 11.7 | 实现 SessionRestoreManager `[tdd:required]` | 创建恢复管理器，支持恢复检测、摘要生成、AI 决策、完整性验证 | 恢复逻辑正确，摘要生成准确，AI 决策合理，测试通过 | 11.6 | cc:完成 ✅ 2026-08-09 |
| 11.8 | 实现 session-init Hook 扩展 `[tdd:required]` | 扩展现有 session-init hook，添加恢复提示生成和显示逻辑 | Hook 扩展正常工作，恢复提示显示正确，与现有功能兼容，测试通过 | 11.7 | cc:完成 ✅ 2026-08-09 |
| 11.9 | 创建会话管理技能 `[tdd:skip:docs-only]` | 创建 skills/harness-session/SKILL.md 和参考文档，定义技能命令接口 | 技能定义完整，命令格式正确，参考文档详细，符合技能规范 | 11.8 | cc:完成 ✅ 2026-08-09 |
| 11.10 | 注册会话管理命令 `[tdd:required]` | 在 CommandRegistry 中注册会话管理命令，实现 save, restore, list, show, cleanup 命令 | 命令注册正确，功能完整，参数解析正常，测试通过 | 11.9 | cc:完成 ✅ 2026-08-09 |
| 11.11 | 端到端集成测试 `[tdd:required]` | 编写完整的端到端测试，验证保存、恢复、清理的完整流程 | 端到端测试覆盖所有场景，性能指标达标，测试通过 | 11.10 | cc:完成 ✅ 2026-08-09 |
| 11.12 | 编写用户指南 `[tdd:skip:docs-only]` | 创建 docs/harness-project/user-guide/session-management.md，包含快速开始、配置说明、故障排除 | 用户指南完整，示例清楚，故障排除有效，文档验证通过 | 11.11 | cc:完成 ✅ 2026-08-09 |
| 11.13 | 系统集成和验收测试 `[tdd:required]` | 运行完整测试套件，功能验证清单，性能验证，兼容性测试 | 所有测试通过，功能完整性达标，性能指标满足，兼容性正常 | 11.12 | cc:完成 ✅ 2026-08-09 |

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
  - `docs/harness-project/user-guide/session-management.md` 用户指南
  
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

## Phase 12: 端到端前后端集成检测功能 🆕

### 📋 需求说明

本 Phase 为 Harness 添加端到端前后端集成检测功能，确保代码审查通过后进行完整的功能验证：
- **自动触发**：review通过后自动调用端到端检测
- **智能修复**：检测失败时自动交回harness-work继续修改
- **完整验证**：前后端集成测试，确保系统功能完整性
- **流程集成**：无缝集成到现有harness-work和harness-review流程中

### 事前确认章节

**Phase 12 涉及流程集成和测试执行，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 修改harness-work和harness-review流程
  - **理由**: Task 12.1-12.6 需要修改现有技能流程和执行模式
  - **scope**: Phase 12 / Task 12.1-12.6

- **事项**: 创建端到端测试脚本
  - **理由**: Task 12.2-12.5 需要创建集成测试脚本和配置
  - **scope**: Phase 12 / Task 12.2-12.5

- **事项**: 测试环境准备和执行
  - **理由**: Task 12.7-12.9 需要启动服务、执行测试、验证结果
  - **scope**: Phase 12 / Task 12.7-12.9

#### 说明
- 流程修改仅限于harness-work和harness-review的执行逻辑
- 端到端测试在review通过后执行，不影响现有审查流程
- 测试失败时自动触发修复流程，保证代码质量
- 支持不同类型项目的端到端测试配置

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 12.1 | 分析当前harness流程 | 检查harness-work和harness-review的当前流程，确定端到端检测插入点 | 分析报告完成，确定最佳集成位置 | - | cc:完成 ✅ 2026-08-10 [分析报告] |
| 12.2 | 设计端到端检测架构 | 设计端到端检测的触发机制、测试类型、失败处理流程 | 架构设计文档完成，包含流程图和接口定义 | 12.1 | cc:完成 ✅ 2026-08-10 [架构文档] |
| 12.3 | 创建端到端检测脚本 | 创建完整的端到端检测脚本系统，支持多项目类型和测试框架 | 脚本系统完成，包含管理器、修复控制器、触发器、报告生成器 | 12.2 | cc:完成 ✅ 2026-08-10 [脚本系统] |
| 12.4 | 实现自动触发机制 | 修改harness-work执行模式，在APPROVE后自动调用端到端检测 | review通过后自动触发检测，修复循环集成完成 | 12.3 | cc:完成 ✅ 2026-08-10 [自动触发] |
| 12.5 | 实现失败自动修复循环 | 修改harness-work，检测失败时自动继续修改并重新检测 | 失败时自动回到修改流程，直到检测通过（已在12.4中实现） | 12.4 | cc:完成 ✅ 2026-08-10 [修复循环] |
| 12.6 | 添加配置支持 | 在 harness.toml 中添加端到端检测配置项，支持多层级配置 | 配置完整，支持开启/关闭和自定义检测类型 | 12.2 | cc:完成 ✅ 2026-08-10 [配置系统] |
| 12.7 | 实现前端启动和测试 | 实现 Playwright 端到端测试，支持多浏览器测试和智能诊断 | 前端测试可自动执行，Playwright完整支持 | 12.3 | cc:完成 ✅ 2026-08-10 [Playwright] |
| 12.8 | 实现后端启动和测试 | 实现多项目类型后端测试（Node.js/Java/Python/Go），自动检测框架 | 后端测试可自动执行，支持主流框架 | 12.3 | cc:完成 ✅ 2026-08-10 [后端测试] |
| 12.9 | 实现检测结果报告 | 生成端到端检测报告，支持多格式（Markdown/JSON/HTML/Console） | 报告格式清晰，失败信息详细，修复建议准确 | 12.8 | cc:完成 ✅ 2026-08-10 [报告系统] |
| 12.10 | 集成测试验证 | 测试完整的review→检测→修复流程，验证所有场景 | 端到端流程测试通过，所有场景覆盖 | 12.9 | cc:完成 ✅ 2026-08-10 [集成测试] |
| 12.11 | 更新相关文档 | 更新SKILL.md和相关文档，说明端到端检测功能和Playwright支持 | 文档完整，使用说明清晰 | 12.10 | cc:完成 ✅ 2026-08-10 [文档更新] |
| 12.12 | 实现智能跳过逻辑 `[tdd:required]` | 添加前端代码检测，当项目没有前端时自动跳过前端检测；支持项目类型自动识别和跳过规则配置 | 能正确识别纯后端项目并跳过前端检测，配置支持自定义跳过规则，测试通过 | 12.6 | cc:完成 ✅ 2026-08-11 [智能跳过] |


---

## 实施说明

### 🎯 主要特点

1. **流程集成**：无缝集成到现有harness-work和harness-review流程
2. **自动触发**：review通过后自动执行端到端检测
3. **智能修复**：检测失败时自动回到修改流程
4. **完整验证**：前后端集成测试，确保功能完整性
5. **可配置**：支持开启/关闭和自定义检测类型

### 📊 工作范围

- **修改文件**：`skills/harness-work/SKILL.md`, `skills/harness-work/references/execution-modes.md`, `skills/harness-review/SKILL.md`
- **新增文件**：`scripts/e2e-detection.sh`, `.claude/config/e2e-detection.config.json`
- **配置文件**：`.claude/settings.json` 添加端到端检测配置项

### 🔄 流程集成

```
harness-work (实现) → harness-review (审查) → 端到端检测 (新增) → 
  ├─ 检测通过 → 完成
  └─ 检测失败 → harness-work (继续修改) → 重新审查 → 重新检测
```

### 🚀 检测类型

- **前端功能测试**：UI交互、表单验证、页面跳转
- **后端API测试**：接口调用、数据验证、错误处理
- **集成测试**：前后端协作、数据流转、异常处理
- **性能测试**：响应时间、并发处理、资源使用

---

## Phase 13: 智能执行模式推荐系统（执行流程协调优化）🆕

### 📋 需求说明

本 Phase 为 Java Harness 添加智能执行模式推荐系统，解决用户在 Solo/Parallel/Breezing 模式选择上的困惑：
- **智能分析**：基于任务特征自动推荐最优执行模式
- **透明决策**：提供推荐理由和置信度，让用户理解为什么选择该模式
- **自动确认**：高置信度推荐自动应用，减少用户决策负担
- **学习能力**：记录历史推荐效果，持续优化推荐算法
- **CLI 集成**：与 `--auto-mode` 参数无缝集成

### 事前确认章节

**Phase 13 涉及核心组件实现和技能修改，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 创建新的 Java 模型类和配置文件
  - **理由**: Task 13.1-13.2 需要创建执行模式枚举、任务特征数据类、推荐配置
  - **scope**: Phase 13 / Task 13.1-13.2

- **事项**: 修改现有技能文件
  - **理由**: Task 13.7, 13.11 需要修改 harness-work SKILL.md 和相关技能文件
  - **scope**: Phase 13 / Task 13.7, 13.11

- **事项**: 创建新测试文件和配置
  - **理由**: Task 13.9-13.10 需要创建单元测试、集成测试和测试数据
  - **scope**: Phase 13 / Task 13.9-13.10

#### 说明
- 所有代码修改仅限于项目现有模块结构内
- 技能文件修改遵循现有技能规范
- 测试文件创建不影响现有功能
- 配置文件采用标准的 TOML/JSON 格式

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 13.1 | 创建执行模式枚举和数据模型 `[tdd:required]` | 实现 ExecutionMode 枚举，定义 SOLO/PARALLEL/BREEZING，创建 TaskCharacteristics、ModeRecommendation 数据类 | 所有数据类包含必要字段，JSON 序列化正常，单元测试通过 | - | cc:完成 ✅ 2026-08-11 |
| 13.2 | 实现任务特征分析器 `[tdd:required]` | 创建 TaskAnalyzer 类，分析任务数量、复杂度、依赖关系、审查需求 | 分析逻辑正确，能识别各种任务特征，测试覆盖率 > 80% | 13.1 | cc:完成 ✅ 2026-08-11 |
| 13.3 | 实现智能评分算法 `[tdd:required]` | 创建 ModeScorer 类，实现针对三种模式的评分算法，包含权重配置 | 评分算法正确，推荐结果符合预期，可配置权重参数 | 13.2 | cc:完成 ✅ 2026-08-11 |
| 13.4 | 实现推荐生成器 `[tdd:required]` | 创建 RecommendationGenerator 类，生成推荐、解释理由、备选方案 | 推荐生成完整，解释清晰合理，置信度计算准确 | 13.3 | cc:完成 ✅ 2026-08-11 |
| 13.5 | 创建 ModeRecommender 核心引擎 `[tdd:required]` | 整合 TaskAnalyzer、ModeScorer、RecommendationGenerator，提供统一的推荐接口 | 核心引擎工作正常，API设计清晰，单元测试通过 | 13.4 | cc:完成 ✅ 2026-08-11 |
| 13.6 | 实现用户交互组件 `[tdd:required]` | 创建 ModeAdvisor 类，实现推荐展示、用户确认机制、交互式模式助手 | 用户交互流畅，推荐展示美观，确认机制正确 | 13.5 | cc:完成 ✅ 2026-08-11 |
| 13.7 | 集成到 harness-work 执行流程 `[tdd:required]` | 修改 harness-work SKILL.md，在 Phase A 前调用 ModeRecommender，添加 --auto-mode 参数 | 集成点正确，与现有流程兼容，自动选择机制工作 | 13.6 | cc:完成 ✅ 2026-08-11 |
| 13.8 | 实现缓存和学习机制 `[tdd:required]` | 创建推荐缓存、历史学习、性能优化机制，提升推荐速度和准确性 | 缓存命中率 > 60%，学习机制有效，性能达标 | 13.5 | cc:完成 ✅ 2026-08-11 |
| 13.9 | 编写单元测试 `[tdd:required]` | 为所有核心组件编写单元测试，覆盖推荐逻辑、边界条件、错误处理 | 测试覆盖率 > 80%，所有核心逻辑有测试 | 13.8 | cc:完成 ✅ 2026-08-11 |
| 13.10 | 编写集成测试 `[tdd:required]` | 测试推荐系统与 harness-work 的完整集成，验证推荐准确性和执行协调性 | 集成测试覆盖所有场景，推荐准确率 > 85%，测试通过 | 13.9 | cc:完成 ✅ 2026-08-11 |
| 13.11 | 更新 harness-work SKILL.md 文档 `[tdd:skip:docs-only]` | 在 SKILL.md 中添加智能推荐说明，更新 Phase A 流程，添加使用示例 | 文档完整，包含推荐机制说明、流程图、使用指南 | 13.10 | cc:完成 ✅ 2026-08-11 |
| 13.12 | 更新用户文档和 README `[tdd:skip:docs-only]` | 在 README.md 中添加智能模式推荐功能介绍，创建使用指南 | 用户能理解推荐功能，会使用自动模式，文档验证通过 | 13.11 | cc:完成 ✅ 2026-08-11 |

---

## 实施说明

### 🎯 主要特点

1. **智能推荐**：基于任务特征自动推荐最优执行模式
2. **透明决策**：提供详细推荐理由和置信度评分
3. **自动确认**：高置信度推荐自动应用，减少用户决策负担
4. **学习能力**：记录历史推荐效果，持续优化算法
5. **CLI 集成**：与 `--auto-mode` 参数无缝集成

### 📊 工作范围

- **新增文件**：
  - `java-harness-workflow/src/main/java/com/chachamaru/harness/mode/` 完整包结构
  - `java-harness-workflow/src/main/java/com/chachamaru/harness/model/` 扩展数据模型
  - `docs/user-guides/smart-mode-selection-guide.md` 用户指南
  
- **修改文件**：
  - `skills/harness-work/SKILL.md` 添加智能推荐流程
  - `README.md` 添加智能模式推荐功能说明
  - `CHANGELOG.md` 记录 Phase 13 变更

### 🔧 技术架构

- **核心组件**：ModeRecommender、TaskAnalyzer、ModeScorer、RecommendationGenerator
- **数据模型**：ExecutionMode、TaskCharacteristics、ModeRecommendation
- **用户交互**：ModeAdvisor、推荐展示、确认机制
- **性能优化**：推荐缓存、历史学习、权重配置

### 🚀 性能目标

- 推荐生成时间：< 50ms
- 任务分析时间：< 100ms
- 置信度计算：< 10ms
- 内存占用：< 5MB
- 推荐准确率：> 85%

### 🔄 与现有系统集成

- **与 harness-work 集成**：在 Phase A 前调用 ModeRecommender
- **与执行模式兼容**：支持 Solo/Parallel/Breezing 三种模式
- **与配置系统集成**：支持 harness.toml 配置权重参数
- **向后兼容**：不影响现有显式模式选择功能

---

## 实现计划总览

### 完成状态
- **已完成 Phases**: 1-14 (文档重建 + 双平台支持 + 多语言规范 + Codex实现 + 智能分支隔离 + 端到端检测 + 会话保存恢复 + 智能模式推荐 + harness-session子命令)
- **所有任务已完成** ✅

### 最终统计
- **总任务数**: 106 件
- **已完成**: 106 件 (100%)
- **剩余 TODO**: 0 件

---

## 🎉 项目完成

所有 Phase (1-13) 均已完成！项目功能包括：

1. **文档体系重建** (Phase 1-6): 中英文 README、安装指南、架构文档
2. **双平台支持** (Phase 7-8): Claude Code + Codex CLI 完整支持
3. **多语言代码规范** (Phase 9): Java/Python/Vue/Go 代码审查标准
4. **智能分支隔离** (Phase 10): 自动检测分支状态并隔离工作区
5. **会话保存恢复** (Phase 11): Token 监控、自动保存、智能恢复
6. **端到端检测** (Phase 12): 前后端集成测试、自动触发、失败修复
7. **智能模式推荐** (Phase 13): 基于任务特征自动推荐执行模式

---

## Phase 14: harness-session 子命令提示处理 🆕

### 📋 需求说明

本 Phase 为 harness-session 技能添加统一的子命令接口和无参数帮助提示，提升用户体验：
- **统一命令格式**: 将 `/harness-save-session` 等独立命令统一为 `/harness-session <subcommand>` 格式
- **无参数帮助提示**: 输入 `/harness-session` 时显示完整的帮助信息
- **移除旧命令**: 旧的独立命令格式不再保留

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 14.1 | 更新 harness-session SKILL.md `[tdd:skip:config-task]` | 修改 SKILL.md 添加无参数帮助提示和统一命令格式 | SKILL.md 包含完整的帮助提示格式，命令定义正确，移除向后兼容说明 | - | cc:完成 ✅ 2026-08-12 |
| 14.2 | 测试验证帮助提示 `[tdd:required]` | 验证 `/harness-session` 无参数时显示帮助，各子命令正常工作 | 无参数调用显示帮助，各子命令参数解析正确 | 14.1 | cc:完成 ✅ 2026-08-12 |
| 14.3 | 更新命令参考文档 `[tdd:skip:docs-only]` | 更新 references/commands.md 文档，说明新的统一命令格式 | 文档包含新命令格式，使用示例清晰，移除旧命令引用 | 14.1 | cc:完成 ✅ 2026-08-12 |
| 14.4 | 更新 Java 命令注册 `[tdd:required]` | 更新 CommandRegistry.java 添加新命令格式，更新 SessionCommand.java 添加帮助提示，更新 SessionInitHandler 使用新命令格式 | 命令注册支持新格式，SessionCommand 显示帮助提示，提示信息使用新格式 | 14.1 | cc:完成 ✅ 2026-08-12 |

---

## 实施说明

### 🎯 主要特点

1. **统一接口**: 使用 `/harness-session <subcommand>` 格式，更符合 CLI 最佳实践
2. **帮助提示**: 无参数时自动显示完整的帮助信息
3. **易于扩展**: 未来添加新子命令更方便

### 📊 工作范围

- **修改文件**:
  - `skills/harness-session/SKILL.md` - 添加帮助提示和统一命令格式
  - `skills/harness-session/references/commands.md` - 更新命令参考文档
  - `java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java` - 添加新命令格式注册
  - `java-harness-cli/src/main/java/com/chachamaru/harness/handler/session/SessionCommand.java` - 统一入口命令
  - `java-harness-cli/src/main/java/com/chachamaru/harness/cli/handlers/SessionInitHandler.java` - 更新提示信息使用新格式

### 🔄 命令映射

| 旧命令 | 新命令 |
|--------|--------|
| `/harness-save-session` | `/harness-session save` |
| `/harness-restore-session` | `/harness-session restore` |
| `/harness-list-sessions` | `/harness-session list` |
| `/harness-show-session` | `/harness-session show` |
| `/harness-cleanup-sessions` | `/harness-session cleanup` |

> **注意**: 旧命令已废弃，不再保留向后兼容别名。

---

## Phase 15: 技能边界清晰化（架构优化）🆕

### 📋 需求说明

本 Phase 解决 Harness 技能边界不清晰的问题，确保各技能职责明确、流程可预测：
- **规划边界**: harness-plan 专注于规划，不执行实现工作
- **实现边界**: harness-work 是唯一执行代码修改的技能
- **清晰交接**: 规划→实现流程有明确的交接标准
- **用户可预测**: 技能行为符合用户预期，无越界操作

### 事前确认章节

**Phase 15 涉及技能文件和架构文档修改，需要在计划批准时预先确认以下事项：**

#### 计划时批准事项

- **事项**: 修改技能定义文件
  - **理由**: Task 15.1-15.6 需要修改 harness-plan 和 harness-work 的 SKILL.md 文件
  - **scope**: Phase 15 / Task 15.1-15.6

- **事项**: 更新架构文档和规范
  - **理由**: Task 15.2-15.4 需要创建和更新架构文档、技能规范
  - **scope**: Phase 15 / Task 15.2-15.4

#### 说明
- 所有修改仅限于技能定义文件和架构文档
- 不修改核心实现代码，仅澄清边界
- 确保向后兼容，不破坏现有功能

---

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 15.1 | 分析当前技能边界问题 `[tdd:skip:analysis]` | 分析 harness-plan 当前可能越界的场景，识别问题根源 | 分析报告完成，问题场景清晰，根源明确 | - | cc:TODO |
| 15.2 | 设计技能边界架构 `[tdd:skip:docs-only]` | 设计明确的技能边界定义，制定协作协议，创建边界检查清单 | 架构设计文档完成，包含边界定义、协作协议、检查清单 | 15.1 | cc:TODO |
| 15.3 | 更新 harness-plan SKILL.md `[tdd:skip:config-task]` | 在 harness-plan 中添加明确的边界声明，禁止直接修改代码，增加边界检查 | SKILL.md 包含边界章节，有明确的禁止操作列表，符合架构设计 | 15.2 | cc:TODO |
| 15.4 | 更新 harness-work SKILL.md `[tdd:skip:config-task]` | 在 harness-work 中明确其作为实现执行者的职责，与规划技能的交接标准 | SKILL.md 包含职责定义和交接标准，与 harness-plan 边界一致 | 15.2 | cc:TODO |
| 15.5 | 创建技能边界测试 `[tdd:required]` | 创建边界测试用例，验证各技能不会越界操作，检测异常行为 | 测试用例覆盖主要技能，能检测越界操作，测试通过 | 15.3 | cc:TODO |
| 15.6 | 更新用户文档和示例 `[tdd:skip:docs-only]` | 在文档中说明技能边界，提供正确使用示例，避免用户混淆 | 文档包含边界说明，使用示例清晰，用户能正确理解技能职责 | 15.4 | cc:TODO |
| 15.7 | 技能边界验证和发布 `[tdd:required]` | 运行完整测试套件，验证边界清晰化效果，确保无破坏性变更 | 所有测试通过，边界清晰化效果明显，无功能回归 | 15.5 | cc:TODO |

---

## 实施说明

### 🎯 主要特点

1. **明确边界**: 每个技能有清晰的职责范围和操作限制
2. **协作协议**: 规划→实现流程有明确的交接标准
3. **可预测性**: 用户可以准确预测每个技能的行为
4. **向后兼容**: 不破坏现有功能，仅澄清边界

### 📊 工作范围

- **修改文件**:
  - `skills/harness-plan/SKILL.md` - 添加边界声明和禁止操作列表
  - `skills/harness-work/SKILL.md` - 添加职责定义和交接标准
  - `docs/harness-project/architecture/skill-boundaries.md` - 创建边界架构文档

- **新增文件**:
  - `tests/skill-boundaries/test-boundary-violations.ts` - 边界测试用例

### 🔧 技能边界定义

#### harness-plan 边界
**允许的操作**:
- ✅ 读取和修改 Plans.md
- ✅ 读取 spec.md 和项目文档
- ✅ 调用 brainstorming 技能进行创意探索
- ✅ 生成任务分解和优先级建议
- ✅ 创建架构设计文档

**禁止的操作**:
- ❌ 修改项目源代码
- ❌ 执行构建或测试命令
- ❌ 修改配置文件（除了 Plans.md）
- ❌ 直接修改文件系统（除了文档和 Plans.md）

#### harness-work 边界
**职责范围**:
- ✅ 执行代码修改和实现
- ✅ 运行测试和构建
- ✅ 管理工作区和分支
- ✅ 处理依赖关系
- ✅ 执行审查后的修复

### 🔄 协作协议

**规划→实现交接标准**:
1. Plans.md 包含完整的任务定义和 DoD
2. 每个任务有明确的依赖关系和状态
3. 用户已批准计划并确认开始实现
4. 没有遗留的规划问题或不确定性

---

## 下一步行动

**推荐执行顺序**:
1. **立即开始** Phase 15 (技能边界清晰化) - 解决当前发现的边界问题，最高优先级
2. 完成 **Phase 13** (智能执行模式推荐系统) - 提升用户体验
3. 完成 **Phase 12** (端到端前后端集成检测功能) - 提升代码质量保证

**启动方式**: 使用 `/harness-work` 开始实施 Phase 15

---

## 下一步行动

**推荐执行顺序**:
1. 完成 **Phase 13** (智能执行模式推荐系统) - 解决模式选择困惑，最高优先级
2. 完成 **Phase 12** (端到端前后端集成检测功能) - 提升代码质量保证
3. 完成 **Phase 11** (会话保存和恢复系统) - 解决最紧迫的 context 满问题
4. 完成 **Phase 10** (智能分支隔离检测) - 改进分支安全性
5. 可选完成 **Phase 8** (Codex 实现完善) - 扩展平台支持

**启动方式**: 使用 `/harness-work` 或 `/breezing` 开始实施 Phase 13