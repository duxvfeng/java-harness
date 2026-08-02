# Java Harness API 文档

## 概述

Java Harness 是一个功能完整的 Claude Code 集成框架，提供了一套丰富的 API 用于构建、执行和管理 AI 驱动的开发工作流。

## 核心模块

### 1. 基础层 (Foundation Layer)

#### 配置管理

```java
package com.chachamaru.harness.foundation.config;

/**
 * 配置接口，提供统一的配置管理
 */
public interface Configuration {
    String getString(String key);
    int getInt(String key);
    boolean getBoolean(String key);
    void set(String key, Object value);
    void save();
}

/**
 * 可配置接口，支持动态配置
 */
public interface Configurable {
    void configure(Configuration config);
    Configuration getConfiguration();
}
```

#### 状态管理

```java
package com.chachamaru.harness.foundation.state;

/**
 * 状态持久化引擎接口
 */
public interface StatePersistenceEngine<T> {
    void save(T state, Path path) throws PersistenceException;
    Optional<T> load(Path path, Class<T> type) throws PersistenceException;
    boolean exists(Path path);
    void delete(Path path) throws PersistenceException;
}

/**
 * 状态持久化工厂
 */
public class StatePersistenceFactory {
    public static <T> StatePersistenceEngine<T> createFromExtension(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".json")) {
            return new JsonStatePersistence<>();
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return new YamlStatePersistence<>();
        }
        throw new IllegalArgumentException("Unsupported file format");
    }
}
```

#### DTO 对象

```java
package com.chachamaru.harness.foundation.dto;

/**
 * Hook 输入对象
 */
public record HookInput(
    String hookName,
    String toolName,
    List<String> arguments,
    String sessionId,
    String errorMessage
) {}

/**
 * Hook 输出对象
 */
public record HookOutput(
    String hookName,
    GuardrailResult guardrailResult,
    long executionTimeMs,
    String sessionId,
    String eventData,
    String agentId,
    String reason,
    boolean shouldEscalate
) {}

/**
 * Guardrail 结果对象
 */
public record GuardrailResult(
    boolean allowed,
    String reason,
    String ruleName
) {}
```

### 2. 协议层 (Protocol Layer)

#### Plans 解析器

```java
package com.chachamaru.harness.workflow.parser;

/**
 * Plans 文档解析器接口
 */
public interface PlansParser {
    PlansDocument parse(String filePath) throws ParseException;
    PlansDocument parse(Path path) throws ParseException;
}

/**
 * 正则表达式 Plans 解析器实现
 */
public class RegexPlansParser implements PlansParser {
    @Override
    public PlansDocument parse(String filePath) {
        return parse(Paths.get(filePath));
    }

    @Override
    public PlansDocument parse(Path path) {
        try {
            String content = Files.readString(path);
            return parseContent(content);
        } catch (IOException e) {
            throw new ParseException("Failed to read Plans.md", e);
        }
    }
}
```

#### Plans 文档模型

```java
package com.chachamaru.harness.workflow.model;

/**
 * Plans 文档
 */
public record PlansDocument(
    String title,
    String description,
    List<Task> tasks,
    String metadata
) {}

/**
 * 任务模型
 */
public record Task(
    String id,
    String title,
    String content,
    String dod,
    List<String> dependencies,
    Status status,
    int priority
) {
    public enum Status {
        TODO, IN_PROGRESS, COMPLETED, BLOCKED
    }
}
```

### 3. 安全层 (Security Layer)

#### Guardrail 系统

```java
package com.chachamaru.harness.security.guardrail;

/**
 * Guardrule 规则接口
 */
public interface Guardrule {
    String getName();
    String getDescription();
    GuardrailResult evaluate(HookInput input);
    int getPriority();
}

/**
 * Guardrail 引擎
 */
public class GuardrailEngine {
    private final List<Guardrule> rules;

    public GuardrailEngine() {
        this.rules = new ArrayList<>();
        loadDefaultRules();
    }

    public GuardrailResult evaluate(HookInput input) {
        // 按优先级排序规则
        rules.sort(Comparator.comparingInt(Guardrule::getPriority).reversed());

        for (Guardrule rule : rules) {
            GuardrailResult result = rule.evaluate(input);
            if (!result.allowed()) {
                return result;
            }
        }

        return new GuardrailResult(true, "All checks passed", "default");
    }

    public void registerRule(Guardrule rule) {
        rules.add(rule);
    }
}
```

