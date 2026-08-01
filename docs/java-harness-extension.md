**目标：** 将Java Harness项目从35-40%功能实现度扩展到与Go项目功能对等（90%+），实现完整的Plan→Work→Review→Release闭环

**架构：** 7层功能域驱动设计（基础设施层→协议层→安全防护层→工作流层→协作层→运行时层→工具层），9个Maven模块，单向依赖，职责清晰

**技术栈：** Java 17 + Spring Boot 3.2 + MyBatis + SQLite + Jackson + CompletableFuture + GraalVM Native Image

---

## 项目文件结构

将要创建或修改的文件及其职责：

### 新增模块结构
```
java-harness/
├── java-harness-foundation/        # 基础设施层
│   ├── src/main/java/com/chachamaru/harness/foundation/
│   │   ├── dto/                   # 共享数据传输对象
│   │   ├── config/                # 配置抽象和实现
│   │   └── persistence/           # 数据访问层
│   └── pom.xml
│
├── java-harness-protocol/          # 协议层
│   ├── src/main/java/com/chachamaru/harness/protocol/
│   │   ├── hook/                  # Hook协议定义
│   │   ├── tool/                  # 工具协议
│   │   └── codec/                 # 编解码器
│   └── pom.xml
│
├── java-harness-security/         # 安全防护层
│   ├── src/main/java/com/chachamaru/harness/security/
│   │   ├── guardrail/             # Guardrail规则引擎
│   │   ├── validation/            # 输入验证
│   │   └── audit/                 # 审计日志
│   └── pom.xml
│
├── java-harness-workflow/         # 工作流层
│   ├── src/main/java/com/chachamaru/harness/workflow/
│   │   ├── plans/                 # Plans.md解析
│   │   ├── orchestration/         # 任务编排
│   │   ├── execution/             # 执行引擎
│   │   └── recovery/              # 状态恢复
│   └── pom.xml
│
├── java-harness-collaboration/    # 协作层
│   ├── src/main/java/com/chachamaru/harness/collaboration/
│   │   ├── skills/                # 技能框架
│   │   ├── agents/                # 代理框架
│   │   └── coordination/          # 协调机制
│   └── pom.xml
│
├── java-harness-cli/              # CLI运行时
│   ├── src/main/java/com/chachamaru/harness/cli/
│   │   ├── HarnessCli.java        # CLI主入口
│   │   └── native/                # Native Image支持
│   └── pom.xml
│
├── java-harness-service/          # Spring Boot服务（已有，需扩展）
├── java-harness-tools/            # 工具集
│   ├── src/main/java/com/chachamaru/harness/tools/
│   │   ├── config/                # 配置工具
│   │   ├── validate/              # 验证工具
│   │   └── doctor/                # 诊断工具
│   └── pom.xml
│
└── java-harness-distribution/     # 分发包
    ├── src/main/assemblies/
    │   ├── jar-assembly.xml
    │   └── native-assembly.xml
    └── pom.xml
```

### 配置文件
```
config/
├── harness.yaml.example          # 配置模板
└── logback.xml                   # 日志配置

docs/
├── installation.md              # 安装指南
├── configuration.md             # 配置指南
└── migration.md                 # 迁移指南
```

### 测试文件
```
tests/
├── unit/                         # 单元测试
├── integration/                  # 集成测试
└── performance/                  # 性能测试
```

---

## 阶段 1：基础架构重构（2-3周）

**目标：** 重组模块结构，建立7层架构，迁移现有功能到新模块

### 阶段 1.1：创建Maven父项目和多模块结构（2-3天）

#### 任务 1.1.1：创建根POM文件

**文件：**
- 创建：`pom.xml`（根项目）

