#!/usr/bin/env node
/**
 * Playwright 端到端测试执行器
 *
 * 专门用于执行和管理 Playwright 端到端测试：
 * - 检测 Playwright 配置和测试文件
 * - 执行 Playwright 测试套件
 * - 收集和分析测试结果
 * - 生成详细的测试报告
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

/**
 * Playwright 测试执行器类
 */
class PlaywrightTestExecutor {
  constructor(config = {}) {
    this.config = {
      timeout: 120000, // 2分钟
      retries: 1,
      headless: true,
      browser: 'chromium', // chromium, firefox, webkit
      ...config
    };
  }

  /**
   * 检测项目是否使用 Playwright
   */
  detectPlaywright(worktreePath) {
    const indicators = {
      hasConfig: false,
      hasTests: false,
      hasDependency: false,
      version: null
    };

    // 检查 Playwright 配置文件
    const configFiles = [
      'playwright.config.js',
      'playwright.config.ts',
      'playwright.config.mjs'
    ];

    for (const configFile of configFiles) {
      if (fs.existsSync(path.join(worktreePath, configFile))) {
        indicators.hasConfig = true;
        break;
      }
    }

    // 检查 package.json 中的依赖
    const packageJsonPath = path.join(worktreePath, 'package.json');
    if (fs.existsSync(packageJsonPath)) {
      const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
      const deps = { ...packageJson.dependencies, ...packageJson.devDependencies };

      if (deps['@playwright/test']) {
        indicators.hasDependency = true;
        indicators.version = deps['@playwright/test'];
      }
    }

    // 检查测试文件
    const testPatterns = [
      '**/*.spec.ts',
      '**/*.spec.js',
      '**/*.e2e.ts',
      '**/*.e2e.js',
      'tests/**/*.ts',
      'tests/**/*.js',
      'e2e/**/*.ts',
      'e2e/**/*.js'
    ];

    for (const pattern of testPatterns) {
      const testFiles = this.findTestFiles(worktreePath, pattern);
      if (testFiles.length > 0) {
        indicators.hasTests = true;
        break;
      }
    }

    return indicators;
  }

