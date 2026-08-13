<!-- superpowers-zh:begin (do not edit between these markers) -->
# Superpowers-ZH 中文增强版

本项目已安装 superpowers-zh 技能框架（20 个 skills）。

## 核心规则

1. **收到任务时，先检查是否有匹配的 skill** — 哪怕只有 1% 的可能性也要检查
2. **设计先于编码** — 收到功能需求时，先用 brainstorming skill 做需求分析
3. **测试先于实现** — 写代码前先写测试（TDD）
4. **验证先于完成** — 声称完成前必须运行验证命令

## 可用 Skills

Skills 位于 `.claude/skills/` 目录，每个 skill 有独立的 `SKILL.md` 文件。

- **brainstorming**: 在任何创造性工作之前必须使用此技能——创建功能、构建组件、添加功能或修改行为。在实现之前先探索用户意图、需求和设计。
- **chinese-code-review**: 中文 review 沟通参考——话术模板、分级标注（必须修复/建议修改/仅供参考）、国内团队常见反模式应对。仅在用户显式 /chinese-code-review 时调用，不要根据上下文自动触发。
- **chinese-commit-conventions**: 中文 commit 与 changelog 配置参考——Conventional Commits 中文适配、commitlint/husky/commitizen 中文模板、conventional-changelog 中文配置。仅在用户显式 /chinese-commit-conventions 时调用，不要根据上下文自动触发。
- **chinese-documentation**: 中文文档排版参考——中英文空格、全半角标点、术语保留、链接格式、中文文案排版指北约定。仅在用户显式 /chinese-documentation 时调用，不要根据上下文自动触发。
- **chinese-git-workflow**: 国内 Git 平台配置参考——Gitee、Coding.net、极狐 GitLab、CNB 的 SSH/HTTPS/凭据/CI 接入差异与镜像同步配置。仅在用户显式 /chinese-git-workflow 时调用，不要根据上下文自动触发。
- **dispatching-parallel-agents**: 当面对 2 个以上可以独立进行、无共享状态或顺序依赖的任务时使用
- **executing-plans**: 当你有一份书面实现计划需要在单独的会话中执行，并设有审查检查点时使用
- **finishing-a-development-branch**: 当实现完成、所有测试通过、需要决定如何集成工作时使用——通过提供合并、PR 或清理等结构化选项来引导开发工作的收尾
- **mcp-builder**: MCP 服务器构建方法论 — 系统化构建生产级 MCP 工具，让 AI 助手连接外部能力
- **receiving-code-review**: 收到代码审查反馈后、实施建议之前使用，尤其当反馈不明确或技术上有疑问时——需要技术严谨性和验证，而非敷衍附和或盲目执行
- **requesting-code-review**: 完成任务、实现重要功能或合并前使用，用于验证工作成果是否符合要求
- **subagent-driven-development**: 当在当前会话中执行包含独立任务的实现计划时使用
- **systematic-debugging**: 遇到任何 bug、测试失败或异常行为时使用，在提出修复方案之前执行
- **test-driven-development**: 在实现任何功能或修复 bug 时使用，在编写实现代码之前
- **using-git-worktrees**: 当需要开始与当前工作区隔离的功能开发，或在执行实现计划之前使用——通过原生工具或 git worktree 回退机制确保隔离工作区存在
- **using-superpowers**: 在开始任何对话时使用——确立如何查找和使用技能，要求在任何响应（包括澄清性问题）之前调用 Skill 工具
- **verification-before-completion**: 在宣称工作完成、已修复或测试通过之前使用，在提交或创建 PR 之前——必须运行验证命令并确认输出后才能声称成功；始终用证据支撑断言
- **workflow-runner**: 在 Claude Code / OpenClaw / Cursor 中直接运行 agency-orchestrator YAML 工作流——无需 API key，使用当前会话的 LLM 作为执行引擎。当用户提供 .yaml 工作流文件或要求多角色协作完成任务时触发。
- **writing-plans**: 当你有规格说明或需求用于多步骤任务时使用，在动手写代码之前
- **writing-skills**: 当创建新技能、编辑现有技能或在部署前验证技能是否有效时使用

## 如何使用

当任务匹配某个 skill 时，使用 `Skill` 工具加载对应 skill 并严格遵循其流程。绝不要用 Read 工具读取 SKILL.md 文件。

如果你认为哪怕只有 1% 的可能性某个 skill 适用于你正在做的事情，你必须调用该 skill 检查。
<!-- superpowers-zh:end -->

---

# Multilingual Code Standards Support 🆕

本项目现已支持多语言代码规范，可在代码审查时自动检测编程语言并应用相应的标准。

## 支持的语言和标准

| 语言 | 标准来源 | 文件扩展名 | 默认严重级别 | 审查范围 |
|------|----------|------------|-------------|----------|
| **Java** | Alibaba Java Development Guide (黄山版) | `.java` | major | 完整 |
| **Python** | PEP 8 + Python Best Practices | `.py`, `.pyi` | moderate | 完整 |
| **Vue** | Vue Style Guide | `.vue` | moderate | 组件 |
| **Go** | Effective Go + Go Code Review Comments | `.go` | major | 完整 |