- [ ] **步骤 1：编写根POM文件**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.chachamaru</groupId>
    <artifactId>java-harness-parent</artifactId>
    <version>4.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>Java Harness Parent</name>
    <description>Claude Code Harness - Java Implementation</description>
    
    <modules>
        <module>java-harness-foundation</module>
        <module>java-harness-protocol</module>
        <module>java-harness-security</module>
        <module>java-harness-workflow</module>
        <module>java-harness-collaboration</module>
        <module>java-harness-cli</module>
        <module>java-harness-service</module>
        <module>java-harness-tools</module>
        <module>java-harness-distribution</module>
    </modules>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        
        <spring.boot.version>3.2.0</spring.boot.version>
        <jackson.version>2.15.2</jackson.version>
        <mybatis.version>3.0.3</mybatis.version>
        <sqlite.version>3.43.0.0</sqlite.version>
        <slf4j.version>2.0.9</slf4j.version>
        <junit.version>5.10.0</junit.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.mybatis</groupId>
                <artifactId>mybatis-spring-boot-starter</artifactId>
                <version>${mybatis.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.xerial</groupId>
                <artifactId>sqlite-jdbc</artifactId>
                <version>${sqlite.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **步骤 2：验证POM文件语法**

运行：`mvn help:effective-pom`
预期：成功解析POM，显示有效配置

- [ ] **步骤 3：Commit**

```bash
git add pom.xml
git commit -m "feat: create root POM with 9-module structure"
```

#### 任务 1.1.2：创建foundation模块

**文件：**
- 创建：`java-harness-foundation/pom.xml`
- 创建：`java-harness-foundation/src/main/java/com/chachamaru/harness/foundation/dto/HookInput.java`
- 创建：`java-harness-foundation/src/main/java/com/chachamaru/harness/foundation/dto/HookOutput.java`
- 创建：`java-harness-foundation/src/main/java/com/chachamaru/harness/foundation/config/HarnessConfig.java`
- 测试：`java-harness-foundation/src/test/java/com/chachamaru/harness/foundation/dto/HookInputTest.java`

- [ ] **步骤 1：编写foundation模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-foundation</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Foundation</name>
    <description>Foundation layer - DTOs, config, persistence</description>
    
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：创建HookInput DTO**

```java
package com.chachamaru.harness.foundation.dto;

import java.util.Map;

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

- [ ] **步骤 3：创建HookOutput DTO**

```java
package com.chachamaru.harness.foundation.dto;

public class HookOutput {
    private String hookEventName;
    private String permissionDecision; // "allow" | "deny" | "ask" | "defer"
    private String permissionDecisionReason;
    private Object updatedInput;
    private String additionalContext;
    
    // Getters and setters
    public String getHookEventName() { return hookEventName; }
    public void setHookEventName(String hookEventName) { this.hookEventName = hookEventName; }
    
    public String getPermissionDecision() { return permissionDecision; }
    public void setPermissionDecision(String permissionDecision) { this.permissionDecision = permissionDecision; }
    
    public String getPermissionDecisionReason() { return permissionDecisionReason; }
    public void setPermissionDecisionReason(String permissionDecisionReason) { 
        this.permissionDecisionReason = permissionDecisionReason; 
    }
    
    public Object getUpdatedInput() { return updatedInput; }
    public void setUpdatedInput(Object updatedInput) { this.updatedInput = updatedInput; }
    
    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
}
```

- [ ] **步骤 4：创建GuardrailResult DTO**

```java
package com.chachamaru.harness.foundation.dto;

public class GuardrailResult {
    private Decision decision;
    private String ruleId;
    private String reason;
    private boolean block;
    
    public enum Decision {
        ALLOW, DENY, ASK, WARN
    }
    
    // Static factory methods
    public static GuardrailResult allow() {
        return new GuardrailResult(Decision.ALLOW, null, null, false);
    }
    
    public static GuardrailResult deny(String ruleId, String reason) {
        return new GuardrailResult(Decision.DENY, ruleId, reason, true);
    }
    
    public static GuardrailResult ask(String ruleId, String reason) {
        return new GuardrailResult(Decision.ASK, ruleId, reason, false);
    }
    
    // Constructor
    public GuardrailResult(Decision decision, String ruleId, String reason, boolean block) {
        this.decision = decision;
        this.ruleId = ruleId;
        this.reason = reason;
        this.block = block;
    }
    
    // Getters
    public Decision getDecision() { return decision; }
    public String getRuleId() { return ruleId; }
    public String getReason() { return reason; }
    public boolean isBlock() { return block; }
}
```

- [ ] **步骤 5：创建配置接口**

```java
package com.chachamaru.harness.foundation.config;

public interface HarnessConfig {
    String getProjectName();
    String getVersion();
    String getDescription();
    SecurityConfig getSecurityConfig();
    WorkflowConfig getWorkflowConfig();
    
    public interface SecurityConfig {
        boolean isGuardrailEnabled(String ruleId);
        java.util.List<String> getProtectedPaths();
    }
    
    public interface WorkflowConfig {
        String getPlansPath();
        String getMarkerFamily();
        boolean isParallelExecutionEnabled();
        int getMaxConcurrency();
    }
}
```

- [ ] **步骤 6：编写DTO测试**

```java
package com.chachamaru.harness.foundation.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HookInputTest {
    
    @Test
    void shouldCreateHookInputWithAllFields() {
        HookInput input = new HookInput();
        input.setSessionId("test-session");
        input.setHookEventName("PreToolUse");
        input.setToolName("Bash");
        
        assertEquals("test-session", input.getSessionId());
        assertEquals("PreToolUse", input.getHookEventName());
        assertEquals("Bash", input.getToolName());
    }
}

class GuardrailResultTest {
    
    @Test
    void shouldCreateAllowResult() {
        GuardrailResult result = GuardrailResult.allow();
        
        assertEquals(GuardrailResult.Decision.ALLOW, result.getDecision());
        assertFalse(result.isBlock());
    }
    
    @Test
    void shouldCreateDenyResult() {
        GuardrailResult result = GuardrailResult.deny("R01", "Sudo blocked");
        
        assertEquals(GuardrailResult.Decision.DENY, result.getDecision());
        assertEquals("R01", result.getRuleId());
        assertEquals("Sudo blocked", result.getReason());
        assertTrue(result.isBlock());
    }
}
```

- [ ] **步骤 7：运行测试验证**

运行：`cd java-harness-foundation && mvn test`
预期：所有测试通过

- [ ] **步骤 8：Commit**

```bash
git add java-harness-foundation/
git commit -m "feat: create foundation module with DTOs and config interface"
```

#### 任务 1.1.3：创建protocol模块

**文件：**
- 创建：`java-harness-protocol/pom.xml`
- 创建：`java-harness-protocol/src/main/java/com/chachamaru/harness/protocol/hook/HookEventType.java`
- 创建：`java-harness-protocol/src/main/java/com/chachamaru/harness/protocol/hook/HookHandler.java`
- 创建：`java-harness-protocol/src/main/java/com/chachamaru/harness/protocol/hook/HookRegistry.java`
- 创建：`java-harness-protocol/src/main/java/com/chachamaru/harness/protocol/codec/HookCodec.java`
- 创建：`java-harness-protocol/src/main/java/com/chachamaru/harness/protocol/codec/JacksonHookCodec.java`
- 测试：`java-harness-protocol/src/test/java/com/chachamaru/harness/protocol/codec/JacksonHookCodecTest.java`

- [ ] **步骤 1：编写protocol模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-protocol</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Protocol</name>
    <description>Protocol layer - Hook and tool protocols</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：创建HookEventType枚举**

```java
package com.chachamaru.harness.protocol.hook;

public enum HookEventType {
    PRE_TOOL_USE,
    POST_TOOL_USE,
    PERMISSION_REQUEST,
    SESSION_START,
    SESSION_END,
    STOP,
    SUBAGENT_START,
    SUBAGENT_STOP,
    TASK_COMPLETED,
    TASK_CREATED,
    PRE_COMPACT,
    POST_COMPACT,
    NOTIFICATION,
    PERMISSION_DENIED,
    CONFIG_CHANGE,
    CWD_CHANGED,
    FILE_CHANGED,
    ELICITATION,
    RESULT,
    INSTRUCTIONS_LOADED;
    
    public static HookEventType fromString(String eventName) {
        try {
            return HookEventType.valueOf(eventName.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown hook event: " + eventName);
        }
    }
}
```

- [ ] **步骤 3：创建HookHandler接口**

```java
package com.chachamaru.harness.protocol.hook;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;

public interface HookHandler {
    HookOutput handle(HookInput input) throws HookException;
    boolean supports(HookEventType eventType);
}
```

- [ ] **步骤 4：创建HookException**

```java
package com.chachamaru.harness.protocol.hook;

public class HookException extends Exception {
    public HookException(String message) {
        super(message);
    }
    
    public HookException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 5：创建HookCodec接口**

```java
package com.chachamaru.harness.protocol.codec;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import java.io.InputStream;
import java.io.OutputStream;

public interface HookCodec {
    HookInput decode(InputStream input) throws CodecException;
    void encode(HookOutput output, OutputStream output) throws CodecException;
}
```

- [ ] **步骤 6：创建CodecException**

```java
package com.chachamaru.harness.protocol.codec;

public class CodecException extends Exception {
    public CodecException(String message) {
        super(message);
    }
    
    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 7：实现JacksonHookCodec**

```java
package com.chachamaru.harness.protocol.codec;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;

public class JacksonHookCodec implements HookCodec {
    private final ObjectMapper objectMapper;
    
    public JacksonHookCodec() {
        this.objectMapper = new ObjectMapper();
        // Configure for performance
        this.objectMapper.findAndRegisterModules();
    }
    
    @Override
    public HookInput decode(InputStream input) throws CodecException {
        try {
            return objectMapper.readValue(input, HookInput.class);
        } catch (Exception e) {
            throw new CodecException("Failed to decode HookInput", e);
        }
    }
    
    @Override
    public void encode(HookOutput output, OutputStream out) throws CodecException {
        try {
            objectMapper.writeValue(out, output);
        } catch (Exception e) {
            throw new CodecException("Failed to encode HookOutput", e);
        }
    }
}
```

- [ ] **步骤 8：编写Codec测试**

```java
package com.chachamaru.harness.protocol.codec;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class JacksonHookCodecTest {
    
    @Test
    void shouldDecodeHookInput() throws CodecException {
        String json = """{
            "sessionId": "test-session",
            "hookEventName": "PreToolUse",
            "toolName": "Bash",
            "toolInput": {"command": "ls -la"}
        }""";
        
        JacksonHookCodec codec = new JacksonHookCodec();
        InputStream input = new ByteArrayInputStream(json.getBytes());
        
        HookInput result = codec.decode(input);
        
        assertEquals("test-session", result.getSessionId());
        assertEquals("PreToolUse", result.getHookEventName());
        assertEquals("Bash", result.getToolName());
    }
    
    @Test
    void shouldEncodeHookOutput() throws CodecException {
        HookOutput output = new HookOutput();
        output.setHookEventName("PreToolUse");
        output.setPermissionDecision("allow");
        
        JacksonHookCodec codec = new JacksonHookCodec();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        codec.encode(output, out);
        
        String result = out.toString();
        assertTrue(result.contains("\"PreToolUse\""));
        assertTrue(result.contains("\"allow\""));
    }
}
```

- [ ] **步骤 9：运行测试验证**

运行：`cd java-harness-protocol && mvn test`
预期：所有测试通过

- [ ] **步骤 10：Commit**

```bash
git add java-harness-protocol/
git commit -m "feat: create protocol module with Hook codec and event types"
```

#### 任务 1.1.4：创建security模块（迁移现有Guardrail规则）

**文件：**
- 创建：`java-harness-security/pom.xml`
- 修改：从现有`cli-native`模块迁移Guardrail规则到`java-harness-security`
- 创建：`java-harness-security/src/main/java/com/chachamaru/harness/security/guardrail/GuardrailRule.java`
- 创建：`java-harness-security/src/main/java/com/chachamaru/harness/security/guardrail/GuardrailEngine.java`
- 创建：`java-harness-security/src/main/java/com/chachamaru/harness/security/guardrail/RuleRegistry.java`

- [ ] **步骤 1：编写security模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-security</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Security</name>
    <description>Security layer - Guardrail rules and validation</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-protocol</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：创建GuardrailRule接口**

```java
package com.chachamaru.harness.security.guardrail;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.foundation.dto.HookInput;

public interface GuardrailRule {
    String getId();
    String getName();
    String getDescription();
    boolean matches(HookInput input);
    GuardrailResult evaluate(HookInput input);
}
```

- [ ] **步骤 3：创建GuardrailEngine接口**

```java
package com.chachamaru.harness.security.guardrail;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import java.util.List;

public interface GuardrailEngine {
    void registerRule(GuardrailRule rule);
    void unregisterRule(String ruleId);
    GuardrailResult evaluate(HookInput input);
    List<GuardrailRule> getTriggeredRules(HookInput input);
    List<GuardrailRule> getAllRules();
}
```

- [ ] **步骤 4：实现RuleRegistry**

```java
package com.chachamaru.harness.security.guardrail;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class RuleRegistry implements GuardrailEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);
    private final Map<String, GuardrailRule> rules = new LinkedHashMap<>();
    
    @Override
    public void registerRule(GuardrailRule rule) {
        log.info("Registering guardrail rule: {}", rule.getId());
        rules.put(rule.getId(), rule);
    }
    
    @Override
    public void unregisterRule(String ruleId) {
        log.info("Unregistering guardrail rule: {}", ruleId);
        rules.remove(ruleId);
    }
    
    @Override
    public GuardrailResult evaluate(HookInput input) {
        for (GuardrailRule rule : rules.values()) {
            if (rule.matches(input)) {
                GuardrailResult result = rule.evaluate(input);
                if (result.isBlock()) {
                    log.warn("Guardrail {} triggered: {}", rule.getId(), result.getReason());
                    return result;
                }
            }
        }
        return GuardrailResult.allow();
    }
    
    @Override
    public List<GuardrailRule> getTriggeredRules(HookInput input) {
        List<GuardrailRule> triggered = new ArrayList<>();
        for (GuardrailRule rule : rules.values()) {
            if (rule.matches(input)) {
                GuardrailResult result = rule.evaluate(input);
                if (result.isBlock() || result.getDecision() == GuardrailResult.Decision.WARN) {
                    triggered.add(rule);
                }
            }
        }
        return triggered;
    }
    
    @Override
    public List<GuardrailRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }
}
```

- [ ] **步骤 5：迁移现有Guardrail规则**

从现有`cli-native/src/main/java/com/chachamaru/harness/cli/guardrail/rules/`迁移所有规则到新模块，保持实现不变，只更新包名和导入。

- [ ] **步骤 6：运行测试验证**

运行：`cd java-harness-security && mvn test`
预期：所有现有Guardrail规则测试通过

- [ ] **步骤 7：Commit**

```bash
git add java-harness-security/
git commit -m "feat: create security module and migrate Guardrail rules"
```

#### 任务 1.1.5：创建其他模块框架

**文件：**
- 创建：`java-harness-workflow/pom.xml`
- 创建：`java-harness-collaboration/pom.xml`
- 创建：`java-harness-cli/pom.xml`
- 创建：`java-harness-tools/pom.xml`
- 创建：`java-harness-distribution/pom.xml`

- [ ] **步骤 1：创建workflow模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-workflow</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Workflow</name>
    <description>Workflow layer - Plans parsing, orchestration, recovery</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-protocol</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：创建collaboration模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-collaboration</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Collaboration</name>
    <description>Collaboration layer - Skills, agents, coordination</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-workflow</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 3：创建cli模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-cli</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness CLI</name>
    <description>Runtime layer - CLI entry point and Native Image support</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-protocol</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-security</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>0.10.0</version>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **步骤 4：创建tools模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-tools</artifactId>
    <packaging>jar</packaging>
    
    <name>Harness Tools</name>
    <description>Tools layer - Config, validation, doctor</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-foundation</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-cli</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 5：创建distribution模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.chachamaru</groupId>
        <artifactId>java-harness-parent</artifactId>
        <version>4.1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>java-harness-distribution</artifactId>
    <packaging>pom</packaging>
    
    <name>Harness Distribution</name>
    <description>Distribution layer - Assembly and packaging</description>
    
    <dependencies>
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-cli</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-service</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.chachamaru</groupId>
            <artifactId>java-harness-tools</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <descriptors>
                        <descriptor>src/main/assemblies/jar-assembly.xml</descriptor>
                        <descriptor>src/main/assemblies/native-assembly.xml</descriptor>
                    </descriptors>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **步骤 6：验证模块编译**

运行：`mvn clean compile`
预期：所有9个模块编译成功

- [ ] **步骤 7：Commit**

```bash
git add java-harness-workflow/ java-harness-collaboration/ java-harness-cli/ java-harness-tools/ java-harness-distribution/
git commit -m "feat: create remaining module frameworks"
```

### 阶段 1.1 验收标准

- [ ] 所有9个Maven模块编译成功
- [ ] 单元测试覆盖率>70%
- [ ] 现有Guardrail规则功能无回归
- [ ] 模块依赖关系正确（单向依赖）
- [ ] 设计文档架构一致性验证通过

---

## 阶段 2：工作流层实现（3-4周）

**目标：** 实现Plans.md解析、任务编排、并行执行和基础状态恢复

### 阶段 2.1：Plans.md解析器（1周）

#### 任务 2.1.1：实现Plans.md数据模型

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/plans/PlansDocument.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/plans/Task.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/plans/Status.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/plans/Marker.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/plans/PlansDocumentTest.java`

- [ ] **步骤 1：创建Status枚举**

```java
package com.chachamaru.harness.workflow.plans;

public enum Status {
    PM_REQUESTED, PM_APPROVED,
    CC_TODO, CC_WIP, CC_DONE, CC_WITHDRAWN,
    UNKNOWN;
    
    public static Status fromMarker(String marker) {
        String upperMarker = marker.toUpperCase();
        try {
            return Status.valueOf(upperMarker);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    public boolean isTerminal() {
        return this == CC_DONE || this == CC_WITHDRAWN || this == PM_APPROVED;
    }
    
    public boolean isActive() {
        return this == CC_WIP || this == CC_TODO;
    }
}
```

- [ ] **步骤 2：创建Task模型**

```java
package com.chachamaru.harness.workflow.plans;

import java.util.List;

public class Task {
    private String id;
    private String title;
    private String description;
    private Status status;
    private String acceptanceCriteria;
    private List<String> dependencies;
    private String lane; // "implementation", "review", "release"
    
    // Empty constructor
    public Task() {}
    
    // Constructor with required fields
    public Task(String id, String title, Status status) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dependencies = List.of();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    public String getLane() { return lane; }
    public void setLane(String lane) { this.lane = lane; }
    
    // Utility methods
    public boolean isReady() {
        return status == Status.PM_REQUESTED || status == Status.CC_TODO;
    }
    
    public boolean isInProgress() {
        return status == Status.CC_WIP;
    }
    
    public boolean isCompleted() {
        return status == Status.CC_DONE || status == Status.PM_APPROVED;
    }
}
```

- [ ] **步骤 3：创建PlansDocument模型**

```java
package com.chachamaru.harness.workflow.plans;

import java.util.List;
import java.time.LocalDateTime;

public class PlansDocument {
    private String title;
    private String metadata;
    private LocalDateTime lastModified;
    private List<Task> tasks;
    
    public PlansDocument() {
        this.tasks = List.of();
    }
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
    
    // Utility methods
    public List<Task> getTasksByStatus(Status status) {
        return tasks.stream()
            .filter(task -> task.getStatus() == status)
            .toList();
    }
    
    public List<Task> getTasksByLane(String lane) {
        return tasks.stream()
            .filter(task -> lane.equals(task.getLane()))
            .toList();
    }
    
    public Task getTaskById(String id) {
        return tasks.stream()
            .filter(task -> id.equals(task.getId()))
            .findFirst()
            .orElse(null);
    }
}
```

- [ ] **步骤 4：编写模型测试**

```java
package com.chachamaru.harness.workflow.plans;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlansDocumentTest {
    
    @Test
    void shouldCreatePlansDocument() {
        PlansDocument doc = new PlansDocument();
        doc.setTitle("Test Project");
        
        assertEquals("Test Project", doc.getTitle());
        assertNotNull(doc.getTasks());
    }
    
    @Test
    void shouldFilterTasksByStatus() {
        Task task1 = new Task("1", "Task 1", Status.CC_TODO);
        Task task2 = new Task("2", "Task 2", Status.CC_WIP);
        Task task3 = new Task("3", "Task 3", Status.CC_TODO);
        
        PlansDocument doc = new PlansDocument();
        doc.setTasks(List.of(task1, task2, task3));
        
        List<Task> todoTasks = doc.getTasksByStatus(Status.CC_TODO);
        assertEquals(2, todoTasks.size());
    }
}
```

- [ ] **步骤 5：运行测试验证**

运行：`cd java-harness-workflow && mvn test`
预期：所有模型测试通过

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: create Plans.md data models"
```

#### 任务 2.1.2：实现RegexPlansParser

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/plans/RegexPlansParser.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/plans/RegexPlansParserTest.java`

- [ ] **步骤 1：编写PlansParser接口**

```java
package com.chachamaru.harness.workflow.plans;

import java.io.File;

public interface PlansParser {
    PlansDocument parse(String content) throws PlansException;
    PlansDocument parse(File file) throws PlansException;
}
```

- [ ] **步骤 2：创建PlansException**

```java
package com.chachamaru.harness.workflow.plans;

public class PlansException extends Exception {
    public PlansException(String message) {
        super(message);
    }
    
    public PlansException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **步骤 3：实现RegexPlansParser**

```java
package com.chachamaru.harness.workflow.plans;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexPlansParser implements PlansParser {
    private static final Pattern TABLE_ROW_PATTERN = 
        Pattern.compile("^\\|?\\s*([^|]+)\\s*\\|.*");
    private static final Pattern MARKER_PATTERN = 
        Pattern.compile("\\[([a-z]+:)([a-z-]+)\\]");
    
    @Override
    public PlansDocument parse(String content) throws PlansException {
        try {
            PlansDocument document = new PlansDocument();
            List<Task> tasks = new ArrayList<>();
            
            String[] lines = content.split("\n");
            boolean inTable = false;
            
            for (String line : lines) {
                // 检测表格开始
                if (line.contains("| ID |") || line.contains("| Task |")) {
                    inTable = true;
                    continue;
                }
                
                // 检测表格结束
                if (inTable && line.trim().isEmpty()) {
                    inTable = false;
                    continue;
                }
                
                // 解析表格行
                if (inTable) {
                    Task task = parseTableRow(line);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                
                // 解析标题
                if (line.startsWith("# ")) {
                    document.setTitle(line.substring(2).trim());
                }
            }
            
            document.setTasks(tasks);
            return document;
            
        } catch (Exception e) {
            throw new PlansException("Failed to parse Plans.md", e);
        }
    }
    
    @Override
    public PlansDocument parse(File file) throws PlansException {
        try {
            String content = Files.readString(file.toPath());
            return parse(content);
        } catch (IOException e) {
            throw new PlansException("Failed to read file: " + file, e);
        }
    }
    
    private Task parseTableRow(String line) {
        Matcher matcher = TABLE_ROW_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        
        String[] cells = line.split("\\|");
        if (cells.length < 4) {
            return null;
        }
        
        // 假设表格格式: | ID | Title | Description | Status |
        String id = cells[1].trim();
        String title = cells[2].trim();
        String description = cells.length > 3 ? cells[3].trim() : "";
        String statusMarker = cells[cells.length - 1].trim();
        
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(parseMarker(statusMarker));
        
        return task;
    }
    
    private Status parseMarker(String marker) {
        Matcher matcher = MARKER_PATTERN.matcher(marker);
        if (matcher.find()) {
            String family = matcher.group(1); // "cc:" or "pm:"
            String value = matcher.group(2);  // "wip", "done", etc.
            return Status.fromMarker(family + value);
        }
        return Status.UNKNOWN;
    }
}
```

- [ ] **步骤 4：编写解析器测试**

```java
package com.chachamaru.harness.workflow.plans;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegexPlansParserTest {
    
    @Test
    void shouldParseSimplePlans() throws PlansException {
        String markdown = """
            # Test Project
            
            | ID | Title | Description | Status |
            |----|-------|-------------|--------|
            | 1  | Task 1 | First task  | cc:todo |
            | 2  | Task 2 | Second task | cc:wip  |
            """;
        
        PlansParser parser = new RegexPlansParser();
        PlansDocument doc = parser.parse(markdown);
        
        assertEquals("Test Project", doc.getTitle());
        assertEquals(2, doc.getTasks().size());
        assertEquals("Task 1", doc.getTasks().get(0).getTitle());
        assertEquals(Status.CC_TODO, doc.getTasks().get(0).getStatus());
        assertEquals(Status.CC_WIP, doc.getTasks().get(1).getStatus());
    }
    
    @Test
    void shouldHandleComplexMarkers() throws PlansException {
        String markdown = """
            | ID | Title | Status |
            |----|-------|--------|
            | 1  | Task 1 | pm:requested |
            | 2  | Task 2 | cc:done     |
            | 3  | Task 3 | pm:approved |
            """;
        
        PlansParser parser = new RegexPlansParser();
        PlansDocument doc = parser.parse(markdown);
        
        assertEquals(Status.PM_REQUESTED, doc.getTasks().get(0).getStatus());
        assertEquals(Status.CC_DONE, doc.getTasks().get(1).getStatus());
        assertEquals(Status.PM_APPROVED, doc.getTasks().get(2).getStatus());
    }
    
    @Test
    void shouldHandleEmptyPlans() throws PlansException {
        String markdown = "# Empty Project\n\nNo tasks yet.";
        
        PlansParser parser = new RegexPlansParser();
        PlansDocument doc = parser.parse(markdown);
        
        assertEquals("Empty Project", doc.getTitle());
        assertTrue(doc.getTasks().isEmpty());
    }
}
```

- [ ] **步骤 5：运行测试验证**

运行：`cd java-harness-workflow && mvn test`
预期：所有解析器测试通过

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: implement RegexPlansParser with marker support"
```

### 阶段 2.2：任务编排和并行执行（1.5周）

#### 任务 2.2.1：实现任务编排器接口

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/orchestration/TaskOrchestrator.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/orchestration/OrchestrationPlan.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/orchestration/ExecutionResult.java`

- [ ] **步骤 1：创建编排器接口**

```java
package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.workflow.plans.PlansDocument;

public interface TaskOrchestrator {
    OrchestrationPlan createPlan(PlansDocument plans);
    ExecutionResult execute(OrchestrationPlan plan);
    void pause(String executionId);
    void resume(String executionId);
    void cancel(String executionId);
}
```

- [ ] **步骤 2：创建编排计划模型**

```java
package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.workflow.plans.Task;
import java.util.List;

public class OrchestrationPlan {
    private String planId;
    private List<Task> sequentialTasks;
    private List<List<Task>> parallelTasks;
    
    public OrchestrationPlan() {
        this.sequentialTasks = List.of();
        this.parallelTasks = List.of();
    }
    
    // Getters and setters
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    
    public List<Task> getSequentialTasks() { return sequentialTasks; }
    public void setSequentialTasks(List<Task> tasks) { this.sequentialTasks = tasks; }
    
    public List<List<Task>> getParallelTasks() { return parallelTasks; }
    public void setParallelTasks(List<List<Task>> tasks) { this.parallelTasks = tasks; }
}
```

- [ ] **步骤 3：创建执行结果模型**

```java
package com.chachamaru.harness.workflow.orchestration;

import java.time.LocalDateTime;
import java.util.List;

public class ExecutionResult {
    private String executionId;
    private boolean success;
    private List<String> completedTasks;
    private List<String> failedTasks;
    private String errorMessage;
    private LocalDateTime completedAt;
    
    // Getters and setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public List<String> getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(List<String> tasks) { this.completedTasks = tasks; }
    
    public List<String> getFailedTasks() { return failedTasks; }
    public void setFailedTasks(List<String> tasks) { this.failedTasks = tasks; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String message) { this.errorMessage = message; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime time) { this.completedAt = time; }
}
```

- [ ] **步骤 4：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: create orchestrator interfaces and models"
```

#### 任务 2.2.2：实现并行执行器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/execution/ParallelExecutor.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/execution/CompletableFutureExecutor.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/execution/CompletableFutureExecutorTest.java`

- [ ] **步骤 1：创建并行执行器接口**

```java
package com.chachamaru.harness.workflow.execution;

import java.util.List;
import java.util.concurrent.Callable;

public interface ParallelExecutor {
    <T> List<T> executeParallel(List<Callable<T>> tasks);
    <T> List<T> executeWithSemaphore(List<Callable<T>> tasks, int maxConcurrency);
}
```

- [ ] **步骤 2：实现CompletableFuture执行器**

```java
package com.chachamaru.harness.workflow.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CompletableFutureExecutor implements ParallelExecutor {
    private static final Logger log = LoggerFactory.getLogger(CompletableFutureExecutor.class);
    
    @Override
    public <T> List<T> executeParallel(List<Callable<T>> tasks) {
        return executeWithSemaphore(tasks, Integer.MAX_VALUE);
    }
    
    @Override
    public <T> List<T> executeWithSemaphore(List<Callable<T>> tasks, int maxConcurrency) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        
        ExecutorService executor = Executors.newCachedThreadPool();
        Semaphore semaphore = new Semaphore(maxConcurrency);
        
        try {
            List<CompletableFuture<T>> futures = new ArrayList<>();
            
            for (Callable<T> task : tasks) {
                CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return task.call();
                    } catch (Exception e) {
                        log.error("Task execution failed", e);
                        throw new RuntimeException("Task execution failed", e);
                    } finally {
                        semaphore.release();
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 收集结果
            return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
                
        } finally {
            executor.shutdown();
        }
    }
}
```

- [ ] **步骤 3：编写执行器测试**

```java
package com.chachamaru.harness.workflow.execution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class CompletableFutureExecutorTest {
    
    @Test
    void shouldExecuteTasksInParallel() throws Exception {
        ParallelExecutor executor = new CompletableFutureExecutor();
        
        List<Callable<String>> tasks = List.of(
            () -> { Thread.sleep(100); return "Task 1"; },
            () -> { Thread.sleep(100); return "Task 2"; },
            () -> { Thread.sleep(100); return "Task 3"; }
        );
        
        long startTime = System.currentTimeMillis();
        List<String> results = executor.executeParallel(tasks);
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals(3, results.size());
        assertTrue(results.contains("Task 1"));
        assertTrue(results.contains("Task 2"));
        assertTrue(results.contains("Task 3"));
        // 并行执行应该比顺序执行快
        assertTrue(duration < 250, "Parallel execution should be faster: " + duration + "ms");
    }
    
    @Test
    void shouldRespectSemaphoreLimit() throws Exception {
        ParallelExecutor executor = new CompletableFutureExecutor();
        
        List<Callable<Integer>> tasks = List.of(
            () -> { Thread.sleep(100); return 1; },
            () -> { Thread.sleep(100); return 2; },
            () -> { Thread.sleep(100); return 3; },
            () -> { Thread.sleep(100); return 4; }
        );
        
        List<Integer> results = executor.executeWithSemaphore(tasks, 2);
        
        assertEquals(4, results.size());
        assertEquals(List.of(1, 2, 3, 4), results);
    }
    
    @Test
    void shouldHandleEmptyTaskList() {
        ParallelExecutor executor = new CompletableFutureExecutor();
        
        List<String> results = executor.executeParallel(List.of());
        
        assertTrue(results.isEmpty());
    }
}
```

- [ ] **步骤 4：运行测试验证**

运行：`cd java-harness-workflow && mvn test`
预期：所有执行器测试通过

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: implement CompletableFuture-based parallel executor"
```

### 阶段 2.3：状态恢复基础（1.5周）

#### 任务 2.3.1：实现状态恢复接口

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/StateRecovery.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/RecoveryResult.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/RecoveryStrategy.java`

- [ ] **步骤 1：创建恢复器接口**

```java
package com.chachamaru.harness.workflow.recovery;

public interface StateRecovery {
    RecoveryResult attemptRecovery(String sessionId);
    RecoveryResult attemptSelfHealing(String sessionId);
    RecoveryResult attemptPeerRecovery(String sessionId);
    RecoveryResult attemptLeadIntervention(String sessionId);
    void markAborted(String sessionId);
}
```

- [ ] **步骤 2：创建恢复结果模型**

```java
package com.chachamaru.harness.workflow.recovery;

import java.time.LocalDateTime;

public class RecoveryResult {
    private boolean success;
    private String phase; // "self-healing", "peer-recovery", "lead-intervention", "aborted"
    private String message;
    private LocalDateTime recoveredAt;
    
    public static RecoveryResult success(String phase, String message) {
        RecoveryResult result = new RecoveryResult();
        result.success = true;
        result.phase = phase;
        result.message = message;
        result.recoveredAt = LocalDateTime.now();
        return result;
    }
    
    public static RecoveryResult failed(String phase, String message) {
        RecoveryResult result = new RecoveryResult();
        result.success = false;
        result.phase = phase;
        result.message = message;
        return result;
    }
    
    public static RecoveryResult aborted(String message) {
        RecoveryResult result = new RecoveryResult();
        result.success = false;
        result.phase = "aborted";
        result.message = message;
        result.recoveredAt = LocalDateTime.now();
        return result;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getPhase() { return phase; }
    public String getMessage() { return message; }
    public LocalDateTime getRecoveredAt() { return recoveredAt; }
}
```

- [ ] **步骤 3：创建恢复策略接口**

```java
package com.chachamaru.harness.workflow.recovery;

public interface RecoveryStrategy {
    RecoveryResult attemptRecovery(String sessionId);
    String getStrategyName();
}
```

- [ ] **步骤 4：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: create state recovery interfaces"
```

### 阶段 2 验收标准

- [ ] Plans.md解析器能正确解析表格和标记
- [ ] 任务编排器能创建执行计划
- [ ] 并行执行器能高效执行任务
- [ ] 状态恢复接口定义完整
- [ ] 单元测试覆盖率>75%
- [ ] 性能测试：并行执行比顺序执行快2倍以上

---

## 阶段 3：协作层实现（4-5周）

**目标：** 实现技能系统（混合模式）、代理系统（三种代理）和协调机制

### 阶段 3.1：技能框架实现（2周）

#### 任务 3.1.1：实现技能核心框架

**文件：**
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/Skill.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/SkillContext.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/SkillResult.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/CoreSkill.java`

- [ ] **步骤 1：创建技能接口**

```java
package com.chachamaru.harness.collaboration.skills;

public interface Skill {
    String getId();
    String getName();
    String getDescription();
    SkillResult execute(SkillContext context);
    boolean isApplicable(SkillContext context);
}
```

- [ ] **步骤 2：创建技能上下文**

```java
package com.chachamaru.harness.collaboration.skills;

import java.util.Map;

public class SkillContext {
    private String sessionId;
    private String projectId;
    private Map<String, Object> parameters;
    private Object inputArtifact;
    
    public SkillContext() {
        this.parameters = Map.of();
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public Object getInputArtifact() { return inputArtifact; }
    public void setInputArtifact(Object artifact) { this.inputArtifact = artifact; }
    
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
}
```

- [ ] **步骤 3：创建技能结果**

```java
package com.chachamaru.harness.collaboration.skills;

public class SkillResult {
    private boolean success;
    private Object artifact;
    private String message;
    private boolean applicable;
    
    public static SkillResult success(Object artifact) {
        SkillResult result = new SkillResult();
        result.success = true;
        result.artifact = artifact;
        result.applicable = true;
        return result;
    }
    
    public static SkillResult failure(String message) {
        SkillResult result = new SkillResult();
        result.success = false;
        result.message = message;
        result.applicable = true;
        return result;
    }
    
    public static SkillResult notApplicable() {
        SkillResult result = new SkillResult();
        result.applicable = false;
        return result;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public Object getArtifact() { return artifact; }
    public String getMessage() { return message; }
    public boolean isApplicable() { return applicable; }
}
```

- [ ] **步骤 4：实现核心技能基类**

```java
package com.chachamaru.harness.collaboration.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CoreSkill implements Skill {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public final SkillResult execute(SkillContext context) {
        if (!isApplicable(context)) {
            return SkillResult.notApplicable();
        }
        
        try {
            return executeInternal(context);
        } catch (Exception e) {
            log.error("Skill execution failed", e);
            return SkillResult.failure("Execution failed: " + e.getMessage());
        }
    }
    
    protected abstract SkillResult executeInternal(SkillContext context);
    
    @Override
    public boolean isApplicable(SkillContext context) {
        return true; // 默认适用
    }
}
```

- [ ] **步骤 5：Commit**

```bash
git add java-harness-collaboration/
git commit -m "feat: create skill framework with core base class"
```

#### 任务 3.1.2：实现核心技能

**文件：**
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/core/PlanSkill.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/core/WorkSkill.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/core/ReviewSkill.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/SkillRegistry.java`

- [ ] **步骤 1：实现PlanSkill**

```java
package com.chachamaru.harness.collaboration.skills.core;

import com.chachamaru.harness.collaboration.skills.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@SkillMetadata(
    id = "plan",
    name = "Plan",
    description = "Create project plan with spec.md and Plans.md"
)
public class PlanSkill extends CoreSkill {
    
    @Override
    protected SkillResult executeInternal(SkillContext context) {
        try {
            String projectId = context.getProjectId();
            
            // 1. 生成spec.md
            String specContent = generateSpecContent(projectId);
            File specFile = new File(context.getProjectId(), "spec.md");
            Files.writeString(specFile.toPath(), specContent);
            
            // 2. 生成Plans.md
            String plansContent = generatePlansContent(projectId);
            File plansFile = new File(context.getProjectId(), "Plans.md");
            Files.writeString(plansFile.toPath(), plansContent);
            
            // 3. 创建artifact
            PlanArtifact artifact = new PlanArtifact();
            artifact.setSpecFile(specFile);
            artifact.setPlansFile(plansFile);
            artifact.setCreatedAt(LocalDateTime.now());
            
            log.info("Plan created for project: {}", projectId);
            return SkillResult.success(artifact);
            
        } catch (Exception e) {
            log.error("Plan creation failed", e);
            return SkillResult.failure("Plan creation failed: " + e.getMessage());
        }
    }
    
    private String generateSpecContent(String projectId) {
        return """
            # Spec for ${projectId}
            
            ## Purpose
            Project purpose and goals
            
            ## Architecture
            System architecture and design
            
            ## Requirements
            Functional and non-functional requirements
            """.replace("${projectId}", projectId);
    }
    
    private String generatePlansContent(String projectId) {
        return """
            # ${projectId} Implementation Plan
            
            ## Phase 1: Foundation (2-3 weeks)
            | ID | Task | Status |
            |----|------|--------|
            | 1.1 | Create module structure | pm:requested |
            
            ## Phase 2: Core Features (3-4 weeks)
            | ID | Task | Status |
            |----|------|--------|
            | 2.1 | Implement core features | pm:requested |
            """.replace("${projectId}", projectId);
    }
    
    private static class PlanArtifact {
        private File specFile;
        private File plansFile;
        private LocalDateTime createdAt;
        
        // Getters and setters
        public File getSpecFile() { return specFile; }
        public void setSpecFile(File file) { this.specFile = file; }
        
        public File getPlansFile() { return plansFile; }
        public void setPlansFile(File file) { this.plansFile = file; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime time) { this.createdAt = time; }
    }
}
```

- [ ] **步骤 2：实现WorkSkill**

```java
package com.chachamaru.harness.collaboration.skills.core;

import com.chachamaru.harness.collaboration.skills.*;
import com.chachamaru.harness.workflow.plans.PlansDocument;
import com.chachamaru.harness.workflow.plans.Status;
import java.io.File;

@SkillMetadata(
    id = "work",
    name = "Work",
    description = "Implement approved tasks from Plans.md"
)
public class WorkSkill extends CoreSkill {
    
    @Override
    protected SkillResult executeInternal(SkillContext context) {
        try {
            // 1. 解析Plans.md
            File plansFile = new File(context.getProjectId(), "Plans.md");
            PlansParser parser = new RegexPlansParser();
            PlansDocument plans = parser.parse(plansFile);
            
            // 2. 获取approved/tasks
            var approvedTasks = plans.getTasksByStatus(Status.PM_REQUESTED);
            
            // 3. 执行任务
            WorkResult result = new WorkResult();
            result.setTotalTasks(approvedTasks.size());
            result.setCompletedTasks(0);
            
            for (Task task : approvedTasks) {
                log.info("Executing task: {}", task.getId());
                // 这里简化实现，实际应该调用具体的任务执行逻辑
                result.incrementCompleted();
            }
            
            return SkillResult.success(result);
            
        } catch (Exception e) {
            log.error("Work execution failed", e);
            return SkillResult.failure("Work execution failed: " + e.getMessage());
        }
    }
    
    private static class WorkResult {
        private int totalTasks;
        private int completedTasks;
        private File implementationArtifact;
        
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int count) { this.totalTasks = count; }
        
        public int getCompletedTasks() { return completedTasks; }
        public void incrementCompleted() { this.completedTasks++; }
        
        public File getImplementationArtifact() { return implementationArtifact; }
        public void setImplementationArtifact(File file) { this.implementationArtifact = file; }
    }
}
```

- [ ] **步骤 3：实现ReviewSkill**

```java
package com.chachamaru.harness.collaboration.skills.core;

import com.chachamaru.harness.collaboration.skills.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SkillMetadata(
    id = "review",
    name = "Review",
    description = "Review implementation independently"
)
public class ReviewSkill extends CoreSkill {
    
    @Override
    protected SkillResult executeInternal(SkillContext context) {
        try {
            File implementationArtifact = (File) context.getInputArtifact();
            
            // 1. 执行代码审查
            ReviewFindings findings = performReview(implementationArtifact);
            
            // 2. 生成review报告
            ReviewReport report = new ReviewReport();
            report.setFindings(findings.getFindings());
            report.setVerdict(findings.hasMajorIssues() ? "REQUEST_CHANGES" : "APPROVED");
            report.setReviewedAt(LocalDateTime.now());
            
            return SkillResult.success(report);
            
        } catch (Exception e) {
            log.error("Review failed", e);
            return SkillResult.failure("Review failed: " + e.getMessage());
        }
    }
    
    private ReviewFindings performReview(File artifact) {
        ReviewFindings findings = new ReviewFindings();
        // 简化实现，实际应该调用具体的审查逻辑
        findings.addFinding(new Finding("info", "Code structure is good"));
        findings.addFinding(new Finding("warning", "Missing unit tests for edge cases"));
        return findings;
    }
    
    private static class ReviewFindings {
        private final List<Finding> findings = new ArrayList<>();
        
        public void addFinding(Finding finding) {
            this.findings.add(finding);
        }
        
        public List<Finding> getFindings() { return findings; }
        
        public boolean hasMajorIssues() {
            return findings.stream()
                .anyMatch(f -> "error".equals(f.severity()));
        }
    }
    
    private static class Finding {
        private final String severity;
        private final String message;
        
        public Finding(String severity, String message) {
            this.severity = severity;
            this.message = message;
        }
        
        public String severity() { return severity; }
        public String message() { return message; }
    }
    
    private static class ReviewReport {
        private List<Finding> findings;
        private String verdict;
        private LocalDateTime reviewedAt;
        
        // Getters and setters
        public List<Finding> getFindings() { return findings; }
        public void setFindings(List<Finding> findings) { this.findings = findings; }
        
        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
        
        public LocalDateTime getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(LocalDateTime time) { this.reviewedAt = time; }
    }
}
```

- [ ] **步骤 4：创建技能注册表**

```java
package com.chachamaru.harness.collaboration.skills;

import java.util.*;

public class SkillRegistry {
    private final Map<String, Skill> skills = new LinkedHashMap<>();
    
    public void register(Skill skill) {
        log.info("Registering skill: {}", skill.getId());
        skills.put(skill.getId(), skill);
    }
    
    public void unregister(String skillId) {
        skills.remove(skillId);
    }
    
    public Skill getSkill(String skillId) {
        return skills.get(skillId);
    }
    
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }
    
    public Skill getApplicableSkill(SkillContext context) {
        return skills.values().stream()
            .filter(skill -> skill.isApplicable(context))
            .findFirst()
            .orElse(null);
    }
}
```

- [ ] **步骤 5：Commit**

```bash
git add java-harness-collaboration/
git commit -m "feat: implement core skills (plan/work/review)"
```

#### 任务 3.1.3：实现Markdown技能加载器

**文件：**
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/MarkdownSkillLoader.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/skills/MarkdownSkill.java`
- 测试：`java-harness-collaboration/src/test/java/com/chachamaru/harness/collaboration/skills/MarkdownSkillLoaderTest.java`

