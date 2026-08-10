package com.chachamaru.harness.model;

/**
 * 环境变量引用解析器
 * 支持解析模型引用，包括环境变量引用（env:VAR_NAME）和直接模型名称
 *
 * <p>解析规则：</p>
 * <ul>
 *   <li>环境变量引用：以 "env:" 开头，解析为对应的环境变量值</li>
 *   <li>直接模型名称：不以 "env:" 开头，原样返回</li>
 * </ul>
 *
 * <p>错误处理：</p>
 * <ul>
 *   <li>环境变量不存在：抛出 ConfigException</li>
 *   <li>环境变量值为空或仅空格：抛出 ConfigException</li>
 *   <li>输入为 null 或空：抛出 IllegalArgumentException</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ModelReferenceResolver resolver = new ModelReferenceResolver();
 *
 * // 直接模型名称
 * String model1 = resolver.resolve("glm-4.7"); // "glm-4.7"
 *
 * // 环境变量引用
 * System.setProperty("ANTHROPIC_DEFAULT_FABLE_MODEL", "claude-fable-5");
 * String model2 = resolver.resolve("env:ANTHROPIC_DEFAULT_FABLE_MODEL"); // "claude-fable-5"
 *
 * // 错误情况
 * resolver.resolve("env:NON_EXISTENT"); // 抛出 ConfigException
 * }</pre>
 */
public class ModelReferenceResolver {

    private static final String ENV_PREFIX = "env:";

    /**
     * 解析模型引用
     *
     * @param reference 模型引用（环境变量引用或直接模型名称）
     * @return 解析后的模型名称
     * @throws IllegalArgumentException 如果输入为 null 或空
     * @throws ConfigException 如果环境变量引用无效或环境变量不存在/为空
     */
    public String resolve(String reference) {
        // 验证输入
        if (reference == null) {
            throw new IllegalArgumentException("Model reference cannot be null");
        }

        String trimmed = reference.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Model reference cannot be empty or whitespace only");
        }

        // 检查是否为环境变量引用
        if (isEnvReference(trimmed)) {
            return resolveEnvReference(trimmed);
        }

        // 直接模型名称，原样返回
        return trimmed;
    }

    /**
     * 检查是否为环境变量引用
     *
     * @param reference 模型引用
     * @return 如果以 "env:" 开头返回 true，否则返回 false
     */
    public boolean isEnvReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        return reference.startsWith(ENV_PREFIX);
    }

    /**
     * 解析环境变量引用
     *
     * @param reference 环境变量引用（格式：env:VAR_NAME）
     * @return 环境变量的值
     * @throws ConfigException 如果环境变量不存在或值为空
     */
    private String resolveEnvReference(String reference) {
        String envVarName = extractEnvVariableName(reference);
        String envValue = System.getenv(envVarName);

        if (envValue == null) {
            throw new ConfigException(envVarName,
                    "Environment variable not found: " + envVarName);
        }

        String trimmedValue = envValue.trim();
        if (trimmedValue.isEmpty()) {
            throw new ConfigException(envVarName,
                    "Environment variable '" + envVarName + "' is empty");
        }

        return trimmedValue;
    }

    /**
     * 从环境变量引用中提取环境变量名称
     *
     * @param reference 环境变量引用（格式：env:VAR_NAME）
     * @return 环境变量名称
     * @throws IllegalArgumentException 如果格式无效
     */
    public String extractEnvVariableName(String reference) {
        if (!isEnvReference(reference)) {
            throw new IllegalArgumentException("Invalid environment variable reference format: " + reference);
        }

        String envVarName = reference.substring(ENV_PREFIX.length());
        if (envVarName.isEmpty()) {
            throw new IllegalArgumentException("Environment variable reference format is 'env:VAR_NAME', got: " + reference);
        }

        return envVarName;
    }

    /**
     * 批量解析模型引用
     *
     * @param references 模型引用数组
     * @return 解析后的模型名称数组
     * @throws IllegalArgumentException 如果输入数组为 null
     * @throws ConfigException 如果任何引用解析失败
     */
    public String[] resolveAll(String[] references) {
        if (references == null) {
            throw new IllegalArgumentException("Model references array cannot be null");
        }

        String[] resolved = new String[references.length];
        for (int i = 0; i < references.length; i++) {
            resolved[i] = resolve(references[i]);
        }
        return resolved;
    }

    /**
     * 获取环境变量引用前缀
     * @return 环境变量引用前缀
     */
    public static String getEnvPrefix() {
        return ENV_PREFIX;
    }
}