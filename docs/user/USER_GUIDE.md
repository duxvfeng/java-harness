# Java Harness 使用指南

> **版本**: 4.1.0-java
> **更新时间**: 2026-08-03
> **目标用户**: 开发者、项目经理、DevOps 工程师

---

## 📖 目录

- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [常用命令](#常用命令)
- [Hook命令组](#hook命令组)
- [工作流执行](#工作流执行)
- [配置管理](#配置管理)
- [故障排查](#故障排查)
- [CLI命令参考](#cli命令参考)

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
mvn clean package

# 运行版本检查
java -jar java-harness-cli/target/java-harness-cli-4.1.0-java-SNAPSHOT.jar version
```

### 5分钟快速体验

```bash
# 1. 查看帮助
harness --help

# 2. 查看版本
harness version

# 3. 初始化项目
harness init

# 4. 运行健康检查
harness doctor

# 5. 创建计划
harness plan

# 6. 执行任务
harness work 1

# 7. 查看状态
harness status
```

---

## 🎯 核心功能

### 1. Hook命令组

Hook命令组包含16个子命令，用于处理 Claude Code Hook 事件。

```bash
# PreTool hook - 评估安全规则
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"ls"}}' | harness hook pre-tool

# PostTool hook - 检测篡改
echo '{"hook_event_name":"PostToolUse","tool_name":"Write"}' | harness hook post-tool

# Permission hook - 自动审批
harness hook permission

# Session hooks
harness hook session-start
harness hook session-init
harness hook session-cleanup
harness hook session-monitor
harness hook session-summary

# CI状态检查
harness hook ci-status

# 代理追踪
harness hook subagent-start
harness hook subagent-stop
```

### 2. 工作流引擎

#### Solo 模式（单个任务）
```bash
harness work 3
```

#### Breezing 模式（4+任务）
```bash
# 自动选择 Breezing 模式
harness work all

# 强制 Breezing 模式
harness work --breezing all
```

#### 并行模式（2-3任务）
```bash
# 并行执行，自动优化
harness work --parallel 3 5-7
```

### 3. Hook 系统

支持 16 个生命周期 Hook：

```bash
# Hook 生命周期
harness hook pre-tool          # PreToolUse guardrail evaluation
harness hook post-tool         # PostToolUse tampering/security checks
harness hook permission        # PermissionRequest auto-approval
harness hook session-start     # SessionStart env setup
harness hook post-tool-failure # PostToolUseFailure counter & escalation
harness hook post-compact      # PostCompact WIP context re-injection
harness hook notification      # Notification event logging
harness hook permission-denied # PermissionDenied event logging
harness hook ask-user-question-normalize  # AskUserQuestion answer bridge
harness hook session-init      # Session initialization + Plans.md summary
harness hook session-cleanup   # SessionEnd temp file cleanup
harness hook session-monitor   # Project state collection + session.json
harness hook session-summary   # Session summary to session-log.md
harness hook ci-status         # CI status check after push/PR
harness hook subagent-start    # Track agent lifecycle start
harness hook subagent-stop     # Track agent lifecycle stop
```

#### Hooks 配置文件

java-harness 包含完整的 hooks 配置，支持 16 个自动化 hooks，涵盖核心安全、会话管理、工作流和监控功能。

**配置文件位置：**
- 源文件: `hooks/hooks.json` （手动维护）
- 生成文件: `.claude-plugin/hooks.json` （由 HooksSyncer 自动同步）

**Hook 事件分类：**

1. **核心安全 Hooks（5个）**
   - `PreToolUse`: 工具调用前安全检查
   - `PostToolUse`: 工具调用后篡改检查
   - `PermissionRequest`: 权限请求自动处理
   - `PostToolUseFailure`: 工具失败计数和升级
   - `PermissionDenied`: 权限拒绝事件记录

2. **会话管理 Hooks（5个）**
   - `SessionInit`: 会话初始化（once: true）
   - `SessionStart`: 会话开始环境设置
   - `SessionEnd`: 会话结束清理
   - `SessionMonitor`: 项目状态收集
   - `SessionSummary`: 会话总结

3. **工作流 Hooks（3个）**
   - `PostCompact`: 上下文压缩后重新注入
   - `Notification`: 通知事件记录
   - `CIStatus`: CI 状态检查（git push/merge 后）

4. **监控 Hooks（2个）**
   - `SubagentStart`: 子代理启动跟踪
   - `SubagentStop`: 子代理停止跟踪

5. **特殊 Hooks（1个）**
   - `AskUserQuestion`: 问题答案桥接

**调用机制：**

所有 hooks 使用统一的调用格式：
```bash
bin/java-harness hook <hook-subcommand>
```

**配置同步：**

运行 `/harness-sync` skill 自动同步：
1. 读取 `harness.toml` 配置
2. 生成 `.claude-plugin/plugin.json`
3. 生成 `.claude-plugin/settings.json`
4. 同步 `hooks/hooks.json` → `.claude-plugin/hooks.json`
5. 检测配置漂移

### 4. 国际化支持

```bash
# 设置语言（通过配置文件）
# harness.toml 中设置 language = "ja" | "zh" | "en"
```

---

## 🛠️ 常用命令

### 构建和测试

```bash
# 构建项目
mvn clean compile
mvn clean package

# 清理
mvn clean

# 测试
mvn test
mvn test -Dtest=HarnessCLIIntegrationTest
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
# 会话管理
harness session

# 会话注册
harness session-register

# 会话注销
harness session-unregister

# 会话初始化
harness hook session-init

# 会话监控
harness hook session-monitor

# 会话清理
harness hook session-cleanup
```

### 计划管理

```bash
# 生成计划
harness plan

# 检查计划依赖
harness plans check-deps

# 监视计划变化
harness plans-watcher
```

### 代码质量

```bash
# 代码审查
harness review <taskID>

# 影响分数计算
harness impact-score --files-changed 10 --lines-changed 100

# TDD检查
harness tdd-check
```

### 服务管理

```bash
# CI检查
harness ci-check

# CI状态
harness ci-status
```

### 验证和测试

```bash
# 系统验证
harness validate all

# 健康检查
harness doctor

# 自审计
harness self-audit hooks --file <path>
```

---

## 🔄 工作流执行

### 标准工作流程

```
1. 计划创建
   └─> harness plan

2. 任务执行
   └─> harness work <taskID>

3. 进度同步
   └─> harness sync

4. 完成确认
   └─> harness review <taskID>
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
harness init

# 2. 运行健康检查
harness doctor

# 3. 创建工作计划
harness plan

# 4. 执行任务
harness work 1
```

### 案例2: 代码审查工作流

```bash
# 1. 生成审查提示
harness review <taskID>

# 2. 运行影响分数计算
harness impact-score --files-changed 10 --lines-changed 100

# 3. TDD检查
harness tdd-check
```

### 案例3: CI/CD 集成

```bash
# 1. CI 检查
harness ci-check

# 2. CI 状态
harness ci-status

# 3. 发布前检查
harness release --check

# 4. 生成发布提示
harness release
```

### 案例4: 会话监控和分析

```bash
# 1. 启动会话
harness session

# 2. 执行任务
harness work 1

# 3. 查看状态
harness status

# 4. 清理会话
harness hook session-cleanup
```

---

## 🔧 故障排查

### 常见问题

#### 1. 构建失败

```bash
# 问题描述
mvn clean package 失败

# 解决方案
mvn clean
mvn dependency:resolve
mvn clean package -X
```

#### 2. 测试超时

```bash
# 问题描述
测试执行超时

# 解决方案
mvn test -Dsurefire.timeout=600
```

#### 3. 命令找不到

```bash
# 问题描述
harness 命令找不到

# 解决方案
# 使用完整路径
java -jar java-harness-cli/target/java-harness-cli-4.1.0-java-SNAPSHOT.jar version

# 或使用Maven运行
mvn exec:java -Dexec.mainClass="com.chachamaru.harness.cli.command.HarnessCLI"
```

### 获取帮助

```bash
# 查看命令帮助
harness --help
harness <command> --help

# 查看项目状态
harness status
harness doctor
```

---

## 📊 性能优化建议

### 1. 使用GraalVM Native Image

```bash
# 编译为Native Image
cd java-harness-cli
mvn -Pnative native:compile

# 运行原生可执行文件（<100ms启动时间）
./target/harness version
```

### 2. 启用缓存

```bash
# 长时间任务使用提示缓存
export ENABLE_PROMPT_CACHING_1H=1
harness work 1
```

### 3. 分阶段执行

```bash
# 大型项目分阶段执行
harness work 1
harness work 2
harness work 3
```

---

## 🔗 相关资源

- **CLI命令参考**: [docs/user/CLI_REFERENCE.md](docs/user/CLI_REFERENCE.md)
- **安装指南**: [docs/installation.md](docs/installation.md)
- **迁移指南**: [docs/user/MIGRATION_GUIDE.md](docs/user/MIGRATION_GUIDE.md)
- **API 文档**: [docs/api/API_DOCUMENTATION.md](docs/api/API_DOCUMENTATION.md)
- **故障排查**: [docs/troubleshooting/TROUBLESHOOTING.md](docs/troubleshooting/TROUBLESHOOTING.md)

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

1. 研究 [Hook 系统](#3-hook-系统)
2. 阅读 [CLI命令参考](docs/user/CLI_REFERENCE.md)
3. 集成 [CI/CD](#案例3-cicd-集成)

---

## 💡 最佳实践

### 1. 计划管理

- 始终使用 `harness plan` 创建详细计划
- 定期使用 `harness sync` 同步进度
- 使用 `harness plans check-deps` 验证依赖关系

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