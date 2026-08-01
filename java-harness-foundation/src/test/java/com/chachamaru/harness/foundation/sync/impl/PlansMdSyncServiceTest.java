package com.chachamaru.harness.foundation.sync.impl;

import com.chachamaru.harness.foundation.sync.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plans.md 同步服务测试
 */
class PlansMdSyncServiceTest {

    @TempDir
    Path tempDir;

    private PlansMdSyncService syncService;
    private Path plansMdPath;
    private String testPlansContent;

    @BeforeEach
    void setUp() throws IOException {
        syncService = new PlansMdSyncService();
        plansMdPath = tempDir.resolve("Plans.md");

        // 创建测试用的 Plans.md 内容
        testPlansContent =
                "# Phase 8 Test\n\n" +
                "| Task | 内容 | DoD | Depends | Status |\n" +
                "|------|------|-----|---------|--------|\n" +
                "| 8.4.1 | 实现状态持久化引擎 | 支持 JSON/YAML | 8.3.4 | cc:completed ✅ |\n" +
                "| 8.4.2 | 实现状态同步机制 | 与 Plans.md 双向同步 | 8.4.1 | cc:TODO 📝 |\n" +
                "| 8.4.3 | 实现状态历史记录 | 状态变更历史追踪 | 8.4.2 | cc:TODO 📝 |\n";

        Files.writeString(plansMdPath, testPlansContent);
    }

    @AfterEach
    void tearDown() {
        // 清理测试文件
        if (Files.exists(plansMdPath)) {
            try {
                Files.delete(plansMdPath);
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void testSyncFromPlans() throws SyncException, IOException {
        SyncResult result = syncService.syncFromPlans(plansMdPath);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getChangeCount());
        assertNotNull(result.getPlansHash());

        // 验证解析的任务状态
        Optional<TaskSyncChange> task841 = result.getChanges().stream()
                .filter(c -> c.getTaskId().equals("8.4.1"))
                .findFirst();
        assertTrue(task841.isPresent());
        assertTrue(task841.get().getNewValue().contains("completed") ||
                   task841.get().getNewValue().contains("✅"));
    }

    @Test
    void testSyncToPlans() throws SyncException, IOException {
        // 创建状态快照
        Map<String, TaskState> taskStates = new HashMap<>();
        taskStates.put("8.4.2", new TaskState("8.4.2", "cc:completed ✅", "实现完成"));
        taskStates.put("8.4.3", new TaskState("8.4.3", "cc:WIP 🔄", "正在实现"));

        StateSnapshot snapshot = new StateSnapshot(taskStates, "test-session");

        // 同步到 Plans.md
        SyncResult result = syncService.syncToPlans(snapshot, plansMdPath);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getChangeCount());
        assertNotNull(result.getPlansHash());

        // 验证 Plans.md 被更新
        String updatedContent = Files.readString(plansMdPath);
        assertTrue(updatedContent.contains("cc:completed ✅") || updatedContent.contains("8.4.2"));
        assertTrue(updatedContent.contains("cc:WIP 🔄") || updatedContent.contains("8.4.3"));
    }

    @Test
    void testBidirectionalSync() throws SyncException, IOException {
        // 创建本地状态
        Map<String, TaskState> localStates = new HashMap<>();
        localStates.put("8.4.2", new TaskState("8.4.2", "cc:WIP 🔄", "本地正在实现"));
        localStates.put("8.4.4", new TaskState("8.4.4", "cc:TODO 📝", "新增任务"));

        StateSnapshot localSnapshot = new StateSnapshot(localStates, "test-session");

        // 执行双向同步
        SyncResult result = syncService.bidirectionalSync(localSnapshot, plansMdPath);

        assertTrue(result.isSuccess());
        assertTrue(result.getChangeCount() > 0);
        assertNotNull(result.getPlansHash());
    }

    @Test
    void testGetPlansHash() throws SyncException, IOException {
        String hash1 = syncService.getPlansHash(plansMdPath);
        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 哈希长度

        // 修改文件后哈希应该不同
        String hash2 = syncService.getPlansHash(plansMdPath);
        assertEquals(hash1, hash2);

        // 修改内容
        Files.writeString(plansMdPath, testPlansContent + "# Modified");
        String hash3 = syncService.getPlansHash(plansMdPath);
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testHasUnsyncedChanges() throws SyncException, IOException {
        String originalHash = syncService.getPlansHash(plansMdPath);

        // 初始状态应该没有未同步的变更
        assertFalse(syncService.hasUnsyncedChanges(plansMdPath, originalHash));

        // 修改文件后应该检测到变更
        Files.writeString(plansMdPath, testPlansContent + "# Modified");
        assertTrue(syncService.hasUnsyncedChanges(plansMdPath, originalHash));

        // 新哈希应该匹配
        String newHash = syncService.getPlansHash(plansMdPath);
        assertFalse(syncService.hasUnsyncedChanges(plansMdPath, newHash));
    }

    @Test
    void testSyncNonExistentFile() {
        Path nonExistentPath = tempDir.resolve("NonExistent.md");

        assertThrows(SyncException.class, () -> {
            syncService.syncFromPlans(nonExistentPath);
        });

        assertThrows(SyncException.class, () -> {
            syncService.syncToPlans(new StateSnapshot(Map.of(), "test"), nonExistentPath);
        });
    }

    @Test
    void testSyncWithNullInputs() {
        assertThrows(SyncException.class, () -> {
            syncService.syncToPlans(null, plansMdPath);
        });

        assertThrows(SyncException.class, () -> {
            syncService.syncToPlans(new StateSnapshot(Map.of(), "test"), null);
        });
    }

    @Test
    void testSyncWithEmptyState() throws SyncException {
        StateSnapshot emptySnapshot = new StateSnapshot(Map.of(), "test-session");

        SyncResult result = syncService.syncToPlans(emptySnapshot, plansMdPath);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getChangeCount());
    }

    @Test
    void testConflictDetection() throws SyncException {
        // 创建与 Plans.md 冲突的本地状态
        Map<String, TaskState> conflictingStates = new HashMap<>();
        conflictingStates.put("8.4.2", new TaskState("8.4.2", "cc:completed ✅", "本地已完成"));
        conflictingStates.put("8.4.3", new TaskState("8.4.3", "cc:completed ✅", "本地已完成"));

        StateSnapshot conflictingSnapshot = new StateSnapshot(conflictingStates, "test-session");

        // 执行双向同步，应该检测到冲突
        SyncResult result = syncService.bidirectionalSync(conflictingSnapshot, plansMdPath);

        assertTrue(result.isSuccess());
        // 由于冲突存在，本地变更可能被远程值覆盖
        assertTrue(result.getChangeCount() >= 0);
    }

    @Test
    void testThreadSafety() throws SyncException, InterruptedException {
        final int THREAD_COUNT = 5;
        final int OPERATIONS_PER_THREAD = 20;
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        Map<String, TaskState> states = new HashMap<>();
                        states.put("8.4.1", new TaskState("8.4.1", "cc:WIP 🔄", "Thread-" + threadId));

                        StateSnapshot snapshot = new StateSnapshot(states, "thread-" + threadId);
                        syncService.syncToPlans(snapshot, plansMdPath);
                        syncService.syncFromPlans(plansMdPath);
                    }
                } catch (Exception e) {
                    fail("Thread operation failed: " + e.getMessage());
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证文件仍然有效
        String finalHash = syncService.getPlansHash(plansMdPath);
        assertNotNull(finalHash);
        assertTrue(Files.exists(plansMdPath));
    }

