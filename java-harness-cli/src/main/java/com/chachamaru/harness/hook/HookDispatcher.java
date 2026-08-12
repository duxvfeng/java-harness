package com.chachamaru.harness.hook;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.handler.CommandHandler;
import com.chachamaru.harness.protocol.MultiHostHookCodec;
import com.chachamaru.harness.security.RuntimeFloor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Hook command dispatcher.
 * Reads hook event from stdin, processes it, and writes response to stdout.
 */
public class HookDispatcher implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(HookDispatcher.class);
    private int lastExitCode;

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java-harness hook <pre-tool|post-tool|permission|...>");
            System.exit(1);
        }

        try {
            String hookType = args[0];
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                reader.transferTo(new StringWriterAdapter(json));
            }
            StringWriter output = new StringWriter();
            int exitCode = process(new String[]{hookType}, new StringReader(json.toString()), output);
            lastExitCode = exitCode;
            System.out.print(output);
            if (exitCode != 0) {
                logger.warn("Hook denied with exit code {}", exitCode);
            }

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

    public int lastExitCode() {
        return lastExitCode;
    }

    public int process(String[] args, Reader inputReader, Writer outputWriter) throws IOException {
        String hookType = args == null || args.length == 0 ? "pre-tool" : args[0];
        StringWriter buffer = new StringWriter();
        inputReader.transferTo(buffer);
        if (buffer.toString().isBlank()) {
            outputWriter.write(encode(HookOutput.allow()));
            outputWriter.write(System.lineSeparator());
            outputWriter.flush();
            lastExitCode = 0;
            return 0;
        }

        try {
            MultiHostHookCodec.NormalizedInput normalized = MultiHostHookCodec.normalize(buffer.toString(), null);
            HookInput input = normalized.input();
            Object command = input.toolInput() == null ? null : input.toolInput().get("command");
            if ("pre-tool".equals(hookType) && command != null) {
                RuntimeFloor.Decision decision = RuntimeFloor.check(
                    String.valueOf(command),
                    input.cwd() == null ? null : new RuntimeFloor.Context(Path.of(input.cwd())));
                if (decision.stopped()) {
                    String reason = "RUNTIME_FLOOR:" + decision.category().id() + ": " + decision.reason();
                    HookOutput denied = HookOutput.deny(reason);
                    denied.setHookEventName(input.hookEventName());
                    outputWriter.write(encode(denied));
                    outputWriter.write(System.lineSeparator());
                    outputWriter.flush();
                    lastExitCode = 2;
                    return 2;
                }
            }
            HookOutput allowed = HookOutput.allow();
            allowed.setHookEventName(input.hookEventName());
            outputWriter.write(encode(allowed));
            outputWriter.write(System.lineSeparator());
            outputWriter.flush();
            lastExitCode = 0;
            return 0;
        } catch (MultiHostHookCodec.HookCodecException | RuntimeException e) {
            logger.warn("Hook input was not normalized; allowing request: {}", e.getMessage());
            outputWriter.write(encode(HookOutput.allow()));
            outputWriter.write(System.lineSeparator());
            outputWriter.flush();
            lastExitCode = 0;
            return 0;
        }
    }

    private static final class StringWriterAdapter extends Writer {
        private final StringBuilder target;

        private StringWriterAdapter(StringBuilder target) {
            this.target = target;
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            target.append(cbuf, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static String encode(HookOutput output) throws IOException {
        try {
            return HookCodec.encode(output);
        } catch (Exception e) {
            throw new IOException("encoding hook output", e);
        }
    }
}
