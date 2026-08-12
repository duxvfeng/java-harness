# 会话保存和恢复系统设计文档

**设计类型**: 功能增强设计  
**创建日期**: 2026-08-09  
**目标**: 解决大任务中 context 满的问题，实现会话持久化和恢复  
**设计师**: Claude (Harness Team)

---

## 1. 问题定义

### 核心问题
在处理大型任务时，Claude Code 的上下文窗口容易达到 token 限制，导致：
- 对话历史丢失
- 工作进度中断
- 需要重新建立上下文
- 降低工作效率

### 用户需求
用户希望在 context 满之前能够：
1. 自动保存当前会话状态
2. 在新对话中恢复工作进度
3. 智能决定恢复时需要的上下文详细程度

---

## 2. 设计方案概述

### 方案选择：轻量级 Hook 扩展（方案 A）

**核心策略**:
- 利用现有 Hook 系统实现自动化
- 项目本地存储（`.claude/state/session-saves/`）
- 完整保存 + 智能摘要恢复
- 技能命令接口

**关键特性**:
- Token 计数触发自动保存
- 完整状态持久化（对话、任务、文件、Git）
- AI 决策的恢复策略
- 无缝集成现有工作流

---

## 3. 架构设计

### 系统架构

```
Claude Code Session (Token Usage Monitoring)
         ↓
session-monitor Hook Extension (Token Counter + Trigger)
         ↓
session-save Hook (NEW) (Complete State Capture → Storage)
         ↓
.claude/state/session-saves/<timestamp>/
  ├── session.jsonl          # 完整对话历史
  ├── task-state.json        # 当前任务状态  
  ├── plans.md               # Plans.md 当前状态
  ├── modified-files.json     # 修改文件列表
  ├── git-state.json         # Git 分支和状态
  └── metadata.json          # 保存元数据
         ↓
session-init Hook Extension (Detect Saves → Generate Summary → Prompt)
```

### Hook 集成点

**现有 Hooks 扩展**:
- `session-monitor` → 添加 token 检测逻辑
- `session-init` → 添加恢复提示逻辑

**新增 Hooks**:
- `session-save` → 执行完整状态保存

---

## 4. 组件设计

### 核心组件

#### SessionSaveManager (新增类)
```java
public class SessionSaveManager {
    private final SessionConfig config;
    private final File sessionSavesDir;
    
    // 执行完整会话保存
    public SessionSaveResult saveSession(SessionContext context, String saveReason);
    
    // 列出所有可用的会话保存点
    public List<SessionMetadata> listSessionSaves();
    
    // 清理过期的会话保存
    public void cleanupOldSaves();
    
    // 生成会话摘要（用于恢复提示）
    public SessionSummary generateSummary(String saveId);
}
```

#### TokenMonitor (扩展 session-monitor)
```java
public class TokenMonitor {
    private final SessionConfig config;
    
    // 检查当前 token 使用率
    public TokenUsageInfo checkTokenUsage();
    
    // 判断是否应该触发保存
    public boolean shouldTriggerSave(int currentPercentage);
}
```

#### SessionRestorePrompt (扩展 session-init)
```java
public class SessionRestorePrompt {
    // 检查是否有可恢复的会话
    public Optional<RestoreSuggestion> checkRestoreOpportunity();
    
    // 生成恢复提示消息
    public String generateRestorePrompt(RestoreSuggestion suggestion);
    
    // AI 决策：是否需要详细上下文
    public boolean needsDetailedContext(SessionSummary summary);
}
```

### 数据结构

#### SessionMetadata 元数据
```json
{
  "saveId": "20260809-153045-token-85",
  "timestamp": "2026-08-09T15:30:45Z",
  "saveReason": "Token usage reached 85%",
  "tokenUsage": 85,
  "taskContext": {
    "currentPhase": "Phase 2",
    "completedTasks": [1.1, 1.2, 1.3],
    "currentTask": "2.1",
    "totalTasks": 25
  },
  "gitState": {
    "branch": "feature/session-save",
    "commit": "a850127",
    "modifiedFiles": 7,
    "uncommittedChanges": true
  },
  "summary": "Phase 1 已完成，正在执行 Phase 2 中文 README 创建，已完成 2.1-2.4 任务",
  "size": {
    "totalFiles": 5,
    "compressedSize": "2.3MB",
    "uncompressedSize": "8.7MB"
  }
}
```

