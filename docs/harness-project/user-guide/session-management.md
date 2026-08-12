# 会话管理系统用户指南

## 目录
1. [快速开始](#快速开始)
2. [核心概念](#核心概念)
3. [配置指南](#配置指南)
4. [使用教程](#使用教程)
5. [故障排除](#故障排除)
6. [最佳实践](#最佳实践)
7. [API参考](#api参考)

---

## 快速开始

### 什么是会话管理系统？

会话管理系统是Java Harness的核心功能，用于解决大型AI开发任务中的context满问题。它可以：

- 💾 **自动保存**：在Token使用率超过阈值时自动保存会话状态
- 🔄 **智能恢复**：新会话启动时自动检测并建议恢复上一次的工作
- 🧹 **存储管理**：自动清理、压缩存储，节省空间
- 🎯 **无缝集成**：与现有的Hook系统、任务管理、Git状态完全集成

### 3分钟快速入门

#### 1. 检查当前状态

```bash
# 查看已保存的会话
/harness-list-sessions

# 查看系统健康状况
java-harness doctor
```

#### 2. 保存当前工作

```bash
# 手动保存当前会话
/harness-save-session "完成Task 11.8实现"

# 强制保存（忽略间隔限制）
/harness-save-session --force
```

#### 3. 恢复会话

```bash
# 查看可恢复的会话
/harness-list-sessions --recent 3

# 恢复到特定会话
/harness-restore-session 20260809-174530-abc123

# 完整恢复（包含所有对话历史）
/harness-restore-session 20260809-174530-abc123 --full
```

---

## 核心概念

### 会话生命周期

```
┌─────────────────────────────────────────────────────────┐
│                   会话生命周期                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  开始 → 工作进行中 → Token阈值 → 自动保存 → 继续工作     │
│   ↓                        ↑                        ↓       │
│   └────────新会话启动 ←── 恢复建议 ←──────┘            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 关键组件

#### 1. SessionSaveManager
负责管理会话保存操作：
- **自动保存**：Token使用率达到80%/90%时触发
- **手动保存**：用户主动调用保存命令
- **并发控制**：防止同时保存冲突
- **间隔限制**：避免频繁保存影响性能

#### 2. SessionRestoreManager
负责会话恢复检测和建议：
- **恢复检测**：检查是否有可恢复的会话
- **摘要生成**：生成会话状态摘要
- **AI决策**：智能判断是否需要详细恢复
- **完整性验证**：确保恢复文件完整

#### 3. FileSystemStorage
负责文件系统存储管理：
- **压缩存储**：使用GZIP压缩会话数据
- **目录管理**：自动创建和清理存储目录
- **健康检查**：验证存储系统状态
- **并发安全**：文件锁保护数据完整性

### Token监控机制

```java
// Token监控触发条件
Token使用率 ≥ 80%  → 自动保存（中优先级）
Token使用率 ≥ 90%  → 强制保存（高优先级）
距离上次保存 ≥ 30分钟 → 允许手动保存
```

---

## 配置指南

### 基本配置

在项目根目录创建或编辑 `harness.toml`：

```toml
[session]
# ===== 自动保存配置 =====
autoSave = true              # 启用自动保存
tokenThreshold80 = true      # 80% Token时触发
tokenThreshold90 = true      # 90% Token时强制触发
saveIntervalMinutes = 30     # 最小保存间隔（分钟）

# ===== 恢复提示配置 =====
restorePrompt = true         # 启用恢复提示
autoShowPrompt = true        # 自动显示提示
includePromptInContext = false # 将提示包含在上下文中

# ===== 存储配置 =====
storageRoot = ".claude/state/session-saves"  # 存储目录
maxStorageMB = 100           # 最大存储空间（MB）
compressionEnabled = true    # 启用压缩
compressionLevel = 6          # 压缩级别（0-9，越高压缩率越大）
maxHistoryAgeDays = 7        # 最大保存天数

# ===== 清理配置 =====
autoCleanup = true           # 自动清理过期会话
keepRecentSessions = 10      # 保留最近会话数量
cleanupIntervalHours = 24   # 清理执行间隔（小时）

# ===== 性能配置 =====
saveTimeoutSeconds = 10     # 保存操作超时（秒）
restoreTimeoutSeconds = 15  # 恢复操作超时（秒）
maxConcurrentOperations = 1  # 最大并发操作数

# ===== 日志配置 =====
logLevel = "INFO"             # 日志级别
logSaveOperations = true    # 记录保存操作
logRestoreOperations = true # 记录恢复操作
logCleanupOperations = true # 记录清理操作
```

### 高级配置

#### 开发环境配置

```toml
[session]
# 开发环境：频繁保存，详细日志
saveIntervalMinutes = 15     # 更频繁的保存
logLevel = "DEBUG"             # 详细调试信息
compressionLevel = 3           # 较低的压缩（更快）
```

#### 生产环境配置

```toml
[session]
# 生产环境：稳定优先，简洁日志
tokenThreshold80 = false     # 只在90%时触发
logLevel = "WARN"              # 只记录警告和错误
compressionLevel = 9           # 最大压缩（节省空间）
keepRecentSessions = 5        # 保留更少会话
```

#### 高性能环境配置

```toml
[session]
# 高性能环境：禁用压缩，增加超时
compressionEnabled = false    # 禁用压缩
saveTimeoutSeconds = 30       # 增加超时时间
restoreTimeoutSeconds = 45    # 增加恢复超时
maxConcurrentOperations = 3  # 允许更多并发
maxStorageMB = 500           # 更大的存储配额
```

### 环境变量配置

可以通过环境变量覆盖配置文件设置：

```bash
# 自动保存配置
export HARNESS_SESSION_AUTO_SAVE=true
export HARNESS_SESSION_TOKEN_80_THRESHOLD=true
export HARNESS_SESSION_TOKEN_90_THRESHOLD=true
export HARNESS_SESSION_SAVE_INTERVAL_MINUTES=30

# 恢复提示配置
export HARNESS_SESSION_RESTORE_PROMPT=true
export HARNESS_SESSION_AUTO_SHOW_PROMPT=true

# 存储配置
export HARNESS_SESSION_STORAGE_ROOT=".claude/state/session-saves"
export HARNESS_SESSION_MAX_STORAGE_MB=100
export HARNESS_SESSION_COMPRESSION_ENABLED=true

# 清理配置
export HARNESS_SESSION_AUTO_CLEANUP=true
export HARNESS_SESSION_KEEP_RECENT_SESSIONS=10
```

---

## 使用教程

### 场景1：长时间开发任务

**使用场景**：执行需要多次对话的复杂任务

**步骤**：

1. **开始任务前保存**
```bash
/harness-save-session "开始Phase 11任务实施"
```

2. **正常工作** - 系统会在适当时机自动保存

3. **Token接近限制时** - 系统自动触发保存
```
💾 自动保存触发: Token使用率 85%
保存ID: 20260809-173045-abc123
```

4. **继续工作** - 新会话中恢复并继续
```bash
# 系统自动显示恢复提示
📋 会话恢复建议

建议恢复到：20260809-173045-abc123
置信度：85%
当前任务：Task 11.8 进行中

使用以下命令恢复：
/harness-restore-session 20260809-173045-abc123
```

### 场景2：Context满后恢复

**使用场景**：Token使用接近上限，需要恢复到之前的工作状态

**步骤**：

1. **启动新会话** - 系统自动检测可恢复会话

2. **查看可用会话**
```bash
/harness-list-sessions --recent 5
```

3. **选择合适的会话恢复**
```bash
# 标准恢复（推荐）
/harness-restore-session 20260809-150000-def456

# 完整恢复（包含所有对话历史）
/harness-restore-session 20260809-150000-def456 --full

# 仅查看摘要
/harness-restore-session 20260809-150000-def456 --summary-only
```

4. **验证恢复结果**
```bash
# 查看会话详情
/harness-show-session 20260809-150000-def456
```

### 场景3：定期存储维护

**使用场景**：管理存储空间，清理旧会话

**步骤**：

1. **查看存储使用情况**
```bash
/harness-list-sessions --all
```

2. **预览将要清理的会话**
```bash
/harness-cleanup-sessions --dry-run
```

3. **执行清理操作**
```bash
# 保留最近20个会话
/harness-cleanup-sessions --keep 20

# 清理超过3天的会话
/harness-cleanup-sessions --older-than 72

# 组合条件
/harness-cleanup-sessions --keep 15 --older-than 168
```

### 场景4：分支切换前保存

**使用场景**：切换Git分支前保存当前工作状态

**步骤**：

1. **保存当前分支的工作**
```bash
/harness-save-session "feature-branch-complete-before-switch"
```

2. **切换分支**
```bash
git checkout main
```

3. **在新分支上工作**（如需要可恢复）
```bash
# 查看之前的会话
/harness-list-sessions --all

# 如需要恢复分支工作
/harness-restore-session <saveId>
```

---

## 故障排除

### 问题1：保存失败

**症状**：执行保存命令时显示"❌ 会话保存失败"

**可能原因**：
- 磁盘空间不足
- 目录权限问题
- 存储配额已满

**解决方案**：

```bash
# 1. 检查磁盘空间
df -h .claude/state/

# 2. 清理旧会话
/harness-cleanup-sessions --keep 5 --older-than 24

# 3. 检查目录权限
ls -la .claude/state/session-saves/

# 4. 增加存储配额（编辑harness.toml）
# [session]
# maxStorageMB = 200
```

### 问题2：恢复失败

**症状**：执行恢复命令时显示"❌ 会话不存在"或"❌ 恢复失败"

**可能原因**：
- 会话ID不正确
- 会话文件被删除
- 文件损坏

**解决方案**：

```bash
# 1. 列出可用会话
/harness-list-sessions --all

# 2. 验证会话完整性
/harness-show-session <saveId>

# 3. 检查存储目录
ls -la .claude/state/session-saves/

# 4. 如果文件损坏，尝试清理重建
rm -rf .claude/state/session-saves/
java-harness init
```

### 问题3：自动保存未触发

**症状**：Token使用率高时没有自动保存

**可能原因**：
- 自动保存功能禁用
- Hook未正确安装
- 环境变量未设置

**解决方案**：

```bash
# 1. 检查配置
grep -A 10 "\[session\]" harness.toml

# 2. 检查环境变量
echo $CLAUDE_TOKEN_COUNT

# 3. 验证Hook安装
ls -la .claude-plugin/hooks.json

# 4. 查看日志
tail -20 .claude/logs/session-monitor.log
```

### 问题4：恢复提示未显示

**症状**：新会话启动时没有显示恢复建议

**可能原因**：
- 恢复提示功能禁用
- 无可恢复的会话
- 会话过于陈旧

**解决方案**：

```bash
# 1. 检查配置
grep -A 5 "restorePrompt" harness.toml

# 2. 手动查看可恢复会话
/harness-list-sessions --recent 5

# 3. 查看详细日志
tail -20 .claude/logs/session-init.log
```

### 问题5：存储空间快速增长

**症状**：`.claude/state/session-saves/` 目录占用空间过大

**可能原因**：
- 自动保存过于频繁
- 压缩功能未启用
- 清理策略设置不当

**解决方案**：

```bash
# 1. 分析会话大小分布
du -sh .claude/state/session-saves/*/

# 2. 检查压缩设置
grep "compression" harness.toml

# 3. 优化清理策略
# 编辑harness.toml：
# [session]
# saveIntervalMinutes = 60  # 增加保存间隔
# compressionLevel = 9       # 提高压缩率
# autoCleanup = true
# keepRecentSessions = 5    # 减少保留数量

# 4. 立即执行清理
/harness-cleanup-sessions --keep 5 --older-than 48
```

---

## 最佳实践

### 1. 保存策略

#### 定期保存原则
- **重要里程碑**：完成关键任务前保存
- **工作分段**：复杂任务分阶段保存
- **每日结束**：一天工作结束时保存
- **分支切换**：切换分支前保存当前状态

#### 自动保存配合
```bash
# 配置合理的自动保存阈值
[session]
tokenThreshold80 = true   # 早期警告
tokenThreshold90 = true   # 强制保存
saveIntervalMinutes = 30  # 避免过度频繁
```

### 2. 恢复策略

#### 恢复模式选择
- **标准恢复**（推荐）：快速恢复，包含基本上下文
- **完整恢复**：大型任务需要完整对话历史
- **摘要查看**：仅检查会话内容，不实际恢复

#### 恢复时机
- **Context接近上限**：及时释放空间
- **新会话开始**：自动提示恢复
- **切换任务分支**：恢复到相关工作状态

### 3. 存储管理

#### 定期维护
```bash
# 每周运行一次完整维护
/harness-list-sessions --all
/harness-cleanup-sessions --dry-run  # 先预览
/harness-cleanup-sessions --keep 10  # 确认后执行
```

#### 容量规划
- **小型项目**：50MB存储，保留10个会话
- **中型项目**：100MB存储，保留15个会话
- **大型项目**：200MB存储，保留20个会话

### 4. 团队协作

#### 分支隔离
```bash
# 在feature分支工作时启用分支隔离
/harness-work --isolate-branch

# 确保不会影响主分支的稳定性
```

#### 状态同步
- 保存前提交重要更改
- 恢复后验证Git状态
- 使用描述性的保存摘要

### 5. 性能优化

#### 保存优化
- 避免在单个对话中保存过多内容
- 使用合理的压缩级别（默认6）
- 定期清理释放空间

#### 恢复优化
- 优先使用标准恢复模式
- 避免不必要的完整恢复
- 利用摘要信息选择性恢复

---

## API参考

### 命令接口

#### /harness-save-session
保存当前会话状态

**语法**：
```
/harness-save-session [summary] [--force]
```

**参数**：
- `summary`（可选）：会话摘要描述
- `--force`（可选）：强制保存，忽略间隔限制

**返回**：保存成功或失败的信息

**示例**：
```bash
/harness-save-session "完成核心功能实现"
/harness-save-session --force
```

#### /harness-restore-session
恢复已保存的会话

**语法**：
```
/harness-restore-session <saveId> [--full] [--summary-only]
```

**参数**：
- `saveId`（必需）：会话保存ID
- `--full`（可选）：完整恢复，包含所有对话历史
- `--summary-only`（可选）：仅显示会话摘要，不执行恢复

**返回**：恢复结果的详细信息

**示例**：
```bash
/harness-restore-session 20260809-174530-abc123
/harness-restore-session 20260809-174530-abc123 --full
/harness-restore-session 20260809-174530-abc123 --summary-only
```

#### /harness-list-sessions
列出所有保存的会话

**语法**：
```
/harness-list-sessions [--recent N] [--all]
```

**参数**：
- `--recent N`（可选）：显示最近N个会话（默认5个）
- `--all`（可选）：显示所有会话

**返回**：会话列表，包含时间、摘要、Token使用率等信息

**示例**：
```bash
/harness-list-sessions
/harness-list-sessions --recent 10
/harness-list-sessions --all
```

#### /harness-show-session
显示特定会话的详细信息

**语法**：
```
/harness-show-session <saveId>
```

**参数**：
- `saveId`（必需）：会话保存ID

**返回**：详细的会话信息，包括任务状态、Git状态、完整性验证等

**示例**：
```bash
/harness-show-session 20260809-174530-abc123
```

#### /harness-cleanup-sessions
清理旧的会话保存文件

**语法**：
```
/harness-cleanup-sessions [--older-than HOURS] [--keep N] [--dry-run]
```

**参数**：
- `--older-than HOURS`（可选）：删除超过指定小时数的会话（默认168小时=7天）
- `--keep N`（可选）：保留最近N个会话（默认10个）
- `--dry-run`（可选）：仅显示将要删除的会话，不实际删除

**返回**：清理操作的结果和统计信息

**示例**：
```bash
/harness-cleanup-sessions
/harness-cleanup-sessions --older-than 24
/harness-cleanup-sessions --keep 20
/harness-cleanup-sessions --dry-run
```

### 配置API

#### SessionSaveConfig
会话保存配置类

**属性**：
- `maxSaves`：最大保存数量
- `maxAgeDays`：最大保存天数
- `maxSingleSaveBytes`：单个保存最大大小
- `minSaveIntervalMinutes`：最小保存间隔（分钟）

**默认值**：
```java
SessionSaveConfig.getDefault()
// maxSaves: 10
// maxAgeDays: 7
// maxSingleSaveBytes: 50MB
// minSaveIntervalMinutes: 5
```

#### RestoreConfig
会话恢复配置类

**属性**：
- `maxHistoryAgeHours`：历史记录最大年龄（小时）

**默认值**：
```java
RestoreConfig.getDefault()
// maxHistoryAgeHours: 168 (7天)
```

### 性能指标

#### 系统性能目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 保存时间 | < 3秒 | 典型会话保存完成时间 |
| 恢复时间 | < 5秒 | 典型会话恢复完成时间 |
| 压缩率 | > 70% | 文本内容的压缩比例 |
| 存储占用 | < 10MB/会话 | 压缩后的平均会话大小 |
| 保存成功率 | > 99% | 保存操作的成功率 |
| 恢复成功率 | > 98% | 恢复操作的成功率 |

#### 性能优化建议

1. **定期清理**：设置合理的清理策略避免存储无限增长
2. **压缩优化**：使用适当的压缩级别平衡性能和空间
3. **并发控制**：避免同时进行多个保存操作
4. **容量规划**：根据项目规模配置适当的存储配额

---

## 附录

### A. 完整配置示例

```toml
# harness.toml 完整会话管理配置示例

[session]
# ===== 自动保存配置 =====
autoSave = true              # 启用自动保存
tokenThreshold80 = true      # 80% Token时触发
tokenThreshold90 = true      # 90% Token时强制触发
saveIntervalMinutes = 30     # 最小保存间隔（分钟）

# ===== 恢复提示配置 =====
restorePrompt = true         # 启用恢复提示
autoShowPrompt = true        # 自动显示提示
includePromptInContext = false # 将提示包含在上下文中

# ===== 存储配置 =====
storageRoot = ".claude/state/session-saves"  # 存储目录
maxStorageMB = 100           # 最大存储空间（MB）
compressionEnabled = true    # 启用压缩
compressionLevel = 6          # 压缩级别（0-9）
maxHistoryAgeDays = 7        # 最大保存天数

# ===== 清理配置 =====
autoCleanup = true           # 自动清理过期会话
keepRecentSessions = 10      # 保留最近会话数量
cleanupIntervalHours = 24   # 清理执行间隔（小时）

# ===== 性能配置 =====
saveTimeoutSeconds = 10     # 保存操作超时（秒）
restoreTimeoutSeconds = 15  # 恢复操作超时（秒）
maxConcurrentOperations = 1  # 最大并发操作数

# ===== 日志配置 =====
logLevel = "INFO"             # 日志级别
logSaveOperations = true    # 记录保存操作
logRestoreOperations = true # 记录恢复操作
logCleanupOperations = true # 记录清理操作
```

### B. 常见问题解答

**Q: 会话保存会影响性能吗？**
A: 会话保存经过优化，典型保存时间<3秒，对性能影响很小。建议在重要里程碑后手动保存，让自动保存处理常规情况。

**Q: 恢复会话会替换当前工作吗？**
A: 不会。恢复操作只是加载历史上下文，不会删除或修改当前文件。你可以选择性地应用历史内容。

**Q: 可以在多个项目间共享会话吗？**
A: 会话数据保存在项目本地，不同项目的会话是独立的。如需跨项目共享，需要手动复制会话文件。

**Q: 会话数据安全吗？**
A: 会话数据仅存储在本地项目目录中，不会上传到外部服务。建议定期备份重要的 `.claude/state/` 目录。

**Q: 如何迁移会话数据？**
A: 复制 `.claude/state/session-saves/` 目录到新项目即可。确保目标项目有兼容的会话管理版本。

---

## 获取帮助

### 日志文件位置
- **保存日志**：`.claude/logs/session-save.log`
- **恢复日志**：`.claude/logs/session-restore.log`
- **清理日志**：`.claude/logs/session-cleanup.log`
- **Hook日志**：`.claude/logs/session-monitor.log`

### 调试模式

启用详细日志：

```toml
# 编辑 harness.toml
[session]
logLevel = "DEBUG"  # 显示详细调试信息
```

### 支持联系

如问题持续存在，请提供以下信息：

1. **系统信息**：
```bash
java -version
mvn -version
```

2. **配置信息**：
```bash
cat harness.toml
```

3. **日志信息**：
```bash
tail -100 .claude/logs/session-save.log
```

4. **存储状态**：
```bash
ls -la .claude/state/session-saves/
du -sh .claude/state/session-saves/
```

---

**文档版本**：1.0  
**最后更新**：2026-08-09  
**适用版本**：Java Harness 4.1.1+