package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CIStatus hook handler
 * <p>
 * Handles CI status check after push/PR operations.
 * </p>
 */
public class CIStatusHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(CIStatusHandler.class);
    private static final String CI_STATUS = "CIStatus";

    @Override
    public String getEventName() {
        return CI_STATUS;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("CI status check for: {}", input.sessionId());

        // Check CI status (mock implementation)
        String ciStatus = checkCIStatus(input);

        if ("failed".equals(ciStatus)) {
            log.warn("CI check failed for session: {}", input.sessionId());
            return new HookOutput(
                CI_STATUS,
                "allow",
                null,
                "WARNING: CI status check failed - review required"
            );
        }

        return HookOutput.allow();
    }

    /**
     * Check CI status (placeholder for actual CI integration)
     */
    private String checkCIStatus(HookInput input) {
        try {
            // Look for CI configuration files
            Path cwd = Paths.get(input.cwd());

            boolean hasGitHubActions = Files.exists(cwd.resolve(".github").resolve("workflows"));
            boolean hasGitLabCI = Files.exists(cwd.resolve(".gitlab-ci.yml"));

            if (hasGitHubActions || hasGitLabCI) {
                // In a real implementation, this would query the CI API
                log.debug("CI configuration detected");
                return "pending";
            }

        } catch (Exception e) {
            log.warn("Failed to check CI status: {}", e.getMessage());
        }

        return "no-ci";
    }
}
