# Java Harness 双平台插件兼容架构

## 🎯 目标

让 Java Harness 同时作为 Claude Code 和 OpenAI Codex 的插件，提供统一的用户体验。

## 🏗️ 架构设计

### 统一插件结构

```
java-harness/
├── plugin.json (Claude 格式)
├── .codex-plugin/
│   └── plugin.json (Codex 格式)
├── .claude-plugin/
│   ├── settings.json (Claude 配置)
│   └── marketplace.json
├── skills/ (兼容两个平台的技能定义)
├── workflows/ (工作流定义)
├── assets/ (资源文件)
└── README.md (双平台安装指南)
```

### 配置兼容层

```
┌─────────────────────────────────────────────────────┐
│         统一 Java Harness 插件                      │
│                                                     │
│  ┌─────────────┐       ┌─────────────────┐         │
│  │  Claude Code  │       │   Codex CLI     │         │
│  │   适配器       │       │   适配器         │         │
│  └──────┬────────┘       └────────┬─────────┘         │
│         │                        │                   │
│         └────────────┬───────────┘                   │
│                      │                               │
│              ┌───────▼──────────────────────┐       │
│              │   核心功能层 (统一实现)       │       │
│              │                               │       │
│              │ - Plan → Work → Review        │       │
│              │ - Backend 抽象               │       │
│              │ - Skill 系统                 │       │
│              │ - Hook 系统                  │       │
│              └───────────────────────────────┘       │
└─────────────────────────────────────────────────────┘
```

## 📋 双平台配置文件

### 1. Claude Code 插件配置

```bash
# Claude 插件安装位置
/Users/apple/.claude/plugins/cache/java-harness-market/java-harness/5.0.0/

# 必需文件
├── plugin.json              (Claude 格式元数据)
├── .claude-plugin/
│   ├── settings.json        (Claude 特定设置)
│   └── marketplace.json
├── skills/                  (技能定义)
└── CLAUDE.md                (Claude 使用指南)
```

### 2. Codex 插件配置

```bash
# Codex 插件安装位置
/Users/apple/.codex/plugins/cache/java-harness/5.0.0/

# 必需文件
├── .codex-plugin/
│   └── plugin.json         (Codex 格式元数据)
├── skills/                  (技能定义)
├── assets/                  (图标和资源)
└── README.md                (Codex 使用指南)
```

### 3. 统一配置适配

```toml
# harness.toml.bak (项目级配置)
[harness]
# 自动检测运行环境
auto_detect_backend = true

# 为不同平台设置默认值
[platform.claude]
default_backend = "claude"
native_skills = true

[platform.codex]
default_backend = "codex"
config_file = ".codex/config.toml"

# 统一的技能配置
[skills]
bridge_enabled = true      # 启用技能桥接
unified_format = true        # 使用统一技能格式
```

## 🔧 实现策略

### 1. 平台检测机制

```java
// 平台检测器
public class PlatformDetector {
    public enum Platform {
        CLAUDE_CODE,
        CODEX,
        UNKNOWN
    }
    
    public static Platform detectCurrentPlatform() {
        // 检查运行环境特征
        if (isClaudeEnvironment()) {
            return Platform.CLAUDE_CODE;
        } else if (isCodexEnvironment()) {
            return Platform.CODEX;
        }
        return Platform.UNKNOWN;
    }
    
    private static boolean isClaudeEnvironment() {
        return System.getenv("CLAUDE_API_KEY") != null ||
               Files.exists(Path.of("/.claude"));
    }
    
    private static boolean isCodexEnvironment() {
        return Files.exists(Path.of(System.getProperty("user.home"), ".codex"));
    }
}
```

### 2. 统一技能接口

```java
// 平台无关的技能接口
public interface UniversalSkill {
    String getId();
    String getName();
    String getDescription();
    
    // 统一的执行接口
    SkillResult execute(SkillContext context);
    
    // 平台特定适配
    default Object adaptForPlatform(Platform platform) {
        // 根据平台调整技能行为
        return switch (platform) {
            case CLAUDE_CODE -> adaptForClaude();
            case CODEX -> adaptForCodex();
            default -> this;
        };
    }
}
```

### 3. 后端选择策略

```java
// 智能后端选择器
public class BackendSelector {
    public static String selectBackend(Config config) {
        // 优先级：明确指定 > 自动检测 > 平台默认 > 系统默认
        
        // 1. 检查明确指定
        if (config.hasExplicitBackend()) {
            return config.getBackend();
        }
        
        // 2. 检查自动检测
        if (config.isAutoDetect()) {
            Platform platform = PlatformDetector.detectCurrentPlatform();
            return switch (platform) {
                case CLAUDE_CODE -> "claude";
                case CODEX -> "codex";
                default -> "claude"; // 系统默认
            };
        }
        
        // 3. 使用配置的默认值
        return config.getDefaultBackend();
    }
}
```

