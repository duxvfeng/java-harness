package com.chachamaru.harness.foundation.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态持久化工厂测试
 */
class StatePersistenceFactoryTest {

    @Test
    void testCreateJsonPersistence() {
        StatePersistenceEngine<Object> jsonPersistence = StatePersistenceFactory.createJsonPersistence();
        assertNotNull(jsonPersistence, "JSON persistence should be created");
        assertEquals("json", jsonPersistence.getFormat());
    }

    @Test
    void testCreateYamlPersistence() {
        StatePersistenceEngine<Object> yamlPersistence = StatePersistenceFactory.createYamlPersistence();
        assertNotNull(yamlPersistence, "YAML persistence should be created");
        assertEquals("yaml", yamlPersistence.getFormat());
    }

    @Test
    void testCreateFromJsonExtension(@TempDir Path tempDir) {
        Path jsonFile = tempDir.resolve("state.json");
        StatePersistenceEngine<Object> persistence = StatePersistenceFactory.createFromExtension(jsonFile);
        assertNotNull(persistence);
        assertEquals("json", persistence.getFormat());
    }

    @Test
    void testCreateFromYamlExtension(@TempDir Path tempDir) {
        Path yamlFile = tempDir.resolve("state.yaml");
        StatePersistenceEngine<Object> persistence = StatePersistenceFactory.createFromExtension(yamlFile);
        assertNotNull(persistence);
        assertEquals("yaml", persistence.getFormat());
    }

    @Test
    void testCreateFromYmlExtension(@TempDir Path tempDir) {
        Path ymlFile = tempDir.resolve("state.yml");
        StatePersistenceEngine<Object> persistence = StatePersistenceFactory.createFromExtension(ymlFile);
        assertNotNull(persistence);
        assertEquals("yaml", persistence.getFormat());
    }

    @Test
    void testCreateFromUnsupportedExtension(@TempDir Path tempDir) {
        Path unsupportedFile = tempDir.resolve("state.xml");
        assertThrows(IllegalArgumentException.class, () -> {
            StatePersistenceFactory.createFromExtension(unsupportedFile);
        });
    }

    @Test
    void testIsSupportedFormatJson(@TempDir Path tempDir) {
        Path jsonFile = tempDir.resolve("state.json");
        assertTrue(StatePersistenceFactory.isSupportedFormat(jsonFile));
    }

    @Test
    void testIsSupportedFormatYaml(@TempDir Path tempDir) {
        Path yamlFile = tempDir.resolve("state.yaml");
        assertTrue(StatePersistenceFactory.isSupportedFormat(yamlFile));
    }

    @Test
    void testIsSupportedFormatYml(@TempDir Path tempDir) {
        Path ymlFile = tempDir.resolve("state.yml");
        assertTrue(StatePersistenceFactory.isSupportedFormat(ymlFile));
    }

    @Test
    void testIsSupportedFormatXml(@TempDir Path tempDir) {
        Path xmlFile = tempDir.resolve("state.xml");
        assertFalse(StatePersistenceFactory.isSupportedFormat(xmlFile));
    }

    @Test
    void testIsSupportedFormatNoExtension(@TempDir Path tempDir) {
        Path noExtFile = tempDir.resolve("state");
        assertFalse(StatePersistenceFactory.isSupportedFormat(noExtFile));
    }

    @Test
    void testIsSupportedFormatCaseInsensitive(@TempDir Path tempDir) {
        Path upperCaseJson = tempDir.resolve("state.JSON");
        Path mixedCaseYaml = tempDir.resolve("state.YaMl");
        assertTrue(StatePersistenceFactory.isSupportedFormat(upperCaseJson));
        assertTrue(StatePersistenceFactory.isSupportedFormat(mixedCaseYaml));
    }
}
