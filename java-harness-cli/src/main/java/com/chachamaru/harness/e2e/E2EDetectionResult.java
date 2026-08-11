package com.chachamaru.harness.e2e;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * E2E Detection Result
 *
 * Represents the result of an end-to-end detection run
 *
 * @since 2.2.0
 */
public class E2EDetectionResult {

    @JsonProperty("schema_version")
    private String schemaVersion = "e2e-detection-result.v1";

    @JsonProperty("detection_id")
    private String detectionId;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("status")
    private String status; // PASS, FAIL, ERROR, SKIPPED

    @JsonProperty("execution_time_ms")
    private long executionTimeMs;

    @JsonProperty("test_results")
    private TestResults testResults = new TestResults();

    @JsonProperty("critical_issues")
    private List<CriticalIssue> criticalIssues = new ArrayList<>();

    @JsonProperty("performance_metrics")
    private PerformanceMetrics performanceMetrics = new PerformanceMetrics();

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    // Constructors
    public E2EDetectionResult() {
        this.timestamp = Instant.now().toString();
    }

    public E2EDetectionResult(String detectionId, String status, long executionTimeMs) {
        this();
        this.detectionId = detectionId;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
    }

    // Factory methods
    public static E2EDetectionResult passed(String detectionId, long executionTimeMs) {
        return new E2EDetectionResult(detectionId, "PASS", executionTimeMs);
    }

    public static E2EDetectionResult failed(String detectionId, String reason, long executionTimeMs) {
        E2EDetectionResult result = new E2EDetectionResult(detectionId, "FAIL", executionTimeMs);
        result.setMessage(reason);
        return result;
    }

    public static E2EDetectionResult error(String detectionId, String errorMessage) {
        E2EDetectionResult result = new E2EDetectionResult(detectionId, "ERROR", 0);
        result.setError(errorMessage);
        result.setMessage(errorMessage);
        return result;
    }

    public static E2EDetectionResult skipped(String detectionId, String reason) {
        E2EDetectionResult result = new E2EDetectionResult(detectionId, "SKIPPED", 0);
        result.setMessage(reason);
        return result;
    }

    // Getters and setters
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public TestResults getTestResults() {
        return testResults;
    }

    public void setTestResults(TestResults testResults) {
        this.testResults = testResults;
    }

    public List<CriticalIssue> getCriticalIssues() {
        return criticalIssues;
    }

    public void setCriticalIssues(List<CriticalIssue> criticalIssues) {
        this.criticalIssues = criticalIssues;
    }

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Convenience methods
    public boolean isPassed() {
        return "PASS".equals(status);
    }

    public boolean isFailed() {
        return "FAIL".equals(status);
    }

    public boolean isError() {
        return "ERROR".equals(status);
    }

    public boolean isSkipped() {
        return "SKIPPED".equals(status);
    }

    public boolean hasCriticalIssues() {
        return criticalIssues != null && !criticalIssues.isEmpty();
    }

    /**
     * Parse from JSON
     */
    public static E2EDetectionResult fromJson(String json, String detectionId, long executionTimeMs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            E2EDetectionResult result = mapper.readValue(json, E2EDetectionResult.class);
            result.setDetectionId(detectionId);
            result.setExecutionTimeMs(executionTimeMs);
            return result;
        } catch (Exception e) {
            // If JSON parsing fails, create a result from the content
            if (json.contains("PASS") || json.contains("✅")) {
                return passed(detectionId, executionTimeMs);
            } else if (json.contains("FAIL") || json.contains("❌")) {
                return failed(detectionId, "Detection failed based on output analysis", executionTimeMs);
            } else {
                return error(detectionId, "Unable to parse detection result");
            }
        }
    }

    /**
     * Convert to JSON
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize result\"}";
        }
    }

    // Nested classes for detailed results
    public static class TestResults {
        @JsonProperty("frontend")
        private TestTypeResult frontend;

        @JsonProperty("backend")
        private TestTypeResult backend;

        @JsonProperty("integration")
        private TestTypeResult integration;

        @JsonProperty("performance")
        private TestTypeResult performance;

        @JsonProperty("security")
        private TestTypeResult security;

        public TestTypeResult getFrontend() {
            return frontend;
        }

        public void setFrontend(TestTypeResult frontend) {
            this.frontend = frontend;
        }

        public TestTypeResult getBackend() {
            return backend;
        }

        public void setBackend(TestTypeResult backend) {
            this.backend = backend;
        }

        public TestTypeResult getIntegration() {
            return integration;
        }

        public void setIntegration(TestTypeResult integration) {
            this.integration = integration;
        }

        public TestTypeResult getPerformance() {
            return performance;
        }

        public void setPerformance(TestTypeResult performance) {
            this.performance = performance;
        }

        public TestTypeResult getSecurity() {
            return security;
        }

        public void setSecurity(TestTypeResult security) {
            this.security = security;
        }
    }

    public static class TestTypeResult {
        @JsonProperty("status")
        private String status;

        @JsonProperty("tests_run")
        private int testsRun;

        @JsonProperty("tests_passed")
        private int testsPassed;

        @JsonProperty("tests_failed")
        private int testsFailed;

        @JsonProperty("execution_time_ms")
        private long executionTimeMs;

        @JsonProperty("framework")
        private String framework;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getTestsRun() {
            return testsRun;
        }

        public void setTestsRun(int testsRun) {
            this.testsRun = testsRun;
        }

        public int getTestsPassed() {
            return testsPassed;
        }

        public void setTestsPassed(int testsPassed) {
            this.testsPassed = testsPassed;
        }

        public int getTestsFailed() {
            return testsFailed;
        }

        public void setTestsFailed(int testsFailed) {
            this.testsFailed = testsFailed;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public String getFramework() {
            return framework;
        }

        public void setFramework(String framework) {
            this.framework = framework;
        }
    }

    public static class CriticalIssue {
        @JsonProperty("severity")
        private String severity; // critical, major, minor

        @JsonProperty("category")
        private String category;

        @JsonProperty("description")
        private String description;

        @JsonProperty("file")
        private String file;

        @JsonProperty("line")
        private int line;

        @JsonProperty("suggestion")
        private String suggestion;

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    public static class PerformanceMetrics {
        @JsonProperty("response_time_ms")
        private long responseTimeMs;

        @JsonProperty("memory_usage_mb")
        private long memoryUsageMb;

        @JsonProperty("cpu_usage_percent")
        private double cpuUsagePercent;

        @JsonProperty("concurrent_users")
        private int concurrentUsers;

        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        public void setResponseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }

        public long getMemoryUsageMb() {
            return memoryUsageMb;
        }

        public void setMemoryUsageMb(long memoryUsageMb) {
            this.memoryUsageMb = memoryUsageMb;
        }

        public double getCpuUsagePercent() {
            return cpuUsagePercent;
        }

        public void setCpuUsagePercent(double cpuUsagePercent) {
            this.cpuUsagePercent = cpuUsagePercent;
        }

        public int getConcurrentUsers() {
            return concurrentUsers;
        }

        public void setConcurrentUsers(int concurrentUsers) {
            this.concurrentUsers = concurrentUsers;
        }
    }
}