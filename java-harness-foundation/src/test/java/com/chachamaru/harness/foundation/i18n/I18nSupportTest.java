package com.chachamaru.harness.foundation.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I18nSupport 单元测试
 *
 * @since 4.0.0
 */
class I18nSupportTest {

    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        originalLocale = I18nSupport.getCurrentLocale();
    }

    @AfterEach
    void tearDown() {
        I18nSupport.setLocale(originalLocale);
    }

    @Test
    void testDefaultLocale() {
        assertEquals(Locale.ENGLISH, I18nSupport.getCurrentLocale());
    }

    @Test
    void testSetJapaneseLocale() {
        I18nSupport.setLanguage("ja");
        assertEquals(Locale.JAPANESE, I18nSupport.getCurrentLocale());
    }

    @Test
    void testSetChineseLocale() {
        I18nSupport.setLanguage("zh");
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nSupport.getCurrentLocale());
    }

    @Test
    void testSetEnglishLocale() {
        I18nSupport.setLanguage("en");
        assertEquals(Locale.ENGLISH, I18nSupport.getCurrentLocale());
    }

    @Test
    void testGetSupportedLanguages() {
        String[] languages = I18nSupport.getSupportedLanguages();
        assertNotNull(languages);
        assertEquals(3, languages.length);
        assertTrue(java.util.Arrays.asList(languages).contains("en"));
        assertTrue(java.util.Arrays.asList(languages).contains("ja"));
        assertTrue(java.util.Arrays.asList(languages).contains("zh"));
    }

    @Test
    void testIsLanguageSupported() {
        assertTrue(I18nSupport.isLanguageSupported("en"));
        assertTrue(I18nSupport.isLanguageSupported("ja"));
        assertTrue(I18nSupport.isLanguageSupported("zh"));
        assertFalse(I18nSupport.isLanguageSupported("fr"));
        assertFalse(I18nSupport.isLanguageSupported("de"));
    }

    @Test
    void testGetEnglishMessage() {
        I18nSupport.setLanguage("en");
        String message = I18nSupport.getMessage("general.welcome");
        assertEquals("Welcome to Java Harness", message);
    }

    @Test
    void testGetJapaneseMessage() {
        I18nSupport.setLanguage("ja");
        String message = I18nSupport.getMessage("general.welcome");
        assertEquals("Java Harness へようこそ", message);
    }

    @Test
    void testGetChineseMessage() {
        I18nSupport.setLanguage("zh");
        String message = I18nSupport.getMessage("general.welcome");
        assertEquals("欢迎使用 Java Harness", message);
    }

    @Test
    void testGetMessageWithParameters() {
        I18nSupport.setLanguage("en");
        String message = I18nSupport.getMessage("error.file.not_found", "test.txt");
        assertTrue(message.contains("test.txt"));
    }

    @Test
    void testGetMessageFallback() {
        I18nSupport.setLanguage("en");
        String message = I18nSupport.getMessage("nonexistent.key");
        assertEquals("nonexistent.key", message);
    }

    @Test
    void testLanguageSwitching() {
        // 切换到日语
        I18nSupport.setLanguage("ja");
        assertEquals(Locale.JAPANESE, I18nSupport.getCurrentLocale());
        assertEquals("Java Harness へようこそ", I18nSupport.getMessage("general.welcome"));

        // 切换到中文
        I18nSupport.setLanguage("zh");
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nSupport.getCurrentLocale());
        assertEquals("欢迎使用 Java Harness", I18nSupport.getMessage("general.welcome"));

        // 切换回英语
        I18nSupport.setLanguage("en");
        assertEquals(Locale.ENGLISH, I18nSupport.getCurrentLocale());
        assertEquals("Welcome to Java Harness", I18nSupport.getMessage("general.welcome"));
    }

    @Test
    void testStatusMessages() {
        I18nSupport.setLanguage("ja");
        assertEquals("完了", I18nSupport.getMessage("status.completed"));
        assertEquals("実行中", I18nSupport.getMessage("status.in_progress"));
        assertEquals("待機中", I18nSupport.getMessage("status.pending"));

        I18nSupport.setLanguage("zh");
        assertEquals("已完成", I18nSupport.getMessage("status.completed"));
        assertEquals("进行中", I18nSupport.getMessage("status.in_progress"));
        assertEquals("待处理", I18nSupport.getMessage("status.pending"));
    }
}