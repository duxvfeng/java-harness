# Claude Code Harness - Java Implementation

Java版本的Claude Code Harness，实现CLI Gateway核心功能，包括Hook协议处理、Guardrail安全引擎和快速响应机制。

## 项目概述

这是claude-code-harness的Java原生实现，目标是通过GraalVM编译为Native Image，实现**<10ms的hook响应时间**，为Claude Code提供实时的安全策略执行。

### 核心特性

- **🚀 高性能**: GraalVM Native Image编译，亚毫秒级响应
- **🔒 安全防护**: 15个Guardrail规则（R01-R15）全覆盖
- **📡 Hook协议**: 完整的Claude Code Hook事件处理
- **🎯 模块化设计**: 共享模块 + CLI Native模块

## 架构设计

### 多模块结构

```
claude-harness-parent/
├── shared/                    # 共享模块
│   ├── dto/                  # 数据传输对象
│   └── constants/           # 常量定义
└── cli-native/               # 原生CLI模块
    ├── hook/                 # Hook协议层
    ├── guardrail/            # 安全引擎
    ├── handlers/             # Hook处理器
    ├── router/               # 事件路由
    └── HarnessCli.java      # 主入口
```

### 技术栈

- **JDK 17** - 基础运行时
- **GraalVM 23.1.0** - Native Image编译
- **Jackson 2.15.2** - JSON处理
- **SLF4J 2.0.9** - 日志接口
- **Logback 1.4.11** - 日志实现
- **JUnit 5.10.0** - 单元测试

## 安全规则（R01-R15）

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
java -cp cli-native/target/harness-cli-native-4.0.0-java-SNAPSHOT.jar \
     com.chachamaru.harness.cli.HarnessCli
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
  java -cp cli-native/target/harness-cli-native-4.0.0-java-SNAPSHOT.jar \
     com.chachamaru.harness.cli.HarnessCli
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

- **当前版本**: 4.0.0-java-SNAPSHOT
- **基于Go版本**: claude-code-harness v5.5.0
- **Java版本**: 17
- **GraalVM版本**: 23.1.0

## 许可证

与claude-code-harness主项目保持一致。

## 贡献指南

1. 遵循Go版本的代码规范
2. 确保所有15个Guardrail规则的测试覆盖
3. 性能测试通过（<10ms响应时间）
4. 提交前运行完整的Maven构建流程

## 联系方式

- **主项目**: https://github.com/your-org/claude-code-harness
- **问题反馈**: 通过GitHub Issues
- **文档**: 参考主项目docs/目录
