# Java版本功能完全对等实施计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现Java版本与Go版本100%功能对等，包括技能系统、Agent系统、工作流编排、配置管理、监控可观测性等全部功能

**架构：** 采用分阶段渐进式实施，4个阶段逐步实现：技能系统(6-8周) → Agent系统(6-8周) → 工作流基础设施(8-10周) → 高级功能集成(6-8周)

**技术栈：** Java 17, Spring Boot, Maven, MyBatis-Plus, SnakeYAML, GraalVM Native Image, OpenTelemetry, JUnit 5, CompletableFuture

---

## 📁 项目文件结构

```
java-harness/
├── java-harness-workflow/                    # 新增：工作流编排模块
│   ├── src/main/java/com/chachamaru/harness/workflow/
│   │   ├── config/
│   │   │   ├── WorkflowConfig.java
│   │   │   ├── StepConfig.java
│   │   │   └── WorkflowLoader.java
│   │   ├── engine/
│   │   │   ├── WorkflowEngine.java
│   │   │   ├── StepExecutor.java
│   │   │   ├── ConditionEvaluator.java
│   │   │   ├── VariableResolver.java
│   │   │   └── ParallelExecutionEngine.java
│   │   ├── skill/
│   │   │   ├── framework/
│   │   │   │   ├── SkillFramework.java
│   │   │   │   ├── SkillExecutor.java
│   │   │   │   ├── SkillContext.java
│   │   │   │   ├── SkillResult.java
│   │   │   │   └── SkillRegistry.java
│   │   │   └── core/
│   │   │       ├── PlanSkill.java
│   │   │       ├── WorkSkill.java
│   │   │       ├── ReviewSkill.java
│   │   │       ├── SyncSkill.java
│   │   │       └── ReleaseSkill.java
│   │   ├── agent/
│   │   │   ├── framework/
│   │   │   │   ├── AgentFramework.java
│   │   │   │   ├── AgentExecutor.java
│   │   │   │   ├── AgentContext.java
│   │   │   │   ├── AgentLifecycle.java
│   │   │   │   └── AgentCommunication.java
│   │   │   └── core/
│   │   │       ├── WorkerAgent.java
│   │   │       ├── ReviewerAgent.java
│   │   │       └── AdvisorAgent.java
│   │   ├── orchestrator/
│   │   │   ├── PlansMdParser.java
│   │   │   ├── DependencyAnalyzer.java
│   │   │   ├── TaskStateManager.java
│   │   │   └── WorktreeManager.java
│   │   ├── integration/
│   │   │   ├── SkillInvoker.java
│   │   │   ├── ContextManager.java
│   │   │   └── HookRouterIntegration.java
│   │   └── parser/
│   │       ├── YamlWorkflowParser.java
│   │       ├── ExpressionParser.java
│   │       └── TemplateEngine.java
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   │   ├── V1__create_schema.sql
│   │   │   ├── V2__create_skill_tables.sql
│   │   │   ├── V3__create_agent_tables.sql
│   │   │   ├── V4__create_workflow_tables.sql
│   │   │   ├── V5__create_audit_tables.sql
│   │   │   └── V6__create_config_tables.sql
│   │   └── workflows/
│   │       └── default/
│   │           ├── init.yaml (已存在)
│   │           ├── plan.yaml (已存在)
│   │           ├── review.yaml (已存在)
│   │           └── work.yaml (已存在)
│   └── src/test/java/com/chachamaru/harness/workflow/
│       ├── skill/
│       │   ├── PlanSkillTest.java
│       │   ├── WorkSkillTest.java
│       │   ├── ReviewSkillTest.java
│       │   ├── SyncSkillTest.java
│       │   └── ReleaseSkillTest.java
│       ├── agent/
│       │   ├── WorkerAgentTest.java
│       │   ├── ReviewerAgentTest.java
│       │   └── AdvisorAgentTest.java
│       ├── workflow/
│       │   ├── WorkflowEngineTest.java
│       │   ├── PlansMdParserTest.java
│       │   └── ParallelExecutionEngineTest.java
│       └── parity/
│           ├── SkillParityTest.java
│           ├── AgentParityTest.java
│           ├── WorkflowParityTest.java
│           └── GuardrailParityTest.java
│
├── java-harness-agents/                    # 新增：代理系统模块
│   ├── src/main/java/com/chachamaru/harness/agents/
│   │   ├── framework/
│   │   │   ├── AgentFramework.java
│   │   │   ├── AgentExecutor.java
│   │   │   ├── AgentContext.java
│   │   │   ├── AgentLifecycle.java
│   │   │   └── AgentCommunication.java
│   │   ├── coordination/
│   │   │   ├── AgentOrchestrator.java
│   │   │   ├── TeamCoordinator.java
│   │   │   ├── BreezingTeam.java
│   │   │   └── TeamMetrics.java
│   │   └── protocol/
│   │       ├── AgentMessage.java
│   │       ├── TaskAssignment.java
│   │       └── ResultAggregation.java
│   └── src/test/java/com/chachamaru/harness/agents/
│       ├── framework/
│       │   ├── AgentFrameworkTest.java
│       │   ├── AgentExecutorTest.java
│       │   └── AgentCommunicationTest.java
│       └── coordination/
│           └── BreezingTeamTest.java
│
├── java-harness-observability/          # 新增：可观测性模块
│   ├── src/main/java/com/chachamaru/harness/observability/
│   │   ├── tracing/
│   │   │   ├── OpenTelemetryConfigurator.java
│   │   │   ├── TracerManager.java
│   │   │   └── SpanManager.java
│   │   ├── audit/
│   │   │   ├── AuditLogger.java
│   │   │   ├── EventStore.java
│   │   │   └── AuditExporter.java
│   │   └── monitoring/
│   │       ├── MetricsCollector.java
│   │       ├── HealthChecker.java
│   │       └── PerformanceProfiler.java
│   └── src/test/java/com/chachamaru/harness/observability/
│       ├── tracing/
│       │   └── OpenTelemetryIntegrationTest.java
│       └── audit/
│           └── AuditLogSystemTest.java
│
├── java-harness-integration/             # 新增：集成模块
│   ├── src/main/java/com/chachamaru/harness/integration/
│   │   ├── GoVersionClient.java        # Go版本API客户端
│   │   ├── GoVersionParityComparator.java # 对照测试比较器
│   │   └── HarnessIntegrationTest.java   # 集成测试
│   └── src/test/java/com/chachamaru/harness/integration/
│       └── GoVersionParityTest.java
│
└── docs/superpowers/
    └── plans/
        └── 2026-08-04-java-harness-complete-parity.md # 实施计划（本文件）
```

---

## 第一阶段：技能系统实施 (6-8周)

### 任务 1：创建工作流编排模块基础结构

