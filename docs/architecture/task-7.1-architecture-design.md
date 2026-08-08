# Task 7.1: Codex 支持架构设计文档

**项目**: Java Harness
**任务编号**: 7.1
**文档类型**: 架构设计
**版本**: 1.0
**创建日期**: 2026-08-08

---

## 1. 架构概览

### 1.1 设计目标

**主要目标**：
- 为 Java Harness 添加可选的 GPT Codex CLI 支持
- 保持 Claude Code 作为核心定位
- 提供清晰的多工具扩展框架
- 确保向后兼容性

**非目标**：
- 不改变现有的 Claude Code 支持
- 不破坏现有 API 和接口
- 不引入破坏性变更

### 1.2 设计原则

1. **可选扩展**：Codex 支持完全可选，用户按需启用
2. **清晰隔离**：Codex 和 Claude Code 状态和配置完全隔离
3. **向后兼容**：不修改现有接口，仅扩展
4. **安全第一**：默认禁用，显式启用
5. **可扩展性**：为未来其他工具（Cursor、Grok）预留扩展点

### 1.3 架构分层

```
┌─────────────────────────────────────────────────────────┐
│                   User Interface Layer                   │
│              (CLI Commands / Skills / Hooks)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Workflow Orchestration Layer                │
│                  (java-harness-workflow)                 │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
┌────────▼────────┐     ┌────────▼────────┐
│  Claude Code    │     │  Multi-Tool     │
│  Backend        │     │  Integration    │
│  (Primary)      │     │  (Optional)     │
└─────────────────┘     └────────┬────────┘
                                 │
                  ┌──────────────┴───────────┐
                  │                          │
         ┌────────▼────────┐        ┌────────▼────────┐
         │  Codex Backend  │        │  Adapter Layer  │
         │  (CLI Proxy)    │        │  (Abstraction)  │
         └─────────────────┘        └────────┬────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │  Skill Bridge       │
                                    │  (Codex ↔ Java)     │
                                    └─────────────────────┘
```

---

## 2. 模块结构设计

### 2.1 扩展现有模块

#### java-harness-collaboration 扩展

**现有职责**：多工具集成和 Agent 协调
**新增职责**：Codex 集成和适配器管理

**新子模块**：
```
java-harness-collaboration/
├── adapter/                    # AI 工具适配器层
│   ├── AIToolAdapter.java     # 适配器接口
│   ├── CodexAdapter.java      # Codex CLI 适配器
│   ├── CursorAdapter.java     # Cursor 预留（未来）
│   └── AdapterRegistry.java   # 适配器注册表
├── integration/               # 外部工具集成
│   ├── codex/
│   │   ├── CodexSetup.java       # Codex 环境设置
│   │   ├── CodexConfig.java      # 配置管理
│   │   ├── CodexCompanion.java   # Codex 进程管理
│   │   └── CodexProcess.java     # 进程通信
│   └── cursor/                   # Cursor 预留
└── skill/                      # 技能系统
    ├── CodexSkillBridge.java  # Codex 技能桥接
    ├── SkillConverter.java    # Markdown ↔ Java 转换
    └── SkillValidator.java    # 技能验证
```

### 2.2 新增接口和类

#### 2.2.1 AIToolAdapter 接口

```java
/**
 * AI 工具适配器接口
 * 为不同的 AI 工具（Claude Code、Codex、Cursor）提供统一的抽象
 */
public interface AIToolAdapter {
    /**
     * 获取工具名称
     */
    String getToolName();

    /**
     * 检查工具是否可用
     */
    boolean isAvailable();

    /**
     * 初始化工具
     */
    CompletableFuture<Void> initialize(AdapterConfig config);

    /**
     * 执行工具请求
     */
    CompletableFuture<ToolResult> execute(ToolRequest request);

    /**
     * 清理资源
     */
    CompletableFuture<Void> cleanup();

    /**
     * 获取工具配置
     */
    ToolConfig getConfig();
}
```

#### 2.2.2 CodexAdapter 实现

