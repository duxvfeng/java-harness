package com.chachamaru.harness.cli.guardrail.index;

import com.chachamaru.harness.cli.guardrail.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Index for fast rule lookup by tool type
 */
public class RuleIndex {
    private static final Logger log = LoggerFactory.getLogger(RuleIndex.class);

    private final Map<String, List<Rule>> toolTypeIndex;
    private final List<Rule> universalRules;
    private final Map<String, Rule> ruleById;
    private boolean modified;

    public RuleIndex() {
        this.toolTypeIndex = new ConcurrentHashMap<>();
        this.universalRules = new ArrayList<>();
        this.ruleById = new ConcurrentHashMap<>();
        this.modified = false;
    }

    /**
     * Add a rule to the index
     */
    public void addRule(Rule rule) {
        // For universal rules that match all tool types
        // We'll need to check if the rule matches all or use a different approach
        // For now, we'll add it to a universal list
        toolTypeIndex.computeIfAbsent("*", k -> new ArrayList<>()).add(rule);
        ruleById.put(rule.getId(), rule);
        modified = true;

        log.debug("Added rule {} to index", rule.getId());
    }

    /**
     * Add a rule for a specific tool type
     */
    public void addRuleForToolType(String toolType, Rule rule) {
        toolTypeIndex.computeIfAbsent(toolType, k -> new ArrayList<>()).add(rule);
        ruleById.put(rule.getId(), rule);
        modified = true;

        log.debug("Added rule {} for tool type {}", rule.getId(), toolType);
    }

    /**
     * Get rules for a specific tool type
     */
    public List<Rule> getRulesForToolType(String toolType) {
        List<Rule> rules = new ArrayList<>();

        // Add universal rules
        List<Rule> universal = toolTypeIndex.get("*");
        if (universal != null) {
            rules.addAll(universal);
        }

        // Add tool-specific rules
        List<Rule> toolRules = toolTypeIndex.get(toolType);
        if (toolRules != null) {
            rules.addAll(toolRules);
        }

        return rules;
    }

    /**
     * Get rule by ID
     */
    public Rule getRuleById(String ruleId) {
        return ruleById.get(ruleId);
    }

    /**
     * Rebuild the index (call after bulk rule updates)
     */
    public synchronized void rebuild() {
        if (!modified) {
            return;
        }

        log.debug("Rebuilding rule index");
        // In a more complex implementation, we might want to optimize the order
        // of rules within each tool type list based on priority or other metrics

        modified = false;
    }

    /**
     * Get index statistics
     */
    public IndexStats getStats() {
        return new IndexStats(
            ruleById.size(),
            toolTypeIndex.size(),
            toolTypeIndex.values().stream().mapToInt(List::size).sum()
        );
    }

    /**
     * Clear the index
     */
    public void clear() {
        toolTypeIndex.clear();
        universalRules.clear();
        ruleById.clear();
        modified = false;
    }

    public static class IndexStats {
        private final int totalRules;
        private final int toolTypeCount;
        private final int totalIndexedRules;

        public IndexStats(int totalRules, int toolTypeCount, int totalIndexedRules) {
            this.totalRules = totalRules;
            this.toolTypeCount = toolTypeCount;
            this.totalIndexedRules = totalIndexedRules;
        }

        public int getTotalRules() { return totalRules; }
        public int getToolTypeCount() { return toolTypeCount; }
        public int getTotalIndexedRules() { return totalIndexedRules; }
    }
}