package com.chachamaru.harness.hook;

import com.chachamaru.harness.handler.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Hook command dispatcher.
 * Reads hook event from stdin, processes it, and writes response to stdout.
 */
public class HookDispatcher implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(HookDispatcher.class);

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java-harness hook <pre-tool|post-tool|permission|...>");
            System.exit(1);
        }

        try {
            String hookType = args[0];
            String[] hookArgs = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[]{};

            // Read JSON from stdin
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            String json = jsonBuilder.toString();
            if (json.isEmpty()) {
                logger.warn("Empty stdin received");
                System.out.println(HookCodec.encode(HookOutput.allow()));
                return;
            }

            // Decode hook input
            HookInput input = HookCodec.decode(json);

            // Dispatch to specific hook handler
            HookOutput output = dispatchHook(hookType, input, hookArgs);

            // Encode and write response
            System.out.println(HookCodec.encode(output));

        } catch (Exception e) {
            logger.error("Hook processing error", e);
            // Fail-open: return allow on error
            try {
                System.out.println(HookCodec.encode(HookOutput.allow()));
            } catch (Exception ex) {
                logger.error("Failed to encode fallback response", ex);
            }
        }
    }

    private HookOutput dispatchHook(String hookType, HookInput input, String[] args) {
        // For now, return a default allow response
        // Specific hook handlers will be implemented in later tasks
        logger.info("Processing hook type: {}", hookType);

        HookOutput output = HookOutput.allow();
        output.setHookEventName(input.getHookEventName());
        return output;
    }
}
