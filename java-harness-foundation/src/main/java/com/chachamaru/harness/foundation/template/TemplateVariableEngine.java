package com.chachamaru.harness.foundation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板变量替换引擎 - 处理模板中的变量替换
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>支持内置变量 (PROJECT_NAME, DATE, AUTHOR 等)</li>
 *   <li>支持自定义变量注入</li>
 *   <li>变量类型验证</li>
 *   <li>条件替换</li>
 *   <li>默认值处理</li>
 *   <li>安全的模板注入防护</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateVariableEngine {

    private static final Logger logger = LoggerFactory.getLogger(TemplateVariableEngine.class);

    // 变量占位符模式: {{variable_name}} 或 {{variable_name:default_value}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)(?::([^}]*))?}}");

    // 条件块模式: {{#if variable}}...{{/if}}
    private static final Pattern CONDITIONAL_IF_PATTERN = Pattern.compile("\\{\\{#if\\s+([a-zA-Z0-9_]+)}}(.*?)\\{\\{/if}}", Pattern.DOTALL);

    // 反向条件块模式: {{#unless variable}}...{{/unless}}
    private static final Pattern CONDITIONAL_UNLESS_PATTERN = Pattern.compile("\\{\\{#unless\\s+([a-zA-Z0-9_]+)}}(.*?)\\{\\{/unless}}", Pattern.DOTALL);

    // 循环块模式: {{#each items}}...{{/each}}
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([a-zA-Z0-9_]+)}}(.*?)\\{\\{/each}}", Pattern.DOTALL);

    // 循环内部变量模式
    private static final Pattern LOOP_VAR_PATTERN = Pattern.compile("\\{\\{(this|@index)}}");

    // 允许的变量名模式（防止注入）
    private static final Pattern VALID_VAR_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final Map<String, Object> context;
    private final Template template;

    /**
     * 构造函数
     *
     * @param template 模板对象
     * @param context 变量上下文
     */
    public TemplateVariableEngine(Template template, Map<String, Object> context) {
        this.template = template;
        this.context = new HashMap<>(context);

        // 添加内置变量（仅包含安全的系统信息）
        addBuiltinVariables();
    }

    /**
     * 添加内置变量到上下文
     * 仅包含安全的、非敏感的系统信息
     */
    private void addBuiltinVariables() {
        LocalDateTime now = LocalDateTime.now();

        // 日期和时间变量
        context.put("DATE", now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        context.put("TIME", now.format(DateTimeFormatter.ISO_LOCAL_TIME));
        context.put("DATETIME", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        context.put("YEAR", String.valueOf(now.getYear()));
        context.put("MONTH", String.valueOf(now.getMonthValue()));
        context.put("DAY", String.valueOf(now.getDayOfMonth()));
        context.put("TIMESTAMP", String.valueOf(System.currentTimeMillis()));

        // 安全的系统属性（仅版本信息）
        context.put("JAVA_VERSION", sanitizeSystemProperty("java.version"));
        context.put("OS_NAME", sanitizeSystemProperty("os.name"));
    }

    /**
     * 安全地获取和清理系统属性
     */
    private String sanitizeSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value != null) {
                // 限制长度并移除危险字符
                return value.replaceAll("[<>\"\']", "_").substring(0, Math.min(100, value.length()));
            }
            return "";
        } catch (SecurityException e) {
            logger.warn("无法访问系统属性: {}", key);
            return "unknown";
        }
    }

    /**
     * 执行变量替换
     *
     * @return 替换后的内容
     */
    public String render() {
        if (template == null || template.getContent() == null) {
            return "";
        }

        String content = template.getContent();

        // 验证所有必需变量
        validateRequiredVariables();

        // 预处理：验证变量名安全性
        validateVariableNames();

        // 处理条件块
        content = processConditionals(content);

        // 处理循环块
        content = processLoops(content);

        // 处理简单变量替换
        content = processVariables(content);

        logger.debug("模板渲染完成: {}", template.getName());
        return content;
    }

    /**
     * 验证变量名安全性
     */
    private void validateVariableNames() {
        Set<String> usedVars = getUsedVariables();
        for (String varName : usedVars) {
            if (!VALID_VAR_NAME_PATTERN.matcher(varName).matches()) {
                throw new TemplateRegistryException(
                    TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                    "无效的变量名: " + varName
                );
            }
        }
    }

    /**
     * 处理简单变量替换
     */
    private String processVariables(String content) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String defaultValue = matcher.group(2);

            // 获取变量值并验证
            Object value = getVariableValue(variableName, defaultValue);
            String replacement = sanitizeReplacement(value);

            // 验证替换后的值
            if (template.getVariables() != null && template.getVariables().containsKey(variableName)) {
                TemplateVariable varDef = template.getVariables().get(variableName);
                if (!varDef.validate(replacement)) {
                    logger.warn("变量验证失败: {} = {}", variableName, replacement);
                    replacement = sanitizeReplacement(defaultValue);
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理条件块
     */
    private String processConditionals(String content) {
        // 处理 {{#if}} 条件块
        Matcher ifMatcher = CONDITIONAL_IF_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (ifMatcher.find()) {
            String variableName = ifMatcher.group(1);
            String blockContent = ifMatcher.group(2);

            // 验证变量名
            if (!VALID_VAR_NAME_PATTERN.matcher(variableName).matches()) {
                logger.warn("跳过无效的条件变量: {}", variableName);
                ifMatcher.appendReplacement(result, "");
                continue;
            }

            Object value = context.get(variableName);
            boolean shouldInclude = evaluateCondition(value);

            ifMatcher.appendReplacement(result,
                Matcher.quoteReplacement(shouldInclude ? blockContent : ""));
        }

        ifMatcher.appendTail(result);
        content = result.toString();

        // 处理 {{#unless}} 条件块
        Matcher unlessMatcher = CONDITIONAL_UNLESS_PATTERN.matcher(content);
        result = new StringBuffer();

        while (unlessMatcher.find()) {
            String variableName = unlessMatcher.group(1);
            String blockContent = unlessMatcher.group(2);

            // 验证变量名
            if (!VALID_VAR_NAME_PATTERN.matcher(variableName).matches()) {
                logger.warn("跳过无效的条件变量: {}", variableName);
                unlessMatcher.appendReplacement(result, "");
                continue;
            }

            Object value = context.get(variableName);
            boolean shouldInclude = !evaluateCondition(value);

            unlessMatcher.appendReplacement(result,
                Matcher.quoteReplacement(shouldInclude ? blockContent : ""));
        }

        unlessMatcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理循环块
     */
    private String processLoops(String content) {
        Matcher matcher = LOOP_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String blockContent = matcher.group(2);

            // 验证变量名
            if (!VALID_VAR_NAME_PATTERN.matcher(variableName).matches()) {
                logger.warn("跳过无效的循环变量: {}", variableName);
                matcher.appendReplacement(result, "");
                continue;
            }

            Object value = context.get(variableName);
            String loopResult = processLoopContent(blockContent, value);

            matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理循环内容（安全版本）
     */
    private String processLoopContent(String blockContent, Object value) {
        StringBuilder loopResult = new StringBuilder();

        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            int index = 0;
            for (Object item : collection) {
                // 安全地处理循环项
                String processedBlock = processLoopBlock(blockContent, item, index);
                loopResult.append(processedBlock);
                index++;
            }
        } else if (value != null) {
            logger.warn("循环变量不是集合类型: {} = {}", value.getClass().getName());
        }

        return loopResult.toString();
    }

    /**
     * 处理单个循环块（安全版本）
     */
    private String processLoopBlock(String blockContent, Object item, int index) {
        Matcher matcher = LOOP_VAR_PATTERN.matcher(blockContent);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varType = matcher.group(1);

            String replacement;
            if ("this".equals(varType)) {
                replacement = sanitizeReplacement(item);
            } else if ("@index".equals(varType)) {
                replacement = String.valueOf(index);
            } else {
                replacement = "";
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 安全地清理替换值
     */
    private String sanitizeReplacement(Object value) {
        if (value == null) {
            return "";
        }

        String str = value.toString();

        // 移除潜在的危险模板语法
        str = str.replaceAll("\\{\\{", "&#123;&#123;");
        str = str.replaceAll("}}", "&#125;&#125;");

        // 移除其他危险字符
        str = str.replaceAll("[<>\"\']", "");

        // 限制长度
        if (str.length() > 10000) {
            logger.warn("替换值过长，已截断");
            str = str.substring(0, 10000);
        }

        return str;
    }

    /**
     * 获取变量值
     */
    private Object getVariableValue(String variableName, String defaultValue) {
        // 首先从上下文中查找
        if (context.containsKey(variableName)) {
            return context.get(variableName);
        }

        // 从模板定义的变量中查找默认值
        if (template.getVariables() != null && template.getVariables().containsKey(variableName)) {
            TemplateVariable varDef = template.getVariables().get(variableName);
            if (varDef.getDefaultValue() != null) {
                return varDef.getDefaultValue();
            }
        }

        // 使用内联默认值
        if (defaultValue != null && !defaultValue.isEmpty()) {
            return defaultValue;
        }

        logger.warn("未找到变量值: {}", variableName);
        return "";
    }

    /**
     * 评估条件值
     */
    private boolean evaluateCondition(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }

        if (value instanceof String) {
            return !((String) value).isEmpty();
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }

        return true;
    }

    /**
     * 验证所有必需变量
     */
    private void validateRequiredVariables() {
        if (template.getVariables() == null) {
            return;
        }

        List<String> missing = new ArrayList<>();

        for (Map.Entry<String, TemplateVariable> entry : template.getVariables().entrySet()) {
            String varName = entry.getKey();
            TemplateVariable varDef = entry.getValue();

            // 验证变量名
            if (!VALID_VAR_NAME_PATTERN.matcher(varName).matches()) {
                throw new TemplateRegistryException(
                    TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                    "无效的变量名: " + varName
                );
            }

            if (varDef.isRequired() && !context.containsKey(varName)) {
                // 检查是否有默认值
                if (varDef.getDefaultValue() == null || varDef.getDefaultValue().isEmpty()) {
                    missing.add(varName);
                }
            }
        }

        if (!missing.isEmpty()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                "缺少必需变量: " + String.join(", ", missing)
            );
        }
    }

    /**
     * 添加变量到上下文
     */
    public void setVariable(String name, Object value) {
        // 验证变量名
        if (!VALID_VAR_NAME_PATTERN.matcher(name).matches()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                "无效的变量名: " + name
            );
        }
        context.put(name, value);
    }

    /**
     * 批量添加变量
     */
    public void setVariables(Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            setVariable(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 获取当前上下文（只读副本）
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(new HashMap<>(context));
    }

    /**
     * 预览替换效果（不进行验证）
     */
    public String preview() {
        if (template == null || template.getContent() == null) {
            return "";
        }

        String content = template.getContent();
        return processVariables(content);
    }

    /**
     * 获取模板中使用的所有变量
     */
    public Set<String> getUsedVariables() {
        if (template == null || template.getContent() == null) {
            return Collections.emptySet();
        }

        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template.getContent());

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return Collections.unmodifiableSet(variables);
    }

    /**
     * 验证单个变量值
     */
    public boolean validateVariable(String name, String value) {
        if (template.getVariables() != null && template.getVariables().containsKey(name)) {
            TemplateVariable varDef = template.getVariables().get(name);
            return varDef.validate(value);
        }
        return true;
    }
}