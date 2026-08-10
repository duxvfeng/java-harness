package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.model.*;

/**
 * Effort 路由器
 * 根据任务复杂度自动选择最优的 Effort 等级和 AI 模型
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>复杂度评分：基于文件数、目录数、关键字、失败历史</li>
 *   <li>Effort tier 决定：根据复杂度分数选择 effort 等级</li>
 *   <li>智能模型选择：集成 SmartModelSelector 选择最优模型</li>
 *   <li>Worker 配置生成：生成完整的 WorkerSpawnConfig</li>
 * </ul>
 *
 * <p>复杂度评分规则：</p>
 * <ul>
 *   <li>文件数：变更对象 4 个文件以上 (+1)</li>
 *   <li>目录：包含 core/、guardrails/、security/ (+1)</li>
 *   <li>关键字：包含 architecture、security、design、migration (+1)</li>
 *   <li>失败历史：有同任务的失败记录 (+2)</li>
 *   <li>显式指定：PM 模板中记载 `effort: high` / `effort: xhigh` (+3)</li>
 * </ul>
 *
 * <p>Effort tier 映射：</p>
 * <ul>
 *   <li>0-2 分：medium（Worker frontmatter 默认）</li>
 *   <li>≥3 分（无 code-risk）：high</li>
 *   <li>≥3 分（有 code-risk）：xhigh</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * EffortRouter router = new EffortRouter();
 *
 * TaskContext context = new TaskContext(5, 2, true, false);
 * WorkerSpawnConfig config = router.determineWorkerConfig(context);
 *
 * System.out.println("Effort Tier: " + config.getEffortTier());
 * System.out.println("Selected Model: " + config.getSelectedModel());
 * }</pre>
 */
public class EffortRouter {

    private final SmartModelSelector modelSelector;

    /**
     * 创建 Effort 路由器
     */
    public EffortRouter() {
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        ModelSelectionConfig config = loader.loadOrDefault();
        this.modelSelector = new SmartModelSelector(config);
    }

    /**
     * 确定 Worker 配置
     *
     * @param context 任务上下文
     * @return Worker 启动配置
     * @throws IllegalArgumentException 如果上下文为 null
     */
    public WorkerSpawnConfig determineWorkerConfig(TaskContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Task context cannot be null");
        }

        // 1. 计算复杂度分数
        int complexityScore = calculateComplexityScore(context);

        // 2. 确定 Effort tier
        String effortTier = determineEffortTier(complexityScore, context);

        // 3. 智能模型选择
        String selectedModel = selectModel(complexityScore);

        // 4. 生成配置
        return new WorkerSpawnConfig(effortTier, selectedModel);
    }

    /**
     * 计算复杂度分数
     *
     * @param context 任务上下文
     * @return 复杂度分数
     */
    public int calculateComplexityScore(TaskContext context) {
        int score = 0;

        // 文件数：4 个文件以上 (+1)
        if (context.getFileCount() >= 4) {
            score += 1;
        }

        // 目录：包含 core/、guardrails/、security/ (+1)
        if (context.getDirectoryCount() > 0) {
            score += 1;
        }

        // 关键字：包含 architecture、security、design、migration (+1)
        if (context.containsKeywords()) {
            score += 1;
        }

        // 失败历史：有同任务的失败记录 (+2)
        if (context.hasFailureHistory()) {
            score += 2;
        }

        return score;
    }

    /**
     * 确定 Effort tier
     *
     * @param score 复杂度分数
     * @return Effort 等级
     */
    public String determineEffortTier(int score) {
        return determineEffortTier(score, null);
    }

    /**
     * 确定 Effort tier（带上下文）
     *
     * @param score 复杂度分数
     * @param context 任务上下文（用于判断 code-risk）
     * @return Effort 等级
     */
    public String determineEffortTier(int score, TaskContext context) {
        // code-risk 判断：有关键字视为潜在的 code-risk
        boolean hasCodeRisk = context != null && context.containsKeywords();

        if (score >= 3 && hasCodeRisk) {
            return "xhigh";
        } else if (score >= 3) {
            return "high";
        } else {
            return "medium";
        }
    }

    /**
     * 选择模型
     *
     * @param complexityScore 复杂度分数
     * @return 选择的模型名称
     */
    private String selectModel(int complexityScore) {
        try {
            return modelSelector.selectModel(complexityScore);
        } catch (ModelUnavailableException e) {
            // 如果智能选择失败，使用默认模型
            return "glm-4.7"; // 安全兜底
        }
    }

    /**
     * 获取模型选择器（用于测试）
     * @return 模型选择器
     */
    public SmartModelSelector getModelSelector() {
        return modelSelector;
    }

    /**
     * 获取默认 Effort tier
     * @return 默认 Effort tier
     */
    public static String getDefaultEffortTier() {
        return "medium";
    }

    /**
     * 获取 Effort tier 列表
     * @return Effort tier 列表
     */
    public static String[] getEffortTiers() {
        return new String[]{"low", "medium", "high", "xhigh"};
    }

    /**
     * 检查 Effort tier 是否有效
     *
     * @param tier Effort 等级
     * @return 如果有效返回 true
     */
    public static boolean isValidEffortTier(String tier) {
        if (tier == null) {
            return false;
        }
        for (String validTier : getEffortTiers()) {
            if (validTier.equals(tier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查智能模型选择是否启用
     * @return 如果启用返回 true
     */
    public boolean isSmartSelectionEnabled() {
        return modelSelector.isEnabled();
    }

    /**
     * 获取当前配置策略
     * @return 配置策略名称
     */
    public String getStrategy() {
        return modelSelector.getStrategy();
    }
}