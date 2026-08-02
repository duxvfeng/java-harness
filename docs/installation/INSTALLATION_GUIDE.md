# Java Harness 安装指南

> **版本**: 4.0.0
> **系统要求**: JDK 17+, Maven 3.8+
> **支持平台**: Linux, macOS, Windows (WSL)

---

## 📋 目录

- [系统要求](#系统要求)
- [安装方式](#安装方式)
- [配置设置](#配置设置)
- [验证安装](#验证安装)
- [卸载说明](#卸载说明)
- [故障排查](#故障排查)

---

## 🔧 系统要求

### 最低要求

| 组件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| **Java** | JDK 17 | JDK 21 |
| **Maven** | 3.8.0 | 3.9.0+ |
| **内存** | 4GB | 8GB+ |
| **磁盘空间** | 2GB | 5GB+ |
| **操作系统** | Linux/macOS/WSL | 最新稳定版 |

### 检查系统环境

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查可用内存
free -h  # Linux
sysctl hw.memsize  # macOS
```

---

## 📦 安装方式

### 方式1: 从源码安装（推荐）

#### 1. 克隆项目

```bash
# 克隆主仓库
git clone https://github.com/your-org/java-harness.git
cd java-harness

# 或者克隆特定分支
git clone -b release/4.0.0 https://github.com/your-org/java-harness.git
cd java-harness
```

#### 2. 构建项目

```bash
# 完整构建
./harness build

# 或者使用 Maven 直接构建
mvn clean package -DskipTests

# 安装到本地 Maven 仓库
mvn clean install -DskipTests
```

#### 3. 验证安装

```bash
# 运行测试
./harness test

# 检查版本
./harness version

# 查看帮助
./harness help
```

### 方式2: 使用安装脚本

```bash
# 运行安装脚本
bash scripts/util/install.sh

# 安装脚本会自动：
# 1. 检查系统环境
# 2. 下载依赖
# 3. 构建项目
# 4. 设置环境变量
# 5. 创建命令别名
```

### 方式3: Docker 容器安装

```bash
# 构建镜像
docker build -t java-harness:4.0.0 .

# 运行容器
docker run -it --rm \
  -v $(pwd)/work:/work \
  java-harness:4.0.0 \
  ./harness build

# 使用 Docker Compose
docker-compose up -d
```

---

## ⚙️ 配置设置

### 1. 环境变量配置

#### 设置 JAVA_HOME

```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 添加到 ~/.bashrc 或 ~/.zshrc
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
```

#### 设置 Maven 配置

```bash
# 创建 ~/.m2/settings.xml
cat > ~/.m2/settings.xml << 'EOF'
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>*</mirrorOf>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
EOF
```

### 2. 项目配置文件

#### 创建配置文件

```bash
# 初始化项目配置
./harness init-project

# 生成配置文件模板
./harness config-template
```

#### 配置文件示例

`.claude-code-harness.config.yaml`:

```yaml
# 项目信息
project:
  name: "My Project"
  description: "项目描述"
  version: "1.0.0"

# 语言设置
i18n:
  language: "zh"  # en, ja, zh
  timezone: "Asia/Shanghai"

# 工作流设置
workflow:
  mode: "auto"  # auto, solo, parallel, breezing
  timeout: 300  # 秒
  retry: 3

# 模板设置
templates:
  registry: ".claude/template-registry.json"
  locale: "templates/locales"

# 脚本设置
scripts:
  base_dir: "scripts"
  timeout: 300
```

### 3. 创建命令别名

#### 添加 shell 别名

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
alias harness='cd /path/to/java-harness && ./harness'
alias java-harness='cd /path/to/java-harness && ./harness'
```

#### 创建符号链接（可选）

```bash
# 创建全局命令
sudo ln -sf /path/to/java-harness/harness /usr/local/bin/java-harness

# 使用
java-harness build
```

---

## ✅ 验证安装

### 1. 基本功能验证

```bash
# 测试构建功能
./harness build

# 测试测试功能
./harness test

# 测试会话功能
./harness session-init
```

### 2. 完整系统验证

```bash
# 运行完整验证
./harness verify

# 运行工作流系统验证
./harness verify-workflow-system

# 运行工作流验证
./harness verify-workflows
```

### 3. 性能基准测试

```bash
# 运行性能测试
./harness performance-test

# 查看性能报告
./harness performance-report --format html
```

### 4. 集成测试

```bash
# 运行集成测试
./harness test-integration

# 生成测试报告
./harness test-report --format html
```

---

## 🎯 快速开始示例

### 示例1: 首次使用

```bash
# 1. 进入项目目录
cd java-harness

# 2. 运行安装脚本
bash scripts/util/install.sh

# 3. 初始化项目
./harness init-project

# 4. 创建测试计划
/harness-plan create

# 5. 执行第一个任务
./harness work 1
```

### 示例2: 日常使用

```bash
# 构建项目
./harness build

# 运行测试
./harness test

# 监控会话
./harness session-monitor
```

### 示例3: CI/CD 集成

```bash
# CI 构建
./harness ci-build

# CI 测试
./harness ci-test

# 部署检查
./harness release-preflight
```

---

## 🗑️ 卸载说明

### 完全卸载

```bash
# 1. 删除项目目录
rm -rf /path/to/java-harness

# 2. 删除 Maven 本地仓库
rm -rf ~/.m2/repository/com/chachamaru/

# 3. 删除配置文件
rm -rf ~/.claude-code-harness
rm -f ~/.claude-code-harness.config.yaml

# 4. 删除命令别名
# 编辑 ~/.bashrc 或 ~/.zshrc，移除添加的别名

# 5. 删除符号链接（如果创建了）
sudo rm -f /usr/local/bin/java-harness
```

### 保留配置卸载

```bash
# 只删除项目，保留配置
rm -rf /path/to/java-harness

# 配置文件保存在：
# ~/.claude-code-harness/
# ~/.claude-code-harness.config.yaml
```

---

## 🔍 故障排查

### 常见安装问题

#### 问题1: Java 版本不匹配

```bash
# 错误信息
java.lang.UnsupportedClassVersionError

# 解决方案
# 安装 JDK 17+
sudo apt install openjdk-17-jdk  # Ubuntu/Debian
brew install openjdk@17          # macOS

# 设置 JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

#### 问题2: Maven 依赖下载失败

```bash
# 错误信息
Could not resolve dependencies

# 解决方案
# 配置国内镜像源
cat > ~/.m2/settings.xml << 'EOF'
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>*</mirrorOf>
        </mirror>
    </mirrors>
</settings>
EOF
```

#### 问题3: 权限问题

```bash
# 错误信息
Permission denied

# 解决方案
# 修改文件权限
chmod +x scripts/**/*.sh

# 或使用 sudo（不推荐）
sudo ./harness build
```

#### 问题4: 内存不足

```bash
# 错误信息
java.lang.OutOfMemoryError: Java heap space

# 解决方案
# 增加 Maven 内存
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"

# 或者修改 Maven 配置
export MAVEN_OPTS="-Xms512m -Xmx4096m"
```

### 获取详细日志

```bash
# 启用调试模式
./harness build --debug

# 查看 Maven 详细日志
mvn clean install -X

# 检查系统状态
./harness session-status --diagnostic
```

---

## 📚 相关文档

- [用户指南](docs/user/USER_GUIDE.md)
- [迁移指南](docs/user/MIGRATION_GUIDE.md)
- [配置参考](docs/configuration.md)
- [故障排查](docs/troubleshooting/TROUBLESHOOTING.md)

---

## 🆘 获取帮助

### 社区支持

- **GitHub Issues**: [提交问题](https://github.com/your-org/java-harness/issues)
- **讨论区**: [参与讨论](https://github.com/your-org/java-harness/discussions)
- **邮件列表**: java-harness@googlegroups.com

### 企业支持

如果您需要企业级支持，请联系：

- **邮箱**: support@java-harness.com
- **网站**: https://java-harness.com/support

---

## 🎓 下一步

安装完成后，建议您：

1. 阅读 [用户指南](docs/user/USER_GUIDE.md)
2. 查看 [示例项目](https://github.com/your-org/java-harness-examples)
3. 加入 [社区讨论](https://github.com/your-org/java-harness/discussions)
4. 探索 [高级功能](docs/developer/)

---

**版本**: 4.0.0 | **更新时间**: 2026-08-02 | **维护团队**: Java Harness Team