package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.*;
import com.chachamaru.harness.workflow.skill.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlanSkill 测试类
 */
@DisplayName("PlanSkill 测试")
public class PlanSkillTest {

    private PlanSkill skill;

    @BeforeEach
    public void setUp() {
        skill = new PlanSkill(null); // 使用默认分析器
    }

    @Test
    @DisplayName("应该生成Plans.md")
    public void testGeneratePlansMd() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        assertNotNull(result);
        assertTrue(result instanceof PlanningOutput);

        PlanningOutput output = (PlanningOutput) result;
        assertNotNull(output.getPlansMd());
        assertTrue(output.getPlansMd().getTaskCount() > 0);
    }

    @Test
    @DisplayName("应该生成SpecDelta")
    public void testGenerateSpecDelta() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        assertNotNull(result);
        PlanningOutput output = (PlanningOutput) result;
        assertNotNull(output.getSpecDelta());
        assertEquals("spec.md", output.getSpecDelta().getTargetSpecPath());
    }

    @Test
    @DisplayName("应该包含任务依赖关系")
    public void testTaskDependencies() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        assertTrue(output.getPlansMd().getTasks().stream()
                .anyMatch(task -> task.getDependencies() != null && !task.getDependencies().equals("-")));
    }

    @Test
    @DisplayName("应该生成交付前确认章节")
    public void testPreApprovalSection() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        assertNotNull(output.getPreApproval());
        assertTrue(output.getPreApproval().getItemCount() > 0);
    }

    @Test
    @DisplayName("应该验证规划结果")
    public void testValidationResult() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        assertNotNull(output.getValidation());
        assertTrue(output.getValidation().isValid());
    }

    @Test
    @DisplayName("应该返回正确的技能信息")
    public void testSkillInfo() {
        assertEquals("plan", skill.getSkillId());
        assertEquals("Planning Skill", skill.getSkillName());
        assertEquals("1.0.0-java", skill.getVersion());
        assertNotNull(skill.getDescription());
    }

    @Test
    @DisplayName("应该处理空用户意图")
    public void testEmptyUserIntent() {
        SkillContext context = SkillContext.builder()
                .userIntent("")
                .projectRoot(Paths.get("."))
                .build();

        assertThrows(SkillExecutionException.class, () -> skill.execute(context));
    }

    @Test
    @DisplayName("应该生成至少3个任务")
    public void testMinimumTaskCount() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        assertTrue(output.getPlansMd().getTaskCount() >= 3,
                "Should generate at least 3 tasks, but got: " + output.getPlansMd().getTaskCount());
    }

    @Test
    @DisplayName("应该包含阶段信息")
    public void testPhasesInformation() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        assertNotNull(output.getPlansMd().getPhases());
        assertTrue(output.getPlansMd().getPhases().size() > 0);
    }

    @Test
    @DisplayName("应该生成有效的TaskEntry")
    public void testTaskEntryStructure() throws SkillExecutionException {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .build();

        Object result = skill.execute(context);

        PlanningOutput output = (PlanningOutput) result;
        TaskEntry firstTask = output.getPlansMd().getTasks().get(0);

        assertNotNull(firstTask.getTaskId());
        assertNotNull(firstTask.getTaskName());
        assertNotNull(firstTask.getContent());
        assertNotNull(firstTask.getDefinitionOfDone());
        assertNotNull(firstTask.getStatus());
    }
}