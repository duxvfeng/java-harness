package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentContext 测试")
public class AgentContextTest {

    @Test
    @DisplayName("应该创建 AgentContext")
    public void testCreateAgentContext() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        SkillContext skillContext = SkillContext.builder()
                .userIntent("实现用户认证")
                .projectRoot(Paths.get("/project"))
                .build();

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .skillContext(skillContext)
                .skillFramework(mockSkillFramework)
                .build();

        assertEquals("task-001", context.getTaskId());
        assertEquals("实现用户认证", context.getUserIntent());
        assertEquals(mockSkillFramework, context.getSkillFramework());
    }

    @Test
    @DisplayName("应该支持共享状态")
    public void testSharedState() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        SkillContext skillContext = SkillContext.builder()
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .build();

        Map<String, Object> sharedState = new HashMap<>();
        sharedState.put("plan", "计划内容");

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .skillContext(skillContext)
                .skillFramework(mockSkillFramework)
                .sharedState(sharedState)
                .build();

        assertEquals("计划内容", context.getSharedState("plan"));
    }

    @Test
    @DisplayName("应该继承 SkillContext 的字段")
    public void testInheritsFromSkillContext() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        SkillContext skillContext = SkillContext.builder()
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .build();

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .skillContext(skillContext)
                .skillFramework(mockSkillFramework)
                .build();

        assertEquals("测试", context.getUserIntent());
        assertEquals(Paths.get("/project"), context.getProjectRoot());
    }

    @Test
    @DisplayName("应该提供调用 Skill 的便捷方法")
    public void testCallSkillMethod() {
        SkillFramework mockSkillFramework = mock(SkillFramework.class);
        SkillContext skillContext = SkillContext.builder()
                .userIntent("测试")
                .projectRoot(Paths.get("/project"))
                .build();

        AgentContext context = AgentContext.builder()
                .taskId("task-001")
                .skillContext(skillContext)
                .skillFramework(mockSkillFramework)
                .build();

        context.callSkill("plan");

        verify(mockSkillFramework).executeSkill(eq("plan"), any());
    }
}
