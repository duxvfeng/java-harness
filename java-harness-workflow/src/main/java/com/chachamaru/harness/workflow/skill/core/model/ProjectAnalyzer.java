package com.chachamaru.harness.workflow.skill.core.model;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;

/**
 * 项目分析器接口
 * 用于分析项目结构和上下文
 */
public interface ProjectAnalyzer {

    /**
     * 分析项目上下文
     *
     * @param context 技能上下文
     * @return 项目上下文
     * @throws SkillExecutionException 分析失败
     */
    ProjectContext analyze(SkillContext context) throws SkillExecutionException;
}