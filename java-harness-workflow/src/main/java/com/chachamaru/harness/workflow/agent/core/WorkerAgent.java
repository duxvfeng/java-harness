package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 工作代理
 * 职责：执行具体的工作任务
 */
public class WorkerAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);

    /**
     * 工作策略
     */
    public enum WorkStrategy {
        PLAN_AND_WORK,    // 先规划再工作
        DIRECT_WORK,      // 直接执行工作
        REVIEW_FIRST      // 先审查再工作
    }

    @Override
    public String getAgentId() {
        return "worker";
    }

    @Override
    public String getAgentName() {
        return "Worker Agent";
    }

    @Override
    public String getVersion() {
        return "1.0.0-java";
    }

    @Override
    public String getDescription() {
        return "执行具体任务的代理，负责分析和完成工作";
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.WORKER;
    }

    @Override
    public List<String> getRequiredSkills() {
        return Arrays.asList("plan", "work", "sync");
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("WorkerAgent executing task: {}", context.getTaskId());

        try {
            // 分析任务，决定工作策略
            WorkStrategy strategy = analyzeTask(context);
            logger.info("Selected work strategy: {}", strategy);

            // 根据策略执行工作
            return executeWork(strategy, context);

        } catch (Exception e) {
            logger.error("WorkerAgent execution failed", e);
            throw new AgentExecutionException("Worker execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.info("WorkerAgent initialized");
    }

    /**
     * 分析任务，决定工作策略
     */
    private WorkStrategy analyzeTask(AgentContext context) {
        // 阶段1：暂时使用 DIRECT_WORK 策略（因为 PlanSkill 依赖较多组件）
        return WorkStrategy.DIRECT_WORK;
    }

    /**
     * 根据策略执行工作
     */
    private AgentResult executeWork(WorkStrategy strategy, AgentContext context) {
        List<SkillCallTrace> skillCalls = new ArrayList<>();
        Object finalOutput = null;
        AgentStatus status = AgentStatus.SUCCESS;
        int callOrder = 0;

        switch (strategy) {
            case PLAN_AND_WORK:
                // 先规划再工作
                SkillResult planResult = context.callSkill("plan");
                skillCalls.add(createSkillCallTrace("plan", planResult, "制定工作计划", ++callOrder));

                if (planResult.isSuccess()) {
                    SkillResult workResult = context.callSkill("work");
                    skillCalls.add(createSkillCallTrace("work", workResult, "执行工作", ++callOrder));
                    finalOutput = workResult.getOutput();
                    status = workResult.isSuccess() ? AgentStatus.SUCCESS : AgentStatus.FAILED;
                } else {
                    status = AgentStatus.FAILED;
                }
                break;

            case DIRECT_WORK:
                // 直接执行工作
                SkillResult directWorkResult = context.callSkill("work");
                skillCalls.add(createSkillCallTrace("work", directWorkResult, "直接执行工作", ++callOrder));
                finalOutput = directWorkResult.getOutput();
                status = directWorkResult.isSuccess() ? AgentStatus.SUCCESS : AgentStatus.FAILED;
                break;

            case REVIEW_FIRST:
                // 先审查再工作（阶段1暂未实现）
                logger.warn("REVIEW_FIRST strategy not yet implemented, falling back to DIRECT_WORK");
                SkillResult fallbackResult = context.callSkill("work");
                skillCalls.add(createSkillCallTrace("work", fallbackResult, "回退到直接工作", ++callOrder));
                finalOutput = fallbackResult.getOutput();
                status = fallbackResult.isSuccess() ? AgentStatus.SUCCESS : AgentStatus.FAILED;
                break;
        }

        return AgentResult.builder()
                .agentId(getAgentId())
                .status(status)
                .output(finalOutput)
                .skillCalls(skillCalls)
                .build();
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
