# Java Harness 迁移指南

## 迁移概述

本指南帮助您从其他工具或旧版本迁移到 Java Harness。我们提供了详细的迁移步骤、兼容性说明和最佳实践。

## 支持的迁移路径

### 从 Go 版本迁移

Go 版本的 Claude Code Harness 用户可以无缝迁移到 Java 版本。

#### 功能对照表

| 功能 | Go 版本 | Java 版本 | 状态 |
|------|---------|-----------|------|
| Hook 协议处理 | ✅ | ✅ | 完全兼容 |
| Guardrail 规则 (R01-R15) | ✅ | ✅ | 完全兼容 |
| Plans.md 解析 | ✅ | ✅ | 完全兼容 |
| 技能系统 | ✅ | ✅ | 完全兼容 |
| 代理系统 | ✅ | ✅ | 完全兼容 |
| 状态恢复 | ✅ | ✅ | 完全兼容 |
| Native Image | ❌ | ✅ | **新增功能** |
| 分布式支持 | ❌ | ✅ | **新增功能** |

#### 迁移步骤

**1. 备份现有数据**

```bash
# 备份 Go 版本配置
cp -r ~/.claude ~/.claude.go.backup

# 备份项目状态
cp -r .claude .claude.go.backup
```

**2. 安装 Java 版本**

```bash
# 按照[安装指南](installation.md)安装 Java Harness
git clone https://github.com/your-org/java-harness.git
cd java-harness
mvn clean package
```

**3. 迁移配置文件**

```bash
# 复制配置文件
cp ~/.claude.go.backup/config.json ~/.config/harness/harness.yaml

# 转换配置格式
java -jar harness.jar tools migrate-config \
    --from go \
    --to java \
    --input ~/.claude.go.backup/config.json \
    --output ~/.config/harness/harness.yaml
```

**4. 验证迁移**

```bash
# 运行验证工具
java -jar harness.jar doctor --check-migration

# 测试基本功能
echo '{"test": "data"}' | java -jar harness.jar hook test
```

**5. 更新集成点**

```bash
# 更新 Claude Code 插件配置
java -jar harness.jar tools update-claude-code-plugin

# 更新 CI/CD 集成
java -jar harness.jar tools update-cicd-integration
```

#### 配置格式转换

**Go JSON 配置 → Java YAML 配置**

```json
// Go config.json
{
  "hook": {
    "enabled": true,
    "timeout": 10000
  },
  "guardrail": {
    "rules": ["R01", "R02", "R03"]
  }
}
```

转换为：

```yaml
# Java harness.yaml
hook:
  enabled: true
  timeout: 10000

guardrail:
  rules:
    - R01
    - R02
    - R03
```

### 从旧版本迁移

#### 从 4.0.x 升级到 4.1.0

**主要变更**

- ✅ 新增 Native Image 支持
- ✅ 性能优化（启动时间 < 100ms）
- ✅ 新增分布式支持
- ✅ 改进的状态恢复机制
- ⚠️ 配置格式变更（JSON → YAML）

**迁移步骤**

**1. 备份现有版本**

```bash
# 创建备份目录
mkdir -p ~/harness-backup/4.0.0

# 备份安装
cp -r ~/harness ~/harness-backup/4.0.0/harness

# 备份配置
cp -r ~/.config/harness ~/harness-backup/4.0.0/config

# 备份项目状态
cp -r .claude ~/harness-backup/4.0.0/claude-state
```

**2. 升级软件**

```bash
# 下载新版本
wget https://github.com/your-org/java-harness/releases/download/v4.1.0/harness-4.1.0.jar

# 或从源码构建
git clone https://github.com/your-org/java-harness.git
cd java-harness
git checkout v4.1.0
mvn clean package
```

**3. 转换配置**

```bash
# 运行迁移工具
java -jar harness-4.1.0.jar tools migrate \
    --from-version 4.0.0 \
    --to-version 4.1.0 \
    --backup-dir ~/harness-backup/4.0.0
```

**4. 验证升级**

