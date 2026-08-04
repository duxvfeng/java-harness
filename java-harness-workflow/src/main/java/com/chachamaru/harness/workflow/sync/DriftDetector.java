package com.chachamaru.harness.workflow.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置漂移检测器
 *
 * <p>检测 settings.json 配置漂移，比较现有文件与新内容
 * <p>主要检测：
 * <ul>
 *   <li>文件内容是否变化</li>
 *   <li>deniedDomains 数量变化（关键指标）</li>
 *   <li>生成详细的警告信息</li>
 * </ul>
 *
 * @see SyncSkill
 * @since 4.0.0-java
 */
public class DriftDetector {

    /**
     * 检测 settings.json 配置漂移
     *
     * @param projectRoot 项目根目录
     * @param newContent 新的文件内容
     * @return 警告列表，如果无漂移则返回空列表
     * @throws IOException 如果读取失败
     */
    public static List<String> check(File projectRoot, byte[] newContent) throws IOException {
        List<String> warnings = new ArrayList<>();

        Path settingsPath = projectRoot.toPath().resolve(".claude-plugin").resolve("settings.json");

        // 文件不存在 = 首次生成，无漂移
        if (!Files.exists(settingsPath)) {
            return warnings;
        }

        // 读取现有内容
        byte[] existingContent = Files.readAllBytes(settingsPath);

        // 去除空白后比较
        String existingTrimmed = new String(existingContent).trim();
        String newTrimmed = new String(newContent).trim();

        if (existingTrimmed.equals(newTrimmed)) {
            return warnings; // 无漂移
        }

        // 检测 deniedDomains 数量变化（关键指标）
        int existingDeniedCount = extractDeniedDomainCount(existingContent);
        int newDeniedCount = extractDeniedDomainCount(newContent);

        if (existingDeniedCount >= 0 && newDeniedCount >= 0 && existingDeniedCount != newDeniedCount) {
            StringBuilder warning = new StringBuilder();
            warning.append(".claude-plugin/settings.json drift detected — sync rewrote the file.\n");
            warning.append(String.format("  sandbox.network.deniedDomains: %d -> %d entries\n",
                existingDeniedCount, newDeniedCount));

            if (existingDeniedCount > newDeniedCount) {
                warning.append("  entries were REMOVED — was settings.json edited directly without updating harness.toml?\n");
                warning.append("  SSOT is harness.toml. Mirror the change there and re-run sync.");
            }

            warnings.add(warning.toString());
        } else {
            warnings.add(".claude-plugin/settings.json drift detected — sync rewrote the file.\n" +
                "  Review with: git diff .claude-plugin/settings.json");
        }

        return warnings;
    }

    /**
     * 提取 deniedDomains 数量
     *
     * @param content JSON 内容
     * @return deniedDomains 数量，如果无法解析则返回 -1
     */
    private static int extractDeniedDomainCount(byte[] content) {
        try {
            String json = new String(content);
            int deniedIndex = json.indexOf("\"deniedDomains\"");
            if (deniedIndex < 0) {
                return -1;
            }

            int bracketStart = json.indexOf("[", deniedIndex);
            if (bracketStart < 0) {
                return -1;
            }

            int bracketEnd = json.indexOf("]", bracketStart);
            if (bracketEnd < 0) {
                return -1;
            }

            // 简单计算逗号数量 + 1 = 条目数量
            String segment = json.substring(bracketStart, bracketEnd);
            if (segment.trim().isEmpty()) {
                return 0;
            }

            int commaCount = 0;
            for (char c : segment.toCharArray()) {
                if (c == ',') {
                    commaCount++;
                }
            }
            return commaCount + 1;

        } catch (Exception e) {
            return -1; // 解析失败
        }
    }
}

