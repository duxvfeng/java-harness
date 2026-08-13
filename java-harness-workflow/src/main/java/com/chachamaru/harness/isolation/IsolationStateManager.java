package com.chachamaru.harness.isolation;

import com.chachamaru.harness.isolation.model.*;
import com.fasterxml.databind.ObjectMapper;
import com.fasterxml.databind.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manager for isolation state file operations.
 * Handles loading, saving, and migrating state files with atomic operations and error handling.
 */
public class IsolationStateManager {

    private static final Logger logger = LoggerFactory.getLogger(IsolationStateManager.class);
    private static final String STATE_DIR = ".claude/state";
    private static final String STATE_FILE_PATH = STATE_DIR + "/branch-isolation-decision.json";
    private static final String SCHEMA_VERSION = "2.0";
    private static final String SCHEMA_TYPE = "branch-isolation-state-v2";

    private final ObjectMapper objectMapper;
    private final File stateFile;
    private final File stateDir;

    public IsolationStateManager() {
        this.objectMapper = createObjectMapper();
        this.stateDir = new File(STATE_DIR);
        this.stateFile = new File(STATE_FILE_PATH);
    }

    /**
     * Create ObjectMapper with proper configuration for state files
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDefaultUseInclusion(true);
        return mapper;
    }

    /**
     * Load isolation state from file
     */
    public IsolationStateFile loadState() throws StateException {
        try {
            ensureStateDirectoryExists();

            if (!stateFile.exists()) {
                logger.info("State file does not exist, creating new one");
                return createNewStateFile();
            }

            String jsonContent = Files.readString(stateFile.toPath());
            logger.debug("Loading state from file: {}", STATE_FILE_PATH);

            IsolationStateFile state = parseStateFile(jsonContent);

            // Validate and migrate if needed
            if (state.getMetadata() == null ||
                !"2.0".equals(state.getMetadata().getVersion())) {
                logger.info("Migrating state file from older version");
                state = migrateToV2(state);
            }

            logger.debug("Successfully loaded state: {}", state.getCurrentSeries());
            return state;

        } catch (IOException e) {
            throw new StateException("Failed to load isolation state from file: " + STATE_FILE_PATH, e);
        }
    }

    /**
     * Save isolation state to file with atomic operation
     */
    public void saveState(IsolationStateFile state) throws StateException {
        try {
            ensureStateDirectoryExists();

            // Update metadata
            if (state.getMetadata() == null) {
                state.setMetadata(new StateMetadata());
            }
            state.getMetadata().markAsUpdated();
            state.setVersion(SCHEMA_VERSION);
            state.setSchemaType(SCHEMA_TYPE);

            // Validate state before saving
            validateState(state);

            // Write to temporary file first
            File tempFile = new File(STATE_FILE_PATH + ".tmp");
            String jsonContent = serializeState(state);

            Files.writeString(tempFile.toPath(), jsonContent);
            logger.debug("State written to temporary file: {}", tempFile.getPath());

            // Atomic replace
            Files.move(
                tempFile.toPath(),
                stateFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );

            logger.info("State saved successfully to file: {}", STATE_FILE_PATH);

        } catch (IOException e) {
            throw new StateException("Failed to save isolation state to file: " + STATE_FILE_PATH, e);
        }
    }

    /**
     * Create new empty state file
     */
    public IsolationStateFile createNewStateFile() {
        logger.info("Creating new state file");

        IsolationStateFile state = new IsolationStateFile();
        state.setVersion(SCHEMA_VERSION);
        state.setSchemaType(SCHEMA_TYPE);

        // Initialize with default values
        ResetTriggers triggers = new ResetTriggers();
        state.setResetTriggers(triggers);

        StateMetadata metadata = new StateMetadata();
        state.setMetadata(metadata);

        logger.debug("New state file created: {}", state);
        return state;
    }

    /**
     * Reset state to initial condition
     */
    public void resetState() throws StateException {
        logger.info("Resetting isolation state to initial condition");

        IsolationStateFile newState = createNewStateFile();
        saveState(newState);

        logger.info("State reset completed successfully");
    }

