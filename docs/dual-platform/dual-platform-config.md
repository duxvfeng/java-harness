# Java Harness 双平台插件配置

## 🎯 插件兼容性配置

### Claude Code 插件配置

```json
// .claude-plugin/settings.json
{
  "agent": "claude-opus-5",
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "permissions": {
    "allow": [
      "Bash(git status:*)",
      "Bash(mvn -version)",
      "Bash(java -version)",
      "Read(**/*.{java,md,toml,json})"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(dd if=/dev/zero of=*)"
    ]
  },
  "sandbox": {
    "failIfUnavailable": true,
    "network": {
      "deniedDomains": [
        "169.254.169.254",
        "metadata.google.internal"
      ]
    }
  },
  "env": {
    "JAVA_HOME": "/usr/lib/jvm/java-17",
    "MAVEN_OPTS": "-Xmx1G -Xms512M"
  },
  "platform": {
    "type": "claude-code",
    "backend": "claude",
    "features": [
      "native_agents",
      "realtime_review",
      "native_hooks"
    ]
  }
}
```

### Codex 插件配置

```json
// .codex-plugin/config.json
{
  "platform": {
    "type": "codex",
    "backend": "codex",
    "features": [
      "codex_adapter",
      "skill_bridge",
      "toml_configuration"
    ]
  },
  "integration": {
    "auto_detect": true,
    "fallback_backend": "claude",
    "config_file": ".codex/config.toml"
  },
  "skills": {
    "bridge_enabled": true,
    "unified_format": true,
    "compatibility_layer": true
  },
  "environment": {
    "JAVA_HOME": "/usr/lib/jvm/java-17",
    "MAVEN_OPTS": "-Xmx1G -Xms512M"
  }
}
```

## 📋 统一项目配置

### harness.toml (跨平台配置)

```toml
# ============================================
# Java Harness 跨平台兼容配置
# ============================================

[harness]
# 自动检测运行平台
auto_detect_platform = true

# 版本信息
version = "5.0.0-java"
project_root = "."

# 平台检测优先级
# 1. 环境检测 > 2. 配置指定 > 3. 系统默认
platform_detection = "auto"

# ============================================
# [platform.claude] Claude Code 平台配置
# ============================================
[platform.claude]
# Claude Code 特定配置
enable = true
default_backend = "claude"

# 原生功能支持
native_agents = true
realtime_review = true
native_hooks = true

# 性能优化
effort = "high"           # Claude 推荐高强度推理
review_rounds = 5        # 更多审查轮数
timeout = 900            # 更长超时时间

# Hook 系统
hooks_enabled = true
pre_tool_use = true
post_tool_use = true

# ============================================
# [platform.codex] Codex 平台配置
# ============================================
[platform.codex]
# Codex 特定配置
enable = true
default_backend = "codex"

# Codex 集成
codex_adapter = true
skill_bridge = true
config_file = ".codex/config.toml"

# 性能优化
effort = "medium"        # Codex 平衡性能
review_rounds = 3        # 标准审查轮数
timeout = 600            # 标准超时时间

# Hook 系统桥接
hooks_enabled = true
bridge_mode = "compatible"

# ============================================
# [compatibility] 兼容性配置
# ============================================
[compatibility]
# 启用跨平台兼容模式
universal_mode = true

# 技能格式桥接
skill_bridge_enabled = true
unified_skill_format = true

# 配置文件同步
sync_config_files = false

# 向后兼容
preserve_legacy_behavior = true

# ============================================
# [features] 功能模块配置
# ============================================
[features]
# 核心功能模块
plan = true
work = true
review = true
release = true

# 高级功能
workflow = true
coordination = true

# 实验性功能
experimental = false

# ============================================
# [work] 工作执行配置
# ============================================
[work]
enable = true
default_effort = "medium"
auto_review = true

# 后端选择策略
backend_selection = "platform_aware"  # platform_aware, explicit, auto
fallback_backend = "claude"

# 并行执行配置
parallel_workers = 4

# ============================================
# [review] 代码审查配置
# ============================================
[review]
enable = true
strict_mode = false
max_review_rounds = 3

# 审查器选择
reviewer_selection = "platform_optimal"

# Claude 审查器配置
[review.claude]
strict_mode = true
max_rounds = 5

# Codex 审查器配置
[review.codex]
strict_mode = false
max_rounds = 3

# ============================================
# [skills] 技能系统配置
# ============================================
[skills]
enable = true
bridge_enabled = true
unified_format = true

# 技能目录配置
claude_skills = "skills/"
codex_skills = ".codex/skills/"

# 技能桥接配置
[skills.bridge]
enable = true
convert_format = "auto"
validate_after_conversion = true

# ============================================
# [logging] 日志配置
# ============================================
[logging]
level = "INFO"
file = ".claude/logs/harness.log"

# 平台特定日志
[logging.platforms]
claude = ".claude/logs/harness-claude.log"
codex = ".claude/logs/harness-codex.log"

# ============================================
# [state] 状态管理配置
# ============================================
[state]
persist = true
session_file = ".claude/state/session.jsonl"
work_file = ".claude/state/work.jsonl"

# 跨平台状态同步
[state.cross_platform]
sync_enabled = false
backup_interval = 300  # 5分钟
```

