# Multilingual Code Standards Test Plan

## Overview

This document describes the comprehensive test plan for validating multilingual code standards support in the Java Harness code review system.

## Test Objectives

1. **Language Detection Accuracy**: Verify that programming languages are correctly detected from file extensions and content
2. **Standards Routing**: Confirm that appropriate standards are applied for each language
3. **Rule Application**: Validate that language-specific rules are correctly applied
4. **Integration Testing**: Test integration with external skills (Alibaba, brainstorming)
5. **Configuration Validation**: Ensure configuration file structure is valid and functional

## Test Environment Setup

### Prerequisites
- Java Harness installed and configured
- Code standards configuration file in place: `.claude/config/code-standards.config.json`
- Reference documents available in `skills/harness-review/references/code-standards/`
- Alibaba Java development guide skill available
- Brainstorming skill available

### Test Files Structure
```
tests/code-standards/
├── test-samples/
│   ├── java/
│   │   ├── GoodExample.java
│   │   ├── BadExample.java
│   │   └── SecurityIssue.java
│   ├── python/
│   │   ├── pep8_compliant.py
│   │   ├── pep8_violations.py
│   │   └── security_risks.py
│   ├── vue/
│   │   ├── ProperComponent.vue
│   │   ├── AntiPatternComponent.vue
│   │   └── SecurityIssue.vue
│   └── go/
│       ├── effective_go.go
│       ├── bad_practices.go
│       └── error_handling.go
├── validation-scripts/
│   ├── test-java-detection.sh
│   ├── test-python-detection.sh
│   ├── test-vue-detection.sh
│   └── test-go-detection.sh
└── expected-results/
    ├── java-findings.json
    ├── python-findings.json
    ├── vue-findings.json
    └── go-findings.json
```

## Test Cases

### TC-01: Java Language Detection
**Objective**: Verify Java files are correctly identified and Alibaba standards applied

**Test Steps**:
1. Create test Java file with `.java` extension
2. Run code review using `/harness-review`
3. Verify language detection output includes `java`
4. Confirm Alibaba Java standards are referenced
5. Check for appropriate rule application

**Expected Results**:
- Language detected as `java`
- Standards applied: `alibaba-java-development-guide`
- Severity level: `major` (configured default)
- Review scope: `full`

**Test Files**:
```java
// GoodExample.java - Should pass with minimal findings
package com.example;

public class GoodExample {
    private static final int MAX_RETRY = 3;
    
    public String getUserInfo(String userId) {
        // Proper implementation
        return userId;
    }
}

// BadExample.java - Should trigger multiple findings
package com.example;

public class badExample {
    String username;
    void getdata() {}
}
```

### TC-02: Python Language Detection
**Objective**: Verify Python files are correctly identified and PEP 8 standards applied

**Test Steps**:
1. Create test Python file with `.py` extension
2. Run code review using `/harness-review`
3. Verify language detection output includes `python`
4. Confirm PEP 8 standards are referenced
5. Check for appropriate rule application

**Expected Results**:
- Language detected as `python`
- Standards applied: `pep-8`, `python-best-practices`
- Severity level: `moderate` (configured default)
- Review scope: `full`

**Test Files**:
```python
# pep8_compliant.py - Should pass with minimal findings
"""
Module for demonstrating PEP 8 compliance.
"""

import os
from typing import List, Optional


class DataProcessor:
    """Class for processing data."""
    
    def __init__(self, name: str) -> None:
        """Initialize the processor."""
        self.name = name
    
    def process(self, items: List[str]) -> Optional[str]:
        """Process the items and return result."""
        if not items:
            return None
        return f"Processed {len(items)} items"


# pep8_violations.py - Should trigger multiple findings
import os
class badClass:
    def badMethod(self):
        x=1+2
        return x
```

### TC-03: Vue Language Detection
**Objective**: Verify Vue files are correctly identified and Vue Style Guide applied

**Test Steps**:
1. Create test Vue file with `.vue` extension
2. Run code review using `/harness-review`
3. Verify language detection output includes `vue`
4. Confirm Vue Style Guide standards are referenced
5. Check for appropriate rule application

**Expected Results**:
- Language detected as `vue`
- Standards applied: `vue-style-guide`
- Severity level: `moderate` (configured default)
- Review scope: `component`

