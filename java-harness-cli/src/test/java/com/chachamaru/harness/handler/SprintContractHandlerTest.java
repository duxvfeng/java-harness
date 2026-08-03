package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SprintContractHandlerTest {
    @Test
    void testSprintContractHandlerExecutes() {
        SprintContractHandler handler = new SprintContractHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testSprintContractGenerate() {
        SprintContractHandler handler = new SprintContractHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"generate", "--task", "1.1"}));
    }

    @Test
    void testSprintContractValidate() {
        SprintContractHandler handler = new SprintContractHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"validate", "--contract", "test.json"}));
    }
}