#### SessionSummary 摘要
```json
{
  "saveId": "20260809-153045-token-85",
  "quickOverview": "Phase 1 文档清理完成，Phase 2 README 创建中 (60%)",
  "currentWork": "正在编写中文 README 功能特性列表 (Task 2.2)",
  "recentProgress": [
    "✅ 完成中文 README 项目概述 (Task 2.1)",
    "✅ 完成架构设计章节 (Task 2.3)",
    "🔄 正在执行功能特性列表 (Task 2.2)"
  ],
  "recommendation": "建议继续 Task 2.2，然后完成 Task 2.4-2.6",
  "aiDecision": {
    "needsDetailedContext": true,
    "reason": "复杂任务，需要恢复 Plans.md 状态和修改文件上下文"
  }
}
```

---

## 5. 命令接口设计

### 技能命令格式

遵循现有 harness 技能命名规范：
```bash
# 自动保存（Hook 系统处理，无需命令）
# 当 token 达到 80%/90% 时自动触发

# 手动保存
/harness-save-session              # 手动触发保存
/harness-save-session --force      # 强制保存（忽略间隔限制）
/harness-save-session --reason="里程碑完成"  # 自定义保存原因

# 恢复会话  
/harness-restore-session 20260809-153045-token-85    # 恢复特定会话
/harness-restore-session --latest                      # 恢复最新的会话

# 管理命令
/harness-list-sessions                                    # 列出所有保存
/harness-list-sessions --limit=5                         # 只显示最近 5 个
/harness-show-session 20260809-153045-token-85          # 显示详细信息
/harness-cleanup-sessions                                # 清理旧保存
/harness-cleanup-sessions --older-than=7d               # 清理 7 天前的保存
```

### 配置集成

在 `harness.toml` 中添加配置：
```toml
[session.auto_save]
enable = true                    # 启用自动保存
thresholds = [80, 90]           # 80% 和 90% 触发
max_saves = 10                   # 最多保留 10 个保存点
compression = true               # 压缩存储
save_interval = "5m"             # 最小保存间隔（避免频繁保存）

[session.restore]
auto_prompt = true               # 启动时自动提示
max_history_age = "7d"           # 恢复提示的历史范围

[session.storage]
max_total_size = "500MB"          # 总存储限制
max_single_save = "50MB"          # 单个保存最大大小
compression_level = "medium"       # 压缩级别: low, medium, high
async_save = true                  # 启用异步保存
incremental_save = true            # 启用增量保存
```

---

## 6. 保存流程设计

### 自动保存触发流程

1. **Token 检测** (`session-monitor` Hook)
   - 从 Claude 环境获取当前 token 使用率
   - 判断是否达到阈值 (80%, 90%)
   - 考虑最小保存间隔 (默认 5 分钟)

2. **状态收集** (`SessionSaveManager`)
   - 对话历史 → session.jsonl
   - 任务状态 → task-state.json (从 Plans.md 提取)
   - Plans.md → 直接复制
   - 修改文件 → 扫描 git status，收集变更文件
   - Git 状态 → branch, commit, status
   - 元数据 → 保存原因、时间、大小等

3. **压缩存储** (可选)
   - 使用 gzip 压缩大文件
   - 保持小文件（metadata.json）未压缩以便快速访问

4. **清理旧保存**
   - 保留最新的 N 个保存 (config.max_saves = 10)
   - 删除超过 7 天的保存
   - 记录清理日志

### 关键实现细节

#### 最小保存间隔检查
```java
public class SessionSaveManager {
    private Instant lastSaveTime = Instant.EPOCH;
    
    public boolean shouldSaveNow() {
        if (lastSaveTime.equals(Instant.EPOCH)) {
            return true;  // 从未保存过
        }
        
        Duration minInterval = config.getMinSaveInterval();
        Duration timeSinceLastSave = Duration.between(lastSaveTime, Instant.now());
        
        return timeSinceLastSave.compareTo(minInterval) >= 0;
    }
}
```

