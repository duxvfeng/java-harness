#!/bin/bash
# Git Error Handler for Branch Isolation
# Provides comprehensive error handling and user-friendly error messages

# Error codes
GIT_ERROR_NOT_REPO=1         # Not in a git repository
GIT_ERROR_NO_BRANCH=2        # Cannot read current branch
GIT_ERROR_NO_WORKTREE=3      # Cannot determine worktree status
GIT_ERROR_WORKTREE_FAIL=4    # Worktree creation failed
GIT_ERROR_INVALID_STRATEGY=5  # Invalid isolation strategy
GIT_ERROR_USER_CANCEL=6      # User cancelled operation

##
# Handle Git repository not found error
# Arguments: context_description
##
handle_not_repo_error() {
    local context="${1:-Git operation}"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ❌ Git Repository Not Found
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: Branch isolation requires a Git repository

Context: ${context}

Possible reasons:
  • You are not in a Git repository directory
  • The .git directory has been deleted or corrupted
  • You don't have permission to access Git files

Resolution:
  1. Initialize a Git repository: git init
  2. Navigate to your project root directory
  3. Check if .git directory exists: ls -la .git
  4. Verify repository permissions: ls -ld .git

EOF

    return ${GIT_ERROR_NOT_REPO}
}

##
# Handle branch detection error
# Arguments: git_command_output
##
handle_branch_detection_error() {
    local git_output="${1:-Unknown error}"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ❌ Cannot Read Current Branch
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: Failed to determine current Git branch

Git output: ${git_output}

Possible reasons:
  • Git is not properly installed or configured
  • Repository is in an invalid state (HEAD corruption)
  • Insufficient permissions to read Git files
  • Repository is in the middle of a rebase/merge operation

Resolution:
  1. Verify Git installation: git --version
  2. Check repository status: git status
  3. Try fixing HEAD: git symbolic-ref HEAD refs/heads/master
  4. Complete any ongoing git operations
  5. Check repository integrity: git fsck --full

If problems persist, consider:
  • Creating a fresh repository clone
  • Restoring from backup
  • Consulting your Git hosting provider

EOF

    return ${GIT_ERROR_NO_BRANCH}
}

##
# Handle worktree detection error
# Arguments: git_command_output
##
handle_worktree_detection_error() {
    local git_output="${1:-Unknown error}"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ❌ Cannot Determine Worktree Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: Failed to determine if in a git worktree

Git output: ${git_output}

Possible reasons:
  • Git version doesn't support worktree (requires 2.7+)
  • Repository structure is corrupted
  • Git installation is incomplete

Resolution:
  1. Check Git version: git --version (need 2.7+)
  2. List existing worktrees: git worktree list
  3. Verify worktree structure: ls -la .git/worktrees 2>/dev/null
  4. Reinstall Git if worktree support is missing

Worktree feature requirements:
  • Git version 2.7 or higher
  • Standard Git installation with worktree support
  • Valid repository structure

EOF

    return ${GIT_ERROR_NO_WORKTREE}
}

##
# Handle worktree creation failure
# Arguments: worktree_path, git_command_output, error_type
##
handle_worktree_creation_error() {
    local worktree_path="$1"
    local git_output="$2"
    local error_type="${3:-creation_failed}"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ❌ Worktree Creation Failed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: Failed to create isolated worktree

Target path: ${worktree_path}
Error type: ${error_type}
Git output: ${git_output}

Possible reasons:
  • Target directory already exists
  • Insufficient disk space or permissions
  • Invalid branch name or reference
  • Git worktree support not available

Resolution steps:
  1. Check if target directory exists: ls -la "${worktree_path}"
  2. Verify disk space: df -h .
  3. Check directory permissions: ls -ld $(dirname "${worktree_path}")
  4. Test worktree creation manually:
     git worktree add -b test-branch "${worktree_path}" HEAD

Alternative solutions:
  • Use different worktree path
  • Clean up existing worktrees: git worktree prune
  • Create branch without worktree isolation
  • Check existing worktrees: git worktree list

Manual worktree cleanup:
  git worktree remove "${worktree_path}"
  git worktree prune

EOF

    return ${GIT_ERROR_WORKTREE_FAIL}
}

