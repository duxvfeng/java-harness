# Java Harness 故障排查指南

## 概述

本指南提供了 Java Harness 的常见问题诊断和解决方案，帮助您快速解决使用过程中遇到的问题。

## 快速诊断

### 使用诊断命令

```bash
# 运行全面诊断
java-harness doctor

# 检查特定方面
java-harness doctor --check configuration
java-harness doctor --check dependencies
java-harness doctor --check environment
```

## 安装问题

### 1. JVM 版本不兼容

**症状**: `UnsupportedClassVersionError`

**诊断**:
```bash
java -version
# 应该显示 Java 17 或更高版本
```

**解决方案**:
```bash
# 安装 JDK 17
# macOS
brew install openjdk@17

# Linux (Ubuntu/Debian)
sudo apt-get install openjdk-17-jdk

# Windows
# 从 https://adoptium.net/ 下载安装

# 设置 JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
```

### 2. Maven 依赖下载失败

**症状**: `Could not resolve dependencies`

**诊断**:
```bash
# 检查 Maven 配置
mvn -X install | grep "Downloading"

# 检查网络连接
ping repo1.maven.org
```

**解决方案**:
```bash
# 配置国内镜像
cat > ~/.m2/settings.xml << EOF
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
```

### 3. 权限问题

**症状**: `PermissionDenied`

**诊断**:
```bash
# 检查文件权限
ls -la ~/.claude
```

**解决方案**:
```bash
# 修复权限
chmod 755 ~/.claude
chmod 644 ~/.claude/settings.json
```

## 配置问题

### 1. 配置文件格式错误

**症状**: `ConfigurationParseException`

**诊断**:
```bash
# 验证配置文件
java-harness config validate
```

**解决方案**:
```bash
# 修复 JSON 格式
cat > ~/.claude/settings.json << EOF
{
  "plugins": ["claude-code-harness"],
  "skills": ["harness-work"],
  "preferences": {
    "autoCommit": true
  }
}
EOF

# 重新验证
java-harness config validate
```

### 2. Hook 配置冲突

**症状**: Hook 没有被调用

**诊断**:
```bash
# 列出所有 Hook
java-harness hook list

# 检查 Hook 状态
java-harness hook status pre-tool
```

**解决方案**:
```bash
# 重新注册 Hook
java-harness hook register pre-tool

# 启用 Hook
java-harness hook enable pre-tool
```

### 3. 环境变量问题

**症状**: `EnvironmentVariableNotFound`

**诊断**:
```bash
# 检查环境变量
echo $JAVA_HOME
echo $MAVEN_HOME
echo $PATH
```

**解决方案**:
```bash
# 设置环境变量
export JAVA_HOME=/path/to/jdk-17
export MAVEN_HOME=/path/to/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

## 工作流问题

### 1. Plans.md 解析失败

**症状**: `ParseException: Invalid Plans.md format`

**诊断**:
```bash
# 验证 Plans.md 格式
java-harness plans validate
```

**解决方案**:
```bash
# 检查 Plans.md 格式
head -20 Plans.md

# 确保格式正确
# ❌ 错误格式
| Task | 内容 | DoD | Depends | Status |
| 1.1 | 任务 | 测试 | - | TODO |

# ✅ 正确格式
| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | 任务 | 测试 | - | TODO |
```

### 2. 任务依赖循环

**症状**: `CircularDependencyException`

**诊断**:
```bash
# 检查依赖关系
java-harness work --analyze-dependencies
```

**解决方案**:
```bash
# 修复循环依赖
# ❌ 错误: 1.1 -> 1.2 -> 1.3 -> 1.1
| 1.1 | 任务A | 测试 | 1.3 | TODO |
| 1.2 | 任务B | 测试 | 1.1 | TODO |
| 1.3 | 任务C | 测试 | 1.2 | TODO |

# ✅ 正确: 线性依赖
| 1.1 | 任务A | 测试 | - | TODO |
| 1.2 | 任务B | 测试 | 1.1 | TODO |
| 1.3 | 任务C | 测试 | 1.2 | TODO |
```

### 3. 工作流执行超时

**症状**: `WorkflowTimeoutException`

**诊断**:
```bash
# 检查执行时间
java-harness work --status
```

**解决方案**:
```bash
# 增加超时时间
java-harness work --timeout 60m

