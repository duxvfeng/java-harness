package com.chachamaru.harness.e2e;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * E2E Detection Configuration
 *
 * Configuration loaded from harness.toml, JSON configs, or defaults
 *
 * @since 2.2.0
 */
public class E2EDetectionConfig {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("mode")
    private String mode = "strict";

    @JsonProperty("timeout")
    private int timeout = 120;

    @JsonProperty("retry_on_failure")
    private boolean retryOnFailure = true;

    @JsonProperty("max_retries")
    private int maxRetries = 3;

    @JsonProperty("auto_fix")
    private AutoFixConfig autoFix = new AutoFixConfig();

    @JsonProperty("test_types")
    private TestTypesConfig testTypes = new TestTypesConfig();

    @JsonProperty("triggers")
    private TriggersConfig triggers = new TriggersConfig();

    @JsonProperty("smart_skip")
    private SmartSkipConfig smartSkip = new SmartSkipConfig();

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isRetryOnFailure() {
        return retryOnFailure;
    }

    public void setRetryOnFailure(boolean retryOnFailure) {
        this.retryOnFailure = retryOnFailure;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public AutoFixConfig getAutoFix() {
        return autoFix;
    }

    public void setAutoFix(AutoFixConfig autoFix) {
        this.autoFix = autoFix;
    }

    public TestTypesConfig getTestTypes() {
        return testTypes;
    }

    public void setTestTypes(TestTypesConfig testTypes) {
        this.testTypes = testTypes;
    }

    public TriggersConfig getTriggers() {
        return triggers;
    }

    public void setTriggers(TriggersConfig triggers) {
        this.triggers = triggers;
    }

    // Convenience methods for commonly used properties
    public boolean isAutoFixEnabled() {
        return autoFix != null && autoFix.isEnabled();
    }

    public int getMaxFixIterations() {
        return autoFix != null ? autoFix.getMaxIterations() : 3;
    }

    public boolean isFrontendEnabled() {
        return testTypes != null && testTypes.frontend != null && testTypes.frontend.isEnabled();
    }

    public String getFrontendFramework() {
        return testTypes != null && testTypes.frontend != null ? testTypes.frontend.getFramework() : "playwright";
    }

    public boolean isBackendEnabled() {
        return testTypes != null && testTypes.backend != null && testTypes.backend.isEnabled();
    }

    public String getBackendFramework() {
        return testTypes != null && testTypes.backend != null ? testTypes.backend.getFramework() : "auto";
    }

    public boolean isRequireCleanWorkspace() {
        return triggers != null && triggers.isRequireCleanWorkspace();
    }

    public List<String> getIncludedBranchPatterns() {
        return triggers != null ? triggers.getBranchPatterns().getInclude() : new ArrayList<>();
    }

    public List<String> getExcludedBranchPatterns() {
        return triggers != null ? triggers.getBranchPatterns().getExclude() : new ArrayList<>();
    }

    public boolean isSmartSkipEnabled() {
        return smartSkip != null && smartSkip.isEnabled();
    }

    public SmartSkipConfig getSmartSkip() {
        return smartSkip;
    }

    public void setSmartSkip(SmartSkipConfig smartSkip) {
        this.smartSkip = smartSkip;
    }

    /**
     * Get default configuration
     */
    public static E2EDetectionConfig getDefault() {
        E2EDetectionConfig config = new E2EDetectionConfig();
        config.setEnabled(true);
        config.setMode("strict");
        config.setTimeout(120);
        config.setRetryOnFailure(true);
        config.setMaxRetries(3);

        // Configure auto-fix
        AutoFixConfig autoFix = new AutoFixConfig();
        autoFix.setEnabled(true);
        autoFix.setMaxIterations(3);
        autoFix.setCommitOnFix(true);
        config.setAutoFix(autoFix);

        // Configure test types
        TestTypesConfig testTypes = new TestTypesConfig();

        TestTypeConfig frontend = new TestTypeConfig();
        frontend.setEnabled(true);
        frontend.setFramework("playwright");
        testTypes.setFrontend(frontend);

        TestTypeConfig backend = new TestTypeConfig();
        backend.setEnabled(true);
        backend.setFramework("auto");
        testTypes.setBackend(backend);

        TestTypeConfig integration = new TestTypeConfig();
        integration.setEnabled(true);
        testTypes.setIntegration(integration);

        TestTypeConfig security = new TestTypeConfig();
        security.setEnabled(true);
        testTypes.setSecurity(security);

        config.setTestTypes(testTypes);

        // Configure triggers
        TriggersConfig triggers = new TriggersConfig();
        triggers.setAutoTriggerOnReviewPass(true);
        triggers.setRequireCleanWorkspace(true);
        triggers.setSkipOnDraftPr(true);
        triggers.setSkipOnWipBranch(true);

        BranchPatternsConfig branchPatterns = new BranchPatternsConfig();
        List<String> includePatterns = new ArrayList<>();
        includePatterns.add("feature/*");
        includePatterns.add("bugfix/*");
        includePatterns.add("hotfix/*");
        branchPatterns.setInclude(includePatterns);

        List<String> excludePatterns = new ArrayList<>();
        excludePatterns.add("draft/*");
        excludePatterns.add("wip/*");
        excludePatterns.add("experimental/*");
        branchPatterns.setExclude(excludePatterns);
        triggers.setBranchPatterns(branchPatterns);

        config.setTriggers(triggers);

        return config;
    }

    // Nested configuration classes
    public static class AutoFixConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;

        @JsonProperty("max_iterations")
        private int maxIterations = 3;

        @JsonProperty("fix_timeout")
        private int fixTimeout = 60;

        @JsonProperty("commit_on_fix")
        private boolean commitOnFix = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        public int getFixTimeout() {
            return fixTimeout;
        }

        public void setFixTimeout(int fixTimeout) {
            this.fixTimeout = fixTimeout;
        }

        public boolean isCommitOnFix() {
            return commitOnFix;
        }

        public void setCommitOnFix(boolean commitOnFix) {
            this.commitOnFix = commitOnFix;
        }
    }

