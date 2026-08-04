# Agent 系统设计方案

**设计日期**: 2026-08-04
**版本**: 1.0
**状态**: 已批准
**阶段**: 阶段1（最小实现）

---

## 设计目标

实现 Java Harness 的 Agent 系统，作为智能决策层，Agent 能够：
- 自主决策何时调用哪些 Skill
- 与其他 Agent 协作完成复杂任务
- 追踪和记录执行过程
- 支持生命周期管理

## 架构定位

Agent 系统在 Java Harness 中的位置：

```
┌─────────────────────────────────────────┐
│         Workflow (工作流编排)            │  ← YAML 配置驱动
└─────────────────┬───────────────────────┘
                  ↓ 调用
┌─────────────────────────────────────────┐
│         Agent System (代理系统)         │  ← 智能决策层
│  ┌──────────┬──────────┬──────────────┐ │
│  │  Worker  │ Reviewer │   Advisor    │ │
│  │  Agent   │  Agent   │    Agent    │ │
│  └──────────┴──────────┴──────────────┘ │
└─────────────────┬───────────────────────┘
                  ↓ 调用
┌─────────────────────────────────────────┐
│         Skill System (技能系统)         │  ← 能力执行层
│  ┌────────┬────────┬────────┬────────┐ │
│  │  Plan  │  Work  │ Review │  Sync  │ │
│  │ Skill  │ Skill  │ Skill  │ Skill  │ │
│  └────────┴────────┴────────┴────────┘ │
└─────────────────────────────────────────┘
```

**核心关系**：
- Agent 调用 Skill 来完成任务
- Workflow 编排 Agent 的执行流程
- Agent 之间通过共享上下文协作

---

## 核心设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| **Agent 与 Skill 关系** | Agent 调用 Skill | 清晰的三层架构：Workflow → Agent → Skill |
| **Agent 间通信** | 同步直接调用 + 共享上下文（阶段1） | 简单有效，后续可扩展到异步/事件驱动 |
| **AgentResult** | 分层模型（阶段1最小实现） | 支持扩展，不过度设计 |
| **生命周期** | 基础生命周期 | 支持长时间运行和协作 |
| **AgentContext** | 扩展 SkillContext | 复用已有设计，保持一致性 |

---

## 模块结构

```
java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/
├── agent/                                    # 🆕 Agent 系统
│   ├── framework/                             # Agent 框架核心
│   │   ├── Agent.java                        # Agent 接口
│   │   ├── AgentFramework.java               # Agent 框架
│   │   ├── AgentExecutor.java                # Agent 执行器
│   │   ├── AgentRegistry.java                # Agent 注册表
│   │   ├── AgentLifecycle.java               # Agent 生命周期
│   │   ├── AgentContext.java                 # Agent 上下文
│   │   ├── AgentResult.java                  # Agent 结果
│   │   ├── AgentMessage.java                 # Agent 消息
│   │   ├── AgentConfig.java                  # Agent 配置
│   │   └── AgentExecutionException.java      # Agent 异常
│   │
│   ├── core/                                  # 核心 Agent 实现
│   │   ├── WorkerAgent.java                  # 工作代理
│   │   ├── ReviewerAgent.java                # 审查代理
│   │   └── AdvisorAgent.java                 # 顾问代理
│   │
│   └── coordination/                          # 团队协调
│       └── BreezingTeam.java                 # Breezing 团队编排
│
└── skill/                                    # ✅ 已实现
    ├── framework/
    └── core/
```

---

## 核心接口设计

### 1. Agent 接口

所有 Agent 必须实现的接口：

```java
public interface Agent extends AgentLifecycle {
    String getAgentId();
    String getAgentName();
    String getVersion();
    String getDescription();
    AgentType getAgentType();
    List<String> getRequiredSkills();

    // 核心方法
    AgentResult execute(AgentContext context) throws AgentExecutionException;

    // 可选方法
    default boolean validatePreconditions(AgentContext context) {
        return true;
    }
}
```

**Agent 类型**：
- `WORKER` - 工作代理（执行具体任务）
- `REVIEWER` - 审查代理（审查和评审）
- `ADVISOR` - 顾问代理（提供建议）

### 2. AgentContext 类

扩展 SkillContext，添加 Agent 特有功能：

