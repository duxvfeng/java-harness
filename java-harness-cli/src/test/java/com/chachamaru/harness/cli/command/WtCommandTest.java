package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WtCommandTest {

    @Test
    void capturesAndDiffsSnapshots(@TempDir Path tempDir) throws Exception {
        Path watched = tempDir.resolve("watched.json");
        Files.writeString(watched, "before");
        Path before = tempDir.resolve("before.json");
        Path after = tempDir.resolve("after.json");

        WtCommand.CaptureCommand capture = new WtCommand.CaptureCommand();
        capture.output = before.toString();
        capture.paths = watched.toString();
        assertEquals(0, capture.call());

        Files.writeString(watched, "after");
        capture.output = after.toString();
        assertEquals(0, capture.call());

        WtCommand.DiffCommand diff = new WtCommand.DiffCommand();
        diff.before = before.toString();
        diff.after = after.toString();
        assertEquals(2, diff.call());
    }
}
