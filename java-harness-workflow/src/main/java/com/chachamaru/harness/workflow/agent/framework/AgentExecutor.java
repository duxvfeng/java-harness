package com.chachamaru.harness.workflow.agent.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 执行器
 */
public class AgentExecutor {
    private static final Logger logger = LoggerFactory.getLogger(AgentExecutor.class);

    private final ConcurrentHashMap<String, AgentExecution> activeExecutions = new ConcurrentHashMap<>();
    private final AtomicInteger executionCounter = new AtomicInteger(0);

    public AgentResult execute(Agent agent, AgentContext context) {
        String executionId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            logger.info("Executing agent: {} (executionId: {})", agent.getAgentId(), executionId);

            // 验证前置条件
            if (!agent.validatePreconditions(context)) {
                logger.warn("Agent {} preconditions failed", agent.getAgentId());
                return AgentResult.builder()
                        .agentId(agent.getAgentId())
                        .executionId(executionId)
                        .status(AgentStatus.FAILED)
                        .startTime(startTime)
                        .completedTime(Instant.now())
                        .errorMessage("Preconditions validation failed")
                        .build();
            }

            // 记录活跃执行
            executionCounter.incrementAndGet();
            activeExecutions.put(executionId, new AgentExecution(executionId, agent.getAgentId()));

            // 执行 Agent
            AgentResult result = agent.execute(context);

            // 清理执行记录
            executionCounter.decrementAndGet();
            activeExecutions.remove(executionId);

            logger.info("Agent {} completed with status: {}", agent.getAgentId(), result.getStatus());
            return result;

        } catch (AgentExecutionException e) {
            logger.error("Agent {} execution failed", agent.getAgentId(), e);

            // 清理执行记录
            executionCounter.decrementAndGet();
            activeExecutions.remove(executionId);

            return AgentResult.builder()
                    .agentId(agent.getAgentId())
                    .executionId(executionId)
                    .status(AgentStatus.FAILED)
                    .startTime(startTime)
                    .completedTime(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error executing agent: {}", agent.getAgentId(), e);

            // 清理执行记录
            executionCounter.decrementAndGet();
            activeExecutions.remove(executionId);

            return AgentResult.builder()
                    .agentId(agent.getAgentId())
                    .executionId(executionId)
                    .status(AgentStatus.FAILED)
                    .startTime(startTime)
                    .completedTime(Instant.now())
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    public void cancelExecution(String executionId) throws AgentLifecycleException {
        AgentExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            throw new AgentLifecycleException("Execution not found: " + executionId);
        }

        // 阶段1：简单实现，只从活跃列表中移除
        activeExecutions.remove(executionId);
        executionCounter.decrementAndGet();

        logger.info("Cancelled execution: {}", executionId);
    }

    public int getActiveExecutionCount() {
        return executionCounter.get();
    }

    /**
     * 执行记录（内部使用）
     */
    private static class AgentExecution {
        private final String executionId;
        private final String agentId;
        private final Instant startTime;

        public AgentExecution(String executionId, String agentId) {
            this.executionId = executionId;
            this.agentId = agentId;
            this.startTime = Instant.now();
        }

        public String getExecutionId() { return executionId; }
        public String getAgentId() { return agentId; }
        public Instant getStartTime() { return startTime; }
    }
}
