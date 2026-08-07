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
| 9.8 | 跨平台验证 | 在 Git Bash (MINGW) 环境测试平台检测 | 平台检测正确，错误处理完整 | 9.7 | cc:completed ✅ 829eb10 |
| 9.9 | 文档和收尾 | 检查其他文档引用，生成实现总结 | 文档完整，实现总结已创建 | 9.8 | cc:completed ✅ 18dcd02 |
| 9.10 | 诊断 Hook 调用失败问题 | 调试 bin/harness 在 Hook 系统中的 "No such file or directory" 错误 | 找到根本原因并确认修复方案 | 9.9 | cc:completed ✅ 91bcb23 |
| 9.11 | 修复 Hook 路径解析问题 | 确保 hooks.json 中的 command 在所有环境下都能正确解析 bin/harness | Hooks 不再报 "bin/harness: No such file or directory" 错误 | 9.10 | cc:completed ✅ 91bcb23 |

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

---

## 📦 归档索引

历史归档已移至 `docs/guidang/` 目录：

| 归档文件 | 归档日期 | 内容 |
|---------|---------|------|
| [Plans-2026-08.md](docs/guidang/Plans-2026-08.md) | 2026-08-07 | Phase 8.1-8.7 (59个已完成任务) |

**归档说明**: 已完成的 Phase 会定期归档到独立文件，保持主文件简洁。归档文件包含完整的任务列表、验收标准和技术文档。