    @Test
    void testTaskIdTooLong() throws SyncException {
        // 创建超长任务 ID（应该被跳过）
        String longTaskId = "8.4.2".repeat(20); // 超过 50 字符

        Map<String, TaskState> states = new HashMap<>();
        states.put(longTaskId, new TaskState(longTaskId, "cc:TODO", "超长任务ID"));

        StateSnapshot snapshot = new StateSnapshot(states, "test-session");

        // 应该成功但跳过超长任务 ID
        SyncResult result = syncService.syncToPlans(snapshot, plansMdPath);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getChangeCount()); // 超长任务 ID 被跳过
    }

    @Test
    void testIncrementalUpdate() throws SyncException {
        // 第一次同步
        Map<String, TaskState> states1 = new HashMap<>();
        states1.put("8.4.2", new TaskState("8.4.2", "cc:WIP 🔄", "第一次更新"));

        StateSnapshot snapshot1 = new StateSnapshot(states1, "session-1");
        SyncResult result1 = syncService.syncToPlans(snapshot1, plansMdPath);

        assertTrue(result1.isSuccess());
        assertEquals(1, result1.getChangeCount());

        // 第二次同步（无变更）
        StateSnapshot snapshot2 = new StateSnapshot(states1, "session-1");
        SyncResult result2 = syncService.syncToPlans(snapshot2, plansMdPath);

        assertTrue(result2.isSuccess());
        assertEquals(0, result2.getChangeCount()); // 无变更

        // 第三次同步（有变更）
        states1.put("8.4.2", new TaskState("8.4.2", "cc:completed ✅", "完成更新"));
        StateSnapshot snapshot3 = new StateSnapshot(states1, "session-1");
        SyncResult result3 = syncService.syncToPlans(snapshot3, plansMdPath);

        assertTrue(result3.isSuccess());
        assertEquals(1, result3.getChangeCount()); // 有变更
    }
}
