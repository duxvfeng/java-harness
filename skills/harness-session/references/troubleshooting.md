# Harness Session Troubleshooting Guide

会话管理系统故障排除指南。

## 常见问题分类

### 保存相关问题
### 恢复相关问题
### 存储相关问题
### 性能相关问题
### 配置相关问题

---

## 保存相关问题

### 问题：保存失败 - 存储空间不足

**错误信息**:
```
❌ 无法保存会话：存储空间不足
需要空间: 15MB
可用空间: 3MB
最大限制: 100MB
```

**原因分析**:
1. 磁盘空间不足
2. 会话存储配额已满
3. 旧会话文件未及时清理

**解决方案**:
```bash
# 1. 检查当前存储使用情况
/harness-list-sessions --all

# 2. 清理旧会话（预览模式）
/harness-cleanup-sessions --dry-run

# 3. 执行实际清理
/harness-cleanup-sessions --keep 10 --older-than 72

# 4. 如果问题持续，手动删除存储目录中的旧会话
rm -rf .claude/state/session-saves/202608*

# 5. 考虑增加存储配额
# 编辑 harness.toml:
# [session]
# maxStorageMB = 200
```

**预防措施**:
- 启用自动清理功能
- 设置合理的保留策略
- 定期检查存储空间使用情况

### 问题：保存失败 - 权限错误

**错误信息**:
```
❌ 无法保存会话：权限被拒绝
存储路径: .claude/state/session-saves/20260809-174530-abc123/
错误: Permission denied
```

**原因分析**:
1. 目录权限不正确
2. 文件被其他进程锁定
3. 用户权限不足

**解决方案**:
```bash
# 1. 检查目录权限
ls -la .claude/state/session-saves/

# 2. 修复目录权限
chmod 755 .claude/state/session-saves/

# 3. 检查文件锁定
lsof .claude/state/session-saves/

# 4. 重启系统或释放文件锁
```

**预防措施**:
- 确保正确的目录权限设置
- 避免多个进程同时访问同一存储
- 使用适当的用户权限运行

### 问题：保存超时

**错误信息**:
```
❌ 保存操作超时
保存ID: 20260809-174530-abc123
超时时间: 10秒
实际用时: 15秒
```

**原因分析**:
1. 会话数据量过大
2. 磁盘IO性能问题
3. 压缩处理时间过长

**解决方案**:
```bash
# 1. 增加超时时间配置
# 编辑 harness.toml:
# [session]
# saveTimeoutSeconds = 20

# 2. 禁用压缩（牺牲存储空间）
# [session]
# compressionEnabled = false

# 3. 检查磁盘性能
iostat -x

# 4. 清理当前工作状态，减少数据量
```

**预防措施**:
- 定期保存，避免单次保存数据量过大
- 优化磁盘性能
- 调整压缩级别

---

## 恢复相关问题

### 问题：恢复失败 - 会话不存在

**错误信息**:
```
❌ 无法恢复会话：会话文件不存在
保存ID: 20260809-174530-abc123
存储路径: .claude/state/session-saves/20260809-174530-abc123/
```

**原因分析**:
1. 会话ID输入错误
2. 会话文件被外部删除
3. 会话已过期清理

**解决方案**:
```bash
# 1. 列出可用的会话
/harness-list-sessions --all

# 2. 查找相似的会话ID
/harness-list-sessions | grep "20260809"

# 3. 验证会话是否存在
ls -la .claude/state/session-saves/

# 4. 检查清理日志
cat .claude/logs/session-cleanup.log
```

**预防措施**:
- 使用准确的会话ID
- 定期备份重要会话
- 设置合理的保留策略

### 问题：恢复失败 - 文件损坏

**错误信息**:
```
❌ 无法恢复会话：文件完整性验证失败
保存ID: 20260809-174530-abc123
损坏文件: session-data.json.gz
错误: GZIP checksum error
```

**原因分析**:
1. 磁盘错误导致文件损坏
2. 不正常的程序终止
3. 文件传输过程中的错误

