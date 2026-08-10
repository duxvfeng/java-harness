#!/usr/bin/env node
/**
 * 自动修复控制器
 *
 * 负责处理端到端检测失败后的自动修复流程：
 * - 生成修复命令
 * - 执行修复操作
 * - 提交修复
 * - 管理重试逻辑
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

/**
 * 自动修复控制器类
 */
class AutoFixController {
  constructor(maxIterations = 3) {
    this.maxIterations = maxIterations;
    this.currentIteration = 0;
    this.fixHistory = [];
  }

  /**
   * 尝试自动修复
   */
  async attemptAutoFix(analysisResult, worktreePath, workerId = null) {
    console.log(`🔧 开始自动修复尝试 ${this.currentIteration + 1}/${this.maxIterations}`);

    if (analysisResult.overall_status === 'PASS') {
      return {
        success: true,
        message: '无需修复',
        iteration: this.currentIteration
      };
    }

    if (!analysisResult.critical_issues || analysisResult.critical_issues.length === 0) {
      return {
        success: false,
        message: '无法自动修复：无关键问题',
        iteration: this.currentIteration
      };
    }

    try {
      // 1. 生成修复命令
      console.log('📋 生成修复命令...');
      const fixCommands = this.generateFixCommands(analysisResult.critical_issues);

      if (fixCommands.length === 0) {
        return {
          success: false,
          message: '无法生成修复命令',
          iteration: this.currentIteration
        };
      }

      // 2. 执行修复
      console.log(`⚡ 执行 ${fixCommands.length} 个修复命令...`);
      const executeResult = await this.executeFixes(fixCommands, worktreePath);

      if (!executeResult.success) {
        return {
          success: false,
          error: executeResult.error,
          iteration: this.currentIteration
        };
      }

      // 3. 提交修复
      console.log('💾 提交修复...');
      const commitResult = await this.commitFixes(worktreePath, analysisResult.critical_issues);

      if (!commitResult.success) {
        return {
          success: false,
          error: commitResult.error,
          iteration: this.currentIteration
        };
      }

      // 4. 记录修复历史
      this.fixHistory.push({
        iteration: this.currentIteration,
        fixes_applied: fixCommands.length,
        commit_hash: commitResult.commit_hash,
        timestamp: new Date().toISOString()
      });

      console.log(`✅ 修复完成: ${commitResult.commit_hash}`);

      return {
        success: true,
        fixes_applied: fixCommands.length,
        commit_hash: commitResult.commit_hash,
        iteration: this.currentIteration,
        fix_summary: this.generateFixSummary(analysisResult.critical_issues)
      };

    } catch (error) {
      console.error(`❌ 修复失败: ${error.message}`);
      return {
        success: false,
        error: error.message,
        iteration: this.currentIteration
      };
    }
  }

  /**
   * 生成修复命令
   */
  generateFixCommands(criticalIssues) {
    const fixCommands = [];

    for (const issue of criticalIssues) {
      const commands = this.generateIssueFixCommands(issue);
      fixCommands.push(...commands);
    }

    return fixCommands;
  }

  /**
   * 为特定问题生成修复命令
   */
  generateIssueFixCommands(issue) {
    const commands = [];

    switch (issue.test_type) {
      case 'frontend':
        commands.push(...this.generateFrontendFix(issue));
        break;
      case 'backend':
        commands.push(...this.generateBackendFix(issue));
        break;
      case 'integration':
        commands.push(...this.generateIntegrationFix(issue));
        break;
      case 'performance':
        commands.push(...this.generatePerformanceFix(issue));
        break;
      case 'security':
        commands.push(...this.generateSecurityFix(issue));
        break;
      default:
        // 通用修复
        commands.push(...this.generateGenericFix(issue));
    }

    return commands;
  }

  /**
   * 生成前端修复命令
   */
  generateFrontendFix(issue) {
    const commands = [];

    if (issue.description.includes('未找到')) {
      // 可能是缺少文件或组件
      commands.push({
        type: 'create_file',
        description: `创建缺失的前端组件: ${issue.description}`,
        command: this.generateCreateComponentCommand(issue),
        priority: 'high'
      });
    } else if (issue.description.includes('测试失败')) {
      // 测试失败，需要修复代码
      commands.push({
        type: 'fix_test',
        description: `修复前端测试: ${issue.description}`,
        command: this.generateFixTestCommand(issue),
        priority: 'high'
      });
    } else if (issue.description.includes('登录') || issue.description.includes('表单')) {
      // UI相关问题
      commands.push({
        type: 'fix_ui',
        description: `修复UI问题: ${issue.description}`,
        command: this.generateFixUICommand(issue),
        priority: 'medium'
      });
    }

    return commands;
  }

