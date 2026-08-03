package com.chachamaru.harness.state;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * State persistence manager.
 * Handles saving and loading session and work states to JSONL files.
 */
public class StatePersistence {
    private final String projectRoot;
    private final String stateDirectory;
    private final Map<String, String> stateFiles;

    private static final String DEFAULT_STATE_DIR = ".claude/state";
    private static final int MAX_BACKUP_FILES = 5;

    /**
     * Create state persistence manager for project root.
     *
     * @param projectRoot Project root directory
     */
    public StatePersistence(String projectRoot) {
        this.projectRoot = projectRoot;
        this.stateDirectory = Paths.get(projectRoot, DEFAULT_STATE_DIR).toString();
        this.stateFiles = new HashMap<>();

        // Initialize state file paths
        stateFiles.put("session", Paths.get(stateDirectory, "session.jsonl").toString());
        stateFiles.put("work", Paths.get(stateDirectory, "work.jsonl").toString());
    }

    /**
     * Get state file paths map.
     *
     * @return Map of state type to file path
     */
    public Map<String, String> getStateFiles() {
        return new HashMap<>(stateFiles);
    }

    /**
     * Initialize state directory structure.
     *
     * @throws IOException if directory creation fails
     */
    public void initializeStateDirectory() throws IOException {
        Path statePath = Paths.get(stateDirectory);
        if (!Files.exists(statePath)) {
            Files.createDirectories(statePath);
        }
    }

    /**
     * Save session state to JSONL file.
     *
     * @param state Session state to save
     * @throws IOException if write fails
     */
    public void saveSessionState(SessionState state) throws IOException {
        ensureStateDirectory();
        String filePath = stateFiles.get("session");
        JsonlWriter.writeLine(filePath, state.toJson());
    }

    /**
     * Load latest session state from JSONL file.
     *
     * @return Latest session state, or null if no state exists
     * @throws IOException if read fails
     */
    public SessionState loadLatestSessionState() throws IOException {
        String filePath = stateFiles.get("session");
        List<String> lines = JsonlWriter.readLastN(filePath, 1);

        if (lines.isEmpty()) {
            return null;
        }

        return SessionState.fromJson(lines.get(0));
    }

    /**
     * Save work state to JSONL file.
     *
     * @param state Work state to save
     * @throws IOException if write fails
     */
    public void saveWorkState(WorkState state) throws IOException {
        ensureStateDirectory();
        String filePath = stateFiles.get("work");
        JsonlWriter.writeLine(filePath, state.toJson());
    }

    /**
     * Load work states from JSONL file.
     *
     * @param limit Maximum number of states to load (most recent first)
     * @return List of work states
     * @throws IOException if read fails
     */
    public List<WorkState> loadWorkStates(int limit) throws IOException {
        String filePath = stateFiles.get("work");
        List<String> lines = JsonlWriter.readLastN(filePath, limit);

        List<WorkState> states = new ArrayList<>();
        for (String line : lines) {
            states.add(WorkState.fromJson(line));
        }

        // Reverse to get most recent first
        Collections.reverse(states);
        return states;
    }

    /**
     * Load specific work state by ID.
     *
     * @param workId Work ID to find
     * @return Work state if found, null otherwise
     * @throws IOException if read fails
     */
    public WorkState loadWorkStateById(String workId) throws IOException {
        String filePath = stateFiles.get("work");
        List<String> lines = JsonlWriter.readAll(filePath);

        for (String line : lines) {
            WorkState state = WorkState.fromJson(line);
            if (workId.equals(state.getWorkId())) {
                return state;
            }
        }

        return null;
    }

    /**
     * Update work state in place.
     * Reads all states, updates the matching one, and rewrites the file.
     *
     * @param updatedState Updated work state
     * @throws IOException if write fails
     */
    public void updateWorkState(WorkState updatedState) throws IOException {
        String filePath = stateFiles.get("work");
        List<String> lines = JsonlWriter.readAll(filePath);

        List<String> updatedLines = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            WorkState state = WorkState.fromJson(line);
            if (updatedState.getWorkId().equals(state.getWorkId())) {
                updatedLines.add(updatedState.toJson());
                found = true;
            } else {
                updatedLines.add(line);
            }
        }

