package com.chachamaru.harness.foundation.script;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 脚本路径管理器 - 管理项目中所有脚本的路径
 *
 * <p>支持的脚本分类：</p>
 * <ul>
 *   <li>build - 构建相关脚本</li>
 *   <li>test - 测试相关脚本</li>
 *   <li>ci - CI/CD 相关脚本</li>
 *   <li>project - 项目管理脚本</li>
 *   <li>session - 会话管理脚本</li>
 *   <li>plan - 计划管理脚本</li>
 *   <li>review - 审查相关脚本</li>
 *   <li>service - 服务管理脚本</li>
 *   <li>util - 工具脚本</li>
 *   <li>verify - 验证脚本</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class ScriptPathManager {

    private static final String SCRIPTS_BASE_DIR = "scripts";
    private static final String PROJECT_ROOT = detectProjectRoot();

    private static final Map<String, String> SCRIPT_PATHS = new HashMap<>();

    static {
        // 初始化所有脚本路径
        initializeScriptPaths();
    }

    /**
     * 检测项目根目录
     */
    private static String detectProjectRoot() {
        Path currentPath = Paths.get("").toAbsolutePath().normalize();

        // 如果当前目录已经是项目根目录（包含 pom.xml）
        if (currentPath.resolve("pom.xml").toFile().exists()) {
            return currentPath.toString();
        }

        // 如果当前目录在子模块中，向上查找项目根目录
        Path path = currentPath;
        while (path != null && path.getParent() != null) {
            if (path.resolve("pom.xml").toFile().exists() &&
                path.resolve("scripts").toFile().exists()) {
                return path.toString();
            }
            path = path.getParent();
        }

        // 如果找不到，返回当前目录的父目录（假设在子模块中）
        Path parentPath = currentPath.getParent();
        if (parentPath != null && parentPath.resolve("pom.xml").toFile().exists()) {
            return parentPath.toString();
        }

        // 最后回退到当前目录
        return currentPath.toString();
    }

    /**
     * 初始化脚本路径映射
     */
    private static void initializeScriptPaths() {
        // Build scripts
        SCRIPT_PATHS.put("build", "build/build.sh");
        SCRIPT_PATHS.put("compile", "build/compile.sh");
        SCRIPT_PATHS.put("clean", "build/clean.sh");
        SCRIPT_PATHS.put("package", "build/package.sh");

        // Test scripts
        SCRIPT_PATHS.put("test", "test/test.sh");
        SCRIPT_PATHS.put("run-tests", "test/run-tests.sh");
        SCRIPT_PATHS.put("test-integration", "test/test-integration.sh");
        SCRIPT_PATHS.put("test-setup", "test/test-setup.sh");
        SCRIPT_PATHS.put("test-report", "test/test-report.sh");
        SCRIPT_PATHS.put("auto-test-runner", "test/auto-test-runner.sh");

        // CI/CD scripts
        SCRIPT_PATHS.put("ci-build", "ci/ci-build.sh");
        SCRIPT_PATHS.put("ci-test", "ci/ci-test.sh");
        SCRIPT_PATHS.put("ci-deploy", "ci/ci-deploy.sh");
        SCRIPT_PATHS.put("release-preflight", "ci/release-preflight.sh");

        // Project management scripts
        SCRIPT_PATHS.put("init-project", "project/init-project.sh");
        SCRIPT_PATHS.put("setup-existing-project", "project/setup-existing-project.sh");
        SCRIPT_PATHS.put("analyze-project", "project/analyze-project.sh");
        SCRIPT_PATHS.put("project-analyzer", "project/project-analyzer.sh");

        // Session management scripts
        SCRIPT_PATHS.put("session-init", "session/session-init.sh");
        SCRIPT_PATHS.put("session-monitor", "session/session-monitor.sh");
        SCRIPT_PATHS.put("session-cleanup", "session/session-cleanup.sh");
        SCRIPT_PATHS.put("session-history", "session/session-history.sh");
        SCRIPT_PATHS.put("session-status", "session/session-status.sh");

        // Plan management scripts
        SCRIPT_PATHS.put("plan-registry", "plan/plan-registry.sh");
        SCRIPT_PATHS.put("plan-switch", "plan/plan-switch.sh");
        SCRIPT_PATHS.put("plan-templates", "plan/plan-templates.sh");
        SCRIPT_PATHS.put("plans-watcher", "plan/plans-watcher.sh");

        // Review scripts
        SCRIPT_PATHS.put("judgment-card", "review/judgment-card.sh");
        SCRIPT_PATHS.put("review-summary", "review/review-summary.sh");
        SCRIPT_PATHS.put("code-quality", "review/code-quality.sh");

        // Service management scripts
        SCRIPT_PATHS.put("start-service", "service/start-service.sh");
        SCRIPT_PATHS.put("stop-service", "service/stop-service.sh");

        // Utility scripts
        SCRIPT_PATHS.put("config-manager", "util/config-manager.sh");
        SCRIPT_PATHS.put("dependencies", "util/dependencies.sh");
        SCRIPT_PATHS.put("doc-generator", "util/doc-generator.sh");
        SCRIPT_PATHS.put("install", "util/install.sh");
        SCRIPT_PATHS.put("progress-snapshot", "util/progress-snapshot.sh");
        SCRIPT_PATHS.put("render-html", "util/render-html.sh");

        // Verification scripts
        SCRIPT_PATHS.put("verify-workflow-system", "verify/verify-workflow-system.sh");
        SCRIPT_PATHS.put("verify-workflows", "verify/verify-workflows.sh");
        SCRIPT_PATHS.put("verify", "verify/verify.sh");
    }

    /**
     * 获取脚本的完整路径
     *
     * @param scriptName 脚本名称
     * @return 脚本的完整路径
     */
    public static String getScriptPath(String scriptName) {
        String relativePath = SCRIPT_PATHS.get(scriptName);
        if (relativePath == null) {
            throw new IllegalArgumentException("未知的脚本名称: " + scriptName);
        }
        return Paths.get(PROJECT_ROOT, SCRIPTS_BASE_DIR, relativePath).toString();
    }

    /**
     * 获取脚本的相对路径（相对于项目根目录）
     *
     * @param scriptName 脚本名称
     * @return 脚本的相对路径
     */
    public static String getScriptRelativePath(String scriptName) {
        String relativePath = SCRIPT_PATHS.get(scriptName);
        if (relativePath == null) {
            throw new IllegalArgumentException("未知的脚本名称: " + scriptName);
        }
        return Paths.get(SCRIPTS_BASE_DIR, relativePath).toString();
    }

    /**
     * 检查脚本是否存在
     *
     * @param scriptName 脚本名称
     * @return 脚本是否存在
     */
    public static boolean scriptExists(String scriptName) {
        try {
            String fullPath = getScriptPath(scriptName);
            File scriptFile = new File(fullPath);
            return scriptFile.exists() && scriptFile.isFile() && scriptFile.canExecute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取所有可用的脚本名称
     *
     * @return 脚本名称数组
     */
    public static String[] getAvailableScripts() {
        return SCRIPT_PATHS.keySet().toArray(new String[0]);
    }

    /**
     * 按分类获取脚本
     *
     * @param category 分类名称 (build, test, ci, etc.)
     * @return 该分类下的脚本名称数组
     */
    public static String[] getScriptsByCategory(String category) {
        return SCRIPT_PATHS.entrySet().stream()
            .filter(entry -> entry.getValue().startsWith(category + "/"))
            .map(Map.Entry::getKey)
            .toArray(String[]::new);
    }

    /**
     * 获取所有分类
     *
     * @return 分类名称数组
     */
    public static String[] getCategories() {
        return new String[]{"build", "test", "ci", "project", "session", "plan", "review", "service", "util", "verify"};
    }

    /**
     * 获取项目根目录
     *
     * @return 项目根目录路径
     */
    public static String getProjectRoot() {
        return PROJECT_ROOT;
    }

    /**
     * 获取脚本基础目录
     *
     * @return 脚本基础目录
     */
    public static String getScriptsBaseDir() {
        return Paths.get(PROJECT_ROOT, SCRIPTS_BASE_DIR).toString();
    }
}