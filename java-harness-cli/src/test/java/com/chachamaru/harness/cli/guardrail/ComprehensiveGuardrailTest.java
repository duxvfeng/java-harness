package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.cli.guardrail.cache.EvaluationCache;
import com.chachamaru.harness.cli.guardrail.config.CustomRuleConfig;
import com.chachamaru.harness.cli.guardrail.index.RuleIndex;
import com.chachamaru.harness.cli.guardrail.loader.CustomRuleLoader;
import com.chachamaru.harness.cli.guardrail.rules.*;
import com.chachamaru.harness.cli.hook.HookInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test comprehensive guardrail functionality including:
 * - Basic rule expansion (R16-R27)
 * - Custom rule support
 * - Rule priority system
 * - Performance optimization (caching and indexing)
 */
public class ComprehensiveGuardrailTest {

    private GuardrailEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new GuardrailEngine();
    }

    @Test
    public void testBasicRuleExpansion() {
        // Test R16: Database Write Rule
        engine.registerRule(new R16DatabaseWrite());
        engine.registerRule(new R17ContainerManagement());
        engine.registerRule(new R18ConfigFileWrite());
        engine.registerRule(new R19ExecutableDownload());
        engine.registerRule(new R20NetworkExposure());
        engine.registerRule(new R21SystemCritical());
        engine.registerRule(new R22CertificateManagement());
        engine.registerRule(new R23BackupDeletion());
        engine.registerRule(new R24LogManipulation());
        engine.registerRule(new R25ServiceRestart());
        engine.registerRule(new R26UserPermission());
        engine.registerRule(new R27CronSchedule());

        // Test database write detection
        HookInput dbInput = createBashInput("mysql -u root -p -e 'DROP DATABASE production'");
        GuardrailResult dbResult = engine.evaluate(dbInput);
        assertTrue(dbResult.isDenied(), "Database write should be denied");
        assertEquals("R16", dbResult.ruleId());

        // Test container management detection
        HookInput containerInput = createBashInput("docker rm -f my-container --env=prod");
        GuardrailResult containerResult = engine.evaluate(containerInput);
        assertTrue(containerResult.isDenied(), "Container management should be denied");
        assertEquals("R17", containerResult.ruleId());
    }

    @Test
    public void testCustomRuleSupport() {
        // Create a custom rule configuration
        CustomRuleConfig config = new CustomRuleConfig();
        config.setId("CUSTOM_01");
        config.setName("Test Custom Rule");
        config.setDescription("Test rule for custom functionality");
        config.setToolType("Bash");
        config.setPriority(50);

        List<CustomRuleConfig.ConditionConfig> conditions = List.of(
            createCondition("command_contains", "test-cmd", false)
        );
        config.setConditions(conditions);

        CustomRuleConfig.ActionConfig action = new CustomRuleConfig.ActionConfig();
        action.setDecision("deny");
        action.setMessage("Test command is not allowed");
        config.setAction(action);

        // Create and register dynamic rule
        DynamicRule customRule = new DynamicRule(config);
        engine.registerRule(customRule);

        // Test the custom rule
        HookInput testInput = createBashInput("test-cmd --arg1 --arg2");
        GuardrailResult result = engine.evaluate(testInput);

        assertTrue(result.isDenied(), "Custom rule should deny test-cmd");
        assertEquals("CUSTOM_01", result.ruleId());
        assertEquals("Test command is not allowed", result.reason());
    }

    @Test
    public void testRulePrioritySystem() {
        // Create rules with different priorities
        CustomRuleConfig lowPriorityConfig = createConfig("LOW_PRIORITY", "Bash", 10, "cmd1", "deny", "Low priority deny");
        CustomRuleConfig highPriorityConfig = createConfig("HIGH_PRIORITY", "Bash", 100, "cmd1", "deny", "High priority deny");

        DynamicRule lowRule = new DynamicRule(lowPriorityConfig);
        DynamicRule highRule = new DynamicRule(highPriorityConfig);

        engine.registerRule(lowRule);
        engine.registerRule(highRule);

        HookInput testInput = createBashInput("cmd1");
        GuardrailResult result = engine.evaluate(testInput);

        // High priority rule should execute first and deny
        assertTrue(result.isDenied(), "High priority rule should deny first");
        assertEquals("High priority deny", result.reason());
    }

    @Test
    public void testPerformanceCaching() {
        // Register a rule
        engine.registerRule(new R16DatabaseWrite());

        HookInput dbInput = createBashInput("mysql -u root -e 'UPDATE users SET status=0'");

        // First evaluation should compute the result
        long startTime = System.nanoTime();
        GuardrailResult firstResult = engine.evaluate(dbInput);
        long firstDuration = System.nanoTime() - startTime;

        // Second evaluation should use cache
        startTime = System.nanoTime();
        GuardrailResult cachedResult = engine.evaluate(dbInput);
        long cachedDuration = System.nanoTime() - startTime;

        assertEquals(firstResult.isDenied(), cachedResult.isDenied());
        assertTrue(cachedDuration < firstDuration, "Cached evaluation should be faster");

        // Test cache clearing
        engine.clearCache();
        startTime = System.nanoTime();
        GuardrailResult afterClearResult = engine.evaluate(dbInput);
        long afterClearDuration = System.nanoTime() - startTime;

        assertTrue(afterClearDuration >= cachedDuration, "After cache clear should take similar or longer time");
    }

    @Test
    public void testRuleIndexing() {
        // Register multiple rules for different tool types
        engine.registerRule(new R16DatabaseWrite()); // Bash
        engine.registerRule(new R18ConfigFileWrite()); // Write/Edit

        RuleIndex index = new RuleIndex();

        // Add rules to index
        index.addRuleForToolType("Bash", new R16DatabaseWrite());
        index.addRuleForToolType("Write", new R18ConfigFileWrite());
        index.addRuleForToolType("Edit", new R18ConfigFileWrite());

        index.rebuild();

        // Test retrieval by tool type
        List<Rule> bashRules = index.getRulesForToolType("Bash");
        assertFalse(bashRules.isEmpty(), "Should have bash rules");

        List<Rule> writeRules = index.getRulesForToolType("Write");
        assertFalse(writeRules.isEmpty(), "Should have write rules");

        // Test index stats
        RuleIndex.IndexStats stats = index.getStats();
        assertTrue(stats.getTotalRules() > 0, "Should have indexed rules");
    }

    @Test
    public void testCustomRuleValidation() {
        CustomRuleLoader loader = new CustomRuleLoader();

        // Test valid configuration
        CustomRuleConfig validConfig = new CustomRuleConfig();
        validConfig.setId("VALID_01");
        validConfig.setName("Valid Rule");
        validConfig.setToolType("Bash");
        validConfig.setConditions(List.of(createCondition("command_contains", "test", false)));
        validConfig.setAction(new CustomRuleConfig.ActionConfig());

        assertTrue(loader.validateRuleConfig(validConfig), "Valid config should pass validation");

        // Test invalid configuration (missing ID)
        CustomRuleConfig invalidConfig = new CustomRuleConfig();
        invalidConfig.setName("Invalid Rule");
        invalidConfig.setToolType("Bash");

        assertFalse(loader.validateRuleConfig(invalidConfig), "Invalid config should fail validation");
    }

    // Helper methods

    private HookInput createBashInput(String command) {
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("command", command);

        return new HookInput(
            "test-session",           // sessionId
            "/tmp/transcript.json",   // transcriptPath
            "/current/working/dir",   // cwd
            "bypass",                 // permissionMode
            "PreToolUse",             // hookEventName
            "Bash",                   // toolName
            toolInput,                // toolInput
            "/plugin/root"            // pluginRoot
        );
    }

    private CustomRuleConfig createConfig(String id, String toolType, int priority, String command, String decision, String message) {
        CustomRuleConfig config = new CustomRuleConfig();
        config.setId(id);
        config.setName(id + " Rule");
        config.setToolType(toolType);
        config.setPriority(priority);

        List<CustomRuleConfig.ConditionConfig> conditions = List.of(
            createCondition("command_contains", command, false)
        );
        config.setConditions(conditions);

        CustomRuleConfig.ActionConfig action = new CustomRuleConfig.ActionConfig();
        action.setDecision(decision);
        action.setMessage(message);
        config.setAction(action);

        return config;
    }

    private CustomRuleConfig.ConditionConfig createCondition(String type, String value, boolean caseSensitive) {
        CustomRuleConfig.ConditionConfig condition = new CustomRuleConfig.ConditionConfig();
        condition.setType(type);
        condition.setValue(value);
        condition.setCaseSensitive(caseSensitive);
        return condition;
    }
}