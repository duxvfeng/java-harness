# Java Harness 功能扩展设计文档

**设计日期**: 2026-08-01  
**项目版本**: java-harness v4.0.0-java-SNAPSHOT  
**设计目标**: 实现claude-code-harness Go项目在Java中的完整功能对等

---

## 执行摘要

本设计文档定义了Java Harness项目从当前35-40%功能实现度扩展到与Go项目功能对等的完整技术方案。基于功能域驱动设计（DDD），采用分层架构，重点实现技能系统、代理系统、工作流编排、状态恢复等核心功能。

**核心决策**:
- **定位**: 轻量级开发者工具，专注Claude Code深度优化
- **架构**: 功能域驱动设计，7层清晰分离
- **技能系统**: 混合模式（Java核心 + Markdown扩展）
- **代理系统**: 完整三种代理（worker/reviewer/advisor）
- **状态管理**: 4阶段恢复机制
- **部署**: 双模式支持（JAR + Native Image同等）
- **配置**: YAML优先，Spring生态友好

---

## 1. 设计背景

### 1.1 当前状态分析

基于功能对比分析报告，Java项目当前实现度为35-40%：

**已实现功能** (✅):
- Guardrail安全规则：100%（15个规则全部实现）
- Hook协议处理：40%（基础PreToolUse/PostToolUse）
- 核心架构：模块化设计良好
- Spring集成：企业级集成能力

**缺失功能** (❌):
- 技能系统：0%（plan/work/review等核心技能）
- 代理系统：0%（worker/reviewer/advisor）
- 工作流编排：15%（缺少Plans.md解析、并行编排）
- 配置管理：0%（缺少harness.yaml处理）
- 状态恢复：50%（只有基础域对象，缺少恢复机制）

### 1.2 设计目标

**主要目标**:
1. 实现与Go项目的功能对等（90%+覆盖率）
2. 保持轻量级定位，避免过度工程化
3. 充分利用Java/Spring生态优势
4. 支持双模式部署（JAR + Native Image）
5. 保持架构一致性，支持长期维护

**非目标**:
- 不追求100%代码级对等，接口对等即可
- 不支持除Claude Code外的其他AI工具
- 不实现微服务化或云原生特性
- 不改变现有的Guardrail规则实现

---

## 2. 架构设计

### 2.1 分层架构

采用7层功能域驱动架构，单向依赖，职责清晰：

```
┌─────────────────────────────────────────────────────────────┐
│                    运行时层 (Runtime)                         │
│  CLI入口 + Native Image支持 + Spring Boot服务                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    工具层 (Tools)                             │
│  配置管理工具 + 验证工具 + 诊断工具                           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  协作层 (Collaboration)                       │
│  技能框架 + 代理框架 + 协调机制                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  工作流层 (Workflow)                         │
│  Plans.md解析 + 任务编排 + 状态恢复                           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  安全防护层 (Security)                       │
│  Guardrail规则 + 输入验证 + 审计日志                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    协议层 (Protocol)                         │
│  Hook协议 + 工具协议 + 编解码器                                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   基础设施层 (Foundation)                     │
│  DTO定义 + 配置抽象 + 数据访问层                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块结构

项目重组为9个Maven模块：

```
java-harness/
├── java-harness-foundation/        # 基础设施层
├── java-harness-protocol/          # 协议层
├── java-harness-security/         # 安全防护层
├── java-harness-workflow/         # 工作流层
├── java-harness-collaboration/    # 协作层
├── java-harness-cli/              # CLI运行时
├── java-harness-service/          # Spring Boot服务
├── java-harness-tools/            # 工具集
└── java-harness-distribution/     # 分发包
```

### 2.3 架构原则

**1. 单向依赖原则**
- 上层依赖下层，下层不依赖上层
- 例如：`workflow` 可以依赖 `protocol`，但 `protocol` 不能依赖 `workflow`

**2. 接口隔离原则**
- 层间通过接口通信，内部实现可替换
- 例如：`SecurityGuardrail` 接口定义安全检查API，具体实现可交换

**3. 职责单一原则**
- 每个模块只负责一个明确的功能域
- 例如：`plans` 模块只负责Plans.md解析，不涉及执行逻辑

**4. 可测试性原则**
- 每层都可以独立测试
- 例如：可以用Mock对象测试 `workflow` 层，不依赖实际的 `security` 层

---

## 3. 核心模块设计

### 3.1 基础设施层 (Foundation)

**职责**: 提供共享的数据结构、配置管理和数据访问能力

#### 关键组件

**foundation/dto/**
- `HookInput`: Hook事件输入数据结构
- `HookOutput`: Hook事件输出数据结构
- `GuardrailResult`: Guardrail评估结果
- `PlansDocument`: Plans.md解析结果模型
- `SessionState`: 会话状态实体
- `WorkState`: 工作状态实体

**foundation/config/**
- `HarnessConfig`: 配置抽象接口
- `YamlHarnessConfig`: YAML配置实现
- `ConfigValidator`: 配置验证器

**foundation/persistence/**
- `HarnessRepository<T>`: 数据访问泛型接口
- `SessionMapper`: Session状态MyBatis映射器
- `WorkStateMapper`: Work状态MyBatis映射器

#### 数据库Schema

```sql
-- Sessions表
CREATE TABLE sessions (
    session_id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    metadata TEXT,
    version INTEGER
);

