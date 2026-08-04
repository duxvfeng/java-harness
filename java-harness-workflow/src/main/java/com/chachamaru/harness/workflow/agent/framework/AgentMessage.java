package com.chachamaru.harness.workflow.agent.framework;

import java.time.Instant;
import java.util.UUID;

/**
 * Agent 消息对象
 * 阶段1：简单消息传递
 */
public class AgentMessage {
    private final String messageId;
    private final String fromAgentId;
    private final String toAgentId;
    private final MessageType type;
    private final Object payload;
    private final Instant timestamp;

    private AgentMessage(Builder builder) {
        this.messageId = builder.messageId;
        this.fromAgentId = builder.fromAgentId;
        this.toAgentId = builder.toAgentId;
        this.type = builder.type;
        this.payload = builder.payload;
        this.timestamp = builder.timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getFromAgentId() { return fromAgentId; }
    public String getToAgentId() { return toAgentId; }
    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
    public Instant getTimestamp() { return timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String messageId = UUID.randomUUID().toString();
        private String fromAgentId;
        private String toAgentId;
        private MessageType type = MessageType.REQUEST;
        private Object payload;
        private Instant timestamp = Instant.now();

        public Builder from(String fromAgentId) {
            this.fromAgentId = fromAgentId;
            return this;
        }

        public Builder to(String toAgentId) {
            this.toAgentId = toAgentId;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public AgentMessage build() {
            if (fromAgentId == null || toAgentId == null) {
                throw new IllegalArgumentException("fromAgentId and toAgentId are required");
            }
            return new AgentMessage(this);
        }
    }

    public enum MessageType {
        REQUEST,
        RESPONSE,
        NOTIFICATION,
        FEEDBACK,
        STATE_UPDATE
    }
}