    public static class TestTypeConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;

        @JsonProperty("framework")
        private String framework = "auto";

        @JsonProperty("test_paths")
        private List<String> testPaths = new ArrayList<>();

        @JsonProperty("test_scenarios")
        private List<String> testScenarios = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFramework() {
            return framework;
        }

        public void setFramework(String framework) {
            this.framework = framework;
        }

        public List<String> getTestPaths() {
            return testPaths;
        }

        public void setTestPaths(List<String> testPaths) {
            this.testPaths = testPaths;
        }

        public List<String> getTestScenarios() {
            return testScenarios;
        }

        public void setTestScenarios(List<String> testScenarios) {
            this.testScenarios = testScenarios;
        }
    }

    public static class TestTypesConfig {
        @JsonProperty("frontend")
        private TestTypeConfig frontend;

        @JsonProperty("backend")
        private TestTypeConfig backend;

        @JsonProperty("integration")
        private TestTypeConfig integration;

        @JsonProperty("performance")
        private TestTypeConfig performance;

        @JsonProperty("security")
        private TestTypeConfig security;

        public TestTypeConfig getFrontend() {
            return frontend;
        }

        public void setFrontend(TestTypeConfig frontend) {
            this.frontend = frontend;
        }

        public TestTypeConfig getBackend() {
            return backend;
        }

        public void setBackend(TestTypeConfig backend) {
            this.backend = backend;
        }

        public TestTypeConfig getIntegration() {
            return integration;
        }

        public void setIntegration(TestTypeConfig integration) {
            this.integration = integration;
        }

        public TestTypeConfig getPerformance() {
            return performance;
        }

        public void setPerformance(TestTypeConfig performance) {
            this.performance = performance;
        }

        public TestTypeConfig getSecurity() {
            return security;
        }

        public void setSecurity(TestTypeConfig security) {
            this.security = security;
        }
    }

    public static class BranchPatternsConfig {
        @JsonProperty("include")
        private List<String> include = new ArrayList<>();

        @JsonProperty("exclude")
        private List<String> exclude = new ArrayList<>();

        public List<String> getInclude() {
            return include;
        }

        public void setInclude(List<String> include) {
            this.include = include;
        }

        public List<String> getExclude() {
            return exclude;
        }

        public void setExclude(List<String> exclude) {
            this.exclude = exclude;
        }
    }

    public static class TriggersConfig {
        @JsonProperty("auto_trigger_on_review_pass")
        private boolean autoTriggerOnReviewPass = true;

        @JsonProperty("require_clean_workspace")
        private boolean requireCleanWorkspace = true;

        @JsonProperty("skip_on_draft_pr")
        private boolean skipOnDraftPr = true;

        @JsonProperty("skip_on_wip_branch")
        private boolean skipOnWipBranch = true;

        @JsonProperty("branch_patterns")
        private BranchPatternsConfig branchPatterns = new BranchPatternsConfig();

        public boolean isAutoTriggerOnReviewPass() {
            return autoTriggerOnReviewPass;
        }

        public void setAutoTriggerOnReviewPass(boolean autoTriggerOnReviewPass) {
            this.autoTriggerOnReviewPass = autoTriggerOnReviewPass;
        }

        public boolean isRequireCleanWorkspace() {
            return requireCleanWorkspace;
        }

        public void setRequireCleanWorkspace(boolean requireCleanWorkspace) {
            this.requireCleanWorkspace = requireCleanWorkspace;
        }

        public boolean isSkipOnDraftPr() {
            return skipOnDraftPr;
        }

        public void setSkipOnDraftPr(boolean skipOnDraftPr) {
            this.skipOnDraftPr = skipOnDraftPr;
        }

        public boolean isSkipOnWipBranch() {
            return skipOnWipBranch;
        }

        public void setSkipOnWipBranch(boolean skipOnWipBranch) {
            this.skipOnWipBranch = skipOnWipBranch;
        }

        public BranchPatternsConfig getBranchPatterns() {
            return branchPatterns;
        }

        public void setBranchPatterns(BranchPatternsConfig branchPatterns) {
            this.branchPatterns = branchPatterns;
        }
    }

    public static class SmartSkipConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;

        @JsonProperty("detect_project_type")
        private boolean detectProjectType = true;

        @JsonProperty("skip_frontend_if_missing")
        private boolean skipFrontendIfMissing = true;

        @JsonProperty("skip_backend_if_missing")
        private boolean skipBackendIfMissing = false;

        @JsonProperty("min_confidence_threshold")
        private double minConfidenceThreshold = 0.7;

        @JsonProperty("fallback_strategy")
        private String fallbackStrategy = "run_backend"; // run_backend, run_all, skip_all

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDetectProjectType() {
            return detectProjectType;
        }

        public void setDetectProjectType(boolean detectProjectType) {
            this.detectProjectType = detectProjectType;
        }

        public boolean isSkipFrontendIfMissing() {
            return skipFrontendIfMissing;
        }

        public void setSkipFrontendIfMissing(boolean skipFrontendIfMissing) {
            this.skipFrontendIfMissing = skipFrontendIfMissing;
        }

        public boolean isSkipBackendIfMissing() {
            return skipBackendIfMissing;
        }

        public void setSkipBackendIfMissing(boolean skipBackendIfMissing) {
            this.skipBackendIfMissing = skipBackendIfMissing;
        }

        public double getMinConfidenceThreshold() {
            return minConfidenceThreshold;
        }

        public void setMinConfidenceThreshold(double minConfidenceThreshold) {
            this.minConfidenceThreshold = minConfidenceThreshold;
        }

        public String getFallbackStrategy() {
            return fallbackStrategy;
        }

        public void setFallbackStrategy(String fallbackStrategy) {
            this.fallbackStrategy = fallbackStrategy;
        }
    }
}