# API 参考文档

Java Harness API 接口详细说明文档。

## 📚 API 概述

Java Harness 提供了丰富的 API 接口，支持以下功能：

- **Hook 协议 API**: Claude Code Hook 事件处理
- **CLI 命令 API**: 86 个命令行接口
- **Guardrail API**: 安全规则评估接口
- **Workflow API**: 工作流执行接口
- **Configuration API**: 配置管理接口

> **注意**: 本文档为 API 参考框架，详细 API 文档将在后续版本中补充完整。

---

## 🔌 Hook 协议 API

### 输入格式

Hook 协议输入为 JSON 格式，通过 stdin 传递：

```json
{
  "session_id": "string",           // 会话 ID
  "transcript_path": "string",     // 转录文件路径
  "cwd": "string",                 // 当前工作目录
  "permission_mode": "string",      // 权限模式: default|auto|bypass
  "hook_event_name": "string",     // Hook 事件名称
  "tool_name": "string",           // 工具名称
  "tool_input": {},                // 工具输入参数
  "plugin_root": "string"          // 插件根目录
}
```

### 输出格式

Hook 协议输出为 JSON 格式，通过 stdout 返回：

```json
{
  "hookEventName": "string",                    // Hook 事件名称
  "permissionDecision": "string",               // 决策: allow|block
  "permissionDecisionReason": "string|null",    // 决策原因
  "additionalContext": {}                        // 额外上下文信息
}
```

### Hook 事件类型

| 事件名称 | 描述 | 处理器 |
|---------|------|--------|
| PreToolUse | 工具使用前检查 | PreToolUseHandler |
| PostToolUse | 工具使用后检查 | PostToolUseHandler |
| PermissionRequest | 权限请求处理 | PermissionRequestHandler |
| SessionStart | 会话开始处理 | SessionStartHandler |
| SessionInit | 会话初始化处理 | SessionInitHandler |
| SessionCleanup | 会话清理处理 | SessionCleanupHandler |
| SessionMonitor | 会话监控处理 | SessionMonitorHandler |
| SessionSummary | 会话总结处理 | SessionSummaryHandler |
| CIStatus | CI 状态检查 | CIStatusHandler |
| SubagentStart | 子代理开始处理 | SubagentStartHandler |
| SubagentStop | 子代理停止处理 | SubagentStopHandler |
| Notification | 通知处理 | NotificationHandler |
| PermissionDenied | 权限拒绝处理 | PermissionDeniedHandler |

---

## 🔒 Guardrail API

### 规则接口

```java
public interface Rule {
    /**
     * 获取规则 ID
     * @return 规则 ID (如 "R01")
     */
    String getId();
    
    /**
     * 获取规则名称
     * @return 规则名称
     */
    String getName();
    
    /**
     * 判断是否匹配该规则
     * @param input Hook 输入
     * @return 是否匹配
     */
    boolean matches(HookInput input);
    
    /**
     * 评估规则
     * @param input Hook 输入
     * @return 评估结果
     */
    GuardrailResult evaluate(HookInput input);
}
```

### 规则结果

```java
public class GuardrailResult {
    private final Decision decision;  // ALLOW | BLOCK
    private final String reason;       // 决策原因
    private final Map<String, Object> context;  // 额外上下文
    
    // 创建允许结果
    public static GuardrailResult allowed();
    
    // 创建阻止结果
    public static GuardrailResult blocked(String reason);
}
```

### 规则注册

```java
public class GuardrailRegistry {
    /**
     * 注册安全规则
     * @param rule 规则实例
     */
    public void registerRule(Rule rule);
    
    /**
     * 评估所有规则
     * @param input Hook 输入
     * @return 评估结果
     */
    public GuardrailResult evaluate(HookInput input);
}
```

---

## 🎯 CLI 命令 API

### 命令接口

```java
@Command(name = "my-command", mixinStandardHelpOptions = true)
public class MyCommand implements Runnable {
    
    @Option(names = {"-o", "--option"}, description = "命令选项")
    private String option;
    
    @Parameters(index = "0", description = "参数")
    private String parameter;
    
    @Override
    public void run() {
        // 命令实现
    }
}
```

### 命令注册

```java
public class CommandRegistry {
    /**
     * 注册命令
     * @param commandClass 命令类
     */
    public void registerCommand(Class<?> commandClass);
    
    /**
     * 执行命令
     * @param args 命令行参数
     */
    public void execute(String[] args);
}
```

---

## 🔄 Workflow API

### 工作流定义

```yaml
name: "示例工作流"
description: "工作流描述"
version: "1.0"

tasks:
  - name: "任务1"
    description: "任务描述"
    command: "echo 'Hello'"
    condition: "true"
    
  - name: "任务2"
    description: "任务描述"
    command: "echo 'World'"
    depends_on: ["任务1"]
    parallel: false
```

### 工作流引擎

