package com.chachamaru.harness.cli.hook;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Hook protocol codec (JSON encoding/decoding)
 */
public class HookCodec {
    private static final Logger log = LoggerFactory.getLogger(HookCodec.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Performance optimization
        mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Parse HookInput from stdin (Reader)
     */
    public HookInput parse(Reader reader) throws IOException {
        try {
            return mapper.readValue(reader, HookInput.class);
        } catch (JsonMappingException e) {
            log.error("Failed to parse HookInput: {}", e.getMessage());
            throw new IOException("Invalid hook input JSON", e);
        }
    }

    /**
     * Serialize HookOutput to stdout (Writer)
     */
    public void serialize(HookOutput output, Writer writer) throws IOException {
        mapper.writer().writeValue(writer, output);
    }
}
