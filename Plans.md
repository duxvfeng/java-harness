# Phase 8: Go 版本功能完全补全计划

**目标**: 实现与 Go 版本 100% 功能对等的 Java Harness

## 功能完备性差距分析

### 当前状态对比

| **功能类别** | **Go 版本** | **Java 版本** | **完成度** |
|-------------|------------|--------------|----------|
| **Hook 系统** | 13+ hooks | 完整实现 14 hooks | 🟢 100% |
| **CLI 命令** | 60+ 命令 | 基础 CLI | 🔴 10% |
| **工作流引擎** | 生产级 | 基础实现 | 🟡 60% |
| **Agent 协调** | Breezing/Cursor/Codex | 部分实现 | 🟡 40% |
| **CI 集成** | 完整集成 | 基础支持 | 🔴 20% |
| **内存管理** | 完整系统 | 基础实现 | 🔴 30% |
| **状态管理** | 持久化+同步 | 基础实现 | 🔴 25% |
| **Guardrail 系统** | 20+ 规则 | 8 条规则 | 🔴 40% |

**总体完成度**: 🟡 **约 35%** (Hook 系统 100% 完成)

---

## Phase 8.1: Hook 系统完全实现（4 周）

### 目标
实现 Go 版本的所有 13+ hooks，支持完整的生命周期管理

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.1.1 | 实现 PreToolUse hook | pre-tool 调用前验证，支持 guardrail 拦截 | Hook 测试通过，能正确拦截危险操作 | - | cc:completed ✅ |
| 8.1.2 | 实现 PostToolUse hook | post-tool 调用后验证，检测篡改 | Hook 测试通过，能检测文件篡改 | 8.1.1 | cc:completed ✅ |
| 8.1.3 | 实现 PermissionRequest hook | permission 请求自动批准逻辑 | Hook 测试通过，按策略自动批准 | 8.1.2 | cc:completed ✅ |
| 8.1.4 | 实现 SessionStart hook | session-start 环境设置 + Plans.md 摘要 | Hook 测试通过，正确初始化会话 | 8.1.3 | cc:completed ✅ |
| 8.1.5 | 实现 PostToolFailure hook | post-tool-failure 计数器 + 升级 | Hook 测试通过，3次失败后自动升级 | 8.1.4 | cc:completed ✅ |
| 8.1.6 | 实现 PostCompact hook | post-compact WIP 上下文重新注入 | Hook 测试通过，正确恢复上下文 | 8.1.5 | cc:completed ✅ |
| 8.1.7 | 实现 Notification hook | notification 事件日志 | Hook 测试通过，正确记录事件 | 8.1.6 | cc:completed ✅ |
| 8.1.8 | 实现 PermissionDenied hook | permission-denied 事件日志 | Hook 测试通过，正确记录拒绝事件 | 8.1.7 | cc:completed ✅ |
| 8.1.9 | 实现 SessionCleanup hook | session-cleanup 临时文件清理 | Hook 测试通过，正确清理临时文件 | 8.1.8 | cc:completed ✅ |
| 8.1.10 | 实现 SessionMonitor hook | session-monitor 项目状态收集 | Hook 测试通过，正确生成 session.json | 8.1.9 | cc:completed ✅ |
| 8.1.11 | 实现 SessionSummary hook | session-summary 会话摘要到 session-log.md | Hook 测试通过，正确生成摘要 | 8.1.10 | cc:completed ✅ |
| 8.1.12 | 实现 CIStatus hook | ci-status push/PR 后 CI 状态检查 | Hook 测试通过，正确检查 CI 状态 | 8.1.11 | cc:completed ✅ |
| 8.1.13 | 实现 SubagentStart hook | subagent-start 代理启动追踪 | Hook 测试通过，正确追踪代理启动 | 8.1.12 | cc:completed ✅ |
| 8.1.14 | 实现 SubagentStop hook | subagent-stop 代理停止追踪 | Hook 测试通过，正确追踪代理停止 | 8.1.13 | cc:completed ✅ |

---

## Phase 8.2: CLI 命令完全实现（6 周）

### 目标
实现 Go 版本的所有 60+ 命令

