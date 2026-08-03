package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompletionHandlerTest {
    @Test
    void testCompletionHandlerExecutes() {
        CompletionHandler handler = new CompletionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testCompletionForBash() {
        CompletionHandler handler = new CompletionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"bash"}));
    }

    @Test
    void testCompletionForZsh() {
        CompletionHandler handler = new CompletionHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"zsh"}));
    }
}