### 4. 工作流层 (Workflow Layer)

#### 任务编排器

```java
package com.chachamaru.harness.workflow.orchestration;

/**
 * 任务编排器
 */
public class TaskOrchestrator {
    public ExecutionPlan createPlan(List<Task> tasks) {
        // 解析依赖关系
        List<Task> sorted = topologicalSort(tasks);
        return new ExecutionPlan(sorted);
    }

    public ExecutionResult executePlan(ExecutionPlan plan, ExecutionMode mode) {
        return switch (mode) {
            case SOLO -> executeSolo(plan);
            case PARALLEL -> executeParallel(plan);
            case BREEZING -> executeBreezing(plan);
        };
    }
}
```

#### 工作流引擎

```java
package com.chachamaru.harness.workflow.engine;

/**
 * 工作流引擎
 */
public class WorkflowEngine {
    public WorkflowResult executeSolo(ExecutionPlan plan) {
        // 单线程执行
        List<TaskResult> results = new ArrayList<>();
        for (Task task : plan.tasks()) {
            results.add(executeTask(task));
        }
        return new WorkflowResult(results, mode);
    }

    public WorkflowResult executeParallel(ExecutionPlan plan, int workerCount) {
        // 并行执行
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<TaskResult>> futures = new ArrayList<>();

        for (Task task : plan.tasks()) {
            futures.add(executor.submit(() -> executeTask(task)));
        }

        List<TaskResult> results = futures.stream()
            .map(this::get)
            .toList();

        return new WorkflowResult(results, mode);
    }
}
```

#### 恢复系统

```java
package com.chachamaru.harness.workflow.recovery;

/**
 * 4阶段恢复系统
 */
public class FourPhaseRecovery {
    public RecoveryResult attemptSelfHealing(String sessionId) {
        // Phase 1: Self-healing
        return executePhase("self-healing", sessionId);
    }

    public RecoveryResult attemptPeerRecovery(String sessionId) {
        // Phase 2: Peer recovery
        return executePhase("peer-recovery", sessionId);
    }

    public RecoveryResult attemptLeadIntervention(String sessionId) {
        // Phase 3: Lead intervention
        return executePhase("lead-intervention", sessionId);
    }

    public void markAborted(String sessionId) {
        // Phase 4: Abort
        recoveryStates.remove(sessionId);
    }
}
```

### 5. 协作层 (Collaboration Layer)

#### Agent 系统

```java
package com.chachamaru.harness.collaboration.agent;

/**
 * Agent 基础接口
 */
public interface Agent {
    String getAgentId();
    AgentType getType();
    CompletableFuture<AgentResult> executeAsync(AgentTask task);
    void stop();
    boolean isRunning();
}

/**
 * Worker Agent 实现
 */
public class WorkerAgent implements Agent {
    @Override
    public CompletableFuture<AgentResult> executeAsync(AgentTask task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 执行任务逻辑
                Object result = executeTaskLogic(task);
                return new AgentResult(task.taskId(), result, true, null);
            } catch (Exception e) {
                return new AgentResult(task.taskId(), null, false, e.getMessage());
            }
        });
    }
}
```

#### 协调器

```java
package com.chachamaru.harness.collaboration.coordination;

/**
 * Agent 协调器
 */
public class AgentCoordinator {
    public Agent createAgent(AgentType type) {
        return createAgent(type, "claude");
    }

    public Agent createAgent(AgentType type, String backend) {
        return switch (type) {
            case LEAD -> new LeadAgent(backend);
            case WORKER -> new WorkerAgent(backend);
            case REVIEWER -> new ReviewerAgent(backend);
            case ADVISOR -> new AdvisorAgent(backend);
        };
    }

    public CoordinationResult executeBreezingWorkflow(
        Agent lead,
        List<Agent> workers,
        Agent reviewer,
        Agent advisor,
        List<AgentTask> tasks
    ) {
        // 实现 Breezing 工作流逻辑
        return executeBreezingLogic(lead, workers, reviewer, advisor, tasks);
    }
}
```