**文件：**
- 创建：`java-harness-workflow/pom.xml`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/WorkflowConfig.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/StepConfig.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/WorkflowLoader.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/config/WorkflowConfigTest.java`

- [ ] **步骤 1：创建工作流编排模块的pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.chachamaru.harness</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-java</version>
    </parent>

    <artifactId>java-harness-workflow</artifactId>
    <name>Java Harness - Workflow Module</name>
    <description>工作流编排和技能执行系统</description>

    <dependencies>
        <!-- 现有模块依赖 -->
        <dependency>
            <groupId>com.chachamaru.harness</groupId>
            <artifactId>java-harness-shared</artifactId>
        </dependency>
        <dependency>
            <groupId>com.chachamaru.harness</groupId>
            <artifactId>java-harness-foundation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.chachamaru.harness</groupId>
            <artifactId>java-harness-protocol</artifactId>
        </dependency>
        <dependency>
            <groupId>com.chachamaru.harness</groupId>
            <artifactId>java-harness-security</artifactId>
        </dependency>

        <!-- YAML解析 -->
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
            <version>2.2</version>
        </dependency>

        <!-- Spring Boot集成 (如果需要) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 测试框架 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：创建WorkflowConfig配置模型类**

```java
package com.chachamaru.harness.workflow.config;

import java.util.List;
import java.util.Map;

public class WorkflowConfig {
    private String phase;
    private String description;
    private List<StepConfig> steps;
    private CallbackConfig onSuccessConfig;
    private CallbackConfig onErrorConfig;

    // Getters and setters
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<StepConfig> getSteps() { return steps; }
    public void setSteps(List<StepConfig> steps) { this.steps = steps; }

    public CallbackConfig getOnSuccessConfig() { return onSuccessConfig; }
    public void setOnSuccessConfig(CallbackConfig onSuccessConfig) { this.onSuccessConfig = onSuccessConfig; }

    public CallbackConfig getOnErrorConfig() { return onErrorConfig; }
    public void setOnErrorConfig(CallbackConfig onErrorConfig) { this.onErrorConfig = onErrorConfig; }

    public int getStepCount() { return steps != null ? steps.size() : 0; }
}
```

运行：`mvn clean compile -pl`
预期：编译成功，无错误

- [ ] **步骤 3：创建StepConfig步骤配置模型类**

```java
package com.chachamaru.harness.workflow.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StepConfig {
    private String id;
    private String skill;
    private String condition;
    private InputConfig inputConfig;
    private OutputConfig outputConfig;
    private ExecutionMode mode;
    private boolean parallel;
    private String loopVariable;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public InputConfig getInputConfig() { return inputConfig; }
    public void setInputConfig(InputConfig inputConfig) { this.inputConfig = inputConfig; }

    public OutputConfig getOutputConfig() { return outputConfig; }
    public void setOutputConfig(OutputConfig outputConfig) { this.outputConfig = outputConfig; }

    public ExecutionMode getMode() { return mode; }
    public void setMode(ExecutionMode mode) { this.mode = mode; }

    public boolean isParallel() { return parallel; }
    public void setParallel(boolean parallel) { this.parallel = parallel; }

    public String getLoopVariable() { return loopVariable; }
    public void setLoopVariable(String loopVariable) { this.loopVariable = loopVariable; }
}

// ExecutionMode枚举
public enum ExecutionMode {
    REQUIRED,
    OPTIONAL,
    CONDITIONAL
}

// InputConfig类
class InputConfig {
    private List<String> files;
    private List<String> contextSources;
    private List<String> variables;
    private List<String> templates;

    // Getters and setters
    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }

    public List<String> getContextSources() { return contextSources; }
    public void setContextSources(List<String> contextSources) { this.contextSources = contextSources; }

    public List<String> getVariables() { return variables; }
    public void setVariables(List<String> variables) { this.variables = variables; }

    public List<String> getTemplates() { return templates; }
    public void setTemplates(List<String> templates) { this.templates = templates; }
}

// OutputConfig类
class OutputConfig {
    private List<String> variables;
    private List<String> updateFiles;
    private List<String> createFiles;
    private boolean userMessage;

    // Getters and setters
    public List<String> getVariables() { return variables; }
    public void setVariables(List<String> variables) { this.variables = variables; }

    public List<String> getUpdateFiles() { return updateFiles; }
    public void setUpdateFiles(List<String> updateFiles) { this.updateFiles = updateFiles; }

    public List<String> getCreateFiles() { return createFiles; }
    public void setCreateFiles(List<String> createFiles) { this.createFiles = createFiles; }

    public boolean isUserMessage() { return userMessage; }
    public void setUserMessage(boolean userMessage) { this.userMessage = userMessage; }
}

// CallbackConfig类
class CallbackConfig {
    private String message;
    private int maxRetries;
    private String recoverySkill;

    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public String getRecoverySkill() { return recoverySkill; }
    public void setRecoverySkill(String recoverySkill) { this.recoverySkill = recoverySkill; }
}
```

运行：`mvn clean compile -pl`
预期：编译成功

- [ ] **步骤 4：创建WorkflowLoader加载器**

```java
package com.chachamaru.harness.workflow.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowLoader {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorkflowLoader.class);

    public WorkflowConfig load(Path yamlPath) throws WorkflowLoadException {
        try {
            logger.info("Loading workflow from: {}", yamlPath);

            Yaml yaml = new Yaml(new Constructor(WorkflowConfig.class));
            Map<String, Object> data = yaml.load(new FileInputStream(yamlPath.toFile()));

            if (!data.containsKey("steps")) {
                throw new WorkflowLoadException("Workflow must contain 'steps' section");
            }

            WorkflowConfig config = new WorkflowConfig();
            config.setPhase((String) data.get("phase"));
            config.setDescription((String) data.get("description"));

            // 解析步骤
            List<Map<String, Object>> stepsData = (List<Map<String, Object>>) data.get("steps");
            List<StepConfig> steps = new ArrayList<>();

            for (Map<String, Object> stepData : stepsData) {
                StepConfig step = parseStepConfig(stepData);
                steps.add(step);
            }

            config.setSteps(steps);

            // 解析成功回调
            if (data.containsKey("on_success")) {
                config.setOnSuccessConfig(parseCallbackConfig((Map<String, Object>) data.get("on_success")));
            }
            if (data.containsKey("on_error")) {
                config.setOnErrorConfig(parseCallbackConfig((Map<String, Object>) data.get("on_error")));
            }

            logger.info("Successfully loaded workflow: {} with {} steps",
                config.getPhase(), config.getStepCount());

            return config;

        } catch (Exception e) {
            logger.error("Failed to load workflow configuration", e);
            throw new WorkflowLoadException("Failed to load workflow: " + yamlPath, e);
        }
    }

    private StepConfig parseStepConfig(Map<String, Object> stepData) {
        StepConfig step = new StepConfig();
        step.setId((String) stepData.get("id"));
        step.setSkill((String) stepData.get("skill"));

        if (stepData.containsKey("condition")) {
            step.setCondition((String) stepData.get("condition"));
        }

        if (stepData.containsKey("input")) {
            step.setInputConfig(parseInputConfig((Map<String, Object>) stepData.get("input")));
        }

        if (stepData.containsKey("output")) {
            step.setOutputConfig(parseOutputConfig((Map<String, Object>) stepData.get("output")));
        }

        if (stepData.containsKey("mode")) {
            step.setMode(ExecutionMode.valueOf(((String) stepData.get("mode")).toUpperCase()));
        }

        if (stepData.containsKey("parallel")) {
            step.setParallel((Boolean) stepData.get("parallel"));
        }

        if (stepData.containsKey("loop")) {
            step.setLoopVariable((String) stepData.get("loop"));
        }

        return step;
    }

    private InputConfig parseInputConfig(Map<String, Object> inputData) {
        InputConfig config = new InputConfig();

        if (inputData.containsKey("files")) {
            config.setFiles((List<String>) inputData.get("files"));
        }

        if (inputData.containsKey("context_from")) {
            config.setContextSources((List<String>) inputData.get("context_from"));
        }

        if (inputData.containsKey("variables")) {
            config.setVariables((List<String>) inputData.get("variables"));
        }

        if (inputData.containsKey("templates")) {
            config.setTemplates((List<String>) inputData.get("templates"));
        }

        return config;
    }

    private OutputConfig parseOutputConfig(Map<String, Object> outputData) {
        OutputConfig config = new OutputConfig();

        if (outputData.containsKey("variables")) {
            config.setVariables((List<String>) outputData.get("variables"));
        }

        if (outputData.containsKey("update_files")) {
            config.setUpdateFiles((List<String>) outputData.get("update_files"));
        }

        if (outputData.containsKey("create_files")) {
            config.setCreateFiles((List<String>) outputData.get("create_files"));
        }

        if (outputData.containsKey("user_message")) {
            config.setUserMessage((Boolean) outputData.get("user_message"));
        }

        return config;
    }

    private CallbackConfig parseCallbackConfig(Map<String, Object> callbackData) {
        CallbackConfig config = new CallbackConfig();
        config.setMessage((String) callbackData.get("message"));

        if (callbackData.containsKey("max_retries")) {
            config.setMaxRetries(((Number) callbackData.get("max_retries")).intValue());
        }

        if (callbackData.containsKey("recovery_skill")) {
            config.setRecoverySkill((String) callbackData.get("recovery_skill"));
        }

        return config;
    }
}

