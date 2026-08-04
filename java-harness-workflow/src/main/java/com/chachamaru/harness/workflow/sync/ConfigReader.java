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
            // 支持 camelCase 和 snake_case 两种格式
            if (projectTable.contains("author_name")) {
                project.setAuthorName(projectTable.getString("author_name"));
            } else if (projectTable.contains("authorName")) {
                project.setAuthorName(projectTable.getString("authorName"));
            }
            if (projectTable.contains("author_url")) {
                project.setAuthorUrl(projectTable.getString("author_url"));
            } else if (projectTable.contains("authorUrl")) {
                project.setAuthorUrl(projectTable.getString("authorUrl"));
            }
            project.setHomepage(projectTable.contains("homepage") ? projectTable.getString("homepage") : null);
            project.setRepository(projectTable.contains("repository") ? projectTable.getString("repository") : null);
            project.setLicense(projectTable.contains("license") ? projectTable.getString("license") : null);
            project.setKeywords(projectTable.contains("keywords") ? toList(projectTable.getArray("keywords")) : null);
            // 支持 camelCase 和 snake_case 两种格式
            if (projectTable.contains("output_styles")) {
                project.setOutputStyles(toList(projectTable.getArray("output_styles")));
            } else if (projectTable.contains("outputStyles")) {
                project.setOutputStyles(toList(projectTable.getArray("outputStyles")));
            }

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

                // 支持 camelCase 和 snake_case 两种格式
                Boolean failIfUnavailable = sandboxTable.getBoolean("fail_if_unavailable");
                if (failIfUnavailable == null) {
                    failIfUnavailable = sandboxTable.getBoolean("failIfUnavailable");
                }
                sandbox.setFailIfUnavailable(failIfUnavailable != null && failIfUnavailable);

                if (sandboxTable.contains("network")) {
                    TomlTable networkTable = sandboxTable.getTable("network");
                    SyncConfig.NetworkConfig network = new SyncConfig.NetworkConfig();
                    // 支持 camelCase 和 snake_case 两种格式
                    if (networkTable.contains("denied_domains")) {
                        network.setDeniedDomains(toList(networkTable.getArray("denied_domains")));
                    } else if (networkTable.contains("deniedDomains")) {
                        network.setDeniedDomains(toList(networkTable.getArray("deniedDomains")));
                    }
                    sandbox.setNetwork(network);
                }

                if (sandboxTable.contains("filesystem")) {
                    TomlTable fsTable = sandboxTable.getTable("filesystem");
                    SyncConfig.FilesystemConfig filesystem = new SyncConfig.FilesystemConfig();
                    // 支持 camelCase 和 snake_case 两种格式
                    if (fsTable.contains("deny_read")) {
                        filesystem.setDenyRead(toList(fsTable.getArray("deny_read")));
                    } else if (fsTable.contains("denyRead")) {
                        filesystem.setDenyRead(toList(fsTable.getArray("denyRead")));
                    }
                    if (fsTable.contains("allow_read")) {
                        filesystem.setAllowRead(toList(fsTable.getArray("allow_read")));
                    } else if (fsTable.contains("allowRead")) {
                        filesystem.setAllowRead(toList(fsTable.getArray("allowRead")));
                    }
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
