package com.chachamaru.harness.security;

import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Non-overridable command-level safety floor executed before normal rules. */
public final class RuntimeFloor {
    private static final Pattern BILLING = Pattern.compile("\\b(stripe\\s+|paypal\\b|aws\\s+ce\\s+|gcloud\\s+billing\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EGRESS_TOOL = Pattern.compile("\\b(curl|wget|nc|scp|rsync)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("(?i)(?:https?|ftp)://([^\\s/]+)");
    private static final Pattern SECRET = Pattern.compile("(?i)(~/.aws|/\\.aws/|~/.ssh|/\\.ssh/|(?:^|[\\s/])\\.env(?:\\b|/)|\\.pem\\b|\\.key\\b|\\bcredentials\\b)");
    private static final Pattern ABSOLUTE_PATH = Pattern.compile("(?i)(?:[A-Z]:[\\\\/][^\\s;|&]+|/(?:Users|home|etc|var|tmp)/[^\\s;|&]+)");

    private RuntimeFloor() {
    }

    public enum Category {
        MONEY_BILLING("money-billing"), EGRESS("egress"), SECRET_READ("secret-read"),
        PROD_DEPLOY("prod-deploy"), WORKTREE_ESCAPE("worktree-escape");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record Context(Path worktreeRoot) {
    }

    public record Decision(boolean stopped, Category category, String pattern, String reason) {
        public static Decision allow() {
            return new Decision(false, null, "", "");
        }
    }

    public static Decision check(String command, Context context) {
        String normalized = stripNonExecutableText(command == null ? "" : command).trim();
        if (normalized.isEmpty()) {
            return Decision.allow();
        }

        Matcher billing = BILLING.matcher(normalized);
        if (billing.find()) {
            return stop(Category.MONEY_BILLING, billing.group(1), "money/billing command requires human approval");
        }

        if (EGRESS_TOOL.matcher(normalized).find() && !isLocalEgress(normalized)) {
            Matcher url = URL.matcher(normalized);
            String pattern = url.find() ? url.group(0) : "external-network";
            return stop(Category.EGRESS, pattern, "external network egress requires human approval");
        }

        Matcher secret = SECRET.matcher(normalized);
        if (looksLikeSecretRead(normalized) && secret.find()) {
            return stop(Category.SECRET_READ, secret.group(1), "credential or secret read requires human approval");
        }

        if (isProductionDeploy(normalized)) {
            return stop(Category.PROD_DEPLOY, normalized, "production deployment requires human approval");
        }

        if (context != null && context.worktreeRoot() != null && escapesWorktree(normalized, context.worktreeRoot())) {
            return stop(Category.WORKTREE_ESCAPE, "absolute-path", "path outside the active worktree requires human approval");
        }
        return Decision.allow();
    }

    private static Decision stop(Category category, String pattern, String detail) {
        return new Decision(true, category, pattern, "runtime action hard floor: " + detail + " (" + pattern + ")");
    }

    private static boolean looksLikeSecretRead(String command) {
        return Pattern.compile("(?i)\\b(cat|less|head|grep|cp|more|tail|sed)\\b").matcher(command).find();
    }

    private static boolean isLocalEgress(String command) {
        Matcher matcher = URL.matcher(command);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String host = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!(host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("localhost:") || host.startsWith("127.0.0.1:"))) {
                return false;
            }
        }
        return found;
    }

    private static boolean isProductionDeploy(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\b(npm\\s+publish|kubectl\\s+apply|terraform\\s+apply|vercel\\s+.*--prod|gh\\s+release\\s+).*\\b.*")
            || lower.matches(".*\\bgit\\s+push\\b.*(--tags|origin\\s+v).*" );
    }

    private static boolean escapesWorktree(String command, Path root) {
        String normalizedRoot = root.toAbsolutePath().normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String commandWithoutUrls = URL.matcher(command).replaceAll("");
        Matcher matcher = ABSOLUTE_PATH.matcher(commandWithoutUrls);
        while (matcher.find()) {
            String candidate = matcher.group().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (candidate.startsWith(normalizedRoot + "/") || candidate.equals(normalizedRoot)) {
                continue;
            }
            if (candidate.startsWith("/tmp/") && normalizedRoot.startsWith("/tmp/")) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static String stripNonExecutableText(String command) {
        StringBuilder result = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                result.append(c);
            } else if (c == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                result.append(c);
            } else if (c == '#' && !singleQuote && !doubleQuote) {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
