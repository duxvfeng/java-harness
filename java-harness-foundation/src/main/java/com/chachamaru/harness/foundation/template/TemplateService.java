package com.chachamaru.harness.foundation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 模板服务 - 管理模板的加载、生成和应用
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>从类路径加载模板</li>
 *   <li>模板变量替换</li>
 *   <li>文件生成</li>
 *   <li>模板版本管理</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    private final TemplateRegistry registry;
    private final FrontmatterParser frontmatterParser;
    private final Map<String, String> builtinTemplates;

    /**
     * 构造函数
     */
    public TemplateService(TemplateRegistry registry) {
        this.registry = registry;
        this.frontmatterParser = new FrontmatterParser();
        this.builtinTemplates = new HashMap<>();

        // 加载内置模板
        loadBuiltinTemplates();
    }

    /**
     * 加载内置模板
     */
    private void loadBuiltinTemplates() {
        // 核心模板
        builtinTemplates.put("claude-md", loadTemplateResource("templates/core/claude-md.template"));
        builtinTemplates.put("agents-md", loadTemplateResource("templates/core/agents-md.template"));
        builtinTemplates.put("plans-md", loadTemplateResource("templates/core/plans-md.template"));
        builtinTemplates.put("readme-md", loadTemplateResource("templates/core/readme-md.template"));

        // 规则模板
        builtinTemplates.put("rule-md", loadTemplateResource("templates/rules/rule-md.template"));
        builtinTemplates.put("guardrail-rule", loadTemplateResource("templates/rules/guardrail-rule.template"));
        builtinTemplates.put("security-rule", loadTemplateResource("templates/rules/security-rule.template"));

        logger.info("加载了 {} 个内置模板", builtinTemplates.size());
    }

    /**
     * 从资源加载模板
     */
    private String loadTemplateResource(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warn("模板资源不存在: {}", resourcePath);
                return createDefaultTemplate(resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("加载模板失败: {}", resourcePath, e);
            return createDefaultTemplate(resourcePath);
        }
    }

    /**
     * 创建默认模板内容
     */
    private String createDefaultTemplate(String resourcePath) {
        String templateName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        return "---\n" +
                "_harness_version: 4.0.0\n" +
                "_harness_template:\n" +
                "  name: " + templateName + "\n" +
                "  version: 1.0.0\n" +
                "  category: core\n" +
                "---\n" +
                "# " + templateName + " Template\n" +
                "\n" +
                "Generated content for " + templateName + "\n" +
                "\n" +
                "{{PROJECT_NAME}}\n" +
                "{{DATE}}\n" +
                "{{AUTHOR}}\n";
    }

    /**
     * 生成文件内容
     *
     * @param templateName 模板名称
     * @param variables 变量上下文
     * @return 生成的内容
     */
    public String generateContent(String templateName, Map<String, Object> variables) {
        // 处理 null 变量
        Map<String, Object> safeVariables = variables != null ? variables : new HashMap<>();

        // 获取模板内容
        String templateContent = getTemplateContent(templateName);

        // 解析 Frontmatter
        FrontmatterParser.FrontmatterResult result = frontmatterParser.parse(templateContent);
        FrontmatterMetadata metadata = result.getMetadata();
        String bodyContent = result.getContent();

        // 创建模板对象
        Template template = createTemplateFromMetadata(templateName, metadata, bodyContent);

        // 执行变量替换
        TemplateVariableEngine engine = new TemplateVariableEngine(template, safeVariables);
        return engine.render();
    }

    /**
     * 获取模板内容
     */
    private String getTemplateContent(String templateName) {
        // 首先查找内置模板
        if (builtinTemplates.containsKey(templateName)) {
            return builtinTemplates.get(templateName);
        }

        // 然后查找注册表中的模板
        try {
            Template template = registry.getTemplateByName("core/" + templateName);
            return template.getContent();
        } catch (TemplateRegistryException e) {
            logger.warn("未找到模板: {}", templateName);
            return createDefaultTemplate("templates/" + templateName + ".template");
        }
    }

    /**
     * 从元数据创建模板对象
     */
    private Template createTemplateFromMetadata(String templateName, FrontmatterMetadata metadata, String content) {
        Template template = new Template();
        template.setId(templateName);
        template.setName(templateName);
        template.setCategory("core");
        template.setContent(content);
        template.setVersion("1.0.0");

        // 从 Frontmatter 提取变量定义
        if (metadata.getCustomMetadata() != null) {
            extractVariablesFromMetadata(metadata, template);
        }

        return template;
    }

    /**
     * 从元数据提取变量定义
     */
    private void extractVariablesFromMetadata(FrontmatterMetadata metadata, Template template) {
        // 这里可以从 metadata 中提取预定义的变量
        // 目前简化处理，添加常用变量
        addCommonVariables(template);
    }

    /**
     * 添加常用变量定义
     */
    private void addCommonVariables(Template template) {
        TemplateVariable projectName = new TemplateVariable("PROJECT_NAME", "项目名称",
            TemplateVariable.VariableType.STRING, true);
        template.addVariable("PROJECT_NAME", projectName);

        TemplateVariable author = new TemplateVariable("AUTHOR", "作者",
            TemplateVariable.VariableType.STRING, false);
        author.setDefaultValue("Unknown");
        template.addVariable("AUTHOR", author);

        TemplateVariable date = new TemplateVariable("DATE", "日期",
            TemplateVariable.VariableType.DATE, false);
        template.addVariable("DATE", date);
    }

    /**
     * 生成文件并写入
     *
     * @param templateName 模板名称
     * @param variables 变量上下文
     * @param outputFile 输出文件路径
     */
    public void generateFile(String templateName, Map<String, Object> variables, Path outputFile) {
        String content = generateContent(templateName, variables);

        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, content);
            logger.info("生成文件: {}", outputFile);
        } catch (IOException e) {
            logger.error("写入文件失败: {}", outputFile, e);
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_SAVE_FAILED,
                "写入文件失败: " + e.getMessage()
            );
        }
    }

    /**
     * 列出所有可用模板
     */
    public List<String> listTemplates() {
        List<String> templates = new ArrayList<>();

        // 添加内置模板
        templates.addAll(builtinTemplates.keySet());

        // 添加注册表中的模板
        for (Template template : registry.getAllTemplates()) {
            if (!templates.contains(template.getName())) {
                templates.add(template.getName());
            }
        }

        return Collections.unmodifiableList(templates);
    }

    /**
     * 获取模板元数据
     */
    public FrontmatterMetadata getTemplateMetadata(String templateName) {
        String content = getTemplateContent(templateName);
        FrontmatterParser.FrontmatterResult result = frontmatterParser.parse(content);
        return result.getMetadata();
    }

    /**
     * 验证模板
     */
    public boolean validateTemplate(String templateName) {
        try {
            String content = getTemplateContent(templateName);
            FrontmatterParser.FrontmatterResult result = frontmatterParser.parse(content);
            return result.hasFrontmatter();
        } catch (Exception e) {
            logger.warn("模板验证失败: {}", templateName, e);
            return false;
        }
    }

    /**
     * 获取内置模板列表
     */
    public List<String> getBuiltinTemplates() {
        return Collections.unmodifiableList(new ArrayList<>(builtinTemplates.keySet()));
    }

    /**
     * 检查模板是否存在
     */
    public boolean hasTemplate(String templateName) {
        return builtinTemplates.containsKey(templateName) ||
               registry.getTemplatesByCategory("core").stream()
                   .anyMatch(t -> t.getName().equals(templateName));
    }
}