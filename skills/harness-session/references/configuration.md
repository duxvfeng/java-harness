# Harness Session Configuration Reference

会话管理系统的配置参考文档。

## 配置文件位置

主配置文件：
- 项目配置: `harness.toml`
- 用户配置: `~/.claude/config/harness.toml`
- 环境变量: 可覆盖配置文件设置

## 配置结构

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
includePromptInContext = false

# 存储配置
storageRoot = ".claude/state/session-saves"
maxStorageMB = 100
compressionEnabled = true
compressionLevel = 6
maxHistoryAgeDays = 7

# 清理配置
autoCleanup = true
keepRecentSessions = 10
cleanupIntervalHours = 24
dryRunCleanup = false

# 性能配置
saveTimeoutSeconds = 10
restoreTimeoutSeconds = 15
maxConcurrentOperations = 1

# 日志配置
logLevel = "INFO"
logSaveOperations = true
logRestoreOperations = true
logCleanupOperations = true
```

## 详细配置说明

### 自动保存配置

#### `autoSave`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 启用或禁用自动保存功能
- **影响**: 控制系统是否在Token阈值时自动保存

#### `tokenThreshold80`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: Token使用率达到80%时触发自动保存
- **影响**: 中等优先级的自动保存触发点

#### `tokenThreshold90`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: Token使用率达到90%时强制自动保存
- **影响**: 高优先级的自动保存触发点

#### `saveIntervalMinutes`
- **类型**: `integer`
- **默认**: `30`
- **范围**: `5-1440` (5分钟到24小时)
- **说明**: 两次保存之间的最小间隔时间
- **影响**: 防止频繁保存，保护系统资源

### 恢复提示配置

#### `restorePrompt`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 启用或禁用恢复提示功能
- **影响**: 控制新会话启动时是否检测和提示恢复

#### `autoShowPrompt`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 自动显示恢复提示（而非静默记录）
- **影响**: 用户是否看到自动生成的恢复建议

#### `includePromptInContext`
- **类型**: `boolean`
- **默认**: `false`
- **说明**: 将恢复提示包含在上下文中传递给AI
- **影响**: 增加上下文大小，但可能提高恢复成功率

### 存储配置

#### `storageRoot`
- **类型**: `string`
- **默认**: `".claude/state/session-saves"`
- **说明**: 会话存储根目录（相对于项目根目录）
- **影响**: 会话文件的存储位置

#### `maxStorageMB`
- **类型**: `integer`
- **默认**: `100`
- **范围**: `10-1024` (10MB到1GB)
- **说明**: 最大存储空间限制（MB）
- **影响**: 超过此限制时触发清理或拒绝保存

#### `compressionEnabled`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 启用GZIP压缩
- **影响**: 减少存储空间但略微增加处理时间

#### `compressionLevel`
- **类型**: `integer`
- **默认**: `6`
- **范围**: `0-9` (0=无压缩, 9=最大压缩)
- **说明**: GZIP压缩级别
- **影响**: 更高压缩率但更慢的处理速度

#### `maxHistoryAgeDays`
- **类型**: `integer`
- **默认**: `7`
- **范围**: `1-365` (1天到1年)
- **说明**: 会话文件的最大保存天数
- **影响**: 自动清理的时间阈值

### 清理配置

#### `autoCleanup`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 启用自动清理过期会话
- **影响**: 系统是否自动删除旧会话

#### `keepRecentSessions`
- **类型**: `integer`
- **默认**: `10`
- **范围**: `5-100`
- **说明**: 清理时保留的最近会话数量
- **影响**: 即使过期也保护的会话数量

#### `cleanupIntervalHours`
- **类型**: `integer`
- **默认**: `24`
- **范围**: `1-168` (1小时到7天)
- **说明**: 自动清理的执行间隔
- **影响**: 清理操作的频率

#### `dryRunCleanup`
- **类型**: `boolean`
- **默认**: `false`
- **说明**: 清理操作默认使用预览模式
- **影响**: 需要手动确认实际删除操作

### 性能配置

#### `saveTimeoutSeconds`
- **类型**: `integer`
- **默认**: `10`
- **范围**: `5-60`
- **说明**: 保存操作的超时时间
- **影响**: 保存失败前的等待时间

#### `restoreTimeoutSeconds`
- **类型**: `integer`
- **默认**: `15`
- **范围**: `5-120`
- **说明**: 恢复操作的超时时间
- **影响**: 恢复失败前的等待时间

#### `maxConcurrentOperations`
- **类型**: `integer`
- **默认**: `1`
- **范围**: `1-5`
- **说明**: 最大并发操作数
- **影响**: 同时执行的保存/恢复操作数量

### 日志配置

#### `logLevel`
- **类型**: `string`
- **默认**: `"INFO"`
- **选项**: `"TRACE"`, `"DEBUG"`, `"INFO"`, `"WARN"`, `"ERROR"`
- **说明**: 日志级别
- **影响**: 日志输出的详细程度

#### `logSaveOperations`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 记录保存操作日志
- **影响**: 保存操作的日志记录

#### `logRestoreOperations`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 记录恢复操作日志
- **影响**: 恢复操作的日志记录

#### `logCleanupOperations`
- **类型**: `boolean`
- **默认**: `true`
- **说明**: 记录清理操作日志
- **影响**: 清理操作的日志记录

## 环境变量覆盖

可以使用环境变量覆盖配置文件设置：

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

## 配置优先级

配置优先级从高到低：
1. 环境变量
2. 项目配置文件 (`harness.toml`)
3. 用户配置文件 (`~/.claude/config/harness.toml`)
4. 默认值

## 配置验证

系统启动时会验证配置的有效性：

### 检查项目
- **数值范围**: 确保数值在合理范围内
- **路径验证**: 检查存储路径是否可访问
- **权限检查**: 验证目录创建和写入权限
- **空间检查**: 验证磁盘空间是否充足

### 无效配置处理
- **警告**: 对非关键配置显示警告
- **使用默认值**: 对无效配置使用默认值
- **启动失败**: 对关键配置失败拒绝启动

## 配置示例

### 开发环境配置
```toml
[session]
# 开发环境：频繁保存，详细日志
autoSave = true
tokenThreshold80 = true
tokenThreshold90 = true
saveIntervalMinutes = 15

