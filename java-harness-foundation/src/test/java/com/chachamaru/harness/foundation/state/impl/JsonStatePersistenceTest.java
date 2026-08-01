package com.chachamaru.harness.foundation.state.impl;

import com.chachamaru.harness.foundation.state.PersistenceException;
import com.chachamaru.harness.foundation.state.StatePersistenceEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON 状态持久化引擎测试
 */
class JsonStatePersistenceTest {

    @TempDir
    Path tempDir;

    private StatePersistenceEngine<TestState> persistence;
    private Path testFile;

    @BeforeEach
    void setUp() {
        persistence = new JsonStatePersistence<>();
        testFile = tempDir.resolve("test-state.json");
    }

    @AfterEach
    void tearDown() {
        // 清理测试文件
        if (persistence.exists(testFile)) {
            try {
                persistence.delete(testFile);
            } catch (PersistenceException ignored) {
            }
        }
    }

    @Test
    void testSaveAndLoadSimpleState() throws PersistenceException {
        TestState state = new TestState();
        state.setName("test-state");
        state.setValue(42);
        state.setEnabled(true);

        // 保存状态
        persistence.save(state, testFile);
        assertTrue(persistence.exists(testFile), "File should exist after save");

        // 加载状态
        Optional<TestState> loaded = persistence.load(testFile, TestState.class);
        assertTrue(loaded.isPresent(), "State should be loaded");

        TestState loadedState = loaded.get();
        assertEquals(state.getName(), loadedState.getName());
        assertEquals(state.getValue(), loadedState.getValue());
        assertEquals(state.isEnabled(), loadedState.isEnabled());
    }

    @Test
    void testSaveAndLoadComplexState() throws PersistenceException {
        ComplexState state = new ComplexState();
        state.setName("complex-state");
        state.setMetadata(Map.of(
                "key1", "value1",
                "key2", "value2"
        ));
        state.setTags(List.of("tag1", "tag2", "tag3"));

        // 使用类型转换的持久化引擎
        StatePersistenceEngine<ComplexState> complexPersistence = new JsonStatePersistence<>();

        // 保存状态
        complexPersistence.save(state, testFile);
        assertTrue(complexPersistence.exists(testFile), "File should exist after save");

        // 加载状态
        Optional<ComplexState> loaded = complexPersistence.load(testFile, ComplexState.class);
        assertTrue(loaded.isPresent(), "State should be loaded");

        ComplexState loadedState = loaded.get();
        assertEquals(state.getName(), loadedState.getName());
        assertEquals(state.getMetadata(), loadedState.getMetadata());
        assertEquals(state.getTags(), loadedState.getTags());
    }

    @Test
    void testLoadNonExistentFile() throws PersistenceException {
        Optional<TestState> loaded = persistence.load(testFile, TestState.class);
        assertFalse(loaded.isPresent(), "Should return empty for non-existent file");
    }

    @Test
    void testDeleteFile() throws PersistenceException {
        TestState state = new TestState();
        state.setName("temp-state");

        persistence.save(state, testFile);
        assertTrue(persistence.exists(testFile), "File should exist after save");

        persistence.delete(testFile);
        assertFalse(persistence.exists(testFile), "File should not exist after delete");
    }

    @Test
    void testSaveNullState() {
        assertThrows(PersistenceException.class, () -> {
            persistence.save(null, testFile);
        });
    }

    @Test
    void testCreateDirectoryIfNeeded() throws PersistenceException {
        Path nestedFile = tempDir.resolve("nested").resolve("dir").resolve("state.json");

        TestState state = new TestState();
        state.setName("nested-state");

        persistence.save(state, nestedFile);
        assertTrue(persistence.exists(nestedFile), "File should be created in nested directory");
    }

    @Test
    void testGetFormat() {
        assertEquals("json", persistence.getFormat());
    }

    @Test
    void testThreadSafety() throws PersistenceException, InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 100;
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        TestState state = new TestState();
                        state.setName("thread-" + threadId + "-op-" + j);
                        state.setValue(j);

                        Path threadFile = tempDir.resolve("thread-" + threadId + ".json");
                        persistence.save(state, threadFile);

                        Optional<TestState> loaded = persistence.load(threadFile, TestState.class);
                        assertTrue(loaded.isPresent());
                        assertEquals("thread-" + threadId + "-op-" + j, loaded.get().getName());
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

        // 验证所有线程都成功完成了操作
        for (int i = 0; i < THREAD_COUNT; i++) {
            Path threadFile = tempDir.resolve("thread-" + i + ".json");
            assertTrue(persistence.exists(threadFile), "Thread file should exist");
        }
    }

    // 测试用的状态类
    static class TestState {
        private String name;
        private int value;
        private boolean enabled;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    static class ComplexState {
        private String name;
        private Map<String, String> metadata;
        private List<String> tags;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
}
