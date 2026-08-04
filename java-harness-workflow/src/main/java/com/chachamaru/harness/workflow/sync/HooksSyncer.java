package com.chachamaru.harness.workflow.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * hooks.json 同步器
 *
 * <p>负责将 hooks/hooks.json 复制到 .claude-plugin/hooks.json
 *
 * @see SyncSkill
 * @since 4.0.0-java
 */
public class HooksSyncer {

    /**
     * 复制 hooks/hooks.json 到 .claude-plugin/hooks.json
     *
     * @param projectRoot 项目根目录
     * @return 目标文件路径
     * @throws IOException 如果源文件不存在或复制失败
     */
    public static String sync(File projectRoot) throws IOException {
        Path sourcePath = projectRoot.toPath().resolve("hooks").resolve("hooks.json");
        Path targetDir = projectRoot.toPath().resolve(".claude-plugin");
        Path targetPath = targetDir.resolve("hooks.json");

        // 检查源文件
        if (!Files.exists(sourcePath)) {
            throw new IOException("Source hooks.json not found: " + sourcePath);
        }

        // 读取并验证 JSON（简单检查格式）
        String content = Files.readString(sourcePath);
        if (content.trim().isEmpty() || !content.trim().startsWith("{")) {
            throw new IOException("Invalid JSON in source hooks.json");
        }

        // 创建目标目录并复制文件
        Files.createDirectories(targetDir);
        Files.writeString(targetPath, content);

        return targetPath.toString();
    }
}
