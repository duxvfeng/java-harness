package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.*;
import com.chachamaru.harness.workflow.skill.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * PlanSkill - 规划技能
 * 将功能需求转换为结构化任务列表，生成spec.md和Plans.md
 */
public class PlanSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(PlanSkill.class);
    private static final String SKILL_ID = "plan";
    private static final String SKILL_NAME = "Planning Skill";
    private static final String VERSION = "1.0.0-java";

    private final ProjectAnalyzer projectAnalyzer;

    /**
     * 构造函数
     *
     * @param projectAnalyzer 项目分析器（可以为null，使用默认分析器）
     */
    public PlanSkill(ProjectAnalyzer projectAnalyzer) {
        this.projectAnalyzer = projectAnalyzer != null ? projectAnalyzer : new DefaultProjectAnalyzer();
    }

    @Override
    public String getSkillId() {
        return SKILL_ID;
    }

    @Override
    public String getSkillName() {
        return SKILL_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "将功能需求转换为结构化任务列表，生成spec.md和Plans.md";
    }

    @Override
    public Object execute(SkillContext context) throws SkillExecutionException {
        logger.info("Executing Plan skill for intent: {}", context.getUserIntent());

        try {
            // 1. 分析用户意图
            PlanningIntent intent = analyzeIntent(context);

            // 2. 分析项目上下文
            ProjectContext projectContext = projectAnalyzer.analyze(context);

            // 3. 生成规格文档
            SpecDelta specDelta = generateSpecDelta(intent, projectContext);

            // 4. 生成Plans.md
            PlansMd plansMd = generatePlans(specDelta, intent, projectContext);

            // 5. 质量验证
            ValidationResult validation = validatePlanningResult(specDelta, plansMd);

            // 6. 生成交付前确认章节
            PreApprovalSection preApproval = generatePreApprovalSection(plansMd);

            // 7. 构建输出结果
            PlanningOutput output = PlanningOutput.builder()
                    .specDelta(specDelta)
                    .plansMd(plansMd)
                    .preApproval(preApproval)
                    .validation(validation)
                    .build();

            logger.info("Plan skill completed successfully: {} tasks generated", plansMd.getTaskCount());
            return output;

        } catch (Exception e) {
            logger.error("Plan skill execution failed", e);
            throw new SkillExecutionException(SKILL_ID, UUID.randomUUID().toString(),
                    "Planning failed: " + e.getMessage(), e);
        }
    }

    /**
     * 分析用户意图
     */
    private PlanningIntent analyzeIntent(SkillContext context) {
        return PlanningIntent.builder()
                .userIntent(context.getUserIntent())
                .targetGoals(extractGoals(context.getUserIntent()))
                .constraints(extractConstraints(context))
                .acceptanceCriteria(extractAcceptanceCriteria(context))
                .build();
    }

    /**
     * 从用户意图中提取目标
     */
    private List<String> extractGoals(String userIntent) {
        // 简化实现：将用户意图作为目标
        return Arrays.asList(userIntent);
    }

    /**
     * 提取约束条件
     */
    private Constraints extractConstraints(SkillContext context) {
        return Constraints.builder()
                .timeConstraints(Arrays.asList("预计4周内完成"))
                .resourceConstraints(Arrays.asList("需要2名开发人员"))
                .build();
    }

    /**
     * 提取验收标准
     */
    private AcceptanceCriteria extractAcceptanceCriteria(SkillContext context) {
        return AcceptanceCriteria.builder()
                .functionalRequirements(Arrays.asList("功能正常运行"))
                .nonFunctionalRequirements(Arrays.asList("响应时间<2秒"))
                .build();
    }

    /**
     * 生成规格文档增量
     */
    private SpecDelta generateSpecDelta(PlanningIntent intent, ProjectContext projectContext) {
        return SpecDelta.builder()
                .targetSpecPath("spec.md")
                .changeType(SpecDelta.ChangeType.UPDATE)
                .changes(generateSpecChanges(intent))
                .rationale(generateRationale(intent))
                .build();
    }

    /**
     * 生成规格变更列表
     */
    private List<SpecChange> generateSpecChanges(PlanningIntent intent) {
        return Arrays.asList(
                SpecChange.builder()
                        .section("功能需求")
                        .type(SpecDelta.ChangeType.ADD)
                        .content(intent.getUserIntent())
                        .rationale("基于用户需求分析")
                        .build()
        );
    }

    /**
     * 生成基本原理说明
     */
    private String generateRationale(PlanningIntent intent) {
        return "基于用户需求分析: " + intent.getUserIntent();
    }

    /**
     * 生成Plans.md
     */
    private PlansMd generatePlans(SpecDelta specDelta, PlanningIntent intent, ProjectContext projectContext) {
        List<TaskEntry> tasks = generateTasks(specDelta, intent);
        return PlansMd.builder()
                .specReference(specDelta.getTargetSpecPath())
                .tasks(tasks)
                .phases(extractPhases(tasks))
                .build();
    }

    /**
     * 生成任务列表
     */
    private List<TaskEntry> generateTasks(SpecDelta specDelta, PlanningIntent intent) {
        // 简化实现：生成示例任务
        return Arrays.asList(
                TaskEntry.builder()
                        .taskId("1.1")
                        .taskName("需求分析")
                        .content("分析用户需求并明确功能范围")
                        .definitionOfDone("完成需求文档")
                        .dependencies("-")
                        .status(TaskEntry.TaskStatus.TODO)
                        .build(),
                TaskEntry.builder()
                        .taskId("1.2")
                        .taskName("技术设计")
                        .content("设计系统架构和技术方案")
                        .definitionOfDone("完成设计文档")
                        .dependencies("1.1")
                        .status(TaskEntry.TaskStatus.TODO)
                        .build(),
                TaskEntry.builder()
                        .taskId("1.3")
                        .taskName("实现核心功能")
                        .content("实现主要业务逻辑")
                        .definitionOfDone("功能完成并通过测试")
                        .dependencies("1.2")
                        .status(TaskEntry.TaskStatus.TODO)
                        .build()
        );
    }

    /**
     * 提取阶段信息
     */
    private List<String> extractPhases(List<TaskEntry> tasks) {
        return Arrays.asList("第一阶段：规划与设计", "第二阶段：实现与测试");
    }

    /**
     * 验证规划结果
     */
    private ValidationResult validatePlanningResult(SpecDelta specDelta, PlansMd plansMd) {
        return ValidationResult.builder()
                .valid(true)
                .issues(Arrays.asList())
                .warnings(Arrays.asList())
                .build();
    }

    /**
     * 生成交付前确认章节
     */
    private PreApprovalSection generatePreApprovalSection(PlansMd plansMd) {
        return PreApprovalSection.builder()
                .items(Arrays.asList(
                        new PreApprovalSection.ApprovalItem("确认任务列表完整性", false),
                        new PreApprovalSection.ApprovalItem("确认技术方案可行性", false),
                        new PreApprovalSection.ApprovalItem("确认时间计划合理性", false)
                ))
                .build();
    }

    /**
     * 默认项目分析器
     */
    private static class DefaultProjectAnalyzer implements ProjectAnalyzer {
        @Override
        public ProjectContext analyze(SkillContext context) throws SkillExecutionException {
            return new ProjectContext(
                    context.getProjectRoot(),
                    "JavaHarness",
                    java.util.Map.of("analyzedAt", java.time.Instant.now().toString())
            );
        }
    }
}