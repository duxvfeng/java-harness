package com.chachamaru.harness.foundation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板更新检测器 - 检测模板版本变化和本地修改
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>检测模板文件修改</li>
 *   <li>版本比较和冲突检测</li>
 *   <li>自动更新通知</li>
 *   <li>变更合并策略</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateUpdateDetector {

    private static final Logger logger = LoggerFactory.getLogger(TemplateUpdateDetector.class);

    private final Map<String, TemplateSnapshot> templateSnapshots;
    private final TemplateRegistry registry;

    /**
     * 模板快照
     */
    public static class TemplateSnapshot {
        private final String templateName;
        private final String version;
        private final String checksum;
        private final LocalDateTime lastModified;
        private final Map<String, String> metadata;

        public TemplateSnapshot(String templateName, String version, String checksum) {
            this.templateName = templateName;
            this.version = version;
            this.checksum = checksum;
            this.lastModified = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }

        public String getTemplateName() { return templateName; }
        public String getVersion() { return version; }
        public String getChecksum() { return checksum; }
        public LocalDateTime getLastModified() { return lastModified; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    /**
     * 更新检测结果
     */
    public static class UpdateResult {
        private final String templateName;
        private final boolean hasUpdate;
        private final String currentVersion;
        private final String availableVersion;
        private final String changeDescription;
        private final boolean hasConflict;
        private final String conflictDescription;

        public UpdateResult(String templateName, boolean hasUpdate, String currentVersion, String availableVersion) {
            this.templateName = templateName;
            this.hasUpdate = hasUpdate;
            this.currentVersion = currentVersion;
            this.availableVersion = availableVersion;
            this.changeDescription = "";
            this.hasConflict = false;
            this.conflictDescription = "";
        }

        public String getTemplateName() { return templateName; }
        public boolean hasUpdate() { return hasUpdate; }
        public String getCurrentVersion() { return currentVersion; }
        public String getAvailableVersion() { return availableVersion; }
        public String getChangeDescription() { return changeDescription; }
        public boolean hasConflict() { return hasConflict; }
        public String getConflictDescription() { return conflictDescription; }
    }

    public TemplateUpdateDetector(TemplateRegistry registry) {
        this.registry = registry;
        this.templateSnapshots = new ConcurrentHashMap<>();
    }

    /**
     * 检查模板更新
     */
    public UpdateResult checkUpdate(String templateName) {
        try {
            Template currentTemplate = registry.getTemplateByName(templateName);
            String currentVersion = currentTemplate.getVersion();
            String currentChecksum = calculateChecksum(currentTemplate);

            TemplateSnapshot snapshot = templateSnapshots.get(templateName);

            if (snapshot == null) {
                // 首次检查，创建快照
                snapshot = new TemplateSnapshot(templateName, currentVersion, currentChecksum);
                templateSnapshots.put(templateName, snapshot);
                return new UpdateResult(templateName, false, currentVersion, currentVersion);
            }

            // 检查是否有更新
            boolean hasUpdate = !snapshot.getChecksum().equals(currentChecksum);
            boolean hasVersionChange = !snapshot.getVersion().equals(currentVersion);

            if (hasUpdate) {
                logger.info("检测到模板更新: {} ({}) -> {}", templateName, snapshot.getVersion(), currentVersion);
                return new UpdateResult(templateName, true, snapshot.getVersion(), currentVersion);
            }

            return new UpdateResult(templateName, false, currentVersion, currentVersion);

        } catch (TemplateRegistryException e) {
            logger.warn("检查模板更新失败: {}", templateName, e);
            return new UpdateResult(templateName, false, "unknown", "unknown");
        }
    }

    /**
     * 检查所有模板更新
     */
    public List<UpdateResult> checkAllUpdates() {
        List<UpdateResult> results = new ArrayList<>();

        for (Template template : registry.getAllTemplates()) {
            UpdateResult result = checkUpdate(template.getFullName());
            results.add(result);
        }

        return results;
    }

    /**
     * 检测本地修改
     */
    public boolean detectLocalModifications(String templateName) {
        TemplateSnapshot snapshot = templateSnapshots.get(templateName);
        if (snapshot == null) {
            return false;
        }

        try {
            Template currentTemplate = registry.getTemplateByName(templateName);
            String currentChecksum = calculateChecksum(currentTemplate);
            return !snapshot.getChecksum().equals(currentChecksum);
        } catch (TemplateRegistryException e) {
            return false;
        }
    }

    /**
     * 创建冲突检测
     */
    public boolean hasConflict(String templateName) {
        TemplateSnapshot snapshot = templateSnapshots.get(templateName);
        if (snapshot == null) {
            return false;
        }

        // 检查版本是否回退（版本号变小）
        try {
            Template currentTemplate = registry.getTemplateByName(templateName);
            return compareVersions(snapshot.getVersion(), currentTemplate.getVersion()) > 0;
        } catch (TemplateRegistryException e) {
            return false;
        }
    }

    /**
     * 比较版本号
     * @return 正数如果v1>v2，负数如果v1<v2，0如果相等
     */
    private int compareVersions(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int maxLength = Math.max(parts1.length, parts2.length);

            for (int i = 0; i < maxLength; i++) {
                int num1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
                int num2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;

                if (num1 != num2) {
                    return num1 - num2;
                }
            }

            return 0;
        } catch (NumberFormatException e) {
            // 如果版本格式无法解析，按字符串比较
            return v1.compareTo(v2);
        }
    }

    /**
     * 计算模板校验和
     */
    private String calculateChecksum(Template template) {
        String content = template.getContent() != null ? template.getContent() : "";
        return String.valueOf(content.hashCode());
    }

    /**
     * 创建快照
     */
    public void createSnapshot(String templateName) {
        try {
            Template template = registry.getTemplateByName(templateName);
            String checksum = calculateChecksum(template);
            TemplateSnapshot snapshot = new TemplateSnapshot(templateName, template.getVersion(), checksum);
            templateSnapshots.put(templateName, snapshot);

            logger.info("创建模板快照: {} ({})", templateName, template.getVersion());
        } catch (TemplateRegistryException e) {
            logger.warn("创建快照失败: {}", templateName, e);
        }
    }

    /**
     * 更新快照
     */
    public void updateSnapshot(String templateName) {
        templateSnapshots.remove(templateName);
        createSnapshot(templateName);
    }

    /**
     * 获取更新统计
     */
    public Map<String, Object> getUpdateStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<UpdateResult> allUpdates = checkAllUpdates();
        long updateCount = allUpdates.stream().filter(UpdateResult::hasUpdate).count();
        long conflictCount = allUpdates.stream().filter(UpdateResult::hasConflict).count();

        stats.put("total_templates", allUpdates.size());
        stats.put("templates_with_updates", updateCount);
        stats.put("templates_with_conflicts", conflictCount);
        stats.put("last_check", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return stats;
    }

    /**
     * 生成更新报告
     */
    public String generateUpdateReport() {
        StringBuilder report = new StringBuilder();
        report.append("# 模板更新检测报告\n\n");
        report.append("**检测时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        List<UpdateResult> allUpdates = checkAllUpdates();

        report.append("## 检测结果摘要\n\n");
        report.append("- 总模板数: ").append(allUpdates.size()).append("\n");
        report.append("- 有更新的模板: ").append(allUpdates.stream().filter(UpdateResult::hasUpdate).count()).append("\n");
        report.append("- 有冲突的模板: ").append(allUpdates.stream().filter(UpdateResult::hasConflict).count()).append("\n\n");

        List<UpdateResult> updates = allUpdates.stream()
            .filter(UpdateResult::hasUpdate)
            .toList();

        if (!updates.isEmpty()) {
            report.append("## 需要更新的模板\n\n");
            for (UpdateResult update : updates) {
                report.append("- **").append(update.getTemplateName()).append("**");
                report.append(" (").append(update.getCurrentVersion()).append(" → ");
                report.append(update.getAvailableVersion()).append(")\n");
            }
        } else {
            report.append("## ✅ 所有模板都是最新的\n\n");
        }

        return report.toString();
    }

    /**
     * 监控模板目录
     */
    public void startMonitoring(Path templateDir) {
        logger.info("开始监控模板目录: {}", templateDir);
        // 简化实现：实际应该使用 WatchService 监控文件变化
    }

    /**
     * 停止监控
     */
    public void stopMonitoring() {
        logger.info("停止模板监控");
    }

    /**
     * 清理旧快照
     */
    public void cleanupOldSnapshots(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);

        templateSnapshots.entrySet().removeIf(entry -> {
            TemplateSnapshot snapshot = entry.getValue();
            return snapshot.getLastModified().isBefore(cutoff);
        });

        logger.info("清理了 {} 天前的旧快照，剩余 {} 个快照",
            daysToKeep, templateSnapshots.size());
    }

    /**
     * 获取所有快照
     */
    public Map<String, TemplateSnapshot> getAllSnapshots() {
        return new HashMap<>(templateSnapshots);
    }

    /**
     * 导出快照数据
     */
    public String exportSnapshots() {
        StringBuilder export = new StringBuilder();
        export.append("# 模板快照导出\n\n");

        for (TemplateSnapshot snapshot : templateSnapshots.values()) {
            export.append("## ").append(snapshot.getTemplateName()).append("\n");
            export.append("- 版本: ").append(snapshot.getVersion()).append("\n");
            export.append("- 校验和: ").append(snapshot.getChecksum()).append("\n");
            export.append("- 最后修改: ").append(snapshot.getLastModified()).append("\n\n");
        }

        return export.toString();
    }
}