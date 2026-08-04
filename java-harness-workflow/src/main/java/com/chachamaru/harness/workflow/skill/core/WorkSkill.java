package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.Skill;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作技能
 * 职责：执行具体的工作任务
 */
public class WorkSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(WorkSkill.class);

    @Override
    public String getSkillId() {
        return "work";
    }

    @Override
    public String getSkillName() {
        return "Work Skill";
    }

    @Override
    public String getVersion() {
        return "1.0.0-java";
    }

    @Override
    public String getDescription() {
        return "执行具体的工作任务，实现功能需求";
    }

    @Override
    public Object execute(SkillContext context) throws SkillExecutionException {
        logger.info("WorkSkill executing: {}", context.getUserIntent());
        Instant startTime = Instant.now();

        try {
            // 分析用户意图
            String userIntent = context.getUserIntent();
            logger.info("Analyzing user intent: {}", userIntent);

            // 生成工作任务列表
            List<String> tasks = generateWorkTasks(userIntent, context);

            // 执行工作任务（阶段1：模拟执行）
            List<String> completedTasks = executeTasks(tasks);

            // 构建结果
            WorkResult.Builder resultBuilder = WorkResult.builder()
                    .success(true)
                    .message("工作完成")
                    .output(completedTasks);

            for (String task : completedTasks) {
                resultBuilder.addCompletedTask(task);
            }

            WorkResult result = resultBuilder
                    .completedTime(Instant.now())
                    .build();

            logger.info("WorkSkill completed: {} tasks", completedTasks.size());
            return result;

        } catch (Exception e) {
            logger.error("WorkSkill execution failed", e);

            return WorkResult.builder()
                    .success(false)
                    .message("工作执行失败: " + e.getMessage())
                    .completedTime(Instant.now())
                    .build();
        }
    }

    @Override
    public boolean validatePreconditions(SkillContext context) {
        // 检查是否有用户意图
        if (context.getUserIntent() == null || context.getUserIntent().isEmpty()) {
            logger.warn("No user intent provided");
            return false;
        }

        // 检查是否有项目根目录
        if (context.getProjectRoot() == null) {
            logger.warn("No project root provided");
            return false;
        }

        return true;
    }

    /**
     * 生成工作任务列表
     */
    private List<String> generateWorkTasks(String userIntent, SkillContext context) {
        List<String> tasks = new ArrayList<>();

        // 阶段1：简单实现，根据用户意图生成通用任务
        if (userIntent.contains("认证") || userIntent.contains("登录")) {
            tasks.add("设计用户表结构");
            tasks.add("实现登录API");
            tasks.add("实现注册API");
            tasks.add("添加JWT认证");
        } else if (userIntent.contains("数据库") || userIntent.contains("存储")) {
            tasks.add("设计数据库模型");
            tasks.add("创建迁移脚本");
            tasks.add("实现数据访问层");
        } else {
            // 通用任务
            tasks.add("分析需求");
            tasks.add("设计方案");
            tasks.add("实现功能");
            tasks.add("编写测试");
        }

        logger.info("Generated {} work tasks", tasks.size());
        return tasks;
    }

    /**
     * 执行工作任务（阶段1：模拟执行）
     */
    private List<String> executeTasks(List<String> tasks) {
        List<String> completed = new ArrayList<>();

        for (String task : tasks) {
            // 阶段1：模拟执行，直接标记为完成
            logger.info("Executing task: {}", task);
            completed.add(task);
        }

        return completed;
    }
}
