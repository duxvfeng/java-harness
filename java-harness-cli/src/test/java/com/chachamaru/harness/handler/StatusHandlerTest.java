package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatusHandlerTest {
    @Test
    void testStatusHandlerExecutes() {
        StatusHandler handler = new StatusHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testStatusWithVerbose() {
        StatusHandler handler = new StatusHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--verbose"}));
    }

    @Test
    void testStatusWithJson() {
        StatusHandler handler = new StatusHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--json"}));
    }
}
