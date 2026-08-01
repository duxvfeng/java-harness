# Java Harness Product Specification

## Purpose

Java Harness 是 Claude Code Harness 的 Java 实现，目标是与 Go 版本功能对等（90%+），提供完整的 Plan→Work→Review→Release 闭环。

## Project Context

**当前状态**: 35-40% 功能实现度
**目标状态**: 90%+ 功能实现度（与 Go 项目对等）
**技术栈**: Java 17 + Spring Boot 3.2 + MyBatis + SQLite + Jackson + CompletableFuture + GraalVM Native Image
**预计周期**: 14-19周

## Architecture Overview

### 7层功能域驱动设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Tools Layer (工具层)                        │
│              Config / Validate / Doctor                       │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                 Runtime Layer (运行时层)                      │
│                    CLI / Native Image                         │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│              Collaboration Layer (协作层)                     │
│               Skills / Agents / Coordination                  │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│               Workflow Layer (工作流层)                        │
│         Plans.md / Orchestration / Execution / Recovery        │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│              Security Layer (安全防护层)                       │
│             Guardrail / Validation / Audit                    │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                Protocol Layer (协议层)                        │
│                  Hook / Tool / Codec                          │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│             Foundation Layer (基础设施层)                     │
│                DTO / Config / Persistence                      │
└─────────────────────────────────────────────────────────────┘
```

### 9个Maven模块

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

**依赖规则**: 单向依赖，上层可以依赖下层，下层不能依赖上层。

## Core Features

### 1. Guardrail System（安全防护系统）
- **R01-R15规则**: 完整的Guardrail规则集
- **动态注册**: 运行时注册/注销规则
- **规则匹配**: 基于HookInput的模式匹配
- **决策引擎**: ALLOW/DENY/ASK/WARN决策

### 2. Workflow System（工作流系统）
- **Plans.md解析**: 支持表格格式和状态标记解析
- **任务编排**: 支持顺序和并行任务执行
- **状态恢复**: 4阶段恢复机制（自我修复→同伴修复→指挥官介入→停止）
- **并行执行**: 基于CompletableFuture的并行任务执行

### 3. Collaboration System（协作系统）
- **技能框架**: 混合模式（Java + Markdown技能）
- **代理系统**: 三种核心代理（Worker/Reviewer/Advisor）
- **协调机制**: 多代理协调和事件广播

### 4. Hook Protocol（Hook协议）
- **HookEventType**: 18种Hook事件类型
- **HookHandler**: 统一的Hook处理接口
- **HookCodec**: Jackson编解码器
- **权限决策**: allow/deny/ask/defer决策

### 5. Tool System（工具系统）
- **配置管理**: 配置同步和验证
- **验证工具**: 技能/代理/Plans验证
- **诊断工具**: 健康检查和建议生成

### 6. Native Image Support
- **GraalVM支持**: 反射和资源配置
- **双模式部署**: JAR和Native Image两种模式
- **性能优化**: <100ms启动时间，<50MB内存占用

## Technical Specifications

### Data Models

#### HookInput
```java
{
  "sessionId": String,
  "transcriptPath": String,
  "cwd": String,
  "permissionMode": String,
  "hookEventName": String,
  "toolName": String,
  "toolInput": Map<String, Object>,
  "pluginRoot": String
}
```

#### HookOutput
```java
{
  "hookEventName": String,
  "permissionDecision": "allow" | "deny" | "ask" | "defer",
  "permissionDecisionReason": String,
  "updatedInput": Object,
  "additionalContext": String
}
```

#### GuardrailResult
```java
{
  "decision": "ALLOW" | "DENY" | "ASK" | "WARN",
  "ruleId": String,
  "reason": String,
  "block": boolean
}
```

#### PlansDocument
```java
{
  "title": String,
  "metadata": String,
  "lastModified": LocalDateTime,
  "tasks": List<Task>
}
```

#### Task
```java
{
  "id": String,
  "title": String,
  "description": String,
  "status": "PM_REQUESTED" | "PM_APPROVED" | "CC_TODO" | "CC_WIP" | "CC_DONE" | "CC_WITHDRAWN",
  "acceptanceCriteria": String,
  "dependencies": List<String>,
  "lane": "implementation" | "review" | "release"
}
```

### API Contracts

#### GuardrailEngine
```java
interface GuardrailEngine {
    void registerRule(GuardrailRule rule);
    void unregisterRule(String ruleId);
    GuardrailResult evaluate(HookInput input);
    List<GuardrailRule> getTriggeredRules(HookInput input);
    List<GuardrailRule> getAllRules();
}
```

#### TaskOrchestrator
```java
interface TaskOrchestrator {
    OrchestrationPlan createPlan(PlansDocument plans);
    ExecutionResult execute(OrchestrationPlan plan);
    void pause(String executionId);
    void resume(String executionId);
    void cancel(String executionId);
}
```

#### StateRecovery
```java
interface StateRecovery {
    RecoveryResult attemptRecovery(String sessionId);
    RecoveryResult attemptSelfHealing(String sessionId);
    RecoveryResult attemptPeerRecovery(String sessionId);
    RecoveryResult attemptLeadIntervention(String sessionId);
    void markAborted(String sessionId);
}
```

## Performance Requirements

- **Hook处理时间**: <10ms（95th percentile）
- **Native Image启动**: <100ms
- **Native Image内存**: <50MB
- **JAR模式启动**: <5秒

## Quality Requirements

- **单元测试覆盖率**: >75%
- **集成测试覆盖率**: >80%
- **代码审查通过率**: >95%
- **无关键性bug**

## Security Requirements

- **Guardrail规则**: R01-R15完整实现
- **敏感路径保护**: .env、.git/、*.pem等
- **权限决策**: allow/deny/ask/defer四级决策
- **审计日志**: 完整的操作日志记录

## Deployment Modes

### 1. JAR Mode
```bash
mvn clean package
java -jar java-harness-cli/target/java-harness-cli-*.jar
```

### 2. Native Image Mode
```bash
cd java-harness-cli
mvn -Pnative native:compile
./target/harness
```

## Configuration

配置文件: `config/harness.yaml`

```yaml
harness:
  project:
    name: "my-project"
    version: "1.0.0"
    description: "My Java Harness Project"

  security:
    guardrails:
      enabled-rules: [R01, R02, R03, R04, R05]
      protected-paths: [".env", ".git/", "*.pem"]

  workflow:
    plans-path: "Plans.md"
    marker-family: "cc"
    parallel-execution: true
    max-concurrency: 4

  agents:
    worker:
      timeout: "5m"
      retry-strategy: "exponential-backoff"
    reviewer:
      cross-model: true
      temperature: 0.2
    advisor:
      enabled: true

  recovery:
    enabled: true
    max-phases: 4
    ttl:
      sessions: "24h"
      work-states: "7d"

  logging:
    level: "INFO"
    format: "json"
    output: "file"
    file: "logs/harness.log"
```

## Migration Strategy

### 从现有代码迁移

1. **保留现有模块**: shared、cli-native、spring-service
2. **新增模块**: foundation、protocol、security、workflow、collaboration、tools、distribution
3. **渐进式迁移**: 逐步将现有功能迁移到新架构
4. **向后兼容**: 保持现有API兼容性

### 与Go版本对等

- **功能对等**: 90%+核心功能对等
- **性能优化**: 启动时间和内存占用优化
- **API一致性**: Hook协议和工具协议保持一致

## Documentation Requirements

- **用户文档**: 安装、配置、迁移指南
- **API文档**: 完整的API接口文档
- **架构文档**: 7层架构和模块设计文档
- **测试文档**: 测试策略和覆盖率报告

## Success Criteria

项目成功标准：

1. **功能对等性**: 90%+核心功能与Go项目对等
2. **性能达标**: Hook处理<10ms，启动<100ms
3. **质量达标**: 单元测试>75%，集成测试>80%
4. **文档完整**: 用户文档、API文档、架构文档完整
5. **部署验证**: JAR和Native Image双模式验证通过

---

**Spec Version**: 1.0
**Created**: 2026-08-01
**适用项目**: java-harness v4.1.0-SNAPSHOT
