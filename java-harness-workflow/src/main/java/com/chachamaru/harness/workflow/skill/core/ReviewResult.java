package com.chachamaru.harness.workflow.skill.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 审查结果
 */
public class ReviewResult {
    private final boolean approved;
    private final String summary;
    private final List<String> findings;
    private final List<String> suggestions;
    private final Instant reviewTime;

    private ReviewResult(Builder builder) {
        this.approved = builder.approved;
        this.summary = builder.summary;
        this.findings = builder.findings;
        this.suggestions = builder.suggestions;
        this.reviewTime = builder.reviewTime;
    }

    public boolean isApproved() { return approved; }
    public String getSummary() { return summary; }
    public List<String> getFindings() { return findings; }
    public List<String> getSuggestions() { return suggestions; }
    public Instant getReviewTime() { return reviewTime; }
    public int getFindingsCount() { return findings.size(); }
    public int getSuggestionsCount() { return suggestions.size(); }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean approved = false;
        private String summary;
        private List<String> findings = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private Instant reviewTime = Instant.now();

        public Builder approved(boolean approved) {
            this.approved = approved;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder addFinding(String finding) {
            this.findings.add(finding);
            return this;
        }

        public Builder addSuggestion(String suggestion) {
            this.suggestions.add(suggestion);
            return this;
        }

        public Builder findings(List<String> findings) {
            this.findings = new ArrayList<>(findings);
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions = new ArrayList<>(suggestions);
            return this;
        }

        public ReviewResult build() {
            return new ReviewResult(this);
        }
    }
}
