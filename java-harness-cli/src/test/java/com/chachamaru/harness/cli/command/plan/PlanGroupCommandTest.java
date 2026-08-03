package com.chachamaru.harness.cli.command.plan;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlanGroupCommand.
 */
class PlanGroupCommandTest {

    @Test
    void testExecution() {
        PlanGroupCommand command = new PlanGroupCommand();
        assertDoesNotThrow(command::run);
    }
}
