package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillCallTrace 测试")
public class SkillCallTraceTest {

    @Test
    @DisplayName("应该创建 Skill 调用追踪")
    public void testCreateSkillCallTrace() {
        SkillResult mockResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .output("工作完成")
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(mockResult)
                .callerDecision("需要执行工作")
                .callOrder(1)
                .build();

        assertEquals("work", trace.getSkillId());
        assertEquals(mockResult, trace.getResult());
        assertEquals("需要执行工作", trace.getCallerDecision());
        assertEquals(1, trace.getCallOrder());
        assertNotNull(trace.getCallId());
        assertNotNull(trace.getCallTime());
    }

    @Test
    @DisplayName("应该判断调用是否成功")
    public void testIsSuccessful() {
        SkillResult successResult = SkillResult.builder()
                .skillId("work")
                .status(SkillResult.SkillStatus.SUCCESS)
                .build();

        SkillCallTrace successTrace = SkillCallTrace.builder()
                .skillId("work")
                .result(successResult)
                .build();

        assertTrue(successTrace.isSuccessful());
    }

    @Test
    @DisplayName("应该获取调用时长")
    public void testGetDuration() {
        SkillResult mockResult = SkillResult.builder()
                .skillId("work")
                .startTime(Instant.now().minusSeconds(5))
                .completedTime(Instant.now())
                .build();

        SkillCallTrace trace = SkillCallTrace.builder()
                .skillId("work")
                .result(mockResult)
                .build();

        assertTrue(trace.getDuration() >= 0);
        assertTrue(trace.getDuration() <= 6000); // 最多6秒
    }
}
