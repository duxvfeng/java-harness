# Marketplace.json 平台自适应说明

## 🎯 功能概述

Java Harness 现在支持**自动根据当前平台调整 marketplace.json 中的可执行文件路径**，确保插件在不同操作系统上都能正确找到对应的 Native Image。

## 📁 支持的平台路径

根据不同的操作系统和架构，可执行文件路径会自动调整为：

| 平台 | 架构 | 可执行文件路径 |
|------|------|----------------|
| **Windows** | AMD64 | `bin/windows/harness.exe` |
| **Linux** | AMD64 | `bin/linux-amd64/harness` |
| **Linux** | ARM64 | `bin/linux-arm64/harness` |
| **macOS** | Intel | `bin/macos-amd64/harness` |
| **macOS** | Apple Silicon | `bin/macos-arm64/harness` |

## 🔧 自动调整机制

### 本地构建时

当运行本地构建脚本时，会自动检测平台并调整 marketplace.json：

#### Windows
```cmd
cd java-harness-cli
mvn -Pnative package -DskipTests

# 自动复制并调整
..\scripts\post-build.bat
```

#### Linux/macOS
```bash
cd java-harness-cli
mvn -Pnative package -DskipTests

# 自动复制并调整
../scripts/post-build.sh
```

### GitHub Actions 构建

在云端构建时，工作流会根据每个平台自动调整对应的 marketplace.json：

```yaml
- name: 📝 调整 marketplace.json
  # 根据当前平台设置可执行文件路径
  if [ "${{ matrix.platform }}" = "windows" ]; then
    EXECUTABLE="bin/windows/harness.exe"
  elif [ "${{ matrix.platform }}" = "linux" ]; then
    EXECUTABLE="bin/linux-${{ matrix.arch }}/harness"
  elif [ "${{ matrix.platform }}" = "darwin" ]; then
    EXECUTABLE="bin/macos-${{ matrix.arch }}/harness"
  fi
```

## 🛠️ 手动调整工具

如果需要手动调整 marketplace.json，可以运行：

### Windows
```cmd
scripts\adjust-marketplace-executable.bat
```

### Linux/macOS
```bash
bash scripts/adjust-marketplace-executable.sh
```

## 📝 marketplace.json 结构

调整后的示例：

```json
{
  "plugins": [
    {
      "id": "java-harness",
      "name": "Java Harness",
      "executable": "bin/windows/harness.exe",  // 根据平台自动调整
      "skills": [...],
      "hooks": ".claude-plugin/hooks.json"
    }
  ]
}
```

## 🧪 测试功能

### Windows
```cmd
scripts\test-marketplace-adjust.bat
```

### Linux/macOS
```bash
bash scripts/test-marketplace-adjust.sh
```

## 🔄 工作流程

### 完整的构建和调整流程

1. **构建 Native Image**
   ```bash
   mvn -Pnative package -DskipTests
   ```

2. **复制到 bin 目录**
   ```bash
   scripts/post-build.sh  # 或 .bat
   ```

3. **自动调整 marketplace.json**
   ```bash
   # post-build 脚本会自动调用调整脚本
   scripts/adjust-marketplace-executable.sh  # 或 .bat
   ```

4. **验证结果**
   ```bash
   # 查看 marketplace.json
   cat .claude-plugin/marketplace.json | grep executable
   ```

## 📦 下载云端构建产物

当从 GitHub Actions 下载构建产物时：

1. **下载对应平台的 Artifacts**
   - `harness-windows-amd64.zip`
   - `harness-linux-amd64.zip`
   - 等等...

2. **解压到项目目录**
   ```bash
   unzip harness-windows-amd64.zip
   ```

3. **验证 marketplace.json**
   - marketplace.json 已经在云端构建时自动调整好了
   - 可执行文件路径已经设置为正确的平台路径

## 🎉 优势

### 自动化
- ✅ 无需手动修改 marketplace.json
- ✅ 构建脚本自动处理平台适配
- ✅ 云端构建自动生成正确的配置

### 跨平台支持
- ✅ 支持 5 种平台架构
- ✅ 统一的目录结构
- ✅ 一致的命名规范

### 开发体验
- ✅ 一次配置，到处运行
- ✅ 本地构建和云端构建行为一致
- ✅ 减少手动错误

## 🔍 故障排除

### 问题 1: marketplace.json 没有更新

**解决方案：**
```bash
# 手动运行调整脚本
scripts/adjust-marketplace-executable.sh

# 检查文件权限
ls -la .claude-plugin/marketplace.json
```

### 问题 2: 可执行文件路径不正确

**解决方案：**
```bash
# 检查当前平台
uname -s
uname -m

# 检查 bin 目录结构
ls -R bin/

# 重新运行调整脚本
bash scripts/adjust-marketplace-executable.sh
```

### 问题 3: Windows PowerShell 权限错误

**解决方案：**
```powershell
# 以管理员身份运行 PowerShell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 或者使用 CMD 而不是 PowerShell
scripts\adjust-marketplace-executable.bat
```

## 📚 相关文档

- [多平台构建指南](../MULTI_PLATFORM_BUILD.md)
- [bin 目录说明](../bin/README.md)
- [GitHub Actions 指南](../GITHUB_ACTIONS_GUIDE.md)
- [快速开始](../QUICK_START.md)

---

**总结：** 现在你可以在任何平台上构建，marketplace.json 都会自动调整为正确的可执行文件路径，无需手动修改配置！🎉
