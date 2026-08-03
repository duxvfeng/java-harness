package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Audit settings.local.json command hooks
 */
@Command(name = "self-audit",
         description = "Audit settings.local.json command hooks")
public class SelfAuditCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SelfAuditCommand executed");
    }
}
