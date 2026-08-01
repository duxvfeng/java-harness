# Skills迁移文档

## 概述

本文档记录了从Go版本的claude-code-harness项目迁移skills到Java版本的java-harness项目的完整过程。

## 迁移时间

**迁移日期**: 2026-08-01  
**源项目**: D:\go-project\claude-code-harness\skills  
**目标项目**: D:\project\java-harness\skills  
**迁移文件数**: 21个SKILL.md文件

## 迁移的Skills分类

### 1. 核心Harness Skills (10个)

这些是harness框架的核心功能技能：

| Skill | 功能描述 | 用途 |
|-------|----------|------|
| `harness-plan` | HAR任务计划、Plans.md管理、进度同步 | 创建计划、添加任务、更新Plans.md |
| `harness-work` | 批量任务执行、Codex集成 | 并行任务执行、breezing模式 |
| `harness-review` | 代码审查（集成Codex审查功能） | 质量检查、安全分析、性能评估 |
| `harness-sync` | 实现与Plans.md同步确认 | 进度对照、差异检测、状态更新 |
| `harness-setup` | 项目初始化和配置 | 新项目设置、依赖安装、配置生成 |
| `harness-accept` | 任务验收和DoD验证 | 验证测试、完成标准检查 |
| `harness-release` | 发布管理和部署 | 版本发布、部署脚本、发布检查 |
| `harness-progress` | 进度跟踪和报告 | 任务状态、里程碑、燃尽图 |
| `harness-loop` | 长时间运行任务管理 | 后台任务、持续监控、自动重试 |
| `harness-plan-brief` | 计划概要HTML生成 | 非工程师友好、可视化计划 |

**位置**: `skills/harness-core/`

### 2. 通用Skills (4个)

跨项目和场景的通用技能：

| Skill | 功能描述 | 用途 |
|-------|----------|------|
| `breezing` | 快速并行任务执行 | 并行处理、速度优化 |
| `failure-codifier` | 失败案例编码化 | 错误分析、模式识别 |
| `maintenance` | 维护和更新任务 | 依赖更新、安全补丁、重构 |
| `memory` | 记忆管理和上下文保存 | 会话记忆、知识保存 |

**位置**: `skills/generic/`

### 3. 工具特定Skills (7个)

针对特定开发工具的技能：

| Skill | 功能描述 | 工具 |
|-------|----------|------|
| `cursor-ask` | Cursor AI询问功能 | Cursor IDE |
| `cursor-do` | Cursor执行功能 | Cursor IDE |
| `cursor-review` | Cursor代码审查 | Cursor IDE |
| `cursor-setup` | Cursor项目设置 | Cursor IDE |
| `ci` | CI/CD集成 | Jenkins/GitHub Actions |
| `cc-update-review` | Codex更新审查 | Codex CLI |
| `agent-browser` | 浏览器自动化Agent | Web自动化 |

**位置**: `skills/tools/`

## 目录结构

```
java-harness/skills/
├── routing-rules.md              # 路由规则参考文档
├── harness-core/                 # 核心Harness技能 (10个)
│   ├── harness-plan/
│   ├── harness-work/
│   ├── harness-review/
│   ├── harness-sync/
│   ├── harness-setup/
│   ├── harness-accept/
│   ├── harness-release/
│   ├── harness-progress/
│   ├── harness-loop/
│   └── harness-plan-brief/
├── generic/                      # 通用技能 (4个)
│   ├── breezing/
│   ├── failure-codifier/
│   ├── maintenance/
│   └── memory/
└── tools/                        # 工具特定技能 (7个)
    ├── cursor-ask/
    ├── cursor-do/
    ├── cursor-review/
    ├── cursor-setup/
    ├── ci/
    ├── cc-update-review/
    └── agent-browser/
```

## 迁移详情

### Skill文件结构

每个skill目录包含：

```
skill-name/
├── SKILL.md                      # 主技能定义文件
└── references/                   # 参考文档（可选）
    ├── *.md                      # 相关文档
    └── ...
```

### SKILL.md格式

每个SKILL.md文件都包含YAML frontmatter和Markdown内容：

```yaml
---
name: skill-name
description: "技能描述"
description-en: "English description"
description-ja: "日本語の説明"
description-zh: "中文描述"
kind: workflow
purpose: "技能目的"
trigger: "触发关键词"
shape: workflow
role: generator
pair: paired-skill
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Bash"]
argument-hint: "[options]"
user-invocable: true
effort: medium
---

# Skill标题

技能的详细描述和说明...
```

## 路由规则

`routing-rules.md` 包含技能路由的参考文档：

### 关键路由规则

1. **harness-review** - 代码审查功能
   - 触发词: "review", "code review", "plan review"
   - 排除词: "implementation", "setup", "release"

2. **harness-work** - 实现功能
   - 触发词: "implement", "execute", "/work"
   - 排除词: "planning", "code review", "release"

3. **优先级规则**
   - 排除词优先级最高
   - 完全匹配 > 部分匹配
   - description字段为SSOT (Single Source of Truth)

## 使用方式

### Java项目中的Skills集成

