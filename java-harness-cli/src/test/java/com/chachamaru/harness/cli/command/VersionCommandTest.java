package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionCommand.
 */
class VersionCommandTest {

    @Test
    void testExecution() {
        VersionCommand command = new VersionCommand();
        assertDoesNotThrow(command::run);
    }
}
