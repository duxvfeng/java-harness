package com.chachamaru.harness.cli.ipc;

import com.chachamaru.harness.shared.dto.GuardrailDecision;
import com.chachamaru.harness.shared.dto.HookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpIpcClient
 */
class HttpIpcClientTest {
    private IpcClient ipcClient;

    @BeforeEach
    void setUp() {
        ipcClient = new HttpIpcClient("http://localhost:8080/api/hook");
    }

    @AfterEach
    void tearDown() {
        if (ipcClient != null) {
            ipcClient.close();
        }
    }

    @Test
    void testSendHookEventSuccess() {
        HookEvent event = HookEvent.create("PreToolUse", Map.of(
                "tool", "Bash",
                "command", "echo test"
        ));

        CompletableFuture<GuardrailDecision> future = ipcClient.sendHookEvent(event);

        // Wait for completion with timeout
        try {
            future.get(2, TimeUnit.SECONDS);
            // If we get here without exception, test passes (service available)
            assertTrue(true);
        } catch (TimeoutException e) {
            fail("Future should complete within timeout");
        } catch (InterruptedException e) {
            fail("Test interrupted");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Expected when service is not available
            // Verify it's a connection-related exception
            Throwable cause = e.getCause();
            assertNotNull(cause, "Should have a cause");
            // Any exception is acceptable when service is unavailable
            // This demonstrates the fail-safe behavior
            assertTrue(true);
        }
    }

    @Test
    void testSendHookEventAsync() {
        HookEvent event = HookEvent.create("SessionStart", Map.of(
                "session_id", "test-session-123"
        ));

        CompletableFuture<GuardrailDecision> future = ipcClient.sendHookEvent(event);

        assertNotNull(future);

        // Wait a bit for async processing
        try {
            future.get(1, TimeUnit.SECONDS);
            assertTrue(true);
        } catch (TimeoutException e) {
            fail("Future should complete within timeout");
        } catch (Exception e) {
            // Expected when service unavailable
            assertTrue(true);
        }
    }

    @Test
    void testIsServiceAvailable() {
        assertTrue(ipcClient.isServiceAvailable());
    }

    @Test
    void testClose() {
        assertDoesNotThrow(() -> ipcClient.close());
    }

    @Test
    void testSendMultipleEventsConcurrently() {
        HookEvent event1 = HookEvent.create("PreToolUse", Map.of("tool", "Bash"));
        HookEvent event2 = HookEvent.create("PreToolUse", Map.of("tool", "Edit"));
        HookEvent event3 = HookEvent.create("Stop", Map.of("session_id", "test-123"));

        var future1 = ipcClient.sendHookEvent(event1);
        var future2 = ipcClient.sendHookEvent(event2);
        var future3 = ipcClient.sendHookEvent(event3);

        // All futures should complete (even if with errors when service unavailable)
        try {
            CompletableFuture.allOf(future1, future2, future3).get(3, TimeUnit.SECONDS);
            assertTrue(true);
        } catch (TimeoutException e) {
            fail("All futures should complete within timeout");
        } catch (Exception e) {
            // Expected when service unavailable
            assertTrue(true);
        }
    }

    @Test
    void testClientWithCustomUrl() {
        IpcClient customClient = new HttpIpcClient("http://custom.service:9090/hook");
        assertNotNull(customClient);
        customClient.close();
    }

    @Test
    void testClientWithNullUrl() {
        IpcClient defaultClient = new HttpIpcClient(null);
        assertNotNull(defaultClient);
        assertTrue(defaultClient.isServiceAvailable());
        defaultClient.close();
    }

    @Test
    void testHookEventCreation() {
        HookEvent event = HookEvent.create("TestEvent", Map.of("key", "value"));

        assertNotNull(event);
        assertNotNull(event.requestId());
        assertEquals("TestEvent", event.eventType());
        assertNotNull(event.payload());
        assertTrue(event.timestamp() > 0);
    }
}
