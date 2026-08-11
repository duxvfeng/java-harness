package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.cli.VersionInfo;
import picocli.CommandLine.Command;

/**
 * Print version information for the Harness CLI.
 *
 * <p>This command outputs the current version of Harness, read from the
 * bundled VERSION resource file. It provides a simple way to check the
 * installed version without running other commands.</p>
 */
@Command(name = "version",
         mixinStandardHelpOptions = true,
         description = "Print version information")
public class VersionCommand implements Runnable {

    /** Default constructor required by Picocli and GraalVM Native Image reflection. */
    public VersionCommand() {
        // Default constructor - required for reflection instantiation
    }

    @Override
    public void run() {
        String version = VersionInfo.getVersion();
        System.out.println("harness " + version);
    }
}
