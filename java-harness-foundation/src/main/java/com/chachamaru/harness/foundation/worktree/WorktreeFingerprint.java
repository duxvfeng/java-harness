package com.chachamaru.harness.foundation.worktree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fingerprints sensitive files so changes outside the worker scope are observable. */
public final class WorktreeFingerprint {
    private static final int MAX_READ_BYTES = 4096;

    private WorktreeFingerprint() {
    }

    public record Snapshot(Map<String, String> files) {
        public Snapshot {
            files = files == null ? Map.of() : Map.copyOf(files);
        }
    }

    public static Snapshot capture(List<Path> paths) throws IOException {
        Map<String, String> fingerprints = new LinkedHashMap<>();
        for (Path path : paths == null ? List.<Path>of() : paths) {
            if (path == null || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            capturePath(path, fingerprints);
        }
        return new Snapshot(fingerprints);
    }

    public static List<String> diff(Snapshot before, Snapshot after) {
        Map<String, String> left = before == null ? Map.of() : before.files();
        Map<String, String> right = after == null ? Map.of() : after.files();
        List<String> changed = new ArrayList<>();
        for (String path : left.keySet()) {
            if (!java.util.Objects.equals(left.get(path), right.get(path))) {
                changed.add(path);
            }
        }
        for (String path : right.keySet()) {
            if (!left.containsKey(path)) {
                changed.add(path);
            }
        }
        return changed.stream().distinct().sorted().toList();
    }

    private static void capturePath(Path path, Map<String, String> output) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (var stream = Files.walk(path)) {
                stream.filter(candidate -> !Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS))
                    .forEach(candidate -> {
                        try {
                            output.put(key(candidate), fingerprint(candidate));
                        } catch (IOException ignored) {
                            // An inaccessible watch target is treated as unavailable.
                        }
                    });
            }
            return;
        }
        output.put(key(path), fingerprint(path));
    }

    private static String key(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        try {
            Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            if (absolute.startsWith(home)) {
                return home.relativize(absolute).toString().replace('\\', '/');
            }
        } catch (RuntimeException ignored) {
            // Fall through to a stable absolute key.
        }
        return absolute.toString().replace('\\', '/');
    }

    private static String fingerprint(Path path) throws IOException {
        long modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        long size = Files.size(path);
        byte[] content;
        if (Files.isSymbolicLink(path)) {
            content = Files.readSymbolicLink(path).toString().getBytes(StandardCharsets.UTF_8);
        } else {
            content = readPrefix(path);
        }
        return modified + "-" + size + "-" + sha256(content);
    }

    private static byte[] readPrefix(Path path) throws IOException {
        byte[] buffer = new byte[MAX_READ_BYTES];
        try (InputStream input = Files.newInputStream(path)) {
            int count = input.read(buffer);
            if (count < 0) {
                return new byte[0];
            }
            return count == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, count);
        }
    }

    private static String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
