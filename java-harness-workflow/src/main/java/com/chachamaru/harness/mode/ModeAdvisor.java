package com.chachamaru.harness.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户交互顾问
 * 提供推荐结果的展示、用户确认机制、交互式模式助手
 */
public class ModeAdvisor {

    private static final double AUTO_APPLY_THRESHOLD = 0.8;
    private static final double USER_CONFIRMATION_THRESHOLD = 0.7;

    /**
     * 格式化显示推荐结果
     * @param recommendation 推荐结果
     * @return 格式化的显示文本
     */
    public String formatRecommendation(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder display = new StringBuilder();
        display.append("╔═══════════════════════════════════════════════════════════════════╗\n");
        display.append("║            🤖 智能执行模式推荐                                    ║\n");
        display.append("╚═══════════════════════════════════════════════════════════════════╝\n\n");

        // 推荐模式和置信度
        display.append("📊 推荐模式: ").append(recommendation.recommendedMode()).append("\n");
        display.append("🎯 置信度:   ").append(String.format("%.1f%%", recommendation.confidence() * 100))
               .append(" (").append(String.format("%.2f", recommendation.confidence())).append(")\n\n");

        // 推荐理由
        display.append("💡 推荐理由:\n");
        display.append("   ").append(recommendation.reason()).append("\n\n");

        // 根据置信度显示不同的推荐强度
        if (recommendation.confidence() >= 0.85) {
            display.append("⭐ 强烈推荐 - 该模式最适合当前任务特征\n");
        } else if (recommendation.confidence() >= 0.7) {
            display.append("✅ 推荐 - 基于任务分析，该模式是较好选择\n");
        } else if (recommendation.confidence() >= 0.5) {
            display.append("💭 建议 - 可以考虑该模式，但也有其他可行选择\n");
        } else {
            display.append("🤔 可选 - 多种模式都可行，请根据具体情况选择\n");
        }

        // 备选方案
        if (!recommendation.alternativeModes().isEmpty()) {
            display.append("\n🔄 备选方案: ").append(recommendation.alternativeModes()).append("\n");
        }

        return display.toString();
    }

    /**
     * 生成推荐摘要
     * @param recommendation 推荐结果
     * @return 推荐摘要
     */
    public String generateSummary(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        return String.format("推荐: %s (置信度: %.0f%%) - %s",
            recommendation.recommendedMode(),
            recommendation.confidence() * 100,
            recommendation.reason());
    }

    /**
     * 生成详细推荐报告
     * @param recommendation 推荐结果
     * @return 详细报告
     */
    public String generateDetailedReport(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder report = new StringBuilder();

        report.append("═══════════════════════════════════════════════════════════════════\n");
        report.append("                    🎯 智能执行模式详细报告\n");
        report.append("═══════════════════════════════════════════════════════════════════\n\n");

        report.append("【推荐决策】\n");
        report.append("执行模式: ").append(recommendation.recommendedMode()).append("\n");
        report.append("置信程度: ").append(String.format("%.1f%%", recommendation.confidence() * 100)).append("\n\n");

        report.append("【推荐理由】\n");
        report.append(recommendation.reason()).append("\n\n");

        report.append("【决策依据】\n");
        if (recommendation.confidence() >= 0.8) {
            report.append("✓ 高置信度推荐 - 该模式与任务特征高度匹配\n");
            report.append("✓ 建议直接采用，除非有特殊考虑\n");
        } else if (recommendation.confidence() >= 0.6) {
            report.append("✓ 中等置信度推荐 - 该模式较为适合当前任务\n");
            report.append("✓ 可以采用，或查看备选方案\n");
        } else {
            report.append("⚠ 低置信度推荐 - 多种模式差异不大\n");
            report.append("⚠ 建议查看备选方案，根据实际情况选择\n");
        }

        if (!recommendation.alternativeModes().isEmpty()) {
            report.append("\n【备选方案】\n");
            for (int i = 0; i < recommendation.alternativeModes().size(); i++) {
                ExecutionMode mode = recommendation.alternativeModes().get(i);
                report.append(String.format("  %d. %s\n", i + 1, mode));
            }
        }

        report.append("\n═══════════════════════════════════════════════════════════════════\n");

        return report.toString();
    }

