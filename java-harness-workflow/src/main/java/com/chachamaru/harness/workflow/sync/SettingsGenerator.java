package com.chachamaru.harness.workflow.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * settings.json 生成器
 *
 * <p>从 SyncConfig 生成 .claude-plugin/settings.json
 * 包含 agent、env、permissions、sandbox 配置
 *
 * @see SyncSkill
 * @since 4.0.0-java
 */
public class SettingsGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 生成 .claude-plugin/settings.json
     *
     * @param projectRoot 项目根目录
     * @param config 配置对象
     * @return 生成的文件路径
     * @throws IOException 如果生成失败
     */
    public static String generate(File projectRoot, SyncConfig config) throws IOException {
        Map<String, Object> settingsJson = new HashMap<>();

        // $schema
        settingsJson.put("$schema", "https://json.schemastore.org/claude-code-settings.json");

        // [agent]
        if (config.getAgent() != null && config.getAgent().getDefaultAgent() != null) {
            settingsJson.put("agent", config.getAgent().getDefaultAgent());
        }

        // [env]
        if (config.getEnv() != null && !config.getEnv().isEmpty()) {
            settingsJson.put("env", config.getEnv());
        }

        // [safety.permissions]
        if (config.getSafety() != null && config.getSafety().getPermissions() != null) {
            SyncConfig.PermissionsConfig perm = config.getSafety().getPermissions();

            List<String> allow = perm.getAllow();
            List<String> deny = perm.getDeny();
            List<String> ask = perm.getAsk();

            if (allow != null || deny != null || ask != null) {
                Map<String, Object> permissions = new HashMap<>();

                if (allow != null && !allow.isEmpty()) {
                    permissions.put("allow", allow);
                }
                if (deny != null && !deny.isEmpty()) {
                    permissions.put("deny", deny);
                }
                if (ask != null && !ask.isEmpty()) {
                    permissions.put("ask", ask);
                }

                settingsJson.put("permissions", permissions);
            }
        }

        // [safety.sandbox]
        if (config.getSafety() != null && config.getSafety().getSandbox() != null) {
            SyncConfig.SandboxConfig sandbox = config.getSafety().getSandbox();

            boolean failIfUnavailable = sandbox.isFailIfUnavailable();
            SyncConfig.NetworkConfig network = sandbox.getNetwork();
            SyncConfig.FilesystemConfig filesystem = sandbox.getFilesystem();

            if (failIfUnavailable || network != null || filesystem != null) {
                Map<String, Object> sandboxMap = new HashMap<>();

                sandboxMap.put("failIfUnavailable", failIfUnavailable);

                if (network != null && network.getDeniedDomains() != null && !network.getDeniedDomains().isEmpty()) {
                    Map<String, Object> networkMap = new HashMap<>();
                    networkMap.put("deniedDomains", network.getDeniedDomains());
                    sandboxMap.put("network", networkMap);
                }

                if (filesystem != null) {
                    List<String> denyRead = filesystem.getDenyRead();
                    List<String> allowRead = filesystem.getAllowRead();

                    if ((denyRead != null && !denyRead.isEmpty()) || (allowRead != null && !allowRead.isEmpty())) {
                        Map<String, Object> fsMap = new HashMap<>();

                        if (denyRead != null && !denyRead.isEmpty()) {
                            fsMap.put("denyRead", denyRead);
                        }
                        if (allowRead != null && !allowRead.isEmpty()) {
                            fsMap.put("allowRead", allowRead);
                        }

                        sandboxMap.put("filesystem", fsMap);
                    }
                }

                settingsJson.put("sandbox", sandboxMap);
            }
        }

        // 写入文件
        Path pluginDir = projectRoot.toPath().resolve(".claude-plugin");
        Files.createDirectories(pluginDir);

        Path outputPath = pluginDir.resolve("settings.json");
        objectMapper.writeValue(outputPath.toFile(), settingsJson);

        return outputPath.toString();
    }
}

