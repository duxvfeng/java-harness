package com.chachamaru.harness.cli;

import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.rules.*;
import com.chachamaru.harness.cli.hook.HookCodec;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.cli.handlers.DefaultHookHandler;
import com.chachamaru.harness.cli.handlers.PreToolUseHandler;
import com.chachamaru.harness.cli.router.HookRouter;
import com.chachamaru.harness.shared.constants.HookConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Main CLI entry point for Harness
 */
public class HarnessCli {
    private static final Logger log = LoggerFactory.getLogger(HarnessCli.class);

    private final HookRouter router;
    private final HookCodec codec;

    public HarnessCli() {
        this.router = new HookRouter();
        this.codec = new HookCodec();
        initializeComponents();
    }

    private void initializeComponents() {
        log.info("Initializing Harness CLI Gateway");

        // Initialize Guardrail Engine
        GuardrailEngine guardrailEngine = new GuardrailEngine();
        registerGuardrailRules(guardrailEngine);

        // Register Hook Handlers
        router.registerHandler(new PreToolUseHandler(guardrailEngine));

        // Register default handlers for other events
        String[] otherEvents = {
            HookConstants.POST_TOOL_USE,
            HookConstants.SESSION_START,
            HookConstants.SESSION_END,
            HookConstants.STOP,
            HookConstants.PRE_COMPACT,
            HookConstants.POST_COMPACT,
            HookConstants.PERMISSION_DENIED,
            HookConstants.NOTIFICATION,
            HookConstants.CONFIG_CHANGE
        };

        for (String event : otherEvents) {
            router.registerHandler(new DefaultHookHandler(event));
        }

        log.info("Harness CLI Gateway initialized with {} handlers",
            router.getRegistry().getHandlerCount());
    }

    private void registerGuardrailRules(GuardrailEngine engine) {
        log.info("Registering Guardrail rules");

        engine.registerRule(new R01NoSudo());
        engine.registerRule(new R02ProtectedPath());
        engine.registerRule(new R05RmRf());
        engine.registerRule(new R06GitPushForce());
        engine.registerRule(new R09SecretRead());
        engine.registerRule(new R10NoVerify());
        engine.registerRule(new R11GitResetHard());
        engine.registerRule(new R12ProtectedBranchPush());

        log.info("Registered {} Guardrail rules", 8);
    }

    public void run() {
        try {
            log.debug("Reading HookInput from stdin");

            // Read HookInput from stdin
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            HookInput input = codec.parse(reader);

            log.info("Processing hook event: {} for tool: {}", input.hookEventName(), input.toolName());

            // Route to appropriate handler
            var handler = router.route(input);
            HookOutput output = handler.handle(input);

            log.debug("Generating hook output: {}", output.permissionDecision());

            // Write output to stdout
            codec.serialize(output, new OutputStreamWriter(System.out));

            // Exit with appropriate code
            int exitCode = getExitCode(output);
            log.info("Hook processing completed with exit code: {}", exitCode);
            System.exit(exitCode);

        } catch (Exception e) {
            log.error("Error processing hook", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private int getExitCode(HookOutput output) {
        if ("deny".equals(output.permissionDecision())) {
            return 2; // EXIT_DENY
        }
        return 0; // EXIT_ALLOW
    }

    public static void main(String[] args) {
        log.info("Starting Harness CLI Gateway v4.0.0-java-SNAPSHOT");

        HarnessCli cli = new HarnessCli();
        cli.run();
    }
}