**解决方案**:
```bash
# 1. 检查磁盘健康
diskutil verify disk

# 2. 尝试修复损坏的文件
gunzip -t .claude/state/session-saves/20260809-174530-abc123/session-data.json.gz

# 3. 查看是否有备份文件
ls -la .claude/state/session-saves/20260809-174530-abc123/

# 4. 如果有元数据文件，可以查看会话信息
cat .claude/state/session-saves/20260809-174530-abc123/metadata.json
```

**预防措施**:
- 使用可靠的存储介质
- 避免强制终止程序
- 启用文件系统校验和

### 问题：恢复超时

**错误信息**:
```
❌ 恢复操作超时
保存ID: 20260809-174530-abc123
超时时间: 15秒
处理进度: 60%
```

**原因分析**:
1. 会话数据量过大
2. 解压缩处理时间过长
3. 系统资源不足

**解决方案**:
```bash
# 1. 增加恢复超时时间
# 编辑 harness.toml:
# [session]
# restoreTimeoutSeconds = 30

# 2. 使用标准恢复而非完整恢复
/harness-restore-session 20260809-174530-abc123

# 3. 先查看摘要，确认会话内容
/harness-restore-session 20260809-174530-abc123 --summary-only

# 4. 检查系统资源
top -o cpu
```

**预防措施**:
- 定期保存，避免单次恢复数据量过大
- 使用合适的恢复模式
- 确保系统资源充足

---

## 存储相关问题

### 问题：存储空间快速增长

**现象**: 会话存储目录占用空间迅速增加

**原因分析**:
1. 自动保存过于频繁
2. 压缩功能未启用
3. 清理策略设置不当

**解决方案**:
```bash
# 1. 检查当前存储使用情况
du -sh .claude/state/session-saves/

# 2. 分析会话大小分布
ls -lh .claude/state/session-saves/*/session-data.json.gz

# 3. 调整保存策略
# 编辑 harness.toml:
# [session]
# saveIntervalMinutes = 60  # 增加保存间隔
# compressionEnabled = true  # 确保压缩启用

# 4. 优化清理策略
# [session]
# autoCleanup = true
# keepRecentSessions = 5
# maxHistoryAgeDays = 3
```

**预防措施**:
- 合理设置保存间隔
- 确保压缩功能启用
- 设置积极的清理策略

### 问题：会话文件损坏

**现象**: 多个会话文件出现损坏

**原因分析**:
1. 磁盘硬件问题
2. 不正常的系统关机
3. 文件系统错误

**解决方案**:
```bash
# 1. 检查磁盘健康
smartctl -a /dev/sda

# 2. 修复文件系统
fsck -f /dev/sda1

# 3. 检查系统日志
dmesg | grep -i error

# 4. 更换存储位置到更健康的磁盘
# 编辑 harness.toml:
# [session]
# storageRoot = "/mnt/healthy-disk/session-saves"
```

**预防措施**:
- 使用可靠的硬件
- 定期检查磁盘健康
- 使用UPS防止突然断电

---

## 性能相关问题

### 问题：保存操作缓慢

**现象**: 保存操作耗时过长（>5秒）

**原因分析**:
1. 压缩级别过高
2. 磁盘IO性能问题
3. 数据量过大

**解决方案**:
```bash
# 1. 检查当前保存性能
time /harness-save-session "性能测试"

# 2. 调整压缩级别
# 编辑 harness.toml:
# [session]
# compressionLevel = 3  # 降低压缩级别

# 3. 禁用压缩（如果性能优先）
# [session]
# compressionEnabled = false

# 4. 检查磁盘IO性能
iostat -x 1 5
```

**预防措施**:
- 选择合适的压缩级别
- 使用高性能存储
- 优化数据结构

### 问题：恢复操作缓慢

**现象**: 恢复操作耗时过长（>10秒）

**原因分析**:
1. 数据量过大
2. 解压缩耗时
3. 网络存储延迟

**解决方案**:
```bash
# 1. 使用标准恢复模式
/harness-restore-session 20260809-174530-abc123

# 2. 分阶段恢复
# 先恢复基本状态，再按需恢复详细上下文

# 3. 检查网络存储性能
ping -c 5 storage-server

# 4. 考虑本地缓存
```

