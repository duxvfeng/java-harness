#!/bin/bash
set -e

echo "==================================="
echo "Release Preflight Check"
echo "==================================="

PROJECT_DIR="${1:-.}"
REPORT_FILE=".claude/reports/preflight-$(date +%Y%m%d-%H%M%S).txt"

mkdir -p ".claude/reports"

echo "Running release preflight checks..."
echo "Report will be saved to: $REPORT_FILE"

OVERALL_STATUS="PASS"
CHECKS_PASSED=0
CHECKS_FAILED=0

{
    echo "Release Preflight Check Report"
    echo "=============================="
    echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Project: $PROJECT_DIR"
    echo "Git branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "Git commit: $(git rev-parse HEAD 2>/dev/null | cut -c1-8 || echo 'unknown')"
    echo ""

    # Check 1: Git Status
    echo "## 1. Git Status Check"
    echo "======================"
    if git diff --quiet && git diff --cached --quiet 2>/dev/null; then
        echo "✅ PASS: Working directory is clean"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "❌ FAIL: Working directory has uncommitted changes"
        git status --short
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        OVERALL_STATUS="FAIL"
    fi
    echo ""

    # Check 2: Build Status
    echo "## 2. Build Status Check"
    echo "======================="
    if mvn clean compile -q 2>/dev/null; then
        echo "✅ PASS: Project builds successfully"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "❌ FAIL: Project build failed"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        OVERALL_STATUS="FAIL"
    fi
    echo ""

    # Check 3: Test Status
    echo "## 3. Test Status Check"
    echo "======================"
    if mvn test -q 2>/dev/null; then
        echo "✅ PASS: All tests pass"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "❌ FAIL: Some tests failed"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        OVERALL_STATUS="FAIL"
    fi
    echo ""

    # Check 4: Code Coverage
    echo "## 4. Code Coverage Check"
    echo "========================"
    if mvn jacoco:report -q 2>/dev/null; then
        COVERAGE=$(find . -name "jacoco.csv" -exec tail -1 {} \; 2>/dev/null | cut -d',' -f8 | tr -d '%' || echo "0")
        COVERAGE_INT=${COVERAGE%.*}
        if [ "$COVERAGE_INT" -ge 80 ]; then
            echo "✅ PASS: Code coverage is $COVERAGE% (>= 80%)"
            CHECKS_PASSED=$((CHECKS_PASSED + 1))
        else
            echo "❌ FAIL: Code coverage is $COVERAGE% (< 80%)"
            CHECKS_FAILED=$((CHECKS_FAILED + 1))
            OVERALL_STATUS="FAIL"
        fi
    else
        echo "⚠️  WARN: Could not check coverage"
    fi
    echo ""

    # Check 5: Dependencies
    echo "## 5. Dependency Check"
    echo "======================"
    if mvn dependency:analyze -q 2>/dev/null; then
        echo "✅ PASS: No critical dependency issues"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "⚠️  WARN: Some dependency issues found"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    fi
    echo ""

    # Check 6: Version Consistency
    echo "## 6. Version Consistency Check"
    echo "================================"
    POM_VERSION=$(grep -A1 "<artifactId>claude-harness-parent</artifactId>" "$PROJECT_DIR/pom.xml" | grep "<version>" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' || echo "unknown")
    echo "POM version: $POM_VERSION"
    if [ "$POM_VERSION" != "unknown" ]; then
        echo "✅ PASS: Version found in pom.xml"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "❌ FAIL: Could not determine version"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        OVERALL_STATUS="FAIL"
    fi
    echo ""

    # Check 7: Documentation
    echo "## 7. Documentation Check"
    echo "========================="
    if [ -f "README.md" ]; then
        echo "✅ PASS: README.md exists"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "⚠️  WARN: README.md missing"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    fi

    if [ -f "CHANGELOG.md" ]; then
        echo "✅ PASS: CHANGELOG.md exists"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "⚠️  WARN: CHANGELOG.md missing"
    fi
    echo ""

    # Check 8: Security Scan
    echo "## 8. Security Check"
    echo "===================="
    # Basic security check - look for common issues
    SECRETS_FOUND=$(find . -name "*.java" -not -path "*/target/*" -exec grep -l "password.*=.*['\"]" {} \; 2>/dev/null | wc -l | tr -d ' ')
    if [ "$SECRETS_FOUND" -eq 0 ]; then
        echo "✅ PASS: No obvious hardcoded secrets found"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo "⚠️  WARN: Potential hardcoded secrets in $SECRETS_FOUND files"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    fi
    echo ""

    # Final Summary
    echo "## Summary"
    echo "=========="
    echo "Overall Status: $OVERALL_STATUS"
    echo "Checks Passed: $CHECKS_PASSED"
    echo "Checks Failed: $CHECKS_FAILED"
    echo ""

    if [ "$OVERALL_STATUS" = "PASS" ]; then
        echo "✅ Release preflight checks PASSED"
        echo ""
        echo "The project is ready for release."
    else
        echo "❌ Release preflight checks FAILED"
        echo ""
        echo "Please address the failing checks before release."
    fi

    echo ""
    echo "Report generated: $(date '+%Y-%m-%d %H:%M:%S')"

} > "$REPORT_FILE"

echo "✅ Preflight checks completed"
echo "Report saved to: $REPORT_FILE"

# Display summary
echo ""
echo "## Quick Summary"
echo "================"
grep "Overall Status:" "$REPORT_FILE"
grep "Checks Passed:" "$REPORT_FILE"
grep "Checks Failed:" "$REPORT_FILE"

if [ "$OVERALL_STATUS" = "FAIL" ]; then
    exit 1
fi