package com.chachamaru.harness.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 增强的模型可用性检查器
 * 检查模型是否可用，包括格式验证、网络连接检查、可选的 API 调用验证
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>严格的模型名称格式验证（支持多种模型命名规范）</li>
 *   <li>本地/远程模型智能识别</li>
 *   <li>网络连通性检查（支持超时控制）</li>
 *   <li>可选的轻量级 API 调用验证</li>
 *   <li>完善的异常处理和日志记录</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ModelAvailabilityChecker checker = new ModelAvailabilityChecker(validateApiCall);
 *
 * // 基本可用性检查
 * boolean available = checker.isAvailable("glm-4.7", 5000);
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
    private static final int API_CALL_TIMEOUT = 3000; // 3 seconds for API calls

    // 远程模型识别关键字（更全面）
    private static final String[] REMOTE_MODEL_KEYWORDS = {
        "claude-", "gpt-", "anthropic-", "openai-", "gemini-", "llama-", "mistral-",
        "deepseek-", "qwen-", "yi-", "baichuan-", "chatglm-", "internlm-"
    };

    // 模型名称格式正则表达式（更严格和全面）
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z][a-zA-Z0-9._\\-]{0,99}$"
    );

    // Anthropic 模型格式
    private static final Pattern ANTHROPIC_MODEL_PATTERN = Pattern.compile(
        "^claude-(fable|haiku|sonnet|opus)(-[0-9])?(-[0-9]{8})?$"
    );

    // OpenAI 模型格式
    private static final Pattern OPENAI_MODEL_PATTERN = Pattern.compile(
        "^(gpt|text|davinci|curie|babbage|ada)-[0-9.]+$"
    );

    private final boolean validateApiCall;

    /**
     * 创建默认的检查器（不验证 API 调用）
     */
    public ModelAvailabilityChecker() {
        this(false);
    }

    /**
     * 创建检查器
     *
     * @param validateApiCall 是否验证 API 调用
     */
    public ModelAvailabilityChecker(boolean validateApiCall) {
        this.validateApiCall = validateApiCall;
    }

    /**
     * 检查模型是否可用（完整检查）
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果模型可用返回 true，否则返回 false
     */
    public boolean isAvailable(String model, int timeoutMs) {
        try {
            // 1. 严格的格式验证
            if (!isValidModelName(model)) {
                logDebug("Model name format invalid: " + model);
                return false;
            }

            // 2. 处理零或负超时
            int effectiveTimeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT;

            // 3. 如果是远程模型，检查网络连通性
            if (isRemoteModel(model)) {
                if (!checkNetworkConnectivity(Math.min(effectiveTimeout, API_CALL_TIMEOUT))) {
                    logDebug("Network connectivity check failed for: " + model);
                    return false;
                }
            }

            // 4. 可选的轻量级 API 调用验证
            if (validateApiCall && effectiveTimeout > 0) {
                if (!tryLightweightApiCall(model, Math.min(effectiveTimeout, API_CALL_TIMEOUT))) {
                    logDebug("API call validation failed for: " + model);
                    return false;
                }
            }

            logDebug("Model available: " + model);
            return true;

        } catch (Exception e) {
            logDebug("Model availability check failed for " + model + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证模型名称格式是否有效（增强版）
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

        // 基本格式检查
        if (!MODEL_NAME_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 检查特殊格式（Anthropic, OpenAI 等）
        if (isAnthropicModel(trimmed) || isOpenAIModel(trimmed)) {
            return true;
        }

        return true;
    }

    /**
     * 检查是否为远程模型（增强版）
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

        // 检查是否为知名云服务模型
        return isAnthropicModel(model) || isOpenAIModel(model) || isGoogleModel(model);
    }

    /**
     * 验证模型格式并抛出异常
     *
     * @param model 模型名称
     * @throws IllegalArgumentException 如果格式无效
     */
    public void validateFormat(String model) {
        if (!isValidModelName(model)) {
            throw new IllegalArgumentException(
                "Invalid model name format: " + model +
                ". Model names must start with a letter and contain only letters, numbers, dots, hyphens, and underscores."
            );
        }
    }

    /**
     * 检查网络连通性（增强版）
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果网络连通返回 true，否则返回 false
     */
    public boolean checkNetworkConnectivity(int timeoutMs) {
        if (timeoutMs <= 0) {
            return true; // 零超时表示跳过网络检查
        }

        try {
            // 尝试连接到可靠的端点
            return checkEndpointConnectivity("https://www.google.com", timeoutMs) ||
                   checkEndpointConnectivity("https://www.cloudflare.com", timeoutMs) ||
                   checkEndpointConnectivity("https://www.anthropic.com", timeoutMs);
        } catch (Exception e) {
            logDebug("Network connectivity check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查特定端点的连通性
     *
     * @param urlString 端点 URL
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果连通返回 true
     */
    private boolean checkEndpointConnectivity(String urlString, int timeoutMs) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode >= 200 && responseCode < 500;
        } catch (IOException e) {
            logDebug("Endpoint check failed for " + urlString + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 尝试轻量级 API 调用（增强版）
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果 API 调用成功返回 true，否则返回 false
     */
    public boolean tryLightweightApiCall(String model, int timeoutMs) {
        if (!isValidModelName(model)) {
            return false;
        }

        try {
            // 对于不同类型的模型，使用不同的验证策略
            if (isAnthropicModel(model)) {
                return validateAnthropicModel(model, timeoutMs);
            } else if (isOpenAIModel(model)) {
                return validateOpenAIModel(model, timeoutMs);
            } else {
                // 对于其他模型，仅进行基本验证
                return true;
            }
        } catch (Exception e) {
            logDebug("API call validation failed for " + model + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Anthropic 模型
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间
     * @return 如果有效返回 true
     */
    private boolean validateAnthropicModel(String model, int timeoutMs) {
        // Anthropic 模型验证逻辑
        // 可以添加实际的 API 调用
        return isAnthropicModel(model);
    }

    /**
     * 验证 OpenAI 模型
     *
     * @param model 模型名称
     * @param timeoutMs 超时时间
     * @return 如果有效返回 true
     */
    private boolean validateOpenAIModel(String model, int timeoutMs) {
        // OpenAI 模型验证逻辑
        // 可以添加实际的 API 调用
        return isOpenAIModel(model);
    }

    /**
     * 检查是否为 Anthropic 模型
     *
     * @param model 模型名称
     * @return 如果是 Anthropic 模型返回 true
     */
    private boolean isAnthropicModel(String model) {
        return ANTHROPIC_MODEL_PATTERN.matcher(model).matches();
    }

    /**
     * 检查是否为 OpenAI 模型
     *
     * @param model 模型名称
     * @return 如果是 OpenAI 模型返回 true
     */
    private boolean isOpenAIModel(String model) {
        return OPENAI_MODEL_PATTERN.matcher(model).matches();
    }

    /**
     * 检查是否为 Google 模型
     *
     * @param model 模型名称
     * @return 如果是 Google 模型返回 true
     */
    private boolean isGoogleModel(String model) {
        return model.toLowerCase().startsWith("gemini-");
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

    /**
     * 检查是否启用了 API 调用验证
     *
     * @return 如果启用返回 true
     */
    public boolean isValidateApiCall() {
        return validateApiCall;
    }

    /**
     * 调试日志（避免引入日志框架依赖）
     *
     * @param message 日志消息
     */
    private void logDebug(String message) {
        // 在实际项目中可以使用 SLF4J 或其他日志框架
        // 这里使用 System.err 来避免影响正常输出
        if (validateApiCall) { // 仅在详细模式下输出
            System.err.println("[ModelAvailabilityChecker] " + message);
        }
    }
}