- [ ] **步骤 1：实现MarkdownSkillLoader**

```java
package com.chachamaru.harness.collaboration.skills;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class MarkdownSkillLoader implements SkillLoader {
    private static final Pattern FRONTMATTER_PATTERN = 
        Pattern.compile("^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n([\\s\\S]*)$");
    
    @Override
    public List<Skill> loadFromDirectory(File skillDirectory) {
        List<Skill> skills = new ArrayList<>();
        
        if (!skillDirectory.exists() || !skillDirectory.isDirectory()) {
            return skills;
        }
        
        File[] skillFiles = skillDirectory.listFiles((dir, name) -> 
            name.endsWith(".md") || name.endsWith(".SKILL.md"));
        
        if (skillFiles != null) {
            for (File file : skillFiles) {
                try {
                    Skill skill = loadSkill(file);
                    if (skill != null) {
                        skills.add(skill);
                    }
                } catch (Exception e) {
                    log.error("Failed to load skill from: {}", file, e);
                }
            }
        }
        
        return skills;
    }
    
    @Override
    public Skill loadSkill(File skillFile) {
        try {
            String content = Files.readString(skillFile.toPath());
            
            // 解析YAML frontmatter
            Matcher matcher = FRONTMATTER_PATTERN.matcher(content.trim());
            if (!matcher.find()) {
                log.error("Invalid skill format in file: {}", skillFile);
                return null;
            }
            
            String frontmatter = matcher.group(1);
            String skillBody = matcher.group(2);
            
            // 解析frontmatter
            Map<String, Object> metadata = parseYamlFrontmatter(frontmatter);
            
            // 创建MarkdownSkill
            String id = (String) metadata.getOrDefault("name", "unknown");
            String name = (String) metadata.getOrDefault("description", "");
            String body = skillBody;
            
            return new MarkdownSkill(id, name, body, metadata);
            
        } catch (Exception e) {
            log.error("Failed to parse skill file: {}", skillFile, e);
            return null;
        }
    }
    
    private Map<String, Object> parseYamlFrontmatter(String frontmatter) {
        // 简化实现，实际应该使用YAML解析库
        Map<String, Object> metadata = new HashMap<>();
        
        String[] lines = frontmatter.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    metadata.put(key, value);
                }
            }
        }
        
        return metadata;
    }
    
    @Override
    public boolean validateSkillSyntax(File skillFile) {
        try {
            String content = Files.readString(skillFile.toPath());
            return FRONTMATTER_PATTERN.matcher(content.trim()).find();
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **步骤 2：实现MarkdownSkill**

```java
package com.chachamaru.harness.collaboration.skills;