### 6. CLI 层 (CLI Layer)

#### 命令路由器

```java
package com.chachamaru.harness.cli.router;

/**
 * 命令路由器
 */
public class CommandRouter {
    private final Map<String, Command> commands = new HashMap<>();

    public void registerCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public CommandResult executeCommand(String name, String... args) {
        Command command = commands.get(name);
        if (command == null) {
            return CommandResult.error("Unknown command: " + name);
        }
        return command.execute(args);
    }
}
```

#### Hook 执行器

```java
package com.chachamaru.harness.cli.hook;

/**
 * Hook 执行器
 */
public class HookExecutor {
    public HookOutput executeHook(HookInput input) {
        String hookName = input.hookName();
        Hook hook = hookRegistry.getHook(hookName);

        if (hook == null) {
            return new HookOutput(hookName, null, 0, input.sessionId(), null, null, null, false);
        }

        long startTime = System.currentTimeMillis();
        HookOutput output = hook.execute(input);
        long executionTime = System.currentTimeMillis() - startTime;

        return new HookOutput(
            hookName,
            output.guardrailResult(),
            executionTime,
            input.sessionId(),
            output.eventData(),
            output.agentId(),
            output.reason(),
            output.shouldEscalate()
        );
    }
}
```

### 7. 服务层 (Service Layer)

#### Harness 服务

```java
package com.chachamaru.harness.service;

/**
 * Harness 主服务
 */
@Service
public class HarnessService {
    private final WorkflowEngine workflowEngine;
    private final AgentCoordinator agentCoordinator;
    private final HookExecutor hookExecutor;

    public ServiceResult executeTask(String taskId) {
        // 创建执行计划
        ExecutionPlan plan = createExecutionPlan(taskId);

        // 执行工作流
        WorkflowResult result = workflowEngine.executeSolo(plan);

        return new ServiceResult(result.isSuccess(), result.getOutput());
    }
}
```

### 8. 工具层 (Tools Layer)

#### 配置同步工具

```java
package com.chachamaru.harness.tools.config;

/**
 * 配置同步工具
 */
public class ConfigSyncTool implements Tool {
    @Override
    public ToolResult execute(ToolInput input) {
        String action = input.getAction();

        return switch (action) {
            case "sync" -> syncConfig(input.getSource(), input.getTarget());
            case "validate" -> validateConfig(input.getSource());
            case "merge" -> mergeConfig(input.getSource(), input.getTarget());
            default -> ToolResult.error("Unknown action: " + action);
        };
    }
}
```

#### 验证工具

```java
package com.chachamaru.harness.tools.validation;

/**
 * 项目验证工具
 */
public class ValidateTool implements Tool {
    @Override
    public ToolResult execute(ToolInput input) {
        Path projectPath = Paths.get(input.getProjectPath());

        // 执行验证检查
        List<ValidationIssue> issues = new ArrayList<>();
        issues.addAll(validateStructure(projectPath));
        issues.addAll(validateConfiguration(projectPath));
        issues.addAll(validateDependencies(projectPath));

        return new ToolResult(issues.isEmpty(), issues);
    }
}
```

### 9. CI 集成层 (CI Layer)

#### CI 状态监控

```java
package com.chachamaru.harness.ci.monitor;

/**
 * CI 状态监控器
 */
public class CIStatusMonitor {
    public void startMonitoring() {
        monitoring = true;
        monitorThread = new Thread(this::monitoringLoop);
        monitorThread.start();
    }

    public CIStatus checkStatus() {
        // 检查 CI 状态
        return aggregateStatus();
    }

    private void monitoringLoop() {
        while (monitoring) {
            CIStatus status = checkStatus();
            notifyListeners(status);
            Thread.sleep(checkInterval);
        }
    }
}
```