## 🔧 安装脚本

### Claude Code 安装脚本

```bash
#!/bin/bash
# install-claude.sh - Claude Code 插件安装脚本

echo "Installing Java Harness for Claude Code..."

# 创建插件目录结构
mkdir -p .claude-plugin/skills
mkdir -p .claude-plugin/assets

# 复制 Claude 特定配置
cp claude-plugin.json .claude-plugin/settings.json

# 验证安装
echo "Java Harness installed for Claude Code!"
echo "Use /harness-plan to get started"
```

### Codex 安装脚本

```bash
#!/bin/bash
# install-codex.sh - Codex 插件安装脚本

echo "Installing Java Harness for Codex..."

# 创建插件目录结构
mkdir -p .codex-plugin/skills
mkdir -p .codex-plugin/assets

# 复制 Codex 特定配置
cp codex-plugin.json .codex-plugin/plugin.json

# 创建 Codex 配置
mkdir -p .codex
cat > .codex/config.toml << 'EOF'
[harness]
backend = "codex"
EOF

# 验证安装
echo "Java Harness installed for Codex!"
echo "Use harness-plan to get started"
```

## 📊 功能兼容性表

| 功能模块 | Claude Code | Codex | 兼容性策略 |
|----------|-----------|-------|-----------|
| **基础命令** | ✅ 原生 | ✅ 桥接 | 完全兼容 |
| **计划管理** | ✅ 原生 | ✅ 桥接 | 完全兼容 |
| **工作执行** | ✅ 原生 | ✅ 桥接 | 完全兼容 |
| **代码审查** | ✅ 原生 | ✅ 桥接 | 完全兼容 |
| **发布管理** | ✅ 原生 | ✅ 桥接 | 完全兼容 |
| **Hook 系统** | ✅ 原生 | 🔄 桥接 | 部分兼容 |
| **Agent 协调** | ✅ 原生 | 🔄 桥接 | 桥接模式 |
| **技能系统** | ✅ 原生 | ✅ 桥接 | 桥接兼容 |
| **工作流** | ✅ 原生 | 🔄 有限 | 基础支持 |

## 🎯 使用示例

### 在 Claude Code 中

```bash
# 自动检测为 Claude 环境
/harness-plan            # 使用 Claude 后端
/harness-work 3          # 使用 Claude 后端

# 明确指定后端
/harness-work 3 --backend claude   # 明确使用 Claude
```

### 在 Codex 中

```bash
# 自动检测为 Codex 环境
harness-plan            # 使用 Codex 后端
harness-work 3          # 使用 Codex 后端

# 明确指定后端
harness-work 3 --backend codex    # 明确使用 Codex
```

### 混合使用

```bash
# 在一个项目中使用不同后端
# 用于不同任务
harness-work 3 --backend claude   # 复杂架构设计
harness-work 4 --backend codex    # 快速代码生成
harness-review --backend claude  # 深度安全审查
```

这个配置确保了 Java Harness 能够在两个平台上无缝运行，提供一致的用户体验。