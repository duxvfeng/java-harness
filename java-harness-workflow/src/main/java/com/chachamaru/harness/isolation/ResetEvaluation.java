package com.chachamaru.harness.isolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of evaluating reset conditions for branch isolation state.
 */
public class ResetEvaluation {

    private final boolean shouldReset;
    private final List<String> satisfiedConditions;
    private final String explanation;

    public ResetEvaluation(boolean shouldReset, List<String> satisfiedConditions, String explanation) {
        this.shouldReset = shouldReset;
        this.satisfiedConditions = new ArrayList<>(satisfiedConditions);
        this.explanation = explanation;
    }

    public boolean shouldReset() {
        return shouldReset;
    }

    public List<String> getSatisfiedConditions() {
        return new ArrayList<>(satisfiedConditions);
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean hasCondition(String condition) {
        return satisfiedConditions.contains(condition);
    }

    public int getConditionCount() {
        return satisfiedConditions.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResetEvaluation that = (ResetEvaluation) o;
        return shouldReset == that.shouldReset &&
               Objects.equals(satisfiedConditions, that.satisfiedConditions) &&
               Objects.equals(explanation, that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shouldReset, satisfiedConditions, explanation);
    }

    @Override
    public String toString() {
        return "ResetEvaluation{" +
               "shouldReset=" + shouldReset +
               ", satisfiedConditions=" + satisfiedConditions +
               ", explanation='" + explanation + '\'' +
               '}';
    }
}