```bash
# 运行测试套件
mvn verify

# 验证功能
java -jar harness-4.1.0.jar doctor --full-check

# 测试 Hook 功能
echo '{"test": "data"}' | java -jar harness-4.1.0.jar hook test
```

**5. 清理旧版本**

```bash
# 验证成功后删除旧版本
rm -rf ~/harness

# 可选：保留备份
# rm -rf ~/harness-backup/4.0.0
```

#### 从 3.x 升级到 4.1.0

**主要变更**

- ✅ 完整的架构重构（7层架构）
- ✅ 新的模块系统
- ✅ 改进的 Plans.md 解析器
- ⚠️ 破坏性 API 变更
- ⚠️ 配置格式完全变更

**迁移步骤**

**1. 备份所有数据**

```bash
# 创建完整备份
mkdir -p ~/harness-backup/3.x
cp -r ~/harness ~/harness-backup/3.x/
cp -r ~/.config/harness ~/harness-backup/3.x/
cp -r .claude ~/harness-backup/3.x/
```

**2. 导出旧数据**

```bash
# 导出 Plans.md 状态
java -jar harness-3.x.jar tools export-state \
    --output ~/harness-backup/3.x/state-export.json

# 导出配置
java -jar harness-3.x.jar tools export-config \
    --output ~/harness-backup/3.x/config-export.json
```

**3. 安装新版本**

```bash
# 按照安装指南安装 4.1.0
# 见 [installation.md](installation.md)
```

**4. 导入数据**

```bash
# 导入并转换配置
java -jar harness-4.1.0.jar tools import-config \
    --input ~/harness-backup/3.x/config-export.json \
    --convert-to-yaml

# 导入状态
java -jar harness-4.1.0.jar tools import-state \
    --input ~/harness-backup/3.x/state-export.json
```

**5. 更新代码集成**

```bash
# 代码扫描工具
java -jar harness-4.1.0.jar tools scan-code \
    --path ./src \
    --report api-changes.md

# 根据报告更新代码
# 编辑受影响的文件
```

**6. 验证迁移**

```bash
# 运行完整测试
mvn verify

# 功能验证
java -jar harness-4.1.0.jar doctor --comprehensive-check
```

## 数据迁移

### Plans.md 迁移

**格式变更**

Java 版本支持更丰富的 Plans.md 格式：

```markdown
# 旧格式 (3.x)
| Task | Description | Status |
|------|-------------|--------|
| 1.1 | 创建基础模块 | TODO |

# 新格式 (4.1.0)
| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | 创建基础模块 | 单元测试通过 | - | TODO |
```

**自动转换**

```bash
# 转换 Plans.md
java -jar harness.jar tools migrate-plans \
    --input Plans.md \
    --output Plans.new.md \
    --add-dod \
    --add-deps
```

### 状态文件迁移

**状态文件结构变更**

```bash
# 旧版本状态
.claude/state.json

# 新版本状态
.claude/state/
├── active-task.json
├── contracts/
├── audit/
└── timeline.jsonl
```

**迁移命令**

```bash
# 迁移状态文件
java -jar harness.jar tools migrate-state \
    --input .claude/state.json \
    --output .claude/state/
```

### 技能文件迁移

**技能格式变更**

```markdown
# 旧格式
## Skill: MySkill
Description: My skill

# 新格式
---
name: my-skill
description: My skill
version: 1.0.0
---

# MySkill

My skill description.
```

**迁移命令**

```bash
# 迁移技能文件
java -jar harness.jar tools migrate-skills \
    --input .claude/skills/ \
    --output .claude/skills.new/
```

## API 迁移

### Hook API 变更

**旧 API (3.x)**

```java
HookResult processHook(HookRequest request);
```

**新 API (4.1.0)**

```java
CompletableFuture<HookOutput> handle(HookInput input);
```

**迁移示例**

