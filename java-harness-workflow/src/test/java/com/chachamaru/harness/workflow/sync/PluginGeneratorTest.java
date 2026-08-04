package com.chachamaru.harness.workflow.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class PluginGeneratorTest {

    @Test
    void testGeneratePluginJSON(@TempDir Path tempDir) throws Exception {
        SyncConfig config = new SyncConfig();
        SyncConfig.ProjectConfig project = new SyncConfig.ProjectConfig();
        project.setName("test-plugin");
        project.setVersion("1.0.0");
        project.setDescription("Test plugin");
        project.setAuthorName("Test Author");
        project.setHomepage("https://example.com");
        config.setProject(project);

        File projectRoot = tempDir.toFile();
        String generatedPath = PluginGenerator.generate(projectRoot, config);

        assertNotNull(generatedPath);
        assertTrue(Files.exists(Path.of(generatedPath)));

        // 验证 JSON 内容
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(new File(generatedPath));

        assertEquals("test-plugin", json.get("name").asText());
        assertEquals("1.0.0", json.get("version").asText());
        assertEquals("./skills/", json.get("skills").get(0).asText());
    }
}