# 或者拆分大任务
java-harness work --split
```

## Agent 问题

### 1. Agent 启动失败

**症状**: `AgentInitializationException`

**诊断**:
```bash
# 检查 Agent 状态
java-harness agent status

# 查看 Agent 日志
tail -f ~/.claude/logs/agent.log
```

**解决方案**:
```bash
# 重启 Agent
java-harness agent restart

# 如果持续失败，重置 Agent
java-harness agent reset
```

### 2. Agent 性能问题

**症状**: Agent 响应缓慢

**诊断**:
```bash
# 检查 Agent 性能
java-harness agent --performance
```

**解决方案**:
```bash
# 调整 Agent 并发数
java-harness agent --workers 4

# 启用 Agent 缓存
java-harness agent --cache
```

### 3. Backend 连接失败

**症状**: `BackendConnectionException`

**诊断**:
```bash
# 测试 backend 连接
java-harness backend test

# 检查网络连接
ping api.anthropic.com
```

**解决方案**:
```bash
# 配置代理
export HTTP_PROXY=http://proxy.example.com:8080
export HTTPS_PROXY=http://proxy.example.com:8080

# 更换 backend
java-harness backend switch claude
```

## Hook 问题

### 1. Hook 执行失败

**症状**: `HookExecutionException`

**诊断**:
```bash
# 检查 Hook 状态
java-harness hook diagnose pre-tool

# 查看 Hook 日志
tail -f ~/.claude/logs/hook.log
```

**解决方案**:
```bash
# 修复 Hook 实现
# 重新注册 Hook
java-harness hook reload pre-tool
```

### 2. Hook 性能影响

**症状**: Hook 导致性能下降

**诊断**:
```bash
# 分析 Hook 性能
java-harness hook --profile
```

**解决方案**:
```bash
# 优化 Hook 实现
# 或禁用不必要的 Hook
java-harness hook disable slow-hook
```

### 3. Guardrule 规则冲突

**症状**: 规则互相冲突

**诊断**:
```bash
# 检查规则冲突
java-harness guardrail check-conflicts
```

**解决方案**:
```bash
# 调整规则优先级
java-harness guardrule set-priority security-check 1
java-harness guardrule set-priority performance-check 2
```

## CI/CD 问题

### 1. GitHub Actions 集成失败

**症状**: `GitHubIntegrationException`

**诊断**:
```bash
# 测试 GitHub 连接
java-harness ci test github
```

**解决方案**:
```bash
# 配置 GitHub token
java-harness ci configure github --token $GITHUB_TOKEN

# 验证配置
java-harness ci validate github
```

### 2. CI 状态检查失败

**症状**: 无法获取 CI 状态

**诊断**:
```bash
# 检查 CI 配置
java-harness ci status --verbose
```

**解决方案**:
```bash
# 重新配置 CI
java-harness ci configure

# 启用调试模式
java-harness ci --debug
```

### 3. 自动修复失败

**症状**: `AutoRepairException`

**诊断**:
```bash
# 查看修复日志
tail -f ~/.claude/logs/autorepair.log

# 分析失败原因
java-harness repair --analyze
```

**解决方案**:
```bash
# 手动修复
java-harness repair --manual

# 或者跳过自动修复
java-harness repair --skip
```

## 性能问题

### 1. 内存溢出

**症状**: `OutOfMemoryError`

**诊断**:
```bash
# 检查内存使用
java-harness performance memory
```

**解决方案**:
```bash
# 增加堆内存
export JAVA_OPTS="-Xmx4g -Xms2g"

# 启用内存分析
java-harness performance --profile
```

### 2. CPU 使用率过高

**症状**: CPU 使用率持续 100%

**诊断**:
```bash
# 分析 CPU 使用
java-harness performance cpu
```

**解决方案**:
```bash
# 减少并发数
java-harness work --parallel 2

# 启用 CPU 节流
java-harness performance --throttle
```

### 3. 响应时间过长

**症状**: 操作响应缓慢

**诊断**:
```bash
# 分析响应时间
java-harness performance latency
```

**解决方案**:
```bash
# 启用缓存
java-harness performance --cache

# 优化数据库查询
java-harness performance --optimize-db
```

## 网络问题

### 1. 代理配置错误

**症状**: 网络连接失败

**诊断**:
```bash
# 检查网络连接
java-harness network test
```

**解决方案**:
```bash
# 配置代理
java-harness network configure --proxy http://proxy:8080