```java
/**
 * Codex CLI 适配器
 * 将 Java Harness 请求转换为 Codex CLI 调用
 */
public class CodexAdapter implements AIToolAdapter {
    private static final String TOOL_NAME = "codex";
    private final CodexProcess codexProcess;
    private final CodexConfig config;
    private final CodexSkillBridge skillBridge;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public boolean isAvailable() {
        try {
            ProcessResult result = executeCommand("codex", "--version");
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 转换请求为 Codex CLI 命令
                String command = convertToCodexCommand(request);

                // 执行命令
                ProcessResult result = codexProcess.execute(command);

                // 转换结果
                return convertToToolResult(result);
            } catch (Exception e) {
                throw new ToolExecutionException("Codex execution failed", e);
            }
        });
    }
}
```

#### 2.2.3 CodexSkillBridge 类

```java
/**
 * Codex 技能桥接
 * 实现 Codex Markdown 技能与 Java 技能系统的互操作
 */
public class CodexSkillBridge {
    private final SkillConverter converter;
    private final SkillValidator validator;

    /**
     * 加载 Codex 技能
     */
    public CompletableFuture<Skill> loadCodexSkill(Path skillPath) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. 读取 Markdown 文件
            String markdown = readMarkdown(skillPath);

            // 2. 转换为 Java Skill
            Skill skill = converter.markdownToSkill(markdown);

            // 3. 验证技能
            validator.validate(skill);

            return skill;
        });
    }

    /**
     * 转换 Java 技能为 Codex Markdown
     */
    public String convertToMarkdown(Skill skill) {
        return converter.skillToMarkdown(skill);
    }
}
```

### 2.3 配置管理

#### CodexConfig 类

```java
/**
 * Codex 配置管理
 * 管理 .codex/config.toml 和 Java 配置
 */
public class CodexConfig {
    private final Path configPath;
    private final Map<String, Object> config;

    /**
     * 从 TOML 文件加载配置
     */
    public static CodexConfig fromToml(Path tomlPath) {
        // 解析 TOML 配置
        Map<String, Object> config = TomlParser.parse(tomlPath);
        return new CodexConfig(tomlPath, config);
    }

    /**
     * 获取 API 端点
     */
    public String getApiEndpoint() {
        return (String) config.getOrDefault("api_endpoint", "https://api.openai.com/v1");
    }

    /**
     * 获取模型配置
     */
    public ModelConfig getModelConfig() {
        Map<String, Object> model = (Map<String, Object>) config.get("model");
        return new ModelConfig(
            (String) model.get("name"),
            (Integer) model.get("max_tokens"),
            (Double) model.get("temperature")
        );
    }
}
```

---

## 3. 接口设计

### 3.1 BackendExecutor 扩展

#### 现有接口（不变）

```java
public interface BackendExecutor {
    CompletableFuture<BackendResult> execute(BackendRequest request);
    boolean isAvailable();
    String getBackendName();
}
```

#### 新增 CodexBackend 扩展

```java
/**
 * Codex 后端执行器
 * 实现 BackendExecutor 接口，集成 CodexAdapter
 */
public class CodexBackend implements BackendExecutor {
    private final CodexAdapter adapter;
    private final BackendConfig config;

    @Override
    public CompletableFuture<BackendResult> execute(BackendRequest request) {
        // 转换 BackendRequest 为 ToolRequest
        ToolRequest toolRequest = convertToToolRequest(request);

        // 通过适配器执行
        CompletableFuture<ToolResult> result = adapter.execute(toolRequest);

        // 转换 ToolResult 为 BackendResult
        return result.thenApply(this::convertToBackendResult);
    }

    @Override
    public boolean isAvailable() {
        return adapter.isAvailable();
    }

    @Override
    public String getBackendName() {
        return "codex";
    }
}
```

### 3.2 工作流集成

#### 工作流配置示例

