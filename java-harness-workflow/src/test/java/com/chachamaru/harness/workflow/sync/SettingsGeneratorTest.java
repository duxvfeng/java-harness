package com.chachamaru.harness.workflow.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SettingsGenerator 测试")
class SettingsGeneratorTest {

    @Test
    @DisplayName("应该生成完整的 settings.json 包含所有配置节")
    void testGenerateSettingsJSON_FullConfig(@TempDir Path tempDir) throws Exception {
        SyncConfig config = createFullConfig();

        String generatedPath = SettingsGenerator.generate(tempDir.toFile(), config);

        assertNotNull(generatedPath);
        assertTrue(Files.exists(Path.of(generatedPath)));

        // 验证 JSON 内容
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(Path.of(generatedPath).toFile());

        // 验证 $schema
        assertEquals("https://json.schemastore.org/claude-code-settings.json", json.get("$schema").asText());

        // 验证 agent
        assertEquals("claude-opus-5", json.get("agent").asText());

        // 验证 env
        assertEquals("VALUE1", json.get("env").get("KEY1").asText());

        // 验证 permissions
        assertTrue(json.get("permissions").get("allow").isArray());
        assertEquals("Bash(git status:*)", json.get("permissions").get("allow").get(0).asText());

        // 验证 sandbox
        assertTrue(json.get("sandbox").get("failIfUnavailable").asBoolean());
        assertEquals("169.254.169.254", json.get("sandbox").get("network").get("deniedDomains").get(0).asText());
    }

    @Test
    @DisplayName("应该生成最小化的 settings.json 只包含 $schema")
    void testGenerateSettingsJSON_MinimalConfig(@TempDir Path tempDir) throws Exception {
        // 最小配置，只设置必填字段
        SyncConfig config = new SyncConfig();

        String generatedPath = SettingsGenerator.generate(tempDir.toFile(), config);

        assertNotNull(generatedPath);

        // 验证 JSON 存在但最小化
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(Path.of(generatedPath).toFile());

        // 应该有 $schema 字段
        assertTrue(json.has("$schema"));
        assertEquals("https://json.schemastore.org/claude-code-settings.json", json.get("$schema").asText());

        // 没有 agent（因为没设置）
        assertFalse(json.has("agent"));

        // 没有 permissions（因为没设置）
        assertFalse(json.has("permissions"));

        // 没有 sandbox（因为没设置）
        assertFalse(json.has("sandbox"));
    }

    @Test
    @DisplayName("应该正确处理环境变量映射")
    void testGenerateSettingsJSON_EnvVariables(@TempDir Path tempDir) throws Exception {
        SyncConfig config = new SyncConfig();
        config.setEnv(Map.of(
            "KEY1", "VALUE1",
            "KEY2", "VALUE2",
            "KEY3", "VALUE3"
        ));

        String generatedPath = SettingsGenerator.generate(tempDir.toFile(), config);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(Path.of(generatedPath).toFile());

        // 验证所有环境变量
        assertEquals(3, json.get("env").size());
        assertEquals("VALUE1", json.get("env").get("KEY1").asText());
        assertEquals("VALUE2", json.get("env").get("KEY2").asText());
        assertEquals("VALUE3", json.get("env").get("KEY3").asText());
    }

    @Test
    @DisplayName("应该正确处理权限配置")
    void testGenerateSettingsJSON_Permissions(@TempDir Path tempDir) throws Exception {
        SyncConfig config = new SyncConfig();
        SyncConfig.SafetyConfig safety = new SyncConfig.SafetyConfig();

        SyncConfig.PermissionsConfig permissions = new SyncConfig.PermissionsConfig();
        permissions.setAllow(List.of("Bash(git status:*)"));
        permissions.setDeny(List.of("Bash(rm -rf *)"));
        permissions.setAsk(List.of("Write**"));

        safety.setPermissions(permissions);
        config.setSafety(safety);

        String generatedPath = SettingsGenerator.generate(tempDir.toFile(), config);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(Path.of(generatedPath).toFile());

        // 验证权限配置
        assertTrue(json.get("permissions").get("allow").isArray());
        assertTrue(json.get("permissions").get("deny").isArray());
        assertTrue(json.get("permissions").get("ask").isArray());

        assertEquals("Bash(git status:*)", json.get("permissions").get("allow").get(0).asText());
        assertEquals("Bash(rm -rf *)", json.get("permissions").get("deny").get(0).asText());
        assertEquals("Write**", json.get("permissions").get("ask").get(0).asText());
    }

    @Test
    @DisplayName("应该创建 .claude-plugin 目录")
    void testGenerateSettingsJSON_CreatesTargetDirectory(@TempDir Path tempDir) throws Exception {
        SyncConfig config = new SyncConfig();

        String generatedPath = SettingsGenerator.generate(tempDir.toFile(), config);

        // 验证目标目录已创建
        Path targetDir = tempDir.resolve(".claude-plugin");
        assertTrue(Files.exists(targetDir));
        assertTrue(Files.isDirectory(targetDir));
    }

    /**
     * 创建完整的配置对象
     */
    private SyncConfig createFullConfig() {
        SyncConfig config = new SyncConfig();

        // Agent
        SyncConfig.AgentConfig agent = new SyncConfig.AgentConfig();
        agent.setDefaultAgent("claude-opus-5");
        config.setAgent(agent);

        // Env
        config.setEnv(Map.of("KEY1", "VALUE1"));

        // Safety
        SyncConfig.SafetyConfig safety = new SyncConfig.SafetyConfig();

        SyncConfig.PermissionsConfig permissions = new SyncConfig.PermissionsConfig();
        permissions.setAllow(List.of("Bash(git status:*)"));
        safety.setPermissions(permissions);

        SyncConfig.SandboxConfig sandbox = new SyncConfig.SandboxConfig();
        sandbox.setFailIfUnavailable(true);

        SyncConfig.NetworkConfig network = new SyncConfig.NetworkConfig();
        network.setDeniedDomains(List.of("169.254.169.254"));
        sandbox.setNetwork(network);

        safety.setSandbox(sandbox);
        config.setSafety(safety);

        return config;
    }
}
