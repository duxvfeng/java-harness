package com.chachamaru.harness.hook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON codec for hook events.
 * Handles serialization and deserialization of hook input/output.
 */
public class HookCodec {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Configure ObjectMapper for hook protocol compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    /**
     * Decode JSON string to HookInput.
     *
     * @param json JSON string
     * @return HookInput object
     * @throws Exception if parsing fails
     */
    public static HookInput decode(String json) throws Exception {
        return mapper.readValue(json, HookInput.class);
    }

    /**
     * Encode HookOutput to JSON string.
     *
     * @param output HookOutput object
     * @return JSON string
     * @throws Exception if serialization fails
     */
    public static String encode(HookOutput output) throws Exception {
        return mapper.writeValueAsString(output);
    }
}
