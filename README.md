# Claude Code Harness - Java Implementation

Java版本的Claude Code Harness，实现CLI Gateway核心功能，包括Hook协议处理、Guardrail安全引擎和快速响应机制。

## 项目概述

这是claude-code-harness的Java原生实现，目标是通过GraalVM编译为Native Image，实现**<10ms的hook响应时间**，为Claude Code提供实时的安全策略执行。

### 核心特性

- **🚀 高性能**: GraalVM Native Image编译，亚毫秒级响应
- **🔒 安全防护**: 27个Guardrail规则（R01-R27）全覆盖
- **📡 Hook协议**: 完整的Claude Code Hook事件处理（16个hook子命令）
- **🎯 模块化设计**: 命令组 + 独立命令，与Go版本完全一致
- **📋 86个CLI命令**: 完全复制Go版本的命令结构，采用kebab-case命名

## 架构设计

### 命令结构

```
harness (主命令)
├── hook (16个子命令)
│   ├── pre-tool              PreToolUse guardrail evaluation
│   ├── post-tool             PostToolUse tampering/security checks
│   ├── permission            PermissionRequest auto-approval
│   ├── session-start         SessionStart env setup
│   ├── post-tool-failure     PostToolUseFailure counter & escalation
│   ├── post-compact          PostCompact WIP context re-injection
│   ├── notification          Notification event logging
│   ├── permission-denied     PermissionDenied event logging
│   ├── ask-user-question-normalize  AskUserQuestion answer bridge
│   ├── session-init          Session initialization + Plans.md summary
│   ├── session-cleanup       SessionEnd temp file cleanup
│   ├── session-monitor       Project state collection + session.json
│   ├── session-summary       Session summary to session-log.md
│   ├── ci-status             CI status check after push/PR
│   ├── subagent-start        Track agent lifecycle start
│   └── subagent-stop         Track agent lifecycle stop
├── evidence
│   └── collect               Collect evidence (test results, build logs)
├── plans
│   └── check-deps            Verify done tasks only depend on closed tasks
├── plan                      Emit the plan prompt for the host to execute
├── work <taskID>             Emit the work prompt + task context
├── review <taskID>           Emit the review prompt + task context
├── release                   Emit the release prompt for the host to execute
├── gen [hooks] [--check]     Generate per-host hooks.json from hosts.toml
├── sprint-contract           Generate sprint-contract from Plans.md
├── status                    Show all tracked agent states
├── init [root]               Create harness.toml template in project root
├── sync [root]               Generate CC files from harness.toml
├── validate                  Validate SKILL.md / agent frontmatter
├── doctor [--migration]      Health check plus migration status/report
├── codex-loop                Run the Codex-native long-running loop
├── mem                       Manage harness-mem companion
├── channels-wake             Bridge channel health check
├── inbox                     Inbox management
├── session                   Session management
├── self-audit                Audit settings.local.json command hooks
├── retired-alias             Scan repo for retired alias residue
├── night-watch               Emit night-watch patrol report
├── failure-codifier          Emit failure-rule.v1 proposals
├── mirror                    Report skills/ mirror drift
├── wt                        Worktree fingerprint operations
├── impact-score              Compute judgment-card impact_score
├── pre-compact               Evaluate whether PreCompact should be blocked
├── version                   Print version
└── ... (更多命令)
```

### 多模块结构

```
java-harness/
├── java-harness-cli/              # CLI模块（主入口）
│   └── command/                   # 86个命令类
│       ├── hook/                  # hook命令组（16个子命令）
│       ├── evidence/              # evidence命令组
│       ├── plan/                  # plans命令组
│       └── *.java                 # 独立命令
├── java-harness-shared/           # 共享模块
├── java-harness-foundation/       # 基础模块
├── java-harness-protocol/         # 协议模块
├── java-harness-security/         # 安全模块
├── java-harness-workflow/         # 工作流模块
├── java-harness-tools/            # 工具模块
├── java-harness-collaboration/    # 协作模块
├── java-harness-ci/               # CI模块
├── java-harness-service/          # 服务模块
└── java-harness-distribution/     # 分发模块
```

### 技术栈

- **JDK 17** - 基础运行时
- **GraalVM 23.1.0** - Native Image编译
- **picocli 4.7** - CLI框架
- **Jackson 2.15.2** - JSON处理
- **SLF4J 2.0.9** - 日志接口
- **Logback 1.4.11** - 日志实现
- **JUnit 5.10.0** - 单元测试

## 安全规则（R01-R27）

| 规则 | 功能 | 状态 |
|------|------|------|
| **R01** | 阻止提权命令 | ✅ |
| **R02** | 保护敏感路径 | ✅ |
| **R03** | 阻止重定向绕过 | ✅ |
| **R04** | 项目路径边界 | ✅ |
| **R05** | 防止递归删除 | ✅ |
| **R06** | 阻止强制推送 | ✅ |
| **R07** | Codex写入监控 | ✅ |
| **R08** | Breezing写入监控 | ✅ |
| **R09** | 密钥文件保护 | ✅ |
| **R10** | 验证绕过阻止 | ✅ |
| **R11** | 硬重置防护 | ✅ |
| **R12** | 主分支推送保护 | ✅ |
| **R13** | 包文件监控 | ✅ |
| **R14** | 计费API限制 | ✅ |
| **R15** | 生产部署保护 | ✅ |
| **R16** | 数据库写入保护 | ✅ |
| **R17** | 容器管理保护 | ✅ |
| **R18** | 配置文件写入保护 | ✅ |
| **R19** | 可执行文件下载保护 | ✅ |
| **R20** | 网络暴露保护 | ✅ |
| **R21** | 系统关键操作保护 | ✅ |
| **R22** | 证书管理保护 | ✅ |
| **R23** | 备份删除保护 | ✅ |
| **R24** | 日志篡改保护 | ✅ |
| **R25** | 服务重启保护 | ✅ |
| **R26** | 用户权限保护 | ✅ |
| **R27** | 定时任务保护 | ✅ |