  /**
   * 查找测试文件
   */
  findTestFiles(worktreePath, pattern) {
    const testFiles = [];

    const searchDir = (dir) => {
      try {
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
          if (entry.isDirectory()) {
            // 跳过 node_modules 和 .git
            if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'dist') {
              searchDir(path.join(dir, entry.name));
            }
          } else if (entry.isFile()) {
            // 简单的文件匹配
            const ext = path.extname(entry.name);
            const name = entry.name;

            if (this.matchesPattern(name, ext, pattern)) {
              testFiles.push(path.join(dir, entry.name));
            }
          }
        }
      } catch (error) {
        // 忽略无法访问的目录
      }
    };

    searchDir(worktreePath);
    return testFiles;
  }

  /**
   * 匹配文件模式
   */
  matchesPattern(filename, ext, pattern) {
    const patternMap = {
      '**/*.spec.ts': ext === '.ts' && filename.includes('.spec.'),
      '**/*.spec.js': ext === '.js' && filename.includes('.spec.'),
      '**/*.e2e.ts': ext === '.ts' && filename.includes('.e2e.'),
      '**/*.e2e.js': ext === '.js' && filename.includes('.e2e.'),
      'tests/**/*.ts': ext === '.ts',
      'tests/**/*.js': ext === '.js',
      'e2e/**/*.ts': ext === '.ts',
      'e2e/**/*.js': ext === '.js'
    };

    return patternMap[pattern] || false;
  }

  /**
   * 执行 Playwright 测试
   */
  async executeTests(worktreePath, options = {}) {
    console.log('🎭 开始执行 Playwright 端到端测试...');

    const playwrightDetection = this.detectPlaywright(worktreePath);

    if (!playwrightDetection.hasDependency) {
      return {
        status: 'SKIPPED',
        reason: 'Playwright 未安装或未配置',
        details: playwrightDetection
      };
    }

    if (!playwrightDetection.hasTests) {
      return {
        status: 'SKIPPED',
        reason: '未找到 Playwright 测试文件',
        details: playwrightDetection
      };
    }

    try {
      // 构建 Playwright 命令
      const command = this.buildPlaywrightCommand(options);

      console.log(`📋 执行命令: npx playwright test ${command.options.join(' ')}`);

      // 执行测试
      const result = await this.runCommand(
        `npx playwright test ${command.options.join(' ')}`,
        worktreePath,
        this.config.timeout
      );

      // 解析测试结果
      const testResult = this.parseTestResult(result, playwrightDetection);

      console.log(`🎭 Playwright 测试完成: ${testResult.status}`);
      console.log(`📊 通过: ${testResult.passed}/${testResult.total}, 失败: ${testResult.failed}/${testResult.total}`);

      return testResult;

    } catch (error) {
      console.error(`❌ Playwright 测试执行失败: ${error.message}`);

      return {
        status: 'ERROR',
        error: error.message,
        output: error.stdout || '',
        errors: error.stderr || ''
      };
    }
  }

  /**
   * 构建 Playwright 命令
   */
  buildPlaywrightCommand(options = {}) {
    const commandOptions = [];

    // 基本选项
    if (this.config.headless) {
      commandOptions.push('--headed=false');
    } else {
      commandOptions.push('--headed=true');
    }

    // 浏览器选择
    if (options.browser) {
      commandOptions.push(`--project=${options.browser}`);
    } else if (this.config.browser) {
      commandOptions.push(`--project=${this.config.browser}`);
    }

    // 重试次数
    if (options.retries !== undefined) {
      commandOptions.push(`--retries=${options.retries}`);
    } else if (this.config.retries > 0) {
      commandOptions.push(`--retries=${this.config.retries}`);
    }

    // 工作人员数量
    if (options.workers) {
      commandOptions.push(`--workers=${options.workers}`);
    }

    // 测试文件模式
    if (options.pattern) {
      commandOptions.push(options.pattern);
    }

    // 输出格式
    commandOptions.push('--reporter=list');

    return {
      command: 'npx playwright test',
      options: commandOptions
    };
  }

  /**
   * 解析测试结果
   */
  parseTestResult(result, detectionInfo) {
    const { stdout, stderr, exitCode } = result;

    // Playwright 输出解析
    const output = stdout + stderr;

    // 提取测试统计信息
    const stats = this.extractTestStats(output);

    // 提取失败的测试
    const failedTests = this.extractFailedTests(output);

    // 提取关键问题
    const criticalIssues = this.analyzeTestFailures(failedTests, output);

    return {
      status: exitCode === 0 && stats.failed === 0 ? 'PASS' : 'FAIL',
      framework: 'playwright',
      version: detectionInfo.version || 'unknown',
      test_stats: stats,
      failed_tests: failedTests,
      critical_issues: criticalIssues,
      execution_time: stats.duration || 0,
      output: output,
      detection_info: detectionInfo
    };
  }

  /**
   * 提取测试统计信息
   */
  extractTestStats(output) {
    const stats = {
      total: 0,
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0,
      flaky: 0
    };

    // Playwright 输出中的统计信息通常在最后
    const lines = output.split('\n');

    for (const line of lines) {
      // 匹配类似 "5 passed (15s)" 的模式
      const passedMatch = line.match(/(\d+)\s+passed\s*\((\d+[smh])\)/);
      if (passedMatch) {
        stats.passed = parseInt(passedMatch[1]);
        stats.duration = this.parseDurationToMs(passedMatch[2]);
      }

      // 匹配失败的测试
      const failedMatch = line.match(/(\d+)\s+failed/);
      if (failedMatch) {
        stats.failed = parseInt(failedMatch[1]);
      }

      // 匹配跳过的测试
      const skippedMatch = line.match(/(\d+)\s+skipped/);
      if (skippedMatch) {
        stats.skipped = parseInt(skippedMatch[1]);
      }

      // 匹配不稳定的测试
      const flakyMatch = line.match(/(\d+)\s+flaky/);
      if (flakyMatch) {
        stats.flaky = parseInt(flakyMatch[1]);
      }

      // 总数通常是 passed + failed + skipped
      if (stats.passed > 0 || stats.failed > 0 || stats.skipped > 0) {
        stats.total = stats.passed + stats.failed + stats.skipped;
      }
    }

    return stats;
  }

  /**
   * 将时间字符串转换为毫秒
   */
  parseDurationToMs(durationStr) {
    const match = durationStr.match(/(\d+)([smh])/);
    if (!match) return 0;

    const value = parseInt(match[1]);
    const unit = match[2];

    switch (unit) {
      case 's': return value * 1000;
      case 'm': return value * 60 * 1000;
      case 'h': return value * 60 * 60 * 1000;
      default: return 0;
    }
  }

  /**
   * 提取失败的测试
   */
  extractFailedTests(output) {
    const failedTests = [];
    const lines = output.split('\n');

    let currentTest = null;
    let inErrorBlock = false;
    let errorLines = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // 检测测试文件行
      const testFileMatch = line.match(/(\S+\.(spec|e2e)\.(js|ts))/);
      if (testFileMatch) {
        // 如果之前有失败的测试，保存它
        if (currentTest && currentTest.status === 'failed') {
          failedTests.push(currentTest);
        }

        // 开始新的测试
        currentTest = {
          file: testFileMatch[1],
          status: 'unknown',
          error: null,
          line: i + 1
        };

        inErrorBlock = false;
        errorLines = [];
      }

      // 检测失败状态
      if (line.includes('✘') || line.includes('failed') || line.includes('FAILED')) {
        if (currentTest) {
          currentTest.status = 'failed';
        }
      }

      // 检测错误块开始
      if (line.includes('Error:') || line.includes('expected')) {
        inErrorBlock = true;
      }

      // 收集错误信息
      if (inErrorBlock && line.trim()) {
        errorLines.push(line.trim());
      }

      // 检测错误块结束
      if (inErrorBlock && (line.includes('at ') || line.match(/^\d+\s+\|/))) {
        if (currentTest && errorLines.length > 0) {
          currentTest.error = errorLines.join('\n');
        }
        inErrorBlock = false;
        errorLines = [];
      }

      // 检测通过状态
      if (line.includes('✓') || line.includes('passed') || line.includes('PASSED')) {
        if (currentTest) {
          currentTest.status = 'passed';
        }
      }
    }

    // 添加最后一个测试
    if (currentTest && currentTest.status === 'failed') {
      failedTests.push(currentTest);
    }

    return failedTests;
  }

  /**
   * 分析测试失败
   */
  analyzeTestFailures(failedTests, output) {
    const criticalIssues = [];

    for (const failedTest of failedTests) {
      const issue = {
        severity: 'critical',
        description: `Playwright测试失败: ${failedTest.file}`,
        file: failedTest.file,
        line: failedTest.line || 0,
        test_type: 'frontend',
        error: failedTest.error || '未知错误',
        suggestion: this.generateFixSuggestion(failedTest)
      };

      criticalIssues.push(issue);
    }

    return criticalIssues;
  }

  /**
   * 生成修复建议
   */
  generateFixSuggestion(failedTest) {
    const error = failedTest.error || '';

    // 常见问题模式匹配
    if (error.includes('timeout')) {
      return '测试超时，检查页面加载时间或增加测试超时时间';
    } else if (error.includes('not found') || error.includes('selector')) {
      return '选择器找不到元素，检查页面结构和选择器是否正确';
    } else if (error.includes('assertion') || error.includes('expected')) {
      return '断言失败，检查预期值和实际值是否匹配';
    } else if (error.includes('network')) {
      return '网络错误，检查服务器是否正常运行';
    } else {
      return '检查测试代码和页面行为是否一致';
    }
  }

  /**
   * 运行命令
   */
  async runCommand(command, cwd, timeout = 120000) {
    return new Promise((resolve, reject) => {
      const [cmd, ...args] = command.split(' ');

      const child = spawn(cmd, args, {
        cwd,
        shell: true,
        timeout,
        env: { ...process.env, FORCE_COLOR: '0' }
      });

      let stdout = '';
      let stderr = '';

      child.stdout.on('data', (data) => {
        stdout += data.toString();
      });

      child.stderr.on('data', (data) => {
        stderr += data.toString();
      });

      child.on('close', (code) => {
        resolve({
          exitCode: code,
          stdout,
          stderr
        });
      });

      child.on('error', (error) => {
        reject({
          error: error.message,
          stdout,
          stderr
        });
      });
    });
  }

  /**
   * 安装 Playwright
   */
  async installPlaywright(worktreePath) {
    console.log('📦 安装 Playwright...');

    try {
      const result = await this.runCommand('npm install --save-dev @playwright/test', worktreePath, 300000);

      if (result.exitCode === 0) {
        console.log('✅ Playwright 安装成功');

        // 安装浏览器
        console.log('🌐 安装 Playwright 浏览器...');
        const browserResult = await this.runCommand('npx playwright install chromium', worktreePath, 300000);

        if (browserResult.exitCode === 0) {
          console.log('✅ Playwright 浏览器安装成功');
          return true;
        }
      }

      return false;

    } catch (error) {
      console.error(`❌ Playwright 安装失败: ${error.message}`);
      return false;
    }
  }

  /**
   * 创建示例 Playwright 测试
   */
  createSampleTests(worktreePath) {
    console.log('📝 创建示例 Playwright 测试...');

    const testsDir = path.join(worktreePath, 'e2e');
    fs.mkdirSync(testsDir, { recursive: true });

    // 创建基本测试文件
    const sampleTest = `import { test, expect } from '@playwright/test';

test.describe('基本端到端测试', () => {
  test.beforeEach(async ({ page }) => {
    // 每个测试前的设置
  });

  test('页面加载测试', async ({ page }) => {
    await page.goto('/');

    // 检查页面标题
    await expect(page).toHaveTitle(/./);

    // 检查基本元素存在
    const mainElement = await page.$('main') || await page.$('body');
    expect(mainElement).not.toBeNull();
  });

  test('用户交互测试', async ({ page }) => {
    await page.goto('/');

    // 示例：查找并点击按钮
    // const button = await page.$('button');
    // if (button) {
    //   await button.click();
    // }

    // 检查导航结果
    // await expect(page).toHaveURL(/.*\/dest/);
  });

  test('表单提交测试', async ({ page }) => {
    await page.goto('/contact');

    // 示例：填写表单
    // await page.fill('input[name="name"]', '测试用户');
    // await page.fill('input[name="email"]', 'test@example.com');
    // await page.click('button[type="submit"]');

    // 检查提交结果
    // await expect(page.locator('.success-message')).toBeVisible();
  });
});
`;

    fs.writeFileSync(path.join(testsDir, 'basic.spec.ts'), sampleTest);

    // 创建配置文件
    const config = `import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  expect: {
    timeout: 5000
  },
  use: {
    headless: true,
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    }
  ],
  reporter: 'html'
});
`;

    fs.writeFileSync(path.join(worktreePath, 'playwright.config.ts'), config);

    console.log('✅ 示例测试文件创建完成');
    console.log(`📁 测试目录: ${testsDir}`);
    console.log('📝 运行测试: npx playwright test');
  }
}

