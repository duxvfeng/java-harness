package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completion command handler.
 * Generates shell completion scripts for better CLI experience.
 */
public class CompletionHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CompletionHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            if (args.length == 0) {
                showCompletionHelp();
                return;
            }

            String shell = args[0];
            boolean install = false;

            // Parse options
            for (String arg : args) {
                if (arg.equals("--install")) {
                    install = true;
                }
            }

            switch (shell.toLowerCase()) {
                case "bash":
                    generateBashCompletion(install);
                    break;
                case "zsh":
                    generateZshCompletion(install);
                    break;
                case "fish":
                    generateFishCompletion(install);
                    break;
                case "powershell":
                    generatePowerShellCompletion(install);
                    break;
                default:
                    System.err.println("Unsupported shell: " + shell);
                    System.err.println("Supported shells: bash, zsh, fish, powershell");
                    System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Completion command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void generateBashCompletion(boolean install) {
        String script = """
# Java Harness Bash Completion

_java_harness_completion() {
    local cur prev words
    words=(${COMP_WORDS[@]})
    cur=${COMP_WORDS[COMP_CWORD]}

    prev=${words[COMP_CWORD-1]}

    case $prev in
        java-harness|harness)
            COMPREPLY=($(compgen -W 'plan work review release sync init validate doctor status gen evidence sprint-contract completion help' -- "$cur"))
            ;;
        plan|work|review|release)
            COMPREPLY=($(compgen -W '--help --verbose --output --format' -- "$cur"))
            ;;
        validate)
            COMPREPLY=($(compgen -W 'skills agents all' -- "$cur"))
            ;;
        evidence)
            COMPREPLY=($(compgen -W 'collect report list' -- "$cur"))
            ;;
        sprint-contract)
            COMPREPLY=($(compgen -W 'generate validate list' -- "$cur"))
            ;;
        completion)
            COMPREPLY=($(compgen -W 'bash zsh fish powershell' -- "$cur"))
            ;;
        *)
            COMPREPLY=($(compgen -f -- "$cur"))
            ;;
    esac
}

complete -F _java_harness_completion java-harness
""";

        if (install) {
            System.out.println("# To install bash completion, add the following to ~/.bashrc or ~/.bash_profile:");
            System.out.println("# eval \"$(java-harness completion bash)\"");
        } else {
            System.out.print(script);
        }
    }

    private void generateZshCompletion(boolean install) {
        String script = """
#compdef java-harness

_java_harness() {
    local -a commands
    commands=(
        'plan:Generate plan prompt'
        'work:Execute work task'
        'review:Review completed work'
        'release:Prepare release'
        'sync:Sync configuration'
        'init:Initialize project'
        'validate:Validate skills/agents'
        'doctor:Health check'
        'status:Show project status'
        'gen:Generate content'
        'evidence:Collect and report evidence'
        'sprint-contract:Manage sprint contracts'
        'completion:Generate shell completions'
        'help:Show help information'
    )

    if (( CURRENT == 1 )); then
        _describe -t commands 'java-harness commands'
        _arguments '1: :->commands' '*:: :->args'
        _values 'commands' "${commands[@]}"
    else
        case $words[2] in
            validate)
                _values 'validation type' skills agents all
                ;;
            evidence)
                _values 'evidence command' collect report list
                ;;
            sprint-contract)
                _values 'sprint-contract command' generate validate list
                ;;
            completion)
                _values 'shell type' bash zsh fish powershell
                ;;
        esac
    fi
}

_java_harness "$@"
""";

        if (install) {
            System.out.println("# To install zsh completion, add the script to your completion path:");
            System.out.println("# mkdir -p ~/.zsh/completion");
            System.out.println("# java-harness completion zsh > ~/.zsh/completion/_java-harness");
        } else {
            System.out.print(script);
        }
    }

    private void generateFishCompletion(boolean install) {
        String script = """
# Java Harness Fish Completion

complete -c java-harness -f

function __java_harness_no_subcommand
    for cmd in plan work review release sync init validate doctor status gen evidence sprint-contract completion help
        if test (commandline -opc) = $cmd
            echo "true"
            return
        end
    end
    echo "false"
end

function __java_harness_using_command
    set -l cmd (commandline -opc)
    if test (commandline -opc)[-1] = $argv[1]
        echo "true"
    else
        echo "false"
    end
end

complete -c java-harness -n '__fish_use_subcommand' -f -a plan work review release sync init validate doctor status gen evidence sprint-contract completion help

complete -c java-harness -n '__java_harness_using_command validate' -f -a skills agents all

complete -c java-harness -n '__java_harness_using_command evidence' -f -a collect report list

complete -c java-harness -n '__java_harness_using_command sprint-contract' -f -a generate validate list

complete -c java-harness -n '__java_harness_using_command completion' -f -a bash zsh fish powershell
""";

        if (install) {
            System.out.println("# To install fish completion, save the script to ~/.config/fish/completions/java-harness.fish");
        } else {
            System.out.print(script);
        }
    }

    private void generatePowerShellCompletion(boolean install) {
        String script = "# Java Harness PowerShell Completion\n" +
            "\n" +
            "# Load this script to enable tab completion:\n" +
            "# java-harness completion powershell | Invoke-Expression\n" +
            "\n" +
            "Register-ArgumentCompleter -CommandName 'java-harness' -ScriptBlock {\n" +
            "    param($commandName, $parameterName, $wordToComplete, $commandAst, $fakeBoundParameter)\n" +
            "\n" +
            "    $commands = @('plan', 'work', 'review', 'release', 'sync', 'init', 'validate', 'doctor', 'status', 'gen', 'evidence', 'sprint-contract', 'completion', 'help')\n" +
            "\n" +
            "    switch ($parameterName) {\n" +
            "        'Command' {\n" +
            "            $commands | Where-Object { $_ -like \"$wordToComplete*\" } | ForEach-Object {\n" +
            "                [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterSetName', $_, $_, $_)\n" +
            "            }\n" +
            "        }\n" +
            "        default {\n" +
            "            $null\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

        if (install) {
            System.out.println("# To install PowerShell completion, run:");
            System.out.println("# java-harness completion powershell | Invoke-Expression");
        } else {
            System.out.print(script);
        }
    }

    private void showCompletionHelp() {
        System.out.println("Usage: java-harness completion <shell> [options]");
        System.out.println();
        System.out.println("Shells:");
        System.out.println("  bash                  Generate bash completion script");
        System.out.println("  zsh                   Generate zsh completion script");
        System.out.println("  fish                  Generate fish completion script");
        System.out.println("  powershell            Generate PowerShell completion script");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --install              Show installation instructions");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness completion bash");
        System.out.println("  java-harness completion zsh --install");
    }
}
