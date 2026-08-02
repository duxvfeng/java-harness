#!/bin/bash
set -e

echo "==================================="
echo "Project Analyzer"
echo "==================================="

ANALYSIS_TYPE="${1:-structure}"
OUTPUT_FORMAT="${2:-text}"

case "$ANALYSIS_TYPE" in
    "structure")
        echo ""
        echo "## Project Structure Analysis"
        echo "============================"

        echo "Directory Structure:"
        echo "==================="
        find . -type d -not -path "*/.*" -not -path "*/target/*" -not -path "*/node_modules/*" | head -20

        echo ""
        echo "File Statistics:"
        echo "================"
        echo "Java files: $(find . -name "*.java" -not -path "*/target/*" | wc -l | tr -d ' ')"
        echo "Test files: $(find . -name "*Test.java" | wc -l | tr -d ' ')"
        echo "Configuration files: $(find . -name "*.xml" -o -name "*.json" -o -name "*.yml" -o -name "*.yaml" | wc -l | tr -d ' ')"
        echo "Markdown files: $(find . -name "*.md" | wc -l | tr -d ' ')"

        echo ""
        echo "Module Breakdown:"
        echo "================="
        find . -name "pom.xml" -not -path "*/target/*" | while read -r pom; do
            module_dir=$(dirname "$pom")
            module_name=$(basename "$module_dir")
            java_count=$(find "$module_dir" -name "*.java" | wc -l | tr -d ' ')
            echo "$module_name: $java_count Java files"
        done
        ;;

    "dependencies")
        echo ""
        echo "## Dependency Analysis"
        echo "====================="

        if [ -f "pom.xml" ]; then
            echo "Maven Dependencies:"
            echo "==================="
            mvn dependency:tree 2>/dev/null | grep -E "INFO.*+- " | head -20
        fi

        echo ""
        echo "External Libraries:"
        echo "=================="
        find . -name "*.jar" -not -path "*/target/*" | wc -l | tr -d ' '
        echo "JAR files found"
        ;;

    "complexity")
        echo ""
        echo "## Code Complexity Analysis"
        echo "=========================="

        echo "Complex Classes (>500 lines):"
        echo "============================"
        find . -name "*.java" -not -path "*/target/*" -exec wc -l {} \; | sort -rn | awk '$1 > 500 { print $0 }' | head -10

        echo ""
        echo "Deep Nesting Indicators:"
        echo "======================="
        find . -name "*.java" -not -path "*/target/*" | while read -r file; do
            max_indent=0
            while IFS= read -r line; do
                indent=${#line}
                if [ $indent -gt $max_indent ]; then
                    max_indent=$indent
                fi
            done < "$file"
            if [ $max_indent -gt 40 ]; then
                echo "$file: max indent $max_indent spaces"
            fi
        done | head -5
        ;;

    "duplicates")
        echo ""
        echo "## Code Duplication Analysis"
        echo "=========================="

        echo "Similar File Names (potential duplication):"
        echo "==========================================="
        find . -name "*.java" -not -path "*/target/*" -basename | sort | uniq -d

        echo ""
        echo "Potential Duplicate Methods:"
        echo "============================"
        # Simple heuristic: similar method names
        find . -name "*.java" -not -path "*/target/*" -exec grep -h "^    public.*(" {} \; | sort | uniq -c | sort -rn | head -10
        ;;

    "security")
        echo ""
        echo "## Security Analysis"
        echo "===================="

        echo "Hardcoded Secrets:"
        echo "==================="
        find . -name "*.java" -not -path "*/target/*" -exec grep -H -n "password.*=.*['\"]" {} \; 2>/dev/null | head -5

        echo ""
        echo "SQL Injection Risks:"
        echo "===================="
        find . -name "*.java" -not -path "*/target/*" -exec grep -H -n "Statement.*execute" {} \; 2>/dev/null | head -5

        echo ""
        echo "XSS Vulnerabilities:"
        echo "===================="
        find . -name "*.java" -not -path "*/target/*" -exec grep -H -n "request.*getParameter.*out" {} \; 2>/dev/null | head -5
        ;;

    "metrics")
        echo ""
        echo "## Project Metrics"
        echo "================="

        TOTAL_FILES=$(find . -name "*.java" -not -path "*/target/*" | wc -l | tr -d ' ')
        TOTAL_LINES=$(find . -name "*.java" -not -path "*/target/*" -exec cat {} \; | wc -l | tr -d ' ')
        TOTAL_TESTS=$(find . -name "*Test.java" | wc -l | tr -d ' ')

        echo "Total Java Files: $TOTAL_FILES"
        echo "Total Lines of Code: $TOTAL_LINES"
        echo "Total Test Files: $TOTAL_TESTS"

        if [ "$TOTAL_FILES" -gt 0 ]; then
            AVG_LINES=$((TOTAL_LINES / TOTAL_FILES))
            echo "Average Lines per File: $AVG_LINES"
            TEST_RATIO=$((TOTAL_TESTS * 100 / TOTAL_FILES))
            echo "Test Ratio: $TEST_RATIO%"
        fi

        echo ""
        echo "Lines per Module:"
        echo "================="
        find . -name "pom.xml" -not -path "*/target/*" | while read -r pom; do
            module_dir=$(dirname "$pom")
            module_lines=$(find "$module_dir" -name "*.java" -exec cat {} \; 2>/dev/null | wc -l | tr -d ' ')
            module_name=$(basename "$module_dir")
            echo "$module_name: $module_lines lines"
        done
        ;;

    *)
        echo ""
        echo "Project Analyzer"
        echo "Usage: ./project-analyzer.sh <analysis_type> [format]"
        echo ""
        echo "Analysis types:"
        echo "  structure    - Analyze project structure"
        echo "  dependencies - Analyze project dependencies"
        echo "  complexity   - Analyze code complexity"
        echo "  duplicates   - Find code duplication"
        echo "  security     - Security vulnerability scan"
        echo "  metrics      - Project metrics and statistics"
        echo ""
        exit 1
        ;;
esac