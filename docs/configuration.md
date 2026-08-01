# Java Harness 配置指南

## 配置概述

Java Harness 支持多种配置方式，按优先级排序：

1. **命令行参数** - 最高优先级
2. **环境变量** - 中等优先级
3. **配置文件** - 默认优先级
4. **默认值** - 最低优先级

## 配置文件

### 默认配置文件位置

- **Unix/Linux**: `~/.config/harness/harness.yaml`
- **macOS**: `~/Library/Application Support/harness/harness.yaml`
- **Windows**: `%APPDATA%\harness\harness.yaml`
- **项目本地**: `.claude/harness.yaml`

### 配置文件模板

```yaml
# ============================================
# Java Harness 配置文件
# ============================================

# Hook 配置
hook:
  # 是否启用 Hook 处理
  enabled: true

  # Hook 超时时间（毫秒）
  timeout: 10000

  # 最大并发 Hook 数
  max_concurrent: 10

  # Hook 缓存配置
  cache:
    enabled: true
    size: 1000
    ttl: 3600

# Guardrail 配置
guardrail:
  # 是否启用 Guardrail 引擎
  enabled: true

  # 启用的规则列表
  rules:
    - R01  # NoSudo
    - R02  # ProtectedPath
    - R03  # RedirectionBypass
    - R04  # ProjectPath
    - R05  # RmRf
    - R06  # GitPushForce
    - R07  # CodexDirectWrite
    - R08  # BreezingWrite
    - R09  # SecretRead
    - R10  # NoVerify
    - R11  # GitResetHard
    - R12  # ProtectedBranchPush
    - R13  # PackageFile
    - R14  # BillingEgress
    - R15  # ProductionDeploy

  # 自定义规则路径
  custom_rules_path: ~/.config/harness/custom-rules/

# 日志配置
logging:
  # 日志级别: TRACE, DEBUG, INFO, WARN, ERROR
  level: INFO

  # 日志文件路径
  file: /var/log/harness/harness.log

  # 日志格式
  format: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

  # 控制台输出
  console:
    enabled: true
    level: INFO

  # 文件输出
  file_output:
    enabled: true
    level: DEBUG
    max_size: 100MB
    max_history: 30

# 性能配置
performance:
  # Native Image 启动优化
  startup_optimization: true

  # JIT 编译优化
  jit_optimization: true

  # 内存限制（MB）
  memory_limit: 512

  # 线程池配置
  thread_pool:
    core_size: 4
    max_size: 16
    queue_size: 1000

# 集成配置
integration:
  # Claude Code 集成
  claude_code:
    enabled: true
    plugin_path: ~/.claude

  # Git 集成
  git:
    enabled: true
    protected_branches:
      - main
      - master
      - develop

  # CI/CD 集成
  cicd:
    enabled: false
    platform: jenkins  # jenkins, github, gitlab

# 安全配置
security:
  # 权限模式: default, auto, bypass_permissions
  permission_mode: default

  # 敏感路径保护
  protected_paths:
    - ~/.ssh
    - ~/.gnupg
    - /etc
    - /usr/local/etc

  # 敏感文件模式
  secret_patterns:
    - "*.key"
    - "*.pem"
    - "*secret*"
    - "*password*"

# 开发配置
development:
  # 调试模式
  debug: false

  # 热重载
  hot_reload: false

  # Profile 激活
  profiles:
    - development

  # 测试配置
  test:
    coverage_threshold: 75
    timeout: 30000
```

## 环境变量配置

### Hook 相关环境变量

```bash
# Hook 超时时间（毫秒）
export HARNESS_HOOK_TIMEOUT=10000

# Hook 缓存大小
export HARNESS_HOOK_CACHE_SIZE=1000

# Hook 并发数
export HARNESS_MAX_CONCURRENT_HOOKS=10
```

### Guardrail 相关环境变量

```bash
# 启用/禁用 Guardrail
export HARNESS_GUARDRAIL_ENABLED=true

# 自定义规则路径
export HARNESS_CUSTOM_RULES_PATH=~/.config/harness/custom-rules/

# 启用的规则（逗号分隔）
export HARNESS_ENABLED_RULES=R01,R02,R03,R04,R05
```

### 日志相关环境变量

```bash
# 日志级别
export HARNESS_LOG_LEVEL=DEBUG

# 日志文件路径
export HARNESS_LOG_FILE=/var/log/harness/harness.log

# 结构化日志（JSON格式）
export HARNESS_STRUCTURED_LOGGING=true
```

### 性能相关环境变量

```bash
# 内存限制（MB）
export HARNESS_MEMORY_LIMIT=512

# 线程池核心大小
export HARNESS_THREAD_POOL_CORE_SIZE=4

# 线程池最大大小
export HARNESS_THREAD_POOL_MAX_SIZE=16

# 启用性能监控
export HARNESS_PERFORMANCE_MONITORING=true
```

### 安全相关环境变量

```bash
# 权限模式
export HARNESS_PERMISSION_MODE=default

# 保护路径（冒号分隔）
export HARNESS_PROTECTED_PATHS=~/.ssh:~/.gnupg:/etc

# 启用审计日志
export HARNESS_AUDIT_ENABLED=true
```

## 命令行参数

### 基本命令

```bash
# 显示版本信息
java -jar harness.jar --version

# 显示帮助信息
java -jar harness.jar --help

# 运行 Hook 处理
java -jar harness.jar hook --input stdin --output stdout
```

### 配置选项

```bash
# 指定配置文件
java -jar harness.jar --config /path/to/config.yaml

# 覆盖配置值
java -jar harness.jar --hook.timeout 15000

# 启用调试模式
java -jar harness.jar --debug

# 设置日志级别
java -jar harness.jar --log-level DEBUG
```

