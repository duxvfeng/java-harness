# Java Harness 架构设计文档

本文档详细说明 Java Harness 的系统架构、设计决策和技术实现。

## 🏗️ 系统架构概览

### 架构分层

Java Harness 采用分层架构设计，从上到下分为：

```
┌─────────────────────────────────────────┐
│         CLI Layer (用户接口层)          │
├─────────────────────────────────────────┤
│      Application Layer (应用层)          │
├─────────────────────────────────────────┤
│       Domain Layer (领域层)              │
├─────────────────────────────────────────┤
│    Infrastructure Layer (基础设施层)      │
└─────────────────────────────────────────┘
```

### 模块依赖关系

```
java-harness-cli (命令行接口)
    ↓
java-harness-workflow (工作流引擎)
    ↓
java-harness-security (安全引擎)
    ↓
java-harness-protocol (协议处理)
    ↓
java-harness-foundation (基础组件)
    ↓
java-harness-shared (共享工具)
```

## 📦 核心模块

### 1. java-harness-cli

**职责**: 命令行接口和用户交互

**主要组件**:
- `CommandRegistry`: 命令注册和路由
- `CommandLineParser`: 命令行参数解析
- `HelpGenerator`: 帮助信息生成
- 86个命令实现类

**设计模式**:
- **Command Pattern**: 每个命令实现 `Runnable` 接口
- **Factory Pattern**: 命令对象创建
- **Strategy Pattern**: 不同命令的执行策略

### 2. java-harness-workflow

**职责**: 工作流引擎和任务编排

**主要组件**:
- `WorkflowEngine`: 工作流执行引擎
- `TaskExecutor`: 任务执行器
- `ConditionEvaluator`: 条件表达式求值
- `ParallelExecutor`: 并行任务执行

**关键特性**:
- YAML 工作流定义支持
- 条件分支执行
- 并行任务处理
- 状态持久化

### 3. java-harness-security

**职责**: 安全规则引擎和权限管理

**主要组件**:
- `GuardrailRegistry`: 规则注册和管理
- `RuleEvaluator`: 规则评估引擎
- `PermissionManager`: 权限管理
- 27个安全规则实现

**安全规则分类**:
```java
public enum RuleCategory {
    SYSTEM_SECURITY,      // 系统安全 (R01-R05)
    GIT_SECURITY,         // Git 安全 (R06, R11-R12)
    FILE_SECURITY,        // 文件安全 (R07-R09, R13, R18)
    PRODUCTION_SECURITY,  // 生产安全 (R15-R17, R19-R20, R25)
    API_SECURITY,         // API 安全 (R14)
    DATABASE_SECURITY,    // 数据库安全 (R16)
    NETWORK_SECURITY,     // 网络安全 (R20)
    CONTAINER_SECURITY,   // 容器安全 (R17)
    CONFIG_SECURITY,      // 配置安全 (R18, R22, R26)
    BACKUP_SECURITY,      // 备份安全 (R23)
    LOG_SECURITY,         // 日志安全 (R24)
    CERT_SECURITY,        // 证书安全 (R22)
    CRON_SECURITY,        // 定时任务安全 (R27)
    PERMISSION_SECURITY   // 权限安全 (R10)
}
```

### 4. java-harness-protocol

**职责**: Hook 协议处理和事件编解码

**主要组件**:
- `HookCodec`: Hook 事件编解码器
- `EventDispatcher`: 事件分发器
- `MessageHandler`: 消息处理器
- 14个 Hook 处理器

**Hook 协议格式**:
```json
{
  "session_id": "string",
  "transcript_path": "string",
  "cwd": "string",
  "permission_mode": "default|auto|bypass",
  "hook_event_name": "PreToolUse|PostToolUse|...",
  "tool_name": "string",
  "tool_input": {},
  "plugin_root": "string"
}
```

### 5. java-harness-foundation

**职责**: 基础设施和数据处理

**主要组件**:
- `ConfigurationManager`: 配置管理
- `FileRepository`: 文件仓储
- `StateStore`: 状态存储
- `LoggerFactory`: 日志工厂

### 6. java-harness-shared

**职责**: 共享工具和通用组件

