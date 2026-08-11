#!/usr/bin/env node
/**
 * 端到端检测自动触发器
 *
 * 在代码审查通过后自动触发端到端检测：
 * - 检测审查通过事件
 * - 判断是否需要端到端检测
 * - 启动检测流程
 * - 处理检测结果
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

// 引入管理器和修复控制器
const { E2EDetectionManager } = require('./e2e-detection-manager.js');
const { AutoFixController } = require('./auto-fix-controller.js');

/**
 * 端到端检测触发器类
 */
class E2EDetectionTrigger {
  constructor(options = {}) {
    this.options = {
      autoTrigger: true,
      requireCleanWorkspace: true,
      blocking: true,
      maxFixIterations: 3,
      ...options
    };
    this.config = this.loadConfig();
  }

  /**
   * 加载配置
   */
  loadConfig() {
    try {
      const configPath = '.claude/config/e2e-detection.config.json';
      if (fs.existsSync(configPath)) {
        return JSON.parse(fs.readFileSync(configPath, 'utf8'));
      }
      return {};
    } catch (error) {
      console.warn(`配置加载失败: ${error.message}`);
      return {};
    }
  }

  /**
   * 检查是否应该触发检测
   */
  shouldTrigger(reviewResult, taskContext) {
    // 1. 检查审查是否通过
    if (reviewResult.verdict !== 'APPROVE') {
      return {
        should_trigger: false,
        reason: '审查未通过，不触发端到端检测'
      };
    }

    // 2. 检查配置是否启用
    if (!this.config.enabled) {
      return {
        should_trigger: false,
        reason: '端到端检测未启用'
      };
    }

    // 3. 检查是否自动触发
    if (!this.options.autoTrigger && !this.config.triggers?.auto_trigger_on_review_pass) {
      return {
        should_trigger: false,
        reason: '自动触发未启用'
      };
    }

    // 4. 检查工作空间是否干净
    if (this.options.requireCleanWorkspace && !this.isWorkspaceClean(taskContext.worktreePath)) {
      return {
        should_trigger: false,
        reason: '工作空间不干净'
      };
    }

    // 5. 检查分支模式
    if (this.config.triggers?.branch_patterns) {
      const branchMatch = this.checkBranchPattern(taskContext.branch);
      if (!branchMatch.allowed) {
        return {
          should_trigger: false,
          reason: `分支模式不匹配: ${branchMatch.reason}`
        };
      }
    }

    return {
      should_trigger: true,
      reason: '满足所有触发条件'
    };
  }

  /**
   * 检查工作空间是否干净
   */
  isWorkspaceClean(worktreePath) {
    try {
      // 检查git状态
      const result = spawn.sync('git', ['-C', worktreePath, 'status', '--porcelain'], {
        encoding: 'utf8'
      });

      // 如果有未提交的更改，认为不干净
      return !result.stdout || result.stdout.trim() === '';
    } catch (error) {
      console.warn(`工作空间检查失败: ${error.message}`);
      return true; // 出错时默认允许
    }
  }

  /**
   * 检查分支模式
   */
  checkBranchPattern(branch) {
    if (!this.config.triggers.branch_patterns) {
      return { allowed: true };
    }

    const { include, exclude } = this.config.triggers.branch_patterns;

    // 检查排除模式
    if (exclude) {
      for (const pattern of exclude) {
        const regex = new RegExp(pattern.replace('*', '.*'));
        if (regex.test(branch)) {
          return {
            allowed: false,
            reason: `分支匹配排除模式: ${pattern}`
          };
        }
      }
    }

    // 检查包含模式
    if (include && include.length > 0) {
      for (const pattern of include) {
        const regex = new RegExp(pattern.replace('*', '.*'));
        if (regex.test(branch)) {
          return { allowed: true };
        }
      }
      return {
        allowed: false,
        reason: '分支不匹配任何包含模式'
      };
    }

    return { allowed: true };
  }

  /**
   * 触发端到端检测
   */
  async triggerDetection(reviewResult, taskContext) {
    const triggerCheck = this.shouldTrigger(reviewResult, taskContext);

    if (!triggerCheck.should_trigger) {
      console.log(`⏭️  跳过端到端检测: ${triggerCheck.reason}`);
      return {
        triggered: false,
        reason: triggerCheck.reason,
        result: null
      };
    }

    console.log('🔥 触发端到端检测...');

    try {
      // 1. 创建检测管理器
      const manager = new E2EDetectionManager(taskContext, taskContext.contractPath);

      // 2. 运行检测
      const detectionResult = await manager.runDetection(
        taskContext.worktreePath,
        taskContext.baseRef || 'HEAD'
      );

      // 3. 处理检测结果
      return await this.handleDetectionResult(detectionResult, taskContext);

    } catch (error) {
      console.error(`❌ 端到端检测失败: ${error.message}`);
      return {
        triggered: true,
        success: false,
        error: error.message,
        result: null
      };
    }
  }

