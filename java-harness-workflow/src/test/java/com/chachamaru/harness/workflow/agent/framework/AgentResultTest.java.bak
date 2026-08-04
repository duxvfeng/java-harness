package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentResult 测试")
public class AgentResultTest {

    @Test
    @DisplayName("应该创建成功的 AgentResult")
    public void testCreateSuccessResult() {
        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .output("任务完成")
                .build();

        assertEquals("worker", result.getAgentId());
        assertEquals(AgentStatus.SUCCESS, result.getStatus());
        assertEquals("任务完成", result.getOutput());
        assertTrue(result.isSuccess());
        assertFalse(result.isPartialSuccess());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("应该创建失败的 AgentResult")
    public void testCreateFailedResult() {
        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.FAILED)
                .errorMessage("执行失败")
                .build();

        assertEquals(AgentStatus.FAILED, result.getStatus());
        assertEquals("执行失败", result.getErrorMessage());
    }

    @Test
    @DisplayName("应该支持添加 Skill 调用追踪")
    public void testAddSkillCalls() {
        SkillResult skillResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(skillResult)
                .build();

        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .addSkillCall(trace)
                .build();

        assertEquals(1, result.getSkillCalls().size());
        assertEquals("work", result.getSkillCalls().get(0).getSkillId());
    }

    @Test
    @DisplayName("应该计算执行时长")
    public void testExecutionDuration() {
        Instant startTime = Instant.now().minusSeconds(5);
        Instant completedTime = Instant.now();

        AgentResult result = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .startTime(startTime)
                .completedTime(completedTime)
                .build();

        assertTrue(result.getExecutionDurationMs() >= 0);
        assertTrue(result.getExecutionDurationMs() <= 6000);
    }

    @Test
    @DisplayName("应该支持便捷方法创建结果")
    public void testConvenienceMethods() {
        AgentResult success = AgentResult.builder()
                .agentId("worker")
                .success("任务完成")
                .build();

        assertTrue(success.isSuccess());

        AgentResult failed = AgentResult.builder()
                .agentId("worker")
                .failed("任务失败")
                .build();

        assertEquals(AgentStatus.FAILED, failed.getStatus());
        assertEquals("任务失败", failed.getErrorMessage());
    }
}
