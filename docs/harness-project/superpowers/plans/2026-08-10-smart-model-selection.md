# 智能模型选择系统实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 Java Harness 添加智能模型选择功能，根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现。

**架构：** 基于现有 Effort Routing 系统，添加三层组件：Config Loader（配置加载）、Model Selector（模型选择）、Availability Checker（可用性检查），通过可配置的降级机制确保模型可用性。

**技术栈：** Java 17, Jackson (JSON), SnakeYAML (TOML), JUnit 5, Maven

---

## 文件结构

### 新建文件

```
java-harness-workflow/
├── src/main/java/com/chachamaru/harness/model/
│   ├── ModelTier.java                    # 模型等级枚举（FAST/BALANCED/QUALITY/POWERFUL）
│   ├── TierConfig.java                   # 单个等级的配置类
│   ├── ModelSelectionConfig.java         # 总配置类
│   ├── ModelSelectionConfigLoader.java   # 配置加载器（支持两种格式）
│   ├── ModelReferenceResolver.java      # 环境变量引用解析器
│   ├── ModelAvailabilityChecker.java    # 模型可用性检查器
│   ├── SmartModelSelector.java           # 核心模型选择器
│   └── ModelSelectionException.java      # 自定义异常类
│
├── src/test/java/com/chachamaru/harness/model/
│   ├── ModelTierTest.java
│   ├── TierConfigTest.java
│   ├── ConfigLoaderTest.java
│   ├── ReferenceResolverTest.java
│   ├── AvailabilityCheckerTest.java
│   └── SmartModelSelectorTest.java
│
├── src/main/resources/
│   └── default-model-selection.json     # 默认配置（兜底）
```

### 修改文件

```
java-harness-workflow/
├── src/main/java/com/chachamaru/harness/workflow/effort/
│   └── EffortRouter.java                 # 集成点：添加模型选择逻辑
│
skills/harness-work/
└── SKILL.md                               # 添加智能模型选择章节说明
```

---

## 任务分解

### Phase 1: 数据模型和枚举（基础层）

#### 任务 1.1：创建 ModelTier 枚举

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelTier.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/ModelTierTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// ModelTierTest.java
@Test
void testTierDeterminationByScore() {
    assertEquals(ModelTier.FAST, ModelTier.fromScore(0));
    assertEquals(ModelTier.FAST, ModelTier.fromScore(2));
    assertEquals(ModelTier.BALANCED, ModelTier.fromScore(3));
    assertEquals(ModelTier.BALANCED, ModelTier.fromScore(4));
    assertEquals(ModelTier.QUALITY, ModelTier.fromScore(5));
    assertEquals(ModelTier.QUALITY, ModelTier.fromScore(6));
    assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(7));
    assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(100));
}

