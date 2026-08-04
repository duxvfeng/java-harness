package com.chachamaru.harness.workflow.skill.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SyncSkill 执行结果
 */
public class SyncResult {
    private final boolean success;
    private final List<String> generatedFiles;
    private final List<String> driftWarnings;
    private final String message;

    private SyncResult(Builder builder) {
        this.success = builder.success;
        this.generatedFiles = new ArrayList<>(builder.generatedFiles);
        this.driftWarnings = new ArrayList<>(builder.driftWarnings);
        this.message = builder.message;
    }

    public boolean isSuccess() { return success; }
    public List<String> getGeneratedFiles() { return Collections.unmodifiableList(generatedFiles); }
    public List<String> getDriftWarnings() { return Collections.unmodifiableList(driftWarnings); }
    public String getMessage() { return message; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = true;
        private List<String> generatedFiles = new ArrayList<>();
        private List<String> driftWarnings = new ArrayList<>();
        private String message = "Sync completed";

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder addGeneratedFile(String file) {
            if (file != null) {
                this.generatedFiles.add(file);
            }
            return this;
        }

        public Builder addDriftWarning(String warning) {
            if (warning != null) {
                this.driftWarnings.add(warning);
            }
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public SyncResult build() {
            return new SyncResult(this);
        }
    }
}