#### GitHub Actions 集成

```java
package com.chachamaru.harness.ci.github;

/**
 * GitHub Actions 集成
 */
public class GitHubActionsIntegration {
    public boolean triggerWorkflow(String workflowName) {
        // 触发 GitHub Actions 工作流
        return githubClient.triggerWorkflow(workflowName);
    }

    public List<WorkflowRun> getWorkflowRuns() {
        // 获取工作流运行记录
        return githubClient.getWorkflowRuns();
    }
}
```

## 使用示例

### 基本工作流执行

```java
// 创建工作流引擎
WorkflowEngine engine = new WorkflowEngine();

// 解析 Plans.md
PlansParser parser = new RegexPlansParser();
PlansDocument document = parser.parse("Plans.md");

// 创建执行计划
TaskOrchestrator orchestrator = new TaskOrchestrator();
ExecutionPlan plan = orchestrator.createPlan(document.tasks());

// 执行工作流
WorkflowResult result = engine.executeSolo(plan);
```

### Agent 协调

```java
// 创建 Agent 协调器
AgentCoordinator coordinator = new AgentCoordinator();

// 创建 Breezing 团队
Agent lead = coordinator.createAgent(AgentType.LEAD);
List<Agent> workers = List.of(
    coordinator.createAgent(AgentType.WORKER),
    coordinator.createAgent(AgentType.WORKER)
);
Agent reviewer = coordinator.createAgent(AgentType.REVIEWER);
Agent advisor = coordinator.createAgent(AgentType.ADVISOR);

// 执行 Breezing 工作流
CoordinationResult result = coordinator.executeBreezingWorkflow(
    lead, workers, reviewer, advisor, tasks
);
```

### 状态管理

```java
// 创建状态持久化引擎
StatePersistenceEngine<AppState> persistence =
    StatePersistenceFactory.createFromExtension(Paths.get("app-state.json"));

// 保存状态
AppState state = new AppState();
persistence.save(state, Paths.get("app-state.json"));

// 加载状态
Optional<AppState> loaded = persistence.load(
    Paths.get("app-state.json"),
    AppState.class
);
```

## 配置

### 应用配置

```java
// 创建配置
Configuration config = new JsonConfiguration();
config.set("workflow.mode", "BREEZING");
config.set("agent.backend", "claude");
config.set("hooks.enabled", "true");
config.save();
```

### Hook 配置

```java
// 配置 Hook 系统
HookRegistry registry = new HookRegistry();
registry.registerHook("pre-tool", new PreToolHook());
registry.registerHook("post-tool", new PostToolHook());
```

## 错误处理

所有 API 方法都遵循统一的错误处理模式：

```java
try {
    result = api.execute();
} catch (ApiException e) {
    // 处理 API 异常
    logger.error("API execution failed", e);
}
```

## 性能考虑

- **异步执行**: 大部分 API 支持异步执行
- **缓存**: 配置和状态信息会被缓存
- **连接池**: 外部服务连接使用连接池
- **批处理**: 支持批量操作以提高性能

## 安全性

- **输入验证**: 所有输入都经过验证
- **权限检查**: 操作前进行权限验证
- **加密**: 敏感数据使用加密存储
- **审计**: 记录关键操作日志

## 扩展性

- **插件系统**: 支持自定义插件
- **Hook 扩展**: 可以添加自定义 Hook
- **Agent 自定义**: 支持自定义 Agent 实现
- **命令扩展**: 可以添加自定义 CLI 命令

## 版本兼容性

当前版本: `4.0.0-java-SNAPSHOT`

支持的 Java 版本: `17+`

支持的 Maven 版本: `3.6+`

## 依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 许可证

Apache License 2.0

## 联系方式

- 项目主页: https://github.com/chachamaru/java-harness
- 问题反馈: https://github.com/chachamaru/java-harness/issues
- 文档: https://docs.java-harness.dev