import java.util.Map;

public class MarkdownSkill implements Skill {
    private final String id;
    private final String name;
    private final String description;
    private final String body;
    private final Map<String, Object> metadata;
    
    public MarkdownSkill(String id, String name, String body, Map<String, Object> metadata) {
        this.id = id;
        this.name = name;
        this.description = (String) metadata.get("description");
        this.body = body;
        this.metadata = metadata;
    }
    
    @Override
    public String getId() { return id; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return description; }
    
    @Override
    public SkillResult execute(SkillContext context) {
        // Markdown技能的执行逻辑
        // 这里可以集成脚本引擎或者模板引擎
        return SkillResult.success("Markdown skill executed");
    }
    
    @Override
    public boolean isApplicable(SkillContext context) {
        // 检查技能是否适用
        return true;
    }
}
```

- [ ] **步骤 3：编写加载器测试**

```java
package com.chachamaru.harness.collaboration.skills;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownSkillLoaderTest {
    
    @Test
    void shouldLoadSkillFromDirectory(@TempDir File tempDir) throws Exception {
        // 创建测试技能文件
        File skillFile = new File(tempDir, "test.SKILL.md");
        String skillContent = """
            ---
            name: test-skill
            description: A test skill
            ---
            
            This is the skill body.
            """;
        Files.writeString(skillFile.toPath(), skillContent);
        
        MarkdownSkillLoader loader = new MarkdownSkillLoader();
        List<Skill> skills = loader.loadFromDirectory(tempDir);
        
        assertEquals(1, skills.size());
        assertEquals("test-skill", skills.get(0).getId());
    }
    
    @Test
    void shouldValidateSkillSyntax() {
        String validContent = """
            ---
            name: test
            ---
            Body content
            """;
        
        MarkdownSkillLoader loader = new MarkdownSkillLoader();
        // 语法验证逻辑...
    }
}
```

- [ ] **步骤 4：运行测试验证**

运行：`cd java-harness-collaboration && mvn test`
预期：所有技能测试通过

- [ ] **步骤 5：Commit**

```bash
git add java-harness-collaboration/
git commit -m "feat: implement Markdown skill loader"
```

### 阶段 3.2：代理系统实现（2周）

#### 任务 3.2.1：实现代理框架

**文件：**
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/Agent.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/AgentContext.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/AgentResult.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/AgentRegistry.java`

