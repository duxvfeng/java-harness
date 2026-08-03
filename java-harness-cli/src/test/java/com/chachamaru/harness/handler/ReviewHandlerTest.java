package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReviewHandlerTest {
    @Test
    void testReviewHandlerExecutes() {
        ReviewHandler handler = new ReviewHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testReviewHandlerWithTaskID(@TempDir Path tempDir) throws Exception {
        // Create Plans.md with completed tasks
        Path plansPath = tempDir.resolve("Plans.md");
        Files.writeString(plansPath, """
            # Plans

            ## TASK-001: First Task
            - [x] Subtask 1
            - [x] Subtask 2

            ## TASK-002: Second Task
            - [x] Subtask 1
            """);

        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                ReviewHandler handler = new ReviewHandler();
                handler.execute(new String[]{"TASK-001"});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("TASK-001") || output.contains("review"));
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
    void testReviewHandlerGeneratesPrompt(@TempDir Path tempDir) throws Exception {
        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                ReviewHandler handler = new ReviewHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("review") || output.contains("completed"));
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