```java
public class AgentContext extends SkillContext {
    private final SkillFramework skillFramework;  // 调用 Skill 的入口
    private final Map<String, Object> sharedState; // Agent 间共享状态
    private final List<AgentMessage> inbox;        // 来自其他 Agent 的消息
    private final AgentConfig config;              // Agent 配置
    private final String taskId;                   // 任务 ID

    // 便捷方法：调用 Skill
    public SkillResult callSkill(String skillId) {
        return callSkill(skillId, this);
    }
}
```

**继承关系**：
- 复用 SkillContext 的所有字段（userIntent、projectRoot、files、variables）
- 添加 Agent 特有功能（sharedState、skillFramework）

### 3. AgentResult 类（阶段1 最小实现）

```java
public class AgentResult {
    // 基础信息
    private final String agentId;
    private final String executionId;
    private final AgentStatus status;
    private final Instant startTime;
    private final Instant completedTime;

    // 输出信息
    private final Object output;
    private final String errorMessage;

    // 执行追踪
    private final List<SkillCallTrace> skillCalls;
}
```

**Agent 状态**：
- `PENDING` - 待执行
- `RUNNING` - 执行中
- `SUCCESS` - 成功
- `FAILED` - 失败
- `SUCCESS_WITH_WARNINGS` - 成功但有警告
- `PARTIAL_SUCCESS` - 部分成功
- `CANCELLED` - 已取消

---

## Agent 框架设计

### AgentFramework 核心类

```java
public class AgentFramework implements AutoCloseable {
    private final AgentRegistry registry;
    private final AgentExecutor executor;
    private final SkillFramework skillFramework;

    public void initialize();
    public void registerAgent(Agent agent);
    public AgentResult executeAgent(String agentId, AgentContext context);
    public Optional<Agent> findAgent(String agentId);
    public Map<String, AgentRegistry.AgentMetadata> getRegisteredAgents();
}
```

**职责**：
- 管理 Agent 的注册和生命周期
- 协调 Agent 的执行
- 提供查询接口

### AgentExecutor 执行器

```java
public class AgentExecutor {
    public AgentResult execute(Agent agent, AgentContext context);
    public void cancelExecution(String executionId);
    public int getActiveExecutionCount();
}
```

**执行流程**：
1. 验证前置条件
2. 初始化 Agent（如果支持）
3. 执行 Agent.execute()
4. 清理执行上下文
5. 返回结果

### AgentRegistry 注册表

```java
public class AgentRegistry {
    private final Map<String, Agent> agents;
    private final Map<String, AgentMetadata> metadata;

    public void register(Agent agent);
    public Agent getAgent(String agentId);
    public AgentMetadata getMetadata(String agentId);
    public boolean isRegistered(String agentId);
}
```

### AgentLifecycle 生命周期

```java
public interface AgentLifecycle {
    void initialize();                    // 初始化
    default boolean supportsPause();      // 是否支持暂停
    default void pause();                 // 暂停
    default void resume();                // 恢复
    default void cancel();                // 取消
    default void cleanup();               // 清理资源
}
```

---

## 核心 Agent 实现

### WorkerAgent（工作代理）

**职责**：执行具体的工作任务

**工作策略**：
- `PLAN_AND_WORK` - 先规划再工作
- `DIRECT_WORK` - 直接执行工作
- `REVIEW_FIRST` - 先审查再工作

**执行流程**：
1. 分析任务，决定工作策略
2. 根据策略调用相应的 Skill
3. 记录每个 Skill 的调用结果
4. 构建并返回 AgentResult

**示例**：
```java
WorkStrategy strategy = analyzeTask(context);

if (strategy == PLAN_AND_WORK) {
    SkillResult planResult = context.callSkill("plan");
    SkillResult workResult = context.callSkill("work");
    return buildResult(planResult, workResult);
}
```

### ReviewerAgent（审查代理）

**职责**：审查和评审工作成果

**执行流程**：
1. 获取需要审查的内容（从共享状态或上下文）
2. 调用 ReviewSkill 进行审查
3. 分析审查结果
4. 返回审查意见和改进建议

### AdvisorAgent（顾问代理）

**职责**：提供建议和指导

**执行流程**：
1. 分析用户需求
2. 根据需求生成建议
3. 返回建议列表和推荐操作

---

## 团队协作设计

### BreezingTeam（阶段1 简单实现）

**Breezing 协作模式**：
- PlannerAgent - 制定计划
- CriticAgent - 评审计划
- WorkerAgent - 执行工作

**阶段1实现**：
- 只实现 WorkerAgent 的协调
- 提供共享工作空间
- 记录团队执行历史

