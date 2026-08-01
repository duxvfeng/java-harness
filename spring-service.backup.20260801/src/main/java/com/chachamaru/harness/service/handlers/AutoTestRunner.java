package com.chachamaru.harness.service.handlers;

import com.chachamaru.harness.service.domain.Session;
import com.chachamaru.harness.service.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 长生命周期 Handler：自动测试运行器
 * 在后台运行测试，定期更新状态
 */
@Component
public class AutoTestRunner {
    private static final Logger log = LoggerFactory.getLogger(AutoTestRunner.class);
    
    private final StateService stateService;
    private final ExecutorService executorService;

    public AutoTestRunner(StateService stateService) {
        this.stateService = stateService;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * 异步启动测试运行
     */
    public CompletableFuture<TestResult> runTestsAsync(String sessionId, String testCommand) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting test run for session: {} with command: {}", sessionId, testCommand);
                
                String testId = UUID.randomUUID().toString();
                LocalDateTime startTime = LocalDateTime.now();
                
                // 模拟测试运行（实际会调用测试框架）
                Thread.sleep(2000);
                
                LocalDateTime endTime = LocalDateTime.now();
                boolean passed = true; // 模拟测试通过
                
                TestResult result = new TestResult(testId, sessionId, testCommand, passed, startTime, endTime);
                log.info("Test run completed for session: {} - Result: {}", sessionId, passed ? "PASSED" : "FAILED");
                
                return result;
                
            } catch (Exception e) {
                log.error("Test run failed for session: {}", sessionId, e);
                return new TestResult(null, sessionId, testCommand, false, null, null);
            }
        }, executorService);
    }

    /**
     * 测试结果
     */
    public record TestResult(
        String testId,
        String sessionId,
        String command,
        boolean passed,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        public long getDuration() {
            if (startTime == null || endTime == null) return 0;
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
