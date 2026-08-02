#!/bin/bash
set -e

echo "==================================="
echo "Code Quality Analysis"
echo "==================================="

TARGET_DIR="${1:-.}"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

REPORT_FILE="$REPORT_DIR/quality-report-$(date +%Y%m%d-%H%M%S).txt"

echo "Analyzing code quality in: $TARGET_DIR"
echo "Report will be saved to: $REPORT_FILE"

{
    echo "Code Quality Analysis Report"
    echo "============================"
    echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Target: $TARGET_DIR"
    echo ""

    # Java file statistics
    echo "## Java File Statistics"
    echo "======================"
    JAVA_FILES=$(find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" 2>/dev/null | wc -l | tr -d ' ')
    echo "Total Java files: $JAVA_FILES"
    echo ""

    # Lines of code
    echo "## Lines of Code"
    echo "================"
    LINES_OF_CODE=$(find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec cat {} \; 2>/dev/null | wc -l | tr -d ' ')
    echo "Total lines of code: $LINES_OF_CODE"
    echo ""

    # Code complexity indicators
    echo "## Code Complexity Analysis"
    echo "=========================="

    # Find long methods (>100 lines)
    echo "Long methods (>100 lines):"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec awk '
    /^{/ { brace_count++; line_start = NR }
    /^}/ { brace_count--; if (brace_count == 0 && line_start > 0) {
            length = NR - line_start
            if (length > 100) {
                print FILENAME ":" line_start "-" NR " (" length " lines)"
            }
            line_start = 0
        }
    ' {} \; 2>/dev/null | head -10 || echo "No long methods found"
    echo ""

    # Find large classes (>500 lines)
    echo "Large classes (>500 lines):"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec wc -l {} \; 2>/dev/null | sort -rn | awk '$1 > 500 { print $0 }' | head -10 || echo "No large classes found"
    echo ""

    # Code duplication indicators
    echo "## Potential Code Duplication"
    echo "============================"
    echo "Files with similar names (possible duplication):"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -basename 2>/dev/null | sort | uniq -d | head -10 || echo "No duplication detected"
    echo ""

    # Test coverage indicators
    echo "## Test Coverage Indicators"
    echo "=========================="
    TEST_FILES=$(find "$TARGET_DIR" -name "*Test.java" -o -name "*Tests.java" 2>/dev/null | wc -l | tr -d ' ')
    echo "Test files: $TEST_FILES"

    if [ "$JAVA_FILES" -gt 0 ]; then
        TEST_RATIO=$((TEST_FILES * 100 / JAVA_FILES))
        echo "Test to code ratio: $TEST_RATIO%"
    fi

    echo "Test classes:"
    find "$TARGET_DIR" -name "*Test.java" -o -name "*Tests.java" 2>/dev/null | head -10
    echo ""

    # Code smell detection
    echo "## Code Smell Detection"
    echo "======================"

    # Empty catch blocks
    echo "Empty catch blocks:"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec grep -l "catch.*{}" {} \; 2>/dev/null | head -5 || echo "No empty catch blocks found"
    echo ""

    # Long parameter lists
    echo "Potential long parameter lists (>7 parameters):"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec awk '/\(.*,.{500,}\)/ {print FILENAME ":" NR}' {} \; 2>/dev/null | head -5 || echo "No long parameter lists found"
    echo ""

    # Deep nesting indicators
    echo "Deep nesting indicators (>4 levels):"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec awk '{if (indent > 4) print FILENAME ":" NR} /^[[:space:]]*/ {indent = length(/^[[:space:]]*/)/4} END' {} \; 2>/dev/null | head -5 || echo "No deep nesting found"
    echo ""

    # Security indicators
    echo "## Security Indicators"
    echo "====================="

    # TODO comments
    echo "TODO/FIXME comments:"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec grep -H -n "TODO\|FIXME" {} \; 2>/dev/null | head -10 || echo "No TODO/FIXME comments found"
    echo ""

    # Hardcoded passwords
    echo "Potential hardcoded secrets:"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec grep -H -n "password.*=\|secret.*=\|api.*key.*=" {} \; 2>/dev/null | head -5 || echo "No hardcoded secrets detected"
    echo ""

    # Documentation indicators
    echo "## Documentation Coverage"
    echo "========================"

    # Files without Javadoc
    echo "Files without package-level Javadoc:"
    find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -not -name "package-info.java" | head -10 | while read file; do
        if ! grep -q "/\*\*" "$file" 2>/dev/null; then
            echo "$file"
        fi
    done | head -5 || echo "All files have Javadoc"
    echo ""

    # Overall quality score estimation
    echo "## Quality Score Estimation"
    echo "=========================="

    SCORE=100
    ISSUES=0

    # Deduct for long methods
    LONG_METHODS=$(find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec awk '/^{/ { brace_count++; line_start = NR } /^}/ { brace_count--; if (brace_count == 0 && line_start > 0) { length = NR - line_start; if (length > 100) print FILENAME } line_start = 0 }' {} \; 2>/dev/null | wc -l | tr -d ' ')
    if [ "$LONG_METHODS" -gt 0 ]; then
        SCORE=$((SCORE - LONG_METHODS * 2))
        ISSUES=$((ISSUES + LONG_METHODS))
    fi

    # Deduct for low test coverage
    if [ "$TEST_RATIO" -lt 30 ]; then
        SCORE=$((SCORE - 10))
        ISSUES=$((ISSUES + 1))
    fi

    # Deduct for empty catch blocks
    EMPTY_CATCHES=$(find "$TARGET_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec grep -l "catch.*{}" {} \; 2>/dev/null | wc -l | tr -d ' ')
    if [ "$EMPTY_CATCHES" -gt 0 ]; then
        SCORE=$((SCORE - EMPTY_CATCHES))
        ISSUES=$((ISSUES + EMPTY_CATCHES))
    fi

    # Ensure score doesn't go below 0
    [ $SCORE -lt 0 ] && SCORE=0

    echo "Estimated Quality Score: $SCORE/100"
    echo "Total Issues Found: $ISSUES"
    echo ""

    # Quality categorization
    echo "Quality Category: "
    if [ $SCORE -ge 80 ]; then
        echo "✅ Excellent (80-100)"
    elif [ $SCORE -ge 60 ]; then
        echo "⚠️  Good (60-79)"
    elif [ $SCORE -ge 40 ]; then
        echo "⚠️  Fair (40-59)"
    else
        echo "❌ Poor (0-39)"
    fi

    echo ""
    echo "## Recommendations"
    echo "=================="

    if [ "$LONG_METHODS" -gt 0 ]; then
        echo "- Refactor $LONG_METHODS long methods (>100 lines)"
    fi

    if [ "$TEST_RATIO" -lt 30 ]; then
        echo "- Improve test coverage (currently $TEST_RATIO%)"
    fi

    if [ "$EMPTY_CATCHES" -gt 0 ]; then
        echo "- Add proper exception handling in $EMPTY_CATCHES empty catch blocks"
    fi

    echo ""
    echo "Report generated: $(date '+%Y-%m-%d %H:%M:%S')"

} > "$REPORT_FILE"

echo "✅ Quality analysis completed"
echo "Report saved to: $REPORT_FILE"

# Display summary
echo ""
echo "## Quick Summary"
echo "================"
grep "Estimated Quality Score" "$REPORT_FILE"
grep "Total Issues Found" "$REPORT_FILE"
grep "Quality Category" "$REPORT_FILE"