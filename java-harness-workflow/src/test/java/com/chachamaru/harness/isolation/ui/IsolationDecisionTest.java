package com.chachamaru.harness.isolation.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsolationDecisionTest {

    @Test
    void decisionTypesExposeExpectedIsolationSemantics() {
        IsolationDecision isolate = new IsolationDecision(
            IsolationDecisionType.ISOLATE, "isolate", "test");
        IsolationDecision continueDecision = new IsolationDecision(
            IsolationDecisionType.CONTINUE, "continue", "test");
        IsolationDecision reset = new IsolationDecision(
            IsolationDecisionType.RESET, "reset", "test");
        IsolationDecision skip = new IsolationDecision(
            IsolationDecisionType.SKIP, "skip", "test");
        IsolationDecision cancel = new IsolationDecision(
            IsolationDecisionType.CANCEL, "cancel", "test");

        assertTrue(isolate.shouldProceed());
        assertTrue(isolate.shouldIsolate());
        assertTrue(continueDecision.shouldProceed());
        assertTrue(continueDecision.shouldIsolate());
        assertTrue(reset.shouldProceed());
        assertTrue(reset.shouldReset());
        assertTrue(skip.shouldProceed());
        assertFalse(skip.shouldIsolate());
        assertFalse(cancel.shouldProceed());
    }

    @Test
    void branchTypesContainMainAndFeatureBranches() {
        assertTrue(java.util.EnumSet.allOf(BranchType.class).contains(BranchType.MAIN));
        assertTrue(java.util.EnumSet.allOf(BranchType.class).contains(BranchType.FEATURE));
    }
}
