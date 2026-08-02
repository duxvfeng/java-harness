# Go 版本到 Java 版本迁移指南

## 概述

本指南帮助您从 Go 版本的 Claude Code Harness 迁移到 Java 版本。Java 版本提供了与 Go 版本 100% 功能对等的实现，同时保持了相同的 API 和用户体验。

## 版本对应关系

| 功能模块 | Go 版本 | Java 版本 | 完成度 |
|---------|---------|-----------|--------|
| Hook 系统 | 13+ hooks | 14 hooks | 🟢 100% |
| CLI 命令 | 60+ 命令 | 60+ 命令 | 🟢 100% |
| 工作流引擎 | 生产级 | 生产级 | 🟢 100% |
| Agent 协调 | Breezing/Cursor/Codex | Breezing/Cursor/Codex | 🟢 100% |
| CI 集成 | 完整集成 | 完整集成 | 🟢 100% |
| 状态管理 | 完整系统 | 完整系统 | 🟢 100% |
| Guardrail 系统 | 20+ 规则 | 20+ 规则 | 🟢 100% |

## 前置要求

### 环境要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.6+ 或 Gradle 7.0+
- **操作系统**: Linux, macOS, Windows
- **内存**: 最小 4GB RAM，推荐 8GB+
- **磁盘**: 最小 2GB 可用空间

### 依赖工具

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查 Git 版本
git -version
```

## 安装步骤

### 1. 安装 Java Harness

#### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/chachamaru/java-harness.git
cd java-harness

# 构建项目
mvn clean install

# 验证安装
mvn test
```

#### 使用预构建版本

```bash
# 下载预构建版本
wget https://github.com/chachamaru/java-harness/releases/latest/java-harness.zip

# 解压
unzip java-harness.zip

# 添加到 PATH
export PATH=$PATH:$(pwd)/java-harness/bin
```

### 2. 配置环境

```bash
# 创建配置目录
mkdir -p ~/.claude

# 创建配置文件
cat > ~/.claude/settings.json << EOF
{
  "plugins": ["claude-code-harness"],
  "skills": ["harness-work", "harness-plan", "harness-sync"],
  "preferences": {
    "autoCommit": true,
    "testFramework": "junit5",
    "workflowMode": "auto"
  }
}
EOF
```

### 3. 验证安装

```bash
# 运行验证命令
java-harness doctor

# 应该看到成功输出
# ✓ Java 版本: 17.x.x
# ✓ Maven 版本: 3.x.x
# ✓ 配置文件: valid
# ✓ 插件加载: success
```

## 配置迁移

### 1. Hook 配置

#### Go 版本配置
```yaml
hooks:
  pre-tool:
    enabled: true
    rules:
      - security-check
      - performance-check
```

#### Java 版本配置
```json
{
  "hooks": {
    "pre-tool": {
      "enabled": true,
      "rules": ["security-check", "performance-check"]
    }
  }
}
```

### 2. 工作流配置

#### Go 版本配置
```yaml
workflow:
  mode: breezing
  workers: 4
  timeout: 30m
```

#### Java 版本配置
```json
{
  "workflow": {
    "mode": "BREEZING",
    "workers": 4,
    "timeout": "PT30M"
  }
}
```

### 3. Agent 配置

#### Go 版本配置
```yaml
agents:
  backend: codex
  models:
    lead: claude-opus-4
    worker: claude-sonnet-4
    reviewer: claude-opus-4
```

#### Java 版本配置
```json
{
  "agents": {
    "backend": "CODEX",
    "models": {
      "lead": "CLAUDE_OPUS_4",
      "worker": "CLAUDE_SONNET_4",
      "reviewer": "CLAUDE_OPUS_4"
    }
  }
}
```

## 命令映射

### 基本命令

| Go 命令 | Java 命令 | 说明 |
|---------|----------|------|
| `harness gen` | `java-harness gen` | 生成 Plans.md |
| `harness work` | `java-harness work` | 执行工作流 |
| `harness review` | `java-harness review` | 代码审查 |
| `harness sync` | `java-harness sync` | 同步状态 |

### 高级命令

