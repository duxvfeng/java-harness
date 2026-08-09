#!/bin/bash
# Integration Tests for Branch Isolation System
# End-to-end testing of complete workflows and scenarios

# Test framework
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project setup
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts/branch-isolation"
TEST_STATE_DIR="${PROJECT_ROOT}/.claude/state"

# Source all required scripts
source "${SCRIPTS_DIR}/detect-branch.sh" 2>/dev/null || {
    echo "Error: Cannot source detect-branch.sh"
    exit 1
}
source "${SCRIPTS_DIR}/handle-isolation.sh" 2>/dev/null || {
    echo "Error: Cannot source handle-isolation.sh"
    exit 1
}
source "${SCRIPTS_DIR}/worktree-manager.sh" 2>/dev/null || {
    echo "Error: Cannot source worktree-manager.sh"
    exit 1
}
source "${SCRIPTS_DIR}/git-error-handler.sh" 2>/dev/null || {
    echo "Error: Cannot source git-error-handler.sh"
    exit 1
}

##
# Test assertion functions
##
test_scenario() {
    local test_name="$1"
    TESTS_RUN=$((TESTS_RUN + 1))
    echo -e "${BLUE}Testing:${NC} ${test_name}"
}

assert_success() {
    local exit_code="$1"
    local test_name="$2"

    if [[ ${exit_code} -eq 0 ]]; then
        echo -e "  ${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} ${test_name} (exit code: ${exit_code})"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

assert_contains() {
    local output="$1"
    local pattern="$2"
    local test_name="$3"

    if echo "${output}" | grep -q "${pattern}"; then
        echo -e "  ${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} ${test_name}"
        echo "    Expected pattern '${pattern}' not found in output"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

##
# Setup and cleanup functions
##
setup_test_env() {
    # Create test state directory
    mkdir -p "${TEST_STATE_DIR}" 2>/dev/null
    # Clear any existing decision files
    rm -f "${TEST_STATE_DIR}/branch-isolation-decision.json" 2>/dev/null
}

cleanup_test_env() {
    # Remove test state files
    rm -f "${TEST_STATE_DIR}/branch-isolation-decision.json" 2>/dev/null
}

##
# Integration Test Suite 1: Main Branch Workflow
##
test_main_branch_workflow() {
    echo -e "\n${BLUE}=== Main Branch Workflow Integration Tests ===${NC}"
    setup_test_env

    local current_branch
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    # Only run if on main/master branch
    if [[ ! "${current_branch}" =~ ^(main|master|develop)$ ]]; then
        echo "Skipping: Not on main branch (currently: ${current_branch})"
        cleanup_test_env
        return 0
    fi

    test_scenario "Main branch detection workflow"

    # Step 1: Detect branch type
    local branch_type
    branch_type=$(detect_branch_type)
    assert_success $? "Branch type detection works"
    assert_equals "main" "${branch_type}" "Main branch correctly identified"

    # Step 2: Detect strategy
    local strategy
    strategy=$(detect_branch_isolation_strategy)
    assert_success $? "Strategy detection works"
    assert_equals "force" "${strategy}" "Main branch returns force strategy"

    # Step 3: Get branch info
    local branch_info
    branch_info=$(get_branch_info)
    assert_success $? "Branch information retrieval works"
    assert_contains "${branch_info}" "\"branchType\": \"main\"" "Branch info shows main type"

    cleanup_test_env
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local test_name="$3"

    if [[ "${expected}" == "${actual}" ]]; then
        echo -e "  ${GREEN}✓${NC} ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} ${test_name}"
        echo "    Expected: ${expected}, Got: ${actual}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

##
# Integration Test Suite 2: State File Management
##
test_state_file_management() {
    echo -e "\n${BLUE}=== State File Management Integration Tests ===${NC}"
    setup_test_env

    test_scenario "State file creation and management"

    # Step 1: Create decision file
    local decision_file="${TEST_STATE_DIR}/branch-isolation-decision.json"
    mkdir -p "${TEST_STATE_DIR}"
    echo '{}' > "${decision_file}"

    assert_success $? "State file can be created"

    # Step 2: Record a test decision
    if type record_decision >/dev/null 2>&1; then
        record_decision "test" "isolate" "Integration test" 2>/dev/null
        assert_success $? "Decision recording function works"

        # Step 3: Verify decision was recorded
        if [[ -f "${decision_file}" ]]; then
            local file_content
            file_content=$(cat "${decision_file}")
            assert_contains "${file_content}" "test" "Decision recorded in state file"
        fi
    fi

    cleanup_test_env
}

##
# Integration Test Suite 3: Configuration System
##
test_configuration_system() {
    echo -e "\n${BLUE}=== Configuration System Integration Tests ===${NC}"
    setup_test_env

    test_scenario "Configuration reading and parsing"

    # Step 1: Read configuration
    local config
    config=$(read_branch_config)
    assert_success $? "Configuration reading works"

    # Step 2: Validate configuration format
    assert_contains "${config}" "mainBranch" "Configuration contains mainBranch"
    assert_contains "${config}" "featureBranch" "Configuration contains featureBranch"

    # Step 3: Test configuration with custom file
    local test_config_file="${PROJECT_ROOT}/.claude/settings.json"
    if [[ ! -f "${test_config_file}" ]]; then
        mkdir -p "$(dirname "${test_config_file}")"
        cat > "${test_config_file}" <<EOF
{
  "branchIsolation": {
    "mainBranch": "force",
    "featureBranch": "ask"
  }
}
EOF
        assert_success $? "Test configuration file created"

        # Re-read configuration
        config=$(read_branch_config)
        assert_success $? "Configuration reading with custom file works"
    fi

    cleanup_test_env
}

##
# Integration Test Suite 4: Error Handling Workflows
##
test_error_handling_workflows() {
    echo -e "\n${BLUE}=== Error Handling Workflow Tests ===${NC}"
    setup_test_env

    test_scenario "Error handling and recovery"

    # Test 1: Git diagnostics
    if command -v test_git_environment >/dev/null 2>&1; then
        local diagnostic_output
        diagnostic_output=$(test_git_environment 2>&1)
        assert_success $? "Git diagnostics can be run"
        assert_contains "${diagnostic_output}" "Git Environment Diagnostics" "Diagnostics output contains header"
    fi

    # Test 2: Error handler availability
    local error_functions=("handle_not_repo_error" "handle_branch_detection_error" "handle_worktree_creation_error" "handle_user_cancellation")
    for func in "${error_functions[@]}"; do
        if command -v "${func}" >/dev/null 2>&1; then
            assert_success 0 "Error handler ${func} is available"
        else
            assert_failure 1 "Error handler ${func} is available"
        fi
    done

    cleanup_test_env
}

##
# Integration Test Suite 5: Complete Detection Flow
##
test_complete_detection_flow() {
    echo -e "\n${BLUE}=== Complete Detection Flow Integration Tests ===${NC}"
    setup_test_env

    test_scenario "End-to-end branch detection flow"

    # Step 1: Validate environment
    validate_git_environment >/dev/null 2>&1
    assert_success $? "Git environment validation"

    # Step 2: Detect branch type
    local branch_type
    branch_type=$(detect_branch_type)
    assert_success $? "Branch type detection"

    # Step 3: Get branch information
    local branch_info
    branch_info=$(get_branch_info)
    assert_success $? "Branch information retrieval"

    # Step 4: Detect isolation strategy
    local strategy
    strategy=$(detect_branch_isolation_strategy)
    assert_success $? "Isolation strategy detection"

    # Step 5: Validate strategy consistency
    case "${branch_type}" in
        main)
            assert_equals "force" "${strategy}" "Main branch => force strategy"
            ;;
        worktree)
            assert_equals "skip" "${strategy}" "Worktree => skip strategy"
            ;;
        feature)
            # Feature branch can be ask or force based on config
            if [[ "${strategy}" == "ask" ]] || [[ "${strategy}" == "force" ]]; then
                assert_success 0 "Feature branch => valid strategy"
            else
                assert_failure 1 "Feature branch => valid strategy"
            fi
            ;;
    esac

    cleanup_test_env
}

