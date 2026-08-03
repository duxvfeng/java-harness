# Java Harness 阶段 1 实现计划：命令格式改造 + 核心技能框架

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 Java 项目从当前 35-40% 功能覆盖率提升到 65-70%，通过移除 picocli 框架、实现 Go 风格命令分发、创建核心 5 技能闭环，所有命令改为 `/java-harness-*` 格式。

**架构：** 移除 picocli CLI 框架，采用类似 Go 项目的简单 switch 命令分发机制。创建 Main.java 作为单一入口点，每个命令对应一个 *Handler 类。通过 .claude-plugin/skills/ 提供技能文件，hooks.json 配置 Hook 集成。

**技术栈：** Java 17, Jackson 2.15.2, SLF4J 2.0.9 + Logback 1.4.11, JUnit 5.10.0, SQLite (xerial/sqlite-jdbc)

---

## 文件结构

### 将要创建的文件

```
java-harness/
├── java-harness-cli/
│   └── src/main/java/com/chachamaru/harness/
│       ├── Main.java                          # 新建：主入口点（替代 HarnessCLI）
│       ├── handler/
│       │   ├── CommandHandler.java            # 新建：命令处理器接口
│       │   ├── CommandRegistry.java          # 新建：命令注册表
│       │   ├── HookDispatcher.java            # 新建：Hook 命令分发器
│       │   ├── PlanHandler.java               # 新建：计划命令处理器
│       │   ├── WorkHandler.java               # 新建：工作命令处理器
│       │   ├── ReviewHandler.java             # 新建：审查命令处理器
│       │   ├── ReleaseHandler.java            # 新建：发布命令处理器
│       │   ├── SyncHandler.java               # 新建：同步命令处理器
│       │   ├── InitHandler.java               # 新建：初始化命令处理器
│       │   ├── DoctorHandler.java             # 新建：诊断命令处理器
│       │   ├── ValidateHandler.java           # 新建：验证命令处理器
│       │   ├── StatusHandler.java             # 新建：状态命令处理器
│       │   ├── GenHandler.java                # 新建：生成命令处理器
│       │   ├── SprintContractHandler.java     # 新建：Sprint 合同处理器
│       │   ├── EvidenceHandler.java           # 新建：证据收集处理器
│       │   └── ... (其他 72 个命令处理器)
│       ├── hook/
│       │   ├── HookCodec.java                 # 新建：Hook JSON 编解码器
│       │   ├── HookInput.java                 # 新建：Hook 输入模型
│       │   ├── HookOutput.java                # 新建：Hook 输出模型
│       │   ├── handler/
│       │   │   ├── PreToolHookHandler.java    # 新建：PreToolUse Hook 处理器
│       │   │   ├── PostToolHookHandler.java   # 新建：PostToolUse Hook 处理器
│       │   │   └── ... (14 个 Hook 处理器)
│       ├── skill/
│       │   ├── PlanSkill.java                 # 新建：计划技能实现
│       │   ├── WorkSkill.java                 # 新建：工作技能实现
│       │   ├── ReviewSkill.java               # 新建：审查技能实现
│       │   ├── SyncSkill.java                 # 新建：同步技能实现
│       │   └── ReleaseSkill.java             # 新建：发布技能实现
│       ├── parser/
│       │   ├── PlansParser.java               # 新建：Plans.md 解析器
│       │   ├── SpecParser.java                # 新建：spec.md 解析器
│       │   └── TaskDependency.java           # 新建：任务依赖模型
│       ├── config/
│       │   ├── HarnessTomlParser.java         # 新建：harness.toml 解析器
│       │   ├── ConfigSync.java                # 新建：配置同步器
│       │   └── ConfigTemplate.java            # 新建：配置模板生成器
│       └── state/
│           ├── SessionState.java              # 新建：会话状态管理
│           ├── WorkState.java                 # 新建：工作状态管理
│           ├── StatePersistence.java          # 新建：状态持久化
│           └── JsonlWriter.java              # 新建：JSONL 写入器
├── .claude-plugin/
│   ├── plugin.json                            # 新建：插件元数据
│   ├── hooks.json                             # 新建：Hook 配置
│   └── skills/
│       ├── core/
│       │   ├── java-harness-plan.claude      # 新建：计划技能文件
│       │   ├── java-harness-work.claude      # 新建：工作技能文件
│       │   ├── java-harness-review.claude    # 新建：审查技能文件
│       │   ├── java-harness-sync.claude      # 新建：同步技能文件
│       │   └── java-harness-release.claude  # 新建：发布技能文件
│       ├── hook/
│       │   └── java-harness-hook-*.claude     # 新建：16 个 Hook 技能文件
│       └── extended/
│           └── java-harness-*.claude          # 新建：65 个扩展技能文件
└── docs/
    └── superpowers/
        └── plans/
            └── 2026-08-03-phase1-command-redesign.md  # 本文件
```