```yaml
# workflow-codex-example.yaml
name: codex-workflow-example
description: 使用 Codex 后端的工作流示例

backend:
  type: codex  # 新增后端类型
  config:
    model: gpt-4
    max_tokens: 2000
    temperature: 0.7

steps:
  - name: code-review
    tool: codex
    input:
      file: src/main/java/com/example/App.java
    output:
      review: output/codex-review.md
```

---

## 4. 集成方案

### 4.1 进程通信架构

```
┌─────────────────────────────────────────────────┐
│         Java Harness Process                    │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │   CodexBackend / CodexAdapter           │   │
│  └─────────────┬───────────────────────────┘   │
│                │                               │
│                │ ProcessBuilder               │
│                │                               │
│  ┌─────────────▼───────────────────────────┐   │
│  │   Codex CLI Process (Subprocess)        │   │
│  │                                          │   │
│  │  - stdin:  JSON requests               │   │
│  │  - stdout: JSON responses              │   │
│  │  - stderr: Error logs                 │   │
│  └──────────────────────────────────────────┘   │
│                                                  │
└─────────────────────────────────────────────────┘
```

#### 进程启动协议

```java
/**
 * Codex 进程管理
 */
public class CodexProcess {
    private Process process;
    private BufferedReader stdoutReader;
    private BufferedWriter stdinWriter;

    /**
     * 启动 Codex 进程
     */
    public void start() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("codex", "daemon", "--json");
        pb.redirectErrorStream(true);

        process = pb.start();
        stdoutReader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        stdinWriter = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream())
        );

        // 等待进程就绪
        waitForReady();
    }

    /**
     * 执行命令
     */
    public ProcessResult execute(String command) throws IOException {
        // 写入 JSON 请求到 stdin
        JsonRequest request = new JsonRequest(command);
        stdinWriter.write(request.toJson());
        stdinWriter.newLine();
        stdinWriter.flush();

        // 从 stdout 读取响应
        String response = stdoutReader.readLine();
        return parseResponse(response);
    }
}
```

### 4.2 技能系统集成

#### Codex 技能格式

**Codex Markdown 格式**：
```markdown
---
name: codex-review
description: 使用 Codex 进行代码审查
---

请对以下代码进行审查：
{{code}}

重点关注：
1. 安全问题
2. 性能问题
3. 代码风格
```

**转换后的 Java Skill**：
```java
@SkillInfo(
    name = "codex-review",
    description = "使用 Codex 进行代码审查"
)
public class CodexReviewSkill implements Skill {
    @Override
    public SkillResult execute(SkillContext context) {
        String code = context.getInput("code");

        // 通过 CodexAdapter 执行
        String prompt = String.format("""
            请对以下代码进行审查：
            %s

            重点关注：
            1. 安全问题
            2. 性能问题
            3. 代码风格
            """, code);

        return codexAdapter.execute(prompt);
    }
}
```

---

## 5. 数据流设计

### 5.1 请求流程

```
┌─────────────┐
│   User      │
│  Request    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  Workflow Engine                         │
│  - Parse workflow config                 │
│  - Select backend (codex)                │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  CodexBackend                            │
│  - Convert request to Codex format      │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  CodexAdapter                            │
│  - Convert to CLI command                │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  CodexProcess                            │
│  - Execute subprocess                     │
│  - Handle stdin/stdout                   │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  Codex CLI                               │
│  - Process request                       │
│  - Call OpenAI API                       │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────┐
│   Response  │
└─────────────┘
```

### 5.2 数据格式转换

#### Java Request → Codex Request

```java
/**
 * 转换 BackendRequest 为 Codex Request
 */
private CodexRequest convertToCodexRequest(BackendRequest request) {
    CodexRequest codexRequest = new CodexRequest();

    // 设置 prompt
    codexRequest.setPrompt(request.getPrompt());

    // 设置工具调用
    if (request.getToolUses() != null) {
        List<CodexTool> tools = request.getToolUses().stream()
            .map(this::convertTool)
            .collect(Collectors.toList());
        codexRequest.setTools(tools);
    }

    // 设置上下文
    codexRequest.setContext(request.getContext());

    return codexRequest;
}
```

