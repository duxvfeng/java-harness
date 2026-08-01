# Java Harness 安装指南

## 前置要求

在安装 Java Harness 之前，请确保您的系统满足以下要求：

### Java 运行时环境

- **JDK 17** 或更高版本
- 验证安装：`java -version`
- 设置 `JAVA_HOME` 环境变量

### 构建工具

- **Maven 3.8+** 或 **Gradle 8+**
- 验证 Maven：`mvn -version`
- 验证 Gradle：`gradle -version`

### GraalVM (可选，用于Native Image)

- **GraalVM 23.1.0** 或更高版本
- 设置 `GRAALVM_HOME` 环境变量
- 验证：`gu --version`

## 安装方式

### 方式一：从源码构建

#### 1. 克隆仓库

```bash
git clone https://github.com/your-org/java-harness.git
cd java-harness
```

#### 2. 构建项目

```bash
# 编译所有模块
mvn clean compile

# 运行测试
mvn test

# 打包JAR文件
mvn clean package
```

#### 3. 验证安装

```bash
# 运行JAR版本
java -cp java-harness-cli/target/harness-cli-4.1.0.jar \
     com.chachamaru.harness.cli.HarnessCli --version
```

### 方式二：编译为 Native Image

#### 1. 安装 GraalVM

```bash
# macOS (使用 Homebrew)
brew install --cask graalvm-jdk23

# Linux (使用 SDKMAN)
curl -s "https://get.sdkman.io" | bash
sdk install java 23-graal

# Windows
# 从 GraalVM 官网下载并安装
```

#### 2. 设置环境变量

```bash
# 设置 GRAALVM_HOME
export GRAALVM_HOME=/path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH

# 安装 native-image 工具
gu install native-image
```

#### 3. 编译 Native Image

```bash
cd java-harness
cd java-harness-cli
mvn -Pnative native:compile
```

#### 4. 验证 Native Image

```bash
# 运行原生可执行文件
./java-harness-cli/target/harness --version

# 验证启动时间 < 100ms
time ./java-harness-cli/target/harness --version
```

### 方式三：下载预编译版本

#### 1. 下载最新版本

```bash
# 下载 JAR 版本
wget https://github.com/your-org/java-harness/releases/download/v4.1.0/harness-4.1.0.jar

# 或下载 Native Image 版本
wget https://github.com/your-org/java-harness/releases/download/v4.1.0/harness-4.1.0-linux-amd64
```

#### 2. 设置执行权限

```bash
chmod +x harness-4.1.0-linux-amd64
```

#### 3. 验证安装

```bash
./harness-4.1.0-linux-amd64 --version
```

## 配置

### 基本配置

创建配置文件 `harness.yaml`：

```yaml
# Hook 配置
hook:
  enabled: true
  timeout: 10000  # 10秒超时

# Guardrail 配置
guardrail:
  enabled: true
  rules:
    - R01
    - R02
    - R03
    # ... 更多规则

# 日志配置
logging:
  level: INFO
  file: /var/log/harness/harness.log
```

### 环境变量

```bash
# 设置 Hook 超时时间
export HARNESS_HOOK_TIMEOUT=10000

# 启用调试模式
export HARNESS_DEBUG=true

# 设置日志级别
export HARNESS_LOG_LEVEL=DEBUG
```

## 验证安装

### 运行测试套件

```bash
# 运行所有测试
mvn verify

# 运行集成测试
mvn verify -Pintegration

# 查看测试报告
open java-hrench-workflow/target/surefire-reports/index.html
```

### 测试 Hook 功能

```bash
# 测试 Hook 输入
echo '{
  "session_id": "test-session",
  "transcript_path": "/tmp/transcript.jsonl",
  "cwd": "/project",
  "permission_mode": "default",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "echo test"},
  "plugin_root": "/plugin"
}' | java -cp harness-cli-4.1.0.jar \
        com.chachamaru.harness.cli.HarnessCli
```

### 验证 Guardrail 规则

```bash
# 测试危险命令阻止
echo '{
  "session_id": "test",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "sudo rm -rf /"}
}' | java -cp harness-cli-4.1.0.jar \
        com.chachamaru.harness.cli.HarnessCli

# 应该返回 DENY 决策
```

## 升级指南

### 从 4.0.x 升级到 4.1.0

1. **备份数据**

```bash
# 备份配置文件
cp harness.yaml harness.yaml.backup

# 备份状态文件
cp -r .claude/state .claude/state.backup
```

2. **下载新版本**

```bash
wget https://github.com/your-org/java-harness/releases/download/v4.1.0/harness-4.1.0.jar
```

3. **迁移配置**

```bash
# 运行迁移工具
java -cp harness-4.1.0.jar \
     com.chachamaru.harness.tools.MigrateTool \
     --from-version 4.0.0 \
     --to-version 4.1.0
```

4. **验证升级**

```bash
# 运行验证工具
java -cp harness-4.1.0.jar \
     com.chachamaru.harness.tools.ValidateTool \
     --check-integration
```

## 故障排除

### 常见问题

#### 1. Maven 构建失败

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository

# 重新构建
mvn clean install -U
```

#### 2. GraalVM 编译失败

```bash
# 检查 GraalVM 版本
java -version

# 确保安装了 native-image 工具
gu install native-image

# 增加编译内存
export JAVA_OPTS="-Xmx8g"
mvn -Pnative native:compile
```

#### 3. Hook 响应时间过慢

```bash
# 检查 JVM 版本
java -version

# 使用 GraalVM Native Image
cd java-harness-cli
mvn -Pnative native:compile

# 验证性能
time ./target/harness --version
```

## 下一步

- 阅读[配置指南](configuration.md)了解详细配置选项
- 查看[迁移指南](migration.md)从其他工具迁移
- 探索[项目文档](../README.md)了解完整功能

## 支持

- **GitHub Issues**: https://github.com/your-org/java-harness/issues
- **文档**: https://docs.java-harness.dev
- **社区**: https://discord.gg/java-harness