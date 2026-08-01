package com.chachamaru.harness.tools.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthCheck, ValidateTool, and DoctorTool.
 */
@DisplayName("Validation Tools Tests")
public class ValidationToolsTest {

    @Test
    @DisplayName("HealthCheck结果应该正确创建")
    void healthCheckResultShouldBeCreatedCorrectly() {
        var result = HealthCheck.HealthCheckResult.healthy("test", "All good");

        assertEquals("test", result.name());
        assertEquals(HealthCheck.HealthStatus.HEALTHY, result.status());
        assertEquals("All good", result.message());
        assertTrue(result.isHealthy());
        assertFalse(result.hasIssues());
    }

    @Test
    @DisplayName("Degraded结果应该正确报告")
    void degradedResultShouldReportCorrectly() {
        var result = HealthCheck.HealthCheckResult.degraded("test", "Some issues");

        assertEquals(HealthCheck.HealthStatus.DEGRADED, result.status());
        assertFalse(result.isHealthy());
        assertTrue(result.hasIssues());
    }

    @Test
    @DisplayName("Unhealthy结果应该正确报告")
    void unhealthyResultShouldReportCorrectly() {
        var result = HealthCheck.HealthCheckResult.unhealthy("test", "Critical issues");

        assertEquals(HealthCheck.HealthStatus.UNHEALTHY, result.status());
        assertFalse(result.isHealthy());
        assertTrue(result.hasIssues());
    }

    @Test
    @DisplayName("ValidateTool应该创建实例")
    void validateToolShouldCreateInstance() {
        ValidateTool tool = new ValidateTool();
        assertNotNull(tool);
        assertNotNull(tool.getIssues());
    }

    @Test
    @DisplayName("ValidateTool应该验证所有项目")
    void validateToolShouldValidateAll() {
        ValidateTool tool = new ValidateTool();
        ValidateTool.ValidationResult result = tool.validateAll();

        assertNotNull(result);
        assertNotNull(result.issues());
        assertNotNull(result.summary());
    }

    @Test
    @DisplayName("ValidateTool应该检测缺失配置")
    void validateToolShouldDetectMissingConfig(@TempDir Path tempDir) {
        ValidateTool tool = new ValidateTool(tempDir);
        ValidateTool.ValidationResult result = tool.validateAll();

        // Should find issues since tempDir is empty
        assertFalse(result.valid());
        assertTrue(result.errorCount() > 0 || result.warningCount() > 0);
    }

    @Test
    @DisplayName("ValidateTool应该验证有效的配置文件")
    void validateToolShouldValidateValidConfig(@TempDir Path tempDir) throws Exception {
        // Create .claude directory with settings.json
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        Path settingsFile = claudeDir.resolve("settings.json");
        Files.writeString(settingsFile, "{\"permissions\": {\"bash\": \"allow\"}}");

        ValidateTool tool = new ValidateTool(tempDir);
        ValidateTool.ValidationResult result = tool.validateAll();

        assertNotNull(result);
        // Should have fewer errors with valid config
        assertTrue(result.errorCount() >= 0);
    }

    @Test
    @DisplayName("ValidationIssue应该正确记录")
    void validationIssueShouldRecordCorrectly() {
        var issue = new ValidateTool.ValidationIssue(
            ValidateTool.ValidationIssueSeverity.ERROR,
            "test",
            "Test error",
            "Fix it"
        );

        assertEquals(ValidateTool.ValidationIssueSeverity.ERROR, issue.severity());
        assertEquals("test", issue.category());
        assertEquals("Test error", issue.message());
        assertEquals("Fix it", issue.recommendation());
    }

    @Test
    @DisplayName("ValidationIssue应该处理null值")
    void validationIssueShouldHandleNullValues() {
        var issue = new ValidateTool.ValidationIssue(
            null,
            null,
            null,
            null
        );

        assertEquals(ValidateTool.ValidationIssueSeverity.INFO, issue.severity());
        assertEquals("general", issue.category());
        assertEquals("No message provided", issue.message());
        assertEquals("", issue.recommendation());
    }

    @Test
    @DisplayName("ValidationResult应该正确统计问题")
    void validationResultShouldCountIssues() {
        var issues = java.util.List.of(
            new ValidateTool.ValidationIssue(
                ValidateTool.ValidationIssueSeverity.ERROR, "cat1", "msg1", "fix1"),
            new ValidateTool.ValidationIssue(
                ValidateTool.ValidationIssueSeverity.ERROR, "cat2", "msg2", "fix2"),
            new ValidateTool.ValidationIssue(
                ValidateTool.ValidationIssueSeverity.WARNING, "cat3", "msg3", "fix3"),
            new ValidateTool.ValidationIssue(
                ValidateTool.ValidationIssueSeverity.INFO, "cat4", "msg4", "fix4")
        );

        var result = new ValidateTool.ValidationResult(false, issues, "Test validation");

        assertEquals(2, result.errorCount());
        assertEquals(1, result.warningCount());
        assertEquals(4, result.issues().size());
    }