#### 修改文件收集
```java
public class ModifiedFilesCollector {
    public List<FileInfo> collectModifiedFiles() throws IOException {
        // 使用 git status 获取修改文件
        ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
        Process process = pb.start();
        
        List<FileInfo> modifiedFiles = new ArrayList<>();
        // 解析 git 输出，收集文件内容和路径
        
        return modifiedFiles;
    }
}
```

---

## 7. 恢复流程设计

### 恢复触发流程

1. **检测保存点** (`session-init` Hook)
   - 扫描 `.claude/state/session-saves/`
   - 按时间戳排序，找到最近的保存
   - 检查保存是否在时间范围内 (config.max_history_age = 7d)
   - 过滤已损坏或不完整的保存

2. **生成摘要** (`SessionSaveManager`)
   - 读取保存元数据
   - 分析任务进度和当前工作
   - 生成智能推荐和 AI 决策

3. **AI 决策** (`SessionRestorePrompt`)
   - 分析任务复杂度
   - 评估上下文重要性
   - 决定是否需要恢复完整上下文

4. **用户交互**
   - 显示恢复提示和选项
   - 等待用户选择恢复方式
   - 执行相应的恢复操作

### AI 决策逻辑

```java
public class SessionRestorePrompt {
    public boolean needsDetailedContext(SessionSummary summary) {
        // 决策因素
        int score = 0;
        
        // 1. 任务复杂度
        if (summary.getCurrentTask().contains("复杂重构")) score += 3;
        if (summary.getCurrentTask().contains("架构设计")) score += 2;
        
        // 2. 修改文件数量
        if (summary.getModifiedFilesCount() > 10) score += 2;
        if (summary.getModifiedFilesCount() > 5) score += 1;
        
        // 3. 未完成任务数量
        if (summary.getPendingTasksCount() > 15) score += 2;
        if (summary.getPendingTasksCount() > 7) score += 1;
        
        // 4. 时间跨度（超过1天的中断需要更多上下文）
        if (summary.getDaysSinceSave() > 1) score += 2;
        
        // 5. Git 状态复杂性
        if (summary.hasUncommittedChanges()) score += 1;
        if (summary.hasBranchConflicts()) score += 2;
        
        // 阈值：>= 4 分建议恢复完整上下文
        return score >= 4;
    }
}
```

### 恢复提示示例

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 检测到会话保存点                                        
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 保存时间: 2026-08-09 15:30:45                          
📊 Token 使用率: 85%                                     
🎯 当前任务: Phase 2 - 中文 README 创建 (Task 2.2)         

📝 工作摘要:                                              
✅ Phase 1 文档清理已完成                                   
🔄 Phase 2 README 创建中 (60%)                             
⏳ 待完成: Tasks 2.4-2.6 + Phase 3                         

🤖 AI 建议: 继续执行 Task 2.2，建议恢复完整上下文            

选择恢复选项:                                              
[1] 恢复完整上下文 (推荐)                                  
[2] 仅查看摘要，手动决定                                    
[3] 忽略，开始新会话                                        

或使用命令:                                                
/harness-restore-session 20260809-153045-token-85         
/harness-list-sessions                                    
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 8. 错误处理和边界情况

### 关键错误场景

#### 1. Token 检测失败
```java
public class TokenMonitor {
    public TokenUsageInfo checkTokenUsage() {
        try {
            int currentUsage = getCurrentTokenUsage();
            return new TokenUsageInfo(currentUsage, true);
        } catch (TokenDetectionException e) {
            // 降级处理：使用备用检测方法
            logger.warn("Primary token detection failed, using fallback", e);
            return fallbackTokenDetection();
        } catch (Exception e) {
            // 完全失败：记录错误但不中断会话
            logger.error("Token detection completely failed", e);
            return new TokenUsageInfo(-1, false); // -1 表示未知
        }
    }
}
```