#### 核心命令（2 周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.2.1 | 实现 `gen` 命令 | 生成 Plans.md，支持 drift 检测 | 命令测试通过，能生成计划 | 8.1.14 | cc:completed ✅ 99c8b8c |
| 8.2.2 | 实现 `plan` 命令 | 计划管理（add/list/switch） | 命令测试通过，能管理多个计划 | 8.2.1 | cc:completed ✅ 99c8b8c |
| 8.2.3 | 实现 `plans` 命令 | Plans.md watcher 和管理 | 命令测试通过，能监控计划变化 | 8.2.2 | cc:completed ✅ 99c8b8c |
| 8.2.4 | 实现 `work` 命令 | 工作流执行（solo/parallel/breezing） | 命令测试通过，能执行工作流 | 8.2.3 | cc:completed ✅ 99c8b8c |
| 8.2.5 | 实现 `review` 命令 | 代码审查和验证 | 命令测试通过，能执行审查 | 8.2.4 | cc:completed ✅ 99c8b8c |
| 8.2.6 | 实现 `sync` 命令 | 实现与 Plans.md 同步 | 命令测试通过，能同步状态 | 8.2.5 | cc:completed ✅ 99c8b8c |
| 8.2.7 | 实现 `validate` 命令 | 项目结构和规范验证 | 命令测试通过，能验证项目 | 8.2.6 | cc:completed ✅ 99c8b8c |

#### 项目管理命令（1 周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.2.8 | 实现 `init` 命令 | 项目初始化 | 命令测试通过，能初始化项目 | 8.2.7 | cc:TODO |
| 8.2.9 | 实现 `doctor` 命令 | 诊断和修复 | 命令测试通过，能诊断问题 | 8.2.8 | cc:TODO |
| 8.2.10 | 实现 `status` 命令 | 显示所有追踪代理状态 | 命令测试通过，能显示状态 | 8.2.9 | cc:TODO |

#### CI 和证据命令（1 周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.2.11 | 实现 `evidence collect` 命令 | 收集测试结果、构建日志 | 命令测试通过，能收集证据 | 8.2.10 | cc:completed ✅ |
| 8.2.12 | 实现 `ci-check` 命令 | CI 状态检查 | 命令测试通过，能检查 CI | 8.2.11 | cc:completed ✅ |
| 8.2.13 | 实现 `ci-status` 命令 | CI 状态查询 | 命令测试通过，能查询 CI | 8.2.12 | cc:completed ✅ |
| 8.2.14 | 实现 `sprint-contract` 命令 | 生成 sprint-contract | 命令测试通过，能生成契约 | 8.2.13 | cc:completed ✅ |

#### Agent 协调命令（1 周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.2.15 | 实现 `codex-loop` 命令 | Codex 循环执行 | 命令测试通过，能执行循环 | 8.2.14 | cc:completed ✅ |
| 8.2.16 | 实现 `breezing-signal` 命令 | Breezing 模式信号处理 | 命令测试通过，能处理信号 | 8.2.15 | cc:completed ✅ |
| 8.2.17 | 实现 `subagent-start` 命令 | 子代理启动 | 命令测试通过，能启动代理 | 8.2.16 | cc:completed ✅ |
| 8.2.18 | 实现 `subagent-stop` 命令 | 子代理停止 | 命令测试通过，能停止代理 | 8.2.17 | cc:completed ✅ |

#### 高级命令（1 周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.2.19 | 实现 `mem` 命令 | 内存操作 | 命令测试通过，能管理内存 | 8.2.18 | cc:completed ✅ |
| 8.2.20 | 实现 `channels-wake` 命令 | 通道唤醒 | 命令测试通过，能唤醒通道 | 8.2.19 | cc:completed ✅ |
| 8.2.21 | 实现 `inbox-check` 命令 | 收件箱检查 | 命令测试通过，能检查收件箱 | 8.2.20 | cc:completed ✅ |
| 8.2.22 | 实现 `failure-codifier` 命令 | 失败编码器 | 命令测试通过，能编码失败 | 8.2.21 | cc:completed ✅ |

---

## Phase 8.3: Guardrail 系统扩展（2 周）

