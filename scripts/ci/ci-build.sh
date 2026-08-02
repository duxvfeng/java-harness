#!/bin/bash
set -e

echo "==================================="
echo "CI Build Automation"
echo "==================================="

BUILD_MODE="${1:-standard}"
BUILD_NUMBER="${2:-$(date +%Y%m%d-%H%M%S)}"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

BUILD_LOG="$REPORT_DIR/ci-build-$BUILD_NUMBER.log"

echo "Starting CI build: $BUILD_NUMBER"
echo "Build mode: $BUILD_MODE"
echo "Build log: $BUILD_LOG"

BUILD_START=$(date +%s)
BUILD_STATUS="SUCCESS"

{
    echo "CI Build Log"
    echo "============"
    echo "Build Number: $BUILD_NUMBER"
    echo "Build Mode: $BUILD_MODE"
    echo "Start Time: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Git Branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "Git Commit: $(git rev-parse HEAD 2>/dev/null | cut -c1-8 || echo 'unknown')"
    echo ""

    case "$BUILD_MODE" in
        "quick")
            echo "## Quick Build"
            echo "=============="
            echo "Skipping tests, compile only..."
            if mvn clean compile -q; then
                echo "✅ Quick build completed successfully"
            else
                echo "❌ Quick build failed"
                BUILD_STATUS="FAILURE"
            fi
            ;;

        "standard")
            echo "## Standard Build"
            echo "================="
            echo "Full build with tests..."

            echo "Step 1: Clean and compile..."
            if mvn clean compile -q; then
                echo "✅ Compilation successful"
            else
                echo "❌ Compilation failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 2: Run tests..."
            if mvn test -q; then
                echo "✅ Tests passed"
            else
                echo "❌ Tests failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 3: Package..."
            if mvn package -DskipTests -q; then
                echo "✅ Package successful"
            else
                echo "❌ Package failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "✅ Standard build completed successfully"
            ;;

        "full")
            echo "## Full Build"
            echo "============="
            echo "Complete build with all checks..."

            echo "Step 1: Clean and compile..."
            if mvn clean compile; then
                echo "✅ Compilation successful"
            else
                echo "❌ Compilation failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 2: Run tests with coverage..."
            if mvn test jacoco:report; then
                echo "✅ Tests passed with coverage"
            else
                echo "❌ Tests failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 3: Package..."
            if mvn package; then
                echo "✅ Package successful"
            else
                echo "❌ Package failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 4: Verify..."
            if mvn verify -q; then
                echo "✅ Verification successful"
            else
                echo "❌ Verification failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "Step 5: Install..."
            if mvn install -DskipTests -q; then
                echo "✅ Install successful"
            else
                echo "❌ Install failed"
                BUILD_STATUS="FAILURE"
                exit 1
            fi

            echo "✅ Full build completed successfully"
            ;;

        *)
            echo "Error: Unknown build mode: $BUILD_MODE"
            echo "Available modes: quick, standard, full"
            BUILD_STATUS="FAILURE"
            exit 1
            ;;
    esac

    BUILD_END=$(date +%s)
    BUILD_DURATION=$((BUILD_END - BUILD_START))
    BUILD_MINUTES=$((BUILD_DURATION / 60))
    BUILD_SECONDS=$((BUILD_DURATION % 60))

    echo ""
    echo "## Build Summary"
    echo "================"
    echo "Build Status: $BUILD_STATUS"
    echo "Build Duration: ${BUILD_MINUTES}m ${BUILD_SECONDS}s"
    echo "End Time: $(date '+%Y-%m-%d %H:%M:%S')"

    if [ "$BUILD_STATUS" = "SUCCESS" ]; then
        echo ""
        echo "✅ CI build completed successfully"
        echo ""
        echo "Artifacts created:"
        find . -name "*.jar" -not -path "*/target/*" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -5
    else
        echo ""
        echo "❌ CI build failed"
        echo "Check the log for details"
    fi

} > "$BUILD_LOG" 2>&1

echo "✅ Build completed"
echo "Status: $BUILD_STATUS"
echo "Log file: $BUILD_LOG"

# Display quick summary
echo ""
grep "Build Status:" "$BUILD_LOG"
grep "Build Duration:" "$BUILD_LOG"

if [ "$BUILD_STATUS" = "FAILURE" ]; then
    exit 1
fi