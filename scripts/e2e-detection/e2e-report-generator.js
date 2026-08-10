#!/usr/bin/env node
/**
 * 端到端检测报告生成器
 *
 * 生成人类可读的端到端检测报告：
 * - Markdown格式报告
 * - JSON格式数据
 * - HTML格式可视化报告
 * - 控制台友好输出
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');

/**
 * 报告生成器类
 */
class E2EReportGenerator {
  constructor(options = {}) {
    this.options = {
      format: 'markdown', // markdown, json, html, console
      includeDetails: true,
      saveArtifacts: true,
      artifactsLocation: '.claude/artifacts/e2e-detection/',
      ...options
    };
  }

  /**
   * 生成报告
   */
  generateReport(detectionResult, analysisResult) {
    switch (this.options.format) {
      case 'markdown':
        return this.generateMarkdownReport(detectionResult, analysisResult);
      case 'json':
        return this.generateJsonReport(detectionResult, analysisResult);
      case 'html':
        return this.generateHtmlReport(detectionResult, analysisResult);
      case 'console':
        return this.generateConsoleReport(detectionResult, analysisResult);
      default:
        return this.generateMarkdownReport(detectionResult, analysisResult);
    }
  }

  /**
   * 生成Markdown报告
   */
  generateMarkdownReport(detectionResult, analysisResult) {
    const lines = [];

    lines.push('# 端到端检测报告\n');
    lines.push('## 检测概要\n');
    lines.push(`- **检测ID**: ${detectionResult.detection_id}`);
    lines.push(`- **执行时间**: ${detectionResult.timestamp}`);
    lines.push(`- **整体状态**: ${this.formatStatus(detectionResult.status)}`);
    lines.push(`- **执行时长**: ${detectionResult.execution_time?.toFixed(2) || 'N/A'}秒`);
    lines.push(`- **测试类型**: ${Object.keys(detectionResult.test_results || {}).join(', ') || '无'}\n`);

    // 状态摘要
    if (detectionResult.status === 'PASS') {
      lines.push('## ✅ 检测通过\n');
      lines.push('所有端到端测试均已通过，系统功能完整且运行正常。\n');
    } else if (detectionResult.status === 'FAIL') {
      lines.push('## ❌ 检测未通过\n');
      lines.push(`检测发现 ${detectionResult.critical_issues?.length || 0} 个关键问题需要修复。\n`);
    } else if (detectionResult.status === 'SKIPPED') {
      lines.push('## ⏭️ 检测被跳过\n');
      lines.push(`跳过原因: ${detectionResult.reason || '未指定'}\n`);
    } else if (detectionResult.status === 'ERROR') {
      lines.push('## ⚠️ 检测出错\n');
      lines.push(`错误信息: ${detectionResult.error || '未知错误'}\n`);
    }

    // 测试结果详情
    if (detectionResult.test_results && Object.keys(detectionResult.test_results).length > 0) {
      lines.push('## 测试结果详情\n');

      for (const [testType, result] of Object.entries(detectionResult.test_results)) {
        lines.push(`### ${this.formatTestType(testType)}\n`);
        lines.push(`- **状态**: ${this.formatStatus(result.status)}`);
        lines.push(`- **执行时间**: ${result.execution_time?.toFixed(2) || 'N/A'}秒`);

        if (result.framework) {
          lines.push(`- **框架**: ${result.framework}`);
        }

        if (result.critical_issues && result.critical_issues.length > 0) {
          lines.push(`- **关键问题**: ${result.critical_issues.length}个`);
        }

        lines.push('');
      }
    }

    // 关键问题详情
    if (detectionResult.critical_issues && detectionResult.critical_issues.length > 0) {
      lines.push('## 关键问题\n');

      detectionResult.critical_issues.forEach((issue, index) => {
        lines.push(`### ${index + 1}. ${issue.description}\n`);
        lines.push(`- **文件**: ${issue.file}`);
        if (issue.line > 0) {
          lines.push(`- **行号**: ${issue.line}`);
        }
        lines.push(`- **严重程度**: ${this.formatSeverity(issue.severity)}`);
        lines.push(`- **测试类型**: ${this.formatTestType(issue.test_type)}`);
        if (issue.suggestion) {
          lines.push(`- **修复建议**: ${issue.suggestion}`);
        }
        lines.push('');
      });
    }

    // 主要问题
    if (detectionResult.major_issues && detectionResult.major_issues.length > 0) {
      lines.push('## 主要问题\n');
      lines.push(`发现 ${detectionResult.major_issues.length} 个主要问题：\n`);

      detectionResult.major_issues.forEach((issue, index) => {
        lines.push(`${index + 1}. **${issue.test_type}**: ${issue.description}`);
        if (issue.suggestion) {
          lines.push(`   - 建议: ${issue.suggestion}`);
        }
        lines.push('');
      });
    }

    // 修复建议
    if (detectionResult.fix_suggestions && detectionResult.fix_suggestions.length > 0) {
      lines.push('## 修复建议\n');

      detectionResult.fix_suggestions.forEach((suggestion, index) => {
        lines.push(`### ${index + 1}. ${suggestion.issue}\n`);
        lines.push(`- **文件**: ${suggestion.file}`);
        if (suggestion.line > 0) {
          lines.push(`- **行号**: ${suggestion.line}`);
        }
        lines.push(`- **建议**: ${suggestion.suggestion}`);
        lines.push(`- **优先级**: ${this.formatSeverity(suggestion.severity)}`);
        lines.push('');
      });
    }

    // 性能指标
    if (detectionResult.performance_metrics) {
      lines.push('## 性能指标\n');
      lines.push(`- **总执行时间**: ${detectionResult.performance_metrics.total_execution_time?.toFixed(2)}秒`);
      lines.push(`- **内存使用**: ${detectionResult.performance_metrics.memory_usage || 'N/A'}`);
      lines.push(`- **CPU使用**: ${detectionResult.performance_metrics.cpu_usage || 'N/A'}`);
      lines.push('');
    }

    // 下一步行动
    lines.push('## 下一步行动\n');

    if (detectionResult.status === 'PASS') {
      lines.push('- ✅ 继续正常开发流程');
      lines.push('- 📝 更新相关文档（如有需要）');
      lines.push('- 🚀 准备合并到主分支\n');
    } else if (detectionResult.status === 'FAIL') {
      lines.push('- 🔧 修复上述关键问题');
      lines.push('- 🔄 重新运行端到端检测');
      lines.push('- 👆 如无法自动修复，请联系团队成员\n');
    } else if (detectionResult.status === 'SKIPPED') {
      lines.push('- ⏭️ 根据跳过原因决定下一步行动');
      lines.push('- 📋 检查配置设置\n');
    }

    // 报告元数据
    lines.push('---\n');
    lines.push(`*报告生成时间: ${new Date().toISOString()}*`);
    lines.push(`*Harness系统版本: 1.0.0*`);

    return lines.join('\n');
  }

