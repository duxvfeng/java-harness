#!/bin/bash
set -e

echo "==================================="
echo "Review Summary Generator"
echo "==================================="

REVIEW_DIR=".claude/reviews"
REPORT_DIR=".claude/reports"
mkdir -p "$REPORT_DIR"

case "${1:-summary}" in
    "summary")
        echo ""
        echo "## Review Summary"
        echo "================="

        if [ ! -d "$REVIEW_DIR" ]; then
            echo "No reviews directory found"
            exit 0
        fi

        REVIEW_COUNT=$(find "$REVIEW_DIR" -name "judgment-*.md" | wc -l | tr -d ' ')

        if [ "$REVIEW_COUNT" -eq 0 ]; then
            echo "No reviews found"
            exit 0
        fi

        echo "Total reviews: $REVIEW_COUNT"
        echo ""

        # Count by status
        echo "### Review Status Distribution"
        echo "============================="
        APPROVED=$(grep -l "✅ APPROVED" "$REVIEW_DIR"/judgment-*.md 2>/dev/null | wc -l | tr -d ' ')
        PENDING=$(grep -l "🤔 PENDING" "$REVIEW_DIR"/judgment-*.md 2>/dev/null | wc -l | tr -d ' ')
        REJECTED=$(grep -l "❌ REJECTED" "$REVIEW_DIR"/judgment-*.md 2>/dev/null | wc -l | tr -d ' ')

        echo "✅ Approved: $APPROVED"
        echo "🤔 Pending: $PENDING"
        echo "❌ Rejected: $REJECTED"
        echo ""

        # Count by type
        echo "### Review Type Distribution"
        echo "==========================="
        CODE_REVIEWS=$(find "$REVIEW_DIR" -name "*code*.md" | wc -l | tr -d ' ')
        ARCH_REVIEWS=$(find "$REVIEW_DIR" -name "*architecture*.md" | wc -l | tr -d ' ')
        SEC_REVIEWS=$(find "$REVIEW_DIR" -name "*security*.md" | wc -l | tr -d ' ')
        PERF_REVIEWS=$(find "$REVIEW_DIR" -name "*performance*.md" | wc -l | tr -d ' ')

        echo "Code reviews: $CODE_REVIEWS"
        echo "Architecture reviews: $ARCH_REVIEWS"
        echo "Security reviews: $SEC_REVIEWS"
        echo "Performance reviews: $PERF_REVIEWS"
        echo ""

        # Recent reviews
        echo "### Recent Reviews"
        echo "=================="
        find "$REVIEW_DIR" -name "judgment-*.md" -printf "%T+ %p\n" 2>/dev/null | sort -rn | head -5 | while read -r line; do
            FILE=$(echo "$line" | cut -d' ' -f2-)
            DATE=$(echo "$line" | cut -d' ' -f1)
            basename "$FILE"
            echo "  Date: $DATE"
            echo "  Status: $(grep -E "Status.*[🤔✅❌]" "$FILE" | head -1 || echo 'Unknown')"
            echo ""
        done
        ;;

    "generate")
        echo ""
        echo "Generating comprehensive review report..."

        REPORT_FILE="$REPORT_DIR/review-summary-$(date +%Y%m%d-%H%M%S).md"

        {
            echo "# Review Summary Report"
            echo ""
            echo "**Generated**: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "**Reviewer**: $(git config user.name 2>/dev/null || echo 'Unknown')"
            echo ""

            # Overall statistics
            echo "## Overall Statistics"
            echo "===================="
            ./review-summary.sh summary | tail -n +3
            echo ""

            # Critical findings
            echo "## Critical Findings"
            echo "===================="
            find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -l "🚨.*Critical" {} \; 2>/dev/null | while read -r file; do
                echo "### $(basename "$file")"
                echo "Date: $(stat -f "%Sm" -t "%Y-%m-%d" "$file" 2>/dev/null || stat -c "%y" "$file" 2>/dev/null | cut -d'.' -f1)"
                grep -A5 "🚨.*Critical" "$file" || echo "No critical issues"
                echo ""
            done

            # Approval recommendations
            echo "## Approval Recommendations"
            echo "=========================="
            find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -l "🤔 PENDING" {} \; 2>/dev/null | while read -r file; do
                echo "- $(basename "$file")"
                echo "  Recommendation: $(grep "Recommendations" -A10 "$file" | head -5 || echo 'See full review')"
            done

            echo ""
            echo "---"
            echo "*End of review summary*"

        } > "$REPORT_FILE"

        echo "✅ Review report generated: $REPORT_FILE"
        ;;

    "aggregate")
        echo ""
        echo "Aggregating review metrics..."

        # Aggregate issues by category
        echo "### Issues by Category"
        echo "======================"

        CRITICAL_COUNT=$(find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -c "🚨.*Critical" {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
        MAJOR_COUNT=$(find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -c "⚠️.*Major" {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
        MINOR_COUNT=$(find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -c "ℹ️.*Minor" {} \; 2>/dev/null | awk '{s+=$1} END {print s}')

        echo "Critical issues: ${CRITICAL_COUNT:-0}"
        echo "Major issues: ${MAJOR_COUNT:-0}"
        echo "Minor issues: ${MINOR_COUNT:-0}"
        echo ""

        # Average approval time
        echo "### Review Metrics"
        echo "=================="

        find "$REVIEW_DIR" -name "judgment-*.md" -exec grep -H "Date:" {} \; 2>/dev/null | wc -l | tr -d ' ' | xargs -I {} echo "Total reviews completed: {}"
        ;;

    "export")
        EXPORT_FILE="${2:-review-export-$(date +%Y%m%d-%H%M%S).json}"
        echo "Exporting reviews to: $EXPORT_FILE"

        if command -v jq &> /dev/null; then
            find "$REVIEW_DIR" -name "judgment-*.md" -exec cat {} \; | jq -R 'split("\n") | map(select(length > 0)) | {reviews: .}' > "$EXPORT_FILE" 2>/dev/null || echo "Export failed"
            echo "✅ Reviews exported"
        else
            echo "Error: jq not found. Creating text export..."
            find "$REVIEW_DIR" -name "judgment-*.md" -exec cat {} \; > "$EXPORT_FILE"
            echo "✅ Reviews exported (text format)"
        fi
        ;;

    *)
        echo ""
        echo "Review Summary Generator"
        echo "Usage: ./review-summary.sh <command> [args]"
        echo ""
        echo "Commands:"
        echo "  summary              - Show review summary (default)"
        echo "  generate             - Generate comprehensive report"
        echo "  aggregate            - Aggregate review metrics"
        echo "  export [file]        - Export reviews to file"
        echo ""
        ;;
esac