### Hook 命令

```bash
# 启用 Hook 处理
java -jar harness.jar hook enable

# 禁用 Hook 处理
java -jar harness.jar hook disable

# 测试 Hook 配置
java -jar harness.jar hook test

# 验证 Hook 规则
java -jar harness.jar hook validate
```

### Guardrail 命令

```bash
# 列出所有规则
java -jar harness.jar guardrail list

# 测试特定规则
java -jar harness.jar guardrail test R01

# 启用规则
java -jar harness.jar guardrail enable R01

# 禁用规则
java -jar harness.jar guardrail disable R01

# 验证规则配置
java -jar harness.jar guardrail validate
```

## 高级配置

### 自定义 Guardrail 规则

创建自定义规则文件 `~/.config/harness/custom-rules/my-rule.yaml`:

```yaml
name: "R16MyCustomRule"
description: "My custom guardrail rule"
version: "1.0.0"
category: "security"

conditions:
  - tool_name: "Bash"
    command_patterns:
      - ".*dangerous.*"
      - ".* risky.*"

actions:
  - decision: "DENY"
    reason: "Custom rule detected dangerous command"
    severity: "high"
```

### 性能调优

#### Native Image 优化

```bash
# 编译时优化
cd java-harness-cli
mvn -Pnative \
    -Dnative.image.build-args="--enable-optimizations" \
    native:compile

# 运行时优化
export HARNESS_NATIVE_IMAGE_OPTS="-XX:+UseSerialGC -Xmx512m"
./target/harness
```

#### JVM 调优

```bash
# 启动优化
export JAVA_OPTS="-XX:+UseG1GC -Xms512m -Xmx2g"

# JIT 编译优化
export JAVA_OPTS="$JAVA_OPTS -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# 启动应用
java -jar harness.jar
```

### 集成配置

#### Claude Code 集成

```yaml
integration:
  claude_code:
    enabled: true
    plugin_path: ~/.claude
    hook_integration: true
    skill_integration: true
```

#### Git 集成

```yaml
integration:
  git:
    enabled: true
    protected_branches:
      - main
      - master
      - develop
      - release/*
    pre_commit_hooks:
      - harness-validate
      - harness-audit
```

## 配置验证

### 验证配置文件

```bash
# 验证配置文件语法
java -jar harness.jar config validate

# 测试配置
java -jar harness.jar config test

# 显示当前配置
java -jar harness.jar config show
```

### 配置诊断

```bash
# 运行诊断工具
java -jar harness.jar doctor

# 生成诊断报告
java -jar harness.jar doctor --report diagnostic-report.json

# 检查集成
java -jar harness.jar doctor --check-integration
```

## 最佳实践

### 1. 环境隔离

```bash
# 开发环境
export HARNESS_ENV=development
export HARNESS_LOG_LEVEL=DEBUG

# 生产环境
export HARNESS_ENV=production
export HARNESS_LOG_LEVEL=WARN
```

### 2. 安全配置

```bash
# 最小权限原则
export HARNESS_PERMISSION_MODE=default

# 启用审计
export HARNESS_AUDIT_ENABLED=true

# 保护敏感路径
export HARNESS_PROTECTED_PATHS=~/.ssh:~/.gnupg:/etc
```

### 3. 性能优化

```bash
# Native Image 用于生产
export HARNESS_USE_NATIVE_IMAGE=true

# 合理的内存限制
export HARNESS_MEMORY_LIMIT=512

# 线程池调优
export HARNESS_THREAD_POOL_CORE_SIZE=4
export HARNESS_THREAD_POOL_MAX_SIZE=16
```

### 4. 日志管理

```bash
# 结构化日志便于分析
export HARNESS_STRUCTURED_LOGGING=true

# 适当的日志级别
export HARNESS_LOG_LEVEL=INFO

# 日志轮转
export HARNESS_LOG_MAX_SIZE=100MB
export HARNESS_LOG_MAX_HISTORY=30
```

## 故障排除

### 配置加载问题

```bash
# 检查配置文件权限
ls -la ~/.config/harness/harness.yaml

# 验证配置语法
java -jar harness.jar config validate

# 查看详细日志
export HARNESS_LOG_LEVEL=TRACE
java -jar harness.jar --debug
```

### 性能问题

```bash
# 生成性能报告
java -jar harness.jar --profile

# 检查内存使用
java -jar harness.jar --memory-stats

# 分析线程状态
java -jar harness.jar --thread-dump
```

### Hook 问题

```bash
# 测试 Hook 处理
echo '{"test": "data"}' | java -jar harness.jar hook test

# 检查 Hook 状态
java -jar harness.jar hook status

# 重置 Hook 配置
java -jar harness.jar hook reset
```

## 参考配置

### 开发环境配置

```yaml
hook:
  timeout: 30000  # 更长的超时用于调试
guardrail:
  enabled: false   # 开发时可以禁用
logging:
  level: DEBUG
development:
  debug: true
  hot_reload: true
```

### 生产环境配置

```yaml
hook:
  timeout: 10000
  cache:
    enabled: true
guardrail:
  enabled: true
  rules: ALL  # 启用所有规则
logging:
  level: WARN
  file_output:
    enabled: true
security:
  permission_mode: default
  audit:
    enabled: true
```

### CI/CD 环境配置

```yaml
hook:
  timeout: 5000
  max_concurrent: 20
guardrail:
  enabled: true
logging:
  level: INFO
  structured: true
integration:
  cicd:
    enabled: true
    platform: jenkins
```

## 下一步

- 查看[安装指南](installation.md)了解安装步骤
- 阅读[迁移指南](migration.md)从其他工具迁移
- 探索[项目文档](../README.md)了解完整功能