## 主要特性

### 自动语言检测
- 基于文件扩展名的主要检测方法
- 基于内容模式的备用检测方法
- 多语言文件的特殊处理（如 Vue 组件）

### 标准路由
- 根据检测到的语言自动应用相应标准
- 与外部技能集成（如 Alibaba Java Development Guide）
- 可配置的标准映射和优先级

### 规则应用框架
- 严重级别分类（critical, major, moderate, minor, info）
- 可配置的规则类别（命名、格式、安全、性能等）
- 豁免处理和审计跟踪

## 使用方式

### 基础代码审查
```bash
# 审查当前更改的文件
/harness-review

# 审查特定文件
/harness-review path/to/file.java
/harness-review path/to/file.py
```

### 自动标准应用
代码审查会自动：
1. 检测文件的语言类型
2. 应用相应的代码标准
3. 生成语言特定的审查结果
4. 提供修复建议和最佳实践

### 配置自定义标准
编辑 `.claude/config/code-standards.config.json` 来自定义标准应用：

```json
{
  "languageMapping": {
    "java": {
      "standards": ["alibaba-java-development-guide"],
      "extensions": [".java"],
      "defaultSeverity": "major",
      "reviewScope": "full"
    }
  }
}
```

## 参考文档

详细的语言标准和指南：

- **架构设计**: `skills/harness-review/references/code-standards/architecture.md`
- **Java 标准**: `skills/harness-review/references/code-standards/java-alibaba-guide.md`
- **Python 标准**: `skills/harness-review/references/code-standards/python-pep8.md`
- **Vue 标准**: `skills/harness-review/references/code-standards/vue-style-guide.md`
- **Go 标准**: `skills/harness-review/references/code-standards/go-effective-go.md`

## Brainstorming 集成

规划技能现已集成创意探索功能：

### Harness Plan
- 在计划制定初期自动触发创意探索
- 为复杂功能提供多样化的实现方案
- 支持架构决策和技术选型的多角度思考

### Harness Plan Brief
- 生成计划概要时使用创意探索增强选项
- 为非工程师提供更全面的决策依据
- 提前识别潜在的技术和业务风险

## 验证和测试

运行验证脚本检查系统配置：

```bash
bash tests/code-standards/validate.sh
```

完整的测试计划：`tests/code-standards/test-plan.md`

## 最佳实践

### 代码审查
1. **依赖自动化**: 让系统自动检测语言和应用标准
2. **关注严重级别**: 优先处理 critical 和 major 级别的问题
3. **理解标准**: 参考语言特定的标准文档了解规则背后的原因
4. **配置豁免**: 对于原型和实验性代码，可以适当豁免某些规则

### 规划和设计
1. **利用创意探索**: 在规划复杂功能时充分利用 brainstorming 集成
2. **多角度思考**: 通过创意探索获得多样化的实现方案
3. **风险评估**: 提前识别技术和业务风险
4. **决策依据**: 使用创意结果作为决策的依据

### 配置管理
1. **版本控制**: 将配置文件纳入版本控制
2. **团队同步**: 确保团队成员使用相同的配置
3. **渐进式采用**: 逐步采用更严格的标准
4. **定期更新**: 定期更新标准以保持最新

## 故障排除

### 语言检测问题
- 检查文件扩展名是否在配置中
- 验证内容模式是否正确配置
- 确认配置文件路径正确

### 标准应用问题
- 验证外部技能集成是否启用
- 检查参考文档是否存在
- 确认配置文件是有效的 JSON

### 性能问题
- 检查大文件处理是否正常
- 验证缓存是否工作
- 监控内存使用情况

## 未来扩展

计划中的功能增强：
- 更多编程语言支持
- ML 驱动的模式检测
- 跨语言一致性检查
- 与静态分析工具集成

---

# 配置文档参考

Java Harness 的配置分布在多种载体（TOML / JSON / .properties / 环境变量），且以 Java 源码为权威。完整说明见：

- **[配置模板](docs/harness-project/config/harness.toml.default)** — 完整配置模板，每个小节标注 ✅ 已实现 / 🟡 约定层 / 🔴 规划中
- **[配置目录](docs/harness-project/config/README.md)** — 目录结构、快速开始、模块案例入口
- **[环境变量参考](docs/harness-project/configuration/environment-variables.md)** — 区分 Java CLI 读取 vs 技能/脚本层读取
- **[配置最佳实践](docs/harness-project/configuration/best-practices.md)** — E2E / 会话字段表、推荐配置、故障排除
- **[配置迁移指南](docs/harness-project/configuration/migration-guide.md)** — 键名纠错与格式迁移

要点：插件同步 schema（`[project]/[agent]/[env]/[safety.*]`）与 `[harness]` 后端声明由 `harness sync` 读取；E2E 配置为 JSON，会话配置为 `.properties`。配置校验命令为 `harness validate config`。
