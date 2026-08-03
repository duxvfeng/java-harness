package com.chachamaru.harness.state;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class JsonlWriterTest {
    @Test
    void testWriteLine() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");
        JsonlWriter writer = new JsonlWriter(tempFile.toString());

        writer.write("{\"test\": \"value\"}");
        writer.close();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(1, lines.size());
        assertEquals("{\"test\": \"value\"}", lines.get(0));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testWriteMultipleLines() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");
        JsonlWriter writer = new JsonlWriter(tempFile.toString());

        writer.write("{\"line\": 1}");
        writer.write("{\"line\": 2}");
        writer.write("{\"line\": 3}");
        writer.close();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(3, lines.size());
        assertEquals("{\"line\": 1}", lines.get(0));
        assertEquals("{\"line\": 2}", lines.get(1));
        assertEquals("{\"line\": 3}", lines.get(2));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testAppendMode() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");

        // Write initial content
        JsonlWriter writer1 = new JsonlWriter(tempFile.toString());
        writer1.write("{\"first\": \"line\"}");
        writer1.close();

        // Append more content
        JsonlWriter writer2 = new JsonlWriter(tempFile.toString(), true);
        writer2.write("{\"second\": \"line\"}");
        writer2.close();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(2, lines.size());
        assertEquals("{\"first\": \"line\"}", lines.get(0));
        assertEquals("{\"second\": \"line\"}", lines.get(1));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testReadAll() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");
        JsonlWriter writer = new JsonlWriter(tempFile.toString());

        writer.write("{\"id\": 1}");
        writer.write("{\"id\": 2}");
        writer.close();

        List<String> lines = JsonlWriter.readAll(tempFile.toString());
        assertEquals(2, lines.size());
        assertEquals("{\"id\": 1}", lines.get(0));
        assertEquals("{\"id\": 2}", lines.get(1));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testReadLastN() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");
        JsonlWriter writer = new JsonlWriter(tempFile.toString());

        for (int i = 1; i <= 10; i++) {
            writer.write("{\"line\": " + i + "}");
        }
        writer.close();

        List<String> last3 = JsonlWriter.readLastN(tempFile.toString(), 3);
        assertEquals(3, last3.size());
        assertEquals("{\"line\": 8}", last3.get(0));
        assertEquals("{\"line\": 9}", last3.get(1));
        assertEquals("{\"line\": 10}", last3.get(2));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testWriteWithFlush() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");
        JsonlWriter writer = new JsonlWriter(tempFile.toString());

        writer.write("{\"test\": \"value\"}");
        writer.flush();

        // File should contain data even before close
        String content = Files.readString(tempFile);
        assertTrue(content.contains("{\"test\": \"value\"}"));

        writer.close();
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testEmptyFile() throws IOException {
        Path tempFile = Files.createTempFile("test", ".jsonl");

        List<String> lines = JsonlWriter.readAll(tempFile.toString());
        assertTrue(lines.isEmpty());

        Files.deleteIfExists(tempFile);
    }
}