  /**
   * 生成后端修复命令
   */
  generateBackendFix(issue) {
    const commands = [];

    if (issue.description.includes('API') || issue.description.includes('端点')) {
      // API相关问题
      commands.push({
        type: 'fix_api',
        description: `修复API端点: ${issue.description}`,
        command: this.generateFixAPICommand(issue),
        priority: 'high'
      });
    } else if (issue.description.includes('数据库') || issue.description.includes('查询')) {
      // 数据库相关问题
      commands.push({
        type: 'fix_database',
        description: `修复数据库问题: ${issue.description}`,
        command: this.generateFixDatabaseCommand(issue),
        priority: 'high'
      });
    } else if (issue.description.includes('测试失败')) {
      // 单元测试失败
      commands.push({
        type: 'fix_unit_test',
        description: `修复单元测试: ${issue.description}`,
        command: this.generateFixUnitTestCommand(issue),
        priority: 'medium'
      });
    }

    return commands;
  }

  /**
   * 生成集成测试修复命令
   */
  generateIntegrationFix(issue) {
    const commands = [];

    if (issue.description.includes('登录') || issue.scenario === 'user_login') {
      commands.push({
        type: 'fix_integration_login',
        description: '修复登录集成问题',
        command: '请检查前后端登录流程是否一致',
        priority: 'high'
      });
    } else if (issue.description.includes('数据流') || issue.scenario === 'data_flow') {
      commands.push({
        type: 'fix_integration_data',
        description: '修复数据流转问题',
        command: '请检查前后端数据格式和传输是否正确',
        priority: 'high'
      });
    } else if (issue.description.includes('错误处理') || issue.scenario === 'error_handling') {
      commands.push({
        type: 'fix_integration_error',
        description: '修复错误处理问题',
        command: '请检查异常情况的前后端处理是否完善',
        priority: 'medium'
      });
    }

    return commands;
  }

  /**
   * 生成性能优化修复命令
   */
  generatePerformanceFix(issue) {
    const commands = [];

    if (issue.description.includes('响应时间')) {
      commands.push({
        type: 'optimize_performance',
        description: '优化响应时间',
        command: '请检查代码性能，考虑缓存、数据库查询优化等',
        priority: 'medium'
      });
    } else if (issue.description.includes('内存') || issue.description.includes('CPU')) {
      commands.push({
        type: 'optimize_resource',
        description: '优化资源使用',
        command: '请检查内存泄漏、CPU密集操作等',
        priority: 'medium'
      });
    }

    return commands;
  }

  /**
   * 生成安全修复命令
   */
  generateSecurityFix(issue) {
    const commands = [];

    if (issue.description.includes('漏洞') || issue.description.includes('依赖')) {
      // 依赖包漏洞
      commands.push({
        type: 'update_dependency',
        description: '更新依赖包到安全版本',
        command: this.generateUpdateDependencyCommand(issue),
        priority: 'critical'
      });
    } else if (issue.description.includes('敏感文件')) {
      // 敏感文件暴露
      commands.push({
        type: 'protect_sensitive',
        description: '保护敏感文件',
        command: this.generateProtectSensitiveCommand(issue),
        priority: 'critical'
      });
    } else if (issue.description.includes('注入') || issue.description.includes('XSS')) {
      // 安全漏洞
      commands.push({
        type: 'fix_security_vulnerability',
        description: '修复安全漏洞',
        command: '请检查输入验证、输出编码等安全措施',
        priority: 'critical'
      });
    }

    return commands;
  }

  /**
   * 生成通用修复命令
   */
  generateGenericFix(issue) {
    return [{
      type: 'generic_fix',
      description: `修复问题: ${issue.description}`,
      command: issue.suggestion || '请检查相关代码并修复',
      priority: 'medium'
    }];
  }

  /**
   * 生成具体修复命令
   */
  generateCreateComponentCommand(issue) {
    // 这里可以根据issue信息生成具体的创建组件命令
    // 简化版本：返回建议
    return `建议创建组件: ${issue.file}`;
  }

  generateFixTestCommand(issue) {
    return `建议修复测试: ${issue.file}:${issue.line}`;
  }

  generateFixUICommand(issue) {
    return `建议修复UI: ${issue.file}`;
  }

