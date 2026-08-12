package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.foundation.clientmirror.ClientMirror;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Report skills/ mirror drift
 */
@Command(name = "mirror",
         description = "Report skills/ mirror drift",
         subcommands = {MirrorCommand.VerifyCommand.class})
public class MirrorCommand implements Callable<Integer> {

    @Option(names = {"--repo-root", "-d"}, defaultValue = ".", description = "Repository root")
    String repoRoot;

    @Option(names = "--json", description = "Output mirror-state.v1 JSON")
    boolean json;

    @Override
    public Integer call() {
        return verify(Path.of(repoRoot), json);
    }

    static int verify(Path root, boolean json) {
        try {
            ClientMirror.State state = ClientMirror.scan(root, java.time.Instant.now());
            print(state, json);
            return state.healthy() ? 0 : 1;
        } catch (Exception e) {
            System.err.println("mirror: " + e.getMessage());
            return 2;
        }
    }

    private static void print(ClientMirror.State state, boolean json) throws Exception {
        if (json) {
            System.out.println(new ObjectMapper().writeValueAsString(state));
            return;
        }
        System.out.printf("mirror=%s reason=%s fingerprint=%s%n",
            state.healthy() ? "healthy" : "drift", state.reason(), state.fingerprint());
        state.mirrors().forEach(entry -> {
            System.out.printf("%s: %s (%d drift)%n", entry.root(), entry.status(), entry.driftCount());
            entry.drifts().forEach(drift -> System.out.println("  " + drift));
        });
    }

    @Command(name = "verify", description = "Verify configured skill mirrors")
    static class VerifyCommand implements Callable<Integer> {
        @Option(names = {"--repo-root", "-d"}, defaultValue = ".", description = "Repository root")
        String repoRoot;

        @Option(names = "--json", description = "Output mirror-state.v1 JSON")
        boolean json;

        @Override
        public Integer call() {
            return verify(Path.of(repoRoot), json);
        }
    }
}