#### 2. 保存空间不足
```java
public class SessionSaveManager {
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        try {
            // 检查磁盘空间
            if (!hasEnoughSpace()) {
                // 自动清理旧保存
                cleanupOldSaves();
                
                // 如果还是不够，警告用户
                if (!hasEnoughSpace()) {
                    logger.warn("Insufficient disk space for session save");
                    return SessionSaveResult.failed("磁盘空间不足");
                }
            }
            
            return performSave(context, reason);
        } catch (IOException e) {
            logger.error("Failed to save session", e);
            return SessionSaveResult.failed("保存失败: " + e.getMessage());
        }
    }
}
```

#### 3. 恢复文件损坏
```java
public class SessionRestoreManager {
    public RestoreResult restoreSession(String saveId) {
        try {
            // 验证保存完整性
            if (!validateSaveIntegrity(saveId)) {
                logger.warn("Save {} is corrupted, marking as invalid", saveId);
                markAsCorrupted(saveId);
                return RestoreResult.failed("保存文件已损坏");
            }
            
            return performRestore(saveId);
        } catch (Exception e) {
            logger.error("Failed to restore session {}", saveId, e);
            return RestoreResult.failed("恢复失败: " + e.getMessage());
        }
    }
}
```

### 边界情况处理

#### 并发保存
```java
public class SessionSaveManager {
    private final Object saveLock = new Object();
    
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        synchronized (saveLock) {
            // 防止并发保存冲突
            if (isSavingInProgress()) {
                logger.info("Save already in progress, skipping duplicate request");
                return SessionSaveResult.skipped("已有保存进行中");
            }
            
            setSavingInProgress(true);
            try {
                return performSave(context, reason);
            } finally {
                setSavingInProgress(false);
            }
        }
    }
}
```

#### 大文件处理
```java
public class ModifiedFilesCollector {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    public List<FileInfo> collectModifiedFiles() {
        List<FileInfo> files = new ArrayList<>();
        
        for (File file : getModifiedFiles()) {
            if (file.length() > MAX_FILE_SIZE) {
                logger.warn("File {} too large ({}MB), skipping", 
                    file.getName(), file.length() / (1024 * 1024));
                // 添加文件信息但不包含内容
                files.add(FileInfo.metadataOnly(file));
            } else {
                files.add(FileInfo.withContent(file));
            }
        }
        
        return files;
    }
}
```

### 降级策略

```java
public class SessionSaveManager {
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        // 尝试完整保存
        try {
            return performFullSave(context, reason);
        } catch (Exception e) {
            logger.warn("Full save failed, attempting minimal save", e);
            
            // 降级到最小保存
            try {
                return performMinimalSave(context, reason);
            } catch (Exception e2) {
                logger.error("Minimal save also failed", e2);
                return SessionSaveResult.failed("保存完全失败");
            }
        }
    }
    
    private SessionSaveResult performMinimalSave(SessionContext context, String reason) {
        // 只保存关键元数据
        MetadataOnlySave save = new MetadataOnlySave();
        save.setReason(reason);
        save.setTimestamp(Instant.now());
        save.setTaskSummary(context.getTaskSummary());
        save.setPlansMetadata(context.getPlansMetadata());
        
        return save.writeTo(metadataFile);
    }
}
```

---

## 9. 性能和存储优化

### 存储优化策略

#### 智能压缩
```java
public class SessionSaveManager {
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        // 根据文件类型选择压缩策略
        CompressionStrategy strategy = selectCompressionStrategy();
        
        // 文本文件：高压缩率
        // 二进制文件：低压缩率或跳过
        // 小文件(<1KB)：跳过压缩
        // 大文件(>1MB)：异步压缩
    }
}
```

