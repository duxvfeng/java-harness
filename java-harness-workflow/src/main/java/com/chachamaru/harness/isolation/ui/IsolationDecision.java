package com.chachamaru.harness.isolation.ui;

import java.util.Objects;

/**
 * User decision about branch isolation.
 */
public class IsolationDecision {

    private final IsolationDecisionType decisionType;
    private final String userChoice;
    private final String reason;
    private final String worktreePath;
    private final String seriesId;

    public IsolationDecision(IsolationDecisionType decisionType, String userChoice, String reason) {
        this(decisionType, userChoice, reason, null, null);
    }

    public IsolationDecision(IsolationDecisionType decisionType, String userChoice, String reason,
                           String worktreePath, String seriesId) {
        this.decisionType = decisionType;
        this.userChoice = userChoice;
        this.reason = reason;
        this.worktreePath = worktreePath;
        this.seriesId = seriesId;
    }

    public IsolationDecisionType getDecisionType() {
        return decisionType;
    }

    public String getUserChoice() {
        return userChoice;
    }

    public String getReason() {
        return reason;
    }

    public String getWorktreePath() {
        return worktreePath;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public boolean shouldProceed() {
        return decisionType != IsolationDecisionType.CANCEL;
    }

    public boolean shouldIsolate() {
        return decisionType == IsolationDecisionType.ISOLATE ||
               decisionType == IsolationDecisionType.CONTINUE;
    }

    public boolean shouldReset() {
        return decisionType == IsolationDecisionType.RESET;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsolationDecision that = (IsolationDecision) o;
        return decisionType == that.decisionType &&
               Objects.equals(userChoice, that.userChoice) &&
               Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decisionType, userChoice, reason);
    }

    @Override
    public String toString() {
        return "IsolationDecision{" +
               "decisionType=" + decisionType +
               ", userChoice='" + userChoice + '\'' +
               ", reason='" + reason + '\'' +
               '}';
    }
}