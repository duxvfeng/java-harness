package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * ModeAdvisor 用户交互组件的单元测试
 * 验证推荐展示、用户确认机制、交互式助手的正确性
 */
@DisplayName("ModeAdvisor 用户交互组件测试")
class ModeAdvisorTest {

    private final ModeAdvisor advisor = new ModeAdvisor();

    @Test
    @DisplayName("应该能够格式化显示推荐结果")
    void shouldDisplayRecommendationInFormattedWay() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.85,
            "3个独立任务，中等复杂度",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        String display = advisor.formatRecommendation(recommendation);

        assertNotNull(display);
        assertTrue(display.contains("PARALLEL"), "显示应该包含推荐模式");
        assertTrue(display.contains("85%") || display.contains("0.85"), "显示应该包含置信度");
        assertTrue(display.contains("3个独立任务"), "显示应该包含推荐理由");
        assertFalse(display.isEmpty(), "显示内容不能为空");
    }

    @Test
    @DisplayName("应该能够生成简洁的推荐摘要")
    void shouldGenerateConciseRecommendationSummary() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.92,
            "单个简单任务",
            List.of()
        );

        String summary = advisor.generateSummary(recommendation);

        assertNotNull(summary);
        assertTrue(summary.length() < 200, "摘要应该简洁，长度 < 200");
        assertTrue(summary.contains("SOLO"), "摘要应该包含推荐模式");
        assertTrue(summary.contains("单个") || summary.contains("简单"), "摘要应该包含关键特征");
    }

    @Test
    @DisplayName("应该能够生成详细的推荐报告")
    void shouldGenerateDetailedRecommendationReport() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.88,
            "7个复杂任务，需要团队协作",
            List.of(ExecutionMode.PARALLEL)
        );

        String report = advisor.generateDetailedReport(recommendation);

        assertNotNull(report);
        assertTrue(report.length() > 100, "详细报告应该有足够内容");
        assertTrue(report.contains("BREEZING"), "报告应该包含推荐模式");
        assertTrue(report.contains("团队") || report.contains("协作"), "报告应该包含推荐理由特征");
        assertTrue(report.contains("备选") || report.contains("alternative"), "报告应该包含备选方案信息");
    }

    @Test
    @DisplayName("应该能够生成用户确认提示")
    void shouldGenerateUserConfirmationPrompt() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "中等复杂度任务组",
            List.of(ExecutionMode.SOLO)
        );

        String prompt = advisor.generateConfirmationPrompt(recommendation);

        assertNotNull(prompt);
        assertTrue(prompt.contains("确认") || prompt.contains("accept") || prompt.contains("agree"),
            "确认提示应该包含确认相关的词语");
        assertTrue(prompt.contains("PARALLEL") || prompt.contains("并行"),
            "确认提示应该包含推荐的执行模式");
    }

    @Test
    @DisplayName("应该能够处理高置信度的自动确认")
    void shouldHandleAutoConfirmationForHighConfidence() {
        ModeRecommendation highConfidenceRecommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.95,
            "明确推荐的简单任务",
            List.of()
        );

        assertTrue(advisor.shouldAutoApply(highConfidenceRecommendation),
            "高置信度推荐应该可以自动应用");

        String autoApplyMessage = advisor.generateAutoApplyMessage(highConfidenceRecommendation);
        assertTrue(autoApplyMessage.contains("自动") || autoApplyMessage.contains("auto"),
            "自动应用消息应该包含自动相关词语");
    }

    @Test
    @DisplayName("应该能够处理低置信度的用户确认")
    void shouldRequireUserConfirmationForLowConfidence() {
        ModeRecommendation lowConfidenceRecommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.55,
            "中等复杂度任务，有多种选择",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        assertTrue(advisor.requiresUserConfirmation(lowConfidenceRecommendation),
            "低置信度推荐应该需要用户确认");

        String confirmationMessage = advisor.generateConfirmationMessage(lowConfidenceRecommendation);
        assertTrue(confirmationMessage.contains("选择") || confirmationMessage.contains("confirm"),
            "确认消息应该包含选择相关词语");
    }

    @Test
    @DisplayName("应该能够解析用户选择")
    void shouldParseUserChoice() {
        // 测试各种用户输入的解析
        assertTrue(advisor.isAffirmative("yes"), "yes 应该被识别为同意");
        assertTrue(advisor.isAffirmative("y"), "y 应该被识别为同意");
        assertTrue(advisor.isAffirmative("1"), "1 应该被识别为同意");

        assertTrue(advisor.isNegative("no"), "no 应该被识别为拒绝");
        assertTrue(advisor.isNegative("n"), "n 应该被识别为拒绝");
        assertTrue(advisor.isNegative("0"), "0 应该被识别为拒绝");

        assertTrue(advisor.isAlternative("alternative"), "alternative 应该被识别为备选");
        assertTrue(advisor.isAlternative("a"), "a 应该被识别为备选");
        assertTrue(advisor.isAlternative("2"), "2 应该被识别为备选");
    }

    @Test
    @DisplayName("应该能够生成交互式帮助信息")
    void shouldGenerateInteractiveHelp() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.72,
            "推荐并行处理",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        String help = advisor.generateInteractiveHelp(recommendation);

        assertNotNull(help);
        assertTrue(help.contains("选择") || help.contains("选项"), "帮助信息应该包含选择说明");
        assertTrue(help.contains("SOLO") || help.contains("BREEZING"), "帮助信息应该包含备选方案");
        assertTrue(help.length() > 50, "帮助信息应该有足够细节");
    }

    @Test
    @DisplayName("应该能够生成推荐的视觉化展示")
    void shouldGenerateVisualizedRecommendation() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.91,
            "大规模复杂任务",
            List.of(ExecutionMode.PARALLEL)
        );

        String visual = advisor.generateVisualization(recommendation);

        assertNotNull(visual);
        assertTrue(visual.contains("█") || visual.contains("=") || visual.contains("*"),
            "可视化展示应该包含图形字符");
        assertTrue(visual.contains("推荐") || visual.contains("BREEZING"),
            "可视化展示应该突出推荐模式");
    }

    @Test
    @DisplayName("应该能够处理不同置信度的展示风格")
    void shouldHandleDifferentDisplayStylesForConfidence() {
        ModeRecommendation highConfidence = new ModeRecommendation(
            ExecutionMode.SOLO, 0.95, "高置信度", List.of()
        );

        String highDisplay = advisor.formatRecommendation(highConfidence);
        assertTrue(highDisplay.contains("强烈推荐") || highDisplay.contains("高置信度"),
            "高置信度应该显示特殊标记");

        ModeRecommendation mediumConfidence = new ModeRecommendation(
            ExecutionMode.PARALLEL, 0.70, "中等置信度", List.of(ExecutionMode.SOLO)
        );

        String mediumDisplay = advisor.formatRecommendation(mediumConfidence);
        // 中等置信度不应该有强烈的推荐标记
        assertFalse(mediumDisplay.contains("强烈推荐"),
            "中等置信度不应该显示强烈推荐标记");

        ModeRecommendation lowConfidence = new ModeRecommendation(
            ExecutionMode.BREEZING, 0.45, "低置信度", List.of(ExecutionMode.PARALLEL, ExecutionMode.SOLO)
        );

        String lowDisplay = advisor.formatRecommendation(lowConfidence);
        assertTrue(lowDisplay.contains("建议") || lowDisplay.contains("考虑") ||
                   lowDisplay.contains("备选"),
            "低置信度应该使用建议性语言");
    }

    @Test
    @DisplayName("应该能够提供模式选择菜单")
    void shouldProvideModeSelectionMenu() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "推荐PARALLEL",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        String menu = advisor.generateSelectionMenu(recommendation);

        assertNotNull(menu);
        assertTrue(menu.contains("1") || menu.contains("[1]"), "菜单应该包含选项编号");
        assertTrue(menu.contains("PARALLEL") || menu.contains("推荐"), "菜单应该突出推荐选项");
        assertTrue(menu.contains("SOLO") || menu.contains("BREEZING"), "菜单应该包含备选选项");
    }

    @Test
    @DisplayName("应该能够解析用户的选择输入")
    void shouldParseUserSelectionInput() {
        // 测试数字选择解析
        assertEquals(ExecutionMode.PARALLEL, advisor.parseModeSelection("1"),
            "应该能解析数字选择1");
        assertEquals(ExecutionMode.SOLO, advisor.parseModeSelection("2"),
            "应该能解析数字选择2");

        // 测试模式名称解析
        assertEquals(ExecutionMode.BREEZING, advisor.parseModeSelection("breezing"),
            "应该能解析模式名称");
        assertEquals(ExecutionMode.SOLO, advisor.parseModeSelection("SOLO"),
            "应该能解析模式名称（大写）");

        // 测试无效输入
        assertThrows(IllegalArgumentException.class, () -> advisor.parseModeSelection("invalid"),
            "无效输入应该抛出异常");
        assertThrows(IllegalArgumentException.class, () -> advisor.parseModeSelection("99"),
            "超出范围的数字应该抛出异常");
    }

    @Test
    @DisplayName("应该能够生成推荐接受消息")
    void shouldGenerateRecommendationAcceptedMessage() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.88,
            "推荐SOLO模式",
            List.of()
        );

        String message = advisor.generateAcceptedMessage(recommendation);

        assertNotNull(message);
        assertTrue(message.contains("接受") || message.contains("采用") || message.contains("使用"),
            "接受消息应该包含接受相关词语");
        assertTrue(message.contains("SOLO"), "接受消息应该包含选择的模式");
    }

    @Test
    @DisplayName("应该能够生成推荐拒绝消息")
    void shouldGenerateRecommendationDeclinedMessage() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.65,
            "推荐PARALLEL模式",
            List.of(ExecutionMode.SOLO)
        );

        String message = advisor.generateDeclinedMessage(recommendation, ExecutionMode.SOLO);

        assertNotNull(message);
        assertTrue(message.contains("选择") || message.contains("采用") || message.contains("使用"),
            "拒绝消息应该包含选择相关词语");
        assertTrue(message.contains("SOLO"), "拒绝消息应该包含实际选择的模式");
    }

    @Test
    @DisplayName("应该能够处理边界情况")
    void shouldHandleBoundaryCases() {
        // 空推荐结果
        assertThrows(IllegalArgumentException.class, () -> advisor.formatRecommendation(null),
            "空推荐结果应该抛出异常");

        // 空备选方案列表
        ModeRecommendation noAlternatives = new ModeRecommendation(
            ExecutionMode.SOLO, 0.9, "唯一推荐", List.of()
        );

        String menu = advisor.generateSelectionMenu(noAlternatives);
        assertNotNull(menu, "即使没有备选方案也应该能生成菜单");

        // 极端置信度值
        ModeRecommendation maxConfidence = new ModeRecommendation(
            ExecutionMode.PARALLEL, 1.0, "最高置信度", List.of()
        );

        assertTrue(advisor.shouldAutoApply(maxConfidence),
            "置信度1.0应该自动应用");

        ModeRecommendation minConfidence = new ModeRecommendation(
            ExecutionMode.BREEZING, 0.0, "最低置信度", List.of()
        );

        assertTrue(advisor.requiresUserConfirmation(minConfidence),
            "置信度0.0应该需要用户确认");
    }

    @Test
    @DisplayName("应该能够生成推荐对比表格")
    void shouldGenerateRecommendationComparisonTable() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.78,
            "推荐并行",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        // 创建模拟的评分数据
        ModeScores scores = new ModeScores(
            0.5,  // SOLO
            0.78, // PARALLEL (推荐)
            0.6,  // BREEZING
            java.util.Map.of(
                ExecutionMode.SOLO, 0.5,
                ExecutionMode.PARALLEL, 0.78,
                ExecutionMode.BREEZING, 0.6
            )
        );

        String table = advisor.generateComparisonTable(recommendation, scores);

        assertNotNull(table);
        assertTrue(table.contains("SOLO") || table.contains("PARALLEL") || table.contains("BREEZING"),
            "对比表格应该包含所有模式");
        assertTrue(table.contains("评分") || table.contains("score") || table.contains("%"),
            "对比表格应该包含评分信息");
    }

    @Test
    @DisplayName("应该能够提供完整的交互流程指导")
    void shouldProvideCompleteInteractionFlow() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.82,
            "复杂任务推荐团队协作",
            List.of(ExecutionMode.PARALLEL)
        );

        String flow = advisor.generateInteractionFlow(recommendation);

        assertNotNull(flow);
        assertTrue(flow.length() > 100, "交互流程指导应该详细");
        assertTrue(flow.contains("步骤") || flow.contains("Step") || flow.contains("流程"),
            "交互流程指导应该包含步骤说明");
    }

    @Test
    @DisplayName("应该能够生成美观的ASCII艺术展示")
    void shouldGenerateBeautifulASCIIArt() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.92,
            "单任务推荐",
            List.of()
        );

        String art = advisor.generateASCIIArt(recommendation);

        assertNotNull(art);
        // ASCII艺术应该包含一些装饰字符
        assertTrue(art.length() > 20, "ASCII艺术应该有基本装饰");
        assertFalse(art.isEmpty(), "ASCII艺术不能为空");
    }

    @Test
    @DisplayName("用户交互API应该设计合理")
    void userInteractionAPIShouldBeWellDesigned() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "推荐",
            List.of(ExecutionMode.SOLO)
        );

        // 验证各种API方法都能正常工作
        assertNotNull(advisor.formatRecommendation(recommendation));
        assertNotNull(advisor.generateSummary(recommendation));
        assertNotNull(advisor.generateDetailedReport(recommendation));
        assertNotNull(advisor.generateConfirmationPrompt(recommendation));
        assertNotNull(advisor.generateInteractiveHelp(recommendation));

        // 验证用户输入解析
        assertTrue(advisor.isAffirmative("yes"));
        assertTrue(advisor.isNegative("no"));
        assertTrue(advisor.isAlternative("2"));
    }
}