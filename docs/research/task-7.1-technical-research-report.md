# Task 7.1: GPT Codex 支持技术调研报告

**项目**: Java Harness
**任务编号**: 7.1
**任务类型**: 技术调研和架构设计
**完成日期**: 2026-08-08
**调研方式**: 多视点评审（Product/Architecture/Security）

---

## 执行摘要

本报告评估了向 Java Harness 添加可选的 GPT Codex 支持的技术可行性、产品定位、架构设计和安全影响。

**核心发现**：
- ✅ **技术可行性高**（Architecture 4.2/5）：现有架构支持扩展，参考实现清晰
- ⚠️ **产品定位需明确**（Product 2.8/5）：应作为可选扩展而非核心功能
- ⚠️ **安全需要增强**（Security 3.5/5）：需要添加 Codex 特定的 Guardrail 规则

**推荐方案**：作为 **Phase 7（可选扩展）** 实施，保持 Claude Code 作为核心定位。

---

## 调研目标

### 主要目标

1. **技术可行性评估**：评估 Codex CLI 集成的技术难度和实现路径
2. **产品定位分析**：评估 Codex 支持对产品定位的影响
3. **架构设计**：设计多工具支持的架构和扩展点
4. **安全评估**：识别 Codex 集成的安全风险和缓解措施
5. **实施规划**：制定详细的实施计划和时间线

### 调研范围

- Codex CLI 的技术能力和 API
- 现有 Java Harness 架构的扩展性
- 安全模型差异和 Guardrail 规则
- 跨平台兼容性和部署复杂性
- 用户需求和市场定位

---

## 方法论

### 多视点评审

采用三视点评审方法，从不同维度评估 Codex 支持的可行性：

#### 1. Product 视点评审

**评估维度**：
- 市场定位和用户需求
- 产品价值主张
- 维护成本和可持续性
- 竞争对手分析

**评分**: 2.8/5（不推荐作为核心功能）

