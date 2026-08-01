package com.chachamaru.harness.workflow.loader;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 条件表达式求值器
 * 支持 Go 版本工作流中的条件表达式语法
 *
 * 支持的操作：
 * - 比较操作：==, !=, <, >, <=, >=
 * - 布尔操作：&&, ||, !
 * - 括号分组
 * - 变量引用：project_type, task_count 等
 * - 字符串字面量：'value', "value"
 * - 数字字面量：42, 3.14
 * - 布尔字面量：true, false
 */
public class ExpressionEvaluator {

    /**
     * 评估条件表达式
     * @param expression 条件表达式字符串
     * @param context 变量上下文
     * @return 表达式的布尔结果
     * @throws ExpressionException 如果表达式语法错误
     */
    public static boolean evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.isBlank()) {
            return true; // 空表达式默认为 true
        }

        try {
            ExpressionParser parser = new ExpressionParser(expression);
            Object result = parser.parseExpression().evaluate(context);
            return toBoolean(result);
        } catch (Exception e) {
            throw new ExpressionException("Failed to evaluate expression: " + expression, e);
        }
    }

    /**
     * 表达式异常
     */
    public static class ExpressionException extends Exception {
        public ExpressionException(String message) {
            super(message);
        }

        public ExpressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 简单的递归下降解析器
     */
    private static class ExpressionParser {
        private final String input;
        private int pos = 0;

        ExpressionParser(String input) {
            this.input = input.trim();
        }

        /**
         * 解析完整表达式
         */
        Expression parseExpression() {
            return parseOr();
        }

        /**
         * 解析逻辑或 (||)
         * 表达式 ::= logicalAnd ('||' logicalAnd)*
         */
        private Expression parseOr() {
            Expression left = parseAnd();

            while (match("\\|\\|")) {
                Expression right = parseAnd();
                return new OrExpression(left, right);
            }

            return left;
        }

        /**
         * 解析逻辑与 (&&)
         * logicalAnd ::= equality ('&&' equality)*
         */
        private Expression parseAnd() {
            Expression left = parseEquality();

            while (match("&&")) {
                Expression right = parseEquality();
                return new AndExpression(left, right);
            }

            return left;
        }

        /**
         * 解析相等性比较
         * equality ::= comparison (('=='|'!=') comparison)?
         */
        private Expression parseEquality() {
            Expression left = parseComparison();

            if (match("==")) {
                Expression right = parseComparison();
                return new EqualsExpression(left, right);
            } else if (match("!=")) {
                Expression right = parseComparison();
                return new NotEqualsExpression(left, right);
            }

            return left;
        }

        /**
         * 解析比较操作 (<, >, <=, >=)
         * comparison ::= unary (('<=' | '>=' | '<' | '>') unary)?
         */
        private Expression parseComparison() {
            Expression left = parseUnary();

            if (match("<=")) {
                Expression right = parseUnary();
                return new LessOrEqualsExpression(left, right);
            } else if (match(">=")) {
                Expression right = parseUnary();
                return new GreaterOrEqualsExpression(left, right);
            } else if (match("<")) {
                Expression right = parseUnary();
                return new LessExpression(left, right);
            } else if (match(">")) {
                Expression right = parseUnary();
                return new GreaterExpression(left, right);
            }

            return left;
        }

        /**
         * 解析一元操作
         * unary ::= '!' unary | primary
         */
        private Expression parseUnary() {
            skipWhitespace();

            if (match("!")) {
                Expression expr = parseUnary();
                return new NotExpression(expr);
            }

            return parsePrimary();
        }

        /**
         * 解析基本表达式
         * primary ::= '(' expression ')' | literal | variable
         */
        private Expression parsePrimary() {
            skipWhitespace();

            if (pos >= input.length()) {
                throw new RuntimeException("Unexpected end of expression");
            }

            // 括号分组
            if (match("\\(")) {
                Expression expr = parseExpression();
                if (!match("\\)")) {
                    throw new RuntimeException("Missing closing parenthesis");
                }
                return expr;
            }

            // 字符串字面值 (单引号或双引号)
            if (peek() == '\'' || peek() == '"') {
                return parseStringLiteral();
            }

            // 布尔字面值
            if (match("true")) {
                return new LiteralExpression(true);
            }
            if (match("false")) {
                return new LiteralExpression(false);
            }

            // 数字字面值
            if (matchNumber()) {
                return parseNumericLiteral();
            }

            // 变量引用
            return parseVariable();
        }

        /**
         * 解析字符串字面值
         */
        private Expression parseStringLiteral() {
            char quote = input.charAt(pos);
            pos++; // 跳过开始引号

            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && input.charAt(pos) != quote) {
                sb.append(input.charAt(pos));
                pos++;
            }

            if (pos >= input.length()) {
                throw new RuntimeException("Unterminated string literal");
            }

            pos++; // 跳过结束引号
            return new LiteralExpression(sb.toString());
        }

        /**
         * 解析数字字面值
         */
        private Expression parseNumericLiteral() {
            // 回退以重新解析数字
            int start = pos - 1;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = input.substring(start, pos);

            try {
                if (numStr.contains(".")) {
                    return new LiteralExpression(Double.parseDouble(numStr));
                } else {
                    return new LiteralExpression(Integer.parseInt(numStr));
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid numeric literal: " + numStr);
            }
        }

        /**
         * 解析变量引用
         */
        private Expression parseVariable() {
            skipWhitespace();
            int start = pos;

            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
                pos++;
            }

            if (start == pos) {
                throw new RuntimeException("Expected variable or literal at position " + pos);
            }

            String varName = input.substring(start, pos);
            return new VariableExpression(varName);
        }

        /**
         * 尝试匹配并消耗指定的 token
         */
        private boolean match(String token) {
            skipWhitespace();

            if (pos + token.length() <= input.length() &&
                input.substring(pos, pos + token.length()).equals(token)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        /**
         * 检查是否匹配数字开头
         */
        private boolean matchNumber() {
            skipWhitespace();
            return pos < input.length() && Character.isDigit(input.charAt(pos));
        }

        /**
         * 查看当前字符但不移动位置
         */
        private char peek() {
            skipWhitespace();
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        /**
         * 跳过空白字符
         */
        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    /**
     * 表达式接口
     */
    private interface Expression {
        Object evaluate(Map<String, Object> context);
    }

    /**
     * 字面值表达式
     */
    private static class LiteralExpression implements Expression {
        private final Object value;

        LiteralExpression(Object value) {
            this.value = value;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            return value;
        }
    }

    /**
     * 变量表达式
     */
    private static class VariableExpression implements Expression {
        private final String varName;

        VariableExpression(String varName) {
            this.varName = varName;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object value = context.get(varName);
            if (value == null) {
                return null; // 未定义的变量返回 null
            }
            return value;
        }
    }

    /**
     * 逻辑或
     */
    private static class OrExpression implements Expression {
        private final Expression left;
        private final Expression right;

        OrExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return toBoolean(leftVal) || toBoolean(rightVal);
        }
    }

    /**
     * 逻辑与
     */
    private static class AndExpression implements Expression {
        private final Expression left;
        private final Expression right;

        AndExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return toBoolean(leftVal) && toBoolean(rightVal);
        }
    }

    /**
     * 逻辑非
     */
    private static class NotExpression implements Expression {
        private final Expression expr;

        NotExpression(Expression expr) {
            this.expr = expr;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object val = expr.evaluate(context);
            return !toBoolean(val);
        }
    }

    /**
     * 相等比较
     */
    private static class EqualsExpression implements Expression {
        private final Expression left;
        private final Expression right;

        EqualsExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) == 0;
        }
    }

    /**
     * 不等比较
     */
    private static class NotEqualsExpression implements Expression {
        private final Expression left;
        private final Expression right;

        NotEqualsExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) != 0;
        }
    }

    /**
     * 小于比较
     */
    private static class LessExpression implements Expression {
        private final Expression left;
        private final Expression right;

        LessExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) < 0;
        }
    }

    /**
     * 大于比较
     */
    private static class GreaterExpression implements Expression {
        private final Expression left;
        private final Expression right;

        GreaterExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) > 0;
        }
    }

    /**
     * 小于等于比较
     */
    private static class LessOrEqualsExpression implements Expression {
        private final Expression left;
        private final Expression right;

        LessOrEqualsExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) <= 0;
        }
    }

    /**
     * 大于等于比较
     */
    private static class GreaterOrEqualsExpression implements Expression {
        private final Expression left;
        private final Expression right;

        GreaterOrEqualsExpression(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object evaluate(Map<String, Object> context) {
            Object leftVal = left.evaluate(context);
            Object rightVal = right.evaluate(context);

            return compareValues(leftVal, rightVal) >= 0;
        }
    }

    /**
     * 将对象转换为布尔值
     */
    private static boolean toBoolean(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue() != 0;
        }
        if (obj instanceof String) {
            return !((String) obj).isEmpty();
        }
        return true; // 非空对象为 true
    }

    /**
     * 比较两个值
     * 返回 -1（小于）、0（相等）、1（大于）
     */
    @SuppressWarnings("unchecked")
    private static int compareValues(Object left, Object right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        // 如果都是 Number 类型
        if (left instanceof Number && right instanceof Number) {
            double leftNum = ((Number) left).doubleValue();
            double rightNum = ((Number) right).doubleValue();
            return Double.compare(leftNum, rightNum);
        }

        // 如果一个是 String，尝试转换比较
        if (left instanceof String || right instanceof String) {
            String leftStr = left.toString();
            String rightStr = right.toString();
            return leftStr.compareTo(rightStr);
        }

        // 尝试 Comparable 接口
        if (left instanceof Comparable) {
            try {
                return ((Comparable<Object>) left).compareTo(right);
            } catch (ClassCastException e) {
                // 不可比较，转字符串比较
                return left.toString().compareTo(right.toString());
            }
        }

        // 默认转字符串比较
        return left.toString().compareTo(right.toString());
    }
}
