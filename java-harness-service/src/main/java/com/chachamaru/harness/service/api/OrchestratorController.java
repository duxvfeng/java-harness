package com.chachamaru.harness.service.api;

import com.chachamaru.harness.service.orchestrator.OrchestratorService;
import com.chachamaru.harness.service.handlers.AutoTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 并行编排和长生命周期 Handler API
 */
@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorController.class);

    private final OrchestratorService orchestratorService;
    private final AutoTestRunner autoTestRunner;

    public OrchestratorController(OrchestratorService orchestratorService, AutoTestRunner autoTestRunner) {
        this.orchestratorService = orchestratorService;
        this.autoTestRunner = autoTestRunner;
    }

    /**
     * 并行执行任务
     */
    @PostMapping("/execute-parallel")
    public CompletableFuture<ResponseEntity<List<?>>> executeParallel(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> taskData = (List<Map<String, Object>>) request.get("tasks");
        
        List<OrchestratorService.TaskDefinition> tasks = taskData.stream()
            .map(task -> new OrchestratorService.TaskDefinition(
                (String) task.get("id"),
                (String) task.get("name"),
                ((Number) task.getOrDefault("duration", 1000)).longValue(),
                (String) task.getOrDefault("metadata", "")
            ))
            .toList();
        
        return orchestratorService.executeParallel(sessionId, tasks)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * 异步运行测试
     */
    @PostMapping("/run-tests")
    public CompletableFuture<ResponseEntity<AutoTestRunner.TestResult>> runTests(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String testCommand = request.getOrDefault("testCommand", "mvn test");
        
        log.info("Starting test run for session: {}", sessionId);
        
        return autoTestRunner.runTestsAsync(sessionId, testCommand)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "orchestrator-service",
            "capabilities", List.of("parallel-execution", "auto-test-runner")
        );
    }
}
