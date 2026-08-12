package com.chachamaru.harness.foundation.clientmirror;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Compares the skills SSOT with the configured client skill mirrors. */
public final class ClientMirror {
    public static final String SCHEMA_VERSION = "mirror-state.v1";
    public static final String REASON_NOT_CONFIGURED = "not-configured";
    public static final String REASON_IN_SYNC = "in-sync";
    public static final String REASON_DRIFT = "drift";

    private static final String SHARED_SSOT = "skills";
    private static final String CODEX_SSOT = "skills-codex";
    private static final List<String> MIRROR_ROOTS = List.of(
        ".agents/skills", "codex/.codex/skills", "opencode/skills");
    private static final Set<String> OPENCODE_SKIPPED = Set.of(
        "allow1", "cc-update-review", "claude-codex-upstream-update",
        "harness-release-internal", "zz-review-empty", "zz-review-escape");
    private static final Set<String> COMMON_EXCLUDES = Set.of(".DS_Store", ".claude");
    private ClientMirror() {
    }

    public record MirrorEntry(
        String root,
        String status,
        @JsonProperty("drift_count") int driftCount,
        List<String> drifts) {
        public MirrorEntry {
            drifts = drifts == null ? List.of() : List.copyOf(drifts);
        }
    }

    public record State(
        @JsonProperty("schema_version") String schemaVersion,
        String fingerprint,
        boolean healthy,
        String reason,
        List<MirrorEntry> mirrors,
        long ts) {
        public State {
            mirrors = mirrors == null ? List.of() : List.copyOf(mirrors);
        }
    }

    public static State scan(Path repoRoot, Instant now) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        List<MirrorEntry> mirrors = new ArrayList<>();
        for (String mirrorRoot : MIRROR_ROOTS) {
            mirrors.add(scanRoot(root, mirrorRoot));
        }

