package com.chachamaru.harness.cli.command.evidence;

import com.chachamaru.harness.protocol.JacksonHookCodec;
import com.chachamaru.harness.protocol.evidence.EvidenceFeed;
import com.chachamaru.harness.protocol.model.EvidenceRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Collect evidence command for Harness CLI.
 *
 * <p>Reads a complete evidence.v1 record from stdin or a file and appends it
 * to the project's shared evidence feed.</p>
 */
@Command(name = "collect",
         description = "Collect evidence (test results, build logs)")
public class CollectCommand implements Runnable {

    private static final ObjectMapper OBJECT_MAPPER = JacksonHookCodec.getObjectMapper();

    @Option(names = {"--label"}, description = "Evidence label", defaultValue = "general")
    private String label;

    @Option(names = {"--file"}, description = "Read content from file instead of stdin")
    private String file;

    @Override
    public void run() {
        try {
            EvidenceRecord record = readRecord();
            EvidenceFeed feed = new EvidenceFeed(Paths.get(System.getProperty("user.dir")));
            feed.append(record);
            System.out.println("Collected evidence with label: " + label);
            System.out.println("Stored in: " + feed.evidenceFile());
        } catch (IOException | RuntimeException exception) {
            throw new CommandLine.ExecutionException(
                new CommandLine(this), "Unable to collect evidence: " + exception.getMessage(), exception);
        }
    }

    private EvidenceRecord readRecord() throws IOException {
        if (file == null && System.in.available() == 0) {
            throw new IOException("evidence.v1 JSON input is empty");
        }
        String json = file == null
            ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
            : Files.readString(Paths.get(file), StandardCharsets.UTF_8);
        if (json.isBlank()) {
            throw new IOException("evidence.v1 JSON input is empty");
        }
        try {
            return OBJECT_MAPPER.readValue(json, EvidenceRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IOException("input is not a valid evidence.v1 record", exception);
        }
    }

}