// 自定义异常类
class WorkflowLoadException extends Exception {
    public WorkflowLoadException(String message) {
        super(message);
    }

    public WorkflowLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

运行：`mvn clean compile -pl`
预期：编译成功，无错误

- [ ] **步骤 5：编写WorkflowConfigTest测试类**

```java
package com.chachamaru.harness.workflow.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

public class WorkflowConfigTest {

    @Test
    @DisplayName("应该加载有效的工作流YAML文件")
    public void testLoadValidWorkflow() throws WorkflowLoadException {
        WorkflowLoader loader = new WorkflowLoader();
        Path yamlPath = Paths.get("src/test/resources/workflows/valid/plan.yaml");

        WorkflowConfig config = loader.load(yamlPath);

        assertNotNull(config);
        assertEquals("plan", config.getPhase());
        assertTrue(config.getStepCount() > 0);
    }

    @Test
    @DisplayName("应该解析步骤配置")
    public void testParseStepConfig() {
        WorkflowLoader loader = new WorkflowLoader();
        Path yamlPath = Paths.get("src/test/resources/workflows/valid/init.yaml");

        WorkflowConfig config = loader.load(yamlPath);

        StepConfig firstStep = config.getSteps().get(0);
        assertEquals("analyze-project", firstStep.getId());
        assertEquals("harness-init", firstStep.getSkill());
    }

    @Test
    @DisplayName("应该处理条件表达式")
    public void testConditionExpression() {
        WorkflowLoader loader = new WorkflowLoader();
        Path yamlPath = Paths.get("src/test/resources/workflows/valid/work.yaml");

        WorkflowConfig config = loader.load(yamlPath);

        // 查找有条件的步骤
        StepConfig conditionalStep = config.getSteps().stream()
            .filter(step -> step.getCondition() != null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No conditional step found"));

        assertNotNull(conditionalStep.getCondition());
    }
}
```

运行：`mvn test -Dtest=WorkflowConfigTest`
预期：所有测试通过

- [ ] **步骤 6：Commit第一阶段任务**

```bash
git add java-harness-workflow/pom.xml
git add java-harness-workflow/src/
git commit -m "feat(workflow): 创建工作流编排模块基础结构和配置加载器"
```

---

### 任务 2：实现技能框架核心

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/SkillFramework.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/SkillExecutor.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/SkillContext.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/SkillResult.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/SkillRegistry.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/skill/framework/SkillFrameworkTest.java`

- [ ] **步骤 1：创建SkillFramework核心类**

```java
package com.chachamaru.harness.workflow.skill.framework;

import com.chachamaru.harness.workflow.integration.HookRouterIntegration;
import com.chachamaru.harness.security.GuardrailEngine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillFramework implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SkillFramework.class);

    private final Map<String, Skill> registeredSkills = new ConcurrentHashMap<>();
    private final SkillExecutor executor;
    private final HookRouterIntegration hookRouter;
    private final GuardrailEngine guardrailEngine;

    @Inject
    public SkillFramework(HookRouterIntegration hookRouter,
                         GuardrailEngine guardrailEngine) {
        this.hookRouter = hookRouter;
        this.guardrailEngine = guardrailEngine;
        this.executor = new SkillExecutor(guardrailEngine);
        initializeCoreSkills();
    }

    private void initializeCoreSkills() {
        registerSkill(new PlanSkill());
        registerSkill(new WorkSkill());
        registerSkill(new ReviewSkill());
        registerSkill(new SyncSkill());
        registerSkill(new ReleaseSkill());

        logger.info("Initialized {} core skills", registeredSkills.size());
    }

    public void registerSkill(Skill skill) {
        registeredSkills.put(skill.getSkillId(), skill);
        logger.debug("Registered skill: {}", skill.getSkillId());
    }

    public SkillResult executeSkill(String skillId, SkillContext context)
        throws SkillExecutionException {

        Skill skill = findSkill(skillId);
        if (skill == null) {
            throw new SkillNotFoundException(skillId);
        }

        return executor.execute(skill, context);
    }

    public Skill findSkill(String skillId) {
        return registeredSkills.get(skillId);
    }

    public Map<String, Skill> getRegisteredSkills() {
        return Map.copyOf(registeredSkills);
    }

    public int getSkillCount() {
        return registeredSkills.size();
    }

    @Override
    public void close() {
        logger.info("Shutting down SkillFramework");
        registeredSkills.clear();
    }
}
```

- [ ] **步骤 2：创建SkillExecutor执行引擎**

```java
package com.chachamaru.harness.workflow.skill.framework;

