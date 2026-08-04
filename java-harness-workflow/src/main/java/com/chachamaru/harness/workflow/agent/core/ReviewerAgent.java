package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 审查代理
 * 职责：审查和评审工作成果
 */
public class ReviewerAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(ReviewerAgent.class);

    @Override
    public String getAgentId() {
        return "reviewer";
    }

    @Override
    public String getAgentName() {
        return "Reviewer Agent";
    }

    @Override
    public String getVersion() {
        return "1.0.0-java";
    }

    @Override
    public String getDescription() {
        return "审查和评审工作成果的代理";
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.REVIEWER;
    }

    @Override
    public List<String> getRequiredSkills() {
        return Arrays.asList("review");
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("ReviewerAgent executing task: {}", context.getTaskId());

        try {
            // 获取需要审查的内容（从共享状态或上下文）
            Object workResult = context.getSharedState("workResult");
            if (workResult == null) {
                logger.warn("No work result found in shared state");
                return AgentResult.builder()
                        .agentId(getAgentId())
                        .status(AgentStatus.FAILED)
                        .errorMessage("No work result to review")
                        .build();
            }

            // 调用 ReviewSkill 进行审查
            SkillResult reviewResult = context.callSkill("review");

            // 构建结果
            List<SkillCallTrace> skillCalls = new ArrayList<>();
            skillCalls.add(createSkillCallTrace("review", reviewResult, "执行审查", 1));

            AgentStatus status = reviewResult.isSuccess() ? AgentStatus.SUCCESS : AgentStatus.FAILED;

            return AgentResult.builder()
                    .agentId(getAgentId())
                    .status(status)
                    .output(reviewResult.getOutput())
                    .skillCalls(skillCalls)
                    .build();

        } catch (Exception e) {
            logger.error("ReviewerAgent execution failed", e);
            throw new AgentExecutionException("Review execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.info("ReviewerAgent initialized");
    }

    @Override
    public boolean validatePreconditions(AgentContext context) {
        // 检查是否有需要审查的内容
        Object workResult = context.getSharedState("workResult");
        return workResult != null;
    }

    /**
     * 创建 Skill 调用追踪
     */
    private SkillCallTrace createSkillCallTrace(String skillId, SkillResult result, String decision, int callOrder) {
        return SkillCallTrace.builder()
                .skillId(skillId)
                .result(result)
                .callerDecision(decision)
                .callOrder(callOrder)
                .build();
    }
}
