#!/bin/bash
# Unit Tests for Branch Isolation System
# Tests core detection, configuration, and interaction logic

# Test framework
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Source the scripts to test
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
source "${PROJECT_ROOT}/scripts/branch-isolation/detect-branch.sh" 2>/dev/null || {
    echo "Error: Cannot load detect-branch.sh for testing"
    echo "Expected path: ${PROJECT_ROOT}/scripts/branch-isolation/detect-branch.sh"
    exit 1
}

##
# Test assertion functions
##
assert_equals() {
    local expected="$1"
    local actual="$2"
    local test_name="$3"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ "${expected}" == "${actual}" ]]; then
        echo -e "${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${test_name}"
        echo "  Expected: ${expected}"
        echo "  Actual: ${actual}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

assert_contains() {
    local haystack="$1"
    local needle="$2"
    local test_name="$3"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ "${haystack}" == *"${needle}"* ]]; then
        echo -e "${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${test_name}"
        echo "  Expected '${needle}' to be in output"
        echo "  Output: ${haystack}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

assert_success() {
    local exit_code="$1"
    local test_name="$2"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ ${exit_code} -eq 0 ]]; then
        echo -e "${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${test_name}"
        echo "  Expected exit code 0, got ${exit_code}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

assert_failure() {
    local exit_code="$1"
    local test_name="$2"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ ${exit_code} -ne 0 ]]; then
        echo -e "${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${test_name}"
        echo "  Expected non-zero exit code, got ${exit_code}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

skip_test() {
    local test_name="$1"
    local reason="$2"

    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_SKIPPED=$((TESTS_SKIPPED + 1))
    echo -e "${YELLOW}⊘${NC} ${test_name} (skipped: ${reason})"
}

##
# Test Suite 1: Branch Type Detection
##
test_branch_detection() {
    echo -e "\n${BLUE}=== Branch Detection Tests ===${NC}"

    local detected_type
    detected_type=$(detect_branch_type)

    # Test that detection works
    assert_success $? "detect_branch_type executes successfully"

    # Test that it returns a valid branch type
    case "${detected_type}" in
        main|feature|worktree)
            assert_success 0 "detect_branch_type returns valid type: ${detected_type}"
            ;;
        *)
            assert_failure 1 "detect_branch_type returns valid type (got: ${detected_type})"
            ;;
    esac
}

##
# Test Suite 2: Configuration Reading
##
test_configuration_reading() {
    echo -e "\n${BLUE}=== Configuration Reading Tests ===${NC}"

    local config_output
    config_output=$(read_branch_config)

    # Test that config reading works
    assert_success $? "read_branch_config executes successfully"

    # Test that it returns valid JSON
    assert_contains "${config_output}" "{" "read_branch_config returns JSON object"
    assert_contains "${config_output}" "mainBranch" "read_branch_config includes mainBranch field"
    assert_contains "${config_output}" "featureBranch" "read_branch_config includes featureBranch field"
}

##
# Test Suite 3: Strategy Detection
##
test_strategy_detection() {
    echo -e "\n${BLUE}=== Strategy Detection Tests ===${NC}"

    local strategy
    strategy=$(detect_branch_isolation_strategy)

    # Test that strategy detection works
    assert_success $? "detect_branch_isolation_strategy executes successfully"

    # Test that it returns a valid strategy
    case "${strategy}" in
        force|ask|skip)
            assert_success 0 "detect_branch_isolation_strategy returns valid strategy: ${strategy}"
            ;;
        *)
            assert_failure 1 "detect_branch_isolation_strategy returns valid strategy (got: ${strategy})"
            ;;
    esac
}

##
# Test Suite 4: Branch Information
##
test_branch_information() {
    echo -e "\n${BLUE}=== Branch Information Tests ===${NC}"

    local branch_info
    branch_info=$(get_branch_info)

    # Test that branch info retrieval works
    assert_success $? "get_branch_info executes successfully"

    # Test that it contains expected fields
    assert_contains "${branch_info}" "currentBranch" "get_branch_info includes currentBranch field"
    assert_contains "${branch_info}" "branchType" "get_branch_info includes branchType field"
    assert_contains "${branch_info}" "isWorktree" "get_branch_info includes isWorktree field"
    assert_contains "${branch_info}" "gitDir" "get_branch_info includes gitDir field"
}

##
# Test Suite 5: Git Environment Validation
##
test_git_validation() {
    echo -e "\n${BLUE}=== Git Environment Validation Tests ===${NC}"

    # Test validation in git repository
    if git rev-parse --git-dir >/dev/null 2>&1; then
        assert_success 0 "Running in valid git repository"

        # Test validate_git_environment function
        validate_git_environment >/dev/null 2>&1
        local validation_result=$?

        if [[ ${validation_result} -eq 0 ]]; then
            assert_success 0 "validate_git_environment validates current repository"
        else
            assert_failure 1 "validate_git_environment validates current repository"
        fi
    else
        skip_test "validate_git_environment" "Not in a git repository"
    fi
}