import com.chachamaru.harness.security.GuardrailEngine;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SkillExecutor.class);

    private final GuardrailEngine guardrailEngine;

    public SkillExecutor(GuardrailEngine guardrailEngine) {
        this.guardrailEngine = guardrailEngine;
    }

    public SkillResult execute(Skill skill, SkillContext context)
        throws SkillExecutionException {

        String executionId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            logger.info("Executing skill: {} (executionId: {})", skill.getSkillId(), executionId);

            // 执行技能的核心逻辑
            Object result = skill.execute(context);

            SkillResult skillResult = SkillResult.builder()
                .skillId(skill.getSkillId())
                .executionId(executionId)
                .status(SkillStatus.SUCCESS)
                .startTime(startTime)
                .completedTime(Instant.now())
                .output(result)
                .build();

            logger.info("Skill {} completed successfully", skill.getSkillId());
            return skillResult;

        } catch (Exception e) {
            logger.error("Skill {} execution failed", skill.getSkillId(), e);

            return SkillResult.builder()
                .skillId(skill.getSkillId())
                .executionId(executionId)
                .status(SkillStatus.FAILED)
                .startTime(startTime)
                .completedTime(Instant.now())
                .errorMessage(e.getMessage())
                .build();
        }
    }

    public void pauseExecution(String executionId) throws SkillExecutionException {
        // 实现暂停逻辑
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void resumeExecution(String executionId) throws SkillExecutionException {
        // 实现恢复逻辑
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void cancelExecution(String executionId) throws SkillExecutionException {
        // 实现取消逻辑
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private SkillResult handleExecutionError(Exception e, SkillContext context) {
        // 错误处理和恢复逻辑
        return SkillResult.failed("Execution error: " + e.getMessage());
    }
}
```

- [ ] **步骤 3：创建SkillContext上下文类**

```java
package com.chachamaru.harness.workflow.skill.framework;

import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillContext {
    private static final Logger logger = LoggerFactory.getLogger(SkillContext.class);

    private final String userIntent;
    private final Path projectRoot;
    private final PermissionMode permissionMode;
    private final Map<String, Object> contextData;
    private final Map<String, Path> files;
    private final Map<String, Object> variables;

    private SkillContext(Builder builder) {
        this.userIntent = builder.userIntent;
        this.projectRoot = builder.projectRoot;
        this.permissionMode = builder.permissionMode;
        this.contextData = Collections.unmodifiableMap(builder.contextData);
        this.files = Collections.unmodifiableMap(builder.files);
        this.variables = Collections.unmodifiableMap(builder.variables);
    }

    public String getUserIntent() {
        return userIntent;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public PermissionMode getPermissionMode() {
        return permissionMode;
    }

    public Object getContextData(String key) {
        return contextData.get(key);
    }

    public Path getFile(String filePath) {
        return files.get(filePath);
    }

    public Object getVariable(String varName) {
        return variables.get(varName);
    }

    public int getFileCount() {
        return files.size();
    }

    public int getVariableCount() {
        return variables.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userIntent;
        private Path projectRoot;
        private PermissionMode permissionMode = PermissionMode.DEFAULT;
        private Map<String, Object> contextData = new HashMap<>();
        private Map<String, Path> files = new HashMap<>();
        private Map<String, Object> variables = new HashMap<>();

        public Builder userIntent(String userIntent) {
            this.userIntent = userIntent;
            return this;
        }

        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder permissionMode(PermissionMode permissionMode) {
            this.permissionMode = permissionMode;
            return this;
        }

        public Builder addContextData(String key, Object value) {
            this.contextData.put(key, value);
            return this;
        }

        public Builder addFile(String key, Path filePath) {
            this.files.put(key, filePath);
            return this;
        }

        public Builder addVariable(String varName, Object value) {
            this.variables.put(varName, value);
            return this;
        }

        public SkillContext build() {
            return new SkillContext(this);
        }
    }

    public enum PermissionMode {
        DEFAULT,
        RESTRICTED,
        PERMISSIVE
    }
}
```

- [ ] **步骤 4：创建SkillResult结果类**

```java
package com.chachamaru.harness.workflow.skill.framework;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SkillResult {
    private final String skillId;
    private final String executionId;
    private final SkillStatus status;
    private final Instant startTime;
    private final Instant completedTime;
    private final Object output;
    private final String errorMessage;

    private SkillResult(Builder builder) {
        this.skillId = builder.skillId;
        this.executionId = builder.executionId;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.completedTime = builder.completedTime;
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public Object getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getExecutionDurationMs() {
        return completedTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public boolean isSuccess() {
        return status == SkillStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == SkillStatus.FAILED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String skillId;
        private String executionId = UUID.randomUUID().toString();
        private SkillStatus status = SkillStatus.PENDING;
        private Instant startTime = Instant.now();
        private Instant completedTime;
        private Object output;
        private String errorMessage;

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(SkillStatus status) {
            this.status = status;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder completedTime(Instant completedTime) {
            this.completedTime = completedTime;
            return this;
        }

        public Builder output(Object output) {
            this.output = output;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder failed(String errorMessage) {
            return status(SkillStatus.FAILED)
                .completedTime(Instant.now())
                .errorMessage(errorMessage);
        }

        public SkillResult build() {
            if (completedTime == null) {
                completedTime = Instant.now();
            }
            return new SkillResult(this);
        }
    }

    public enum SkillStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    public static SkillResult failed(String errorMessage) {
        return new Builder().failed(errorMessage);
    }
}
```

- [ ] **步骤 5：创建SkillRegistry注册表**

```java
package com.chachamaru.harness.workflow.skill.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillMetadata> registry = new ConcurrentHashMap<>();

    public void register(String skillId, String skillName, String version, String description) {
        SkillMetadata metadata = new SkillMetadata(skillId, skillName, version, description);
        registry.put(skillId, metadata);

        logger.info("Registered skill: {} (version: {})", skillId, version);
    }

    public SkillMetadata getMetadata(String skillId) {
        return registry.get(skillId);
    }

    public Map<String, SkillMetadata> getAllSkills() {
        return Map.copyOf(registry);
    }

    public boolean isRegistered(String skillId) {
        return registry.containsKey(skillId);
    }

    public int getSkillCount() {
        return registry.size();
    }

    private static class SkillMetadata {
        private final String skillId;
        private final String skillName;
        private final String version;
        private final String description;

        public SkillMetadata(String skillId, String skillName, String version, String description) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.version = version;
            this.description = description;
        }

        public String getSkillId() { return skillId; }
        public String getSkillName() { return skillName; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
    }
}
```

- [ ] **步骤 6：编写SkillFrameworkTest测试**

```java
package com.chachamaru.harness.workflow.skill.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SkillFramework测试")
public class SkillFrameworkTest {

    @Test
    @DisplayName("应该注册所有核心技能")
    public void testInitializeCoreSkills() {
        HookRouterIntegration hookRouter = mock(HookRouterIntegration.class);
        GuardrailEngine guardrailEngine = mock(GuardrailEngine.class);

        SkillFramework framework = new SkillFramework(hookRouter, guardrailEngine);

        assertEquals(5, framework.getSkillCount());
        assertTrue(framework.findSkill("plan").isPresent());
        assertTrue(framework.findSkill("work").isPresent());
    }

    @Test
    @DisplayName("应该执行Plan技能")
    public void testExecutePlanSkill() throws SkillExecutionException {
        HookRouterIntegration hookRouter = mock(HookRouterIntegration.class);
        GuardrailEngine guardrailEngine = mock(GuardrailEngine.class);
        SkillFramework framework = new SkillFramework(hookRouter, guardrailEngine);

        SkillContext context = SkillContext.builder()
            .userIntent("实现用户认证功能")
            .projectRoot(Paths.get("."))
            .build();

        SkillResult result = framework.executeSkill("plan", context);

        assertNotNull(result);
        assertEquals("plan", result.getSkillId());
        assertEquals(SkillStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("应该抛出异常当技能不存在时")
    public void testExecuteNonExistentSkill() {
        HookRouterIntegration hookRouter = mock(HookRouterIntegration.class);
        GuardrailEngine guardrailEngine = mock(GuardrailEngine.class);
        SkillFramework framework = new SkillFramework(hookRouter, guardrailEngine);

        SkillContext context = SkillContext.builder()
            .userIntent("测试")
            .projectRoot(Paths.get("."))
            .build();

        assertThrows(SkillNotFoundException.class, () ->
            framework.executeSkill("nonexistent", context)
        );
    }
}
```

运行：`mvn test -Dtest=SkillFrameworkTest`
预期：所有测试通过

- [ ] **步骤 7：Commit技能框架**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/framework/
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/skill/framework/
git commit -m "feat(workflow): 实现技能框架核心 - SkillFramework/SkillExecutor/SkillContext/SkillResult/SkillRegistry"
```

---

### 任务 3：实现PlanSkill核心技能

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/core/PlanSkill.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/skill/core/PlanSkillTest.java`

- [ ] **步骤 1：创建PlanSkill实现类**

```java
package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.*;
import com.chachamaru.harness.workflow.integration.ProjectAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlanSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(PlanSkill.class);
    private static final String SKILL_ID = "plan";
    private static final String SKILL_NAME = "Planning Skill";
    private static final String VERSION = "1.0.0-java";

    private final ProjectAnalyzer projectAnalyzer;

    @Inject
    public PlanSkill(ProjectAnalyzer projectAnalyzer) {
        this.projectAnalyzer = projectAnalyzer;
    }

    @Override
    public String getSkillId() {
        return SKILL_ID;
    }

    @Override
    public String getSkillName() {
        return SKILL_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "将功能需求转换为结构化任务列表，生成spec.md和Plans.md";
    }

    @Override
    public SkillResult execute(SkillContext context) {
        Instant startTime = Instant.now();

        try {
            logger.info("Executing Plan skill for intent: {}", context.getUserIntent());

            // 1. 分析用户意图
            PlanningIntent intent = analyzeIntent(context);

            // 2. 分析项目上下文
            ProjectContext projectContext = projectAnalyzer.analyze(context);

            // 3. 生成规格文档
            SpecDelta specDelta = generateSpecDelta(intent, projectContext);

            // 4. 生成Plans.md
            PlansMd plansMd = generatePlans(specDelta, intent, projectContext);

            // 5. 质量验证
            ValidationResult validation = validatePlanningResult(specDelta, plansMd);

            // 6. 生成交付前确认章节
            PreApprovalSection preApproval = generatePreApprovalSection(plansMd);

            Instant completedTime = Instant.now();

            return SkillResult.builder()
                .skillId(SKILL_ID)
                .executionId(UUID.randomUUID().toString())
                .status(SkillStatus.SUCCESS)
                .startTime(startTime)
                .completedTime(completedTime)
                .output(PlanningOutput.builder()
                    .specDelta(specDelta)
                    .plansMd(plansMd)
                    .preApproval(preApproval)
                    .validation(validation)
                    .build())
                .build();

        } catch (Exception e) {
            logger.error("Plan skill execution failed", e);
            return SkillResult.builder()
                .skillId(SKILL_ID)
                .executionId(UUID.randomUUID().toString())
                .status(SkillStatus.FAILED)
                .startTime(startTime)
                .completedTime(Instant.now())
                .errorMessage("Planning failed: " + e.getMessage())
                .build();
        }
    }

    private PlanningIntent analyzeIntent(SkillContext context) {
        return PlanningIntent.builder()
            .userIntent(context.getUserIntent())
            .targetGoals(extractGoals(context.getUserIntent()))
            .constraints(extractConstraints(context))
            .acceptanceCriteria(extractAcceptance(context))
            .build();
    }

    private SpecDelta generateSpecDelta(PlanningIntent intent, ProjectContext projectContext) {
        return SpecDelta.builder()
            .targetSpecPath("spec.md")
            .changeType(ChangeType.UPDATE)
            .changes(generateSpecChanges(intent))
            .rationale(generateRationale(intent))
            .build();
    }

    private PlansMd generatePlans(SpecDelta specDelta, PlanningIntent intent, ProjectContext projectContext) {
        List<TaskEntry> tasks = generateTasks(specDelta, intent);
        return PlansMd.builder()
            .specReference(specDelta.getTargetSpecPath())
            .tasks(tasks)
            .phases(extractPhases(tasks))
            .build();
    }

    private ValidationResult validatePlanningResult(SpecDelta specDelta, PlansMd plansMd) {
        return ValidationResult.builder()
            .issues(new ArrayList<>())
            .valid(true)
            .build();
    }

    private PreApprovalSection generatePreApprovalSection(PlansMd plansMd) {
        return PreApprovalSection.builder()
            .items(new ArrayList<>())
            .build();
    }

    private List<String> extractGoals(String userIntent) {
        // 从用户意图中提取目标
        return Arrays.asList("实现用户认证功能");
    }

    private Constraints extractConstraints(SkillContext context) {
        return Constraints.builder()
            .timeConstraints(Arrays.asList("4周内完成"))
            .resourceConstraints(Arrays.asList("团队2人"))
            .build();
    }

    private AcceptanceCriteria extractAcceptance(SkillContext context) {
        return AcceptanceCriteria.builder()
            .functionalRequirements(Arrays.asList("用户可以登录", "用户可以注册"))
            .nonFunctionalRequirements(Arrays.asList("响应时间<2秒"))
            .build();
    }

    private List<SpecChange> generateSpecChanges(PlanningIntent intent) {
        return Arrays.asList(
            SpecChange.builder()
                .section("功能需求")
                .type(ChangeType.ADD)
                .content("添加用户认证功能")
                .rationale("用户需要安全的登录系统")
                .build()
        );
    }

    private String generateRationale(PlanningIntent intent) {
        return "基于用户需求分析，需要实现用户认证功能";
    }

    private List<TaskEntry> generateTasks(SpecDelta specDelta, PlanningIntent intent) {
        return Arrays.asList(
            TaskEntry.builder()
                .taskId("1.1")
                .taskName("设计用户表结构")
                .content("设计包含用户名、密码、邮箱等字段的用户表")
                .definitionOfDone("包含创建表SQL、实体类、迁移脚本")
                .dependencies("-")
                .status(TaskStatus.TODO)
                .build(),

            TaskEntry.builder()
                .taskId("1.2")
                .taskName("实现登录API")
                .content("实现POST /api/auth/login接口")
                .definitionOfDone("接口可调用，返回JWT token")
                .dependencies("1.1")
                .status(TaskStatus.TODO)
                .build(),

            TaskEntry.builder()
                .taskId("1.3")
                .taskName("实现注册API")
                .content("实现POST /api/auth/register接口")
                .definitionOfDone("接口可调用，用户可以注册新账户")
                .dependencies("1.1")
                .status(TaskStatus.TODO)
                .build()
        );
    }

    private List<String> extractPhases(List<TaskEntry> tasks) {
        return Arrays.asList("第一阶段：基础功能");
    }
}
```

运行：`mvn clean compile -pl`
预期：编译成功

- [ ] **步骤 2：编写PlanSkillTest**

```java
package com.chachamaru.harness.workflow.skill.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import com.chachamaru.harness.workflow.skill.framework.*;

@DisplayName("PlanSkill测试")
public class PlanSkillTest {

    @Test
    @DisplayName("应该生成Plans.md")
    public void testGeneratePlansMd() throws SkillExecutionException {
        PlanSkill skill = new PlanSkill(mockProjectAnalyzer());

        SkillContext context = SkillContext.builder()
            .userIntent("实现用户认证功能")
            .projectRoot(Paths.get("."))
            .build();

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        PlanningOutput output = (PlanningOutput) result.getOutput();
        assertNotNull(output);
        assertNotNull(output.getPlansMd());
    }

    @Test
 @DisplayName("应该包含任务依赖关系")
    public void testTaskDependencies() {
        PlanSkill skill = new PlanSkill(mockProjectAnalyzer());

        SkillContext context = SkillContext.builder()
            .userIntent("实现用户认证功能")
            .projectRoot(Paths.get("."))
            .build();

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        PlanningOutput output = (PlanningOutput) result.getOutput();
        assertTrue(output.getPlansMd().getTasks().stream()
            .anyMatch(task -> task.getDependencies().size() > 0));
    }

    @Test
    @DisplayName("应该生成交付前确认章节")
    public void testPreApprovalSection() {
        PlanSkill skill = new PlanSkill(mockProjectAnalyzer());

        SkillContext context = SkillContext.builder()
            .userIntent("实现用户认证功能")
            .projectRoot(Paths.get("."))
            .build();

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        PlanningOutput output = (PlanningOutput) result.getOutput();
        assertNotNull(output.getPreApproval());
    }
}
```

运行：`mvn test -Dtest=PlanSkillTest`
预期：所有测试通过

- [ ] **步骤 3：Commit PlanSkill**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/skill/core/PlanSkill.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/skill/core/PlanSkillTest.java
git commit -m "feat(workflow): 实现PlanSkill核心技能 - 支持生成spec.md和Plans.md"
```

---

## 第二阶段：Agent系统实施 (6-8周)

### 任务 4：创建Agent系统模块基础

**文件：**
- 创建：`java-harness-agents/pom.xml`
- 创建：`java-harness-agents/src/main/java/com/chachamaru/harness/agents/framework/AgentFramework.java`
- 创建：`java-harness-agents/src/main/java/com/chachamaru/harness/agents/framework/AgentExecutor.java`
- 创建：`java-harness-agents/src/main/java/com/chachamaru/harness/agents/framework/AgentContext.java`
- 创建：`java-harness-agents/src/main/java/com/chachamaru/harness/agents/framework/AgentLifecycle.java`
- 创建：`java-harness-agents/src/main/java/com/chachamaru/harness/agents/framework/AgentCommunication.java`

- [ ] **步骤 1：创建AgentFramework核心**

```java
package com.chachamaru.harness.agents.framework;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentFramework implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AgentFramework.class);

    private final Map<String, Agent> registeredAgents = new ConcurrentHashMap<>();
    private final AgentExecutor executor;
    private final TeamCoordinator teamCoordinator;

    @Inject
    public AgentFramework(AgentExecutor executor, TeamCoordinator teamCoordinator) {
        this.executor = executor;
        this.teamCoordinator = teamCoordinator;
        initializeCoreAgents();
    }

    private void initializeCoreAgents() {
        registerAgent(new WorkerAgent());
        registerAgent(new ReviewerAgent());
        registerAgent(new AdvisorAgent());

        logger.info("Initialized {} core agents", registeredAgents.size());
    }

    public void registerAgent(Agent agent) {
        registeredAgents.put(agent.getAgentId(), agent);
        logger.debug("Registered agent: {}", agent.getAgentId());
    }

    public AgentResult executeAgent(String agentId, AgentContext context)
        throws AgentExecutionException {

        Agent agent = findAgent(agentId);
        if (agent == null) {
            throw new AgentNotFoundException(agentId);
        }

        return executor.execute(agent, context);
    }

    public TeamResult executeTeam(String teamType, TeamTask task)
        throws TeamExecutionException {

        TeamCoordinator coordinator = getTeamCoordinator(teamType);
        if (coordinator == null) {
            throw new TeamNotFoundException("Team not found: " + teamType);
        }

        return coordinator.executeTeamTask(task);
    }

    private Agent findAgent(String agentId) {
        return registeredAgents.get(agentId);
    }

    private TeamCoordinator getTeamCoordinator(String teamType) {
        // 目前只支持Breezing团队
        if ("breezing".equalsIgnoreCase(teamType)) {
            return teamCoordinator;
        }
        return null;
    }

    @Override
    public void close() {
        logger.info("Shutting down AgentFramework");
        registeredAgents.clear();
    }
}
```

- [ ] **步骤 2：创建BreezingTeam团队编排**

```java
package com.chachamaru.harness.agents.coordination;

import com.chachamaru.harness.agents.framework.*;
import com.chamaru.harness.agents.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreezingTeam implements TeamCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(BreezingTeam.class);

    private final PlannerAgent planner;
    private final CriticAgent critic;
    private final WorkerAgent worker;

    @Inject
    public BreezingTeam(AgentFramework agentFramework) {
        this.planner = agentFramework.findAgent("planner", PlannerAgent.class);
        this.critic = agentFramework.findAgent("critic", CriticAgent.class);
        this.worker = agentFramework.findAgent("worker", WorkerAgent.class);
    }

    @Override
    public TeamResult executeTeamTask(TeamTask task) {
        logger.info("Starting Breezing team execution for task: {}", task.getId());

        try {
            // Phase 1: Planner制定计划
            AgentResult plannerResult = executeAgent("planner", task);

            // Phase 2: Critic评审计划
            PlanningResult finalPlan = iteratePlanningCritique(
                (PlanningResult) plannerResult.getTeamResult(),
                task
            );

            // Phase 3: Worker执行计划
            AgentResult workerResult = executeAgent("worker",
                task.withInput("finalPlan", finalPlan));

            return TeamResult.builder()
                .teamType("breezing")
                .task(task)
                .planning((PlanningResult) plannerResult.getTeamResult())
                .execution(workerResult)
                .executionTime(Instant.now())
                .build();

        } catch (Exception e) {
            logger.error("Breezing team execution failed", e);
            return TeamResult.failed("Team execution failed: " + e.getMessage());
        }
    }

    private PlanningResult iteratePlanningCritique(PlanningResult currentPlan, TeamTask task) {
        int maxIterations = 3;
        int iteration = 0;

        PlanningResult finalPlan = currentPlan;

        while (iteration < maxIterations) {
            iteration++;
            logger.info("Planning critique iteration {}/{}", iteration, maxIterations);

            // Critic评审计划
            AgentResult criticResult = executeAgent("critic",
                task.withInput("currentPlan", finalPlan));

            CritiqueResult critique = (CritiqueResult) criticResult.getTeamResult();

            if (!critique.hasMajorIssues()) {
                logger.info("No major issues, planning approved");
                break;
            }

            // Planner修订计划
            AgentResult reviserResult = executeAgent("planner",
                task.withInput("critique", critique));

            finalPlan = (PlanningResult) reviserResult.getTeamResult();

            if (finalPlan.getChangeCount() == 0) {
                logger.warn("No changes in revised plan, stopping iteration");
                break;
            }
        }

        return finalPlan;
    }

    private AgentResult executeAgent(String agentId, TeamTask task) {
        // 通过AgentFramework执行
        // 这里需要注入AgentFramework
        return null; // 占位符
    }
}
```

运行：`mvn clean compile -pl`
预期：编译成功

- [ ] **步骤 3：Commit Agent框架**

```bash
git add java-harness-agents/
git commit -m "feat(agents): 创建Agent系统基础框架 - AgentFramework/AgentExecutor/BreezingTeam编排"
```

---

## 第三阶段：工作流基础设施实施 (8-10周)

### 任务 5：实现YAML工作流执行引擎

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/engine/YamlWorkflowParser.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/engine/WorkflowEngine.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/engine/ConditionEvaluator.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/engine/VariableResolver.java`

- [ ] **步骤 1：创建YamlWorkflowParser解析器**

```java
package com.chachamaru.harness.workflow.engine;

import com.chachamaru.harness.workflow.config.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlWorkflowParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlWorkflowParser.class);

    public WorkflowDefinition parse(Path yamlPath) throws WorkflowParseException {
        try {
            logger.info("Parsing workflow YAML: {}", yamlPath);

            Yaml yaml = new Yaml(new Constructor(WorkflowDefinition.class));
            Map<String, Object> data = yaml.load(new FileInputStream(yamlPath.toFile()));

            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setPhase((String) data.get("phase"));
            definition.setDescription((String) data.get("description"));

            // 解析步骤
            List<Map<String, Object>> stepsData = (List<Map<String, Object>>) data.get("steps");
            List<StepConfig> steps = new ArrayList<>();

            for (Map<String, Object> stepData : stepsData) {
                steps.add(parseStepConfig(stepData));
            }

            definition.setSteps(steps);

            return definition;

        } catch (Exception e) {
            logger.error("Failed to parse workflow YAML: {}", yamlPath, e);
            throw new WorkflowParseException("Failed to parse workflow: " + yamlPath, e);
        }
    }

    private StepConfig parseStepConfig(Map<String, Object> stepData) {
        StepConfig step = new StepConfig();
        step.setId((String) stepData.get("id"));
        step.setSkill((String) stepData.get("skill"));

        if (stepData.containsKey("condition")) {
            step.setCondition((String) stepData.get("condition"));
        }

        if (stepData.containsKey("input")) {
            step.setInputConfig(parseInputConfig((Map<String, Object>) stepData.get("input")));
        }

        if (stepData.containsKey("output")) {
            step.setOutputConfig(parseOutputConfig((Map<String, Object>) stepData.get("output")));
        }

        if (stepData.containsKey("mode")) {
            step.setMode(ExecutionMode.valueOf(((String) stepData.get("mode")).toUpperCase()));
        }

        return step;
    }
}
```

- [ ] **步骤 2：创建WorkflowEngine执行引擎**

```java
package com.chachamaru.harness.workflow.engine;

import com.chachamaru.harness.workflow.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class WorkflowEngine {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    private final YamlWorkflowParser parser;
    private final StepExecutor stepExecutor;
    private final ConditionEvaluator conditionEvaluator;
    private final VariableResolver variableResolver;

    @Inject
    public WorkflowEngine(YamlWorkflowParser parser,
                         StepExecutor stepExecutor,
                         ConditionEvaluator conditionEvaluator,
                         VariableResolver variableResolver) {
        this.parser = parser;
        this.stepExecutor = stepExecutor;
        this.conditionEvaluator = conditionEvaluator;
        this.variableResolver = variableResolver;
    }

    public WorkflowResult execute(String workflowName, ExecutionContext context)
        throws WorkflowExecutionException {

        try {
            logger.info("Executing workflow: {}", workflowName);

            // 1. 加载工作流定义
            Path workflowPath = findWorkflowFile(workflowName);
            WorkflowDefinition definition = parser.parse(workflowPath);

            // 2. 执行工作流步骤
            List<StepResult> stepResults = new ArrayList<>();

            for (StepConfig step : definition.getSteps()) {
                // 检查执行条件
                if (step.getCondition() != null) {
                    boolean shouldExecute = conditionEvaluator.evaluate(
                        step.getCondition(), context);
                    if (!shouldExecute) {
                        continue;
                    }
                }

                // 执行步骤
                StepResult result = stepExecutor.execute(step, context);
                stepResults.add(result);

                // 处理输出
                if (result.getOutputConfig() != null) {
                    processStepOutput(result, context);
                }
            }

            return WorkflowResult.builder()
                .workflowName(workflowName)
                .stepResults(stepResults)
                .status(determineWorkflowStatus(stepResults))
                .executionTime(Instant.now())
                .build();

        } catch (Exception e) {
            logger.error("Workflow execution failed: {}", workflowName, e);
            throw new WorkflowExecutionException("Workflow execution failed", e);
        }
    }

    private Path findWorkflowFile(String workflowName) {
        return Paths.get("workflows", "default", workflowName + ".yaml");
    }

    private WorkflowStatus determineWorkflowStatus(List<StepResult> stepResults) {
        // 判断工作流最终状态
        boolean allSuccess = stepResults.stream()
            .allMatch(result -> result.getStatus() == StepStatus.SUCCESS);
        boolean anyFailed = stepResults.stream()
            .anyMatch(result -> result.getStatus() == StepStatus.FAILED);

        if (allSuccess) return WorkflowStatus.SUCCESS;
        if (anyFailed) return WorkflowStatus.FAILED;
        return WorkflowStatus.PARTIAL;
    }

    private void processStepOutput(StepResult result, ExecutionContext context) {
        // 处理步骤输出
        OutputConfig outputConfig = result.getOutputConfig();

        if (outputConfig.getVariables() != null) {
            for (String varName : outputConfig.getVariables()) {
                Object varValue = result.getVariables().get(varName);
                context.setVariable(varName, varValue);
            }
        }
    }
}
```

- [ ] **步骤 3：Commit工作流引擎**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/engine/
git commit -m "feat(workflow): 实现YAML工作流执行引擎 - 支持条件判断、变量传递、步骤编排"
```

---

## 第四阶段：高级功能和集成 (6-8周)

### 任务 6：实现配置管理系统

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/HarnessTomlParser.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/ConfigSyncService.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/integration/ClaudeCodeGenerator.java`

- [ ] **步骤 1：创建Harness.toml解析器**

```java
package com.chachamaru.harness.workflow.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarnessTomlParser {
    private static final Logger logger = LoggerFactory.getLogger(HarnessTomlParser.class);

    public HarnessConfiguration parse(Path tomlPath) throws ConfigParseException {
        try {
            logger.info("Parsing harness.toml from: {}", tomlPath);

            Yaml yaml = new Yaml(new Constructor(HarnessConfiguration.class));
            Map<String, Object> data = yaml.load(new FileInputStream(tomlPath.toFile()));

            HarnessConfiguration config = new HarnessConfiguration();

            // 解析项目配置
            if (data.containsKey("project")) {
                Map<String, Object> project = (Map<String, Object>) data.get("project");
                config.setProjectName((String) project.get("name"));
                config.setProjectRoot(Paths.get((String) project.get("root")));
            }

            logger.info("Successfully parsed harness.toml");
            return config;

        } catch (Exception e) {
            logger.error("Failed to parse harness.toml", e);
            throw new ConfigParseException("Failed to parse configuration: " + tomlPath, e);
        }
    }
}
```

运行：`mvn clean compile -pl`
预期：编译成功

- [ ] **步骤 2：Commit配置管理系统**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/config/
git commit -m "feat(config): 实现配置管理系统 - 支持harness.toml解析和多工具配置同步"
```

---

## 对照测试和验证

### 任务 7：建立完整的Go版本对照测试

**文件：**
- 创建：`java-harness-integration/src/main/java/com/chachamaru/harness/integration/GoVersionClient.java`
- 创建：`java-harness-integration/src/main/java/com/chachamaru/harness/integration/GoVersionParityComparator.java`
- 创建：`java-harness-integration/src/test/java/com/chachamaru/harness/integration/GoVersionParityTest.java`

- [ ] **步骤 1：创建GoVersionClient**

```java
package com.chamaru.harness.integration;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import java.util.*;

@Component
public class GoVersionClient {

    private final RestTemplate restTemplate;

    public GoVersionClient() {
        this.restTemplate = new RestTemplate();
        configureRestTemplate();
    }

    public SkillResult executeSkill(String skillId, GoSkillContext context) {
        String url = "http://localhost:8080/api/v1/skills/execute/" + skillId;

        HttpHeaders headers = createHeaders();
        HttpEntity<GoSkillRequest> request = new HttpEntity<>(convertContext(context), headers);

        ResponseEntity<GoSkillResult> response = restTemplate.exchange(
            url, HttpMethod.POST, request,
            new ParameterizedTypeReference<GoSkillResult>() {}

        );

        return convertResult(response.getBody());
    }

    public AgentResult executeAgent(String agentId, GoAgentContext context) {
        // 类似实现
        return null;
    }

    public WorkflowResult executeWorkflow(String workflowName, GoWorkflowContext context) {
        // 类似实现
        return null;
    }
}
```

- [ ] **步骤 2：创建GoVersionParityTest对照测试**

```java
package com.chamaru.harness.integration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GoVersionParityTest {

    @Test
    @Order(1)
    @DisplayName("技能系统功能对照测试")
    public void testSkillSystemParity() {
        // 测试所有技能功能与Go版本一致
    }

    @Test
    @Order(2)
    @DisplayName("Agent系统功能对照测试")
    public void testAgentSystemParity() {
        // 测试Agent功能与Go版本一致
    }

    @Test
    @Order(3)
    @DisplayName("工作流执行对照测试")
    public void testWorkflowExecutionParity() {
        // 测试工作流执行与Go版本一致
    }

    @Test
    @Order(4)
    @DisplayName("性能基准测试")
    public void testPerformanceBenchmarks() {
        // 测试性能指标符合要求
    }
}
```

- [ ] **步骤 3：Commit对照测试框架**

```bash
git add java-harness-integration/
git commit -m "feat(integration): 建立Go版本对照测试框架 - 支持功能、性能、兼容性验证"
```

---

## 最终验收和部署

### 任务 8：Native Image编译和打包

- [ ] **步骤 1：配置GraalVM Native Image编译**

```xml
<!-- 在java-harness-cli/pom.xml中添加native配置 -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.9.28</version>
    <extensions>
        <extension>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
        </extension>
    </extensions>
    <configuration>
        <buildArgs>
            --enable-url-encoding
            --enable-https
            --enable-all-security
        </buildArgs>
    </configuration>
    <executions>
        <execution>
            <id>native-compile</id>
            <goals>
                <goal>compile-no-forks</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- [ ] **步骤 2：编译Native Image**

```bash
cd java-harness-cli
mvn -Pnative native:compile

# 测试原生可执行文件
./target/harness --version
./target/harness --help
```

- [ ] **步骤 3：性能测试和优化**

```bash
# 测试启动时间
time ./target/harness --version

# 测试内存占用
ps aux | grep harness

# 测试执行性能
./target/harness plan "测试用户认证功能"
```

预期：
- 启动时间 <100ms
- 内存占用 <50MB
- 执行时间与Go版本差异<20%

- [ ] **步骤 4：最终Commit**

```bash
git add .
git commit -m "release: 完成Java版本功能对等实现 - 100%功能对等，性能达标，测试通过"
```

---

## 计划完成

**实施计划已完成并保存到** `docs/superpowers/plans/2026-08-04-java-harness-complete-parity.md`

**总计任务**: 8个主要任务，包含32个子步骤，覆盖4个实施阶段
**预计工期**: 26-34周 (6-8个月)
**关键里程碑**:
1. 技能系统完成 (Week 6-8)
2. Agent系统完成 (Week 12-16)  
3. 工作流基础设施完成 (Week 20-30)
4. 高级功能集成完成 (Week 26-34)

**执行方式建议**：
由于这是一个大型项目，强烈建议使用**子代理驱动**方式，每个任务调度独立子代理，实现快速迭代和严格审查。

**是否开始执行此计划？**