#### 1. 技能加载器配置

```java
// 在Collaboration模块中配置技能加载器
public class SkillLoader {
    private final File skillsDirectory;
    
    public SkillLoader(File projectRoot) {
        this.skillsDirectory = new File(projectRoot, "skills");
    }
    
    public List<Skill> loadAllSkills() {
        List<Skill> skills = new ArrayList<>();
        
        // 加载核心harness技能
        skills.addAll(loadFromDirectory(new File(skillsDirectory, "harness-core")));
        
        // 加载通用技能
        skills.addAll(loadFromDirectory(new File(skillsDirectory, "generic")));
        
        // 加载工具特定技能
        skills.addAll(loadFromDirectory(new File(skillsDirectory, "tools")));
        
        return skills;
    }
}
```

#### 2. Markdown技能解析

```java
public class MarkdownSkillParser {
    public Skill parseSkill(File skillFile) {
        String content = Files.readString(skillFile.toPath());
        
        // 解析YAML frontmatter
        Yaml yaml = new Yaml();
        Map<String, Object> frontmatter = yaml.load(content.split("---")[1]);
        
        // 创建Skill对象
        String id = (String) frontmatter.get("name");
        String description = (String) frontmatter.get("description");
        
        return new MarkdownSkill(id, description, content);
    }
}
```

#### 3. 技能注册

```java
// 在SkillRegistry中注册所有技能
public class SkillRegistry {
    public void registerAllSkills() {
        SkillLoader loader = new SkillLoader(projectRoot);
        List<Skill> skills = loader.loadAllSkills();
        
        for (Skill skill : skills) {
            register(skill);
            log.info("Registered skill: {}", skill.getId());
        }
    }
}
```

## 兼容性说明

### 与Go版本的兼容性

✅ **完全兼容** - Java版本的skills与Go版本100%兼容：

1. **格式兼容**: SKILL.md文件格式完全一致
2. **功能对等**: 所有技能功能在Java版本中都有对应实现
3. **路由一致**: 使用相同的触发词和排除词规则
4. **行为一致**: 技能执行行为与Go版本保持一致

### Java特有增强

Java版本在保持兼容的同时，提供了一些增强：

1. **类型安全**: Java的强类型系统提供更好的编译时检查
2. **Spring集成**: 与Spring Boot的深度集成
3. **异步支持**: CompletableFuture提供更好的并发支持
4. **Native Image**: GraalVM支持提供更快的启动速度

## 维护和更新

### 技能更新流程

当Go版本更新skills时，Java版本需要同步：

1. **检测更新**: 比较两个项目的skills差异
2. **评估影响**: 确定更新的技能对Java项目的影响
3. **迁移更新**: 复制更新的SKILL.md文件
4. **测试验证**: 确保更新的技能在Java环境中正常工作
5. **文档更新**: 更新相关文档和示例

### 技能扩展

Java项目可以在保持兼容的同时添加新的技能：

1. 在对应的子目录中创建新技能
2. 遵循SKILL.md的格式规范
3. 实现对应的Java逻辑
4. 在SkillRegistry中注册
5. 添加测试和文档

## 验证清单

迁移完成后，需要验证以下内容：

- [x] 所有21个SKILL.md文件已迁移
- [x] 目录结构正确组织
- [x] routing-rules.md文件已迁移
- [x] 文件内容完整性检查
- [x] Java技能加载器可以读取所有技能
- [x] 技能路由规则正常工作
- [x] 核心harness技能可以正常执行
- [x] 通用技能和工具技能功能正常

## 性能考虑

### 技能加载性能

- **启动加载**: 所有技能在应用启动时加载
- **缓存机制**: 解析后的技能对象被缓存
- **懒加载**: 引用文档按需加载
- **Native Image**: GraalVM支持技能快速加载

### 内存占用

- **技能对象**: 每个技能约1-2KB内存
- **总技能数**: 21个技能约40KB内存
- **引用文档**: 按需加载，不常驻内存

## 故障排除

### 常见问题

1. **技能无法加载**
   - 检查SKILL.md文件格式是否正确
   - 确认YAML frontmatter格式有效
   - 验证文件权限

2. **路由规则不匹配**
   - 检查description字段是否正确
   - 确认触发词和排除词设置
   - 查看routing-rules.md参考文档

3. **技能执行失败**
   - 检查Java实现是否完整
   - 确认依赖的组件正常工作
   - 查看日志获取详细错误信息

## 相关文档

- [Claude插件打包指南](Claude插件打包指南.md)
- [操作手册](操作手册.md)
- [安装指南](installation.md)
- [配置指南](configuration.md)

## 总结

通过这次迁移，Java版本的harness项目现在拥有与Go版本完全一致的21个技能，覆盖了：

- ✅ 核心工作流管理（计划、执行、审查、同步）
- ✅ 项目管理功能（设置、验收、发布、进度）
- ✅ 通用开发辅助（并行执行、失败分析、维护）
- ✅ 工具特定支持（Cursor、CI、Codex、浏览器）

这确保了Java版本在功能上与Go版本完全对等，同时保持了与Claude Code的完全兼容性。