**Test Files**:
```vue
<!-- ProperComponent.vue - Should pass with minimal findings -->
<template>
  <div class="user-card">
    <UserAvatar :user="user" />
    <UserInfo :user="user" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

interface Props {
  userId: string
}

const props = defineProps<Props>()
const user = ref(null)

const userName = computed(() => user.value?.name || '')
</script>

<style scoped>
.user-card {
  padding: 1rem;
}
</style>

<!-- AntiPatternComponent.vue - Should trigger multiple findings -->
<template>
  <div>
    <button v-for="item in items" v-if="item.active">
      {{ item.name }}
    </button>
  </div>
</template>
```

### TC-04: Go Language Detection
**Objective**: Verify Go files are correctly identified and Effective Go applied

**Test Steps**:
1. Create test Go file with `.go` extension
2. Run code review using `/harness-review`
3. Verify language detection output includes `go`
4. Confirm Effective Go standards are referenced
5. Check for appropriate rule application

**Expected Results**:
- Language detected as `go`
- Standards applied: `effective-go`, `go-code-review-comments`
- Severity level: `major` (configured default)
- Review scope: `full`

**Test Files**:
```go
// effective_go.go - Should pass with minimal findings
package user

import "context"

// GetUser retrieves a user from the database.
func GetUser(ctx context.Context, id string) (*User, error) {
    user, err := db.QueryUser(ctx, id)
    if err != nil {
        return nil, fmt.Errorf("getting user: %w", err)
    }
    return user, nil
}

// bad_practices.go - Should trigger multiple findings
package main

import "fmt"

func badFunction() {
    data, _ := ioutil.ReadFile("file.txt") // Ignoring error
    
    for i := 0; i < 10; i++ {
        go func() {
            fmt.Println(i) // Race condition
        }()
    }
}
```

### TC-05: Content-based Language Detection
**Objective**: Verify fallback content analysis for ambiguous files

**Test Steps**:
1. Create test files without standard extensions
2. Include language-specific patterns in content
3. Run code review
4. Verify correct language detection from content

**Expected Results**:
- Correct language identification from content patterns
- Appropriate standards applied based on detected language

### TC-06: Multi-language File Handling
**Objective**: Verify proper handling of files containing multiple languages

**Test Steps**:
1. Create Vue component with template, script, and style sections
2. Run code review
3. Verify each section is analyzed with appropriate standards

**Expected Results**:
- Template analyzed with HTML standards
- Script analyzed with JavaScript/TypeScript standards
- Style analyzed with CSS standards

### TC-07: Skill Integration Testing
**Objective**: Verify integration with external skills

**Test Steps**:
1. Test Java code review with Alibaba skill integration
2. Verify brainstorming integration in planning phase
3. Test skill failure fallback behavior

**Expected Results**:
- Alibaba skill correctly invoked for Java code
- Brainstorming skill available in planning context
- Graceful fallback when skills unavailable

### TC-08: Configuration Validation
**Objective**: Verify configuration file structure and functionality

**Test Steps**:
1. Parse configuration file JSON structure
2. Validate required fields and data types
3. Test language mapping resolution
4. Verify severity level mappings
5. Check rule category definitions

**Expected Results**:
- Configuration file valid JSON
- All required fields present
- Data types match schema
- Language mappings resolve correctly
- Severity levels properly defined

## Validation Scripts

### Language Detection Test
```bash
#!/bin/bash
# test-java-detection.sh

echo "Testing Java language detection..."

# Create test file
cat > /tmp/TestJava.java << 'EOF'
package test;
public class TestJava {
    public static void main(String[] args) {
        System.out.println("Test");
    }
}
EOF

# Run detection (pseudo-code)
detected=$(detect-language /tmp/TestJava.java)

if [ "$detected" = "java" ]; then
    echo "✓ Java detection successful"
else
    echo "✗ Java detection failed: got $detected"
fi
```

### Standards Application Test
```bash
#!/bin/bash
# test-standards-application.sh

echo "Testing standards application..."

# Test each language
for lang in java python vue go; do
    echo "Testing $lang standards..."
    
    # Get expected standards from config
    expected_standards=$(jq -r ".languageMapping.$lang.standards[]" .claude/config/code-standards.config.json)
    
    # Run review and capture applied standards (pseudo-code)
    applied_standards=$(get-applied-standards $lang)
    
    if [ "$expected_standards" = "$applied_standards" ]; then
        echo "✓ $lang standards correctly applied"
    else
        echo "✗ $lang standards mismatch: expected $expected_standards, got $applied_standards"
    fi
done
```

