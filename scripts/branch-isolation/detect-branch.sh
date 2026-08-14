#!/bin/bash
# Branch Isolation Detection for Harness Work
# Provides intelligent branch detection and isolation strategy determination

# Source the error handler
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/git-error-handler.sh" 2>/dev/null || {
    echo "Warning: Error handler not available" >&2
}

# Branch Isolation State Types
ISOLATION_STRATEGY_FORCE="force"      # Mandatory isolation (main branch)
ISOLATION_STRATEGY_ASK="ask"          # User choice required
ISOLATION_STRATEGY_SKIP="skip"        # Already isolated or no isolation needed

# Branch Types
BRANCH_TYPE_MAIN="main"               # Main/master branch
BRANCH_TYPE_FEATURE="feature"         # Feature branch
BRANCH_TYPE_WORKTREE="worktree"       # Git worktree

# State file path
STATE_FILE=".claude/state/branch-isolation-decision.json"

# Global configuration
CONFIG_FILE=".claude/settings.json"
CONFIG_KEY="branchIsolation"

# Default configuration values
DEFAULT_MAIN_BRANCH_POLICY="ask"      # Ask user before isolating the main branch
DEFAULT_FEATURE_BRANCH_POLICY="ask"   # Ask user before isolating a feature branch

##
# Validate git repository and command availability with enhanced error handling
# Returns: 0 if git commands work, 1 otherwise
##
validate_git_environment() {
    # Check if we're in a git repository
    if ! git rev-parse --git-dir >/dev/null 2>&1; then
        if command -v handle_not_repo_error >/dev/null 2>&1; then
            handle_not_repo_error "Branch isolation detection"
        else
            echo "ERROR: Not in a git repository" >&2
        fi
        return ${GIT_ERROR_NOT_REPO:-1}
    fi

    # Test basic git commands
    if ! git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        if command -v handle_branch_detection_error >/dev/null 2>&1; then
            handle_branch_detection_error "git rev-parse --abbrev-ref HEAD failed"
        else
            echo "ERROR: Cannot read current branch" >&2
        fi
        return ${GIT_ERROR_NO_BRANCH:-2}
    fi

    if ! git rev-parse --is-inside-worktree >/dev/null 2>&1; then
        if command -v handle_worktree_detection_error >/dev/null 2>&1; then
            handle_worktree_detection_error "git rev-parse --is-inside-worktree failed"
        else
            echo "ERROR: Cannot determine worktree status" >&2
        fi
        return ${GIT_ERROR_NO_WORKTREE:-3}
    fi

    return 0
}

##
# Detect current git branch type
# Returns: "main", "feature", or "worktree"
##
detect_branch_type() {
    local current_branch
    local git_dir

    # Check if we're in a git repository
    if ! git rev-parse --git-dir >/dev/null 2>&1; then
        echo "ERROR: Not in a git repository" >&2
        return 1
    fi

    # Get current branch name or commit
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    # Check if we're in a worktree (detached HEAD in worktree)
    git_dir=$(git rev-parse --git-dir 2>/dev/null)
    if [[ -d "${git_dir}/worktrees" ]] && [[ "$(git rev-parse --is-inside-worktree 2>/dev/null)" == "true" ]]; then
        echo "${BRANCH_TYPE_WORKTREE}"
        return 0
    fi

    # Check for main/master branch names
    case "${current_branch}" in
        main|master|develop|production|staging)
            echo "${BRANCH_TYPE_MAIN}"
            ;;
        *)
            echo "${BRANCH_TYPE_FEATURE}"
            ;;
    esac

    return 0
}

##
# Read branch isolation configuration
# Returns: Configuration JSON for branch isolation policy
##
read_branch_config() {
    local main_policy="${DEFAULT_MAIN_BRANCH_POLICY}"
    local feature_policy="${DEFAULT_FEATURE_BRANCH_POLICY}"

    # Check if configuration file exists
    if [[ -f "${CONFIG_FILE}" ]]; then
        # Try to read configuration using grep (no jq dependency)
        if grep -q "${CONFIG_KEY}" "${CONFIG_FILE}" 2>/dev/null; then
            # Extract main branch policy
            local main_config=$(grep -A 5 "\"${CONFIG_KEY}\"" "${CONFIG_FILE}" | grep "\"mainBranch\"" | sed 's/.*"mainBranch"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
            if [[ -n "${main_config}" ]]; then
                main_policy="${main_config}"
            fi

            # Extract feature branch policy
            local feature_config=$(grep -A 5 "\"${CONFIG_KEY}\"" "${CONFIG_FILE}" | grep "\"featureBranch\"" | sed 's/.*"featureBranch"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
            if [[ -n "${feature_config}" ]]; then
                feature_policy="${feature_config}"
            fi
        fi
    fi

    # Return as JSON object
    echo "{\"mainBranch\":\"${main_policy}\",\"featureBranch\":\"${feature_policy}\"}"
}

##
# Detect branch isolation strategy based on current branch type and configuration
# Returns: "force", "ask", or "skip"
##
detect_branch_isolation_strategy() {
    local branch_type
    local config
    local main_policy
    local feature_policy
    local strategy

    # Detect current branch type
    branch_type=$(detect_branch_type) || return 1

    # Read configuration
    config=$(read_branch_config)
    main_policy=$(echo "${config}" | grep -o '"mainBranch":"[^"]*"' | cut -d'"' -f4)
    feature_policy=$(echo "${config}" | grep -o '"featureBranch":"[^"]*"' | cut -d'"' -f4)

    # Apply default values if empty
    main_policy="${main_policy:-${DEFAULT_MAIN_BRANCH_POLICY}}"
    feature_policy="${feature_policy:-${DEFAULT_FEATURE_BRANCH_POLICY}}"

    # Determine strategy based on branch type
    case "${branch_type}" in
        "${BRANCH_TYPE_WORKTREE}")
            # Already in worktree - no additional isolation needed
            strategy="${ISOLATION_STRATEGY_SKIP}"
            ;;
        "${BRANCH_TYPE_MAIN}")
            # Use main branch policy
            strategy="${main_policy}"
            ;;
        "${BRANCH_TYPE_FEATURE}")
            # Use feature branch policy
            strategy="${feature_policy}"
            ;;
        *)
            # Default to asking user
            strategy="${ISOLATION_STRATEGY_ASK}"
            ;;
    esac

    echo "${strategy}"
    return 0
}

