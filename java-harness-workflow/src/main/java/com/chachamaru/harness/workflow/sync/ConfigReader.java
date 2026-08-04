package com.chachamaru.harness.workflow.sync;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.tomlj.TomlArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TOML 配置文件读取器
 * 职责：解析 harness.toml 并填充 SyncConfig 模型
 */
public class ConfigReader {

    /**
     * 解析 harness.toml 文件
     *
     * @param tomlFile TOML 文件
     * @return 填充的配置对象
     * @throws IOException 文件不存在或解析失败
     */
    public static SyncConfig parse(File tomlFile) throws IOException {
        Path tomlPath = tomlFile.toPath();

        if (!Files.exists(tomlPath)) {
            throw new IOException("Config file not found: " + tomlPath);
        }

        String content = Files.readString(tomlPath);
        TomlParseResult toml = Toml.parse(content);

        if (toml.hasErrors()) {
            throw new IOException("TOML parse errors: " + toml.errors());
        }

        SyncConfig config = new SyncConfig();

        // Parse [project]
        if (toml.contains("project")) {
            TomlTable projectTable = toml.getTable("project");
            SyncConfig.ProjectConfig project = new SyncConfig.ProjectConfig();

            project.setName(projectTable.getString("name"));
            project.setVersion(projectTable.getString("version"));
            project.setDescription(projectTable.getString("description"));
            project.setAuthorName(projectTable.contains("author_name") ? projectTable.getString("author_name") : null);
            project.setAuthorUrl(projectTable.contains("author_url") ? projectTable.getString("author_url") : null);
            project.setHomepage(projectTable.contains("homepage") ? projectTable.getString("homepage") : null);
            project.setRepository(projectTable.contains("repository") ? projectTable.getString("repository") : null);
            project.setLicense(projectTable.contains("license") ? projectTable.getString("license") : null);
            project.setKeywords(projectTable.contains("keywords") ? toList(projectTable.getArray("keywords")) : null);
            project.setOutputStyles(projectTable.contains("output_styles") ? toList(projectTable.getArray("output_styles")) : null);

            config.setProject(project);
        }

        // Parse [agent]
        if (toml.contains("agent")) {
            TomlTable agentTable = toml.getTable("agent");
            SyncConfig.AgentConfig agent = new SyncConfig.AgentConfig();

            agent.setDefaultAgent(agentTable.contains("default") ? agentTable.getString("default") : null);

            config.setAgent(agent);
        }

        // Parse [env]
        if (toml.contains("env")) {
            TomlTable envTable = toml.getTable("env");
            config.setEnv(toStringMap(envTable));
        }

        // Parse [safety]
        if (toml.contains("safety")) {
            TomlTable safetyTable = toml.getTable("safety");
            SyncConfig.SafetyConfig safety = new SyncConfig.SafetyConfig();

            // [safety.permissions]
            if (safetyTable.contains("permissions")) {
                TomlTable permTable = safetyTable.getTable("permissions");
                SyncConfig.PermissionsConfig permissions = new SyncConfig.PermissionsConfig();

                permissions.setAllow(permTable.contains("allow") ? toList(permTable.getArray("allow")) : null);
                permissions.setDeny(permTable.contains("deny") ? toList(permTable.getArray("deny")) : null);
                permissions.setAsk(permTable.contains("ask") ? toList(permTable.getArray("ask")) : null);

                safety.setPermissions(permissions);
            }

            // [safety.sandbox]
            if (safetyTable.contains("sandbox")) {
                TomlTable sandboxTable = safetyTable.getTable("sandbox");
                SyncConfig.SandboxConfig sandbox = new SyncConfig.SandboxConfig();

                sandbox.setFailIfUnavailable(sandboxTable.getBoolean("fail_if_unavailable"));

                if (sandboxTable.contains("network")) {
                    TomlTable networkTable = sandboxTable.getTable("network");
                    SyncConfig.NetworkConfig network = new SyncConfig.NetworkConfig();
                    network.setDeniedDomains(networkTable.contains("denied_domains") ? toList(networkTable.getArray("denied_domains")) : null);
                    sandbox.setNetwork(network);
                }

                if (sandboxTable.contains("filesystem")) {
                    TomlTable fsTable = sandboxTable.getTable("filesystem");
                    SyncConfig.FilesystemConfig filesystem = new SyncConfig.FilesystemConfig();
                    filesystem.setDenyRead(fsTable.contains("deny_read") ? toList(fsTable.getArray("deny_read")) : null);
                    filesystem.setAllowRead(fsTable.contains("allow_read") ? toList(fsTable.getArray("allow_read")) : null);
                    sandbox.setFilesystem(filesystem);
                }

                safety.setSandbox(sandbox);
            }

            config.setSafety(safety);
        }

        return config;
    }

    /**
     * 将 TomlArray 转换为 String 列表
     */
    private static List<String> toList(TomlArray array) {
        if (array == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Object element = array.get(i);
            if (element != null) {
                result.add(element.toString());
            }
        }
        return result;
    }

    /**
     * 将 TomlTable 转换为 String->String 的 Map
     */
    private static Map<String, String> toStringMap(TomlTable table) {
        if (table == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : table.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
    }
}