### 将要修改的文件

```
java-harness/
├── java-harness-cli/
│   ├── src/main/java/com/chachamaru/harness/cli/command/
│   │   ├── HarnessCLI.java                    # 修改：移除 picocli 注解，改为调用 Main
│   │   └── (所有 *Command.java)               # 保留：作为 Handler 实现
│   └── pom.xml                                # 修改：移除 picocli 依赖
└── README.md                                   # 修改：更新命令格式说明
```

---

## 任务 1：创建核心命令基础设施

### 任务 1.1：创建命令处理器接口

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandHandler.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandHandlerTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandHandlerTest.java
package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommandHandlerTest {
    @Test
    void testCommandHandlerInterfaceExists() {
        CommandHandler handler = new CommandHandler() {
            @Override
            public void execute(String[] args) {
                // Test implementation
            }
        };
        assertNotNull(handler);
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=CommandHandlerTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'CommandHandler'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandHandler.java
package com.chachamaru.harness.handler;

/**
 * Command handler interface.
 * All command handlers must implement this interface.
 */
public interface CommandHandler {
    /**
     * Execute the command with given arguments.
     *
     * @param args Command arguments (excluding the command name itself)
     */
    void execute(String[] args);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=CommandHandlerTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandHandler.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandHandlerTest.java
git commit -m "feat: add CommandHandler interface"
```

---

### 任务 1.2：创建命令注册表

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandRegistryTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandRegistryTest.java
package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommandRegistryTest {
    @Test
    void testCommandRegistry() {
        CommandHandler handler = CommandRegistry.getHandler("plan");
        assertNotNull(handler);
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testUnknownCommandReturnsNull() {
        CommandHandler handler = CommandRegistry.getHandler("unknown-command");
        assertNull(handler);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=CommandRegistryTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'CommandRegistry'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java
package com.chachamaru.harness.handler;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all command handlers.
 * Maps command names to their handler implementations.
 */
public class CommandRegistry {
    private static final Map<String, CommandHandler> handlers = new HashMap<>();

    static {
        // Commands will be registered here as we implement them
        // For now, register a placeholder for "plan"
        handlers.put("plan", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                System.out.println("Plan handler placeholder");
            }
        });
    }

    /**
     * Get the handler for a given command name.
     *
     * @param command The command name (without prefix)
     * @return The handler, or null if not found
     */
    public static CommandHandler getHandler(String command) {
        return handlers.get(command);
    }

    /**
     * Register a new command handler.
     *
     * @param command The command name
     * @param handler The handler implementation
     */
    public static void register(String command, CommandHandler handler) {
        handlers.put(command, handler);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=CommandRegistryTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/handler/CommandRegistryTest.java
git commit -m "feat: add CommandRegistry"
```

---

### 任务 1.3：创建 Main.java 入口点

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/Main.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/MainTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/MainTest.java
package com.chachamaru.harness;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    @Test
    void testMainExists() {
        assertDoesNotThrow(() -> Main.main(new String[]{"plan"}));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=MainTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'Main'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/Main.java
package com.chachamaru.harness;

import com.chachamaru.harness.handler.CommandHandler;
import com.chachamaru.harness.handler.CommandRegistry;
import java.util.Arrays;

/**
 * Main entry point for Java Harness CLI.
 * This is the primary entry point that replaces the picocli-based HarnessCLI.
 *
 * Usage: java-harness <command> [args...]
 */
public class Main {
    private static final String VERSION = "5.0.0-java";

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if (command.equals("--version") || command.equals("-v")) {
            System.out.println("java-harness " + VERSION);
            System.exit(0);
        }

        if (command.equals("help") || command.equals("--help") || command.equals("-h")) {
            printUsage();
            System.exit(0);
        }

        CommandHandler handler = CommandRegistry.getHandler(command);
        if (handler == null) {
            System.err.println("Unknown command: " + command);
            printUsage();
            System.exit(1);
        }

        try {
            handler.execute(commandArgs);
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java-harness <command> [args...]");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  plan                    Generate plan prompt");
        System.err.println("  work <taskID>           Execute work task");
        System.err.println("  review <taskID>         Review completed work");
        System.err.println("  release [--check]       Prepare release");
        System.err.println("  sync [root]            Sync configuration");
        System.err.println("  init [root]            Initialize project");
        System.err.println("  validate [skills|agents|all] [root]  Validate skills/agents");
        System.err.println("  doctor [--migration] [root]  Health check");
        System.err.println("  hook <type>             Execute hook handler");
        System.err.println("");
        System.err.println("  --version, -v           Print version");
        System.err.println("  help, --help, -h        Show this help");
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=MainTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：测试命令行执行**

运行：
```bash
cd java-harness-cli
mvn package -q
java -cp target/harness-cli-*.jar com.chachamaru.harness.Main plan
```
预期：输出 "Plan handler placeholder"

运行：
```bash
java -cp target/harness-cli-*.jar com.chachamaru.harness.Main --version
```
预期：输出 "java-harness 5.0.0-java"

- [ ] **步骤 6：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/Main.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/MainTest.java
git commit -m "feat: add Main.java entry point"
```

---

## 任务 2：创建 Hook 协议处理层

### 任务 2.1：创建 Hook 输入输出模型

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookInput.java`
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookOutput.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookModelTest.java`

- [ ] **步骤 1：编写 HookInput 模型**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookInput.java
package com.chachamaru.harness.hook;

import java.util.Map;

/**
 * Hook input event model.
 * Represents a hook event from Claude Code.
 */
public class HookInput {
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String hookEventName;
    private String toolName;
    private Map<String, Object> toolInput;
    private String pluginRoot;

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTranscriptPath() { return transcriptPath; }
    public void setTranscriptPath(String transcriptPath) { this.transcriptPath = transcriptPath; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }

    public String getPermissionMode() { return permissionMode; }
    public void setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; }

    public String getHookEventName() { return hookEventName; }
    public void setHookEventName(String hookEventName) { this.hookEventName = hookEventName; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getToolInput() { return toolInput; }
    public void setToolInput(Map<String, Object> toolInput) { this.toolInput = toolInput; }

    public String getPluginRoot() { return pluginRoot; }
    public void setPluginRoot(String pluginRoot) { this.pluginRoot = pluginRoot; }
}
```

- [ ] **步骤 2：编写 HookOutput 模型**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookOutput.java
package com.chachamaru.harness.hook;

/**
 * Hook output response model.
 * Represents the response to a hook event.
 */
public class HookOutput {
    private String hookEventName;
    private String permissionDecision;  // "allow" or "deny"
    private String permissionDecisionReason;
    private String additionalContext;

    public static HookOutput allow() {
        HookOutput output = new HookOutput();
        output.setPermissionDecision("allow");
        return output;
    }

    public static HookOutput deny(String reason) {
        HookOutput output = new HookOutput();
        output.setPermissionDecision("deny");
        output.setPermissionDecisionReason(reason);
        return output;
    }

    // Getters and setters
    public String getHookEventName() { return hookEventName; }
    public void setHookEventName(String hookEventName) { this.hookEventName = hookEventName; }

    public String getPermissionDecision() { return permissionDecision; }
    public void setPermissionDecision(String permissionDecision) { this.permissionDecision = permissionDecision; }

    public String getPermissionDecisionReason() { return permissionDecisionReason; }
    public void setPermissionDecisionReason(String permissionDecisionReason) { this.permissionDecisionReason = permissionDecisionReason; }

    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
}
```

- [ ] **步骤 3：编写测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookModelTest.java
package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookModelTest {
    @Test
    void testHookInputSettersGetters() {
        HookInput input = new HookInput();
        input.setSessionId("test-session");
        input.setHookEventName("PreToolUse");
        input.setToolName("Write");

        assertEquals("test-session", input.getSessionId());
        assertEquals("PreToolUse", input.getHookEventName());
        assertEquals("Write", input.getToolName());
    }

    @Test
    void testHookOutputAllow() {
        HookOutput output = HookOutput.allow();
        assertEquals("allow", output.getPermissionDecision());
        assertNull(output.getPermissionDecisionReason());
    }

    @Test
    void testHookOutputDeny() {
        HookOutput output = HookOutput.deny("Test reason");
        assertEquals("deny", output.getPermissionDecision());
        assertEquals("Test reason", output.getPermissionDecisionReason());
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=HookModelTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookInput.java
git add java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookOutput.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookModelTest.java
git commit -m "feat: add Hook input/output models"
```

---

### 任务 2.2：创建 Hook JSON 编解码器

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookCodec.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookCodecTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookCodecTest.java
package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookCodecTest {
    @Test
    void testDecodeHookInput() throws Exception {
        String json = """
            {
              "session_id": "test-session",
              "hook_event_name": "PreToolUse",
              "tool_name": "Write",
              "tool_input": {"file_path": "/test.txt"}
            }
            """;

        HookInput input = HookCodec.decode(json);
        assertEquals("test-session", input.getSessionId());
        assertEquals("PreToolUse", input.getHookEventName());
        assertEquals("Write", input.getToolName());
    }

    @Test
    void testEncodeHookOutput() throws Exception {
        HookOutput output = HookOutput.allow();
        output.setHookEventName("PreToolUse");

        String json = HookCodec.encode(output);
        assertNotNull(json);
        assertTrue(json.contains("\"permissionDecision\":\"allow\""));
    }

    @Test
    void testRoundTrip() throws Exception {
        String originalJson = """
            {
              "session_id": "test",
              "hook_event_name": "PreToolUse",
              "tool_name": "Bash",
              "tool_input": {"command": "echo test"}
            }
            """;

        HookInput input = HookCodec.decode(originalJson);
        assertNotNull(input);
        assertEquals("test", input.getSessionId());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=HookCodecTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'HookCodec'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookCodec.java
package com.chachamaru.harness.hook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON codec for hook events.
 * Handles serialization and deserialization of hook input/output.
 */
public class HookCodec {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Configure ObjectMapper for hook protocol compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    /**
     * Decode JSON string to HookInput.
     *
     * @param json JSON string
     * @return HookInput object
     * @throws Exception if parsing fails
     */
    public static HookInput decode(String json) throws Exception {
        return mapper.readValue(json, HookInput.class);
    }

    /**
     * Encode HookOutput to JSON string.
     *
     * @param output HookOutput object
     * @return JSON string
     * @throws Exception if serialization fails
     */
    public static String encode(HookOutput output) throws Exception {
        return mapper.writeValueAsString(output);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=HookCodecTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookCodec.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookCodecTest.java
git commit -m "feat: add Hook JSON codec"
```

---

### 任务 2.3：创建 HookDispatcher

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookDispatcher.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookDispatcherTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookDispatcherTest.java
package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookDispatcherTest {
    @Test
    void testHookDispatcherExecutes() {
        HookDispatcher dispatcher = new HookDispatcher();
        assertDoesNotThrow(() -> dispatcher.execute(new String[]{"pre-tool"}));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=HookDispatcherTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'HookDispatcher'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookDispatcher.java
package com.chachamaru.harness.hook;

import com.chachamaru.harness.handler.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Hook command dispatcher.
 * Reads hook event from stdin, processes it, and writes response to stdout.
 */
public class HookDispatcher implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(HookDispatcher.class);

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java-harness hook <pre-tool|post-tool|permission|...>");
            System.exit(1);
        }

        try {
            String hookType = args[0];
            String[] hookArgs = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[]{};

            // Read JSON from stdin
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            String json = jsonBuilder.toString();
            if (json.isEmpty()) {
                logger.warn("Empty stdin received");
                System.out.println(HookCodec.encode(HookOutput.allow()));
                return;
            }

            // Decode hook input
            HookInput input = HookCodec.decode(json);

            // Dispatch to specific hook handler
            HookOutput output = dispatchHook(hookType, input, hookArgs);

            // Encode and write response
            System.out.println(HookCodec.encode(output));

        } catch (Exception e) {
            logger.error("Hook processing error", e);
            // Fail-open: return allow on error
            try {
                System.out.println(HookCodec.encode(HookOutput.allow()));
            } catch (Exception ex) {
                logger.error("Failed to encode fallback response", ex);
            }
        }
    }

    private HookOutput dispatchHook(String hookType, HookInput input, String[] args) {
        // For now, return a default allow response
        // Specific hook handlers will be implemented in later tasks
        logger.info("Processing hook type: {}", hookType);

        HookOutput output = HookOutput.allow();
        output.setHookEventName(input.getHookEventName());
        return output;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=HookDispatcherTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：注册 HookDispatcher**

修改 `java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java`:

```java
static {
    // Register hook dispatcher
    handlers.put("hook", new HookDispatcher());

    // Register plan placeholder
    handlers.put("plan", new CommandHandler() {
        @Override
        public void execute(String[] args) {
            System.out.println("Plan handler placeholder");
        }
    });
}
```

添加导入：
```java
import com.chachamaru.harness.hook.HookDispatcher;
```

- [ ] **步骤 6：测试 Hook 命令**

运行：
```bash
cd java-harness-cli
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Write","tool_input":{"file_path":"/test.txt"}}' | \
java -cp target/harness-cli-*.jar com.chachamaru.harness.Main hook pre-tool
```
预期：输出 JSON 包含 `"permissionDecision":"allow"`

- [ ] **步骤 7：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/hook/HookDispatcher.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/hook/HookDispatcherTest.java
git add java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java
git commit -m "feat: add HookDispatcher"
```

---

## 任务 3：创建核心命令处理器

### 任务 3.1：创建 PlanHandler

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/handler/PlanHandler.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/handler/PlanHandlerTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/handler/PlanHandlerTest.java
package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlanHandlerTest {
    @Test
    void testPlanHandlerExecutes() {
        PlanHandler handler = new PlanHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=PlanHandlerTest -pl java-harness-cli`
预期：FAIL，报错 "Cannot resolve symbol 'PlanHandler'"

- [ ] **步骤 3：编写最少实现代码**

```java
// java-harness-cli/src/main/java/com/chachamaru/harness/handler/PlanHandler.java
package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plan command handler.
 * Generates a plan prompt for the host to execute.
 */
public class PlanHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlanHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Read spec.md if it exists
            Path specPath = Paths.get("spec.md");
            String specContent = "";
            if (Files.exists(specPath)) {
                specContent = Files.readString(specPath);
            }

            // Read Plans.md if it exists
            Path plansPath = Paths.get("Plans.md");
            String plansContent = "";
            if (Files.exists(plansPath)) {
                plansContent = Files.readString(plansPath);
            }

            // Generate plan prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Plan Generation\n\n");

            if (!specContent.isEmpty()) {
                prompt.append("## Specification\n\n");
                prompt.append(specContent);
                prompt.append("\n\n");
            }

            if (!plansContent.isEmpty()) {
                prompt.append("## Existing Plans\n\n");
                prompt.append(plansContent);
                prompt.append("\n\n");
            }

            prompt.append("Please generate or update the plan based on the above specifications.\n");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading plan files", e);
            System.err.println("Error reading plan files: " + e.getMessage());
            System.exit(1);
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=PlanHandlerTest -pl java-harness-cli`
预期：PASS

- [ ] **步骤 5：注册 PlanHandler**

修改 `java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java`:

```java
static {
    handlers.put("hook", new HookDispatcher());
    handlers.put("plan", new PlanHandler());  // Replace placeholder
    // ... other handlers
}
```

添加导入：
```java
import com.chachamaru.harness.handler.PlanHandler;
```

- [ ] **步骤 6：测试 plan 命令**

运行：
```bash
cd java-harness-cli
echo "# Test Spec" > spec.md
java -cp target/harness-cli-*.jar com.chachamaru.harness.Main plan
```
预期：输出包含 "## Specification" 和 "# Test Spec"

- [ ] **步骤 7：Commit**

```bash
git add java-harness-cli/src/main/java/com/chachamaru/harness/handler/PlanHandler.java
git add java-harness-cli/src/test/java/com/chachamaru/harness/handler/PlanHandlerTest.java
git add java-harness-cli/src/main/java/com/chachamaru/harness/handler/CommandRegistry.java
git commit -m "feat: add PlanHandler"
```

---

### 任务 3.2-3.5：创建其他核心命令处理器

（由于篇幅限制，以下任务的详细步骤省略，但按照相同的 TDD 模式）

- [ ] **任务 3.2：创建 WorkHandler**
- [ ] **任务 3.3：创建 ReviewHandler**
- [ ] **任务 3.4：创建 ReleaseHandler**
- [ ] **任务 3.5：创建 SyncHandler**

每个任务遵循相同的模式：
1. 编写失败的测试
2. 运行测试验证失败
3. 编写最少实现代码
4. 运行测试验证通过
5. 注册到 CommandRegistry
6. 手动测试命令
7. Commit

---

## 任务 4：创建 Plans.md 解析器

### 任务 4.1：创建 PlansParser

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/parser/PlansParser.java`
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/parser/Task.java`
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/parser/TaskDependency.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/parser/PlansParserTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// java-harness-cli/src/test/java/com/chachamaru/harness/parser/PlansParserTest.java
package com.chachamaru.harness.parser;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PlansParserTest {
    @Test
    void testParsePlansMarkdown() {
        String markdown = """
            # Plans

            ## TASK-001: First Task
            - [x] Subtask 1
            - [ ] Subtask 2

            ## TASK-002: Second Task
            Depends on: TASK-001
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertEquals(2, tasks.size());
        assertEquals("TASK-001", tasks.get(0).getId());
        assertEquals("First Task", tasks.get(0).getTitle());
    }
}
```

- [ ] **步骤 2-7：完整实现**（遵循 TDD 模式）

---

## 任务 5：创建 .claude-plugin 技能文件

### 任务 5.1：创建 plugin.json 和 hooks.json

**文件：**
- 创建：`.claude-plugin/plugin.json`
- 创建：`.claude-plugin/hooks.json`

- [ ] **步骤 1：创建 plugin.json**

```json
{
  "name": "java-harness",
  "version": "5.0.0-java",
  "description": "Java implementation of Claude Code Harness",
  "author": "Chachamaru",
  "homepage": "https://github.com/Chachamaru127/java-harness",
  "cli": {
    "binary": "java-harness",
    "commands": [
      {
        "name": "plan",
        "skill": "java-harness-plan"
      },
      {
        "name": "work",
        "skill": "java-harness-work"
      },
      {
        "name": "review",
        "skill": "java-harness-review"
      },
      {
        "name": "release",
        "skill": "java-harness-release"
      },
      {
        "name": "sync",
        "skill": "java-harness-sync"
      }
    ]
  }
}
```

- [ ] **步骤 2：创建 hooks.json**

```json
{
  "PreToolUse": {
    "command": "java-harness",
    "args": ["hook", "pre-tool"],
    "env": {
      "JAVA_HARNESS_VERSION": "5.0.0-java"
    }
  },
  "PostToolUse": {
    "command": "java-harness",
    "args": ["hook", "post-tool"]
  },
  "PermissionRequest": {
    "command": "java-harness",
    "args": ["hook", "permission"]
  },
  "SessionStart": {
    "command": "java-harness",
    "args": ["hook", "session-start"]
  },
  "SessionEnd": {
    "command": "java-harness",
    "args": ["hook", "session-cleanup"]
  }
}
```

- [ ] **步骤 3：Commit**

```bash
git add .claude-plugin/
git commit -m "feat: add Claude Code plugin configuration"
```

---

### 任务 5.2：创建核心技能文件

**文件：**
- 创建：`.claude-plugin/skills/core/java-harness-plan.claude`
- 创建：`.claude-plugin/skills/core/java-harness-work.claude`
- 创建：`.claude-plugin/skills/core/java-harness-review.claude`
- 创建：`.claude-plugin/skills/core/java-harness-sync.claude`
- 创建：`.claude-plugin/skills/core/java-harness-release.claude`

- [ ] **步骤 1：创建 java-harness-plan.claude**

```
---
name: java-harness-plan
description: Generate plan prompt for the host to execute
---

Please generate a plan based on the current project context.

Use the java-harness CLI to generate the plan:
```bash
java-harness plan
```

This will read spec.md and Plans.md (if they exist) and generate a comprehensive plan.
```

- [ ] **步骤 2-6：创建其他核心技能文件**（类似格式）

- [ ] **步骤 7：Commit**

```bash
git add .claude-plugin/skills/core/
git commit -m "feat: add core 5 skill files"
```

---

## 任务 6：创建其他扩展命令处理器

（实现剩余的 72 个命令处理器，每个遵循 TDD 模式）

由于命令数量众多，建议按功能分组实现：

- [ ] **任务 6.1：证据相关命令** (EvidenceHandler, CollectCommand)
- [ ] **任务 6.2：状态相关命令** (StatusHandler, DoctorHandler)
- [ ] **任务 6.3：配置相关命令** (InitHandler, ValidateHandler, SyncHandler)
- [ ] **任务 6.4：代理相关命令** (SubagentStartHandler, SubagentStopHandler, BreezingSignalHandler)
- [ ] **任务 6.5：监控相关命令** (NightWatchHandler, MirrorHandler, FailureCodifierHandler)
- [ ] **任务 6.6：Worktree 相关命令** (WtHandler, WorktreeCreateHandler, WorktreeRemoveHandler)
- [ ] **任务 6.7：评分和质量命令** (ImpactScoreHandler, QualityPackHandler)
- [ ] **任务 6.8：其他扩展命令** (剩余命令)

---

## 任务 7：创建配置管理模块

### 任务 7.1：创建 HarnessTomlParser

**文件：**
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/config/HarnessTomlParser.java`
- 测试：`java-harness-cli/src/test/java/com/chachamaru/harness/config/HarnessTomlParserTest.java`

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

### 任务 7.2：实现 InitHandler

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

### 任务 7.3：实现 SyncHandler

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

## 任务 8：创建状态管理模块

### 任务 8.1：创建 SessionState 和 WorkState

**文件：**
- 创建：`java-harness-foundation/src/main/java/com/chachamaru/harness/state/SessionState.java`
- 创建：`java-harness-foundation/src/main/java/com/chachamaru/harness/state/WorkState.java`
- 测试：相应测试文件

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

### 任务 8.2：创建 StatePersistence

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

### 任务 8.3：创建 JsonlWriter

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

## 任务 9：集成测试和端到端测试

### 任务 9.1：创建集成测试套件

**文件：**
- 创建：`java-harness-cli/src/test/java/com/chachamaru/harness/integration/IntegrationTest.java`

- [ ] **步骤 1-7：完整实现**（TDD 模式）

---

### 任务 9.2：创建 E2E 测试脚本

**文件：**
- 创建：`tests/e2e/test-e2e.sh`

- [ ] **步骤 1：编写测试脚本**

```bash
#!/bin/bash
set -e

echo "Running E2E test for Java Harness Phase 1"

# Build project
mvn clean package -q

# Initialize test project
TEST_DIR=/tmp/java-harness-test-$$
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

# Test init command
java -cp $PROJECT_DIR/java-harness-cli/target/harness-cli-*.jar com.chachamaru.harness.Main init

# Create spec.md
cat > spec.md << 'EOF'
# Test Spec
Test specification for E2E test
EOF

# Test plan command
java -cp $PROJECT_DIR/java-harness-cli/target/harness-cli-*.jar com.chachamaru.harness.Main plan > plan.txt
if [ ! -s plan.txt ]; then
    echo "FAIL: plan command produced empty output"
    exit 1
fi

# Test hook command
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Write","tool_input":{"file_path":"/test.txt"}}' | \
java -cp $PROJECT_DIR/java-harness-cli/target/harness-cli-*.jar com.chachamaru.harness.Main hook pre-tool > hook.txt
if ! grep -q '"permissionDecision":"allow"' hook.txt; then
    echo "FAIL: hook command did not return allow"
    exit 1
fi

# Cleanup
rm -rf "$TEST_DIR"

echo "E2E test passed!"
```

- [ ] **步骤 2：运行 E2E 测试**

```bash
chmod +x tests/e2e/test-e2e.sh
PROJECT_DIR=$(pwd) bash tests/e2e/test-e2e.sh
```

预期：输出 "E2E test passed!"

---

## 任务 10：性能测试和优化

### 任务 10.1：创建性能基准测试

**文件：**
- 创建：`java-harness-cli/src/test/java/com/chachamaru/harness/performance/HookResponseTimeTest.java`

- [ ] **步骤 1：编写性能测试**

```java
package com.chachamaru.harness.performance;

import com.chachamaru.harness.hook.HookCodec;
import com.chachamaru.harness.hook.HookInput;
import com.chachamaru.harness.hook.HookDispatcher;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HookResponseTimeTest {
    @Test
    void testHookResponseTime_P95Under10ms() {
        // Warm-up
        for (int i = 0; i < 100; i++) {
            executeHook();
        }

        // Measure
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            long start = System.nanoTime();
            executeHook();
            long end = System.nanoTime();
            times.add((end - start) / 1_000_000); // Convert to ms
        }

        // Calculate P95
        Collections.sort(times);
        long p95 = times.get((int) (times.size() * 0.95));

        System.out.println("P95 hook response time: " + p95 + "ms");
        assertTrue(p95 < 10, "P95 response time must be under 10ms, got: " + p95 + "ms");
    }

    private void executeHook() {
        try {
            String json = "{\"session_id\":\"test\",\"hook_event_name\":\"PreToolUse\",\"tool_name\":\"Write\",\"tool_input\":{\"file_path\":\"/test.txt\"}}";
            HookInput input = HookCodec.decode(json);
            HookDispatcher dispatcher = new HookDispatcher();
            dispatcher.execute(new String[]{"pre-tool"});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **步骤 2：运行性能测试**

```bash
mvn test -Dtest=HookResponseTimeTest -pl java-harness-cli
```

预期：输出 P95 响应时间，应该 < 10ms

---

## 任务 11：文档更新

### 任务 11.1：更新 README.md

**文件：**
- 修改：`README.md`

- [ ] **步骤 1：更新命令格式说明**

将所有 `harness <command>` 改为 `java-harness <command>`，添加 `/java-harness-*` 技能格式说明。

- [ ] **步骤 2：更新功能列表**

添加阶段 1 实现的所有功能。

- [ ] **步骤 3：Commit**

```bash
git add README.md
git commit -m "docs: update README for Phase 1 completion"
```

---

## 任务 12：Native Image 编译和打包

### 任务 12.1：配置 GraalVM Native Image

**文件：**
- 修改：`java-harness-cli/pom.xml`
- 创建：`java-harness-cli/src/main/resources/META-INF/native-image/reflect-config.json`

- [ ] **步骤 1-7：完整配置和测试**

---

### 任务 12.2：生成 Native Image

- [ ] **步骤 1：编译 Native Image**

```bash
cd java-harness-cli
mvn -Pnative native:compile
```

- [ ] **步骤 2：测试 Native Image**

```bash
./target/harness --version
./target/harness plan
```

- [ ] **步骤 3：测量性能**

验证启动时间 < 100ms，Hook 响应 < 10ms

---

## 任务 13：发布准备

### 任务 13.1：创建安装脚本

**文件：**
- 创建：`scripts/install-java-harness.sh`

- [ ] **步骤 1-7：完整实现**

---

### 任务 13.2：创建 GitHub Actions CI/CD

**文件：**
- 创建：`.github/workflows/build.yml`

- [ ] **步骤 1-7：完整实现**

---

## 任务 14：最终验证和发布

### 任务 14.1：运行完整测试套件

- [ ] **步骤 1：运行所有测试**

```bash
mvn clean test
```

预期：所有测试通过，覆盖率 ≥ 80%

- [ ] **步骤 2：运行 E2E 测试**

```bash
bash tests/e2e/test-e2e.sh
```

预期：E2E 测试通过

- [ ] **步骤 3：运行性能测试**

```bash
mvn test -Dtest=HookResponseTimeTest -pl java-harness-cli
```

预期：P95 响应时间 < 10ms

---

### 任务 14.2：创建 Release

- [ ] **步骤 1：更新版本号**

将 `VERSION` 常量从 "5.0.0-java" 更新为 "5.0.0-java-SNAPSHOT" 准备下一个版本。

- [ ] **步骤 2：创建 tag**

```bash
git tag -a v5.0.0-java -m "Phase 1 complete: Command format redesign and core skills"
```

- [ ] **步骤 3：Push 和发布**

```bash
git push origin master --tags
```

---

## 总结

此实现计划涵盖阶段 1 的所有核心功能：

✅ **命令系统改造** (任务 1-3)
✅ **Hook 协议处理** (任务 2)
✅ **核心 5 技能实现** (任务 3)
✅ **Plans.md 解析器** (任务 4)
✅ **技能文件配置** (任务 5)
✅ **扩展命令实现** (任务 6)
✅ **配置管理** (任务 7)
✅ **状态管理** (任务 8)
✅ **测试覆盖** (任务 9)
✅ **性能优化** (任务 10)
✅ **文档更新** (任务 11)
✅ **Native Image** (任务 12)
✅ **发布准备** (任务 13-14)

**预期成果**:
- 86 个命令全部实现
- 功能覆盖率从 35-40% 提升到 65-70%
- 核心 Plan → Work → Review → Release 闭环可用
- P95 Hook 响应时间 < 10ms
- 单元测试覆盖率 ≥ 80%

**下一步**：阶段 2 - 工作流编排 + 高级功能
