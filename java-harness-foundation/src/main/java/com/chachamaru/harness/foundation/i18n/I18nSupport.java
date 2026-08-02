package com.chachamaru.harness.foundation.i18n;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * 国际化支持类 - 提供多语言消息和模板支持
 *
 * <p>支持的语言：</p>
 * <ul>
 *   <li>en - English (默认)</li>
 *   <li>ja - 日本語</li>
 *   <li>zh - 中文</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class I18nSupport {

    private static final String BUNDLE_BASE_NAME = "messages";
    private static final String TEMPLATE_BUNDLE_BASE_NAME = "templates";

    private static Locale currentLocale = Locale.getDefault();
    private static ResourceBundle messageBundle;
    private static ResourceBundle templateBundle;

    static {
        loadBundles();
    }

    /**
     * 设置当前语言
     *
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundles();
    }

    /**
     * 设置当前语言（通过语言代码）
     *
     * @param languageCode 语言代码 (en, ja, zh)
     */
    public static void setLanguage(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "ja":
                setLocale(Locale.JAPANESE);
                break;
            case "zh":
                setLocale(Locale.SIMPLIFIED_CHINESE);
                break;
            case "en":
            default:
                setLocale(Locale.ENGLISH);
                break;
        }
    }

    /**
     * 获取当前语言
     *
     * @return 当前语言环境
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * 获取消息字符串
     *
     * @param key 消息键
     * @return 本地化消息
     */
    public static String getMessage(String key) {
        if (messageBundle == null) {
            return key; // 回退到键名
        }
        try {
            return messageBundle.getString(key);
        } catch (MissingResourceException e) {
            // 返回键名作为默认值
            return key;
        }
    }

    /**
     * 获取消息字符串（带参数）
     *
     * @param key 消息键
     * @param args 参数
     * @return 本地化消息
     */
    public static String getMessage(String key, Object... args) {
        String message = getMessage(key);
        return String.format(message, args);
    }

    /**
     * 获取模板内容
     *
     * @param templateKey 模板键
     * @return 本地化模板内容
     */
    public static String getTemplate(String templateKey) {
        try {
            return templateBundle.getString(templateKey);
        } catch (MissingResourceException e) {
            // 返回键名作为默认值
            return templateKey;
        }
    }

    /**
     * 检查是否支持某种语言
     *
     * @param languageCode 语言代码
     * @return 是否支持
     */
    public static boolean isLanguageSupported(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en":
            case "ja":
            case "zh":
                return true;
            default:
                return false;
        }
    }

    /**
     * 获取支持的语言列表
     *
     * @return 支持的语言代码数组
     */
    public static String[] getSupportedLanguages() {
        return new String[]{"en", "ja", "zh"};
    }

    /**
     * 加载资源包
     */
    private static void loadBundles() {
        try {
            messageBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
            templateBundle = ResourceBundle.getBundle(TEMPLATE_BUNDLE_BASE_NAME, currentLocale);
        } catch (Exception e) {
            // 回退到默认资源或创建空资源
            try {
                messageBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
                templateBundle = ResourceBundle.getBundle(TEMPLATE_BUNDLE_BASE_NAME, Locale.ENGLISH);
            } catch (Exception fallbackException) {
                // 如果仍然失败，设置为null，使用方法中的回退逻辑
                messageBundle = null;
                templateBundle = null;
            }
        }
    }

    /**
     * 获取本地化的项目配置
     *
     * @param languageCode 语言代码
     * @return 配置内容
     */
    public static String getLocalizedConfig(String languageCode) {
        String configKey = "config." + languageCode;
        return getTemplate(configKey);
    }
}