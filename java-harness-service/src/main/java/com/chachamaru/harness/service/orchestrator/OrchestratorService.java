package com.chachamaru.harness.service.orchestrator;

import com.chachamaru.harness.service.domain.WorkState;
import com.chachamaru.harness.service.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 并行编排服务（Breezing 模式支持）
 * 负责协调多个 Worker 的并行执行
 */
@Service
public class OrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    
    private final StateService stateService;
    private final ExecutorService executorService;

    public OrchestratorService(StateService stateService) {
        this.stateService = stateService;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * 并行执行多个工作项
     */
    public CompletableFuture<List<WorkState>> executeParallel(String sessionId, List<TaskDefinition> tasks) {
        log.info("Starting parallel execution for session: {} with {} tasks", sessionId, tasks.size());
        
        List<CompletableFuture<WorkState>> futures = tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(() -> executeTask(sessionId, task), executorService))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * 执行单个任务
     */
    private WorkState executeTask(String sessionId, TaskDefinition task) {
        try {
            log.debug("Executing task: {} for session: {}", task.id(), sessionId);
            
            // 创建工作状态
            WorkState workState = stateService.createWorkState(sessionId, "running");
            
            // 模拟任务执行
            Thread.sleep(task.duration());
            
            // 更新状态为完成
            workState = stateService.updateWorkState(workState.getId(), "completed", task.metadata());
            
            log.debug("Completed task: {} for session: {}", task.id(), sessionId);
            return workState;
            
        } catch (Exception e) {
            log.error("Task execution failed: {} for session: {}", task.id(), sessionId, e);
            throw new RuntimeException("Task execution failed", e);
        }
    }

    /**
     * 任务定义
     */
    public record TaskDefinition(
        String id,
        String name,
        long duration,
        String metadata
    ) {}

    /**
     * 关闭资源
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
