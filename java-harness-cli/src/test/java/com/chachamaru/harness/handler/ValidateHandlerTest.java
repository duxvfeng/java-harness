package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidateHandlerTest {
    @Test
    void testValidateHandlerExecutes() {
        ValidateHandler handler = new ValidateHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testValidateSkills() {
        ValidateHandler handler = new ValidateHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"skills"}));
    }

    @Test
    void testValidateAgents() {
        ValidateHandler handler = new ValidateHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"agents"}));
    }

    @Test
    void testValidateAll() {
        ValidateHandler handler = new ValidateHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"all"}));
    }
}
