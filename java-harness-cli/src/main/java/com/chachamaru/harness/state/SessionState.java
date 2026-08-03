package com.chachamaru.harness.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session state management.
 * Tracks session information including ID, start time, and custom attributes.
 */
public class SessionState {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private final String sessionId;
    private final Instant startTime;
    private Instant endTime;
    private boolean active = true;
    private final Map<String, Object> attributes = new HashMap<>();

    public SessionState() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = Instant.now();
    }

    /**
     * Get session ID.
     *
     * @return Unique session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get session start time.
     *
     * @return Start timestamp
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Get session end time.
     *
     * @return End timestamp, or null if session is still active
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Check if session is active.
     *
     * @return true if session is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Close the session.
     */
    public void close() {
        this.active = false;
        this.endTime = Instant.now();
    }

    /**
     * Get session duration in seconds.
     *
     * @return Duration in seconds, or current duration if still active
     */
    public long getDuration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return ChronoUnit.SECONDS.between(startTime, end);
    }

    /**
     * Set an attribute value.
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Get an attribute value.
     *
     * @param key Attribute key
     * @return Attribute value, or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Get all attributes.
     *
     * @return Map of all attributes
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * Convert session state to JSON.
     *
     * @return JSON string representation
     */
    public String toJson() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("session_id", sessionId);
            data.put("start_time", startTime.toString());
            data.put("end_time", endTime != null ? endTime.toString() : null);
            data.put("active", active);
            data.put("duration_seconds", getDuration());
            data.put("attributes", attributes);

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize session state", e);
        }
    }

    /**
     * Create SessionState from JSON.
     *
     * @param json JSON string
     * @return SessionState instance
     */
    public static SessionState fromJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(json, Map.class);

            // Create a new session but will override fields
            SessionState state = new SessionState();

            // Restore original session ID
            String originalSessionId = (String) data.get("session_id");
            if (originalSessionId != null) {
                // Use reflection to set the final field (temporary workaround)
                var field = SessionState.class.getDeclaredField("sessionId");
                field.setAccessible(true);
                field.set(state, originalSessionId);
            }

            // Restore original start time
            String startTimeStr = (String) data.get("start_time");
            if (startTimeStr != null) {
                Instant originalStartTime = Instant.parse(startTimeStr);
                var field = SessionState.class.getDeclaredField("startTime");
                field.setAccessible(true);
                field.set(state, originalStartTime);
            }

            // Parse end time if present
            String endTimeStr = (String) data.get("end_time");
            if (endTimeStr != null) {
                state.endTime = Instant.parse(endTimeStr);
                state.active = (Boolean) data.get("active");
            }

            // Restore attributes
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) data.get("attributes");
            if (attrs != null) {
                state.attributes.putAll(attrs);
            }

            return state;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize session state", e);
        }
    }

    @Override
    public String toString() {
        return "SessionState{" +
                "sessionId='" + sessionId + '\'' +
                ", startTime=" + startTime +
                ", active=" + active +
                ", duration=" + getDuration() + "s" +
                '}';
    }
}
