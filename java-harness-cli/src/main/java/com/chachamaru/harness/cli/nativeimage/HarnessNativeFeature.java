package com.chachamaru.harness.cli.nativeimage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Native Image configuration helper for Java Harness.
 *
 * <p>This class provides utilities for configuring and validating
 * GraalVM Native Image builds for Java Harness.</p>
 *
 * @spec_reference spec.md#Native Image Configuration
 */
public class HarnessNativeFeature {

    private static final String VERSION = "1.0";
    private static final String FEATURE_NAME = "Java Harness Native Image Feature";

    /**
     * Validates native image build environment.
     *
     * @return true if environment is valid
     */
    public static boolean validateEnvironment() {
        String graalVMVersion = System.getProperty("org.graalvm.nativeimage.version");
        if (graalVMVersion == null) {
            System.err.println("[HarnessNativeFeature] Warning: Not running in GraalVM Native Image environment");
            return false;
        }

        System.out.println("[HarnessNativeFeature] GraalVM version: " + graalVMVersion);
        return true;
    }

    /**
     * Gets feature information.
     *
     * @return Feature description
     */
    public static String getFeatureInfo() {
        return FEATURE_NAME + " v" + VERSION +
               " - Configures Java Harness for GraalVM Native Image compilation";
    }

    /**
     * Validates that required configuration files exist.
     *
     * @return true if all config files are present
     */
    public static boolean validateConfigFiles() {
        String[] configFiles = {
            "reflect-config.json",
            "resource-config.json",
            "serialization-config.json"
        };

        boolean allPresent = true;
        for (String configFile : configFiles) {
            Path configPath = Paths.get("src/main/resources/META-INF/native-image/" + configFile);
            if (!Files.exists(configPath)) {
                System.err.println("[HarnessNativeFeature] Missing config: " + configFile);
                allPresent = false;
            } else {
                System.out.println("[HarnessNativeFeature] ✓ Config present: " + configFile);
            }
        }

        return allPresent;
    }

    /**
     * Gets the estimated native image size.
     *
     * @return Estimated size in MB, or -1 if unable to estimate
     */
    public static long getEstimatedImageSize() {
        // Basic estimate based on typical GraalVM native images
        // Real size depends on included features and optimizations
        return 50; // ~50MB base size
    }

    /**
     * Gets the expected startup time.
     *
     * @return Expected startup time in milliseconds
     */
    public static int getExpectedStartupTime() {
        return 80; // ~80ms target startup time
    }

    /**
     * Checks if this is a native image build.
     *
     * @return true if running in native image mode
     */
    public static boolean isNativeImage() {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    /**
     * Gets build configuration properties.
     *
     * @return Build properties
     */
    public static Properties getBuildProperties() {
        Properties props = new Properties();

        // Set default build properties
        props.setProperty("image.name", "harness");
        props.setProperty("main.class", "com.chachamaru.harness.cli.HarnessCLI");
        props.setProperty("quick.build", "true");
        props.setProperty("optimization.level", "2");

        // Add required build arguments
        props.setProperty("build.arg.1", "--no-fallback");
        props.setProperty("build.arg.2", "-H:+ReportExceptionStackTraces");

        return props;
    }

    /**
     * Generates a build report summary.
     *
     * @return Build report string
     */
    public static String generateBuildReport() {
        StringBuilder report = new StringBuilder();
        report.append("Java Harness Native Image Build Report\n");
        report.append("=====================================\n");
        report.append("Feature Version: ").append(VERSION).append("\n");
        report.append("Is Native Image: ").append(isNativeImage()).append("\n");
        report.append("Config Files Valid: ").append(validateConfigFiles()).append("\n");
        report.append("Estimated Image Size: ~").append(getEstimatedImageSize()).append(" MB\n");
        report.append("Expected Startup Time: <").append(getExpectedStartupTime()).append(" ms\n");

        if (isNativeImage()) {
            report.append("\n✓ Running in Native Image mode\n");
        } else {
            report.append("\n⚠ Not in Native Image mode (JVM mode)\n");
        }

        return report.toString();
    }

    /**
     * Gets the GraalVM version.
     *
     * @return GraalVM version string, or null if not available
     */
    public static String getGraalVMVersion() {
        return System.getProperty("org.graalvm.nativeimage.version");
    }

    /**
     * Gets the Java version.
     *
     * @return Java version string
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Gets the OS and architecture information.
     *
     * @return OS and arch string
     */
    public static String getOSInfo() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        return os + " " + arch;
    }

    /**
     * Validates the runtime environment for native image execution.
     *
     * @return true if runtime environment is valid
     */
    public static boolean validateRuntimeEnvironment() {
        if (!isNativeImage()) {
            return true; // JVM runtime is always valid
        }

        // Check for required system properties
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            System.err.println("[HarnessNativeFeature] Unable to determine Java version");
            return false;
        }

        return true;
    }
}
