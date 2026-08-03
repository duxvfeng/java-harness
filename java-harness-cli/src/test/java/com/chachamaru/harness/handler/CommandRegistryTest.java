package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommandRegistryTest {
    @Test
    void testCommandRegistry() {
        CommandHandler handler = CommandRegistry.getHandler("plan");
        assertNotNull(handler);
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testUnknownCommandReturnsNull() {
        CommandHandler handler = CommandRegistry.getHandler("unknown-command");
        assertNull(handler);
    }
}
