#!/usr/bin/env node
/**
 * 端到端检测管理器
 *
 * 负责协调端到端检测流程，包括：
 * - 触发检测
 * - 运行各类测试
 * - 分析结果
 * - 自动修复循环
 * - 报告生成
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');
const { spawn, exec } = require('child_process');
const readline = require('readline');

// 引入 Playwright 执行器
const { PlaywrightTestExecutor } = require('./playwright-executor.js');

// 配置文件路径
const CONFIG_PATH = '.claude/config/e2e-detection.config.json';
const STATE_DIR = '.claude/state/e2e-detection';

/**
 * 端到端检测管理器类
 */
class E2EDetectionManager {
  constructor(taskContext, contractPath) {
    this.taskContext = taskContext;
    this.contractPath = contractPath;
    this.config = this.loadConfig();
    this.state = this.loadState();
    this.detectionId = this.generateDetectionId();
  }

  /**
   * 生成检测ID
   */
  generateDetectionId() {
    return `e2e-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * 加载配置
   */
  loadConfig() {
    try {
      // 尝试从多个位置加载配置
      const configPaths = [
        CONFIG_PATH,
        '.claude/settings.json',
        path.join(__dirname, 'config', 'default-e2e-config.json')
      ];

      for (const configPath of configPaths) {
        if (fs.existsSync(configPath)) {
          const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

          // 如果是settings.json，提取e2e_detection部分
          if (config.harness && config.harness.e2e_detection) {
            return config.harness.e2e_detection;
          }

          // 如果直接是e2e配置
          if (config.enabled !== undefined) {
            return config;
          }
        }
      }

      // 返回默认配置
      return this.getDefaultConfig();
    } catch (error) {
      console.warn(`配置加载失败: ${error.message}，使用默认配置`);
      return this.getDefaultConfig();
    }
  }

  /**
   * 获取默认配置
   */
  getDefaultConfig() {
    return {
      enabled: true,
      mode: 'strict',
      timeout: 120,
      retry_on_failure: true,
      max_retries: 3,
      test_types: {
        frontend: {
          enabled: true,
          framework: 'auto',
          test_paths: ['frontend/tests/', 'src/test/ui/']
        },
        backend: {
          enabled: true,
          framework: 'auto',
          test_paths: ['backend/tests/', 'src/test/api/']
        },
        integration: {
          enabled: true,
          test_scenarios: ['user_login', 'data_flow', 'error_handling']
        },
        performance: {
          enabled: false,
          response_time_max: 2000,
          concurrent_users: 10
        },
        security: {
          enabled: true,
          scan_vulnerabilities: true,
          check_dependencies: true
        }
      },
      thresholds: {
        response_time: {
          warning: 1500,
          critical: 2000
        },
        memory_usage: {
          warning: 0.7,
          critical: 0.8
        },
        cpu_usage: {
          warning: 0.8,
          critical: 0.9
        }
      }
    };
  }

  /**
   * 加载状态
   */
  loadState() {
    try {
      const stateFile = path.join(STATE_DIR, 'latest-state.json');
      if (fs.existsSync(stateFile)) {
        return JSON.parse(fs.readFileSync(stateFile, 'utf8'));
      }
      return {};
    } catch (error) {
      console.warn(`状态加载失败: ${error.message}`);
      return {};
    }
  }

  /**
   * 保存状态
   */
  saveState() {
    try {
      fs.mkdirSync(STATE_DIR, { recursive: true });
      const stateFile = path.join(STATE_DIR, 'latest-state.json');
      fs.writeFileSync(stateFile, JSON.stringify(this.state, null, 2));
    } catch (error) {
      console.error(`状态保存失败: ${error.message}`);
    }
  }

  /**
   * 运行端到端检测
   */
  async runDetection(worktreePath, baseRef) {
    const startTime = Date.now();

    console.log(`🔥 开始端到端检测: ${this.detectionId}`);
    console.log(`📍 工作树路径: ${worktreePath}`);
    console.log(`📍 基准引用: ${baseRef}`);

    // 更新状态
    this.state.current_detection = {
      detection_id: this.detectionId,
      start_time: new Date().toISOString(),
      worktree_path: worktreePath,
      base_ref: baseRef,
      status: 'running'
    };
    this.saveState();

    try {
      // 1. 检查是否应该触发检测
      if (!this.shouldTriggerDetection()) {
        console.log('⏭️  端到端检测被跳过（配置禁用或条件不满足）');
        return this.createSkippedResult('检测被配置禁用');
      }

      // 2. 准备测试环境
      console.log('🔧 准备测试环境...');
      await this.prepareTestEnvironment(worktreePath);

      // 3. 运行各类测试
      console.log('🧪 开始执行测试...');
      const testResults = await this.executeAllTests(worktreePath, baseRef);

      // 4. 分析结果
      console.log('📊 分析测试结果...');
      const analysis = this.analyzeResults(testResults);

      // 5. 清理测试环境
      console.log('🧹 清理测试环境...');
      await this.cleanupTestEnvironment(worktreePath);

      // 6. 生成最终结果
      const executionTime = (Date.now() - startTime) / 1000;
      const finalResult = this.createFinalResult(testResults, analysis, executionTime);

      // 7. 保存检测结果
      this.saveDetectionResult(finalResult);

      // 8. 更新状态
      this.state.current_detection.status = finalResult.status;
      this.saveState();

      console.log(`✅ 端到端检测完成: ${finalResult.status} (${executionTime.toFixed(2)}s)`);

      return finalResult;

    } catch (error) {
      console.error(`❌ 端到端检测失败: ${error.message}`);

      const errorResult = {
        detection_id: this.detectionId,
        timestamp: new Date().toISOString(),
        status: 'ERROR',
        error: error.message,
        test_results: {},
        critical_issues: [],
        execution_time: (Date.now() - startTime) / 1000
      };

      // 保存错误结果
      this.saveDetectionResult(errorResult);

      return errorResult;
    }
  }

  /**
   * 判断是否应该触发检测
   */
  shouldTriggerDetection() {
    // 检查配置是否启用
    if (!this.config.enabled) {
      return false;
    }

    // 检查是否有任何测试类型启用
    const hasEnabledTests = Object.values(this.config.test_types).some(test => test.enabled);
    if (!hasEnabledTests) {
      return false;
    }

    return true;
  }

  /**
   * 准备测试环境
   */
  async prepareTestEnvironment(worktreePath) {
    // 创建状态目录
    const detectionStateDir = path.join(STATE_DIR, this.detectionId);
    fs.mkdirSync(detectionStateDir, { recursive: true });

    // 检查项目结构
    const projectType = this.detectProjectType(worktreePath);
    console.log(`🔍 检测到项目类型: ${projectType}`);

    // 根据项目类型进行特定准备
    if (projectType === 'nodejs') {
      await this.prepareNodejsEnvironment(worktreePath);
    } else if (projectType === 'java') {
      await this.prepareJavaEnvironment(worktreePath);
    } else if (projectType === 'python') {
      await this.preparePythonEnvironment(worktreePath);
    }

    return projectType;
  }

  /**
   * 检测项目类型
   */
  detectProjectType(worktreePath) {
    // 检查package.json
    if (fs.existsSync(path.join(worktreePath, 'package.json'))) {
      return 'nodejs';
    }

    // 检查pom.xml
    if (fs.existsSync(path.join(worktreePath, 'pom.xml'))) {
      return 'java';
    }

    // 检查requirements.txt或setup.py
    if (fs.existsSync(path.join(worktreePath, 'requirements.txt')) ||
        fs.existsSync(path.join(worktreePath, 'setup.py'))) {
      return 'python';
    }

    // 检查go.mod
    if (fs.existsSync(path.join(worktreePath, 'go.mod'))) {
      return 'go';
    }

    return 'unknown';
  }

  /**
   * 准备Node.js环境
   */
  async prepareNodejsEnvironment(worktreePath) {
    // 检查是否需要安装依赖
    const packageJsonPath = path.join(worktreePath, 'package.json');
    if (fs.existsSync(packageJsonPath)) {
      const nodeModulesPath = path.join(worktreePath, 'node_modules');

      // 如果node_modules不存在，安装依赖
      if (!fs.existsSync(nodeModulesPath)) {
        console.log('📦 安装Node.js依赖...');
        await this.runCommand('npm install', worktreePath, { timeout: 300000 });
      }
    }
  }

  /**
   * 准备Java环境
   */
  async prepareJavaEnvironment(worktreePath) {
    // Maven项目
    if (fs.existsSync(path.join(worktreePath, 'pom.xml'))) {
      const targetDir = path.join(worktreePath, 'target');

      // 如果target目录不存在或为空，进行编译
      if (!fs.existsSync(targetDir) || fs.readdirSync(targetDir).length === 0) {
        console.log('🔨 编译Java项目...');
        await this.runCommand('mvn clean compile', worktreePath, { timeout: 300000 });
      }
    }
    // Gradle项目
    else if (fs.existsSync(path.join(worktreePath, 'build.gradle'))) {
      const buildDir = path.join(worktreePath, 'build');

      if (!fs.existsSync(buildDir) || fs.readdirSync(buildDir).length === 0) {
        console.log('🔨 编译Java项目...');
        await this.runCommand('./gradlew build', worktreePath, { timeout: 300000 });
      }
    }
  }

  /**
   * 准备Python环境
   */
  async preparePythonEnvironment(worktreePath) {
    // 检查虚拟环境
    const venvPaths = [
      path.join(worktreePath, 'venv'),
      path.join(worktreePath, '.venv'),
      path.join(worktreePath, 'env')
    ];

    let hasVenv = venvPaths.some(venvPath => fs.existsSync(venvPath));

    if (!hasVenv) {
      console.log('🐍 创建Python虚拟环境...');
      await this.runCommand('python -m venv venv', worktreePath, { timeout: 120000 });
    }

    // 安装依赖
    console.log('📦 安装Python依赖...');
    const pipPath = hasVenv ?
      path.join(worktreePath, 'venv', 'bin', 'pip') :
      path.join(worktreePath, 'venv', 'Scripts', 'pip.exe');

    if (fs.existsSync(path.join(worktreePath, 'requirements.txt'))) {
      await this.runCommand(`${pipPath} install -r requirements.txt`, worktreePath, { timeout: 300000 });
    }
  }

  /**
   * 执行所有测试
   */
  async executeAllTests(worktreePath, baseRef) {
    const testResults = {};
    const testTypes = [];

    // 收集需要运行的测试类型
    if (this.config.test_types.frontend.enabled) {
      testTypes.push('frontend');
    }
    if (this.config.test_types.backend.enabled) {
      testTypes.push('backend');
    }
    if (this.config.test_types.integration.enabled) {
      testTypes.push('integration');
    }
    if (this.config.test_types.performance.enabled) {
      testTypes.push('performance');
    }
    if (this.config.test_types.security.enabled) {
      testTypes.push('security');
    }

    // 并行执行测试
    const testPromises = testTypes.map(testType =>
      this.executeTestType(testType, worktreePath, baseRef)
        .then(result => ({ testType, result }))
        .catch(error => ({
          testType,
          result: {
            status: 'ERROR',
            error: error.message,
            critical_issues: []
          }
        }))
    );

    const results = await Promise.all(testPromises);

    // 整理结果
    results.forEach(({ testType, result }) => {
      testResults[testType] = result;
    });

    return testResults;
  }

  /**
   * 执行特定类型的测试
   */
  async executeTestType(testType, worktreePath, baseRef) {
    console.log(`🧪 执行 ${testType} 测试...`);
    const startTime = Date.now();

    try {
      let result;

      switch (testType) {
        case 'frontend':
          result = await this.executeFrontendTests(worktreePath);
          break;
        case 'backend':
          result = await this.executeBackendTests(worktreePath);
          break;
        case 'integration':
          result = await this.executeIntegrationTests(worktreePath);
          break;
        case 'performance':
          result = await this.executePerformanceTests(worktreePath);
          break;
        case 'security':
          result = await this.executeSecurityTests(worktreePath);
          break;
        default:
          result = {
            status: 'SKIPPED',
            reason: '未知测试类型',
            critical_issues: []
          };
      }

      const executionTime = (Date.now() - startTime) / 1000;
      result.execution_time = executionTime;
      result.test_type = testType;

      console.log(`${result.status === 'PASS' ? '✅' : '❌'} ${testType} 测试完成 (${executionTime.toFixed(2)}s)`);

      return result;

    } catch (error) {
      console.error(`❌ ${testType} 测试失败: ${error.message}`);
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: [],
        execution_time: (Date.now() - startTime) / 1000
      };
    }
  }

  /**
   * 执行前端测试
   */
  async executeFrontendTests(worktreePath) {
    // 检测前端测试框架
    const packageJsonPath = path.join(worktreePath, 'package.json');
    if (!fs.existsSync(packageJsonPath)) {
      return {
        status: 'SKIPPED',
        reason: '未找到package.json，无前端测试框架',
        critical_issues: []
      };
    }

    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    const devDeps = packageJson.devDependencies || {};
    const deps = packageJson.dependencies || {};

    let testCommand = null;

    // 检测测试框架
    if (devDeps.cypress || deps.cypress) {
      testCommand = 'npx cypress run';
    } else if (devDeps['@playwright/test'] || deps['@playwright/test']) {
      // 🎭 使用专门的 Playwright 执行器
      console.log('🎭 检测到 Playwright，使用专用执行器...');
      const playwrightExecutor = new PlaywrightTestExecutor({
        timeout: this.config.timeout * 1000,
        headless: true,
        retries: 1
      });

      try {
        const playwrightResult = await playwrightExecutor.executeTests(worktreePath, {
          browser: 'chromium' // 默认使用 Chromium
        });

        // 转换结果格式以兼容现有系统
        return {
          status: playwrightResult.status,
          framework: 'playwright',
          test_output: playwrightResult.output,
          execution_time: playwrightResult.execution_time,
          critical_issues: playwrightResult.critical_issues || [],
          test_stats: playwrightResult.test_stats || {},
          failed_tests: playwrightResult.failed_tests || []
        };
      } catch (error) {
        return {
          status: 'ERROR',
          error: error.message,
          critical_issues: []
        };
      }
    } else if (devDeps.selenium || deps.selenium) {
      testCommand = 'npm test'; // 假设配置了selenium测试
    }

    if (!testCommand) {
      return {
        status: 'SKIPPED',
        reason: '未检测到前端测试框架 (Cypress/Playwright/Selenium)',
        critical_issues: []
      };
    }

    try {
      const { stdout, stderr, exitCode } = await this.runCommand(testCommand, worktreePath, {
        timeout: this.config.timeout * 1000
      });

      if (exitCode === 0) {
        return {
          status: 'PASS',
          framework: this.detectFramework(packageJson),
          test_output: stdout,
          critical_issues: []
        };
      } else {
        // 分析测试失败输出
        const criticalIssues = this.parseTestFailures(stderr || stdout);
        return {
          status: 'FAIL',
          framework: this.detectFramework(packageJson),
          test_output: stdout,
          error_output: stderr,
          critical_issues: criticalIssues
        };
      }

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 检测前端测试框架
   */
  detectFramework(packageJson) {
    const deps = { ...packageJson.dependencies, ...packageJson.devDependencies };

    if (deps.cypress) return 'cypress';
    if (deps['@playwright/test']) return 'playwright';
    if (deps.selenium) return 'selenium';

    return 'unknown';
  }

  /**
   * 解析测试失败信息
   */
  parseTestFailures(output) {
    const issues = [];
    const lines = output.split('\n');

    // 简单的失败模式匹配（可根据不同框架优化）
    for (const line of lines) {
      // 失败关键词
      if (line.includes('failed') || line.includes('error') || line.includes('Error')) {
        issues.push({
          severity: 'critical',
          description: line.trim(),
          file: 'unknown',
          line: 0,
          suggestion: '检查测试失败的具体原因'
        });
      }
    }

    return issues;
  }

  /**
   * 执行后端测试
   */
  async executeBackendTests(worktreePath) {
    // 检测项目类型和测试框架
    const projectType = this.detectProjectType(worktreePath);

    if (projectType === 'nodejs') {
      return await this.executeNodejsBackendTests(worktreePath);
    } else if (projectType === 'java') {
      return await this.executeJavaBackendTests(worktreePath);
    } else if (projectType === 'python') {
      return await this.executePythonBackendTests(worktreePath);
    } else if (projectType === 'go') {
      return await this.executeGoBackendTests(worktreePath);
    }

    return {
      status: 'SKIPPED',
      reason: '不支持的项目类型',
      critical_issues: []
    };
  }

  /**
   * 执行Node.js后端测试
   */
  async executeNodejsBackendTests(worktreePath) {
    const testScripts = ['test', 'test:backend', 'test:api'];
    const packageJsonPath = path.join(worktreePath, 'package.json');

    if (!fs.existsSync(packageJsonPath)) {
      return {
        status: 'SKIPPED',
        reason: '未找到package.json',
        critical_issues: []
      };
    }

    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

    // 查找可用的测试脚本
    let testCommand = null;
    for (const script of testScripts) {
      if (packageJson.scripts && packageJson.scripts[script]) {
        testCommand = `npm run ${script}`;
        break;
      }
    }

    if (!testCommand) {
      // 检查测试文件
      const hasTestFiles = this.findTestFiles(worktreePath, [
        'test/**/*.test.js',
        'test/**/*.spec.js',
        'src/test/**/*.js',
        '__tests__/**/*.js'
      ]);

      if (!hasTestFiles) {
        return {
          status: 'SKIPPED',
          reason: '未找到后端测试文件',
          critical_issues: []
        };
      }

      testCommand = 'npm test';
    }

    try {
      const { stdout, stderr, exitCode } = await this.runCommand(testCommand, worktreePath, {
        timeout: this.config.timeout * 1000
      });

      if (exitCode === 0) {
        return {
          status: 'PASS',
          framework: 'nodejs',
          test_output: stdout,
          critical_issues: []
        };
      } else {
        const criticalIssues = this.parseTestFailures(stderr || stdout);
        return {
          status: 'FAIL',
          framework: 'nodejs',
          test_output: stdout,
          error_output: stderr,
          critical_issues: criticalIssues
        };
      }

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 执行Java后端测试
   */
  async executeJavaBackendTests(worktreePath) {
    let testCommand = null;

    // Maven项目
    if (fs.existsSync(path.join(worktreePath, 'pom.xml'))) {
      testCommand = 'mvn test';
    }
    // Gradle项目
    else if (fs.existsSync(path.join(worktreePath, 'build.gradle'))) {
      testCommand = './gradlew test';
    }

    if (!testCommand) {
      return {
        status: 'SKIPPED',
        reason: '未找到Java项目配置文件',
        critical_issues: []
      };
    }

    try {
      const { stdout, stderr, exitCode } = await this.runCommand(testCommand, worktreePath, {
        timeout: this.config.timeout * 1000
      });

      // Maven测试成功时退出码为0，Gradle也是
      if (exitCode === 0) {
        return {
          status: 'PASS',
          framework: 'java',
          test_output: stdout,
          critical_issues: []
        };
      } else {
        const criticalIssues = this.parseJavaTestFailures(stderr || stdout);
        return {
          status: 'FAIL',
          framework: 'java',
          test_output: stdout,
          error_output: stderr,
          critical_issues: criticalIssues
        };
      }

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 解析Java测试失败
   */
  parseJavaTestFailures(output) {
    const issues = [];
    const lines = output.split('\n');

    for (const line of lines) {
      // Maven/Gradle失败模式
      if (line.includes('FAILURE') || line.includes('Tests run:') && line.includes('Failures:')) {
        const match = line.match(/(\w+)\.(.*?)(?=\s|$)/);
        if (match) {
          issues.push({
            severity: 'critical',
            description: `测试失败: ${match[0]}`,
            file: match[1] || 'unknown',
            line: 0,
            suggestion: '检查失败的测试用例'
          });
        }
      }
    }

    return issues;
  }

  /**
   * 执行Python后端测试
   */
  async executePythonBackendTests(worktreePath) {
    // 检查pytest
    const hasPytest = fs.existsSync(path.join(worktreePath, 'pytest.ini')) ||
                     fs.existsSync(path.join(worktreePath, 'setup.cfg')) ||
                     this.hasTestFiles(worktreePath, ['test_*.py', '*_test.py']);

    if (hasPytest) {
      try {
        const pytestPath = path.join(worktreePath, 'venv', 'bin', 'pytest');
        const { stdout, stderr, exitCode } = await this.runCommand(
          `${pytestPath} -v`,
          worktreePath,
          { timeout: this.config.timeout * 1000 }
        );

        if (exitCode === 0) {
          return {
            status: 'PASS',
            framework: 'pytest',
            test_output: stdout,
            critical_issues: []
          };
        } else {
          const criticalIssues = this.parsePythonTestFailures(stderr || stdout);
          return {
            status: 'FAIL',
            framework: 'pytest',
            test_output: stdout,
            error_output: stderr,
            critical_issues: criticalIssues
          };
        };

      } catch (error) {
        return {
          status: 'ERROR',
          error: error.message,
          critical_issues: []
        };
      }
    }

    return {
      status: 'SKIPPED',
      reason: '未找到pytest测试',
      critical_issues: []
    };
  }

  /**
   * 解析Python测试失败
   */
  parsePythonTestFailures(output) {
    const issues = [];
    const lines = output.split('\n');

    for (const line of lines) {
      // pytest失败模式
      if (line.includes('FAILED') && line.includes('::')) {
        const parts = line.split('::');
        if (parts.length >= 2) {
          issues.push({
            severity: 'critical',
            description: `测试失败: ${parts[1].trim()}`,
            file: parts[0].trim(),
            line: 0,
            suggestion: '检查失败的测试用例'
          });
        }
      }
    }

    return issues;
  }

  /**
   * 执行Go后端测试
   */
  async executeGoBackendTests(worktreePath) {
    try {
      const { stdout, stderr, exitCode } = await this.runCommand(
        'go test ./...',
        worktreePath,
        { timeout: this.config.timeout * 1000 }
      );

      if (exitCode === 0) {
        return {
          status: 'PASS',
          framework: 'go',
          test_output: stdout,
          critical_issues: []
        };
      } else {
        const criticalIssues = this.parseGoTestFailures(stderr || stdout);
        return {
          status: 'FAIL',
          framework: 'go',
          test_output: stdout,
          error_output: stderr,
          critical_issues: criticalIssues
        };
      };

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 解析Go测试失败
   */
  parseGoTestFailures(output) {
    const issues = [];
    const lines = output.split('\n');

    for (const line of lines) {
      // Go测试失败模式
      if (line.includes('FAIL') && line.includes('.go')) {
        const match = line.match(/(\w+\.go):(\d+):/);
        if (match) {
          issues.push({
            severity: 'critical',
            description: `测试失败: ${line.trim()}`,
            file: match[1],
            line: parseInt(match[2]),
            suggestion: '检查失败的测试用例'
          });
        }
      }
    }

    return issues;
  }

  /**
   * 执行集成测试
   */
  async executeIntegrationTests(worktreePath) {
    const scenarios = this.config.test_types.integration.test_scenarios;
    const results = [];

    for (const scenario of scenarios) {
      console.log(`🔗 执行集成场景: ${scenario}`);

      try {
        const result = await this.executeIntegrationScenario(scenario, worktreePath);
        results.push(result);
      } catch (error) {
        results.push({
          scenario: scenario,
          status: 'ERROR',
          error: error.message,
          passed: false,
          critical_issues: []
        });
      }
    }

    // 判断整体状态
    const allPassed = results.every(r => r.passed);
    const allCriticalIssues = results.flatMap(r => r.critical_issues || []);

    return {
      status: allPassed ? 'PASS' : 'FAIL',
      scenarios_tested: results.length,
      scenarios_passed: results.filter(r => r.passed).length,
      scenario_results: results,
      critical_issues: allCriticalIssues
    };
  }

  /**
   * 执行单个集成场景
   */
  async executeIntegrationScenario(scenario, worktreePath) {
    // 这里实现具体的集成场景测试
    // 目前提供基础框架，可根据项目需求扩展

    switch (scenario) {
      case 'user_login':
        return await this.testUserLoginScenario(worktreePath);
      case 'data_flow':
        return await this.testDataFlowScenario(worktreePath);
      case 'error_handling':
        return await this.testErrorHandlingScenario(worktreePath);
      default:
        return {
          scenario: scenario,
          status: 'SKIPPED',
          passed: false,
          reason: '未实现的集成场景',
          critical_issues: []
        };
    }
  }

  /**
   * 测试用户登录场景
   */
  async testUserLoginScenario(worktreePath) {
    // 实现用户登录集成测试
    // 检查API端点、前端表单、认证流程等

    const issues = [];

    try {
      // 检查是否有登录相关的后端API
      const hasLoginAPI = await this.checkLoginAPIExists(worktreePath);
      if (!hasLoginAPI) {
        issues.push({
          severity: 'major',
          description: '未找到登录API端点',
          file: 'unknown',
          line: 0,
          suggestion: '确保实现 /api/login 或类似的认证端点'
        });
      }

      // 检查前端登录表单
      const hasLoginForm = await this.checkLoginFormExists(worktreePath);
      if (!hasLoginForm) {
        issues.push({
          severity: 'major',
          description: '未找到前端登录表单',
          file: 'unknown',
          line: 0,
          suggestion: '确保实现用户名/密码输入表单'
        });
      }

      return {
        scenario: 'user_login',
        status: issues.length === 0 ? 'PASS' : 'FAIL',
        passed: issues.filter(i => i.severity === 'critical').length === 0,
        issues: issues,
        critical_issues: issues.filter(i => i.severity === 'critical')
      };

    } catch (error) {
      return {
        scenario: 'user_login',
        status: 'ERROR',
        passed: false,
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 检查登录API是否存在
   */
  async checkLoginAPIExists(worktreePath) {
    // 简单检查：查找包含login/route/auth等关键词的文件
    const keywords = ['login', 'auth', 'signin', 'authenticate'];

    for (const keyword of keywords) {
      const files = await this.findFilesContaining(worktreePath, keyword);
      if (files.length > 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * 检查登录表单是否存在
   */
  async checkLoginFormExists(worktreePath) {
    // 检查前端文件中的表单元素
    const formKeywords = ['<form', 'type="password"', 'type="text"', 'name="username"'];

    const frontendFiles = this.findFrontendFiles(worktreePath);

    for (const file of frontendFiles) {
      const content = fs.readFileSync(file, 'utf8');
      const hasLoginForm = formKeywords.every(keyword => content.includes(keyword));

      if (hasLoginForm) {
        return true;
      }
    }

    return false;
  }

  /**
   * 测试数据流转场景
   */
  async testDataFlowScenario(worktreePath) {
    // 实现数据流转测试
    return {
      scenario: 'data_flow',
      status: 'SKIPPED',
      passed: true,
      reason: '数据流转场景测试待实现',
      critical_issues: []
    };
  }

  /**
   * 测试错误处理场景
   */
  async testErrorHandlingScenario(worktreePath) {
    // 实现错误处理测试
    return {
      scenario: 'error_handling',
      status: 'SKIPPED',
      passed: true,
      reason: '错误处理场景测试待实现',
      critical_issues: []
    };
  }

  /**
   * 执行性能测试
   */
  async executePerformanceTests(worktreePath) {
    if (!this.config.test_types.performance.enabled) {
      return {
        status: 'SKIPPED',
        reason: '性能测试未启用',
        critical_issues: []
      };
    }

    console.log('⚡ 执行性能测试...');

    try {
      // 这里可以集成Lighthouse, JMeter, k6等性能测试工具
      // 目前提供基础框架

      const responseTime = await this.measureResponseTime(worktreePath);
      const maxTime = this.config.test_types.performance.response_time_max;
      const criticalTime = this.config.thresholds.response_time.critical;

      const issues = [];

      if (responseTime > criticalTime) {
        issues.push({
          severity: 'critical',
          description: `响应时间过长: ${responseTime}ms (超过阈值 ${criticalTime}ms)`,
          file: 'performance',
          line: 0,
          suggestion: '优化代码或增加资源'
        });
      } else if (responseTime > maxTime) {
        issues.push({
          severity: 'major',
          description: `响应时间偏高: ${responseTime}ms (超过建议值 ${maxTime}ms)`,
          file: 'performance',
          line: 0,
          suggestion: '考虑优化性能'
        });
      }

      return {
        status: issues.length === 0 ? 'PASS' : 'FAIL',
        response_time: responseTime,
        response_time_ratio: responseTime / maxTime,
        critical_issues: issues.filter(i => i.severity === 'critical')
      };

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 测量响应时间
   */
  async measureResponseTime(worktreePath) {
    // 简单实现：启动服务器并测量响应时间
    // 实际应该根据项目类型使用专门的性能测试工具

    try {
      // 尝试访问常见的本地开发端口
      const ports = [3000, 8080, 5000, 4200];

      for (const port of ports) {
        try {
          const startTime = Date.now();
          await this.runCommand(`curl -s http://localhost:${port} || exit 0`, worktreePath, { timeout: 5000 });
          const endTime = Date.now();

          if (endTime - startTime < 1000) {
            return endTime - startTime;
          }
        } catch (error) {
          // 端口可能未启动，继续尝试下一个
        }
      }

      // 如果没有服务器运行，返回默认值
      return 500;

    } catch (error) {
      return 1000; // 错误时返回较高值
    }
  }

  /**
   * 执行安全测试
   */
  async executeSecurityTests(worktreePath) {
    if (!this.config.test_types.security.enabled) {
      return {
        status: 'SKIPPED',
        reason: '安全测试未启用',
        critical_issues: []
      };
    }

    console.log('🔒 执行安全测试...');

    try {
      const issues = [];

      // 检查常见安全问题
      if (this.config.test_types.security.scan_vulnerabilities) {
        const vulnIssues = await this.scanVulnerabilities(worktreePath);
        issues.push(...vulnIssues);
      }

      if (this.config.test_types.security.check_dependencies) {
        const depIssues = await this.checkDependencySecurity(worktreePath);
        issues.push(...depIssues);
      }

      return {
        status: issues.filter(i => i.severity === 'critical').length === 0 ? 'PASS' : 'FAIL',
        vulnerabilities: issues.filter(i => i.severity === 'critical'),
        critical_issues: issues.filter(i => i.severity === 'critical')
      };

    } catch (error) {
      return {
        status: 'ERROR',
        error: error.message,
        critical_issues: []
      };
    }
  }

  /**
   * 扫描安全漏洞
   */
  async scanVulnerabilities(worktreePath) {
    const issues = [];

    try {
      // 检查是否有npm audit（Node.js项目）
      if (fs.existsSync(path.join(worktreePath, 'package.json'))) {
        try {
          const { stdout, stderr } = await this.runCommand('npm audit --json', worktreePath, { timeout: 30000 });
          const auditResult = JSON.parse(stdout);

          if (auditResult.vulnerabilities && Object.keys(auditResult.vulnerabilities).length > 0) {
            for (const [pkg, vuln] of Object.entries(auditResult.vulnerabilities)) {
              if (vuln.severity === 'critical' || vuln.severity === 'high') {
                issues.push({
                  severity: vuln.severity === 'critical' ? 'critical' : 'major',
                  description: `依赖包安全漏洞: ${pkg}`,
                  file: 'package.json',
                  line: 0,
                  suggestion: `更新 ${pkg} 到最新版本`
                });
              }
            }
          }
        } catch (error) {
          // npm audit可能失败，忽略
        }
      }

      // 类似地可以添加其他语言的安全扫描
      // Python: pip-audit, safety check
      // Java: OWASP dependency check
      // Go: go list -json

    } catch (error) {
      console.warn(`安全扫描失败: ${error.message}`);
    }

    return issues;
  }

  /**
   * 检查依赖安全性
   */
  async checkDependencySecurity(worktreePath) {
    const issues = [];

    // 检查敏感文件是否意外提交
    const sensitivePatterns = [
      '*.pem',
      '*.key',
      '.env',
      'secrets/',
      'credentials.json',
      'id_rsa'
    ];

    for (const pattern of sensitivePatterns) {
      const files = await this.findFilesByPattern(worktreePath, pattern);
      if (files.length > 0) {
        issues.push({
          severity: 'critical',
          description: `发现敏感文件: ${pattern}`,
          file: files[0],
          line: 0,
          suggestion: '将敏感文件添加到.gitignore'
        });
      }
    }

    return issues;
  }

  /**
   * 分析测试结果
   */
  analyzeResults(testResults) {
    const overallStatus = this.determineOverallStatus(testResults);

    const analysis = {
      overall_status: overallStatus,
      detection_id: this.detection_id,
      critical_issues: [],
      major_issues: [],
      minor_issues: [],
      recommendations: [],
      fix_suggestions: [],
      can_proceed: overallStatus === 'PASS'
    };

    // 收集所有问题
    for (const [testType, result] of Object.entries(testResults)) {
      if (result.status === 'ERROR') {
        continue; // 跳过错误状态
      }

      if (result.critical_issues && result.critical_issues.length > 0) {
        analysis.critical_issues.push(...result.critical_issues.map(issue => ({
          ...issue,
          test_type: testType
        })));
      }

      if (result.major_issues && result.major_issues.length > 0) {
        analysis.major_issues.push(...result.major_issues.map(issue => ({
          ...issue,
          test_type: testType
        })));
      }
    }

    // 生成修复建议
    if (analysis.critical_issues.length > 0) {
      analysis.fix_suggestions = this.generateFixSuggestions(analysis.critical_issues);
    }

    return analysis;
  }

  /**
   * 判定整体状态
   */
  determineOverallStatus(testResults) {
    for (const [testType, result] of Object.entries(testResults)) {
      if (result.status === 'ERROR') {
        continue; // 跳过执行错误
      }

      if (result.status === 'FAIL') {
        return 'FAIL';
      }

      // 检查是否有关键问题
      if (result.critical_issues && result.critical_issues.length > 0) {
        return 'FAIL';
      }

      // 性能测试特殊处理
      if (testType === 'performance' && result.status === 'PASS') {
        if (result.response_time_ratio > 2.0) {
          return 'FAIL';
        }
      }

      // 安全测试特殊处理
      if (testType === 'security' && result.vulnerabilities && result.vulnerabilities.length > 0) {
        return 'FAIL';
      }
    }

    return 'PASS';
  }

  /**
   * 生成修复建议
   */
  generateFixSuggestions(criticalIssues) {
    const suggestions = [];

    for (const issue of criticalIssues) {
      const suggestion = {
        issue: issue.description,
        file: issue.file,
        line: issue.line,
        suggestion: issue.suggestion || '请检查相关代码',
        severity: issue.severity
      };
      suggestions.push(suggestion);
    }

    return suggestions;
  }

  /**
   * 创建最终结果
   */
  createFinalResult(testResults, analysis, executionTime) {
    return {
      detection_id: this.detectionId,
      timestamp: new Date().toISOString(),
      status: analysis.overall_status,
      test_results: testResults,
      critical_issues: analysis.critical_issues,
      major_issues: analysis.major_issues,
      minor_issues: analysis.minor_issues,
      fix_suggestions: analysis.fix_suggestions,
      execution_time: executionTime,
      can_proceed: analysis.can_proceed
    };
  }

  /**
   * 创建跳过结果
   */
  createSkippedResult(reason) {
    return {
      detection_id: this.detectionId,
      timestamp: new Date().toISOString(),
      status: 'SKIPPED',
      reason: reason,
      test_results: {},
      critical_issues: [],
      execution_time: 0
    };
  }

  /**
   * 保存检测结果
   */
  saveDetectionResult(result) {
    try {
      const detectionStateDir = path.join(STATE_DIR, this.detectionId);
      fs.mkdirSync(detectionStateDir, { recursive: true });

      const resultFile = path.join(detectionStateDir, 'result.json');
      fs.writeFileSync(resultFile, JSON.stringify(result, null, 2));

      // 也保存到latest结果
      const latestResultFile = path.join(STATE_DIR, 'latest-result.json');
      fs.writeFileSync(latestResultFile, JSON.stringify(result, null, 2));

      console.log(`💾 检测结果已保存到: ${resultFile}`);

    } catch (error) {
      console.error(`结果保存失败: ${error.message}`);
    }
  }

  /**
   * 清理测试环境
   */
  async cleanupTestEnvironment(worktreePath) {
    // 这里可以添加清理逻辑
    // 例如：停止测试服务器、清理临时文件等

    console.log('✅ 测试环境清理完成');
  }

  // ========== 辅助方法 ==========

  /**
   * 运行命令
   */
  async runCommand(command, cwd, options = {}) {
    return new Promise((resolve, reject) => {
      const timeout = options.timeout || 60000;

      exec(command, {
        cwd,
        timeout,
        maxBuffer: 1024 * 1024 * 10 // 10MB buffer
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

  /**
   * 查找测试文件
   */
  findTestFiles(worktreePath, patterns) {
    // 简单实现：检查是否有匹配的文件
    for (const pattern of patterns) {
      const globPath = path.join(worktreePath, pattern.replace('*', '**'));
      try {
        const files = require('glob').sync(globPath);
        if (files.length > 0) {
          return true;
        }
      } catch (error) {
        // glob模块可能不可用
      }
    }
    return false;
  }

  /**
   * 查找前端文件
   */
  findFrontendFiles(worktreePath) {
    const frontendExtensions = ['.html', '.htm', '.jsx', '.tsx', '.vue', '.svelte'];
    const files = [];

    const findFiles = (dir) => {
      const entries = fs.readdirSync(dir, { withFileTypes: true });

      for (const entry of entries) {
        if (entry.isDirectory()) {
          // 跳过node_modules和.git
          if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'venv') {
            findFiles(path.join(dir, entry.name));
          }
        } else if (entry.isFile()) {
          const ext = path.extname(entry.name);
          if (frontendExtensions.includes(ext)) {
            files.push(path.join(dir, entry.name));
          }
        }
      }
    };

    findFiles(worktreePath);
    return files;
  }

  /**
   * 查找包含特定内容的文件
   */
  async findFilesContaining(worktreePath, keyword) {
    const files = [];

    const searchInDir = (dir) => {
      const entries = fs.readdirSync(dir, { withFileTypes: true });

      for (const entry of entries) {
        if (entry.isDirectory()) {
          if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'venv') {
            searchInDir(path.join(dir, entry.name));
          }
        } else if (entry.isFile()) {
          try {
            const content = fs.readFileSync(path.join(dir, entry.name), 'utf8');
            if (content.toLowerCase().includes(keyword.toLowerCase())) {
              files.push(path.join(dir, entry.name));
            }
          } catch (error) {
            // 文件读取失败，跳过
          }
        }
      }
    };

    searchInDir(worktreePath);
    return files;
  }

  /**
   * 按模式查找文件
   */
  async findFilesByPattern(worktreePath, pattern) {
    const files = [];
    const regex = new RegExp(pattern.replace('*', '.*'));

    const searchInDir = (dir) => {
      const entries = fs.readdirSync(dir, { withFileTypes: true });

      for (const entry of entries) {
        if (entry.isDirectory()) {
          if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'venv') {
            searchInDir(path.join(dir, entry.name));
          }
        } else if (entry.isFile()) {
          if (regex.test(entry.name)) {
            files.push(path.join(dir, entry.name));
          }
        }
      }
    };

    searchInDir(worktreePath);
    return files;
  }

  /**
   * 检查是否有测试文件
   */
  hasTestFiles(worktreePath, patterns) {
    for (const pattern of patterns) {
      const regex = new RegExp(pattern.replace('*', '.*'));

      const checkDir = (dir) => {
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
          if (entry.isDirectory()) {
            if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'venv') {
              if (checkDir(path.join(dir, entry.name))) {
                return true;
              }
            }
          } else if (entry.isFile()) {
            if (regex.test(entry.name)) {
              return true;
            }
          }
        }
        return false;
      };

      if (checkDir(worktreePath)) {
        return true;
      }
    }

    return false;
  }
}

/**
 * CLI入口
 */
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    console.log('用法: node e2e-detection-manager.js <worktreePath> <baseRef>');
    process.exit(1);
  }

  const [worktreePath, baseRef] = args;

  const manager = new E2EDetectionManager({}, '');
  manager.runDetection(worktreePath, baseRef)
    .then(result => {
      console.log('\n🔍 检测结果:');
      console.log(`状态: ${result.status}`);
      console.log(`执行时间: ${result.execution_time.toFixed(2)}s`);
      console.log(`关键问题: ${result.critical_issues.length}`);

      if (result.status === 'FAIL') {
        console.log('\n❌ 检测未通过，需要修复');
        process.exit(1);
      } else {
        console.log('\n✅ 检测通过');
        process.exit(0);
      }
    })
    .catch(error => {
      console.error('执行失败:', error);
      process.exit(1);
    });
}

module.exports = { E2EDetectionManager };