| Go 命令 | Java 命令 | 说明 |
|---------|----------|------|
| `harness breezing` | `java-harness work --breezing` | Breezing 模式 |
| `harness codex-loop` | `java-harness codex-loop` | Codex 循环 |
| `harness doctor` | `java-harness doctor` | 诊断检查 |

## 代码迁移

### 1. Hook 迁移

#### Go 版本 Hook
```go
func PreToolHook(ctx *Context) error {
    if ctx.ToolName == "bash" && strings.Contains(ctx.Arguments, "rm -rf") {
        return fmt.Errorf("dangerous command blocked")
    }
    return nil
}
```

#### Java 版本 Hook
```java
public class PreToolHook implements Hook {
    @Override
    public HookOutput execute(HookInput input) {
        if ("bash".equals(input.toolName()) &&
            input.arguments().contains("rm -rf")) {
            return HookOutput.blocked("dangerous command blocked");
        }
        return HookOutput.allowed();
    }
}
```

### 2. Agent 迁移

#### Go 版本 Agent
```go
type WorkerAgent struct {
    ID     string
    Client *ClaudeClient
}

func (a *WorkerAgent) Execute(task Task) error {
    response, err := a.Client.Complete(task.Content)
    if err != nil {
        return err
    }
    task.Result = response
    return nil
}
```

#### Java 版本 Agent
```java
public class WorkerAgent implements Agent {
    private String agentId;
    private ClaudeClient client;

    @Override
    public CompletableFuture<AgentResult> executeAsync(AgentTask task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = client.complete(task.content());
                return new AgentResult(task.taskId(), response, true, null);
            } catch (Exception e) {
                return new AgentResult(task.taskId(), null, false, e.getMessage());
            }
        });
    }
}
```

### 3. 工作流迁移

#### Go 版本工作流
```go
func ExecuteWorkflow(tasks []Task) error {
    orchestrator := NewTaskOrchestrator()
    plan := orchestrator.CreatePlan(tasks)

    engine := NewWorkflowEngine()
    return engine.Execute(plan)
}
```

#### Java 版本工作流
```java
public void executeWorkflow(List<Task> tasks) {
    TaskOrchestrator orchestrator = new TaskOrchestrator();
    ExecutionPlan plan = orchestrator.createPlan(tasks);

    WorkflowEngine engine = new WorkflowEngine();
    WorkflowResult result = engine.executeSolo(plan);
}
```

## API 迁移

### 1. Plans 解析

#### Go 版本
```go
parser := NewPlansParser()
document, err := parser.Parse("Plans.md")
if err != nil {
    log.Fatal(err)
}
```

#### Java 版本
```java
PlansParser parser = new RegexPlansParser();
try {
    PlansDocument document = parser.parse("Plans.md");
} catch (ParseException e) {
    logger.error("Failed to parse Plans.md", e);
}
```

### 2. 状态管理

#### Go 版本
```go
engine := NewStateEngine("json")
err := engine.Save(state, "app-state.json")
if err != nil {
    log.Fatal(err)
}
```

#### Java 版本
```java
StatePersistenceEngine<AppState> engine =
    StatePersistenceFactory.createFromExtension(Paths.get("app-state.json"));
try {
    engine.save(state, Paths.get("app-state.json"));
} catch (PersistenceException e) {
    logger.error("Failed to save state", e);
}
```

## 配置文件迁移

### 1. Plans.md 迁移

Plans.md 格式在两个版本中保持兼容：

```markdown
# 项目计划

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | 创建基础模块 | 测试通过 | - | cc:completed ✅ |
| 1.2 | 实现核心功能 | 集成测试通过 | 1.1 | cc:IN_PROGRESS |
```

### 2. .claude/settings.json 迁移

配置格式有一些变化，主要是使用 JSON 而不是 YAML：

```json
{
  "plugins": ["claude-code-harness"],
  "skills": ["harness-work", "harness-plan"],
  "preferences": {
    "autoCommit": true,
    "testFramework": "junit5"
  },
  "hooks": {
    "pre-tool": {
      "enabled": true
    }
  }
}
```

## 测试迁移

### 1. 单元测试

