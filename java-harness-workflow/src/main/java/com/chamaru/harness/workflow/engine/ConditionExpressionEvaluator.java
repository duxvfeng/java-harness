package com.chachamaru.harness.workflow.engine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式引擎
 * 用于评估工作流步骤的执行条件
 * 支持Go版本的所有条件语法
 */
public class ConditionExpressionEvaluator {

    /**
     * 评估条件表达式
     */
    public boolean evaluate(String condition, ExecutionContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        condition = condition.trim();

        // 处理复合表达式（AND, OR）
        if (condition.contains(" && ")) {
            return evaluateAndExpression(condition, context);
        } else if (condition.contains(" || ")) {
            return evaluateOrExpression(condition, context);
        } else if (condition.contains(" !")) {
            return evaluateNotExpression(condition, context);
        }

        // 处理括号表达式
        if (condition.startsWith("(") && condition.endsWith(")")) {
            return evaluate(condition.substring(1, condition.length() - 1), context);
        }

        // 处理基本比较表达式
        return evaluateBasicExpression(condition, context);
    }

    /**
     * 评估AND表达式
     */
    private boolean evaluateAndExpression(String expression, ExecutionContext context) {
        String[] parts = expression.split(" && ");
        for (String part : parts) {
            if (!evaluate(part.trim(), context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 评估OR表达式
     */
    private boolean evaluateOrExpression(String expression, ExecutionContext context) {
        String[] parts = expression.split(" \\|\\| ");
        for (String part : parts) {
            if (evaluate(part.trim(), context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 评估NOT表达式
     */
    private boolean evaluateNotExpression(String expression, ExecutionContext context) {
        String part = expression.substring(2).trim(); // 移除 " !"
        return !evaluate(part, context);
    }

    /**
     * 评估基本表达式
     */
    private boolean evaluateBasicExpression(String expression, ExecutionContext context) {
        // 相等比较 ==
        if (expression.contains("==")) {
            return evaluateComparison(expression, "==", context);
        }

        // 不等比较 !=
        if (expression.contains("!=")) {
            return evaluateComparison(expression, "!=", context);
        }

        // 大于比较 >
        if (expression.contains(">")) {
            return evaluateComparison(expression, ">", context);
        }

        // 小于比较 <
        if (expression.contains("<")) {
            return evaluateComparison(expression, "<", context);
        }

        // 大于等于 >=
        if (expression.contains(">=")) {
            return evaluateComparison(expression, ">=", context);
        }

        // 小于等于 <=
        if (expression.contains("<=")) {
            return evaluateComparison(expression, "<=", context);
        }

        // 包含检查 includes()
        if (expression.contains(".includes(")) {
            return evaluateIncludes(expression, context);
        }

        // 正则匹配 =~
        if (expression.contains("=~")) {
            return evaluateRegex(expression, context);
        }

        // 布尔变量
        if (expression.equals("true")) {
            return true;
        }
        if (expression.equals("false")) {
            return false;
        }

        // 变量存在性检查
        if (context.hasVariable(expression)) {
            Object value = context.getVariable(expression);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return value != null;
        }

        // 默认为false
        return false;
    }

    /**
     * 评估比较表达式
     */
    private boolean evaluateComparison(String expression, String operator, ExecutionContext context) {
        String[] parts = expression.split(Pattern.quote(operator), 2);
        if (parts.length != 2) {
            return false;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        Object leftValue = resolveValue(left, context);
        Object rightValue = resolveValue(right, context);

        return compareValues(leftValue, rightValue, operator);
    }

    /**
     * 解析值
     */
    private Object resolveValue(String valueStr, ExecutionContext context) {
        // 移除引号
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        if (valueStr.startsWith("'") && valueStr.endsWith("'")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // 数字
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            // 不是数字，继续
        }

        // 布尔值
        if (valueStr.equals("true")) {
            return true;
        }
        if (valueStr.equals("false")) {
            return false;
        }

        // 变量
        if (context.hasVariable(valueStr)) {
            return context.getVariable(valueStr);
        }

        // 默认返回字符串
        return valueStr;
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compareValues(Object left, Object right, String operator) {
        switch (operator) {
            case "==":
                if (left == null && right == null) return true;
                if (left == null || right == null) return false;
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) == 0;
                }
                return left.equals(right);

            case "!=":
                if (left == null && right == null) return false;
                if (left == null || right == null) return true;
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) != 0;
                }
                return !left.equals(right);

            case ">":
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) > 0;
                }
                return false;

            case "<":
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) < 0;
                }
                return false;

            case ">=":
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) >= 0;
                }
                return false;

            case "<=":
                if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
                    return ((Comparable) left).compareTo(right) <= 0;
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * 评估includes表达式
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateIncludes(String expression, ExecutionContext context) {
        Pattern pattern = Pattern.compile("(.+?)\\.includes\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(expression);

        if (!matcher.matches()) {
            return false;
        }

        String variable = matcher.group(1).trim();
        String checkValue = matcher.group(2).trim();

        Object collection = context.getVariable(variable);
        if (collection == null) {
            return false;
        }

        if (collection instanceof Collection) {
            return ((Collection<?>) collection).contains(checkValue);
        }

        if (collection instanceof String) {
            return ((String) collection).contains(checkValue);
        }

        return false;
    }

    /**
     * 评估正则表达式
     */
    private boolean evaluateRegex(String expression, ExecutionContext context) {
        String[] parts = expression.split("=~", 2);
        if (parts.length != 2) {
            return false;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        Object leftValue = resolveValue(left, context);
        String pattern = (String) resolveValue(right, context);

        if (leftValue == null || pattern == null) {
            return false;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            return regex.matcher(leftValue.toString()).find();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证条件表达式语法
     */
    public boolean validateSyntax(String condition) {
        try {
            // 基本语法检查
            if (condition == null || condition.trim().isEmpty()) {
                return true;
            }

            // 检查括号匹配
            int parenCount = 0;
            for (char c : condition.toCharArray()) {
                if (c == '(') parenCount++;
                if (c == ')') parenCount--;
                if (parenCount < 0) return false;
            }
            if (parenCount != 0) return false;

            // 检查基本操作符
            String[] operators = {"==", "!=", ">=", "<=", ">", "<", ".includes(", "=~", "&&", "||", "!"};
            for (String op : operators) {
                if (condition.contains(op)) {
                    // 简单验证：操作符不能在开头或结尾
                    int index = condition.indexOf(op);
                    if (index <= 0 || index >= condition.length() - op.length()) {
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取条件中使用的所有变量名
     */
    public Set<String> getUsedVariables(String condition) {
        Set<String> variables = new HashSet<>();

        if (condition == null || condition.trim().isEmpty()) {
            return variables;
        }

        // 提取includes()中的变量
        Pattern includesPattern = Pattern.compile("([\\w.]+)\\.includes\\(");
        Matcher includesMatcher = includesPattern.matcher(condition);
        while (includesMatcher.find()) {
            variables.add(includesMatcher.group(1));
        }

        // 提取比较表达式中的变量（简单启发式）
        String[] parts = condition.split("==|!=|>=|<=|>|<|=~");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.matches("\\d+|\".*\"|'.*'|true|false")) {
                variables.add(trimmed);
            }
        }

        return variables;
    }
}
