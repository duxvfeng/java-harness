package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * ChannelsWake command for channel wake-up event management.
 *
 * <p>This command provides channel wake-up capabilities:
 * <ul>
 *   <li>check - Check channel wake events and status</li>
 * </ul>
 * </p>
 */
@Command(name = "channels-wake",
         mixinStandardHelpOptions = true,
         subcommands = {
             ChannelsWakeCommand.CheckCommand.class
         },
         description = "Manage channel wake-up events")
public class ChannelsWakeCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Check channel wake events
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Check channel wake events and status")
    public static class CheckCommand implements Callable<Integer> {

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        boolean jsonOutput;

        @Option(names = {"--event-dir"},
                 description = "Channel events directory")
        String eventDir;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                ChannelsWakeChecker checker = new ChannelsWakeChecker(verbose);
                ChannelsWakeResult result = checker.check(eventDir);

                if (jsonOutput) {
                    outputJsonResult(result);
                } else {
                    outputHumanResult(result);
                }

                return result.hasEvents() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Channel wake check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonResult(ChannelsWakeResult result) {
            System.out.println("{");
            System.out.println("  \"has_events\": " + result.hasEvents() + ",");
            System.out.println("  \"event_count\": " + result.eventCount() + ",");
            System.out.println("  \"last_event\": \"" + result.lastEvent().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
            System.out.println("  \"events\": [");

            List<ChannelWakeEvent> events = result.events();
            for (int i = 0; i < events.size(); i++) {
                ChannelWakeEvent event = events.get(i);
                System.out.println("    {");
                System.out.println("      \"id\": \"" + event.id() + "\",");
                System.out.println("      \"channel\": \"" + event.channel() + "\",");
                System.out.println("      \"source\": \"" + event.source() + "\",");
                System.out.println("      \"timestamp\": \"" + event.timestamp().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
                System.out.println("      \"processed\": " + event.processed());
                System.out.println("    }" + (i < events.size() - 1 ? "," : ""));
            }

            System.out.println("  ]");
            System.out.println("}");
        }

        private void outputHumanResult(ChannelsWakeResult result) {
            System.out.println();
            System.out.println("📡 Channel Wake Events");
            System.out.println();

            if (!result.hasEvents()) {
                System.out.println("  No channel wake events found");
                System.out.println();
                return;
            }

            System.out.println("  Total events: " + result.eventCount());
            System.out.println("  Last event: " + result.lastEvent().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println();

            System.out.println("  Recent events:");
            for (ChannelWakeEvent event : result.events()) {
                String status = event.processed() ? "✓" : "○";
                System.out.println("    " + status + " " + event.channel() +
                    " from " + event.source() + " at " +
                    event.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            System.out.println();
        }
    }

    /**
     * Records
     */
    public record ChannelWakeEvent(
        String id,
        String channel,
        String source,
        LocalDateTime timestamp,
        boolean processed
    ) {
        public ChannelWakeEvent {
            if (id == null) id = "";
            if (channel == null) channel = "";
            if (source == null) source = "";
            if (timestamp == null) timestamp = LocalDateTime.now();
        }
    }

    public record ChannelsWakeResult(
        boolean hasEvents,
        int eventCount,
        LocalDateTime lastEvent,
        List<ChannelWakeEvent> events
    ) {
        public ChannelsWakeResult {
            if (events == null) events = List.of();
        }
    }

    /**
     * Channels wake checker - checks for channel wake events
     */
    public static class ChannelsWakeChecker {
        private final boolean verbose;
        private static final String DEFAULT_EVENT_DIR = ".claude/state/channels-wake";
        private static final String CHANNEL_WAKE_SCHEMA = "channel-wake-event.v1.json";

        public ChannelsWakeChecker(boolean verbose) {
            this.verbose = verbose;
        }

        public ChannelsWakeResult check(String eventDirOverride) {
            try {
                String eventDir = eventDirOverride != null ?
                    eventDirOverride : getDefaultEventDir();

                Path eventPath = Paths.get(eventDir);
                if (!Files.exists(eventPath)) {
                    if (verbose) {
                        System.out.println("Channel wake event directory not found: " + eventDir);
                    }
                    return new ChannelsWakeResult(false, 0, LocalDateTime.now(), List.of());
                }

                List<ChannelWakeEvent> events = collectEvents(eventPath);

                if (events.isEmpty()) {
                    return new ChannelsWakeResult(false, 0, LocalDateTime.now(), List.of());
                }

                // Sort by timestamp descending
                events.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

                LocalDateTime lastEvent = events.stream()
                    .map(ChannelWakeEvent::timestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

                return new ChannelsWakeResult(true, events.size(), lastEvent, events);

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Channel wake check failed: " + e.getMessage());
                }
                return new ChannelsWakeResult(false, 0, LocalDateTime.now(), List.of());
            }
        }

        private String getDefaultEventDir() {
            // Check for project-specific event directory
            String projectDir = System.getProperty("user.dir");
            Path projectEventPath = Paths.get(projectDir, ".claude", "state", "channels-wake");
            if (Files.exists(projectEventPath)) {
                return projectEventPath.toString();
            }

            // Check for global event directory
            String home = System.getProperty("user.home");
            Path globalEventPath = Paths.get(home, ".claude", "state", "channels-wake");
            if (Files.exists(globalEventPath)) {
                return globalEventPath.toString();
            }

            // Fall back to default
            return DEFAULT_EVENT_DIR;
        }

        private List<ChannelWakeEvent> collectEvents(Path eventPath) throws Exception {
            List<ChannelWakeEvent> events = new ArrayList<>();

            // Collect all JSON files in the event directory
            try (var stream = Files.walk(eventPath, 1)) {
                stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(jsonFile -> {
                        try {
                            ChannelWakeEvent event = parseEventFile(jsonFile);
                            if (event != null) {
                                events.add(event);
                            }
                        } catch (Exception e) {
                            if (verbose) {
                                System.err.println("Failed to parse event file: " + jsonFile);
                            }
                        }
                    });
            }

            return events;
        }

        private ChannelWakeEvent parseEventFile(Path jsonFile) throws Exception {
            String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
            return parseEventJson(content);
        }

        private ChannelWakeEvent parseEventJson(String json) {
            // Simplified JSON parsing - in real implementation use Jackson
            // Expected format:
            // {
            //   "id": "uuid",
            //   "channel": "channel-name",
            //   "source": "source-agent",
            //   "timestamp": "2024-01-01T12:00:00",
            //   "processed": true/false
            // }

            try {
                String id = extractJsonString(json, "id");
                String channel = extractJsonString(json, "channel");
                String source = extractJsonString(json, "source");
                String timestampStr = extractJsonString(json, "timestamp");
                boolean processed = extractJsonBoolean(json, "processed");

                LocalDateTime timestamp = parseTimestamp(timestampStr);

                return new ChannelWakeEvent(id, channel, source, timestamp, processed);

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("JSON parsing failed: " + e.getMessage());
                }
                return null;
            }
        }

        private String extractJsonString(String json, String key) {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            return m.find() ? m.group(1) : "";
        }

        private boolean extractJsonBoolean(String json, String key) {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            return m.find() && Boolean.parseBoolean(m.group(1));
        }

        private LocalDateTime parseTimestamp(String timestampStr) {
            try {
                return LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ChannelsWakeCommand()).execute(args);
        System.exit(exitCode);
    }
}
