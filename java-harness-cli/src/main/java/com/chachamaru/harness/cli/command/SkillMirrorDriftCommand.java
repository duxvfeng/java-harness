package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.foundation.clientmirror.ClientMirror;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Detect skill mirror drift
 */
@Command(name = "skill-mirror-drift",
         description = "Detect skill mirror drift")
public class SkillMirrorDriftCommand implements Callable<Integer> {

    @Option(names = {"--repo-root", "-d"}, defaultValue = ".", description = "Repository root")
    String repoRoot;

    @Option(names = "--json", description = "Output drift list as JSON")
    boolean json;

    @Override
    public Integer call() {
        try {
            var drifts = ClientMirror.diff(Path.of(repoRoot));
            if (json) {
                System.out.println(new ObjectMapper().writeValueAsString(drifts));
            } else if (drifts.isEmpty()) {
                System.out.println("skill mirrors: in-sync");
            } else {
                drifts.forEach(System.out::println);
            }
            return drifts.isEmpty() ? 0 : 1;
        } catch (Exception e) {
            System.err.println("skill-mirror-drift: " + e.getMessage());
            return 2;
        }
    }
}
