package com.chachamaru.harness.foundation.doc;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MultiLanguageDocSystem 单元测试
 *
 * @since 4.0.0
 */
class MultiLanguageDocSystemTest {

    @Test
    void testGenerateEnglishDocument() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "en", "Test Project");

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Test Project"));
        assertTrue(html.contains("Claude Code Execution Instructions"));
    }

    @Test
    void testGenerateJapaneseDocument() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "ja", "テストプロジェクト");

        assertNotNull(html);
        assertTrue(html.contains("テストプロジェクト"));
        assertTrue(html.contains("Claude Code 実行指示書"));
    }

    @Test
    void testGenerateChineseDocument() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "zh", "测试项目");

        assertNotNull(html);
        assertTrue(html.contains("测试项目"));
        assertTrue(html.contains("Claude Code 执行指示"));
    }

    @Test
    void testGenerateDocumentSet() {
        Map<String, String> docs = MultiLanguageDocSystem.generateDocumentSet("My Project", "en");

        assertNotNull(docs);
        assertEquals(3, docs.size());
        assertTrue(docs.containsKey("CLAUDE.md"));
        assertTrue(docs.containsKey("AGENTS.md"));
        assertTrue(docs.containsKey("Plans.md"));
    }

    @Test
    void testGenerateAllLanguageDocuments() {
        Map<String, Map<String, String>> allDocs = MultiLanguageDocSystem.generateAllLanguageDocuments("Test Project");

        assertNotNull(allDocs);
        assertEquals(3, allDocs.size()); // en, ja, zh
        assertTrue(allDocs.containsKey("en"));
        assertTrue(allDocs.containsKey("ja"));
        assertTrue(allDocs.containsKey("zh"));

        // 检查每种语言的文档数量
        for (Map<String, String> langDocs : allDocs.values()) {
            assertEquals(3, langDocs.size()); // CLAUDE, AGENTS, Plans
        }
    }

    @Test
    void testGetSupportedLanguages() {
        String[] languages = MultiLanguageDocSystem.getSupportedLanguages();

        assertNotNull(languages);
        assertEquals(3, languages.length);
        assertTrue(java.util.Arrays.asList(languages).contains("en"));
        assertTrue(java.util.Arrays.asList(languages).contains("ja"));
        assertTrue(java.util.Arrays.asList(languages).contains("zh"));
    }

    @Test
    void testIsLanguageSupported() {
        assertTrue(MultiLanguageDocSystem.isLanguageSupported("en"));
        assertTrue(MultiLanguageDocSystem.isLanguageSupported("ja"));
        assertTrue(MultiLanguageDocSystem.isLanguageSupported("zh"));
        assertFalse(MultiLanguageDocSystem.isLanguageSupported("fr"));
        assertFalse(MultiLanguageDocSystem.isLanguageSupported("de"));
    }

    @Test
    void testGenerateAgentsDocument() {
        String html = MultiLanguageDocSystem.generateDocument("AGENTS", "en", "Test Project");

        assertNotNull(html);
        assertTrue(html.contains("Agent Team Configuration"));
        assertTrue(html.contains("Lead"));
        assertTrue(html.contains("Worker"));
        assertTrue(html.contains("Reviewer"));
    }

    @Test
    void testGeneratePlansDocument() {
        String html = MultiLanguageDocSystem.generateDocument("PLANS", "en", "Test Project");

        assertNotNull(html);
        assertTrue(html.contains("Project Plan"));
        assertTrue(html.contains("Phase 8.8"));
        assertTrue(html.contains("Phase 8.9"));
        assertTrue(html.contains("Phase 8.10"));
    }

    @Test
    void testDocumentContentContainsProjectStructure() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "en", "Test Project");

        assertTrue(html.contains("java-harness-foundation"));
        assertTrue(html.contains("java-harness-protocol"));
        assertTrue(html.contains("java-harness-service"));
    }

    @Test
    void testJapaneseDocumentContainsJapaneseWorkflow() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "ja", "テスト");

        assertTrue(html.contains("Breezing Mode"));
        assertTrue(html.contains("Lead"));
        assertTrue(html.contains("Worker"));
        assertTrue(html.contains("Reviewer"));
    }

    @Test
    void testChineseDocumentContainsChineseWorkflow() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "zh", "测试");

        assertTrue(html.contains("Breezing 模式"));
        assertTrue(html.contains("Lead"));
        assertTrue(html.contains("Worker"));
        assertTrue(html.contains("Reviewer"));
    }

    @Test
    void testDocumentContainsHtmlStructure() {
        String html = MultiLanguageDocSystem.generateDocument("CLAUDE", "en", "Test");

        assertTrue(html.contains("<html"));
        assertTrue(html.contains("</html>"));
        assertTrue(html.contains("<head>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("</body>"));
    }
}