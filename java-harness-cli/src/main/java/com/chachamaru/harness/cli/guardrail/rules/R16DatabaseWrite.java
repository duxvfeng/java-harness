package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R16: Database write operation detection
 */
public class R16DatabaseWrite implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R16_DATABASE_WRITE;
    }

    @Override
    public String getName() {
        return "Database Write Rule";
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
        // Check for database write operations
        if (lowerCmd.contains("insert into") || lowerCmd.contains("update ") ||
            lowerCmd.contains("delete from") || lowerCmd.contains("drop table") ||
            lowerCmd.contains("truncate table") || lowerCmd.contains("alter table") ||
            lowerCmd.contains("create table") || lowerCmd.contains("create database") ||
            lowerCmd.contains("drop database") || lowerCmd.contains("mysqldump") ||
            lowerCmd.contains("pg_dump") || lowerCmd.contains("mongodump")) {
            // Check for production database indicators
            if (lowerCmd.contains("production") || lowerCmd.contains("prod") ||
                lowerCmd.contains("--host=prod") || lowerCmd.contains("--host=production") ||
                lowerCmd.contains("prod-db") || lowerCmd.contains("production-db")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R16_DATABASE_WRITE,
                    "Direct production database write operations are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}