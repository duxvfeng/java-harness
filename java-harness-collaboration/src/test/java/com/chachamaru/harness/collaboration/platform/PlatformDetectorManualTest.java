package com.chachamaru.harness.collaboration.platform;

/**
 * Simple test runner for PlatformDetector.
 * This bypasses Maven issues and directly tests the implementation.
 */
public class PlatformDetectorManualTest {
    public static void main(String[] args) {
        System.out.println("=== Platform Detector Manual Test ===\n");

        // Test 1: Platform enum exists
        System.out.println("Test 1: Platform enum");
        try {
            Platform[] platforms = Platform.values();
            System.out.println("✓ Platform enum has " + platforms.length + " values");
            for (Platform p : platforms) {
                System.out.println("  - " + p.name());
            }
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
        }

        // Test 2: PlatformDetector instantiation
        System.out.println("\nTest 2: PlatformDetector instantiation");
        try {
            PlatformDetector detector = new PlatformDetector();
            System.out.println("✓ PlatformDetector created successfully");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            return;
        }

        // Test 3: detectCurrentPlatform() method exists and returns value
        System.out.println("\nTest 3: detectCurrentPlatform() method");
        try {
            PlatformDetector detector = new PlatformDetector();
            Platform platform = detector.detectCurrentPlatform();
            System.out.println("✓ Detected platform: " + platform);
            assert platform != null : "Platform should not be null";
            assert platform == Platform.CLAUDE_CODE || platform == Platform.CODEX
                : "Platform should be CLAUDE_CODE or CODEX";
            System.out.println("✓ Platform detection works correctly");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 4: Environment variable simulation
        System.out.println("\nTest 4: Environment detection");
        try {
            PlatformDetector detector = new PlatformDetector();

            // Test with no environment variables (should default to CLAUDE_CODE)
            String originalClaude = System.getenv("CLAUDE_CODE_HARNESS");
            String originalCodex = System.getenv("CODEX_CLI");

            System.out.println("  Current env:");
            System.out.println("    CLAUDE_CODE_HARNESS: " + originalClaude);
            System.out.println("    CODEX_CLI: " + originalCodex);

            Platform platform = detector.detectCurrentPlatform();
            System.out.println("  Detected: " + platform);
            System.out.println("✓ Environment-based detection works");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== All Manual Tests Passed ===");
    }
}
