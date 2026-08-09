#!/bin/bash
# Basic validation script for multilingual code standards support
# This script performs basic checks to verify the system is configured correctly

echo "=== Multilingual Code Standards Validation ==="
echo "Starting validation checks..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Function to check test result
check_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Test 1: Configuration file exists and is valid JSON
echo "Test 1: Validating configuration file..."
if [ -f ".claude/config/code-standards.config.json" ]; then
    if jq empty .claude/config/code-standards.config.json 2>/dev/null; then
        check_result 0 "Configuration file exists and is valid JSON"

        # Check version field
        VERSION=$(jq -r '.version' .claude/config/code-standards.config.json)
        echo "  Configuration version: $VERSION"

        # Check schema field
        SCHEMA=$(jq -r '.schema' .claude/config/code-standards.config.json)
        echo "  Schema: $SCHEMA"
    else
        check_result 1 "Configuration file is not valid JSON"
    fi
else
    check_result 1 "Configuration file not found"
fi

echo ""

# Test 2: Reference documents exist
echo "Test 2: Checking reference documents..."

REF_DOCS=(
    "skills/harness-review/references/code-standards/architecture.md"
    "skills/harness-review/references/code-standards/java-alibaba-guide.md"
    "skills/harness-review/references/code-standards/python-pep8.md"
    "skills/harness-review/references/code-standards/vue-style-guide.md"
    "skills/harness-review/references/code-standards/go-effective-go.md"
)

for doc in "${REF_DOCS[@]}"; do
    if [ -f "$doc" ]; then
        check_result 0 "Reference document exists: $doc"
    else
        check_result 1 "Reference document missing: $doc"
    fi
done

echo ""

# Test 3: Language mappings in configuration
echo "Test 3: Validating language mappings..."

LANGUAGES=("java" "python" "vue" "go")
for lang in "${LANGUAGES[@]}"; do
    LANG_CONFIG=$(jq -r ".languageMapping.$lang" .claude/config/code-standards.config.json)
    if [ "$LANG_CONFIG" != "null" ]; then
        check_result 0 "Language $lang is configured"

        # Check for required fields
        STANDARDS=$(jq -r ".languageMapping.$lang.standards" .claude/config/code-standards.config.json)
        EXTENSIONS=$(jq -r ".languageMapping.$lang.extensions" .claude/config/code-standards.config.json)
        SEVERITY=$(jq -r ".languageMapping.$lang.defaultSeverity" .claude/config/code-standards.config.json)

        echo "  Standards: $STANDARDS"
        echo "  Extensions: $EXTENSIONS"
        echo "  Severity: $SEVERITY"
    else
        check_result 1 "Language $lang is not configured"
    fi
done

echo ""

# Test 4: Skill integration configuration
echo "Test 4: Checking skill integration..."

JAVA_SKILL_INTEGRATION=$(jq -r '.languageMapping.java.skillIntegration' .claude/config/code-standards.config.json)
JAVA_SKILL_NAME=$(jq -r '.languageMapping.java.skillName' .claude/config/code-standards.config.json)

if [ "$JAVA_SKILL_INTEGRATION" = "true" ]; then
    check_result 0 "Java skill integration is enabled"
    echo "  Skill name: $JAVA_SKILL_NAME"
else
    check_result 1 "Java skill integration is not enabled"
fi

echo ""

# Test 5: Severity level mappings
echo "Test 5: Validating severity level mappings..."

SEVERITY_LEVELS=("critical" "major" "moderate" "minor" "info")
for severity in "${SEVERITY_LEVELS[@]}"; do
    SEVERITY_CONFIG=$(jq -r ".severityMapping.$severity" .claude/config/code-standards.config.json)
    if [ "$SEVERITY_CONFIG" != "null" ]; then
        check_result 0 "Severity level $severity is configured"

        LEVEL=$(jq -r ".severityMapping.$severity.level" .claude/config/code-standards.config.json)
        ICON=$(jq -r ".severityMapping.$severity.icon" .claude/config/code-standards.config.json)

        echo "  Level: $LEVEL, Icon: $ICON"
    else
        check_result 1 "Severity level $severity is not configured"
    fi
done

echo ""

# Test 6: Harness review skill modification
echo "Test 6: Checking harness-review skill integration..."

if grep -q "Multilingual Code Standards Integration" skills/harness-review/SKILL.md; then
    check_result 0 "harness-review skill contains multilingual integration section"
else
    check_result 1 "harness-review skill missing multilingual integration section"
fi

if grep -q "alibaba-java-development-guide" skills/harness-review/SKILL.md; then
    check_result 0 "harness-review skill references Alibaba Java guide"
else
    check_result 1 "harness-review skill missing Alibaba Java guide reference"
fi

echo ""

# Test 7: Harness plan skill modification
echo "Test 7: Checking harness-plan skill integration..."

if grep -q "Brainstorming 集成" skills/harness-plan/SKILL.md; then
    check_result 0 "harness-plan skill contains brainstorming integration"
else
    check_result 1 "harness-plan skill missing brainstorming integration"
fi

if grep -q "创意探索" skills/harness-plan/SKILL.md; then
    check_result 0 "harness-plan skill contains creative exploration section"
else
    check_result 1 "harness-plan skill missing creative exploration section"
fi

echo ""

# Test 8: Test plan exists
echo "Test 8: Checking test documentation..."

if [ -f "tests/code-standards/test-plan.md" ]; then
    check_result 0 "Test plan document exists"

    # Check test plan contains required test cases
    if grep -q "TC-01: Java Language Detection" tests/code-standards/test-plan.md; then
        check_result 0 "Test plan contains Java test cases"
    else
        check_result 1 "Test plan missing Java test cases"
    fi
else
    check_result 1 "Test plan document not found"
fi

echo ""

# Summary
echo "=== Validation Summary ==="
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All validation checks passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some validation checks failed${NC}"
    echo "Please review the failed checks above."
    exit 1
fi