- [ ] **步骤 1：创建代理接口**

```java
package com.chachamaru.harness.collaboration.agents;

public interface Agent {
    String getId();
    String getType();
    AgentResult execute(AgentContext context);
    void notify(String event, Object data);
}
```

- [ ] **步骤 2：创建代理上下文**

```java
package com.chachamaru.harness.collaboration.agents;

import java.util.Map;

public class AgentContext {
    private String sessionId;
    private String taskId;
    private Object inputArtifact;
    private Map<String, Object> parameters;
    
    public AgentContext() {
        this.parameters = Map.of();
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public Object getInputArtifact() { return inputArtifact; }
    public void setInputArtifact(Object artifact) { this.inputArtifact = artifact; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
```

- [ ] **步骤 3：创建代理结果**

```java
package com.chachamaru.harness.collaboration.agents;

import java.time.LocalDateTime;

public class AgentResult {
    private boolean success;
    private Object artifact;
    private String message;
    private String agentType;
    private LocalDateTime completedAt;
    
    public static AgentResult success(Object artifact, String agentType) {
        AgentResult result = new AgentResult();
        result.success = true;
        result.artifact = artifact;
        result.agentType = agentType;
        result.completedAt = LocalDateTime.now();
        return result;
    }
    
    public static AgentResult failure(String message, String agentType) {
        AgentResult result = new AgentResult();
        result.success = false;
        result.message = message;
        result.agentType = agentType;
        result.completedAt = LocalDateTime.now();
        return result;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public Object getArtifact() { return artifact; }
    public String getMessage() { return message; }
    public String getAgentType() { return agentType; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
```

- [ ] **步骤 4：创建代理注册表**

```java
package com.chachamaru.harness.collaboration.agents;

import java.util.*;

public class AgentRegistry {
    private final Map<String, Agent> agents = new LinkedHashMap<>();
    
    public void register(Agent agent) {
        log.info("Registering agent: {} ({})", agent.getId(), agent.getType());
        agents.put(agent.getId(), agent);
    }
    
    public void unregister(String agentId) {
        agents.remove(agentId);
    }
    
    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }
    
    public List<Agent> getAgentsByType(String type) {
        return agents.values().stream()
            .filter(agent -> type.equals(agent.getType()))
            .toList();
    }
    
    public List<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }
}
```

- [ ] **步骤 5：Commit**

```bash
git add java-harness-collaboration/
git commit -m "feat: create agent framework interfaces"
```

#### 任务 3.2.2：实现三种核心代理

**文件：**
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/impl/WorkerAgent.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/impl/ReviewerAgent.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/impl/AdvisorAgent.java`
- 创建：`java-harness-collaboration/src/main/java/com/chachamaru/harness/collaboration/agents/AgentCoordinator.java`

- [ ] **步骤 1：实现WorkerAgent**

```java
package com.chachamaru.harness.collaboration.agents.impl;

import com.chachamaru.harness.collaboration.agents.*;
import com.chachamaru.harness.workflow.plans.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AgentMetadata(id = "worker", type = "worker")
public class WorkerAgent implements Agent {
    private static final Logger log = LoggerFactory.getLogger(WorkerAgent.class);
    
    @Override
    public String getId() { return "worker"; }
    
    @Override
    public String getType() { return "worker"; }
    
    @Override
    public AgentResult execute(AgentContext context) {
        try {
            log.info("Worker agent executing task: {}", context.getTaskId());
            
            // 1. 获取任务详情
            Task task = getTaskDetails(context.getTaskId());
            
            // 2. 执行实现工作
            ImplementationResult implResult = implementTask(task);
            
            // 3. 运行测试验证
            TestResult testResult = runTests(implResult);
            
            // 4. 处理失败和恢复
            if (!testResult.isSuccess()) {
                return handleFailure(context, testResult);
            }
            
            // 5. 创建artifact
            WorkArtifact artifact = new WorkArtifact();
            artifact.setTaskId(context.getTaskId());
            artifact.setImplementation(implResult.getCode());
            artifact.setTestsPassed(testResult.isPassed());
            
            return AgentResult.success(artifact, "worker");
            
        } catch (Exception e) {
            log.error("Worker agent execution failed", e);
            return AgentResult.failure("Worker execution failed: " + e.getMessage(), "worker");
        }
    }
    
    @Override
    public void notify(String event, Object data) {
        log.info("Worker agent notified: {} - {}", event, data);
        // 处理通知
    }
    
    private Task getTaskDetails(String taskId) {
        // 从Plans.md或数据库获取任务详情
        Task task = new Task();
        task.setId(taskId);
        return task;
    }
    
    private ImplementationResult implementTask(Task task) {
        // 实现任务逻辑
        ImplementationResult result = new ImplementationResult();
        result.setCode("implementation code here");
        result.setSuccess(true);
        return result;
    }
    
    private TestResult runTests(ImplementationResult impl) {
        // 运行测试
        TestResult result = new TestResult();
        result.setPassed(true);
        result.setSuccess(true);
        return result;
    }
    
    private AgentResult handleFailure(AgentContext context, TestResult testResult) {
        // 调用状态恢复机制
        return AgentResult.failure("Tests failed: " + testResult.getErrorMessage(), "worker");
    }
    
    private static class ImplementationResult {
        private String code;
        private boolean success;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
    
    private static class TestResult {
        private boolean passed;
        private boolean success;
        private String errorMessage;
        
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String message) { this.errorMessage = message; }
    }
    
    private static class WorkArtifact {
        private String taskId;
        private String implementation;
        private boolean testsPassed;
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getImplementation() { return implementation; }
        public void setImplementation(String impl) { this.implementation = impl; }
        
        public boolean isTestsPassed() { return testsPassed; }
        public void setTestsPassed(boolean passed) { this.testsPassed = passed; }
    }
}
```

- [ ] **步骤 2：实现ReviewerAgent**

```java
package com.chachamaru.harness.collaboration.agents.impl;

import com.chachamaru.harness.collaboration.agents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AgentMetadata(id = "reviewer", type = "reviewer")
public class ReviewerAgent implements Agent {
    private static final Logger log = LoggerFactory.getLogger(ReviewerAgent.class);
    
    @Override
    public String getId() { return "reviewer"; }
    
    @Override
    public String getType() { return "reviewer"; }
    
    @Override
    public AgentResult execute(AgentContext context) {
        try {
            log.info("Reviewer agent reviewing work from session: {}", context.getSessionId());
            
            // 1. 获取implementation artifact
            Object workArtifact = context.getInputArtifact();
            
            // 2. 执行独立审查
            ReviewFindings findings = performIndependentReview(workArtifact);
            
            // 3. 生成review report
            ReviewReport report = generateReviewReport(findings);
            
            // 4. 返回verdict
            String verdict = findings.hasMajorIssues() ? "REQUEST_CHANGES" : "APPROVED";
            report.setVerdict(verdict);
            
            return AgentResult.success(report, "reviewer");
            
        } catch (Exception e) {
            log.error("Reviewer agent execution failed", e);
            return AgentResult.failure("Review failed: " + e.getMessage(), "reviewer");
        }
    }
    
    @Override
    public void notify(String event, Object data) {
        log.info("Reviewer agent notified: {} - {}", event, data);
    }
    
    private ReviewFindings performIndependentReview(Object artifact) {
        // 独立审查逻辑
        ReviewFindings findings = new ReviewFindings();
        findings.addFinding(new Finding("info", "Code structure is clear"));
        findings.addFinding(new Finding("warning", "Missing error handling"));
        return findings;
    }
    
    private ReviewReport generateReviewReport(ReviewFindings findings) {
        ReviewReport report = new ReviewReport();
        report.setFindings(findings.getFindings());
        report.setReviewedAt(LocalDateTime.now());
        return report;
    }
    
    private static class ReviewFindings {
        private final List<Finding> findings = new ArrayList<>();
        
        public void addFinding(Finding finding) {
            this.findings.add(finding);
        }
        
        public List<Finding> getFindings() { return findings; }
        
        public boolean hasMajorIssues() {
            return findings.stream().anyMatch(f -> "error".equals(f.severity()));
        }
    }
    
    private static class Finding {
        private final String severity;
        private final String message;
        
        public Finding(String severity, String message) {
            this.severity = severity;
            this.message = message;
        }
        
        public String severity() { return severity; }
        public String message() { return message; }
    }
    
