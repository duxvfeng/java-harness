# Native Image构建说明

## 快速构建

使用新的多平台构建脚本：

```bash
./scripts/build/build-all-native.sh
```

此脚本会：
1. 自动检测当前平台（操作系统和架构）
2. 生成对应平台的Native Image二进制文件
3. 将二进制文件复制到bin目录，命名格式为 `harness-{os}-{arch}`

## 手动构建

如需手动构建特定平台：

```bash
# macOS ARM64
mvn -Pnative -Dnative.os=darwin -Dnative.arch=arm64 native:compile

# Linux AMD64
mvn -Pnative -Dnative.os=linux -Dnative.arch=amd64 native:compile

# Windows AMD64
mvn -Pnative -Dnative.os=windows -Dnative.arch=amd64 native:compile
```

## 二进制文件命名

构建后的二进制文件采用与Go版本一致的命名约定：

- `harness-darwin-amd64` - macOS Intel
- `harness-darwin-arm64` - macOS Apple Silicon
- `harness-linux-amd64` - Linux x86_64
- `harness-linux-arm64` - Linux ARM64
- `harness-windows-amd64.exe` - Windows x86_64

## 运行

使用包装脚本自动选择正确的二进制文件：

```bash
./bin/harness --version
./bin/harness --help
```

包装脚本会自动检测当前平台并调用对应的二进制文件。

## 平台支持

- **macOS**: Intel (x86_64) 和 Apple Silicon (ARM64)
- **Linux**: AMD64 (x86_64) 和 ARM64 (aarch64)
- **Windows**: AMD64 (x86_64)

## 技术要求

- Maven 3.6+
- Java 17+
- GraalVM 21+ (用于Native Image构建)
- 对于不同平台，可能需要交叉编译工具链

## 故障排除

### 构建失败
- 确保已安装GraalVM
- 检查Maven版本是否兼容
- 验证环境变量 `GRAALVM_HOME` 是否设置

### 二进制文件无法运行
- 确认文件权限：`chmod +x bin/harness-*`
- 检查平台架构是否匹配
- 对于macOS，可能需要绕过Gatekeeper：`xattr -cr bin/harness-*`