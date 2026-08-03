package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReleaseCommand.
 */
class ReleaseCommandTest {

    @Test
    void testExecution() {
        ReleaseCommand command = new ReleaseCommand();
        assertDoesNotThrow(command::run);
    }
}