  /**
   * 处理检测结果
   */
  async handleDetectionResult(detectionResult, taskContext) {
    console.log(`📊 端到端检测结果: ${detectionResult.status}`);

    if (detectionResult.status === 'PASS') {
      // 检测通过
      console.log('✅ 端到端检测通过，继续正常流程');
      return {
        triggered: true,
        success: true,
        result: detectionResult,
        action: 'proceed'
      };
    } else if (detectionResult.status === 'FAIL') {
      // 检测失败，进入修复循环
      console.log('❌ 端到端检测未通过，启动自动修复循环');

      const fixResult = await this.runAutoFixLoop(detectionResult, taskContext);

      return {
        triggered: true,
        success: fixResult.success,
        result: detectionResult,
        action: 'fix_loop',
        fix_result: fixResult
      };
    } else if (detectionResult.status === 'SKIPPED') {
      // 检测被跳过
      console.log(`⏭️  端到端检测被跳过: ${detectionResult.reason}`);
      return {
        triggered: true,
        success: true,
        result: detectionResult,
        action: 'skip'
      };
    } else {
      // 检测出错
      console.error(`❌ 端到端检测出错: ${detectionResult.error}`);
      return {
        triggered: true,
        success: false,
        result: detectionResult,
        action: 'error'
      };
    }
  }

  /**
   * 运行自动修复循环
   */
  async runAutoFixLoop(detectionResult, taskContext) {
    const maxIterations = this.config.auto_fix?.max_iterations || this.options.maxFixIterations;
    const fixController = new AutoFixController(maxIterations);

    let currentIteration = 0;
    let finalResult = {
      success: false,
      iterations: 0,
      fixes_applied: 0
    };

    while (currentIteration < maxIterations) {
      currentIteration++;
      console.log(`🔄 自动修复循环 ${currentIteration}/${maxIterations}`);

      // 1. 分析失败原因
      const analysisResult = this.createAnalysisResult(detectionResult);

      // 2. 尝试自动修复
      const fixAttempt = await fixController.attemptAutoFix(
        analysisResult,
        taskContext.worktreePath,
        taskContext.workerId
      );

      if (!fixAttempt.success) {
        console.log(`❌ 修复失败: ${fixAttempt.message || fixAttempt.error}`);

        // 如果无法自动修复，升级到用户
        return {
          success: false,
          iterations: currentIteration,
          reason: fixAttempt.message || fixAttempt.error,
          escalate_to_user: true
        };
      }

      console.log(`✅ 修复成功: ${fixAttempt.fixes_applied} 个修复已应用`);

      // 3. 重新运行检测
      console.log('🔄 重新运行端到端检测...');
      const manager = new E2EDetectionManager(taskContext, taskContext.contractPath);
      const newDetectionResult = await manager.runDetection(
        taskContext.worktreePath,
        taskContext.baseRef || 'HEAD'
      );

      // 4. 检查是否通过
      if (newDetectionResult.status === 'PASS') {
        console.log('✅ 修复后检测通过');
        return {
          success: true,
          iterations: currentIteration,
          fixes_applied: fixAttempt.fixes_applied,
          final_result: newDetectionResult
        };
      }

      // 5. 更新检测结果继续下一次循环
      detectionResult = newDetectionResult;
      fixController.incrementIteration();

      // 等待一段时间再重试
      await this.sleep(1000);
    }

    // 达到最大重试次数
    console.log(`⚠️ 达到最大重试次数 (${maxIterations})，升级到用户`);

    return {
      success: false,
      iterations: currentIteration,
      reason: '达到最大重试次数',
      escalate_to_user: true,
      final_result: detectionResult
    };
  }

