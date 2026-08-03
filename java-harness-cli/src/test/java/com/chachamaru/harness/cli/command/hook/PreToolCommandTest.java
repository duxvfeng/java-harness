package com.chachamaru.harness.cli.command.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PreToolCommand.
 */
class PreToolCommandTest {

    @Test
    void testExecution() {
        PreToolCommand command = new PreToolCommand();
        assertDoesNotThrow(command::run);
    }
}
