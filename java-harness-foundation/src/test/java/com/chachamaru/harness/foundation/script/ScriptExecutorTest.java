package com.chachamaru.harness.foundation.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScriptExecutor 单元测试
 *
 * @since 4.0.0
 */
class ScriptExecutorTest {

    @Test
    void testExecuteSimpleScript() {
        // 测试执行一个简单的脚本
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("build", new String[]{"--help"});

        assertNotNull(result);
        assertNotNull(result.getScriptPath());
        assertNotNull(result.getOutput());
        assertTrue(result.getExecutionTime() >= 0);
    }

    @Test
    void testExecuteWithArguments() {
        String[] args = {"version", "check"};
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("config-manager", args);

        assertNotNull(result);
        assertTrue(result.getScriptPath().contains("config-manager.sh"));
    }

    @Test
    void testExecuteNonExistentScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            ScriptExecutor.execute("nonexistent-script");
        });
    }

    @Test
    void testScriptResultStructure() {
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("clean");

        assertNotNull(result.getScriptPath());
        assertNotNull(result.getOutput());
        assertNotNull(result.getExitCode());
        assertNotNull(result.getExecutionTime());
        assertNotNull(result.isSuccess());
    }

    @Test
    void testScriptResultToString() {
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("test");

        String toString = result.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ScriptResult"));
        assertTrue(toString.contains("scriptPath"));
    }

    @Test
    void testExecuteMultipleScripts() {
        // 测试连续执行多个脚本
        ScriptExecutor.ScriptResult result1 = ScriptExecutor.execute("build", new String[]{"--version"});
        ScriptExecutor.ScriptResult result2 = ScriptExecutor.execute("test", new String[]{"--help"});

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getScriptPath(), result2.getScriptPath());
    }

    @Test
    void testScriptCategories() {
        // 测试不同分类的脚本都能执行
        String[] categories = ScriptPathManager.getCategories();

        for (String category : categories) {
            String[] scripts = ScriptPathManager.getScriptsByCategory(category);
            if (scripts.length > 0) {
                ScriptExecutor.ScriptResult result = ScriptExecutor.execute(scripts[0], new String[]{"--help"});
                assertNotNull(result, "分类 " + category + " 的脚本 " + scripts[0] + " 应该能执行");
            }
        }
    }

    @Test
    void testExecuteWithEnvironmentVariables() {
        // 测试带环境变量的脚本执行
        java.util.Map<String, String> envVars = java.util.Map.of("TEST_VAR", "test_value");
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("build", null, envVars);

        assertNotNull(result);
    }

    @Test
    void testExecuteTimeout() {
        // 测试超时控制
        ScriptExecutor.ScriptResult result = ScriptExecutor.execute("clean", null, 30);

        assertNotNull(result);
        assertTrue(result.getExecutionTime() < 30000); // 应该在30秒内完成
    }
}