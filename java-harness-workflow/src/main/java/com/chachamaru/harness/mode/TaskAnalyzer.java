package com.chachamaru.harness.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 任务特征分析器
 * 分析任务列表和变更文件，提取用于执行模式推荐的特征信息
 */
public class TaskAnalyzer {

    // 核心目录关键词（增加复杂度）
    private static final Set<String> CORE_DIRECTORIES = Set.of(
        "core/", "src/core/", "src/main/core/"
    );

    // 安全相关目录关键词（增加复杂度 + 需要审查）
    private static final Set<String> SECURITY_DIRECTORIES = Set.of(
        "security/", "src/security/", "guardrails/", "src/guardrails/"
    );

    // 高风险关键字（增加复杂度 + 需要审查）
    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
        "architecture", "security", "design", "migration"
    );

    // 超高风险关键字（直接导致最高复杂度）
    private static final Set<String> ULTRA_HIGH_RISK_KEYWORDS = Set.of(
        "architecture", "migration", "refactor", "database", "schema"
    );

    // 依赖关系指示关键词
    private static final Set<String> SEQUENTIAL_DEPENDENCY_KEYWORDS = Set.of(
        "then", "after", "depends on", "follow", "subsequent", "next"
    );

    // 审查需求关键词
    private static final Set<String> REVIEW_REQUIRED_KEYWORDS = Set.of(
        "core/", "security/", "guardrails/", "payment", "auth", "database"
    );

    // 无需审查关键词
    private static final Set<String> NO_REVIEW_KEYWORDS = Set.of(
        "docs/", "documentation", "readme", "comment", "typo", "format"
    );

    /**
     * 分析任务特征
     *
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @param hasFailureHistory 是否有失败历史
     * @param explicitEffort 显式指定的 effort 等级 ("low", "medium", "high", "xhigh")
     * @return 任务特征对象
     */
    public TaskCharacteristics analyzeTask(List<String> tasks,
                                          List<String> files,
                                          boolean hasFailureHistory,
                                          String explicitEffort) {

        // 处理空输入
        List<String> safeTasks = tasks != null ? tasks : new ArrayList<>();
        List<String> safeFiles = files != null ? files : new ArrayList<>();

        int taskCount = safeTasks.size();

        // 计算复杂度分数
        int complexityScore = calculateComplexityScore(safeTasks, safeFiles, hasFailureHistory, explicitEffort);
        ComplexityLevel complexity = determineComplexityLevel(complexityScore, explicitEffort);

        // 判断依赖关系
        DependencyType dependencies = determineDependencyType(safeTasks, safeFiles);

        // 判断审查需求
        ReviewRequirement reviewNeed = determineReviewRequirement(safeTasks, safeFiles, complexity);

        return new TaskCharacteristics(taskCount, complexity, dependencies, reviewNeed);
    }

    /**
     * 计算复杂度分数
     * 分数越高表示任务越复杂
     */
    private int calculateComplexityScore(List<String> tasks, List<String> files,
                                       boolean hasFailureHistory, String explicitEffort) {
        int score = 0;

        // 显式指定优先
        if (explicitEffort != null) {
            switch (explicitEffort.toLowerCase()) {
                case "xhigh":
                    return 10; // 直接返回最高分
                case "high":
                    return 7;  // 直接返回高复杂度分数
                case "medium":
                    return 3;  // 中等复杂度分数
                case "low":
                    return 0;  // 低复杂度分数
            }
        }

        // 任务数量评分 (多任务 = 更高复杂度)
        if (tasks.size() >= 4) {
            score += 2;
        } else if (tasks.size() >= 2) {
            score += 1;
        }

        // 文件数评分 (多文件 = 更高复杂度)
        if (files.size() >= 5) {
            score += 2;
        } else if (files.size() >= 3) {
            score += 1;
        }

        // 目录评分 (核心目录 +2, 安全目录 +2，独立于文件数量)
        boolean hasCoreDirectory = false;
        boolean hasSecurityDirectory = false;

        for (String file : files) {
            String lowerFile = file.toLowerCase();
            for (String coreDir : CORE_DIRECTORIES) {
                if (lowerFile.contains(coreDir)) {
                    hasCoreDirectory = true;
                    break;
                }
            }
            for (String securityDir : SECURITY_DIRECTORIES) {
                if (lowerFile.contains(securityDir)) {
                    hasSecurityDirectory = true;
                    break;
                }
            }
        }

        if (hasCoreDirectory) {
            score += 3; // 核心目录额外加分（提高权重）
        }
        if (hasSecurityDirectory) {
            score += 3; // 安全目录额外加分（提高权重）
        }

        // 关键字评分 (在任务描述和文件路径中都检查)
        Set<String> allTexts = new HashSet<>();
        for (String task : tasks) {
            allTexts.add(task.toLowerCase());
        }
        for (String file : files) {
            allTexts.add(file.toLowerCase());
        }

        boolean hasHighRiskKeywords = false;
        boolean hasUltraHighRiskKeywords = false;

        for (String text : allTexts) {
            for (String keyword : HIGH_RISK_KEYWORDS) {
                if (text.contains(keyword)) {
                    hasHighRiskKeywords = true;
                    break;
                }
            }
            for (String keyword : ULTRA_HIGH_RISK_KEYWORDS) {
                if (text.contains(keyword)) {
                    hasUltraHighRiskKeywords = true;
                    break;
                }
            }
        }

        // 超高风险关键字特殊处理（直接给予极高复杂度）
        boolean hasArchitectureTask = false;
        boolean hasMigrationTask = false;

        for (String text : allTexts) {
            if (text.contains("architecture") || text.contains("system design")) {
                hasArchitectureTask = true;
            }
            if (text.contains("migration") || text.contains("data migration") || text.contains("schema")) {
                hasMigrationTask = true;
            }
        }

        if (hasArchitectureTask || hasMigrationTask) {
            score += 8; // 架构和迁移任务直接给予极高分数
        } else if (hasUltraHighRiskKeywords) {
            score += 5; // 其他超高风险关键字大幅加分
        } else if (hasHighRiskKeywords) {
            score += 2; // 高风险关键字额外加分
        }

        // 失败历史评分 (+3，提高权重)
        if (hasFailureHistory) {
            score += 3;
        }

        // 保证最低分数为0
        return Math.max(0, Math.min(score, 10)); // 限制最高分为10
    }

    /**
     * 根据分数确定复杂度等级
     */
    private ComplexityLevel determineComplexityLevel(int score, String explicitEffort) {
        // 显式指定优先
        if (explicitEffort != null) {
            switch (explicitEffort.toLowerCase()) {
                case "xhigh":
                    return ComplexityLevel.VERY_COMPLEX;
                case "high":
                    return ComplexityLevel.COMPLEX;
                case "medium":
                    return ComplexityLevel.MODERATE;
                case "low":
                    return ComplexityLevel.SIMPLE;
            }
        }

        // 基于分数的映射（进一步调整阈值）
        if (score >= 7) {
            return ComplexityLevel.VERY_COMPLEX;
        } else if (score >= 3) {
            return ComplexityLevel.COMPLEX;
        } else if (score >= 1) {
            return ComplexityLevel.MODERATE;
        } else {
            return ComplexityLevel.SIMPLE;
        }
    }

    /**
     * 判断任务依赖关系类型
     */
    private DependencyType determineDependencyType(List<String> tasks, List<String> files) {
        if (tasks.size() <= 1) {
            return DependencyType.INDEPENDENT;
        }

        boolean hasSequentialIndicators = false;
        boolean hasDifferentModules = false;
        Set<String> modules = new HashSet<>();

        // 检查任务描述中的依赖指示词
        for (String task : tasks) {
            String lowerTask = task.toLowerCase();
            for (String indicator : SEQUENTIAL_DEPENDENCY_KEYWORDS) {
                if (lowerTask.contains(indicator)) {
                    hasSequentialIndicators = true;
                    break;
                }
            }
        }

        // 检查文件路径的模块分布和层次关系
        Set<String> directories = new HashSet<>();
        for (String file : files) {
            String dir = getDirectoryPath(file);
            if (!dir.isEmpty()) {
                directories.add(dir);
                // 提取模块名（第一级目录）
                String module = extractModuleName(dir);
                if (module != null && !module.isEmpty()) {
                    modules.add(module);
                }
            }
        }

        hasDifferentModules = modules.size() > 1;

        // 判断依赖类型
        if (hasSequentialIndicators) {
            // 明确的顺序依赖指示词
            return DependencyType.SEQUENTIAL;
        } else if (hasDifferentModules && tasks.size() >= 3) {
            // 多个任务涉及多个不同模块，判断为混合依赖
            return DependencyType.MIXED;
        } else if (!hasDifferentModules && tasks.size() == 2) {
            // 两个任务在同一模块，判断为独立
            return DependencyType.INDEPENDENT;
        } else if (tasks.size() == 2) {
            // 两个任务没有明显的顺序依赖，判断为独立
            return DependencyType.INDEPENDENT;
        } else if (hasDifferentModules && tasks.size() == 2) {
            // 两个任务涉及不同模块，可能是独立的
            return DependencyType.INDEPENDENT;
        } else {
            // 多个任务的默认情况
            return DependencyType.MIXED;
        }
    }

    /**
     * 从目录路径中提取模块名
     */
    private String extractModuleName(String dirPath) {
        String[] parts = dirPath.split("/");
        if (parts.length >= 1) {
            // 返回第一级非空目录名
            for (String part : parts) {
                if (!part.isEmpty()) {
                    return part;
                }
            }
        }
        return dirPath;
    }

    /**
     * 判断代码审查需求
     */
    private ReviewRequirement determineReviewRequirement(List<String> tasks, List<String> files,
                                                        ComplexityLevel complexity) {
        // 检查是否包含必须审查的关键字
        for (String file : files) {
            String lowerFile = file.toLowerCase();
            for (String keyword : REVIEW_REQUIRED_KEYWORDS) {
                if (lowerFile.contains(keyword)) {
                    return ReviewRequirement.REQUIRED;
                }
            }
        }

        // 检查是否必须审查的关键字在任务描述中
        for (String task : tasks) {
            String lowerTask = task.toLowerCase();
            // 安全相关关键字需要审查
            if (lowerTask.contains("security") || lowerTask.contains("auth") ||
                lowerTask.contains("payment") || lowerTask.contains("database")) {
                return ReviewRequirement.REQUIRED;
            }
        }

        // 检查是否包含文档类型的关键字
        boolean isDocumentationTask = true;
        for (String task : tasks) {
            String lowerTask = task.toLowerCase();
            boolean isDocKeyword = false;
            for (String keyword : NO_REVIEW_KEYWORDS) {
                if (lowerTask.contains(keyword)) {
                    isDocKeyword = true;
                    break;
                }
            }
            if (!isDocKeyword) {
                isDocumentationTask = false;
                break;
            }
        }

        // 检查文件是否都是文档文件
        boolean allDocFiles = true;
        for (String file : files) {
            String lowerFile = file.toLowerCase();
            if (!lowerFile.startsWith("docs/") &&
                !lowerFile.endsWith(".md") &&
                !lowerFile.endsWith(".txt")) {
                allDocFiles = false;
                break;
            }
        }

        // 如果是纯文档任务，不需要审查
        if (isDocumentationTask && allDocFiles) {
            return ReviewRequirement.NONE;
        }

        // 根据复杂度确定审查需求
        if (complexity == ComplexityLevel.VERY_COMPLEX) {
            return ReviewRequirement.REQUIRED;
        } else if (complexity == ComplexityLevel.COMPLEX) {
            return ReviewRequirement.REQUIRED;
        } else if (complexity == ComplexityLevel.MODERATE) {
            // 中等复杂度任务建议审查
            return ReviewRequirement.OPTIONAL;
        } else {
            // 简单任务通常可选审查，除非是纯文档
            if (allDocFiles && tasks.size() == 1) {
                return ReviewRequirement.NONE;
            }
            return ReviewRequirement.OPTIONAL;
        }
    }

    /**
     * 获取文件所在目录路径
     */
    private String getDirectoryPath(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash > 0) {
            return filePath.substring(0, lastSlash);
        }
        return "";
    }
}