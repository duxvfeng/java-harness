package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 模板注册表系统 - 管理所有模板的注册、查询和版本控制
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>模板注册和注销</li>
 *   <li>模板查询和检索</li>
 *   <li>版本管理和映射</li>
 *   <li>模板持久化</li>
 *   <li>依赖关系管理</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TemplateRegistry.class);

    private final Map<String, Template> templatesById;
    private final Map<String, Template> templatesByName;
    private final Map<String, List<TemplateVersion>> versionHistory;
    private final ObjectMapper objectMapper;
    private final Path registryFile;
    private final Path templatesDir;

    /**
     * 构造函数
     *
     * @param registryPath template-registry.json 文件路径
     * @param templatesDir 模板文件目录
     */
    public TemplateRegistry(String registryPath, String templatesDir) {
        this.templatesById = new ConcurrentHashMap<>();
        this.templatesByName = new ConcurrentHashMap<>();
        this.versionHistory = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to handle Java 8 date/time types
        this.objectMapper.registerModule(new JavaTimeModule());
        this.registryFile = Paths.get(registryPath);
        this.templatesDir = Paths.get(templatesDir);

        // 确保目录存在
        ensureDirectoriesExist();

        // 加载现有注册表
        loadRegistry();
    }

    /**
     * 注册新模板
     *
     * @param template 要注册的模板
     * @throws TemplateRegistryException 如果模板已存在或无效
     */
    public void registerTemplate(Template template) {
        logger.info("注册模板: {}", template.getFullName());

        // 验证模板
        validateTemplate(template);

        // 检查是否已存在
        if (templatesById.containsKey(template.getId())) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_ALREADY_EXISTS,
                template.getId()
            );
        }

        if (templatesByName.containsKey(template.getFullName())) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_ALREADY_EXISTS,
                template.getFullName()
            );
        }

        // 设置时间戳
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        // 添加到注册表
        templatesById.put(template.getId(), template);
        templatesByName.put(template.getFullName(), template);

        // 初始化版本历史
        List<TemplateVersion> versions = new ArrayList<>();
        versions.add(createInitialVersion(template));
        versionHistory.put(template.getId(), versions);

        // 保存注册表
        saveRegistry();

        logger.info("模板注册成功: {} (版本: {})", template.getFullName(), template.getVersion());
    }

    /**
     * 注销模板
     *
     * @param templateId 模板ID
     * @throws TemplateRegistryException 如果模板不存在
     */
    public void unregisterTemplate(String templateId) {
        logger.info("注销模板: {}", templateId);

        Template template = templatesById.get(templateId);
        if (template == null) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_NOT_FOUND,
                templateId
            );
        }

        templatesById.remove(templateId);
        templatesByName.remove(template.getFullName());
        versionHistory.remove(templateId);

        saveRegistry();

        logger.info("模板注销成功: {}", template.getFullName());
    }

    /**
     * 根据ID获取模板
     *
     * @param templateId 模板ID
     * @return 模板对象
     * @throws TemplateRegistryException 如果模板不存在
     */
    public Template getTemplateById(String templateId) {
        Template template = templatesById.get(templateId);
        if (template == null) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_NOT_FOUND,
                templateId
            );
        }
        return template;
    }

    /**
     * 根据名称获取模板
     *
     * @param fullName 完整名称 (category/name)
     * @return 模板对象
     * @throws TemplateRegistryException 如果模板不存在
     */
    public Template getTemplateByName(String fullName) {
        Template template = templatesByName.get(fullName);
        if (template == null) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_NOT_FOUND,
                fullName
            );
        }
        return template;
    }

    /**
     * 根据分类查询模板
     *
     * @param category 分类名称
     * @return 该分类下的所有模板
     */
    public List<Template> getTemplatesByCategory(String category) {
        return templatesById.values().stream()
            .filter(t -> category.equals(t.getCategory()))
            .collect(Collectors.toList());
    }

    /**
     * 获取所有模板
     *
     * @return 所有模板列表
     */
    public List<Template> getAllTemplates() {
        return new ArrayList<>(templatesById.values());
    }

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param updatedTemplate 更新后的模板数据
     */
    public void updateTemplate(String templateId, Template updatedTemplate) {
        logger.info("更新模板: {}", templateId);

        Template existing = getTemplateById(templateId);

        // 保存旧版本到历史
        TemplateVersion newVersion = createVersionFromUpdate(existing, updatedTemplate);
        versionHistory.get(templateId).add(newVersion);

        // 更新模板
        existing.setName(updatedTemplate.getName());
        existing.setDescription(updatedTemplate.getDescription());
        existing.setVersion(updatedTemplate.getVersion());
        existing.setContent(updatedTemplate.getContent());
        existing.setVariables(updatedTemplate.getVariables());
        existing.setMetadata(updatedTemplate.getMetadata());
        existing.updateTimestamp();

        saveRegistry();

        logger.info("模板更新成功: {} (版本: {})", existing.getFullName(), existing.getVersion());
    }

    /**
     * 获取模板的版本历史
     *
     * @param templateId 模板ID
     * @return 版本历史列表
     */
    public List<TemplateVersion> getVersionHistory(String templateId) {
        List<TemplateVersion> versions = versionHistory.get(templateId);
        if (versions == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(versions);
    }

    /**
     * 检查模板依赖是否满足
     *
     * @param template 模板对象
     * @return 依赖检查结果
     */
    public boolean checkDependencies(Template template) {
        if (template.getMetadata() == null) {
            return true;
        }

        Map<String, String> dependencies = template.getMetadata().getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> dep : dependencies.entrySet()) {
            String depTemplateId = dep.getKey();
            String requiredVersion = dep.getValue();

            if (!templatesById.containsKey(depTemplateId)) {
                logger.warn("缺少依赖模板: {}", depTemplateId);
                return false;
            }

            Template depTemplate = templatesById.get(depTemplateId);
            if (!isVersionCompatible(depTemplate.getVersion(), requiredVersion)) {
                logger.warn("版本不兼容: {} 需要 {} 但找到 {}",
                    depTemplateId, requiredVersion, depTemplate.getVersion());
                return false;
            }
        }

        return true;
    }

    /**
     * 验证模板定义
     *
     * @param template 模板对象
     * @throws TemplateRegistryException 如果模板无效
     */
    private void validateTemplate(Template template) {
        if (template.getId() == null || template.getId().trim().isEmpty()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                "模板ID不能为空"
            );
        }

        if (template.getName() == null || template.getName().trim().isEmpty()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                "模板名称不能为空"
            );
        }

        if (template.getVersion() == null || template.getVersion().trim().isEmpty()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                "模板版本不能为空"
            );
        }

        if (template.getCategory() == null || template.getCategory().trim().isEmpty()) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                "模板分类不能为空"
            );
        }

        // 验证变量定义
        if (template.getVariables() != null) {
            for (TemplateVariable var : template.getVariables().values()) {
                if (var.getName() == null || var.getName().trim().isEmpty()) {
                    throw new TemplateRegistryException(
                        TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                        "变量名称不能为空"
                    );
                }
            }
        }
    }

    /**
     * 创建初始版本记录
     */
    private TemplateVersion createInitialVersion(Template template) {
        TemplateVersion version = new TemplateVersion(
            template.getId(),
            template.getVersion(),
            "初始版本"
        );
        version.setChecksum(calculateChecksum(template));
        return version;
    }

    /**
     * 从更新创建版本记录
     */
    private TemplateVersion createVersionFromUpdate(Template oldTemplate, Template newTemplate) {
        TemplateVersion version = new TemplateVersion(
            newTemplate.getId(),
            newTemplate.getVersion(),
            "更新版本"
        );
        version.setChecksum(calculateChecksum(newTemplate));

        // 记录变更
        if (!oldTemplate.getVersion().equals(newTemplate.getVersion())) {
            version.addChange("version", "从 " + oldTemplate.getVersion() + " 更新到 " + newTemplate.getVersion());
        }

        return version;
    }

    /**
     * 计算模板校验和
     */
    private String calculateChecksum(Template template) {
        try {
            String content = template.getContent() != null ? template.getContent() : "";
            return String.valueOf(content.hashCode());
        } catch (Exception e) {
            logger.warn("计算校验和失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 检查版本兼容性
     */
    private boolean isVersionCompatible(String currentVersion, String requiredVersion) {
        // 简化版本：完全匹配
        // 未来可以实现语义化版本比较
        return currentVersion.equals(requiredVersion);
    }

    /**
     * 确保目录存在
     */
    private void ensureDirectoriesExist() {
        try {
            if (registryFile.getParent() != null) {
                Files.createDirectories(registryFile.getParent());
            }
            Files.createDirectories(templatesDir);
        } catch (IOException e) {
            logger.error("创建目录失败: {}", e.getMessage());
        }
    }

    /**
     * 加载注册表
     */
    private void loadRegistry() {
        if (!Files.exists(registryFile)) {
            logger.info("注册表文件不存在，将创建新注册表: {}", registryFile);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(registryFile.toFile());
            JsonNode templatesNode = root.get("templates");

            if (templatesNode != null && templatesNode.isArray()) {
                for (JsonNode templateNode : templatesNode) {
                    Template template = objectMapper.treeToValue(templateNode, Template.class);
                    templatesById.put(template.getId(), template);
                    templatesByName.put(template.getFullName(), template);
                }
            }

            logger.info("成功加载 {} 个模板", templatesById.size());
        } catch (IOException e) {
            logger.error("加载注册表失败: {}", e.getMessage());
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_LOAD_FAILED,
                e
            );
        }
    }

    /**
     * 保存注册表
     */
    private void saveRegistry() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("version", "1.0");
            root.put("updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            root.set("templates", objectMapper.valueToTree(templatesById.values()));

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(registryFile.toFile(), root);

            logger.debug("注册表保存成功: {}", registryFile);
        } catch (IOException e) {
            logger.error("保存注册表失败: {}", e.getMessage());
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_SAVE_FAILED,
                e
            );
        }
    }

    /**
     * 获取注册表统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_templates", templatesById.size());
        stats.put("categories", templatesById.values().stream()
            .map(Template::getCategory)
            .distinct()
            .collect(Collectors.toList())
            .size());
        stats.put("total_versions", versionHistory.values().stream()
            .mapToInt(List::size)
            .sum());

        return stats;
    }
}