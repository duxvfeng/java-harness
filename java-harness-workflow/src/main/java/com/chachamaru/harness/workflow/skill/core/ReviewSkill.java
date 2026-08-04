package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.Skill;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 审查技能
 * 职责：审查和评审工作成果
 */
public class ReviewSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(ReviewSkill.class);

    @Override
    public String getSkillId() {
        return "review";
    }

    @Override
    public String getSkillName() {
        return "Review Skill";
    }

    @Override
    public String getVersion() {
        return "1.0.0-java";
    }

    @Override
    public String getDescription() {
        return "审查和评审工作成果，提供改进建议";
    }

    @Override
    public Object execute(SkillContext context) throws SkillExecutionException {
        logger.info("ReviewSkill executing: {}", context.getUserIntent());

        try {
            // 分析需要审查的内容
            String userIntent = context.getUserIntent();

            // 执行审查（阶段1：模拟审查）
            List<String> findings = generateFindings(userIntent, context);
            List<String> suggestions = generateSuggestions(findings);

            // 判断是否通过审查
            boolean approved = isApproved(findings);

            String summary = approved ? "审查通过，无重大问题" : "发现需要改进的问题";

            return ReviewResult.builder()
                    .approved(approved)
                    .summary(summary)
                    .findings(findings)
                    .suggestions(suggestions)
                    .build();

        } catch (Exception e) {
            logger.error("ReviewSkill execution failed", e);
            throw new SkillExecutionException("Review failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validatePreconditions(SkillContext context) {
        // 检查是否有用户意图
        return context.getUserIntent() != null && !context.getUserIntent().isEmpty();
    }

    /**
     * 生成审查发现
     */
    private List<String> generateFindings(String userIntent, SkillContext context) {
        List<String> findings = new ArrayList<>();

        // 阶段1：简单实现，生成通用审查发现
        if (userIntent.contains("代码") || userIntent.contains("实现")) {
            findings.add("代码结构清晰，符合设计模式");
            findings.add("建议添加更多注释说明");
            findings.add("测试覆盖率可以提高");
        } else if (userIntent.contains("设计") || userIntent.contains("架构")) {
            findings.add("设计方案合理");
            findings.add("建议考虑扩展性");
            findings.add("文档可以更详细");
        } else {
            findings.add("工作内容完整");
            findings.add("建议进一步优化");
        }

        logger.info("Generated {} review findings", findings.size());
        return findings;
    }

    /**
     * 生成改进建议
     */
    private List<String> generateSuggestions(List<String> findings) {
        List<String> suggestions = new ArrayList<>();

        for (String finding : findings) {
            if (finding.contains("建议")) {
                // 将发现转换为建议
                suggestions.add(finding.substring(finding.indexOf("建议") + 3));
            }
        }

        // 添加通用建议
        if (suggestions.isEmpty()) {
            suggestions.add("保持当前质量标准");
            suggestions.add("持续改进和优化");
        }

        logger.info("Generated {} suggestions", suggestions.size());
        return suggestions;
    }

    /**
     * 判断是否通过审查
     */
    private boolean isApproved(List<String> findings) {
        // 阶段1：如果没有严重问题，则通过审查
        return findings.stream()
                .noneMatch(f -> f.contains("严重") || f.contains("错误") || f.contains("失败"));
    }
}