```java
public class WorkflowEngine {
    /**
     * 执行工作流
     * @param workflow 工作流定义
     * @return 执行结果
     */
    public WorkflowResult execute(Workflow workflow);
    
    /**
     * 获取工作流状态
     * @param workflowId 工作流 ID
     * @return 工作流状态
     */
    public WorkflowStatus getStatus(String workflowId);
}
```

---

## ⚙️ Configuration API

### 配置接口

```java
public class ConfigurationManager {
    /**
     * 加载配置文件
     * @param path 配置文件路径
     * @return 配置对象
     */
    public Config loadConfig(String path);
    
    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值
     */
    public String getValue(String key);
    
    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    public void setValue(String key, String value);
    
    /**
     * 保存配置
     * @param path 配置文件路径
     */
    public void saveConfig(String path);
}
```

### 配置结构

```toml
[harness]
version = "4.1.1"
mode = "standard"

[paths]
data = ".claude/data"
cache = ".claude/cache"
logs = ".claude/logs"

[security]
guardrails = true
strict_mode = false

[logging]
level = "INFO"
file = true
console = true
```

---

## 🔍 验证 API

### 验证接口

```java
public class ValidationEngine {
    /**
     * 验证配置文件
     * @return 验证结果
     */
    public ValidationResult validateConfig();
    
    /**
     * 验证 SKILL.md 文件
     * @param path 文件路径
     * @return 验证结果
     */
    public ValidationResult validateSkillFile(String path);
    
    /**
     * 验证 Plans.md 文件
     * @return 验证结果
     */
    public ValidationResult validatePlansFile();
}
```

---

## 📊 状态 API

### 状态接口

```java
public class StatusReporter {
    /**
     * 获取系统状态
     * @return 系统状态
     */
    public SystemStatus getSystemStatus();
    
    /**
     * 获取 Agent 状态
     * @param agentId Agent ID
     * @return Agent 状态
     */
    public AgentStatus getAgentStatus(String agentId);
    
    /**
     * 获取所有 Agent 状态
     * @return Agent 状态列表
     */
    public List<AgentStatus> getAllAgentStatuses();
}
```

---

## 🧪 测试 API

### 测试接口

```java
public class TestRunner {
    /**
     * 运行单元测试
     * @return 测试结果
     */
    public TestResult runUnitTests();
    
    /**
     * 运行集成测试
     * @return 测试结果
     */
    public TestResult runIntegrationTests();
    
    /**
     * 运行特定测试
     * @param testClass 测试类
     * @return 测试结果
     */
    public TestResult runTest(Class<?> testClass);
}
```

---

## 📝 使用示例

### Hook 处理示例

```java
// 创建 Hook 处理器
HookHandler handler = new PreToolUseHandler();

// 解析输入
HookInput input = HookCodec.decode(jsonString);

// 处理 Hook
HookOutput output = handler.handle(input);

// 编码输出
String result = HookCodec.encode(output);
```

### 规则评估示例

```java
// 创建规则注册表
GuardrailRegistry registry = new GuardrailRegistry();

// 注册规则
registry.registerRule(new R01BlockPrivilegeEscalation());

// 评估规则
GuardrailResult result = registry.evaluate(input);

if (result.isBlocked()) {
    System.out.println("Blocked: " + result.getReason());
}
```

### 工作流执行示例

```java
// 创建工作流引擎
WorkflowEngine engine = new WorkflowEngine();

// 加载工作流
Workflow workflow = WorkflowLoader.load("workflow.yaml");

// 执行工作流
WorkflowResult result = engine.execute(workflow);

// 检查结果
if (result.isSuccess()) {
    System.out.println("Workflow completed successfully");
}
```

---

## 🔗 相关文档

- Hook 协议规范：待补充独立参考文档；当前以本文的 Hook 协议章节和源码测试为准。
- Guardrail 规则参考：待补充独立参考文档；当前以 Java CLI 的 guardrail 模块和测试为准。
- CLI 命令参考：待补充独立参考文档；当前以 `java-harness-cli` 的帮助输出和命令测试为准。
- [架构设计文档](../developer-guide/architecture.md) - 系统架构和设计决策

---

## ⚠️ 版本兼容性

| API 版本 | Harness 版本 | 状态 |
|---------|-------------|------|
| 4.1.x | 4.1.1 | 当前版本 |
| 4.0.x | 4.0.x | 维护模式 |
| 3.x | 3.x | 已停止支持 |

---

## 📞 API 支持

如需 API 支持，请通过以下方式联系：

- **文档**: 查阅相关文档和示例
- **Issues**: [GitHub Issues](https://github.com/your-org/java-harness/issues)
- **讨论**: [GitHub Discussions](https://github.com/your-org/java-harness/discussions)

---

**API 参考文档版本**: 1.0  
**最后更新**: 2026-08-08  
**维护者**: Java Harness Team  
**适用于版本**: 4.1.1

> **注**: 本文档为 API 参考框架，详细 API 文档将在后续版本中持续补充完善。
