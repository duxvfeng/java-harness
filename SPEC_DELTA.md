# Spec Delta: 添加 GPT Codex 支持到 Java Harness

**生成时间**: 2026-08-08
**提案人**: 用户请求
**评审方式**: 多视点评审（Product/Architecture/Security）

---

## 变更摘要

向 Java Harness 添加可选的 GPT Codex 支持，使其能够作为统一的工作流框架同时服务于 Claude Code 和 OpenAI Codex CLI 用户。

---

## 多视点评审摘要

### Product 视点评审（评分：2.8/5）

**主要发现**：
- ✅ **技术可行性**（4/5）：参考实现清晰，架构可移植
- ⚠️ **产品定位**（2/5）：偏离专注 Claude Code 的核心定位
- ⚠️ **维护成本**（中高风险）：需要跟踪多个工具的 API 变化
- ⚠️ **安全模型差异**（中等风险）：Codex 的安全模型与 Claude Code 不同

**推荐方案**：**建议推迟**，当前应专注完善 Claude Code 支持

**替代方案**：
- 方案 A（推荐）：专注 Claude Code 完善
- 方案 B（可考虑）：Codex 支持作为社区扩展
- 方案 C（不推荐）：同时支持（当前提案）

**来源**：
- [Claude Code vs OpenAI Codex: Which AI Coding Agent Is Better?](https://www.mindstudio.ai/blog/claude-code-vs-openai-codex-comparison)
- [Claude Code vs Codex: Dev Workflow Comparison](https://dev.to/composiodev/claude-code-vs-codex-dev-workflow-comparison-4jjf)
- [Claude Code vs OpenAI Codex: terminal AI coding agents compared](https://daily.dev/blog/claude-code-vs-openai-codex-terminal-ai-coding-agents-compared)

### Architecture 视点评审（评分：4.2/5）

**主要发现**：
- ✅ **现有架构优势**：已有良好的模块化设计和 BackendExecutor 接口
- ✅ **技术可行性高**：CodexBackend 类已实现基本功能
- ⚠️ **脚本执行跨平台兼容性**（高风险）：Shell 脚本在 Windows 环境需要特别处理
- ⚠️ **状态同步复杂度**（高风险）：Java 进程与 Codex 进程的状态一致性

**推荐方案**：基于现有 BackendExecutor 扩展，在 `java-harness-collaboration` 模块中增强 Codex 支持

**关键设计**：
```
java-harness-collaboration/
├── adapter/ (AI 工具适配器)
│   ├── AIToolAdapter.java (接口)
│   └── CodexAdapter.java
├── integration/codex/
│   ├── CodexSetup.java
│   ├── CodexConfig.java
│   └── CodexCompanion.java
└── skill/
    └── CodexSkillBridge.java
```

**实施时间线**：约 11 周（技术验证 2 周 + 安全对齐 3 周 + 文档测试 2 周 + 软发布 4 周）

**来源**：
- [OpenAI Codex CLI: How to Install, Setup & Use (2026 Guide)](https://serenitiesai.com/articles/openai-codex-cli-guide-2026)
- [Codex CLI Guide (2026): How It Works, Costs, Models](https://www.aibuilderclub.com/blog/codex-cli-guide-2026)
- [Best practices | ChatGPT Learn](https://learn.chatgpt.com/guides/best-practices)

### Security 视点评审（评分：3.5/5 - 中高）

**主要发现**：
- ⚠️ **高风险**：Codex 内部工具调用可能绕过 Guardrail 执行
- ⚠️ **中高风险**：双供应商凭证管理（OpenAI + Anthropic）
- ⚠️ **中风险**：数据传输至第三方（OpenAI 服务器）
- ⚠️ **中风险**：npm 供应链风险（Codex CLI 安装）
- ✅ **现有优势**：27 条 Guardrail 规则通过 Hook 集成全部适用

**推荐新增 Guardrail 规则**：
- **R28：Codex Worktree 写入监控** - 监控 worktree 中的写入操作
- **R29：Codex 凭证访问保护** - 阻止 Codex 访问 API 密钥
- **R30：敏感数据传输阻断** - 检测并阻止敏感数据传输

**推荐安全实施路径**：
1. **阶段 1（立即）**：添加 R28-R30 规则，实施选择加入后端，增强审计
2. **阶段 2（短期）**：凭证隔离，数据分类验证，供应链验证
3. **阶段 3（长期）**：行为分析，异常检测，合规报告

**评审结论**：在实施阶段 1 安全增强后进行 Codex 集成。现有 Guardrail 系统提供足够保护，但双供应商运营需要增强监控。

**来源**：
- 安全评审基于参考项目 claude-code-harness 的 Codex 实现分析
- Worktree containment 机制和 orchestration ledger 设计

---

## Spec Delta

### 需要更新的 spec.md 章节

#### 1. 产品身份（Product Identity）

**当前**：
```markdown
**Purpose**: Java implementation of Claude Code Harness v4
```

**更新为**：
```markdown
**Purpose**: Java-native AI workflow framework supporting Claude Code and OpenAI Codex CLI
**Scope**: Unified workflow orchestration with multi-tool support (Claude Code primary, Codex optional)
```

#### 2. 功能完整性要求（Functional Completeness Requirements）

**新增章节**：

```markdown
### Multi-Tool Support (Optional Extension)

The Java implementation MAY support additional AI tools beyond Claude Code:

1. **OpenAI Codex CLI Integration** (Optional)
   - Backend execution via `CodexBackend`
   - Skill compatibility layer via `CodexSkillBridge`
   - Configuration management for `.codex/config.toml`
   - Security rules specific to Codex operations
   - Isolated from Claude Code operations

2. **Adapter Layer** (Optional)
   - `AIToolAdapter` interface for tool abstraction
   - Tool-specific adapters (CodexAdapter, CursorAdapter, etc.)
   - Unified API surface across different tools

3. **Plugin Architecture** (Future)
   - `BackendPlugin` interface for extensibility
   - Dynamic backend registration
   - Runtime backend switching

**Implementation Priority**: Claude Code support is REQUIRED. Codex support is OPTIONAL.
```

#### 3. 技术架构（Technical Architecture）

**更新模块结构**：

```markdown
#### Module Structure

```
java-harness-shared         - Shared utilities and constants
java-harness-foundation     - Data access and configuration
java-harness-protocol       - Event types and codecs
java-harness-security       - Security and validation
java-harness-workflow       - Workflow orchestration
java-harness-collaboration  - Multi-tool integration and agents
│   ├── backend/            - Backend executors (Claude, Codex, Native)
│   ├── adapter/            - AI tool adapters (Optional)
│   ├── integration/       - External tool integration (Codex, Cursor)
│   └── skill/              - Skill system and bridges
java-harness-cli           - Command-line interface
java-harness-service       - Service layer
java-harness-tools         - Development tools
```
```

#### 4. 集成点（Integration Points）

**新增章节**：

```markdown
#### OpenAI Codex Integration (Optional)

- Backend integration via `CodexBackend`
- Skill translation from Markdown to Java
- Configuration compatibility with `.codex/config.toml`
- State management for Codex operations
- Isolated from Claude Code state and operations
- Guardrail rules applicable to Codex tool uses
```

#### 5. 质量标准（Quality Standards）

**更新性能标准**：

```markdown
#### Performance

- Hook processing: < 50ms per hook (Claude Code)
- Hook processing: < 100ms per hook (Codex, due to CLI overhead)
- Workflow startup: < 100ms
- Simple workflow execution: < 1s (Claude Code)
- Simple workflow execution: < 2s (Codex, due to CLI overhead)
- Memory usage: Comparable to Go version
```

#### 6. 成功标准（Success Criteria）

**更新功能对等性**：

```markdown
#### Functional Parity

**Required (Claude Code)**:
- [ ] All 16 hooks implemented and tested
- [ ] All 86 CLI commands working
- [ ] Workflow engine feature-complete
- [ ] Agent coordination fully operational
- [ ] CI integration complete

**Optional (Codex Support)**:
- [ ] Codex backend execution working
- [ ] Codex skill translation functional
- [ ] Codex configuration management complete
- [ ] Codex-specific security rules implemented
- [ ] Integration tests passing
```

#### 7. 迁移策略（Migration Strategy）

**新增章节**：

```markdown
### Multi-Tool Support Strategy

#### Phase 1: Foundation (Current)
- Focus on Claude Code support
- Establish solid foundation
- Build plugin architecture

#### Phase 2: Extension (Optional, 6-12 months later)
- Add Codex support as optional extension
- Validate plugin architecture
- Gather user feedback

#### Phase 3: Evolution (Future)
- Consider additional tools (Grok, etc.)
- Refine adapter patterns
- Establish best practices
```

---

## 风险评估

### 高风险 🔴
1. **产品定位模糊**：可能模糊 Java Harness 的核心定位
2. **维护成本增加**：需要跟踪多个工具的 API 变化
3. **状态同步复杂**：Java 进程与 Codex 进程的状态一致性

### 中等风险 🟡
1. **安全模型差异**：Codex 与 Claude Code 的安全模型不同
2. **跨平台兼容性**：Shell 脚本在 Windows 环境需要特别处理
3. **技能系统转换**：Markdown 技能与 Java 技能的桥接

### 低风险 🟢
1. **向后兼容性**：设计合理可以保持完全向后兼容
2. **测试覆盖**：现有测试框架可以扩展到 Codex

---

## 推荐方案

### 建议：作为可选扩展（Phase 7）

**理由**：
1. **产品聚焦优先**：当前应专注完善 Claude Code 支持
2. **架构验证通过**：技术上可行，风险可控
3. **用户需求存在**：有一定用户需求，但非主流
4. **扩展性良好**：可以作为插件化架构的验证案例

**实施路径**：
- Phase 1-6：专注 Claude Code 和文档（核心任务）
- Phase 7：Codex 支持作为可选扩展（可选任务）
- 用户可根据需要选择是否实施 Phase 7

---

## 下一步行动

1. **用户确认**：用户是否同意将 Codex 支持作为可选扩展（Phase 7）？
2. **Spec 批准**：用户是否批准上述 Spec delta 更新？
3. **计划批准**：用户是否批准 Phase 7 的任务分解？
4. **事前确认**：用户是否批准 Phase 7 的事前确认事项？

---

**附件**：
- [参考实现：claude-code-harness](https://github.com/Chachamaru127/claude-code-harness)
- [技术调研报告](#技术调研)
- [架构评审详细报告](#architecture-评审)
- [产品评审详细报告](#product-评审)