  generateFixAPICommand(issue) {
    return `建议修复API: ${issue.file}`;
  }

  generateFixDatabaseCommand(issue) {
    return `建议修复数据库操作: ${issue.file}`;
  }

  generateFixUnitTestCommand(issue) {
    return `建议修复单元测试: ${issue.file}`;
  }

  generateUpdateDependencyCommand(issue) {
    // 根据不同包管理器生成命令
    return '建议运行依赖更新命令';
  }

  generateProtectSensitiveCommand(issue) {
    return `建议将 ${issue.file} 添加到 .gitignore`;
  }

  /**
   * 执行修复命令
   */
  async executeFixes(fixCommands, worktreePath) {
    let executedCount = 0;
    let successCount = 0;

    for (const fix of fixCommands) {
      console.log(`🔧 执行修复: ${fix.description}`);

      try {
        // 这里应该根据不同类型的修复执行不同的操作
        // 简化版本：记录修复建议，实际需要人工或AI处理

        const result = await this.applyFix(fix, worktreePath);

        if (result.success) {
          successCount++;
          console.log(`✅ 修复成功: ${fix.description}`);
        } else {
          console.log(`⚠️ 修复跳过: ${fix.description} - ${result.reason}`);
        }

        executedCount++;

      } catch (error) {
        console.error(`❌ 修复失败: ${fix.description} - ${error.message}`);
      }
    }

    console.log(`📊 修复执行完成: ${successCount}/${executedCount} 成功`);

    return {
      success: successCount > 0,
      executed: executedCount,
      succeeded: successCount
    };
  }

  /**
   * 应用单个修复
   */
  async applyFix(fix, worktreePath) {
    // 这里应该实现具体的修复逻辑
    // 目前简化为返回成功，实际需要根据fix类型处理

    switch (fix.type) {
      case 'update_dependency':
        return await this.applyDependencyUpdate(fix, worktreePath);
      case 'protect_sensitive':
        return await this.applySensitiveFileProtection(fix, worktreePath);
      case 'fix_test':
      case 'fix_api':
      case 'fix_ui':
        // 这些需要AI介入或人工处理
        return {
          success: false,
          reason: '需要人工或AI处理'
        };
      default:
        return {
          success: false,
          reason: '未实现的修复类型'
        };
    }
  }

  /**
   * 应用依赖更新
   */
  async applyDependencyUpdate(fix, worktreePath) {
    try {
      // 检测包管理器并执行更新
      const packageManager = this.detectPackageManager(worktreePath);

      if (packageManager === 'npm') {
        await this.runCommand('npm audit fix', worktreePath);
        return { success: true };
      } else if (packageManager === 'yarn') {
        await this.runCommand('yarn upgrade', worktreePath);
        return { success: true };
      }

      return {
        success: false,
        reason: '未检测到包管理器'
      };

    } catch (error) {
      return {
        success: false,
        reason: error.message
      };
    }
  }

  /**
   * 应用敏感文件保护
   */
  async applySensitiveFileProtection(fix, worktreePath) {
    try {
      const gitignorePath = path.join(worktreePath, '.gitignore');
      let gitignoreContent = '';

      if (fs.existsSync(gitignorePath)) {
        gitignoreContent = fs.readFileSync(gitignorePath, 'utf8');
      }

      // 添加敏感文件到gitignore
      const sensitiveFile = path.basename(fix.file);
      if (!gitignoreContent.includes(sensitiveFile)) {
        gitignoreContent += `\n# E2E检测添加\n${sensitiveFile}\n`;
        fs.writeFileSync(gitignorePath, gitignoreContent);
        return { success: true };
      }

      return {
        success: false,
        reason: '文件已在gitignore中'
      };

    } catch (error) {
      return {
        success: false,
        reason: error.message
      };
    }
  }

  /**
   * 检测包管理器
   */
  detectPackageManager(worktreePath) {
    if (fs.existsSync(path.join(worktreePath, 'package-lock.json'))) {
      return 'npm';
    } else if (fs.existsSync(path.join(worktreePath, 'yarn.lock'))) {
      return 'yarn';
    } else if (fs.existsSync(path.join(worktreePath, 'package.json'))) {
      return 'npm'; // 默认npm
    }

    return null;
  }