        boolean configured = mirrors.stream().anyMatch(entry -> !REASON_NOT_CONFIGURED.equals(entry.status()));
        boolean drifted = mirrors.stream().anyMatch(entry -> REASON_DRIFT.equals(entry.status()));
        String reason = drifted ? REASON_DRIFT : configured ? REASON_IN_SYNC : REASON_NOT_CONFIGURED;
        Instant timestamp = now == null ? Instant.now() : now;
        State provisional = new State(SCHEMA_VERSION, "", !drifted, reason, mirrors, timestamp.getEpochSecond());
        return new State(SCHEMA_VERSION, fingerprint(provisional), provisional.healthy(), provisional.reason(),
            provisional.mirrors(), provisional.ts());
    }

    public static List<String> diff(Path repoRoot) throws IOException {
        return scan(repoRoot, Instant.now()).mirrors().stream()
            .flatMap(entry -> entry.drifts().stream())
            .sorted()
            .toList();
    }

    public static String fingerprint(State state) {
        try {
            StringBuilder canonical = new StringBuilder();
            canonical.append(state.schemaVersion()).append('|')
                .append(state.healthy()).append('|').append(state.reason()).append('|');
            state.mirrors().stream().sorted(Comparator.comparing(MirrorEntry::root)).forEach(entry -> {
                canonical.append(entry.root()).append('|').append(entry.status()).append('|')
                    .append(entry.driftCount()).append('|');
                entry.drifts().stream().sorted().forEach(drift -> canonical.append(drift).append('|'));
            });
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("sha256:");
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static MirrorEntry scanRoot(Path repoRoot, String mirrorRel) throws IOException {
        Path mirror = repoRoot.resolve(mirrorRel.replace('/', java.io.File.separatorChar));
        if (!Files.exists(mirror, LinkOption.NOFOLLOW_LINKS)) {
            return new MirrorEntry(mirrorRel, REASON_NOT_CONFIGURED, 0, List.of());
        }
        if (!Files.isDirectory(mirror, LinkOption.NOFOLLOW_LINKS)) {
            return new MirrorEntry(mirrorRel, REASON_DRIFT, 1,
                List.of(mirrorRel + " is not a directory"));
        }

        List<String> drifts = "opencode/skills".equals(mirrorRel)
            ? diffOpenCode(repoRoot, mirror)
            : diffDirectory(repoRoot, mirrorRel, mirror);
        return new MirrorEntry(mirrorRel, drifts.isEmpty() ? REASON_IN_SYNC : REASON_DRIFT,
            drifts.size(), drifts);
    }

    private static List<String> diffDirectory(Path repoRoot, String mirrorRel, Path mirror) throws IOException {
        List<String> drifts = new ArrayList<>();
        try (var entries = Files.list(mirror)) {
            for (Path entry : entries.sorted().toList()) {
                String name = entry.getFileName().toString();
                if ("routing-rules.md".equals(name)) {
                    continue;
                }
                if (!Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                    || "node_modules".equals(name) || ".git".equals(name)) {
                    continue;
                }
                Path source = resolveSource(repoRoot, mirrorRel, name);
                if (source == null) {
                    continue;
                }
                if (Files.isSymbolicLink(entry)) {
                    drifts.add("symlink " + mirrorRel + "/" + name);
                } else if (!directoriesEqual(source, entry, COMMON_EXCLUDES)) {
                    drifts.add("drift " + mirrorRel + "/" + name);
                }
            }
        }

        Path sourceRules = repoRoot.resolve(SHARED_SSOT).resolve("routing-rules.md");
        Path mirrorRules = mirror.resolve("routing-rules.md");
        if (Files.isRegularFile(sourceRules) && Files.isRegularFile(mirrorRules)
            && !filesEqual(sourceRules, mirrorRules)) {
            drifts.add("drift " + mirrorRel + "/routing-rules.md");
        }
        return drifts.stream().sorted().toList();
    }

    private static List<String> diffOpenCode(Path repoRoot, Path mirror) throws IOException {
        Path shared = repoRoot.resolve(SHARED_SSOT);
        if (!Files.isDirectory(shared)) {
            return List.of("missing skills SSOT: skills");
        }
        Map<String, String> expected = new LinkedHashMap<>();
        try (var entries = Files.list(shared)) {
            for (Path entry : entries.filter(Files::isDirectory).sorted().toList()) {
                String skill = entry.getFileName().toString();
                if (isOpenCodeSkipped(skill) || !Files.isRegularFile(entry.resolve("SKILL.md"))) {
                    continue;
                }
                expected.put(normalizeOpenCodeName(skill), skill);
            }
        }
        Set<String> actual = new TreeSet<>();
        try (var entries = Files.list(mirror)) {
            entries.filter(Files::isDirectory).map(path -> path.getFileName().toString()).forEach(actual::add);
        }

        List<String> drifts = new ArrayList<>();
        if (!expected.keySet().equals(actual)) {
            drifts.add("drift opencode/skills generated skill set");
        }
        for (Map.Entry<String, String> item : expected.entrySet()) {
            Path source = shared.resolve(item.getValue());
            Path destination = mirror.resolve(item.getKey());
            if (!Files.isDirectory(destination)) {
                drifts.add("missing opencode skill mirror: " + item.getKey());
                continue;
            }
            if (!markdownBodiesEqual(source.resolve("SKILL.md"), destination.resolve("SKILL.md"))) {
                drifts.add("drift opencode/skills/" + item.getKey() + "/SKILL.md body");
            }
            if (!directoriesEqual(source, destination, Set.of("SKILL.md", "CLAUDE.md", "node_modules",
                "coverage", ".claude"))) {
                drifts.add("drift opencode/skills/" + item.getKey() + " support files");
            }
        }
        return drifts.stream().sorted().toList();
    }

    private static Path resolveSource(Path repoRoot, String mirrorRel, String skill) {
        if ("codex/.codex/skills".equals(mirrorRel)) {
            Path codex = repoRoot.resolve(CODEX_SSOT).resolve(skill);
            if (Files.isDirectory(codex, LinkOption.NOFOLLOW_LINKS)) {
                return codex;
            }
        }
        Path shared = repoRoot.resolve(SHARED_SSOT).resolve(skill);
        return Files.isDirectory(shared, LinkOption.NOFOLLOW_LINKS) ? shared : null;
    }

    private static boolean directoriesEqual(Path left, Path right, Set<String> excludes) throws IOException {
        Map<String, Path> leftEntries = entries(left, excludes);
        Map<String, Path> rightEntries = entries(right, excludes);
        if (!leftEntries.keySet().equals(rightEntries.keySet())) {
            return false;
        }
        for (String name : leftEntries.keySet()) {
            Path source = leftEntries.get(name);
            Path target = rightEntries.get(name);
            boolean sourceDir = Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS);
            if (sourceDir != Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
            if (sourceDir) {
                if (!directoriesEqual(source, target, excludes)) {
                    return false;
                }
            } else if (!filesEqual(source, target)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Path> entries(Path directory, Set<String> excludes) throws IOException {
        Map<String, Path> result = new LinkedHashMap<>();
        try (var stream = Files.list(directory)) {
            for (Path entry : stream.toList()) {
                String name = entry.getFileName().toString();
                if (excludes.contains(name) || name.startsWith("IMPLEMENTATION_") || name.startsWith("TASK_")) {
                    continue;
                }
                result.put(name, entry);
            }
        }
        return result;
    }

    private static boolean filesEqual(Path left, Path right) throws IOException {
        return Files.isRegularFile(left, LinkOption.NOFOLLOW_LINKS)
            && Files.isRegularFile(right, LinkOption.NOFOLLOW_LINKS)
            && java.util.Arrays.equals(Files.readAllBytes(left), Files.readAllBytes(right));
    }

    private static boolean markdownBodiesEqual(Path left, Path right) throws IOException {
        return extractMarkdownBody(left).equals(extractMarkdownBody(right));
    }

    private static String extractMarkdownBody(Path path) throws IOException {
        String content = Files.readString(path);
        if (!content.startsWith("---\n")) {
            return content;
        }
        int end = content.indexOf("\n---\n", 4);
        return end < 0 ? content : content.substring(end + 5);
    }

    private static boolean isOpenCodeSkipped(String skill) {
        return OPENCODE_SKIPPED.contains(skill) || skill.startsWith("test-") || skill.startsWith("x-");
    }

    private static String normalizeOpenCodeName(String skill) {
        return skill.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "").replaceAll("-+", "-");
    }
}
