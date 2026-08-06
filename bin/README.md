# bin 目录 - 多平台 Native Image 可执行文件

此目录包含各平台的 Native Image 可执行文件，构建后自动生成。

## 目录结构

```
bin/
├── windows/
│   └── harness.exe          # Windows x86_64 可执行文件
├── linux/
│   └── harness              # Linux x86_64 可执行文件
├── linux-arm64/
│   └── harness              # Linux ARM64 可执行文件
├── macos/
│   └── harness              # macOS Intel 可执行文件
└── macos-arm64/
    └── harness              # macOS Apple Silicon 可执行文件
```

## 使用方法

### Windows

```cmd
# 运行可执行文件
bin\windows\harness.exe --version
bin\windows\harness.exe --help

# 添加到 PATH (可选)
set PATH=%PATH%;%CD%\bin\windows
```

### Linux

```bash
# 运行可执行文件
bin/linux/harness --version
bin/linux/harness --help

# 添加到 PATH (可选)
export PATH=$PATH:$(pwd)/bin/linux
```

### macOS

```bash
# 运行可执行文件
bin/macos/harness --version
bin/macos/harness --help

# 添加到 PATH (可选)
export PATH=$PATH:$(pwd)/bin/macos
```

## 构建方法

### 本地构建

```bash
# 使用 post-build 脚本自动复制到 bin 目录
cd java-harness-cli
mvn -Pnative package -DskipTests

# 运行构建后脚本
bash ../scripts/post-build.sh    # Linux/macOS
# 或
scripts\post-build.bat            # Windows
```

### GitHub Actions 构建

```bash
# 1. 推送代码到 GitHub
git add .
git commit -m "build: 更新 Native Image"
git push origin main

# 2. 在 GitHub Actions 页面触发构建
# 3. 下载构建产物并解压到此目录
```

### Docker 构建

```bash
# Linux AMD64
docker buildx build \
  --platform linux/amd64 \
  --file docker/Dockerfile.native \
  --output type=local,dest=bin/linux-amd64 \
  .

# Linux ARM64
docker buildx build \
  --platform linux/arm64 \
  --file docker/Dockerfile.native \
  --output type=local,dest=bin/linux-arm64 \
  .
```

## 性能对比

| 平台 | 启动时间 | 内存占用 | 文件大小 |
|------|---------|---------|----------|
| Windows | ~70ms | ~55MB | ~85MB |
| Linux | ~60ms | ~50MB | ~80MB |
| macOS Intel | ~50ms | ~45MB | ~75MB |
| macOS ARM64 | ~50ms | ~45MB | ~75MB |

## 故障排除

### Windows

```cmd
; 如果遇到 "无法找到入口" 错误
; 检查是否安装了 Visual C++ Redistributable
; 下载: https://aka.ms/vs/17/release/vc_redist.x64.exe

; 检查文件完整性
bin\windows\harness.exe --version
```

### Linux

```bash
# 缺少运行时库
sudo apt-get install -y libstdc++6

# 权限问题
chmod +x bin/linux/harness

# 检查文件类型
file bin/linux/harness
```

### macOS

```bash
# Gatekeeper 限制
xattr -d com.apple.quarantine bin/macos/harness

# 权限问题
chmod +x bin/macos/harness

# 检查架构
file bin/macos/harness
uname -m  # 应该匹配架构
```

## 集成到 Claude Code

```bash
# 创建插件目录
mkdir -p ~/.claude/plugins/java-harness

# 复制对应平台的可执行文件
# Linux
cp bin/linux/harness ~/.claude/plugins/java-harness/java-harness
chmod +x ~/.claude/plugins/java-harness/java-harness

# macOS
cp bin/macos/harness ~/.claude/plugins/java-harness/java-harness
chmod +x ~/.claude/plugins/java-harness/java-harness

# Windows
copy bin\windows\harness.exe %USERPROFILE%\.claude\plugins\java-harness\java-harness.exe
```

## 自动化构建脚本

使用项目根目录下的脚本一键构建所有平台：

```bash
# Windows
scripts\multi-arch-build.bat

# Linux/macOS
bash scripts/multi-arch-build.sh
```
