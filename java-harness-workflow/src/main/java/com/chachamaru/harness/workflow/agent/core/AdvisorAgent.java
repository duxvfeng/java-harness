package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * 顾问代理
 * 职责：提供建议和指导
 */
public class AdvisorAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(AdvisorAgent.class);

    @Override
    public String getAgentId() {
        return "advisor";
    }

    @Override
    public String getAgentName() {
        return "Advisor Agent";
    }

    @Override
    public String getVersion() {
        return "1.0.0-java";
    }

    @Override
    public String getDescription() {
        return "提供建议和指导的代理";
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.ADVISOR;
    }

    @Override
    public List<String> getRequiredSkills() {
        return Arrays.asList(); // 顾问代理不需要特定的 Skill
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("AdvisorAgent executing task: {}", context.getTaskId());

        try {
            // 分析用户需求
            String userIntent = context.getUserIntent();
            logger.info("Analyzing user intent: {}", userIntent);

            // 根据需求生成建议
            List<String> suggestions = generateSuggestions(userIntent, context);

            return AgentResult.builder()
                    .agentId(getAgentId())
                    .status(AgentStatus.SUCCESS)
                    .output(suggestions)
                    .build();

        } catch (Exception e) {
            logger.error("AdvisorAgent execution failed", e);
            throw new AgentExecutionException("Advice generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.info("AdvisorAgent initialized");
    }

    /**
     * 根据需求生成建议
     */
    private List<String> generateSuggestions(String userIntent, AgentContext context) {
        // 阶段1：简单实现，返回基础建议
        List<String> suggestions = new java.util.ArrayList<>();

        // 根据用户意图提供不同类型的建议
        if (userIntent.contains("优化") || userIntent.contains("性能")) {
            suggestions.add("建议进行性能分析和瓶颈识别");
            suggestions.add("考虑使用缓存策略");
            suggestions.add("优化数据库查询");
        } else if (userIntent.contains("架构") || userIntent.contains("设计")) {
            suggestions.add("建议遵循 SOLID 原则");
            suggestions.add("考虑设计模式的应用");
            suggestions.add("保持模块化和低耦合");
        } else {
            suggestions.add("建议先分析项目结构");
            suggestions.add("制定清晰的实施计划");
            suggestions.add("考虑测试覆盖率和代码质量");
        }

        logger.info("Generated {} suggestions", suggestions.size());
        return suggestions;
    }
}
