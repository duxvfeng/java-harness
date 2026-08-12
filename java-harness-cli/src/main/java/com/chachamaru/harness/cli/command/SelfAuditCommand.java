package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.foundation.audit.SelfAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Audit settings.local.json command hooks
 */
@Command(name = "self-audit",
         description = "Audit settings.local.json command hooks")
public class SelfAuditCommand implements Runnable {

    @Option(names = "--file", description = "settings.local.json path", defaultValue = ".claude/settings.local.json")
    String file;

    @Option(names = "--json", description = "Output JSON")
    boolean json;

    @Override
    public void run() {
        try {
            call();
        } catch (Exception e) {
            System.err.println("self-audit: " + e.getMessage());
        }
    }

    public int call() {
        try {
            Path path = Path.of(file == null ? ".claude/settings.local.json" : file);
            if (!Files.exists(path)) {
                if (json) {
                    System.out.println("{\"known\":0,\"unknown\":0,\"unknown_entries\":[]}");
                }
                return 0;
            }
            SelfAudit.Report report = SelfAudit.audit(Files.readString(path));
            if (json) {
                System.out.println(new ObjectMapper().writeValueAsString(new AuditOutput(
                    report.known().size(), report.unknown().size(), report.unknown())));
            } else {
                System.out.printf("known=%d unknown=%d%n", report.known().size(), report.unknown().size());
                report.unknown().forEach(entry -> System.out.println(entry.event() + ": " + entry.command()));
            }
            return report.warningCount() == 0 ? 0 : 1;
        } catch (Exception e) {
            System.err.println("self-audit: " + e.getMessage());
            return 2;
        }
    }

    private record AuditOutput(int known, int unknown, java.util.List<SelfAudit.HookEntry> unknownEntries) {
    }
}
