package com.chachamaru.harness.foundation.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateRegistry 单元测试
 *
 * @since 4.0.0
 */
class TemplateRegistryTest {

    @TempDir
    Path tempDir;

    private TemplateRegistry registry;
    private String registryPath;
    private String templatesDir;

    @BeforeEach
    void setUp() {
        registryPath = tempDir.resolve("template-registry.json").toString();
        templatesDir = tempDir.resolve("templates").toString();
        registry = new TemplateRegistry(registryPath, templatesDir);
    }

    @Test
    void testRegisterTemplate() {
        Template template = createTestTemplate("test-template", "1.0.0");
        registry.registerTemplate(template);

        Template retrieved = registry.getTemplateById("test-template");
        assertNotNull(retrieved);
        assertEquals("test-template", retrieved.getId());
        assertEquals("core/test-template", retrieved.getFullName());
        assertEquals("1.0.0", retrieved.getVersion());
    }

    @Test
    void testRegisterDuplicateTemplate() {
        Template template1 = createTestTemplate("duplicate", "1.0.0");
        registry.registerTemplate(template1);

        Template template2 = createTestTemplate("duplicate", "2.0.0");

        assertThrows(TemplateRegistryException.class, () -> {
            registry.registerTemplate(template2);
        });
    }

    @Test
    void testGetTemplateById() {
        Template template = createTestTemplate("get-by-id", "1.0.0");
        registry.registerTemplate(template);

        Template retrieved = registry.getTemplateById("get-by-id");
        assertNotNull(retrieved);
        assertEquals("get-by-id", retrieved.getId());
    }

    @Test
    void testGetTemplateByIdNotFound() {
        assertThrows(TemplateRegistryException.class, () -> {
            registry.getTemplateById("non-existent");
        });
    }

    @Test
    void testGetTemplateByName() {
        Template template = createTestTemplate("by-name", "1.0.0");
        registry.registerTemplate(template);

        Template retrieved = registry.getTemplateByName("core/by-name");
        assertNotNull(retrieved);
        assertEquals("by-name", retrieved.getName());
    }

    @Test
    void testGetTemplatesByCategory() {
        Template template1 = createTestTemplate("cat1", "1.0.0");
        template1.setCategory("core");
        registry.registerTemplate(template1);

        Template template2 = createTestTemplate("cat2", "1.0.0");
        template2.setCategory("rules");
        registry.registerTemplate(template2);

        List<Template> coreTemplates = registry.getTemplatesByCategory("core");
        assertEquals(1, coreTemplates.size());
        assertEquals("cat1", coreTemplates.get(0).getName());

        List<Template> rulesTemplates = registry.getTemplatesByCategory("rules");
        assertEquals(1, rulesTemplates.size());
        assertEquals("cat2", rulesTemplates.get(0).getName());
    }

    @Test
    void testUpdateTemplate() {
        Template template = createTestTemplate("update-test", "1.0.0");
        registry.registerTemplate(template);

        Template updated = createTestTemplate("update-test", "2.0.0");
        updated.setDescription("Updated description");
        updated.setContent("Updated content");

        registry.updateTemplate("update-test", updated);

        Template retrieved = registry.getTemplateById("update-test");
        assertEquals("2.0.0", retrieved.getVersion());
        assertEquals("Updated description", retrieved.getDescription());
        assertEquals("Updated content", retrieved.getContent());
    }

    @Test
    void testUnregisterTemplate() {
        Template template = createTestTemplate("unregister", "1.0.0");
        registry.registerTemplate(template);

        assertNotNull(registry.getTemplateById("unregister"));

        registry.unregisterTemplate("unregister");

        assertThrows(TemplateRegistryException.class, () -> {
            registry.getTemplateById("unregister");
        });
    }

    @Test
    void testGetVersionHistory() {
        Template template = createTestTemplate("version-history", "1.0.0");
        registry.registerTemplate(template);

        List<TemplateVersion> versions = registry.getVersionHistory("version-history");
        assertNotNull(versions);
        assertEquals(1, versions.size());
        assertEquals("1.0.0", versions.get(0).getVersion());
    }

    @Test
    void testTemplateWithVariables() {
        Template template = createTestTemplate("var-template", "1.0.0");

        TemplateVariable var1 = new TemplateVariable("PROJECT_NAME", "项目名称",
            TemplateVariable.VariableType.STRING, true);
        template.addVariable("PROJECT_NAME", var1);

        TemplateVariable var2 = new TemplateVariable("AUTHOR", "作者",
            TemplateVariable.VariableType.STRING, false);
        var2.setDefaultValue("Unknown");
        template.addVariable("AUTHOR", var2);

        registry.registerTemplate(template);

        Template retrieved = registry.getTemplateById("var-template");
        assertEquals(2, retrieved.getVariables().size());
        assertTrue(retrieved.getVariables().containsKey("PROJECT_NAME"));
        assertTrue(retrieved.getVariables().containsKey("AUTHOR"));
    }

    @Test
    void testTemplateWithDependencies() {
        Template depTemplate = createTestTemplate("dependency", "1.0.0");
        registry.registerTemplate(depTemplate);

        Template template = createTestTemplate("dependent", "1.0.0");
        TemplateMetadata metadata = new TemplateMetadata("test", "MIT");
        metadata.addDependency("dependency", "1.0.0");
        template.setMetadata(metadata);

        assertTrue(registry.checkDependencies(template));
    }

    @Test
    void testGetAllTemplates() {
        Template template1 = createTestTemplate("all1", "1.0.0");
        registry.registerTemplate(template1);

        Template template2 = createTestTemplate("all2", "1.0.0");
        registry.registerTemplate(template2);

        List<Template> all = registry.getAllTemplates();
        assertEquals(2, all.size());
    }

    @Test
    void testGetStatistics() {
        Template template1 = createTestTemplate("stat1", "1.0.0");
        registry.registerTemplate(template1);

        Map<String, Object> stats = registry.getStatistics();
        assertEquals(1, stats.get("total_templates"));
    }

    @Test
    void testInvalidTemplateDefinition() {
        Template template = new Template();
        // Missing required fields

        assertThrows(TemplateRegistryException.class, () -> {
            registry.registerTemplate(template);
        });
    }

    @Test
    void testVariableValidation() {
        TemplateVariable var = new TemplateVariable("test", "Test Variable",
            TemplateVariable.VariableType.STRING, true);
        var.setPattern("[a-zA-Z]+");

        assertTrue(var.validate("ValidName"));
        assertFalse(var.validate("123"));
        assertFalse(var.validate(""));
    }

    @Test
    void testTemplatePersistence() throws Exception {
        Template template = createTestTemplate("persist", "1.0.0");
        registry.registerTemplate(template);

        // Create new registry instance from same file
        TemplateRegistry newRegistry = new TemplateRegistry(registryPath, templatesDir);

        Template retrieved = newRegistry.getTemplateById("persist");
        assertNotNull(retrieved);
        assertEquals("persist", retrieved.getId());
        assertEquals("1.0.0", retrieved.getVersion());
    }

    private Template createTestTemplate(String id, String version) {
        Template template = new Template(id, id,  // Use id as name for simplicity
            "Test description", version, "core");
        template.setContent("# " + id + " Template\n\nTemplate content here.");
        return template;
    }
}