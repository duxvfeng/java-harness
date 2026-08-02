#!/bin/bash
set -e

echo "==================================="
echo "Analyzing Project Structure"
echo "==================================="

PROJECT_DIR="${1:-.}"

if [ ! -d "$PROJECT_DIR" ]; then
    echo "Error: Project directory does not exist: $PROJECT_DIR"
    exit 1
fi

cd "$PROJECT_DIR"

echo ""
echo "Analyzing project: $PROJECT_DIR"
echo "Analysis date: $(date +%Y-%m-%d)"

# Detect project type
echo ""
echo "## Project Type Detection"
if [ -f "pom.xml" ]; then
    PROJECT_TYPE="maven"
    echo "✅ Maven project detected"
    echo "   pom.xml found"
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    PROJECT_TYPE="gradle"
    echo "✅ Gradle project detected"
    if [ -f "build.gradle" ]; then
        echo "   build.gradle found"
    fi
    if [ -f "build.gradle.kts" ]; then
        echo "   build.gradle.kts found (Kotlin DSL)"
    fi
else
    PROJECT_TYPE="generic"
    echo "⚠️  No standard build system detected"
fi

# Check Java version
echo ""
echo "## Java Version"
if [ -f "pom.xml" ]; then
    JAVA_VERSION=$(grep -A2 "<maven.compiler.source>" pom.xml | grep -v "<maven.compiler.source>" | grep -o '<[^>]*>' | tr -d '<>' | head -1)
    if [ -z "$JAVA_VERSION" ]; then
        JAVA_VERSION=$(grep -o 'java.version>[^<]*<' pom.xml | cut -d'>' -f2 | cut -d'<' -f1 | head -1)
    fi
    echo "Java version: ${JAVA_VERSION:-unknown}"
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    GRADLE_FILE=$([ -f "build.gradle" ] && echo "build.gradle" || echo "build.gradle.kts")
    JAVA_VERSION=$(grep -o 'sourceCompatibility.*=.*[0-9]' "$GRADLE_FILE" | grep -o '[0-9]*' | head -1)
    echo "Java version: ${JAVA_VERSION:-unknown}"
fi

# Analyze project structure
echo ""
echo "## Project Structure"
echo "Source directories:"
for dir in src/main/java src/test/java src/main/resources src/test/resources; do
    if [ -d "$dir" ]; then
        FILE_COUNT=$(find "$dir" -type f | wc -l | tr -d ' ')
        echo "  ✅ $dir ($FILE_COUNT files)"
    else
        echo "  ❌ $dir (missing)"
    fi
done

# Check for Java Harness integration
echo ""
echo "## Java Harness Integration"
if [ -d ".claude" ]; then
    echo "✅ Java Harness directory exists"
    echo "   .claude configuration found"

    if [ -f ".claude/settings.json" ]; then
        echo "  ✅ settings.json exists"
    else
        echo "  ❌ settings.json missing"
    fi

    if [ -f "Plans.md" ]; then
        echo "  ✅ Plans.md exists"
    else
        echo "  ❌ Plans.md missing"
    fi
else
    echo "❌ Java Harness not integrated"
    echo "   Run setup-existing-project.sh to integrate"
fi

# Analyze dependencies
echo ""
echo "## Dependencies Analysis"
if [ "$PROJECT_TYPE" = "maven" ]; then
    echo "Analyzing Maven dependencies..."
    if mvn dependency:list -q 2>/dev/null | grep -q "INFO"; then
        DEP_COUNT=$(mvn dependency:list -q 2>/dev/null | grep "^\[INFO\]" | grep -v "BUILD" | wc -l | tr -d ' ')
        echo "✅ Found $DEP_COUNT dependencies"

        echo ""
        echo "Top 5 dependencies:"
        mvn dependency:list -q 2>/dev/null | grep "^\[INFO\]" | grep -v "BUILD" | head -5
    else
        echo "⚠️  Could not analyze dependencies (Maven not available or build errors)"
    fi
elif [ "$PROJECT_TYPE" = "gradle" ]; then
    echo "Analyzing Gradle dependencies..."
    if [ -f "./gradlew" ]; then
        echo "✅ Gradle wrapper available"
        ./gradlew dependencies 2>/dev/null | head -20 || echo "Could not analyze dependencies"
    else
        echo "⚠️  Gradle wrapper not found"
    fi
fi

