# GitHub Actions 构建触发指南

## 🚀 触发 Native Image 构建的三种方式

### 方式 1: 手动触发（Workflow Dispatch）⭐ 推荐

#### 步骤 1: 推送代码到 GitHub

```bash
# 确保工作流文件已提交
git add .github/workflows/build-native.yml
git commit -m "feat: 添加 Native Image 多平台构建工作流"
git push origin main
```

#### 步骤 2: 在 GitHub 上手动触发

1. **打开 GitHub 仓库页面**
   ```
   https://github.com/你的用户名/java-harness
   ```

2. **进入 Actions 页面**
   - 点击仓库顶部的 **"Actions"** 标签页

3. **选择工作流**
   - 在左侧边栏找到 **"Build Native Images"**
   - 点击该工作流名称

4. **点击 "Run workflow"**
   - 在右侧点击 **"Run workflow"** 按钮

5. **选择分支和选项**
   - **Branch**: 选择 `main` 或 `master`
   - **Create release**:
     - `false` - 仅构建，不上传到 Release（推荐首次使用）
     - `true` - 构建并创建 GitHub Release

6. **确认运行**
   - 点击绿色按钮 **"Run workflow"**

#### 步骤 3: 监控构建进度

- 构建会显示在 "Actions" 页面的最新运行列表中
- 点击运行记录可以查看详细日志
- 构建时间约 10-15 分钟

#### 步骤 4: 下载构建产物

1. **在运行记录页面**
   - 滚动到页面底部的 **"Artifacts"** 部分

2. **下载对应平台的文件**
   ```
   harness-linux-amd64    ← Linux x86_64
   harness-linux-arm64    ← Linux ARM64
   harness-darwin-amd64    ← macOS Intel
   harness-darwin-arm64    ← macOS Apple Silicon
   harness-windows-amd64    ← Windows x86_64
   ```

3. **解压到项目 bin 目录**
   ```bash
   # 下载后解压，将文件复制到项目的 bin/ 目录
   unzip harness-linux-amd64.zip
   cp harness bin/linux/
   chmod +x bin/linux/harness
   ```

---

### 方式 2: 推送自动触发

修改工作流文件，添加推送触发器：

```yaml
on:
  push:
    branches:
      - main
      - master
    paths:
      - 'java-harness-cli/src/**'
      - 'pom.xml'
      - '.github/workflows/build-native.yml'
  workflow_dispatch:  # 保留手动触发
    inputs:
      create_release:
        description: '创建 GitHub Release'
        required: false
        default: 'false'
        type: boolean
```

**效果：** 每次推送代码到 main 分支时自动触发构建

---

### 方式 3: Git 推送直接触发

#### 快速触发命令

```bash
# 创建一个空提交来触发构建
git commit --allow-empty -m "trigger: 触发 Native Image 构建"
git push origin main

# 或修改工作流文件本身
git touch .github/workflows/build-native.yml
git add .github/workflows/build-native.yml
git commit -m "trigger: 更新构建工作流"
git push origin main
```

---

## 📋 构建配置说明

### 当前工作流支持的平台

| 平台 | 架构 | GitHub Runner | 产物名称 |
|------|------|--------------|----------|
| **Linux** | AMD64 | ubuntu-latest | harness-linux-amd64 |
| **Linux** | ARM64 | ubuntu-latest | harness-linux-arm64 |
| **macOS** | Intel | macos-latest | harness-darwin-amd64 |
| **macOS** | Apple Silicon | macos-latest | harness-darwin-arm64 |
| **Windows** | AMD64 | windows-latest | harness-windows-amd64 |

### 工作流配置选项

```yaml
inputs:
  create_release:
    description: '创建 GitHub Release'
    required: false
    default: 'false'    # 不创建 Release，仅提供下载
    type: boolean
```

- **false**（默认）: 构建完成后，产物在 Artifacts 中保留 7 天
- **true**: 构建完成后，创建 GitHub Release，永久保存