#### 增量保存
```java
public class SessionSaveManager {
    private SessionSaveResult lastSave = null;
    
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        if (lastSave != null && shouldUseIncrementalSave()) {
            return performIncrementalSave(context, reason, lastSave);
        } else {
            return performFullSave(context, reason);
        }
    }
    
    private boolean shouldUseIncrementalSave() {
        // 如果距离上次保存时间很短(<5分钟)且变化不大
        Duration timeSinceLastSave = Duration.between(
            lastSave.timestamp(), Instant.now()
        );
        
        return timeSinceLastSave.compareTo(Duration.ofMinutes(5)) < 0 
            && estimatedChanges() < 10; // 变化文件少于10个
    }
}
```

#### 存储清理策略
```java
public class SessionCleanupManager {
    public void cleanupOldSaves(SessionConfig config) {
        List<SessionMetadata> allSaves = listAllSaves();
        
        // 多重清理策略
        cleanupByCount(allSaves, config.getMaxSaves());           // 按数量
        cleanupByAge(allSaves, config.getMaxAge());              // 按时间
        cleanupByStorageLimit(allSaves, config.getMaxStorage()); // 按存储空间
    }
}
```

### 性能优化

#### 异步保存
```java
public class SessionSaveManager {
    private final ExecutorService saveExecutor = 
        Executors.newSingleThreadExecutor();
    
    public void saveSessionAsync(SessionContext context, String reason) {
        saveExecutor.submit(() -> {
            try {
                SessionSaveResult result = saveSession(context, reason);
                logger.info("Async save completed: {}", result.getSaveId());
            } catch (Exception e) {
                logger.error("Async save failed", e);
            }
        });
    }
}
```

#### 缓存优化
```java
public class SessionRestoreManager {
    private final Cache<String, SessionSummary> summaryCache = 
        Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();
    
    public SessionSummary getSummary(String saveId) {
        return summaryCache.get(saveId, id -> {
            return loadSummaryFromDisk(id);
        });
    }
}
```

---

## 10. 实现计划

### 分阶段实现

#### Phase 1: 核心基础设施 (1-2天)
**目标**: 建立基础的会话保存和恢复机制

**任务**:
1. 创建 `SessionSaveManager` 和相关数据模型
2. 实现 `session-save` Hook
3. 实现基础存储结构和文件组织
4. 实现 `/harness-save-session` 手动命令

**验收标准**:
- [ ] 能够手动保存会话并验证文件完整性
- [ ] 保存目录结构正确，包含所有必需文件
- [ ] 元数据 JSON 格式正确且可解析

#### Phase 2: 自动保存触发 (1天)
**目标**: 实现 Token 检测和自动保存

**任务**:
1. 扩展 `session-monitor` Hook 添加 Token 检测
2. 实现自动保存触发逻辑
3. 添加阈值检查和最小间隔限制
4. 实现配置管理

**验收标准**:
- [ ] Token 达到阈值时自动触发保存
- [ ] 遵守最小保存间隔限制
- [ ] 配置文件参数正确生效

#### Phase 3: 恢复和提示系统 (1-2天)
**目标**: 实现会话恢复和智能提示

**任务**:
1. 扩展 `session-init` Hook 添加恢复提示
2. 实现所有恢复命令 (`/harness-restore-session` 等)
3. 实现 AI 决策逻辑和摘要生成
4. 添加恢复验证和错误处理

**验收标准**:
- [ ] 新会话启动时正确显示恢复提示
- [ ] 能够成功恢复完整的会话状态
- [ ] AI 决策逻辑合理，推荐准确

#### Phase 4: 优化和清理 (1天)
**目标**: 性能优化和存储管理

**任务**:
1. 实现压缩和增量保存
2. 添加自动清理策略
3. 实现性能监控和健康检查
4. 完善错误处理和边界情况

**验收标准**:
- [ ] 压缩后存储空间有效减少
- [ ] 自动清理策略正确执行
- [ ] 健康检查命令显示准确状态

---

## 11. 成功标准

### 功能完整性
- [ ] 支持完整的会话保存（对话历史、任务状态、文件、Git状态）
- [ ] 支持 Token 触发的自动保存
- [ ] 支持手动保存和恢复命令
- [ ] 支持智能恢复提示和 AI 决策
- [ ] 支持会话清理和健康管理

