package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.foundation.monitoring.NightWatchReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;

/**
 * Emit night-watch patrol report
 */
@Command(name = "night-watch",
         description = "Emit night-watch patrol report")
public class NightWatchCommand implements Callable<Integer> {

    @Option(names = {"--repo-root", "-d"}, defaultValue = ".", description = "Repository root")
    String repoRoot;

    @Option(names = "--dry-run", description = "Emit a report without side effects")
    boolean dryRun;

    @Option(names = "--json", description = "Output night-watch-report.v1 JSON")
    boolean json;

    @Override
    public Integer call() {
        try {
            NightWatchReport.Report report = NightWatchReport.build(Path.of(repoRoot), dryRun, Instant.now());
            if (json) {
                System.out.println(new ObjectMapper().writeValueAsString(report));
            } else {
                System.out.printf("night-watch=%s reason=%s%n",
                    report.health().healthy() ? "healthy" : "unhealthy", report.health().reason());
                System.out.printf("unresolved_loops=%d stale_tasks=%d open_decisions=%d%n",
                    report.unresolvedLoops().size(), report.staleTasks().size(), report.openDecisions().size());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("night-watch: " + e.getMessage());
            return 1;
        }
    }
}
