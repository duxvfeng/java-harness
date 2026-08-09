#!/bin/bash
# Worktree Manager for Branch Isolation
# Handles worktree creation, validation, and cleanup with comprehensive error handling

# Source the error handler
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/git-error-handler.sh" 2>/dev/null || {
    echo "Warning: Error handler not available" >&2
}

# Worktree configuration
WORKTREE_BASE_DIR=".claude/worktrees"
WORKTIME_BRANCH_PREFIX="isolated-work"

# Error codes
WORKTREE_ERROR_EXISTS=1
WORKTREE_ERROR_CREATE_FAILED=2
WORKTREE_ERROR_VALIDATION_FAILED=3
WORKTREE_ERROR_CLEANUP_FAILED=4

##
# Generate a unique worktree branch name
# Arguments: task_id, [timestamp]
# Outputs: Unique branch name
##
generate_worktree_branch_name() {
    local task_id="${1:-task}"
    local timestamp="${2:-$(date +%Y%m%d-%H%M%S)}"
    local random_suffix="${RANDOM:0:4}"

    echo "${WORKTIME_BRANCH_PREFIX}/${task_id}-${timestamp}-${random_suffix}"
}

##
# Generate worktree path for a given branch name
# Arguments: branch_name
# Outputs: Worktree directory path
##
generate_worktree_path() {
    local branch_name="$1"
    # Sanitize branch name for directory usage
    local safe_name=$(echo "${branch_name}" | sed 's/[\/]/-/g' | sed 's/--\+/-/g')
    echo "${WORKTREE_BASE_DIR}/${safe_name}"
}

##
# Check if a worktree path already exists
# Arguments: worktree_path
# Returns: 0 if exists, 1 if doesn't exist
##
worktree_path_exists() {
    local worktree_path="$1"

    if [[ -d "${worktree_path}" ]]; then
        return 0
    fi
    return 1
}

##
# Validate an existing worktree
# Arguments: worktree_path
# Returns: 0 if valid, 1 if invalid
##
validate_worktree() {
    local worktree_path="$1"
    local validation_errors=0

    echo "Validating worktree: ${worktree_path}"

    # Check if directory exists
    if [[ ! -d "${worktree_path}" ]]; then
        echo "❌ Worktree directory does not exist: ${worktree_path}"
        return ${WORKTREE_ERROR_VALIDATION_FAILED}
    fi

    # Check if .git file exists (worktree indicator)
    if [[ ! -f "${worktree_path}/.git" ]]; then
        echo "❌ Worktree .git file missing: ${worktree_path}/.git"
        validation_errors=$((validation_errors + 1))
    fi

    # Check if directory is writable
    if [[ ! -w "${worktree_path}" ]]; then
        echo "❌ Worktree directory is not writable: ${worktree_path}"
        validation_errors=$((validation_errors + 1))
    fi

    # Verify it's a proper git worktree
    if git -C "${worktree_path}" rev-parse --is-inside-worktree >/dev/null 2>&1; then
        echo "✅ Worktree is properly configured"
    else
        echo "❌ Worktree git configuration is invalid"
        validation_errors=$((validation_errors + 1))
    fi

    if [[ ${validation_errors} -eq 0 ]]; then
        echo "✅ Worktree validation passed"
        return 0
    else
        echo "❌ Worktree validation failed with ${validation_errors} error(s)"
        return ${WORKTREE_ERROR_VALIDATION_FAILED}
    fi
}

##
# Create a new worktree with comprehensive error handling
# Arguments: base_ref, task_id, [branch_name], [custom_path]
# Returns: 0 on success, error code on failure
# Outputs: worktree_path on success
##
create_worktree() {
    local base_ref="${1:-HEAD}"
    local task_id="${2:-task}"
    local branch_name="${3:-}"
    local custom_path="${4:-}"
    local worktree_path
    local worktree_branch
    local create_output
    local create_result

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Creating Isolated Worktree"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Generate branch name if not provided
    if [[ -z "${branch_name}" ]]; then
        worktree_branch=$(generate_worktree_branch_name "${task_id}")
    else
        worktree_branch="${branch_name}"
    fi

    # Generate worktree path
    if [[ -n "${custom_path}" ]]; then
        worktree_path="${custom_path}"
    else
        worktree_path=$(generate_worktree_path "${worktree_branch}")
    fi

    echo "Configuration:"
    echo "  Base ref: ${base_ref}"
    echo "  Branch: ${worktree_branch}"
    echo "  Path: ${worktree_path}"
    echo ""

    # Check if worktree path already exists
    if worktree_path_exists "${worktree_path}"; then
        echo "⚠️  Worktree path already exists: ${worktree_path}"

        # Validate existing worktree
        if validate_worktree "${worktree_path}"; then
            echo "✅ Existing worktree is valid, reusing it"
            echo "${worktree_path}"
            return 0
        else
            echo "❌ Existing worktree is invalid, cannot proceed"
            if command -v handle_worktree_creation_error >/dev/null 2>&1; then
                handle_worktree_creation_error "${worktree_path}" "Path exists and validation failed" "path_exists"
            fi
            return ${WORKTREE_ERROR_EXISTS}
        fi
    fi

    # Ensure base directory exists
    mkdir -p "$(dirname "${worktree_path}")" 2>/dev/null

    # Create the worktree
    echo "Creating worktree..."
    create_output=$(git worktree add -b "${worktree_branch}" "${worktree_path}" "${base_ref}" 2>&1)
    create_result=$?

    if [[ ${create_result} -ne 0 ]]; then
        echo "❌ Worktree creation failed"

        if command -v handle_worktree_creation_error >/dev/null 2>&1; then
            handle_worktree_creation_error "${worktree_path}" "${create_output}" "creation_failed"
        fi

        # Cleanup attempt
        echo "Attempting cleanup..."
        rm -rf "${worktree_path}" 2>/dev/null
        git worktree prune 2>/dev/null

        return ${WORKTREE_ERROR_CREATE_FAILED}
    fi

    echo "✅ Worktree created successfully"

    # Validate the created worktree
    if ! validate_worktree "${worktree_path}"; then
        echo "⚠️  Worktree created but validation failed, attempting cleanup..."

        # Rollback: remove the failed worktree
        cleanup_worktree "${worktree_path}" "${worktree_branch}"

        return ${WORKTREE_ERROR_VALIDATION_FAILED}
    fi

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✅ Worktree Ready: ${worktree_path}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Output worktree path for use by caller
    echo "${worktree_path}"
    return 0
}

