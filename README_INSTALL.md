# Java Harness - Claude Code Plugin

<div align="center">

**Java 版本的 Claude Code Harness**

完整的 Hook 处理、Guardrail 安全规则和多代理协作支持

[![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE.md)
[![Gitee](https://img.shields.io/badge/Gitee-java--harness-red.svg)](https://gitee.com/duxvfeng/java-harness)

</div>

## ✨ 特性

- 🛡️ **27 个 Guardrail 安全规则** - 保护你的代码和系统
- 🔄 **完整的 Hook 处理** - 与 Claude Code 无缝集成
- 🤖 **多代理协作** - Worker、Reviewer、Advisor 代理
- 📋 **工作流管理** - Plans.md 解析和执行
- 🔌 **技能系统** - 可扩展的技能框架
- 🚀 **CI/CD 集成** - GitHub Actions 和 GitLab CI 支持

## 🚀 一键安装

### Linux/macOS/Git Bash

```bash
curl -fsSL https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.sh | bash
```

### Windows

```cmd
powershell -Command "Invoke-WebRequest -Uri 'https://gitee.com/duxvfeng/java-harness/raw/main/scripts/install.bat' -OutFile 'install.bat' && install.bat"
```

详细安装说明见 [INSTALL.md](INSTALL.md)

## 📖 快速开始

### 安装后验证

```bash
# 重载插件
/reload-plugins

# 查看插件
/plugin list

# 测试启动
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --version
```

### 基本使用

```bash
# CLI 模式
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness --help
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness plan list
~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness work solo

# Hook 模式（自动拦截工具调用）
echo '{
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {"command": "sudo rm -rf /"}
}' | ~/.claude/plugins/marketplaces/java-harness-marketplace/bin/harness
```

## 🛡️ 安全保护

插件内置 27 个安全规则，自动阻止危险操作：

| 规则 | 功能 | 严重程度 |
|------|------|----------|
| R01 | 阻止提权命令 (sudo, su) | 🔴 High |
| R02 | 保护敏感路径 (.env, .git) | 🔴 High |
| R03 | 阻止重定向绕过 | 🟡 Medium |
| R05 | 防止递归删除 (rm -rf) | 🔴 High |
| R06 | 阻止强制推送 | 🟡 Medium |
| R09 | 阻止访问密钥文件 | 🔴 High |
| ... | 更多规则 | ... |

## 📊 性能

| 模式 | 启动时间 | 内存占用 | Hook 处理 |
|------|----------|----------|-----------|
| JAR（当前） | ~2-3s | ~150MB | <15ms |
| Native Image | ~80ms | ~45MB | <10ms |

## 📁 项目结构

```
java-harness-marketplace/
├── .claude-plugin/       # 标准插件配置
├── bin/                  # 可执行文件
├── agents/              # 代理配置
├── skills/              # 技能文件
└── workflows/           # 工作流定义
```

## 🔧 系统要求

- **Java**: JDK 17 或更高版本
- **Git**: 用于克隆仓库
- **磁盘空间**: 约 10MB

## 📚 文档

- [安装指南](INSTALL.md)
- [打包指南](docs/Claude插件打包指南.md)
- [API 文档](docs/)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License - 详见 [LICENSE.md](LICENSE.md)

## 🌟 Star History

如果这个项目对你有帮助，请给我们一个 ⭐ Star！

---

<div align="center">

**[Gitee](https://gitee.com/duxvfeng/java-harness) · [文档](INSTALL.md) · [问题反馈](https://gitee.com/duxvfeng/java-harness/issues)**

Made with ❤️ by duxvfeng

</div>
