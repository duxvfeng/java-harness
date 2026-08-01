package com.chachamaru.harness.foundation.sync;

import java.nio.file.Path;
import java.util.Map;

/**
 * 状态同步引擎接口
 * 提供状态与 Plans.md 之间的双向同步功能
 */
public interface StateSynchronizationEngine {

    /**
     * 将状态同步到 Plans.md
     *
     * @param state 状态对象
     * @param plansMdPath Plans.md 文件路径
     * @return 同步结果
     * @throws SyncException 同步失败时抛出
     */
    SyncResult syncToPlans(StateSnapshot state, Path plansMdPath) throws SyncException;

    /**
     * 从 Plans.md 同步状态
     *
     * @param plansMdPath Plans.md 文件路径
     * @return 同步结果
     * @throws SyncException 同步失败时抛出
     */
    SyncResult syncFromPlans(Path plansMdPath) throws SyncException;

    /**
     * 执行双向同步（状态和 Plans.md 都可能被更新）
     *
     * @param state 状态对象
     * @param plansMdPath Plans.md 文件路径
     * @return 同步结果
     * @throws SyncException 同步失败时抛出
     */
    SyncResult bidirectionalSync(StateSnapshot state, Path plansMdPath) throws SyncException;

    /**
     * 检测 Plans.md 是否有未同步的变更
     *
     * @param plansMdPath Plans.md 文件路径
     * @param lastKnownHash 上次已知的 Plans.md 哈希值
     * @return 如果有变更返回 true
     */
    boolean hasUnsyncedChanges(Path plansMdPath, String lastKnownHash);

    /**
     * 获取 Plans.md 的当前哈希值
     *
     * @param plansMdPath Plans.md 文件路径
     * @return 文件内容的哈希值
     * @throws SyncException 计算哈希失败时抛出
     */
    String getPlansHash(Path plansMdPath) throws SyncException;
}
