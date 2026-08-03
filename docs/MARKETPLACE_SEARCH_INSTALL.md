# Claude Marketplace 操作步骤详解

> **如何在 Claude Marketplace 中找到并安装 Java Harness**

---

## 🎯 两种安装方式

### 方式一：通过命令行安装（最简单）

```bash
# 直接安装 Java Harness
claude plugin install duxvfeng/java-harness@v4.0.0
```

✅ **一行命令搞定，推荐使用**

---

### 方式二：通过 Claude Marketplace 界面添加

## 📱 Claude Code Desktop 操作步骤

### 步骤 1: 打开 Claude Marketplace

```
1. 打开 Claude Code Desktop 应用
2. 点击左下角的 ⚙️ 设置图标
3. 在设置菜单中选择 "Plugins" (插件)
4. 点击 "Marketplace" 或 "浏览市场" 按钮
```

### 步骤 2: 搜索 Java Harness

```
1. 在搜索框中输入: "java-harness" 或 "Java Harness"
2. 或者搜索作者: "duxvfeng"
3. 在搜索结果中找到 "Java Harness - Claude Code 安全框架"
```

### 步骤 3: 查看插件详情

```
点击插件卡片，您可以查看：

📋 基本信息:
- 插件名称: java-harness
- 作者: duxvfeng
- 版本: 4.0.0
- 许可证: MIT

⭐ 功能特性:
- 15个安全规则
- 工作流编排
- 代理协调系统
- 技能生态系统

📊 性能指标:
- 启动时间: <100ms
- Hook响应: <10ms
```

### 步骤 4: 安装插件

```
1. 点击 "Install" (安装) 按钮
2. 选择版本（推荐选择稳定版本 v4.0.0）
3. 等待安装完成（通常需要 1-2 分钟）
4. 安装完成后会显示成功提示
```

### 步骤 5: 配置插件

```
1. 在插件列表中找到 "java-harness"
2. 点击 "Settings" 或齿轮图标
3. 根据需要配置选项:
   ✓ 启用安全规则
   ✓ 配置工作流路径
   ✓ 设置代理参数
4. 点击 "Apply" 保存配置
```

### 步骤 6: 重启 Claude Code

```
1. 关闭 Claude Code Desktop
2. 重新打开应用
3. 插件会自动加载
```

---

## 🔧 Claude Code CLI 操作步骤

### 步骤 1: 搜索插件

```bash
# 搜索可用插件
claude plugin search java-harness

# 或搜索特定作者
claude plugin search duxvfeng

# 浏览所有可用插件
claude plugin marketplace list
```

### 步骤 2: 查看插件信息

```bash
# 查看插件详情
claude plugin info duxvfeng/java-harness

# 查看可用版本
claude plugin versions duxvfeng/java-harness

# 阅读插件描述
claude plugin describe duxvfeng/java-harness
```

### 步骤 3: 安装插件

```bash
# 安装最新版本
claude plugin install duxvfeng/java-harness

# 安装特定版本（推荐）
claude plugin install duxvfeng/java-harness@v4.0.0

# 安装并启用
claude plugin install --enable duxvfeng/java-harness@v4.0.0
```

### 步骤 4: 验证安装

```bash
# 查看已安装插件
claude plugin list

# 验证插件
claude plugin verify java-harness

# 测试插件
echo 'test' | claude plugin invoke java-harness
```

---

## 🌐 Claude Code Web 操作步骤

### 步骤 1: 访问 Claude Code Web

```
1. 打开浏览器访问: https://claude.ai/code
2. 登录您的 Anthropic 账户
3. 进入工作区设置
```

### 步骤 2: 打开 Marketplace

```
1. 点击左上角的菜单按钮
2. 选择 "Plugins" 或 "插件"
3. 点击 "Marketplace" 标签
```

### 步骤 3: 搜索和安装

```
1. 在搜索框输入: "duxvfeng/java-harness"
2. 找到插件后点击 "Add to Workspace" (添加到工作区)
3. 等待安装完成
4. 刷新页面使插件生效
```

---

## 🎨 Claude Code IDE 扩展操作步骤

### VS Code 扩展

```bash
# 通过命令面板安装
1. 按 Ctrl+Shift+P (Windows/Linux) 或 Cmd+Shift+P (Mac)
2. 输入 "Claude: Install Plugin"
3. 输入插件名称: "duxvfeng/java-harness"
4. 按 Enter 确认安装

# 或通过扩展市场
1. 按 Ctrl+Shift+X 打开扩展面板
2. 搜索 "Claude Code"
3. 找到 "Java Harness" 插件
4. 点击 "Install" 按钮
```

### JetBrains IDEs

```
1. 打开 IntelliJ IDEA 或其他 JetBrains IDE
2. 进入 File → Settings → Plugins
3. 点击 "Marketplace" 标签
4. 搜索 "Java Harness"
5. 点击 "Install" 按钮
6. 重启 IDE
```

---

## 🔍 搜索技巧

### 精确搜索

```
# 完整插件名称
java-harness

# 作者/插件名格式
duxvfeng/java-harness

# 关键词搜索
claude code security harness
```

### 模糊搜索

```
# 搜索相关功能
security guardrail
workflow orchestration
java implementation
```