**主要组件**:
- `JsonUtil`: JSON 工具类
- `YamlUtil`: YAML 工具类
- `ProcessUtil`: 进程工具类
- `NetworkUtil`: 网络工具类

## 🔧 技术选型

### 核心技术栈

| 技术 | 版本 | 用途 | 选择理由 |
|------|------|------|---------|
| Java | 17+ | 核心语言 | LTS 版本，性能优秀 |
| Maven | 3.8+ | 构建工具 | 标准化构建，依赖管理 |
| picocli | 4.7 | CLI 框架 | 功能强大，易用性好 |
| Jackson | 2.15.2 | JSON 处理 | 高性能，易用性强 |
| SnakeYAML | 2.0+ | YAML 处理 | YAML 标准实现 |
| SLF4J | 2.0.9 | 日志接口 | 标准化日志接口 |
| Logback | 1.4.11 | 日志实现 | 高性能，配置灵活 |
| JUnit | 5.10.0 | 单元测试 | 现代化测试框架 |
| GraalVM | 23.1.0+ | Native 编译 | 快速启动，低内存 |

### 设计模式应用

| 模式 | 应用场景 | 优势 |
|------|---------|------|
| Command Pattern | CLI 命令实现 | 解耦命令调用和实现 |
| Strategy Pattern | 安全规则评估 | 灵活的规则扩展 |
| Factory Pattern | 对象创建 | 统一创建接口 |
| Observer Pattern | 事件监听 | 解耦事件源和监听器 |
| Chain of Responsibility | Hook 处理链 | 灵活的责任链 |
| Builder Pattern | 复杂对象构建 | 清晰的构建流程 |
| Singleton Pattern | 配置管理 | 全局唯一实例 |

## 🚀 性能设计

### 性能目标

| 指标 | 目标值 | 优化策略 |
|------|--------|---------|
| Hook 响应时间 | < 10ms (95th) | 规则缓存，并行评估 |
| Workflow 启动 | < 100ms | 延迟加载，缓存优化 |
| 简单 Workflow 执行 | < 1s | 串行优化，最小化 IO |
| 内存占用 (Native) | < 50MB | GraalVM 编译优化 |
| 启动时间 (Native) | < 100ms | Native Image 编译 |

### 性能优化技术

#### 1. 规则评估优化

```java
public class GuardrailRegistry {
    private final Map<String, Rule> ruleCache;
    private final Predicate<HookInput> ruleMatcher;
    
    // 并行规则评估
    public GuardrailResult evaluateParallel(HookInput input) {
        return rules.parallelStream()
            .filter(rule -> rule.matches(input))
            .map(rule -> rule.evaluate(input))
            .filter(Result::isBlocked)
            .findFirst()
            .orElse(GuardrailResult.allowed());
    }
}
```

#### 2. 配置缓存

```java
public class ConfigurationManager {
    private final Cache<String, Config> configCache;
    
    public Config getConfig(String path) {
        return configCache.get(path, () -> loadConfig(path));
    }
}
```

#### 3. Native Image 编译

```bash
# GraalVM Native Image 编译
mvn -Pnative native:compile

# 性能优化选项
-H:+ReportExceptionStackTraces
-H:+RemoveSaturatedTypeFlows
-H:OptimizationSize=polymorphic
```

## 🔒 安全设计

### 安全架构

```
┌──────────────────────────────────┐
│     User Input (用户输入)         │
├──────────────────────────────────┤
│   Input Validation (输入验证)     │
├──────────────────────────────────┤
│  Guardrail Engine (安全规则引擎)  │
├──────────────────────────────────┤
│ Permission Check (权限检查)       │
├──────────────────────────────────┤
│   Audit Logging (审计日志)        │
├──────────────────────────────────┤
│    Output Sanitization (输出清理)  │
└──────────────────────────────────┘
```

### 安全措施

1. **输入验证**: 所有用户输入经过严格验证
2. **规则引擎**: 27个安全规则覆盖常见威胁
3. **权限管理**: 基于角色的访问控制
4. **审计日志**: 完整的操作审计跟踪
5. **输出清理**: 防止信息泄露

### 安全规则实现

