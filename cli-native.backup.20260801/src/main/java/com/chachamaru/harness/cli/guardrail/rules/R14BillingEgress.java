package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R14: Billing egress detection
 */
public class R14BillingEgress implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R14_BILLING_EGRESS;
    }

    @Override
    public String getName() {
        return "Billing Egress Rule";
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
        // Check for external API calls that might incur billing
        if (lowerCmd.contains("curl ") || lowerCmd.contains("wget ") ||
            lowerCmd.contains("http request") || lowerCmd.contains("api call")) {
            // Check for known billing-related domains
            if (lowerCmd.contains("api.openai.com") ||
                lowerCmd.contains("api.anthropic.com") ||
                lowerCmd.contains("aws.amazon.com")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R14_BILLING_EGRESS,
                    "External API calls to billing services are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}