### 分类浏览

```
# 安全类别
Security → Guardrail → Enterprise

# 工具类别  
Tools → CLI → Framework

# 开发类别
Development → Java → Claude
```

---

## ✅ 安装验证检查清单

安装完成后，请验证以下项目：

```bash
# 1. 检查插件是否已安装
claude plugin list | grep java-harness
# 预期输出: java-harness 4.0.0 duxvfeng

# 2. 检查插件状态
claude plugin status java-harness
# 预期输出: Plugin is active and running

# 3. 检查插件文件
ls ~/.claude/plugins/java-harness/
# 预期输出: settings.json, harness, 等文件

# 4. 测试基本功能
echo 'test' | claude plugin invoke java-harness
# 预期输出: JSON 响应

# 5. 检查配置文件
cat ~/.claude/plugins/java-harness/settings.json
# 预期输出: 有效的 JSON 配置
```

---

## 🎯 快速安装命令对比

| 安装方式 | 命令/操作 | 时间 | 难度 |
|---------|----------|------|------|
| **CLI 命令** | `claude plugin install duxvfeng/java-harness@v4.0.0` | 30秒 | ⭐ 简单 |
| **Desktop UI** | 点击安装按钮 | 2分钟 | ⭐⭐ 中等 |
| **Web 界面** | 浏览器操作 | 3分钟 | ⭐⭐ 中等 |
| **IDE 扩展** | 扩展市场搜索 | 2分钟 | ⭐⭐ 中等 |

---

## 🚨 常见问题解决

### 问题 1: 搜索不到插件

```bash
# 解决方案: 更新插件索引
claude plugin marketplace refresh

# 或重新加载 Marketplace
claude plugin marketplace reload
```

### 问题 2: 安装失败

```bash
# 检查网络连接
ping api.anthropic.com

# 检查 Claude Code 版本
claude --version
# 需要 2.1.71+

# 更新 Claude Code
claude update
```

### 问题 3: 安装后无法使用

```bash
# 检查插件状态
claude plugin status java-harness

# 查看错误日志
claude plugin logs java-harness

# 重新安装
claude plugin install --force duxvfeng/java-harness@v4.0.0
```

### 问题 4: 版本冲突

```bash
# 卸载旧版本
claude plugin uninstall java-harness

# 安装新版本
claude plugin install duxvfeng/java-harness@v4.0.0

# 清理缓存
claude plugin cleanup
```

---

## 📱 平台特定说明

### Windows 用户

```bash
# 使用 PowerShell 或 CMD
claude plugin install duxvfeng/java-harness@v4.0.0

# 或使用 Git Bash
claude plugin install duxvfeng/java-harness@v4.0.0
```

### macOS 用户

```bash
# 使用 Terminal
claude plugin install duxvfeng/java-harness@v4.0.0

# 或使用 iTerm2
claude plugin install duxvfeng/java-harness@v4.0.0
```

### Linux 用户

```bash
# 使用任何终端
claude plugin install duxvfeng/java-harness@v4.0.0

# 或使用 sudo (如需要)
sudo claude plugin install duxvfeng/java-harness@v4.0.0
```

---

## 🎓 推荐安装流程

### 首次用户（推荐）

```bash
# 1. 更新 Claude Code
claude update

# 2. 安装 Java Harness
claude plugin install duxvfeng/java-harness@v4.0.0

# 3. 验证安装
claude plugin verify java-harness

# 4. 查看帮助
claude plugin help java-harness

# 5. 开始使用
echo 'Hello, Java Harness!' | claude plugin invoke java-harness
```

### 企业用户

```bash
# 1. 锁定版本
claude plugin install duxvfeng/java-harness@v4.0.0

# 2. 禁用自动更新
export DISABLE_AUTOUPDATER=true

# 3. 配置企业策略
claude plugin config set java-harness security.strict_mode true

# 4. 验证配置
claude plugin verify java-harness --detailed
```

### 开发者

```bash
# 1. 安装最新开发版本
claude plugin install duxvfeng/java-harness@master

# 2. 启用调试模式
claude plugin config set java-harness logging.level DEBUG

# 3. 查看日志
claude plugin logs java-harness --follow
```

---

## 📚 相关文档

- **[完整安装指南](MARKETPLACE_INSTALLATION_GUIDE.md)** - 详细的技术文档
- **[快速入门](QUICKSTART_MARKETPLACE.md)** - 5分钟快速上手
- **[用户手册](docs/user/USER_GUIDE.md)** - 完整使用说明
- **[故障排查](docs/troubleshooting/TROUBLESHOOTING.md)** - 问题诊断

---

## 🆘 获取帮助

如果遇到问题：

1. **查看文档**: 阅读相关文档
2. **运行诊断**: `claude plugin doctor java-harness`
3. **搜索问题**: GitHub Issues
4. **提问讨论**: GitHub Discussions
5. **联系支持**: support@java-harness.com

---

<p align="center">
  <b>现在您知道如何在 Claude Marketplace 中找到并安装 Java Harness 了！</b><br>
  <sub>如有问题，请查看我们的详细文档或联系支持团队</sub>
</p>

---

**版本**: 4.0.0 | **更新时间**: 2026-08-03 | **维护团队**: Java Harness Team