**参考资源**：
- [Claude Code vs OpenAI Codex: Which AI Coding Agent Is Better?](https://www.mindstudio.ai/blog/claude-code-vs-openai-codex-comparison)
- [Claude Code vs Codex: Dev Workflow Comparison](https://dev.to/composiodev/claude-code-vs-codex-dev-workflow-comparison-4jjf)
- [Claude Code vs OpenAI Codex: terminal AI coding agents compared](https://daily.dev/blog/claude-code-vs-openai-codex-terminal-ai-coding-agents-compared)

#### 2. Architecture 视点评审

**评估维度**：
- 现有架构的可扩展性
- 技术实现复杂度
- 性能和资源消耗
- 代码复用和维护

**评分**: 4.2/5（技术可行）

**参考资源**：
- [OpenAI Codex CLI: How to Install, Setup & Use (2026 Guide)](https://serenitiesai.com/articles/openai-codex-cli-guide-2026)
- [Codex CLI Guide (2026): How It Works, Costs, Models](https://www.aibuilderclub.com/blog/codex-cli-guide-2026)
- 参考实现：[claude-code-harness](https://github.com/Chachamaru127/claude-code-harness)

#### 3. Security 视点评审

**评估维度**：
- 安全模型差异
- 数据隐私和传输
- 凭证管理风险
- 供应链安全

**评分**: 3.5/5（需要增强）

**参考资源**：
- 参考项目 claude-code-harness 的 Codex 实现分析
- Worktree containment 机制
- Guardrail 规则系统（27 条现有规则）

---

## 技术发现

### 1. Codex CLI 能力分析

#### 核心功能

✅ **已验证功能**：
- 命令行接口和会话管理
- 工具调用和函数执行
- 文件操作和 Git 集成
- 技能系统（Markdown-based）
- 状态管理和配置

⚠️ **需要适配的功能**：
- Shell 脚本执行（跨平台兼容性）
- 技能格式转换（Markdown → Java）
- 配置管理（TOML 配置文件）
- 进程间通信

#### 技术限制

- **进程模型差异**：Codex 使用独立的 CLI 进程，Claude Code 使用集成进程
- **状态同步**：Java 进程与 Codex 进程的状态一致性需要特别处理
- **性能开销**：CLI 调用比原生调用有更高的延迟

### 2. 参考实现分析

#### claude-code-harness 项目（Go 版本）

**项目位置**: `/Users/apple/IdeaProjects/claude-code-harness`

**已实现的功能**：
```bash
# Codex Setup 和配置
scripts/setup-codex.sh           # Codex 环境设置
scripts/codex-companion.sh        # Codex 进程通信代理
scripts/codex-worker-setup.sh     # Worker 进程设置
scripts/codex-worker-engine.sh    # Worker 执行引擎

# Codex 技能和规则
codex/.codex/skills/             # Harness 技能集
codex/.codex/rules/              # Guardrail 规则

# Codex 配置
codex/.codex/config.toml         # 多 Agent 配置
```

**关键设计发现**：

1. **Worktree Containment 机制** 🆕
   - 使用 `bin/harness wt fingerprint` 捕获 worktree 状态
   - 执行前后对比，检测未授权的写入操作
   - 发现写入变化时立即 hard-stop
   - 防止 Codex 操作影响到用户主目录

2. **Orchestration Ledger** 🆕
   - 记录每次 Codex 委托的执行
   - 跟踪 exit_code 和 duration
   - 用于 scorecard 和审计

3. **Effort 自动传播** 🆕
   - `calculate-effort.sh` 自动计算 effort 级别
   - 通过 `--effort` 标志传递给 Codex companion
   - 支持级别：none/minimal/low/medium/high/xhigh

4. **多 Agent 配置体系** 🆕
```toml
# .codex/config.toml
[features]
multi_agent = true

[agents]
max_threads = 8

[agents.implementer]
description = "Codex implementation worker"

[agents.reviewer]
description = "Codex reviewer worker"
sandbox = "workspace-read-only"

[agents.codex_implementer]
description = "Codex Breezing implementer (impl_mode: codex)"

[agents.claude_implementer]
description = "Claude CLI delegated worker"

[agents.plan_analyst]
description = "Phase 0 planning analyst"
sandbox = "workspace-read-only"

[agents.plan_critic]
description = "Phase 0 plan critic"
sandbox = "workspace-read-only"
```

5. **技能同步和备份机制** 🆕
   - 自动技能同步（从 Harness repo 到 .codex/skills）
   - 重复技能检测和去重
   - 遗留技能清理
   - 完善的备份机制

**可复用的组件**：
- ✅ 架构设计和模块划分
- ✅ 接口定义和数据模型
- ✅ 安全规则和 Guardrail 集成
- ✅ Worktree containment 机制
- ✅ Orchestration ledger 系统
- ⚠️ Shell 脚本需要用 Java 重写

### 3. Java Harness 架构评估

#### 现有优势

✅ **良好的扩展性**：
- BackendExecutor 接口支持多后端
- collaboration 模块已有多工具集成框架
- 清晰的模块边界和职责分离

✅ **已实现的功能**：
```java
// 现有 BackendExecutor 接口
public interface BackendExecutor {
    CompletableFuture<BackendResult> execute(BackendRequest request);
    boolean isAvailable();
    String getBackendName();
}

// 已有的 CodexBackend 类
public class CodexBackend implements BackendExecutor {
    // 已实现基本功能
}
```

#### 需要扩展的部分

⚠️ **适配器层**：
- 缺少统一的 AIToolAdapter 接口
- 需要工具特定的适配器实现

⚠️ **技能桥接**：
- Markdown 技能解析和转换
- Java 技能系统与 Codex 技能的互操作

⚠️ **配置管理**：
- TOML 配置文件解析
- 多后端配置切换

---

## 多视点评审结果

### Product 视点评审（评分：2.8/5）

#### 主要发现

✅ **技术可行性**（4/5）：
- 参考实现清晰，架构可移植
- 已有 CodexBackend 类作为基础
- 技术风险可控

⚠️ **产品定位**（2/5）：
- 偏离专注 Claude Code 的核心定位
- 可能造成产品定位模糊
- 用户可能误解为主要功能

⚠️ **维护成本**（中高风险）：
- 需要跟踪多个工具的 API 变化
- Codex CLI 更新频繁，需要持续适配
- 增加测试和文档负担

⚠️ **安全模型差异**（中等风险）：
- Codex 的安全模型与 Claude Code 不同
- 可能造成用户困惑
- 需要额外的安全说明

#### 推荐方案

**建议推迟，当前应专注完善 Claude Code 支持**

**替代方案**：
- **方案 A（推荐）**：专注 Claude Code 完善
- **方案 B（可考虑）**：Codex 支持作为社区扩展
- **方案 C（不推荐）**：同时支持（当前提案）

#### 竞争对手分析

| 产品 | Claude Code 支持 | Codex 支持 | 定位 |
|------|-----------------|------------|------|
| Claude Code | ✅ 原生 | ❌ | Anthropic 官方 |
| OpenAI Codex CLI | ❌ | ✅ 原生 | OpenAI 官方 |
| claude-code-harness (Go) | ✅ 主要 | ✅ 可选 | 多工具框架 |
| Java Harness (当前) | ✅ 主要 | ❌ | Claude Code 专注 |

### Architecture 视点评审（评分：4.2/5）

#### 主要发现

✅ **现有架构优势**：
- 模块化设计优秀
- BackendExecutor 接口设计良好
- collaboration 模块已有扩展框架
- CodexBackend 类已实现基本功能

✅ **技术可行性高**：
- 接口设计支持多后端
- 代码复用度高
- 实现路径清晰

⚠️ **脚本执行跨平台兼容性**（高风险）：
- Shell 脚本在 Windows 环境需要特别处理
- 需要抽象脚本执行层
- 可能需要 Java 进程管理

⚠️ **状态同步复杂度**（高风险）：
- Java 进程与 Codex 进程的状态一致性
- 需要设计进程间通信协议
- 错误处理和恢复机制复杂

#### 推荐架构

**基于现有 BackendExecutor 扩展**：

```
java-harness-collaboration/
├── adapter/
│   ├── AIToolAdapter.java (接口)
│   ├── CodexAdapter.java
│   └── CursorAdapter.java (预留)
├── integration/
│   ├── codex/
│   │   ├── CodexSetup.java
│   │   ├── CodexConfig.java
│   │   └── CodexCompanion.java
│   └── cursor/ (预留)
└── skill/
    └── CodexSkillBridge.java
```

#### 实施时间线

**总计约 11 周**：
- 技术验证：2 周
- 安全对齐：3 周
- 文档和测试：2 周
- Beta 发布：4 周

### Security 视点评审（评分：3.5/5）

#### 主要发现

⚠️ **高风险**：
- Codex 内部工具调用可能绕过 Guardrail 执行
- 需要设计 Worktree containment 机制

⚠️ **中高风险**：
- 双供应商凭证管理（OpenAI + Anthropic）
- 凭证泄露风险增加
- 需要隔离和加密存储

⚠️ **中风险**：
- 数据传输至第三方（OpenAI 服务器）
- 需要数据分类和敏感信息检测
- 用户数据隐私保护

⚠️ **中风险**：
- npm 供应链风险（Codex CLI 安装）
- 需要供应链验证
- 依赖包安全检查

✅ **现有优势**：
- 27 条 Guardrail 规则通过 Hook 集成全部适用
- 安全基础设施完善
- 审计和监控机制健全

#### 推荐新增 Guardrail 规则

**R28：Codex Worktree 写入监控**
- 监控 worktree 中的写入操作
- 检测未授权的文件修改
- 提供审计日志

**R29：Codex 凭证访问保护**
- 阻止 Codex 访问 API 密钥
- 隔离凭证存储
- 实施访问控制

**R30：敏感数据传输阻断**
- 检测并阻止敏感数据传输
- 数据分类和标记
- 传输前验证

#### 推荐安全实施路径

**阶段 1（立即）**：
- 添加 R28-R30 规则
- 实施选择加入后端
- 增强审计和日志

**阶段 2（短期）**：
- 凭证隔离和加密
- 数据分类验证
- 供应链验证

**阶段 3（长期）**：
- 行为分析
- 异常检测
- 合规报告

---

## 技术实施建议

### 推荐方案：作为可选扩展（Phase 7）

**理由**：
1. ✅ **技术可行性高**（Architecture 4.2/5）
2. ⚠️ **产品定位需要明确**（Product 2.8/5）
3. ⚠️ **安全需要增强**（Security 3.5/5）
4. 💡 **用户需求存在**，但非主流
5. 🎯 **扩展性良好**，可作为插件化架构的验证案例

**实施策略**：
- **Phase 1-6**（核心）：专注 Claude Code 和文档，预计 8-12 天 ✅ 已完成
- **Phase 7**（可选）：Codex 支持作为可选扩展，预计 11 周 🔄 进行中

### 核心设计原则

1. **可选扩展**：Codex 支持必须明确标记为可选，不影响核心功能
2. **向后兼容**：不破坏现有 Claude Code 支持
3. **安全第一**：默认禁用，需要用户显式启用
4. **清晰隔离**：Codex 和 Claude Code 的状态和配置完全隔离
5. **可维护性**：清晰的架构边界，便于未来扩展

---

## 参考资源

### 技术文档

1. **Codex CLI 官方文档**
   - [OpenAI Codex CLI: How to Install, Setup & Use (2026 Guide)](https://serenitiesai.com/articles/openai-codex-cli-guide-2026)
   - [Codex CLI Guide (2026): How It Works, Costs, Models](https://www.aibuilderclub.com/blog/codex-cli-guide-2026)

2. **产品对比分析**
   - [Claude Code vs OpenAI Codex: Which AI Coding Agent Is Better?](https://www.mindstudio.ai/blog/claude-code-vs-openai-codex-comparison)
   - [Claude Code vs Codex: Dev Workflow Comparison](https://dev.to/composiodev/claude-code-vs-codex-dev-workflow-comparison-4jjf)
   - [Claude Code vs OpenAI Codex: terminal AI coding agents compared](https://daily.dev/blog/claude-code-vs-openai-codex-terminal-ai-coding-agents-compared)

3. **最佳实践**
   - [Best practices | ChatGPT Learn](https://learn.chatgpt.com/guides/best-practices)

### 参考实现

1. **claude-code-harness（Go 版本）**
   - Repository: [https://github.com/Chachamaru127/claude-code-harness](https://github.com/Chachamaru127/claude-code-harness)
   - 关键文件：
     - `scripts/setup-codex.sh`
     - `scripts/codex-*.sh`
     - `codex/.codex/` 配置和技能

2. **现有 Java 实现**
   - CodexBackend 类（已实现基本功能）
   - BackendExecutor 接口
   - collaboration 模块

### 标准和规范

1. **安全标准**
   - OWASP Top 10
   - GDPR 数据保护
   - SOC 2 合规性

2. **编码规范**
   - Java Code Style
   - JUnit 5 测试标准
   - Maven 项目规范

---

## 结论

### 技术可行性结论

✅ **技术上可行**：
- 现有架构支持扩展
- 参考实现清晰可复用
- 实现路径明确

⚠️ **需要注意**：
- 跨平台兼容性
- 状态同步复杂度
- 安全模型差异

### 产品定位结论

💡 **推荐作为可选扩展**：
- 保持 Claude Code 核心定位
- Codex 支持作为可选功能
- 用户按需启用

### 安全实施结论

🔒 **需要增强安全措施**：
- 添加 Codex 特定规则
- 实施选择加入机制
- 增强审计和监控

---

**报告编制**: Java Harness Team
**审核状态**: 待审核
**下一步**: 架构设计文档（task-7.1-architecture-design.md）