```java
// 旧代码
HookResult result = harness.processHook(request);

// 新代码
HookInput input = new HookInput(
    request.getSessionId(),
    request.getTranscriptPath(),
    request.getCwd(),
    PermissionMode.valueOf(request.getMode()),
    HookEventType.valueOf(request.getEvent()),
    request.getToolName(),
    request.getToolInput(),
    request.getPluginRoot()
);

CompletableFuture<HookOutput> future = harness.handle(input);
HookOutput output = future.get();
```

### Guardrail API 变更

**旧 API (3.x)**

```java
List<Rule> getRules();
RuleResult evaluate(Rule rule, HookRequest request);
```

**新 API (4.1.0)**

```java
List<Rule> matchRules(HookInput input);
GuardrailResult evaluate(HookInput input);
```

**迁移示例**

```java
// 旧代码
List<Rule> rules = harness.getRules();
for (Rule rule : rules) {
    if (rule.matches(request)) {
        RuleResult result = harness.evaluate(rule, request);
        if (result.isDenied()) {
            return Result.deny(result.getReason());
        }
    }
}

// 新代码
GuardrailEngine engine = new GuardrailEngine(ruleRegistry);
GuardrailResult result = engine.evaluate(input);
if (result.isDenied()) {
    return HookOutput.denied(result.getReason());
}
```

## 性能优化迁移

### Native Image 编译

**准备工作**

```bash
# 确保 GraalVM 已安装
java -version

# 安装 native-image 工具
gu install native-image
```

**编译步骤**

```bash
cd java-harness-cli
mvn -Pnative native:compile
```

**性能验证**

```bash
# 测试启动时间
time ./target/harness --version
# 预期: < 100ms

# 测试内存使用
./target/harness --memory-stats
# 预期: < 50MB

# 测试 Hook 响应时间
./target/harness --benchmark
# 预期: < 10ms (95th percentile)
```

### JVM 调优

**生产环境 JVM 参数**

```bash
export JAVA_OPTS="
-XX:+UseG1GC
-Xms512m
-Xmx2g
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
"
```

## 集成迁移

### Claude Code 集成

**插件配置更新**

```json
// 旧版本插件配置
{
  "name": "claude-code-harness",
  "version": "3.0.0",
  "hookHandler": "go-harness"
}

// 新版本插件配置
{
  "name": "claude-code-harness",
  "version": "4.1.0",
  "hookHandler": "java-harness",
  "nativeImage": true,
  "configPath": "~/.config/harness/harness.yaml"
}
```

**更新命令**

```bash
# 更新插件配置
java -jar harness.jar tools update-plugin-config

# 验证集成
java -jar harness.jar tools test-claude-code-integration
```

### CI/CD 集成

**Jenkins Pipeline 更新**

```groovy
// 旧版本
pipeline {
    stages {
        stage('Harness') {
            steps {
                sh './go-harness hook validate'
            }
        }
    }
}

// 新版本
pipeline {
    stages {
        stage('Harness') {
            steps {
                sh 'java -jar harness.jar hook validate'
            }
        }
    }
}
```

### Git Hooks 迁移

**更新 Git Hooks**

```bash
# 旧版本
echo '#!/bin/bash
./go-harness hook pre-commit' > .git/hooks/pre-commit

# 新版本
echo '#!/bin/bash
java -jar harness.jar hook pre-commit' > .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## 验证和测试

### 迁移验证清单

- [ ] 备份创建成功
- [ ] 新版本安装完成
- [ ] 配置文件转换成功
- [ ] 状态文件迁移成功
- [ ] Plans.md 格式正确
- [ ] Hook 功能正常
- [ ] Guardrail 规则生效
- [ ] 技能系统工作正常
- [ ] 代理协调正常
- [ ] 性能指标达标
- [ ] 集成测试通过
- [ ] 用户验收测试通过

### 功能测试

**基础功能测试**

```bash
# Hook 处理测试
echo '{"test": "data"}' | java -jar harness.jar hook test

# Guardrail 测试
java -jar harness.jar guardrail test R01

# 配置验证
java -jar harness.jar config validate
```

**集成功能测试**

```bash
# Plans.md 解析测试
java -jar harness.jar workflow parse Plans.md

