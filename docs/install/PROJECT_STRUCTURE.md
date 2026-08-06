# Java Harness 项目结构文档

**生成时间**: 2026-08-06
**项目版本**: 4.0.0-java-SNAPSHOT
**技术栈**: JDK 17 + GraalVM Native Image

---

## 📊 项目概览

### 模块统计

| 模块名称 | Java 文件数 | 主要职责 |
|---------|------------|---------|
| java-harness-cli | 192 | CLI 命令行工具（主入口） |
| java-harness-workflow | 84 | 工作流引擎 |
| java-harness-collaboration | 33 | 协作功能 |
| java-harness-foundation | 44 | 基础设施层 |
| java-harness-service | 15 | Spring Boot 后端服务 |
| java-harness-ci | 8 | CI/CD 集成 |
| java-harness-protocol | 7 | 协议定义 |
| java-harness-shared | 6 | 共享工具类 |
| java-harness-security | 4 | 安全防护 |
| java-harness-tools | 5 | 工具集 |
| java-harness-distribution | 0 | 分发打包 |

**总计**: 398 个 Java 文件

---

## 🏗️ 架构设计

### 7 层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    java-harness-cli                         │
│                    (CLI 层 - 192 文件)                       │
│              命令行接口 + 86 个命令实现                       │
└──────────────────┬──────────────────────────────────────────┘
                   │ HTTP/IPC (可选)
┌──────────────────┴──────────────────────────────────────────┐
│                 java-harness-service                         │
│                 (Service 层 - 15 文件)                       │
│            Spring Boot 后端 + 数据持久化                      │
└────────────────────────────────────────────────────────────┘
         │
    ┌────┴────┬────────┬────────┬────────┬────────┐
    │         │        │        │        │        │
┌───▼───┐ ┌──▼──┐ ┌───▼──┐ ┌───▼──┐ ┌───▼──┐ ┌───▼──┐
│工具层 │ │工作流│ │协作层│ │安全层│ │协议层│ │基础层│
│tools  │ │workflow│ │collab│ │security│ │protocol│ │foundation│
│(5 文件)│ │(84) │ │(33) │ │(4) │ │(7) │ │(44) │
└───────┘ └─────┘ └──────┘ └──────┘ └─────┘ └──────┘
    │         │        │        │        │        │
    └─────────┴────────┴────────┴────────┴────────┘
                   │
            ┌──────▼──────┐
            │  shared     │
            │  (6 文件)   │
            └─────────────┘
