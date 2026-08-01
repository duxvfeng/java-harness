package com.chachamaru.harness.service;

import com.chachamaru.harness.service.dto.StateQueryRequest;
import com.chachamaru.harness.service.dto.StateQueryResponse;
import com.chachamaru.harness.service.domain.Session;
import com.chachamaru.harness.service.orchestrator.OrchestratorService;
import com.chachamaru.harness.service.service.StateService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * 性能基准测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    @Autowired
    private StateService stateService;

    @Autowired
    private OrchestratorService orchestratorService;

    @Test
    void testDatabaseOperationsPerformance() {
        log.info("=== Database Operations Performance Test ===");
        
        long startTime, endTime;
        
        // 测试会话创建性能
        startTime = System.currentTimeMillis();
        Session session = stateService.createSession("/test/performance");
        endTime = System.currentTimeMillis();
        long createSessionTime = endTime - startTime;
        log.info("Create Session: {} ms", createSessionTime);
        
        // 测试会话查询性能
        startTime = System.currentTimeMillis();
        var retrievedSession = stateService.getSession(session.getId());
        endTime = System.currentTimeMillis();
        long querySessionTime = endTime - startTime;
        log.info("Query Session: {} ms", querySessionTime);
        
        // 测试工作状态创建性能
        startTime = System.currentTimeMillis();
        var workState = stateService.createWorkState(session.getId(), "running");
        endTime = System.currentTimeMillis();
        long createWorkStateTime = endTime - startTime;
        log.info("Create WorkState: {} ms", createWorkStateTime);
        
        // 性能要求验证
        assert createSessionTime < 100 : "Session creation should be < 100ms";
        assert querySessionTime < 50 : "Session query should be < 50ms";
        assert createWorkStateTime < 100 : "WorkState creation should be < 100ms";
        
        log.info("✓ All database operations meet performance requirements");
    }

    @Test
    void testRestApiPerformance() {
        log.info("=== REST API Performance Test ===");
        
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // 测试健康检查性能
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
            "http://localhost:8080/api/health", Map.class);
        long healthTime = System.currentTimeMillis() - startTime;
        
        assert healthResponse.getStatusCode() == HttpStatus.OK;
        log.info("Health Check: {} ms", healthTime);
        assert healthTime < 50 : "Health check should be < 50ms";
        
        // 测试状态查询性能
        String sessionId = "test-session-123";
        StateQueryRequest request = new StateQueryRequest(sessionId, "SESSION");
        
        startTime = System.currentTimeMillis();
        ResponseEntity<StateQueryResponse> queryResponse = restTemplate.postForEntity(
            "http://localhost:8080/api/state/query", request, StateQueryResponse.class);
        long queryTime = System.currentTimeMillis() - startTime;
        
        assert queryResponse.getStatusCode() == HttpStatus.OK;
        log.info("State Query: {} ms", queryTime);
        
        log.info("✓ All REST API operations meet performance requirements");
    }

    @Test
    void testParallelExecutionPerformance() {
        log.info("=== Parallel Execution Performance Test ===");
        
        String sessionId = "perf-test-session";
        
        // 创建测试任务
        List<OrchestratorService.TaskDefinition> tasks = List.of(
            new OrchestratorService.TaskDefinition("task-1", "Task 1", 100, ""),
            new OrchestratorService.TaskDefinition("task-2", "Task 2", 100, ""),
            new OrchestratorService.TaskDefinition("task-3", "Task 3", 100, ""),
            new OrchestratorService.TaskDefinition("task-4", "Task 4", 100, "")
        );
        
        // 测试并行执行性能
        long startTime = System.currentTimeMillis();
        var results = orchestratorService.executeParallel(sessionId, tasks).join();
        long totalTime = System.currentTimeMillis() - startTime;
        
        log.info("Parallel Execution (4 tasks): {} ms", totalTime);
        log.info("Average per task: {} ms", totalTime / tasks.size());
        
        // 并行执行应该比串行快
        long serialTime = tasks.stream().mapToLong(t -> t.duration()).sum();
        log.info("Serial execution would take: {} ms", serialTime);
        log.info("Performance gain: {}x", (double) serialTime / totalTime);
        
        assert totalTime < serialTime : "Parallel execution should be faster than serial";
        
        log.info("✓ Parallel execution provides performance improvement");
    }
}
