package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Objects;

/**
 * 会话保存结果
 *
 * <p>表示会话保存操作的结果，包含成功状态、保存ID、时间戳等信息。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class SessionSaveResult {

    @JsonProperty("saveId")
    private final String saveId;

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant timestamp;

    @JsonProperty("size")
    private final Long size;

    @JsonProperty("errorMessage")
    private final String errorMessage;

    private SessionSaveResult(Builder builder) {
        this.saveId = builder.saveId;
        this.success = builder.success;
        this.message = builder.message;
        this.timestamp = builder.timestamp;
        this.size = builder.size;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public String getSaveId() { return saveId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public Long getSize() { return size; }
    public String getErrorMessage() { return errorMessage; }

    /**
     * 创建成功结果
     */
    public static SessionSaveResult success(String saveId, String message, long size) {
        return new Builder()
                .saveId(saveId)
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .size(size)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static SessionSaveResult failed(String errorMessage) {
        return new Builder()
                .success(false)
                .message("Save failed")
                .timestamp(Instant.now())
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static SessionSaveResult skipped(String reason) {
        return new Builder()
                .success(false)
                .message("Save skipped")
                .timestamp(Instant.now())
                .errorMessage(reason)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionSaveResult that = (SessionSaveResult) o;
        return success == that.success &&
               Objects.equals(saveId, that.saveId) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveId, success, timestamp);
    }

    @Override
    public String toString() {
        return "SessionSaveResult{" +
                "saveId='" + saveId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", size=" + size +
                '}';
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private String saveId;
        private boolean success;
        private String message;
        private Instant timestamp;
        private Long size;
        private String errorMessage;

        public Builder saveId(String saveId) {
            this.saveId = saveId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public SessionSaveResult build() {
            return new SessionSaveResult(this);
        }
    }
}