```

---

## 📁 详细目录结构

### 根目录结构

```
D:\project\java-harness\
├── java-harness-cli/           # CLI 模块（主入口）
├── java-harness-service/       # 服务模块（可选后端）
├── java-harness-foundation/    # 基础模块
├── java-harness-protocol/      # 协议模块
├── java-harness-security/      # 安全模块
├── java-harness-workflow/      # 工作流模块
├── java-harness-collaboration/ # 协作模块
├── java-harness-tools/         # 工具模块
├── java-harness-ci/            # CI/CD 模块
├── java-harness-distribution/  # 分发模块
├── java-harness-shared/        # 共享模块
├── docs/                        # 文档目录
├── scripts/                    # 脚本目录
├── skills/                     # Claude Skills
├── skills-codex/               # Codex Skills
├── workflows/                  # 工作流定义
├── agents/                     # AI Agents
├── bin/                        # 可执行文件
└── pom.xml                     # Maven 父 POM
```

---

## 🔧 模块详解

### 1. java-harness-cli (CLI 层)

**职责**: 命令行接口实现，86 个命令，主入口
**技术栈**: Picocli 4.7.5 + GraalVM Native Image

#### 包结构

```
java-harness-cli/
├── src/main/java/com/chachamaru/harness/cli/
│   ├── command/                  # 192 个命令类
│   │   ├── hook/                # Hook 命令组 (16 个)
│   │   │   ├── PreToolCommand.java
│   │   │   ├── PostToolCommand.java
│   │   │   ├── PermissionCommand.java
│   │   │   ├── SessionStartCommand.java
│   │   │   ├── SessionCleanupCommand.java
│   │   │   ├── NotificationCommand.java
│   │   │   ├── SessionInitCommand.java
│   │   │   ├── SessionMonitorCommand.java
│   │   │   ├── CiStatusHookCommand.java
│   │   │   ├── SubagentStartCommand.java
│   │   │   ├── SubagentStopCommand.java
│   │   │   ├── PostCompactCommand.java
│   │   │   ├── AskUserQuestionNormalizeCommand.java
│   │   │   ├── PermissionDeniedCommand.java
│   │   │   └── PostToolFailureCommand.java
│   │   ├── plan/                # 计划命令组
│   │   ├── evidence/            # 证据收集命令组
│   │   ├── HarnessCLI.java     # 主入口
│   │   ├── InitCommand.java
│   │   ├── SyncCommand.java
│   │   ├── StatusCommand.java
│   │   ├── DoctorCommand.java
│   │   ├── WorkCommand.java
│   │   ├── ReviewCommand.java
│   │   ├── ReleaseCommand.java
│   │   ├── GenCommand.java
│   │   ├── ValidateCommand.java
│   │   ├── CodexLoopCommand.java
│   │   └── ... (更多命令)
│   ├── ipc/                     # 进程间通信
│   │   ├── HttpIpcClient.java
│   │   └── IpcClient.java
│   ├── guardrail/               # 安全防护引擎
│   │   └── GuardrailEngine.java
│   └── config/                  # CLI 配置
└── src/test/java/              # 测试代码
```

#### 关键文件

- **主入口**: `HarnessCLI.java` - 86 个命令的入口点
- **IPC 客户端**: `HttpIpcClient.java` - 与 Service 通信
- **Guardrail 引擎**: `GuardrailEngine.java` - 27 个安全规则执行

---

### 2. java-harness-service (Service 层)

**职责**: Spring Boot 后端服务，提供数据持久化和 REST API
**技术栈**: Spring Boot 3.2.0 + MyBatis + SQLite + Flyway
**定位**: 可选的独立后端服务（非 CLI 必需依赖）

#### 为什么 CLI 不依赖 Service？

这是**架构设计的解耦决策**，而非遗漏：

1. **独立部署模式** - CLI 作为轻量级命令行工具可以完全独立运行
2. **降级容错机制** - CLI 内置降级处理，Service 挂了不影响 CLI 基本功能
3. **灵活的部署选项** - 用户可根据需要选择：
   - 仅使用 CLI（轻量级，无需启动额外服务）
   - CLI + Service（需要数据持久化和团队协作时）

#### 部署架构

```
┌─────────────────────────────────────┐
│     模式 1：独立 CLI (默认)          │
├─────────────────────────────────────┤
│  java-harness-cli (harness.exe)     │
│  - 86 个命令                         │
│  - Guardrail 引擎                    │
│  - Hook 处理                         │
│  - 完全独立运行                       │
│  - 无需依赖 Service                  │
└─────────────────────────────────────┘

┌──────────────────┐         HTTP          ┌──────────────────┐
│     模式 2：分布式 │ ─────────────────────> │   可选 Service   │
├──────────────────┤                       ├──────────────────┤
│ java-harness-cli │                       │ java-harness-    │
│                  │ <─────────────────────│    service       │
│  - CLI 命令      │    响应结果           │                  │
│  - IPC 客户端    │                       │  - REST API      │
│  - 降级处理 ✅   │                       │  - 数据持久化    │
│  (HttpIpcClient) │                       │  - 编排服务      │
└──────────────────┘                       │  - 会话管理      │
                                           └──────────────────┘
