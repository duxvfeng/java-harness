#!/bin/bash
# Branch Isolation User Interaction Handler
# Handles user interaction for branch isolation decisions

# Source the detection functions with error handling
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/detect-branch.sh" 2>/dev/null || {
    echo "ERROR: Failed to load branch detection functions" >&2
    exit 1
}

# State file management
STATE_DIR=".claude/state"
DECISION_FILE="${STATE_DIR}/branch-isolation-decision.json"

# User response types
RESPONSE_ISOLATE="isolate"        # Create isolated branch
RESPONSE_SKIP="skip"             # Skip isolation, continue on current branch
RESPONSE_CANCEL="cancel"         # Cancel execution entirely
RESPONSE_RETAIN="retain"         # Retain isolated branch after completion

# Decision record timestamps
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

##
# Initialize state directory and file
##
init_state_file() {
    mkdir -p "${STATE_DIR}" 2>/dev/null

    # Create empty decision file if it doesn't exist
    if [[ ! -f "${DECISION_FILE}" ]]; then
        cat > "${DECISION_FILE}" <<EOF
{
  "decisions": [],
  "currentStrategy": null,
  "lastUpdated": "${TIMESTAMP}"
}
EOF
    fi
}

##
# Record user decision in state file
# Arguments: strategy, user_response, [reason]
##
record_decision() {
    local strategy="$1"
    local user_response="$2"
    local reason="${3:-No reason provided}"
    local current_branch
    local worktree_path="${4:-N/A}"

    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    # Read existing decisions
    local existing_decisions=$(cat "${DECISION_FILE}" 2>/dev/null || echo '{"decisions":[]}')

    # Create new decision entry
    local new_decision=$(cat <<EOF
{
  "timestamp": "${TIMESTAMP}",
  "branch": "${current_branch}",
  "strategy": "${strategy}",
  "userResponse": "${user_response}",
  "reason": "${reason}",
  "worktreePath": "${worktree_path}"
}
EOF
)

    # Update decision file
    cat > "${DECISION_FILE}" <<EOF
{
  "decisions": $(echo "${existing_decisions}" | jq --argjson new "${new_decision}" '.decisions + [$new]' 2>/dev/null || echo "[${new_decision}]"),
  "currentStrategy": "${strategy}",
  "lastUpdated": "${TIMESTAMP}"
}
EOF

    echo "Decision recorded: ${strategy} -> ${user_response}"
}

##
# Handle force isolation strategy (main branch)
# Returns: 0 for proceed, 1 for cancel
##
handle_force_isolation() {
    local current_branch
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    echo "🔒 Main Branch Protection Activated"
    echo ""
    echo "You are currently on the '${current_branch}' branch."
    echo "For safety, work will be performed in an isolated branch."
    echo ""
    echo "The system will automatically create a feature branch to protect"
    echo "the main branch from unstable commits."
    echo ""

    # Record forced decision
    record_decision "force" "auto-isolate" "Main branch protection required"

    return 0
}

##
# Handle ask isolation strategy (feature branch)
# Returns: 0 for proceed, 1 for cancel
##
handle_ask_isolation() {
    local current_branch
    local user_choice
    local reason

    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    echo "🔀 Feature Branch Detected"
    echo ""
    echo "You are currently on the '${current_branch}' feature branch."
    echo ""
    echo "Branch isolation protects your current branch from potential issues"
    echo "during development. You can choose:"
    echo ""
    echo "1) Isolate branch (recommended) - Create isolated worktree for this task"
    echo "2) Skip isolation - Continue directly on current branch"
    echo "3) Cancel - Stop execution and decide manually"
    echo ""

    # Get user choice
    read -p "Your choice [1/2/3]: " user_choice

    case "${user_choice}" in
        1|"isolate"|"yes"|"y")
            reason="User chose to isolate feature branch"
            record_decision "ask" "isolate" "${reason}"
            echo "✅ Branch isolation will be created"
            return 0
            ;;
        2|"skip"|"no"|"n")
            reason="User chose to skip isolation on feature branch"
            record_decision "ask" "skip" "${reason}"
            echo "⚠️  Proceeding without branch isolation"
            return 0
            ;;
        3|"cancel"|"c"|"q"|"quit")
            reason="User cancelled execution on feature branch"
            record_decision "ask" "cancel" "${reason}"
            echo "❌ Execution cancelled by user"
            return 1
            ;;
        *)
            echo "⚠️  Invalid choice. Defaulting to isolation (recommended)."
            record_decision "ask" "isolate" "Invalid choice, defaulted to isolation"
            return 0
            ;;
    esac
}