## 文档

### 📱 Claude Marketplace 安装指南
- **[安装流程图](docs/INSTALLATION_FLOW_DIAGRAM.md)** - 可视化的安装步骤流程图
- **[操作步骤详解](docs/MARKETPLACE_SEARCH_INSTALL.md)** - 在 Claude Marketplace 中搜索和安装的详细步骤
- **[安装指南](docs/MARKETPLACE_INSTALLATION_GUIDE.md)** - 通过 Claude Marketplace 安装的完整技术文档
- **[快速入门](docs/QUICKSTART_MARKETPLACE.md)** - 5分钟快速安装和上手

### 📚 传统文档
- **[安装指南](docs/installation.md)** - 手动安装步骤和系统要求
- **[配置指南](docs/configuration.md)** - 完整的配置选项和最佳实践
- **[迁移指南](docs/migration.md)** - 从其他工具或旧版本迁移的指南
- **[项目文档](docs/README.md)** - 完整的项目文档和架构说明

## 快速开始

### 安装

```bash
# 克隆仓库
git clone https://github.com/your-org/java-harness.git
cd java-harness

# 构建项目
mvn clean package

# 运行
java -cp java-harness-cli/target/java-harness-cli-4.1.1.jar \
     com.chachamaru.harness.cli.HarnessCli --version
```

### Native Image 编译

```bash
# 安装 GraalVM 23.1.0+
cd java-harness-cli
mvn -Pnative native:compile

# 运行原生可执行文件（<100ms 启动时间）
./target/harness --version
```

详细的安装步骤请参考[安装指南](docs/installation.md)。

## 构建和运行

### 编译项目

```bash
# 编译所有模块
mvn clean compile

# 打包JAR文件
mvn clean package
```

### 运行

```bash
# 运行编译后的JAR
java -cp java-harness-cli/target/java-harness-cli-4.1.1.jar \
     com.chachamaru.harness.cli.HarnessCLI
```

### Hook协议

**输入（stdin）**:
```json
{
  "session_id": "test-session",
  "transcript_path": "/path/to/transcript",
  "cwd": "/project",
  "permission_mode": "default",
  "hook_event_name": "PreToolUse",
  "tool_name": "Write",
  "tool_input": {
    "file_path": "/project/test.txt"
  },
  "plugin_root": "/plugin"
}
```

**输出（stdout）**:
```json
{
  "hookEventName": "PreToolUse",
  "permissionDecision": "allow",
  "permissionDecisionReason": null,
  "additionalContext": null
}
```

## Native Image编译

### 前置要求

- GraalVM 23.1.0或更高版本
- JDK 17
- Visual Studio Build Tools（Windows）或构建工具（Linux/macOS）

### 编译步骤

```bash
# 编译为Native Image
cd cli-native
mvn -Pnative native:compile

# 运行原生可执行文件
./target/harness
```

## 开发指南

### 添加新的Guardrail规则

1. 在`cli-native/src/main/java/com/chachamaru/harness/cli/guardrail/rules/`创建新规则类
2. 实现`Rule`接口
3. 在`HarnessCli.java`的`registerGuardrailRules()`方法中注册

```java
public class R16MyRule implements Rule {
    @Override
    public String getId() {
        return "R16";
    }

    @Override
    public String getName() {
        return "My Custom Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        // 匹配条件
        return true;
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        // 评估逻辑
        return GuardrailResult.allowed();
    }
}
```

### 添加新的Hook处理器

1. 在`cli-native/src/main/java/com/chachamaru/harness/cli/handlers/`创建处理器
2. 实现`HookHandler`接口
3. 在`HarnessCli.java`的`initializeComponents()`方法中注册

## 测试

### 运行单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=HookCodecTest
```

### 集成测试

```bash
# 模拟Hook输入
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"sudo rm -rf /"}}' | \
  java -cp java-harness-cli/target/java-harness-cli-4.1.1.jar \
     com.chachamaru.harness.cli.HarnessCLI
```

## 性能目标

- **Hook响应时间**: < 10ms（95th percentile）
- **内存占用**: < 50MB（Native Image）
- **启动时间**: < 100ms

## 与Go版本的关系

这是claude-code-harness Go版本的Java实现，目标是：

1. **功能对等**: 实现相同的安全规则和Hook协议
2. **性能优化**: 通过GraalVM获得更好的启动性能
3. **部署简化**: 单一可执行文件，无JVM依赖
4. **架构一致**: 保持与Go版本相同的模块化设计

## 版本信息

- **当前版本**: 4.1.1
- **基于Go版本**: claude-code-harness v5.5.0
- **Java版本**: 17
- **GraalVM版本**: 23.1.0
- **CLI框架**: picocli 4.7
- **命令数量**: 86个（与Go版本完全一致）
- **安全规则**: 27个（R01-R27）

## 许可证

与claude-code-harness主项目保持一致。

## 贡献指南

1. 遵循Go版本的代码规范
2. 确保所有27个Guardrail规则的测试覆盖
3. 命令命名使用kebab-case格式（如`sprint-contract`）
4. 性能测试通过（<10ms响应时间）
5. 提交前运行完整的Maven构建流程

## 联系方式

- **主项目**: https://github.com/your-org/claude-code-harness
- **问题反馈**: 通过GitHub Issues
- **文档**: 参考主项目docs/目录
