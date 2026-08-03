package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Version command handler.
 * Displays version and build information.
 */
public class VersionHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(VersionHandler.class);
    private static final String VERSION = "5.0.0-java";
    private static final String BUILD_DATE = "2026-08-03";

    @Override
    public void execute(String[] args) {
        try {
            boolean detailed = false;
            boolean json = false;

            // Parse arguments
            for (String arg : args) {
                if (arg.equals("--detailed") || arg.equals("-d")) {
                    detailed = true;
                } else if (arg.equals("--json") || arg.equals("-j")) {
                    json = true;
                }
            }

            if (json) {
                outputJsonVersion(detailed);
            } else {
                outputTextVersion(detailed);
            }

        } catch (Exception e) {
            logger.error("Version command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void outputTextVersion(boolean detailed) {
        System.out.println("Java Harness CLI");
        System.out.println("Version: " + VERSION);
        System.out.println("Build Date: " + BUILD_DATE);

        if (detailed) {
            System.out.println("\nSystem Information:");
            System.out.println("  Java Version: " + System.getProperty("java.version"));
            System.out.println("  Java Home: " + System.getProperty("java.home"));
            System.out.println("  OS Name: " + System.getProperty("os.name"));
            System.out.println("  OS Version: " + System.getProperty("os.version"));
            System.out.println("  OS Arch: " + System.getProperty("os.arch"));

            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            System.out.println("\nJVM Information:");
            System.out.println("  JVM Name: " + runtimeMXBean.getVmName());
            System.out.println("  JVM Vendor: " + runtimeMXBean.getVmVendor());
            System.out.println("  JVM Version: " + runtimeMXBean.getVmVersion());

            System.out.println("\nRuntime Information:");
            System.out.println("  Available Processors: " + Runtime.getRuntime().availableProcessors());
            System.out.println("  Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
            System.out.println("  Total Memory: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " MB");
        }

        System.out.println("\nFor more information, visit: https://github.com/Chachamaru127/java-harness");
    }

    private void outputJsonVersion(boolean detailed) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(VERSION).append("\",\n");
        json.append("  \"buildDate\": \"").append(BUILD_DATE).append("\",\n");
        json.append("  \"java\": {\n");
        json.append("    \"version\": \"").append(System.getProperty("java.version")).append("\",\n");
        json.append("    \"home\": \"").append(System.getProperty("java.home")).append("\",\n");
        json.append("    \"os\": \"").append(System.getProperty("os.name")).append("\"\n");
        json.append("  }\n");

        if (detailed) {
            json.append("  ,\"runtime\": {\n");
            json.append("    \"availableProcessors\": ").append(Runtime.getRuntime().availableProcessors()).append(",\n");
            json.append("    \"maxMemory\": ").append(Runtime.getRuntime().maxMemory()).append(",\n");
            json.append("    \"totalMemory\": ").append(Runtime.getRuntime().totalMemory()).append("\n");
            json.append("  }\n");
        }

        json.append("}\n");
        System.out.print(json.toString());
    }
}