##
# Cleanup a worktree safely
# Arguments: worktree_path, branch_name
# Returns: 0 on success, error code on failure
##
cleanup_worktree() {
    local worktree_path="$1"
    local branch_name="$2"
    local cleanup_result

    echo "Cleaning up worktree: ${worktree_path}"

    # Try to remove the worktree
    if git worktree remove "${worktree_path}" 2>/dev/null; then
        echo "✅ Worktree removed successfully"
        cleanup_result=0
    else
        echo "⚠️  Git worktree remove failed, trying manual cleanup..."
        # Fallback to manual removal
        rm -rf "${worktree_path}" 2>/dev/null
        cleanup_result=$?
    fi

    # Prune worktree metadata
    git worktree prune 2>/dev/null

    # Try to delete the branch if specified
    if [[ -n "${branch_name}" ]] && git rev-parse --verify "${branch_name}" >/dev/null 2>&1; then
        echo "Deleting branch: ${branch_name}"
        git branch -D "${branch_name}" 2>/dev/null
    fi

    if [[ ${cleanup_result} -eq 0 ]]; then
        echo "✅ Cleanup completed successfully"
        return 0
    else
        echo "⚠️  Cleanup completed with warnings"
        return ${WORKTREE_ERROR_CLEANUP_FAILED}
    fi
}

##
# List all worktrees with detailed information
# Returns: 0 always
##
list_worktrees() {
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Active Worktrees"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    git worktree list --verbose 2>/dev/null || echo "No worktrees found or git worktree command not available"

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

##
# Prune stale worktree metadata
# Returns: 0 always
##
prune_worktrees() {
    echo "Pruning stale worktree metadata..."
    git worktree prune 2>/dev/null
    echo "✅ Pruning completed"
    return 0
}

# Export functions for use in other scripts
export -f generate_worktree_branch_name
export -f generate_worktree_path
export -f worktree_path_exists
export -f validate_worktree
export -f create_worktree
export -f cleanup_worktree
export -f list_worktrees
export -f prune_worktrees

# If script is executed directly (not sourced), run demo
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    case "${1:-}" in
        --create)
            # Create a test worktree
            create_worktree "HEAD" "test-task" "" "${2:-}"
            ;;
        --validate)
            # Validate specified worktree
            validate_worktree "${2:-.claude/worktrees/test-branch}"
            ;;
        --cleanup)
            # Cleanup specified worktree
            cleanup_worktree "${2:-.claude/worktrees/test-branch}" "${3:-isolated-work/test-task}"
            ;;
        --list)
            # List all worktrees
            list_worktrees
            ;;
        --prune)
            # Prune stale worktree metadata
            prune_worktrees
            ;;
        *)
            # Show usage
            cat <<EOF
Worktree Manager for Branch Isolation

Usage: $0 [OPTIONS] [ARGUMENTS]

Options:
  --create [path]       Create a new isolated worktree
  --validate <path>     Validate an existing worktree
  --cleanup <path> [branch]  Cleanup and remove worktree
  --list                List all active worktrees
  --prune               Prune stale worktree metadata

Examples:
  $0 --create .claude/worktrees/my-task
  $0 --validate .claude/worktrees/my-task
  $0 --cleanup .claude/worktrees/my-task isolated-work/my-task
  $0 --list
  $0 --prune

Worktree Directory Structure:
  ${WORKTREE_BASE_DIR}/
  ├── <branch-name-1>/
  ├── <branch-name-2>/
  └── ...

Error Handling:
  • Validates worktree after creation
  • Automatic rollback on validation failure
  • Comprehensive error messages with resolution suggestions
  • Safe cleanup with fallback mechanisms
EOF
            ;;
    esac
fi