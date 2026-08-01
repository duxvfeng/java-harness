package com.chachamaru.harness.cli;

import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.rules.*;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.ipc.IpcClient;

import java.util.Map;

/**
 * CLI Gateway 性能测试
 */
public class IpcPerformanceTest {
    
    public static void main(String[] args) {
        System.out.println("=== CLI Gateway Performance Test ===\n");
        
        // 测试 Guardrail Engine 性能
        testGuardrailPerformance();
        
        // 测试 Hook 处理性能
        testHookProcessingPerformance();
        
        // 测试 IPC 通信性能
        testIpcPerformance();
        
        System.out.println("\n=== Performance Test Complete ===");
    }
    
    private static void testGuardrailPerformance() {
        System.out.println("Guardrail Engine Performance Test:");
        
        GuardrailEngine engine = new GuardrailEngine();
        engine.registerRule(new R01NoSudo());
        engine.registerRule(new R02ProtectedPath());
        engine.registerRule(new R05RmRf());
        engine.registerRule(new R06GitPushForce());
        
        HookInput input = new HookInput(
            "test-session",
            "/transcript",
            "/project",
            "default",
            "PreToolUse",
            "Bash",
            Map.of("command", "echo 'hello world'"),
            "/plugin"
        );
        
        // 预热
        for (int i = 0; i < 100; i++) {
            engine.evaluate(input);
        }
        
        // 测试
        int iterations = 1000;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            engine.evaluate(input);
        }
        
        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1_000_000; // 转换为毫秒
        double avgTime = (double) totalTime / iterations;
        
        System.out.printf("  - %d iterations: %d ms total%n", iterations, totalTime);
        System.out.printf("  - Average time: %.3f ms%n", avgTime);
        System.out.printf("  - Target: < 5ms%n");
        
        if (avgTime < 5.0) {
            System.out.println("  ✓ Performance requirement met");
        } else {
            System.out.println("  ✗ Performance requirement NOT met");
        }
        
        System.out.println();
    }
    
    private static void testHookProcessingPerformance() {
        System.out.println("Hook Processing Performance Test:");
        
        // 模拟完整的 Hook 处理流程
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 100; i++) {
            HookInput input = new HookInput(
                "test-session",
                "/transcript",
                "/project",
                "default",
                "PreToolUse",
                "Read",
                Map.of("file_path", "/project/README.md"),
                "/plugin"
            );
            
            // 模拟处理步骤
            boolean isValid = input.isValid();
            assert isValid;
        }
        
        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1_000_000;
        double avgTime = (double) totalTime / 100;
        
        System.out.printf("  - Average processing time: %.3f ms%n", avgTime);
        System.out.printf("  - Target: < 10ms (total hook response)%n");
        
        if (avgTime < 10.0) {
            System.out.println("  ✓ Performance requirement met");
        } else {
            System.out.println("  ✗ Performance requirement NOT met");
        }
        
        System.out.println();
    }
    
    private static void testIpcPerformance() {
        System.out.println("IPC Communication Performance Test:");
        
        // 注意：这个测试需要 Spring Boot Service 运行
        System.out.println("  - Requires Spring Boot Service running");
        System.out.println("  - To test: Start service and run integration tests");
        System.out.println("  - Target: < 100ms for async IPC calls");
        System.out.println();
    }
}