# 详细调试信息
logLevel = "DEBUG"
logSaveOperations = true
logRestoreOperations = true
logCleanupOperations = true
```

### 生产环境配置
```toml
[session]
# 生产环境：稳定优先，简洁日志
autoSave = true
tokenThreshold80 = false
tokenThreshold90 = true
saveIntervalMinutes = 60

# 简洁日志
logLevel = "INFO"
logSaveOperations = true
logRestoreOperations = false
logCleanupOperations = false
```

### 测试环境配置
```toml
[session]
# 测试环境：禁用自动保存，手动控制
autoSave = false
saveIntervalMinutes = 5

# 测试存储位置
storageRoot = ".claude/state/test-sessions"
maxStorageMB = 50

# 快速清理
autoCleanup = true
keepRecentSessions = 5
cleanupIntervalHours = 1
```

### 高性能环境配置
```toml
[session]
# 高性能环境：禁用压缩，增加超时
compressionEnabled = false
saveTimeoutSeconds = 30
restoreTimeoutSeconds = 45

# 增加并发操作
maxConcurrentOperations = 3

# 大容量存储
maxStorageMB = 500
keepRecentSessions = 50
```

## 故障排除配置问题

### 配置不生效

**问题**: 修改配置后没有效果

**解决方案**:
1. 检查配置文件位置是否正确
2. 验证配置语法是否正确
3. 确认没有环境变量覆盖
4. 重启应用程序

### 无效配置值

**问题**: 配置值被系统拒绝

**解决方案**:
1. 检查数值范围限制
2. 验证路径是否可访问
3. 确认数据类型正确
4. 查看日志中的详细错误信息

### 存储空间问题

**问题**: 存储空间不足导致保存失败

**解决方案**:
1. 增加 `maxStorageMB` 配置值
2. 减少 `keepRecentSessions` 数量
3. 降低 `maxHistoryAgeDays` 天数
4. 手动执行清理操作

## 配置迁移

### 旧版本升级

从旧版本升级时可能需要迁移配置：

#### 版本4.0 → 4.1
```toml
# 新增配置项
[session]
# 新增恢复提示配置
restorePrompt = true
autoShowPrompt = true

# 新增性能配置
saveTimeoutSeconds = 10
restoreTimeoutSeconds = 15
```

#### 配置兼容性
- 旧配置文件在新版本中继续有效
- 缺失的新配置项使用默认值
- 弃用的配置项会被忽略但显示警告

## 相关配置

相关配置部分：
- `[hooks]` - Hook系统配置（session-monitor, session-init）
- `[commands]` - 命令注册配置
- `[storage]` - 通用存储配置
- `[logging]` - 全局日志配置