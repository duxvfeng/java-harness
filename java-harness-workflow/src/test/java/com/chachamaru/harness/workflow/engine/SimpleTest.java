import java.util.Map;
import java.util.HashMap;

public class SimpleTest {
    public static void main(String[] args) {
        ExecutionContext context = new ExecutionContext();
        
        // 测试1: 变量管理
        context.setVariable("project_type", "new");
        context.setVariable("task_count", 5);
        assert "new".equals(context.getVariable("project_type"));
        assert 5 == context.getVariable("task_count");
        System.out.println("✓ Variable management works");
        
        // 测试2: 批量设置
        Map<String, Object> vars = new HashMap<>();
        vars.put("flag", true);
        vars.put("name", "Alice");
        context.setVariables(vars);
        assert true == context.getVariable("flag");
        System.out.println("✓ Batch variable setting works");
        
        // 测试3: 子上下文
        ExecutionContext child = context.createChildContext();
        assert "new".equals(child.getVariable("project_type"));
        child.setVariable("child_only", "value");
        assert null == context.getVariable("child_only");
        System.out.println("✓ Child context isolation works");
        
        // 测试4: 模板渲染（需要 VariableResolver）
        // context.setVariable("name", "Test");
        // String result = context.renderTemplate("Hello ${name}");
        // System.out.println("Template result: " + result);
        
        // 测试5: 执行栈
        context.pushExecution("step1");
        context.pushExecution("step2");
        assert "step2".equals(context.getCurrentExecution());
        context.popExecution();
        assert "step1".equals(context.getCurrentExecution());
        System.out.println("✓ Execution stack works");
        
        // 测试6: 文件和会话上下文
        context.setFileContext("test.md", "content");
        context.setSessionState("phase", "7.1.2");
        assert "content".equals(context.getFileContext("test.md"));
        assert "7.1.2".equals(context.getSessionState("phase"));
        System.out.println("✓ File and session context works");
        
        System.out.println("\n✅ All core functionality verified!");
    }
}
