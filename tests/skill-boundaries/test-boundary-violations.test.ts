/**
 * Harness 技能边界测试套件
 *
 * 目的: 验证各技能不会越界操作，检测异常行为
 * 覆盖: harness-plan, harness-work, harness-sync 主要边界
 *
 * 测试策略:
 * 1. 静态分析: 检查技能文件中的边界声明
 * 2. 运行时检测: 模拟技能执行并检测违规操作
 * 3. 集成测试: 验证技能间协作不会越界
 */

import { test, expect, describe } from '@testing-framework/core';
import { SkillBoundaryAnalyzer } from '../utils/skill-boundary-analyzer';
import { OperationMonitor } from '../utils/operation-monitor';
import { harnessPlanSkills, harnessWorkSkills } from '../fixtures/skill-definitions';

describe('Harness Plan 边界测试', () => {
  describe('静态边界验证', () => {
    test('应该在 SKILL.md 中包含边界定义章节', async () => {
      const planSkill = await SkillBoundaryAnalyzer.load('harness-plan');

      expect(planSkill.hasBoundarySection()).toBe(true);
      expect(planSkill.hasAllowedOperations()).toBe(true);
      expect(planSkill.hasForbiddenOperations()).toBe(true);
    });

    test('应该明确禁止代码修改操作', async () => {
      const planSkill = await SkillBoundaryAnalyzer.load('harness-plan');
      const forbiddenOps = planSkill.getForbiddenOperations();

      expect(forbiddenOps).toContain('modify_source_code');
      expect(forbiddenOps).toContain('execute_build_commands');
      expect(forbiddenOps).toContain('deep_git_analysis');
    });

    test('应该限制允许操作的文件范围', async () => {
      const planSkill = await SkillBoundaryAnalyzer.load('harness-plan');
      const allowedPatterns = planSkill.getAllowedFilePatterns();

      expect(allowedPatterns).toContain('Plans.md');
      expect(allowedPatterns).toContain('spec.md');
      expect(allowedPatterns).toContain('docs/**/*.md');
      expect(allowedPatterns).not.toContain('src/**');
    });
  });

  describe('运行时边界检测', () => {
    test('执行规划时应该不修改源代码', async () => {
      const monitor = new OperationMonitor();
      const planExecutor = new SkillExecutor('harness-plan');

      await planExecutor.execute({
        task: 'create a new feature',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.codeModifications).toHaveLength(0);
    });

    test('执行规划时应该不执行构建命令', async () => {
      const monitor = new OperationMonitor();
      const planExecutor = new SkillExecutor('harness-plan');

      await planExecutor.execute({
        task: 'plan implementation',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.buildCommands).toHaveLength(0);
    });

    test('执行规划时应该不进行深度 git 分析', async () => {
      const monitor = new OperationMonitor();
      const planExecutor = new SkillExecutor('harness-plan');

      await planExecutor.execute({
        task: 'sync status',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.deepGitAnalysis).toHaveLength(0);
    });
  });

  describe('边界强制执行', () => {
    test('应该阻止修改源代码文件', async () => {
      const planExecutor = new SkillExecutor('harness-plan');

      await expect(
        planExecutor.attemptOperation({
          type: 'Write',
          file: 'src/main/java/com/example/Code.java'
        })
      ).rejects.toThrow('BoundaryViolation');
    });

    test('应该阻止执行构建命令', async () => {
      const planExecutor = new SkillExecutor('harness-plan');

      await expect(
        planExecutor.attemptOperation({
          type: 'Bash',
          command: 'mvn compile'
        })
      ).rejects.toThrow('BoundaryViolation');
    });

    test('应该阻止 git status 深度分析', async () => {
      const planExecutor = new SkillExecutor('harness-plan');

      await expect(
        planExecutor.attemptOperation({
          type: 'Bash',
          command: 'git status --porcelain'
        })
      ).rejects.toThrow('BoundaryViolation');
    });
  });
});

describe('Harness Work 边界测试', () => {
  describe('静态边界验证', () => {
    test('应该在 SKILL.md 中包含职责定义章节', async () => {
      const workSkill = await SkillBoundaryAnalyzer.load('harness-work');

      expect(workSkill.hasRoleDefinition()).toBe(true);
      expect(workSkill.hasAllowedOperations()).toBe(true);
      expect(workSkill.hasForbiddenOperations()).toBe(true);
    });

    test('应该明确允许代码修改操作', async () => {
      const workSkill = await SkillBoundaryAnalyzer.load('harness-work');
      const allowedOps = workSkill.getAllowedOperations();

      expect(allowedOps).toContain('modify_source_code');
      expect(allowedOps).toContain('execute_build_commands');
      expect(allowedOps).toContain('deep_git_analysis');
    });

    test('应该明确禁止规划决策操作', async () => {
      const workSkill = await SkillBoundaryAnalyzer.load('harness-work');
      const forbiddenOps = workSkill.getForbiddenOperations();

      expect(forbiddenOps).toContain('modify_task_definition');
      expect(forbiddenOps).toContain('change_task_priority');
      expect(forbiddenOps).toContain('modify_product_spec');
    });
  });

  describe('运行时边界检测', () => {
    test('执行实现时应该允许修改源代码', async () => {
      const monitor = new OperationMonitor();
      const workExecutor = new SkillExecutor('harness-work');

      await workExecutor.execute({
        task: 'implement feature',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.codeModifications).not.toHaveLength(0); // 允许代码修改
    });

    test('执行实现时应该允许执行构建命令', async () => {
      const monitor = new OperationMonitor();
      const workExecutor = new SkillExecutor('harness-work');

      await workExecutor.execute({
        task: 'build and test',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.buildCommands).not.toHaveLength(0); // 允许构建
    });

    test('执行实现时不应该修改任务定义', async () => {
      const monitor = new OperationMonitor();
      const workExecutor = new SkillExecutor('harness-work');

      await workExecutor.execute({
        task: 'implement task 15.1',
        monitor
      });

      const violations = monitor.detectViolations();
      expect(violations.taskDefinitionModifications).toHaveLength(0);
    });
  });
});

describe('技能协作边界测试', () => {
  test('plan → work 交接应该保持边界清晰', async () => {
    // Step 1: harness-plan 完成规划
    const planExecutor = new SkillExecutor('harness-plan');
    const planResult = await planExecutor.execute({
      task: 'create implementation plan'
    });

    // 验证规划结果不包含实现操作
    expect(planResult.codeModifications).toHaveLength(0);
    expect(planResult.buildCommands).toHaveLength(0);

    // Step 2: harness-work 接收 Plans.md
    const workExecutor = new SkillExecutor('harness-work');
    const workMonitor = new OperationMonitor();

    await workExecutor.execute({
      task: 'implement from plan',
      plan: planResult.plan,
      monitor: workMonitor
    });

    // 验证实现结果不包含规划决策
    const workViolations = workMonitor.detectViolations();
    expect(workViolations.taskDefinitionModifications).toHaveLength(0);
  });

  test('work → sync 交接应该正确处理状态更新', async () => {
    // Step 1: harness-work 完成实现
    const workExecutor = new SkillExecutor('harness-work');
    const workResult = await workExecutor.execute({
      task: 'implement feature'
    });

    // Step 2: harness-sync 进行状态同步
    const syncExecutor = new SkillExecutor('harness-sync');
    const syncMonitor = new OperationMonitor();

    await syncExecutor.execute({
      task: 'sync status',
      implementationState: workResult.state,
      monitor: syncMonitor
    });

    // 验证同步不包含规划或实现决策
    const syncViolations = syncMonitor.detectViolations();
    expect(syncViolations.taskDefinitionModifications).toHaveLength(0);
    expect(syncViolations.codeModifications).toHaveLength(0);
  });
});

describe('边界违规检测系统', () => {
  test('应该检测到规划技能的越界操作', async () => {
    const detector = new BoundaryViolationDetector();
    const violation = await detector.detectSkillViolation({
      skill: 'harness-plan',
      operation: {
        type: 'Write',
        file: 'src/main/java/Code.java'
      }
    });

    expect(violation.detected).toBe(true);
    expect(violation.type).toBe('code_modification');
    expect(violation.severity).toBe('critical');
  });

  test('应该检测到实现技能的越界操作', async () => {
    const detector = new BoundaryViolationDetector();
    const violation = await detector.detectSkillViolation({
      skill: 'harness-work',
      operation: {
        type: 'Edit',
        field: 'task_definition',
        value: 'changed task content'
      }
    });

    expect(violation.detected).toBe(true);
    expect(violation.type).toBe('planning_decision');
    expect(violation.severity).toBe('critical');
  });

  test('应该生成违规报告', async () => {
    const detector = new BoundaryViolationDetector();
    const violations = await detector.scanSession([
      { skill: 'harness-plan', operation: { type: 'Write', file: 'src/Code.java' } },
      { skill: 'harness-work', operation: { type: 'Edit', field: 'task_definition' } }
    ]);

    const report = detector.generateReport(violations);

    expect(report.totalViolations).toBe(2);
    expect(report.criticalViolations).toBe(2);
    expect(report.recommendations).toContain('Review harness-plan boundary violations');
    expect(report.recommendations).toContain('Review harness-work boundary violations');
  });
});

describe('性能和回归测试', () => {
  test('边界检测不应该显著影响性能', async () => {
    const startTime = Date.now();

    const monitor = new OperationMonitor();
    const executor = new SkillExecutor('harness-plan');

    await executor.execute({
      task: 'complex planning task',
      monitor,
      boundaryChecking: true
    });

    const endTime = Date.now();
    const executionTime = endTime - startTime;

    // 边界检查不应该增加超过 20% 的执行时间
    expect(executionTime).toBeLessThan(120000); // 2 分钟
  });

  test('边界检测应该与现有功能兼容', async () => {
    // 验证添加边界检测不会破坏现有技能功能
    const planExecutor = new SkillExecutor('harness-plan');
    const result = await planExecutor.execute({
      task: 'create plan',
      boundaryChecking: true
    });

    expect(result.success).toBe(true);
    expect(result.plan).toBeDefined();
    expect(result.violations).toHaveLength(0);
  });
});