```

#### 降级机制示例

CLI 的 `HttpIpcClient` 内置智能降级：

```java
// Service 不可用时自动降级到本地处理
catch (IOException e) {
    markServiceUnavailable();
    return GuardrailDecision.allow("Communication error - default allow");
}

// HTTP 503 时降级
if (response.statusCode() == 503) {
    return GuardrailDecision.allow("Service unavailable - default allow");
}
```

**降级策略**：
- Service 可用 → 通过 HTTP 调用远程 API
- Service 不可用 → 自动降级到本地默认处理
- 网络错误 → 标记 Service 不可用，避免反复重试

#### 包结构

```
java-harness-service/
├── src/main/java/com/chachamaru/harness/service/
│   ├── HarnessService.java      # Spring Boot 主入口
│   ├── api/                     # REST API 控制器
│   │   ├── HealthController.java
│   │   ├── OrchestratorController.java
│   │   └── StateController.java
│   ├── service/                 # 业务服务层
│   │   ├── StateService.java
│   │   └── OrchestratorService.java
│   ├── domain/                  # 数据模型
│   │   ├── Session.java
│   │   └── WorkState.java
│   ├── mapper/                  # MyBatis 映射器
│   │   ├── SessionMapper.java
│   │   └── WorkStateMapper.java
│   ├── dto/                     # 数据传输对象
│   │   ├── StateQueryRequest.java
│   │   └── StateQueryResponse.java
│   ├── config/                  # 配置类
│   │   ├── DataSourceConfig.java
│   │   └── MyBatisConfig.java
│   └── handlers/                # 事件处理器
│       └── AutoTestRunner.java
└── src/test/java/              # 测试代码
    ├── integration/
    └── performance/