## Performance Testing

### Load Testing
- Test with multiple files of different languages simultaneously
- Verify performance with large codebases
- Monitor memory usage and processing time

### Accuracy Testing
- Measure language detection accuracy rate (target: >95%)
- Verify false positive rate for language detection (target: <5%)
- Test standards application accuracy (target: >90%)

## Integration Testing

### With Harness Review
```bash
# Test complete workflow
echo "Testing complete workflow..."

# Java code review
/harness-review tests/test-samples/java/GoodExample.java
/harness-review tests/test-samples/java/BadExample.java

# Python code review
/harness-review tests/test-samples/python/pep8_compliant.py
/harness-review tests/test-samples/python/pep8_violations.py

# Vue component review
/harness-review tests/test-samples/vue/ProperComponent.vue
/harness-review tests/test-samples/vue/AntiPatternComponent.vue

# Go code review
/harness-review tests/test-samples/go/effective_go.go
/harness-review tests/test-samples/go/bad_practices.go
```

### With Planning Skills
```bash
# Test brainstorming integration
/harness-plan create --with-brainstorming "Add user authentication feature"

# Verify brainstorming was invoked
# Verify creative options were generated
# Verify integration with planning process
```

## Test Results Documentation

### Results Format
```json
{
  "testRun": "TS-001",
  "timestamp": "2025-01-09T10:00:00Z",
  "environment": {
    "harnessVersion": "4.1.1",
    "configVersion": "1.0"
  },
  "results": {
    "TC-01": {
      "status": "pass",
      "duration": "2.3s",
      "findings": ["All validations passed"]
    },
    "TC-02": {
      "status": "pass", 
      "duration": "1.8s",
      "findings": ["All validations passed"]
    }
  },
  "summary": {
    "total": 10,
    "passed": 9,
    "failed": 1,
    "skipped": 0
  }
}
```

## Continuous Testing

### Automated Testing
- Integrate test suite into CI/CD pipeline
- Run tests on configuration changes
- Test new reference document additions
- Validate skill integration after updates

### Regression Testing
- Test for breaking changes when updating standards
- Verify backward compatibility
- Test migration between configuration versions

## Troubleshooting

### Common Issues
1. **Language Detection Failure**
   - Check file extensions are recognized
   - Verify content patterns are configured
   - Ensure config file paths are correct

2. **Standards Not Applied**
   - Verify skill integration is enabled
   - Check reference documents exist
   - Ensure configuration is valid JSON

3. **Performance Issues**
   - Check for large file handling
   - Verify caching is working
   - Monitor memory usage

## Success Criteria

### Functional Requirements
- ✅ All 4 languages (Java, Python, Vue, Go) correctly detected
- ✅ Appropriate standards applied for each language
- ✅ Severity levels correctly assigned
- ✅ External skills integration working
- ✅ Configuration file validation passing

### Non-Functional Requirements
- ✅ Language detection accuracy >95%
- ✅ Review processing time <10 seconds per file
- ✅ Memory usage within acceptable limits
- ✅ Error handling and fallback mechanisms working

### Integration Requirements
- ✅ Integration with harness-review skill working
- ✅ Integration with planning skills working
- ✅ External skill integration functional
- ✅ Configuration changes hot-reloadable

## Test Execution Schedule

### Initial Testing
- Day 1: Setup test environment and create test files
- Day 2: Execute language detection tests
- Day 3: Execute standards application tests
- Day 4: Integration testing
- Day 5: Performance testing and validation

### Ongoing Testing
- Weekly: Regression testing on configuration changes
- Monthly: Full test suite execution
- Quarterly: Performance benchmarking
- Annually: Comprehensive validation and updates

## Conclusion

This test plan provides comprehensive coverage for validating multilingual code standards support in the Java Harness code review system. Successful execution of these tests will ensure that:

1. Language detection works accurately across all supported languages
2. Appropriate standards are applied correctly
3. External skills integration functions properly
4. Configuration management is robust and reliable
5. Performance meets requirements for real-world usage

The test results will provide confidence in the multilingual code standards feature and identify any areas requiring improvement or additional development.
