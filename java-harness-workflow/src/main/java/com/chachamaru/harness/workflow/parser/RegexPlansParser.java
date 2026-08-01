package com.chachamaru.harness.workflow.parser;

import com.chachamaru.harness.workflow.model.PlansDocument;
import com.chachamaru.harness.protocol.model.Status;
import com.chachamaru.harness.protocol.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regular expression-based Plans.md parser.
 *
 * <p>Parses Plans.md files in table format with status markers.
 * Supports:
 * <ul>
 *   <li>Tables with columns: Task | Content | DoD | Depends | Status</li>
 *   <li>Status markers: cc:TODO, cc:WIP, cc:DONE, cc:✅, cc:WITHDRAWN</li>
 *   <li>Phase headers and metadata</li>
 * </ul>
 *
 * @spec_reference spec.md#Workflow System
 */
public class RegexPlansParser implements PlansParser {

    // Table row patterns - support both 5-column and 6-column formats
    // 5-column: | ID | Title | Description | Depends | Status |
    // 6-column (Plans.md): | ID | Title | Description | DoD | Depends | Status |
    private static final Pattern TABLE_ROW_PATTERN_5COL = Pattern.compile(
        "^\\|\\s*(\\d+\\.\\d+\\.\\d+)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|$"
    );

    private static final Pattern TABLE_ROW_PATTERN_6COL = Pattern.compile(
        "^\\|\\s*(\\d+\\.\\d+\\.\\d+)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|$"
    );

    // Status marker patterns
    private static final Pattern TODO_PATTERN = Pattern.compile("cc:(TODO|[:📝])");
    private static final Pattern WIP_PATTERN = Pattern.compile("cc:(WIP|[:🔄])");
    private static final Pattern DONE_PATTERN = Pattern.compile("cc:(DONE|✅|[:✅])");
    private static final Pattern WITHDRAWN_PATTERN = Pattern.compile("cc:(WITHDRAWN|[:❌])");
    private static final Pattern PM_APPROVED_PATTERN = Pattern.compile("pm:approved|PM_APPROVED");

    // Header patterns
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s+(.+)$");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\*\\*Created.*?\\*\\*:\\s*(.+)");

    @Override
    public PlansDocument parse(Path plansPath) throws ParseException {
        try {
            String content = Files.readString(plansPath);
            return parseString(content, plansPath.toString());
        } catch (IOException e) {
            throw new ParseException("Failed to read file: " + e.getMessage(), e, 0, plansPath.toString());
        }
    }

    @Override
    public PlansDocument parseString(String content, String sourcePath) throws ParseException {
        String[] lines = content.split("\n");
        String title = "Untitled Plan";
        LocalDateTime lastModified = LocalDateTime.now();
        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Parse title
            Matcher titleMatcher = TITLE_PATTERN.matcher(line);
            if (titleMatcher.find()) {
                title = titleMatcher.group(1).trim();
                continue;
            }

            // Parse date
            Matcher dateMatcher = DATE_PATTERN.matcher(line);
            if (dateMatcher.find()) {
                try {
                    String dateStr = dateMatcher.group(1).trim();
                    // Try parsing common date formats
                    lastModified = parseDate(dateStr);
                } catch (Exception e) {
                    // Keep current time if parsing fails
                }
                continue;
            }

            // Parse table row
            Matcher rowMatcher6Col = TABLE_ROW_PATTERN_6COL.matcher(line);
            Matcher rowMatcher5Col = TABLE_ROW_PATTERN_5COL.matcher(line);

            if (rowMatcher6Col.find()) {
                try {
                    Task task = parseTableRow6Col(rowMatcher6Col, i + 1, sourcePath);
                    tasks.add(task);
                } catch (ParseException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ParseException("Failed to parse task row: " + e.getMessage(), e, i + 1, sourcePath);
                }
            } else if (rowMatcher5Col.find()) {
                try {
                    Task task = parseTableRow5Col(rowMatcher5Col, i + 1, sourcePath);
                    tasks.add(task);
                } catch (ParseException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ParseException("Failed to parse task row: " + e.getMessage(), e, i + 1, sourcePath);
                }
            }
        }

        return new PlansDocument(title, null, lastModified, tasks);
    }

    private Task parseTableRow6Col(Matcher matcher, int lineNumber, String source) throws ParseException {
        String id = matcher.group(1).trim();
        String title = matcher.group(2).trim();
        String description = matcher.group(3).trim();
        String acceptanceCriteria = matcher.group(4).trim();
        String dependsStr = matcher.group(5).trim();
        String statusMarker = matcher.group(6).trim();

        Status status = parseStatus(statusMarker, lineNumber, source);
        List<String> dependencies = parseDependencies(dependsStr);
        String lane = determineLane(title);

        return new Task(id, title, description, status, acceptanceCriteria, dependencies, lane);
    }

    private Task parseTableRow5Col(Matcher matcher, int lineNumber, String source) throws ParseException {
        String id = matcher.group(1).trim();
        String title = matcher.group(2).trim();
        String description = matcher.group(3).trim();
        String dependsStr = matcher.group(4).trim();
        String statusMarker = matcher.group(5).trim();

        Status status = parseStatus(statusMarker, lineNumber, source);
        List<String> dependencies = parseDependencies(dependsStr);
        String lane = determineLane(title);

        // 5-column format: description is used as both description and acceptance criteria
        return new Task(id, title, description, status, description, dependencies, lane);
    }

    private Status parseStatus(String marker, int lineNumber, String source) throws ParseException {
        if (DONE_PATTERN.matcher(marker).find()) {
            return Status.CC_DONE;
        } else if (WIP_PATTERN.matcher(marker).find()) {
            return Status.CC_WIP;
        } else if (TODO_PATTERN.matcher(marker).find()) {
            return Status.CC_TODO;
        } else if (WITHDRAWN_PATTERN.matcher(marker).find()) {
            return Status.CC_WITHDRAWN;
        } else if (PM_APPROVED_PATTERN.matcher(marker).find()) {
            return Status.PM_APPROVED;
        } else if (marker.toLowerCase().contains("pm_requested") || marker.toLowerCase().contains("requested")) {
            return Status.PM_REQUESTED;
        } else {
            // Default to TODO if no pattern matches
            return Status.CC_TODO;
        }
    }

    private List<String> parseDependencies(String dependsStr) {
        List<String> dependencies = new ArrayList<>();
        if (dependsStr == null || dependsStr.isBlank() || dependsStr.equals("-")) {
            return dependencies;
        }

        // Split by comma and trim
        String[] parts = dependsStr.split(",");
        for (String part : parts) {
            String depId = part.trim();
            if (!depId.isEmpty() && !depId.equals("-")) {
                dependencies.add(depId);
            }
        }

        return dependencies;
    }

    private String determineLane(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("review") || lowerTitle.contains("验证")) {
            return "review";
        } else if (lowerTitle.contains("release") || lowerTitle.contains("发布")) {
            return "release";
        } else {
            return "implementation";
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        // Try common date formats
        List<String> formats = List.of(
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy年MM月dd日",
            "MMMM d, yyyy",
            "dd MMMM yyyy"
        );

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }

        // Return current time if all formats fail
        return LocalDateTime.now();
    }
}
