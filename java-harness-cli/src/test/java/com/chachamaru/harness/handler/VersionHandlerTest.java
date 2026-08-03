package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VersionHandlerTest {
    @Test
    void testVersionHandlerExecutes() {
        VersionHandler handler = new VersionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testVersionWithDetailed() {
        VersionHandler handler = new VersionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--detailed"}));
    }

    @Test
    void testVersionWithJson() {
        VersionHandler handler = new VersionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--json"}));
    }
}