**预防措施**:
- 合理设置恢复模式
- 优化数据结构
- 使用本地存储

---

## 配置相关问题

### 问题：配置不生效

**现象**: 修改配置文件后没有效果

**原因分析**:
1. 配置文件位置错误
2. 配置语法错误
3. 环境变量覆盖

**解决方案**:
```bash
# 1. 确认配置文件位置
ls -la harness.toml

# 2. 验证配置语法
cat harness.toml | grep -A 10 "\[session\]"

# 3. 检查环境变量覆盖
env | grep HARNESS_SESSION

# 4. 重启应用程序
```

**预防措施**:
- 使用正确的配置文件位置
- 验证配置语法
- 检查环境变量设置

### 问题：自动保存未触发

**现象**: Token使用率高时没有自动保存

**原因分析**:
1. 自动保存功能禁用
2. Token监控未启用
3. 保存间隔限制

**解决方案**:
```bash
# 1. 检查自动保存配置
# 编辑 harness.toml:
# [session]
# autoSave = true
# tokenThreshold80 = true
# tokenThreshold90 = true

# 2. 检查Token监控环境变量
echo $CLAUDE_TOKEN_COUNT

# 3. 检查Hook安装
# 确认 session-monitor hook 正确安装

# 4. 查看日志了解详细情况
tail -f .claude/logs/session-monitor.log
```

**预防措施**:
- 确保自动保存功能启用
- 验证Hook正确安装
- 检查环境变量设置

---

## 诊断工具

### 健康检查脚本

```bash
#!/bin/bash
# session-health-check.sh

echo "🔍 会话系统健康检查"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. 存储空间检查
echo "📁 存储空间检查:"
du -sh .claude/state/session-saves/
df -h .claude/state/session-saves/

# 2. 文件完整性检查
echo "📋 文件完整性检查:"
find .claude/state/session-saves/ -name "*.gz" -exec gunzip -t {} \;

# 3. 配置验证
echo "⚙️ 配置验证:"
grep -A 20 "\[session\]" harness.toml

# 4. 权限检查
echo "🔒 权限检查:"
ls -la .claude/state/session-saves/

# 5. 日志分析
echo "📊 日志分析:"
tail -20 .claude/logs/session-save.log
```

### 性能分析脚本

```bash
#!/bin/bash
# session-perf-analysis.sh

echo "🚀 会话性能分析"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. 保存性能测试
echo "⏱️ 保存性能测试:"
time /harness-save-session "性能测试"

# 2. 恢复性能测试
echo "⏱️ 恢复性能测试:"
SAVE_ID=$(ls -t .claude/state/session-saves/ | head -1)
time /harness-restore-session "$SAVE_ID" --summary-only

# 3. 存储IO性能
echo "💾 存储IO性能:"
dd if=/dev/zero of=.claude/state/test-io.tmp bs=1M count=10
rm .claude/state/test-io.tmp
```

---

## 获取帮助

### 日志文件位置

- **保存日志**: `.claude/logs/session-save.log`
- **恢复日志**: `.claude/logs/session-restore.log`
- **清理日志**: `.claude/logs/session-cleanup.log`
- **Hook日志**: `.claude/logs/session-monitor.log`

### 调试模式

启用详细日志记录：

```toml
# 编辑 harness.toml:
[session]
logLevel = "DEBUG"
logSaveOperations = true
logRestoreOperations = true
logCleanupOperations = true
```

### 支持联系

如问题持续存在，请提供以下信息：

1. **系统信息**:
   ```bash
   uname -a
   java -version
   ```

2. **配置信息**:
   ```bash
   cat harness.toml
   env | grep HARNESS
   ```

3. **日志信息**:
   ```bash
   tail -100 .claude/logs/session-save.log
   tail -100 .claude/logs/session-restore.log
   ```

4. **存储状态**:
   ```bash
   ls -la .claude/state/session-saves/
   du -sh .claude/state/session-saves/
   ```