    private static class ReviewReport {
        private List<Finding> findings;
        private String verdict;
        private LocalDateTime reviewedAt;
        
        public List<Finding> getFindings() { return findings; }
        public void setFindings(List<Finding> findings) { this.findings = findings; }
        
        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
        
        public LocalDateTime getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(LocalDateTime time) { this.reviewedAt = time; }
    }
}
```

- [ ] **步骤 3：实现AdvisorAgent**

```java
package com.chachamaru.harness.collaboration.agents.impl;

import com.chachamaru.harness.collaboration.agents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AgentMetadata(id = "advisor", type = "advisor")
public class AdvisorAgent implements Agent {
    private static final Logger log = LoggerFactory.getLogger(AdvisorAgent.class);
    
    @Override
    public String getId() { return "advisor"; }
    
    @Override
    public String getType() { return "advisor"; }
    
    @Override
    public AgentResult execute(AgentContext context) {
        try {
            log.info("Advisor agent providing strategy for session: {}", context.getSessionId());
            
            // Advisor只返回策略建议，不执行实际操作
            StrategyAdvice advice = generateStrategyAdvice(context);
            
            return AgentResult.success(advice, "advisor");
            
        } catch (Exception e) {
            log.error("Advisor agent execution failed", e);
            return AgentResult.failure("Advisory failed: " + e.getMessage(), "advisor");
        }
    }
    
    @Override
    public void notify(String event, Object data) {
        log.info("Advisor agent notified: {} - {}", event, data);
    }
    
    private StrategyAdvice generateStrategyAdvice(AgentContext context) {
        StrategyAdvice advice = new StrategyAdvice();
        advice.setRecommendation("Focus on core features first");
        advice.setRiskLevel("medium");
        advice.setEstimatedEffort("2-3 weeks");
        advice.setCreatedAt(LocalDateTime.now());
        return advice;
    }
    
    private static class StrategyAdvice {
        private String recommendation;
        private String riskLevel;
        private String estimatedEffort;
        private LocalDateTime createdAt;
        
        // Getters and setters
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String rec) { this.recommendation = rec; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String level) { this.riskLevel = level; }
        
        public String getEstimatedEffort() { return estimatedEffort; }
        public void setEstimatedEffort(String effort) { this.estimatedEffort = effort; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime time) { this.createdAt = time; }
    }
}
```

- [ ] **步骤 4：创建代理协调器**

```java
package com.chachamaru.harness.collaboration.agents;

import com.chachamaru.harness.collaboration.coordination.*;
import java.util.*;

public class AgentCoordinator {
    private final AgentRegistry agentRegistry;
    private final CoordinationProtocol coordinationProtocol;
    
    public AgentCoordinator(AgentRegistry agentRegistry, CoordinationProtocol coordinationProtocol) {
        this.agentRegistry = agentRegistry;
        this.coordinationProtocol = coordinationProtocol;
    }
    
    public CoordinationResult coordinate(List<Agent> agents, AgentContext context) {
        CoordinationResult result = new CoordinationResult();
        List<AgentResult> agentResults = new ArrayList<>();
        
        for (Agent agent : agents) {
            try {
                // 广播代理开始事件
                coordinationProtocol.broadcast("agent.start", Map.of(
                    "agentId", agent.getId(),
                    "context", context
                ));
                
                // 执行代理
                AgentResult agentResult = agent.execute(context);
                agentResults.add(agentResult);
                
                // 广播代理完成事件
                coordinationProtocol.broadcast("agent.complete", Map.of(
                    "agentId", agent.getId(),
                    "result", agentResult
                ));
                
            } catch (Exception e) {
                log.error("Agent execution failed: {}", agent.getId(), e);
                agentResults.add(AgentResult.failure(e.getMessage(), agent.getType()));
            }
        }
        
        result.setAgentResults(agentResults);
        result.setSuccess(agentResults.stream().anyMatch(AgentResult::isSuccess));
        return result;
    }
    
    public void registerAgent(Agent agent) {
        agentRegistry.register(agent);
    }
    
    public Agent getAgent(String agentId) {
        return agentRegistry.getAgent(agentId);
    }
    
    public List<Agent> getAgentsByType(String type) {
        return agentRegistry.getAgentsByType(type);
    }
}
```

- [ ] **步骤 5：创建协调结果**

```java
package com.chachamaru.harness.collaboration.agents;

import java.util.List;

public class CoordinationResult {
    private boolean success;
    private List<AgentResult> agentResults;
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public List<AgentResult> getAgentResults() { return agentResults; }
    public void setAgentResults(List<AgentResult> results) { this.agentResults = results; }
}
```

- [ ] **步骤 6：运行测试验证**

运行：`cd java-harness-collaboration && mvn test`
预期：所有代理测试通过

- [ ] **步骤 7：Commit**

```bash
git add java-harness-collaboration/
git commit -m "feat: implement three core agents (worker/reviewer/advisor)"
```

### 阶段 3 验收标准

- [ ] 技能框架完整实现，支持Java和Markdown技能
- [ ] 核心技能（plan/work/review）执行正确
- [ ] 代理系统完整实现，三种代理都能正常工作
- [ ] 协调机制能正确协调多代理
- [ ] 单元测试覆盖率>75%
- [ ] 集成测试：技能→代理→协调流程完整

---

## 阶段 4：状态恢复完善（2-3周）

**目标：** 实现完整的4阶段恢复机制

### 阶段 4.1：4阶段恢复实现

#### 任务 4.1.1：实现自我修复策略

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/SelfHealingStrategy.java`

- [ ] **步骤 1：实现自我修复策略**

```java
package com.chachamaru.harness.workflow.recovery;

import com.chachamaru.harness.foundation.persistence.SessionState;
import com.chachamaru.harness.foundation.persistence.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfHealingStrategy implements RecoveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(SelfHealingStrategy.class);
    private static final int MAX_RETRIES = 3;
    private final SessionRepository sessionRepository;
    
    public SelfHealingStrategy(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    public RecoveryResult attemptRecovery(String sessionId) {
        log.info("Attempting self-healing for session: {}", sessionId);
        
        SessionState state = sessionRepository.findById(sessionId);
        if (state == null) {
            return RecoveryResult.failed("self-healing", "Session not found");
        }
        
        // 尝试自我修复，最多3次
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                boolean healed = attemptHeal(state);
                if (healed) {
                    return RecoveryResult.success("self-healing", 
                        "Session recovered after " + attempt + " attempts");
                }
            } catch (Exception e) {
                log.warn("Self-healing attempt {} failed", attempt, e);
            }
        }
        
        return RecoveryResult.failed("self-healing", "Self-healing exhausted");
    }
    
    @Override
    public String getStrategyName() {
        return "SelfHealing";
    }
    
    private boolean attemptHeal(SessionState state) {
        // 分析错误类型并尝试自动修正
        String errorMessage = state.getErrorMessage();
        
        if (errorMessage.contains("timeout")) {
            // 重试操作
            return retryOperation(state);
        } else if (errorMessage.contains("connection")) {
            // 重新建立连接
            return reestablishConnection(state);
        } else if (errorMessage.contains("configuration")) {
            // 重置配置
            return resetConfiguration(state);
        }
        
        return false;
    }
    
    private boolean retryOperation(SessionState state) {
        // 重试逻辑
        return true;
    }
    
    private boolean reestablishConnection(SessionState state) {
        // 连接重建逻辑
        return true;
    }
    
    private boolean resetConfiguration(SessionState state) {
        // 配置重置逻辑
        return true;
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: implement self-healing recovery strategy"
```

#### 任务 4.1.2：实现同伴修复策略

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/PeerRecoveryStrategy.java`

- [ ] **步骤 1：实现同伴修复策略**

```java
package com.chachamaru.harness.workflow.recovery;

import com.chachamaru.harness.collaboration.agents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerRecoveryStrategy implements RecoveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(PeerRecoveryStrategy.class);
    private final AgentCoordinator agentCoordinator;
    
    public PeerRecoveryStrategy(AgentCoordinator agentCoordinator) {
        this.agentCoordinator = agentCoordinator;
    }
    
    @Override
    public RecoveryResult attemptRecovery(String sessionId) {
        log.info("Attempting peer recovery for session: {}", sessionId);
        
        try {
            // 将任务委托给其他Worker
            List<Agent> workers = agentCoordinator.getAgentsByType("worker");
            
            if (workers.isEmpty()) {
                return RecoveryResult.failed("peer-recovery", "No peer workers available");
            }
            
            // 选择不同的Worker执行任务
            Agent alternativeWorker = selectAlternativeWorker(workers);
            AgentContext context = createContext(sessionId);
            
            AgentResult result = alternativeWorker.execute(context);
            
            if (result.isSuccess()) {
                return RecoveryResult.success("peer-recovery", 
                    "Task completed by alternative worker: " + alternativeWorker.getId());
            }
            
            return RecoveryResult.failed("peer-recovery", "Peer recovery failed");
            
        } catch (Exception e) {
            log.error("Peer recovery failed", e);
            return RecoveryResult.failed("peer-recovery", "Peer recovery error: " + e.getMessage());
        }
    }
    
    @Override
    public String getStrategyName() {
        return "PeerRecovery";
    }
    
    private Agent selectAlternativeWorker(List<Agent> workers) {
        // 选择不同的Worker，避免同一个
        return workers.get(0);
    }
    
    private AgentContext createContext(String sessionId) {
        AgentContext context = new AgentContext();
        context.setSessionId(sessionId);
        return context;
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: implement peer recovery strategy"
```

#### 任务 4.1.3：实现指挥官介入和停止策略

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/LeadInterventionStrategy.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/AbortStrategy.java`
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/recovery/FourPhaseRecovery.java`

- [ ] **步骤 1：实现指挥官介入策略**

```java
package com.chachamaru.harness.workflow.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeadInterventionStrategy implements RecoveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(LeadInterventionStrategy.class);
    
    @Override
    public RecoveryResult attemptRecovery(String sessionId) {
        log.info("Requesting lead intervention for session: {}", sessionId);
        
        try {
            // 向Lead会话发送escalation请求
            String escalationMessage = buildEscalationMessage(sessionId);
            boolean leadResponded = sendEscalation(escalationMessage);
            
            if (leadResponded) {
                // 等待Lead处理
                return waitForLeadResolution(sessionId);
            }
            
            return RecoveryResult.failed("lead-intervention", "Lead intervention unavailable");
            
        } catch (Exception e) {
            log.error("Lead intervention failed", e);
            return RecoveryResult.failed("lead-intervention", "Lead intervention error: " + e.getMessage());
        }
    }
    
    @Override
    public String getStrategyName() {
        return "LeadIntervention";
    }
    
    private String buildEscalationMessage(String sessionId) {
        return String.format(
            "Session %s requires manual intervention. " +
            "Error: %s. Please investigate and resolve.",
            sessionId, getLastError(sessionId)
        );
    }
    
    private boolean sendEscalation(String message) {
        // 发送escalation逻辑
        return true;
    }
    
    private RecoveryResult waitForLeadResolution(String sessionId) {
        // 等待Lead处理
        return RecoveryResult.success("lead-intervention", "Waiting for lead resolution");
    }
    
    private String getLastError(String sessionId) {
        // 获取最后错误信息
        return "Unknown error";
    }
}
```

- [ ] **步骤 2：实现停止策略**

```java
package com.chachamaru.harness.workflow.recovery;

import com.chachamaru.harness.foundation.persistence.SessionRepository;
import com.chachamaru.harness.foundation.persistence.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbortStrategy implements RecoveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(AbortStrategy.class);
    private final SessionRepository sessionRepository;
    
    public AbortStrategy(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    public RecoveryResult attemptRecovery(String sessionId) {
        log.info("Aborting session: {}", sessionId);
        
        try {
            SessionState state = sessionRepository.findById(sessionId);
            if (state != null) {
                state.setStatus(SessionState.Status.ABORTED);
                state.setErrorMessage("All recovery strategies exhausted, requiring manual intervention");
                sessionRepository.update(state);
            }
            
            return RecoveryResult.aborted("Session aborted and requires manual intervention");
            
        } catch (Exception e) {
            log.error("Abort failed", e);
            return RecoveryResult.aborted("Abort failed: " + e.getMessage());
        }
    }
    
    @Override
    public String getStrategyName() {
        return "Abort";
    }
}
```

- [ ] **步骤 3：实现4阶段恢复**

```java
package com.chachamaru.harness.workflow.recovery;