# Code statistics
echo ""
echo "## Code Statistics"
JAVA_FILES=$(find . -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" | wc -l | tr -d ' ')
echo "Java files: $JAVA_FILES"

if [ "$JAVA_FILES" -gt 0 ]; then
    LINES_OF_CODE=$(find . -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec cat {} \; | wc -l | tr -d ' ')
    echo "Lines of code: $LINES_OF_CODE"

    echo ""
    echo "Largest files:"
    find . -name "*.java" -not -path "*/target/*" -not -path "*/.git/*" -exec wc -l {} \; | sort -rn | head -5
fi

# Test analysis
echo ""
echo "## Test Analysis"
TEST_FILES=$(find . -path "*/test/*" -name "*Test.java" -o -name "*Tests.java" | wc -l | tr -d ' ')
echo "Test files: $TEST_FILES"

if [ "$TEST_FILES" -gt 0 ]; then
    echo "✅ Test files found"

    # Check test frameworks
    if [ "$PROJECT_TYPE" = "maven" ]; then
        if grep -q "junit-jupiter" pom.xml || grep -q "junit" pom.xml; then
            echo "  ✅ JUnit detected"
        fi
        if grep -q "testng" pom.xml; then
            echo "  ✅ TestNG detected"
        fi
        if grep -q "mockito" pom.xml; then
            echo "  ✅ Mockito detected"
        fi
    fi
else
    echo "⚠️  No test files found"
fi

# Build configuration
echo ""
echo "## Build Configuration"
if [ "$PROJECT_TYPE" = "maven" ]; then
    echo "✅ Maven build system"
    if grep -q "<plugin>" pom.xml; then
        PLUGIN_COUNT=$(grep -c "<plugin>" pom.xml)
        echo "  Build plugins: $PLUGIN_COUNT"
    fi
elif [ "$PROJECT_TYPE" = "gradle" ]; then
    echo "✅ Gradle build system"
fi

# Issues and recommendations
echo ""
echo "## Analysis Summary & Recommendations"

ISSUES=0
RECOMMENDATIONS=0

if [ ! -d ".claude" ]; then
    echo "⚠️  Consider integrating Java Harness for better project management"
    RECOMMENDATIONS=$((RECOMMENDATIONS + 1))
fi

if [ "$TEST_FILES" -eq 0 ]; then
    echo "⚠️  No test files found - consider adding tests"
    ISSUES=$((ISSUES + 1))
fi

if [ ! -f ".gitignore" ]; then
    echo "⚠️  .gitignore missing - recommended for version control"
    ISSUES=$((ISSUES + 1))
fi

if [ "$PROJECT_TYPE" = "maven" ] && [ ! -f "mvnw" ]; then
    echo "ℹ️  Consider adding Maven wrapper for consistent builds"
    RECOMMENDATIONS=$((RECOMMENDATIONS + 1))
fi

echo ""
echo "Issues found: $ISSUES"
echo "Recommendations: $RECOMMENDATIONS"

# Generate analysis report
echo ""
echo "## Analysis Report"
REPORT_FILE="project-analysis-$(date +%Y%m%d-%H%M%S).md"
cat > "$REPORT_FILE" << EOF
# Project Analysis Report

**Date**: $(date +%Y-%m-%d)
**Project**: $PROJECT_DIR
**Type**: $PROJECT_TYPE

## Summary

- Project Type: $PROJECT_TYPE
- Java Files: $JAVA_FILES
- Test Files: $TEST_FILES
- Lines of Code: ${LINES_OF_CODE:-0}

## Structure

$(find . -type d -not -path "*/.*" -not -path "*/target/*" -not -path "*/node_modules/*" | head -20)

## Dependencies

See full dependency list in the console output above.

## Next Steps

1. Review the analysis results
2. Address any issues found
3. Consider implementing recommendations
4. Set up Java Harness if not already configured

---

This report was auto-generated by analyze-project.sh
EOF

echo "✅ Analysis report saved to: $REPORT_FILE"

echo ""
echo "✅ Project analysis completed successfully!"
echo ""
echo "Summary:"
echo "  - Project type: $PROJECT_TYPE"
echo "  - Java files: $JAVA_FILES"
echo "  - Test files: $TEST_FILES"
echo "  - Issues found: $ISSUES"
echo "  - Recommendations: $RECOMMENDATIONS"
echo ""
echo "Review the analysis report: $REPORT_FILE"