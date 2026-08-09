# Harness Session Commands Reference

会话管理命令的详细参考文档。

## 命令概览

| 命令 | 功能 | 类别 |
|------|------|------|
| `/harness-save-session` | 保存当前会话状态 | 保存操作 |
| `/harness-restore-session` | 恢复已保存的会话 | 恢复操作 |
| `/harness-list-sessions` | 列出所有保存的会话 | 查询操作 |
| `/harness-show-session` | 显示会话详细信息 | 查询操作 |
| `/harness-cleanup-sessions` | 清理旧的会话保存 | 维护操作 |

## 详细命令说明

### /harness-save-session

保存当前会话状态到本地存储。

#### 用法
```bash
/harness-save-session [summary] [--force]
```

#### 参数
- `summary` (可选): 会话摘要描述，用于识别会话内容
- `--force` (可选): 强制保存，忽略保存间隔限制

#### 示例
```bash
# 基本保存
/harness-save-session

# 带摘要的保存
/harness-save-session "完成Task 11.8的session-init Hook实现"

# 强制保存
/harness-save-session --force
```

#### 输出格式
```
✅ 会话保存成功
保存ID: 20260809-174530-abc123
时间: 2026-08-09 17:45:30
摘要: 完成Task 11.8的session-init Hook实现
Token使用率: 85%
存储路径: .claude/state/session-saves/20260809-174530-abc123/
```

#### 注意事项
- 保存操作会记录当前的对话历史、任务状态、Git状态
- 默认有保存间隔限制（默认30分钟），使用 `--force` 可绕过
- 大型会话可能需要较长时间（通常<3秒）

### /harness-restore-session

从保存的会话中恢复工作状态。

#### 用法
```bash
/harness-restore-session <saveId> [--full] [--summary-only]
```

#### 参数
- `saveId` (必需): 会话保存ID
- `--full` (可选): 完整恢复，包括所有上下文
- `--summary-only` (可选): 仅显示会话摘要，不执行恢复

#### 示例
```bash
# 基本恢复
/harness-restore-session 20260809-174530-abc123

# 完整恢复
/harness-restore-session 20260809-174530-abc123 --full

# 仅查看摘要
/harness-restore-session 20260809-174530-abc123 --summary-only
```

#### 输出格式
```
📋 会话恢复: 20260809-174530-abc123
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

会话概述: Token使用率: 85% - 完成 Task 11.8 的 session-init Hook 实现
当前工作: 正在执行 Task 11.8/13
最近进度:
  ✅ 已完成 8 个任务
  🔄 当前任务: Task 11.8

AI决策: 建议恢复完整上下文以继续复杂任务
置信度: 85%

恢复内容:
- 对话历史: 150轮对话
- 任务状态: Phase 11, Task 11.8 进行中
- Git状态: master分支，3个未提交修改
- 文件状态: 12个文件被修改

恢复时间: 2.3秒
状态: ✅ 恢复成功
```

#### 恢复模式
- **标准恢复**: 恢复基本的任务状态和工作上下文
- **完整恢复** (`--full`): 恢复所有对话历史和详细上下文
- **摘要查看** (`--summary-only`): 不实际恢复，仅显示会话信息

### /harness-list-sessions

列出所有保存的会话。

#### 用法
```bash
/harness-list-sessions [--recent N] [--all]
```

#### 参数
- `--recent N` (可选): 显示最近N个会话，默认5个
- `--all` (可选): 显示所有会话

#### 示例
```bash
# 显示最近5个会话
/harness-list-sessions

# 显示最近10个会话
/harness-list-sessions --recent 10

# 显示所有会话
/harness-list-sessions --all
```

#### 输出格式
```
💾 已保存的会话 (最近5个)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. 20260809-174530-abc123
   时间: 2026-08-09 17:45:30 (2小时前)
   摘要: 完成 Task 11.8 的 session-init Hook 实现
   Token使用率: 85%
   任务进度: Phase 11, Task 11.8/13

2. 20260809-150000-def456
   时间: 2026-08-09 15:00:00 (5小时前)
   摘要: Task 11.7 完成后的保存点
   Token使用率: 72%
   任务进度: Phase 11, Task 11.7/13

...
```

#### 显示字段
- **保存ID**: 唯一标识符，用于恢复操作
- **时间**: 保存时间，显示相对时间（如"2小时前"）
- **摘要**: 用户提供的会话描述
- **Token使用率**: 保存时的Token使用情况
- **任务进度**: 当前任务和完成度

### /harness-show-session

显示特定会话的详细信息。

#### 用法
```bash
/harness-show-session <saveId>
```

#### 参数
- `saveId` (必需): 会话保存ID

#### 示例
```bash
/harness-show-session 20260809-174530-abc123
```

#### 输出格式
```
📄 会话详细信息: 20260809-174530-abc123
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

基本信息:
保存ID: 20260809-174530-abc123
保存时间: 2026-08-09 17:45:30
会话摘要: 完成 Task 11.8 的 session-init Hook 实现
Token使用率: 85%

任务状态:
当前阶段: Phase 11
当前任务: Task 11.8
总任务数: 13
已完成任务: ["11.1", "11.2", "11.3", "11.4", "11.5", "11.6", "11.7", "11.8"]

Git状态:
分支: master
最新提交: 49c854e feat(phase-11): implement session save and restore system core components
未提交修改: 是
修改文件: ["SessionInitHandler.java", "SessionInitHandlerTest.java"]

存储信息:
存储路径: .claude/state/session-saves/20260809-174530-abc123/
数据大小: 2.3MB (压缩后)
完整性: ✅ 验证通过
```

