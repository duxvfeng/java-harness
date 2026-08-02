#!/bin/bash
set -e

echo "==================================="
echo "Automated Test Runner"
echo "==================================="

TEST_MODE="${1:-continuous}"
TEST_PATTERN="${2:-*}"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

echo "Test mode: $TEST_MODE"
echo "Test pattern: $TEST_PATTERN"

case "$TEST_MODE" in
    "once")
        echo ""
        echo "Running single test execution..."
        echo "=============================="

        TEST_REPORT="$REPORT_DIR/test-run-$(date +%Y%m%d-%H%M%S).txt"

        {
            echo "Test Execution Report"
            echo "====================="
            echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "Pattern: $TEST_PATTERN"
            echo ""

            echo "Running tests..."
            if mvn test -Dtest="$TEST_PATTERN" 2>&1 | tee "$TEST_REPORT"; then
                echo ""
                echo "✅ Tests completed successfully"

                # Extract test results
                TESTS_RUN=$(grep -o "Tests run: [0-9]*" "$TEST_REPORT" 2>/dev/null | grep -o "[0-9]*" | head -1 || echo "N/A")
                echo "Tests executed: $TESTS_RUN"
            else
                echo ""
                echo "❌ Tests failed"

                # Extract failure details
                FAILURES=$(grep -o "Failures: [0-9]*" "$TEST_REPORT" 2>/dev/null | grep -o "[0-9]*" | head -1 || echo "N/A")
                ERRORS=$(grep -o "Errors: [0-9]*" "$TEST_REPORT" 2>/dev/null | grep -o "[0-9]*" | head -1 || echo "N/A")
                echo "Failures: $FAILURES"
                echo "Errors: $ERRORS"
            fi

        } 2>&1

        echo "Report saved to: $TEST_REPORT"
        ;;

    "continuous")
        echo ""
        echo "Continuous test monitoring"
        echo "=========================="
        echo "Watching for test failures..."
        echo "Press Ctrl+C to stop"
        echo ""

        LAST_TEST_HASH=""
        TEST_COUNT=0
        FAILURE_COUNT=0

        while true; do
            # Check if source files changed
            CURRENT_HASH=$(find src -name "*.java" -type f -exec md5sum {} \; 2>/dev/null | md5sum | cut -d' ' -f1)

            if [ "$CURRENT_HASH" != "$LAST_TEST_HASH" ]; then
                echo "[$(date '+%H:%M:%S')] Source files changed, running tests..."
                TEST_COUNT=$((TEST_COUNT + 1))

                if mvn test -Dtest="$TEST_PATTERN" -q 2>/dev/null; then
                    echo "✅ Tests passed ($TEST_COUNT test runs, $FAILURE_COUNT failures)"
                else
                    echo "❌ Tests failed ($TEST_COUNT test runs, $FAILURE_COUNT failures)"
                    FAILURE_COUNT=$((FAILURE_COUNT + 1))

                    # Create failure report
                    FAILURE_REPORT="$REPORT_DIR/test-failure-$(date +%Y%m%d-%H%M%S).txt"
                    {
                        echo "Test Failure Report"
                        echo "==================="
                        echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
                        echo "Pattern: $TEST_PATTERN"
                        echo ""
                        echo "Failed tests:"
                        mvn test -Dtest="$TEST_PATTERN" 2>&1 | grep -A3 "<<< FAILURE"
                    } > "$FAILURE_REPORT"
                    echo "Failure report: $FAILURE_REPORT"
                fi

                LAST_TEST_HASH="$CURRENT_HASH"
            fi

            sleep 5
        done
        ;;

    "watch")
        echo ""
        echo "Test file watcher"
        echo "================"
        echo "Watching for test file changes..."
        echo "Press Ctrl+C to stop"
        echo ""

        while true; do
            # Find and run changed test files
            find src/test -name "*Test.java" -newer /tmp/test-marker 2>/dev/null | while read -r test_file; do
                test_class=$(basename "$test_file" .java)
                echo "[$(date '+%H:%M:%S')] Running test: $test_class"

                if mvn test -Dtest="$test_class" -q 2>/dev/null; then
                    echo "✅ $test_class passed"
                else
                    echo "❌ $test_class failed"
                fi
            done

            # Update marker
            touch /tmp/test-marker
            sleep 3
        done
        ;;

    "coverage")
        echo ""
        echo "Test coverage analysis"
        echo "======================"

        COVERAGE_REPORT="$REPORT_DIR/coverage-$(date +%Y%m%d-%H%M%S).txt"

        {
            echo "Coverage Analysis Report"
            echo "======================="
            echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
            echo ""

            echo "Running tests with coverage..."
            if mvn test jacoco:report -q 2>/dev/null; then
                echo "✅ Coverage report generated"
                echo ""

                # Find and display coverage data
                JACOCO_CSV=$(find . -name "jacoco.csv" | head -1)
                if [ -n "$JACOCO_CSV" ]; then
                    echo "Coverage Summary:"
                    echo "================="
                    tail -1 "$JACOCO_CSV" | awk -F',' '{
                        printf "Instruction Coverage: %.1f%%\n", $7*100
                        printf "Branch Coverage: %.1f%%\n", $9*100
                        printf "Line Coverage: %.1f%%\n", $5*100
                        printf "Method Coverage: %.1f%%\n", $4*100
                        printf "Class Coverage: %.1f%%\n", $2*100
                    }'
                fi
            else
                echo "❌ Coverage generation failed"
            fi

        } > "$COVERAGE_REPORT"

        cat "$COVERAGE_REPORT"
        echo ""
        echo "Report saved to: $COVERAGE_REPORT"
        ;;

    "failed")
        echo ""
        echo "Running failed tests only"
        echo "=========================="

        # Find last test report
        LAST_REPORT=$(find "$REPORT_DIR" -name "test-*.txt" -type f -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -1 | cut -d' ' -f2-)

        if [ -n "$LAST_REPORT" ]; then
            FAILED_TESTS=$(grep "<<< FAILURE" "$LAST_REPORT" | awk '{print $1}' | sed 's/\[.*\]//' | sort -u)

            if [ -n "$FAILED_TESTS" ]; then
                echo "Re-running failed tests:"
                echo "$FAILED_TESTS"
                echo ""

                for test in $FAILED_TESTS; do
                    echo "Running: $test"
                    if mvn test -Dtest="$test" -q 2>/dev/null; then
                        echo "✅ $test passed"
                    else
                        echo "❌ $test still failing"
                    fi
                done
            else
                echo "No failed tests found in last report"
            fi
        else
            echo "No test reports found"
        fi
        ;;

    *)
        echo ""
        echo "Automated Test Runner"
        echo "Usage: ./auto-test-runner.sh <mode> [pattern]"
        echo ""
        echo "Modes:"
        echo "  once           - Run tests once and report"
        echo "  continuous     - Watch for changes and re-run tests"
        echo "  watch          - Watch for test file changes"
        echo "  coverage       - Generate coverage report"
        echo "  failed         - Re-run failed tests only"
        echo ""
        echo "Pattern: Test pattern (default: *)"
        echo ""
        exit 1
        ;;
esac