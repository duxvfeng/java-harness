package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.cli.VersionInfo;
import picocli.CommandLine;

/**
 * Picocli version provider that reads the version from the bundled VERSION resource.
 *
 * <p>This keeps the version string out of the {@link HarnessCLI} annotation and
 * uses {@link VersionInfo} as the single source of truth.</p>
 */
public class HarnessVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        return new String[] { VersionInfo.getVersion() };
    }
}