  /**
   * 生成JSON报告
   */
  generateJsonReport(detectionResult, analysisResult) {
    return JSON.stringify({
      report_metadata: {
        generated_at: new Date().toISOString(),
        format: 'json',
        version: '1.0.0'
      },
      detection_result: detectionResult,
      analysis_result: analysisResult
    }, null, 2);
  }

  /**
   * 生成HTML报告
   */
  generateHtmlReport(detectionResult, analysisResult) {
    const html = [];

    html.push('<!DOCTYPE html>');
    html.push('<html lang="zh-CN">');
    html.push('<head>');
    html.push('  <meta charset="UTF-8">');
    html.push('  <meta name="viewport" content="width=device-width, initial-scale=1.0">');
    html.push('  <title>端到端检测报告</title>');
    html.push('  <style>');
    html.push('    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }');
    html.push('    .status-pass { color: #28a745; font-weight: bold; }');
    html.push('    .status-fail { color: #dc3545; font-weight: bold; }');
    html.push('    .status-skipped { color: #ffc107; font-weight: bold; }');
    html.push('    .status-error { color: #fd7e14; font-weight: bold; }');
    html.push('    .severity-critical { color: #dc3545; }');
    html.push('    .severity-major { color: #fd7e14; }');
    html.push('    .severity-minor { color: #6c757d; }');
    html.push('    .issue { border-left: 4px solid #dc3545; padding: 10px; margin: 10px 0; background: #f8f9fa; }');
    html.push('    .summary { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }');
    html.push('    table { width: 100%; border-collapse: collapse; margin: 20px 0; }');
    html.push('    th, td { border: 1px solid #dee2e6; padding: 8px; text-align: left; }');
    html.push('    th { background-color: #f8f9fa; }');
    html.push('  </style>');
    html.push('</head>');
    html.push('<body>');

    // 标题
    html.push('<h1>端到端检测报告</h1>');

    // 摘要
    html.push('<div class="summary">');
    html.push(`<p><strong>检测ID:</strong> ${detectionResult.detection_id}</p>`);
    html.push(`<p><strong>执行时间:</strong> ${detectionResult.timestamp}</p>`);
    html.push(`<p><strong>整体状态:</strong> <span class="status-${detectionResult.status.toLowerCase()}">${this.formatStatus(detectionResult.status)}</span></p>`);
    html.push(`<p><strong>执行时长:</strong> ${detectionResult.execution_time?.toFixed(2) || 'N/A'}秒</p>`);
    html.push('</div>');

    // 状态消息
    if (detectionResult.status === 'PASS') {
      html.push('<h2>✅ 检测通过</h2>');
      html.push('<p>所有端到端测试均已通过，系统功能完整且运行正常。</p>');
    } else if (detectionResult.status === 'FAIL') {
      html.push('<h2>❌ 检测未通过</h2>');
      html.push(`<p>检测发现 ${detectionResult.critical_issues?.length || 0} 个关键问题需要修复。</p>`);
    }

    // 测试结果表格
    if (detectionResult.test_results && Object.keys(detectionResult.test_results).length > 0) {
      html.push('<h2>测试结果详情</h2>');
      html.push('<table>');
      html.push('<tr><th>测试类型</th><th>状态</th><th>执行时间</th><th>框架</th><th>关键问题</th></tr>');

      for (const [testType, result] of Object.entries(detectionResult.test_results)) {
        html.push('<tr>');
        html.push(`<td>${this.formatTestType(testType)}</td>`);
        html.push(`<td class="status-${result.status.toLowerCase()}">${this.formatStatus(result.status)}</td>`);
        html.push(`<td>${result.execution_time?.toFixed(2) || 'N/A'}秒</td>`);
        html.push(`<td>${result.framework || 'N/A'}</td>`);
        html.push(`<td>${result.critical_issues?.length || 0}</td>`);
        html.push('</tr>');
      }

      html.push('</table>');
    }

    // 关键问题
    if (detectionResult.critical_issues && detectionResult.critical_issues.length > 0) {
      html.push('<h2>关键问题</h2>');

      detectionResult.critical_issues.forEach((issue, index) => {
        html.push('<div class="issue">');
        html.push(`<h3>${index + 1}. ${issue.description}</h3>`);
        html.push(`<p><strong>文件:</strong> ${issue.file}</p>`);
        if (issue.line > 0) {
          html.push(`<p><strong>行号:</strong> ${issue.line}</p>`);
        }
        html.push(`<p><strong>严重程度:</strong> <span class="severity-${issue.severity}">${this.formatSeverity(issue.severity)}</span></p>`);
        html.push(`<p><strong>测试类型:</strong> ${this.formatTestType(issue.test_type)}</p>`);
        if (issue.suggestion) {
          html.push(`<p><strong>修复建议:</strong> ${issue.suggestion}</p>`);
        }
        html.push('</div>');
      });
    }

    // 修复建议
    if (detectionResult.fix_suggestions && detectionResult.fix_suggestions.length > 0) {
      html.push('<h2>修复建议</h2>');
      html.push('<ul>');

      detectionResult.fix_suggestions.forEach(suggestion => {
        html.push('<li>');
        html.push(`<strong>${suggestion.issue}</strong> (${suggestion.file})`);
        if (suggestion.suggestion) {
          html.push(`<br>建议: ${suggestion.suggestion}`);
        }
        html.push('</li>');
      });

      html.push('</ul>');
    }

    // 页脚
    html.push('<hr>');
    html.push(`<p><small>报告生成时间: ${new Date().toISOString()} | Harness系统版本: 1.0.0</small></p>`);

    html.push('</body>');
    html.push('</html>');

    return html.join('\n');
  }

