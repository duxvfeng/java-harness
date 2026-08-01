package com.chachamaru.harness.foundation.state;

import com.chachamaru.harness.foundation.state.impl.JsonStatePersistence;
import com.chachamaru.harness.foundation.state.impl.YamlStatePersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态持久化引擎集成测试
 * 测试完整的工作流程和系统集成
 */
class StatePersistenceIntegrationTest {

    @TempDir
    Path tempDir;

    private Path jsonStateFile;
    private Path yamlStateFile;
    private StatePersistenceEngine<AppState> jsonPersistence;
    private StatePersistenceEngine<AppState> yamlPersistence;

    @BeforeEach
    void setUp() {
        jsonStateFile = tempDir.resolve(".claude").resolve("state").resolve("app-state.json");
        yamlStateFile = tempDir.resolve(".claude").resolve("state").resolve("app-state.yaml");

        jsonPersistence = new JsonStatePersistence<>();
        yamlPersistence = new YamlStatePersistence<>();
    }

    @AfterEach
    void tearDown() {
        // 清理测试文件
        try {
            if (jsonPersistence.exists(jsonStateFile)) {
                jsonPersistence.delete(jsonStateFile);
            }
            if (yamlPersistence.exists(yamlStateFile)) {
                yamlPersistence.delete(yamlStateFile);
            }
        } catch (PersistenceException ignored) {
        }
    }

    @Test
    void testCompleteWorkflowJson() throws PersistenceException {
        // 创建初始状态
        AppState initialState = new AppState();
        initialState.setSessionId("test-session-001");
        initialState.setProjectName("java-harness-test");
        initialState.setLastCommitHash("abc123");

        // 保存状态
        jsonPersistence.save(initialState, jsonStateFile);
        assertTrue(jsonPersistence.exists(jsonStateFile));

        // 加载状态
        Optional<AppState> loadedState = jsonPersistence.load(jsonStateFile, AppState.class);
        assertTrue(loadedState.isPresent());

        AppState state = loadedState.get();
        assertEquals(initialState.getSessionId(), state.getSessionId());
        assertEquals(initialState.getProjectName(), state.getProjectName());
        assertEquals(initialState.getLastCommitHash(), state.getLastCommitHash());
    }

    @Test
    void testCompleteWorkflowYaml() throws PersistenceException {
        // 创建初始状态
        AppState initialState = new AppState();
        initialState.setSessionId("test-session-002");
        initialState.setProjectName("java-harness-test");
        initialState.setLastCommitHash("def456");

        // 保存状态
        yamlPersistence.save(initialState, yamlStateFile);
        assertTrue(yamlPersistence.exists(yamlStateFile));

        // 加载状态
        Optional<AppState> loadedState = yamlPersistence.load(yamlStateFile, AppState.class);
        assertTrue(loadedState.isPresent());

        AppState state = loadedState.get();
        assertEquals(initialState.getSessionId(), state.getSessionId());
        assertEquals(initialState.getProjectName(), state.getProjectName());
        assertEquals(initialState.getLastCommitHash(), state.getLastCommitHash());
    }

    @Test
    void testStateUpdateWorkflow() throws PersistenceException {
        // 创建并保存初始状态
        AppState state = new AppState();
        state.setSessionId("session-001");
        state.setProjectName("initial-project");

        jsonPersistence.save(state, jsonStateFile);

        // 更新状态
        Optional<AppState> loaded = jsonPersistence.load(jsonStateFile, AppState.class);
        assertTrue(loaded.isPresent());

        AppState updatedState = loaded.get();
        updatedState.setProjectName("updated-project");
        updatedState.setLastCommitHash("new-hash-789");

        // 保存更新后的状态
        jsonPersistence.save(updatedState, jsonStateFile);

        // 验证更新
        Optional<AppState> finalState = jsonPersistence.load(jsonStateFile, AppState.class);
        assertTrue(finalState.isPresent());
        assertEquals("updated-project", finalState.get().getProjectName());
        assertEquals("new-hash-789", finalState.get().getLastCommitHash());
    }

    @Test
    void testMultipleFormatsConsistency() throws PersistenceException {
        // 创建测试状态
        AppState state = new AppState();
        state.setSessionId("consistency-test");
        state.setProjectName("multi-format-test");
        state.setLastCommitHash("consistency-hash");

        // 保存为 JSON
        jsonPersistence.save(state, jsonStateFile);

        // 保存为 YAML
        yamlPersistence.save(state, yamlStateFile);

        // 分别加载并验证一致性
        Optional<AppState> jsonLoaded = jsonPersistence.load(jsonStateFile, AppState.class);
        Optional<AppState> yamlLoaded = yamlPersistence.load(yamlStateFile, AppState.class);

        assertTrue(jsonLoaded.isPresent());
        assertTrue(yamlLoaded.isPresent());

        AppState jsonState = jsonLoaded.get();
        AppState yamlState = yamlLoaded.get();

        assertEquals(jsonState.getSessionId(), yamlState.getSessionId());
        assertEquals(jsonState.getProjectName(), yamlState.getProjectName());
        assertEquals(jsonState.getLastCommitHash(), yamlState.getLastCommitHash());
    }

    @Test
    void testErrorRecovery() throws PersistenceException {
        // 测试损坏文件的恢复
        AppState validState = new AppState();
        validState.setSessionId("recovery-test");

        jsonPersistence.save(validState, jsonStateFile);

        // 删除文件后重新保存
        jsonPersistence.delete(jsonStateFile);
        assertFalse(jsonPersistence.exists(jsonStateFile));

        AppState recoveredState = new AppState();
        recoveredState.setSessionId("recovered-session");

        jsonPersistence.save(recoveredState, jsonStateFile);

        Optional<AppState> loaded = jsonPersistence.load(jsonStateFile, AppState.class);
        assertTrue(loaded.isPresent());
        assertEquals("recovered-session", loaded.get().getSessionId());
    }

    @Test
    void testFactoryBasedWorkflow() throws PersistenceException {
        // 使用工厂创建持久化引擎
        StatePersistenceEngine<AppState> jsonEngine =
                StatePersistenceFactory.createFromExtension(jsonStateFile);
        StatePersistenceEngine<AppState> yamlEngine =
                StatePersistenceFactory.createFromExtension(yamlStateFile);

        AppState state = new AppState();
        state.setSessionId("factory-test");
        state.setProjectName("factory-project");

        // 使用工厂创建的引擎保存和加载
        jsonEngine.save(state, jsonStateFile);
        Optional<AppState> loaded = jsonEngine.load(jsonStateFile, AppState.class);

        assertTrue(loaded.isPresent());
        assertEquals("factory-test", loaded.get().getSessionId());
    }

    @Test
    void testNestedDirectoryCreation() throws PersistenceException {
        // 测试深层嵌套目录的自动创建
        Path deepNestedFile = tempDir.resolve("level1")
                .resolve("level2")
                .resolve("level3")
                .resolve("deep-state.json");

        StatePersistenceEngine<AppState> persistence = new JsonStatePersistence();

        AppState state = new AppState();
        state.setSessionId("deep-test");

        persistence.save(state, deepNestedFile);
        assertTrue(persistence.exists(deepNestedFile));

        Optional<AppState> loaded = persistence.load(deepNestedFile, AppState.class);
        assertTrue(loaded.isPresent());
    }

    // 测试用的应用状态类
    static class AppState {
        private String sessionId;
        private String projectName;
        private String lastCommitHash;
        private long timestamp;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getLastCommitHash() { return lastCommitHash; }
        public void setLastCommitHash(String lastCommitHash) { this.lastCommitHash = lastCommitHash; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
