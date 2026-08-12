package com.chachamaru.harness.workflow.scope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScopeLeashTest {

    @Test
    void infersAndChecksDeclaredPaths() {
        String plans = "| 101.7 | Update `src/main/App.java` and `docs/guide.md` | files: src/main/App.java, docs/guide.md |";

        List<String> scope = ScopeLeash.inferScopeFromPlan(plans, "101.7");

        assertEquals(List.of("docs/guide.md", "src/main/App.java"), scope);
        assertTrue(ScopeLeash.checkWrite(scope, "C:/repo/src/main/App.java", "C:/repo"));
        assertFalse(ScopeLeash.checkWrite(scope, "C:/repo/src/main/Other.java", "C:/repo"));
    }

    @Test
    void reportsUntouchedScope() {
        List<String> dropped = ScopeLeash.droppedScope(
            List.of("src/A.java", "docs/guide.md"),
            List.of("src/A.java"));

        assertEquals(List.of("docs/guide.md"), dropped);
    }
}
