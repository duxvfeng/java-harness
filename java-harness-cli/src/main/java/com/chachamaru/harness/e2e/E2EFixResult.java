package com.chachamaru.harness.e2e;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * E2E Fix Result
 *
 * Represents the result of an automatic fix attempt
 *
 * @since 2.2.0
 */
public class E2EFixResult {

    @JsonProperty("schema_version")
    private String schemaVersion = "e2e-fix-result.v1";

    @JsonProperty("detection_id")
    private String detectionId;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("fix_attempted")
    private boolean fixAttempted;

    @JsonProperty("fix_successful")
    private boolean fixSuccessful;

    @JsonProperty("iterations")
    private int iterations;

    @JsonProperty("max_iterations")
    private int maxIterations;

    @JsonProperty("fixes_applied")
    private List<FixApplied> fixesApplied = new ArrayList<>();

    @JsonProperty("remaining_issues")
    private int remainingIssues;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("commit_created")
    private boolean commitCreated;

    @JsonProperty("commit_hash")
    private String commitHash;

    // Constructors
    public E2EFixResult() {
        this.timestamp = Instant.now().toString();
    }

    public E2EFixResult(String detectionId, boolean fixSuccessful, int iterations) {
        this();
        this.detectionId = detectionId;
        this.fixSuccessful = fixSuccessful;
        this.iterations = iterations;
    }

    // Factory methods
    public static E2EFixResult successful(String detectionId, int iterations, List<FixApplied> fixes) {
        E2EFixResult result = new E2EFixResult(detectionId, true, iterations);
        result.setFixAttempted(true);
        result.setFixesApplied(fixes);
        result.setRemainingIssues(0);
        return result;
    }

    public static E2EFixResult partial(String detectionId, int iterations, List<FixApplied> fixes, int remainingIssues) {
        E2EFixResult result = new E2EFixResult(detectionId, false, iterations);
        result.setFixAttempted(true);
        result.setFixesApplied(fixes);
        result.setRemainingIssues(remainingIssues);
        return result;
    }

    public static E2EFixResult failed(String errorMessage) {
        E2EFixResult result = new E2EFixResult();
        result.setFixAttempted(false);
        result.setFixSuccessful(false);
        result.setErrorMessage(errorMessage);
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

    public boolean isFixAttempted() {
        return fixAttempted;
    }

    public void setFixAttempted(boolean fixAttempted) {
        this.fixAttempted = fixAttempted;
    }

    public boolean isFixSuccessful() {
        return fixSuccessful;
    }

    public void setFixSuccessful(boolean fixSuccessful) {
        this.fixSuccessful = fixSuccessful;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public List<FixApplied> getFixesApplied() {
        return fixesApplied;
    }

    public void setFixesApplied(List<FixApplied> fixesApplied) {
        this.fixesApplied = fixesApplied;
    }

    public int getRemainingIssues() {
        return remainingIssues;
    }

    public void setRemainingIssues(int remainingIssues) {
        this.remainingIssues = remainingIssues;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isCommitCreated() {
        return commitCreated;
    }

    public void setCommitCreated(boolean commitCreated) {
        this.commitCreated = commitCreated;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    // Convenience methods
    public boolean isFixed() {
        return fixAttempted && fixSuccessful;
    }

    public boolean isPartiallyFixed() {
        return fixAttempted && !fixSuccessful && (fixesApplied != null && !fixesApplied.isEmpty());
    }

    /**
     * Parse from JSON
     */
    public static E2EFixResult fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, E2EFixResult.class);
        } catch (Exception e) {
            // If JSON parsing fails, analyze the content
            if (json.contains("✅") || json.contains("fix successful")) {
                return successful("unknown", 1, new ArrayList<>());
            } else if (json.contains("❌") || json.contains("fix failed")) {
                return failed("Fix failed based on output analysis");
            } else {
                return failed("Unable to parse fix result");
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
            return "{\"error\":\"Failed to serialize fix result\"}";
        }
    }

    // Nested class for individual fixes
    public static class FixApplied {
        @JsonProperty("issue_type")
        private String issueType;

        @JsonProperty("description")
        private String description;

        @JsonProperty("file")
        private String file;

        @JsonProperty("line")
        private int line;

        @JsonProperty("fix_action")
        private String fixAction;

        @JsonProperty("successful")
        private boolean successful;

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
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

        public String getFixAction() {
            return fixAction;
        }

        public void setFixAction(String fixAction) {
            this.fixAction = fixAction;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }
    }
}