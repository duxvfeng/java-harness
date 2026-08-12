package com.chachamaru.harness.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeFloorTest {

    @Test
    void blocksBillingBeforeOtherChecks() {
        RuntimeFloor.Decision decision = RuntimeFloor.check("stripe charge --amount 10", new RuntimeFloor.Context(Path.of("C:/repo")));

        assertTrue(decision.stopped());
        assertEquals(RuntimeFloor.Category.MONEY_BILLING, decision.category());
    }

    @Test
    void blocksUnallowlistedExternalEgress() {
        RuntimeFloor.Decision decision = RuntimeFloor.check("curl https://example.com/api", new RuntimeFloor.Context(Path.of("C:/repo")));

        assertTrue(decision.stopped());
        assertEquals(RuntimeFloor.Category.EGRESS, decision.category());
    }

    @Test
    void allowsLocalhostEgress() {
        RuntimeFloor.Decision decision = RuntimeFloor.check("curl http://localhost:8080/health", new RuntimeFloor.Context(Path.of("C:/repo")));

        assertFalse(decision.stopped());
    }

    @Test
    void blocksSecretReadButIgnoresCommentText() {
        assertEquals(RuntimeFloor.Category.SECRET_READ,
            RuntimeFloor.check("cat .env", new RuntimeFloor.Context(Path.of("C:/repo"))).category());
        assertFalse(RuntimeFloor.check("echo ok # cat .env", new RuntimeFloor.Context(Path.of("C:/repo"))).stopped());
    }

    @Test
    void blocksPathOutsideWorktree() {
        RuntimeFloor.Decision decision = RuntimeFloor.check(
            "cat C:/outside/secrets.txt",
            new RuntimeFloor.Context(Path.of("C:/repo")));

        assertTrue(decision.stopped());
        assertEquals(RuntimeFloor.Category.WORKTREE_ESCAPE, decision.category());
    }
}
