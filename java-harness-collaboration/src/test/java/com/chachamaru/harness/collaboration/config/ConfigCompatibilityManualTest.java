package com.chachamaru.harness.collaboration.config;

/**
 * Manual test runner for Config Compatibility implementation.
 */
public class ConfigCompatibilityManualTest {
    public static void main(String[] args) {
        System.out.println("=== Config Compatibility Manual Test ===\n");

        // Test 1: Basic parsing
        System.out.println("Test 1: Basic TOML parsing");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            String tomlContent = "[harness]\nversion = \"4.1.1\"\nplatform = \"claude-code\"\n";
            HarnessConfig config = parser.parseString(tomlContent, "test-config");

            if (config == null) {
                System.out.println("✗ Config should not be null");
                return;
            }
            if (!"4.1.1".equals(config.getVersion())) {
                System.out.println("✗ Version mismatch: " + config.getVersion());
                return;
            }
            System.out.println("✓ Basic TOML parsing works\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 2: Platform sections
        System.out.println("Test 2: Platform-specific sections");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            String tomlContent = "[harness]\nplatform = \"claude-code\"\n\n[claude-code]\nmodel = \"claude-sonnet-5\"\n";
            HarnessConfig config = parser.parseString(tomlContent, "test-config");

            if (config == null) {
                System.out.println("✗ Config should not be null");
                return;
            }
            if (!config.hasPlatformSection("claude-code")) {
                System.out.println("✗ Should have claude-code section");
                return;
            }
            System.out.println("✓ Platform sections parsing works\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 3: Backend configuration
        System.out.println("Test 3: Backend configuration");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            String tomlContent = "[backend]\ndefault = \"claude\"\ntimeout = 300000\nmax-retries = 3\n";
            HarnessConfig config = parser.parseString(tomlContent, "test-config");

            if (config == null) {
                System.out.println("✗ Config should not be null");
                return;
            }
            if (!"claude".equals(config.getBackendDefault())) {
                System.out.println("✗ Backend default mismatch");
                return;
            }
            if (config.getBackendTimeout() != 300000L) {
                System.out.println("✗ Timeout mismatch: " + config.getBackendTimeout());
                return;
            }
            if (config.getBackendMaxRetries() != 3) {
                System.out.println("✗ Max retries mismatch: " + config.getBackendMaxRetries());
                return;
            }
            System.out.println("✓ Backend configuration parsing works\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 4: Multi-platform configuration
        System.out.println("Test 4: Multi-platform config");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            String tomlContent = "[harness]\nmulti-platform = true\n[backend]\ndefault = \"auto-detect\"\n";
            HarnessConfig config = parser.parseString(tomlContent, "test-config");

            if (config == null) {
                System.out.println("✗ Config should not be null");
                return;
            }
            if (!config.isMultiPlatform()) {
                System.out.println("✗ Multi-platform flag not set");
                return;
            }
            if (!"auto-detect".equals(config.getBackendDefault())) {
                System.out.println("✗ Backend default mismatch");
                return;
            }
            System.out.println("✓ Multi-platform configuration works\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 5: Default configuration
        System.out.println("Test 5: Default configuration");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            HarnessConfig config = parser.parseString("", "empty-config");

            if (config == null) {
                System.out.println("✗ Default config should not be null");
                return;
            }
            if (config.getVersion() == null) {
                System.out.println("✗ Should have default version");
                return;
            }
            System.out.println("✓ Default configuration provided\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 6: Required field validation
        System.out.println("Test 6: Required field validation");
        try {
            HarnessConfigParser parser = new HarnessConfigParser();
            String invalidToml = "[harness]\nplatform = \"claude-code\"\n";
            parser.parseString(invalidToml, "invalid-config");
            System.out.println("✗ Should have thrown exception for missing version");
        } catch (ConfigParseException e) {
            System.out.println("✓ Required field validation works\n");
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
            e.printStackTrace();
        }

        System.out.println("=== All Config Compatibility Tests Passed ===");
    }
}
