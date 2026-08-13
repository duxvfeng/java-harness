package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enhanced state file model for branch isolation state management.
 * Version 2.0 - Supports task series tracking, code status detection, and intelligent reset.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IsolationStateFile {

    @JsonProperty("version")
    private String version = "2.0";

    @JsonProperty("schemaType")
    private String schemaType = "branch-isolation-state-v2";

    @JsonProperty("currentSeries")
    private SeriesInfo currentSeries;

    @JsonProperty("codeStatus")
    private CodeStatus codeStatus;

    @JsonProperty("resetTriggers")
    private ResetTriggers resetTriggers;

    @JsonProperty("decisionHistory")
    @JsonAlias("decisions")
    private List<DecisionRecord> decisionHistory = new ArrayList<>();

    @JsonProperty("metadata")
    private StateMetadata metadata;

    // Constructors
    public IsolationStateFile() {
        this.metadata = new StateMetadata();
    }

    // Getters and Setters
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(String schemaType) {
        this.schemaType = schemaType;
    }

    public SeriesInfo getCurrentSeries() {
        return currentSeries;
    }

    public void setCurrentSeries(SeriesInfo currentSeries) {
        this.currentSeries = currentSeries;
    }

    public CodeStatus getCodeStatus() {
        return codeStatus;
    }

    public void setCodeStatus(CodeStatus codeStatus) {
        this.codeStatus = codeStatus;
    }

    public ResetTriggers getResetTriggers() {
        return resetTriggers;
    }

    public void setResetTriggers(ResetTriggers resetTriggers) {
        this.resetTriggers = resetTriggers;
    }

    public List<DecisionRecord> getDecisionHistory() {
        return decisionHistory;
    }

    public void setDecisionHistory(List<DecisionRecord> decisionHistory) {
        this.decisionHistory = decisionHistory;
    }

    public StateMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(StateMetadata metadata) {
        this.metadata = metadata;
    }

    // Helper methods
    public boolean hasActiveSeries() {
        return currentSeries != null && currentSeries.isIsolationActive();
    }

    @JsonIgnore
    public boolean isReadyForReset() {
        return currentSeries != null &&
               currentSeries.isAutoResetPending() &&
               codeStatus != null &&
               codeStatus.isBranchClean();
    }

    public void addDecisionRecord(DecisionRecord record) {
        if (this.decisionHistory == null) {
            this.decisionHistory = new ArrayList<>();
        }
        this.decisionHistory.add(record);
        if (metadata != null) {
            metadata.setUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsolationStateFile that = (IsolationStateFile) o;
        return Objects.equals(version, that.version) &&
               Objects.equals(schemaType, that.schemaType) &&
               Objects.equals(currentSeries, that.currentSeries) &&
               Objects.equals(codeStatus, that.codeStatus) &&
               Objects.equals(resetTriggers, that.resetTriggers) &&
               Objects.equals(decisionHistory, that.decisionHistory) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, schemaType, currentSeries, codeStatus,
                           resetTriggers, decisionHistory, metadata);
    }

    @Override
    public String toString() {
        return "IsolationStateFile{" +
               "version='" + version + '\'' +
               ", schemaType='" + schemaType + '\'' +
               ", currentSeries=" + currentSeries +
               ", codeStatus=" + codeStatus +
               ", resetTriggers=" + resetTriggers +
               ", decisionHistory=" + decisionHistory +
               ", metadata=" + metadata +
               '}';
    }
}
