package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * PreToolUse hook handler
 */
public class PreToolUseHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PreToolUseHandler.class);
    private static final String PRE_TOOL_USE = "PreToolUse";
    private final GuardrailEngine guardrailEngine;

    public PreToolUseHandler(GuardrailEngine guardrailEngine) {
        this.guardrailEngine = guardrailEngine;
    }

    @Override
    public String getEventName() {
        return PRE_TOOL_USE;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("Handling PreToolUse for tool: {}", input.toolName());

        // Validate input
        if (!input.isValid()) {
            return HookOutput.deny("Invalid hook input: missing required fields");
        }

        // Evaluate guardrails
        GuardrailResult result = guardrailEngine.evaluate(input);

        if (result.isDenied()) {
            log.warn("Guardrail denied request: rule={}, reason={}",
                result.decision().ruleId(), result.decision().reason());
            return HookOutput.deny(result.decision().reason());
        }

        // Allow the operation
        return HookOutput.allow();
    }
}