---

## 🔍 故障排除

### 问题 1: 看不到 "Run workflow" 按钮

**原因：** 工作流文件未推送到 GitHub

**解决方案：**
```bash
# 检查文件是否存在
ls -la .github/workflows/build-native.yml

# 确保已提交并推送
git add .github/workflows/build-native.yml
git commit -m "feat: 添加构建工作流"
git push origin main
```

### 问题 2: Actions 页面显示空白

**原因：** GitHub Actions 未启用

**解决方案：**
1. 进入仓库 Settings
2. 点击 **"Actions"** → **"General"**
3. 在 **"Actions permissions"** 中选择：
   - ✅ **"Allow all actions and reusable workflows"**
4. 点击 **"Save"**

### 问题 3: 构建失败

**常见错误：**

#### 错误 1: Java 版本不匹配
```
Error: GraalVM version 21 not found
```

**解决方案：** 工作流应使用 JDK 17
```yaml
env:
  GRAALVM_VERSION: '17'
```

#### 错误 2: 依赖下载失败
```
Error: Could not resolve dependencies
```

**解决方案：** 检查 pom.xml 中的依赖版本和网络连接

#### 错误 3: 构建超时
```
Error: Build timeout (360 minutes)
```

**解决方案：** 增加超时时间
```yaml
jobs:
  build-matrix:
    timeout-minutes: 180
```

---

## 📊 构建时间和成本

### 构建时间

| 平台 | 构建时间 | 总时间（并行） |
|------|---------|--------------|
| Linux AMD64 | ~8分钟 | |
| Linux ARM64 | ~10分钟 | ~15 分钟 |
| macOS Intel | ~10分钟 | （所有平台并行）|
| macOS ARM64 | ~8分钟 | |
| Windows AMD64 | ~12分钟 | |

### GitHub Actions 使用限制

| 账号类型 | 免费分钟/月 | 超出费用 |
|---------|-----------|---------|
| **公共仓库** | 无限 | 免费 |
| **私有仓库（免费版）** | 2000 分钟 | $0.008/分钟 |
| **私有仓库（Pro）** | 3000 分钟 | $0.008/分钟 |

**估算：** 每次完整构建约消耗 50-70 分钟

---

## 🎯 最佳实践

### 1. 首次使用

```bash
# 1. 推送工作流
git add .github/workflows/build-native.yml
git commit -m "feat: 添加构建工作流"
git push origin main

# 2. 手动触发测试
# 在 GitHub Actions 页面点击 "Run workflow"
# Create release 设置为 false

# 3. 验证构建产物
# 下载 Artifacts，测试可执行文件
```

### 2. 日常开发

```bash
# 修改代码后触发构建
git commit -m "feat: 新功能"
git push origin main
# 自动触发构建（如果配置了 on.push）
```

### 3. 发布版本

```bash
# 创建 Git Tag
git tag v5.0.0
git push origin v5.0.0

# 手动触发工作流，设置 Create release = true
# 工作流会自动创建 Release 并上传构建产物
```

---

## 🔗 相关链接

- **GitHub Actions 文档**: https://docs.github.com/en/actions
- **GraalVM Native Image**: https://www.graalvm.org/latest/reference-manual/native-image/
- **工作流语法**: https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions

---

## ✅ 检查清单

使用此清单确保成功触发构建：

- [ ] `.github/workflows/build-native.yml` 文件存在
- [ ] 工作流文件已推送到 GitHub
- [ ] GitHub Actions 已启用
- [ ] 有推送权限（可以写仓库）
- [ ] 选择了正确的分支
- [ ] 网络连接正常

---

**快速链接：**

- 🚀 **立即触发**: https://github.com/你的用户名/java-harness/actions
- 👁️ **查看工作流**: https://github.com/你的用户名/java-harness/blob/main/.github/workflows/build-native.yml
- 📥 **下载产物**: https://github.com/你的用户名/java-harness/actions （点击运行记录 → Artifacts）