  /**
   * 生成控制台报告
   */
  generateConsoleReport(detectionResult, analysisResult) {
    const lines = [];

    lines.push('════════════════════════════════════════════════════════════════');
    lines.push('🔥 端到端检测报告');
    lines.push('════════════════════════════════════════════════════════════════');
    lines.push('');
    lines.push(`📋 检测ID:     ${detectionResult.detection_id}`);
    lines.push(`⏰ 执行时间:  ${detectionResult.timestamp}`);
    lines.push(`📊 整体状态:  ${this.formatConsoleStatus(detectionResult.status)}`);
    lines.push(`⏱️  执行时长:   ${detectionResult.execution_time?.toFixed(2) || 'N/A'}秒`);
    lines.push('');

    // 测试结果
    if (detectionResult.test_results && Object.keys(detectionResult.test_results).length > 0) {
      lines.push('─────────────────────────────────────────────────────────────');
      lines.push('📊 测试结果详情');
      lines.push('─────────────────────────────────────────────────────────────');

      for (const [testType, result] of Object.entries(detectionResult.test_results)) {
        const statusIcon = this.getConsoleStatusIcon(result.status);
        lines.push(`${statusIcon} ${this.formatTestType(testType).padEnd(15)} ${result.status.padEnd(8)} ${result.execution_time?.toFixed(2) || 'N/A'}s`);

        if (result.critical_issues && result.critical_issues.length > 0) {
          result.critical_issues.slice(0, 3).forEach((issue, i) => {
            lines.push(`   ${i + 1}. ${issue.description.substring(0, 60)}...`);
          });
        }
      }
      lines.push('');
    }

    // 关键问题
    if (detectionResult.critical_issues && detectionResult.critical_issues.length > 0) {
      lines.push('─────────────────────────────────────────────────────────────');
      lines.push(`🚨 关键问题 (${detectionResult.critical_issues.length})`);
      lines.push('─────────────────────────────────────────────────────────────');

      detectionResult.critical_issues.slice(0, 5).forEach((issue, index) => {
        lines.push(`${index + 1}. [${issue.test_type}] ${issue.description}`);
        lines.push(`   📁 ${issue.file}:${issue.line > 0 ? issue.line : '?'}`);
        if (issue.suggestion) {
          lines.push(`   💡 ${issue.suggestion}`);
        }
      });

      if (detectionResult.critical_issues.length > 5) {
        lines.push(`   ... 还有 ${detectionResult.critical_issues.length - 5} 个问题`);
      }
      lines.push('');
    }

    // 修复建议
    if (detectionResult.fix_suggestions && detectionResult.fix_suggestions.length > 0) {
      lines.push('─────────────────────────────────────────────────────────────');
      lines.push('🔧 修复建议');
      lines.push('─────────────────────────────────────────────────────────────');

      detectionResult.fix_suggestions.slice(0, 3).forEach((suggestion, index) => {
        lines.push(`${index + 1}. ${suggestion.issue}`);
        lines.push(`   ${suggestion.suggestion}`);
      });
      lines.push('');
    }

    // 下一步行动
    lines.push('─────────────────────────────────────────────────────────────');
    lines.push('📋 下一步行动');
    lines.push('─────────────────────────────────────────────────────────────');

    if (detectionResult.status === 'PASS') {
      lines.push('✅ 继续正常开发流程');
      lines.push('📝 更新相关文档（如有需要）');
      lines.push('🚀 准备合并到主分支');
    } else if (detectionResult.status === 'FAIL') {
      lines.push('🔧 修复上述关键问题');
      lines.push('🔄 重新运行端到端检测');
      lines.push('👆 如无法自动修复，请联系团队成员');
    } else if (detectionResult.status === 'SKIPPED') {
      lines.push('⏭️ 根据跳过原因决定下一步行动');
      lines.push('📋 检查配置设置');
    }

    lines.push('');
    lines.push('════════════════════════════════════════════════════════════════');

    return lines.join('\n');
  }