    /**
     * Parse state file JSON content
     */
    private IsolationStateFile parseStateFile(String jsonContent) throws StateException {
        try {
            return objectMapper.readValue(jsonContent, IsolationStateFile.class);
        } catch (Exception e) {
            throw new StateException("Failed to parse state file JSON", e);
        }
    }

    /**
     * Serialize state to JSON
     */
    private String serializeState(IsolationStateFile state) throws StateException {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                              .writeValueAsString(state);
        } catch (Exception e) {
            throw new StateException("Failed to serialize state to JSON", e);
        }
    }

    /**
     * Validate state before saving
     */
    private void validateState(IsolationStateFile state) throws StateException {
        if (state == null) {
            throw new StateException("State cannot be null");
        }

        // Validate version
        if (!SCHEMA_VERSION.equals(state.getVersion())) {
            throw new StateException("Invalid state version: " + state.getVersion());
        }

        // Validate current series if present
        if (state.getCurrentSeries() != null) {
            SeriesInfo series = state.getCurrentSeries();
            if (series.getSeriesId() == null || series.getSeriesId().isEmpty()) {
                throw new StateException("Series ID cannot be null or empty");
            }
        }

        logger.debug("State validation passed");
    }

    /**
     * Migrate older state format to v2
     */
    private IsolationStateFile migrateToV2(IsolationStateFile oldState) throws StateException {
        logger.info("Migrating state file from version {} to {}",
                   oldState.getVersion(), SCHEMA_VERSION);

        try {
            IsolationStateFile newState = createNewStateFile();

            // Preserve decision history if available
            if (oldState.getDecisionHistory() != null) {
                newState.setDecisionHistory(oldState.getDecisionHistory());
                logger.debug("Migrated {} decision records",
                           oldState.getDecisionHistory().size());
            }

            // Try to preserve current series info if compatible
            if (oldState.getCurrentSeries() != null) {
                // Convert old series info to new format
                SeriesInfo oldSeries = oldState.getCurrentSeries();
                SeriesInfo newSeries = convertSeriesInfo(oldSeries);
                newState.setCurrentSeries(newSeries);
            }

            // Set migration metadata
            newState.getMetadata().setMigratedFrom(oldState.getVersion());
            newState.getMetadata().setVersion(SCHEMA_VERSION);

            logger.info("State migration completed successfully");
            return newState;

        } catch (Exception e) {
            throw new StateException("Failed to migrate state file to v2", e);
        }
    }

    /**
     * Convert old SeriesInfo format to new format
     */
    private SeriesInfo convertSeriesInfo(com.chachamaru.harness.isolation.model.SeriesInfo oldSeries) {
        // For now, just use the old object as-is since the format is compatible
        // In future, might need field-by-field conversion
        return oldSeries;
    }

    /**
     * Ensure state directory exists
     */
    private void ensureStateDirectoryExists() throws StateException {
        try {
            if (!stateDir.exists()) {
                Files.createDirectories(stateDir.toPath());
                logger.debug("Created state directory: {}", STATE_DIR);
            }
        } catch (IOException e) {
            throw new StateException("Failed to create state directory: " + STATE_DIR, e);
        }
    }

    /**
     * Check if state file exists
     */
    public boolean stateFileExists() {
        return stateFile.exists();
    }

    /**
     * Delete state file (for testing or manual cleanup)
     */
    public void deleteStateFile() throws StateException {
        try {
            if (stateFile.exists()) {
                Files.delete(stateFile.toPath());
                logger.info("State file deleted: {}", STATE_FILE_PATH);
            }
        } catch (IOException e) {
            throw new StateException("Failed to delete state file: " + STATE_FILE_PATH, e);
        }
    }

    /**
     * Get state file path
     */
    public String getStateFilePath() {
        return STATE_FILE_PATH;
    }

    /**
     * Load state safely with fallback to new state
     */
    public IsolationStateFile loadStateSafely() {
        try {
            return loadState();
        } catch (StateException e) {
            logger.warn("Failed to load state, creating new one: {}", e.getMessage());
            return createNewStateFile();
        }
    }
}