### 目标
从现有的 8 条规则扩展到 Go 版本的 20+ 规则

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.3.1 | 实现基础规则扩展 | 添加 12 条新规则 | 测试通过，规则生效 | 8.2.22 | cc:completed ✅ |
| 8.3.2 | 实现自定义规则支持 | 允许用户添加自定义规则 | 测试通过，能添加规则 | 8.3.1 | cc:completed ✅ |
| 8.3.3 | 实现规则优先级系统 | 规则冲突时的优先级处理 | 测试通过，优先级正确 | 8.3.2 | cc:completed ✅ |
| 8.3.4 | 实现规则性能优化 | 规则执行性能优化 | 性能测试通过 | 8.3.3 | cc:completed ✅ |

---

## Phase 8.4: 状态管理和持久化（3 周）

### 目标
实现完整的状态管理和持久化系统

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.4.1 | 实现状态持久化引擎 | 支持 JSON/YAML 持久化 | 测试通过，状态能正确保存和恢复 | 8.3.4 | cc:completed ✅ |
| 8.4.2 | 实现状态同步机制 | 与 Plans.md 双向同步 | 测试通过，状态能正确同步 | 8.4.1 | cc:completed ✅ |
| 8.4.3 | 实现状态历史记录 | 状态变更历史追踪 | 测试通过，历史能正确记录 | 8.4.2 | cc:completed ✅ |
| 8.4.4 | 实现状态恢复机制 | 崩溃后状态恢复 | 测试通过，能正确恢复状态 | 8.4.3 | cc:completed ✅ |
| 8.4.5 | 实现并发状态控制 | 多会话状态并发控制 | 测试通过，并发安全 | 8.4.4 | cc:completed ✅ |

---

## Phase 8.5: Agent 协调系统完善（4 周）

### 目标
实现完整的 Breezing/Cursor/Codex backend 支持

| Task | 内容 | DoD | Depends | Status |
|------|------|------|---------|--------|
| 8.5.1 | 完善 Breezing 模式 | Lead/Worker/Reviewer 完整流程 | 8.4.5 | cc:completed ✅ |
| 8.5.2 | 实现 Cursor backend 支持 | Cursor agent 集成 | 8.5.1 | cc:completed ✅ |
| 8.5.3 | 实现 Codex backend 支持 | Codex CLI 集成 | 8.5.2 | cc:completed ✅ |
| 8.5.4 | 实现并行工作流优化 | 多任务并行执行优化 | 8.5.3 | cc:completed ✅ |
| 8.5.5 | 实现 Advisor 协议 | 实现顾问协议 | 8.5.4 | cc:completed ✅ |
| 8.5.6 | 实现 Reviewer 协议 | 实现审查员协议 | 8.5.5 | cc:completed ✅ |

---

## Phase 8.6: CI/CD 集成（3 周）

### 目标
实现完整的 CI/CD 系统集成

| Task | 内容 | DoD | Depends | Status | State |
|------|------|------|---------|--------|--------|
| 8.6.1 | 实现 GitHub Actions 集成 | 支持 GitHub Actions | 测试通过，集成正常 | 8.5.6 | cc:completed ✅ |
| 8.6.2 | 实现 GitLab CI 集成 | 支持 GitLab CI | 测试通过，集成正常 | 8.6.1 | cc:completed ✅ |
| 8.6.3 | 实现 CI 状态监控 | 实时监控 CI 状态 | 测试通过，监控正常 | 8.6.2 | cc:completed ✅ |
| 8.6.4 | 实现失败自动修复 | CI 失败自动重试和修复 | 测试通过，自动修复正常 | 8.6.3 | cc:completed ✅ |

---

## Phase 8.7: 测试和文档（4 周）

### 目标
完整的测试覆盖和文档体系

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.7.1 | 实现端到端测试 | 所有功能端到端测试 | 测试覆盖率 > 85% | 8.6.4 | cc:completed ✅ |
| 8.7.2 | 实现性能基准测试 | 与 Go 版本性能对比 | 性能差异 < 20% | 8.7.1 | cc:completed ✅ |
| 8.7.3 | 编写 API 文档 | 完整的 API 文档 | 文档完整准确 | 8.7.2 | cc:completed ✅ |
| 8.7.4 | 编写迁移指南 | 从 Go 版本迁移指南 | 迁移指南清晰完整 | 8.7.3 | cc:completed ✅ |
| 8.7.5 | 编写故障排查指南 | 常见问题解决方案 | 故障指南完整 | 8.7.4 | cc:completed ✅ |

---

## Phase 8 验收标准