-- WorkStates表
CREATE TABLE work_states (
    work_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    recovery_phase INTEGER,
    error_message TEXT,
    FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);

-- AuditLogs表
CREATE TABLE audit_logs (
    log_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT,
    severity TEXT
);
```

---

### 3.2 协议层 (Protocol)

**职责**: 定义与Claude Code通信的协议和编解码

#### 关键接口

```java
// Hook事件类型
public enum HookEventType {
    PRE_TOOL_USE, POST_TOOL_USE, PERMISSION_REQUEST,
    SESSION_START, SESSION_END, STOP,
    SUBAGENT_START, SUBAGENT_STOP, TASK_COMPLETED,
    PRE_COMPACT, POST_COMPACT
}

// Hook处理器接口
public interface HookHandler {
    HookOutput handle(HookInput input) throws HookException;
    boolean supports(HookEventType eventType);
}

// Hook编解码器
public interface HookCodec {
    HookInput decode(InputStream input) throws CodecException;
    void encode(HookOutput output, OutputStream out) throws CodecException;
}
```

#### 性能优化

使用Jackson进行高性能JSON处理，支持：
- 流式处理大JSON
- 性能优化的序列化配置
- Native Image反射配置

---

### 3.3 安全防护层 (Security)

**职责**: 实现Guardrail规则引擎和安全检查

#### Guardrail规则引擎

```java
// Guardrail规则接口
public interface GuardrailRule {
    String getId(); // "R01", "R02", etc.
    String getName();
    boolean matches(HookInput input);
    GuardrailResult evaluate(HookInput input);
}

// Guardrail引擎
public interface GuardrailEngine {
    void registerRule(GuardrailRule rule);
    GuardrailResult evaluate(HookInput input);
    List<GuardrailRule> getTriggeredRules(HookInput input);
}
```

#### 规则实现示例

```java
// R01规则：阻止提权命令
public class R01NoSudoRule implements GuardrailRule {
    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.getToolName());
    }
    
    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = extractCommand(input.getToolInput());
        if (command.contains("sudo ")) {
            return GuardrailResult.deny("R01", "提权命令被阻止: sudo");
        }
        return GuardrailResult.allow();
    }
}
```

#### 安全验证

- **路径安全验证**: 防止路径遍历攻击
- **输入验证**: 验证所有外部输入
- **审计日志**: 记录所有安全事件

---

### 3.4 工作流层 (Workflow)

**职责**: Plans.md解析、任务编排和状态恢复

#### Plans.md解析器

```java
// Plans解析器接口
public interface PlansParser {
    PlansDocument parse(String content) throws PlansException;
    PlansDocument parse(File file) throws PlansException;
}

// Plans文档模型
public class PlansDocument {
    private String title;
    private List<Task> tasks;
    
    public static class Task {
        private String id;
        private String title;
        private String description;
        private Status status;
        private String acceptanceCriteria;
        private List<String> dependencies;
        private String lane;
    }
}
```

#### 技术实现

**使用正则表达式解析**，避免引入重量级解析库：
- 表格行解析：`\\|?\\s*([^|]+)\\s*\\|.*`
- 标记解析：`\\[([a-z]+:)([a-z-]+)\\]`

#### 任务编排器

```java
// 任务编排器接口
public interface TaskOrchestrator {
    OrchestrationPlan createPlan(PlansDocument plans);
    ExecutionResult execute(OrchestrationPlan plan);
    void pause(String executionId);
    void resume(String executionId);
    void cancel(String executionId);
}