**执行流程**：
```java
public TeamResult executeTeamTask(TeamTask task) {
    // 1. 创建 Agent 上下文
    AgentContext context = createAgentContext(task);

    // 2. 执行 WorkerAgent
    AgentResult workerResult = agentFramework.executeAgent("worker", context);

    // 3. 将结果写入共享工作空间
    sharedWorkspace.put("workerResult", workerResult.getOutput());

    // 4. 返回团队结果
    return new TeamResult(task.getId(), status, executions, workspace);
}
```

**共享工作空间**：
- Agent 之间传递数据
- 记录中间结果
- 支持协作决策

---

## 数据流和错误处理

### 典型执行流程

```
用户发起任务
  ↓
AgentFramework.executeAgent()
  ↓
WorkerAgent.execute(context)
  - 分析任务 → WorkStrategy
  - 调用 context.callSkill("work")
  ↓
SkillFramework.executeSkill("work")
  ↓
WorkSkill.execute(skillContext)
  - 执行具体工作逻辑
  - 返回 SkillResult
  ↓
WorkerAgent 构建结果
  - 创建 SkillCallTrace
  - 构建 AgentResult
  ↓
返回 AgentResult
```

### 错误处理策略

1. **Skill 执行失败**：记录失败，继续执行（如果可能），返回 PARTIAL_SUCCESS
2. **Agent 执行异常**：捕获异常，返回 FAILED 状态
3. **Agent 未找到**：抛出 AgentNotFoundException
4. **前置条件失败**：返回 FAILED 状态
5. **并发冲突**（阶段2+）：预留接口

### 异常层次

```
AgentExecutionException
├── AgentNotFoundException
├── AgentValidationException
└── AgentLifecycleException
```

---

## 测试策略

### 单元测试覆盖目标

| 组件 | 目标覆盖率 | 关键测试场景 |
|------|-----------|------------|
| AgentFramework | ≥80% | 注册、执行、异常处理 |
| AgentExecutor | ≥70% | 执行流程、并发、错误恢复 |
| WorkerAgent | ≥75% | 策略选择、Skill 调用、结果构建 |
| ReviewerAgent | ≥75% | 审查逻辑、状态判断 |
| AdvisorAgent | ≥70% | 建议生成 |
| AgentContext | ≥60% | Builder 模式、Skill 调用 |
| AgentResult | ≥60% | Builder 模式、状态判断 |
| BreezingTeam | ≥70% | 团队执行、工作空间 |

### 集成测试

- 完整的 Agent 执行流程
- Agent 间协作
- BreezingTeam 端到端测试

---

## 扩展路径

### 阶段2：完整追踪
- 添加 DecisionLog（决策日志）
- 添加 CollaborationHistory（协作历史）
- 添加 QualityMetrics（质量指标）

### 阶段3：高级特性
- 异步消息传递
- 事件驱动通信
- 状态持久化
- 性能分析

---

## 技术债务和注意事项

### 命名约定
- Agent 类以 "Agent" 结尾：WorkerAgent、ReviewerAgent
- AgentResult 类统一命名
- 异常类以 "Exception" 结尾

### 依赖关系
- Agent 系统依赖 Skill 系统
- 必须先初始化 SkillFramework，再初始化 AgentFramework

### 并发考虑
- 阶段1 不支持并发执行
- AgentRegistry 使用 ConcurrentHashMap（为阶段2+准备）

---

## 成功标准

### 功能要求
- ✅ Agent 能够调用 Skill 完成任务
- ✅ Agent 能够自主决策工作策略
- ✅ Agent 能够追踪 Skill 调用
- ✅ Agent 支持生命周期管理
- ✅ BreezingTeam 能够协调 Agent 执行

### 质量要求
- ✅ 单元测试覆盖率 ≥70%
- ✅ 集成测试覆盖核心场景
- ✅ 与 Skill 系统无缝集成
- ✅ 错误处理完善

---

## 验收标准

- [ ] 所有核心接口实现完成
- [ ] 三个核心 Agent 实现（Worker、Reviewer、Advisor）
- [ ] BreezingTeam 基础实现
- [ ] 单元测试覆盖率达标
- [ ] 集成测试通过
- [ ] 与 Skill 系统集成测试通过
- [ ] 文档完整

---

**设计完成时间**: 2026-08-04
**设计版本**: 1.0
**下一步**: 创建实现计划 → 开始阶段1开发
