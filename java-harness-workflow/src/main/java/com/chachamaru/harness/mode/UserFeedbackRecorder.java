package com.chachamaru.harness.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户反馈记录器
 * 记录和管理用户对推荐结果的反馈
 */
public class UserFeedbackRecorder {

    private final List<UserFeedback> feedbackHistory;

    /**
     * 创建反馈记录器
     */
    public UserFeedbackRecorder() {
        this.feedbackHistory = new ArrayList<>();
    }

    /**
     * 记录用户接受推荐
     * @param tasks 任务列表
     * @param files 文件列表
     * @param recommendation 推荐结果
     */
    public void recordAcceptance(
        List<String> tasks,
        List<String> files,
        ModeRecommendation recommendation
    ) {
        UserFeedback feedback = UserFeedback.accepted(tasks, files, recommendation);
        feedbackHistory.add(feedback);
    }

    /**
     * 记录用户拒绝推荐
     * @param tasks 任务列表
     * @param files 文件列表
     * @param recommendation 推荐结果
     * @param selectedMode 用户选择的模式
     */
    public void recordRejection(
        List<String> tasks,
        List<String> files,
        ModeRecommendation recommendation,
        ExecutionMode selectedMode
    ) {
        UserFeedback feedback = UserFeedback.rejected(tasks, files, recommendation, selectedMode);
        feedbackHistory.add(feedback);
    }

    /**
     * 获取指定任务和文件的反馈
     * @param tasks 任务列表
     * @param files 文件列表
     * @return 反馈记录（如果存在）
     */
    public Optional<UserFeedback> getFeedback(List<String> tasks, List<String> files) {
        return feedbackHistory.stream()
            .filter(fb -> fb.tasks().equals(tasks) && fb.files().equals(files))
            .findFirst();
    }

    /**
     * 获取所有反馈历史
     * @return 反馈历史记录
     */
    public UserFeedbackHistory getFeedbackHistory() {
        return new UserFeedbackHistory(feedbackHistory);
    }

    /**
     * 清除所有反馈记录
     */
    public void clear() {
        feedbackHistory.clear();
    }

    /**
     * 获取反馈记录数量
     * @return 反馈数量
     */
    public int getFeedbackCount() {
        return feedbackHistory.size();
    }

    /**
     * 判断是否有反馈记录
     * @return 是否有反馈记录
     */
    public boolean hasFeedback() {
        return !feedbackHistory.isEmpty();
    }
}