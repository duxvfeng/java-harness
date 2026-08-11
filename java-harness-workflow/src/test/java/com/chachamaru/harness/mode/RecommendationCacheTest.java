package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

/**
 * 推荐缓存和学习机制的单元测试
 * 验证缓存性能、用户反馈记录、学习优化的正确性
 */
@DisplayName("推荐缓存和学习机制测试")
class RecommendationCacheTest {

    @Test
    @DisplayName("应该能够缓存推荐结果")
    void shouldCacheRecommendationResults() {
        RecommendationCache cache = new RecommendationCache();

        List<String> tasks = List.of("fix typo in README");
        List<String> files = List.of("README.md");

        // 首次推荐应该计算
        ModeRecommendation rec1 = cache.getOrCompute(tasks, files, () -> {
            return new ModeRecommendation(
                ExecutionMode.SOLO,
                0.9,
                "简单文档任务",
                List.of()
            );
        });

        assertNotNull(rec1);
        assertFalse(cache.wasCached(), "首次访问应该不是缓存命中");

        // 再次推荐应该从缓存读取
        ModeRecommendation rec2 = cache.getOrCompute(tasks, files, () -> {
            fail("不应该执行计算，应该从缓存读取");
            return null;
        });

        assertNotNull(rec2);
        assertTrue(cache.wasCached(), "第二次访问应该是缓存命中");
        assertEquals(rec1.recommendedMode(), rec2.recommendedMode());
        assertEquals(rec1.confidence(), rec2.confidence(), 0.001);
    }

    @Test
    @DisplayName("应该能够记录用户接受推荐的反馈")
    void shouldRecordUserAcceptanceFeedback() {
        RecommendationCache cache = new RecommendationCache();
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();

        List<String> tasks = List.of("add unit tests");
        List<String> files = List.of("test.java");

        // 生成推荐
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.85,
            "单个测试任务",
            List.of(ExecutionMode.PARALLEL)
        );

        // 记录用户接受反馈
        recorder.recordAcceptance(tasks, files, recommendation);

