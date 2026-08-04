# SyncSkill 和 ReleaseSkill 设计文档

**创建日期**: 2026-08-04
**状态**: 设计阶段
**目标**: 实现 Java Harness 的同步和发布功能，与 Go 版本对等

---

## 概述

本设计实现两个核心 Skill：

1. **SyncSkill** - 从 `harness.toml`（SSOT）生成 Claude Code 插件配置文件
2. **ReleaseSkill** - 检查是否需要发布，基于发布列车算法

参考实现：`D:\go-project\claude-code-harness\go\cmd\harness\sync.go` 和 `release_check.go`

---

## 架构设计

### 整体结构

```
java-harness-workflow/
└── src/main/java/.../workflow/
    ├── skill/core/
    │   ├── SyncSkill.java          # 同步技能
    │   ├── ReleaseSkill.java       # 发布技能
    │   ├── SyncResult.java         # 同步结果模型
    │   └── ReleaseResult.java      # 发布结果模型
    ├── sync/
    │   ├── SyncConfig.java         # harness.toml 配置模型
    │   ├── ConfigReader.java       # TOML 配置读取器
    │   ├── PluginGenerator.java    # plugin.json 生成器
    │   ├── SettingsGenerator.java  # settings.json 生成器
    │   ├── HooksSyncer.java        # hooks.json 同步器
    │   └── DriftDetector.java      # 配置漂移检测器
    └── release/
        ├── ReleaseTrain.java       # 发布列车逻辑
        ├── ChangelogParser.java    # CHANGELOG.md 解析器
        ├── TagResolver.java        # Git tag 解析器
        └── BumpEstimator.java      # 版本升级估算器
```

### 职责划分

- **SyncSkill**：协调各生成器，执行完整同步流程
- **ReleaseSkill**：检查发布条件，输出候选发布信息（只读操作）
- **sync 包**：处理配置读取和 JSON 文件生成
- **release 包**：实现发布列车算法（与 Go 版本对等）

---

## SyncSkill 设计

### 功能

从 `harness.toml` 生成以下文件：

1. `.claude-plugin/plugin.json` - 项目元数据
2. `.claude-plugin/hooks.json` - 钩子配置（从 `hooks/hooks.json` 复制）
3. `.claude-plugin/settings.json` - Agent 配置、环境变量、权限、沙箱规则
4. 检测 `settings.json` 配置漂移（手动编辑警告）

### 数据流

```
harness.toml (SSOT)
    ↓
ConfigReader.parse()
    ↓
SyncConfig (内存模型)
    ├─→ PluginGenerator.generate() → plugin.json
    ├─→ HooksSyncer.sync() → .claude-plugin/hooks.json
    └─→ SettingsGenerator.generate() → .claude-plugin/settings.json
        ↓
DriftDetector.check() → 警告到 stderr
```

### SyncConfig 模型

```java
public class SyncConfig {
    private ProjectConfig project;
    private AgentConfig agent;
    private Map<String, String> env;
    private SafetyConfig safety;
}

class ProjectConfig {
    private String name;
    private String version;
    private String description;
    private String authorName;
    private String authorUrl;
    private String homepage;
    private String repository;
    private String license;
    private List<String> keywords;
    private List<String> outputStyles;
}

class AgentConfig {
    private String defaultAgent;  // "claude-sonnet-5"
}

class SafetyConfig {
    private PermissionsConfig permissions;
    private SandboxConfig sandbox;
}

class PermissionsConfig {
    private List<String> allow;
    private List<String> deny;
    private List<String> ask;
}

class SandboxConfig {
    private boolean failIfUnavailable;
    private NetworkConfig network;
    private FilesystemConfig filesystem;
}
```

### SyncResult 模型

```java
public class SyncResult {
    private boolean success;
    private List<String> generatedFiles;
    private List<String> driftWarnings;  // 配置漂移警告
    private String message;

    public static Builder builder() { ... }
}
```

### 错误处理