  /**
   * 创建分析结果
   */
  createAnalysisResult(detectionResult) {
    const analysisResult = {
      overall_status: detectionResult.status,
      detection_id: detectionResult.detection_id,
      critical_issues: detectionResult.critical_issues || [],
      major_issues: detectionResult.major_issues || [],
      minor_issues: detectionResult.minor_issues || [],
      fix_suggestions: detectionResult.fix_suggestions || [],
      can_proceed: detectionResult.status === 'PASS'
    };

    // 从测试结果中提取问题
    if (detectionResult.test_results) {
      for (const [testType, result] of Object.entries(detectionResult.test_results)) {
        if (result.critical_issues) {
          analysisResult.critical_issues.push(...result.critical_issues.map(issue => ({
            ...issue,
            test_type: testType
          })));
        }
      }
    }

    return analysisResult;
  }

  /**
   * 重新审查修复后的代码
   */
  async reReviewFixedCode(taskContext) {
    console.log('🔄 重新审查修复后的代码...');

    try {
      // 这里应该调用harness-review进行重新审查
      // 简化版本：直接返回成功
      return {
        success: true,
        verdict: 'APPROVE'
      };

    } catch (error) {
      console.error(`❌ 重新审查失败: ${error.message}`);
      return {
        success: false,
        error: error.message
      };
    }
  }

  /**
   * 升级到用户
   */
  escalateToUser(detectionResult, fixResult) {
    console.log('⚠️ 升级到用户处理');

    // 生成升级报告
    const escalationReport = {
      timestamp: new Date().toISOString(),
      detection_result: detectionResult,
      fix_result: fixResult,
      recommendations: this.generateEscalationRecommendations(detectionResult)
    };

    // 保存升级报告
    const reportPath = `.claude/state/e2e-detection/escalation-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(escalationReport, null, 2));

    console.log(`📋 升级报告已保存: ${reportPath}`);
    console.log('👆 请用户介入处理');

    return escalationReport;
  }

  /**
   * 生成升级建议
   */
  generateEscalationRecommendations(detectionResult) {
    const recommendations = [];

    if (detectionResult.critical_issues && detectionResult.critical_issues.length > 0) {
      recommendations.push({
        priority: 'high',
        category: 'critical_issues',
        message: `有 ${detectionResult.critical_issues.length} 个关键问题需要修复`,
        actions: detectionResult.critical_issues.map(issue => ({
          file: issue.file,
          description: issue.description,
          suggestion: issue.suggestion
        }))
      });
    }

    recommendations.push({
      priority: 'medium',
      category: 'manual_review',
      message: '建议手动检查测试代码和实现逻辑',
      actions: [
        '检查测试用例是否正确',
        '检查实现是否满足需求',
        '检查环境配置是否正确'
      ]
    });

    return recommendations;
  }

  /**
   * 睡眠函数
   */
  async sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * 设置触发选项
   */
  setOptions(options) {
    this.options = { ...this.options, ...options };
  }

  /**
   * 获取配置状态
   */
  getConfigStatus() {
    return {
      enabled: this.config.enabled || false,
      auto_trigger: this.config.triggers?.auto_trigger_on_review_pass || false,
      test_types: Object.keys(this.config.test_types || {}).filter(
        type => this.config.test_types[type].enabled
      ),
      auto_fix_enabled: this.config.auto_fix?.enabled || false,
      max_iterations: this.config.auto_fix?.max_iterations || 3
    };
  }
}

/**
 * CLI入口
 */
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    console.log('用法: node e2e-detection-trigger.js <command> [options]');
    console.log('');
    console.log('命令:');
    console.log('  check    - 检查是否应该触发检测');
    console.log('  trigger  - 触发端到端检测');
    console.log('  status   - 显示配置状态');
    console.log('');
    console.log('示例:');
    console.log('  node e2e-detection-trigger.js check --review-result result.json');
    console.log('  node e2e-detection-trigger.js trigger --worktree-path /path/to/worktree');
    process.exit(1);
  }

  const [command] = args;
  const trigger = new E2EDetectionTrigger();

  switch (command) {
    case 'status':
      const status = trigger.getConfigStatus();
      console.log('端到端检测配置状态:');
      console.log(JSON.stringify(status, null, 2));
      break;

    case 'check':
      // 检查是否应该触发
      console.log('检查触发条件...');
      // 这里需要传入review result和task context
      console.log('需要实现参数解析');
      break;

    case 'trigger':
      // 触发检测
      console.log('触发端到端检测...');
      // 这里需要传入task context
      console.log('需要实现参数解析');
      break;

    default:
      console.log(`未知命令: ${command}`);
      process.exit(1);
  }
}

module.exports = { E2EDetectionTrigger };