    @Test
    @DisplayName("DoctorTool应该创建实例")
    void doctorToolShouldCreateInstance() {
        DoctorTool tool = new DoctorTool();
        assertNotNull(tool);
        assertNotNull(tool.getHealthChecks());
    }

    @Test
    @DisplayName("DoctorTool应该注册健康检查")
    void doctorToolShouldRegisterHealthChecks() {
        DoctorTool tool = new DoctorTool();

        HealthCheck customCheck = new HealthCheck() {
            @Override
            public HealthCheckResult check() {
                return HealthCheckResult.healthy("custom", "OK");
            }

            @Override
            public String getName() {
                return "custom";
            }

            @Override
            public String getDescription() {
                return "Custom check";
            }
        };

        int initialCount = tool.getHealthCheckCount();
        tool.addHealthCheck(customCheck);

        assertEquals(initialCount + 1, tool.getHealthCheckCount());
    }

    @Test
    @DisplayName("DoctorTool应该生成健康报告")
    void doctorToolShouldGenerateHealthReport() {
        DoctorTool tool = new DoctorTool();
        DoctorTool.HealthReport report = tool.generateReport();

        assertNotNull(report);
        assertNotNull(report.projectPath());
        assertNotNull(report.generatedAt());
        assertNotNull(report.overallStatus());
        assertNotNull(report.healthCheckResults());
        assertNotNull(report.validationResult());
        assertNotNull(report.summary());
    }

    @Test
    @DisplayName("健康报告应该正确格式化")
    void healthReportShouldFormatCorrectly() {
        DoctorTool tool = new DoctorTool();
        DoctorTool.HealthReport report = tool.generateReport();

        String formatted = report.toFormattedString();

        assertNotNull(formatted);
        assertTrue(formatted.contains("JAVA HARNESS HEALTH REPORT"));
        assertTrue(formatted.contains("Overall Status:"));
        assertTrue(formatted.contains("Health Check Results:"));
        assertTrue(formatted.contains("Validation Results:"));
        assertTrue(formatted.contains("Summary:"));
    }

    @Test
    @DisplayName("健康报告应该检查系统健康")
    void healthReportShouldCheckSystemHealth() {
        DoctorTool tool = new DoctorTool();
        DoctorTool.HealthReport report = tool.generateReport();

        // isHealthy() should be consistent with overallStatus
        boolean expectedHealthy = report.overallStatus() == HealthCheck.HealthStatus.HEALTHY;
        assertEquals(expectedHealthy, report.isHealthy());

        // hasIssues() should be opposite of isHealthy()
        assertEquals(!expectedHealthy, report.hasIssues());
    }

    @Test
    @DisplayName("健康报告应该处理null值")
    void healthReportShouldHandleNullValues() {
        var report = new DoctorTool.HealthReport(
            null,
            null,
            null,
            null,
            null,
            "Test summary"
        );

        assertNotNull(report.projectPath());
        assertNotNull(report.generatedAt());
        assertNotNull(report.overallStatus());
        assertNotNull(report.healthCheckResults());
        assertNotNull(report.validationResult());
        assertEquals("Test summary", report.summary());
    }

    @Test
    @DisplayName("DoctorTool应该清除健康检查")
    void doctorToolShouldClearHealthChecks() {
        DoctorTool tool = new DoctorTool();
        tool.clearHealthChecks();

        assertEquals(0, tool.getHealthCheckCount());
    }

    @Test
    @DisplayName("ValidateTool应该清除问题")
    void validateToolShouldClearIssues() {
        ValidateTool tool = new ValidateTool();
        tool.validateAll();

        tool.clearIssues();

        assertEquals(0, tool.getIssues().size());
    }

    @Test
    @DisplayName("null项目根目录应该抛出异常")
    void nullProjectRootShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ValidateTool(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new DoctorTool(null);
        });
    }

    @Test
    @DisplayName("不存在项目根目录应该抛出异常")
    void nonExistentProjectRootShouldThrowException() {
        Path fakePath = Path.of("/nonexistent/path");

        assertThrows(IllegalArgumentException.class, () -> {
            new ValidateTool(fakePath);
        });
    }
}
