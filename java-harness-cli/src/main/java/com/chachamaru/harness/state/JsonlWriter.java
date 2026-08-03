package com.chachamaru.harness.state;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSONL (JSON Lines) file writer and reader.
 * Each line is a valid JSON object, optimized for append-only logging.
 */
public class JsonlWriter implements Closeable {
    private final String filePath;
    private final BufferedWriter writer;
    private final boolean append;

    /**
     * Create a new JsonlWriter (overwrite mode).
     *
     * @param filePath Path to the JSONL file
     * @throws IOException if file cannot be opened
     */
    public JsonlWriter(String filePath) throws IOException {
        this(filePath, false);
    }

    /**
     * Create a new JsonlWriter with specified mode.
     *
     * @param filePath Path to the JSONL file
     * @param append If true, append to existing file; if false, overwrite
     * @throws IOException if file cannot be opened
     */
    public JsonlWriter(String filePath, boolean append) throws IOException {
        this.filePath = filePath;
        this.append = append;

        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        // Create parent directories if they don't exist
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Open file in append or overwrite mode
        FileWriter fileWriter = new FileWriter(filePath, append);
        this.writer = new BufferedWriter(fileWriter);
    }

    /**
     * Write a JSON line to the file.
     *
     * @param json JSON string to write
     * @throws IOException if write fails
     */
    public void write(String json) throws IOException {
        writer.write(json);
        writer.newLine();
    }

    /**
     * Flush buffered data to disk.
     *
     * @throws IOException if flush fails
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Close the writer and flush remaining data.
     *
     * @throws IOException if close fails
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Read all lines from a JSONL file.
     *
     * @param filePath Path to the JSONL file
     * @return List of JSON strings
     * @throws IOException if read fails
     */
    public static List<String> readAll(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }

        return Files.readAllLines(path);
    }

    /**
     * Read last N lines from a JSONL file.
     *
     * @param filePath Path to the JSONL file
     * @param n Number of lines to read from the end
     * @return List of last N JSON strings
     * @throws IOException if read fails
     */
    public static List<String> readLastN(String filePath, int n) throws IOException {
        List<String> allLines = readAll(filePath);

        if (allLines.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = Math.max(0, allLines.size() - n);
        return new ArrayList<>(allLines.subList(fromIndex, allLines.size()));
    }

    /**
     * Get the file path.
     *
     * @return File path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Check if writer is in append mode.
     *
     * @return true if in append mode
     */
    public boolean isAppend() {
        return append;
    }

    /**
     * Create a JsonlWriter with auto-close after write.
     * Convenience method for single writes.
     *
     * @param filePath Path to the JSONL file
     * @param json JSON string to write
     * @throws IOException if write fails
     */
    public static void writeLine(String filePath, String json) throws IOException {
        try (JsonlWriter writer = new JsonlWriter(filePath, true)) {
            writer.write(json);
        }
    }

    /**
     * Append multiple lines efficiently.
     *
     * @param filePath Path to the JSONL file
     * @param jsonLines List of JSON strings to write
     * @throws IOException if write fails
     */
    public static void writeLines(String filePath, List<String> jsonLines) throws IOException {
        try (JsonlWriter writer = new JsonlWriter(filePath, true)) {
            for (String line : jsonLines) {
                writer.write(line);
            }
        }
    }

    /**
     * Get file size in bytes.
     *
     * @param filePath Path to the JSONL file
     * @return File size in bytes, or 0 if file doesn't exist
     */
    public static long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (IOException e) {
            // Ignore error and return 0
        }
        return 0;
    }

    /**
     * Count lines in file.
     *
     * @param filePath Path to the JSONL file
     * @return Number of lines
     * @throws IOException if read fails
     */
    public static long countLines(String filePath) throws IOException {
        return readAll(filePath).size();
    }

    /**
     * Rotate file if it exceeds max size.
     * Renames current file to .old and creates new file.
     *
     * @param filePath Path to the JSONL file
     * @param maxSizeBytes Maximum size in bytes before rotation
     * @throws IOException if rotation fails
     */
    public static void rotateIfExceeds(String filePath, long maxSizeBytes) throws IOException {
        long size = getFileSize(filePath);
        if (size > maxSizeBytes) {
            Path path = Paths.get(filePath);
            Path oldPath = Paths.get(filePath + ".old");

            // Delete old file if it exists
            if (Files.exists(oldPath)) {
                Files.delete(oldPath);
            }

            // Rotate current file
            if (Files.exists(path)) {
                Files.move(path, oldPath);
            }
        }
    }
}