### 功能完整性
- [ ] 所有 13+ hooks 实现并测试通过
- [ ] 所有 60+ CLI 命令工作正常
- [ ] Guardrail 系统规则完整
- [ ] 状态管理和持久化完整
- [ ] Agent 协调系统完整
- [ ] CI/CD 集成完整

### 质量标准
- [ ] 测试覆盖率 > 85%
- [ ] 性能与 Go 版本差异 < 20%
- [ ] 所有集成测试通过
- [ ] 文档完整准确

### 用户体验
- [ ] CLI 命令清晰易用
- [ ] 错误信息明确有用
- [ ] 迁移路径清晰
- [ ] 学习曲线平缓

---

## 预估时间

| **阶段** | **时间** | **团队规模** |
|---------|---------|------------|
| Phase 8.1: Hook 系统 | 4 周 | 2-3 人 |
| Phase 8.2: CLI 命令 | 6 周 | 3-4 人 |
| Phase 8.3: Guardrail 系统 | 2 周 | 2 人 |
| Phase 8.4: 状态管理 | 3 周 | 2-3 人 |
| Phase 8.5: Agent 协调 | 4 周 | 3-4 人 |
| Phase 8.6: CI/CD 集成 | 3 周 | 2-3 人 |
| Phase 8.7: 测试和文档 | 4 周 | 2-3 人 |

**总计**: **26 周**（约 6 个月）

---

## 风险评估

### 技术风险

**风险1**: Go 版本特有功能难以在 Java 中实现
- **描述**: 某些 Go 特性在 Java 中无直接对应
- **缓解**: 架构调整，寻找 Java 生态对应方案

**风险2**: 性能差距难以缩小
- **描述**: Java 性能可能不如 Go
- **缓解**: 性能优化，异步处理

### 进度风险

**风险1**: 工作量可能被低估
- **描述**: 功能差距比预期大
- **缓解**: 分阶段交付，优先核心功能

**风险2**: 人员技能可能不足
- **描述**: 团队对 Go 版本理解不够
- **缓解**: 培训和文档学习

---

## 成功标准

### 功能对等性
- ✅ 所有 Go 版本功能都有 Java 实现
- ✅ API 和命令行接口一致
- ✅ 配置文件格式兼容
- ✅ 行为和输出一致

### 质量标准
- ✅ 测试覆盖率 > 85%
- ✅ 性能差距 < 20%
- ✅ 零已知严重 bug
- ✅ 文档完整准确

### 用户体验
- ✅ 学习曲线平缓
- ✅ 迁移成本低
- ✅ 社区接受度高

---

## Phase 8.8: 模板系统移植（3-4 周）

### 目标
实现 Go 版本完整的模板系统，包括模板注册、变量替换、版本追踪和国际化支持

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.8.1 | 实现模板注册表系统 | template-registry.json 管理、版本映射 | 能正确注册和查询模板 | 8.7.5 | cc:TODO |
| 8.8.2 | 实现模板变量替换引擎 | {{PROJECT_NAME}}, {{DATE}} 等变量替换 | 变量替换功能完整 | 8.8.1 | cc:TODO |
| 8.8.3 | 实现 Frontmatter 版本管理 | _harness_version, _harness_template 解析 | 版本追踪功能正常 | 8.8.2 | cc:TODO |
| 8.8.4 | 移植核心项目模板 | CLAUDE.md, AGENTS.md, Plans.md 模板 | 模板生成功能正常 | 8.8.3 | cc:TODO |
| 8.8.5 | 移植规则模板系统 | 12个规则文件的模板生成 | 规则模板能正确生成 | 8.8.4 | cc:TODO |
| 8.8.6 | 移植内存模板 | decisions.md, patterns.md 模板 | 内存模板功能正常 | 8.8.5 | cc:TODO |
| 8.8.7 | 实现模板追踪器 | template-tracker.sh 的 Java 实现 | 追踪功能完整 | 8.8.6 | cc:TODO |
| 8.8.8 | 实现条件模板生成 | 基于 config 的条件模板逻辑 | 条件模板正确生成 | 8.8.7 | cc:TODO |
| 8.8.9 | 实现模板更新检测 | 检测模板版本变化和本地修改 | 更新检测准确 | 8.8.8 | cc:TODO |

---

## Phase 8.9: 脚本功能移植（6-8 周）

