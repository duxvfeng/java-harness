package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkSkill 测试")
public class WorkSkillTest {

    private WorkSkill skill;
    private SkillContext context;

    @BeforeEach
    public void setUp() {
        skill = new WorkSkill();
        context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("/project"))
                .build();
    }

    @Test
    @DisplayName("应该返回正确的技能信息")
    public void testSkillInfo() {
        assertEquals("work", skill.getSkillId());
        assertEquals("Work Skill", skill.getSkillName());
        assertEquals("1.0.0-java", skill.getVersion());
        assertNotNull(skill.getDescription());
    }

    @Test
    @DisplayName("应该执行工作技能")
    public void testExecuteWork() throws SkillExecutionException {
        Object result = skill.execute(context);

        assertNotNull(result);
        assertTrue(result instanceof WorkResult);
    }

    @Test
    @DisplayName("应该在工作完成时返回成功结果")
    public void testWorkSuccess() throws SkillExecutionException {
        context = SkillContext.builder()
                .userIntent("简单任务")
                .projectRoot(Paths.get("/project"))
                .build();

        Object result = skill.execute(context);

        WorkResult workResult = (WorkResult) result;
        assertTrue(workResult.isSuccess());
        assertNotNull(workResult.getOutput());
    }

    @Test
    @DisplayName("应该验证前置条件")
    public void testValidatePreconditions() {
        assertTrue(skill.validatePreconditions(context));
    }
}