// 并行执行器
public interface ParallelExecutor {
    <T> List<T> executeParallel(List<Callable<T>> tasks);
    <T> List<T> executeWithSemaphore(List<Callable<T>> tasks, int maxConcurrency);
}
```

#### 技术选型

**并行执行使用CompletableFuture + 自定义Semaphore**：
- Java 8+内置支持，无需额外依赖
- 与Spring Boot集成良好
- 支持异常处理和超时控制

#### 状态恢复器

```java
// 状态恢复器接口
public interface StateRecovery {
    RecoveryResult attemptRecovery(String sessionId);
    RecoveryResult attemptSelfHealing(String sessionId);    // 阶段1
    RecoveryResult attemptPeerRecovery(String sessionId);   // 阶段2
    RecoveryResult attemptLeadIntervention(String sessionId); // 阶段3
    void markAborted(String sessionId);                      // 阶段4
}
```

#### 4阶段恢复机制

**阶段1: 自我修复**
- 分析错误类型
- 自动修正并重试
- 最多3次重试

**阶段2: 同伴修复**
- 将任务委托给其他Worker
- 使用不同的执行策略
- 记录修复历史

**阶段3: 指挥官介入**
- 向Lead会话发送escalation
- 等待人工干预
- 提供详细的错误信息

**阶段4: 停止**
- 标记为ABORTED状态
- 通知用户需要人工介入
- 保存完整的错误日志

---

### 3.5 协作层 (Collaboration)

**职责**: 技能框架、代理系统和协调机制

#### 技能系统设计

**混合模式**：Java核心技能 + Markdown扩展

```java
// 技能接口
public interface Skill {
    String getId();
    String getName();
    String getDescription();
    SkillResult execute(SkillContext context);
    boolean isApplicable(SkillContext context);
}

// Java核心技能基类
public abstract class CoreSkill implements Skill {
    @Override
    public SkillResult execute(SkillContext context) {
        if (!isApplicable(context)) {
            return SkillResult.notApplicable();
        }
        return executeInternal(context);
    }
    
    protected abstract SkillResult executeInternal(SkillContext context);
}
```

**核心技能实现**：
- `@Skill(id = "plan")`: PlanSkill - 创建项目计划
- `@Skill(id = "work")`: WorkSkill - 实现批准的任务
- `@Skill(id = "review")`: ReviewSkill - 独立审查代码

**Markdown技能加载器**：
```java
public interface MarkdownSkillLoader {
    List<Skill> loadFromMarkdown(File skillDirectory);
    Skill loadSkill(File skillFile);
    boolean validateSkillSyntax(File skillFile);
}
```

#### 代理系统设计

**完整三种代理对等Go项目**：

```java
// 代理接口
public interface Agent {
    String getId();
    String getType(); // "worker", "reviewer", "advisor"
    AgentResult execute(AgentContext context);
    void notify(String event, Object data);
}
```

**代理实现**：
- `@Agent(id = "worker", type = "worker")`: WorkerAgent - 执行实现工作
- `@Agent(id = "reviewer", type = "reviewer")`: ReviewerAgent - 独立代码审查
- `@Agent(id = "advisor", type = "advisor")`: AdvisorAgent - 策略建议

#### 协调机制

```java
// 协调协议接口
public interface CoordinationProtocol {
    void broadcast(String event, Object data);
    void subscribe(String event, EventHandler handler);
    void unsubscribe(String event, EventHandler handler);
}

// 基于内存的协调协议实现
public class InMemoryCoordinationProtocol implements CoordinationProtocol {
    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    
    @Override
    public void broadcast(String event, Object data) {
        List<EventHandler> eventHandlers = handlers.getOrDefault(event, Collections.emptyList());
        eventHandlers.forEach(handler -> handler.handle(event, data));
    }
}
```

---

### 3.6 运行时层 (Runtime)

**职责**: CLI入口、Native Image支持和Spring Boot服务

#### CLI主入口

```java
public class HarnessCli {
    private final HookRegistry hookRegistry;
    private final HookCodec codec;
    
    public static void main(String[] args) {
        HarnessCli cli = new HarnessCli();
        cli.run(args);
    }
    