  /**
   * 格式化状态
   */
  formatStatus(status) {
    const statusMap = {
      'PASS': '✅ 通过',
      'FAIL': '❌ 失败',
      'SKIPPED': '⏭️ 跳过',
      'ERROR': '⚠️ 错误'
    };
    return statusMap[status] || status;
  }

  /**
   * 格式化控制台状态
   */
  formatConsoleStatus(status) {
    const statusMap = {
      'PASS': '✅ 通过',
      'FAIL': '❌ 失败',
      'SKIPPED': '⏭️ 跳过',
      'ERROR': '⚠️ 错误'
    };
    return statusMap[status] || status;
  }

  /**
   * 获取控制台状态图标
   */
  getConsoleStatusIcon(status) {
    const iconMap = {
      'PASS': '✅',
      'FAIL': '❌',
      'SKIPPED': '⏭️',
      'ERROR': '⚠️'
    };
    return iconMap[status] || '❓';
  }

  /**
   * 格式化测试类型
   */
  formatTestType(testType) {
    const typeMap = {
      'frontend': '前端测试',
      'backend': '后端测试',
      'integration': '集成测试',
      'performance': '性能测试',
      'security': '安全测试'
    };
    return typeMap[testType] || testType;
  }

  /**
   * 格式化严重程度
   */
  formatSeverity(severity) {
    const severityMap = {
      'critical': '🔴 严重',
      'major': '🟠 重要',
      'minor': '🟡 次要',
      'info': '🔵 信息'
    };
    return severityMap[severity] || severity;
  }

