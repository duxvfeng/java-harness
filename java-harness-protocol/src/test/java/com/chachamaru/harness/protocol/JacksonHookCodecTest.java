package com.chachamaru.harness.protocol;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JacksonHookCodec.
 */
class JacksonHookCodecTest {

    @Test
    void testEncodeDecodeInput() throws HookCodecException {
        // Create a sample HookInput using the actual constructor
        HookInput input = new HookInput(
            "test-session-123",
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            "PreToolUse",
            "test-tool",
            Map.of("key1", "value1", "key2", 42),
            "/plugin/root"
        );

        // Encode to JSON
        String json = JacksonHookCodec.encodeInput(input);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("test-session-123"));
        assertTrue(json.contains("PreToolUse"));

        // Decode back to object
        HookInput decoded = JacksonHookCodec.decodeInput(json);
        assertNotNull(decoded);
        assertEquals(input.sessionId(), decoded.sessionId());
        assertEquals(input.transcriptPath(), decoded.transcriptPath());
        assertEquals(input.cwd(), decoded.cwd());
        assertEquals(input.permissionMode(), decoded.permissionMode());
        assertEquals(input.hookEventName(), decoded.hookEventName());
        assertEquals(input.toolName(), decoded.toolName());
        assertEquals(input.pluginRoot(), decoded.pluginRoot());
    }

    @Test
    void testEncodeDecodeOutput() throws HookCodecException {
        // Create a sample HookOutput
        HookOutput output = HookOutput.allow("PreToolUse");

        // Encode to JSON
        String json = JacksonHookCodec.encodeOutput(output);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("ALLOW"));

        // Decode back to object
        HookOutput decoded = JacksonHookCodec.decodeOutput(json);
        assertNotNull(decoded);
        assertEquals(output.hookEventName(), decoded.hookEventName());
        assertEquals(output.permissionDecision(), decoded.permissionDecision());
        assertTrue(decoded.isAllowed());
        assertFalse(decoded.isDenied());
    }

    @Test
    void testEncodeDecodeDenyOutput() throws HookCodecException {
        HookOutput output = HookOutput.deny("PostToolUse", "Security violation");

        String json = JacksonHookCodec.encodeOutput(output);
        HookOutput decoded = JacksonHookCodec.decodeOutput(json);

        assertEquals("PostToolUse", decoded.hookEventName());
        assertEquals(HookOutput.PermissionDecision.DENY, decoded.permissionDecision());
        assertTrue(decoded.isDenied());
        assertFalse(decoded.isAllowed());
        assertEquals("Security violation", decoded.permissionDecisionReason());
    }

    @Test
    void testEncodeInputWithNull() {
        assertThrows(HookCodecException.class, () -> {
            JacksonHookCodec.encodeInput(null);
        });
    }

    @Test
    void testDecodeInputWithNull() {
        assertThrows(HookCodecException.class, () -> {
            JacksonHookCodec.decodeInput(null);
        });
    }

    @Test
    void testDecodeInputWithEmptyString() {
        assertThrows(HookCodecException.class, () -> {
            JacksonHookCodec.decodeInput("");
        });
    }

    @Test
    void testEncodeOutputWithNull() {
        assertThrows(HookCodecException.class, () -> {
            JacksonHookCodec.encodeOutput(null);
        });
    }

    @Test
    void testDecodeOutputWithNull() {
        assertThrows(HookCodecException.class, () -> {
            JacksonHookCodec.decodeOutput(null);
        });
    }

    @Test
    void testObjectMapperNotNull() {
        assertNotNull(JacksonHookCodec.getObjectMapper());
    }

    @Test
    void testRoundTripComplexInput() throws HookCodecException {
        // Test with a complex input scenario
        HookInput original = new HookInput(
            "complex-session-456",
            "/complex/transcript.json",
            "/workspace/project",
            "default",
            "GuardrailHook",
            "guardrail-tool",
            Map.of("param1", "value1", "param2", 100, "param3", true),
            "/complex/plugin"
        );

        String json = JacksonHookCodec.encodeInput(original);
        HookInput decoded = JacksonHookCodec.decodeInput(json);

        assertEquals(original.sessionId(), decoded.sessionId());
        assertEquals(original.hookEventName(), decoded.hookEventName());
        assertEquals(original.toolName(), decoded.toolName());
        assertEquals(original.permissionMode(), decoded.permissionMode());
    }

    @Test
    void testRoundTripAllOutputTypes() throws HookCodecException {
        // Test all permission decision types
        HookOutput allow = HookOutput.allow("TestHook");
        HookOutput deny = HookOutput.deny("TestHook", "Test deny");
        HookOutput ask = HookOutput.ask("TestHook", "Test ask");
        HookOutput defer = HookOutput.defer("TestHook", "Test defer");

        // Test ALLOW
        String allowJson = JacksonHookCodec.encodeOutput(allow);
        HookOutput decodedAllow = JacksonHookCodec.decodeOutput(allowJson);
        assertEquals(HookOutput.PermissionDecision.ALLOW, decodedAllow.permissionDecision());

        // Test DENY
        String denyJson = JacksonHookCodec.encodeOutput(deny);
        HookOutput decodedDeny = JacksonHookCodec.decodeOutput(denyJson);
        assertEquals(HookOutput.PermissionDecision.DENY, decodedDeny.permissionDecision());

        // Test ASK
        String askJson = JacksonHookCodec.encodeOutput(ask);
        HookOutput decodedAsk = JacksonHookCodec.decodeOutput(askJson);
        assertEquals(HookOutput.PermissionDecision.ASK, decodedAsk.permissionDecision());

        // Test DEFER
        String deferJson = JacksonHookCodec.encodeOutput(defer);
        HookOutput decodedDefer = JacksonHookCodec.decodeOutput(deferJson);
        assertEquals(HookOutput.PermissionDecision.DEFER, decodedDefer.permissionDecision());
    }

    @Test
    void testOutputWithUpdatedInput() throws HookCodecException {
        Map<String, Object> updatedInput = Map.of("modified", "value");
        HookOutput output = HookOutput.withUpdatedInput("PreToolUse", updatedInput);

        String json = JacksonHookCodec.encodeOutput(output);
        HookOutput decoded = JacksonHookCodec.decodeOutput(json);

        assertEquals("Input modified", decoded.permissionDecisionReason());
        assertEquals(HookOutput.PermissionDecision.ALLOW, decoded.permissionDecision());
    }
}
