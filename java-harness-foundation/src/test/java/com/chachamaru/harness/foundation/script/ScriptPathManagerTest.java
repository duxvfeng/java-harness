package com.chachamaru.harness.foundation.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScriptPathManager 单元测试
 *
 * @since 4.0.0
 */
class ScriptPathManagerTest {

    @Test
    void testGetScriptPath() {
        String path = ScriptPathManager.getScriptPath("build");
        assertNotNull(path);
        assertTrue(path.contains("scripts"));
        assertTrue(path.contains("build.sh"));
    }

    @Test
    void testGetScriptRelativePath() {
        String relativePath = ScriptPathManager.getScriptRelativePath("test");
        assertNotNull(relativePath);
        assertTrue(relativePath.startsWith("scripts/"));
        assertTrue(relativePath.endsWith("test.sh"));
    }

    @Test
    void testScriptExists() {
        // 测试存在的脚本
        assertTrue(ScriptPathManager.scriptExists("build"));
        assertTrue(ScriptPathManager.scriptExists("test"));
        assertTrue(ScriptPathManager.scriptExists("clean"));

        // 测试不存在的脚本
        assertFalse(ScriptPathManager.scriptExists("nonexistent"));
    }

    @Test
    void testGetAvailableScripts() {
        String[] scripts = ScriptPathManager.getAvailableScripts();
        assertNotNull(scripts);
        assertTrue(scripts.length > 30); // 我们有39个脚本

        // 检查一些关键的脚本
        assertTrue(java.util.Arrays.asList(scripts).contains("build"));
        assertTrue(java.util.Arrays.asList(scripts).contains("test"));
        assertTrue(java.util.Arrays.asList(scripts).contains("session-init"));
    }

    @Test
    void testGetScriptsByCategory() {
        String[] buildScripts = ScriptPathManager.getScriptsByCategory("build");
        assertNotNull(buildScripts);
        assertTrue(buildScripts.length >= 4); // build, compile, clean, package
        assertTrue(java.util.Arrays.asList(buildScripts).contains("build"));
        assertTrue(java.util.Arrays.asList(buildScripts).contains("clean"));

        String[] testScripts = ScriptPathManager.getScriptsByCategory("test");
        assertNotNull(testScripts);
        assertTrue(testScripts.length >= 6); // test, run-tests, etc.
        assertTrue(java.util.Arrays.asList(testScripts).contains("test"));
        assertTrue(java.util.Arrays.asList(testScripts).contains("auto-test-runner"));
    }

    @Test
    void testGetCategories() {
        String[] categories = ScriptPathManager.getCategories();
        assertNotNull(categories);
        assertEquals(10, categories.length); // 10个分类
        assertTrue(java.util.Arrays.asList(categories).contains("build"));
        assertTrue(java.util.Arrays.asList(categories).contains("test"));
        assertTrue(java.util.Arrays.asList(categories).contains("ci"));
    }

    @Test
    void testGetProjectRoot() {
        String projectRoot = ScriptPathManager.getProjectRoot();
        assertNotNull(projectRoot);
        assertFalse(projectRoot.isEmpty());
    }

    @Test
    void testGetScriptsBaseDir() {
        String baseDir = ScriptPathManager.getScriptsBaseDir();
        assertNotNull(baseDir);
        assertTrue(baseDir.contains("scripts"));
    }

    @Test
    void testInvalidScriptName() {
        assertThrows(IllegalArgumentException.class, () -> {
            ScriptPathManager.getScriptPath("invalid-script-name");
        });
    }

    @Test
    void testSessionManagementScripts() {
        String[] sessionScripts = ScriptPathManager.getScriptsByCategory("session");
        assertTrue(sessionScripts.length >= 5);
        assertTrue(java.util.Arrays.asList(sessionScripts).contains("session-init"));
        assertTrue(java.util.Arrays.asList(sessionScripts).contains("session-monitor"));
        assertTrue(java.util.Arrays.asList(sessionScripts).contains("session-cleanup"));
    }

    @Test
    void testPlanManagementScripts() {
        String[] planScripts = ScriptPathManager.getScriptsByCategory("plan");
        assertTrue(planScripts.length >= 4);
        assertTrue(java.util.Arrays.asList(planScripts).contains("plan-registry"));
        assertTrue(java.util.Arrays.asList(planScripts).contains("plans-watcher"));
    }

    @Test
    void testAllMajorCategories() {
        // 确保所有主要分类都有脚本
        String[] categories = ScriptPathManager.getCategories();
        for (String category : categories) {
            String[] scripts = ScriptPathManager.getScriptsByCategory(category);
            assertTrue(scripts.length > 0, "分类 " + category + " 应该有脚本");
        }
    }
}