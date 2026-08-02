package com.chachamaru.harness.foundation.render;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HtmlRenderer 单元测试
 *
 * @since 4.0.0
 */
class HtmlRendererTest {

    @Test
    void testRenderWithVariables() {
        String template = "Hello {{NAME}}, welcome to {{PLACE}}!";
        Map<String, String> variables = Map.of(
            "NAME", "World",
            "PLACE", "Java"
        );

        String result = HtmlRenderer.render(template, variables);
        assertEquals("Hello World, welcome to Java!", result);
    }

    @Test
    void testRenderWithMissingVariables() {
        String template = "Hello {{NAME}}, welcome to {{PLACE}}!";
        Map<String, String> variables = Map.of("NAME", "World");

        String result = HtmlRenderer.render(template, variables);
        assertEquals("Hello World, welcome to !", result);
    }

    @Test
    void testRenderDefault() {
        Map<String, String> variables = Map.of(
            "PROJECT_NAME", "Test Project",
            "VERSION", "1.0.0"
        );

        String result = HtmlRenderer.renderDefault("Test Title", "<p>Test Content</p>", variables);

        assertTrue(result.contains("<title>Test Title</title>"));
        assertTrue(result.contains("Test Project"));
        assertTrue(result.contains("1.0.0"));
        assertTrue(result.contains("<p>Test Content</p>"));
        assertTrue(result.contains("生成时间:")); // 中文时间戳
    }

    @Test
    void testMarkdownToHtmlBasic() {
        String markdown = "# Hello World\n\nThis is a test.";
        String html = HtmlRenderer.markdownToHtml(markdown);

        assertTrue(html.contains("<h1>Hello World</h1>"));
        assertTrue(html.contains("<p>This is a test.</p>"));
    }

    @Test
    void testMarkdownToHtmlWithBoldAndItalic() {
        String markdown = "**bold text** and *italic text*";
        String html = HtmlRenderer.markdownToHtml(markdown);

        assertTrue(html.contains("<strong>bold text</strong>"));
        assertTrue(html.contains("<em>italic text</em>"));
    }

    @Test
    void testMarkdownToHtmlWithCode() {
        String markdown = "Use `System.out.println()` for output.";
        String html = HtmlRenderer.markdownToHtml(markdown);

        assertTrue(html.contains("<code>System.out.println()</code>"));
    }

    @Test
    void testMarkdownToHtmlWithCodeBlock() {
        String markdown = "```\npublic class Test {\n}\n```";
        String html = HtmlRenderer.markdownToHtml(markdown);

        assertTrue(html.contains("<pre><code>"));
    }

    @Test
    void testMarkdownToHtmlWithList() {
        String markdown = "- Item 1\n- Item 2\n- Item 3";
        String html = HtmlRenderer.markdownToHtml(markdown);

        assertTrue(html.contains("<li>Item 1</li>"));
        assertTrue(html.contains("<li>Item 2</li>"));
        assertTrue(html.contains("<li>Item 3</li>"));
    }

    @Test
    void testMarkdownToHtmlWithNull() {
        String html = HtmlRenderer.markdownToHtml(null);
        assertEquals("", html);
    }

    @Test
    void testGenerateProgressReport() {
        Map<String, Object> progressData = Map.of(
            "总任务数", "25",
            "已完成", "18",
            "进行中", "2",
            "待处理", "5"
        );

        String html = HtmlRenderer.generateProgressReport("Test Project", progressData);

        assertTrue(html.contains("项目进度报告"));
        assertTrue(html.contains("Test Project"));
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("总任务数"));
        assertTrue(html.contains("25"));
    }

    @Test
    void testGenerateDocument() {
        String markdown = "# Test Document\n\nThis is test content.";
        String html = HtmlRenderer.generateDocument("Test Document", markdown, "My Project");

        assertTrue(html.contains("<title>Test Document</title>"));
        assertTrue(html.contains("My Project"));
        assertTrue(html.contains("<h1>Test Document</h1>"));
        assertTrue(html.contains("This is test content"));
    }

    @Test
    void testGetDefaultTemplate() {
        String template = HtmlRenderer.getDefaultTemplate();
        assertNotNull(template);
        assertTrue(template.contains("<!DOCTYPE html>"));
        assertTrue(template.contains("<html"));
        assertTrue(template.contains("</html>"));
    }

    @Test
    void testRenderWithSpecialCharacters() {
        String template = "Price: {{PRICE}}";
        Map<String, String> variables = Map.of("PRICE", "$100");

        String result = HtmlRenderer.render(template, variables);
        assertEquals("Price: $100", result);
    }

    @Test
    void testRenderWithMultilineContent() {
        String template = "<div>\n{{CONTENT}}\n</div>";
        Map<String, String> variables = Map.of("CONTENT", "<p>Line 1\nLine 2</p>");

        String result = HtmlRenderer.render(template, variables);
        assertTrue(result.contains("<p>Line 1\nLine 2</p>"));
    }
}