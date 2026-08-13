package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Metadata about the state file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateMetadata {

    @JsonProperty("createdAt")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @JsonProperty("version")
    private String version = "2.0";

    @JsonProperty("migratedFrom")
    private String migratedFrom;

    // Constructors
    public StateMetadata() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = "2.0";
    }

    // Getters and Setters
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMigratedFrom() {
        return migratedFrom;
    }

    public void setMigratedFrom(String migratedFrom) {
        this.migratedFrom = migratedFrom;
    }

    // Helper methods
    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isMigrated() {
        return migratedFrom != null && !migratedFrom.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateMetadata that = (StateMetadata) o;
        return Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(updatedAt, that.updatedAt) &&
               Objects.equals(version, that.version) &&
               Objects.equals(migratedFrom, that.migratedFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, updatedAt, version, migratedFrom);
    }

    @Override
    public String toString() {
        return "StateMetadata{" +
               "version='" + version + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               ", migratedFrom='" + migratedFrom + '\'' +
               '}';
    }
}