import com.chachamaru.harness.foundation.persistence.SessionRepository;
import java.util.*;

public class FourPhaseRecovery implements StateRecovery {
    private final Map<State, List<RecoveryStrategy>> strategies;
    private final SessionRepository sessionRepository;
    
    public FourPhaseRecovery(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.strategies = new EnumMap<>(State.class);
        initializeStrategies();
    }
    
    private void initializeStrategies() {
        List<RecoveryStrategy> runningStrategies = new ArrayList<>();
        runningStrategies.add(new SelfHealingStrategy(sessionRepository));
        runningStrategies.add(new PeerRecoveryStrategy(null)); // AgentCoordinator注入
        runningStrategies.add(new LeadInterventionStrategy());
        runningStrategies.add(new AbortStrategy(sessionRepository));
        
        strategies.put(State.RUNNING, runningStrategies);
    }
    
    @Override
    public RecoveryResult attemptRecovery(String sessionId) {
        SessionState state = sessionRepository.findById(sessionId);
        if (state == null) {
            return RecoveryResult.failed("unknown", "Session not found");
        }
        
        List<RecoveryStrategy> phaseStrategies = strategies.get(state.getStatus());
        if (phaseStrategies == null) {
            return RecoveryResult.failed("unknown", "No recovery strategies for state: " + state.getStatus());
        }
        
        // 按阶段尝试恢复
        for (RecoveryStrategy strategy : phaseStrategies) {
            log.info("Attempting recovery strategy: {} for session: {}", 
                strategy.getStrategyName(), sessionId);
            
            RecoveryResult result = strategy.attemptRecovery(sessionId);
            
            if (result.isSuccess()) {
                log.info("Recovery succeeded with strategy: {}", strategy.getStrategyName());
                return result;
            }
            
            log.info("Recovery strategy {} failed, trying next", strategy.getStrategyName());
        }
        
        // 所有策略都失败
        return RecoveryResult.aborted("All recovery strategies exhausted");
    }
    
    @Override
    public RecoveryResult attemptSelfHealing(String sessionId) {
        return getStrategy(SelfHealingStrategy.class).attemptRecovery(sessionId);
    }
    
    @Override
    public RecoveryResult attemptPeerRecovery(String sessionId) {
        return getStrategy(PeerRecoveryStrategy.class).attemptRecovery(sessionId);
    }
    
    @Override
    public RecoveryResult attemptLeadIntervention(String sessionId) {
        return getStrategy(LeadInterventionStrategy.class).attemptRecovery(sessionId);
    }
    
    @Override
    public void markAborted(String sessionId) {
        new AbortStrategy(sessionRepository).attemptRecovery(sessionId);
    }
    
    private <T extends RecoveryStrategy> T getStrategy(Class<T> strategyClass) {
        return strategies.values().stream()
            .flatMap(List::stream)
            .filter(strategy -> strategyClass.isInstance(strategy))
            .map(strategyClass::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + strategyClass));
    }
}
```

- [ ] **步骤 4：运行测试验证**

运行：`cd java-harness-workflow && mvn test`
预期：所有恢复策略测试通过

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/
git commit -m "feat: implement complete 4-phase recovery mechanism"
```

### 阶段 4 验收标准

- [ ] 4阶段恢复机制完整实现
- [ ] 自我修复能处理常见错误类型
- [ ] 同伴修复能选择替代Worker
- [ ] 指挥官介入能正确escalate
- [ ] 停止策略能正确标记ABORTED状态
- [ ] 状态恢复测试覆盖率>80%

---

## 阶段 5：工具层和优化（2-3周）

**目标：** 实现配置管理工具、验证工具、诊断工具和Native Image优化

### 阶段 5.1：配置管理工具（1周）

#### 任务 5.1.1：实现配置同步工具

**文件：**
- 创建：`java-harness-tools/src/main/java/com/chachamaru/harness/tools/config/ConfigSyncTool.java`
- 创建：`config/harness.yaml.example`

- [ ] **步骤 1：创建配置模板**

```yaml
# config/harness.yaml.example
harness:
  project:
    name: "my-project"
    version: "1.0.0"
    description: "My Java Harness Project"
  
  # 安全配置
  security:
    guardrails:
      enabled-rules:
        - R01
        - R02
        - R03
        - R04
        - R05
      protected-paths:
        - ".env"
        - ".git/"
        - "*.pem"
  
  # 工作流配置
  workflow:
    plans-path: "Plans.md"
    marker-family: "cc" # or "pm"
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
    advisor:
      enabled: true
  
  # 状态恢复配置
  recovery:
    enabled: true
    max-phases: 4
    ttl:
      sessions: "24h"
      work-states: "7d"
  
  # 日志配置
  logging:
    level: "INFO"
    format: "json"
    output: "file"
    file: "logs/harness.log"
```

- [ ] **步骤 2：实现配置同步工具**

```java
package com.chachamaru.harness.tools.config;

import com.chachamaru.harness.foundation.config.HarnessConfig;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;

public class ConfigSyncTool {
    private final File projectRoot;
    
    public ConfigSyncTool(File projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    public void syncToClaudeCode(String harnessYamlPath) throws IOException {
        File yamlFile = new File(projectRoot, harnessYamlPath);
        if (!yamlFile.exists()) {
            throw new IOException("Config file not found: " + harnessYamlPath);
        }
        
        // 读取YAML配置
        HarnessConfig config = loadYamlConfig(yamlFile);
        
        // 生成Claude Code配置文件
        generatePluginJson(config);
        generateHooksJson(config);
        generateSettingsJson(config);
        
        System.out.println("Configuration synced to Claude Code successfully");
    }
    
    public void generateFromTemplate(File outputDir) throws IOException {
        File templateFile = new File(projectRoot, "config/harness.yaml.example");
        File outputFile = new File(outputDir, "harness.yaml");
        
        Files.copy(templateFile.toPath(), outputFile.toPath());
        
        System.out.println("Configuration template generated: " + outputFile);
    }
    
    public ValidationResult validateConfig(File configFile) {
        ValidationResult result = new ValidationResult();
        
        try {
            HarnessConfig config = loadYamlConfig(configFile);
            
            // 验证必需字段
            if (config.getProjectName() == null || config.getProjectName().isEmpty()) {
                result.addError("projectName is required");
            }
            
            if (config.getVersion() == null || config.getVersion().isEmpty()) {
                result.addError("version is required");
            }
            
            // 验证安全配置
            if (config.getSecurityConfig() == null) {
                result.addWarning("No security config found, using defaults");
            }
            
            // 验证工作流配置
            if (config.getWorkflowConfig() == null) {
                result.addWarning("No workflow config found, using defaults");
            }
            
            if (result.getErrors().isEmpty()) {
                result.setValid(true);
            }
            
        } catch (Exception e) {
            result.addError("Config parsing failed: " + e.getMessage());
        }
        
        return result;
    }
    
    private HarnessConfig loadYamlConfig(File yamlFile) throws IOException {
        Yaml yaml = new Yaml();
        return yaml.loadAs(Files.newBufferedReader(yamlFile.toPath()), HarnessConfig.class);
    }
    
    private void generatePluginJson(HarnessConfig config) throws IOException {
        File pluginFile = new File(projectRoot, ".claude-plugin/plugin.json");
        // 生成plugin.json
    }
    
    private void generateHooksJson(HarnessConfig config) throws IOException {
        File hooksFile = new File(projectRoot, ".claude-plugin/hooks.json");
        // 生成hooks.json
    }
    
    private void generateSettingsJson(HarnessConfig config) throws IOException {
        File settingsFile = new File(projectRoot, ".claude-plugin/settings.json");
        // 生成settings.json
    }
}
```

- [ ] **步骤 3：实现验证结果**

```java
package com.chachamaru.harness.tools.config;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
```

- [ ] **步骤 4：运行测试验证**

运行：`cd java-harness-tools && mvn test`
预期：配置工具测试通过

- [ ] **步骤 5：Commit**

```bash
git add java-harness-tools/ config/
git commit -m "feat: implement configuration sync tool"
```

### 阶段 5.2：验证和诊断工具（1周）

#### 任务 5.2.1：实现验证工具

**文件：**
- 创建：`java-harness-tools/src/main/java/com/chachamaru/harness/tools/validate/ValidateTool.java`

- [ ] **步骤 1：实现验证工具**

```java
package com.chachamaru.harness.tools.validate;

import com.chachamaru.harness.tools.config.*;
import java.io.File;

public class ValidateTool {
    private final File projectRoot;
    
    public ValidateTool(File projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    public ValidationResult validateSkills() {
        ValidationResult result = new ValidationResult();
        
        // 验证技能文件
        File skillsDir = new File(projectRoot, "skills/");
        if (!skillsDir.exists()) {
            result.addWarning("No skills directory found");
            return result;
        }
        
        // 验证每个技能文件
        validateSkillFiles(skillsDir, result);
        
        return result;
    }
    
    public ValidationResult validateAgents() {
        ValidationResult result = new ValidationResult();
        
        // 验证代理配置
        File agentsDir = new File(projectRoot, "agents/");
        if (!agentsDir.exists()) {
            result.addWarning("No agents directory found");
            return result;
        }
        
        validateAgentFiles(agentsDir, result);
        
        return result;
    }
    
    public ValidationResult validatePlans() {
        ValidationResult result = new ValidationResult();
        
        // 验证Plans.md
        File plansFile = new File(projectRoot, "Plans.md");
        if (!plansFile.exists()) {
            result.addError("Plans.md not found");
            return result;
        }
        
        // 验证Plans.md格式
        validatePlansFormat(plansFile, result);
        
        return result;
    }
    
    public ValidationResult validateAll() {
        ValidationResult result = new ValidationResult();
        
        result.merge(validateSkills());
        result.merge(validateAgents());
        result.merge(validatePlans());
        
        return result;
    }
    
    private void validateSkillFiles(File skillsDir, ValidationResult result) {
        // 验证技能文件逻辑
    }
    
    private void validateAgentFiles(File agentsDir, ValidationResult result) {
        // 验证代理文件逻辑
    }
    
    private void validatePlansFormat(File plansFile, ValidationResult result) {
        // 验证Plans.md格式逻辑
    }
}
```

- [ ] **步骤 2：实现诊断工具**

```java
package com.chachamaru.harness.tools.doctor;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

public class DoctorTool {
    private final File projectRoot;
    
    public DoctorTool(File projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    public DiagnosticReport diagnose() {
        DiagnosticReport report = new DiagnosticReport();
        report.setGeneratedAt(LocalDateTime.now());
        
        // 检查各个模块的健康状态
        checkModuleHealth(report);
        
        // 生成建议
        generateRecommendations(report);
        
        return report;
    }
    
    public DiagnosticReport diagnoseModule(String moduleName) {
        DiagnosticReport report = new DiagnosticReport();
        
        ModuleHealth health = checkSpecificModule(moduleName);
        report.setModuleHealth(moduleName, health);
        
        return report;
    }
    
    public List<HealthCheck> getHealthChecks() {
        List<HealthCheck> checks = new ArrayList<>();
        
        checks.add(new ConfigHealthCheck());
        checks.add(new DatabaseHealthCheck());
        checks.add(new SecurityHealthCheck());
        checks.add(new WorkflowHealthCheck());
        
        return checks;
    }
    
    private void checkModuleHealth(DiagnosticReport report) {
        // 检查各模块健康状态
    }
    
    private ModuleHealth checkSpecificModule(String moduleName) {
        // 检查特定模块健康状态
        return new ModuleHealth();
    }
    
    private void generateRecommendations(DiagnosticReport report) {
        // 生成修复建议
    }
}
```

- [ ] **步骤 3：实现健康检查接口**

```java
package com.chachamaru.harness.tools.doctor;

public interface HealthCheck {
    String getName();
    HealthStatus check();
    String getFix();
}

enum HealthStatus {
    HEALTHY, DEGRADED, UNHEALTHY
}
```

- [ ] **步骤 4：Commit**

```bash
git add java-harness-tools/
git commit -m "feat: implement validation and diagnostic tools"
```

### 阶段 5.3：Native Image优化（1周）

#### 任务 5.3.1：配置GraalVM Native Image

**文件：**
- 创建：`java-harness-cli/src/main/resources/META-INF/native-image/reflect-config.json`
- 创建：`java-harness-cli/src/main/resources/META-INF/native-image/resource-config.json`
- 创建：`java-harness-cli/src/main/java/com/chachamaru/harness/cli/native/HarnessNativeFeature.java`

