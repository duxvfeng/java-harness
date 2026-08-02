package com.chachamaru.harness.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * 端到端测试基类
 * 提供通用测试环境设置、清理和工具方法
 *
 * <p>端到端测试验证完整的功能路径，从用户输入到系统输出，
 * 确保所有组件正确集成和工作。</p>
 *
 * @spec_reference E2E Testing Framework
 */
public abstract class EndToEndTestBase {

    /**
     * 临时目录，用于测试文件操作
     */
    @TempDir
    protected Path tempDir;

    /**
     * 项目根目录
     */
    protected Path projectRoot;

    /**
     * 测试输出捕获器
     */
    protected ByteArrayOutputStream outputStreamCapture;
    protected PrintStream originalOut;

    /**
     * 设置测试环境
     */
    @BeforeEach
    void setUpBase() {
        // 初始化项目根目录
        projectRoot = Paths.get(System.getProperty("user.dir"));

        // 设置输出捕获
        outputStreamCapture = new ByteArrayOutputStream();
        originalOut = System.out;

        // 重置日志系统
        resetLogging();
    }

    /**
     * 清理测试环境
     */
    @AfterEach
    void tearDownBase() {
        // 恢复原始输出
        if (originalOut != null) {
            System.setOut(originalOut);
        }

        // 清理临时文件
        cleanupTempFiles();
    }

    /**
     * 重置日志系统
     */
    protected void resetLogging() {
        try {
            LogManager.getLogManager().reset();
            Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            globalLogger.setLevel(java.util.logging.Level.OFF);
        } catch (Exception e) {
            // 忽略日志重置错误
        }
    }

    /**
     * 清理临时文件
     */
    protected void cleanupTempFiles() {
        // @TempDir 会自动清理，这里处理其他临时资源
    }

    /**
     * 捕获系统输出
     */
    protected void captureOutput() {
        System.setOut(new PrintStream(outputStreamCapture));
    }

    /**
     * 获取捕获的输出
     */
    protected String getCapturedOutput() {
        System.setOut(originalOut);
        return outputStreamCapture.toString();
    }

    /**
     * 创建测试项目目录结构
     */
    protected Path createTestProjectStructure(String projectName) throws Exception {
        Path projectDir = tempDir.resolve(projectName);

        // 创建标准项目结构
        java.nio.file.Files.createDirectories(projectDir.resolve(".claude"));
        java.nio.file.Files.createDirectories(projectDir.resolve("src"));
        java.nio.file.Files.createDirectories(projectDir.resolve("tests"));

        // 创建基础配置文件
        createTestConfigFile(projectDir);

        return projectDir;
    }

    /**
     * 创建测试配置文件
     */
    protected void createTestConfigFile(Path projectDir) throws Exception {
        Path configFile = projectDir.resolve(".claude").resolve("settings.json");
        String config = """
            {
              "plugins": ["claude-code-harness"],
              "skills": ["harness-work", "harness-plan", "harness-sync"],
              "preferences": {
                "autoCommit": true,
                "testFramework": "junit5"
              }
            }
            """;
        java.nio.file.Files.writeString(configFile, config);
    }

    /**
     * 创建测试 Plans.md 文件
     */
    protected void createTestPlansFile(Path projectDir, String content) throws Exception {
        Path plansFile = projectDir.resolve("Plans.md");
        java.nio.file.Files.writeString(plansFile, content);
    }

    /**
     * 验证项目结构完整性
     */
    protected void assertProjectStructureValid(Path projectDir) {
        assertTrue(java.nio.file.Files.exists(projectDir.resolve(".claude")),
            ".claude 目录应该存在");
        assertTrue(java.nio.file.Files.exists(projectDir.resolve("src")),
            "src 目录应该存在");
        assertTrue(java.nio.file.Files.exists(projectDir.resolve(".claude").resolve("settings.json")),
            "配置文件应该存在");
    }

    /**
     * 等待异步操作完成
     */
    protected void waitForCompletion(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 验证性能要求
     */
    protected void assertPerformanceRequirement(long startTime, long maxDuration) {
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration <= maxDuration,
            String.format("操作应该在 %dms 内完成，实际用时 %dms", maxDuration, duration));
    }

    /**
     * 创建测试工作区
     */
    protected Path createTestWorkspace(String workspaceName) throws Exception {
        Path workspace = tempDir.resolve(workspaceName);
        java.nio.file.Files.createDirectories(workspace);
        return workspace;
    }

    /**
     * 验证测试覆盖率要求
     */
    protected void assertCoverageRequirement(double coverage, double minimum) {
        assertTrue(coverage >= minimum,
            String.format("测试覆盖率 %.2f%% 应该 >= %.2f%%", coverage, minimum));
    }

    /**
     * 获取模块测试类
     */
    protected abstract Class<?> getModuleTestClass();

    /**
     * 获取模块名称
     */
    protected abstract String getModuleName();

    /**
     * 验证模块完整性
     */
    protected void assertModuleIntegrity() {
        try {
            // 验证主类存在
            assertNotNull(Class.forName(getModuleTestClass().getName()),
                getModuleName() + " 主类应该存在");

            // 验证模块能正常加载
            ClassLoader classLoader = getClass().getClassLoader();
            assertNotNull(classLoader,
                "类加载器应该存在");

        } catch (ClassNotFoundException e) {
            fail(getModuleName() + " 模块类未找到: " + e.getMessage());
        }
    }

    /**
     * 简单断言方法（避免导入静态类）
     */
    protected void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    protected void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    protected void assertEquals(Object expected, Object actual, String message) {
        if ((expected == null && actual != null) ||
            (expected != null && !expected.equals(actual))) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    protected void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new AssertionError(message);
        }
    }

    protected void assertNull(Object object, String message) {
        if (object != null) {
            throw new AssertionError(message);
        }
    }

    protected void fail(String message) {
        throw new AssertionError(message);
    }
}