##
# Handle invalid strategy error
# Arguments: invalid_strategy_value
##
handle_invalid_strategy_error() {
    local invalid_strategy="$1"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ❌ Invalid Isolation Strategy
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: Unknown or invalid isolation strategy: "${invalid_strategy}"

Valid strategies are:
  • force  - Mandatory branch isolation (main branch protection)
  • ask    - User choice required (feature branch)
  • skip   - No isolation needed (already isolated)

This error indicates:
  • Configuration file contains invalid strategy value
  • Auto-detection logic failed
  • Manual strategy parameter is incorrect

Resolution:
  1. Check .claude/settings.json configuration
  2. Use auto-detection instead: --auto flag
  3. Use valid strategy name only
  4. Reset to default configuration

Example correct usage:
  bash scripts/branch-isolation/handle-isolation.sh --auto
  bash scripts/branch-isolation/handle-isolation.sh --strategy force

EOF

    return ${GIT_ERROR_INVALID_STRATEGY}
}

##
# Handle user cancellation gracefully
# Arguments: cancellation_reason
##
handle_user_cancellation() {
    local reason="${1:-User cancelled the operation}"

    cat <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   ⚠️  Operation Cancelled by User
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Reason: ${reason}

The operation has been cancelled and no changes were made.

What happens next:
  • No branches or worktrees were created
  • No files were modified
  • Your repository remains in its current state
  • The cancellation has been recorded for audit purposes

You can:
  • Retry the operation with different choices
  • Manually create branches as needed
  • Configure automatic isolation in settings
  • Proceed without branch isolation (if applicable)

Recorded decision saved to:
  .claude/state/branch-isolation-decision.json

EOF

    return ${GIT_ERROR_USER_CANCEL}
}

##
# Test Git environment and provide detailed diagnostics
# Returns: 0 if all tests pass, error code otherwise
##
test_git_environment() {
    local all_tests_passed=true
    local git_version

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Git Environment Diagnostics"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Test 1: Git installation
    echo "Test 1: Git Installation"
    if git_version=$(git --version 2>/dev/null); then
        echo "✅ Git is installed: ${git_version}"
        # Check version for worktree support
        local major_minor=$(echo "${git_version}" | sed 's/git version //' | cut -d. -f1,2)
        echo "   Version: ${major_minor}"
        if [[ "${major_minor}" < "2.7" ]]; then
            echo "⚠️  Warning: Git version ${major_minor} may not fully support worktree feature"
        else
            echo "✅ Worktree support: Available"
        fi
    else
        echo "❌ Git is not installed or not in PATH"
        all_tests_passed=false
    fi
    echo ""

    # Test 2: Repository access
    echo "Test 2: Repository Access"
    if git rev-parse --git-dir >/dev/null 2>&1; then
        local git_dir=$(git rev-parse --git-dir 2>/dev/null)
        echo "✅ Repository accessible: ${git_dir}"
    else
        echo "❌ Not in a Git repository"
        all_tests_passed=false
    fi
    echo ""

    # Test 3: Branch detection
    echo "Test 3: Branch Detection"
    if git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
        echo "✅ Current branch: ${current_branch}"
    else
        echo "❌ Cannot detect current branch"
        all_tests_passed=false
    fi
    echo ""

    # Test 4: Worktree support
    echo "Test 4: Worktree Support"
    if git worktree list >/dev/null 2>&1; then
        local worktree_count=$(git worktree list 2>/dev/null | wc -l | tr -d ' ')
        echo "✅ Worktree command works: ${worktree_count} worktree(s) found"
        git worktree list 2>/dev/null | head -3 | sed 's/^/   /'
    else
        echo "⚠️  Worktree command not available (Git version may be too old)"
    fi
    echo ""

    # Test 5: File permissions
    echo "Test 5: File Permissions"
    if [[ -d ".git" ]] && [[ -w ".git" ]]; then
        echo "✅ Repository directory is writable"
    else
        echo "⚠️  Repository directory may not be writable"
        all_tests_passed=false
    fi
    echo ""

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    if ${all_tests_passed}; then
        echo "✅ All diagnostic tests passed"
    else
        echo "❌ Some diagnostic tests failed - see details above"
    fi
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    ${all_tests_passed} && return 0 || return 1
}

# Export error handler functions
export -f handle_not_repo_error
export -f handle_branch_detection_error
export -f handle_worktree_detection_error
export -f handle_worktree_creation_error
export -f handle_invalid_strategy_error
export -f handle_user_cancellation
export -f test_git_environment

# If script is executed directly (not sourced), run diagnostics
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    test_git_environment
    exit $?
fi