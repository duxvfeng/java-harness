---
name: harness-session
description: "HAR: Session management commands for saving, restoring, and managing Claude Code sessions. Trigger: save session, restore session, list sessions, session management, context full."
description-en: "HAR: Session management commands for saving, restoring, and managing Claude Code sessions. Trigger: save session, restore session, list sessions, session management, context full."
description-zh: "HAR：会话管理命令，用于保存、恢复和管理 Claude Code 会话。当用户提到保存会话、恢复会话、列出会话、会话管理、context 满时启动。"
kind: utility
purpose: "Manage Claude Code sessions to prevent context loss"
trigger: "save session, restore session, list sessions, show session, cleanup sessions, session management, context full, token usage, session save, session restore"
shape: command
role: utility
owner: harness-core
since: "2026-08-09"
allowed-tools: ["Read", "Write", "Bash", "Glob"]
argument-hint: "<command> [args]"
user-invocable: true
effort: medium
---

# Harness Session Management

会话管理技能 - 提供会话保存、恢复、清理等完整会话生命周期管理功能。

## 功能概述

本技能提供完整的会话管理功能，解决大型任务中的context满问题：

- **自动保存** - Token使用率达到阈值时自动触发保存
- **智能恢复** - 新会话启动时自动检测并建议恢复
- **手动管理** - 提供完整的命令接口进行手动操作
- **存储优化** - GZIP压缩、自动清理、健康检查

## 命令接口

### /harness-save-session
保存当前会话状态到本地存储。

**使用场景**：
- 执行重要任务前手动保存进度
- 长时间工作后创建检查点
- Token使用率较高时主动保存

**语法**：
```
/harness-save-session [summary] [--force]
```

**参数**：
- `summary` - 可选的会话摘要描述
- `--force` - 强制保存，忽略保存间隔限制

**示例**：
```
/harness-save-session "完成Task 11.8实现"
/harness-save-session --force
```

### /harness-restore-session
从保存的会话中恢复工作状态。

**使用场景**：
- 新会话启动时恢复上次工作
- Context满后重新加载历史上下文
- 切换到不同的工作分支

**语法**：
```
/harness-restore-session <saveId> [--full] [--summary-only]
```

**参数**：
- `saveId` - 会话保存ID（必需）
- `--full` - 完整恢复，包括所有上下文
- `--summary-only` - 仅显示会话摘要，不执行恢复

**示例**：
```
/harness-restore-session 20260809-174530-abc123
/harness-restore-session 20260809-174530-abc123 --full
/harness-restore-session 20260809-174530-abc123 --summary-only
```

### /harness-list-sessions
列出所有保存的会话。

**使用场景**：
- 查看可恢复的会话列表
- 选择合适的会话进行恢复
- 管理存储空间

**语法**：
```
/harness-list-sessions [--recent N] [--all]
```

**参数**：
- `--recent N` - 显示最近N个会话（默认5个）
- `--all` - 显示所有会话

**示例**：
```
/harness-list-sessions
/harness-list-sessions --recent 10
/harness-list-sessions --all
```

### /harness-show-session
显示特定会话的详细信息。

**使用场景**：
- 恢复前查看会话详情
- 了解会话包含的任务进度
- 确认恢复的会话内容

**语法**：
```
/harness-show-session <saveId>
```

**参数**：
- `saveId` - 会话保存ID（必需）

**示例**：
```
/harness-show-session 20260809-174530-abc123
```

### /harness-cleanup-sessions
清理旧的会话保存文件。

**使用场景**：
- 释放存储空间
- 删除不需要的历史会话
- 定期维护会话存储

**语法**：
```
/harness-cleanup-sessions [--older-than HOURS] [--keep N] [--dry-run]
```

**参数**：
- `--older-than HOURS` - 删除超过指定小时数的会话（默认168小时=7天）
- `--keep N` - 保留最近N个会话（默认10个）
- `--dry-run` - 仅显示将要删除的会话，不实际删除

**示例**：
```
/harness-cleanup-sessions
/harness-cleanup-sessions --older-than 24
/harness-cleanup-sessions --keep 20
/harness-cleanup-sessions --dry-run
```

## 自动化功能

### 自动保存触发条件

会话自动保存在以下情况下触发：

