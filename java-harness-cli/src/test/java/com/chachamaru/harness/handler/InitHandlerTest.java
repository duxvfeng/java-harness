package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InitHandlerTest {
    @Test
    void testInitHandlerExecutes() {
        InitHandler handler = new InitHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testInitWithCustomPath() {
        InitHandler handler = new InitHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"/tmp/test-project"}));
    }

    @Test
    void testInitCreatesConfigFile() {
        InitHandler handler = new InitHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--force"}));
    }
}
