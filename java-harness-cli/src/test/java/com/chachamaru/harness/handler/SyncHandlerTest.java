package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncHandlerTest {
    @Test
    void testSyncHandlerExecutes() {
        SyncHandler handler = new SyncHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testSyncHandlerWithRootPath(@TempDir Path tempDir) throws Exception {
        SyncHandler handler = new SyncHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{tempDir.toString()}));
    }

    @Test
    void testSyncHandlerGeneratesPrompt(@TempDir Path tempDir) throws Exception {
        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                SyncHandler handler = new SyncHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("sync") || output.contains("configuration"));
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
    void testSyncHandlerReadsPlans(@TempDir Path tempDir) throws Exception {
        // Create Plans.md
        Path plansPath = tempDir.resolve("Plans.md");
        Files.writeString(plansPath, "# Plans\n\n## TASK-001: First Task\n- [x] Completed");

        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                SyncHandler handler = new SyncHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("sync") || output.contains("Plans"));
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
