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
 * 模板追踪器 - 跟踪模板的使用情况和变更历史
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>记录模板使用情况</li>
 *   <li>监控模板文件变更</li>
 *   <li>生成使用统计</li>
 *   <li>版本历史追踪</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateTracker {

    private static final Logger logger = LoggerFactory.getLogger(TemplateTracker.class);

    private final Map<String, TemplateUsageRecord> usageRecords;
    private final Map<String, List<TemplateChangeEvent>> changeEvents;
    private final Path trackingFile;

    /**
     * 模板使用记录
     */
    public static class TemplateUsageRecord {
        private final String templateName;
        private final String templateVersion;
        private int usageCount;
        private LocalDateTime lastUsed;
        private final Set<String> usedByProjects;

        public TemplateUsageRecord(String templateName, String templateVersion) {
            this.templateName = templateName;
            this.templateVersion = templateVersion;
            this.usageCount = 0;
            this.lastUsed = LocalDateTime.now();
            this.usedByProjects = new HashSet<>();
        }

        public void recordUsage(String project) {
            this.usageCount++;
            this.lastUsed = LocalDateTime.now();
            this.usedByProjects.add(project);
        }

        public String getTemplateName() { return templateName; }
        public String getTemplateVersion() { return templateVersion; }
        public int getUsageCount() { return usageCount; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public Set<String> getUsedByProjects() { return usedByProjects; }
    }

    /**
     * 模板变更事件
     */
    public static class TemplateChangeEvent {
        private final String templateName;
        private final LocalDateTime timestamp;
        private final String eventType; // CREATED, MODIFIED, DELETED
        private final String version;
        private final String changeDescription;

        public TemplateChangeEvent(String templateName, String eventType, String version, String changeDescription) {
            this.templateName = templateName;
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
            this.version = version;
            this.changeDescription = changeDescription;
        }

        public String getTemplateName() { return templateName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getVersion() { return version; }
        public String getChangeDescription() { return changeDescription; }
    }

    /**
     * 构造函数
     */
    public TemplateTracker(Path trackingFile) {
        this.trackingFile = trackingFile;
        this.usageRecords = new ConcurrentHashMap<>();
        this.changeEvents = new ConcurrentHashMap<>();

        // 加载历史追踪数据
        loadTrackingData();
    }

    /**
     * 记录模板使用
     */
    public void recordUsage(String templateName, String templateVersion, String project) {
        String key = templateName + ":" + templateVersion;
        TemplateUsageRecord record = usageRecords.computeIfAbsent(
            key,
            k -> new TemplateUsageRecord(templateName, templateVersion)
        );
        record.recordUsage(project);

        logger.debug("记录模板使用: {} {} by {}", templateName, templateVersion, project);
    }

    /**
     * 记录模板变更事件
     */
    public void recordChange(String templateName, String eventType, String version, String description) {
        List<TemplateChangeEvent> events = changeEvents.computeIfAbsent(templateName, k -> new ArrayList<>());
        TemplateChangeEvent event = new TemplateChangeEvent(templateName, eventType, version, description);
        events.add(event);

        logger.info("记录模板变更: {} {} - {}", templateName, eventType, description);
    }

    /**
     * 获取模板使用统计
     */
    public TemplateUsageRecord getUsageStats(String templateName, String version) {
        String key = templateName + ":" + version;
        return usageRecords.get(key);
    }

    /**
     * 获取最常用的模板
     */
    public List<TemplateUsageRecord> getMostUsedTemplates(int limit) {
        return usageRecords.values().stream()
            .sorted(Comparator.comparingInt(TemplateUsageRecord::getUsageCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 获取模板变更历史
     */
    public List<TemplateChangeEvent> getChangeHistory(String templateName) {
        return changeEvents.getOrDefault(templateName, new ArrayList<>());
    }

    /**
     * 获取所有模板的变更历史
     */
    public Map<String, List<TemplateChangeEvent>> getAllChangeHistory() {
        return new HashMap<>(changeEvents);
    }

    /**
     * 生成使用统计报告
     */
    public String generateUsageReport() {
        StringBuilder report = new StringBuilder();
        report.append("# 模板使用统计报告\n\n");
        report.append("**生成时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        report.append("## 总体统计\n\n");
        report.append("- 总模板使用次数: ").append(getTotalUsageCount()).append("\n");
        report.append("- 活跃模板数量: ").append(usageRecords.size()).append("\n");
        report.append("- 变更事件数量: ").append(getTotalChangeCount()).append("\n\n");

        report.append("## 最常用模板 (TOP 10)\n\n");
        List<TemplateUsageRecord> mostUsed = getMostUsedTemplates(10);
        for (int i = 0; i < mostUsed.size(); i++) {
            TemplateUsageRecord record = mostUsed.get(i);
            report.append(i + 1).append(". ").append(record.getTemplateName())
                  .append(" (").append(record.getTemplateVersion()).append(")")
                  .append(" - 使用次数: ").append(record.getUsageCount())
                  .append(", 最后使用: ").append(record.getLastUsed().format(DateTimeFormatter.ISO_LOCAL_DATE))
                  .append("\n");
        }

        return report.toString();
    }

    /**
     * 获取总使用次数
     */
    private int getTotalUsageCount() {
        return usageRecords.values().stream()
            .mapToInt(TemplateUsageRecord::getUsageCount)
            .sum();
    }

    /**
     * 获取总变更次数
     */
    private int getTotalChangeCount() {
        return changeEvents.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * 加载追踪数据
     */
    private void loadTrackingData() {
        if (!Files.exists(trackingFile)) {
            logger.info("追踪文件不存在，将创建新的追踪记录: {}", trackingFile);
            return;
        }

        try {
            // 简化实现：只记录日志，实际应该从文件加载
            logger.info("加载追踪数据: {}", trackingFile);
        } catch (Exception e) {
            logger.error("加载追踪数据失败: {}", e.getMessage());
        }
    }

    /**
     * 保存追踪数据
     */
    public void saveTrackingData() {
        try {
            Files.createDirectories(trackingFile.getParent());
            // 简化实现：只记录日志
            logger.info("保存追踪数据: {} ({} 条使用记录, {} 条变更事件)",
                trackingFile, usageRecords.size(), changeEvents.size());
        } catch (IOException e) {
            logger.error("保存追踪数据失败: {}", e.getMessage());
        }
    }

    /**
     * 监控模板文件变更
     */
    public void startMonitoring() {
        logger.info("开始监控模板文件变更");
        // 简化实现：实际应该使用 WatchService
    }

    /**
     * 停止监控
     */
    public void stopMonitoring() {
        logger.info("停止监控模板文件变更");
        saveTrackingData();
    }

    /**
     * 清理旧记录
     */
    public void cleanupOldRecords(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);

        usageRecords.entrySet().removeIf(entry -> {
            TemplateUsageRecord record = entry.getValue();
            return record.getLastUsed().isBefore(cutoff);
        });

        logger.info("清理了 {} 天前的旧记录，剩余 {} 条记录",
            daysToKeep, usageRecords.size());
    }

    /**
     * 获取追踪统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_usage_count", getTotalUsageCount());
        stats.put("total_templates", usageRecords.size());
        stats.put("total_changes", getTotalChangeCount());
        stats.put("tracking_file", trackingFile.toString());
        stats.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return stats;
    }
}