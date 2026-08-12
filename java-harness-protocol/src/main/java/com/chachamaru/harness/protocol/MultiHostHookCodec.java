package com.chachamaru.harness.protocol;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Normalizes the pre-tool payloads emitted by supported hosts. */
public final class MultiHostHookCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MultiHostHookCodec() {
    }

    public enum Host {
        CLAUDE("claude"), CODEX("codex"), CURSOR("cursor"), GROK("grok");

        private final String id;

        Host(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        static Host parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            for (Host host : values()) {
                if (host.id.equalsIgnoreCase(value.trim())) {
                    return host;
                }
            }
            return null;
        }
    }

    public record NormalizedInput(HookInput input, Host host) {
    }

    public static final class HookCodecException extends Exception {
        public HookCodecException(String message) {
            super(message);
        }

        public HookCodecException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static NormalizedInput normalize(String json, String hostHint) throws HookCodecException {
        if (json == null || json.isBlank()) {
            throw new HookCodecException("empty input");
        }

        final JsonNode node;
        try {
            node = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new HookCodecException("parsing JSON: " + e.getOriginalMessage(), e);
        }
        if (node == null || !node.isObject()) {
            throw new HookCodecException("hook input must be a JSON object");
        }

        Host host = resolveHost(node, hostHint);
        String sessionId = firstText(node, "session_id", "conversation_id", "conversationId");
        String cwd = firstText(node, "cwd", "workspace_root");
        if (cwd == null) {
            JsonNode roots = node.get("workspace_roots");
            if (roots != null && roots.isArray() && !roots.isEmpty()) {
                cwd = text(roots.get(0));
            }
        }

        String event = firstText(node, "hook_event_name");
        if (event == null) {
            event = "PreToolUse";
        }
        String toolName = firstText(node, "tool_name", "toolName");
        String command = firstText(node, "command");
        if (toolName == null && command != null) {
            toolName = "Bash";
        }
        if ("Shell".equals(toolName)) {
            toolName = "Bash";
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new HookCodecException("missing required field 'session_id'");
        }
        if (toolName == null || toolName.isBlank()) {
            throw new HookCodecException("missing required field 'tool_name'");
        }

        Map<String, Object> toolInput = new LinkedHashMap<>();
        JsonNode explicitInput = node.get("tool_input");
        if (explicitInput != null && explicitInput.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = explicitInput.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                toolInput.put(field.getKey(), MAPPER.convertValue(field.getValue(), Object.class));
            }
        }
        if (!toolInput.containsKey("command") && command != null) {
            toolInput.put("command", command);
        }
        if (!toolInput.containsKey("file_path")) {
            String filePath = firstText(node, "file_path", "path");
            if (filePath != null) {
                toolInput.put("file_path", filePath);
            }
        }

        HookInput input = new HookInput(
            sessionId,
            firstText(node, "transcript_path"),
            cwd,
            firstText(node, "permission_mode"),
            event,
            toolName,
            toolInput,
            firstText(node, "plugin_root")
        );
        return new NormalizedInput(input, host);
    }

    public static String denyOutput(Host host, String reason) throws HookCodecException {
        if (host == null) {
            throw new HookCodecException("host is required");
        }
        String safeReason = reason == null ? "blocked by harness policy" : reason;
        Map<String, Object> output = new LinkedHashMap<>();
        if (host == Host.CURSOR) {
            output.put("permission", "deny");
            output.put("agent_message", safeReason);
        } else {
            Map<String, Object> specific = new LinkedHashMap<>();
            specific.put("hookEventName", "PreToolUse");
            specific.put("permissionDecision", "deny");
            specific.put("permissionDecisionReason", safeReason);
            output.put("hookSpecificOutput", specific);
        }
        try {
            return MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new HookCodecException("serializing deny output", e);
        }
    }

    private static Host resolveHost(JsonNode node, String hostHint) {
        Host hinted = Host.parse(hostHint);
        if (hinted != null) {
            return hinted;
        }
        if ("preToolUse".equals(node.path("hook_event_name").asText())
            || node.has("workspace_roots")
            || node.has("workspace_root")
            || node.has("command")) {
            return Host.CURSOR;
        }
        if (node.has("conversation_id") || node.has("conversationId")) {
            return Host.CODEX;
        }
        return Host.CLAUDE;
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node.get(name));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode value) {
        if (value == null || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String result = value.asText();
        return result == null || result.isBlank() ? null : result;
    }
}
