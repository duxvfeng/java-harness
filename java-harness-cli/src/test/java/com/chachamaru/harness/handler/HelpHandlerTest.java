package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HelpHandlerTest {
    @Test
    void testHelpHandlerExecutes() {
        HelpHandler handler = new HelpHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testHelpWithCommand() {
        HelpHandler handler = new HelpHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"plan"}));
    }

    @Test
    void testHelpWithAll() {
        HelpHandler handler = new HelpHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--all"}));
    }
}