#### Codex Response → Java Response

```java
/**
 * 转换 Codex Response 为 BackendResponse
 */
private BackendResponse convertToBackendResponse(CodexResponse response) {
    BackendResponse backendResponse = new BackendResponse();

    // 设置结果
    backendResponse.setResult(response.getText());

    // 设置工具调用结果
    if (response.getToolCalls() != null) {
        List<ToolCallResult> toolResults = response.getToolCalls().stream()
            .map(this::convertToolCall)
            .collect(Collectors.toList());
        backendResponse.setToolCallResults(toolResults);
    }

    // 设置元数据
    backendResponse.setMetadata(Map.of(
        "backend", "codex",
        "model", response.getModel(),
        "tokens_used", response.getUsage()
    ));

    return backendResponse;
}
```

---

## 6. 部署架构

### 6.1 本地部署

#### 单机部署（推荐）

```
┌─────────────────────────────────────────┐
│   Developer Machine                      │
│                                          │
│  ┌──────────────────────────────────┐  │
│  │  Java Harness JAR                │  │
│  │  - codex-backend module          │  │
│  │  - codex-adapter module          │  │
│  └──────────────┬───────────────────┘  │
│                 │                       │
│                 │ Process                │
│                 │                       │
│  ┌──────────────▼───────────────────┐  │
│  │  Codex CLI (npm install)         │  │
│  └──────────────────────────────────┘  │
│                                          │
│  Configuration:                          │
│  - .claude/state/config.json            │
│  - .codex/config.toml                   │
└──────────────────────────────────────────┘
```

### 6.2 配置文件

#### Java Harness 配置

```json
// .claude/state/config.json
{
  "backend": {
    "default": "claude",
    "available": ["claude", "codex"],
    "codex": {
      "enabled": true,
      "apiEndpoint": "https://api.openai.com/v1",
      "model": "gpt-4",
      "maxTokens": 2000,
      "temperature": 0.7
    }
  },
  "guardrails": {
    "enableR28": true,
    "enableR29": true,
    "enableR30": true
  }
}
```

#### Codex 配置

```toml
# .codex/config.toml
[api]
endpoint = "https://api.openai.com/v1"
key = "${OPENAI_API_KEY}"

[model]
name = "gpt-4"
max_tokens = 2000
temperature = 0.7

[integration]
java_harness = true
skill_path = ".codex/skills"
```

---

## 7. 技术决策

### 7.1 为什么选择进程通信而非直接 API 调用？

**决策**：使用进程通信（`ProcessBuilder`）而非直接调用 OpenAI API

**理由**：
1. **兼容性**：Codex CLI 已经提供了完整的工具调用逻辑
2. **维护性**：Codex CLI 更新时，自动获得新功能
3. **隔离性**：进程隔离提供更好的错误处理和资源管理
4. **合规性**：使用官方 CLI，避免 API 契约变更风险

**权衡**：
- ❌ 性能开销（进程启动和 IPC）
- ❌ 依赖 Codex CLI 安装
- ✅ 降低维护成本
- ✅ 自动获得 CLI 更新

### 7.2 为什么需要适配器层？

**决策**：引入 AIToolAdapter 适配器层

**理由**：
1. **抽象化**：统一不同 AI 工具的接口
2. **可扩展性**：未来支持 Cursor、Grok 等工具
3. **可测试性**：便于单元测试和 Mock
4. **灵活性**：运行时切换后端

**权衡**：
- ❌ 增加一层抽象，复杂度上升
- ❌ 可能引入性能开销
- ✅ 清晰的架构边界
- ✅ 便于未来扩展

### 7.3 为什么 Codex 支持是可选的？

**决策**：Codex 支持作为可选扩展，不作为核心功能

**理由**：
1. **产品定位**：保持 Claude Code 作为核心定位
2. **维护成本**：减少对多工具的维护负担
3. **用户清晰**：避免用户混淆主要功能
4. **安全考虑**：默认禁用，显式启用