##
# Integration Test Suite 6: Worktree Management Integration
##
test_worktree_management_integration() {
    echo -e "\n${BLUE}=== Worktree Management Integration Tests ===${NC}"
    setup_test_env

    test_scenario "Worktree management operations"

    # Test 1: List worktrees
    local worktree_list
    worktree_list=$(list_worktrees 2>&1)
    assert_success $? "Worktree listing works"
    assert_contains "${worktree_list}" "Active Worktrees" "Worktree list header present"

    # Test 2: Worktree pruning
    prune_worktrees >/dev/null 2>&1
    assert_success $? "Worktree pruning works"

    # Test 3: Branch name generation
    local test_branch_name
    test_branch_name=$(generate_worktree_branch_name "test-task" "20250101-120000" "1234")
    assert_success $? "Branch name generation works"
    assert_contains "${test_branch_name}" "isolated-work" "Generated branch name has correct prefix"

    # Test 4: Worktree path generation
    local test_path
    test_path=$(generate_worktree_path "test-branch-name")
    assert_success $? "Worktree path generation works"
    assert_contains "${test_path}" ".claude/worktrees" "Generated path has correct base directory"

    cleanup_test_env
}

##
# Integration Test Suite 7: User Interaction Scenarios
##
test_user_interaction_scenarios() {
    echo -e "\n${BLUE}=== User Interaction Scenario Tests ===${NC}"
    setup_test_env

    test_scenario "User interaction workflows"

    # Test 1: User cancellation handling
    if command -v handle_user_cancellation >/dev/null 2>&1; then
        local cancel_output
        cancel_output=$(handle_user_cancellation "Test scenario cancellation" 2>&1)
        assert_success $? "User cancellation can be handled"
        assert_contains "${cancel_output}" "Operation Cancelled" "Cancellation message displayed"
    fi

    # Test 2: Decision state file creation
    if command -v record_decision >/dev/null 2>&1; then
        mkdir -p "${TEST_STATE_DIR}"
        record_decision "test" "cancel" "User cancellation test" "test-path" >/dev/null 2>&1
        assert_success $? "Decision recording works"

        # Verify file was created
        if [[ -f "${TEST_STATE_DIR}/branch-isolation-decision.json" ]]; then
            assert_success 0 "Decision state file created"
        else
            assert_failure 1 "Decision state file created"
        fi
    fi

    cleanup_test_env
}

##
# Main integration test runner
##
main() {
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Branch Isolation System - Integration Tests"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Testing end-to-end workflows and scenarios"
    echo "Project root: ${PROJECT_ROOT}"
    echo ""

    # Run all integration test suites
    test_main_branch_workflow
    test_state_file_management
    test_configuration_system
    test_error_handling_workflows
    test_complete_detection_flow
    test_worktree_management_integration
    test_user_interaction_scenarios

    # Print summary
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Integration Test Summary"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Total test scenarios: ${TESTS_RUN}"
    echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
    echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
    echo ""

    # Calculate success rate
    if [[ ${TESTS_RUN} -gt 0 ]]; then
        local success_rate=$((TESTS_PASSED * 100 / TESTS_RUN))
        echo "Success rate: ${success_rate}%"

        if [[ ${TESTS_FAILED} -eq 0 ]]; then
            echo -e "${GREEN}All integration tests passed!${NC}"
            return 0
        else
            echo -e "${RED}Some integration tests failed.${NC}"
            return 1
        fi
    else
        echo "No integration tests were run."
        return 1
    fi
}

# Run integration tests if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
    exit $?
fi