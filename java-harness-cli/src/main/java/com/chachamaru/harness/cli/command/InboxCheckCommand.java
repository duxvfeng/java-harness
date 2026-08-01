package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * InboxCheck command for checking agent inbox messages.
 *
 * <p>This command provides inbox checking capabilities:
 * <ul>
 *   <li>check - Check inbox messages for a team/agent</li>
 * </ul>
 * </p>
 */
@Command(name = "inbox-check",
         mixinStandardHelpOptions = true,
         subcommands = {
             InboxCheckCommand.CheckCommand.class
         },
         description = "Check agent inbox messages")
public class InboxCheckCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Check inbox messages
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Check inbox messages for a team/agent")
    public static class CheckCommand implements Callable<Integer> {

        @Option(names = {"--team"},
                 description = "Team name (default: from env or 'default')")
        String team;

        @Option(names = {"--agent"},
                 description = "Agent identifier")
        String agent;

        @Option(names = {"--db"},
                 description = "Livemsg database path")
        String dbPath;

        @Option(names = {"--from-env"},
                 description = "Resolve identity from environment variables")
        boolean fromEnv;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                InboxCheckOptions opts = resolveOptions();
                InboxChecker checker = new InboxChecker(verbose);
                InboxCheckResult result = checker.check(opts);

                if (result.unreadCount() == 0) {
                    return 0;
                }

                // Output JSON result
                outputJsonResult(result);

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Inbox check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private InboxCheckOptions resolveOptions() {
            String resolvedTeam = team;
            String resolvedAgent = agent;
            String resolvedDb = dbPath;

            // Resolve from environment if requested
            if (fromEnv) {
                DeliveryIdentity identity = DeliveryIdentity.resolve();
                if (identity != null) {
                    resolvedTeam = identity.team();
                    resolvedAgent = identity.agent();
                }
            }

            // Fall back to environment variables
            if (resolvedTeam == null || resolvedTeam.isEmpty()) {
                resolvedTeam = System.getenv().getOrDefault("HARNESS_LIVEMSG_TEAM", "default");
            }

            if (resolvedAgent == null || resolvedAgent.isEmpty()) {
                resolvedAgent = System.getenv().getOrDefault("HARNESS_LIVEMSG_AGENT", "");
            }

            // Resolve database path
            if (resolvedDb == null || resolvedDb.isEmpty()) {
                resolvedDb = resolveDefaultDbPath();
            }

            return new InboxCheckOptions(resolvedTeam, resolvedAgent, resolvedDb);
        }

        private String resolveDefaultDbPath() {
            // Try plugin data directory
            String pluginData = System.getenv("CLAUDE_PLUGIN_DATA");
            if (pluginData != null && !pluginData.isEmpty()) {
                return Paths.get(pluginData, "livemsg.db").toString();
            }

            // Try project directory
            String projectDir = System.getenv("CLAUDE_PROJECT_DIR");
            if (projectDir != null && !projectDir.isEmpty()) {
                return Paths.get(projectDir, ".harness", "livemsg.db").toString();
            }

            // Try current working directory
            String cwd = System.getProperty("user.dir");
            if (cwd != null) {
                return Paths.get(cwd, ".harness", "livemsg.db").toString();
            }

            // Fall back to relative path
            return ".harness/livemsg.db";
        }

        private void outputJsonResult(InboxCheckResult result) {
            System.out.println("{");
            System.out.println("  \"team\": \"" + result.team() + "\",");
            System.out.println("  \"agent\": \"" + result.agent() + "\",");
            System.out.println("  \"unread\": " + result.unreadCount() + ",");
            System.out.println("  \"messages\": [");

            List<InboxMessage> messages = result.messages();
            for (int i = 0; i < messages.size(); i++) {
                InboxMessage msg = messages.get(i);
                System.out.println("    {");
                System.out.println("      \"id\": \"" + msg.id() + "\",");
                System.out.println("      \"team\": \"" + msg.team() + "\",");
                System.out.println("      \"from_agent\": \"" + msg.fromAgent() + "\",");
                System.out.println("      \"to_agent\": \"" + msg.toAgent() + "\",");
                System.out.println("      \"subject\": \"" + escapeJson(msg.subject()) + "\",");
                System.out.println("      \"body\": \"" + escapeJson(msg.body()) + "\",");
                System.out.println("      \"created_at\": \"" + msg.createdAt().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
                System.out.println("    }" + (i < messages.size() - 1 ? "," : ""));
            }

            System.out.println("  ],");

            if (result.injectContext() != null && !result.injectContext().isEmpty()) {
                System.out.println("  \"inject_context\": \"" +
                    escapeJson(result.injectContext()) + "\"");
            } else {
                System.out.println("  \"inject_context\": \"\"");
            }

            System.out.println("}");
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Records
     */
    public record InboxCheckOptions(
        String team,
        String agent,
        String dbPath
    ) {
        public InboxCheckOptions {
            if (team == null) team = "default";
            if (agent == null) agent = "";
            if (dbPath == null) dbPath = "";
        }
    }

    public record InboxMessage(
        String id,
        String team,
        String fromAgent,
        String toAgent,
        String subject,
        String body,
        LocalDateTime createdAt
    ) {
        public InboxMessage {
            if (id == null) id = "";
            if (team == null) team = "";
            if (fromAgent == null) fromAgent = "";
            if (toAgent == null) toAgent = "";
            if (subject == null) subject = "";
            if (body == null) body = "";
            if (createdAt == null) createdAt = LocalDateTime.now();
        }
    }

    public record InboxCheckResult(
        String team,
        String agent,
        int unreadCount,
        List<InboxMessage> messages,
        String injectContext
    ) {
        public InboxCheckResult {
            if (team == null) team = "";
            if (agent == null) agent = "";
            if (messages == null) messages = List.of();
            if (injectContext == null) injectContext = "";
        }
    }

    /**
     * Delivery identity resolver
     */
    public static class DeliveryIdentity {
        private final String team;
        private final String agent;

        public DeliveryIdentity(String team, String agent) {
            this.team = team;
            this.agent = agent;
        }

        public static DeliveryIdentity resolve() {
            String team = System.getenv().getOrDefault("HARNESS_LIVEMSG_TEAM", "");
            String agent = System.getenv().getOrDefault("HARNESS_LIVEMSG_AGENT", "");

            if (!team.isEmpty() && !agent.isEmpty()) {
                return new DeliveryIdentity(team, agent);
            }

            return null;
        }

        public String team() {
            return team != null ? team : "";
        }

        public String agent() {
            return agent != null ? agent : "";
        }
    }

    /**
     * Inbox checker - checks livemsg database for messages
     */
    public static class InboxChecker {
        private final boolean verbose;
        private static final String INBOX_QUERY =
            "SELECT id, team, from_agent, to_agent, subject, body, created_at " +
            "FROM livemsg " +
            "WHERE team = ? AND to_agent = ? " +
            "ORDER BY created_at ASC";

        private static final String MARK_READ_QUERY =
            "UPDATE livemsg SET read_by = ? WHERE id = ?";

        public InboxChecker(boolean verbose) {
            this.verbose = verbose;
        }

        public InboxCheckResult check(InboxCheckOptions opts) {
            try {
                // Check if database exists
                Path dbPath = Paths.get(opts.dbPath());
                if (!Files.exists(dbPath)) {
                    if (verbose) {
                        System.out.println("Livemsg database not found: " + opts.dbPath());
                    }
                    return emptyResult(opts);
                }

                // Check if agent is resolved
                if (opts.agent() == null || opts.agent().isEmpty()) {
                    // Try to resolve from stdin
                    String resolvedAgent = resolveAgentFromStdin();
                    if (resolvedAgent == null || resolvedAgent.isEmpty()) {
                        return emptyResult(opts);
                    }
                    opts = new InboxCheckOptions(opts.team(), resolvedAgent, opts.dbPath());
                }

                // Open database and query messages
                List<InboxMessage> messages = queryMessages(opts);

                if (messages.isEmpty()) {
                    return emptyResult(opts);
                }

                // Mark messages as read
                markMessagesAsRead(opts, messages);

                // Build inject context
                String injectContext = buildInjectContext(messages);

                return new InboxCheckResult(
                    opts.team(),
                    opts.agent(),
                    messages.size(),
                    messages,
                    injectContext
                );

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Inbox check failed: " + e.getMessage());
                }
                return emptyResult(opts);
            }
        }

        private InboxCheckResult emptyResult(InboxCheckOptions opts) {
            return new InboxCheckResult(
                opts.team(),
                opts.agent(),
                0,
                List.of(),
                ""
            );
        }

        private String resolveAgentFromStdin() {
            try {
                // Check if stdin has data
                if (System.in.available() == 0) {
                    return "";
                }

                // Read stdin
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line = reader.readLine();

                if (line == null || line.trim().isEmpty()) {
                    return "";
                }

                // Try to parse as JSON hint
                // Expected format: {"session_id": "agent-id"}
                if (line.contains("session_id")) {
                    String[] parts = line.split("\"session_id\"\\s*:\\s*\"");
                    if (parts.length > 1) {
                        String sessionId = parts[1].split("\"")[0];
                        return sessionId.trim();
                    }
                }

                return "";

            } catch (Exception e) {
                return "";
            }
        }

        private List<InboxMessage> queryMessages(InboxCheckOptions opts) throws SQLException {
            List<InboxMessage> messages = new ArrayList<>();

            String url = "jdbc:sqlite:" + opts.dbPath();

            try (Connection conn = DriverManager.getConnection(url)) {
                PreparedStatement stmt = conn.prepareStatement(INBOX_QUERY);
                stmt.setString(1, opts.team());
                stmt.setString(2, opts.agent());

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    InboxMessage msg = new InboxMessage(
                        rs.getString("id"),
                        rs.getString("team"),
                        rs.getString("from_agent"),
                        rs.getString("to_agent"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        parseDateTime(rs.getString("created_at"))
                    );
                    messages.add(msg);
                }

            } catch (SQLException e) {
                if (verbose) {
                    System.err.println("Database query failed: " + e.getMessage());
                }
                throw e;
            }

            return messages;
        }

        private void markMessagesAsRead(InboxCheckOptions opts, List<InboxMessage> messages) {
            String url = "jdbc:sqlite:" + opts.dbPath();

            try (Connection conn = DriverManager.getConnection(url)) {
                conn.setAutoCommit(false);

                try {
                    PreparedStatement stmt = conn.prepareStatement(MARK_READ_QUERY);
                    stmt.setString(1, opts.agent());

                    for (InboxMessage msg : messages) {
                        stmt.setString(2, msg.id());
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                    conn.commit();

                } catch (SQLException e) {
                    conn.rollback();
                    if (verbose) {
                        System.err.println("Failed to mark messages as read: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                if (verbose) {
                    System.err.println("Database connection failed: " + e.getMessage());
                }
            }
        }

        private String buildInjectContext(List<InboxMessage> messages) {
            if (messages.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("You have received the following messages:\n");

            for (int i = 0; i < messages.size(); i++) {
                InboxMessage msg = messages.get(i);
                context.append(String.format("%d. From %s: %s\n%s\n",
                    i + 1, msg.fromAgent(), msg.subject(), msg.body()));
            }

            return context.toString();
        }

        private LocalDateTime parseDateTime(String dateTimeStr) {
            if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                return LocalDateTime.now();
            }

            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new InboxCheckCommand()).execute(args);
        System.exit(exitCode);
    }
}
