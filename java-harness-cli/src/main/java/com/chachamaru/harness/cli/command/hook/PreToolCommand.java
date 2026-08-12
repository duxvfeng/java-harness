package com.chachamaru.harness.cli.command.hook;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.protocol.MultiHostHookCodec;
import com.chachamaru.harness.security.RuntimeFloor;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * PreTool hook command for Harness CLI.
 *
 * <p>Evaluates PreToolUse guardrails before tool execution.</p>
 */
@Command(name = "pre-tool",
         description = "Evaluate PreToolUse guardrails")
public class PreToolCommand implements Callable<Integer> {

    @Option(names = "--host", description = "Host payload format: claude, codex, cursor, or grok")
    String host;

    @Override
    public Integer call() {
        try {
            execute(new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), host);
            return executeResult;
        } catch (IOException e) {
            System.err.println("pre-tool: " + e.getMessage());
            return 0;
        }
    }

    private int executeResult;

    public int execute(Reader inputReader, Writer outputWriter, String hostHint) throws IOException {
        StringWriter buffer = new StringWriter();
        inputReader.transferTo(buffer);
        try {
            MultiHostHookCodec.NormalizedInput normalized = MultiHostHookCodec.normalize(buffer.toString(), hostHint);
            HookInput input = normalized.input();
            Object commandValue = input.toolInput() == null ? null : input.toolInput().get("command");
            if (commandValue == null) {
                executeResult = 0;
                return 0;
            }
            RuntimeFloor.Decision decision = RuntimeFloor.check(
                String.valueOf(commandValue),
                input.cwd() == null ? null : new RuntimeFloor.Context(Path.of(input.cwd())));
            if (!decision.stopped()) {
                executeResult = 0;
                return 0;
            }
            String reason = "RUNTIME_FLOOR:" + decision.category().id() + ": " + decision.reason();
            outputWriter.write(MultiHostHookCodec.denyOutput(normalized.host(), reason));
            outputWriter.write(System.lineSeparator());
            outputWriter.flush();
            executeResult = 2;
            return 2;
        } catch (MultiHostHookCodec.HookCodecException | RuntimeException e) {
            executeResult = 0;
            return 0;
        }
    }
}