/**
 * CLI 入口
 */
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    console.log('用法: node playwright-executor.js <command> [worktreePath] [options]');
    console.log('');
    console.log('命令:');
    console.log('  detect    - 检测项目中的 Playwright 配置');
    console.log('  run       - 运行 Playwright 测试');
    console.log('  install   - 安装 Playwright');
    console.log('  init      - 初始化 Playwright 测试');
    console.log('');
    console.log('示例:');
    console.log('  node playwright-executor.js detect /path/to/project');
    console.log('  node playwright-executor.js run /path/to/project --browser=firefox');
    console.log('  node playwright-executor.js install /path/to/project');
    console.log('  node playwright-executor.js init /path/to/project');
    process.exit(1);
  }

  const [command, worktreePath, ...options] = args;

  if (!worktreePath) {
    console.error('❌ 缺少工作树路径参数');
    process.exit(1);
  }

  const executor = new PlaywrightTestExecutor();

  switch (command) {
    case 'detect':
      const detection = executor.detectPlaywright(worktreePath);
      console.log('🎭 Playwright 检测结果:');
      console.log(JSON.stringify(detection, null, 2));
      break;

    case 'run':
      executor.executeTests(worktreePath, {
        browser: options.find(opt => opt.startsWith('--browser='))?.split('=')[1],
        headless: !options.includes('--headed'),
        workers: options.find(opt => opt.startsWith('--workers='))?.split('=')[1]
      }).then(result => {
        console.log('🎭 测试结果:');
        console.log(JSON.stringify(result, null, 2));
        process.exit(result.status === 'PASS' ? 0 : 1);
      });
      break;

    case 'install':
      executor.installPlaywright(worktreePath).then(success => {
        process.exit(success ? 0 : 1);
      });
      break;

    case 'init':
      executor.createSampleTests(worktreePath);
      console.log('✅ Playwright 初始化完成');
      break;

    default:
      console.error(`❌ 未知命令: ${command}`);
      process.exit(1);
  }
}

module.exports = { PlaywrightTestExecutor };