package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReleaseHandlerTest {
    @Test
    void testReleaseHandlerExecutes() {
        ReleaseHandler handler = new ReleaseHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testReleaseHandlerWithCheckFlag() {
        ReleaseHandler handler = new ReleaseHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--check"}));
    }

    @Test
    void testReleaseHandlerGeneratesPrompt(@TempDir Path tempDir) throws Exception {
        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                ReleaseHandler handler = new ReleaseHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("release") || output.contains("prepare"));
        } finally {
            // Restore original system property
            if (originalWorkDir == null) {
                System.clearProperty("java.harness.work.dir");
            } else {
                System.setProperty("java.harness.work.dir", originalWorkDir);
            }
        }
    }

    @Test
    void testReleaseHandlerCheckMode(@TempDir Path tempDir) throws Exception {
        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                ReleaseHandler handler = new ReleaseHandler();
                handler.execute(new String[]{"--check"});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("check") || output.contains("release"));
        } finally {
            // Restore original system property
            if (originalWorkDir == null) {
                System.clearProperty("java.harness.work.dir");
            } else {
                System.setProperty("java.harness.work.dir", originalWorkDir);
            }
        }
    }
}