**权衡**：
- ❌ 功能分割，可能影响用户体验
- ❌ 需要额外的配置和启用步骤
- ✅ 清晰的产品定位
- ✅ 降低维护成本

---

## 8. 性能考虑

### 8.1 性能指标

| 操作 | Claude Code 后端 | Codex 后端 | 备注 |
|------|------------------|------------|------|
| Hook 处理 | < 50ms | < 100ms | 进程通信开销 |
| 工作流启动 | < 100ms | < 200ms | 进程启动开销 |
| 简单工作流执行 | < 1s | < 2s | CLI 调用开销 |
| 内存使用 | ~500MB | ~700MB | Codex 进程额外内存 |

### 8.2 优化策略

1. **进程复用**：保持 Codex 进程运行，避免重复启动
2. **连接池**：复用进程通信连接
3. **缓存**：缓存常用技能和配置
4. **异步处理**：使用 CompletableFuture 异步执行

---

## 9. 安全架构

### 9.1 安全分层

```
┌─────────────────────────────────────────┐
│  Application Layer                      │
│  - Java Harness Workflow                │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Guardrail Layer (27 rules + 3 new)     │
│  - R28: Codex Worktree 监控             │
│  - R29: Codex 凭证保护                  │
│  - R30: 敏感数据阻断                    │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Adapter Layer                          │
│  - AIToolAdapter 接口                   │
│  - CodexAdapter 实现                    │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Process Layer                          │
│  - CodexProcess 管理                    │
│  - 进程隔离和沙箱                       │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  External Layer                         │
│  - Codex CLI                            │
│  - OpenAI API                           │
└─────────────────────────────────────────┘
```

### 9.2 隔离机制

1. **进程隔离**：Codex 在独立进程中运行
2. **配置隔离**：Codex 配置独立于 Claude Code
3. **状态隔离**：Codex 状态存储在独立目录
4. **凭证隔离**：OpenAI API Key 独立存储

---

## 10. 测试策略

### 10.1 测试层次

```
┌─────────────────────────────────────────┐
│  E2E Tests                              │
│  - 完整工作流测试                        │
│  - 多后端集成测试                        │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Integration Tests                      │
│  - CodexAdapter 集成测试                │
│  - CodexProcess 通信测试                │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Unit Tests                             │
│  - AIToolAdapter 接口测试               │
│  - SkillConverter 转换测试              │
│  - CodexConfig 配置测试                 │
└─────────────────────────────────────────┘
```

### 10.2 测试覆盖率目标

| 模块 | 覆盖率目标 |
|------|-----------|
| adapter/ | 85% |
| integration/codex/ | 80% |
| skill/ | 85% |
| 总体 | 80% |

---

## 11. 实施路线图

### 11.1 分阶段实施

**Phase 1：基础架构（Task 7.1-7.3）**
- Task 7.1: 技术调研和架构设计 ✅
- Task 7.2: AIToolAdapter 接口和 CodexAdapter 实现
- Task 7.3: CodexBackend 增强和状态管理

**Phase 2：功能完善（Task 7.4-7.5）**
- Task 7.4: CodexSkillBridge 技能桥接
- Task 7.5: CodexSetup 和配置管理

**Phase 3：安全集成（Task 7.6）**
- Task 7.6: Codex 安全规则集成

**Phase 4：测试和发布（Task 7.7-7.8）**
- Task 7.7: 集成测试和文档
- Task 7.8: Beta 发布和反馈收集

### 11.2 时间估算

**总计：11 周**
- 技术验证：2 周
- 核心实现：3 周
- 安全集成：2 周
- 测试和文档：2 周
- Beta 发布：2 周

---

## 12. 依赖关系

### 12.1 内部依赖

