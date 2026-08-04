package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewSkill 测试")
public class ReviewSkillTest {

    private ReviewSkill skill;
    private SkillContext context;

    @BeforeEach
    public void setUp() {
        skill = new ReviewSkill();
        context = SkillContext.builder()
                .userIntent("审查代码")
                .projectRoot(Paths.get("/project"))
                .build();
    }

    @Test
    @DisplayName("应该返回正确的技能信息")
    public void testSkillInfo() {
        assertEquals("review", skill.getSkillId());
        assertEquals("Review Skill", skill.getSkillName());
        assertNotNull(skill.getDescription());
    }

    @Test
    @DisplayName("应该执行审查")
    public void testExecuteReview() throws SkillExecutionException {
        Object result = skill.execute(context);

        assertNotNull(result);
        assertTrue(result instanceof ReviewResult);
    }

    @Test
    @DisplayName("应该返回审查结果")
    public void testReviewResult() throws SkillExecutionException {
        Object result = skill.execute(context);

        ReviewResult reviewResult = (ReviewResult) result;
        assertNotNull(reviewResult.getSummary());
        assertNotNull(reviewResult.getFindings());
    }

    @Test
    @DisplayName("应该验证前置条件")
    public void testValidatePreconditions() {
        assertTrue(skill.validatePreconditions(context));
    }
}