```

#### REST API 端点

- **健康检查**: `GET /actuator/health`
- **状态查询**: `POST /api/state`
- **编排服务**: `POST /api/orchestrator`
- **Hook 事件**: `POST /api/hook` (默认 http://localhost:8080/api/hook)

---

### 3. java-harness-foundation (基础层)

**职责**: 基础设施、工具类、配置管理
**文件数**: 44

#### 包结构

```
java-harness-foundation/
├── src/main/java/com/chachamaru/harness/foundation/
│   ├── config/                  # 配置管理
│   ├── constants/               # 常量定义
│   ├── util/                    # 工具类
│   ├── exception/               # 异常定义
│   ├── doc/                     # 文档系统
│   │   └── MultiLanguageDocSystem.java
│   └── logging/                 # 日志系统
```

---

### 4. java-harness-protocol (协议层)

**职责**: 协议定义、数据传输对象
**文件数**: 7

#### 包结构

```
java-harness-protocol/
├── src/main/java/com/chachamaru/harness/protocol/
│   ├── dto/                     # 数据传输对象
│   ├── enums/                   # 枚举定义
│   └── model/                   # 协议模型
```

---

### 5. java-harness-security (安全层)

**职责**: 安全防护、加密解密、权限控制
**文件数**: 4

#### 包结构

```
java-harness-security/
├── src/main/java/com/chachamaru/harness/security/
│   ├── encrypt/                 # 加密服务
│   ├── auth/                    # 认证授权
│   └── policy/                  # 安全策略
```

---

### 6. java-harness-workflow (工作流层)

**职责**: 工作流引擎、任务编排
**文件数**: 84

#### 包结构

```
java-harness-workflow/
├── src/main/java/com/chachamaru/harness/workflow/
│   ├── engine/                  # 工作流引擎
│   ├── executor/                # 任务执行器
│   ├── model/                   # 工作流模型
│   └── handler/                 # 处理器
```

---

### 7. java-harness-collaboration (协作层)

**职责**: 团队协作、通知、会话管理
**文件数**: 33

#### 包结构

```
java-harness-collaboration/
├── src/main/java/com/chachamaru/harness/collaboration/
│   ├── session/                 # 会话管理
│   ├── notification/            # 通知系统
│   ├── team/                    # 团队功能
│   └── bridge/                  # 桥接服务
```

---

### 8. java-harness-tools (工具层)

**职责**: 通用工具集
**文件数**: 5

#### 包结构

```
java-harness-tools/
├── src/main/java/com/chachamaru/harness/tools/
│   ├── file/                    # 文件工具
│   ├── git/                     # Git 工具
│   └── system/                  # 系统工具
```

---

### 9. java-harness-ci (CI/CD 层)

**职责**: CI/CD 集成、GitHub/GitLab 支持
**文件数**: 8

#### 包结构

```
java-harness-ci/
├── src/main/java/com/chachamaru/harness/ci/
│   ├── github/                  # GitHub 集成
│   │   ├── GitHubActionsIntegration.java
│   │   └── GitHubActionsWebhookHandler.java
│   ├── gitlab/                  # GitLab 集成
│   │   ├── GitLabCIIntegration.java
│   │   └── GitLabWebhookHandler.java
│   ├── monitor/                 # CI 状态监控
│   │   ├── CIStatusMonitor.java
│   │   ├── GitHubStatusProvider.java
│   │   └── GitLabStatusProvider.java
│   └── repair/                  # 自动修复
│       └── AutoRepairEngine.java
```

---

### 10. java-harness-distribution (分发层)

**职责**: 打包分发、安装程序
**文件数**: 0 (待实现)

---

### 11. java-harness-shared (共享层)

**职责**: 跨模块共享的常量和工具
**文件数**: 6

#### 包结构

```
java-harness-shared/
├── src/main/java/com/chachamaru/harness/shared/
│   ├── dto/                     # 共享 DTO
│   ├── constants/               # 共享常量
│   └── util/                    # 共享工具
```

---

## 🚀 部署架构

### 独立部署模式

```
┌─────────────────────────────────────┐
│     java-harness-cli                │
│     (harness.exe - Native Image)    │
│                                      │
│  - 命令行工具                        │
│  - Guardrail 引擎                   │
│  - Hook 处理                         │
│  - 86 个命令                         │
└─────────────────────────────────────┘
```

### 分布式部署模式

```
┌──────────────────┐         HTTP          ┌──────────────────┐
│ java-harness-cli│ ─────────────────────> │java-harness-     │
│                  │                       │    service       │
│  - CLI 命令      │ <─────────────────────│                  │
│  - IPC 客户端    │    响应结果           │  - REST API      │
│  - 降级处理      │                       │  - 数据持久化    │
└──────────────────┘                       │  - 编排服务      │
                                           └──────────────────┘
```

---

## 📦 依赖关系图

```
                    ┌──────────────┐
                    │    shared    │
                    └──────┬───────┘
                           │
           ┌───────────────┼─────────────────┐
           │               │                 │
      ┌────▼────┐     ┌────▼────┐      ┌────▼────┐
      │protocol │     │security │      │foundation│
      └────┬────┘     └────┬────┘      └────┬────┘
           │               │                 │
           └───────┬───────┴─────────────────┘
                   │
      ┌────────────┴────────────────────────┐
      │                                      │
 ┌────▼───────┐  ┌───────────┐  ┌──────────▼──┐
 │  workflow  │  │   tools   │  │collaboration│
 └─────┬──────┘  └─────┬─────┘  └──────┬─────┘
       │               │                │
       └───────┬───────┴────────────────┘
               │
         ┌─────▼──────┐  ┌─────────────┐
         │    cli     │  │   service   │
         │ (主入口)   │  │  (可选)      │
         └────────────┘  └─────────────┘
