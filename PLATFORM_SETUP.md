# java-harness Platform Setup

## 验证安装

### Windows (CMD/PowerShell)
```bash
bin\harness.bat --version
```

### Linux/macOS/Windows (Git Bash)
```bash
bin/harness --version
```

**预期输出**：`harness 4.1.1`

## 自动平台检测

`bin/harness`（Unix）和 `bin/harness.bat`（Windows）会自动检测您的操作系统和架构，调用正确的二进制文件。

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
