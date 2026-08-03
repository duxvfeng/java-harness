package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlanHandlerTest {
    @Test
    void testPlanHandlerExecutes(@TempDir Path tempDir) {
        PlanHandler handler = new PlanHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testPlanHandlerReadsSpecMd(@TempDir Path tempDir) throws Exception {
        // Create spec.md
        Path specPath = tempDir.resolve("spec.md");
        Files.writeString(specPath, "# Test Spec\n\nThis is a test specification.");

        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                PlanHandler handler = new PlanHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("# Test Spec"));
            assertTrue(output.contains("This is a test specification."));
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
    void testPlanHandlerReadsPlansMd(@TempDir Path tempDir) throws Exception {
        // Create Plans.md
        Path plansPath = tempDir.resolve("Plans.md");
        Files.writeString(plansPath, "# Existing Plans\n\nSome plan content.");

        // Set work directory system property
        String originalWorkDir = System.getProperty("java.harness.work.dir");
        System.setProperty("java.harness.work.dir", tempDir.toString());

        try {
            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                PlanHandler handler = new PlanHandler();
                handler.execute(new String[]{});
            } finally {
                System.setOut(originalOut);
            }

            String output = outContent.toString();
            assertTrue(output.contains("# Existing Plans"));
            assertTrue(output.contains("Some plan content."));
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
    void testPlanHandlerGeneratesPlanPrompt(@TempDir Path tempDir) throws Exception {
        // Capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            PlanHandler handler = new PlanHandler();
            handler.execute(new String[]{});
        } finally {
            System.setOut(originalOut);
        }

        String output = outContent.toString();
        assertTrue(output.contains("# Plan Generation"));
        assertTrue(output.contains("Please generate or update the plan"));
    }
}
