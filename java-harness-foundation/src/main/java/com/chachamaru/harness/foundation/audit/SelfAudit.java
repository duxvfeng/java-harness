package com.chachamaru.harness.foundation.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Audits command hooks against the harness-owned command allowlist. */
public final class SelfAudit {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> KNOWN = List.of(
        "bin/harness inbox check",
        "bin/harness inbox monitor"
    );

    private SelfAudit() {
    }

    public record HookEntry(String event, String type, String command) {
    }

    public record Report(List<HookEntry> known, List<HookEntry> unknown) {
        public int warningCount() {
            return unknown.size();
        }
    }

    public static Report audit(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return new Report(List.of(), List.of());
        }
        try {
            JsonNode hooks = MAPPER.readTree(settingsJson).path("hooks");
            List<HookEntry> known = new ArrayList<>();
            List<HookEntry> unknown = new ArrayList<>();
            hooks.fields().forEachRemaining(event -> collect(event.getKey(), event.getValue(), known, unknown));
            return new Report(List.copyOf(known), List.copyOf(unknown));
        } catch (Exception ignored) {
            return new Report(List.of(), List.of());
        }
    }

    public static boolean isKnown(String command) {
        if (command == null) {
            return false;
        }
        String value = command.trim();
        return KNOWN.stream().anyMatch(value::startsWith);
    }

    private static void collect(String event, JsonNode value, List<HookEntry> known, List<HookEntry> unknown) {
        if (!value.isArray()) {
            return;
        }
        for (JsonNode item : value) {
            JsonNode nested = item.path("hooks");
            if (nested.isArray()) {
                for (JsonNode hook : nested) {
                    add(event, hook, known, unknown);
                }
            } else {
                add(event, item, known, unknown);
            }
        }
    }

    private static void add(String event, JsonNode node, List<HookEntry> known, List<HookEntry> unknown) {
        if (!"command".equals(node.path("type").asText())) {
            return;
        }
        String command = node.path("command").asText("").trim();
        if (command.isEmpty()) {
            return;
        }
        HookEntry entry = new HookEntry(event, "command", command);
        if (isKnown(command)) {
            known.add(entry);
        } else {
            unknown.add(entry);
        }
    }
}
