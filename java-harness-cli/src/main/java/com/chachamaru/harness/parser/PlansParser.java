package com.chachamaru.harness.parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Plans.md files.
 * Supports both table format and checkbox format for task definitions.
 */
public class PlansParser {

    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile(
            "^\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|$"
    );

    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^-\\s+\\[([ x])\\]\\s+(.+)$");
    private static final Pattern TASK_HEADER_PATTERN = Pattern.compile("^##\\s+([A-Z0-9-]+):\\s*(.+)$");

    /**
     * Parse Plans.md markdown content and extract tasks.
     *
     * @param markdown The markdown content
     * @return List of parsed tasks
     */
    public static List<Task> parse(String markdown) {
        List<Task> tasks = new ArrayList<>();

        if (markdown == null || markdown.trim().isEmpty()) {
            return tasks;
        }

        String[] lines = markdown.split("\n");
        Task currentTask = null;
        boolean inTable = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Check for task header (checkbox format)
            Matcher headerMatcher = TASK_HEADER_PATTERN.matcher(trimmedLine);
            if (headerMatcher.find()) {
                if (currentTask != null) {
                    tasks.add(currentTask);
                }
                currentTask = new Task();
                currentTask.setId(headerMatcher.group(1));
                currentTask.setTitle(headerMatcher.group(2));
                inTable = false;
                continue;
            }

            // Check for table header
            if (trimmedLine.startsWith("|") && trimmedLine.contains("Task") && trimmedLine.contains("Content")) {
                inTable = true;
                continue;
            }

            // Skip table separator line
            if (trimmedLine.startsWith("|--")) {
                continue;
            }

            // Parse table row
            if (inTable && trimmedLine.startsWith("|")) {
                Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(trimmedLine);
                if (rowMatcher.find()) {
                    if (currentTask != null) {
                        tasks.add(currentTask);
                    }
                    currentTask = new Task();
                    currentTask.setId(rowMatcher.group(1).trim());
                    currentTask.setContent(rowMatcher.group(2).trim());
                    currentTask.setDod(rowMatcher.group(3).trim());
                    currentTask.setDepends(rowMatcher.group(4).trim());
                    currentTask.setStatus(rowMatcher.group(5).trim());
                }
                continue;
            }

            // Parse checkbox (for both formats)
            Matcher checkboxMatcher = CHECKBOX_PATTERN.matcher(trimmedLine);
            if (checkboxMatcher.find() && currentTask != null) {
                boolean completed = checkboxMatcher.group(1).equals("x");
                String text = checkboxMatcher.group(2).trim();
                currentTask.addSubtask(text, completed);
            }
        }

        // Add the last task
        if (currentTask != null) {
            tasks.add(currentTask);
        }

        return tasks;
    }

    /**
     * Extract dependency relationships from tasks.
     *
     * @param tasks List of tasks
     * @return List of task dependencies
     */
    public static List<TaskDependency> extractDependencies(List<Task> tasks) {
        List<TaskDependency> dependencies = new ArrayList<>();

        for (Task task : tasks) {
            String depends = task.getDepends();
            if (depends != null && !depends.equals("-") && !depends.trim().isEmpty()) {
                dependencies.add(new TaskDependency(task.getId(), depends));
            }
        }

        return dependencies;
    }

    /**
     * Find a task by its ID.
     *
     * @param tasks List of tasks
     * @param taskId Task ID to find
     * @return Optional containing the task if found
     */
    public static Optional<Task> findTaskById(List<Task> tasks, String taskId) {
        return tasks.stream()
                .filter(t -> taskId.equals(t.getId()))
                .findFirst();
    }

    /**
     * Validate task dependencies for circular references.
     *
     * @param tasks List of tasks
     * @return true if dependencies are valid (no circular references)
     */
    public static boolean validateDependencies(List<Task> tasks) {
        Map<String, Task> taskMap = new HashMap<>();
        for (Task task : tasks) {
            taskMap.put(task.getId(), task);
        }

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (Task task : tasks) {
            if (!visited.contains(task.getId())) {
                if (hasCircularDependency(task, taskMap, visiting, visited)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean hasCircularDependency(Task task, Map<String, Task> taskMap,
                                                  Set<String> visiting, Set<String> visited) {
        String taskId = task.getId();

        if (visited.contains(taskId)) {
            return false;
        }

        if (visiting.contains(taskId)) {
            return true; // Circular dependency detected
        }

        visiting.add(taskId);

        String depends = task.getDepends();
        if (depends != null && !depends.equals("-") && !depends.trim().isEmpty()) {
            // Handle comma-separated dependencies
            String[] depIds = depends.split(",");
            for (String depId : depIds) {
                depId = depId.trim();
                Task depTask = taskMap.get(depId);
                if (depTask != null) {
                    if (hasCircularDependency(depTask, taskMap, visiting, visited)) {
                        return true;
                    }
                }
            }
        }

        visiting.remove(taskId);
        visited.add(taskId);
        return false;
    }
}