  /**
   * 保存报告到文件
   */
  async saveReport(detectionResult, format = 'markdown') {
    try {
      // 确保目录存在
      const artifactsDir = this.options.artifactsLocation;
      fs.mkdirSync(artifactsDir, { recursive: true });

      // 生成文件名
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
      const filename = `e2e-report-${detectionResult.detection_id}-${timestamp}`;

      let content, extension;

      switch (format) {
        case 'markdown':
          content = this.generateMarkdownReport(detectionResult);
          extension = '.md';
          break;
        case 'json':
          content = this.generateJsonReport(detectionResult);
          extension = '.json';
          break;
        case 'html':
          content = this.generateHtmlReport(detectionResult);
          extension = '.html';
          break;
        case 'console':
          content = this.generateConsoleReport(detectionResult);
          extension = '.txt';
          break;
        default:
          content = this.generateMarkdownReport(detectionResult);
          extension = '.md';
      }

      // 保存文件
      const filepath = path.join(artifactsDir, `${filename}${extension}`);
      fs.writeFileSync(filepath, content, 'utf8');

      console.log(`📋 报告已保存: ${filepath}`);

      return filepath;

    } catch (error) {
      console.error(`报告保存失败: ${error.message}`);
      return null;
    }
  }

  /**
   * 生成并保存所有格式的报告
   */
  async saveAllFormats(detectionResult) {
    const formats = ['markdown', 'json', 'html'];
    const savedFiles = [];

    for (const format of formats) {
      const filepath = await this.saveReport(detectionResult, format);
      if (filepath) {
        savedFiles.push(filepath);
      }
    }

    // 保存控制台版本到最新报告
    const latestReportPath = path.join(this.options.artifactsLocation, 'latest-report.txt');
    const consoleContent = this.generateConsoleReport(detectionResult);
    fs.writeFileSync(latestReportPath, consoleContent, 'utf8');
    savedFiles.push(latestReportPath);

    return savedFiles;
  }

  /**
   * 打印控制台报告
   */
  printConsoleReport(detectionResult) {
    const report = this.generateConsoleReport(detectionResult);
    console.log(report);
  }
}

/**
 * CLI入口
 */
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    console.log('用法: node e2e-report-generator.js <result.json> [format]');
    console.log('');
    console.log('格式: markdown (默认), json, html, console');
    console.log('');
    console.log('示例:');
    console.log('  node e2e-report-generator.js result.json');
    console.log('  node e2e-report-generator.js result.json json');
    console.log('  node e2e-report-generator.js result.html html');
    process.exit(1);
  }

  const [resultPath, format = 'markdown'] = args;

  try {
    // 读取检测结果
    const detectionResult = JSON.parse(fs.readFileSync(resultPath, 'utf8'));

    // 创建生成器
    const generator = new E2EReportGenerator({ format });

    // 生成报告
    const report = generator.generateReport(detectionResult);

    // 输出到控制台或文件
    if (format === 'console') {
      console.log(report);
    } else {
      // 保存到文件
      const artifactsDir = '.claude/artifacts/e2e-detection/';
      fs.mkdirSync(artifactsDir, { recursive: true });

      const extension = format === 'markdown' ? '.md' : `.${format}`;
      const filename = `e2e-report-${detectionResult.detection_id}${extension}`;
      const filepath = path.join(artifactsDir, filename);

      fs.writeFileSync(filepath, report, 'utf8');
      console.log(`📋 报告已保存: ${filepath}`);
    }

  } catch (error) {
    console.error('生成报告失败:', error.message);
    process.exit(1);
  }
}

module.exports = { E2EReportGenerator };