## 📦 安装流程

### Claude Code 安装

```bash
# 在 Claude Code 中安装
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git

# 安装完成后自动识别为 Claude 环境
# 默认配置：backend = "claude"
```

### Codex 安装

```bash
# 在 Codex CLI 中安装
codex plugin install https://gitee.com/duxvfeng/java-harness.git

# 或手动复制到 Codex 插件目录
cd /Users/apple/.codex/plugins/cache
git clone https://gitee.com/duxvfeng/java-harness.git java-harness

# 安装完成后自动识别为 Codex 环境
# 默认配置：backend = "codex"
```

## 🎯 统一使用体验

### 相同的命令接口

```bash
# 在 Claude Code 中
/harness-plan           # 使用 Claude 后端
/harness-work 3         # 使用 Claude 后端
/harness-review         # 使用 Claude 后端

# 在 Codex 中  
harness-plan            # 使用 Codex 后端
harness-work 3          # 使用 Codex 后端
harness-review          # 使用 Codex 后端
```

### 跨平台配置

```toml
# harness.toml.bak (跨平台配置)
[harness]
# 跨平台兼容模式
universal_mode = true

# 为不同平台优化配置
[optimization.claude]
effort = "high"          # Claude 推荐高强度推理
review_rounds = 5

[optimization.codex]
effort = "medium"        # Codex 平衡性能
review_rounds = 3
```

## 🔍 平台特定功能

### Claude Code 特有功能

```java
// Claude Code 特有的原生功能
if (platform == Platform.CLAUDE_CODE) {
    // 启用原生 Agent 协调
    enableNativeAgentCoordination();
    
    // 使用 Claude 特有的 Hook 系统
    enableClaudeNativeHooks();
    
    // 支持实时审查
    enableRealTimeReview();
}
```

### Codex 特有功能

```java
// Codex 特有的集成功能
if (platform == Platform.CODEX) {
    // 启用 Codex 适配器
    enableCodexAdapter();
    
    // 使用 Codex 配置文件
    loadCodexConfig();
    
    // 支持 Codex 技能桥接
    enableCodexSkillBridge();
}
```

## 📊 兼容性矩阵

| 功能 | Claude Code | Codex | 兼容性 |
|------|-----------|-------|--------|
| 基础命令 | ✅ | ✅ | ✅ 完全兼容 |
| 计划管理 | ✅ | ✅ | ✅ 完全兼容 |
| 工作执行 | ✅ | ✅ | ✅ 完全兼容 |
| 代码审查 | ✅ | ✅ | ✅ 完全兼容 |
| 发布管理 | ✅ | ✅ | ✅ 完全兼容 |
| Hook 系统 | ✅ | ⚠️ | 🔄 部分兼容 |
| Agent 协调 | ✅ | ⚠️ | 🔄 桥接模式 |
| 技能系统 | ✅ | ✅ | ✅ 桥接兼容 |
| 工作流 | ✅ | ⚠️ | 🔄 有限支持 |

## 🎁 资源共享策略

### 统一资源

```
assets/
├── harness-logo.svg          (Claude 用)
├── harness-logo-dark.svg     (Claude 暗色模式)
├── harness-icon.svg          (Codex 用)
└── README.md                 (通用文档)
```

### 平台特定资源

```
.claude-plugin/assets/       (Claude 专用)
├── claude-specific.svg

.codex-plugin/assets/       (Codex 专用)
├── codex-specific.svg
```

## 🚀 发布流程

### 1. 统一构建

```bash
# 构建支持双平台的插件包
mvn clean package -Puniversal

# 生成两个平台的插件包
target/
├── java-harness-claude-plugin.zip    (Claude Code 市场)
└── java-harness-codex-plugin.zip     (Codex 插件目录)
```

### 2. 双平台发布

```bash
# 发布到 Claude Code 市场
# 1. 更新 marketplace.json
# 2. 发布到 Gitee marketplace
# 3. 用户通过 /plugins marketplace add 安装

# 发布到 Codex 插件系统
# 1. 更新 plugin.json
# 2. 复制到 Codex 插件目录
# 3. 用户通过 codex plugin install 安装
```

这个架构设计确保了 Java Harness 能够在两个平台上提供一致的用户体验，同时充分利用各自平台的优势。