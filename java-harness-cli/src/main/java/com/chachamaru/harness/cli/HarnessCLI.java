package com.chachamaru.harness.cli;

import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.rules.*;
import com.chachamaru.harness.cli.hook.HookCodec;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.cli.handlers.DefaultHookHandler;
import com.chachamaru.harness.cli.handlers.PostToolUseHandler;
import com.chachamaru.harness.cli.handlers.PreCompactHandler;
import com.chachamaru.harness.cli.handlers.PreToolUseHandler;
import com.chachamaru.harness.cli.handlers.SessionEndHandler;
import com.chachamaru.harness.cli.handlers.SessionStartHandler;
import com.chachamaru.harness.cli.handlers.StopHandler;
import com.chachamaru.harness.cli.router.HookRouter;
import com.chachamaru.harness.shared.constants.HookConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Main CLI entry point for Java Harness.
 *
 * <p>This class provides the complete CLI implementation with Hook processing,
 * Guardrail rules, and event routing capabilities.</p>
 */
public class HarnessCLI {
    private static final Logger log = LoggerFactory.getLogger(HarnessCLI.class);

    private final HookRouter router;
    private final HookCodec codec;

    public HarnessCLI() {
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
        router.registerHandler(new PostToolUseHandler());
        router.registerHandler(new SessionStartHandler());
        router.registerHandler(new SessionEndHandler());
        router.registerHandler(new StopHandler());
        router.registerHandler(new PreCompactHandler());

        // Register default handlers for remaining events
        String[] otherEvents = {
            HookConstants.POST_COMPACT,
            HookConstants.PERMISSION_DENIED,
            HookConstants.NOTIFICATION,
            HookConstants.CONFIG_CHANGE,
            HookConstants.USER_PROMPT_SUBMIT,
            HookConstants.POST_TOOL_FAILURE,
            HookConstants.SUBAGENT_START,
            HookConstants.SUBAGENT_STOP
        };

        for (String event : otherEvents) {
            router.registerHandler(new DefaultHookHandler(event));
        }

        log.info("Harness CLI Gateway initialized with {} handlers",
            router.getRegistry().getHandlerCount());
    }

    private void registerGuardrailRules(GuardrailEngine engine) {
        log.info("Registering Guardrail rules");

        // Register all 27 built-in Guardrail rules
        engine.registerRule(new R01NoSudo());
        engine.registerRule(new R02ProtectedPath());
        engine.registerRule(new R03RedirectionBypass());
        engine.registerRule(new R04ProjectPath());
        engine.registerRule(new R05RmRf());
        engine.registerRule(new R06GitPushForce());
        engine.registerRule(new R07CodexDirectWrite());
        engine.registerRule(new R08BreezingWrite());
        engine.registerRule(new R09SecretRead());
        engine.registerRule(new R10NoVerify());
        engine.registerRule(new R11GitResetHard());
        engine.registerRule(new R12ProtectedBranchPush());
        engine.registerRule(new R13PackageFile());
        engine.registerRule(new R14BillingEgress());
        engine.registerRule(new R15ProductionDeploy());
        engine.registerRule(new R16DatabaseWrite());
        engine.registerRule(new R17ContainerManagement());
        engine.registerRule(new R18ConfigFileWrite());
        engine.registerRule(new R19ExecutableDownload());
        engine.registerRule(new R20NetworkExposure());
        engine.registerRule(new R21SystemCritical());
        engine.registerRule(new R22CertificateManagement());
        engine.registerRule(new R23BackupDeletion());
        engine.registerRule(new R24LogManipulation());
        engine.registerRule(new R25ServiceRestart());
        engine.registerRule(new R26UserPermission());
        engine.registerRule(new R27CronSchedule());

        log.info("Registered {} built-in Guardrail rules", 27);

        // Load custom rules from configuration files
        engine.loadCustomRules();
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
        String version = VersionInfo.getVersion();

        // Handle CLI flags before initializing the heavy hook gateway
        if (args.length > 0) {
            String firstArg = args[0];
            if ("--version".equals(firstArg) || "-v".equals(firstArg)) {
                System.out.println("harness " + version);
                System.exit(0);
            }
            if ("--help".equals(firstArg) || "-h".equals(firstArg) || "help".equals(firstArg)) {
                printHelp(version);
                System.exit(0);
            }
        }

        log.info("Starting Java Harness CLI Gateway v{}", version);

        HarnessCLI cli = new HarnessCLI();
        cli.run();
    }

    private static void printHelp(String version) {
        System.out.println("harness " + version + " - Java Harness CLI Gateway");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  harness --version              Print version information");
        System.out.println("  harness --help                 Show this help message");
        System.out.println("  harness hook <event>           Read a Claude Code hook event from stdin");
        System.out.println();
        System.out.println("When invoked as a hook, harness reads a JSON HookInput from stdin and");
        System.out.println("writes a JSON HookOutput to stdout. The <event> argument is ignored;");
        System.out.println("routing is determined by the hook_event_name field in the input.");
    }
}