# 技能执行测试
java -jar harness.jar skill run plan

# 代理协调测试
java -jar harness.jar agent test coordinator
```

**性能测试**

```bash
# 启动时间测试
time java -jar harness.jar --version

# Hook 响应时间测试
java -jar harness.jar benchmark --hook

# 内存使用测试
java -jar harness.jar memory --stats
```

## 回滚计划

### 回滚步骤

**1. 停止新版本**

```bash
# 停止运行中的实例
pkill -f harness-4.1.0
```

**2. 恢复备份**

```bash
# 恢复旧版本
cp -r ~/harness-backup/4.0.0/harness ~/harness

# 恢复配置
cp -r ~/harness-backup/4.0./config ~/.config/harness

# 恢复状态
cp -r ~/harness-backup/4.0.0/claude-state .claude
```

**3. 重启旧版本**

```bash
# 启动旧版本
~/harness/bin/harness start

# 验证功能
echo '{"test": "data"}' | ~/harness/bin/harness hook test
```

### 回滚验证

```bash
# 运行旧版本测试
~/harness/bin/harness doctor --full-check

# 验证状态恢复
~/harness/bin/harness state verify

# 验证集成功能
~/harness/bin/harness integration test
```

## 常见问题

### Q1: 配置转换失败

**问题**: 配置文件转换时出现错误

**解决方案**:

```bash
# 手动验证配置
java -jar harness.jar config validate --input config.yaml

# 使用详细日志
java -jar harness.jar --debug config convert \
    --from json \
    --to yaml \
    --input config.json \
    --output config.yaml
```

### Q2: 状态迁移丢失数据

**问题**: 状态迁移后数据不完整

**解决方案**:

```bash
# 检查备份
ls -la ~/harness-backup/

# 重新导入状态
java -jar harness.jar state import \
    --input ~/harness-backup/state.json \
    --verify

# 手动合并状态
java -jar harness.jar state merge \
    --source ~/harness-backup/state.json \
    --target .claude/state/
```

### Q3: 性能下降

**问题**: 迁移后性能不如预期

**解决方案**:

```bash
# 使用 Native Image
cd java-harness-cli
mvn -Pnative native:compile

# JVM 调优
export JAVA_OPTS="-XX:+UseG1GC -Xms512m -Xmx2g"

# 性能分析
java -jar harness.jar --profile --output profile.html
```

### Q4: API 不兼容

**问题**: 代码使用旧 API 无法编译

**解决方案**:

```bash
# API 兼容性检查
java -jar harness.jar tools scan-api \
    --path ./src \
    --report api-report.md

# 使用兼容性层
export HARNESS_COMPATIBILITY_MODE=true
```

## 最佳实践

### 1. 渐进式迁移

```bash
# 阶段1: 并行运行
./go-harness start
java -jar harness.jar start --port 8081

# 阶段2: 逐步切换流量
# 10% → 50% → 100%

# 阶段3: 完全切换
java -jar harness.jar start
```

### 2. 数据同步

```bash
# 实时同步状态
java -jar harness.jar tools sync-state \
    --source ./go-harness/.claude \
    --target .claude \
    --continuous
```

### 3. 监控和日志

```bash
# 启用详细日志
export HARNESS_LOG_LEVEL=DEBUG

# 监控迁移过程
java -jar harness.jar --monitor > migration.log
```

### 4. 测试驱动迁移

```bash
# 运行完整测试套件
mvn verify

# 集成测试
mvn verify -Pintegration

# 性能测试
mvn verify -Pperformance
```

## 支持

- **迁移工具**: `java -jar harness.jar tools migrate --help`
- **诊断工具**: `java -jar harness.jar doctor --help`
- **文档**: [完整文档](../README.md)
- **问题反馈**: GitHub Issues
- **社区支持**: Discord 社区

## 下一步

- 查看[安装指南](installation.md)了解安装步骤
- 阅读[配置指南](configuration.md)了解详细配置
- 探索[项目文档](../README.md)了解完整功能