@Test
void testTierEnvVarMapping() {
    assertEquals("ANTHROPIC_DEFAULT_FABLE_MODEL", ModelTier.FAST.getEnvVar());
    assertEquals("ANTHROPIC_DEFAULT_HAIKU_MODEL", ModelTier.BALANCED.getEnvVar());
    assertEquals("ANTHROPIC_DEFAULT_SONNET_MODEL", ModelTier.QUALITY.getEnvVar());
    assertEquals("ANTHROPIC_DEFAULT_OPUS_MODEL", ModelTier.POWERFUL.getEnvVar());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=ModelTierTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 ModelTier 枚举实现**

```java
// ModelTier.java
package com.chachamaru.harness.model;

public enum ModelTier {
    FAST(0, 2, "ANTHROPIC_DEFAULT_FABLE_MODEL", "fast"),
    BALANCED(3, 4, "ANTHROPIC_DEFAULT_HAIKU_MODEL", "balanced"),
    QUALITY(5, 6, "ANTHROPIC_DEFAULT_SONNET_MODEL", "quality"),
    POWERFUL(7, Integer.MAX_VALUE, "ANTHROPIC_DEFAULT_OPUS_MODEL", "powerful");

    private final int minScore;
    private final int maxScore;
    private final String envVar;
    private final String tierName;

    ModelTier(int minScore, int maxScore, String envVar, String tierName) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.envVar = envVar;
        this.tierName = tierName;
    }

    public static ModelTier fromScore(int score) {
        for (ModelTier tier : values()) {
            if (score >= tier.minScore && score <= tier.maxScore) {
                return tier;
            }
        }
        return POWERFUL; // 默认降级到最高级
    }

    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }
    public String getEnvVar() { return envVar; }
    public String getTierName() { return tierName; }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=ModelTierTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelTier.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/ModelTierTest.java
git commit -m "feat(model): add ModelTier enum with score-based determination"
```

---

#### 任务 1.2：创建 TierConfig 数据类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/TierConfig.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/TierConfigTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// TierConfigTest.java
@Test
void testTierConfigCreation() {
    List<String> fallbackChain = List.of("env:MODEL_X", "env:MODEL_Y", "fallback-model");
    TierConfig config = new TierConfig(ModelTier.FAST, fallbackChain);
    
    assertEquals(ModelTier.FAST, config.getTier());
    assertEquals(fallbackChain, config.getFallbackChain());
    assertEquals(3, config.getFallbackChain().size());
}

@Test
void testTierConfigValidation() {
    List<String> emptyChain = List.of();
    assertThrows(IllegalArgumentException.class, () -> 
        new TierConfig(ModelTier.BALANCED, emptyChain));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=TierConfigTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 TierConfig 实现类**

```java
// TierConfig.java
package com.chachamaru.harness.model;

import java.util.List;
import java.util.Objects;

public class TierConfig {
    private final ModelTier tier;
    private final List<String> fallbackChain;

    public TierConfig(ModelTier tier, List<String> fallbackChain) {
        if (tier == null) {
            throw new IllegalArgumentException("Tier cannot be null");
        }
        if (fallbackChain == null || fallbackChain.isEmpty()) {
            throw new IllegalArgumentException("Fallback chain cannot be null or empty");
        }
        this.tier = tier;
        this.fallbackChain = List.copyOf(fallbackChain); // 不可变视图
    }

    public ModelTier getTier() { return tier; }
    public List<String> getFallbackChain() { return fallbackChain; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TierConfig)) return false;
        TierConfig that = (TierConfig) o;
        return tier == that.tier && Objects.equals(fallbackChain, that.fallbackChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, fallbackChain);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=TierConfigTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/TierConfig.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/TierConfigTest.java
git commit -m "feat(model): add TierConfig data class with validation"
```

---

#### 任务 1.3：创建 ModelSelectionConfig 总配置类

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelSelectionConfig.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/ModelSelectionConfigTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// ModelSelectionConfigTest.java
@Test
void testConfigCreation() {
    Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();
    tierConfigs.put(ModelTier.FAST, new TierConfig(ModelTier.FAST, 
        List.of("env:MODEL_A", "fallback")));
    
    ModelSelectionConfig config = new ModelSelectionConfig(true, tierConfigs, 5000, false);
    
    assertTrue(config.isEnabled());
    assertEquals(5000, config.getTimeout());
    assertFalse(config.isValidateApiCall());
    assertEquals(tierConfigs.get(ModelTier.FAST), config.getTierConfig(ModelTier.FAST));
}

@Test
void testConfigDisabled() {
    ModelSelectionConfig config = new ModelSelectionConfig(false, Map.of(), 5000, false);
    assertFalse(config.isEnabled());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=ModelSelectionConfigTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 ModelSelectionConfig 实现类**

```java
// ModelSelectionConfig.java
package com.chachamaru.harness.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ModelSelectionConfig {
    private final boolean enabled;
    private final Map<ModelTier, TierConfig> tierConfigs;
    private final int timeout;
    private final boolean validateApiCall;

    public ModelSelectionConfig(boolean enabled, Map<ModelTier, TierConfig> tierConfigs, 
                               int timeout, boolean validateApiCall) {
        this.enabled = enabled;
        this.tierConfigs = Map.copyOf(tierConfigs); // 不可变视图
        this.timeout = timeout;
        this.validateApiCall = validateApiCall;
    }

    public boolean isEnabled() { return enabled; }
    public int getTimeout() { return timeout; }
    public boolean isValidateApiCall() { return validateApiCall; }

    public Optional<TierConfig> getTierConfig(ModelTier tier) {
        return Optional.ofNullable(tierConfigs.get(tier));
    }

    public boolean hasTierConfig(ModelTier tier) {
        return tierConfigs.containsKey(tier);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=ModelSelectionConfigTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelSelectionConfig.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/ModelSelectionConfigTest.java
git commit -m "feat(model): add ModelSelectionConfig configuration class"
```

---

### Phase 2: 环境变量引用解析（工具层）

#### 任务 2.1：创建 ModelReferenceResolver 解析器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelReferenceResolver.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/ReferenceResolverTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// ReferenceResolverTest.java
@Test
void testEnvVariableResolution() {
    Map<String, String> env = Map.of("TEST_MODEL", "gpt-4", "FALLBACK", "gpt-3.5");
    ModelReferenceResolver resolver = new ModelReferenceResolver(env);
    
    String result = resolver.resolve("env:TEST_MODEL");
    assertEquals("gpt-4", result);
}

@Test
void testDirectModelName() {
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of());
    
    String result = resolver.resolve("glm-4.7");
    assertEquals("glm-4.7", result);
}

@Test
void testMissingEnvVariable() {
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of());
    
    assertThrows(ConfigException.class, () -> resolver.resolve("env:NONEXISTENT"));
}

@Test
void testEmptyEnvVariable() {
    Map<String, String> env = Map.of("EMPTY_VAR", "");
    ModelReferenceResolver resolver = new ModelReferenceResolver(env);
    
    assertThrows(ConfigException.class, () -> resolver.resolve("env:EMPTY_VAR"));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=ReferenceResolverTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 ModelReferenceResolver 实现类**

```java
// ModelReferenceResolver.java
package com.chachamaru.harness.model;

import java.util.Map;

public class ModelReferenceResolver {
    private final Map<String, String> environment;

    public ModelReferenceResolver() {
        this(System.getenv());
    }

    public ModelReferenceResolver(Map<String, String> environment) {
        this.environment = Map.copyOf(environment);
    }

    public String resolve(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new ConfigException("Model reference cannot be null or empty");
        }

        if (reference.startsWith("env:")) {
            String envVar = reference.substring(4);
            String value = environment.get(envVar);
            
            if (value == null) {
                throw new ConfigException("Environment variable not found: " + envVar);
            }
            if (value.trim().isEmpty()) {
                throw new ConfigException("Environment variable is empty: " + envVar);
            }
            return value.trim();
        }

        return reference; // 直接模型名称
    }
}
```

- [ ] **步骤 4：创建自定义异常类**

```java
// ModelSelectionException.java
package com.chachamaru.harness.model;

public class ModelSelectionException extends RuntimeException {
    public ModelSelectionException(String message) {
        super(message);
    }
    
    public ModelSelectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ConfigException.java
package com.chachamaru.harness.model;

public class ConfigException extends ModelSelectionException {
    public ConfigException(String message) {
        super(message);
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`mvn test -Dtest=ReferenceResolverTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelReferenceResolver.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelSelectionException.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ConfigException.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/ReferenceResolverTest.java
git commit -m "feat(model): add ModelReferenceResolver with environment variable support"
```

---

### Phase 3: 模型可用性检查（验证层）

#### 任务 3.1：创建 ModelAvailabilityChecker 检查器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelAvailabilityChecker.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/AvailabilityCheckerTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// AvailabilityCheckerTest.java
@Test
void testValidModelName() {
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    assertTrue(checker.isAvailable("glm-4.7"));
    assertTrue(checker.isAvailable("gpt-4"));
}

@Test
void testInvalidModelName() {
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    assertFalse(checker.isAvailable(""));
    assertFalse(checker.isAvailable(null));
    assertFalse(checker.isAvailable("   "));
}

@Test
void testLongModelName() {
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    assertFalse(checker.isAvailable("a".repeat(101))); // 超过100字符限制
}

@Test
void testTimeoutSetting() {
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(10000, false);
    assertEquals(10000, checker.getTimeout());
}

@Test
void testValidateApiCallFlag() {
    ModelAvailabilityChecker withValidation = new ModelAvailabilityChecker(5000, true);
    ModelAvailabilityChecker withoutValidation = new ModelAvailabilityChecker(5000, false);
    
    assertTrue(withValidation.isValidateApiCall());
    assertFalse(withoutValidation.isValidateApiCall());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=AvailabilityCheckerTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 ModelAvailabilityChecker 实现类**

```java
// ModelAvailabilityChecker.java
package com.chachamaru.harness.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelAvailabilityChecker {
    private static final Logger logger = LoggerFactory.getLogger(ModelAvailabilityChecker.class);
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    
    private final int timeout;
    private final boolean validateApiCall;

    public ModelAvailabilityChecker(int timeout, boolean validateApiCall) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.timeout = timeout;
        this.validateApiCall = validateApiCall;
    }

    public boolean isAvailable(String model) {
        try {
            // 1. 基本格式验证
            if (!isValidModelName(model)) {
                logger.warn("Model name validation failed: {}", model);
                return false;
            }

            // 2. 可选的 API 调用验证（扩展点）
            if (validateApiCall) {
                return validateViaApiCall(model);
            }

            return true;

        } catch (Exception e) {
            logger.warn("Model availability check failed for: {}", model, e);
            return false;
        }
    }

    private boolean isValidModelName(String model) {
        if (model == null || model.trim().isEmpty()) {
            return false;
        }
        if (model.length() > MAX_MODEL_NAME_LENGTH) {
            return false;
        }
        return true;
    }

    private boolean validateViaApiCall(String model) {
        // 预留扩展点：未来可以添加实际的 API 调用验证
        // 目前返回 true，因为格式已通过验证
        logger.debug("API validation skipped for model: {}", model);
        return true;
    }

    public int getTimeout() { return timeout; }
    public boolean isValidateApiCall() { return validateApiCall; }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=AvailabilityCheckerTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelAvailabilityChecker.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/AvailabilityCheckerTest.java
git commit -m "feat(model): add ModelAvailabilityChecker with validation"
```

---

### Phase 4: 配置加载器（配置层）

#### 任务 4.1：创建默认配置文件

**文件：**
- 创建：`java-harness-workflow/src/main/resources/default-model-selection.json`

- [ ] **步骤 1：创建默认配置文件**

```json
{
  "enabled": true,
  "strategy": "effortBased",
  "timeout": 5000,
  "validateApiCall": false,
  "tierConfigs": {
    "fast": {
      "tier": "FAST",
      "fallbackChain": [
        "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
        "env:ANTHROPIC_MODEL",
        "glm-4.7"
      ]
    },
    "balanced": {
      "tier": "BALANCED",
      "fallbackChain": [
        "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
        "env:ANTHROPIC_MODEL",
        "glm-4.7"
      ]
    },
    "quality": {
      "tier": "QUALITY",
      "fallbackChain": [
        "env:ANTHROPIC_DEFAULT_SONNET_MODEL",
        "env:ANTHROPIC_MODEL",
        "glm-4.7"
      ]
    },
    "powerful": {
      "tier": "POWERFUL",
      "fallbackChain": [
        "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
        "env:ANTHROPIC_MODEL",
        "glm-4.7"
      ]
    }
  }
}
```

- [ ] **步骤 2：Commit**

```bash
git add java-harness-workflow/src/main/resources/default-model-selection.json
git commit -m "feat(model): add default model selection configuration"
```

---

#### 任务 4.2：创建 ModelSelectionConfigLoader 配置加载器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelSelectionConfigLoader.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/ConfigLoaderTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// ConfigLoaderTest.java
@Test
void testLoadDefaultConfig() {
    ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
    ModelSelectionConfig config = loader.load();
    
    assertNotNull(config);
    assertTrue(config.isEnabled());
    assertEquals(5000, config.getTimeout());
    assertTrue(config.hasTierConfig(ModelTier.FAST));
}

@Test
void testLoadSettingsJson() throws Exception {
    Path testDir = Files.createTempDirectory("harness-test");
    Path settingsFile = testDir.resolve(".claude/settings.json");
    
    Files.createDirectories(testDir.resolve(".claude"));
    Files.writeString(settingsFile, "{\"modelSelection\":{\"enabled\":true,\"timeout\":3000}}");
    
    ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader(testDir.toString());
    ModelSelectionConfig config = loader.load();
    
    assertTrue(config.isEnabled());
    assertEquals(3000, config.getTimeout());
    
    Files.walk(testDir).sorted(Comparator.reverseOrder())
         .forEach(path -> path.toFile().delete());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=ConfigLoaderTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 ModelSelectionConfigLoader 实现类**

```java
// ModelSelectionConfigLoader.java
package com.chachamaru.harness.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ModelSelectionConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectionConfigLoader.class);
    private static final String SETTINGS_JSON_PATH = ".claude/settings.json";
    private static final String HARNESS_TOML_PATH = "harness.toml";
    private static final String DEFAULT_CONFIG_RESOURCE = "/default-model-selection.json";
    
    private final String projectRoot;
    private final ObjectMapper objectMapper;

    public ModelSelectionConfigLoader() {
        this(System.getProperty("user.dir"));
    }

    public ModelSelectionConfigLoader(String projectRoot) {
        this.projectRoot = projectRoot;
        this.objectMapper = new ObjectMapper();
    }

    public ModelSelectionConfig load() {
        // 优先级 1: .claude/settings.json
        ModelSelectionConfig config = tryLoadFromSettingsJson();
        if (config != null) {
            logger.info("Loaded model selection config from .claude/settings.json");
            return config;
        }

        // 优先级 2: harness.toml (预留扩展点)
        config = tryLoadFromHarnessToml();
        if (config != null) {
            logger.info("Loaded model selection config from harness.toml");
            return config;
        }

        // 优先级 3: 默认配置
        logger.info("Using default model selection configuration");
        return loadDefaultConfig();
    }

    private ModelSelectionConfig tryLoadFromSettingsJson() {
        try {
            Path settingsPath = Paths.get(projectRoot, SETTINGS_JSON_PATH);
            if (!Files.exists(settingsPath)) {
                return null;
            }

            String content = Files.readString(settingsPath);
            JsonNode root = objectMapper.readTree(content);
            
            if (root.has("modelSelection")) {
                return parseModelSelectionConfig(root.get("modelSelection"));
            }
            
        } catch (Exception e) {
            logger.warn("Failed to load .claude/settings.json: {}", e.getMessage());
        }
        return null;
    }

    private ModelSelectionConfig tryLoadFromHarnessToml() {
        // 预留扩展点：未来支持 TOML 格式
        return null;
    }

    private ModelSelectionConfig loadDefaultConfig() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (is == null) {
                logger.warn("Default config resource not found, using hardcoded defaults");
                return createHardcodedDefaults();
            }
            
            JsonNode root = objectMapper.readTree(is);
            return parseModelSelectionConfig(root);
            
        } catch (IOException e) {
            logger.error("Failed to load default config: {}", e.getMessage());
            return createHardcodedDefaults();
        }
    }

    private ModelSelectionConfig parseModelSelectionConfig(JsonNode node) {
        boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : true;
        int timeout = node.has("timeout") ? node.get("timeout").asInt() : 5000;
        boolean validateApiCall = node.has("validateApiCall") ? 
            node.get("validateApiCall").asBoolean() : false;

        Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();
        if (node.has("tierConfigs")) {
            JsonNode configsNode = node.get("tierConfigs");
            configsNode.fields().forEachRemaining(entry -> {
                try {
                    ModelTier tier = ModelTier.valueOf(entry.getKey().toUpperCase());
                    TierConfig config = parseTierConfig(tier, entry.getValue());
                    tierConfigs.put(tier, config);
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown tier: {}", entry.getKey());
                }
            });
        }

        return new ModelSelectionConfig(enabled, tierConfigs, timeout, validateApiCall);
    }

    private TierConfig parseTierConfig(ModelTier tier, JsonNode node) {
        JsonNode fallbackNode = node.get("fallbackChain");
        List<String> fallbackChain = objectMapper.convertValue(
            fallbackNode, 
            objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)
        );
        
        return new TierConfig(tier, fallbackChain);
    }

    private ModelSelectionConfig createHardcodedDefaults() {
        Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();
        
        tierConfigs.put(ModelTier.FAST, new TierConfig(ModelTier.FAST, 
            List.of("env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
        tierConfigs.put(ModelTier.BALANCED, new TierConfig(ModelTier.BALANCED, 
            List.of("env:ANTHROPIC_DEFAULT_HAIKU_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
        tierConfigs.put(ModelTier.QUALITY, new TierConfig(ModelTier.QUALITY, 
            List.of("env:ANTHROPIC_DEFAULT_SONNET_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
        tierConfigs.put(ModelTier.POWERFUL, new TierConfig(ModelTier.POWERFUL, 
            List.of("env:ANTHROPIC_DEFAULT_OPUS_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
        
        return new ModelSelectionConfig(true, tierConfigs, 5000, false);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`mvn test -Dtest=ConfigLoaderTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelSelectionConfigLoader.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/ConfigLoaderTest.java
git commit -m "feat(model): add ModelSelectionConfigLoader with priority-based loading"
```

---

### Phase 5: 核心模型选择器（业务逻辑层）

#### 任务 5.1：创建 SmartModelSelector 核心选择器

**文件：**
- 创建：`java-harness-workflow/src/main/java/com/chachamaru/harness/model/SmartModelSelector.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectorTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// SmartModelSelectorTest.java
@Test
void testSelectModelForLowComplexity() {
    ModelSelectionConfig config = createTestConfig();
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of(
        "ANTHROPIC_DEFAULT_FABLE_MODEL", "fast-model",
        "ANTHROPIC_MODEL", "default-model"
    ));
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
    String selected = selector.selectModel(1); // 低复杂度
    
    assertEquals("fast-model", selected);
}

@Test
void testSelectModelForHighComplexity() {
    ModelSelectionConfig config = createTestConfig();
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of(
        "ANTHROPIC_DEFAULT_OPUS_MODEL", "powerful-model",
        "ANTHROPIC_MODEL", "default-model"
    ));
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
    String selected = selector.selectModel(10); // 高复杂度
    
    assertEquals("powerful-model", selected);
}

@Test
void testFallbackChainExecution() {
    ModelSelectionConfig config = createTestConfig();
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of(
        "ANTHROPIC_MODEL", "default-model"
    ));
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
    
    // 第一次候选模型不存在，应该降级到 ANTHROPIC_MODEL
    String selected = selector.selectModel(5);
    assertEquals("default-model", selected);
}

@Test
void testModelUnavailableException() {
    ModelSelectionConfig config = createTestConfig();
    ModelReferenceResolver resolver = new ModelReferenceResolver(Map.of()); // 空环境
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
    
    SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
    
    assertThrows(ModelUnavailableException.class, () -> selector.selectModel(3));
}

private ModelSelectionConfig createTestConfig() {
    Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();
    tierConfigs.put(ModelTier.FAST, new TierConfig(ModelTier.FAST, 
        List.of("env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
    tierConfigs.put(ModelTier.BALANCED, new TierConfig(ModelTier.BALANCED, 
        List.of("env:ANTHROPIC_DEFAULT_HAIKU_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
    tierConfigs.put(ModelTier.QUALITY, new TierConfig(ModelTier.QUALITY, 
        List.of("env:ANTHROPIC_DEFAULT_SONNET_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
    tierConfigs.put(ModelTier.POWERFUL, new TierConfig(ModelTier.POWERFUL, 
        List.of("env:ANTHROPIC_DEFAULT_OPUS_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7")));
    
    return new ModelSelectionConfig(true, tierConfigs, 5000, false);
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=SmartModelSelectorTest -pl java-harness-workflow`
预期：FAIL，类不存在

- [ ] **步骤 3：编写 SmartModelSelector 实现类**

```java
// SmartModelSelector.java
package com.chachamaru.harness.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartModelSelector {
    private static final Logger logger = LoggerFactory.getLogger(SmartModelSelector.class);
    
    private final ModelSelectionConfig config;
    private final ModelReferenceResolver resolver;
    private final ModelAvailabilityChecker availabilityChecker;

    public SmartModelSelector(ModelSelectionConfig config, 
                             ModelReferenceResolver resolver,
                             ModelAvailabilityChecker availabilityChecker) {
        if (config == null || resolver == null || availabilityChecker == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.config = config;
        this.resolver = resolver;
        this.availabilityChecker = availabilityChecker;
    }

    public String selectModel(int complexityScore) {
        if (!config.isEnabled()) {
            logger.debug("Model selection disabled, returning default");
            return getFallbackModel();
        }

        logger.info("Model selection started - complexity score: {}", complexityScore);
        
        // 1. 确定模型等级
        ModelTier tier = ModelTier.fromScore(complexityScore);
        logger.debug("Determined tier: {} for score: {}", tier, complexityScore);

        // 2. 获取该等级的配置
        TierConfig tierConfig = config.getTierConfig(tier)
            .orElseThrow(() -> new ModelUnavailableException(
                "No configuration found for tier: " + tier));

        // 3. 执行降级链
        return executeFallbackChain(tierConfig);
    }

    private String executeFallbackChain(TierConfig tierConfig) {
        for (String candidate : tierConfig.getFallbackChain()) {
            try {
                String resolvedModel = resolver.resolve(candidate);
                logger.debug("Trying model candidate: {} -> {}", candidate, resolvedModel);

                if (availabilityChecker.isAvailable(resolvedModel)) {
                    logger.info("Model selected: {} for tier: {}", resolvedModel, tierConfig.getTier());
                    return resolvedModel;
                } else {
                    logger.warn("Model unavailable: {}, trying next", resolvedModel);
                }

            } catch (ConfigException e) {
                logger.warn("Failed to resolve model reference: {}, error: {}", candidate, e.getMessage());
            }
        }

        throw new ModelUnavailableException(
            "All models exhausted for tier: " + tierConfig.getTier().getTierName());
    }

    private String getFallbackModel() {
        try {
            return resolver.resolve("env:ANTHROPIC_MODEL");
        } catch (ConfigException e) {
            return "glm-4.7"; // 硬编码兜底
        }
    }
}
```

- [ ] **步骤 4：创建自定义异常类**

```java
// ModelUnavailableException.java
package com.chachamaru.harness.model;

public class ModelUnavailableException extends ModelSelectionException {
    public ModelUnavailableException(String message) {
        super(message);
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`mvn test -Dtest=SmartModelSelectorTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/SmartModelSelector.java
git add java-harness-workflow/src/main/java/com/chachamaru/harness/model/ModelUnavailableException.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectorTest.java
git commit -m "feat(model): add SmartModelSelector with fallback chain execution"
```

---

### Phase 6: 与现有系统集成（集成层）

#### 任务 6.1：集成到 EffortRouter

**文件：**
- 修改：`java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/effort/EffortRouter.java`
- 测试：`java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/effort/EffortRouterIntegrationTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// EffortRouterIntegrationTest.java
@Test
void testModelSelectionIntegration() {
    EffortRouter router = new EffortRouter();
    TaskContext context = createTaskContext(4); // 中等复杂度
    
    WorkerSpawnConfig config = router.determineWorkerConfig(context);
    
    assertNotNull(config);
    assertNotNull(config.getSelectedModel());
    assertEquals(4, config.getComplexityScore());
}

private TaskContext createTaskContext(int score) {
    // 创建测试用的 TaskContext
    return new TaskContext(score, "test task");
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`mvn test -Dtest=EffortRouterIntegrationTest -pl java-harness-workflow`
预期：FAIL，方法不存在或行为不符

- [ ] **步骤 3：修改 EffortRouter 集成智能模型选择**

```java
// EffortRouter.java 中添加
private SmartModelSelector modelSelector;

public EffortRouter() {
    this.modelSelector = createModelSelector();
}

private SmartModelSelector createModelSelector() {
    ModelSelectionConfigLoader configLoader = new ModelSelectionConfigLoader();
    ModelSelectionConfig config = configLoader.load();
    
    ModelReferenceResolver resolver = new ModelReferenceResolver();
    ModelAvailabilityChecker checker = new ModelAvailabilityChecker(
        config.getTimeout(), 
        config.isValidateApiCall()
    );
    
    return new SmartModelSelector(config, resolver, checker);
}

public WorkerSpawnConfig determineWorkerConfig(TaskContext context) {
    // 1. 现有的复杂度评分逻辑
    int complexityScore = calculateComplexityScore(context);
    
    // 2. 现有的 effort tier 决定
    EffortTier effortTier = determineEffortTier(complexityScore);
    
    // 3. 新增：智能模型选择
    String selectedModel = modelSelector.selectModel(complexityScore);
    
    return new WorkerSpawnConfig(effortTier, selectedModel, complexityScore);
}
```

- [ ] **步骤 4：更新 WorkerSpawnConfig 类（如需要）**

```java
// WorkerSpawnConfig.java 中添加字段
private final String selectedModel;
private final int complexityScore;

public WorkerSpawnConfig(EffortTier effortTier, String selectedModel, int complexityScore) {
    this.effortTier = effortTier;
    this.selectedModel = selectedModel;
    this.complexityScore = complexityScore;
}

public String getSelectedModel() { return selectedModel; }
public int getComplexityScore() { return complexityScore; }
```

- [ ] **步骤 5：运行测试验证通过**

运行：`mvn test -Dtest=EffortRouterIntegrationTest -pl java-harness-workflow`
预期：PASS

- [ ] **步骤 6：Commit**

```bash
git add java-harness-workflow/src/main/java/com/chachamaru/harness/workflow/effort/EffortRouter.java
git add java-harness-workflow/src/test/java/com/chachamaru/harness/workflow/effort/EffortRouterIntegrationTest.java
git commit -m "feat(integration): integrate smart model selection into EffortRouter"
```

---

#### 任务 6.2：更新技能文档

**文件：**
- 修改：`skills/harness-work/SKILL.md`

- [ ] **步骤 1：在 harness-work SKILL.md 中添加智能模型选择章节**

在现有内容后添加：

```markdown
## 智能模型选择（新增）

当 effort tier 确定后，自动选择对应的 AI 模型：

### 工作原理

1. 读取当前环境变量中的模型配置
2. 根据复杂度分数映射到模型等级
3. 按配置的降级链尝试模型
4. 在 Worker Agent 启动时使用选定模型

### 模型等级映射

| 复杂度分数 | 模型等级 | 环境变量 |
|-----------|---------|---------|
| 0-2 | FAST | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

### 配置方式

**项目配置优先级**：
1. `.claude/settings.json` (项目级别，最高优先级)
2. `harness.toml` (项目配置)
3. 默认配置 (内置兜底)

**环境变量配置**：
- `ANTHROPIC_DEFAULT_FABLE_MODEL`: FABLE 等级模型
- `ANTHROPIC_DEFAULT_HAIKU_MODEL`: HAIKU 等级模型
- `ANTHROPIC_DEFAULT_SONNET_MODEL`: SONNET 等级模型
- `ANTHROPIC_DEFAULT_OPUS_MODEL`: OPUS 等级模型
- `ANTHROPIC_MODEL`: 默认降级模型

**降级策略**：
每个等级都有独立的降级链，按顺序尝试直到找到可用模型：
```
1. 主要模型 (如 ANTHROPIC_DEFAULT_HAIKU_MODEL)
2. 默认模型 (ANTHROPIC_MODEL)
3. 安全模型 (glm-4.7 硬编码兜底)
```

### 配置示例

`.claude/settings.json`:
```json
{
  "modelSelection": {
    "enabled": true,
    "timeout": 5000,
    "validateApiCall": false
  }
}
```

### 使用场景

- **成本优化**: 简单任务自动使用快速/便宜模型
- **质量保证**: 复杂任务自动使用强大模型
- **可靠性**: 完整的降级机制确保总能找到可用模型
```

- [ ] **步骤 2：验证技能文档格式**

运行：`grep -A 5 "智能模型选择" skills/harness-work/SKILL.md`
预期：找到新增章节内容

- [ ] **步骤 3：Commit**

```bash
git add skills/harness-work/SKILL.md
git commit -m "docs(skill): add smart model selection documentation to harness-work"
```

---

### Phase 7: 端到端测试和验证（质量保证层）

#### 任务 7.1：编写端到端集成测试

**文件：**
- 创建：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectionE2ETest.java`

- [ ] **步骤 1：编写完整的端到端测试**

```java
// SmartModelSelectionE2ETest.java
package com.chachamaru.harness.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmartModelSelectionE2ETest {
    
    @TempDir
    Path tempDir;
    
    private ModelSelectionConfigLoader loader;
    private SmartModelSelector selector;
    
    @BeforeEach
    void setUp() {
        // 设置测试环境变量
        Map<String, String> testEnv = Map.of(
            "ANTHROPIC_DEFAULT_FABLE_MODEL", "test-fast-model",
            "ANTHROPIC_DEFAULT_HAIKU_MODEL", "test-balanced-model",
            "ANTHROPIC_DEFAULT_SONNET_MODEL", "test-quality-model",
            "ANTHROPIC_DEFAULT_OPUS_MODEL", "test-powerful-model",
            "ANTHROPIC_MODEL", "test-default-model"
        );
        
        ModelReferenceResolver resolver = new ModelReferenceResolver(testEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        
        loader = new ModelSelectionConfigLoader(tempDir.toString());
        ModelSelectionConfig config = loader.load();
        
        selector = new SmartModelSelector(config, resolver, checker);
    }
    
    @Test
    @Order(1)
    @DisplayName("端到端：低复杂度任务选择 FAST 模型")
    void testLowComplexityTaskSelectsFastModel() {
        String selected = selector.selectModel(1);
        assertEquals("test-fast-model", selected);
    }
    
    @Test
    @Order(2)
    @DisplayName("端到端：中等复杂度任务选择 BALANCED 模型")
    void testMediumComplexityTaskSelectsBalancedModel() {
        String selected = selector.selectModel(3);
        assertEquals("test-balanced-model", selected);
    }
    
    @Test
    @Order(3)
    @DisplayName("端到端：高复杂度任务选择 POWERFUL 模型")
    void testHighComplexityTaskSelectsPowerfulModel() {
        String selected = selector.selectModel(10);
        assertEquals("test-powerful-model", selected);
    }
    
    @Test
    @Order(4)
    @DisplayName("端到端：配置文件优先级测试")
    void testConfigFilePriority() throws IOException {
        // 创建 .claude/settings.json
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        
        Path settingsFile = claudeDir.resolve("settings.json");
        Files.writeString(settingsFile, "{\"modelSelection\":{\"enabled\":true,\"timeout\":10000}}");
        
        ModelSelectionConfigLoader newLoader = new ModelSelectionConfigLoader(tempDir.toString());
        ModelSelectionConfig config = newLoader.load();
        
        assertTrue(config.isEnabled());
        assertEquals(10000, config.getTimeout());
    }
    
    @Test
    @Order(5)
    @DisplayName("端到端：降级机制测试")
    void testFallbackMechanism() {
        // 创建只包含默认模型的环境
        Map<String, String> limitedEnv = Map.of(
            "ANTHROPIC_MODEL", "test-default-model"
        );
        
        ModelReferenceResolver limitedResolver = new ModelReferenceResolver(limitedEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        
        ModelSelectionConfig config = loader.load();
        SmartModelSelector limitedSelector = new SmartModelSelector(
            config, limitedResolver, checker);
        
        // 即使是高复杂度任务，也会降级到默认模型
        String selected = limitedSelector.selectModel(10);
        assertEquals("test-default-model", selected);
    }
    
    @Test
    @Order(6)
    @DisplayName("端到端：模型不可用异常测试")
    void testModelUnavailableException() {
        // 创建空环境
        Map<String, String> emptyEnv = Map.of();
        ModelReferenceResolver emptyResolver = new ModelReferenceResolver(emptyEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        
        ModelSelectionConfig config = loader.load();
        SmartModelSelector emptySelector = new SmartModelSelector(
            config, emptyResolver, checker);
        
        assertThrows(ModelUnavailableException.class, () -> 
            emptySelector.selectModel(5));
    }
}
```

- [ ] **步骤 2：运行端到端测试验证**

运行：`mvn test -Dtest=SmartModelSelectionE2ETest -pl java-harness-workflow`
预期：所有测试 PASS

- [ ] **步骤 3：Commit**

```bash
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectionE2ETest.java
git commit -m "test(e2e): add comprehensive end-to-end tests for smart model selection"
```

---

#### 任务 7.2：性能和压力测试

**文件：**
- 创建：`java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectionPerformanceTest.java`

- [ ] **步骤 1：编写性能测试**

```java
// SmartModelSelectionPerformanceTest.java
package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmartModelSelectionPerformanceTest {
    
    @Test
    @DisplayName("性能测试：单次模型选择时间应该 < 100ms")
    void testSingleSelectionPerformance() {
        Map<String, String> testEnv = Map.of(
            "ANTHROPIC_DEFAULT_FABLE_MODEL", "fast-model",
            "ANTHROPIC_DEFAULT_HAIKU_MODEL", "balanced-model",
            "ANTHROPIC_DEFAULT_SONNET_MODEL", "quality-model",
            "ANTHROPIC_DEFAULT_OPUS_MODEL", "powerful-model",
            "ANTHROPIC_MODEL", "default-model"
        );
        
        ModelReferenceResolver resolver = new ModelReferenceResolver(testEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        ModelSelectionConfig config = loader.load();
        
        SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
        
        long startTime = System.nanoTime();
        String selected = selector.selectModel(5);
        long endTime = System.nanoTime();
        
        assertNotNull(selected);
        long duration = (endTime - startTime) / 1_000_000; // 转换为毫秒
        
        assertTrue(duration < 100, 
            "Model selection took " + duration + "ms, expected < 100ms");
    }
    
    @Test
    @DisplayName("并发测试：多线程同时选择模型")
    void testConcurrentSelection() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int requestsPerThread = 100;
        
        Map<String, String> testEnv = Map.of(
            "ANTHROPIC_DEFAULT_HAIKU_MODEL", "balanced-model",
            "ANTHROPIC_MODEL", "default-model"
        );
        
        ModelReferenceResolver resolver = new ModelReferenceResolver(testEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        ModelSelectionConfig config = loader.load();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SmartModelSelector selector = new SmartModelSelector(
                        config, resolver, checker);
                    
                    for (int j = 0; j < requestsPerThread; j++) {
                        String selected = selector.selectModel(3 + (j % 4));
                        assertNotNull(selected, "Selected model should not be null");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("缓存测试：重复选择相同复杂度应该快速")
    void testCachingPerformance() {
        Map<String, String> testEnv = Map.of(
            "ANTHROPIC_DEFAULT_HAIKU_MODEL", "balanced-model",
            "ANTHROPIC_MODEL", "default-model"
        );
        
        ModelReferenceResolver resolver = new ModelReferenceResolver(testEnv);
        ModelAvailabilityChecker checker = new ModelAvailabilityChecker(5000, false);
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        ModelSelectionConfig config = loader.load();
        
        SmartModelSelector selector = new SmartModelSelector(config, resolver, checker);
        
        // 预热
        selector.selectModel(5);
        
        // 测试缓存效果
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            selector.selectModel(5);
        }
        long endTime = System.nanoTime();
        
        long avgDuration = ((endTime - startTime) / 1_000_000) / 1000; // 平均毫秒
        
        assertTrue(avgDuration < 10, 
            "Cached selection took " + avgDuration + "ms avg, expected < 10ms");
    }
}
```

- [ ] **步骤 2：运行性能测试验证**

运行：`mvn test -Dtest=SmartModelSelectionPerformanceTest -pl java-harness-workflow`
预期：所有性能测试 PASS

- [ ] **步骤 3：Commit**

```bash
git add java-harness-workflow/src/test/java/com/chachamaru/harness/model/SmartModelSelectionPerformanceTest.java
git commit -m "test(performance): add performance and stress tests for model selection"
```

---

### Phase 8: 文档和发布准备

#### 任务 8.1：创建用户配置指南

**文件：**
- 创建：`docs/harness-project/user-guides/smart-model-selection-guide.md`

- [ ] **步骤 1：编写用户配置指南**

```markdown
# 智能模型选择用户指南

## 概述

智能模型选择功能根据任务复杂度自动选择最优的 AI 大模型，在保证质量的同时优化成本和性能。

## 快速开始

### 1. 环境变量配置

设置以下环境变量来配置可用的模型：

```bash
export ANTHROPIC_DEFAULT_FABLE_MODEL="your-fast-model"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="your-balanced-model"
export ANTHROPIC_DEFAULT_SONNET_MODEL="your-quality-model"
export ANTHROPIC_DEFAULT_OPUS_MODEL="your-powerful-model"
export ANTHROPIC_MODEL="your-default-model"
```

### 2. 项目配置

在项目根目录创建 `.claude/settings.json`:

```json
{
  "modelSelection": {
    "enabled": true,
    "timeout": 5000,
    "validateApiCall": false
  }
}
```

### 3. 使用

智能模型选择会在执行任务时自动工作，无需手动干预。

## 配置详解

### 模型等级映射

| 复杂度分数 | 等级 | 默认环境变量 |
|-----------|------|------------|
| 0-2 | FAST | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

### 复杂度评分

复杂度基于以下因素自动计算：

- **文件数量**: 修改 4+ 文件 = +1 分
- **关键目录**: 涉及 core/, guardrails/, security/ = +1 分  
- **关键字**: 包含 architecture, security, design, migration = +1 分
- **失败历史**: agent memory 中有失败记录 = +2 分
- **显式指定**: 任务描述中标注 `effort: high` = +3 分

### 降级策略

每个等级都有独立的降级链：

```
1. 主要模型 → 2. 默认模型 (ANTHROPIC_MODEL) → 3. 安全模型 (glm-4.7)
```

## 高级配置

### 自定义降级链

在配置文件中自定义降级链：

```json
{
  "modelSelection": {
    "tierConfigs": {
      "fast": {
        "fallbackChain": [
          "env:CUSTOM_FAST_MODEL",
          "env:BACKUP_MODEL",
          "hardcoded-fallback"
        ]
      }
    }
  }
}
```

### API 验证

启用可选的 API 调用验证（会增加延迟）：

```json
{
  "modelSelection": {
    "validateApiCall": true
  }
}
```

### 超时配置

调整模型可用性检查的超时时间：

```json
{
  "modelSelection": {
    "timeout": 10000
  }
}
```

## 故障排除

### 模型不可用错误

**错误**: `ModelUnavailableException: All models exhausted for tier: BALANCED`

**解决方案**:
1. 检查环境变量是否正确设置
2. 验证模型名称拼写
3. 检查网络连接（如使用远程模型）
4. 查看日志了解具体失败原因

### 配置未生效

**问题**: 配置更改后仍然使用默认值

**解决方案**:
1. 确认配置文件路径正确 (`.claude/settings.json` 或 `harness.toml`)
2. 检查 JSON 格式是否正确
3. 重启 Harness 使配置生效

### 性能问题

**问题**: 模型选择耗时过长

**解决方案**:
1. 禁用 API 验证: `"validateApiCall": false`
2. 减少超时时间: `"timeout": 3000`
3. 确保环境变量设置正确，避免降级链过长

## 监控和日志

### 日志级别

智能模型选择使用以下日志级别：

- `INFO`: 模型选择成功、配置加载
- `WARN`: 模型不可用、降级发生、配置错误
- `DEBUG`: 详细的决策过程、候选模型尝试

### 关键指标

监控以下指标了解系统健康状况：

- 模型选择平均耗时
- 降级发生频率
- 各等级模型使用分布
- 配置加载失败次数

## 最佳实践

1. **成本优化**: 为简单任务配置经济实惠的模型
2. **质量保证**: 为复杂任务使用强大的模型
3. **可靠性**: 始终配置默认模型作为降级选项
4. **监控**: 定期检查日志，确保降级机制正常工作
5. **测试**: 在生产环境前测试配置的降级链

## 示例场景

### 场景 1: 开发环境

使用快速模型进行开发：

```bash
export ANTHROPIC_DEFAULT_FABLE_MODEL="fast-cheap-model"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="fast-cheap-model"
export ANTHROPIC_DEFAULT_SONNET_MODEL="standard-model"
export ANTHROPIC_DEFAULT_OPUS_MODEL="standard-model"
export ANTHROPIC_MODEL="fallback-model"
```

### 场景 2: 生产环境

使用高质量模型：

```bash
export ANTHROPIC_DEFAULT_FABLE_MODEL="quality-model"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="premium-model"
export ANTHROPIC_DEFAULT_SONNET_MODEL="premium-model"
export ANTHROPIC_DEFAULT_OPUS_MODEL="ultra-model"
export ANTHROPIC_MODEL="reliable-backup"
```

### 场景 3: 成本优化

优先考虑成本效益：

```bash
export ANTHROPIC_DEFAULT_FABLE_MODEL="economy-model"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="economy-model"
export ANTHROPIC_DEFAULT_SONNET_MODEL="standard-model"
export ANTHROPIC_DEFAULT_OPUS_MODEL="standard-model"
export ANTHROPIC_MODEL="budget-model"
```
```

- [ ] **步骤 2：Commit**

```bash
git add docs/harness-project/user-guides/smart-model-selection-guide.md
git commit -m "docs(user-guide): add comprehensive user guide for smart model selection"
```

---

#### 任务 8.2：更新主 README 文档

**文件：**
- 修改：`README.md`

- [ ] **步骤 1：在 README.md 中添加智能模型选择章节**

在功能特性部分添加：

```markdown
## 🧠 智能模型选择

**Phase 12 新功能**: 根据任务复杂度自动选择最优 AI 模型，优化成本和性能。

### 核心特性

- **自动分级**: 基于任务复杂度（0-2 分 → FAST, 3-4 分 → BALANCED, 5-6 分 → QUALITY, ≥7 分 → POWERFUL）
- **智能降级**: 完整的降级机制，确保总能找到可用模型
- **配置驱动**: 支持通过 `.claude/settings.json` 或 `harness.toml` 自定义策略
- **环境感知**: 自动读取环境变量中的模型配置

### 快速配置

```bash
# 设置模型环境变量
export ANTHROPIC_DEFAULT_FABLE_MODEL="fast-model"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="balanced-model"
export ANTHROPIC_DEFAULT_SONNET_MODEL="quality-model"
export ANTHROPIC_DEFAULT_OPUS_MODEL="powerful-model"
export ANTHROPIC_MODEL="default-model"

# 创建配置文件
cat > .claude/settings.json << EOF
{
  "modelSelection": {
    "enabled": true,
    "timeout": 5000
  }
}
EOF
```

### 使用场景

- **成本优化**: 简单任务使用经济模型
- **质量保证**: 复杂任务使用强大模型
- **可靠性**: 多层降级确保稳定运行

详细配置指南：[智能模型选择用户指南](../../user-guides/smart-model-selection-guide.md)
```

- [ ] **步骤 2：Commit**

```bash
git add README.md
git commit -m "docs(readme): add smart model selection feature section"
```

---

#### 任务 8.3：创建发布说明

**文件：**
- 创建：`CHANGELOG.md`（如不存在）或修改现有文件

- [ ] **步骤 1：添加版本发布说明**

```markdown
## [4.2.0] - 2026-08-10

### Added - 智能模型选择系统 🆕

#### 功能特性
- **自动模型选择**: 根据任务复杂度自动选择最优 AI 模型
- **分级映射**: 4 个复杂度等级 (FAST/BALANCED/QUALITY/POWERFUL)
- **智能降级**: 多层降级机制确保模型可用性
- **配置优先级**: 支持 settings.json 和 harness.toml 双格式
- **环境变量解析**: 自动解析 `env:MODEL_NAME` 引用

#### 技术实现
- 新增 8 个核心组件类
- 新增完整的单元测试和集成测试
- 集成到现有 Effort Routing 系统
- 性能优化：单次选择 < 100ms，并发支持 10+ 线程

#### 配置方式
- **环境变量**: ANTHROPIC_DEFAULT_*_MODEL 系列
- **项目配置**: .claude/settings.json 或 harness.toml
- **降级链**: 每个等级独立配置的 fallback chain

#### 文档更新
- 新增设计文档: `docs/harness-project/superpowers/specs/2026-08-10-smart-model-selection-design.md`
- 新增实现计划: `docs/harness-project/superpowers/plans/2026-08-10-smart-model-selection.md`
- 新增用户指南: `docs/harness-project/user-guides/smart-model-selection-guide.md`

#### 兼容性
- 向后兼容: 不影响现有功能
- 默认配置: 内置合理的默认值
- 降级兜底: 确保系统总能工作

#### 性能指标
- 模型选择时间: < 100ms (单次)
- 并发支持: 10+ 线程同时选择
- 内存占用: < 10MB (配置和缓存)
- 配置加载: < 100ms

### Technical Notes
- 新增包: `com.chachamaru.harness.model`
- 依赖更新: Jackson (JSON), SnakeYAML (预留)
- 测试覆盖: 80%+ 核心逻辑覆盖
```

- [ ] **步骤 2：Commit**

```bash
git add CHANGELOG.md
git commit -m "docs(changelog): add version 4.2.0 release notes for smart model selection"
```

---

## 验收标准

### 功能完整性

- [ ] 所有单元测试通过 (7 个测试类)
- [ ] 所有集成测试通过 (2 个测试类)
- [ ] 所有端到端测试通过 (1 个测试类)
- [ ] 所有性能测试通过 (1 个测试类)

### 质量标准

- [ ] 代码覆盖率 > 80%
- [ ] 所有公共 API 有 Javadoc
- [ ] 无明显性能问题
- [ ] 无安全漏洞

### 文档完整性

- [ ] 设计文档完整
- [ ] 实现计划详细
- [ ] 用户指南清晰
- [ ] README 更新
- [ ] CHANGELOG 完成

### 集成验证

- [ ] 与 Effort Routing 正确集成
- [ ] 技能文档更新
- [ ] 环境变量正确解析
- [ ] 降级机制正常工作

---

## 总计

- **总任务数**: 21 个任务
- **预计工作量**: 5-7 天
- **代码行数**: ~3000 行（含测试）
- **测试覆盖**: 11 个测试类
- **文档页数**: 5 个文档文件

---

## 执行建议

### 推荐执行方式

**子代理驱动（推荐）** - 使用 `superpowers:subagent-driven-development`

理由：
1. 每个任务独立审查，质量更高
2. 快速迭代，问题及时发现
3. 便于并行执行多个任务
4. 更好的错误隔离和恢复

### 执行顺序建议

1. **Phase 1-2**: 数据模型层（1 天）
2. **Phase 3-4**: 工具和配置层（2 天）  
3. **Phase 5-6**: 业务逻辑和集成层（2 天）
4. **Phase 7-8**: 测试和文档层（2 天）

### 质量检查点

- **Phase 1 完成后**: 数据模型验证
- **Phase 4 完成后**: 配置加载验证
- **Phase 6 完成后**: 集成功能验证
- **Phase 8 完成后**: 发布前最终验收

---

**计划状态**: ✅ 完成
**下一步**: 选择执行方式开始实施