- **配置文件不存在** → 退出，显示错误
- **TOML 解析失败** → 退出，显示详细错误
- **生成器部分失败** → 收集所有错误，批量报告
- **配置漂移** → 警告到 stderr，不退出

---

## ReleaseSkill 设计

### 功能

检查是否需要发布新版本，基于发布列车算法（只读操作）：

1. 解析 `CHANGELOG.md` 的 `[Unreleased]` 部分
2. 查询最新 semver tag 及日期
3. 评估发布条件：
   - Breaking 变化
   - Tag 年龄超过阈值（7 天 / 2 天）
4. 输出 `RELEASE_CANDIDATE` 行或空字符串

### 数据流

```
CHANGELOG.md
    ↓
ChangelogParser.extractUnreleased()
    ↓
[Unreleased] 标题列表 → BumpEstimator.estimate()
    ↓
Git 命令: git tag -l "v*" --sort=-v:refname
    ↓
TagResolver.resolveLatest() → tagName, tagDate
    ↓
ReleaseTrain.evaluate(changelog, tagDate, now)
    ↓
ReleaseResult (三态: candidate/none/not-applicable)
```

### ReleaseResult 模型

```java
public class ReleaseResult {
    private String state;  // "candidate" | "none" | "not-applicable"
    private String bump;   // "major" | "minor" | "patch"
    private List<String> reasons;  // ["breaking", "tag_age"]
    private String tagName;
    private int tagAgeDays;
    private int thresholdDays;

    public String formatLine() {
        // 输出: RELEASE_CANDIDATE: bump=patch tag=v1.2.3 age_days=8 threshold_days=7 reasons=tag_age
    }
}
```

### 发布列车算法（ReleaseTrain）

从 Go 版本移植的逻辑：

```java
public class ReleaseTrain {
    private static final int DEFAULT_THRESHOLD_DAYS = 7;
    private static final int SECURITY_THRESHOLD_DAYS = 2;

    public static ReleaseResult evaluate(String changelog, Instant lastTagDate, boolean hasTag, Instant now) {
        // 1. 检查是否有 [Unreleased] 内容
        if (!hasUnreleased(changelog) || !hasTag) {
            return notApplicable();
        }

        // 2. 收集标题
        List<String> headings = collectHeadings(changelog);
        if (headings.isEmpty()) {
            return none();
        }

        // 3. 确定阈值
        int threshold = hasHeadingPrefix(headings, "Security") ?
            SECURITY_THRESHOLD_DAYS : DEFAULT_THRESHOLD_DAYS;

        // 4. 计算 tag 年龄
        int tagAge = tagAgeDays(lastTagDate, now);
        boolean hasBreaking = hasBreakingHeading(headings);

        // 5. 评估触发条件
        List<String> reasons = new ArrayList<>();
        boolean triggered = false;

        if (hasBreaking) {
            triggered = true;
            reasons.add("breaking");
        }
        if (tagAge >= threshold) {
            triggered = true;
            reasons.add("tag_age");
        }

        if (!triggered) {
            return none(tagAge, threshold);
        }

        // 6. 估算 bump 类型
        String bump = estimateBump(headings);

        return candidate(bump, reasons, tagName, tagAge, threshold);
    }

    private static String estimateBump(List<String> headings) {
        if (hasBreakingHeading(headings) || hasHeadingPrefix(headings, "Removed")) {
            return "major";
        }
        if (hasHeadingPrefix(headings, "Added") || hasHeadingPrefix(headings, "Deprecated")) {
            return "minor";
        }
        return "patch";
    }
}
```

### 错误处理

- **CHANGELOG.md 不存在** → `state = "not-applicable"`（不是错误）
- **无 tag** → `state = "not-applicable"`（不是错误）
- **Git 命令失败** → 真正的错误，返回失败状态

---

## 技术选型

### 依赖项

**TOML 解析器**：
```xml
<dependency>
    <groupId>org.tomlj</groupId>
    <artifactId>tomlj</artifactId>
    <version>1.1.0</version>
</dependency>
```

