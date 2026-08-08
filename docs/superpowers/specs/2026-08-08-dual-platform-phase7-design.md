# Phase 7 双平台适配设计文档

**日期**: 2026-08-08
**状态**: 设计阶段 - 等待用户批准
**作者**: Claude & duxvfeng

## 1. 背景和目标

### 当前状态
Java Harness 当前 Plans.md 中的 Phase 7 为"GPT Codex 支持（可选扩展）"，专注于单一的 Codex 平台适配。

### 变更驱动
项目已具备完整的双平台架构设计（dual-platform-compatibility.md），需要将 Phase 7 从"单一 Codex 支持"更新为"Claude + Codex 双平台支持"。

### 目标
重新设计 Phase 7 任务分解，使其：
- 反映双平台适配而非单一平台
- 基于 dual-platform-compatibility.md 的架构设计
- 保持任务结构的合理性和可执行性
- 维持可选扩展的性质

## 2. 设计原则

### 更新范围
- **主要更新**: 文档和计划内容，反映双平台架构
- **次要更新**: 任务分解优化，匹配实际实现流程
- **保持不变**: 可选扩展性质、现有完成的任务

### 架构基础
基于已有的双平台架构文档：
- 统一插件结构（双平台 plugin.json）
- 平台检测机制（PlatformDetector）
- 统一技能接口（UniversalSkill）
- 后端选择策略（BackendSelector）
- 配置兼容层（harness.toml + 平台特定配置）

## 3. 新 Phase 7 任务设计

### 7.1 双平台架构设计验证 ✅
- **类型**: 研究任务
- **内容**: 验证现有的架构设计是否符合双平台需求，确认统一插件结构和配置兼容层的设计
- **状态**: cc:完成（已完成）
- **TDD**: [tdd:skip:research-task]

### 7.2 平台检测机制实现 (cc:TODO)
- **类型**: 核心代码实现
- **内容**:
  - 实现 `PlatformDetector` 类，支持 Claude Code 和 Codex 环境检测
  - 环境特征检测：`CLAUDE_API_KEY`、`/.claude`、`~/.codex` 等
  - 自动识别运行环境并返回平台枚举
- **DoD**: `PlatformDetector.detectCurrentPlatform()` 能准确识别当前运行平台，单元测试通过
- **TDD**: [tdd:required]
- **依赖**: 7.1

### 7.3 统一技能接口实现 (cc:TODO)
- **类型**: 核心代码实现
- **内容**:
  - 实现 `UniversalSkill` 接口，包含 `getId()`, `getName()`, `execute()`
  - 实现 `adaptForPlatform(Platform)` 方法，支持平台特定适配
  - 保持现有技能系统兼容性，确保现有技能无需修改即可工作
- **DoD**: 现有技能在新接口下正常运行，兼容性测试通过
- **TDD**: [tdd:required]
- **依赖**: 7.2

### 7.4 后端选择策略实现 (cc:TODO)
- **类型**: 核心代码实现
- **内容**:
  - 实现 `BackendSelector` 类，实现优先级逻辑：明确指定 > 自动检测 > 平台默认 > 系统默认
  - 与 `harness.toml` 配置集成，支持 `[harness.backend]` 配置
  - 环境变量支持（`HARNESS_BACKEND`）
- **DoD**: 能根据配置和环境选择正确的后端，集成测试通过
- **TDD**: [tdd:required]
- **依赖**: 7.3

### 7.5 配置兼容层实现 (cc:TODO)
- **类型**: 核心代码实现
- **内容**:
  - 创建双平台配置文件结构：`.claude-plugin/` 和 `.codex-plugin/`
  - 实现统一的 `harness.toml` 配置解析器
  - 支持平台特定配置段（`[platform.claude]`, `[platform.codex]`）
- **DoD**: 两个平台都能正确读取配置，配置解析测试通过
- **TDD**: [tdd:required]
- **依赖**: 7.4

### 7.6 双平台配置文件更新 (cc:TODO)
- **类型**: 配置和文档
- **内容**:
  - 更新根目录 `plugin.json` 以支持两个平台的元数据
  - 创建 `.claude-plugin/marketplace.json`（Claude Code 专用）
  - 创建 `.codex-plugin/plugin.json`（Codex 专用）
  - 更新安装脚本，支持双平台安装
- **DoD**: 两个平台都能正确安装和识别插件，安装测试通过
- **TDD**: [tdd:skip:config-task]
- **依赖**: 7.5

### 7.7 双平台集成测试 (cc:TODO)
- **类型**: 测试任务
- **内容**:
  - 编写端到端测试，覆盖两个平台的完整工作流
  - 验证平台检测、技能执行、后端选择、配置解析的集成
  - 测试功能等价性（相同命令在两个平台产生等价结果）
  - 性能差异测试和优化建议
- **DoD**: 测试覆盖率达到 80%，两个平台功能等价性验证通过
- **TDD**: [tdd:skip:test-task]
- **依赖**: 7.6

### 7.8 双平台文档发布 (cc:TODO)
- **类型**: 文档任务
- **内容**:
  - 更新 README.md 包含双平台安装指南（基于 dual-platform-guide.md）
  - 创建 docs/dual-platform-usage.md 详细使用文档
  - 更新 CHANGELOG.md 记录双平台支持
  - 更新 claude-plugin/dual-platform-config.md 配置参考
