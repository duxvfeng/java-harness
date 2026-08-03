package com.chachamaru.harness.cli.command.evidence;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvidenceCommand.
 */
class EvidenceCommandTest {

    @Test
    void testExecution() {
        EvidenceCommand command = new EvidenceCommand();
        assertDoesNotThrow(command::run);
    }
}
