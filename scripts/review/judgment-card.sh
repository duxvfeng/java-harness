#!/bin/bash
set -e

echo "==================================="
echo "Judgment Card Generator"
echo "==================================="

REVIEW_DIR=".claude/reviews"
mkdir -p "$REVIEW_DIR"

REVIEW_TYPE="${1:-code}"
TARGET="${2:-.}"
REVIEW_ID="$(date +%Y%m%d-%H%M%S)-$REVIEW_TYPE"
REVIEW_FILE="$REVIEW_DIR/judgment-$REVIEW_ID.md"

echo ""
echo "Creating judgment card..."
echo "Type: $REVIEW_TYPE"
echo "Target: $TARGET"
echo "Review ID: $REVIEW_ID"

case "$REVIEW_TYPE" in
    "code")
        cat > "$REVIEW_FILE" << EOF
# Code Review Judgment Card

**Review ID**: $REVIEW_ID
**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Reviewer**: $(git config user.name 2>/dev/null || echo 'Unknown')
**Target**: $TARGET

## Review Summary

**Overall Status**: 🔍 PENDING

### Quick Assessment
- [x] Code correctness
- [x] Code style and formatting
- [x] Documentation completeness
- [x] Test coverage
- [x] Security considerations
- [x] Performance implications

## Detailed Findings

### ✅ Strengths
<!-- List positive aspects of the code -->

### ⚠️ Issues Found
<!-- List issues found during review -->

#### Critical Issues
<!-- Issues that must be fixed -->

#### Major Issues
<!-- Issues that should be fixed -->

#### Minor Issues
<!-- Nice-to-have fixes -->

### 🔍 Specific Code Analysis
<!-- Detailed analysis of specific code sections -->

## Recommendations

### Must Fix (Blocking)
1. <!-- Critical issues that block merge -->

### Should Fix (Important)
1. <!-- Important issues that don't block merge -->

### Consider (Optional)
1. <!-- Suggestions for improvement -->

## Approval Decision

**Status**: 🤔 PENDING REVIEW

**Comments**:
<!-- Overall review comments -->

## Next Steps

- [ ] Address critical issues
- [ ] Address major issues
- [ ] Consider minor issues
- [ ] Re-review if needed
- [ ] Approve or request changes

---
*Review completed: $(date '+%Y-%m-%d %H:%M:%S')*
EOF
        ;;

    "architecture")
        cat > "$REVIEW_FILE" << EOF
# Architecture Review Judgment Card

**Review ID**: $REVIEW_ID
**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Reviewer**: $(git config user.name 2>/dev/null || echo 'Unknown')
**Target**: $TARGET

## Architecture Assessment

**Overall Status**: 🔍 PENDING

### Design Principles Evaluation
- [x] Separation of concerns
- [x] Modularity
- [x] Scalability
- [x] Maintainability
- [x] Testability
- [x] Security

### Architecture Patterns
<!-- Evaluation of architectural patterns used -->

### Technology Choices
<!-- Assessment of technology decisions -->

## Findings

### ✅ Strengths
<!-- Architectural strengths -->

### ⚠️ Concerns
<!-- Architectural concerns or issues -->

### 🔧 Recommendations
<!-- Architectural improvements -->

## Approval Decision

**Status**: 🤔 PENDING REVIEW

**Comments**:
<!-- Overall architecture review comments -->

---
*Architecture review completed: $(date '+%Y-%m-%d %H:%M:%S')*
EOF
        ;;

    "security")
        cat > "$REVIEW_FILE" << EOF
# Security Review Judgment Card

**Review ID**: $REVIEW_ID
**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Reviewer**: $(git config user.name 2>/dev/null || echo 'Unknown')
**Target**: $TARGET

## Security Assessment

**Overall Status**: 🔍 PENDING

### Security Categories
- [x] Input validation
- [x] Authentication/authorization
- [x] Data protection
- [x] Error handling
- [x] Logging/auditing
- [x] Dependencies

### Vulnerability Assessment
<!-- Assessment of potential vulnerabilities -->

### Compliance Check
<!-- Compliance with security standards -->

## Findings

### 🚨 Critical Security Issues
<!-- Critical security vulnerabilities -->

### ⚠️ Security Concerns
<!-- Security concerns that should be addressed -->

### ✅ Security Strengths
<!-- Good security practices found -->

## Recommendations

### Must Fix (Critical)
1. <!-- Critical security fixes -->

### Should Fix (Important)
1. <!-- Important security improvements -->

### Best Practices
1. <!-- Security best practice recommendations -->

## Approval Decision

**Status**: 🤔 PENDING REVIEW

**Risk Level**: TBD

**Comments**:
<!-- Overall security review comments -->

---
*Security review completed: $(date '+%Y-%m-%d %H:%M:%S')*
EOF
        ;;

    "performance")
        cat > "$REVIEW_FILE" << EOF
# Performance Review Judgment Card

**Review ID**: $REVIEW_ID
**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Reviewer**: $(git config user.name 2>/dev/null || echo 'Unknown')
**Target**: $TARGET

## Performance Assessment

**Overall Status**: 🔍 PENDING

### Performance Metrics
- [x] Response time
- [x] Throughput
- [x] Resource utilization
- [x] Scalability
- [x] Memory usage
- [x] Database efficiency

### Performance Analysis
<!-- Detailed performance analysis -->

### Bottleneck Identification
<!-- Identified performance bottlenecks -->

## Findings

### ✅ Performance Strengths
<!-- Good performance characteristics -->

### ⚠️ Performance Issues
<!-- Performance concerns -->

### 🚀 Optimization Opportunities
<!-- Performance improvement suggestions -->

## Recommendations

### Critical Optimizations
1. <!-- Must-have performance fixes -->

### Recommended Optimizations
1. <!-- Performance improvements -->

### Future Considerations
1. <!-- Long-term performance considerations -->

## Approval Decision

**Status**: 🤔 PENDING REVIEW

**Performance Impact**: TBD

**Comments**:
<!-- Overall performance review comments -->

---
*Performance review completed: $(date '+%Y-%m-%d %H:%M:%S')*
EOF
        ;;

    *)
        echo "Error: Unknown review type: $REVIEW_TYPE"
        echo "Available types: code, architecture, security, performance"
        rm -f "$REVIEW_FILE"
        exit 1
        ;;
esac

echo ""
echo "✅ Judgment card created: $REVIEW_FILE"
echo ""
echo "Next steps:"
echo "  1. Review the code/architecture/security/performance"
echo "  2. Fill in the judgment card findings"
echo "  3. Update approval decision"
echo "  4. Share with team for discussion"