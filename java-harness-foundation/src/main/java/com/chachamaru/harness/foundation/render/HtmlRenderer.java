package com.chachamaru.harness.foundation.render;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML 渲染系统 - 生成 HTML 格式的文档和报告
 *
 * <p>支持的渲染功能：</p>
 * <ul>
 *   <li>Markdown 到 HTML 转换</li>
 *   <li>模板渲染</li>
 *   <li>进度报告生成</li>
 *   <li>文档生成</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class HtmlRenderer {

    private static final String DEFAULT_TEMPLATE = """
<!DOCTYPE html>
<html lang="{{LANGUAGE}}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{TITLE}}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            line-height: 1.6;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1, h2, h3 {
            color: #333;
        }
        .metadata {
            background-color: #f9f9f9;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
        .content {
            margin: 30px 0;
        }
        code {
            background-color: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: "Courier New", monospace;
        }
        pre {
            background-color: #f4f4f4;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }
        .timestamp {
            color: #666;
            font-size: 0.9em;
            text-align: right;
            margin-top: 30px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>{{TITLE}}</h1>
        <div class="metadata">
            <p><strong>项目:</strong> {{PROJECT_NAME}}</p>
            <p><strong>日期:</strong> {{DATE}}</p>
            <p><strong>版本:</strong> {{VERSION}}</p>
        </div>
        <div class="content">
            {{CONTENT}}
        </div>
        <div class="timestamp">
            生成时间: {{TIMESTAMP}}
        </div>
    </div>
</body>
</html>
""";

    /**
     * 渲染 HTML 文档
     *
     * @param template 模板内容
     * @param variables 变量映射
     * @return 渲染后的 HTML
     */
    public static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }

        String result = template;

        // 替换变量
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        // 将未提供值的占位符替换为空字符串
        result = result.replaceAll("\\{\\{([^}]+)\\}\\}", "");

        return result;
    }

    /**
     * 使用默认模板渲染 HTML
     *
     * @param title 标题
     * @param content 内容
     * @param variables 变量映射
     * @return 渲染后的 HTML
     */
    public static String renderDefault(String title, String content, Map<String, String> variables) {
        Map<String, String> mutableVariables = new HashMap<>(variables != null ? variables : Map.of());
        mutableVariables.put("TITLE", title);
        mutableVariables.put("CONTENT", content);
        mutableVariables.put("TIMESTAMP", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 设置默认值
        mutableVariables.putIfAbsent("LANGUAGE", "en");
        mutableVariables.putIfAbsent("VERSION", "4.0.0");
        mutableVariables.putIfAbsent("DATE", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        return render(DEFAULT_TEMPLATE, mutableVariables);
    }

    /**
     * Markdown 转 HTML（简化版本）
     *
     * @param markdown Markdown 内容
     * @return HTML 内容
     */
    public static String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String[] rawLines = markdown.split("\\r?\\n");
        List<String> result = new ArrayList<>();
        List<String> listItems = new ArrayList<>();
        StringBuilder codeBlock = new StringBuilder();
        boolean inList = false;
        boolean inCodeBlock = false;

        for (String rawLine : rawLines) {
            String trimmed = rawLine.trim();

            // 代码块边界
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // 结束代码块
                    result.add("<pre><code>" + escapeHtml(codeBlock.toString()) + "</code></pre>");
                    codeBlock.setLength(0);
                    inCodeBlock = false;
                } else {
                    // 开始代码块
                    if (inList) {
                        closeList(result, listItems);
                        inList = false;
                    }
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                if (codeBlock.length() > 0) {
                    codeBlock.append("\n");
                }
                codeBlock.append(rawLine);
                continue;
            }

            if (trimmed.isEmpty()) {
                if (inList) {
                    closeList(result, listItems);
                    inList = false;
                }
                continue;
            }

            // 标题
            if (trimmed.startsWith("# ")) {
                if (inList) {
                    closeList(result, listItems);
                    inList = false;
                }
                result.add("<h1>" + escapeHtml(trimmed.substring(2)) + "</h1>");
                continue;
            }
            if (trimmed.startsWith("## ")) {
                if (inList) {
                    closeList(result, listItems);
                    inList = false;
                }
                result.add("<h2>" + escapeHtml(trimmed.substring(3)) + "</h2>");
                continue;
            }
            if (trimmed.startsWith("### ")) {
                if (inList) {
                    closeList(result, listItems);
                    inList = false;
                }
                result.add("<h3>" + escapeHtml(trimmed.substring(4)) + "</h3>");
                continue;
            }

            // 列表项
            if (trimmed.startsWith("- ")) {
                String itemContent = formatInline(trimmed.substring(2));
                listItems.add("<li>" + itemContent + "</li>");
                inList = true;
                continue;
            }

            if (inList) {
                closeList(result, listItems);
                inList = false;
            }

            // 普通段落
            result.add("<p>" + formatInline(trimmed) + "</p>");
        }

        // 处理未关闭的代码块
        if (inCodeBlock && codeBlock.length() > 0) {
            result.add("<pre><code>" + escapeHtml(codeBlock.toString()) + "</code></pre>");
        }

        if (inList) {
            closeList(result, listItems);
        }

        return String.join("\n", result);
    }

    private static void closeList(List<String> result, List<String> listItems) {
        if (listItems.isEmpty()) {
            return;
        }
        result.add("<ul>");
        result.addAll(listItems);
        result.add("</ul>");
        listItems.clear();
    }

    private static String formatInline(String text) {
        String result = escapeHtml(text);
        // 粗体
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        // 斜体（避免匹配粗体已处理的部分）
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        // 行内代码
        result = result.replaceAll("`(.+?)`", "<code>$1</code>");
        return result;
    }

    private static String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /**
     * 生成进度报告 HTML
     *
     * @param projectName 项目名称
     * @param progressData 进度数据
     * @return HTML 报告
     */
    public static String generateProgressReport(String projectName, Map<String, Object> progressData) {
        StringBuilder content = new StringBuilder();

        content.append("<h2>项目进度报告</h2>");
        content.append("<table border=\"1\" cellpadding=\"10\" cellspacing=\"0\">");
        content.append("<tr><th>指标</th><th>状态</th></tr>");

        for (Map.Entry<String, Object> entry : progressData.entrySet()) {
            content.append("<tr>");
            content.append("<td>").append(entry.getKey()).append("</td>");
            content.append("<td>").append(entry.getValue()).append("</td>");
            content.append("</tr>");
        }

        content.append("</table>");

        return renderDefault("项目进度报告 - " + projectName, content.toString(), Map.of(
            "PROJECT_NAME", projectName,
            "VERSION", "4.0.0"
        ));
    }

    /**
     * 生成文档 HTML
     *
     * @param title 文档标题
     * @param markdownContent Markdown 格式的内容
     * @param projectName 项目名称
     * @return HTML 文档
     */
    public static String generateDocument(String title, String markdownContent, String projectName) {
        String htmlContent = markdownToHtml(markdownContent);
        return renderDefault(title, htmlContent, Map.of(
            "PROJECT_NAME", projectName != null ? projectName : "Java Harness",
            "VERSION", "4.0.0"
        ));
    }

    /**
     * 获取默认模板
     *
     * @return 默认 HTML 模板
     */
    public static String getDefaultTemplate() {
        return DEFAULT_TEMPLATE;
    }
}