- **DoD**: 文档完整，两个平台的安装流程清晰，用户能按照文档成功安装
- **TDD**: [tdd:skip:docs-only]
- **依赖**: 7.7

### 7.9 Beta 发布和反馈收集 (cc:TODO)
- **类型**: 发布任务
- **内容**:
  - 将双平台支持作为 Beta 功能发布到两个平台
  - 收集两个平台的用户反馈（安装体验、功能使用、性能表现）
  - 分析反馈并优化两个平台的性能差异
  - 准备正式发布计划
- **DoD**: Beta 发布完成，反馈收集和分析报告完成
- **TDD**: [tdd:skip:release-task]
- **依赖**: 7.8

## 4. 架构设计要点

### 4.1 平台检测机制
```java
public class PlatformDetector {
    public enum Platform { CLAUDE_CODE, CODEX, UNKNOWN }

    public static Platform detectCurrentPlatform() {
        // 检查环境变量和文件系统特征
        // 运行时自动识别，无需手动配置
    }
}
```

### 4.2 统一技能接口
```java
public interface UniversalSkill {
    String getId();
    String getName();
    SkillResult execute(SkillContext context);

    // 平台特定适配
    default Object adaptForPlatform(Platform platform) {
        return switch (platform) {
            case CLAUDE_CODE -> adaptForClaude();
            case CODEX -> adaptForCodex();
            default -> this;
        };
    }
}
```

### 4.3 后端选择策略
优先级：明确指定 > 自动检测 > 平台默认 > 系统默认

### 4.4 配置兼容层
- 统一配置文件：`harness.toml`
- 平台特定配置：`[platform.claude]`, `[platform.codex]`
- 环境变量支持：`HARNESS_BACKEND`

## 5. 任务依赖关系

```
7.1 (架构验证) ✅
  ↓
7.2 (平台检测) → 7.3 (统一接口) → 7.4 (后端选择) → 7.5 (配置兼容)
  ↓
7.6 (配置文件更新) → 7.7 (集成测试) → 7.8 (文档发布) → 7.9 (Beta 发布)
```

## 6. 兼容性保证

### 6.1 向后兼容
- 现有技能无需修改即可工作
- 现有配置文件继续有效
- 现有命令接口保持不变

### 6.2 功能等价性
- 相同命令在两个平台产生等价结果
- 核心功能（plan、work、review）完全兼容
- 平台特定功能通过适配器桥接

### 6.3 测试覆盖
- 单元测试：平台检测、接口实现、后端选择
- 集成测试：端到端工作流、配置解析、技能执行
- 兼容性测试：两个平台的功能等价性验证

## 7. 更新影响分析

### 7.1 Plans.md 更新
- 重命名 Phase 7 标题：从"GPT Codex 支持"改为"双平台适配"
- 重新分解任务：从 8 个任务调整为 9 个任务
- 更新任务内容：反映双平台而非单一平台
- 更新事前确认章节：包含双平台的 secret-read 和 external-send

### 7.2 文档更新
- 保留 dual-platform-compatibility.md 作为架构参考
- 保留 dual-platform-guide.md 作为用户指南
- 更新 README.md 添加双平台安装章节
- 可能需要创建 docs/dual-platform-implementation.md 实现细节文档

### 7.3 代码变更
- 新增：`PlatformDetector` 类
- 新增：`UniversalSkill` 接口和实现
- 新增：`BackendSelector` 类
- 更新：配置解析逻辑以支持双平台
- 更新：安装脚本和测试用例

## 8. 风险和缓解

### 8.1 技术风险
- **风险**: 平台检测可能不准确
- **缓解**: 多种检测方法组合（环境变量 + 文件系统 + 运行时特征），提供手动覆盖选项

### 8.2 兼容性风险
- **风险**: 现有技能可能在双平台环境下行为不一致
- **缓解**: 统一技能接口设计，确保默认行为一致，平台特定适配显式声明

### 8.3 测试风险
- **风险**: 两个平台的测试环境可能难以同时维护
- **缓解**: 使用抽象测试基类，共享测试用例，平台特定测试隔离

## 9. 验收标准

### 9.1 功能验收
- ✅ 平台检测准确率 100%
- ✅ 相同命令在两个平台功能等价
- ✅ 配置文件在两个平台都能正确解析
- ✅ 现有技能无需修改即可运行

### 9.2 质量验收
- ✅ 单元测试覆盖率 ≥ 80%
- ✅ 集成测试通过率 100%
- ✅ 文档完整且准确

### 9.3 用户体验验收
- ✅ 安装流程简单清晰（两个平台）
- ✅ 配置示例完整可用
- ✅ Beta 反馈整体正面

## 10. 下一步

1. **用户批准**: 用户审阅本设计文档，确认任务分解和架构设计
2. **Plans.md 更新**: 根据本设计更新 Plans.md 中的 Phase 7 内容
3. **实现计划**: 调用 writing-plans skill 创建详细的实现计划
4. **开始实施**: 按照 7.2-7.9 的顺序实现双平台适配

---

**设计文档版本**: 1.0
**最后更新**: 2026-08-08
**状态**: 等待用户批准