        // 验证反馈被记录
        Optional<UserFeedback> feedback = recorder.getFeedback(tasks, files);
        assertTrue(feedback.isPresent(), "反馈应该被记录");
        assertTrue(feedback.get().wasAccepted(), "应该标记为已接受");
        assertEquals(recommendation.recommendedMode(), feedback.get().selectedMode());
    }

    @Test
    @DisplayName("应该能够记录用户拒绝推荐的反馈")
    void shouldRecordUserRejectionFeedback() {
        RecommendationCache cache = new RecommendationCache();
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();

        List<String> tasks = List.of("update documentation");
        List<String> files = List.of("docs/README.md");

        // 生成推荐
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "中等复杂度任务",
            List.of(ExecutionMode.SOLO)
        );

        // 用户选择了其他模式
        ExecutionMode selectedMode = ExecutionMode.SOLO;

        // 记录用户拒绝反馈
        recorder.recordRejection(tasks, files, recommendation, selectedMode);

        // 验证反馈被记录
        Optional<UserFeedback> feedback = recorder.getFeedback(tasks, files);
        assertTrue(feedback.isPresent(), "反馈应该被记录");
        assertFalse(feedback.get().wasAccepted(), "应该标记为已拒绝");
        assertEquals(selectedMode, feedback.get().selectedMode());
    }

    @Test
    @DisplayName("缓存应该支持LRU淘汰策略")
    void cacheShouldSupportLRUEviction() {
        RecommendationCache cache = new RecommendationCache(3); // 最大3个条目

        // 添加4个不同的任务
        cache.getOrCompute(List.of("task1"), List.of("file1"), () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "task1", List.of()));

        cache.getOrCompute(List.of("task2"), List.of("file2"), () ->
            new ModeRecommendation(ExecutionMode.PARALLEL, 0.8, "task2", List.of()));

        cache.getOrCompute(List.of("task3"), List.of("file3"), () ->
            new ModeRecommendation(ExecutionMode.BREEZING, 0.7, "task3", List.of()));

        // 第4个任务应该导致第1个被淘汰
        cache.getOrCompute(List.of("task4"), List.of("file4"), () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "task4", List.of()));

        // 验证task1已不在缓存中
        assertFalse(cache.containsKey(List.of("task1"), List.of("file1")),
            "task1应该已被LRU淘汰");

        // 验证其他任务仍在缓存中
        assertTrue(cache.containsKey(List.of("task2"), List.of("file2")),
            "task2应该仍在缓存中");
    }

    @Test
    @DisplayName("应该能够基于用户反馈优化推荐权重")
    void shouldOptimizeWeightsBasedOnUserFeedback() {
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();
        WeightOptimizer optimizer = new WeightOptimizer();

        // 记录多个用户反馈模式
        List<String> tasks1 = List.of("simple task");
        List<String> files1 = List.of("file.java");

        recorder.recordAcceptance(tasks1, files1, new ModeRecommendation(
            ExecutionMode.SOLO, 0.8, "简单任务", List.of()
        ));

        // 模拟用户更倾向于SOLO模式的反馈
        for (int i = 0; i < 5; i++) {
            List<String> tasks = List.of("task " + i);
            List<String> files = List.of("file" + i + ".java");

            recorder.recordRejection(tasks, files,
                new ModeRecommendation(ExecutionMode.PARALLEL, 0.7, "中等任务", List.of()),
                ExecutionMode.SOLO
            );
        }

        // 获取优化后的权重
        ScoringWeights optimizedWeights = optimizer.optimizeWeights(recorder.getFeedbackHistory());

        assertNotNull(optimizedWeights);
        // 验证权重调整（用户倾向于SOLO应该增加简单任务的权重）
        assertTrue(optimizedWeights.taskCountWeight() > 0 ||
                   optimizedWeights.complexityWeight() > 0,
            "优化后的权重应该反映用户偏好");
    }

    @Test
    @DisplayName("应该能够持久化学习结果")
    void shouldPersistLearningResults() {
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();
        LearningPersistence persistence = new LearningPersistence();

        List<String> tasks = List.of("important task");
        List<String> files = List.of("important.java");

        recorder.recordAcceptance(tasks, files, new ModeRecommendation(
            ExecutionMode.BREEZING, 0.9, "重要任务", List.of()
        ));

        // 持久化学习结果
        persistence.saveLearningData(recorder.getFeedbackHistory());

        // 清除内存数据
        recorder.clear();

        // 加载持久化的数据
        UserFeedbackHistory loadedHistory = persistence.loadLearningData();

        assertNotNull(loadedHistory);
        assertFalse(loadedHistory.isEmpty(), "加载的历史记录不应该为空");

        // 验证数据完整性
        Optional<UserFeedback> feedback = loadedHistory.findFeedback(tasks, files);
        assertTrue(feedback.isPresent(), "持久化的反馈应该能被找到");
        assertTrue(feedback.get().wasAccepted(), "加载的反馈应该保持正确性");
    }

    @Test
    @DisplayName("缓存应该提供命中率统计")
    void cacheShouldProvideHitRateStatistics() {
        RecommendationCache cache = new RecommendationCache();

        List<String> tasks = List.of("cached task");
        List<String> files = List.of("file.java");

        // 首次访问 - 缓存未命中
        cache.getOrCompute(tasks, files, () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "cached", List.of()));

        assertEquals(0, cache.getHitCount(), "首次访问应该没有命中");
        assertEquals(1, cache.getMissCount(), "首次访问应该记录为未命中");

        // 再次访问 - 缓存命中
        cache.getOrCompute(tasks, files, () -> {
            fail("不应该执行计算");
            return null;
        });

        assertEquals(1, cache.getHitCount(), "第二次访问应该命中");
        assertEquals(1, cache.getMissCount(), "未命中计数应该保持不变");

        // 计算命中率
        double hitRate = cache.getHitRate();
        assertEquals(0.5, hitRate, 0.001, "命中率应该是50%");
    }

    @Test
    @DisplayName("应该能够清除过期缓存")
    void shouldClearExpiredCacheEntries() {
        RecommendationCache cache = new RecommendationCache();

        // 添加一些缓存条目
        cache.getOrCompute(List.of("task1"), List.of("file1"), () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "task1", List.of()));

        // 模拟时间流逝（在实际实现中会基于时间戳）
        // cache.simulateTimePassing(1000);

        cache.clearExpired();

        // 验证过期条目被清除
        assertFalse(cache.containsKey(List.of("task1"), List.of("file1")),
            "过期条目应该被清除");
    }

    @Test
    @DisplayName("学习机制应该支持渐进式优化")
    void learningMechanismShouldSupportProgressiveOptimization() {
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();
        AdaptiveLearner learner = new AdaptiveLearner();

        // 初始权重
        ScoringWeights initialWeights = ScoringWeights.DEFAULT;
        learner.setInitialWeights(initialWeights);

        // 逐步提供反馈
        for (int i = 0; i < 10; i++) {
            List<String> tasks = List.of("task " + i);
            List<String> files = List.of("file" + i + ".java");

            if (i < 7) {
                // 前7次用户倾向于SOLO
                recorder.recordRejection(tasks, files,
                    new ModeRecommendation(ExecutionMode.PARALLEL, 0.7, "task", List.of()),
                    ExecutionMode.SOLO
                );
            } else {
                // 后3次用户接受PARALLEL
                recorder.recordAcceptance(tasks, files,
                    new ModeRecommendation(ExecutionMode.PARALLEL, 0.8, "task", List.of())
                );
            }

            // 每次反馈后逐步优化
            if (i % 3 == 0) {
                ScoringWeights updatedWeights = learner.learnFromFeedback(recorder.getFeedbackHistory());
                assertNotNull(updatedWeights);
            }
        }

        // 最终优化结果
        ScoringWeights finalWeights = learner.getCurrentWeights();
        assertNotNull(finalWeights);

        // 验证权重发生了调整
        assertTrue(finalWeights.taskCountWeight() != initialWeights.taskCountWeight() ||
                   finalWeights.complexityWeight() != initialWeights.complexityWeight(),
            "权重应该根据用户反馈进行渐进式调整");
    }

    @Test
    @DisplayName("应该能够处理缓存键冲突")
    void shouldHandleCacheKeyCollisions() {
        RecommendationCache cache = new RecommendationCache();

        List<String> tasks1 = List.of("same task");
        List<String> files1 = List.of("same file");

        List<String> tasks2 = List.of("same task");
        List<String> files2 = List.of("same file");

        // 第一次缓存
        ModeRecommendation rec1 = cache.getOrCompute(tasks1, files1, () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "first", List.of()));

        // 第二次相同键
        ModeRecommendation rec2 = cache.getOrCompute(tasks2, files2, () ->
            new ModeRecommendation(ExecutionMode.PARALLEL, 0.8, "second", List.of()));

        // 应该返回缓存的第一个结果
        assertEquals(rec1.recommendedMode(), rec2.recommendedMode(),
            "相同键应该返回相同的缓存结果");
        assertEquals(ExecutionMode.SOLO, rec2.recommendedMode(),
            "应该使用第一个缓存的结果");
    }

    @Test
    @DisplayName("缓存应该支持手动清空")
    void cacheShouldSupportManualClear() {
        RecommendationCache cache = new RecommendationCache();

        // 添加缓存
        cache.getOrCompute(List.of("task1"), List.of("file1"), () ->
            new ModeRecommendation(ExecutionMode.SOLO, 0.9, "task1", List.of()));

        assertTrue(cache.containsKey(List.of("task1"), List.of("file1")),
            "缓存应该包含添加的条目");

        // 手动清空
        cache.clear();

        assertFalse(cache.containsKey(List.of("task1"), List.of("file1")),
            "清空后缓存应该为空");
        assertEquals(0, cache.size(), "缓存大小应该为0");
    }

    @Test
    @DisplayName("应该能够分析用户模式偏好")
    void shouldAnalyzeUserModePreferences() {
        UserFeedbackRecorder recorder = new UserFeedbackRecorder();
        PreferenceAnalyzer analyzer = new PreferenceAnalyzer();

        // 记录多种用户选择
        for (int i = 0; i < 10; i++) {
            List<String> tasks = List.of("task " + i);
            List<String> files = List.of("file" + i + ".java");

            switch (i % 3) {
                case 0:
                    recorder.recordAcceptance(tasks, files,
                        new ModeRecommendation(ExecutionMode.SOLO, 0.8, "task", List.of()));
                    break;
                case 1:
                    recorder.recordAcceptance(tasks, files,
                        new ModeRecommendation(ExecutionMode.PARALLEL, 0.7, "task", List.of()));
                    break;
                case 2:
                    recorder.recordRejection(tasks, files,
                        new ModeRecommendation(ExecutionMode.BREEZING, 0.6, "task", List.of()),
                        ExecutionMode.SOLO
                    );
                    break;
            }
        }

        // 分析用户偏好
        UserPreferences preferences = analyzer.analyzePreferences(recorder.getFeedbackHistory());

        assertNotNull(preferences);
        assertTrue(preferences.getMostPreferredMode().isPresent(),
            "应该能识别用户最偏好的模式");

        // 验证偏好统计
        assertTrue(preferences.getModeSelectionCount(ExecutionMode.SOLO) > 0,
            "应该统计SOLO模式的选择次数");
    }

    @Test
    @DisplayName("学习数据应该支持版本控制")
    void learningDataShouldSupportVersioning() {
        LearningPersistence persistence = new LearningPersistence();

        // 创建初始版本
        UserFeedbackHistory v1 = new UserFeedbackHistory();
        persistence.saveLearningData(v1, "v1");

        // 创建新版本
        UserFeedbackHistory v2 = new UserFeedbackHistory();
        persistence.saveLearningData(v2, "v2");

        // 验证版本控制
        assertTrue(persistence.versionExists("v1"), "v1版本应该存在");
        assertTrue(persistence.versionExists("v2"), "v2版本应该存在");

        // 加载特定版本
        UserFeedbackHistory loaded = persistence.loadVersion("v1");
        assertNotNull(loaded, "应该能加载特定版本");
    }
}