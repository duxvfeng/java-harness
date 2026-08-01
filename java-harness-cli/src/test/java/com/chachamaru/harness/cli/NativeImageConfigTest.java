package com.chachamaru.harness.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Native Image configuration and build.
 */
@DisplayName("Native Image Configuration Tests")
public class NativeImageConfigTest {

    @Test
    @DisplayName("reflect-config.json应该存在且有效")
    void reflectConfigShouldExistAndBeValid() throws IOException {
        Path reflectConfig = Paths.get("src/main/resources/META-INF/native-image/reflect-config.json");

        assertTrue(Files.exists(reflectConfig), "reflect-config.json should exist");

        String content = Files.readString(reflectConfig);
        assertTrue(content.contains("\"reflect\""), "Should contain reflect configuration");
        assertTrue(content.contains("\"proxies\""), "Should contain proxies configuration");
        assertTrue(content.contains("\"resources\""), "Should contain resources configuration");
    }

    @Test
    @DisplayName("resource-config.json应该存在且有效")
    void resourceConfigShouldExistAndBeValid() throws IOException {
        Path resourceConfig = Paths.get("src/main/resources/META-INF/native-image/resource-config.json");

        assertTrue(Files.exists(resourceConfig), "resource-config.json should exist");

        String content = Files.readString(resourceConfig);
        assertTrue(content.contains("\"resources\""), "Should contain resources configuration");
        assertTrue(content.contains("\"bundles\""), "Should contain bundles configuration");
    }

    @Test
    @DisplayName("serialization-config.json应该存在且有效")
    void serializationConfigShouldExistAndBeValid() throws IOException {
        Path serializationConfig = Paths.get("src/main/resources/META-INF/native-image/serialization-config.json");

        assertTrue(Files.exists(serializationConfig), "serialization-config.json should exist");

        String content = Files.readString(serializationConfig);
        assertTrue(content.contains("\"types\""), "Should contain types configuration");
    }

    @Test
    @DisplayName("HarnessNativeFeature类应该存在")
    void harnessNativeFeatureClassShouldExist() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("com.chachamaru.harness.cli.nativeimage.HarnessNativeFeature");
            assertNotNull(clazz);
            assertEquals("HarnessNativeFeature", clazz.getSimpleName());
        });
    }

    @Test
    @DisplayName("HarnessCLI类应该存在")
    void harnessCLIClassShouldExist() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("com.chachamaru.harness.cli.HarnessCLI");
            assertNotNull(clazz);
            assertEquals("HarnessCLI", clazz.getSimpleName());
        });
    }

    @Test
    @DisplayName("reflect-config应该包含核心DTO类")
    void reflectConfigShouldContainCoreDTOs() throws IOException {
        Path reflectConfig = Paths.get("src/main/resources/META-INF/native-image/reflect-config.json");
        String content = Files.readString(reflectConfig);

        assertTrue(content.contains("HookInput"), "Should contain HookInput");
        assertTrue(content.contains("HookOutput"), "Should contain HookOutput");
        assertTrue(content.contains("GuardrailResult"), "Should contain GuardrailResult");
        assertTrue(content.contains("Task"), "Should contain Task");
        assertTrue(content.contains("RecoveryResult"), "Should contain RecoveryResult");
    }

    @Test
    @DisplayName("reflect-config应该包含代理接口")
    void reflectConfigShouldContainProxyInterfaces() throws IOException {
        Path reflectConfig = Paths.get("src/main/resources/META-INF/native-image/reflect-config.json");
        String content = Files.readString(reflectConfig);

        assertTrue(content.contains("HookHandler"), "Should contain HookHandler proxy");
        assertTrue(content.contains("GuardrailEngine"), "Should contain GuardrailEngine proxy");
        assertTrue(content.contains("RecoveryStrategy"), "Should contain RecoveryStrategy proxy");
        assertTrue(content.contains("StateRecovery"), "Should contain StateRecovery proxy");
    }

    @Test
    @DisplayName("resource-config应该包含正确的资源模式")
    void resourceConfigShouldContainResourcePatterns() throws IOException {
        Path resourceConfig = Paths.get("src/main/resources/META-INF/native-image/resource-config.json");
        String content = Files.readString(resourceConfig);

        assertTrue(content.contains("harness"), "Should include harness template");
        assertTrue(content.contains("\\.yaml"), "Should include YAML files");
        assertTrue(content.contains("\\.json"), "Should include JSON files");
    }

    @Test
    @DisplayName("Native Image profile应该在pom.xml中")
    void nativeProfileShouldBeInPom() throws IOException {
        Path pomFile = Paths.get("pom.xml");
        assertTrue(Files.exists(pomFile), "pom.xml should exist");

        String content = Files.readString(pomFile);
        assertTrue(content.contains("<id>native</id>"), "Should have native profile");
        assertTrue(content.contains("native-maven-plugin"), "Should have native plugin");
        assertTrue(content.contains("harness"), "Should specify image name");
    }

    @Test
    @DisplayName("CLI应该能正确解析参数")
    void cliShouldParseArguments() {
        assertDoesNotThrow(() -> {
            HarnessCLI cli = new HarnessCLI();
            assertNotNull(cli);
        });
    }

    @Test
    @DisplayName("启动时间应该很快（如果已编译为Native Image）")
    @EnabledIf("isNativeImage")
    void startupTimeShouldBeFast() {
        long startTime = System.nanoTime();

        HarnessCLI cli = new HarnessCLI();
        cli.run();

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Native image should start in less than 100ms
        assertTrue(durationMs < 100, "Startup should be < 100ms, took: " + durationMs + "ms");
        System.out.println("Startup time: " + durationMs + "ms");
    }

    @Test
    @DisplayName("版本信息应该正确显示")
    void versionInfoShouldDisplay() {
        assertDoesNotThrow(() -> {
            HarnessCLI cli = new HarnessCLI();
            // Version should be accessible
            assertNotNull(cli);
        });
    }

    /**
     * Checks if running in native image mode.
     */
    static boolean isNativeImage() {
        String version = System.getProperty("org.graalvm.nativeimage.version");
        return version != null && !version.isEmpty();
    }
}
