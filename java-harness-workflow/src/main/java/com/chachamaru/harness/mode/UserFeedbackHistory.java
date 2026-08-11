package com.chachamaru.harness.mode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户反馈历史
 * 管理和分析用户反馈记录
 */
public record UserFeedbackHistory(List<UserFeedback> feedbacks) implements Serializable {

    /**
     * 序列化版本UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建空的反馈历史
     */
    public UserFeedbackHistory() {
        this(new ArrayList<>());
    }

    /**
     * 创建反馈历史
     */
    public UserFeedbackHistory {
        feedbacks = new ArrayList<>(feedbacks != null ? feedbacks : List.of());
    }

    /**
     * 查找指定任务和文件的反馈
     */
    public Optional<UserFeedback> findFeedback(List<String> tasks, List<String> files) {
        return feedbacks.stream()
            .filter(fb -> fb.tasks().equals(tasks) && fb.files().equals(files))
            .findFirst();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return feedbacks.isEmpty();
    }

    /**
     * 获取反馈数量
     */
    public int size() {
        return feedbacks.size();
    }

    /**
     * 获取所有接受的反馈
     */
    public List<UserFeedback> getAcceptedFeedbacks() {
        return feedbacks.stream()
            .filter(UserFeedback::wasAccepted)
            .toList();
    }

    /**
     * 获取所有拒绝的反馈
     */
    public List<UserFeedback> getRejectedFeedbacks() {
        return feedbacks.stream()
            .filter(UserFeedback::wasRejected)
            .toList();
    }

    /**
     * 获取指定模式的反馈数量
     */
    public int getModeSelectionCount(ExecutionMode mode) {
        return (int) feedbacks.stream()
            .filter(fb -> fb.selectedMode() == mode)
            .count();
    }

    /**
     * 添加反馈记录
     */
    public UserFeedbackHistory addFeedback(UserFeedback feedback) {
        List<UserFeedback> newFeedbacks = new ArrayList<>(this.feedbacks);
        newFeedbacks.add(feedback);
        return new UserFeedbackHistory(newFeedbacks);
    }
}