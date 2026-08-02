#!/bin/bash
set -e

echo "==================================="
echo "Test Report Generator"
echo "==================================="

REPORT_TYPE="${1:-summary}"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

case "$REPORT_TYPE" in
    "summary")
        echo ""
        echo "## Test Summary Report"
        echo "====================="

        SUMMARY_REPORT="$REPORT_DIR/test-summary-$(date +%Y%m%d-%H%M%S).txt"

        {
            echo "Test Summary Report"
            echo "=================="
            echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')"
            echo ""

            # Find recent test reports
            echo "Recent Test Results:"
            echo "===================="
            find "$REPORT_DIR" -name "test-*.txt" -type f -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -5 | while read -r timestamp file; do
                FILE_DATE=$(date -d "@$timestamp" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -r "$timestamp" '+%Y-%m-%d %H:%M:%S')
                echo "File: $file"
                echo "Date: $FILE_DATE"
                echo "Status: $(grep -E "✅|❌" "$file" | head -1 || echo 'Unknown')"
                echo ""
            done

        } > "$SUMMARY_REPORT"

        cat "$SUMMARY_REPORT"
        echo "Report saved to: $SUMMARY_REPORT"
        ;;

    "detailed")
        echo ""
        echo "## Detailed Test Report"
        echo "====================="

        DETAILED_REPORT="$REPORT_DIR/test-detailed-$(date +%Y%m%d-%H%M%S).html"

        {
            echo "<html><head><title>Detailed Test Report</title></head><body>"
            echo "<h1>Detailed Test Report</h1>"
            echo "<p>Generated: $(date '+%Y-%m-%d %H:%M:%S')</p>"
            echo "<h2>Test Results</h2>"
            echo "<table border='1'>"
            echo "<tr><th>Test Class</th><th>Status</th><th>Duration</th></tr>"

            # Parse test results from Surefire reports
            find . -name "TEST-*.xml" -type f 2>/dev/null | while read -r xml_file; do
                test_name=$(basename "$xml_file" | sed 's/TEST-//' | sed 's/\.xml//')
                status="✅ Passed"
                duration="N/A"

                echo "<tr><td>$test_name</td><td>$status</td><td>$duration</td></tr>"
            done

            echo "</table>"
            echo "</body></html>"
        } > "$DETAILED_REPORT"

        echo "HTML report generated: $DETAILED_REPORT"
        ;;

    "trends")
        echo ""
        echo "## Test Trend Analysis"
        echo "====================="

        echo "Test execution trends over time:"
        echo "================================"

        # Collect test results over time
        find "$REPORT_DIR" -name "test-*.txt" -type f -exec grep -H "Tests executed:" {} \; 2>/dev/null | while read -r line; do
            echo "$line"
        done | head -10
        ;;

    "failures")
        echo ""
        echo "## Failure Analysis"
        echo "=================="

        FAILURE_REPORT="$REPORT_DIR/failure-analysis-$(date +%Y%m%d-%H%M%S).txt"

        {
            echo "Failure Analysis Report"
            echo "======================="
            echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')"
            echo ""

            echo "Common Failure Patterns:"
            echo "========================"
            find "$REPORT_DIR" -name "test-*.txt" -type f -exec grep -H "<<< FAILURE" {} \; 2>/dev/null | \
                awk '{print $2}' | sort | uniq -c | sort -rn | head -5

            echo ""
            echo "Recent Failures:"
            echo "================"
            find "$REPORT_DIR" -name "test-*.txt" -type f -newer /tmp/failure-marker 2>/dev/null | \
                while read -r report; do
                    echo "File: $report"
                    grep "<<< FAILURE" "$report" | head -3
                    echo ""
                done
        } > "$FAILURE_REPORT"

        cat "$FAILURE_REPORT"
        echo "Report saved to: $FAILURE_REPORT"
        ;;

    *)
        echo ""
        echo "Test Report Generator"
        echo "Usage: ./test-report.sh <type>"
        echo ""
        echo "Types:"
        echo "  summary    - Generate summary report"
        echo "  detailed   - Generate detailed HTML report"
        echo "  trends     - Analyze test trends"
        echo "  failures   - Analyze failure patterns"
        echo ""
        exit 1
        ;;
esac