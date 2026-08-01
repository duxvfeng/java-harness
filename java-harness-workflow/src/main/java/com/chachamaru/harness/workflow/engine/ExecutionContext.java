package com.chachamaru.harness.workflow.engine;

import java.util.Map;
import java.util.HashMap;
import java.util.Stack;

/**
 * 工作流执行上下文
 * 管理工作流执行过程中的变量、文件上下文、会话状态
 */
public class ExecutionContext {
    private final Map<String, Object> variables;
    private final Map<String, Object> fileContext;
    private final Map<String, Object> sessionState;
    private final Stack<String> executionStack;
    private final Map<String, Object> metadata;

    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.fileContext = new HashMap<>();
        this.sessionState = new HashMap<>();
        this.executionStack = new Stack<>();
        this.metadata = new HashMap<>();
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 获取变量（带类型转换）
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object value = variables.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 批量设置变量
     */
    public void setVariables(Map<String, Object> vars) {
        if (vars != null) {
            variables.putAll(vars);
        }
    }

    /**
     * 获取所有变量
     */
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(variables);
    }

    /**
     * 文件上下文管理
     */
    public void setFileContext(String file, Object context) {
        fileContext.put(file, context);
    }

    public Object getFileContext(String file) {
        return fileContext.get(file);
    }

    /**
     * 会话状态管理
     */
    public void setSessionState(String key, Object value) {
        sessionState.put(key, value);
    }

    public Object getSessionState(String key) {
        return sessionState.get(key);
    }

    /**
     * 执行栈管理（用于嵌套工作流）
     */
    public void pushExecution(String stepId) {
        executionStack.push(stepId);
    }

    public String popExecution() {
        return executionStack.isEmpty() ? null : executionStack.pop();
    }

    public String getCurrentExecution() {
        return executionStack.isEmpty() ? null : executionStack.peek();
    }

    /**
     * 元数据管理
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 清空上下文
     */
    public void clear() {
        variables.clear();
        fileContext.clear();
        sessionState.clear();
        executionStack.clear();
        metadata.clear();
    }

    /**
     * 创建子上下文（用于并行执行）
     */
    public ExecutionContext createChildContext() {
        ExecutionContext child = new ExecutionContext();
        child.variables.putAll(this.variables);
        child.fileContext.putAll(this.fileContext);
        child.sessionState.putAll(this.sessionState);
        // 不复制执行栈和元数据
        return child;
    }

    /**
     * 合并子上下文的结果
     */
    public void mergeChildContext(ExecutionContext child) {
        if (child != null) {
            this.variables.putAll(child.getAllVariables());
        }
    }

    /**
     * 渲染模板变量
     */
    public String renderTemplate(String template) {
        if (template == null) {
            return "";
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                result = result.replace(placeholder, value);
            }
        }
        return result;
    }

    /**
     * 检查变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 获取上下文摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Variables: ").append(variables.size()).append("\n");
        summary.append("Files: ").append(fileContext.size()).append("\n");
        summary.append("Session State: ").append(sessionState.size()).append("\n");
        summary.append("Execution Depth: ").append(executionStack.size()).append("\n");
        summary.append("Metadata: ").append(metadata.size()).append("\n");
        return summary.toString();
    }
}