#### 用途
- 恢复前确认会话内容
- 了解会话的任务进度
- 验证会话文件完整性

### /harness-cleanup-sessions

清理旧的会话保存文件。

#### 用法
```bash
/harness-cleanup-sessions [--older-than HOURS] [--keep N] [--dry-run]
```

#### 参数
- `--older-than HOURS` (可选): 删除超过指定小时数的会话，默认168小时（7天）
- `--keep N` (可选): 保留最近N个会话，默认10个
- `--dry-run` (可选): 仅显示将要删除的会话，不实际删除

#### 示例
```bash
# 基本清理（删除超过7天的会话，保留最近10个）
/harness-cleanup-sessions

# 删除超过1天的会话
/harness-cleanup-sessions --older-than 24

# 保留最近20个会话
/harness-cleanup-sessions --keep 20

# 预览将要删除的会话
/harness-cleanup-sessions --dry-run

# 组合条件：保留最近15个，删除超过3天的
/harness-cleanup-sessions --keep 15 --older-than 72
```

#### 输出格式
```
🧹 会话清理操作
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

清理条件:
- 时间限制: 超过24小时的会话
- 保留数量: 最近15个会话
- 操作模式: 预览模式 (dry-run)

扫描结果:
总会话数: 25个
符合删除条件: 8个
受保护会话: 17个

将要删除的会话:
1. 20260801-120000-old123 (5天前)
2. 20260802-153000-old456 (4天前)
...
8. 20260805-090000-old789 (1天前)

释放空间: 约 18.4MB

确认删除？移除 --dry-run 参数执行实际删除
```

#### 清理策略
- **时间限制**: 删除超过指定时间的会话
- **数量保护**: 始终保留最近N个会话
- **安全模式**: 使用 `--dry-run` 预览后再执行

#### 注意事项
- 删除操作不可逆，请谨慎使用
- 建议先使用 `--dry-run` 预览
- 重要会话建议备份或使用更高的保留数量

## 配置选项

在 `harness.toml` 中配置会话管理行为：

```toml
[session]
# 自动保存配置
autoSave = true              # 启用自动保存
tokenThreshold80 = true      # 80% Token时触发
tokenThreshold90 = true      # 90% Token时强制触发
saveIntervalMinutes = 30     # 最小保存间隔

# 恢复提示配置
restorePrompt = true         # 启用恢复提示
autoShowPrompt = true        # 自动显示提示

# 存储配置
maxStorageMB = 100           # 最大存储空间
compressionEnabled = true    # 启用压缩
maxHistoryAgeDays = 7        # 最大保存天数

# 清理配置
autoCleanup = true           # 自动清理
keepRecentSessions = 10      # 保留会话数量
cleanupIntervalHours = 24    # 清理间隔
```

## 错误处理

### 常见错误信息

**错误**: `无法保存会话：存储空间不足`
```
解决方案:
1. 使用 /harness-cleanup-sessions 清理旧会话
2. 增加 harness.toml 中的 maxStorageMB 配置
3. 手动删除 .claude/state/session-saves/ 中的旧会话
```

**错误**: `无法恢复会话：会话文件不存在`
```
解决方案:
1. 使用 /harness-list-sessions 查看可用的会话
2. 确认保存ID正确无误
3. 检查会话文件是否被外部删除
```

**错误**: `无法恢复会话：文件完整性验证失败`
```
解决方案:
1. 检查磁盘是否有错误
2. 验证会话文件权限
3. 可能需要重新创建会话保存
```

## 性能优化

### 保存性能
- **压缩设置**: 启用压缩可减少存储空间，但略微增加保存时间
- **并发控制**: 避免同时保存多个会话
- **空间检查**: 保存前检查可用空间可避免失败

### 恢复性能
- **选择性恢复**: 使用标准恢复模式而非完整恢复
- **摘要预览**: 使用 `--summary-only` 先确认会话内容
- **缓存优化**: 首次恢复后系统会缓存部分信息

### 存储优化
- **定期清理**: 设置合理的清理策略
- **压缩比例**: 文本内容通常可压缩70%+
- **空间监控**: 注意存储空间使用情况

## 最佳实践

### 保存策略
1. **重要里程碑**: 完成重要任务前保存
2. **定期保存**: 设置合理的自动保存间隔
3. **描述清晰**: 提供有意义的会话摘要

### 恢复策略
1. **预览确认**: 恢复前使用 `--summary-only` 查看
2. **选择模式**: 根据需要选择标准或完整恢复
3. **验证完整性**: 恢复后验证工作状态

### 维护策略
1. **定期清理**: 设置合理的自动清理策略
2. **空间监控**: 注意存储空间使用情况
3. **备份重要**: 重要会话考虑手动备份

## 相关命令

相关技能命令：
- `/harness-plan` - 任务规划与会话保存配合
- `/harness-work` - 任务执行中的会话管理
- `/harness-sync` - 会话状态同步
- `/harness-progress` - 进度跟踪与会话保存