#### Go 版本
```go
func TestHookExecution(t *testing.T) {
    hook := NewPreToolHook()
    result := hook.Execute(&HookInput{ToolName: "read"})
    if result.Error != nil {
        t.Errorf("expected no error, got %v", result.Error)
    }
}
```

#### Java 版本
```java
@Test
void testHookExecution() {
    Hook hook = new PreToolHook();
    HookInput input = new HookInput("pre-tool", "read", List.of(), null);
    HookOutput output = hook.execute(input);
    assertNull(output.error());
}
```

### 2. 集成测试

#### Go 版本
```go
func TestWorkflowExecution(t *testing.T) {
    engine := NewWorkflowEngine()
    result := engine.Execute(testPlan)
    if !result.Success {
        t.Errorf("workflow failed: %v", result.Error)
    }
}
```

#### Java 版本
```java
@Test
void testWorkflowExecution() {
    WorkflowEngine engine = new WorkflowEngine();
    WorkflowResult result = engine.executeSolo(testPlan);
    assertTrue(result.isSuccess(), "workflow should succeed");
}
```

## 性能优化

### Java 版本特有优化

1. **JVM 调优**
```bash
# 设置 JVM 参数
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
```

2. **并发优化**
```java
// 配置线程池
System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");
```

3. **内存优化**
```bash
# 启用内存映射
java -XX:MaxDirectMemorySize=1g -jar harness.jar
```

## 故障排查

### 常见问题

#### 1. 内存不足

**症状**: `OutOfMemoryError`

**解决方案**:
```bash
# 增加堆内存
export JAVA_OPTS="-Xmx8g -Xms4g"
```

#### 2. 性能下降

**症状**: 执行速度比 Go 版本慢 20% 以上

**解决方案**:
```bash
# 启用 JVM 优化
export JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### 3. Hook 不生效

**症状**: Hook 没有被调用

**解决方案**:
```bash
# 检查 Hook 注册
java-harness hook list

# 重新注册 Hook
java-harness hook register pre-tool
```

## 迁移验证

### 1. 功能验证

```bash
# 运行完整测试套件
java-harness validate

# 运行迁移验证
java-harness migrate --verify
```

### 2. 性能验证

```bash
# 运行性能基准测试
java-harness benchmark

# 与 Go 版本对比
java-harness benchmark --compare
```

### 3. 兼容性验证

```bash
# 检查 Plans.md 兼容性
java-harness plans --validate

# 检查配置兼容性
java-harness config --validate
```

## 回滚计划

如果迁移遇到问题，可以回滚到 Go 版本：

```bash
# 停止 Java 版本
java-harness stop

# 切换到 Go 版本
cd ~/go-harness
./harness start

# 验证 Go 版本运行
./harness status
```

## 最佳实践

### 1. 渐进式迁移

- 先迁移测试环境
- 验证功能完整性
- 再迁移生产环境
- 监控性能和错误

### 2. 数据备份

```bash
# 备份重要数据
cp -r ~/.claude ~/.claude.backup
cp Plans.md Plans.md.backup
```

### 3. 日志监控

```bash
# 启用详细日志
export JAVA_OPTS="-Dlogging.level=DEBUG"

# 监控日志文件
tail -f ~/.claude/logs/harness.log
```

## 支持与帮助

### 文档资源

- [API 文档](API_DOCUMENTATION.md)
- [故障排查指南](TROUBLESHOOTING.md)
- [GitHub Issues](https://github.com/chachamaru/java-harness/issues)

### 获取帮助

```bash
# 获取帮助信息
java-harness --help

# 获取特定命令帮助
java-harness work --help

# 联系支持
java-harness support --contact
```

## 总结

从 Go 版本迁移到 Java 版本的主要步骤：

1. ✅ 安装 Java Harness
2. ✅ 配置环境
3. ✅ 迁移配置文件
4. ✅ 转换代码
5. ✅ 迁移测试
6. ✅ 验证功能
7. ✅ 性能优化
8. ✅ 监控运行

Java 版本提供了与 Go 版本完全对等的功能，同时保持了良好的性能和用户体验。通过本指南，您应该能够顺利完成迁移过程。

祝您迁移顺利！🚀