```

---

## 🎯 核心特性

### 1. 高性能
- **GraalVM Native Image**: 亚毫秒级启动时间
- **<10ms Hook 响应**: 实时安全策略执行
- **轻量级设计**: 最小化依赖

### 2. 安全防护
- **27 个 Guardrail 规则**: R01-R27 全覆盖
- **16 个 Hook 子命令**: 完整的事件处理
- **加密支持**: 数据传输加密

### 3. 模块化
- **清晰分层**: 7 层架构
- **独立部署**: CLI 和 Service 可独立运行
- **松耦合**: 通过 IPC 通信

### 4. 丰富的命令集
- **86 个 CLI 命令**: 覆盖所有核心功能
- **命令分组**: hook, evidence, plan, work, review
- **kebab-case 命名**: 与 Go 版本一致

---

## 🔧 构建与打包

### 构建 CLI

```bash
# 编译
mvn clean package -pl java-harness-cli

# 构建 Native Image
mvn clean package -Pnative -pl java-harness-cli

# 输出
# java-harness-cli/target/harness.exe
```

### 构建 Service

```bash
# 编译
mvn clean package -pl java-harness-service

# 运行
java -jar java-harness-service/target/java-harness-service-4.0.0-java-SNAPSHOT.jar
```

### 构建全部

```bash
# 构建所有模块
mvn clean package

# 跳过测试
mvn clean package -DskipTests
```

---

## 📝 配置文件

### 主要配置文件

```
java-harness/
├── pom.xml                          # Maven 父配置
├── .claude-plugin/                  # Claude 插件配置
│   ├── hooks.json                   # Hooks 配置
│   ├── settings.json                # 插件设置
│   └── marketplace.json             # Marketplace 配置
├── java-harness-cli/
│   └── META-INF/native-image/       # Native Image 配置
│       ├── reflect-config.json
│       ├── resource-config.json
│       └── serialization-config.json
└── java-harness-service/
    └── src/main/resources/
        └── application.yml          # Spring Boot 配置
```

---

## 🧪 测试结构

### 测试目录

```
java-harness-*/
└── src/test/java/
    ├── unit/                         # 单元测试
    ├── integration/                  # 集成测试
    ├── e2e/                          # 端到端测试
    └── performance/                  # 性能测试
```

### 测试命令

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl java-harness-cli

# 生成覆盖率报告
mvn test jacoco:report
```

---

## 📚 文档资源

### 项目文档

```
docs/
├── install/                        # 安装文档
├── superpowers/                    # Skills 文档
│   ├── plans/                      # 实现计划
│   ├── specs/                      # 规格说明
│   └── reports/                    # 报告文档
└── README.md                       # 项目说明
```

### 脚本资源

```
scripts/
├── build/                          # 构建脚本
├── ci/                             # CI 脚本
├── test/                           # 测试脚本
├── service/                        # 服务管理
│   ├── start-service.sh
│   └── stop-service.sh
└── util/                           # 工具脚本
```

---

## 🎓 技术栈总结

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 17 | 运行时环境 |
| GraalVM | 23.1.0 | Native Image 编译 |
| Spring Boot | 3.2.0 | 后端服务框架 |
| MyBatis | 3.0.3 | ORM 框架 |
| SQLite | 3.43.0.0 | 嵌入式数据库 |
| Flyway | 9.22.3 | 数据库迁移 |
| Jackson | 2.15.2 | JSON 处理 |
| Picocli | 4.7.5 | CLI 框架 |
| SLF4J | 2.0.9 | 日志接口 |
| Logback | 1.4.11 | 日志实现 |
| JUnit | 5.10.0 | 单元测试 |

---

## 📊 项目状态

| 指标 | 数值 |
|------|------|
| 总模块数 | 11 |
| 总文件数 | 398 |
| 最大模块 | java-harness-cli (192) |
| 代码覆盖率目标 | 85% |
| 支持的 CI 平台 | GitHub Actions, GitLab CI |

---

## 🔗 相关链接

- **GitHub**: [项目地址]
- **文档**: [完整文档]
- **问题反馈**: [Issue Tracker]

---

**文档版本**: 1.0
**最后更新**: 2026-08-06