1. **Token阈值** - Token使用率达到80%或90%
2. **定时保存** - 距离上次保存超过配置的间隔时间
3. **重要里程碑** - 完成重要任务或阶段

### 智能恢复建议

新会话启动时，系统会：

1. 检测最近的会话保存
2. 分析会话内容和完成度
3. 生成AI决策和恢复建议
4. 显示格式化的恢复提示

### 存储管理策略

自动存储管理包括：

1. **压缩存储** - 使用GZIP压缩会话数据
2. **空间检查** - 保存前检查可用空间
3. **自动清理** - 定期清理过期的会话
4. **健康检查** - 验证会话文件完整性

## 配置选项

在 `harness.toml` 中配置会话管理行为：

```toml
[session]
# 自动保存配置
autoSave = true
tokenThreshold80 = true
tokenThreshold90 = true
saveIntervalMinutes = 30

# 恢复提示配置
restorePrompt = true
autoShowPrompt = true

# 存储配置
maxStorageMB = 100
compressionEnabled = true
maxHistoryAgeDays = 7

# 清理配置
autoCleanup = true
keepRecentSessions = 10
cleanupIntervalHours = 24
```

## 使用场景

### 场景1：长时间开发任务

```bash
# 开始重要任务前保存
/harness-save-session "开始Phase 11实施"

# 工作过程中自动保存触发
# (当Token使用率>80%时自动保存)

# 恢复工作时查看可用的会话
/harness-list-sessions --recent 3

# 选择合适的会话恢复
/harness-restore-session 20260809-174530-abc123 --full
```

### 场景2：Context满后恢复

```bash
# 当context满时，启动新会话

# 系统自动显示恢复提示
# (使用session-init hook)

# 或者手动查看可恢复的会话
/harness-list-sessions --recent 5

# 恢复到合适的工作点
/harness-restore-session 20260809-150000-def456
```

### 场景3：定期存储维护

```bash
# 查看存储使用情况
/harness-list-sessions --all

# 预览将要清理的会话
/harness-cleanup-sessions --dry-run

# 执行清理（保留最近20个会话）
/harness-cleanup-sessions --keep 20

# 清理超过3天的会话
/harness-cleanup-sessions --older-than 72
```

## 故障排除

### 保存失败

**问题**：执行 `/harness-save-session` 时保存失败

**解决方案**：
1. 检查磁盘空间是否充足
2. 验证 `.claude/state/session-saves/` 目录权限
3. 查看日志文件了解详细错误信息

### 恢复失败

**问题**：执行 `/harness-restore-session` 时恢复失败

**解决方案**：
1. 使用 `/harness-show-session <saveId>` 验证会话完整性
2. 确认保存ID正确无误
3. 检查会话文件是否损坏

### 自动保存未触发

**问题**：Token使用率高时自动保存未触发

**解决方案**：
1. 检查 `harness.toml` 中是否启用了自动保存
2. 验证环境变量 `CLAUDE_TOKEN_COUNT` 是否可用
3. 确认session-monitor hook正确安装

## 技术细节

### 存储位置

会话保存在项目目录中：
```
.claude/state/session-saves/<timestamp>-<id>/
├── metadata.json          # 会话元数据
├── session-data.json.gz   # 压缩的会话数据
└── git-state.json         # Git状态信息
```

### 性能指标

- **保存时间**：典型会话 < 3秒
- **恢复时间**：典型会话 < 5秒
- **压缩率**：文本内容 > 70%
- **存储占用**：压缩后 < 10MB/会话
- **可靠性**：保存成功率 > 99%，恢复成功率 > 98%

### Hook集成

- **session-monitor** - Token监控和自动保存
- **session-init** - 恢复提示生成和显示

## 相关文档

- [会话管理系统设计文档](../../docs/user-guide/session-management.md)
- [会话保存和恢复系统规格说明](../../docs/superpowers/specs/2026-08-09-session-save-and-restore-design.md)
- [SessionRestoreManager API文档](../../java-harness-workflow/src/main/java/com/chachamaru/harness/session/restore/SessionRestoreManager.java)

## 参考命令

相关技能命令：
- `/harness-plan` - 任务规划（保存前规划任务）
- `/harness-work` - 任务执行（保存点管理）
- `/harness-sync` - 进度同步（与会话保存配合）