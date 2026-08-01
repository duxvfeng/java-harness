package com.chachamaru.harness.workflow.loader;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换器
 * 支持类似 ${variable.name} 的变量替换语法
 */
public class VariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 解析字符串中的变量引用
     * @param template 包含变量引用的模板字符串
     * @param context 变量上下文
     * @return 替换后的字符串
     */
    public static String resolve(String template, Map<String, Object> context) {
        if (template == null) {
            return null;
        }

        if (context == null || context.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = context.get(variableName);

            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            } else {
                // 如果变量不存在，保持原样或替换为空字符串
                // 这里选择保持原样，便于调试
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析对象中的变量引用
     * 支持 String、Map、List 类型的递归解析
     */
    public static Object resolveObject(Object obj, Map<String, Object> context) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String) {
            return resolve((String) obj, context);
        }

        // Map 类型需要特殊处理，因为 Workflow 模型使用了 Map<String, Object>
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object resolvedValue = resolveObject(entry.getValue(), context);
                entry.setValue(resolvedValue);
            }
            return map;
        }

        // 其他类型（Integer、Boolean、List 等）不需要处理
        return obj;
    }
}
