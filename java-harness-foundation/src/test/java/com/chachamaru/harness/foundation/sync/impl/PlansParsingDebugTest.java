package com.chachamaru.harness.foundation.sync.impl;

import com.chachamaru.harness.foundation.sync.SyncException;
import com.chachamaru.harness.foundation.sync.SyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plans.md 解析调试测试
 */
class PlansParsingDebugTest {

    @TempDir
    Path tempDir;

    @Test
    void testSimpleParsing() throws IOException, SyncException {
        PlansMdSyncService syncService = new PlansMdSyncService();
        Path plansPath = tempDir.resolve("Plans.md");

        // 创建简化的测试内容
        String content =
                "# Test Plans\n\n" +
                "| Task | 内容 | DoD | Depends | Status |\n" +
                "|------|------|-----|---------|--------|\n" +
                "| 8.4.1 | Task 1 | DoD1 | - | cc:completed ✅ |\n" +
                "| 8.4.2 | Task 2 | DoD2 | 8.4.1 | cc:TODO 📝 |\n";

        Files.writeString(plansPath, content);

        // 测试同步
        SyncResult result = syncService.syncFromPlans(plansPath);

        System.out.println("Success: " + result.isSuccess());
        System.out.println("Change count: " + result.getChangeCount());
        result.getChanges().forEach(change ->
            System.out.println("Task: " + change.getTaskId() + ", Status: " + change.getNewValue())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getChangeCount() >= 1, "Should parse at least one task");
    }

    @Test
    void testExactFormatFromPlans() throws IOException, SyncException {
        PlansMdSyncService syncService = new PlansMdSyncService();
        Path plansPath = tempDir.resolve("Plans.md");

        // 完全按照实际 Plans.md 的格式（包含缺少的标题）
        String content =
                "| Task | 内容 | DoD | Depends | Status |\n" +
                "|------|------|-----|---------|--------|\n" +
                "| 8.4.1 | 实现状态持久化引擎 | 支持 JSON/YAML 持久化 | 测试通过，状态能正确保存和恢复 | 8.3.4 | cc:completed ✅ |\n" +
                "| 8.4.2 | 实现状态同步机制 | 与 Plans.md 双向同步 | 测试通过，状态能正确同步 | 8.4.1 | cc:TODO 📝 |\n";

        Files.writeString(plansPath, content);

        SyncResult result = syncService.syncFromPlans(plansPath);

        System.out.println("Parse result:");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Changes: " + result.getChangeCount());
        result.getChanges().forEach(change ->
            System.out.println("  " + change.getTaskId() + " -> " + change.getNewValue())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getChangeCount() >= 1, "Should parse at least 1 task, got: " + result.getChangeCount());
    }
}
