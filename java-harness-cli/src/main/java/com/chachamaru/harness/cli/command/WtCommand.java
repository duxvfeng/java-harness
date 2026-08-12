package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.foundation.worktree.WorktreeFingerprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Worktree fingerprint operations
 */
@Command(name = "wt",
         description = "Worktree fingerprint operations",
         subcommands = {WtCommand.FingerprintCommand.class})
public class WtCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "fingerprint",
             description = "Capture or compare sensitive path fingerprints",
             subcommands = {CaptureCommand.class, DiffCommand.class})
    public static class FingerprintCommand implements Runnable {
        @Override
        public void run() {
            CommandLine.usage(this, System.out);
        }
    }

    @Command(name = "capture", description = "Capture a fingerprint snapshot")
    public static class CaptureCommand implements Callable<Integer> {
        @Option(names = "--output", required = true, description = "Snapshot output file")
        String output;

        @Option(names = "--paths", description = "Semicolon-separated paths to watch")
        String paths;

        @Override
        public Integer call() throws Exception {
            List<Path> watched = paths == null || paths.isBlank()
                ? List.of()
                : Arrays.stream(paths.split(";"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Path::of)
                    .toList();
            WorktreeFingerprint.Snapshot snapshot = WorktreeFingerprint.capture(watched);
            Files.writeString(Path.of(output), new ObjectMapper().writeValueAsString(snapshot));
            return 0;
        }
    }

    @Command(name = "diff", description = "Compare two fingerprint snapshots")
    public static class DiffCommand implements Callable<Integer> {
        @Option(names = "--before", required = true, description = "Before snapshot")
        String before;

        @Option(names = "--after", required = true, description = "After snapshot")
        String after;

        @Override
        public Integer call() throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            WorktreeFingerprint.Snapshot left = mapper.readValue(
                Files.readString(Path.of(before)), WorktreeFingerprint.Snapshot.class);
            WorktreeFingerprint.Snapshot right = mapper.readValue(
                Files.readString(Path.of(after)), WorktreeFingerprint.Snapshot.class);
            List<String> changed = WorktreeFingerprint.diff(left, right);
            changed.forEach(System.err::println);
            return changed.isEmpty() ? 0 : 2;
        }
    }
}