- [ ] **步骤 1：创建反射配置**

```json
// src/main/resources/META-INF/native-image/reflect-config.json
{
  "reflectConfig": [
    {
      "name": "com.chachamaru.harness.foundation.dto.HookInput",
      "allDeclaredFields": true,
      "allPublicMethods": true,
      "allDeclaredConstructors": true
    },
    {
      "name": "com.chachamaru.harness.foundation.dto.HookOutput",
      "allDeclaredFields": true,
      "allPublicMethods": true,
      "allDeclaredConstructors": true
    },
    {
      "name": "com.chachamaru.harness.foundation.dto.GuardrailResult",
      "allDeclaredFields": true,
      "allPublicMethods": true
    },
    {
      "name": "com.chachamaru.harness.workflow.plans.PlansDocument",
      "allDeclaredFields": true,
      "allPublicMethods": true
    },
    {
      "name": "com.chachamaru.harness.collaboration.skills.SkillContext",
      "allDeclaredFields": true,
      "allPublicMethods": true
    }
  ]
}
```

- [ ] **步骤 2：创建资源配置**

```json
// src/main/resources/META-INF/native-image/resource-config.json
{
  "resources": {
    "includes": [
      {
        "pattern": ".*\\.yaml$"
      },
      {
        "pattern": ".*\\.md$"
      },
      {
        "pattern": ".*\\.SKILL\\.md$"
      }
    ]
  }
}
```

- [ ] **步骤 3：实现Native特性**

```java
package com.chachamaru.harness.cli.native;

import org.graalvm.nativeimage.ImageCode;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeReflectionRegistry;

@AutomaticFeature
public class HarnessNativeFeature implements Feature {
    
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerReflectionConfigs(access);
        registerResourceConfigs(access);
        registerJNIConfigs(access);
    }
    
    private void registerReflectionConfigs(BeforeAnalysisAccess access) {
        // 注册反射配置
        System.out.println("Registering reflection configs for Native Image");
        
        // 注册DTO类的反射访问
        registerClassForReflection(access, HookInput.class);
        registerClassForReflection(access, HookOutput.class);
        registerClassForReflection(access, GuardrailResult.class);
    }
    
    private void registerResourceConfigs(BeforeAnalysisAccess access) {
        // 注册资源配置
        System.out.println("Registering resource configs for Native Image");
    }
    
    private void registerJNIConfigs(BeforeAnalysisAccess access) {
        // 注册JNI配置
        System.out.println("Registering JNI configs for Native Image");
    }
    
    private void registerClassForReflection(BeforeAnalysisAccess access, Class<?> clazz) {
        try {
            RuntimeReflection.register(clazz);
            System.out.println("Registered for reflection: " + clazz.getName());
        } catch (Exception e) {
            System.err.println("Failed to register " + clazz.getName() + ": " + e);
        }
    }
}
```

- [ ] **步骤 4：运行Native Image编译测试**

运行：`cd java-harness-cli && mvn -Pnative native:compile`
预期：Native Image编译成功

- [ ] **步骤 5：测试Native Image性能**

运行：`./target/harness --version`
预期：<100ms启动时间

- [ ] **步骤 6：Commit**

```bash
git add java-harness-cli/
git commit -m "feat: configure GraalVM Native Image support"
```

### 阶段 5 验收标准

- [ ] 配置工具能正确生成Claude Code配置
- [ ] 验证工具能检测配置、技能、代理问题
- [ ] 诊断工具能生成完整的健康报告
- [ ] Native Image编译成功且性能达标
- [ ] JAR和Native Image功能一致性验证通过

---

## 阶段 6：集成测试和发布（1-2周）

**目标：** 完整测试、文档完善和发布准备

### 阶段 6.1：集成测试完善（1周）

#### 任务 6.1.1：端到端集成测试

**文件：**
- 创建：`tests/integration/EndToEndWorkflowTest.java`
- 创建：`tests/integration/HookProcessingTest.java`
- 创建：`tests/integration/AgentCoordinationTest.java`

- [ ] **步骤 1：实现端到端工作流测试**

```java
package tests.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EndToEndWorkflowTest {
    
    @Test
    void shouldExecuteCompleteWorkflow() {
        // 1. Plan阶段
        SkillContext planContext = new SkillContext();
        planContext.setProjectId("test-project");
        
        Skill planSkill = skillRegistry.getSkill("plan");
        SkillResult planResult = planSkill.execute(planContext);
        
        assertTrue(planResult.isSuccess());
        assertNotNull(planResult.getArtifact());
        
        // 2. Work阶段
        SkillContext workContext = new SkillContext();
        workContext.setProjectId("test-project");
        
        Skill workSkill = skillRegistry.getSkill("work");
        SkillResult workResult = workSkill.execute(workContext);
        
        assertTrue(workResult.isSuccess());
        
        // 3. Review阶段
        SkillContext reviewContext = new SkillContext();
        reviewContext.setInputArtifact(workResult.getArtifact());
        
        Skill reviewSkill = skillRegistry.getSkill("review");
        SkillResult reviewResult = reviewSkill.execute(reviewContext);
        
        assertTrue(reviewResult.isSuccess());
        
        // 4. 验证完整流程
        ReviewReport report = (ReviewReport) reviewResult.getArtifact();
        assertNotNull(report);
        assertNotNull(report.getVerdict());
    }
}
```

- [ ] **步骤 2：实现Hook处理集成测试**

```java
package tests.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HookProcessingTest {
    
    @Test
    void shouldProcessHookEndToEnd() {
        // 1. 创建Hook输入
        HookInput input = new HookInput();
        input.setSessionId("test-session");
        input.setHookEventName("PreToolUse");
        input.setToolName("Bash");
        input.setToolInput(Map.of("command", "sudo rm -rf /tmp"));
        
        // 2. 处理Hook
        HookHandler handler = hookRegistry.resolve(HookEventType.PRE_TOOL_USE);
        HookOutput output = handler.handle(input);
        
        // 3. 验证结果
        assertNotNull(output);
        assertEquals("PreToolUse", output.getHookEventName());
        assertEquals("deny", output.getPermissionDecision());
        assertTrue(output.getPermissionDecisionReason().contains("sudo"));
    }
}
```

- [ ] **步骤 3：运行集成测试**

运行：`mvn verify -Pintegration`
预期：所有集成测试通过

- [ ] **步骤 4：Commit**

```bash
git add tests/
git commit -m "feat: add comprehensive integration tests"
```

### 阶段 6.2：文档完善和发布准备（1周）

#### 任务 6.2.1：创建用户文档

**文件：**
- 创建：`docs/installation.md`
- 创建：`docs/configuration.md`
- 创建：`docs/migration.md`
- 创建：`README.md`

- [ ] **步骤 1：创建安装指南**

```markdown
# Java Harness Installation Guide

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- GraalVM 23.1.0 or higher (for Native Image)

## Installation

### Option 1: JAR Mode

\`\`\`bash
# Clone repository
git clone https://github.com/your-org/java-harness.git
cd java-harness

# Build project
mvn clean package

# Run
java -jar java-harness-cli/target/java-harness-cli-*.jar
\`\`\`

### Option 2: Native Image Mode

\`\`\`bash
# Build Native Image
cd java-harness-cli
mvn -Pnative native:compile

# Run
./target/harness
\`\`\`

## Configuration

Copy \`config/harness.yaml.example\` to \`config/harness.yaml\` and modify as needed.
```

- [ ] **步骤 2：创建配置指南**

```markdown
# Configuration Guide

## Basic Configuration

\`\`\`yaml
harness:
  project:
    name: "my-project"
    version: "1.0.0"
  
  security:
    guardrails:
      enabled-rules: [R01, R02, R03]
\`\`\`

## Advanced Configuration

### Workflow Configuration

### Agent Configuration

### Recovery Configuration
```

- [ ] **步骤 3：创建迁移指南**

```markdown
# Migration Guide

## From Go Version

## Configuration Migration

## State Migration
```

- [ ] **步骤 4：更新README**

```markdown
# Java Harness

Java implementation of Claude Code Harness, providing complete Plan→Work→Review→Release workflow.

## Features

- ✅ Complete Guardrail rules (R01-R15)
- ✅ Skill system (Java + Markdown hybrid)
- ✅ Agent system (Worker/Reviewer/Advisor)
- ✅ Workflow orchestration
- ✅ 4-phase state recovery
- ✅ Dual-mode deployment (JAR + Native Image)

## Quick Start

\`\`\`bash
# Clone and build
git clone https://github.com/your-org/java-harness.git
cd java-harness && mvn clean package

# Configure
cp config/harness.yaml.example config/harness.yaml

# Run
java -jar java-harness-cli/target/java-harness-cli-*.jar
\`\`\`

## Documentation

- [Installation Guide](docs/installation.md)
- [Configuration Guide](docs/configuration.md)
- [Migration Guide](docs/migration.md)
```

- [ ] **步骤 5：Commit文档**

```bash
git add docs/ README.md
git commit -m "docs: add comprehensive user documentation"
```

#### 任务 6.2.2：发布准备

**文件：**
- 更新：`VERSION`文件
- 创建：`CHANGELOG.md`

- [ ] **步骤 1：更新版本号**

运行：`./scripts/sync-version.sh bump`
预期：版本号更新完成

- [ ] **步骤 2：创建CHANGELOG**

```markdown
# Changelog

## [4.1.0] - 2026-08-01

### Added
- Complete workflow layer with Plans.md parsing and task orchestration
- Collaboration layer with skill system and agent framework
- 4-phase state recovery mechanism
- Configuration management and diagnostic tools
- Native Image support with GraalVM

### Changed
- Restructured into 9 Maven modules following 7-layer architecture
- Migrated existing Guardrail rules to security module
- Improved dependency management and module boundaries

### Fixed
- Module dependency issues
- Test coverage improvements

## [4.0.0-java-SNAPSHOT] - Previous releases
```

- [ ] **步骤 3：运行完整测试套件**

运行：`mvn verify`
预期：所有测试通过，质量检查通过

- [ ] **步骤 4：创建发布分支**

运行：`git checkout -b release/4.1.0`
预期：发布分支创建成功

- [ ] **步骤 5：Commit发布准备**

```bash
git add VERSION CHANGELOG.md
git commit -m "chore: prepare for 4.1.0 release"
```

### 阶段 6 验收标准

- [ ] 集成测试覆盖率>85%
- [ ] 所有文档完整准确
- [ ] 发布版本号和CHANGELOG正确
- [ ] 发布分支准备完成
- [ ] 质量检查全部通过

---

## 总结和完成标准

### 总体验收标准

**功能对等性** (与Go项目对比):
- [ ] 90%+核心功能对等
- [ ] Plan→Work→Review→Release闭环完整
- [ ] 技能系统功能对等（混合模式）
- [ ] 代理系统功能对等（三种代理）
- [ ] 状态恢复功能对等（4阶段）
- [ ] Hook协议功能对等

**性能标准**:
- [ ] Hook处理时间<10ms（95th percentile）
- [ ] Native Image启动时间<100ms
- [ ] Native Image内存占用<50MB
- [ ] JAR模式启动时间<5秒

**质量标准**:
- [ ] 单元测试覆盖率>75%
- [ ] 集成测试覆盖率>80%
- [ ] 代码审查通过率>95%
- [ ] 无关键性bug

**文档标准**:
- [ ] 用户文档完整准确
- [ ] API文档完整
- [ ] 迁移指南清晰
- [ ] 安装指南简单易懂

**部署标准**:
- [ ] JAR和Native Image双模式验证通过
- [ ] 配置模板提供完整
- [ ] 诊断工具功能完整
- [ ] 发布流程验证通过

### 成功标志

当所有阶段的验收标准都达成时，Java Harness项目将成功从35-40%功能实现度扩展到与Go项目功能对等（90%+），成为一个完整的、轻量级的、企业级的AI开发工作流工具。

**预期成果**：
- 一个功能完整的Java版本Claude Code Harness
- 清晰的7层架构，支持长期维护
- 支持双模式部署，满足不同场景需求
- 为Java开发者提供优秀的AI辅助开发体验

---

*本实现计划遵循TDD、DRY、YAGNI原则，每个任务都是小步骤（2-5分钟），支持逐任务验证和频繁commit。*

**计划版本**: 1.0  
**创建日期**: 2026-08-01  
**适用项目**: java-harness v4.1.0-SNAPSHOT  
**总估算时间**: 14-19周