package com.chachamaru.harness.workflow.scope;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic task-scope inference and write/drop checks. */
public final class ScopeLeash {
    private static final Pattern TASK_ROW = Pattern.compile("(?m)^\\s*\\|\\s*([^|]+?)\\s*\\|([^\\n]*)$");
    private static final Pattern PATH = Pattern.compile("(?i)(?:^|[^A-Za-z0-9._/-])((?:\\.?claude|src|app|cmd|go|lib|pkg|internal|docs|scripts|tests|agents|skills|hooks|templates|frontend|mcp-server|harness-ui)/[A-Za-z0-9._/-]*\\.[A-Za-z0-9]+)");

    private ScopeLeash() {
    }

    public static List<String> inferScopeFromPlan(String markdown, String taskId) {
        if (markdown == null || taskId == null) {
            return List.of();
        }
        String row = findRow(markdown, taskId.trim());
        if (row == null) {
            return List.of();
        }
        Set<String> paths = new LinkedHashSet<>();
        Matcher matcher = PATH.matcher(row);
        while (matcher.find()) {
            paths.add(normalize(matcher.group(1)));
        }
        return paths.stream().sorted().toList();
    }

    public static boolean checkWrite(List<String> scope, String targetPath, String projectRoot) {
        String target = relative(targetPath, projectRoot);
        for (String entry : scope == null ? List.<String>of() : scope) {
            String normalized = normalize(entry);
            if (target.equals(normalized) || target.startsWith(normalized + "/")) {
                return true;
            }
        }
        return false;
    }

    public static List<String> droppedScope(List<String> scope, List<String> touched) {
        Set<String> touchedSet = new LinkedHashSet<>();
        for (String path : touched == null ? List.<String>of() : touched) {
            touchedSet.add(normalize(path));
        }
        List<String> dropped = new ArrayList<>();
        for (String path : scope == null ? List.<String>of() : scope) {
            String normalized = normalize(path);
            if (!touchedSet.contains(normalized)) {
                dropped.add(normalized);
            }
        }
        return dropped.stream().distinct().sorted().toList();
    }

    private static String findRow(String markdown, String taskId) {
        Matcher matcher = TASK_ROW.matcher(markdown);
        while (matcher.find()) {
            if (matcher.group(1).trim().equals(taskId)) {
                return matcher.group();
            }
        }
        return null;
    }

    private static String relative(String targetPath, String projectRoot) {
        String target = normalize(targetPath);
        if (projectRoot == null || projectRoot.isBlank()) {
            return target;
        }
        String root = normalize(projectRoot);
        if (target.equals(root)) {
            return "";
        }
        if (target.startsWith(root + "/")) {
            return target.substring(root.length() + 1);
        }
        try {
            Path targetPathObject = Path.of(targetPath).toAbsolutePath().normalize();
            Path rootPathObject = Path.of(projectRoot).toAbsolutePath().normalize();
            return normalize(rootPathObject.relativize(targetPathObject).toString());
        } catch (RuntimeException ignored) {
            return target;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replace('\\', '/').replaceFirst("^\\./", "");
    }
}
