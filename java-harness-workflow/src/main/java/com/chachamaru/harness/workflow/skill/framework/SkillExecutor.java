package com.chachamaru.harness.workflow.skill.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能执行引擎
 * 负责技能的执行、暂停、恢复和取消
 */
public class SkillExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SkillExecutor.class);

    private final ConcurrentHashMap<String, SkillExecution> activeExecutions;

    public SkillExecutor() {
        this.activeExecutions = new ConcurrentHashMap<>();
    }

    /**
     * 执行技能
     *
     * @param skill 技能实例
     * @param context 执行上下文
     * @return 执行结果
     */
    public SkillResult execute(Skill skill, SkillContext context) throws SkillExecutionException {
        String executionId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            logger.info("Executing skill: {} (executionId: {})", skill.getSkillId(), executionId);

            // 验证前置条件
            if (!skill.validatePreconditions(context)) {
                logger.warn("Skill preconditions failed: {}", skill.getSkillId());
                return SkillResult.builder()
                        .skillId(skill.getSkillId())
                        .executionId(executionId)
                        .status(SkillResult.SkillStatus.FAILED)
                        .startTime(startTime)
                        .completedTime(Instant.now())
                        .errorMessage("Preconditions validation failed")
                        .build();
            }

            // 创建执行记录
            SkillExecution execution = new SkillExecution(executionId, skill, context, startTime);
            activeExecutions.put(executionId, execution);

            // 更新状态为运行中
            SkillResult runningResult = SkillResult.builder()
                    .skillId(skill.getSkillId())
                    .executionId(executionId)
                    .status(SkillResult.SkillStatus.RUNNING)
                    .startTime(startTime)
                    .build();

            // 执行技能的核心逻辑
            Object result = skill.execute(context);

            // 创建成功结果
            SkillResult skillResult = SkillResult.builder()
                    .skillId(skill.getSkillId())
                    .executionId(executionId)
                    .status(SkillResult.SkillStatus.SUCCESS)
                    .startTime(startTime)
                    .completedTime(Instant.now())
                    .output(result)
                    .build();

            logger.info("Skill {} completed successfully in {}ms",
                    skill.getSkillId(), skillResult.getExecutionDurationMs());

            return skillResult;

        } catch (Exception e) {
            logger.error("Skill {} execution failed", skill.getSkillId(), e);

            SkillResult.SkillStatus status = SkillResult.SkillStatus.FAILED;
            if (e.getCause() instanceof InterruptedException) {
                status = SkillResult.SkillStatus.CANCELLED;
                Thread.currentThread().interrupt();
            }

            return SkillResult.builder()
                    .skillId(skill.getSkillId())
                    .executionId(executionId)
                    .status(status)
                    .startTime(startTime)
                    .completedTime(Instant.now())
                    .errorMessage("Execution error: " + e.getMessage())
                    .addMetadata("errorType", e.getClass().getSimpleName())
                    .build();
        } finally {
            // 清理执行记录
            activeExecutions.remove(executionId);
        }
    }

    /**
     * 暂停执行
     *
     * @param executionId 执行ID
     */
    public void pauseExecution(String executionId) throws SkillExecutionException {
        SkillExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            throw new SkillExecutionException("Execution not found: " + executionId);
        }

        if (!execution.skill.supportsPause()) {
            throw new SkillExecutionException("Skill does not support pause: " + execution.skill.getSkillId());
        }

        logger.info("Pausing execution: {}", executionId);
        execution.pause();

        // TODO: 实现暂停逻辑
        throw new UnsupportedOperationException("Pause not yet implemented");
    }

    /**
     * 恢复执行
     *
     * @param executionId 执行ID
     */
    public void resumeExecution(String executionId) throws SkillExecutionException {
        SkillExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            throw new SkillExecutionException("Execution not found: " + executionId);
        }

        if (!execution.skill.supportsResume()) {
            throw new SkillExecutionException("Skill does not support resume: " + execution.skill.getSkillId());
        }

        logger.info("Resuming execution: {}", executionId);
        execution.resume();

        // TODO: 实现恢复逻辑
        throw new UnsupportedOperationException("Resume not yet implemented");
    }

    /**
     * 取消执行
     *
     * @param executionId 执行ID
     */
    public void cancelExecution(String executionId) throws SkillExecutionException {
        SkillExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            throw new SkillExecutionException("Execution not found: " + executionId);
        }

        if (!execution.skill.supportsCancel()) {
            throw new SkillExecutionException("Skill does not support cancel: " + execution.skill.getSkillId());
        }

        logger.info("Cancelling execution: {}", executionId);
        execution.cancel();

        // TODO: 实现取消逻辑
        throw new UnsupportedOperationException("Cancel not yet implemented");
    }

    /**
     * 获取活跃执行数量
     */
    public int getActiveExecutionCount() {
        return activeExecutions.size();
    }

    /**
     * 技能执行记录
     */
    private static class SkillExecution {
        private final String executionId;
        private final Skill skill;
        private final SkillContext context;
        private final Instant startTime;
        private ExecutionState state;

        public SkillExecution(String executionId, Skill skill, SkillContext context, Instant startTime) {
            this.executionId = executionId;
            this.skill = skill;
            this.context = context;
            this.startTime = startTime;
            this.state = ExecutionState.RUNNING;
        }

        public void pause() {
            this.state = ExecutionState.PAUSED;
        }

        public void resume() {
            this.state = ExecutionState.RUNNING;
        }

        public void cancel() {
            this.state = ExecutionState.CANCELLED;
        }

        private enum ExecutionState {
            RUNNING, PAUSED, CANCELLED, COMPLETED
        }
    }
}