    /**
     * 生成用户确认提示
     * @param recommendation 推荐结果
     * @return 确认提示
     */
    public String generateConfirmationPrompt(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        if (shouldAutoApply(recommendation)) {
            return String.format("✅ 系统推荐使用 %s 模式 (置信度: %.0f%%)\n" +
                             "accept 推荐？[Y/n]",
                recommendation.recommendedMode(),
                recommendation.confidence() * 100);
        } else {
            return String.format("🤔 系统建议使用 %s 模式 (置信度: %.0f%%)\n" +
                             "由于置信度较低，建议您仔细考虑后确认\n是否 accept 推荐？[Y/n]",
                recommendation.recommendedMode(),
                recommendation.confidence() * 100);
        }
    }

    /**
     * 判断是否应该自动应用推荐
     * @param recommendation 推荐结果
     * @return 如果置信度 >= 0.8 返回 true
     */
    public boolean shouldAutoApply(ModeRecommendation recommendation) {
        if (recommendation == null) {
            return false;
        }
        return recommendation.confidence() >= AUTO_APPLY_THRESHOLD;
    }

    /**
     * 判断是否需要用户确认
     * @param recommendation 推荐结果
     * @return 如果置信度 < 0.7 返回 true
     */
    public boolean requiresUserConfirmation(ModeRecommendation recommendation) {
        if (recommendation == null) {
            return true;
        }
        return recommendation.confidence() < USER_CONFIRMATION_THRESHOLD;
    }

    /**
     * 生成自动应用消息
     * @param recommendation 推荐结果
     * @return 自动应用消息
     */
    public String generateAutoApplyMessage(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        return String.format("🚀 已自动应用推荐: 使用 %s 模式执行任务 (置信度: %.0f%%)",
            recommendation.recommendedMode(),
            recommendation.confidence() * 100);
    }

    /**
     * 生成确认消息
     * @param recommendation 推荐结果
     * @return 确认消息
     */
    public String generateConfirmationMessage(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder message = new StringBuilder();
        message.append("请选择执行模式:\n");
        message.append("  [1] ").append(recommendation.recommendedMode()).append(" (推荐)\n");

        if (!recommendation.alternativeModes().isEmpty()) {
            for (int i = 0; i < recommendation.alternativeModes().size(); i++) {
                ExecutionMode mode = recommendation.alternativeModes().get(i);
                message.append(String.format("  [%d] %s (备选)\n", i + 2, mode));
            }
        }

        message.append("\n请输入选择 [1-").append(1 + recommendation.alternativeModes().size()).append("]: ");

        return message.toString();
    }

    /**
     * 判断输入是否为同意
     */
    public boolean isAffirmative(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lower = input.toLowerCase().trim();
        return lower.equals("yes") || lower.equals("y") ||
               lower.equals("1") || lower.equals("ok") ||
               lower.equals("accept") || lower.equals("confirmed");
    }

    /**
     * 判断输入是否为拒绝
     */
    public boolean isNegative(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lower = input.toLowerCase().trim();
        return lower.equals("no") || lower.equals("n") ||
               lower.equals("0") || lower.equals("cancel") ||
               lower.equals("reject") || lower.equals("declined");
    }