```java
public interface Rule {
    String getId();
    String getName();
    boolean matches(HookInput input);
    GuardrailResult evaluate(HookInput input);
}

public class R01BlockPrivilegeEscalation implements Rule {
    @Override
    public boolean matches(HookInput input) {
        return input.getToolName().equals("Bash") &&
               input.getToolInput().contains("sudo");
    }
    
    @Override
    public GuardrailResult evaluate(HookInput input) {
        return GuardrailResult.blocked("R01: 阻止提权命令");
    }
}
```

## 📊 数据流设计

### Hook 处理流程

```
Input (JSON) 
    ↓
HookCodec.decode()
    ↓
EventDispatcher.dispatch()
    ↓
GuardrailRegistry.evaluate()
    ↓
PermissionManager.check()
    ↓
CommandExecutor.execute()
    ↓
HookCodec.encode()
    ↓
Output (JSON)
```

### 工作流执行流程

```
Workflow YAML
    ↓
WorkflowParser.parse()
    ↓
TaskExecutor.execute()
    ↓
ConditionEvaluator.eval()
    ↓
ParallelExecutor.parallel()
    ↓
StateStore.persist()
    ↓
Result
```

## 🔌 扩展设计

### 插件系统

Java Harness 支持插件扩展：

```java
public interface Plugin {
    String getName();
    String getVersion();
    void initialize(PluginContext context);
    void shutdown();
    // 插件特定接口
}
```

### 规则扩展

添加新的安全规则：

```java
public class R28CustomRule implements Rule {
    // 实现规则逻辑
}
```

注册规则：

```java
public class CustomRulePlugin implements Plugin {
    @Override
    public void initialize(PluginContext context) {
        context.registerRule(new R28CustomRule());
    }
}
```

## 🧪 测试策略

### 测试层次

```
┌─────────────────────────────────┐
│    E2E Tests (端到端测试)        │
├─────────────────────────────────┤
│   Integration Tests (集成测试)    │
├─────────────────────────────────┤
│    Unit Tests (单元测试)          │
└─────────────────────────────────┘
```

### 测试覆盖率目标

- **单元测试**: ≥ 80% 行覆盖率
- **集成测试**: 核心流程 100% 覆盖
- **E2E 测试**: 主要使用场景覆盖

## 📈 监控和诊断

### 性能监控

```java
public class PerformanceMonitor {
    private final MeterRegistry meterRegistry;
    
    public void recordHookTime(long duration) {
        meterRegistry.timer("hook.duration")
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### 诊断工具

- **health check**: `harness doctor`
- **status**: `harness status`
- **validate**: `harness validate`
- **debug mode**: `harness --debug`

## 🔄 部署架构

### 本地部署

```
Project/
├── .claude/
│   ├── harness.toml         # 配置文件
│   ├── data/                # 数据目录
│   ├── cache/               # 缓存目录
│   └── logs/                # 日志目录
├── bin/
│   └── harness              # 可执行文件
└── README.md
```

### 云端部署

```
┌─────────────────────────────────┐
│      Load Balancer               │
├─────────────────────────────────┤
│  Harness Instance 1, 2, 3...     │
├─────────────────────────────────┤
│      Shared Storage              │
├─────────────────────────────────┤
│      Monitoring & Logging         │
└─────────────────────────────────┘
```

## 🔗 相关文档

- **[项目 README](../../README.md)** - 项目概述和快速开始
- **[安装指南](../user-guide/installation.md)** - 详细安装步骤
- **[API 参考](../reference/api-reference.md)** - API 接口文档
- **[文档索引](../README.md)** - 完整文档导航

### 近期计划 (4.2.0)

- [ ] 增强工作流引擎功能
- [ ] 支持更多 Hook 事件
- [ ] 优化 Native Image 性能
- [ ] 增强安全规则

### 中期计划 (5.0.0)

- [ ] 分布式任务执行
- [ ] 插件市场
- [ ] Web 管理界面
- [ ] 多语言支持

### 长期愿景

- [ ] 云原生架构
- [ ] AI 辅助决策
- [ ] 跨平台移动支持
- [ ] 企业级功能

---

**架构文档版本**: 1.0  
**最后更新**: 2026-08-08  
**维护者**: Java Harness Team  
**适用于版本**: 4.1.1