##
# Get detailed branch information for debugging/logging
# Returns: JSON object with branch detection details
##
get_branch_info() {
    local current_branch
    local branch_type
    local is_worktree
    local git_dir

    # Get basic git info
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
    git_dir=$(git rev-parse --git-dir 2>/dev/null)
    is_worktree="false"

    # Check if in worktree
    if [[ -d "${git_dir}/worktrees" ]] && [[ "$(git rev-parse --is-inside-worktree 2>/dev/null)" == "true" ]]; then
        is_worktree="true"
    fi

    # Detect branch type
    branch_type=$(detect_branch_type)

    # Return JSON with branch information
    cat <<EOF
{
  "currentBranch": "${current_branch}",
  "branchType": "${branch_type}",
  "isWorktree": ${is_worktree},
  "gitDir": "${git_dir}"
}
EOF
}

##
# Validate git repository and command availability with enhanced error handling
# Returns: 0 if git commands work, 1 otherwise
##
validate_git_environment() {
    # Check if we're in a git repository
    if ! git rev-parse --git-dir >/dev/null 2>&1; then
        if command -v handle_not_repo_error >/dev/null 2>&1; then
            handle_not_repo_error "Branch isolation detection"
        else
            echo "ERROR: Not in a git repository" >&2
        fi
        return ${GIT_ERROR_NOT_REPO:-1}
    fi

    # Test basic git commands
    if ! git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        if command -v handle_branch_detection_error >/dev/null 2>&1; then
            handle_branch_detection_error "git rev-parse --abbrev-ref HEAD failed"
        else
            echo "ERROR: Cannot read current branch" >&2
        fi
        return ${GIT_ERROR_NO_BRANCH:-2}
    fi

    if ! git rev-parse --is-inside-worktree >/dev/null 2>&1; then
        if command -v handle_worktree_detection_error >/dev/null 2>&1; then
            handle_worktree_detection_error "git rev-parse --is-inside-worktree failed"
        else
            echo "ERROR: Cannot determine worktree status" >&2
        fi
        return ${GIT_ERROR_NO_WORKTREE:-3}
    fi

    return 0
}

##
# Validate git repository and command availability
# Returns: 0 if git commands work, 1 otherwise
##
validate_git_environment_basic() {
    # Check if we're in a git repository
    if ! git rev-parse --git-dir >/dev/null 2>&1; then
        echo "ERROR: Not in a git repository" >&2
        return 1
    fi

    # Test basic git commands
    if ! git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        echo "ERROR: Cannot read current branch" >&2
        return 1
    fi

    if ! git rev-parse --is-inside-worktree >/dev/null 2>&1; then
        echo "ERROR: Cannot determine worktree status" >&2
        return 1
    fi

    return 0
}

# Export functions for use in other scripts
export -f detect_branch_type
export -f read_branch_config
export -f detect_branch_isolation_strategy
export -f get_branch_info
export -f validate_git_environment

# If script is executed directly (not sourced), run detection
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Parse command line arguments
    case "${1:-}" in
        --strategy)
            # Output only the strategy
            if validate_git_environment; then
                detect_branch_isolation_strategy
                exit $?
            else
                exit 1
            fi
            ;;
        --info)
            # Output detailed branch information
            if validate_git_environment; then
                get_branch_info
                exit $?
            else
                exit 1
            fi
            ;;
        --validate)
            # Validate git environment
            if validate_git_environment; then
                echo "OK: Git environment is valid"
                exit 0
            else
                exit 1
            fi
            ;;
        *)
            # Default: show usage
            cat <<EOF
Branch Isolation Detection Script

Usage: $0 [OPTIONS]

Options:
  --strategy    Detect and output isolation strategy (force/ask/skip)
  --info        Output detailed branch information as JSON
  --validate    Validate git environment

Examples:
  $0 --strategy    # Output: force, ask, or skip
  $0 --info        # Output: {"currentBranch":"main","branchType":"main","isWorktree":false,...}
  $0 --validate    # Output: OK: Git environment is valid

Isolation Strategies:
  force  - Mandatory branch isolation (required for main branch)
  ask    - User choice required (for feature branches)
  skip   - No isolation needed (already in worktree)

Branch Types:
  main    - Main/master branch (requires protection)
  feature - Feature branch (flexible isolation)
  worktree - Git worktree (already isolated)
EOF
            ;;
    esac
fi
