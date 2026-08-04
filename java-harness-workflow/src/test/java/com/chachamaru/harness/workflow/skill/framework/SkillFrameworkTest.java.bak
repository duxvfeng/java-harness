package com.chachamaru.harness.workflow.skill.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillFramework 测试类
 */
@DisplayName("SkillFramework 测试")
public class SkillFrameworkTest {

    private SkillFramework framework;

    @BeforeEach
    public void setUp() {
        framework = new SkillFramework();
        framework.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (framework != null) {
            framework.close();
        }
    }

    @Test
    @DisplayName("应该成功初始化框架")
    public void testInitialize() {
        SkillFramework newFramework = new SkillFramework();
        assertFalse(newFramework.isInitialized());

        newFramework.initialize();
        assertTrue(newFramework.isInitialized());

        newFramework.close();
    }

    @Test
    @DisplayName("应该注册技能")
    public void testRegisterSkill() {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        assertTrue(framework.isSkillRegistered("test-skill"));
        assertEquals(1, framework.getSkillCount());
    }

    @Test
    @DisplayName("应该执行技能成功")
    public void testExecuteSkillSuccess() throws SkillExecutionException {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        SkillContext context = SkillContext.builder()
                .userIntent("测试意图")
                .projectRoot(Paths.get("."))
                .build();

        SkillResult result = framework.executeSkill("test-skill", context);

        assertNotNull(result);
        assertEquals("test-skill", result.getSkillId());
        assertEquals(SkillResult.SkillStatus.SUCCESS, result.getStatus());
        assertEquals("Test executed successfully", result.getOutput());
    }

    @Test
    @DisplayName("应该抛出异常当技能不存在时")
    public void testExecuteNonExistentSkill() {
        framework.initialize();

        SkillContext context = SkillContext.builder()
                .userIntent("测试意图")
                .projectRoot(Paths.get("."))
                .build();

        assertThrows(SkillNotFoundException.class, () ->
                framework.executeSkill("nonexistent", context)
        );
    }

    @Test
    @DisplayName("应该验证前置条件失败时返回失败结果")
    public void testValidatePreconditionsFailure() throws SkillExecutionException {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        // 创建没有用户意图的上下文
        SkillContext context = SkillContext.builder()
                .userIntent("")  // 空意图
                .projectRoot(Paths.get("."))
                .build();

        SkillResult result = framework.executeSkill("test-skill", context);

        assertNotNull(result);
        assertEquals(SkillResult.SkillStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Preconditions"));
    }

    @Test
    @DisplayName("应该查找已注册的技能")
    public void testFindSkill() {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        assertTrue(framework.findSkill("test-skill").isPresent());
        assertFalse(framework.findSkill("nonexistent").isPresent());
    }

    @Test
    @DisplayName("应该获取所有注册的技能")
    public void testGetRegisteredSkills() {
        TestSkill skill1 = new TestSkill();
        framework.registerSkill(skill1);

        Map<String, SkillRegistry.SkillMetadata> skills = framework.getRegisteredSkills();

        assertNotNull(skills);
        assertEquals(1, skills.size());
        assertTrue(skills.containsKey("test-skill"));
    }

    @Test
    @DisplayName("应该注销技能")
    public void testUnregisterSkill() {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        assertTrue(framework.isSkillRegistered("test-skill"));

        framework.unregisterSkill("test-skill");

        assertFalse(framework.isSkillRegistered("test-skill"));
        assertEquals(0, framework.getSkillCount());
    }

    @Test
    @DisplayName("应该获取技能元数据")
    public void testGetSkillMetadata() {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        var metadata = framework.getSkillMetadata("test-skill");

        assertTrue(metadata.isPresent());
        assertEquals("test-skill", metadata.get().getSkillId());
        assertEquals("Test Skill", metadata.get().getSkillName());
        assertEquals("1.0.0", metadata.get().getVersion());
    }

    @Test
    @DisplayName("应该创建SkillContext Builder模式")
    public void testSkillContextBuilder() {
        SkillContext context = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("."))
                .permissionMode(SkillContext.PermissionMode.DEFAULT)
                .addVariable("var1", "value1")
                .addFile("file1", Paths.get("test.txt"))
                .build();

        assertNotNull(context);
        assertEquals("实现用户认证功能", context.getUserIntent());
        assertEquals(1, context.getVariableCount());
        assertEquals(1, context.getFileCount());
        assertEquals("value1", context.getVariable("var1"));
    }

    @Test
    @DisplayName("应该创建SkillResult Builder模式")
    public void testSkillResultBuilder() {
        SkillResult result = SkillResult.builder()
                .skillId("test-skill")
                .status(SkillResult.SkillStatus.SUCCESS)
                .output("test output")
                .build();

        assertNotNull(result);
        assertEquals("test-skill", result.getSkillId());
        assertEquals(SkillResult.SkillStatus.SUCCESS, result.getStatus());
        assertEquals("test output", result.getOutput());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("应该正确计算执行时间")
    public void testExecutionDuration() throws SkillExecutionException, InterruptedException {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        SkillContext context = SkillContext.builder()
                .userIntent("测试意图")
                .projectRoot(Paths.get("."))
                .build();

        SkillResult result = framework.executeSkill("test-skill", context);

        assertTrue(result.getExecutionDurationMs() >= 0);
        assertTrue(result.getExecutionDurationMs() < 10000); // 应该小于10秒
    }

    @Test
    @DisplayName("应该关闭框架并清理资源")
    public void testClose() {
        TestSkill skill = new TestSkill();
        framework.registerSkill(skill);

        assertEquals(1, framework.getSkillCount());

        framework.close();

        assertEquals(0, framework.getSkillCount());
        assertFalse(framework.isInitialized());
    }
}