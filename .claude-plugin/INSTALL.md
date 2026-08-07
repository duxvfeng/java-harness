# java-harness Platform Setup

java-harness 使用跨平台 wrapper 脚本自动检测平台并调用正确的二进制文件。

## 快速验证

安装完成后，运行以下命令验证安装：

```bash
# Windows
bin\harness.bat --version

# Linux/macOS/Windows (Git Bash)
bin/harness --version
```

**预期输出**：`harness 4.1.1`

## 自动平台检测

`bin/harness` 和 `bin/harness.bat` 会自动检测您的操作系统和架构：

### 支持的平台

| 操作系统 | 架构 | 二进制路径 |
|---------|------|-----------|
| Windows | x86_64 | `bin/windows/harness.exe` |
| Linux | x86_64 (AMD64) | `bin/linux/linux-amd64/harness` |
| Linux | ARM64 | `bin/linux/linux-arm64/harness` |
| macOS | Intel (x86_64) | `bin/macos/macos-amd64/harness` |
| macOS | Apple Silicon (ARM64) | `bin/macos/macos-arm64/harness` |

### Fallback 机制

如果平台特定的二进制文件不存在，wrapper 会自动回退到 JAR 文件：
```
java-harness-cli/target/java-harness-cli-*-shaded.jar
```

**注意**：JAR fallback 性能较慢，建议构建 Native Image 以获得最佳性能。

## 重新加载插件

安装完成后，重新加载插件：
```bash
/plugin reload
```

## 验证 Hooks 配置

```bash
# 检查 hooks 配置（应该使用统一的 wrapper）
cat hooks/hooks.json | grep "bin/harness"
```

**预期输出**：所有 command 字段应该显示 `bin/harness hook ...`