    public void run(String[] args) {
        // 解析命令行参数
        CommandLineArgs cmdArgs = parseArgs(args);
        
        // 读取Hook输入
        HookInput input = codec.decode(System.in);
        
        // 路由到对应的Hook处理器
        HookHandler handler = hookRegistry.resolve(input.getHookEventName());
        HookOutput output = handler.handle(input);
        
        // 输出结果
        codec.encode(output, System.out);
        
        // 返回适当的退出码
        System.exit(output.getPermissionDecision().equals("deny") ? 2 : 0);
    }
}
```

#### Native Image支持

**GraalVM反射配置**：
```json
// reflect-config.json
{
  "reflectConfig": [
    {
      "name": "com.chachamaru.harness.foundation.dto.HookInput",
      "allDeclaredFields": true,
      "allPublicMethods": true
    }
  ]
}
```

**自动特性注册**：
```java
@AutomaticFeature
public class HarnessNativeFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerReflectionConfigs(access);
        registerResourceConfigs(access);
        registerJNIConfigs(access);
    }
}
```

#### Spring Boot服务

```java
@SpringBootApplication
@EnableConfigurationProperties(HarnessConfig.class)
public class HarnessService {
    public static void main(String[] args) {
        SpringApplication.run(HarnessService.class, args);
    }
}

// REST API控制器
@RestController
@RequestMapping("/api/v1")
public class HarnessApiController {
    @PostMapping("/webhook/hook")
    public ResponseEntity<HookOutput> handleHook(@RequestBody HookInput input) {
        return ResponseEntity.ok(hookService.process(input));
    }
    
    @PostMapping("/orchestration/execute")
    public ResponseEntity<ExecutionResult> executeOrchestration(
        @RequestBody OrchestrationRequest request) {
        return ResponseEntity.ok(orchestrationService.execute(request));
    }
}
```

---

## 4. 关键技术决策

### 4.1 Plans.md解析技术

**选择**: 正则表达式 + 自定义解析器

**理由**:
- Plans.md是结构化的Markdown，不复杂到需要完整AST
- 自定义解析器更轻量，避免引入重量级解析库
- 正则表达式可以处理表格解析和标记识别
- 性能足够，维护简单

### 4.2 并行执行技术

**选择**: CompletableFuture + 自定义Semaphore

**理由**:
- Java 8+内置支持，无需额外依赖
- 与Spring Boot集成良好
- 支持异常处理和超时控制
- 支持Native Image编译

### 4.3 状态恢复技术

**选择**: 状态机 + 指数退避重试

**理由**:
- 状态机模式清晰地定义恢复流程
- 指数退避避免无限重试
- 支持可配置的恢复策略
- 与Go项目的4阶段恢复机制对等

### 4.4 配置管理技术

**选择**: YAML + Spring Boot配置抽象

**理由**:
- YAML对Java开发者更友好
- Spring Boot有成熟的YAML支持
- 支持profile管理和环境变量覆盖
- 可以提供Go项目TOML转换工具

### 4.5 技能系统设计

**选择**: 混合模式（Java核心 + Markdown扩展）

**理由**:
- Java核心技能保证性能和稳定性
- Markdown扩展提供灵活性
- 支持高级用户自定义技能
- 保持与Go项目技能格式兼容

---

## 5. 数据流设计

### 5.1 Hook处理流程

```
Claude Code → stdin → HookCodec.decode() → HookInput
                              ↓
                        HookRegistry.resolve()
                              ↓
    ┌─────────────────────────┴─────────────────────────┐
    │                                                   │
    ↓                                                   ↓
PreToolUseHandler                               PermissionRequestHandler
    ↓                                                   ↓
GuardrailEngine.evaluate()                    PermissionEvaluator
    ↓                                                   ↓
R01-R15规则评估                                  Context检查
    ↓                                                   ↓
GuardrailResult ←────────────────────────────────→ HookOutput
    ↓                                                   ↓
HookCodec.encode() → stdout → Claude Code
```

### 5.2 工作流执行流程

```
用户触发 /work → SkillContext
                      ↓
              WorkSkill.execute()
                      ↓
         ┌────────────┴────────────┐
         ↓                         ↓
PlansParser.parse()      TaskOrchestrator.createPlan()
         ↓                         ↓
PlansDocument         OrchestrationPlan
         ↓                         ↓
