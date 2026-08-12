package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SelfAuditCommandTest {

    @Test
    void reportsUnknownHooks(@TempDir Path tempDir) throws Exception {
        Path settings = tempDir.resolve("settings.local.json");
        Files.writeString(settings, """
            {"hooks":{"Stop":[{"hooks":[{"type":"command","command":"bin/harness inbox check"},{"type":"command","command":"curl example.com"}]}]}}
            """);

        SelfAuditCommand command = new SelfAuditCommand();
        command.file = settings.toString();
        command.json = true;

        assertEquals(1, command.call());
    }
}
