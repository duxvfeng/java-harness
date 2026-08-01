package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R27: Cron schedule modification detection
 */
public class R27CronSchedule implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R27_CRON_SCHEDULE;
    }

    @Override
    public String getName() {
        return "Cron Schedule Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName()) || "Write".equals(input.toolName()) ||
               "Edit".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        String filePath = (String) input.toolInput().get("file_path");

        // Bash command checks
        if (command != null && "Bash".equals(input.toolName())) {
            String lowerCmd = command.toLowerCase();
            if (lowerCmd.contains("crontab") || lowerCmd.contains("cron") ||
                lowerCmd.contains("systemd-timer") || lowerCmd.contains("anacron")) {
                if (lowerCmd.contains("crontab -e") || lowerCmd.contains("crontab -r") ||
                    lowerCmd.contains("systemctl restart cron") || lowerCmd.contains("service cron restart")) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R27_CRON_SCHEDULE,
                        "Cron schedule modifications are not allowed"
                    );
                }
            }
        }

        // File write checks for cron files
        if (filePath != null && ("Write".equals(input.toolName()) || "Edit".equals(input.toolName()))) {
            String lowerPath = filePath.toLowerCase();
            if (lowerPath.contains("crontab") || lowerPath.contains("/etc/cron.") ||
                lowerPath.endsWith(".cron") || lowerPath.contains("/etc/cron.d/") ||
                lowerPath.contains("/etc/cron.hourly/") || lowerPath.contains("/etc/cron.daily/") ||
                lowerPath.contains("/etc/cron.weekly/") || lowerPath.contains("/etc/cron.monthly/")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R27_CRON_SCHEDULE,
                    "Direct cron file modifications are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}