package com.chachamaru.harness.cli.command.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HookCommand.
 */
class HookCommandTest {

    @Test
    void testExecution() {
        HookCommand command = new HookCommand();
        assertDoesNotThrow(command::run);
    }
}