DependencyResolver.resolve()   TaskExecutor
         ↓                         ↓
ExecutionOrder      ┌──────────┴──────────┐
         ↓          ↓                     ↓
  Sequential执行   ParallelExecutor   StateRecovery
         ↓          ↓                     ↓
    Implementation  并行任务处理      失败恢复
         ↓          ↓                     ↓
         └──────────┴───────────────────┘
                      ↓
              WorkResult Artifact
```

### 5.3 代理协作流程

```
Lead Claude会话
      ↓
AgentCoordinator.coordinate()
      ↓
  ┌───┴────┬─────────┐
  ↓        ↓         ↓
Worker   Reviewer  Advisor
  ↓        ↓         ↓
执行任务   代码审查   策略建议
  ↓        ↓         ↓
  └────────┴─────────┘
      ↓
CoordinationProtocol.broadcast()
      ↓
AgentResult → Lead Claude会话
```

---

## 6. 双模式部署策略

### 6.1 模式对比

| 特性 | JAR模式 | Native Image模式 |
|------|---------|------------------|
| **启动时间** | 2-5秒 | <100ms |
| **内存占用** | 150-200MB | 50-80MB |
| **部署包大小** | 30-50MB | 15-25MB |
| **开发调试** | ✅ 友好 | ⚠️ 需要重新编译 |
| **动态加载** | ✅ 完全支持 | ⚠️ 需要配置 |
| **生产性能** | 良好 | 优秀 |

### 6.2 模式切换设计

```java
// 运行时模式检测
public enum RuntimeMode {
    JAR, NATIVE_IMAGE, UNKNOWN;
    
    public static RuntimeMode detect() {
        try {
            Class.forName("org.graalvm.nativeimage.ImageCode");
            return NATIVE_IMAGE;
        } catch (ClassNotFoundException e) {
            return JAR;
        }
    }
}
```

### 6.3 构建流程

```bash
# JAR模式构建
mvn clean package -DskipTests

# Native Image模式构建
mvn clean package -Pnative -DskipTests

# 开发模式（快速迭代）
mvn compile -plasma

# 生产模式（完整测试）
mvn clean package -Pproduction
```

---

## 7. 测试策略

### 7.1 测试金字塔

```
           ┌─────────────┐
           │  E2E Tests  │  ← 5%
           ├─────────────┤
           │Integration  │  ← 15%
           │   Tests      │
           ├─────────────┤
           │ Unit Tests  │  ← 80%
           └─────────────┘
```

### 7.2 单元测试重点

- **Guardrail规则测试**: 每个规则的匹配和评估逻辑
- **Plans解析器测试**: 表格解析、标记识别、依赖解析
- **技能执行测试**: 核心技能的执行逻辑
- **代理执行测试**: Worker/Reviewer/Advisor执行

### 7.3 集成测试重点

- **Hook处理流程**: 完整的输入→处理→输出流程
- **工作流执行**: Plans.md解析→编排→执行→恢复
- **代理协作**: 多代理协调和通信
- **状态恢复**: 4阶段恢复机制

### 7.4 性能测试

- **Hook处理时间**: 目标<10ms（95th percentile）
- **内存占用**: Native Image目标<50MB
- **启动时间**: Native Image目标<100ms
- **并发处理**: 支持多个Hook事件并行处理

---

## 8. 配置管理

### 8.1 配置文件结构

```yaml
# harness.yaml
harness:
  project:
    name: "my-project"
    version: "1.0.0"
  
  # 安全配置
  security:
    guardrails:
      enabled-rules: [R01, R02, R03, R04, R05]
      protected-paths: [".env", ".git/", "*.pem"]
  
  # 工作流配置
  workflow:
    plans-path: "Plans.md"
    marker-family: "cc"
    parallel-execution: true
    max-concurrency: 4
  
  # 代理配置
  agents:
    worker:
      timeout: "5m"
      retry-strategy: "exponential-backoff"
    reviewer:
      cross-model: true
      temperature: 0.2
  
  # 状态恢复配置
  recovery:
    enabled: true
    max-phases: 4
    ttl:
      sessions: "24h"
      work-states: "7d"
```

### 8.2 配置兼容性

提供Go项目配置转换工具：
```bash
java -jar java-harness-tools.jar config convert \
  --from-toml path/to/harness.toml \
  --to-yaml path/to/harness.yaml