        if (found) {
            // Rewrite file with updated state
            try (JsonlWriter writer = new JsonlWriter(filePath, false)) {
                for (String line : updatedLines) {
                    writer.write(line);
                }
            }
        }
    }

    /**
     * Cleanup old states, keeping only the most recent N.
     *
     * @param stateType Type of state ("session" or "work")
     * @param keep Number of recent states to keep
     * @throws IOException if cleanup fails
     */
    public void cleanupOldStates(String stateType, int keep) throws IOException {
        String filePath = stateFiles.get(stateType);
        List<String> lines = JsonlWriter.readAll(filePath);

        if (lines.size() <= keep) {
            return; // Nothing to cleanup
        }

        // Keep only the last N lines
        List<String> keepLines = lines.subList(lines.size() - keep, lines.size());

        // Rewrite file
        try (JsonlWriter writer = new JsonlWriter(filePath, false)) {
            for (String line : keepLines) {
                writer.write(line);
            }
        }
    }

    /**
     * Get state file size in bytes.
     *
     * @param stateType Type of state
     * @return File size in bytes
     */
    public long getStateFileSize(String stateType) {
        String filePath = stateFiles.get(stateType);
        return JsonlWriter.getFileSize(filePath);
    }

    /**
     * Get count of states in file.
     *
     * @param stateType Type of state
     * @return Number of state entries
     * @throws IOException if read fails
     */
    public long getStateCount(String stateType) throws IOException {
        String filePath = stateFiles.get(stateType);
        return JsonlWriter.countLines(filePath);
    }

    /**
     * Rotate state file if it exceeds maximum size.
     *
     * @param stateType Type of state
     * @param maxSizeBytes Maximum size in bytes
     * @throws IOException if rotation fails
     */
    public void rotateIfExceeds(String stateType, long maxSizeBytes) throws IOException {
        String filePath = stateFiles.get(stateType);
        JsonlWriter.rotateIfExceeds(filePath, maxSizeBytes);
    }

    /**
     * Export state to file.
     *
     * @param stateType Type of state to export
     * @param exportPath Path to export to
     * @throws IOException if export fails
     */
    public void exportState(String stateType, String exportPath) throws IOException {
        String sourcePath = stateFiles.get(stateType);
        List<String> lines = JsonlWriter.readAll(sourcePath);

        try (JsonlWriter writer = new JsonlWriter(exportPath, false)) {
            for (String line : lines) {
                writer.write(line);
            }
        }
    }

    /**
     * Import state from file.
     *
     * @param stateType Type of state to import
     * @param importPath Path to import from
     * @throws IOException if import fails
     */
    public void importState(String stateType, String importPath) throws IOException {
        ensureStateDirectory();
        String targetPath = stateFiles.get(stateType);
        List<String> lines = JsonlWriter.readAll(importPath);

        try (JsonlWriter writer = new JsonlWriter(targetPath, true)) {
            for (String line : lines) {
                writer.write(line);
            }
        }
    }

    /**
     * Clear all states for a type.
     *
     * @param stateType Type of state to clear
     * @throws IOException if deletion fails
     */
    public void clearStates(String stateType) throws IOException {
        String filePath = stateFiles.get(stateType);
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Ensure state directory exists.
     *
     * @throws IOException if directory creation fails
     */
    private void ensureStateDirectory() throws IOException {
        Path statePath = Paths.get(stateDirectory);
        if (!Files.exists(statePath)) {
            Files.createDirectories(statePath);
        }
    }

    /**
     * Get project root.
     *
     * @return Project root directory
     */
    public String getProjectRoot() {
        return projectRoot;
    }

    /**
     * Get state directory.
     *
     * @return State directory path
     */
    public String getStateDirectory() {
        return stateDirectory;
    }
}