### 性能指标
- [ ] 保存时间 < 3 秒（典型会话）
- [ ] 恢复时间 < 5 秒（典型会话）
- [ ] 压缩率 > 70%（文本内容）
- [ ] 存储空间 < 10MB/会话（压缩后）

### 可靠性
- [ ] 保存成功率 > 99%
- [ ] 恢复成功率 > 98%（验证完整性后）
- [ ] 错误处理覆盖所有已知边界情况
- [ ] 降级策略在失败时正确工作

### 用户体验
- [ ] 恢复提示清晰且易于理解
- [ ] AI 决策准确率 > 80%
- [ ] 命令响应时间 < 500ms（非 I/O 操作）
- [ ] 错误消息友好且可操作

---

## 12. 风险和缓解措施

### 技术风险

#### 1. Token 检测准确性
**风险**: 可能无法准确获取 token 使用率
**缓解**: 实现多种检测方法，降级到估算

#### 2. 大文件处理
**风险**: 修改大型文件可能导致保存缓慢
**缓解**: 文件大小限制、异步处理、增量保存

#### 3. 跨平台兼容性
**风险**: 不同操作系统的文件系统差异
**缓解**: 使用跨平台的 Java API，充分测试

### 用户体验风险

#### 1. 频繁保存干扰
**风险**: 过于频繁的保存可能影响工作流
**缓解**: 最小间隔限制、智能触发条件

#### 2. 恢复上下文丢失
**风险**: 恢复后上下文不完整
**缓解**: 完整的保存验证、AI 决策优化

---

## 13. 技术实现要点

### 关键技术选择

1. **序列化**: 使用 Jackson JSON 进行对象序列化
2. **压缩**: 使用 GZIP 进行文件压缩
3. **并发**: 使用 synchronized 锁防止并发冲突
4. **缓存**: 使用 Caffeine 进行元数据缓存
5. **异步**: 使用 ExecutorService 进行异步保存

### 文件结构
```
.claude/state/session-saves/
├── 20260809-153045-token-85/
│   ├── session.jsonl          # 对话历史
│   ├── task-state.json        # 任务状态
│   ├── plans.md               # Plans.md 快照
│   ├── modified-files/
│   │   ├── file1.java
│   │   └── file2.md
│   ├── git-state.json         # Git 状态
│   └── metadata.json          # 元数据
├── 20260809-154512-milestone-complete/
└── .index.json                # 保存点索引
```

---

## 14. 测试策略

### 单元测试
- [ ] `SessionSaveManager` 保存/恢复逻辑
- [ ] `TokenMonitor` token 检测和触发
- [ ] `SessionRestorePrompt` AI 决策逻辑
- [ ] 压缩/解压缩功能
- [ ] 错误处理和降级策略

### 集成测试
- [ ] Hook 系统集成
- [ ] 端到端保存/恢复流程
- [ ] 跨平台兼容性
- [ ] 大文件和边界情况

### 性能测试
- [ ] 保存/恢复时间基准
- [ ] 压缩效果验证
- [ ] 存储空间使用
- [ ] 并发保存性能

---

## 15. 部署和监控

### 部署步骤
1. 更新 `harness.toml` 配置模板
2. 创建新技能 `skills/harness-session/`
3. 实现 Java 核心类
4. 添加 Hook 扩展
5. 编写测试和文档

### 监控指标
- 保存成功率/失败率
- 恢复成功率/失败率
- 平均保存/恢复时间
- 存储空间使用趋势
- Token 触发频率

### 日志记录
- 保存操作日志（成功/失败）
- 恢复操作日志（成功/失败）
- Token 检测日志
- 清理操作日志
- 性能指标日志

---

## 附录

### 相关文档
- [Harness 项目规格](../../../../spec.md)
- [Plans.md 当前状态](../../../../Plans.md)
- [Hook 系统文档](../../../../docs/reference/api-reference.md)

### 变更历史
- 2026-08-09: 初始设计创建

---

**设计状态**: ✅ 已完成，等待用户审查  
**下一步**: 用户审查后，创建实现计划（Plans.md）