# 测试连接
java-harness network test --connectivity
```

### 2. DNS 解析失败

**症状**: `UnknownHostException`

**诊断**:
```bash
# 测试 DNS 解析
nslookup api.anthropic.com
```

**解决方案**:
```bash
# 配置 DNS
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf

# 或使用 hosts 文件
echo "127.0.0.1 localhost" | sudo tee -a /etc/hosts
```

### 3. SSL 证书问题

**症状**: `SSLHandshakeException`

**诊断**:
```bash
# 检查证书
java-harness security check-certificate
```

**解决方案**:
```bash
# 导入证书
keytool -import -alias claude -file certificate.crt \
  -keystore $JAVA_HOME/lib/security/cacerts

# 或跳过证书验证（不推荐）
java-harness security --skip-verification
```

## 日志调试

### 1. 启用调试日志

```bash
# 启用详细日志
export JAVA_OPTS="-Dlogging.level=DEBUG"

# 启用特定模块日志
export JAVA_OPTS="-Dlogging.level.com.chachamaru.harness.workflow=DEBUG"
```

### 2. 日志文件位置

```bash
# 主日志文件
~/.claude/logs/harness.log

# Agent 日志
~/.claude/logs/agent.log

# Hook 日志
~/.claude/logs/hook.log

# CI 日志
~/.claude/logs/ci.log
```

### 3. 日志分析

```bash
# 实时查看日志
tail -f ~/.claude/logs/harness.log

# 搜索错误
grep ERROR ~/.claude/logs/harness.log

# 统计错误类型
grep ERROR ~/.claude/logs/harness.log | awk '{print $5}' | sort | uniq -c
```

## 系统监控

### 1. 实时监控

```bash
# 启动监控面板
java-harness monitor

# 查看系统状态
java-harness status
```

### 2. 性能指标

```bash
# 查看性能指标
java-harness performance metrics

# 导出性能报告
java-harness performance report --output performance.json
```

### 3. 健康检查

```bash
# 全面健康检查
java-harness health-check

# 特定模块检查
java-harness health-check --module workflow
```

## 恢复和回滚

### 1. 状态恢复

```bash
# 从备份恢复状态
java-harness state restore --backup state-backup.json

# 重置状态
java-harness state reset
```

### 2. 配置恢复

```bash
# 恢复默认配置
java-harness config reset

# 从备份恢复
java-harness config restore --backup config-backup.json
```

### 3. 完全重置

```bash
# 完全重置（谨慎使用）
java-harness reset --hard

# 备份数据后重置
java-harness reset --backup-first
```

## 获取帮助

### 1. 内置帮助

```bash
# 查看帮助信息
java-harness --help

# 查看特定命令帮助
java-harness work --help

# 查看示例
java-harness work --examples
```

### 2. 在线资源

- **文档**: https://docs.java-harness.dev
- **GitHub Issues**: https://github.com/chachamaru/java-harness/issues
- **Discussions**: https://github.com/chachamaru/java-harness/discussions

### 3. 社区支持

```bash
# 联系支持
java-harness support --ticket "问题描述"

# 加入社区
java-harness community --join
```

## 预防措施

### 1. 定期备份

```bash
# 自动备份
java-harness backup schedule --daily

# 手动备份
java-harness backup create
```

### 2. 健康监控

```bash
# 设置健康检查
java-harness monitor setup --interval 5m

# 配置告警
java-harness alert configure --email admin@example.com
```

### 3. 更新维护

```bash
# 检查更新
java-harness update check

# 应用更新
java-harness update apply
```

## 故障排查清单

遇到问题时，按以下顺序检查：

- [ ] 运行 `java-harness doctor` 进行诊断
- [ ] 检查日志文件中的错误信息
- [ ] 验证配置文件格式正确
- [ ] 确认网络连接正常
- [ ] 检查系统资源使用情况
- [ ] 尝试重启相关服务
- [ ] 查看是否有已知问题
- [ ] 寻求社区支持

## 总结

本故障排查指南涵盖了 Java Harness 的主要问题和解决方案。通过系统化的诊断流程和有效的解决方法，您可以快速定位和解决问题。

记住：
1. 使用 `doctor` 命令快速诊断
2. 检查日志文件获取详细信息
3. 遵循最佳实践预防问题
4. 遇到困难时寻求社区支持

祝您使用愉快！🔧