**JSON 处理**（使用已有的 Jackson）：
```xml
<!-- 已在项目中 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**Git 操作**（无需新依赖，使用 ProcessBuilder）：
```java
ProcessBuilder pb = new ProcessBuilder(
    "git", "tag", "-l", "v*", "--sort=-v:refname"
);
pb.directory(new File(projectRoot));
Process process = pb.start();
```

---

## 测试策略

### 单元测试

每个类独立测试：

- `SyncSkillTest` - 测试完整同步流程
- `ReleaseSkillTest` - 测试发布检查逻辑
- `ChangelogParserTest` - 测试 CHANGELOG 解析
- `ReleaseTrainTest` - 测试发布列车算法（从 Go 移植测试用例）
- `ConfigReaderTest` - 测试 TOML 解析
- `DriftDetectorTest` - 测试漂移检测

### 集成测试

- `SyncIntegrationTest` - 真实文件系统测试
- `ReleaseIntegrationTest` - 真实 Git 仓库测试

### 测试覆盖重点

- 配置漂移检测逻辑
- 各种 bump 类型估算（major/minor/patch）
- 边界条件：
  - 无 tag
  - 无 changelog
  - 空 `[Unreleased]` 内容
  - Breaking 变化
  - Security 标题（2 天阈值）

---

## 与 Go 版本的对等性

### 功能对等

| Go 功能 | Java 对等 | 状态 |
|---------|-----------|------|
| `harness sync` | `SyncSkill` | 待实现 |
| 生成 plugin.json | `PluginGenerator` | 待实现 |
| 同步 hooks.json | `HooksSyncer` | 待实现 |
| 生成 settings.json | `SettingsGenerator` | 待实现 |
| 配置漂移检测 | `DriftDetector` | 待实现 |
| `harness release --check` | `ReleaseSkill` | 待实现 |
| 发布列车算法 | `ReleaseTrain` | 待实现 |

### 输出格式对等

Go 版本输出：
```
RELEASE_CANDIDATE: bump=patch tag=v1.2.3 age_days=8 threshold_days=7 reasons=tag_age
```

Java 版本输出（完全一致）：
```
RELEASE_CANDIDATE: bump=patch tag=v1.2.3 age_days=8 threshold_days=7 reasons=tag_age
```

---

## 实现计划

参见实现计划文档：`docs/superpowers/plans/YYYY-MM-DD-sync-release-implementation.md`

---

## 设计决策记录

### 为什么选择 harness.toml 作为 SSOT？

**决策**：使用独立的 `harness.toml` 配置文件

**理由**：
1. 与 Go 版本保持一致，便于维护
2. 单一配置源，避免配置分散
3. TOML 格式简洁，易于阅读和编辑

### 为什么使用 ProcessBuilder 而不是 JGit？

**决策**：使用 `ProcessBuilder` 调用 git 命令

**理由**：
1. 与 Go 版本方式一致，便于对等实现
2. 无需额外依赖（JGit 体积较大）
3. 利用系统已安装的 git，保持环境一致性

### 为什么 ReleaseSkill 是只读操作？

**决策**：`ReleaseSkill` 只检查发布条件，不执行发布

**理由**：
1. 与 Go 版本 `--check` 功能对等
2. 发布是危险操作，应该由用户显式触发
3. 检查和发布分离，符合单一职责原则

---

## 未来扩展

### 可能的未来功能

1. **ReleaseSkill 执行发布**：实际执行发布（打 tag、推送到远程）
2. **更多配置源**：支持从 `pom.xml` 或 `build.gradle` 读取部分配置
3. **配置验证**：在 sync 前验证配置完整性
4. **更多漂移检测**：检测 `plugin.json` 和 `hooks.json` 的手动编辑

---

## 参考资料

- Go 实现：`D:\go-project\claude-code-harness\go\cmd\harness\sync.go`
- Go 实现：`D:\go-project\claude-code-harness\go\cmd\harness\release_check.go`
- Go 实现：`D:\go-project\claude-code-harness\go\internal\releasetrain\releasetrain.go`
- TOML 规范：https://toml.io/en/
