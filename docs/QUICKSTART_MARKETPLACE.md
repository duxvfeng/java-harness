# Claude Marketplace 快速入门 - Java Harness

> **5分钟快速安装指南**

---

## 🚀 一键安装

```bash
# 安装 Java Harness
claude plugin install duxvfeng/java-harness@v4.0.0

# 验证安装
claude plugin verify java-harness

# 开始使用
echo 'test' | claude plugin invoke java-harness
```

---

## ✅ 安装完成验证

```bash
# 查看插件列表
claude plugin list | grep java-harness

# 查看插件信息
claude plugin info java-harness

# 运行测试
claude plugin test java-harness
```

---

## 📖 基础使用

### 1. 安全防护

```bash
# Java Harness 自动保护您的项目
# 尝试危险命令将被阻止

claude ask "删除所有文件"
# → Java Harness 将阻止此操作
```

### 2. 工作流执行

```bash
# 创建 Plans.md
cat > Plans.md << 'EOF'
# 我的项目计划

## 阶段1: 设计
- [ ] 架构设计
- [ ] 接口设计

## 阶段2: 实现  
- [ ] 核心功能
- [ ] 测试用例
EOF

# 执行工作流
claude workflow execute Plans.md
```

### 3. 技能调用

```bash
# 列出可用技能
claude skill list

# 使用 Plan 技能
/harness-plan create

# 使用 Work 技能
/harness-work start

# 使用 Review 技能
/harness-review review
```

---

## 🎯 下一步

- 📚 阅读完整指南: [MARKETPLACE_INSTALLATION_GUIDE.md](MARKETPLACE_INSTALLATION_GUIDE.md)
- 🎓 查看示例: https://github.com/duxvfeng/java-harness/tree/main/examples
- 💬 获取帮助: https://github.com/duxvfeng/java-harness/discussions

---

<p align="center">
  <b>安装完成！开始安全高效的 Claude Code 之旅吧！</b>
</p>