### 目标
移植 Go 版本 201 个脚本的核心功能到 java-harness

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.9.1 | 移植构建脚本系统 | build.sh, test.sh 等构建相关脚本 | 构建功能完整 | 8.8.9 | cc:TODO |
| 8.9.2 | 移植项目初始化脚本 | setup-existing-project.sh, analyze-project.sh | 初始化功能正常 | 8.9.1 | cc:TODO |
| 8.9.3 | 移植计划管理脚本 | plan-registry.sh, plans-watcher.sh 等 | 计划管理功能完整 | 8.9.2 | cc:TODO |
| 8.9.4 | 移植会话管理脚本 | session-init.sh, session-monitor.sh 等 | 会话管理功能正常 | 8.9.3 | cc:TODO |
| 8.9.5 | 移植审查和质量脚本 | judgment-card.sh, review-相关脚本 | 审查功能完整 | 8.9.4 | cc:TODO |
| 8.9.6 | 移植 CI/CD 脚本 | release-preflight.sh, ci-相关脚本 | CI/CD 功能正常 | 8.9.5 | cc:TODO |
| 8.9.7 | 移植测试脚本 | auto-test-runner.sh, test-相关脚本 | 测试脚本功能完整 | 8.9.6 | cc:TODO |
| 8.9.8 | 移植工具和辅助脚本 | 渲染、生成、分析工具脚本 | 辅助工具功能正常 | 8.9.7 | cc:TODO |
| 8.9.9 | 创建 Java 原生等价实现 | 将核心脚本逻辑转为 Java 类 | Java 实现功能完整 | 8.9.8 | cc:TODO |
| 8.9.10 | 实现脚本调用桥接 | Java 系统调用 shell 脚本的桥接层 | 桥接功能正常 | 8.9.9 | cc:TODO |

---

## Phase 8.10: 国际化和文档系统（2-3 周）

### 目标
实现完整的多语言模板支持和文档生成系统

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 8.10.1 | 实现日语模板支持 | locales/ja/ 目录下的所有模板 | 日语模板功能正常 | 8.9.10 | cc:TODO |
| 8.10.2 | 实现中文模板支持 | locales/zh/ 目录下的所有模板 | 中文模板功能正常 | 8.10.1 | cc:TODO |
| 8.10.3 | 实现多语言切换逻辑 | 基于配置的语言切换功能 | 语言切换正确 | 8.10.2 | cc:TODO |
| 8.10.4 | 移植文档生成脚本 | generate-相关脚本的移植 | 文档生成功能正常 | 8.10.3 | cc:TODO |
| 8.10.5 | 实现 HTML 渲染系统 | render-html.sh 的 Java 实现 | HTML 渲染功能完整 | 8.10.4 | cc:TODO |
| 8.10.6 | 创建多语言文档系统 | 支持多语言的项目文档生成 | 文档系统功能正常 | 8.10.5 | cc:TODO |

---

## 更新后的预估时间

| **阶段** | **时间** | **团队规模** |
|---------|---------|------------|
| Phase 8.1: Hook 系统 | 4 周 | 2-3 人 |
| Phase 8.2: CLI 命令 | 6 周 | 3-4 人 |
| Phase 8.3: Guardrail 系统 | 2 周 | 2 人 |
| Phase 8.4: 状态管理 | 3 周 | 2-3 人 |
| Phase 8.5: Agent 协调 | 4 周 | 3-4 人 |
| Phase 8.6: CI/CD 集成 | 3 周 | 2-3 人 |
| Phase 8.7: 测试和文档 | 4 周 | 2-3 人 |
| Phase 8.8: 模板系统 | 3-4 周 | 2-3 人 |
| Phase 8.9: 脚本功能移植 | 6-8 周 | 3-4 人 |
| Phase 8.10: 国际化系统 | 2-3 周 | 2 人 |

**总计**: **37-41 周**（约 9-10 个月）

---

## 扩展风险评估

### 模板系统风险

**风险3**: 模板语法解析复杂性
- **描述**: 变量替换和 Frontmatter 解析可能复杂
- **缓解**: 使用成熟的模板引擎（如 Freemarker, Thymeleaf）

### 脚本移植风险

**风险4**: 脚本功能过于分散
- **描述**: 201个脚本功能分散，难以统一管理
- **缓解**: 创建统一的脚本调用框架和配置系统

