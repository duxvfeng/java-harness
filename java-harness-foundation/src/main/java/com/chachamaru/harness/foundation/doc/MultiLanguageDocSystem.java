package com.chachamaru.harness.foundation.doc;

import com.chachamaru.harness.foundation.i18n.I18nSupport;
import com.chachamaru.harness.foundation.render.HtmlRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * 多语言文档系统 - 支持多语言的项目文档生成
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>多语言文档生成</li>
 *   <li>模板系统支持</li>
 *   <li>HTML 输出格式</li>
 *   <li>配置化文档结构</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class MultiLanguageDocSystem {

    /**
     * 生成多语言文档
     *
     * @param docType 文档类型 (CLAUDE, AGENTS, PLANS)
     * @param language 语言代码 (en, ja, zh)
     * @param projectName 项目名称
     * @return 生成的 HTML 文档
     */
    public static String generateDocument(String docType, String language, String projectName) {
        // 设置语言
        I18nSupport.setLanguage(language);

        // 获取本地化内容
        String title = getLocalizedTitle(docType, language);
        String content = getLocalizedContent(docType, language);

        // 准备变量
        Map<String, String> variables = new HashMap<>();
        variables.put("PROJECT_NAME", projectName != null ? projectName : "Java Harness");
        variables.put("LANGUAGE", language);
        variables.put("DOC_TYPE", docType);

        // 生成 HTML
        return HtmlRenderer.renderDefault(title, content, variables);
    }

    /**
     * 生成特定语言的项目文档集
     *
     * @param projectName 项目名称
     * @param language 语言代码
     * @return 文档集映射
     */
    public static Map<String, String> generateDocumentSet(String projectName, String language) {
        Map<String, String> documents = new HashMap<>();

        documents.put("CLAUDE.md", generateDocument("CLAUDE", language, projectName));
        documents.put("AGENTS.md", generateDocument("AGENTS", language, projectName));
        documents.put("Plans.md", generateDocument("PLANS", language, projectName));

        return documents;
    }

    /**
     * 生成所有支持语言的文档集
     *
     * @param projectName 项目名称
     * @return 多语言文档集映射
     */
    public static Map<String, Map<String, String>> generateAllLanguageDocuments(String projectName) {
        Map<String, Map<String, String>> allDocs = new HashMap<>();

        for (String language : I18nSupport.getSupportedLanguages()) {
            allDocs.put(language, generateDocumentSet(projectName, language));
        }

        return allDocs;
    }

    /**
     * 获取本地化标题
     *
     * @param docType 文档类型
     * @param language 语言代码
     * @return 本地化标题
     */
    private static String getLocalizedTitle(String docType, String language) {
        I18nSupport.setLanguage(language);

        return switch (docType.toUpperCase()) {
            case "CLAUDE" -> I18nSupport.getMessage("template.claude.title");
            case "AGENTS" -> I18nSupport.getMessage("template.agents.title");
            case "PLANS" -> I18nSupport.getMessage("template.plans.title");
            default -> "Document";
        };
    }

    /**
     * 获取本地化内容
     *
     * @param docType 文档类型
     * @param language 语言代码
     * @return 本地化内容
     */
    private static String getLocalizedContent(String docType, String language) {
        // 这里可以从本地化模板文件中读取内容
        // 简化版本：返回基于语言的基本内容

        String content = switch (language) {
            case "ja" -> getJapaneseContent(docType);
            case "zh" -> getChineseContent(docType);
            default -> getEnglishContent(docType);
        };

        return HtmlRenderer.markdownToHtml(content);
    }

    /**
     * 获取英语内容
     */
    private static String getEnglishContent(String docType) {
        return switch (docType.toUpperCase()) {
            case "CLAUDE" -> """
                ## Claude Code Execution Instructions

                ### Responsibilities
                - Implementation of complex features
                - Architecture design
                - Performance optimization

                ### Project Structure
                ```
                java-harness/
                ├── java-harness-foundation/
                ├── java-harness-protocol/
                └── java-harness-service/
                ```

                ### Workflow
                The project uses **Breezing Mode** for 4+ tasks:
                - Lead coordination
                - Worker implementation
                - Reviewer validation
                """;
            case "AGENTS" -> """
                ## Agent Team Configuration

                ### Roles
                - **Lead**: Coordination and planning
                - **Worker**: Implementation and execution
                - **Reviewer**: Quality validation
                - **Advisor**: Strategic guidance

                ### Communication
                Agents communicate through structured messages and shared state.
                """;
            case "PLANS" -> """
                ## Project Plan

                ### Current Status
                - Phase 8.8: Template System ✅
                - Phase 8.9: Script Porting ✅
                - Phase 8.10: Internationalization ✅

                ### Next Steps
                Continue with feature enhancements and quality improvements.
                """;
            default -> "# Content\n\nDocument content goes here.";
        };
    }

    /**
     * 获取日语内容
     */
    private static String getJapaneseContent(String docType) {
        return switch (docType.toUpperCase()) {
            case "CLAUDE" -> """
                ## Claude Code 実行指示書

                ### 責務範囲
                - 複雑な機能の実装
                - アーキテクチャ設計
                - パフォーマンス最適化

                ### プロジェクト構造
                ```
                java-harness/
                ├── java-harness-foundation/
                ├── java-harness-protocol/
                └── java-harness-service/
                ```

                ### ワークフロー
                4つ以上のタスクには **Breezing Mode** を使用：
                - Lead 協調
                - Worker 実装
                - Reviewer 検証
                """;
            case "AGENTS" -> """
                ## エージェントチーム設定

                ### 役割
                - **Lead**: 調整と計画
                - **Worker**: 実装と実行
                - **Reviewer**: 品質検証
                - **Advisor**: 戦略的ガイダンス

                ### コミュニケーション
                エージェントは構造化されたメッセージと共有状態で通信します。
                """;
            case "PLANS" -> """
                ## プロジェクト計画

                ### 現在のステータス
                - Phase 8.8: テンプレートシステム ✅
                - Phase 8.9: スクリプト移植 ✅
                - Phase 8.10: 国際化 ✅

                ### 次のステップ
                機能拡張と品質改善を継続します。
                """;
            default -> "# コンテンツ\n\nドキュメントコンテンツがここに入ります。";
        };
    }

    /**
     * 获取中文内容
     */
    private static String getChineseContent(String docType) {
        return switch (docType.toUpperCase()) {
            case "CLAUDE" -> """
                ## Claude Code 执行指示

                ### 职责范围
                - 复杂功能实现
                - 架构设计
                - 性能优化

                ### 项目结构
                ```
                java-harness/
                ├── java-harness-foundation/
                ├── java-harness-protocol/
                └── java-harness-service/
                ```

                ### 工作流
                4个或更多任务使用 **Breezing 模式**：
                - Lead 协调
                - Worker 实现
                - Reviewer 验证
                """;
            case "AGENTS" -> """
                ## 代理团队配置

                ### 角色
                - **Lead**: 协调和规划
                - **Worker**: 实现和执行
                - **Reviewer**: 质量验证
                - **Advisor**: 战略指导

                ### 通信
                代理通过结构化消息和共享状态进行通信。
                """;
            case "PLANS" -> """
                ## 项目计划

                ### 当前状态
                - Phase 8.8: 模板系统 ✅
                - Phase 8.9: 脚本移植 ✅
                - Phase 8.10: 国际化 ✅

                ### 下一步
                继续功能增强和质量改进。
                """;
            default -> "# 内容\n\n文档内容在这里。";
        };
    }

    /**
     * 获取支持的语言列表
     *
     * @return 支持的语言数组
     */
    public static String[] getSupportedLanguages() {
        return I18nSupport.getSupportedLanguages();
    }

    /**
     * 检查是否支持某种语言
     *
     * @param languageCode 语言代码
     * @return 是否支持
     */
    public static boolean isLanguageSupported(String languageCode) {
        return I18nSupport.isLanguageSupported(languageCode);
    }
}