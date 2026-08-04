package com.chachamaru.harness.foundation.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

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

    private static Locale currentLocale = Locale.ENGLISH;
    private static Properties messageProperties = new Properties();
    private static Properties templateProperties = new Properties();

    static {
        loadBundles();
    }

    /**
     * 设置当前语言
     *
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale != null ? locale : Locale.ENGLISH;
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
        if (key == null) {
            return null;
        }
        return messageProperties.getProperty(key, key);
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
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    /**
     * 获取模板内容
     *
     * @param templateKey 模板键
     * @return 本地化模板内容
     */
    public static String getTemplate(String templateKey) {
        return templateProperties.getProperty(templateKey, templateKey);
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
        messageProperties = loadProperties(BUNDLE_BASE_NAME, currentLocale);
        templateProperties = loadProperties(TEMPLATE_BUNDLE_BASE_NAME, currentLocale);
    }

    /**
     * 加载指定语言环境的 properties 文件
     */
    private static Properties loadProperties(String baseName, Locale locale) {
        Properties properties = new Properties();
        String language = locale.getLanguage();
        String resourceName = baseName + "_" + language + ".properties";

        // 先加载英语作为默认 fallback
        if (!"en".equals(language)) {
            loadPropertiesFile(properties, baseName + "_en.properties");
        }

        // 再加载目标语言，覆盖默认值
        loadPropertiesFile(properties, resourceName);

        return properties;
    }

    private static void loadPropertiesFile(Properties properties, String resourceName) {
        try (InputStream is = I18nSupport.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            // 忽略加载失败，使用已有内容或空属性
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
