#!/usr/bin/env node
/**
 * 配置文件更新脚本
 *
 * 用于更新端到端检测配置文件
 *
 * @author Harness System
 * @version 1.0.0
 */

const fs = require('fs');
const path = require('path');

/**
 * 解析命令行参数
 */
function parseArgs() {
  const args = process.argv.slice(2);
  const options = {};

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];

    switch (arg) {
      case '--config':
        options.config = args[++i];
        break;
      case '--enabled':
        options.enabled = args[++i] === 'true';
        break;
      case '--mode':
        options.mode = args[++i];
        break;
      case '--timeout':
        options.timeout = parseInt(args[++i]);
        break;
      case '--max-retries':
        options.maxRetries = parseInt(args[++i]);
        break;
      case '--auto-fix':
        options.autoFix = args[++i] === 'true';
        break;
      default:
        // 未知参数，忽略
        break;
    }
  }

  return options;
}

/**
 * 更新配置文件
 */
function updateConfig(configPath, options) {
  try {
    // 读取现有配置
    let config = {};
    if (fs.existsSync(configPath)) {
      const content = fs.readFileSync(configPath, 'utf8');
      config = JSON.parse(content);
    }

    // 更新配置项
    if (options.enabled !== undefined) {
      config.enabled = options.enabled;
    }

    if (options.mode !== undefined) {
      config.mode = options.mode;
    }

    if (options.timeout !== undefined) {
      config.timeout = options.timeout;
    }

    if (options.maxRetries !== undefined) {
      config.max_retries = options.maxRetries;
    }

    if (options.autoFix !== undefined) {
      if (!config.auto_fix) {
        config.auto_fix = {};
      }
      config.auto_fix.enabled = options.autoFix;
    }

    // 确保test_types部分存在
    if (!config.test_types) {
      config.test_types = {
        frontend: { enabled: true },
        backend: { enabled: true },
        integration: { enabled: true },
        performance: { enabled: false },
        security: { enabled: true }
      };
    }

    // 保存更新后的配置
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2), 'utf8');

    console.log(`✅ 配置已更新: ${configPath}`);

    return config;

  } catch (error) {
    console.error(`❌ 配置更新失败: ${error.message}`);
    process.exit(1);
  }
}

/**
 * 主函数
 */
function main() {
  const options = parseArgs();

  if (!options.config) {
    console.error('❌ 缺少 --config 参数');
    process.exit(1);
  }

  // 检查配置文件是否存在
  if (!fs.existsSync(options.config)) {
    console.error(`❌ 配置文件不存在: ${options.config}`);
    process.exit(1);
  }

  // 更新配置
  updateConfig(options.config, options);
}

// CLI 入口
if (require.main === module) {
  main();
}

module.exports = { updateConfig };