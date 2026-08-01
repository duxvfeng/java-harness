package com.chachamaru.harness.protocol;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON codec for hook input/output using Jackson.
 *
 * <p>This class provides serialization and deserialization of HookInput and HookOutput
 * objects using Jackson's ObjectMapper.</p>
 *
 * @since 4.1.0
 */
public class JacksonHookCodec {

    private static final Logger logger = LoggerFactory.getLogger(JacksonHookCodec.class);
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enable pretty printing for readability
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Don't fail on empty beans (for forward compatibility)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // Configure for Java records - detect constructor parameters for deserialization
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        return mapper;
    }

    /**
     * Encodes a HookInput object to JSON string.
     *
     * @param input the input to encode
     * @return the JSON string representation
     * @throws HookCodecException if encoding fails
     */
    public static String encodeInput(HookInput input) throws HookCodecException {
        if (input == null) {
            throw new HookCodecException("Input cannot be null");
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            logger.error("Failed to encode HookInput: {}", e.getMessage(), e);
            throw new HookCodecException("Failed to encode HookInput: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a JSON string to a HookInput object.
     *
     * @param json the JSON string to decode
     * @return the decoded HookInput object
     * @throws HookCodecException if decoding fails
     */
    public static HookInput decodeInput(String json) throws HookCodecException {
        if (json == null || json.trim().isEmpty()) {
            throw new HookCodecException("JSON string cannot be null or empty");
        }

        try {
            return OBJECT_MAPPER.readValue(json, HookInput.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to decode HookInput: {}", e.getMessage(), e);
            throw new HookCodecException("Failed to decode HookInput: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes a HookOutput object to JSON string.
     *
     * @param output the output to encode
     * @return the JSON string representation
     * @throws HookCodecException if encoding fails
     */
    public static String encodeOutput(HookOutput output) throws HookCodecException {
        if (output == null) {
            throw new HookCodecException("Output cannot be null");
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            logger.error("Failed to encode HookOutput: {}", e.getMessage(), e);
            throw new HookCodecException("Failed to encode HookOutput: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a JSON string to a HookOutput object.
     *
     * @param json the JSON string to decode
     * @return the decoded HookOutput object
     * @throws HookCodecException if decoding fails
     */
    public static HookOutput decodeOutput(String json) throws HookCodecException {
        if (json == null || json.trim().isEmpty()) {
            throw new HookCodecException("JSON string cannot be null or empty");
        }

        try {
            return OBJECT_MAPPER.readValue(json, HookOutput.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to decode HookOutput: {}", e.getMessage(), e);
            throw new HookCodecException("Failed to decode HookOutput: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the shared ObjectMapper instance.
     *
     * @return the ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
