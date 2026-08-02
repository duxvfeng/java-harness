#!/bin/bash
set -e

echo "==================================="
echo "CI Test Automation"
echo "==================================="

TEST_TYPE="${1:-all}"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

TEST_REPORT="$REPORT_DIR/ci-test-$(date +%Y%m%d-%H%M%S).txt"

echo "Running CI tests: $TEST_TYPE"
echo "Test report: $TEST_REPORT"

TEST_START=$(date +%s)
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

{
    echo "CI Test Report"
    echo "=============="
    echo "Test Type: $TEST_TYPE"
    echo "Start Time: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Git Branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo ""

    case "$TEST_TYPE" in
        "unit")
            echo "## Unit Tests"
            echo "============="
            echo "Running unit tests only..."
            if mvn test -Dtest="*Test" -q; then
                echo "✅ Unit tests passed"
                TESTS_PASSED=1
            else
                echo "❌ Unit tests failed"
                TESTS_FAILED=1
            fi
            TESTS_RUN=1
            ;;

        "integration")
            echo "## Integration Tests"
            echo "===================="
            echo "Running integration tests only..."
            if mvn test -Dtest="*IntegrationTest" -q; then
                echo "✅ Integration tests passed"
                TESTS_PASSED=1
            else
                echo "❌ Integration tests failed"
                TESTS_FAILED=1
            fi
            TESTS_RUN=1
            ;;

        "e2e")
            echo "## End-to-End Tests"
            echo "===================="
            echo "Running E2E tests only..."
            if mvn test -Dtest="*E2ETest" -q; then
                echo "✅ E2E tests passed"
                TESTS_PASSED=1
            else
                echo "❌ E2E tests failed"
                TESTS_FAILED=1
            fi
            TESTS_RUN=1
            ;;

        "all")
            echo "## All Tests"
            echo "============="
            echo "Running all tests..."

            # Run tests and capture results
            if mvn test 2>&1 | tee "$REPORT_DIR/test-output.log"; then
                TESTS_PASSED=$(grep -o "Tests run: [0-9]*" "$REPORT_DIR/test-output.log" | grep -o "[0-9]*" | head -1 || echo "0")
                TESTS_FAILED=0
                echo "✅ All tests passed"
            else
                TESTS_PASSED=$(grep -o "Failures: [0-9]*" "$REPORT_DIR/test-output.log" | grep -o "[0-9]*" | head -1 || echo "0")
                TESTS_FAILED=$(grep -o "Errors: [0-9]*" "$REPORT_DIR/test-output.log" | grep -o "[0-9]*" | head -1 || echo "0")
                echo "❌ Some tests failed"
            fi
            TESTS_RUN=1
            ;;

        "coverage")
            echo "## Test Coverage"
            echo "================"
            echo "Running tests with coverage analysis..."
            if mvn test jacoco:report -q; then
                echo "✅ Tests completed with coverage"
                echo ""
                echo "Coverage Summary:"
                find . -name "jacoco.csv" -exec tail -1 {} \; 2>/dev/null || echo "Coverage report not found"
                TESTS_PASSED=1
            else
                echo "❌ Tests or coverage failed"
                TESTS_FAILED=1
            fi
            TESTS_RUN=1
            ;;

        *)
            echo "Error: Unknown test type: $TEST_TYPE"
            echo "Available types: unit, integration, e2e, all, coverage"
            exit 1
            ;;
    esac

    TEST_END=$(date +%s)
    TEST_DURATION=$((TEST_END - TEST_START))
    TEST_MINUTES=$((TEST_DURATION / 60))
    TEST_SECONDS=$((TEST_DURATION % 60))

    echo ""
    echo "## Test Summary"
    echo "==============="
    echo "Tests Run: $TESTS_RUN"
    echo "Tests Passed: $TESTS_PASSED"
    echo "Tests Failed: $TESTS_FAILED"
    echo "Tests Skipped: $TESTS_SKIPPED"
    echo "Test Duration: ${TEST_MINUTES}m ${TEST_SECONDS}s"
    echo "End Time: $(date '+%Y-%m-%d %H:%M:%S')"

    if [ "$TESTS_FAILED" -eq 0 ] && [ "$TESTS_RUN" -gt 0 ]; then
        echo ""
        echo "✅ CI tests completed successfully"
    else
        echo ""
        echo "❌ CI tests failed"
    fi

} > "$TEST_REPORT" 2>&1

echo "✅ Test run completed"
echo "Report saved to: $TEST_REPORT"

# Display summary
echo ""
grep "Tests Run:" "$TEST_REPORT"
grep "Tests Passed:" "$TEST_REPORT"
grep "Tests Failed:" "$TEST_REPORT"

if [ "$TESTS_FAILED" -gt 0 ]; then
    exit 1
fi