##
# Handle skip isolation strategy (already isolated)
# Returns: 0 for proceed
##
handle_skip_isolation() {
    local git_dir
    local worktree_name

    git_dir=$(git rev-parse --git-dir 2>/dev/null)

    # Try to get worktree name
    if [[ -d "${git_dir}/worktrees" ]]; then
        worktree_name=$(basename "$(pwd)")
    fi

    echo "✅ Already Isolated"
    echo ""
    echo "Current work is already isolated in a git worktree."
    echo "No additional isolation is needed."
    echo ""

    # Record skip decision
    record_decision "skip" "skip-already-isolated" "Already in worktree: ${worktree_name}"

    return 0
}

##
# Main handler function - determines appropriate interaction based on strategy
# Arguments: [strategy] (if not provided, will auto-detect)
# Returns: 0 for proceed, 1 for cancel/stop
##
handle_branch_isolation() {
    local strategy="${1:-}"
    local exit_code=0

    # Auto-detect strategy if not provided
    if [[ -z "${strategy}" ]]; then
        strategy=$(detect_branch_isolation_strategy) || {
            echo "❌ ERROR: Failed to detect isolation strategy"
            return 1
        }
    fi

    # Initialize state file
    init_state_file

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Branch Isolation Detection"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Handle based on strategy
    case "${strategy}" in
        "${ISOLATION_STRATEGY_FORCE}")
            handle_force_isolation
            exit_code=$?
            ;;
        "${ISOLATION_STRATEGY_ASK}")
            handle_ask_isolation
            exit_code=$?
            ;;
        "${ISOLATION_STRATEGY_SKIP}")
            handle_skip_isolation
            exit_code=0
            ;;
        *)
            echo "❌ ERROR: Unknown isolation strategy: ${strategy}"
            record_decision "unknown" "error" "Unknown strategy: ${strategy}"
            exit_code=1
            ;;
    esac

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    return ${exit_code}
}

##
# Get current isolation decision from state file
# Returns: Current decision JSON or empty if none
##
get_current_decision() {
    if [[ -f "${DECISION_FILE}" ]]; then
        cat "${DECISION_FILE}"
    else
        echo "{}"
    fi
}

##
# Clear all recorded decisions (for testing or reset)
##
clear_decisions() {
    init_state_file
    cat > "${DECISION_FILE}" <<EOF
{
  "decisions": [],
  "currentStrategy": null,
  "lastUpdated": "${TIMESTAMP}"
}
EOF
    echo "All decisions cleared"
}

# Export functions for use in other scripts
export -f init_state_file
export -f record_decision
export -f handle_force_isolation
export -f handle_ask_isolation
export -f handle_skip_isolation
export -f handle_branch_isolation
export -f get_current_decision
export -f clear_decisions

# If script is executed directly (not sourced), run interaction
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Parse command line arguments
    case "${1:-}" in
        --strategy)
            # Handle specific strategy
            handle_branch_isolation "${2:-}"
            exit $?
            ;;
        --auto)
            # Auto-detect and handle strategy
            handle_branch_isolation
            exit $?
            ;;
        --clear)
            # Clear all decisions
            clear_decisions
            exit 0
            ;;
        --show)
            # Show current decision
            get_current_decision
            exit 0
            ;;
        *)
            # Default: auto-detect and handle
            handle_branch_isolation
            exit $?
            ;;
    esac
fi