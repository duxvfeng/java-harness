# 工作流自定义指南

## 🎯 无需代码调整的自定义

### 1. 创建新工作流 (无需改代码)

```bash
# 在 workflows/default/ 下创建新工作流
cp workflows/default/work.yaml workflows/default/my-custom-workflow.yaml
```

### 2. 自定义现有工作流 (无需改代码)

**编辑 workflows/default/work.yaml**:
```yaml
steps:
  # 添加新的检查步骤
  - id: my-custom-check
    skill: custom-review-skill
    condition: "env == 'production'"
    input:
      files: ["README.md", "CLAUDE.md"]
    mode: optional
    parallel: true

  # 修改步骤顺序
  - id: reorganize-files
    skill: file-organizer
    input:
      context_from:
        - previous_step_output
    mode: required
    depends: "my-custom-check"  # 添加依赖关系
```

### 3. 高级条件表达式 (无需改代码)

```yaml
# 支持的复杂条件表达式
condition: "user_choice == 'yes' && has_permission"
condition: "(tech_stack.includes('react') || tech_stack.includes('vue')) && deployment_ready"
condition: "!error_occurred && (is_weekend || business_hours)"
condition: "file_count > 10 && (auto_approve || admin_approved)"
```

### 4. 循环执行 (无需改代码)

```yaml
steps:
  - id: process-all-items
    skill: batch-processor
    input:
      variables:
        - items_list
    loop: "items_list"  # 循环执行每个项目
    mode: required
```

### 5. 并行执行 (无需改代码)

```yaml
steps:
  # 并行执行的步骤组
  - id: security-scan
    skill: security-checker
    parallel: true
    input:
      files: "{{changed_files}}"

  - id: performance-scan
    skill: performance-checker
    parallel: true
    input:
      files: "{{changed_files}}"

  - id: quality-scan
    skill: quality-checker
    parallel: true
    input:
      files: "{{changed_files}}"
```

## 🔧 需要代码调整的自定义

### 1. 扩展条件求值引擎

**场景**: 需要支持新的条件运算符或自定义条件函数

**需要修改**: `ConditionExpressionEvaluator.java`

```java
// 在 ConditionExpressionEvaluator 中添加自定义条件
public boolean evaluateCustomCondition(String expression, ExecutionContext context) {
    if (expression.startsWith("is_weekend")) {
        return isWeekend();  // 自定义方法
    }
    if (expression.startsWith("has_permission:")) {
        String permission = expression.substring(15);
        return checkPermission(permission, context);
    }
    // 回退到默认求值
    return evaluate(expression, context);
}
```

### 2. 添加新的工作流执行策略

**场景**: 需要自定义步骤执行逻辑

**需要修改**: `WorkflowEngine.java`

```java
// 在 WorkflowEngine 中添加自定义执行策略
@Override
protected WorkflowStepExecution executeStep(Workflow.WorkflowStep step, ExecutionContext context) {
    // 自定义执行前逻辑
    if (step.getId().startsWith("critical-")) {
        context.setVariable("require_approval", true);
    }

    // 执行步骤
    WorkflowStepExecution result = super.executeStep(step, context);

    // 自定义执行后逻辑
    if (result.isSuccess() && step.getMode().equals("required")) {
        updateSuccessMetrics(step.getId());
    }

    return result;
}
```

### 3. 注册自定义技能或代理

**场景**: 需要添加新的业务逻辑组件

**需要修改**: 相应的注册表类

```java
// 在自定义技能中注册
SkillFramework framework = new SkillFramework();
framework.registerSkill(new MyCustomSkill());

// 在自定义代理中注册
AgentRegistry agentRegistry = new AgentRegistry();
agentRegistry.registerAgent(new MyCustomAgent());
```

## 📋 自定义决策树

```
自定义需求判断流程:

1. 仅修改工作流流程?
   YES → 直接编辑 YAML 文件，无需改代码 ✅

2. 需要新的条件逻辑?
   YES → 检查现有条件引擎是否满足
        ├─ 满足 → 使用复杂条件表达式 ✅
        └─ 不满足 → 扩展 ConditionExpressionEvaluator ⚠️

3. 需要新的执行行为?
   YES → 检查工作流引擎是否满足
        ├─ 满足 → 使用模式设置 (parallel/loop) ✅
        └─ 不满足 → 扩展 WorkflowEngine ⚠️

4. 需要新的业务能力?
   YES → 创建新的技能/代理
        └─ 在相应注册表中注册 ⚠️
```

## 🎯 推荐做法

### 对于简单流程定制 (推荐)
```bash
# 1. 复制现有工作流
cp workflows/default/work.yaml workflows/default/my-work.yaml

# 2. 编辑步骤和条件
vim workflows/default/my-work.yaml

# 3. 在技能系统中引用
# 工作流会自动发现和执行
```

### 对于复杂逻辑定制 (高级)
```java
// 1. 创建自定义技能
public class MyCustomSkill implements Skill {
    @Override
    public SkillResult execute(SkillContext context) {
        // 自定义业务逻辑
        return new SkillResult(true, "执行成功");
    }
}

// 2. 在工作流中引用
# skill: my-custom-skill
```

### 对于系统级扩展 (专家)
```java
// 扩展工作流引擎核心能力
public class CustomWorkflowEngine extends WorkflowEngine {
    @Override
    public WorkflowExecutionResult executeWorkflow(...) {
        // 自定义执行策略
    }
}
```

## 🔍 验证自定义工作流

```bash
# 验证工作流语法
java -jar java-harness-workflow-*.jar \
  -cp java-harness-workflow/target/classes \
  com.chachamaru.harness.workflow.loader.WorkflowLoader \
  workflows/default/my-workflow.yaml

# 查看所有可用工作流
ls -la workflows/default/

# 运行集成测试
mvn test -Dtest=WorkflowIntegrationExample
```

## ✅ 总结

**大多数情况下无需调整代码**！只需要：

1. ✅ **复制/编辑 YAML 文件** - 流程定制
2. ✅ **使用复杂条件表达式** - 逻辑定制  
3. ✅ **设置模式和参数** - 行为定制
4. ✅ **创建技能/代理** - 业务逻辑定制

**仅在需要系统级扩展时才需要调整 Java 代码**，这种情况很少见。