package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Plans management command for watching and managing Plans.md files.
 *
 * <p>This command provides functionality to:
 * <ul>
 *   <li>watch - Monitor Plans.md file for changes</li>
 *   <li>status - Show current plans status</li>
 *   <li>backup - Create backup of current plans</li>
 *   <li>restore - Restore from backup</li>
 * </ul>
 * </p>
 */
@Command(name = "plans",
         mixinStandardHelpOptions = true,
         subcommands = {
             PlansCommand.WatchCommand.class,
             PlansCommand.StatusCommand.class,
             PlansCommand.BackupCommand.class,
             PlansCommand.RestoreCommand.class
         },
         description = "Watch and manage Plans.md files")
public class PlansCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Watch Plans.md for changes
     */
    @Command(name = "watch",
             mixinStandardHelpOptions = true,
             description = "Monitor Plans.md file for changes")
    public static class WatchCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to watch (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"-i", "--interval"},
                 description = "Check interval in seconds (default: 5)",
                 defaultValue = "5")
        private int interval;

        @Option(names = {"-o", "--once"},
                 description = "Check once and exit")
        private boolean once;

        @Override
        public void run() {
            try {
                Path watchPath = Paths.get(planFile);

                if (!Files.exists(watchPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.err.println("Create it first with: gen create");
                    System.exit(1);
                    return;
                }

                System.out.println("📡 Watching Plans.md: " + planFile);
                System.out.println("  Check interval: " + interval + " seconds");
                System.out.println();
                System.out.println("✓ Started watching. Press Ctrl+C to stop.");
                System.out.println();

                long lastModified = Files.getLastModifiedTime(watchPath).toMillis();
                long lastSize = Files.size(watchPath);

                while (true) {
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    Path parentDir = watchPath.getParent();

                    if (parentDir == null) {
                        parentDir = Paths.get(".");
                    }

                    parentDir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                    WatchKey key = watchService.poll(interval, TimeUnit.SECONDS);

                    if (key == null) {
                        // Timeout - check once more
                        if (once) {
                            long[] newSize = {lastSize};
                            checkForChanges(watchPath, lastModified, lastSize, newSize);
                            break;
                        }
                        watchService.close();
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();

                        if (filename.toString().equals(watchPath.getFileName().toString())) {
                            System.out.println("📝 [" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME) + "] " +
                                             kind + ": " + filename);

                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                boolean[] changed = {false};
                                long[] newSize = {lastSize};
                                changed[0] = checkForChanges(watchPath, lastModified, lastSize, newSize);
                                if (changed[0]) {
                                    lastModified = Files.getLastModifiedTime(watchPath).toMillis();
                                    lastSize = newSize[0];
                                }
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("⚠️  Plans.md was deleted!");
                                System.out.println("   Waiting for recreation...");
                            } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                System.out.println("✅ Plans.md was created/recreated");
                                long[] newSize = {lastSize};
                                checkForChanges(watchPath, lastModified, lastSize, newSize);
                                lastModified = Files.getLastModifiedTime(watchPath).toMillis();
                                lastSize = newSize[0];
                            }
                        }
                    }

                    boolean valid = key.reset();
                    watchService.close();

                    if (!valid) {
                        System.out.println("⚠️  Watch key no longer valid");
                        break;
                    }

                    if (once) {
                        break;
                    }
                }

            } catch (Exception e) {
                System.err.println("✗ Watch failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        private boolean checkForChanges(Path watchPath, long lastModified, long lastSize, long[] newSize) {
            try {
                long currentModified = Files.getLastModifiedTime(watchPath).toMillis();
                long currentSize = Files.size(watchPath);
                newSize[0] = currentSize;

                if (currentModified > lastModified || currentSize != lastSize) {
                    System.out.println("📋 File modified - analyzing changes...");

                    // Analyze changes
                    PlansAnalyzer analyzer = new PlansAnalyzer();
                    List<ChangeInfo> changes = analyzer.analyzeChanges(watchPath);

                    if (!changes.isEmpty()) {
                        System.out.println("📊 Changes detected:");
                        for (ChangeInfo change : changes) {
                            System.out.println("  [" + change.type + "] " + change.description);
                            if (change.details != null && !change.details.isEmpty()) {
                                System.out.println("      " + change.details);
                            }
                        }
                    } else {
                        System.out.println("ℹ️  File changed but no significant content changes detected");
                    }

                    return true;
                }
                return false;
            } catch (Exception e) {
                System.err.println("✗ Change check failed: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Show current plans status
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Show current plans status")
    public static class StatusCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to check (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        private boolean jsonOutput;

        @Override
        public void run() {
            try {
                Path plansPath = Paths.get(planFile);

                if (!Files.exists(plansPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                PlansAnalyzer analyzer = new PlansAnalyzer();
                PlansStatus status = analyzer.analyzeStatus(plansPath);

                if (jsonOutput) {
                    // JSON output
                    System.out.println("{");
                    System.out.println("  \"file\": \"" + planFile + "\",");
                    System.out.println("  \"exists\": true,");
                    System.out.println("  \"tasks\": " + status.totalTasks + ",");
                    System.out.println("  \"completed\": " + status.completedTasks + ",");
                    System.out.println("  \"inProgress\": " + status.inProgressTasks + ",");
                    System.out.println("  \"todo\": " + status.todoTasks + ",");
                    System.out.println("  \"completionPercentage\": " + status.completionPercentage);
                    System.out.println("}");
                } else {
                    // Human-readable output
                    System.out.println("📋 Plans.md Status");
                    System.out.println("  File: " + planFile);
                    System.out.println("  Total tasks: " + status.totalTasks);
                    System.out.println("  Completed: " + status.completedTasks);
                    System.out.println("  In Progress: " + status.inProgressTasks);
                    System.out.println("  Todo: " + status.todoTasks);
                    System.out.println();

                    if (status.totalTasks > 0) {
                        int percentage = status.completionPercentage;
                        System.out.print("  Progress: [");
                        int bars = percentage / 10;
                        for (int i = 0; i < 10; i++) {
                            System.out.print(i < bars ? "=" : (i == bars ? ">" : " "));
                        }
                        System.out.println("] " + percentage + "%");
                    }

                    System.out.println();
                    System.out.println("  Last modified: " + status.lastModified);
                }

            } catch (Exception e) {
                System.err.println("✗ Status check failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Create backup of current plans
     */
    @Command(name = "backup",
             mixinStandardHelpOptions = true,
             description = "Create backup of current plans")
    public static class BackupCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to backup (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"-o", "--output"},
                 description = "Backup file path (default: Plans.md.backup.YYYYMMDD-HHMMSS)")
        private String outputFile;

        @Override
        public void run() {
            try {
                Path sourcePath = Paths.get(planFile);

                if (!Files.exists(sourcePath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                String backupName = outputFile != null ? outputFile : "Plans.md.backup." + timestamp;
                Path backupPath = Paths.get(backupName);

                Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("✓ Backup created: " + backupName);
                System.out.println("  Source: " + planFile);
                System.out.println("  Size: " + Files.size(backupPath) + " bytes");

            } catch (Exception e) {
                System.err.println("✗ Backup failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Restore from backup
     */
    @Command(name = "restore",
             mixinStandardHelpOptions = true,
             description = "Restore from backup")
    public static class RestoreCommand implements Runnable {

        @Parameters(index = "0", description = "Backup file to restore from")
        private String backupFile;

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to restore to (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--force"},
                 description = "Overwrite existing file without confirmation")
        private boolean force;

        @Override
        public void run() {
            try {
                Path backupPath = Paths.get(backupFile);

                if (!Files.exists(backupPath)) {
                    System.err.println("✗ Backup file not found: " + backupFile);
                    System.exit(1);
                    return;
                }

                Path targetPath = Paths.get(planFile);

                if (Files.exists(targetPath) && !force) {
                    System.out.println("⚠️  Target file exists: " + planFile);
                    System.out.println("   Backup to restore: " + backupFile);
                    System.out.println("   Size: " + Files.size(backupPath) + " bytes");
                    System.out.println();
                    System.out.print("   Restore? (y/N): ");

                    try {
                        java.util.Scanner scanner = new java.util.Scanner(System.in);
                        String response = scanner.nextLine().trim();
                        if (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("yes")) {
                            System.out.println("✗ Restore cancelled");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("✗ Failed to read confirmation");
                        System.exit(1);
                        return;
                    }
                }

                Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("✓ Restored from backup: " + backupFile);
                System.out.println("  Target: " + planFile);
                System.out.println("  Size: " + Files.size(targetPath) + " bytes");

            } catch (Exception e) {
                System.err.println("✗ Restore failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Plans analyzer for change detection and status analysis
     */
    public static class PlansAnalyzer {
        public List<ChangeInfo> analyzeChanges(Path plansPath) throws IOException {
            List<ChangeInfo> changes = new ArrayList<>();

            // Simple change detection based on file analysis
            List<String> lines = Files.readAllLines(plansPath);

            int todoCount = 0;
            int completedCount = 0;
            int inProgressCount = 0;

            for (String line : lines) {
                if (line.contains("cc:TODO")) {
                    todoCount++;
                } else if (line.contains("cc:completed")) {
                    completedCount++;
                } else if (line.contains("cc:in-progress")) {
                    inProgressCount++;
                }
            }

            // Add completion status change
            if (completedCount > 0) {
                ChangeInfo completionChange = new ChangeInfo();
                completionChange.type = "STATUS";
                completionChange.description = "Completion progress updated";
                completionChange.details = completedCount + " tasks completed";
                changes.add(completionChange);
            }

            // Add todo status change
            if (todoCount > 0) {
                ChangeInfo todoChange = new ChangeInfo();
                todoChange.type = "STATUS";
                todoChange.description = "Pending tasks";
                todoChange.details = todoCount + " tasks remaining";
                changes.add(todoChange);
            }

            return changes;
        }

        public PlansStatus analyzeStatus(Path plansPath) throws IOException {
            PlansStatus status = new PlansStatus();

            status.fileExists = Files.exists(plansPath);
            status.lastModified = Files.getLastModifiedTime(plansPath).toString();

            List<String> lines = Files.readAllLines(plansPath);

            for (String line : lines) {
                if (line.contains("cc:TODO")) {
                    status.todoTasks++;
                    status.totalTasks++;
                } else if (line.contains("cc:completed")) {
                    status.completedTasks++;
                    status.totalTasks++;
                } else if (line.contains("cc:in-progress")) {
                    status.inProgressTasks++;
                    status.totalTasks++;
                }
            }

            if (status.totalTasks > 0) {
                status.completionPercentage = (status.completedTasks * 100) / status.totalTasks;
            }

            return status;
        }
    }

    /**
     * Change information holder
     */
    public static class ChangeInfo {
        String type;
        String description;
        String details;
    }

    /**
     * Plans status holder
     */
    public static class PlansStatus {
        boolean fileExists;
        String lastModified;
        int totalTasks;
        int completedTasks;
        int inProgressTasks;
        int todoTasks;
        int completionPercentage;
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new PlansCommand()).execute(args);
        System.exit(exitCode);
    }
}