**风险5**: Shell 和 Java 混合调用
- **描述**: 跨语言调用可能带来兼容性问题
- **缓解**: 设计清晰的桥接层和错误处理机制

---

这个 Phase 8 扩展计划将确保 Java Harness 实现与 Go 版本 **100% 功能对等**，包括完整的模板系统、脚本功能和国际化支持，真正实现"功能完全完备"的目标。

---

## Phase 9: 跨平台 Hooks Wrapper 统一方案（1 周）

### 目标
统一跨平台 hooks 配置，使用 wrapper 脚本自动检测平台并调用正确的二进制，消除维护多份平台特定模板文件的开销

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 9.1 | 创建备份目录并移动旧配置 | 将 4 个多平台 hooks 配置移到 docs/reference/multi-platform-hooks-backup/ | 备份完成，4 个文件已移动 | - | cc:completed ✅ 0cdc64c |
| 9.2 | 创建 Unix Wrapper 脚本 | bin/harness 平台检测脚本，支持 Linux/macOS/Windows | shell 语法验证通过，bin/harness --version 输出版本号 | 9.1 | cc:completed ✅ 66b0cb5 |
| 9.3 | 创建 Windows Wrapper 脚本 | bin/harness.bat 平台检测脚本，支持 Windows 和 JAR fallback | batch 脚本创建，bin/harness.bat --version 工作正常 | 9.2 | cc:completed ✅ f55f9b1 |
| 9.4 | 更新 hooks.json 为统一配置 | 批量替换所有 command 字段为 bin/harness | JSON 格式验证通过，所有 command 统一为 bin/harness | 9.3 | cc:completed ✅ 2ebbbd9 |
| 9.5 | 提交 Wrapper 脚本和统一配置 | git commit bin/harness, bin/harness.bat, hooks/hooks.json | 提交完成，commit message 符合规范 | 9.4 | cc:completed ✅ 66b0cb5,f55f9b1,2ebbbd9 |
| 9.6 | 更新 PLATFORM_SETUP.md 文档 | 移除多平台复制步骤，添加 wrapper 验证步骤 | 文档更新正确，验证步骤清晰 | 9.5 | cc:completed ✅ 1dfeb71 |
| 9.7 | 集成测试验证 Wrapper 功能 | 测试版本命令、Hook 子命令、Fallback 机制 | 所有测试通过，wrapper 功能正常 | 9.6 | cc:completed ✅ d5f0065 |
| 9.8 | 跨平台验证 | 在 Git Bash (MINGW) 环境测试平台检测 | 平台检测正确，错误处理完整 | 9.7 | cc:TODO |
| 9.9 | 文档和收尾 | 检查其他文档引用，生成实现总结 | 文档完整，实现总结已创建 | 9.8 | cc:TODO |

### 详细说明

**实现范围**：
- 创建 `bin/harness`（Unix wrapper）和 `bin/harness.bat`（Windows wrapper）
- 统一 `hooks/hooks.json` 配置，所有平台使用 `bin/harness` 命令
- 备份旧的多平台配置到参考目录
- 更新文档移除手动配置步骤

**支持平台**：
- Windows x86_64 → `bin/windows/harness.exe`
- Linux AMD64 → `bin/linux/linux-amd64/harness`
- Linux ARM64 → `bin/linux/linux-arm64/harness`
- macOS Intel → `bin/macos/macos-amd64/harness`
- macOS ARM64 → `bin/macos/macos-arm64/harness`
- 全平台 JAR fallback

**验收标准**：
- ✅ `bin/harness` 和 `bin/harness.bat` 创建并通过语法检查
- ✅ `bin/harness --version` 输出 `harness 4.1.1`
- ✅ `hooks/hooks.json` 所有 command 统一为 `bin/harness`
- ✅ 旧配置已备份到 `docs/reference/multi-platform-hooks-backup/`
- ✅ `PLATFORM_SETUP.md` 已更新
- ✅ Fallback 机制验证通过
- ✅ 所有变更已 commit

### 相关文档
- 设计文档: `docs/superpowers/specs/2026-08-07-cross-platform-hooks-wrapper-design.md`
- 实现计划: `docs/superpowers/plans/2026-08-07-cross-platform-hooks-wrapper.md`