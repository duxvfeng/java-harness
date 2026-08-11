package com.chachamaru.harness.e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

/**
 * Project Type Detector for E2E Detection
 *
 * Detects project type (frontend/backend/fullstack) to enable smart skipping of E2E tests.
 *
 * @since 2.2.0
 */
public class ProjectTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(ProjectTypeDetector.class);

    // Frontend indicators
    private static final List<String> FRONTEND_FILES = List.of(
        "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
        "index.html", "index.htm", "index.jsx", "index.tsx",
        "vue.config.js", "vite.config.js", "webpack.config.js",
        "angular.json", "tsconfig.json", "tsconfig.app.json"
    );

    private static final List<String> FRONTEND_DIRS = List.of(
        "src/components", "src/views", "src/pages", "src/hooks",
        "public", "static", "assets", "styles",
        "node_modules", ".next", ".nuxt", "dist", "build"
    );

    private static final List<String> FRONTEND_FRAMEWORK_FILES = List.of(
        "react", "vue", "angular", "@angular/core", "svelte",
        "next", "nuxt", "gatsby", "remix", "astro"
    );

    // Backend indicators
    private static final List<String> BACKEND_FILES = List.of(
        "pom.xml", "build.gradle", "build.gradle.kts",
        "requirements.txt", "setup.py", "pyproject.toml",
        "go.mod", "go.sum",
        "composer.json", "composer.lock",
        "Gemfile", "Gemfile.lock",
        "Cargo.toml", "Cargo.lock"
    );

    private static final List<String> BACKEND_DIRS = List.of(
        "src/main", "src/test", "app", "lib", "pkg",
        "controllers", "models", "views", "services",
        "routes", "middleware", "handlers", "utils"
    );

    private final Path projectRoot;

    public ProjectTypeDetector(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /**
     * Detect project type
     */
    public ProjectType detect() {
        logger.info("Detecting project type for: {}", projectRoot);

        boolean hasFrontend = detectFrontend();
        boolean hasBackend = detectBackend();

        if (hasFrontend && hasBackend) {
            logger.info("Detected full-stack project");
            return ProjectType.FULLSTACK;
        } else if (hasFrontend) {
            logger.info("Detected frontend-only project");
            return ProjectType.FRONTEND_ONLY;
        } else if (hasBackend) {
            logger.info("Detected backend-only project");
            return ProjectType.BACKEND_ONLY;
        } else {
            logger.info("Unable to detect project type, assuming backend");
            return ProjectType.UNKNOWN;
        }
    }

    /**
     * Detect if project has frontend code
     */
    private boolean detectFrontend() {
        // Check for frontend indicator files
        for (String file : FRONTEND_FILES) {
            if (Files.exists(projectRoot.resolve(file))) {
                logger.debug("Found frontend indicator: {}", file);
                return true;
            }
        }

        // Check for frontend directories
        for (String dir : FRONTEND_DIRS) {
            if (Files.exists(projectRoot.resolve(dir))) {
                logger.debug("Found frontend directory: {}", dir);
                return true;
            }
        }

        // Check package.json for frontend frameworks
        Path packageJson = projectRoot.resolve("package.json");
        if (Files.exists(packageJson)) {
            try {
                String content = Files.readString(packageJson);
                for (String framework : FRONTEND_FRAMEWORK_FILES) {
                    if (content.contains(framework)) {
                        logger.debug("Found frontend framework in package.json: {}", framework);
                        return true;
                    }
                }
            } catch (IOException e) {
                logger.debug("Error reading package.json", e);
            }
        }

        // Check for frontend file patterns
        try {
            boolean hasJsFiles = Files.walk(projectRoot)
                .filter(Files::isRegularFile)
                .anyMatch(file -> file.toString().endsWith(".js") ||
                               file.toString().endsWith(".jsx") ||
                               file.toString().endsWith(".ts") ||
                               file.toString().endsWith(".tsx") ||
                               file.toString().endsWith(".vue") ||
                               file.toString().endsWith(".svelte"));

            if (hasJsFiles) {
                logger.debug("Found frontend JavaScript/TypeScript files");
                return true;
            }
        } catch (IOException e) {
            logger.debug("Error searching for frontend files", e);
        }

        return false;
    }

    /**
     * Detect if project has backend code
     */
    private boolean detectBackend() {
        // Check for backend indicator files
        for (String file : BACKEND_FILES) {
            if (Files.exists(projectRoot.resolve(file))) {
                logger.debug("Found backend indicator: {}", file);
                return true;
            }
        }

        // Check for backend directories
        for (String dir : BACKEND_DIRS) {
            if (Files.exists(projectRoot.resolve(dir))) {
                logger.debug("Found backend directory: {}", dir);
                return true;
            }
        }

        // Check for Java source files
        try {
            boolean hasJavaFiles = Files.walk(projectRoot)
                .filter(Files::isRegularFile)
                .anyMatch(file -> file.toString().endsWith(".java"));

            if (hasJavaFiles) {
                logger.debug("Found Java backend files");
                return true;
            }
        } catch (IOException e) {
            logger.debug("Error searching for Java files", e);
        }

        return false;
    }

    /**
     * Get recommended E2E test types based on project type
     */
    public List<String> getRecommendedTestTypes() {
        ProjectType type = detect();
        List<String> recommended = new ArrayList<>();

        switch (type) {
            case FRONTEND_ONLY:
                recommended.add("frontend");
                break;
            case BACKEND_ONLY:
                recommended.add("backend");
                recommended.add("api");
                break;
            case FULLSTACK:
                recommended.add("frontend");
                recommended.add("backend");
                recommended.add("integration");
                break;
            case UNKNOWN:
            default:
                // Default to backend tests
                recommended.add("backend");
                break;
        }

        return recommended;
    }

    /**
     * Check if frontend tests should be skipped
     */
    public boolean shouldSkipFrontendTests() {
        ProjectType type = detect();
        return type == ProjectType.BACKEND_ONLY || type == ProjectType.UNKNOWN;
    }

    /**
     * Check if backend tests should be skipped
     */
    public boolean shouldSkipBackendTests() {
        ProjectType type = detect();
        return type == ProjectType.FRONTEND_ONLY;
    }

    /**
     * Get detection report
     */
    public ProjectDetectionReport getReport() {
        ProjectType type = detect();
        ProjectDetectionReport report = new ProjectDetectionReport();
        report.setProjectType(type);
        report.setHasFrontend(type == ProjectType.FRONTEND_ONLY || type == ProjectType.FULLSTACK);
        report.setHasBackend(type == ProjectType.BACKEND_ONLY || type == ProjectType.FULLSTACK);
        report.setRecommendedTestTypes(getRecommendedTestTypes());
        report.setShouldSkipFrontend(shouldSkipFrontendTests());
        report.setShouldSkipBackend(shouldSkipBackendTests());

        return report;
    }

    /**
     * Project type enum
     */
    public enum ProjectType {
        FRONTEND_ONLY,   // Only frontend code
        BACKEND_ONLY,    // Only backend code
        FULLSTACK,       // Both frontend and backend
        UNKNOWN          // Unable to detect
    }

    /**
     * Project detection report
     */
    public static class ProjectDetectionReport {
        private ProjectType projectType;
        private boolean hasFrontend;
        private boolean hasBackend;
        private List<String> recommendedTestTypes = new ArrayList<>();
        private boolean shouldSkipFrontend;
        private boolean shouldSkipBackend;

        // Getters and setters
        public ProjectType getProjectType() {
            return projectType;
        }

        public void setProjectType(ProjectType projectType) {
            this.projectType = projectType;
        }

        public boolean hasFrontend() {
            return hasFrontend;
        }

        public void setHasFrontend(boolean hasFrontend) {
            this.hasFrontend = hasFrontend;
        }

        public boolean hasBackend() {
            return hasBackend;
        }

        public void setHasBackend(boolean hasBackend) {
            this.hasBackend = hasBackend;
        }

        public List<String> getRecommendedTestTypes() {
            return recommendedTestTypes;
        }

        public void setRecommendedTestTypes(List<String> recommendedTestTypes) {
            this.recommendedTestTypes = recommendedTestTypes;
        }

        public boolean shouldSkipFrontend() {
            return shouldSkipFrontend;
        }

        public void setShouldSkipFrontend(boolean shouldSkipFrontend) {
            this.shouldSkipFrontend = shouldSkipFrontend;
        }

        public boolean shouldSkipBackend() {
            return shouldSkipBackend;
        }

        public void setShouldSkipBackend(boolean shouldSkipBackend) {
            this.shouldSkipBackend = shouldSkipBackend;
        }

        /**
         * Get summary report
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Project Detection Report\n");
            sb.append("========================\n");
            sb.append("Project Type: ").append(projectType).append("\n");
            sb.append("Has Frontend: ").append(hasFrontend).append("\n");
            sb.append("Has Backend: ").append(hasBackend).append("\n");
            sb.append("Skip Frontend Tests: ").append(shouldSkipFrontend).append("\n");
            sb.append("Skip Backend Tests: ").append(shouldSkipBackend).append("\n");
            sb.append("Recommended Test Types: ").append(String.join(", ", recommendedTestTypes)).append("\n");
            return sb.toString();
        }
    }
}