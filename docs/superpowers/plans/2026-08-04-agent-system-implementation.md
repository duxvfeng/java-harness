# Agent 系统实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 Java Harness 的 Agent 系统（阶段1：最小实现），作为智能决策层，Agent 能够调用 Skill 完成任务、自主决策工作策略、追踪执行过程。

**架构：** 三层架构 - Workflow（编排）→ Agent（决策）→ Skill（执行）。Agent 通过 AgentContext 调用 SkillFramework 执行 Skill，通过共享状态与其他 Agent 协作。

**技术栈：** Java 17, Maven, JUnit 5, Mockito, SLF4J

---

## 文件结构

将要创建的文件及其职责：

### 框架基础
- `AgentType.java` - Agent 类型枚举（WORKER、REVIEWER、ADVISOR）
- `AgentConfig.java` - Agent 配置类（阶段1简单配置）
- `AgentMessage.java` - Agent 消息对象（阶段1：简单消息传递）
- `AgentExecutionException.java` - Agent 执行异常基类
- `AgentNotFoundException.java` - Agent 未找到异常
- `AgentValidationException.java` - Agent 验证异常
- `AgentLifecycleException.java` - Agent 生命周期异常

### 结果模型
- `AgentResult.java` - Agent 执行结果（包含基础信息、输出、Skill调用追踪）
- `AgentStatus.java` - Agent 状态枚举（PENDING、SUCCESS、FAILED等）
- `SkillCallTrace.java` - Skill 调用追踪记录

### 上下文
- `AgentContext.java` - Agent 执行上下文（扩展 SkillContext）

### 核心接口
- `Agent.java` - Agent 接口（所有Agent必须实现）
- `AgentLifecycle.java` - Agent 生命周期接口

### 框架核心
- `AgentRegistry.java` - Agent 注册表
- `AgentExecutor.java` - Agent 执行器
- `AgentFramework.java` - Agent 框架核心

### 核心实现
- `WorkerAgent.java` - 工作代理（执行具体任务）
- `ReviewerAgent.java` - 审查代理（审查工作成果）
- `AdvisorAgent.java` - 顾问代理（提供建议）

### 团队协作
- `BreezingTeam.java` - Breezing 团队编排
- `TeamTask.java` - 团队任务
- `TeamResult.java` - 团队结果
- `TeamStatus.java` - 团队状态
- `AgentExecution.java` - Agent 执行记录

### 测试文件
- `AgentTypeTest.java`
- `AgentResultTest.java`
- `AgentContextTest.java`
- `AgentRegistryTest.java`
- `AgentExecutorTest.java`
- `AgentFrameworkTest.java`
- `WorkerAgentTest.java`
- `ReviewerAgentTest.java`
- `AdvisorAgentTest.java`
- `BreezingTeamTest.java`

---

## 第一部分：框架基础（类型、配置、消息、异常）

### 任务 1：实现 AgentType 枚举

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentType.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentTypeTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentType 测试")
public class AgentTypeTest {

    @Test
    @DisplayName("应该有 WORKER 类型")
    public void testWorkerType() {
        assertEquals("WORKER", AgentType.WORKER.name());
        assertEquals("工作代理", AgentType.WORKER.getDisplayName());
        assertEquals("执行具体任务的代理", AgentType.WORKER.getDescription());
    }

    @Test
    @DisplayName("应该有 REVIEWER 类型")
    public void testReviewerType() {
        assertEquals("REVIEWER", AgentType.REVIEWER.name());
        assertEquals("审查代理", AgentType.REVIEWER.getDisplayName());
    }

    @Test
    @DisplayName("应该有 ADVISOR 类型")
    public void testAdvisorType() {
        assertEquals("ADVISOR", AgentType.ADVISOR.name());
        assertEquals("顾问代理", AgentType.ADVISOR.getDisplayName());
    }