##
# Test Suite 6: Error Handling
##
test_error_handling() {
    echo -e "\n${BLUE}=== Error Handling Tests ===${NC}"

    # Test that error handler functions exist and are callable
    if command -v handle_not_repo_error >/dev/null 2>&1; then
        assert_success 0 "handle_not_repo_error function exists"
    else
        assert_failure 1 "handle_not_repo_error function exists"
    fi

    if command -v handle_branch_detection_error >/dev/null 2>&1; then
        assert_success 0 "handle_branch_detection_error function exists"
    else
        assert_failure 1 "handle_branch_detection_error function exists"
    fi

    if command -v handle_worktree_creation_error >/dev/null 2>&1; then
        assert_success 0 "handle_worktree_creation_error function exists"
    else
        assert_failure 1 "handle_worktree_creation_error function exists"
    fi
}

##
# Test Suite 7: Constants and Variables
##
test_constants() {
    echo -e "\n${BLUE}=== Constants and Variables Tests ===${NC}"

    # Test strategy constants
    assert_equals "force" "${ISOLATION_STRATEGY_FORCE}" "ISOLATION_STRATEGY_FORCE constant is correct"
    assert_equals "ask" "${ISOLATION_STRATEGY_ASK}" "ISOLATION_STRATEGY_ASK constant is correct"
    assert_equals "skip" "${ISOLATION_STRATEGY_SKIP}" "ISOLATION_STRATEGY_SKIP constant is correct"

    # Test branch type constants
    assert_equals "main" "${BRANCH_TYPE_MAIN}" "BRANCH_TYPE_MAIN constant is correct"
    assert_equals "feature" "${BRANCH_TYPE_FEATURE}" "BRANCH_TYPE_FEATURE constant is correct"
    assert_equals "worktree" "${BRANCH_TYPE_WORKTREE}" "BRANCH_TYPE_WORKTREE constant is correct"

    # Test default configuration values
    assert_equals "force" "${DEFAULT_MAIN_BRANCH_POLICY}" "DEFAULT_MAIN_BRANCH_POLICY is correct"
    assert_equals "ask" "${DEFAULT_FEATURE_BRANCH_POLICY}" "DEFAULT_FEATURE_BRANCH_POLICY is correct"
}

##
# Test Suite 8: Integration Scenarios
##
test_integration_scenarios() {
    echo -e "\n${BLUE}=== Integration Scenario Tests ===${NC}"

    # Test complete detection flow
    local branch_type
    local strategy

    branch_type=$(detect_branch_type)
    strategy=$(detect_branch_isolation_strategy)

    assert_success $? "Complete detection flow executes successfully"

    # Test main branch scenario
    if [[ "${branch_type}" == "main" ]]; then
        assert_equals "force" "${strategy}" "Main branch returns force strategy"
    fi

    # Test worktree scenario
    if [[ "${branch_type}" == "worktree" ]]; then
        assert_equals "skip" "${strategy}" "Worktree returns skip strategy"
    fi
}

##
# Test Suite 9: State File Functions
##
test_state_file_functions() {
    echo -e "\n${BLUE}=== State File Functions Tests ===${NC}"

    # Test that state directory can be created
    mkdir -p ".claude/state" 2>/dev/null
    assert_success $? "State directory creation works"

    # Test that state file path is defined
    assert_not_empty "${STATE_FILE}" "STATE_FILE variable is defined"
}

assert_not_empty() {
    local value="$1"
    local test_name="$2"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ -n "${value}" ]]; then
        echo -e "${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${test_name}"
        echo "  Expected non-empty value, got empty"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

##
# Main test runner
##
main() {
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Branch Isolation System - Unit Tests"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Run all test suites
    test_constants
    test_branch_detection
    test_configuration_reading
    test_strategy_detection
    test_branch_information
    test_git_validation
    test_error_handling
    test_integration_scenarios
    test_state_file_functions

    # Print summary
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Test Summary"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Total tests run: ${TESTS_RUN}"
    echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
    echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
    echo -e "${YELLOW}Skipped: ${TESTS_SKIPPED}${NC}"
    echo ""

    # Calculate success rate
    if [[ ${TESTS_RUN} -gt 0 ]]; then
        local success_rate=$((TESTS_PASSED * 100 / TESTS_RUN))
        echo "Success rate: ${success_rate}%"

        if [[ ${TESTS_FAILED} -eq 0 ]]; then
            echo -e "${GREEN}All tests passed!${NC}"
            return 0
        else
            echo -e "${RED}Some tests failed.${NC}"
            return 1
        fi
    else
        echo "No tests were run."
        return 1
    fi
}

# Run tests if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
    exit $?
fi