```

---

## 9. 部署策略

### 9.1 本地开发部署

```bash
# 1. 构建项目
mvn clean install -DskipTests

# 2. 配置项目
cp config/harness.yaml.example config/harness.yaml

# 3. 启动服务
java -jar java-harness-service/target/java-harness-service.jar

# 4. 或者使用CLI模式
java -jar java-harness-cli/target/java-harness-cli.jar hook pre-tool
```

### 9.2 生产环境部署

```bash
# Native Image模式（推荐）
./bin/harness hook pre-tool < hook-input.json

# Docker部署
docker run -d \
  -v /path/to/config:/app/config \
  -p 8080:8080 \
  java-harness:latest
```

### 9.3 监控和健康检查

Spring Boot Actuator端点：
- `/api/v1/monitoring/health`: 健康状态
- `/api/v1/monitoring/metrics`: 性能指标
- `/api/v1/plans/status`: Plans.md状态

---

## 10. 与Go项目的互操作性

### 10.1 配置兼容性

提供转换工具支持Go项目的`harness.toml`到Java项目的`harness.yaml`的转换。

### 10.2 状态迁移

提供状态导入/导出工具，支持Go项目的状态数据迁移到Java项目。

### 10.3 功能对等性

确保Java项目实现Go项目的所有核心功能：
- ✅ Hook协议处理
- ✅ Guardrail安全规则
- ✅ 技能系统（混合模式）
- ✅ 代理系统（三种代理）
- ✅ 工作流编排（Plans.md + 并行执行）
- ✅ 状态恢复（4阶段机制）
- ✅ 配置管理（YAML优先）

---

## 11. 实施路线图

### 11.1 阶段1：基础架构重构（2-3周）

**目标**: 重组模块结构，建立分层架构

**任务**:
- 创建9个Maven模块结构
- 实现基础设施层（DTO、配置、持久化）
- 实现协议层（Hook协议、编解码器）
- 迁移现有代码到新模块结构

**验收标准**:
- [ ] 所有模块编译成功
- [ ] 单元测试覆盖率>70%
- [ ] 现有功能无回归

### 11.2 阶段2：工作流层实现（3-4周）

**目标**: 实现Plans.md解析和任务编排

**任务**:
- 实现Plans.md解析器（正则表达式）
- 实现任务编排器
- 实现并行执行器（CompletableFuture）
- 实现依赖解析器
- 实现基础的状态恢复

**验收标准**:
- [ ] 能正确解析Plans.md
- [ ] 支持任务依赖解析
- [ ] 支持并行任务执行
- [ ] 单元测试覆盖率>75%

### 11.3 阶段3：协作层实现（4-5周）

**目标**: 实现技能系统和代理系统

**任务**:
- 实现技能框架（Java + Markdown）
- 实现核心技能（plan/work/review）
- 实现代理框架
- 实现三种代理（worker/reviewer/advisor）
- 实现代理协调机制

**验收标准**:
- [ ] 核心技能执行正确
- [ ] Markdown技能可以加载和执行
- [ ] 三种代理可以正常工作
- [ ] 代理协调机制运行正常

### 11.4 阶段4：状态恢复完善（2-3周）

**目标**: 实现完整的4阶段恢复机制

**任务**:
- 实现自我修复策略
- 实现同伴修复策略
- 实现指挥官介入策略
- 实现停止策略
- 完善状态机管理

**验收标准**:
- [ ] 4阶段恢复机制完整实现
- [ ] 状态恢复测试覆盖率>80%
- [ ] 恢复策略可配置

### 11.5 阶段5：工具层和优化（2-3周）

**目标**: 实现配置管理工具和性能优化

**任务**:
- 实现配置同步工具
- 实现验证工具
- 实现诊断工具（doctor）
- 性能优化和Native Image支持
- 文档完善

**验收标准**:
- [ ] 配置工具可以正常工作
- [ ] 验证工具可以检测问题
- [ ] 诊断工具可以报告健康状态
- [ ] Native Image编译成功
- [ ] 性能测试通过

### 11.6 阶段6：集成测试和发布（1-2周）

**目标**: 完整测试和发布准备

**任务**:
- 集成测试完善
- 性能测试和优化
- 文档完善
- 发布准备

**验收标准**:
- [ ] 集成测试覆盖率>85%
- [ ] 性能测试通过
- [ ] 文档完整
- [ ] 发布准备完成

---

## 12. 风险和挑战

### 12.1 技术风险

**风险1: Plans.md解析复杂性**
- **描述**: Plans.md格式可能比预期复杂
- **缓解**: 先实现核心功能，逐步完善解析器
- **应急**: 可以考虑引入轻量级Markdown解析库

**风险2: Native Image兼容性**
- **描述**: GraalVM Native Image可能有兼容性问题
- **缓解**: 早期进行Native Image测试，及时发现问题
- **应急**: 优先保证JAR模式，Native Image作为优化

**风险3: 性能目标达成**
- **描述**: Hook处理时间可能无法达到<10ms目标
- **缓解**: 性能测试和优化并行进行
- **应急**: 调整性能目标，优先保证功能正确性

### 12.2 实施风险

**风险1: 模块重构复杂度**
- **描述**: 模块重组可能影响现有功能
- **缓解**: 渐进式重构，保持现有功能运行
- **应急**: 分阶段重构，每个阶段都保持可用性

**风险2: 测试覆盖率不足**
- **描述**: 复杂功能可能测试不充分
- **缓解**: 增加集成测试和端到端测试
- **应急**: 延长测试阶段，确保质量

### 12.3 维护风险

**风险1: 与Go项目同步**
- **描述**: Go项目更新后，Java项目需要同步
- **缓解**: 定期对比功能差异，及时同步
- **应急**: 建立自动化对比工具

---

## 13. 成功标准

### 13.1 功能对等性

- [ ] 实现Go项目90%+的核心功能
- [ ] 支持Plan→Work→Review→Release闭环
- [ ] 支持三种代理协作
- [ ] 支持4阶段状态恢复

### 13.2 性能标准

- [ ] Hook处理时间<10ms（95th percentile）
- [ ] Native Image启动时间<100ms
- [ ] Native Image内存占用<50MB
- [ ] JAR模式启动时间<5秒

### 13.3 质量标准

- [ ] 单元测试覆盖率>75%
- [ ] 集成测试覆盖率>80%
- [ ] 代码审查通过率>95%
- [ ] 无关键性bug

### 13.4 易用性标准

- [ ] 配置文件简单易懂
- [ ] 错误信息清晰有用
- [ ] 文档完整准确
- [ ] 安装部署简单

---

## 14. 后续考虑

### 14.1 未来扩展

**多工具支持**: 如果需要支持Codex/Cursor等其他工具：
- 扩展Hook协议支持
- 添加工具特定的处理器
- 配置工具检测和适配

**云原生支持**: 如果需要云原生部署：
- 添加Kubernetes部署支持
- 实现健康检查和就绪探针
- 支持配置管理

**插件系统**: 如果需要更强的扩展性：
- 实现插件加载机制
- 提供插件开发SDK
- 建立插件市场

### 14.2 持续改进

**性能优化**: 持续监控和优化性能
- Hook处理时间优化
- 内存占用优化
- 启动时间优化

**功能增强**: 根据用户反馈持续改进
- 新的技能类型
- 新的代理类型
- 新的编排策略

**生态建设**: 建立健康的社区生态
- 技能分享平台
- 代理模板库
- 最佳实践文档

---

## 15. 结论

本设计文档定义了Java Harness项目从当前35-40%功能实现度扩展到与Go项目功能对等的完整技术方案。采用功能域驱动设计，7层清晰分离，重点实现技能系统、代理系统、工作流编排、状态恢复等核心功能。

**核心优势**:
- ✅ 清晰的架构边界，易于理解和维护
- ✅ 符合Java/Spring生态，开发者友好
- ✅ 支持双模式部署，灵活性强
- ✅ 功能对等Go项目，完整性高
- ✅ 轻量级定位，避免过度工程化

**实施建议**:
- 按阶段逐步实施，每阶段保持可用性
- 重视测试和质量，确保稳定性
- 保持与Go项目的功能同步
- 收集用户反馈，持续改进

**预期成果**:
- 一个功能完整的Java版本Claude Code Harness
- 与Go项目功能对等，架构一致
- 支持轻量级部署和快速上手
- 为Java开发者提供优秀的AI开发工作流工具

---

*本设计文档已经过详细讨论和验证，作为实施的技术指南。*

**文档版本**: 1.0  
**最后更新**: 2026-08-01  
**状态**: 已批准