    /**
     * 判断输入是否为备选方案
     */
    public boolean isAlternative(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lower = input.toLowerCase().trim();
        if (lower.equals("alternative") || lower.equals("a")) {
            return true;
        }
        // 检查是否为数字（备选方案编号）
        try {
            int num = Integer.parseInt(lower);
            return num >= 2 && num <= 9; // 假设最多9个备选
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 生成交互式帮助信息
     * @param recommendation 推荐结果
     * @return 帮助信息
     */
    public String generateInteractiveHelp(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder help = new StringBuilder();
        help.append("═══════════════════════════════════════════════════════════════════\n");
        help.append("                       📖 交互式选择帮助\n");
        help.append("═══════════════════════════════════════════════════════════════════\n\n");

        help.append("🎯 推荐模式: ").append(recommendation.recommendedMode()).append("\n");
        help.append("📊 推荐理由: ").append(recommendation.reason()).append("\n\n");

        help.append("🔄 可选方案:\n");
        if (recommendation.alternativeModes().isEmpty()) {
            help.append("   无其他备选方案\n");
        } else {
            for (int i = 0; i < recommendation.alternativeModes().size(); i++) {
                ExecutionMode mode = recommendation.alternativeModes().get(i);
                help.append(String.format("   %d. %s\n", i + 2, mode));
            }
        }

        help.append("\n💡 使用提示:\n");
        help.append("   • 输入 'y' 或 '1' 接受推荐\n");
        help.append("   • 输入 'n' 或 '0' 拒绝推荐\n");
        help.append("   • 输入 'a' 或备选编号选择其他模式\n");
        help.append("   • 输入 'help' 查看此帮助\n");

        help.append("═══════════════════════════════════════════════════════════════════\n");

        return help.toString();
    }

    /**
     * 生成可视化展示
     * @param recommendation 推荐结果
     * @return 可视化展示文本
     */
    public String generateVisualization(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder viz = new StringBuilder();

        viz.append("╔═══════════════════════════════════════════════════════════════════╗\n");
        viz.append("║                    📊 执行模式推荐可视化                            ║\n");
        viz.append("╠═══════════════════════════════════════════════════════════════════╣\n");
        viz.append("║                                                                  ║\n");

        // 推荐模式进度条
        double confidence = recommendation.confidence();
        int barLength = (int)(confidence * 40);
        String bar = "█".repeat(barLength) + "░".repeat(40 - barLength);

        viz.append("║  📈 推荐模式: ").append(String.format("%-8s", recommendation.recommendedMode())).append("       ║\n");
        viz.append("║  📊 置信度:   ").append(String.format("%-40s", bar)).append(" ║\n");
        viz.append("║  ").append(String.format("%.0f%%", confidence * 100)).append("                                                  ║\n");
        viz.append("║                                                                  ║\n");

        // 理由展示
        viz.append("║  💡 理由:                                                      ║\n");
        String[] words = recommendation.reason().split(" ");
        StringBuilder line = new StringBuilder("║           ");
        for (String word : words) {
            if (line.length() + word.length() > 65) {
                viz.append(line).append(" ║\n");
                line = new StringBuilder("║           ");
            }
            line.append(word).append(" ");
        }
        if (line.length() > "║           ".length()) {
            viz.append(line).append(" ║\n");
        }
        viz.append("║                                                                  ║\n");

        // 推荐强度
        if (confidence >= 0.8) {
            viz.append("║  ⭐ 强烈推荐 - 推荐模式与任务特征高度匹配                         ║\n");
        } else if (confidence >= 0.6) {
            viz.append("║  ✅ 推荐     - 推荐模式适合当前任务特征                             ║\n");
        } else {
            viz.append("║  💭 建议     - 多种模式都可行，请根据实际情况选择                   ║\n");
        }

        viz.append("║                                                                  ║\n");
        viz.append("╚═══════════════════════════════════════════════════════════════════╝\n");

        return viz.toString();
    }

    /**
     * 生成选择菜单
     * @param recommendation 推荐结果
     * @return 选择菜单
     */
    public String generateSelectionMenu(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder menu = new StringBuilder();

        menu.append("┌─────────────────────────────────────────────────────────────┐\n");
        menu.append("│               🎯 执行模式选择菜单                                │\n");
        menu.append("├─────────────────────────────────────────────────────────────┤\n");

        // 推荐选项
        menu.append("│  [1] ").append(String.format("%-10s ⭐ 推荐", recommendation.recommendedMode()));
        menu.append(String.format(" (置信度: %.0f%%)", recommendation.confidence() * 100)).append("     │\n");

        // 备选选项
        if (!recommendation.alternativeModes().isEmpty()) {
            for (int i = 0; i < recommendation.alternativeModes().size(); i++) {
                ExecutionMode mode = recommendation.alternativeModes().get(i);
                menu.append("│  [")
                    .append(i + 2)
                    .append("] ")
                    .append(String.format("%-10s", mode))
                    .append(" 备选                             │\n");
            }
        } else {
            menu.append("│  [2] 无备选方案                                                  │\n");
        }

        menu.append("├─────────────────────────────────────────────────────────────┤\n");
        menu.append("│  请选择 [1-").append(1 + recommendation.alternativeModes().size()).append("]:                                                │\n");
        menu.append("└─────────────────────────────────────────────────────────────┘\n");

        return menu.toString();
    }

    /**
     * 解析用户的选择输入
     * @param input 用户输入
     * @return 选择的执行模式
     */
    public ExecutionMode parseModeSelection(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("输入不能为空");
        }

        String trimmed = input.trim().toLowerCase();

        // 尝试解析为数字
        try {
            int choice = Integer.parseInt(trimmed);
            if (choice == 1) {
                return ExecutionMode.PARALLEL;
            } else if (choice == 2) {
                return ExecutionMode.SOLO;
            } else if (choice == 3) {
                return ExecutionMode.BREEZING;
            } else {
                throw new IllegalArgumentException("无效的选择: " + choice + " (必须在 1-3 范围内)");
            }
        } catch (NumberFormatException e) {
            // 不是数字，尝试解析为模式名称
        }

        // 解析为模式名称
        if (trimmed.equals("solo") || trimmed.equals("2")) {
            return ExecutionMode.SOLO;
        } else if (trimmed.equals("parallel") || trimmed.equals("1")) {
            return ExecutionMode.PARALLEL;
        } else if (trimmed.equals("breezing") || trimmed.equals("3")) {
            return ExecutionMode.BREEZING;
        } else {
            throw new IllegalArgumentException("无法识别的模式: " + input);
        }
    }

    /**
     * 生成接受消息
     * @param recommendation 推荐结果
     * @return 接受消息
     */
    public String generateAcceptedMessage(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        return String.format("✅ 已接受推荐: 使用 %s 模式执行任务\n" +
                            "   推荐理由: %s",
            recommendation.recommendedMode(),
            recommendation.reason());
    }

    /**
     * 生成拒绝消息
     * @param recommendation 原推荐结果
     * @param selectedMode 用户选择的模式
     * @return 拒绝消息
     */
    public String generateDeclinedMessage(ModeRecommendation recommendation, ExecutionMode selectedMode) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }
        if (selectedMode == null) {
            throw new IllegalArgumentException("选择的模式不能为null");
        }

