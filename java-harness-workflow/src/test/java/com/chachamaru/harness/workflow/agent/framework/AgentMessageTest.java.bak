package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentMessage 测试")
public class AgentMessageTest {

    @Test
    @DisplayName("应该创建 REQUEST 类型消息")
    public void testRequestMessage() {
        AgentMessage message = AgentMessage.builder()
                .from("worker")
                .to("reviewer")
                .type(AgentMessage.MessageType.REQUEST)
                .payload("请审查代码")
                .build();

        assertEquals("worker", message.getFromAgentId());
        assertEquals("reviewer", message.getToAgentId());
        assertEquals(AgentMessage.MessageType.REQUEST, message.getType());
        assertEquals("请审查代码", message.getPayload());
        assertNotNull(message.getMessageId());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("应该创建 RESPONSE 类型消息")
    public void testResponseMessage() {
        AgentMessage message = AgentMessage.builder()
                .from("reviewer")
                .to("worker")
                .type(AgentMessage.MessageType.RESPONSE)
                .payload("审查完成，有2个问题")
                .build();

        assertEquals(AgentMessage.MessageType.RESPONSE, message.getType());
    }

    @Test
    @DisplayName("应该支持所有消息类型")
    public void testAllMessageTypes() {
        assertEquals(5, AgentMessage.MessageType.values().length);
        assertEquals("REQUEST", AgentMessage.MessageType.REQUEST.name());
        assertEquals("RESPONSE", AgentMessage.MessageType.RESPONSE.name());
        assertEquals("NOTIFICATION", AgentMessage.MessageType.NOTIFICATION.name());
        assertEquals("FEEDBACK", AgentMessage.MessageType.FEEDBACK.name());
        assertEquals("STATE_UPDATE", AgentMessage.MessageType.STATE_UPDATE.name());
    }
}