  /**
   * 提交修复
   */
  async commitFixes(worktreePath, criticalIssues) {
    try {
      // 1. 检查是否有变更
      const statusResult = await this.runCommand('git status --porcelain', worktreePath);
      if (!statusResult.stdout || statusResult.stdout.trim() === '') {
        return {
          success: false,
          error: '没有变更需要提交'
        };
      }

      // 2. 添加所有变更
      await this.runCommand('git add -A', worktreePath);

      // 3. 生成提交信息
      const commitMessage = this.generateCommitMessage(criticalIssues);

      // 4. 提交
      const commitResult = await this.runCommand(
        `git commit -m "${commitMessage}"`,
        worktreePath
      );

      if (commitResult.exitCode !== 0) {
        return {
          success: false,
          error: commitResult.stderr
        };
      }

      // 5. 获取commit hash
      const hashResult = await this.runCommand('git rev-parse HEAD', worktreePath);
      const commitHash = hashResult.stdout.trim();

      console.log(`✅ 修复已提交: ${commitHash}`);

      return {
        success: true,
        commit_hash: commitHash,
        commit_message: commitMessage
      };

    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  }

  /**
   * 生成提交信息
   */
  generateCommitMessage(criticalIssues) {
    const issueTypes = [...new Set(criticalIssues.map(issue => issue.test_type))];
    const issueCount = criticalIssues.length;

    let message = `fix: 端到端检测问题修复 (${issueCount}个问题)\n\n`;

    message += `修复的问题类型: ${issueTypes.join(', ')}\n`;

    // 添加具体问题描述
    if (criticalIssues.length <= 5) {
      message += '\n详细问题:\n';
      criticalIssues.forEach((issue, index) => {
        message += `${index + 1}. [${issue.test_type}] ${issue.description}\n`;
      });
    }

    message += `\n自动修复生成于端到端检测系统\n`;
    message += `修复迭代: ${this.currentIteration + 1}`;

    return message;
  }

  /**
   * 生成修复摘要
   */
  generateFixSummary(criticalIssues) {
    const summary = {
      total_issues: criticalIssues.length,
      by_type: {},
      by_severity: {},
      files_affected: [...new Set(criticalIssues.map(issue => issue.file))]
    };

    // 按类型统计
    criticalIssues.forEach(issue => {
      summary.by_type[issue.test_type] = (summary.by_type[issue.test_type] || 0) + 1;
      summary.by_severity[issue.severity] = (summary.by_severity[issue.severity] || 0) + 1;
    });

    return summary;
  }

  /**
   * 增加迭代计数
   */
  incrementIteration() {
    this.currentIteration++;
  }

  /**
   * 重置迭代计数
   */
  resetIteration() {
    this.currentIteration = 0;
  }

  /**
   * 检查是否还有重试机会
   */
  hasRetryLeft() {
    return this.currentIteration < this.maxIterations;
  }

  /**
   * 获取修复历史
   */
  getFixHistory() {
    return this.fixHistory;
  }

  /**
   * 获取当前迭代次数
   */
  getCurrentIteration() {
    return this.currentIteration;
  }

  /**
   * 运行命令
   */
  async runCommand(command, cwd) {
    return new Promise((resolve, reject) => {
      exec(command, {
        cwd,
        timeout: 30000
      }, (error, stdout, stderr) => {
        if (error) {
          resolve({
            exitCode: error.code || 1,
            stdout: stdout || '',
            stderr: stderr || error.message,
            error: error.message
          });
        } else {
          resolve({
            exitCode: 0,
            stdout: stdout || '',
            stderr: stderr || ''
          });
        }
      });
    });
  }
}

/**
 * CLI入口
 */
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length < 2) {
    console.log('用法: node auto-fix-controller.js <analysisResult.json> <worktreePath>');
    process.exit(1);
  }

  const [analysisPath, worktreePath] = args;

  try {
    const analysisResult = JSON.parse(fs.readFileSync(analysisPath, 'utf8'));
    const controller = new AutoFixController(3);

    controller.attemptAutoFix(analysisResult, worktreePath)
      .then(result => {
        if (result.success) {
          console.log('✅ 自动修复成功');
          console.log(`修复数量: ${result.fixes_applied}`);
          console.log(`提交Hash: ${result.commit_hash}`);
          process.exit(0);
        } else {
          console.log('❌ 自动修复失败');
          console.log(`原因: ${result.message || result.error}`);
          process.exit(1);
        }
      })
      .catch(error => {
        console.error('执行失败:', error);
        process.exit(1);
      });

  } catch (error) {
    console.error('参数解析失败:', error);
    process.exit(1);
  }
}

module.exports = { AutoFixController };