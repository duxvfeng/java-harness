# Java Harness 使用指南

> **版本**: 4.0.0
> **更新时间**: 2026-08-02
> **目标用户**: 开发者、项目经理、DevOps 工程师

---

## 📖 目录

- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [常用命令](#常用命令)
- [工作流执行](#工作流执行)
- [配置管理](#配置管理)
- [故障排查](#故障排查)

---

## 🚀 快速开始

### 前置要求

- **Java**: JDK 17+
- **Maven**: 3.8+
- **操作系统**: Linux / macOS / Windows (WSL)

### 快速安装

```bash
# 克隆项目
git clone https://github.com/your-org/java-harness.git
cd java-harness

# 构建项目
./harness build

# 运行测试
./harness test
```

### 5分钟快速体验

```bash
# 1. 初始化项目
./harness init-project

# 2. 创建计划
./harness plan-registry create my-plan

# 3. 执行任务
./harness work 1

# 4. 查看状态
./harness session-status
```

---

## 🎯 核心功能

### 1. 模板系统

Java Harness 提供强大的模板系统，支持：

- **项目模板**: CLAUDE.md, AGENTS.md, Plans.md
- **规则模板**: 12个核心规则文件
- **内存模板**: decisions.md, patterns.md
- **变量替换**: {{PROJECT_NAME}}, {{DATE}} 等
- **国际化支持**: 英语、日语、中文

```bash
# 生成项目模板
./harness util/template-generate --type claude --lang ja
```

### 2. 工作流引擎

#### Solo 模式（单个任务）
```bash
./harness work 3
```

#### Breezing 模式（4+任务）
```bash
# 自动选择 Breezing 模式
./harness work all

# 强制 Breezing 模式
./harness work --breezing all
```

#### 并行模式（2-3任务）
```bash
# 并行执行，自动优化
./harness work --parallel 3 5-7
```

### 3. Hook 系统

支持 14 个生命周期 Hook：

```bash
# Hook 生命周期
PreToolUse → ToolUse → PostToolUse → PostToolFailure
SessionStart → PermissionRequest → Notification
SessionCleanup → SessionMonitor → SessionSummary
SubagentStart → SubagentStop → CIStatus
```

### 4. 国际化支持

```bash
# 设置语言
./harness config set language ja  # 日语
./harness config set language zh  # 中文
./harness config set language en  # 英语（默认）

# 生成多语言文档
./harness util/doc-generator --lang zh
```

---

## 🛠️ 常用命令

### 构建和测试

```bash
# 构建项目
./harness build
./harness compile
./harness package

# 清理
./harness clean

# 测试
./harness test
./harness run-tests
./harness test-integration
./harness auto-test-runner
```

### 项目管理

```bash
# 项目初始化
./harness init-project
./harness setup-existing-project
./harness analyze-project

# 项目配置
./harness config-manager list
./harness config-manager set key value
```

### 会话管理

```bash
# 会话初始化
./harness session-init

# 会话监控
./harness session-monitor

# 会话清理
./harness session-cleanup

# 会话历史
./harness session-history
```

### 计划管理

```bash
# 计划注册
./harness plan-registry list
./harness plan-registry create
./harness plan-registry switch

# 计划监控
./harness plans-watcher
```

### 代码质量

```bash
# 代码审查
./harness review-summary
./harness judgment-card

# 代码质量检查
./harness code-quality
```

### 服务管理

```bash
# 启动服务
./harness start-service

# 停止服务
./harness stop-service
```

### 验证和测试

```bash
# 系统验证
./harness verify
./harness verify-workflows
./harness verify-workflow-system
```

---

## 🔄 工作流执行

### 标准工作流程

```
1. 计划创建
   └─> /harness-plan create

2. 任务执行
   └─> /harness-work all (自动选择模式)

3. 进度同步
   └─> /harness-sync

4. 完成确认
   └─> /harness-review
```

### Breezing 模式架构

```
Lead (协调)
├── Worker (实现)
├── Advisor (顾问)
└── Reviewer (审查)
```

### 任务状态标记

| 标记 | 含义 |
|------|------|
| `cc:TODO` | 未着手 |
| `cc:WIP` | 作業中 |
| `cc:完了` | 完成 |
| `blocked` | 阻塞 |

---

## ⚙️ 配置管理

### 项目配置文件

`.claude-code-harness.config.yaml`:

```yaml
# 项目配置
project:
  name: "My Project"
  version: "1.0.0"
  language: "zh"

# 工作流配置
workflow:
  mode: auto  # auto, solo, parallel, breezing
  max_parallel: 4

# 模板配置
templates:
  registry: ".claude/template-registry.json"
  variables:
    PROJECT_NAME: "${project.name}"
    VERSION: "${project.version}"
```

### 环境变量

```bash
# 设置项目根目录
export JAVA_HARNESS_ROOT=/path/to/project

# 启用提示缓存
export ENABLE_PROMPT_CACHING_1H=1

# 设置语言
export CLAUDE_CODE_HARNESS_LANG=ja
```

---

## 📋 实际使用案例

### 案例1: 新项目启动

```bash
# 1. 初始化项目
./harness init-project --name my-new-project

# 2. 生成项目模板
./harness util/template-generate --type claude --lang zh

# 3. 创建工作计划
/harness-plan create

# 4. 执行第一阶段任务
./harness work all
```

### 案例2: 代码审查工作流

```bash
# 1. 生成审查卡片
./harness judgment-card --pr 123

# 2. 运行代码质量检查
./harness code-quality

# 3. 生成审查摘要
./harness review-summary --format html
```

### 案例3: CI/CD 集成

```bash
# 1. CI 构建
./harness ci-build

# 2. CI 测试
./harness ci-test

# 3. 发布前检查
./harness release-preflight

# 4. CI 部署
./harness ci-deploy
```

### 案例4: 会话监控和分析

```bash
# 1. 启动会话监控
./harness session-monitor &

# 2. 执行任务
./harness work all

# 3. 查看会话状态
./harness session-status

# 4. 清理会话
./harness session-cleanup
```

---

## 🔧 故障排查

### 常见问题

#### 1. 构建失败

```bash
# 问题描述
./harness build 失败

# 解决方案
./harness clean
./harness dependencies check
./harness build --debug
```

#### 2. 测试超时

```bash
# 问题描述
测试执行超时

# 解决方案
./harness test --timeout 600
./harness auto-test-runner --retry 3
```

#### 3. 脚本找不到

```bash
# 问题描述
脚本路径错误

# 解决方案
./harness verify  # 验证脚本路径
./harness util/dependencies check
```

### 获取帮助

```bash
# 查看命令帮助
./harness help
./harness build --help

# 查看项目状态
./harness session-status

# 生成诊断报告
./harness verify-workflow-system --diagnostic
```

---

## 📊 性能优化建议

### 1. 使用并行模式

```bash
# 2-3个任务时使用并行模式
./harness work --parallel 3
```

### 2. 启用缓存

```bash
# 长时间任务使用提示缓存
ENABLE_PROMPT_CACHING_1H=1 ./harness work all
```

### 3. 分阶段执行

```bash
# 大型项目分阶段执行
./harness work all | grep "Phase 1"
./harness work all | grep "Phase 2"
```

---

## 🔗 相关资源

- **安装指南**: [docs/installation.md](docs/installation.md)
- **迁移指南**: [docs/user/MIGRATION_GUIDE.md](docs/user/MIGRATION_GUIDE.md)
- **API 文档**: [docs/api/API_DOCUMENTATION.md](docs/api/API_DOCUMENTATION.md)
- **故障排查**: [docs/troubleshooting/TROUBLESHOOTING.md](docs/troubleshooting/TROUBLESHOOTING.md)
- **项目状态**: [docs/developer/PROJECT_STATUS.md](docs/developer/PROJECT_STATUS.md)

---

## 🎓 学习路径

### 初学者

1. 阅读 [安装指南](docs/installation.md)
2. 完成 [快速开始](#快速开始)
3. 尝试 [案例1: 新项目启动](#案例1-新项目启动)

### 中级用户

1. 学习 [工作流执行](#工作流执行)
2. 配置 [配置管理](#配置管理)
3. 实践 [实际使用案例](#实际使用案例)

### 高级用户

1. 研究 [Hook 系统](#1-hook-系统)
2. 自定义 [模板系统](#2-模板系统)
3. 集成 [CI/CD](#案例3-cicd-集成)

---

## 💡 最佳实践

### 1. 计划管理

- 始终使用 `/harness-plan create` 创建详细计划
- 定期使用 `/harness-sync` 同步进度
- 合理使用依赖关系（Depends）避免阻塞

### 2. 工作流选择

- 1个任务: Solo 模式
- 2-3个任务: Parallel 模式
- 4+个任务: Breezing 模式

### 3. 质量控制

- 每个任务完成后运行 `/harness-review`
- 定期执行 `./harness code-quality`
- 使用 `./harness auto-test-runner` 自动化测试

### 4. 文档维护

- 及时更新 Plans.md
- 使用 `./harness util/doc-generator` 生成文档
- 维护 CHANGELOG.md

---

## 🎯 下一步

- 阅读 [开发者文档](docs/developer/)
- 探索 [API 文档](docs/api/)
- 查看 [配置参考](docs/configuration.md)
- 加入 [社区讨论](https://github.com/your-org/java-harness/discussions)

---

**版本**: 4.0.0 | **更新时间**: 2026-08-02 | **维护团队**: Java Harness Team