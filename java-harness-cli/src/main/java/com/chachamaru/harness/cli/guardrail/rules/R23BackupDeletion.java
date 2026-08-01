package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R23: Backup deletion detection
 */
public class R23BackupDeletion implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R23_BACKUP_DELETION;
    }

    @Override
    public String getName() {
        return "Backup Deletion Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        if (command == null) {
            return GuardrailResult.allowed();
        }

        String lowerCmd = command.toLowerCase();
        // Check for deletion operations targeting backup directories
        boolean hasDeletion = lowerCmd.contains("rm -rf") || lowerCmd.contains("rm -r") ||
                           lowerCmd.contains("delete") || lowerCmd.contains("del /s") ||
                           lowerCmd.contains("remove-item -recurse");

        boolean hasBackupIndicator = lowerCmd.contains("backup") || lowerCmd.contains("bak") ||
                                   lowerCmd.contains(".bak") || lowerCmd.contains("archive") ||
                                   lowerCmd.contains("snapshot") || lowerCmd.contains("dump");

        if (hasDeletion && hasBackupIndicator) {
            return GuardrailResult.denied(
                GuardrailConstants.R23_BACKUP_DELETION,
                "Backup deletion operations are not allowed without explicit confirmation"
            );
        }

        return GuardrailResult.allowed();
    }
}