        return String.format("🔄 已拒绝推荐: 选择使用 %s 模式 (原推荐: %s)\n" +
                            "   推荐理由: %s",
            selectedMode,
            recommendation.recommendedMode(),
            recommendation.reason());
    }

    /**
     * 生成对比表格
     * @param recommendation 推荐结果
     * @param scores 评分结果
     * @return 对比表格
     */
    public String generateComparisonTable(ModeRecommendation recommendation, ModeScores scores) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }
        if (scores == null) {
            throw new IllegalArgumentException("评分结果不能为null");
        }

        StringBuilder table = new StringBuilder();

        table.append("┌────────────────────────────────────────────────────────────┐\n");
        table.append("│                    📊 模式评分对比                              │\n");
        table.append("├────────────────────────────────────────────────────────────┤\n");
        table.append("│  模式        │ 评分     │ 状态                                │\n");
        table.append("├────────────────────────────────────────────────────────────┤\n");

        // SOLO 行
        table.append(String.format("│  %-10s  │  %.1f%%   │",
            "SOLO", scores.soloScore() * 100));
        if (recommendation.recommendedMode() == ExecutionMode.SOLO) {
            table.append(" ⭐ 推荐                          │\n");
        } else if (recommendation.alternativeModes().contains(ExecutionMode.SOLO)) {
            table.append(" 🔄 备选                          │\n");
        } else {
            table.append("                                 │\n");
        }

        // PARALLEL 行
        table.append(String.format("│  %-10s  │  %.1f%%   │",
            "PARALLEL", scores.parallelScore() * 100));
        if (recommendation.recommendedMode() == ExecutionMode.PARALLEL) {
            table.append(" ⭐ 推荐                          │\n");
        } else if (recommendation.alternativeModes().contains(ExecutionMode.PARALLEL)) {
            table.append(" 🔄 备选                          │\n");
        } else {
            table.append("                                 │\n");
        }

        // BREEZING 行
        table.append(String.format("│  %-10s  │  %.1f%%   │",
            "BREEZING", scores.breezingScore() * 100));
        if (recommendation.recommendedMode() == ExecutionMode.BREEZING) {
            table.append(" ⭐ 推荐                          │\n");
        } else if (recommendation.alternativeModes().contains(ExecutionMode.BREEZING)) {
            table.append(" 🔄 备选                          │\n");
        } else {
            table.append("                                 │\n");
        }

        table.append("└────────────────────────────────────────────────────────────┘\n");

        return table.toString();
    }

    /**
     * 生成交互流程指导
     * @param recommendation 推荐结果
     * @return 交互流程指导
     */
    public String generateInteractionFlow(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder flow = new StringBuilder();

        flow.append("═══════════════════════════════════════════════════════════════════\n");
        flow.append("                       🎮 交互式推荐流程                               \n");
        flow.append("═══════════════════════════════════════════════════════════════════\n\n");

        flow.append("Step 1: 📊 系统分析任务特征\n");
        flow.append("       系统已分析当前任务，生成了执行模式推荐\n\n");

        flow.append("Step 2: 🎯 查看推荐结果\n");
        flow.append("       推荐模式: ").append(recommendation.recommendedMode()).append("\n");
        flow.append("       置信度:   ").append(String.format("%.0f%%\n", recommendation.confidence() * 100));
        flow.append("       推荐理由: ").append(recommendation.reason()).append("\n\n");

        flow.append("Step 3: 🔄 做出选择\n");
        if (shouldAutoApply(recommendation)) {
            flow.append("       ✓ 高置信度推荐 - 系统建议自动应用\n");
            flow.append("       输入 'y' 接受推荐，其他任意键查看备选\n\n");
        } else {
            flow.append("       ⚠ 中/低置信度 - 请仔细考虑后选择\n");
            flow.append("       输入 'y' 接受推荐，或选择备选方案\n\n");
        }

        if (!recommendation.alternativeModes().isEmpty()) {
            flow.append("       可用备选: ").append(recommendation.alternativeModes()).append("\n\n");
        } else {
            flow.append("       无备选方案\n\n");
        }

        flow.append("Step 4: ✅ 确认选择\n");
        flow.append("       系统将根据您的选择执行任务\n\n");

        flow.append("═══════════════════════════════════════════════════════════════════\n");

        return flow.toString();
    }

    /**
     * 生成ASCII艺术装饰
     * @param recommendation 推荐结果
     * @return ASCII艺术文本
     */
    public String generateASCIIArt(ModeRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }

        StringBuilder art = new StringBuilder();

        // 根据推荐模式生成不同的ASCII艺术
        switch (recommendation.recommendedMode()) {
            case SOLO -> {
                art.append("    ████\n");
                art.append("  ████\n");
                art.append(" █████\n");
                art.append("  █████\n");
                art.append("   █████\n");
                art.append("    █████\n");
                art.append("     █████\n");
                break;
            }
            case PARALLEL -> {
                art.append("  ████    ████\n");
                art.append(" █████    █████\n");
                art.append("████████████████\n");
                art.append(" █████    █████\n");
                art.append("  ████    ████\n");
                break;
            }
            case BREEZING -> {
                art.append("      ████\n");
                art.append("    ███████\n");
                art.append("  █████████\n");
                art.append("███████████\n");
                art.append("  █████████\n");
                art.append("    ███████\n");
                art.append("      ████\n");
                break;
            }
        }

        return art.toString();
    }
}