    @Test
    @DisplayName("应该有 PLANNER 和 CRITIC 类型（为阶段2+预留）")
    public void testFutureTypes() {
        assertEquals("PLANNER", AgentType.PLANNER.name());
        assertEquals("CRITIC", AgentType.CRITIC.name());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentTypeTest -pl java-harness-workflow`
预期：FAIL，报错 "cannot find symbol: class AgentType"

- [ ] **步骤 3：实现 AgentType 枚举**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 类型枚举
 */
public enum AgentType {
    WORKER("工作代理", "执行具体任务的代理"),
    REVIEWER("审查代理", "审查和评审工作的代理"),
    ADVISOR("顾问代理", "提供建议和指导的代理"),
    PLANNER("规划代理", "制定计划的代理"),
    CRITIC("批评代理", "评审和提出改进的代理");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentTypeTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
cd D:\project\java-harness\.claude\worktrees\java-harness-complete-parity
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentType.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentTypeTest.java
git commit -m "feat(agent): 实现 AgentType 枚举 - 定义5种Agent类型（WORKER/REVIEWER/ADVISOR/PLANNER/CRITIC）"
```

---

### 任务 2：实现 AgentConfig 配置类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentConfig.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentConfigTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentConfig 测试")
public class AgentConfigTest {

    @Test
    @DisplayName("应该创建默认配置")
    public void testDefaultConfig() {
        AgentConfig config = AgentConfig.defaultConfig();
        assertNotNull(config);
        assertFalse(config.isParallelExecutionEnabled());
        assertEquals(300, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("应该创建自定义配置")
    public void testCustomConfig() {
        AgentConfig config = AgentConfig.builder()
                .parallelExecutionEnabled(true)
                .timeoutSeconds(600)
                .build();

        assertTrue(config.isParallelExecutionEnabled());
        assertEquals(600, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("应该支持 Builder 模式")
    public void testBuilderPattern() {
        AgentConfig config = AgentConfig.builder()
                .parallelExecutionEnabled(false)
                .timeoutSeconds(120)
                .maxRetries(3)
                .build();

        assertEquals(120, config.getTimeoutSeconds());
        assertEquals(3, config.getMaxRetries());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentConfigTest -pl java-harness-workflow`
预期：FAIL，报错 "cannot find symbol: class AgentConfig"

- [ ] **步骤 3：实现 AgentConfig 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 配置类
 * 阶段1：简单配置
 */
public class AgentConfig {
    private final boolean parallelExecutionEnabled;
    private final int timeoutSeconds;
    private final int maxRetries;

    private AgentConfig(Builder builder) {
        this.parallelExecutionEnabled = builder.parallelExecutionEnabled;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.maxRetries = builder.maxRetries;
    }

    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public static AgentConfig defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean parallelExecutionEnabled = false;
        private int timeoutSeconds = 300;
        private int maxRetries = 0;

        public Builder parallelExecutionEnabled(boolean enabled) {
            this.parallelExecutionEnabled = enabled;
            return this;
        }

        public Builder timeoutSeconds(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentConfigTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentConfig.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentConfigTest.java
git commit -m "feat(agent): 实现 AgentConfig 配置类 - 支持超时、重试、并行执行配置"
```

---

### 任务 3：实现 AgentMessage 消息类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentMessage.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentMessageTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentMessage 测试")
public class AgentMessageTest {

    @Test
    @DisplayName("应该创建 REQUEST 类型消息")
    public void testRequestMessage() {
        AgentMessage message = AgentMessage.builder()
                .from("worker")
                .to("reviewer")
                .type(AgentMessage.MessageType.REQUEST)
                .payload("请审查代码")
                .build();

        assertEquals("worker", message.getFromAgentId());
        assertEquals("reviewer", message.getToAgentId());
        assertEquals(AgentMessage.MessageType.REQUEST, message.getType());
        assertEquals("请审查代码", message.getPayload());
        assertNotNull(message.getMessageId());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("应该创建 RESPONSE 类型消息")
    public void testResponseMessage() {
        AgentMessage message = AgentMessage.builder()
                .from("reviewer")
                .to("worker")
                .type(AgentMessage.MessageType.RESPONSE)
                .payload("审查完成，有2个问题")
                .build();

        assertEquals(AgentMessage.MessageType.RESPONSE, message.getType());
    }

    @Test
    @DisplayName("应该支持所有消息类型")
    public void testAllMessageTypes() {
        assertEquals(5, AgentMessage.MessageType.values().length);
        assertEquals("REQUEST", AgentMessage.MessageType.REQUEST.name());
        assertEquals("RESPONSE", AgentMessage.MessageType.RESPONSE.name());
        assertEquals("NOTIFICATION", AgentMessage.MessageType.NOTIFICATION.name());
        assertEquals("FEEDBACK", AgentMessage.MessageType.FEEDBACK.name());
        assertEquals("STATE_UPDATE", AgentMessage.MessageType.STATE_UPDATE.name());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentMessageTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 AgentMessage 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

import java.time.Instant;
import java.util.UUID;

/**
 * Agent 消息对象
 * 阶段1：简单消息传递
 */
public class AgentMessage {
    private final String messageId;
    private final String fromAgentId;
    private final String toAgentId;
    private final MessageType type;
    private final Object payload;
    private final Instant timestamp;

    private AgentMessage(Builder builder) {
        this.messageId = builder.messageId;
        this.fromAgentId = builder.fromAgentId;
        this.toAgentId = builder.toAgentId;
        this.type = builder.type;
        this.payload = builder.payload;
        this.timestamp = builder.timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getFromAgentId() { return fromAgentId; }
    public String getToAgentId() { return toAgentId; }
    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
    public Instant getTimestamp() { return timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String messageId = UUID.randomUUID().toString();
        private String fromAgentId;
        private String toAgentId;
        private MessageType type = MessageType.REQUEST;
        private Object payload;
        private Instant timestamp = Instant.now();

        public Builder from(String fromAgentId) {
            this.fromAgentId = fromAgentId;
            return this;
        }

        public Builder to(String toAgentId) {
            this.toAgentId = toAgentId;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public AgentMessage build() {
            if (fromAgentId == null || toAgentId == null) {
                throw new IllegalArgumentException("fromAgentId and toAgentId are required");
            }
            return new AgentMessage(this);
        }
    }

    public enum MessageType {
        REQUEST,
        RESPONSE,
        NOTIFICATION,
        FEEDBACK,
        STATE_UPDATE
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentMessageTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentMessage.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentMessageTest.java
git commit -m "feat(agent): 实现 AgentMessage 消息类 - 支持5种消息类型（REQUEST/RESPONSE/NOTIFICATION/FEEDBACK/STATE_UPDATE）"
```

---

### 任务 4：实现异常层次

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentExecutionException.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentNotFoundException.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentValidationException.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentLifecycleException.java`

- [ ] **步骤 1：实现 AgentExecutionException 基类**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 执行异常基类
 */
public class AgentExecutionException extends Exception {
    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 2：实现 AgentNotFoundException**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 未找到异常
 */
public class AgentNotFoundException extends AgentExecutionException {
    private final String agentId;

    public AgentNotFoundException(String agentId) {
        super("Agent not found: " + agentId);
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}
```

- [ ] **步骤 3：实现 AgentValidationException**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 验证异常
 */
public class AgentValidationException extends AgentExecutionException {
    public AgentValidationException(String message) {
        super(message);
    }

    public AgentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 4：实现 AgentLifecycleException**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 生命周期异常
 */
public class AgentLifecycleException extends AgentExecutionException {
    public AgentLifecycleException(String message) {
        super(message);
    }

    public AgentLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 5：编译验证**

运行：`mvn compile -pl java-harness-workflow`
预期：SUCCESS

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentExecutionException.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentNotFoundException.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentValidationException.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentLifecycleException.java
git commit -m "feat(agent): 实现异常层次 - AgentExecutionException/AgentNotFoundException/AgentValidationException/AgentLifecycleException"
```

---

## 第二部分：结果模型

### 任务 5：实现 AgentStatus 枚举

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentStatus.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentStatusTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentStatus 测试")
public class AgentStatusTest {

    @Test
    @DisplayName("应该有所有必需的状态")
    public void testAllStatuses() {
        assertEquals(7, AgentStatus.values().length);
        assertEquals("PENDING", AgentStatus.PENDING.name());
        assertEquals("RUNNING", AgentStatus.RUNNING.name());
        assertEquals("SUCCESS", AgentStatus.SUCCESS.name());
        assertEquals("FAILED", AgentStatus.FAILED.name());
        assertEquals("SUCCESS_WITH_WARNINGS", AgentStatus.SUCCESS_WITH_WARNINGS.name());
        assertEquals("PARTIAL_SUCCESS", AgentStatus.PARTIAL_SUCCESS.name());
        assertEquals("CANCELLED", AgentStatus.CANCELLED.name());
    }

    @Test
    @DisplayName("应该判断成功状态")
    public void testIsSuccess() {
        assertTrue(AgentStatus.SUCCESS.isSuccess());
        assertFalse(AgentStatus.FAILED.isSuccess());
        assertFalse(AgentStatus.PARTIAL_SUCCESS.isSuccess());
    }

    @Test
    @DisplayName("应该判断失败状态")
    public void testIsFailed() {
        assertTrue(AgentStatus.FAILED.isFailed());
        assertFalse(AgentStatus.SUCCESS.isFailed());
        assertFalse(AgentStatus.PARTIAL_SUCCESS.isFailed());
    }

    @Test
    @DisplayName("应该判断部分成功状态")
    public void testIsPartialSuccess() {
        assertTrue(AgentStatus.PARTIAL_SUCCESS.isPartialSuccess());
        assertTrue(AgentStatus.SUCCESS_WITH_WARNINGS.isPartialSuccess());
        assertFalse(AgentStatus.SUCCESS.isPartialSuccess());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentStatusTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 AgentStatus 枚举**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 状态枚举
 */
public enum AgentStatus {
    PENDING {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    RUNNING {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    SUCCESS {
        @Override
        public boolean isSuccess() { return true; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    FAILED {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return true; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    SUCCESS_WITH_WARNINGS {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return true; }
    },
    PARTIAL_SUCCESS {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return true; }
    },
    CANCELLED {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    };

    public abstract boolean isSuccess();
    public abstract boolean isFailed();
    public abstract boolean isPartialSuccess();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentStatusTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentStatus.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentStatusTest.java
git commit -m "feat(agent): 实现 AgentStatus 枚举 - 定义7种Agent状态（PENDING/RUNNING/SUCCESS/FAILED/SUCCESS_WITH_WARNINGS/PARTIAL_SUCCESS/CANCELLED）"
```

---

### 任务 6：实现 SkillCallTrace 类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/SkillCallTrace.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/SkillCallTraceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillCallTrace 测试")
public class SkillCallTraceTest {

    @Test
    @DisplayName("应该创建 Skill 调用追踪")
    public void testCreateSkillCallTrace() {
        SkillResult mockResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .output("工作完成")
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(mockResult)
                .callerDecision("需要执行工作")
                .callOrder(1)
                .build();

        assertEquals("work", trace.getSkillId());
        assertEquals(mockResult, trace.getResult());
        assertEquals("需要执行工作", trace.getCallerDecision());
        assertEquals(1, trace.getCallOrder());
        assertNotNull(trace.getCallId());
        assertNotNull(trace.getCallTime());
    }

    @Test
    @DisplayName("应该判断调用是否成功")
    public void testIsSuccessful() {
        SkillResult successResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .build();

        SkillCallTrace successTrace = SkillCallTrace.builder()
                .skillId("work")
                .result(successResult)
                .build();

        assertTrue(successTrace.isSuccessful());
    }

    @Test
    @DisplayName("应该获取调用时长")
    public void testGetDuration() {
        SkillResult mockResult = SkillResult.builder()
                .skillId("work")
                .startTime(Instant.now().minusSeconds(5))
                .completedTime(Instant.now())
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(mockResult)
                .build();

        assertTrue(trace.getDuration() >= 0);
        assertTrue(trace.getDuration() <= 6000); // 最多6秒
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=SkillCallTraceTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 SkillCallTrace 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import java.time.Instant;
import java.util.UUID;

/**
 * Skill 调用追踪
 */
public class SkillCallTrace {
    private final String callId;
    private final String skillId;
    private final SkillResult result;
    private final Instant callTime;
    private final String callerDecision;
    private final int callOrder;

    private SkillCallTrace(Builder builder) {
        this.callId = builder.callId;
        this.skillId = builder.skillId;
        this.result = builder.result;
        this.callTime = builder.callTime;
        this.callerDecision = builder.callerDecision;
        this.callOrder = builder.callOrder;
    }

    public String getCallId() { return callId; }
    public String getSkillId() { return skillId; }
    public SkillResult getResult() { return result; }
    public Instant getCallTime() { return callTime; }
    public String getCallerDecision() { return callerDecision; }
    public int getCallOrder() { return callOrder; }

    public boolean isSuccessful() {
        return result != null && result.isSuccess();
    }

    public long getDuration() {
        if (result == null) return 0;
        return result.getExecutionDurationMs();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String callId = UUID.randomUUID().toString();
        private String skillId;
        private SkillResult result;
        private Instant callTime = Instant.now();
        private String callerDecision;
        private int callOrder;

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder result(SkillResult result) {
            this.result = result;
            return this;
        }

        public Builder callerDecision(String callerDecision) {
            this.callerDecision = callerDecision;
            return this;
        }

        public Builder callOrder(int callOrder) {
            this.callOrder = callOrder;
            return this;
        }

        public SkillCallTrace build() {
            if (skillId == null) {
                throw new IllegalArgumentException("skillId is required");
            }
            return new SkillCallTrace(this);
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=SkillCallTraceTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/SkillCallTrace.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/SkillCallTraceTest.java
git commit -m "feat(agent): 实现 SkillCallTrace 类 - 追踪 Skill 调用记录（skillId/result/callerDecision/callOrder）"
```

---

### 任务 7：实现 AgentResult 类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentResult.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentResultTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentResult 测试")
public class AgentResultTest {

    @Test
    @DisplayName("应该创建成功的 AgentResult")
    public void testCreateSuccessResult() {
        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .output("任务完成")
                .build();

        assertEquals("worker", result.getAgentId());
        assertEquals(AgentStatus.SUCCESS, result.getStatus());
        assertEquals("任务完成", result.getOutput());
        assertTrue(result.isSuccess());
        assertFalse(result.isPartialSuccess());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("应该创建失败的 AgentResult")
    public void testCreateFailedResult() {
        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.FAILED)
                .errorMessage("执行失败")
                .build();

        assertEquals(AgentStatus.FAILED, result.getStatus());
        assertEquals("执行失败", result.getErrorMessage());
    }

    @Test
    @DisplayName("应该支持添加 Skill 调用追踪")
    public void testAddSkillCalls() {
        SkillResult skillResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(skillResult)
                .build();

        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .addSkillCall(trace)
                .build();

        assertEquals(1, result.getSkillCalls().size());
        assertEquals("work", result.getSkillCalls().get(0).getSkillId());
    }

    @Test
    @DisplayName("应该计算执行时长")
    public void testExecutionDuration() {
        Instant startTime = Instant.now().minusSeconds(5);
        Instant completedTime = Instant.now();

        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .startTime(startTime)
                .completedTime(completedTime)
                .build();

        assertTrue(result.getExecutionDurationMs() >= 0);
        assertTrue(result.getExecutionDurationMs() <= 6000);
    }

    @Test
    @DisplayName("应该支持便捷方法创建结果")
    public void testConvenienceMethods() {
        AgentResult success = AgentResult.builder()
                .agentId("worker")
                .success("任务完成")
                .build();

        assertTrue(success.isSuccess());

        AgentResult failed = AgentResult.builder()
                .agentId("worker")
                .failed("任务失败")
                .build();

        assertEquals(AgentStatus.FAILED, failed.getStatus());
        assertEquals("任务失败", failed.getErrorMessage());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentResultTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 AgentResult 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

import java.time.Instant;
import java.util.*;

/**
 * Agent 执行结果（阶段1：最小实现）
 */
public class AgentResult {
    private final String agentId;
    private final String executionId;
    private final AgentStatus status;
    private final Instant startTime;
    private final Instant completedTime;
    private final Object output;
    private final String errorMessage;
    private final List<SkillCallTrace> skillCalls;

    private AgentResult(Builder builder) {
        this.agentId = builder.agentId;
        this.executionId = builder.executionId;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.completedTime = builder.completedTime;
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
        this.skillCalls = Collections.unmodifiableList(builder.skillCalls);
    }

    public String getAgentId() { return agentId; }
    public String getExecutionId() { return executionId; }
    public AgentStatus getStatus() { return status; }
    public Instant getStartTime() { return startTime; }
    public Instant getCompletedTime() { return completedTime; }
    public Object getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }
    public List<SkillCallTrace> getSkillCalls() { return skillCalls; }

    public long getExecutionDurationMs() {
        return completedTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    public boolean isPartialSuccess() {
        return status.isPartialSuccess();
    }

    public boolean hasWarnings() {
        return status == AgentStatus.SUCCESS_WITH_WARNINGS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String executionId = UUID.randomUUID().toString();
        private AgentStatus status = AgentStatus.PENDING;
        private Instant startTime = Instant.now();
        private Instant completedTime;
        private Object output;
        private String errorMessage;
        private List<SkillCallTrace> skillCalls = new ArrayList<>();

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(AgentStatus status) {
            this.status = status;
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

        public Builder addSkillCall(SkillCallTrace skillCall) {
            this.skillCalls.add(skillCall);
            return this;
        }

        public Builder skillCalls(List<SkillCallTrace> skillCalls) {
            this.skillCalls = new ArrayList<>(skillCalls);
            return this;
        }

        public Builder completedTime(Instant completedTime) {
            this.completedTime = completedTime;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public AgentResult build() {
            if (completedTime == null) {
                completedTime = Instant.now();
            }
            return new AgentResult(this);
        }

        public Builder success(Object output) {
            return status(AgentStatus.SUCCESS)
                    .output(output)
                    .completedTime(Instant.now());
        }

        public Builder failed(String errorMessage) {
            return status(AgentStatus.FAILED)
                    .errorMessage(errorMessage)
                    .completedTime(Instant.now());
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentResultTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentResult.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentResultTest.java
git commit -m "feat(agent): 实现 AgentResult 类 - Agent执行结果（agentId/status/output/skillCalls/executionDuration）"
```

---

## 第三部分：上下文和核心接口

### 任务 8：实现 AgentContext 类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentContext.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentContextTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentContext 测试")
public class AgentContextTest {

    @Test
    @DisplayName("应该创建 AgentContext")
    public void testCreateAgentContext() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .userIntent("实现用户认证")
                .projectRoot(Paths.get("/project"))
                .skillFramework(mockSkillFramework)
                .build();

        assertEquals("task-001", context.getTaskId());
        assertEquals("实现用户认证", context.getUserIntent());
        assertEquals(mockSkillFramework, context.getSkillFramework());
    }

    @Test
    @DisplayName("应该支持共享状态")
    public void testSharedState() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        Map<String, Object> sharedState = new HashMap<>();
        sharedState.put("plan", "计划内容");

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .skillFramework(mockSkillFramework)
                .sharedState(sharedState)
                .build();

        assertEquals("计划内容", context.getSharedState("plan"));
    }

    @Test
    @DisplayName("应该继承 SkillContext 的字段")
    public void testInheritsFromSkillContext() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .skillFramework(mockSkillFramework)
                .build();

        assertEquals("测试", context.getUserIntent());
        assertEquals(Paths.get("/project"), context.getProjectRoot());
    }

    @Test
    @DisplayName("应该提供调用 Skill 的便捷方法")
    public void testCallSkillMethod() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .skillFramework(mockSkillFramework)
                .build();

        context.callSkill("plan");

        verify(mockSkillFramework).executeSkill(eq("plan"), any());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentContextTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 AgentContext 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import java.nio.file.Path;
import java.util.*;

/**
 * Agent 执行上下文
 * 扩展 SkillContext，添加 Agent 特有功能
 */
public class AgentContext extends SkillContext {
    private final SkillFramework skillFramework;
    private final Map<String, Object> sharedState;
    private final List<AgentMessage> inbox;
    private final AgentConfig config;
    private final String taskId;

    private AgentContext(Builder builder) {
        super(builder);
        this.skillFramework = builder.skillFramework;
        this.sharedState = Collections.unmodifiableMap(builder.sharedState);
        this.inbox = Collections.unmodifiableList(builder.inbox);
        this.config = builder.config;
        this.taskId = builder.taskId;
    }

    public SkillFramework getSkillFramework() {
        return skillFramework;
    }

    public Object getSharedState(String key) {
        return sharedState.get(key);
    }

    public Map<String, Object> getSharedState() {
        return sharedState;
    }

    public List<AgentMessage> getInbox() {
        return inbox;
    }

    public AgentConfig getConfig() {
        return config;
    }

    public String getTaskId() {
        return taskId;
    }

    /**
     * 调用 Skill（便捷方法）
     */
    public SkillResult callSkill(String skillId) {
        return callSkill(skillId, this);
    }

    /**
     * 调用 Skill（自定义上下文）
     */
    public SkillResult callSkill(String skillId, SkillContext skillContext) {
        try {
            return skillFramework.executeSkill(skillId, skillContext);
        } catch (com.chachamaru.harness.workflow.skill.framework.SkillExecutionException e) {
            return SkillResult.builder()
                    .skillId(skillId)
                    .status(SkillResult.SkillStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends SkillContext.Builder {
        private SkillFramework skillFramework;
        private Map<String, Object> sharedState = new HashMap<>();
        private List<AgentMessage> inbox = new ArrayList<>();
        private AgentConfig config = AgentConfig.defaultConfig();
        private String taskId;

        public Builder skillFramework(SkillFramework skillFramework) {
            this.skillFramework = skillFramework;
            return this;
        }

        public Builder addSharedState(String key, Object value) {
            this.sharedState.put(key, value);
            return this;
        }

        public Builder sharedState(Map<String, Object> sharedState) {
            this.sharedState = new HashMap<>(sharedState);
            return this;
        }

        public Builder addMessage(AgentMessage message) {
            this.inbox.add(message);
            return this;
        }

        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        @Override
        public AgentContext build() {
            if (skillFramework == null) {
                throw new IllegalStateException("skillFramework is required");
            }
            return new AgentContext(this);
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentContextTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentContext.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentContextTest.java
git commit -m "feat(agent): 实现 AgentContext 类 - Agent执行上下文（扩展SkillContext，添加skillFramework/sharedState/taskId）"
```

---

### 任务 9：实现 AgentLifecycle 接口

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentLifecycle.java`

- [ ] **步骤 1：实现 AgentLifecycle 接口**

```java
package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 生命周期接口
 */
public interface AgentLifecycle {
    
    /**
     * 初始化 Agent
     */
    void initialize();
    
    /**
     * 是否支持暂停
     */
    default boolean supportsPause() {
        return false;
    }
    
    /**
     * 是否支持恢复
     */
    default boolean supportsResume() {
        return false;
    }
    
    /**
     * 暂停执行
     */
    default void pause() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Pause not supported");
    }
    
    /**
     * 恢复执行
     */
    default void resume() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Resume not supported");
    }
    
    /**
     * 取消执行
     */
    default void cancel() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Cancel not supported");
    }
    
    /**
     * 清理资源
     */
    default void cleanup() {
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`mvn compile -pl java-harness-workflow`
预期：SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentLifecycle.java
git commit -m "feat(agent): 实现 AgentLifecycle 接口 - Agent生命周期管理（initialize/pause/resume/cancel/cleanup）"
```

---

### 任务 10：实现 Agent 接口

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/Agent.java`

- [ ] **步骤 1：实现 Agent 接口**

```java
package com.chachamaru.harness.workflow.agent.framework;

import java.util.List;

/**
 * Agent 接口
 * 所有 Agent 必须实现此接口
 */
public interface Agent extends AgentLifecycle {
    
    /**
     * 获取 Agent 唯一标识符
     */
    String getAgentId();
    
    /**
     * 获取 Agent 名称
     */
    String getAgentName();
    
    /**
     * 获取 Agent 版本
     */
    String getVersion();
    
    /**
     * 获取 Agent 描述
     */
    String getDescription();
    
    /**
     * 获取 Agent 类型
     */
    AgentType getAgentType();
    
    /**
     * 获取 Agent 所需的能力
     */
    List<String> getRequiredSkills();
    
    /**
     * 执行 Agent 任务（核心方法）
     */
    AgentResult execute(AgentContext context) throws AgentExecutionException;
    
    /**
     * 验证前置条件
     */
    default boolean validatePreconditions(AgentContext context) {
        return true;
    }
    
    /**
     * 获取 Agent 配置
     */
    default AgentConfig getConfig() {
        return AgentConfig.defaultConfig();
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`mvn compile -pl java-harness-workflow`
预期：SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/Agent.java
git commit -m "feat(agent): 实现 Agent 接口 - Agent核心接口（agentId/name/version/type/requiredSkills/execute）"
```

---

## 第四部分：框架核心

### 任务 11：实现 AgentRegistry 注册表

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentRegistry.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentRegistryTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentRegistry 测试")
public class AgentRegistryTest {

    @Test
    @DisplayName("应该注册 Agent")
    public void testRegisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);

        assertTrue(registry.isRegistered("worker"));
        assertEquals(1, registry.getAgentCount());
    }

    @Test
    @DisplayName("应该获取 Agent 元数据")
    public void testGetAgentMetadata() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");
        when(mockAgent.getAgentName()).thenReturn("Worker Agent");
        when(mockAgent.getVersion()).thenReturn("1.0.0");
        when(mockAgent.getDescription()).thenReturn("执行工作");
        when(mockAgent.getAgentType()).thenReturn(AgentType.WORKER);

        registry.register(mockAgent);

        AgentRegistry.AgentMetadata metadata = registry.getMetadata("worker");
        assertNotNull(metadata);
        assertEquals("worker", metadata.getAgentId());
        assertEquals("Worker Agent", metadata.getAgentName());
    }

    @Test
    @DisplayName("应该获取所有 Agent 元数据")
    public void testGetAllAgents() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent1 = mock(Agent.class);
        Agent mockAgent2 = mock(Agent.class);
        when(mockAgent1.getAgentId()).thenReturn("worker");
        when(mockAgent2.getAgentId()).thenReturn("reviewer");

        registry.register(mockAgent1);
        registry.register(mockAgent2);

        assertEquals(2, registry.getAllAgents().size());
        assertTrue(registry.getAllAgents().containsKey("worker"));
        assertTrue(registry.getAllAgents().containsKey("reviewer"));
    }

    @Test
    @DisplayName("应该注销 Agent")
    public void testUnregisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);
        assertTrue(registry.isRegistered("worker"));

        registry.unregister("worker");
        assertFalse(registry.isRegistered("worker"));
    }

    @Test
    @DisplayName("应该清空所有 Agent")
    public void testClear() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);
        registry.clear();

        assertEquals(0, registry.getAgentCount());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AgentRegistryTest -pl java-harness-workflow`
预期：FAIL

- [ ] **步骤 3：实现 AgentRegistry 类**

```java
package com.chachamaru.harness.workflow.agent.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册表
 */
public class AgentRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);
    
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, AgentMetadata> metadata = new ConcurrentHashMap<>();

    public void register(Agent agent) {
        agents.put(agent.getAgentId(), agent);
        metadata.put(agent.getAgentId(), new AgentMetadata(agent));
        logger.debug("Registered agent: {}", agent.getAgentId());
    }

    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }

    public AgentMetadata getMetadata(String agentId) {
        return metadata.get(agentId);
    }

    public Map<String, AgentMetadata> getAllAgents() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean isRegistered(String agentId) {
        return agents.containsKey(agentId);
    }

    public int getAgentCount() {
        return agents.size();
    }

    public void unregister(String agentId) {
        agents.remove(agentId);
        metadata.remove(agentId);
        logger.debug("Unregistered agent: {}", agentId);
    }

    public void clear() {
        agents.clear();
        metadata.clear();
    }

    /**
     * Agent 元数据
     */
    public static class AgentMetadata {
        private final String agentId;
        private final String agentName;
        private final AgentType type;
        private final String version;
        private final String description;
        private final java.util.List<String> requiredSkills;

        public AgentMetadata(Agent agent) {
            this.agentId = agent.getAgentId();
            this.agentName = agent.getAgentName();
            this.type = agent.getAgentType();
            this.version = agent.getVersion();
            this.description = agent.getDescription();
            this.requiredSkills = agent.getRequiredSkills();
        }

        public String getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public AgentType getType() { return type; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public java.util.List<String> getRequiredSkills() { return requiredSkills; }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AgentRegistryTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentRegistry.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentRegistryTest.java
git commit -m "feat(agent): 实现 AgentRegistry 类 - Agent注册表（register/unregister/getAgent/getMetadata）"
```

---

### 任务 12：实现 AgentExecutor 执行器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/agent/framework/AgentExecutor.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/agent/framework/AgentExecutorTest.java`

由于篇幅限制，继续按相同模式实现剩余任务...

---

（计划将在下一条消息中继续，因为已经接近 token 限制。剩余任务包括：
- 任务 12-16：AgentExecutor、AgentFramework 实现
- 任务 17-25：三个核心 Agent 实现
- 任务 26-28：BreezingTeam 实现
- 任务 29-31：集成测试）

继续吗？
