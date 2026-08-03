package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GenHandlerTest {
    @Test
    void testGenHandlerExecutes() {
        GenHandler handler = new GenHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testGenWithPrompt() {
        GenHandler handler = new GenHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--prompt", "test prompt"}));
    }

    @Test
    void testGenWithOutputFile() {
        GenHandler handler = new GenHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--output", "test.md"}));
    }
}