```
Task 7.1 (架构设计) ✅
    │
    ├─── Task 7.2 (适配器实现)
    │        │
    │        ├─── Task 7.3 (Backend 增强)
    │        │        │
    │        │        ├─── Task 7.4 (技能桥接)
    │        │        │        │
    │        │        │        ├─── Task 7.5 (配置管理)
    │        │        │        │        │
    │        │        │        │        ├─── Task 7.6 (安全规则)
    │        │        │        │        │        │
    │        │        │        │        │        ├─── Task 7.7 (测试文档)
    │        │        │        │        │        │        │
    │        │        │        │        │        │        └─── Task 7.8 (Beta 发布)
```

### 12.2 外部依赖

| 依赖项 | 版本要求 | 用途 |
|--------|---------|------|
| Java | 17+ | 运行环境 |
| Maven | 3.8+ | 构建工具 |
| Codex CLI | 最新 | 外部工具 |
| JUnit | 5.8+ | 测试框架 |
| SnakeYAML | 1.30+ | YAML 解析 |
| Toml4j | 0.5.0+ | TOML 解析 |

---

## 13. 风险和缓解措施

### 13.1 技术风险

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 进程通信复杂度 | 🔴 高 | 充分的集成测试，错误处理机制 |
| 跨平台兼容性 | 🟡 中 | 抽象进程管理层，充分测试 |
| 状态同步 | 🔴 高 | 设计明确的状态协议，心跳机制 |

### 13.2 维护风险

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| Codex CLI 频繁更新 | 🟡 中 | 版本锁定，自动化测试 |
| API 契约变更 | 🟡 中 | 使用官方 CLI，避免直接 API 调用 |
| 多后端维护成本 | 🔴 高 | 清晰的接口抽象，充分文档 |

### 13.3 安全风险

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 凭证泄露 | 🔴 高 | 加密存储，访问控制，审计日志 |
| 数据传输 | 🟡 中 | HTTPS，数据分类验证 |
| 供应链攻击 | 🟡 中 | 依赖验证，校验和检查 |

---

## 14. 监控和日志

### 14.1 监控指标

```java
/**
 * Codex 后端监控指标
 */
public class CodexMetrics {
    // 性能指标
    private final Counter executionCount;
    private final Timer executionTimer;
    private final Counter errorCount;

    // 资源指标
    private final Gauge processMemoryUsage;
    private final Gauge activeConnections;

    // 业务指标
    private final Counter skillInvocations;
    private final Histogram tokenUsage;
}
```

### 14.2 日志策略

```java
/**
 * Codex 日志记录
 */
public class CodexLogger {
    private static final Logger logger = LoggerFactory.getLogger(CodexLogger.class);

    public void logRequest(CodexRequest request) {
        logger.info("Codex request: tool={}, prompt_length={}",
            request.getTool(),
            request.getPrompt().length()
        );
    }

    public void logResponse(CodexResponse response) {
        logger.info("Codex response: model={}, tokens={}, duration={}ms",
            response.getModel(),
            response.getUsage(),
            response.getDuration()
        );
    }
}
```

---

## 15. 文档和培训

### 15.1 用户文档

- **安装指南**：如何安装 Codex CLI 和启用 Codex 支持
- **配置指南**：如何配置 Codex 后端和技能
- **使用指南**：如何使用 Codex 支持的工作流
- **故障排除**：常见问题和解决方案

### 15.2 开发者文档

- **架构文档**：本文档
- **API 文档**：接口和类说明
- **集成指南**：如何扩展适配器层
- **测试指南**：如何测试 Codex 集成

---

## 16. 结论

### 16.1 架构总结

本架构设计提供了：
- ✅ 清晰的多工具扩展框架
- ✅ 完整的 Codex 集成方案
- ✅ 向后兼容的接口设计
- ✅ 全面的安全考虑
- ✅ 可行的实施路径

### 16.2 下一步

1. **实施 Task 7.2**：开始 AIToolAdapter 接口和 CodexAdapter 实现
2. **安全评审**：通过安全评审后开始实施
3. **Beta 测试**：邀请用户参与 Beta 测试
4. **反馈收集**：根据反馈调整和优化

---

**文档编制**: Java Harness Team
**架构评审**: 待评审
**下一步**: 风险评估文档（task-7.1-risk-assessment.md）
