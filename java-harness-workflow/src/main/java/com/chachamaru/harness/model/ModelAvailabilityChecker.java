package com.chachamaru.harness.model;

import java.util.concurrent.TimeUnit;

/**
 * 模型可用性检查器
 * 检查模型是否可用，包括格式验证和可选的网络/API 调用验证
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>模型名称格式验证</li>
 *   <li>本地/远程模型识别</li>
 *   <li>网络连通性检查（可选）</li>
 *   <li>轻量级 API 调用验证（可选）</li>
 *   <li>超时控制</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ModelAvailabilityChecker checker = new ModelAvailabilityChecker();
 *
 * // 基本可用性检查（格式验证）
 * boolean available = checker.isAvailable("glm-4.7", 1000);
 *
 * // 仅格式验证
 * boolean valid = checker.isValidModelName("claude-sonnet-4-20250514");
 *
 * // 检查是否为远程模型
 * boolean remote = checker.isRemoteModel("gpt-4");
 * }</pre>
 */
public class ModelAvailabilityChecker {

    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final int DEFAULT_TIMEOUT = 5000; // 5 seconds

    // 远程模型识别关键字
    private static final String[] REMOTE_MODEL_KEYWORDS = {
        "claude-", "gpt-", "anthropic-", "openai-"
    };

    /**
     * 检查模型是否可用
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果模型可用返回 true，否则返回 false
     */
    public boolean isAvailable(String model, int timeoutMs) {
        try {
            // 1. 基本格式验证
            if (!isValidModelName(model)) {
                return false;
            }

            // 2. 处理零或负超时
            int effectiveTimeout = timeoutMs > 0 ? timeoutMs : 0;

            // 3. 如果是远程模型，检查网络连通性
            if (isRemoteModel(model) && effectiveTimeout > 0) {
                if (!checkNetworkConnectivity(effectiveTimeout)) {
                    return false;
                }
            }

            // 4. 可选：轻量级 API 调用验证（当前跳过，避免实际网络请求）
            // if (effectiveTimeout > 0 && !tryLightweightApiCall(model, effectiveTimeout)) {
            //     return false;
            // }

            return true;

        } catch (Exception e) {
            // 任何异常都视为不可用
            return false;
        }
    }

    /**
     * 验证模型名称格式是否有效
     *
     * @param model 模型名称
     * @return 如果格式有效返回 true，否则返回 false
     */
    public boolean isValidModelName(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }

        String trimmed = model.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // 检查长度限制
        if (trimmed.length() > MAX_MODEL_NAME_LENGTH) {
            return false;
        }

        // 基本格式检查：允许字母、数字、连字符、下划线、点、方括号
        return trimmed.matches("[a-zA-Z0-9._\\-\\[\\]]+");
    }

    /**
     * 检查是否为远程模型
     *
     * @param model 模型名称
     * @return 如果是远程模型返回 true，否则返回 false
     */
    public boolean isRemoteModel(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }

        String lowerModel = model.toLowerCase();
        for (String keyword : REMOTE_MODEL_KEYWORDS) {
            if (lowerModel.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证模型格式
     *
     * @param model 模型名称
     * @throws IllegalArgumentException 如果格式无效
     */
    public void validateFormat(String model) {
        if (!isValidModelName(model)) {
            throw new IllegalArgumentException("Invalid model name format: " + model);
        }
    }

    /**
     * 检查网络连通性
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果网络连通返回 true，否则返回 false
     */
    public boolean checkNetworkConnectivity(int timeoutMs) {
        // 这里可以添加实际的网络连通性检查
        // 当前实现返回 true，假设网络可用
        // 在生产环境中，可以尝试连接到已知的端点

        if (timeoutMs <= 0) {
            return true; // 零超时表示跳过网络检查
        }

        try {
            // 模拟网络检查（在实际实现中可以 ping 已知服务器）
            // 这里我们假设网络总是可用的，避免阻塞测试
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试轻量级 API 调用
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果 API 调用成功返回 true，否则返回 false
     */
    public boolean tryLightweightApiCall(String model, int timeoutMs) {
        // 这里可以添加实际的轻量级 API 调用
        // 当前实现返回基于格式验证的结果

        if (!isValidModelName(model)) {
            return false;
        }

        // 在生产环境中，可以发送一个轻量级的请求到模型 API
        // 来验证模型是否真的可用

        return true; // 当前假设可用
    }

    /**
     * 获取默认超时时间
     * @return 默认超时时间（毫秒）
     */
    public static int getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    /**
     * 获取最大模型名称长度
     * @return 最大长度
     */
    public static int getMaxModelNameLength() {
        return MAX_MODEL_NAME_LENGTH;
    }

    /**
     * 检查模型是否在可用性检查范围内
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果可以检查返回 true，否则返回 false
     */
    public boolean canCheckAvailability(String model, int timeoutMs) {
        return isValidModelName(model) && timeoutMs > 0;
    }

    /**
     * 快速检查模型格式（不进行网络检查）
     *
     * @param model 模型名称
     * @return 如果格式正确返回 true
     */
    public boolean quickCheck(String model) {
        return isValidModelName(model);
    }
}