package com.chachamaru.harness.service;

import com.chachamaru.harness.service.dto.StateQueryRequest;
import com.chachamaru.harness.service.dto.StateQueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 端到端集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testReadyEndpoint() throws Exception {
        mockMvc.perform(get("/api/ready"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ready").value(true));
    }

    @Test
    void testCreateSession() throws Exception {
        String projectRoot = "/test/project";
        
        MvcResult result = mockMvc.perform(post("/api/state/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"projectRoot\":\"" + projectRoot + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.projectRoot").value(projectRoot))
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"id\":"));
    }

    @Test
    void testStateQueryFlow() throws Exception {
        // 1. 创建会话
        MvcResult sessionResult = mockMvc.perform(post("/api/state/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"projectRoot\":\"/test/project\"}"))
            .andExpect(status().isOk())
            .andReturn();
        
        String sessionResponse = sessionResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionResponse).get("id").asText();
        
        // 2. 创建工作状态
        mockMvc.perform(post("/api/state/work-states")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":\"" + sessionId + "\",\"status\":\"pending\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.status").value("pending"));
        
        // 3. 查询工作状态
        mockMvc.perform(get("/api/state/work-states/" + sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
        
        // 4. 测试状态查询 API（IPC 通信端点）
        StateQueryRequest queryRequest = new StateQueryRequest(sessionId, "WORK_STATES");
        mockMvc.perform(post("/api/state/query")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(queryRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testErrorHandling() throws Exception {
        // 测试不存在的会话
        mockMvc.perform(get("/api/state/sessions/non-existent-session"))
            .andExpect(status().isNotFound());
        
        // 测试无效的状态查询
        StateQueryRequest queryRequest = new StateQueryRequest("test-id", "INVALID_TYPE");
        mockMvc.perform(post("/api/state/query")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(queryRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error").exists());
    }
}
